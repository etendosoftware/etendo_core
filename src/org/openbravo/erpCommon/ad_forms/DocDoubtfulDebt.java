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
 * All portions are Copyright (C) 2013-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_forms;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.List;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.FieldProviderFactory;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.AcctSchemaTableDocType;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaTable;
import org.openbravo.model.financialmgmt.payment.DoubtfulDebt;

public class DocDoubtfulDebt extends AcctServer {
  private String SeqNo = "0";

  public DocDoubtfulDebt() {
  }

  public DocDoubtfulDebt(final String AD_Client_ID, final String AD_Org_ID,
      final ConnectionProvider connectionProvider) {
    super(AD_Client_ID, AD_Org_ID, connectionProvider);
  }

  @Override
  public void loadObjectFieldProvider(final ConnectionProvider conn, final String pAdClientId,
      final String id) throws ServletException {
    final DoubtfulDebt dd = OBDal.getInstance().get(DoubtfulDebt.class, id);
    final FieldProviderFactory[] data = new FieldProviderFactory[1];
    data[0] = new FieldProviderFactory(null);
    FieldProviderFactory.setField(data[0], "AD_Client_ID", dd.getClient().getId());
    FieldProviderFactory.setField(data[0], "AD_Org_ID", dd.getOrganization().getId());
    FieldProviderFactory.setField(data[0], "DocumentNo", dd.getDocumentNo());
    String strAcctDate = OBDateUtils.formatDate(dd.getAccountingDate());
    FieldProviderFactory.setField(data[0], "DateAcct", strAcctDate);
    FieldProviderFactory.setField(data[0], "DateDoc", strAcctDate);
    FieldProviderFactory.setField(data[0], "C_BPartner_ID", dd.getBusinessPartner().getId());
    FieldProviderFactory.setField(data[0], "C_DocType_ID", dd.getDocumentType().getId());
    FieldProviderFactory.setField(data[0], "C_Currency_ID", dd.getCurrency().getId());
    FieldProviderFactory.setField(data[0], "Description", dd.getDescription());
    FieldProviderFactory.setField(data[0], "Amount", dd.getAmount().toString());
    FieldProviderFactory.setField(data[0], "Posted", dd.getPosted());
    FieldProviderFactory.setField(data[0], "Processed", dd.isProcessed() ? "Y" : "N");
    FieldProviderFactory.setField(data[0], "Processing", dd.isProcessNow() ? "Y" : "N");
    // Accounting dimensions
    FieldProviderFactory.setField(data[0], "cProjectId",
        dd.getProject() != null ? dd.getProject().getId() : "");
    FieldProviderFactory.setField(data[0], "cCampaignId",
        dd.getSalesCampaign() != null ? dd.getSalesCampaign().getId() : "");
    FieldProviderFactory.setField(data[0], "cActivityId",
        dd.getActivity() != null ? dd.getActivity().getId() : "");
    FieldProviderFactory.setField(data[0], "user1Id",
        dd.getStDimension() != null ? dd.getStDimension().getId() : "");
    FieldProviderFactory.setField(data[0], "user2Id",
        dd.getNdDimension() != null ? dd.getNdDimension().getId() : "");
    FieldProviderFactory.setField(data[0], "cCostcenterId",
        dd.getCostCenter() != null ? dd.getCostCenter().getId() : "");
    FieldProviderFactory.setField(data[0], "aAssetId",
        dd.getAsset() != null ? dd.getAsset().getId() : "");

    setObjectFieldProvider(data);

  }

  @Override
  public boolean loadDocumentDetails(final FieldProvider[] data, final ConnectionProvider conn) {
    loadDocumentType();
    Amounts[AMTTYPE_Gross] = data[0].getField("Amount");
    // p_lines = loadLines();
    return true;
  }

  @Override
  public BigDecimal getBalance() {
    return BigDecimal.ZERO;
  }

