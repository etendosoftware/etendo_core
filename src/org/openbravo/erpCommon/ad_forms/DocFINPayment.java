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
 * All portions are Copyright (C) 2010-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_forms;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.CashVATUtil;
import org.openbravo.erpCommon.utility.FieldProviderFactory;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.AcctSchemaTableDocType;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaTable;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FIN_Payment_Credit;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;

public class DocFINPayment extends AcctServer {
  private static final Logger docFINPaymentLog4j = LogManager.getLogger();

  String SeqNo = "0";
  String generatedAmount = "";
  String usedAmount = "";

  public DocFINPayment() {
  }

  public DocFINPayment(String AD_Client_ID, String AD_Org_ID,
      ConnectionProvider connectionProvider) {
    super(AD_Client_ID, AD_Org_ID, connectionProvider);
  }

  @Override
  public boolean loadDocumentDetails(FieldProvider[] data, ConnectionProvider conn) {
    DateDoc = data[0].getField("PaymentDate");
    m_Record_Id2 = data[0].getField("recordId2");
    Amounts[AMTTYPE_Gross] = data[0].getField("Amount");
    generatedAmount = data[0].getField("GeneratedCredit");
    usedAmount = data[0].getField("UsedCredit");
    loadDocumentType();
    p_lines = loadLines();
    return true;
  }

  public FieldProviderFactory[] loadLinesFieldProvider(String Id) {
    FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, Id);
    List<String> paymentDetails = FIN_Utility.getOrderedPaymentDetailList(Id);
    if (paymentDetails == null) {
      return null;
    }

