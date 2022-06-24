/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2008-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_forms;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.materialmgmt.transaction.ProductionLine;

public class DocProduction extends AcctServer {
  private static final long serialVersionUID = 1L;
  private static final Logger log4jDocProduction = LogManager.getLogger();

  private String SeqNo = "0";

  /**
   * Constructor
   * 
   * @param AD_Client_ID
   *          AD_Client_ID
   */
  public DocProduction(String AD_Client_ID, String AD_Org_ID, ConnectionProvider conn) {
    super(AD_Client_ID, AD_Org_ID, conn);
  }

  @Override
  public void loadObjectFieldProvider(ConnectionProvider conn, String stradClientId, String Id)
      throws ServletException {
    setObjectFieldProvider(DocProductionData.selectRegistro(conn, stradClientId, Id));
  }

  /**
   * Load Specific Document Details
   * 
   * @return true if loadDocumentType was set
   */
  @Override
  public boolean loadDocumentDetails(FieldProvider[] data, ConnectionProvider conn) {
    DocumentType = AcctServer.DOCTYPE_MatProduction;
    C_Currency_ID = NO_CURRENCY;
    DateDoc = data[0].getField("Movementdate");
    DateAcct = data[0].getField("Movementdate");
    loadDocumentType(); // lines require doc type
    // Contained Objects
    p_lines = loadLines(conn);
    log4jDocProduction.debug("Record_ID = " + Record_ID + " - Lines=" + p_lines.length);
    return false;
  } // loadDocumentDetails

  /**
   * Load Lines.
   * 
   * @return DocLine Array
   */
  private DocLine[] loadLines(ConnectionProvider conn) {
    ArrayList<Object> list = new ArrayList<Object>();
    DocLineProductionData[] data = null;

    try {
      data = DocLineProductionData.select(conn, Record_ID);
      log4jDocProduction.debug("LoadLines: data.len" + data.length + " record_ID " + Record_ID);
    } catch (ServletException e) {
      log4jDocProduction.warn(e);
    }

    //
    for (int i = 0; data != null && i < data.length; i++) {
      String Line_ID = data[i].getField("M_PRODUCTIONLINE_ID");
      DocLine_Material docLine = new DocLine_Material(DocumentType, Record_ID, Line_ID);
      docLine.loadAttributes(data[i], this);
      docLine.setQty(data[i].getField("MOVEMENTQTY"), conn); // sets Trx
      // and
      // Storage
      // Qty
      docLine.m_M_Locator_ID = data[i].getField("M_LOCATOR_ID");
      docLine.m_Productiontype = data[i].getField("PRODUCTIONTYPE");
      docLine.m_M_Warehouse_ID = data[i].getField("M_WAREHOUSE_ID");
      OBContext.setAdminMode(false);
      try {
        // Get related M_Transaction_ID
        ProductionLine prodLine = OBDal.getInstance().get(ProductionLine.class, Line_ID);
        if (prodLine.getMaterialMgmtMaterialTransactionList().size() > 0) {
          // Internal movement lines have 2 related transactions, both of them with the same cost
          docLine.setTransaction(prodLine.getMaterialMgmtMaterialTransactionList().get(0));
        }
      } finally {
        OBContext.restorePreviousMode();
      }
      list.add(docLine);
    }

    // Return Array
    DocLine[] dl = new DocLine[list.size()];
    list.toArray(dl);
    return dl;
  } // loadLines

  /**
   * Get Source Currency Balance - always zero
   * 
   * @return Zero (always balanced)
   */
  @Override
  public BigDecimal getBalance() {
    BigDecimal retValue = ZERO;

    return retValue;
  } // getBalance