  @Override
  public Fact createFact(final AcctSchema as, final ConnectionProvider conn, final Connection con,
      final VariablesSecureApp vars) throws ServletException {
    String strClassname = "";
    final Fact fact = new Fact(this, as, Fact.POST_Actual);
    String factAcctGroupID = SequenceIdData.getUUID();

    try {
      OBContext.setAdminMode(false);
      //@formatter:off
      final String hql =
              "as astdt " +
              " where astdt.acctschemaTable.accountingSchema.id = :acctSchemaID" +
              "   and astdt.acctschemaTable.table.id = :tableID" +
              "   and astdt.documentCategory = :documentType";
      //@formatter:on

      final List<AcctSchemaTableDocType> acctSchemaTableDocTypes = OBDal.getInstance()
          .createQuery(AcctSchemaTableDocType.class, hql)
          .setNamedParameter("acctSchemaID", as.m_C_AcctSchema_ID)
          .setNamedParameter("tableID", AD_Table_ID)
          .setNamedParameter("documentType", DocumentType)
          .list();

      if (acctSchemaTableDocTypes != null && !acctSchemaTableDocTypes.isEmpty()
          && acctSchemaTableDocTypes.get(0).getCreatefactTemplate() != null) {
        strClassname = acctSchemaTableDocTypes.get(0).getCreatefactTemplate().getClassname();
      }

      if (strClassname.equals("")) {
        //@formatter:off
        final String hqlWhere =
                      "as ast " +
                      " where ast.accountingSchema.id = :acctSchemaID" +
                      "   and ast.table.id = :tableID";
        //@formatter:on

        final List<AcctSchemaTable> acctSchemaTables = OBDal.getInstance()
            .createQuery(AcctSchemaTable.class, hqlWhere)
            .setNamedParameter("acctSchemaID", as.m_C_AcctSchema_ID)
            .setNamedParameter("tableID", AD_Table_ID)
            .list();
        if (acctSchemaTables != null && !acctSchemaTables.isEmpty()
            && acctSchemaTables.get(0).getCreatefactTemplate() != null) {
          strClassname = acctSchemaTables.get(0).getCreatefactTemplate().getClassname();
        }
      }
      if (!strClassname.equals("")) {
        try {
          final DocDoubtfulDebtTemplate newTemplate = (DocDoubtfulDebtTemplate) Class
              .forName(strClassname)
              .getDeclaredConstructor()
              .newInstance();
          return newTemplate.createFact(this, as, conn, con, vars);
        } catch (Exception e) {
          log4j.error("Error while creating new instance for DocUnbilledRevenueTemplate - ", e);
        }
      }
      final DoubtfulDebt dd = OBDal.getInstance().get(DoubtfulDebt.class, Record_ID);
      final BigDecimal bpAmountConverted = convertAmount(new BigDecimal(Amounts[AMTTYPE_Gross]),
          !dd.getFINPaymentSchedule().getInvoice().isSalesTransaction(), DateAcct, TABLEID_Invoice,
          dd.getFINPaymentSchedule().getInvoice().getId(), C_Currency_ID, as.m_C_Currency_ID, null,
          as, fact, factAcctGroupID, nextSeqNo(SeqNo), conn, false);
      // Doubtful debt recognition
      fact.createLine(null, getAccountBPartner(C_BPartner_ID, as, true, false, true, conn),
          this.C_Currency_ID, bpAmountConverted.toString(), "", factAcctGroupID, nextSeqNo(SeqNo),
          DocumentType, conn);
      fact.createLine(null, getAccountBPartner(C_BPartner_ID, as, true, false, false, conn),
          this.C_Currency_ID, "", bpAmountConverted.toString(), factAcctGroupID, nextSeqNo(SeqNo),
          DocumentType, conn);
      // Provision
      factAcctGroupID = SequenceIdData.getUUID();

      // Assign expense to the dimensions of the invoice lines
      BigDecimal assignedAmount = BigDecimal.ZERO;
      final DocDoubtfulDebtData[] data = DocDoubtfulDebtData.select(conn,
          dd.getFINPaymentSchedule().getInvoice().getId());
      final Currency currency = OBDal.getInstance().get(Currency.class, C_Currency_ID);
      for (int i = 0; i < data.length; i++) {
        BigDecimal lineAmount = bpAmountConverted.multiply(new BigDecimal(data[i].percentage))
            .setScale(currency.getStandardPrecision().intValue(), RoundingMode.HALF_UP);
        if (i == data.length - 1) {
          lineAmount = bpAmountConverted.subtract(assignedAmount);
        }
        final DocLine line = new DocLine(DocumentType, Record_ID, "");
        line.m_A_Asset_ID = data[i].aAssetId;
        line.m_M_Product_ID = data[i].mProductId;
        line.m_C_Project_ID = data[i].cProjectId;
        line.m_C_BPartner_ID = data[i].cBpartnerId;
        line.m_C_Costcenter_ID = data[i].cCostcenterId;
        line.m_C_Campaign_ID = data[i].cCampaignId;
        line.m_C_Activity_ID = data[i].cActivityId;
        line.m_C_Glitem_ID = data[i].mCGlitemId;
        line.m_User1_ID = data[i].user1id;
        line.m_User2_ID = data[i].user2id;
        line.m_AD_Org_ID = data[i].adOrgId;
        fact.createLine(line, getAccountBPartnerBadDebt(C_BPartner_ID, true, as, conn),
            this.C_Currency_ID, lineAmount.toString(), "", factAcctGroupID, nextSeqNo(SeqNo),
            DocumentType, conn);
        assignedAmount = assignedAmount.add(lineAmount);
      }
      fact.createLine(null, getAccountBPartnerAllowanceForDoubtfulDebt(C_BPartner_ID, as, conn),
          this.C_Currency_ID, "", bpAmountConverted.toString(), factAcctGroupID, nextSeqNo(SeqNo),
          DocumentType, conn);
    } finally {
      OBContext.restorePreviousMode();
    }

    SeqNo = "0";

    return fact;
  }

  @Override
  public boolean getDocumentConfirmation(final ConnectionProvider conn, final String strRecordId) {
    return true;
  }

  public String nextSeqNo(final String oldSeqNo) {
    final BigDecimal seqNo = new BigDecimal(oldSeqNo);
    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
    return SeqNo;
  }
}