    final int noOfPaymentDetails = paymentDetails.size();
    final BigDecimal usedCredit = payment.getUsedCredit();
    final BigDecimal creditAmtToAdjust = usedCredit.divide(new BigDecimal(noOfPaymentDetails),
        payment.getCurrency().getPricePrecision().intValue(), RoundingMode.HALF_UP);
    BigDecimal totalCreditAdjusted = BigDecimal.ZERO;
    final FieldProviderFactory[] data = new FieldProviderFactory[noOfPaymentDetails];
    String psId = null;
    String pdId = null;
    BigDecimal totalAmount = BigDecimal.ZERO;
    OBContext.setAdminMode();
    try {
      for (int i = 0; i < data.length; i++) {
        FIN_PaymentDetail paymentDetail = OBDal.getInstance()
            .get(FIN_PaymentDetail.class, paymentDetails.get(i));

        // Details refunded used credit are excluded as the entry will be created using the credit
        // used
        if (paymentDetail.isRefund() && paymentDetail.isPrepayment()) {
          continue;
        }

        // If the Payment Detail has already been processed, skip it
        if (StringUtils.equals(paymentDetail.getId(), pdId)) {
          continue;
        }
        pdId = paymentDetail.getId();

        data[i] = new FieldProviderFactory(null);
        FIN_PaymentSchedule psi = paymentDetail.getFINPaymentScheduleDetailList()
            .get(0)
            .getInvoicePaymentSchedule();
        FIN_PaymentSchedule pso = paymentDetail.getFINPaymentScheduleDetailList()
            .get(0)
            .getOrderPaymentSchedule();
        // Related to Issue Issue 19567. Some Payment Detail's amount and writeoff amount are merged
        // into one.
        // https://issues.openbravo.com/view.php?id=19567
        String paymentDetailNextId = null;
        String paymentDetailPreviousId = null;
        if (i < paymentDetails.size() - 1) {
          paymentDetailNextId = paymentDetails.get(i + 1);
        }
        if (i > 0) {
          paymentDetailPreviousId = paymentDetails.get(i - 1);
        }
        HashMap<String, BigDecimal> amountAndWriteOff = getPaymentDetailWriteOffAndAmount(
            paymentDetail, paymentDetailNextId, paymentDetailPreviousId,
            psId != null ? OBDal.getInstance().get(FIN_PaymentSchedule.class, psId) : null, psi,
            pso, data[i]);
        BigDecimal amount = amountAndWriteOff.get("amount");
        BigDecimal writeOff = amountAndWriteOff.get("writeoff");
        if (amount == null) {
          data[i] = null;
          psId = psi != null ? psi.getId() : null;
          continue;
        } else {
          if (amountAndWriteOff.get("merged").compareTo(BigDecimal.ONE) == 0
              && StringUtils.isNotEmpty(data[i].getField("MergedPaymentDetailId"))) {
            // keeps only the current line while merging the amounts
            data[i - 1] = null;
            totalAmount = totalAmount.add(amount);
          } else {
            totalAmount = amount;
          }
          FieldProviderFactory.setField(data[i], "Amount", totalAmount.toString());
          BigDecimal amountExcludingCredit = totalAmount.subtract(creditAmtToAdjust);
          totalCreditAdjusted = totalCreditAdjusted.add(creditAmtToAdjust);
          if (i == noOfPaymentDetails - 1) { // Last Payment Detail
            amountExcludingCredit = amountExcludingCredit
                .subtract(usedCredit.subtract(totalCreditAdjusted));
          }
          FieldProviderFactory.setField(data[i], "AmountExcludingCredit",
              amountExcludingCredit.toString());
        }
        psId = psi != null ? psi.getId() : null;

        FieldProviderFactory.setField(data[i], "AD_Client_ID", paymentDetail.getClient().getId());
        FieldProviderFactory.setField(data[i], "AD_Org_ID",
            paymentDetail.getOrganization().getId());
        FieldProviderFactory.setField(data[i], "FIN_Payment_Detail_ID", paymentDetail.getId());
        // Calculate Business Partner from the PSD in case of GL Item or from payment header or from
        // details if header is null
        BusinessPartner bPartner = paymentDetail.getFINPaymentScheduleDetailList()
            .get(0)
            .getBusinessPartner() != null
                ? paymentDetail.getFINPaymentScheduleDetailList().get(0).getBusinessPartner()
                : payment.getBusinessPartner() != null ? payment.getBusinessPartner()
                    : paymentDetail.getFINPaymentScheduleDetailList()
                        .get(0)
                        .getInvoicePaymentSchedule() != null
                            ? paymentDetail.getFINPaymentScheduleDetailList()
                                .get(0)
                                .getInvoicePaymentSchedule()
                                .getInvoice()
                                .getBusinessPartner()
                            : paymentDetail.getFINPaymentScheduleDetailList()
                                .get(0)
                                .getOrderPaymentSchedule() != null
                                    ? paymentDetail.getFINPaymentScheduleDetailList()
                                        .get(0)
                                        .getOrderPaymentSchedule()
                                        .getOrder()
                                        .getBusinessPartner()
                                    : null;
        FieldProviderFactory.setField(data[i], "cBpartnerId",
            bPartner != null ? bPartner.getId() : "");
        FieldProviderFactory.setField(data[i], "DoubtFulDebtAmount",
            paymentDetail.getFINPaymentScheduleDetailList()
                .get(0)
                .getDoubtfulDebtAmount()
                .toString());
        FieldProviderFactory.setField(data[i], "WriteOffAmt", writeOff.toString());
        FieldProviderFactory.setField(data[i], "C_GLItem_ID",
            paymentDetail.getGLItem() != null ? paymentDetail.getGLItem().getId() : "");
        FieldProviderFactory.setField(data[i], "Refund", paymentDetail.isRefund() ? "Y" : "N");
        // Check if payment against invoice is in a previous date than invoice accounting date
        boolean isPaymentDatePriorToInvoiceDate = isPaymentDatePriorToInvoiceDate(paymentDetail);
        FieldProviderFactory.setField(data[i], "isprepayment",
            paymentDetail.isPrepayment() ? "Y" : (isPaymentDatePriorToInvoiceDate ? "Y" : "N"));
        FieldProviderFactory.setField(data[i], "isPaymentDatePriorToInvoiceDate",
            isPaymentDatePriorToInvoiceDate && !paymentDetail.isPrepayment() ? "Y" : "N");
        FieldProviderFactory.setField(data[i], "cInvoiceId",
            psi != null && psi.getInvoice() != null ? psi.getInvoice().getId() : null);
        FieldProviderFactory.setField(data[i], "cProjectId",
            paymentDetail.getFINPaymentScheduleDetailList()
                .get(0)
                .getInvoicePaymentSchedule() != null
                && paymentDetail.getFINPaymentScheduleDetailList()
                    .get(0)
                    .getInvoicePaymentSchedule()
                    .getInvoice()
                    .getProject() != null
                        ? paymentDetail.getFINPaymentScheduleDetailList()
                            .get(0)
                            .getInvoicePaymentSchedule()
                            .getInvoice()
                            .getProject()
                            .getId()
                        : (paymentDetail.getFINPaymentScheduleDetailList()
                            .get(0)
                            .getOrderPaymentSchedule() != null
                            && paymentDetail.getFINPaymentScheduleDetailList()
                                .get(0)
                                .getOrderPaymentSchedule()
                                .getOrder()
                                .getProject() != null
                                    ? paymentDetail.getFINPaymentScheduleDetailList()
                                        .get(0)
                                        .getOrderPaymentSchedule()
                                        .getOrder()
                                        .getProject()
                                        .getId()
                                    : (paymentDetail.getFINPaymentScheduleDetailList()
                                        .get(0)
                                        .getProject() != null
                                            ? paymentDetail.getFINPaymentScheduleDetailList()
                                                .get(0)
                                                .getProject()
                                                .getId()
                                            : "")));
        FieldProviderFactory.setField(data[i], "cCampaignId",
            paymentDetail.getFINPaymentScheduleDetailList()
                .get(0)
                .getInvoicePaymentSchedule() != null
                && paymentDetail.getFINPaymentScheduleDetailList()
                    .get(0)
                    .getInvoicePaymentSchedule()
                    .getInvoice()
                    .getSalesCampaign() != null
                        ? paymentDetail.getFINPaymentScheduleDetailList()
                            .get(0)
                            .getInvoicePaymentSchedule()
                            .getInvoice()
                            .getSalesCampaign()
                            .getId()
                        : (paymentDetail.getFINPaymentScheduleDetailList()
                            .get(0)
                            .getOrderPaymentSchedule() != null
                            && paymentDetail.getFINPaymentScheduleDetailList()
                                .get(0)
                                .getOrderPaymentSchedule()
                                .getOrder()
                                .getSalesCampaign() != null
                                    ? paymentDetail.getFINPaymentScheduleDetailList()
                                        .get(0)
                                        .getOrderPaymentSchedule()
                                        .getOrder()
                                        .getSalesCampaign()
                                        .getId()
                                    : (paymentDetail.getFINPaymentScheduleDetailList()
                                        .get(0)
                                        .getSalesCampaign() != null
                                            ? paymentDetail.getFINPaymentScheduleDetailList()
                                                .get(0)
                                                .getSalesCampaign()
                                                .getId()
                                            : "")));
        FieldProviderFactory.setField(data[i], "cActivityId",
            paymentDetail.getFINPaymentScheduleDetailList()
                .get(0)
                .getInvoicePaymentSchedule() != null
                && paymentDetail.getFINPaymentScheduleDetailList()
                    .get(0)
                    .getInvoicePaymentSchedule()
                    .getInvoice()
                    .getActivity() != null
                        ? paymentDetail.getFINPaymentScheduleDetailList()
                            .get(0)
                            .getInvoicePaymentSchedule()
                            .getInvoice()
                            .getActivity()
                            .getId()
                        : (paymentDetail.getFINPaymentScheduleDetailList()
                            .get(0)
                            .getOrderPaymentSchedule() != null
                            && paymentDetail.getFINPaymentScheduleDetailList()
                                .get(0)
                                .getOrderPaymentSchedule()
                                .getOrder()
                                .getActivity() != null
                                    ? paymentDetail.getFINPaymentScheduleDetailList()
                                        .get(0)
                                        .getOrderPaymentSchedule()
                                        .getOrder()
                                        .getActivity()
                                        .getId()
                                    : (paymentDetail.getFINPaymentScheduleDetailList()
                                        .get(0)
                                        .getActivity() != null
                                            ? paymentDetail.getFINPaymentScheduleDetailList()
                                                .get(0)
                                                .getActivity()
                                                .getId()
                                            : "")));
        FieldProviderFactory.setField(data[i], "mProductId",
            paymentDetail.getFINPaymentScheduleDetailList().get(0).getProduct() != null
                ? paymentDetail.getFINPaymentScheduleDetailList().get(0).getProduct().getId()
                : "");
        FieldProviderFactory.setField(data[i], "cSalesregionId",
            paymentDetail.getFINPaymentScheduleDetailList().get(0).getSalesRegion() != null
                ? paymentDetail.getFINPaymentScheduleDetailList().get(0).getSalesRegion().getId()
                : "");
        FieldProviderFactory.setField(data[i], "cCostcenterId",
            paymentDetail.getFINPaymentScheduleDetailList()
                .get(0)
                .getInvoicePaymentSchedule() != null
                && paymentDetail.getFINPaymentScheduleDetailList()
                    .get(0)
                    .getInvoicePaymentSchedule()
                    .getInvoice()
                    .getCostcenter() != null
                        ? paymentDetail.getFINPaymentScheduleDetailList()
                            .get(0)
                            .getInvoicePaymentSchedule()
                            .getInvoice()
                            .getCostcenter()
                            .getId()
                        : (paymentDetail.getFINPaymentScheduleDetailList()
                            .get(0)
                            .getOrderPaymentSchedule() != null
                            && paymentDetail.getFINPaymentScheduleDetailList()
                                .get(0)
                                .getOrderPaymentSchedule()
                                .getOrder()
                                .getCostcenter() != null
                                    ? paymentDetail.getFINPaymentScheduleDetailList()
                                        .get(0)
                                        .getOrderPaymentSchedule()
                                        .getOrder()
                                        .getCostcenter()
                                        .getId()
                                    : (paymentDetail.getFINPaymentScheduleDetailList()
                                        .get(0)
                                        .getCostCenter() != null
                                            ? paymentDetail.getFINPaymentScheduleDetailList()
                                                .get(0)
                                                .getCostCenter()
                                                .getId()
                                            : "")));

        FieldProviderFactory.setField(data[i], "user1Id",
            paymentDetail.getFINPaymentScheduleDetailList()
                .get(0)
                .getInvoicePaymentSchedule() != null
                && paymentDetail.getFINPaymentScheduleDetailList()
                    .get(0)
                    .getInvoicePaymentSchedule()
                    .getInvoice()
                    .getStDimension() != null
                        ? paymentDetail.getFINPaymentScheduleDetailList()
                            .get(0)
                            .getInvoicePaymentSchedule()
                            .getInvoice()
                            .getStDimension()
                            .getId()
                        : (paymentDetail.getFINPaymentScheduleDetailList()
                            .get(0)
                            .getOrderPaymentSchedule() != null
                            && paymentDetail.getFINPaymentScheduleDetailList()
                                .get(0)
                                .getOrderPaymentSchedule()
                                .getOrder()
                                .getStDimension() != null
                                    ? paymentDetail.getFINPaymentScheduleDetailList()
                                        .get(0)
                                        .getOrderPaymentSchedule()
                                        .getOrder()
                                        .getStDimension()
                                        .getId()
                                    : (paymentDetail.getFINPaymentScheduleDetailList()
                                        .get(0)
                                        .getStDimension() != null
                                            ? paymentDetail.getFINPaymentScheduleDetailList()
                                                .get(0)
                                                .getStDimension()
                                                .getId()
                                            : "")));
        FieldProviderFactory.setField(data[i], "user2Id",
            paymentDetail.getFINPaymentScheduleDetailList()
                .get(0)
                .getInvoicePaymentSchedule() != null
                && paymentDetail.getFINPaymentScheduleDetailList()
                    .get(0)
                    .getInvoicePaymentSchedule()
                    .getInvoice()
                    .getNdDimension() != null
                        ? paymentDetail.getFINPaymentScheduleDetailList()
                            .get(0)
                            .getInvoicePaymentSchedule()
                            .getInvoice()
                            .getNdDimension()
                            .getId()
                        : (paymentDetail.getFINPaymentScheduleDetailList()
                            .get(0)
                            .getOrderPaymentSchedule() != null
                            && paymentDetail.getFINPaymentScheduleDetailList()
                                .get(0)
                                .getOrderPaymentSchedule()
                                .getOrder()
                                .getNdDimension() != null
                                    ? paymentDetail.getFINPaymentScheduleDetailList()
                                        .get(0)
                                        .getOrderPaymentSchedule()
                                        .getOrder()
                                        .getNdDimension()
                                        .getId()
                                    : (paymentDetail.getFINPaymentScheduleDetailList()
                                        .get(0)
                                        .getNdDimension() != null
                                            ? paymentDetail.getFINPaymentScheduleDetailList()
                                                .get(0)
                                                .getNdDimension()
                                                .getId()
                                            : "")));
        FieldProviderFactory.setField(data[i], "recordId2",
            paymentDetail.isPrepayment() ? (pso != null ? pso.getId() : "")
                : (psi != null ? psi.getId() : ""));

        if ((i + 1) % 100 == 0) {
          OBDal.getInstance().getSession().clear();
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return data;
  }

  private DocLine[] loadLines() {
    ArrayList<Object> list = new ArrayList<Object>();
    FieldProviderFactory[] data = loadLinesFieldProvider(Record_ID);
    if (data == null || data.length == 0) {
      return null;
    }
    for (int i = 0; i < data.length; i++) {
      if (data[i] == null) {
        continue;
      }
      String Line_ID = data[i].getField("FIN_Payment_Detail_ID");
      OBContext.setAdminMode();
      try {
        DocLine_FINPayment docLine = new DocLine_FINPayment(DocumentType, Record_ID, Line_ID);
        docLine.loadAttributes(data[i], this);
        docLine.setAmount(data[i].getField("Amount"));
        docLine.setIsPrepayment(data[i].getField("isprepayment"));
        docLine.setWriteOffAmt(data[i].getField("WriteOffAmt"));
        docLine.setDoubtFulDebtAmount(new BigDecimal(data[i].getField("DoubtFulDebtAmount")));
        docLine.setC_GLItem_ID(data[i].getField("C_GLItem_ID"));
        docLine.setPrepaymentAgainstInvoice(
            "Y".equals(data[i].getField("isPaymentDatePriorToInvoiceDate")) ? true : false);
        docLine.setInvoiceId(data[i].getField("cInvoiceId"));
        docLine.m_Record_Id2 = data[i].getField("recordId2");
        docLine.setInvoiceTaxCashVAT_V(Line_ID);
        docLine.setInvoiceTaxCashVAT_V(data[i].getField("MergedPaymentDetailId"));
        list.add(docLine);
      } finally {
        OBContext.restorePreviousMode();
      }
    }
    // Return Array
    DocLine_FINPayment[] dl = new DocLine_FINPayment[list.size()];
    list.toArray(dl);
    return dl;
  } // loadLines

  @Override
  public Fact createFact(AcctSchema as, ConnectionProvider conn, Connection con,
      VariablesSecureApp vars) throws ServletException {
    // Select specific definition
    String strClassname = "";
    Fact fact = new Fact(this, as, Fact.POST_Actual);
    String Fact_Acct_Group_ID = SequenceIdData.getUUID();
    String Fact_Acct_Group_ID2 = SequenceIdData.getUUID();
    String Fact_Acct_Group_ID3 = SequenceIdData.getUUID();

    OBContext.setAdminMode();
    try {
      //@formatter:off
      final String whereClause = " as astdt "
          + " where astdt.acctschemaTable.accountingSchema.id = :accountSchemaId"
          + "   and astdt.acctschemaTable.table.id = :tableId"
          + "   and astdt.documentCategory = :documentType";

      //@formatter:on
      final OBQuery<AcctSchemaTableDocType> obqParameters = OBDal.getInstance()
          .createQuery(AcctSchemaTableDocType.class, whereClause);
      obqParameters.setNamedParameter("accountSchemaId", as.m_C_AcctSchema_ID);
      obqParameters.setNamedParameter("tableId", AD_Table_ID);
      obqParameters.setNamedParameter("documentType", DocumentType);
      obqParameters.setFilterOnReadableClients(false);
      obqParameters.setFilterOnReadableOrganization(false);
      final List<AcctSchemaTableDocType> acctSchemaTableDocTypes = obqParameters.list();

      if (acctSchemaTableDocTypes != null && acctSchemaTableDocTypes.size() > 0
          && acctSchemaTableDocTypes.get(0).getCreatefactTemplate() != null) {
        strClassname = acctSchemaTableDocTypes.get(0).getCreatefactTemplate().getClassname();
      }

      if (strClassname.equals("")) {
        //@formatter:off
        final String whereClause2 = " as ast "
            + " where ast.accountingSchema.id = :accountSchemaId"
            + "   and ast.table.id = :tableId";

        //@formatter:on
        final OBQuery<AcctSchemaTable> obqParameters2 = OBDal.getInstance()
            .createQuery(AcctSchemaTable.class, whereClause2);
        obqParameters2.setNamedParameter("accountSchemaId", as.m_C_AcctSchema_ID);
        obqParameters2.setNamedParameter("tableId", AD_Table_ID);
        obqParameters2.setFilterOnReadableClients(false);
        obqParameters2.setFilterOnReadableOrganization(false);
        final List<AcctSchemaTable> acctSchemaTables = obqParameters2.list();
        if (acctSchemaTables != null && acctSchemaTables.size() > 0
            && acctSchemaTables.get(0).getCreatefactTemplate() != null) {
          strClassname = acctSchemaTables.get(0).getCreatefactTemplate().getClassname();
        }
      }
      if (!strClassname.equals("")) {
        try {
          DocFINPaymentTemplate newTemplate = (DocFINPaymentTemplate) Class.forName(strClassname)
              .getDeclaredConstructor()
              .newInstance();
          return newTemplate.createFact(this, as, conn, con, vars);
        } catch (Exception e) {
          docFINPaymentLog4j.error("Error while creating new instance for DocFINPaymentTemplate - ",
              e);
        }
      }

      for (int i = 0; p_lines != null && i < p_lines.length; i++) {
        DocLine_FINPayment line = (DocLine_FINPayment) p_lines[i];

        boolean isReceipt = DocumentType.equals("ARR");
        boolean isPrepayment = line.getIsPrepayment().equals("Y");
        String bpartnerId = (line.m_C_BPartner_ID == null || line.m_C_BPartner_ID.equals(""))
            ? this.C_BPartner_ID
            : line.m_C_BPartner_ID;

        String bpAmount = line.getAmount();
        if (line.WriteOffAmt != null && !line.WriteOffAmt.equals("")
            && new BigDecimal(line.WriteOffAmt).compareTo(ZERO) != 0) {
          Account account = isReceipt ? getAccount(AcctServer.ACCTTYPE_WriteOff, as, conn)
              : getAccount(AcctServer.ACCTTYPE_WriteOff_Revenue, as, conn);
          if (account == null) {
            account = isReceipt ? getAccount(AcctServer.ACCTTYPE_WriteOffDefault, as, conn)
                : getAccount(AcctServer.ACCTTYPE_WriteOffDefault_Revenue, as, conn);
          }
          fact.createLine(line, account, C_Currency_ID, (isReceipt ? line.WriteOffAmt : ""),
              (isReceipt ? "" : line.WriteOffAmt), Fact_Acct_Group_ID, nextSeqNo(SeqNo),
              DocumentType, conn);
          bpAmount = new BigDecimal(bpAmount).add(new BigDecimal(line.WriteOffAmt)).toString();
        }
        if ("".equals(line.getC_GLItem_ID())) {
          String bpAmountConverted = bpAmount;
          Invoice invoice = line.getInvoice();
          String strcCurrencyId = C_Currency_ID;
          if (!isOrderPrepayment(line.getLine_ID()) && invoice != null) {
            // To force opposite posting isReceipt is opposite as well. this is required when
            // looking backwards
            bpAmountConverted = convertAmount(new BigDecimal(bpAmount), !isReceipt, DateAcct,
                TABLEID_Invoice, invoice.getId(), C_Currency_ID, as.m_C_Currency_ID, line, as, fact,
                Fact_Acct_Group_ID, nextSeqNo(SeqNo), conn).toString();
            // Cash VAT
            SeqNo = CashVATUtil.createFactCashVAT(as, conn, fact, Fact_Acct_Group_ID, line, invoice,
                DocumentType, SeqNo);
            if (!isPrepayment) {
              if (line.getDoubtFulDebtAmount().signum() != 0) {
                BigDecimal doubtFulDebtAmount = convertAmount(line.getDoubtFulDebtAmount(),
                    isReceipt, DateAcct, TABLEID_Invoice, invoice.getId(), C_Currency_ID,
                    as.m_C_Currency_ID, line, as, fact, Fact_Acct_Group_ID, nextSeqNo(SeqNo), conn,
                    false);
                fact.createLine(line, getAccountBPartner(bpartnerId, as, true, false, true, conn),
                    strcCurrencyId, "", doubtFulDebtAmount.toString(), Fact_Acct_Group_ID,
                    nextSeqNo(SeqNo), DocumentType, conn);
                bpAmountConverted = new BigDecimal(bpAmountConverted).subtract(doubtFulDebtAmount)
                    .toString();
                fact.createLine(line,
                    getAccountBPartnerAllowanceForDoubtfulDebt(bpartnerId, as, conn),
                    this.C_Currency_ID, doubtFulDebtAmount.toString(), "", Fact_Acct_Group_ID2,
                    nextSeqNo(SeqNo), DocumentType, conn);
                // Assign expense to the dimensions of the invoice lines
                BigDecimal assignedAmount = BigDecimal.ZERO;
                DocDoubtfulDebtData[] data = DocDoubtfulDebtData.select(conn, invoice.getId());
                Currency currency = OBDal.getInstance().get(Currency.class, C_Currency_ID);
                for (int j = 0; j < data.length; j++) {
                  BigDecimal lineAmount = doubtFulDebtAmount
                      .multiply(new BigDecimal(data[j].percentage))
                      .setScale(currency.getStandardPrecision().intValue(), RoundingMode.HALF_UP);
                  if (j == data.length - 1) {
                    lineAmount = doubtFulDebtAmount.subtract(assignedAmount);
                  }
                  DocLine lineDD = new DocLine(DocumentType, Record_ID, "");
                  lineDD.m_A_Asset_ID = data[j].aAssetId;
                  lineDD.m_M_Product_ID = data[j].mProductId;
                  lineDD.m_C_Project_ID = data[j].cProjectId;
                  lineDD.m_C_BPartner_ID = data[j].cBpartnerId;
                  lineDD.m_C_Costcenter_ID = data[j].cCostcenterId;
                  lineDD.m_C_Campaign_ID = data[j].cCampaignId;
                  lineDD.m_C_Activity_ID = data[j].cActivityId;
                  lineDD.m_C_Glitem_ID = data[j].mCGlitemId;
                  lineDD.m_User1_ID = data[j].user1id;
                  lineDD.m_User2_ID = data[j].user2id;
                  lineDD.m_AD_Org_ID = data[j].adOrgId;
                  fact.createLine(lineDD,
                      getAccountBPartnerBadDebt(
                          (lineDD.m_C_BPartner_ID == null || lineDD.m_C_BPartner_ID.equals(""))
                              ? this.C_BPartner_ID
                              : lineDD.m_C_BPartner_ID,
                          false, as, conn),
                      this.C_Currency_ID, "", lineAmount.toString(), Fact_Acct_Group_ID2,
                      nextSeqNo(SeqNo), DocumentType, conn);
                  assignedAmount = assignedAmount.add(lineAmount);
                }
              }
            }
          }
          fact.createLine(line, getAccountBPartner(bpartnerId, as, isReceipt, isPrepayment, conn),
              strcCurrencyId, (isReceipt ? "" : bpAmountConverted),
              (isReceipt ? bpAmountConverted : ""), Fact_Acct_Group_ID, nextSeqNo(SeqNo),
              DocumentType, conn);
          // If payment date is prior to invoice date book invoice as a pre-payment not as a regular
          // Receivable/Payable
          if (line.isPrepaymentAgainstInvoice()) {
            DocLine line2 = new DocLine(DocumentType, Record_ID, line.m_TrxLine_ID);
            line2.copyInfo(line);
            line2.m_DateAcct = OBDateUtils.formatDate(invoice.getAccountingDate());
            // checking if the prepayment account and ReceivablesNo account in the Business Partner
            // is the same.In this case we do not need to create more accounting lines
            if (!getAccountBPartner(bpartnerId, as, isReceipt, true, conn).Account_ID
                .equals(getAccountBPartner(bpartnerId, as, isReceipt, false, conn).Account_ID)) {
              fact.createLine(line2, getAccountBPartner(bpartnerId, as, isReceipt, false, conn),
                  strcCurrencyId, (isReceipt ? "" : bpAmountConverted),
                  (isReceipt ? bpAmountConverted : ""), Fact_Acct_Group_ID3, nextSeqNo(SeqNo),
                  DocumentType, conn);
              fact.createLine(line2, getAccountBPartner(bpartnerId, as, isReceipt, true, conn),
                  strcCurrencyId, (!isReceipt ? "" : bpAmountConverted),
                  (!isReceipt ? bpAmountConverted : ""), Fact_Acct_Group_ID3, nextSeqNo(SeqNo),
                  DocumentType, conn);
            }
          }
        } else {
          fact.createLine(line,
              getAccountGLItem(OBDal.getInstance().get(GLItem.class, line.getC_GLItem_ID()), as,
                  isReceipt, conn),
              C_Currency_ID, (isReceipt ? "" : bpAmount), (isReceipt ? bpAmount : ""),
              Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
        }

        if ((i + 1) % 100 == 0) {
          OBDal.getInstance().getSession().clear();
        }
      }
      FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, Record_ID);
      if (BigDecimal.ZERO.compareTo(new BigDecimal(Amounts[AMTTYPE_Gross])) != 0) {
        fact.createLine(null,
            getAccount(conn, payment.getPaymentMethod(), payment.getAccount(), as,
                payment.isReceipt()),
            C_Currency_ID, (payment.isReceipt() ? Amounts[AMTTYPE_Gross] : ""),
            (payment.isReceipt() ? "" : Amounts[AMTTYPE_Gross]), Fact_Acct_Group_ID, "999999",
            DocumentType, conn);
      }
      // Pre-payment is consumed when Used Credit Amount not equals Zero. When consuming Credit no
      // credit is generated
      if (new BigDecimal(usedAmount).compareTo(ZERO) != 0
          && new BigDecimal(generatedAmount).compareTo(ZERO) == 0) {
        List<FIN_Payment_Credit> creditPayments = payment.getFINPaymentCreditList();
        BigDecimal amtDiff = BigDecimal.ZERO;
        for (FIN_Payment_Credit creditPayment : creditPayments) {
          String creditAmountConverted = convertAmount(creditPayment.getAmount(),
              creditPayment.getCreditPaymentUsed().isReceipt(), DateAcct, TABLEID_Payment,
              creditPayment.getCreditPaymentUsed().getId(),
              creditPayment.getCreditPaymentUsed().getCurrency().getId(), as.m_C_Currency_ID, null,
              as, fact, Fact_Acct_Group_ID, nextSeqNo(SeqNo), conn, false).toString();
          fact.createLine(null,
              getAccountBPartner(C_BPartner_ID, as,
                  creditPayment.getCreditPaymentUsed().isReceipt(), true, conn),
              creditPayment.getCreditPaymentUsed().getCurrency().getId(),
              (creditPayment.getCreditPaymentUsed().isReceipt() ? creditAmountConverted : ""),
              (creditPayment.getCreditPaymentUsed().isReceipt() ? "" : creditAmountConverted),
              Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
          amtDiff = amtDiff.add(creditPayment.getAmount())
              .subtract(new BigDecimal(creditAmountConverted));
        }
        if (!payment.isReceipt() && amtDiff.compareTo(BigDecimal.ZERO) == 1
            || payment.isReceipt() && amtDiff.compareTo(BigDecimal.ZERO) == -1) {
          fact.createLine(null, getAccount(AcctServer.ACCTTYPE_ConvertGainDefaultAmt, as, conn),
              payment.getCurrency().getId(), "", amtDiff.abs().toString(), Fact_Acct_Group_ID,
              nextSeqNo(SeqNo), DocumentType, conn);
        } else {
          fact.createLine(null, getAccount(AcctServer.ACCTTYPE_ConvertChargeDefaultAmt, as, conn),
              payment.getCurrency().getId(), amtDiff.abs().toString(), "", Fact_Acct_Group_ID,
              nextSeqNo(SeqNo), DocumentType, conn);
        }
        if (creditPayments.isEmpty()) {
          fact.createLine(null,
              getAccountBPartner(C_BPartner_ID, as, payment.isReceipt(), true, conn), C_Currency_ID,
              (payment.isReceipt() ? usedAmount : ""), (payment.isReceipt() ? "" : usedAmount),
              Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    SeqNo = "0";
    return fact;
  }

  public boolean isOrderPrepayment(String paymentDetailID) {
    FIN_PaymentDetail pd = OBDal.getInstance().get(FIN_PaymentDetail.class, paymentDetailID);
    if (pd != null) {
      return pd.isPrepayment();
    }
    return false;
  }

  public String nextSeqNo(String oldSeqNo) {
    BigDecimal seqNo = new BigDecimal(oldSeqNo);
    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
    return SeqNo;
  }

  /**
   * Get Source Currency Balance - subtracts line amounts from total - no rounding
   * 
   * @return positive amount, if total is bigger than lines
   */
  @Override
  public BigDecimal getBalance() {
    BigDecimal retValue = ZERO;
    StringBuffer sb = new StringBuffer(" [");
    // Total
    retValue = retValue.add(new BigDecimal(getAmount(AcctServer.AMTTYPE_Gross)));
    if ((new BigDecimal(generatedAmount)).signum() == 0) {
      retValue = retValue.add(new BigDecimal(usedAmount));
    }
    sb.append(getAmount(AcctServer.AMTTYPE_Gross));
    // - Lines
    for (int i = 0; i < p_lines.length; i++) {
      BigDecimal lineBalance = new BigDecimal(((DocLine_FINPayment) p_lines[i]).Amount);
      retValue = retValue.subtract(lineBalance);
      sb.append("-").append(lineBalance);
    }
    sb.append("]");
    //
    docFINPaymentLog4j.debug(" Balance=" + retValue + sb.toString());
    return retValue;
  } // getBalance

  @Override
  public boolean getDocumentConfirmation(ConnectionProvider conn, String strRecordId) {
    // Checks if this step is configured to generate accounting for the selected financial account
    boolean confirmation = false;
    OBContext.setAdminMode();
    try {
      FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, strRecordId);
      // Posting can just happen if payment is in the right status
      if (FIN_Utility.isPaymentConfirmed(payment.getStatus(), null)) {
        OBCriteria<FinAccPaymentMethod> obCriteria = OBDal.getInstance()
            .createCriteria(FinAccPaymentMethod.class);
        obCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, payment.getAccount()));
        obCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD,
            payment.getPaymentMethod()));
        obCriteria.setFilterOnReadableClients(false);
        obCriteria.setFilterOnReadableOrganization(false);
        List<FinAccPaymentMethod> lines = obCriteria.list();
        List<FIN_FinancialAccountAccounting> accounts = payment.getAccount()
            .getFINFinancialAccountAcctList();
        for (FIN_FinancialAccountAccounting account : accounts) {
          if (confirmation) {
            return confirmation;
          }
          if (payment.isReceipt()) {
            if (("INT").equals(lines.get(0).getUponReceiptUse())
                && account.getInTransitPaymentAccountIN() != null) {
              confirmation = true;
            } else if (("DEP").equals(lines.get(0).getUponReceiptUse())
                && account.getDepositAccount() != null) {
              confirmation = true;
            } else if (("CLE").equals(lines.get(0).getUponReceiptUse())
                && account.getClearedPaymentAccount() != null) {
              confirmation = true;
            }
          } else {
            if (("INT").equals(lines.get(0).getUponPaymentUse())
                && account.getFINOutIntransitAcct() != null) {
              confirmation = true;
            } else if (("WIT").equals(lines.get(0).getUponPaymentUse())
                && account.getWithdrawalAccount() != null) {
              confirmation = true;
            } else if (("CLE").equals(lines.get(0).getUponPaymentUse())
                && account.getClearedPaymentAccountOUT() != null) {
              confirmation = true;
            }
          }
          // For payments with Amount ZERO always create an entry as no transaction will be created
          if (payment.getAmount().compareTo(ZERO) == 0) {
            confirmation = true;
          }
        }
      }
    } catch (Exception e) {
      setStatus(STATUS_DocumentDisabled);
      return confirmation;
    } finally {
      OBContext.restorePreviousMode();
    }
    if (!confirmation) {
      setStatus(STATUS_DocumentDisabled);
    }
    return confirmation;
  }

  @Override
  public void loadObjectFieldProvider(ConnectionProvider conn, String strAD_Client_ID, String Id)
      throws ServletException {
    FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, Id);
    FieldProviderFactory[] data = new FieldProviderFactory[1];
    data[0] = new FieldProviderFactory(null);
    FieldProviderFactory.setField(data[0], "AD_Client_ID", payment.getClient().getId());
    FieldProviderFactory.setField(data[0], "AD_Org_ID", payment.getOrganization().getId());
    FieldProviderFactory.setField(data[0], "C_BPartner_ID",
        payment.getBusinessPartner() != null ? payment.getBusinessPartner().getId() : "");
    FieldProviderFactory.setField(data[0], "DocumentNo", payment.getDocumentNo());
    String dateFormat = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("dateFormat.java");
    SimpleDateFormat outputFormat = new SimpleDateFormat(dateFormat);
    FieldProviderFactory.setField(data[0], "PaymentDate",
        outputFormat.format(payment.getPaymentDate()));
    FieldProviderFactory.setField(data[0], "C_DocType_ID", payment.getDocumentType().getId());
    FieldProviderFactory.setField(data[0], "C_Currency_ID", payment.getCurrency().getId());
    FieldProviderFactory.setField(data[0], "FIN_Financial_Account_ID",
        payment.getAccount().getId());
    FieldProviderFactory.setField(data[0], "Amount", payment.getAmount().toString());
    FieldProviderFactory.setField(data[0], "GeneratedCredit",
        payment.getGeneratedCredit().toString());
    FieldProviderFactory.setField(data[0], "UsedCredit", payment.getUsedCredit().toString());
    FieldProviderFactory.setField(data[0], "WriteOffAmt", payment.getWriteoffAmount().toString());
    FieldProviderFactory.setField(data[0], "Description", payment.getDescription());
    FieldProviderFactory.setField(data[0], "Posted", payment.getPosted());
    FieldProviderFactory.setField(data[0], "Processed", payment.isProcessed() ? "Y" : "N");
    FieldProviderFactory.setField(data[0], "Processing", payment.isProcessNow() ? "Y" : "N");
    FieldProviderFactory.setField(data[0], "C_Project_ID",
        payment.getProject() != null ? payment.getProject().getId() : "");
    FieldProviderFactory.setField(data[0], "C_Campaign_ID",
        payment.getSalesCampaign() != null ? payment.getSalesCampaign().getId() : "");
    FieldProviderFactory.setField(data[0], "C_Activity_ID",
        payment.getActivity() != null ? payment.getActivity().getId() : "");
    // User1_ID and User2_ID
    DocFINPaymentData[] paymentInfo = DocFINPaymentData.select(conn, payment.getId());
    if (paymentInfo.length > 0) {
      FieldProviderFactory.setField(data[0], "User1_ID", paymentInfo[0].user1Id);
      FieldProviderFactory.setField(data[0], "User2_ID", paymentInfo[0].user2Id);
      FieldProviderFactory.setField(data[0], "C_Costcenter_ID", paymentInfo[0].cCostcenterId);
    }
    // Used to match balances
    FieldProviderFactory.setField(data[0], "recordId2", payment.getId());
    setObjectFieldProvider(data);
  }

  /*
   * Retrieves Account for receipt / Payment for the given payment method + Financial Account
   */
  public Account getAccount(ConnectionProvider conn, FIN_PaymentMethod paymentMethod,
      FIN_FinancialAccount finAccount, AcctSchema as, boolean bIsReceipt) throws ServletException {
    OBContext.setAdminMode();
    Account account = null;
    try {
      OBCriteria<FIN_FinancialAccountAccounting> accounts = OBDal.getInstance()
          .createCriteria(FIN_FinancialAccountAccounting.class);
      accounts.add(Restrictions.eq(FIN_FinancialAccountAccounting.PROPERTY_ACCOUNT, finAccount));
      accounts.add(Restrictions.eq(FIN_FinancialAccountAccounting.PROPERTY_ACCOUNTINGSCHEMA,
          OBDal.getInstance()
              .get(org.openbravo.model.financialmgmt.accounting.coa.AcctSchema.class,
                  as.m_C_AcctSchema_ID)));
      accounts.add(Restrictions.eq(FIN_FinancialAccountAccounting.PROPERTY_ACTIVE, true));
      accounts.setFilterOnReadableClients(false);
      accounts.setFilterOnReadableOrganization(false);
      List<FIN_FinancialAccountAccounting> accountList = accounts.list();
      if (accountList == null || accountList.size() == 0) {
        return null;
      }
      OBCriteria<FinAccPaymentMethod> accPaymentMethod = OBDal.getInstance()
          .createCriteria(FinAccPaymentMethod.class);
      accPaymentMethod.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, finAccount));
      accPaymentMethod
          .add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, paymentMethod));
      accPaymentMethod.setFilterOnReadableClients(false);
      accPaymentMethod.setFilterOnReadableOrganization(false);
      List<FinAccPaymentMethod> lines = accPaymentMethod.list();
      if (bIsReceipt) {
        account = getAccount(conn, lines.get(0).getUponReceiptUse(), accountList.get(0),
            bIsReceipt);
      } else {
        account = getAccount(conn, lines.get(0).getUponPaymentUse(), accountList.get(0),
            bIsReceipt);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return account;
  }

  @Deprecated
  public String convertAmount(String Amount, boolean isReceipt, String mDateAcct,
      String conversionDate, String C_Currency_ID_From, String C_Currency_ID_To, DocLine line,
      AcctSchema as, Fact fact, String Fact_Acct_Group_ID, ConnectionProvider conn)
      throws ServletException {
    if (Amount == null || Amount.equals("")) {
      return "0";
    }
    if (C_Currency_ID_From.equals(C_Currency_ID_To)) {
      return Amount;
    } else {
      MultiCurrency = true;
    }
    String Amt = getConvertedAmt(Amount, C_Currency_ID_From, C_Currency_ID_To, conversionDate, "",
        AD_Client_ID, AD_Org_ID, conn);
    if (docFINPaymentLog4j.isDebugEnabled()) {
      docFINPaymentLog4j.debug("Amt:" + Amt);
    }

    String AmtTo = getConvertedAmt(Amount, C_Currency_ID_From, C_Currency_ID_To, mDateAcct, "",
        AD_Client_ID, AD_Org_ID, conn);
    if (docFINPaymentLog4j.isDebugEnabled()) {
      docFINPaymentLog4j.debug("AmtTo:" + AmtTo);
    }

    BigDecimal AmtDiff = (new BigDecimal(AmtTo)).subtract(new BigDecimal(Amt));
    if (docFINPaymentLog4j.isDebugEnabled()) {
      docFINPaymentLog4j.debug("AmtDiff:" + AmtDiff);
    }

    if (docFINPaymentLog4j.isDebugEnabled()) {
      docFINPaymentLog4j.debug("curr from:" + C_Currency_ID_From + " Curr to:" + C_Currency_ID_To
          + " convDate:" + conversionDate + " DateAcct:" + mDateAcct);
      docFINPaymentLog4j.debug("Amt:" + Amt + " AmtTo:" + AmtTo + " Diff:" + AmtDiff.toString());
    }

    if ((isReceipt && AmtDiff.compareTo(new BigDecimal("0.00")) == 1)
        || (!isReceipt && AmtDiff.compareTo(new BigDecimal("0.00")) == -1)) {
      fact.createLine(line, getAccount(AcctServer.ACCTTYPE_ConvertGainDefaultAmt, as, conn),
          C_Currency_ID_To, "", AmtDiff.abs().toString(), Fact_Acct_Group_ID, nextSeqNo(SeqNo),
          DocumentType, conn);
    } else {
      fact.createLine(line, getAccount(AcctServer.ACCTTYPE_ConvertChargeDefaultAmt, as, conn),
          C_Currency_ID_To, AmtDiff.abs().toString(), "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
          DocumentType, conn);
    }

    return Amt;
  }

  public String getSeqNo() {
    return SeqNo;
  }

  public void setSeqNo(String seqNo) {
    SeqNo = seqNo;
  }

  public String getGeneratedAmount() {
    return generatedAmount;
  }

  public void setGeneratedAmount(String generatedAmount) {
    this.generatedAmount = generatedAmount;
  }

  public String getUsedAmount() {
    return usedAmount;
  }

  public void setUsedAmount(String usedAmount) {
    this.usedAmount = usedAmount;
  }

  boolean isPaymentDatePriorToInvoiceDate(FIN_PaymentDetail paymentDetail) {
    List<FIN_PaymentScheduleDetail> schedDetails = paymentDetail.getFINPaymentScheduleDetailList();
    if (schedDetails.size() == 0) {
      return false;
    } else {
      if (schedDetails.get(0).getInvoicePaymentSchedule() != null && schedDetails.get(0)
          .getInvoicePaymentSchedule()
          .getInvoice()
          .getAccountingDate()
          .after(paymentDetail.getFinPayment().getPaymentDate())) {
        return true;
      } else {
        return false;
      }
    }
  }
}