  /**
   * Create Facts (the accounting logic) for
   * 
   * @param as
   *          accounting schema
   * @return Fact
   */
  @Override
  public Fact createFact(AcctSchema as, ConnectionProvider conn, Connection con,
      VariablesSecureApp vars) throws ServletException {
    // Select specific definition
    String strClassname = AcctServerData.selectTemplateDoc(conn, as.m_C_AcctSchema_ID,
        DocumentType);
    if (strClassname.equals("")) {
      strClassname = AcctServerData.selectTemplate(conn, as.m_C_AcctSchema_ID, AD_Table_ID);
    }
    if (!strClassname.equals("")) {
      try {
        DocProductionTemplate newTemplate = (DocProductionTemplate) Class.forName(strClassname)
            .getDeclaredConstructor()
            .newInstance();
        return newTemplate.createFact(this, as, conn, con, vars);
      } catch (Exception e) {
        log4jDocProduction
            .error("Error while creating new instance for DocProductionTemplate - " + e);
      }
    }
    log4jDocProduction.debug("createFact - Inicio");
    // create Fact Header
    Fact fact = null;
    FactLine factLine = null;
    String Fact_Acct_Group_ID = SequenceIdData.getUUID();
    log4jDocProduction.debug("createFact - object created");
    // Lines
    int countProductionlinesWithTrnCostZero = 0;
    for (int i = 0; i < p_lines.length; i++) {
      DocLine_Material line = (DocLine_Material) p_lines[i];
      if (line.transaction != null && line.transaction.getTransactionCost() != null
          && line.transaction.getTransactionCost().compareTo(ZERO) == 0) {
        countProductionlinesWithTrnCostZero++;
      }
    }
    if (p_lines.length == countProductionlinesWithTrnCostZero) {
      setStatus(STATUS_DocumentDisabled);
    }
    fact = new Fact(this, as, Fact.POST_Actual);
    for (int i = 0; p_lines != null && i < p_lines.length; i++) {
      DocLine_Material line = (DocLine_Material) p_lines[i];
      Currency costCurrency = FinancialUtils
          .getLegalEntityCurrency(OBDal.getInstance().get(Organization.class, line.m_AD_Org_ID));
      if (line.transaction != null && line.transaction.getCurrency() != null) {
        costCurrency = line.transaction.getCurrency();
      }
      if (line.transaction != null && !line.transaction.isCostCalculated()) {
        Map<String, String> parameters = getNotCalculatedCostParameters(line.transaction);
        setMessageResult(conn, STATUS_NotCalculatedCost, "error", parameters);
        throw new OBException("@NotCalculatedCost@");
      }
      String costs = line.getProductCosts(DateAcct, as, conn, con);
      log4jDocProduction
          .debug("DocProduction - createFact - line.m_Productiontype - " + line.m_Productiontype);
      if (line.m_Productiontype.equals("+")) {
        factLine = fact.createLine(line, line.getAccount(ProductInfo.ACCTTYPE_P_Asset, as, conn),
            costCurrency.getId(), costs, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType,
            conn);
        if (factLine != null) {
          factLine.setM_Locator_ID(line.m_M_Locator_ID);
        }
        factLine = fact.createLine(line, getAccountWarehouse(line.m_M_Warehouse_ID, as, conn),
            costCurrency.getId(), "", costs, Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType,
            conn);
        if (factLine != null) {
          factLine.setM_Locator_ID(line.m_M_Locator_ID);
        }
      } else {
        factLine = fact.createLine(line, line.getAccount(ProductInfo.ACCTTYPE_P_Asset, as, conn),
            costCurrency.getId(), "", costs, Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType,
            conn);
        if (factLine != null) {
          factLine.setM_Locator_ID(line.m_M_Locator_ID);
        }
        factLine = fact.createLine(line, getAccountWarehouse(line.m_M_Warehouse_ID, as, conn),
            costCurrency.getId(), costs, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType,
            conn);
        if (factLine != null) {
          factLine.setM_Locator_ID(line.m_M_Locator_ID);
        }
      }
    }
    SeqNo = "0";
    return fact;
  } // createFact

  /**
   * @return the seqNo
   */
  public String getSeqNo() {
    return SeqNo;
  }

  /**
   * @param seqNo
   *          the seqNo to set
   */
  public void setSeqNo(String seqNo) {
    SeqNo = seqNo;
  }

  /**
   * @return the serialVersionUID
   */
  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public String nextSeqNo(String oldSeqNo) {
    log4jDocProduction.debug("DocAmortization - oldSeqNo = " + oldSeqNo);
    BigDecimal seqNo = new BigDecimal(oldSeqNo);
    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
    log4jDocProduction.debug("DocAmortization - nextSeqNo = " + SeqNo);
    return SeqNo;
  }

  /**
   * Get the account for Accounting Schema
   * 
   * @param strmWarehouseId
   *          warehouse
   * @param as
   *          accounting schema
   * @return Account
   */
  public final Account getAccountWarehouse(String strmWarehouseId, AcctSchema as,
      ConnectionProvider conn) throws ServletException {
    AcctServerData[] data = null;
    data = AcctServerData.selectWDifferencesAcct(conn, strmWarehouseId, as.getC_AcctSchema_ID());
    // Get Acct
    String Account_ID = "";
    if (data != null && data.length != 0) {
      Account_ID = data[0].accountId;
    } else {
      return null;
    }
    // No account
    if (Account_ID.equals("")) {
      return null;
    }
    // if (log4j.isDebugEnabled())
    // log4j.debug("AcctServer - *******************************getAccount 4");
    // Return Account
    Account acct = null;
    try {
      acct = Account.getAccount(conn, Account_ID);
    } catch (ServletException e) {
      log4jDocProduction.warn(e);
    }
    return acct;
  }

  /**
   * Get Document Confirmation
   * 
   * not used
   */
  @Override
  public boolean getDocumentConfirmation(ConnectionProvider conn, String strRecordId) {
    return true;
  }

  @Override
  public String getServletInfo() {
    return "Servlet for the accounting";
  } // end of getServletInfo() method
}
