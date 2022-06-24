/*
 *************************************************************************
 * The contents of conn file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use conn
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2013-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.businessUtility;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.calendar.Year;

public class EndYearCloseUtility {

  private Organization organization;
  private Year year;
  private ConnectionProvider conn;
  private Connection con;
  private VariablesSecureApp vars;
  private BigDecimal ExpenseAmtDr = new BigDecimal("0");
  private BigDecimal ExpenseAmtCr = new BigDecimal("0");
  private BigDecimal RevenueAmtDr = new BigDecimal("0");
  private BigDecimal RevenueAmtCr = new BigDecimal("0");
  protected Logger log4j = LogManager.getLogger();

  public EndYearCloseUtility(Organization _organization, Year _year, ConnectionProvider _conn,
      Connection _con, VariablesSecureApp _vars) {
    organization = _organization;
    year = _year;
    conn = _conn;
    vars = _vars;
    con = _con;
  }

  public OBError processYearClose() {
    String strYearId = year.getId();
    String strOrgId = organization.getId();
    OBError myError = new OBError();
    try {
      boolean isYearNotClose = EndYearCloseUtilityData.selectYearNotClosed(conn, strYearId,
          strOrgId);
      if (isYearNotClose) {
        throw new OBException(Utility.messageBD(conn, "YearNotClose", vars.getLanguage()));
      }
      EndYearCloseUtilityData[] dataOrgs = EndYearCloseUtilityData.treeOrg(conn, vars.getClient(),
          strOrgId);
      EndYearCloseUtilityData[] dataOrgAcctSchemas = EndYearCloseUtilityData
          .treeOrgAcctSchemas(conn, vars.getClient(), strOrgId);
      EndYearCloseUtilityData[] acctSchema = EndYearCloseUtilityData.treeAcctSchema(conn,
          vars.getClient(), strOrgId);
      String strPediodId = EndYearCloseUtilityData.getLastPeriod(conn, strYearId);
      Set<String> closedOrganizations = new HashSet<String>();
      for (int j = 0; j < acctSchema.length; j++) {
        String balanceAmount = EndYearCloseUtilityData.balanceAmount(conn, strYearId,
            acctSchema[j].id,
            Utility.getInStrSet(new OrganizationStructureProvider().getChildTree(strOrgId, true)));
        if (BigDecimal.ZERO.compareTo(new BigDecimal(balanceAmount)) != 0) {
          Map<String, String> parameters = new HashMap<String, String>();
          try {
            OBContext.setAdminMode();
            AcctSchema schema = OBDal.getInstance().get(AcctSchema.class, acctSchema[j].id);
            parameters.put("AcctSchema", schema.getName());
          } finally {
            OBContext.restorePreviousMode();
          }
          throw new OBException(Utility.parseTranslation(conn, vars, parameters, vars.getLanguage(),
              Utility.messageBD(conn, "BalanceIsNotBalanced", vars.getLanguage())));
        }
        String strRegId = SequenceIdData.getUUID();
        String strCloseId = SequenceIdData.getUUID();
        String strOpenId = SequenceIdData.getUUID();
        String strDivideUpId = SequenceIdData.getUUID();
        boolean createClosing = OBDal.getInstance()
            .get(AcctSchema.class, acctSchema[j].id)
            .getFinancialMgmtAcctSchemaGLList()
            .size() > 0
                ? OBDal.getInstance()
                    .get(AcctSchema.class, acctSchema[j].id)
                    .getFinancialMgmtAcctSchemaGLList()
                    .get(0)
                    .isCreateClosing()
                : true;
        EndYearCloseUtilityData[] retainedEarningAccount = EndYearCloseUtilityData
            .retainedearning(conn, acctSchema[j].id);
        if (retainedEarningAccount == null || retainedEarningAccount.length == 0) {
          strDivideUpId = "";
        }
        for (int i = 0; i < dataOrgs.length; i++) {
          if (log4j.isDebugEnabled()) {
            log4j.debug("Output: Before buttonReg");
          }
          String regCount = EndYearCloseUtilityData.getRegCount(conn, vars.getClient(),
              dataOrgs[i].org, acctSchema[j].id, strPediodId);
          if (Integer.parseInt(regCount) > 0) {
            throw new OBException(
                Utility.messageBD(conn, "RegularizationDoneAlready", vars.getLanguage()));
          }
          String strRegOut = processButtonReg(strYearId, dataOrgs[i].org, strRegId,
              acctSchema[j].id, strDivideUpId, retainedEarningAccount);
          String strCloseOut = createClosing
              ? processButtonClose(strYearId, dataOrgs[i].org, strCloseId, strOpenId,
                  acctSchema[j].id, strDivideUpId)
              : "Success";
          if (!createClosing) {
            strCloseId = "";
            strOpenId = "";
          }
          if (!strRegOut.equals("Success")) {
            throw new OBException(Utility.messageBD(conn, "ProcessRunError", vars.getLanguage()));
          } else if (!strCloseOut.equals("Success")) {
            throw new OBException(
                Utility.messageBD(conn, "ProcessRunError_CreateNextPeriod", vars.getLanguage()));
          }
          ExpenseAmtDr = BigDecimal.ZERO;
          ExpenseAmtCr = BigDecimal.ZERO;
          RevenueAmtDr = BigDecimal.ZERO;
          RevenueAmtCr = BigDecimal.ZERO;
        }
        for (int i = 0; i < dataOrgAcctSchemas.length; i++) {
          String strOrgSchemaId = EndYearCloseUtilityData.orgAcctschema(conn,
              dataOrgAcctSchemas[i].org, acctSchema[j].id);
          if (strOrgSchemaId != null && !strOrgSchemaId.equals("")) {
            if (EndYearCloseUtilityData.insertOrgClosing(con, conn, vars.getClient(), strOrgId,
                vars.getUser(), strYearId, strOrgSchemaId, strRegId, strCloseId, strDivideUpId,
                strOpenId) == 0) {
              throw new OBException(Utility.messageBD(conn, "ProcessRunError", vars.getLanguage()));
            }
          }
        }
        if (!closedOrganizations.contains(strOrgId)) {
          if (EndYearCloseUtilityData.updateClose(con, conn, vars.getUser(), strYearId,
              strOrgId) == 0) {
            throw new OBException(
                Utility.messageBD(conn, "AllPeriodsPermanentClosed", vars.getLanguage()));
          }
        }
        closedOrganizations.add(strOrgId);
      }

      conn.releaseCommitConnection(con);
      myError.setType("Success");
      myError.setTitle("");
      myError.setMessage(Utility.messageBD(conn, "Success", vars.getLanguage()));
    } catch (Exception e) {
      log4j.error(e);
      try {
        conn.releaseRollbackConnection(con);
      } catch (Exception ignored) {
      }
      myError = Utility.translateError(conn, vars, vars.getLanguage(), e.getMessage());
    }
    return myError;
  }

  private String processButtonReg(String strYearId, String stradOrgId, String strFact_Acct_Group_ID,
      String strAcctSchema, String strDivideUpId, EndYearCloseUtilityData[] account2)
      throws ServletException {
    String Fact_Acct_ID = "";
    String Fact_Acct_Group_ID = strFact_Acct_Group_ID;
    String strPediodId = EndYearCloseUtilityData.getLastPeriod(conn, strYearId);
    String strRegEntry = Utility.messageBD(conn, "RegularizationEntry", vars.getLanguage());
    String currency = EndYearCloseUtilityData.cCurrencyId(conn, strAcctSchema);
    EndYearCloseUtilityData[] totalAmountsExpense = EndYearCloseUtilityData.getTotalAmounts(con,
        conn, strYearId, "E", stradOrgId, strAcctSchema);
    ExpenseAmtDr = ExpenseAmtDr.add(new BigDecimal(totalAmountsExpense[0].totalamtdr));
    ExpenseAmtCr = ExpenseAmtCr.add(new BigDecimal(totalAmountsExpense[0].totalamtcr));
    EndYearCloseUtilityData[] totalAmountsRevenue = EndYearCloseUtilityData.getTotalAmounts(con,
        conn, strYearId, "R", stradOrgId, strAcctSchema);
    RevenueAmtDr = RevenueAmtDr.add(new BigDecimal(totalAmountsRevenue[0].totalamtdr));
    RevenueAmtCr = RevenueAmtCr.add(new BigDecimal(totalAmountsRevenue[0].totalamtcr));
    // Inserts income summary statement
    EndYearCloseUtilityData.insertSelect(con, conn, vars.getClient(), stradOrgId, vars.getUser(),
        EndYearCloseUtilityData.getEndDate(conn, strPediodId), strPediodId, currency,
        Fact_Acct_Group_ID, "10", "R", strRegEntry, strYearId, "'E'", strAcctSchema, "");
    EndYearCloseUtilityData.insertSelect(con, conn, vars.getClient(), stradOrgId, vars.getUser(),
        EndYearCloseUtilityData.getEndDate(conn, strPediodId), strPediodId, currency,
        Fact_Acct_Group_ID, "20", "R", strRegEntry, strYearId, "'R'", strAcctSchema, "");
    EndYearCloseUtilityData[] account = EndYearCloseUtilityData.incomesummary(conn, strAcctSchema);
    if (ExpenseAmtDr.add(RevenueAmtDr).subtract(RevenueAmtCr).subtract(ExpenseAmtCr).signum() > 0) {
      Fact_Acct_ID = SequenceIdData.getUUID();
      EndYearCloseUtilityData.insert(con, conn, Fact_Acct_ID, vars.getClient(), stradOrgId,
          vars.getUser(), strAcctSchema, account[0].accountId,
          EndYearCloseUtilityData.getEndDate(conn, strPediodId), strPediodId,
          EndYearCloseUtilityData.adTableId(conn), "A",
          EndYearCloseUtilityData.cCurrencyId(conn, strAcctSchema), "0",
          ExpenseAmtDr.add(RevenueAmtDr).subtract(RevenueAmtCr).subtract(ExpenseAmtCr).toString(),
          "0",
          ExpenseAmtDr.add(RevenueAmtDr).subtract(RevenueAmtCr).subtract(ExpenseAmtCr).toString(),
          Fact_Acct_Group_ID, "10", account[0].name, account[0].value, account[0].cBpartnerId,
          account[0].mProductId, account[0].aAssetId, strRegEntry, account[0].cTaxId,
          account[0].cProjectId, account[0].cActivityId, account[0].user1Id, account[0].user2Id,
          account[0].cCampaignId, account[0].cSalesregionId);
    } else if (ExpenseAmtDr.add(RevenueAmtDr)
        .subtract(RevenueAmtCr)
        .subtract(ExpenseAmtCr)
        .signum() < 0) {
      Fact_Acct_ID = SequenceIdData.getUUID();

      EndYearCloseUtilityData.insert(con, conn, Fact_Acct_ID, vars.getClient(), stradOrgId,
          vars.getUser(), strAcctSchema, account[0].accountId,
          EndYearCloseUtilityData.getEndDate(conn, strPediodId), strPediodId,
          EndYearCloseUtilityData.adTableId(conn), "A",
          EndYearCloseUtilityData.cCurrencyId(conn, strAcctSchema),
          ExpenseAmtCr.add(RevenueAmtCr).subtract(RevenueAmtDr).subtract(ExpenseAmtDr).toString(),
          "0",
          ExpenseAmtCr.add(RevenueAmtCr).subtract(RevenueAmtDr).subtract(ExpenseAmtDr).toString(),
          "0", Fact_Acct_Group_ID, "10", account[0].name, account[0].value, account[0].cBpartnerId,
          account[0].mProductId, account[0].aAssetId, strRegEntry, account[0].cTaxId,
          account[0].cProjectId, account[0].cActivityId, account[0].user1Id, account[0].user2Id,
          account[0].cCampaignId, account[0].cSalesregionId);
    }
    // Inserts retained earning statement
    String strClosingEntry = Utility.messageBD(conn, "ClosingEntry", vars.getLanguage());
    if (account2 != null && account2.length > 0) {
      if (ExpenseAmtDr.add(RevenueAmtDr)
          .subtract(RevenueAmtCr)
          .subtract(ExpenseAmtCr)
          .signum() > 0) {
        Fact_Acct_ID = SequenceIdData.getUUID();
        EndYearCloseUtilityData.insertClose(con, conn, Fact_Acct_ID, vars.getClient(), stradOrgId,
            vars.getUser(), strAcctSchema, account[0].accountId,
            EndYearCloseUtilityData.getEndDate(conn, strPediodId), strPediodId,
            EndYearCloseUtilityData.adTableId(conn), "A",
            EndYearCloseUtilityData.cCurrencyId(conn, strAcctSchema),
            ExpenseAmtDr.add(RevenueAmtDr).subtract(RevenueAmtCr).subtract(ExpenseAmtCr).toString(),
            "0",
            ExpenseAmtDr.add(RevenueAmtDr).subtract(RevenueAmtCr).subtract(ExpenseAmtCr).toString(),
            "0", strDivideUpId, "10", "D", account[0].name, account[0].value,
            account[0].cBpartnerId, account[0].mProductId, account[0].aAssetId, strClosingEntry,
            account[0].cTaxId, account[0].cProjectId, account[0].cActivityId, account[0].user1Id,
            account[0].user2Id, account[0].cCampaignId, account[0].cSalesregionId);
        Fact_Acct_ID = SequenceIdData.getUUID();
        EndYearCloseUtilityData.insertClose(con, conn, Fact_Acct_ID, vars.getClient(), stradOrgId,
            vars.getUser(), strAcctSchema, account2[0].accountId,
            EndYearCloseUtilityData.getEndDate(conn, strPediodId), strPediodId,
            EndYearCloseUtilityData.adTableId(conn), "A",
            EndYearCloseUtilityData.cCurrencyId(conn, strAcctSchema), "0",
            ExpenseAmtDr.add(RevenueAmtDr).subtract(RevenueAmtCr).subtract(ExpenseAmtCr).toString(),
            "0",
            ExpenseAmtDr.add(RevenueAmtDr).subtract(RevenueAmtCr).subtract(ExpenseAmtCr).toString(),
            strDivideUpId, "10", "D", account2[0].name, account2[0].value, account2[0].cBpartnerId,
            account2[0].mProductId, account2[0].aAssetId, strClosingEntry, account2[0].cTaxId,
            account2[0].cProjectId, account2[0].cActivityId, account2[0].user1Id,
            account2[0].user2Id, account2[0].cCampaignId, account2[0].cSalesregionId);
      } else if (ExpenseAmtDr.add(RevenueAmtDr)
          .subtract(RevenueAmtCr)
          .subtract(ExpenseAmtCr)
          .signum() < 0) {
        Fact_Acct_ID = SequenceIdData.getUUID();
        EndYearCloseUtilityData.insertClose(con, conn, Fact_Acct_ID, vars.getClient(), stradOrgId,
            vars.getUser(), strAcctSchema, account[0].accountId,
            EndYearCloseUtilityData.getEndDate(conn, strPediodId), strPediodId,
            EndYearCloseUtilityData.adTableId(conn), "A",
            EndYearCloseUtilityData.cCurrencyId(conn, strAcctSchema), "0",
            ExpenseAmtCr.add(RevenueAmtCr).subtract(RevenueAmtDr).subtract(ExpenseAmtDr).toString(),
            "0",
            ExpenseAmtCr.add(RevenueAmtCr).subtract(RevenueAmtDr).subtract(ExpenseAmtDr).toString(),
            strDivideUpId, "10", "D", account[0].name, account[0].value, account[0].cBpartnerId,
            account[0].mProductId, account[0].aAssetId, strClosingEntry, account[0].cTaxId,
            account[0].cProjectId, account[0].cActivityId, account[0].user1Id, account[0].user2Id,
            account[0].cCampaignId, account[0].cSalesregionId);
        Fact_Acct_ID = SequenceIdData.getUUID();
        EndYearCloseUtilityData.insertClose(con, conn, Fact_Acct_ID, vars.getClient(), stradOrgId,
            vars.getUser(), strAcctSchema, account2[0].accountId,
            EndYearCloseUtilityData.getEndDate(conn, strPediodId), strPediodId,
            EndYearCloseUtilityData.adTableId(conn), "A",
            EndYearCloseUtilityData.cCurrencyId(conn, strAcctSchema),
            ExpenseAmtCr.add(RevenueAmtCr).subtract(RevenueAmtDr).subtract(ExpenseAmtDr).toString(),
            "0",
            ExpenseAmtCr.add(RevenueAmtCr).subtract(RevenueAmtDr).subtract(ExpenseAmtDr).toString(),
            "0", strDivideUpId, "10", "D", account2[0].name, account2[0].value,
            account2[0].cBpartnerId, account2[0].mProductId, account2[0].aAssetId, strClosingEntry,
            account2[0].cTaxId, account2[0].cProjectId, account2[0].cActivityId,
            account2[0].user1Id, account2[0].user2Id, account2[0].cCampaignId,
            account2[0].cSalesregionId);
      }
    }
    return "Success";
  }

  private String processButtonClose(String strYearId, String stradOrgId, String strCloseID,
      String strOpenID, String strAcctSchema, String strDivideUpId) throws ServletException {
    String Fact_Acct_Group_ID = strCloseID;
    String strPediodId = EndYearCloseUtilityData.getLastPeriod(conn, strYearId);
    String newPeriod = EndYearCloseUtilityData.getNextPeriod(conn, strPediodId);
    String strOpeningEntry = Utility.messageBD(conn, "OpeningEntry", vars.getLanguage());
    String strClosingEntry = Utility.messageBD(conn, "ClosingEntry", vars.getLanguage());
    if (newPeriod.equals("")) {
      return "ProcessRunError";
    }

    String currency = EndYearCloseUtilityData.cCurrencyId(conn, strAcctSchema);

    EndYearCloseUtilityData.insertSelect(con, conn, vars.getClient(), stradOrgId, vars.getUser(),
        EndYearCloseUtilityData.getEndDate(conn, strPediodId), strPediodId, currency,
        Fact_Acct_Group_ID, "20", "C", strClosingEntry, strYearId, "'A'", strAcctSchema,
        strDivideUpId);

    EndYearCloseUtilityData.insertSelect(con, conn, vars.getClient(), stradOrgId, vars.getUser(),
        EndYearCloseUtilityData.getEndDate(conn, strPediodId), strPediodId, currency,
        Fact_Acct_Group_ID, "10", "C", strClosingEntry, strYearId, "'L','O'", strAcctSchema,
        strDivideUpId);

    String Fact_Acct_Group_ID2 = strOpenID;
    EndYearCloseUtilityData.insertSelectOpening(con, conn, vars.getClient(), stradOrgId,
        vars.getUser(), EndYearCloseUtilityData.getStartDate(conn, newPeriod), newPeriod, currency,
        Fact_Acct_Group_ID2, "20", "O", strOpeningEntry, strYearId, "'A','L','O'", strAcctSchema,
        strDivideUpId);

    return "Success";
  }

  public OBError processUndoYearClose() {
    String stradOrgId = organization.getId();
    String strYearId = year.getId();
    OBError myError = null;
    try {
      String strRegFactAcctGroupId = "";
      String strCloseFactAcctGroupId = "";
      String strDivideUpFactAcctGroupId = "";
      String strOpenUpFactAcctGroupId = "";
      String strOrgClosingId = "";
      try {
        EndYearCloseUtilityData[] data = EndYearCloseUtilityData.selectFactAcctGroupId(conn,
            stradOrgId, strYearId);
        if (data != null && data.length != 0) {
          for (int i = 0; i < data.length; i++) {
            strRegFactAcctGroupId = data[i].regFactAcctGroupId;
            strCloseFactAcctGroupId = data[i].closeFactAcctGroupId;
            strDivideUpFactAcctGroupId = data[i].divideupFactAcctGroupId;
            strOpenUpFactAcctGroupId = data[i].openFactAcctGroupId;
            strOrgClosingId = data[i].adOrgClosingId;
            String strResult = processUndoYearClose(strYearId, stradOrgId, strRegFactAcctGroupId,
                strCloseFactAcctGroupId, strDivideUpFactAcctGroupId, strOpenUpFactAcctGroupId,
                strOrgClosingId);
            if (!"ProcessOK".equals(strResult)) {
              throw new OBException(Utility.messageBD(conn, strResult, vars.getLanguage()));
            }
          }
          EndYearCloseUtilityData.updatePeriodsOpen(con, conn, vars.getUser(), strYearId,
              stradOrgId);
        }
      } catch (ServletException ex) {
        throw new OBException(Utility.messageBD(conn, ex.getMessage(), vars.getLanguage()));
      }

      conn.releaseCommitConnection(con);
      myError = new OBError();
      myError.setType("Success");
      myError.setTitle("");
      myError.setMessage(Utility.messageBD(conn, "Success", vars.getLanguage()));
    } catch (Exception e) {
      log4j.error(e);
      try {
        conn.releaseRollbackConnection(con);
      } catch (Exception ignored) {
      }
      myError = Utility.translateError(conn, vars, vars.getLanguage(), e.getMessage());
    }
    return myError;
  }

  private String processUndoYearClose(String strYearId, String stradOrgId,
      String strRegFactAcctGroupId, String strCloseFactAcctGroupId,
      String strDivideUpFactAcctGroupId, String strOpenUpFactAcctGroupId, String strOrgClosingId)
      throws ServletException {
    boolean isYearNotAllowed = EndYearCloseUtilityData.selectUndoAllowed(conn, strYearId,
        stradOrgId);
    if (isYearNotAllowed) {
      return "UndoNotAllowedForYear";
    }
    EndYearCloseUtilityData.deleteOrgClosing(con, conn, strOrgClosingId);
    EndYearCloseUtilityData.deleteFactAcctClose(con, conn, strRegFactAcctGroupId,
        strCloseFactAcctGroupId, strDivideUpFactAcctGroupId, strOpenUpFactAcctGroupId, stradOrgId);
    return "ProcessOK";
  }
}
