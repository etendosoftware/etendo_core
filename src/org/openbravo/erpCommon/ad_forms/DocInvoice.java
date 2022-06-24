/*
 ******************************************************************************
 * The contents of this file are subject to the   Compiere License  Version 1.1
 * ("License"); You may not use this file except in compliance with the License
 * You may obtain a copy of the License at http://www.compiere.org/license.html
 * Software distributed under the License is distributed on an  "AS IS"  basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * The Original Code is                  Compiere  ERP & CRM  Business Solution
 * The Initial Developer of the Original Code is Jorg Janke  and ComPiere, Inc.
 * Portions created by Jorg Janke are Copyright (C) 1999-2001 Jorg Janke, parts
 * created by ComPiere are Copyright (C) ComPiere, Inc.;   All Rights Reserved.
 * Contributor(s): Openbravo SLU
 * Contributions are Copyright (C) 2001-2018 Openbravo S.L.U.
 ******************************************************************************
 */
package org.openbravo.erpCommon.ad_forms;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.AccDefUtility;
import org.openbravo.erpCommon.utility.CashVATUtil;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.invoice.InvoiceTax;
import org.openbravo.model.financialmgmt.calendar.Period;

public class DocInvoice extends AcctServer {
  private static final long serialVersionUID = 1L;
  private static final Logger log4jDocInvoice = LogManager.getLogger();

  DocTax[] m_taxes = null;
  DocLine_FinPaymentSchedule[] m_payments = null;

  DocLine[] p_lines_taxes = null;

  boolean isCashVAT = false;
  boolean isReversedInvoice = false;
  String prepaymentamt;

  String SeqNo = "0";

  /**
   * Constructor
   * 
   * @param AD_Client_ID
   *          AD_Client_ID
   */
  public DocInvoice(String AD_Client_ID, String AD_Org_ID, ConnectionProvider connectionProvider) {
    super(AD_Client_ID, AD_Org_ID, connectionProvider);
  }

  @Override
  public void loadObjectFieldProvider(ConnectionProvider conn, String stradClientId, String Id)
      throws ServletException {
    setObjectFieldProvider(DocInvoiceData.selectRegistro(conn, stradClientId, Id));
  }

  @Override
  public boolean loadDocumentDetails(FieldProvider[] data, ConnectionProvider conn) {
    DateDoc = data[0].getField("DateInvoiced");
    TaxIncluded = data[0].getField("IsTaxIncluded");
    C_BPartner_Location_ID = data[0].getField("C_BPartner_Location_ID");
    // Amounts
    Amounts[AMTTYPE_Gross] = data[0].getField("GrandTotal");
    if (Amounts[AMTTYPE_Gross] == null) {
      Amounts[AMTTYPE_Gross] = "0";
    }
    Amounts[AMTTYPE_Net] = data[0].getField("TotalLines");
    if (Amounts[AMTTYPE_Net] == null) {
      Amounts[AMTTYPE_Net] = "0";
    }
    Amounts[AMTTYPE_Charge] = data[0].getField("ChargeAmt");
    if (Amounts[AMTTYPE_Charge] == null) {
      Amounts[AMTTYPE_Charge] = "0";
    }

    loadDocumentType(); // lines require doc type
    // Contained Objects
    p_lines = loadLines();
    m_taxes = loadTaxes();
    m_payments = loadPayments();
    m_debt_payments = loadDebtPayments();
    isCashVAT = StringUtils.equals("Y", data[0].getField("iscashvat"));
    isReversedInvoice = isReversedInvoice();
    prepaymentamt = data[0].getField("prepaymentamt");

    return true;

  }

  private boolean isReversedInvoice() {
    try {
      DocInvoiceData[] revInv = DocInvoiceData.getIsReversedInvoice(connectionProvider, Record_ID);
      return (revInv != null && revInv.length != 0);
    } catch (ServletException e) {
      log4jDocInvoice.warn(e);
    }
    return false;
  }

  private DocLine[] loadLines() {
    ArrayList<Object> list = new ArrayList<Object>();
    DocLineInvoiceData[] data = null;
    try {
      log4jDocInvoice.debug("############### groupLines = " + groupLines);
      if (groupLines.equals("Y")) {
        data = DocLineInvoiceData.selectTotal(connectionProvider, Record_ID);
      } else {
        data = DocLineInvoiceData.select(connectionProvider, Record_ID);
      }
    } catch (ServletException e) {
      log4jDocInvoice.warn(e);
    }
    if (data == null || data.length == 0) {
      return null;
    }
    for (int i = 0; i < data.length; i++) {
      String Line_ID = data[i].cInvoicelineId;
      DocLine_Invoice docLine = new DocLine_Invoice(DocumentType, Record_ID, Line_ID);
      docLine.loadAttributes(data[i], this);
      String strQty = data[i].qtyinvoiced;
      docLine.setQty(strQty);
      String LineNetAmt = data[i].linenetamt;
      String PriceList = data[i].pricelist;
      docLine.setAmount(LineNetAmt, PriceList, strQty);
      // Accruals & Deferrals for revenue products
      docLine.setIsDeferred("Y".equals(data[i].isdeferred));
      docLine.setDefPlanType(data[i].defplantype);
      docLine.setPeriodNumber(
          !"".equals(data[i].periodnumber) ? Integer.parseInt(data[i].periodnumber) : 0);
      docLine.setStartingPeriodId(data[i].cPeriodId);

      list.add(docLine);
    }
    // Return Array
    DocLine[] dl = new DocLine[list.size()];
    list.toArray(dl);
    return dl;
  } // loadLines

  private DocTax[] loadTaxes() {
    ArrayList<Object> list = new ArrayList<Object>();
    DocInvoiceData[] data = null;
    try {
      data = DocInvoiceData.select(connectionProvider, Record_ID);
    } catch (ServletException e) {
      log4jDocInvoice.warn(e);
    }
    log4jDocInvoice.debug("############### Taxes.length = " + data.length);
    for (int i = 0; i < data.length; i++) {
      String C_Tax_ID = data[i].cTaxId;
      String name = data[i].name;
      String rate = data[i].rate;

      String taxBaseAmt = data[i].taxbaseamt;
      String amount = data[i].taxamt;
      boolean isTaxDeductable = false;
      boolean isTaxUndeductable = ("Y".equals(data[i].ratetaxundeductable))
          || ("Y".equals(data[i].orgtaxundeductable));
      if ("Y".equals(data[i].orgtaxundeductable)) {
        /*
         * If any tax line level has tax deductable flag then override isTaxUndeductable flag for
         * intracommunity non tax deductible organization
         */
        if ("Y".equals(data[i].istaxdeductable)) {
          isTaxUndeductable = false;
          isTaxDeductable = true;
        }
      } else {
        // configured for intracommunity with tax liability
        if ("Y".equals(data[i].istaxdeductable)) {
          isTaxDeductable = true;
        }
      }

      boolean taxIsCashVAT = StringUtils.equals(data[i].iscashvat, "Y");

      DocTax taxLine = new DocTax(C_Tax_ID, name, rate, taxBaseAmt, amount, isTaxUndeductable,
          isTaxDeductable, taxIsCashVAT);
      list.add(taxLine);
    }
    // Return Array
    DocTax[] tl = new DocTax[list.size()];
    list.toArray(tl);
    return tl;
  } // loadTaxes

  private DocLine_Payment[] loadDebtPayments() {
    ArrayList<Object> list = new ArrayList<Object>();
    DocInvoiceData[] data = null;
    try {
      data = DocInvoiceData.selectDebtPayments(connectionProvider, Record_ID);
      log4jDocInvoice.debug("############### DebtPayments.length = " + data.length);
      for (int i = 0; i < data.length; i++) {
        //
        String Line_ID = data[i].cDebtPaymentId;
        DocLine_Payment dpLine = new DocLine_Payment(DocumentType, Record_ID, Line_ID);
        log4jDocInvoice.debug(" dpLine.m_Record_Id2 = " + data[i].cDebtPaymentId);
        dpLine.m_Record_Id2 = data[i].cDebtPaymentId;
        dpLine.C_Currency_ID_From = data[i].cCurrencyId;
        dpLine.dpStatus = data[i].status;
        dpLine.isReceipt = data[i].isreceipt;
        dpLine.isPaid = data[i].ispaid;
        dpLine.isManual = data[i].ismanual;
        dpLine.WriteOffAmt = data[i].writeoffamt;
        dpLine.Amount = data[i].amount;
        list.add(dpLine);
      }
    } catch (ServletException e) {
      log4jDocInvoice.warn(e);
    }

    // Return Array
    DocLine_Payment[] tl = new DocLine_Payment[list.size()];
    list.toArray(tl);
    return tl;
  } // loadDebtPayments

  private DocLine_FinPaymentSchedule[] loadPayments() {
    ArrayList<Object> list = new ArrayList<Object>();
    DocInvoiceData[] data = null;
    try {
      data = DocInvoiceData.selectPayments(connectionProvider, Record_ID);
      log4jDocInvoice.debug("############### DebtPayments.length = " + data.length);
      for (int i = 0; i < data.length; i++) {
        //
        String Line_ID = data[i].finPaymentScheduleId;
        DocLine_FinPaymentSchedule dpLine = new DocLine_FinPaymentSchedule(DocumentType, Record_ID,
            Line_ID);
        log4jDocInvoice.debug(" dpLine.m_Record_Id2 = " + data[i].finPaymentScheduleId);
        dpLine.m_Record_Id2 = data[i].finPaymentScheduleId;
        dpLine.C_Currency_ID_From = data[i].cCurrencyId;
        dpLine.isPaid = data[i].ispaid;
        dpLine.Amount = data[i].amount;
        dpLine.PrepaidAmount = data[i].prepaidamt;

        list.add(dpLine);
      }
    } catch (ServletException e) {
      log4jDocInvoice.warn(e);
    }

    // Return Array
    DocLine_FinPaymentSchedule[] tl = new DocLine_FinPaymentSchedule[list.size()];
    list.toArray(tl);
    return tl;
  } // loadPayments

  /**
   * Create Facts (the accounting logic) for ARI, ARC, ARF, API, APC.
   * 
   * <pre>
   *  ARI, ARF
   *      Receivables     DR
   *      Charge                  CR
   *      TaxDue                  CR
   *      Revenue                 CR
   *  ARC
   *      Receivables             CR
   *      Charge          DR
   *      TaxDue          DR
   *      Revenue         RR
   *  API
   *      Payables                CR
   *      Charge          DR
   *      TaxCredit       DR
   *      Expense         DR
   *  APC
   *      Payables        DR
   *      Charge                  CR
   *      TaxCredit               CR
   *      Expense                 CR
   * </pre>
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
        DocInvoiceTemplate newTemplate = (DocInvoiceTemplate) Class.forName(strClassname)
            .getDeclaredConstructor()
            .newInstance();
        return newTemplate.createFact(this, as, conn, con, vars);
      } catch (Exception e) {
        log4j.error("Error while creating new instance for DocInvoiceTemplate - " + e);
      }
    }
    log4jDocInvoice.debug("Starting create fact");
    // create Fact Header
    Fact fact = new Fact(this, as, Fact.POST_Actual);
    String Fact_Acct_Group_ID = SequenceIdData.getUUID();
    // Cash based accounting
    if (!as.isAccrual()) {
      return null;
    }

    /** @todo Assumes TaxIncluded = N */

    // ARI, ARF, ARI_RM
    if (DocumentType.equals(AcctServer.DOCTYPE_ARInvoice)
        || DocumentType.equals(AcctServer.DOCTYPE_ARProForma)
        || DocumentType.equals(AcctServer.DOCTYPE_RMSalesInvoice)) {
      log4jDocInvoice.debug("Point 1");
      // Receivables DR
      if (m_payments == null || m_payments.length == 0) {
        for (int i = 0; m_debt_payments != null && i < m_debt_payments.length; i++) {
          if (m_debt_payments[i].isReceipt.equals("Y")) {
            fact.createLine(m_debt_payments[i],
                getAccountBPartner(C_BPartner_ID, as, true, m_debt_payments[i].dpStatus, conn),
                this.C_Currency_ID,
                getConvertedAmt(m_debt_payments[i].Amount, m_debt_payments[i].C_Currency_ID_From,
                    this.C_Currency_ID, DateAcct, "", AD_Client_ID, AD_Org_ID, conn),
                "", Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
          } else {
            fact.createLine(m_debt_payments[i],
                getAccountBPartner(C_BPartner_ID, as, false, m_debt_payments[i].dpStatus, conn),
                this.C_Currency_ID, "",
                getConvertedAmt(m_debt_payments[i].Amount, m_debt_payments[i].C_Currency_ID_From,
                    this.C_Currency_ID, DateAcct, "", AD_Client_ID, AD_Org_ID, conn),
                Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
          }
        }
      } else {
        for (int i = 0; m_payments != null && i < m_payments.length; i++) {
          fact.createLine(m_payments[i], getAccountBPartner(C_BPartner_ID, as, true, false, conn),
              this.C_Currency_ID, m_payments[i].Amount, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
              DocumentType, conn);
          if (m_payments[i].C_Currency_ID_From.equals(as.m_C_Currency_ID)
              && new BigDecimal(m_payments[i].PrepaidAmount).compareTo(ZERO) != 0) {
            fact.createLine(m_payments[i], getAccountBPartner(C_BPartner_ID, as, true, true, conn),
                this.C_Currency_ID, m_payments[i].PrepaidAmount, "", Fact_Acct_Group_ID,
                nextSeqNo(SeqNo), DocumentType, conn);
          } else if (!m_payments[i].C_Currency_ID_From.equals(as.m_C_Currency_ID)) {
            try {
              DocInvoiceData[] prepayments = DocInvoiceData.selectPrepayments(connectionProvider,
                  m_payments[i].Line_ID, Record_ID);
              for (int j = 0; j < prepayments.length; j++) {
                BigDecimal prePaymentAmt = convertAmount(new BigDecimal(prepayments[j].prepaidamt),
                    true, DateAcct, TABLEID_Payment, prepayments[j].finPaymentId,
                    m_payments[i].C_Currency_ID_From, as.m_C_Currency_ID, m_payments[i], as, fact,
                    Fact_Acct_Group_ID, nextSeqNo(SeqNo), conn);
                fact.createLine(m_payments[i],
                    getAccountBPartner(C_BPartner_ID, as, true, true, conn),
                    m_payments[i].C_Currency_ID_From, prePaymentAmt.toString(), "",
                    Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
              }
            } catch (ServletException e) {
              log4jDocInvoice.warn(e);
            }
          }
        }
      }
      if ((m_payments == null || m_payments.length == 0)
          && (m_debt_payments == null || m_debt_payments.length == 0)) {
        BigDecimal grossamt = new BigDecimal(Amounts[AMTTYPE_Gross]);
        BigDecimal prepayment = new BigDecimal(prepaymentamt);
        BigDecimal difference = grossamt.subtract(prepayment);
        if (!prepaymentamt.equals("0")) {
          if (grossamt.abs().compareTo(prepayment.abs()) != 0) {
            if (IsReturn.equals("Y") || isReversedInvoice) {
              fact.createLine(null, getAccountBPartner(C_BPartner_ID, as, true, true, conn),
                  this.C_Currency_ID, "", prepaymentamt, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
                  DocumentType, conn);
              fact.createLine(null, getAccountBPartner(C_BPartner_ID, as, true, false, conn),
                  this.C_Currency_ID, "", difference.toString(), Fact_Acct_Group_ID,
                  nextSeqNo(SeqNo), DocumentType, conn);
            } else {
              fact.createLine(null, getAccountBPartner(C_BPartner_ID, as, true, true, conn),
                  this.C_Currency_ID, prepaymentamt, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
                  DocumentType, conn);
              fact.createLine(null, getAccountBPartner(C_BPartner_ID, as, true, false, conn),
                  this.C_Currency_ID, difference.toString(), "", Fact_Acct_Group_ID,
                  nextSeqNo(SeqNo), DocumentType, conn);
            }
          } else {
            if (IsReturn.equals("Y") || isReversedInvoice) {
              fact.createLine(null, getAccountBPartner(C_BPartner_ID, as, true, true, conn),
                  this.C_Currency_ID, "", prepaymentamt, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
                  DocumentType, conn);
            } else {
              fact.createLine(null, getAccountBPartner(C_BPartner_ID, as, true, true, conn),
                  this.C_Currency_ID, prepaymentamt, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
                  DocumentType, conn);
            }
          }
        } else {
          fact.createLine(null, getAccountBPartner(C_BPartner_ID, as, true, false, conn),
              this.C_Currency_ID, Amounts[AMTTYPE_Gross], "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
              DocumentType, conn);
        }

      }
      // Charge CR
      log4jDocInvoice.debug("The first create line");
      fact.createLine(null, getAccount(AcctServer.ACCTTYPE_Charge, as, conn), C_Currency_ID, "",
          getAmount(AcctServer.AMTTYPE_Charge), Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType,
          conn);
      // TaxDue CR
      log4jDocInvoice.debug("m_taxes.length: " + m_taxes);
      BigDecimal grossamt = new BigDecimal(Amounts[AMTTYPE_Gross]);
      BigDecimal prepayment = new BigDecimal(prepaymentamt);

      for (int i = 0; m_taxes != null && i < m_taxes.length; i++) {
        // New docLine created to assign C_Tax_ID value to the entry
        DocLine docLine = new DocLine(DocumentType, Record_ID, "");
        docLine.m_C_Tax_ID = m_taxes[i].m_C_Tax_ID;

        BigDecimal percentageFinalAccount = CashVATUtil._100;
        final BigDecimal taxesAmountTotal = new BigDecimal(
            StringUtils.isBlank(m_taxes[i].m_amount) ? "0" : m_taxes[i].m_amount);
        BigDecimal taxToTransAccount = BigDecimal.ZERO;
        int precission = 0;
        OBContext.setAdminMode(true);
        try {
          Currency currency = OBDal.getInstance().get(Currency.class, C_Currency_ID);
          precission = currency.getStandardPrecision().intValue();
        } finally {
          OBContext.restorePreviousMode();
        }
        if (IsReversal.equals("Y")) {
          if (isCashVAT && m_taxes[i].m_isCashVAT) {
            if ((m_payments == null || m_payments.length == 0)
                && (m_debt_payments == null || m_debt_payments.length == 0)
                && (!prepaymentamt.equals("0"))) {
              percentageFinalAccount = ((prepayment.multiply(new BigDecimal(100)))
                  .divide(grossamt.abs(), precission, RoundingMode.HALF_UP));
              taxToTransAccount = CashVATUtil.calculatePercentageAmount(
                  CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
                  C_Currency_ID);
            } else {
              percentageFinalAccount = CashVATUtil
                  .calculatePrepaidPercentageForCashVATTax(m_taxes[i].m_C_Tax_ID, Record_ID);
              taxToTransAccount = CashVATUtil.calculatePercentageAmount(
                  CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
                  C_Currency_ID);
            }
            fact.createLine(docLine, m_taxes[i].getAccount(DocTax.ACCTTYPE_TaxDue_Trans, as, conn),
                C_Currency_ID, taxToTransAccount.toString(), "", Fact_Acct_Group_ID,
                nextSeqNo(SeqNo), DocumentType, conn);
          }
          final BigDecimal taxToFinalAccount = taxesAmountTotal.subtract(taxToTransAccount);
          fact.createLine(docLine, m_taxes[i].getAccount(DocTax.ACCTTYPE_TaxDue, as, conn),
              C_Currency_ID, taxToFinalAccount.toString(), "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
              DocumentType, conn);
        } else {
          if (isCashVAT && m_taxes[i].m_isCashVAT) {
            if ((m_payments == null || m_payments.length == 0)
                && (m_debt_payments == null || m_debt_payments.length == 0)
                && (!prepaymentamt.equals("0"))) {
              percentageFinalAccount = ((prepayment.multiply(new BigDecimal(100)))
                  .divide(grossamt.abs(), precission, RoundingMode.HALF_UP));
              taxToTransAccount = CashVATUtil.calculatePercentageAmount(
                  CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
                  C_Currency_ID);
            } else {
              percentageFinalAccount = CashVATUtil
                  .calculatePrepaidPercentageForCashVATTax(m_taxes[i].m_C_Tax_ID, Record_ID);
              taxToTransAccount = CashVATUtil.calculatePercentageAmount(
                  CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
                  C_Currency_ID);
            }
            fact.createLine(docLine, m_taxes[i].getAccount(DocTax.ACCTTYPE_TaxDue_Trans, as, conn),
                C_Currency_ID, "", taxToTransAccount.toString(), Fact_Acct_Group_ID,
                nextSeqNo(SeqNo), DocumentType, conn);
          }
          final BigDecimal taxToFinalAccount = taxesAmountTotal.subtract(taxToTransAccount);
          fact.createLine(docLine, m_taxes[i].getAccount(DocTax.ACCTTYPE_TaxDue, as, conn),
              C_Currency_ID, "", taxToFinalAccount.toString(), Fact_Acct_Group_ID, nextSeqNo(SeqNo),
              DocumentType, conn);
        }
      }
      // Revenue CR
      if (p_lines != null && p_lines.length > 0) {
        for (int i = 0; i < p_lines.length; i++) {
          Account account = ((DocLine_Invoice) p_lines[i])
              .getAccount(ProductInfo.ACCTTYPE_P_Revenue, as, conn);
          if (DocumentType.equals(AcctServer.DOCTYPE_RMSalesInvoice)) {
            Account accountReturnMaterial = ((DocLine_Invoice) p_lines[i])
                .getAccount(ProductInfo.ACCTTYPE_P_RevenueReturn, as, conn);
            if (accountReturnMaterial != null) {
              account = accountReturnMaterial;
            }
          }
          String amount = p_lines[i].getAmount();
          String amountConverted = "";
          ConversionRateDoc conversionRateCurrentDoc = getConversionRateDoc(TABLEID_Invoice,
              Record_ID, C_Currency_ID, as.m_C_Currency_ID);
          if (conversionRateCurrentDoc != null) {
            amountConverted = applyRate(new BigDecimal(p_lines[i].getAmount()),
                conversionRateCurrentDoc, true).setScale(2, RoundingMode.HALF_UP).toString();
          } else {
            amountConverted = getConvertedAmt(p_lines[i].getAmount(), C_Currency_ID,
                as.m_C_Currency_ID, DateAcct, "", AD_Client_ID, AD_Org_ID, conn);
          }
          if (((DocLine_Invoice) p_lines[i]).isDeferred()) {
            amount = createAccDefRevenueFact(
                fact, (DocLine_Invoice) p_lines[i], account, ((DocLine_Invoice) p_lines[i])
                    .getAccount(ProductInfo.ACCTTYPE_P_DefRevenue, as, conn),
                amountConverted, as.m_C_Currency_ID, conn);
            if (IsReversal.equals("Y")) {
              fact.createLine(p_lines[i], account, as.m_C_Currency_ID, amount, "",
                  Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
            } else {
              fact.createLine(p_lines[i], account, as.m_C_Currency_ID, "", amount,
                  Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
            }
          } else {
            if (IsReversal.equals("Y")) {
              fact.createLine(p_lines[i], account, this.C_Currency_ID, amount, "",
                  Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
            } else {
              fact.createLine(p_lines[i], account, this.C_Currency_ID, "", amount,
                  Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
            }
          }
          // If revenue has been deferred
          if (((DocLine_Invoice) p_lines[i]).isDeferred() && !amount.equals(amountConverted)) {
            amount = new BigDecimal(amountConverted).subtract(new BigDecimal(amount)).toString();
            if (IsReversal.equals("Y")) {
              fact.createLine(p_lines[i],
                  ((DocLine_Invoice) p_lines[i]).getAccount(ProductInfo.ACCTTYPE_P_DefRevenue, as,
                      conn),
                  as.m_C_Currency_ID, amount, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
                  DocumentType, conn);
            } else {
              fact.createLine(p_lines[i],
                  ((DocLine_Invoice) p_lines[i]).getAccount(ProductInfo.ACCTTYPE_P_DefRevenue, as,
                      conn),
                  as.m_C_Currency_ID, "", amount, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
                  DocumentType, conn);
            }
          }
        }
      }
      // Set Locations
      FactLine[] fLines = fact.getLines();
      for (int i = 0; i < fLines.length; i++) {
        if (fLines[i] != null) {
          fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(conn), true, conn); // from Loc
          fLines[i].setLocationFromBPartner(C_BPartner_Location_ID, false, conn); // to Loc
        }
      }
    }
    // ARC
    else if (this.DocumentType.equals(AcctServer.DOCTYPE_ARCredit)) {
      log4jDocInvoice.debug("Point 2");
      // Receivables CR
      if (m_payments == null || m_payments.length == 0) {
        for (int i = 0; m_debt_payments != null && i < m_debt_payments.length; i++) {
          BigDecimal amount = new BigDecimal(m_debt_payments[i].Amount);
          // BigDecimal ZERO = BigDecimal.ZERO;
          fact.createLine(m_debt_payments[i],
              getAccountBPartner(C_BPartner_ID, as, true, m_debt_payments[i].dpStatus, conn),
              this.C_Currency_ID, "",
              getConvertedAmt(((amount.negate())).toPlainString(),
                  m_debt_payments[i].C_Currency_ID_From, this.C_Currency_ID, DateAcct, "",
                  AD_Client_ID, AD_Org_ID, conn),
              Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
        }
      } else {
        for (int i = 0; m_payments != null && i < m_payments.length; i++) {
          BigDecimal amount = new BigDecimal(m_payments[i].Amount);
          BigDecimal prepaidAmount = new BigDecimal(m_payments[i].PrepaidAmount);
          fact.createLine(m_payments[i], getAccountBPartner(C_BPartner_ID, as, true, false, conn),
              this.C_Currency_ID, "", amount.negate().toString(), Fact_Acct_Group_ID,
              nextSeqNo(SeqNo), DocumentType, conn);
          // Pre-payment: Probably not needed as at this point we can not generate pre-payments
          // against ARC. Amount is negated
          if (m_payments[i].C_Currency_ID_From.equals(as.m_C_Currency_ID)
              && prepaidAmount.compareTo(ZERO) != 0) {
            fact.createLine(m_payments[i], getAccountBPartner(C_BPartner_ID, as, true, true, conn),
                this.C_Currency_ID, "", prepaidAmount.negate().toString(), Fact_Acct_Group_ID,
                nextSeqNo(SeqNo), DocumentType, conn);
          } else if (!m_payments[i].C_Currency_ID_From.equals(as.m_C_Currency_ID)) {
            try {
              DocInvoiceData[] prepayments = DocInvoiceData.selectPrepayments(connectionProvider,
                  m_payments[i].Line_ID, Record_ID);
              for (int j = 0; j < prepayments.length; j++) {
                BigDecimal prePaymentAmt = convertAmount(
                    new BigDecimal(prepayments[j].prepaidamt).negate(), true, DateAcct,
                    TABLEID_Payment, prepayments[j].finPaymentId, m_payments[i].C_Currency_ID_From,
                    as.m_C_Currency_ID, m_payments[i], as, fact, Fact_Acct_Group_ID,
                    nextSeqNo(SeqNo), conn);
                fact.createLine(m_payments[i],
                    getAccountBPartner(C_BPartner_ID, as, true, true, conn),
                    m_payments[i].C_Currency_ID_From, "", prePaymentAmt.toString(),
                    Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
              }
            } catch (ServletException e) {
              log4jDocInvoice.warn(e);
            }
          }
        }
      }
      if ((m_payments == null || m_payments.length == 0)
          && (m_debt_payments == null || m_debt_payments.length == 0)) {
        fact.createLine(null, getAccountBPartner(C_BPartner_ID, as, true, false, conn),
            this.C_Currency_ID, "", Amounts[AMTTYPE_Gross], Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            DocumentType, conn);
      }
      // Charge DR
      fact.createLine(null, getAccount(AcctServer.ACCTTYPE_Charge, as, conn), this.C_Currency_ID,
          getAmount(AcctServer.AMTTYPE_Charge), "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
          DocumentType, conn);
      // TaxDue DR
      for (int i = 0; m_taxes != null && i < m_taxes.length; i++) {
        // New docLine created to assign C_Tax_ID value to the entry
        DocLine docLine = new DocLine(DocumentType, Record_ID, "");
        docLine.m_C_Tax_ID = m_taxes[i].m_C_Tax_ID;

        BigDecimal percentageFinalAccount = CashVATUtil._100;
        final BigDecimal taxesAmountTotal = new BigDecimal(
            StringUtils.isBlank(m_taxes[i].getAmount()) ? "0" : m_taxes[i].getAmount());
        BigDecimal taxToTransAccount = BigDecimal.ZERO;
        if (isCashVAT && m_taxes[i].m_isCashVAT) {
          percentageFinalAccount = CashVATUtil
              .calculatePrepaidPercentageForCashVATTax(m_taxes[i].m_C_Tax_ID, Record_ID);
          taxToTransAccount = CashVATUtil.calculatePercentageAmount(
              CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal, C_Currency_ID);
          fact.createLine(docLine, m_taxes[i].getAccount(DocTax.ACCTTYPE_TaxDue_Trans, as, conn),
              this.C_Currency_ID, taxToTransAccount.toString(), "", Fact_Acct_Group_ID,
              nextSeqNo(SeqNo), DocumentType, conn);
        }
        final BigDecimal taxToFinalAccount = taxesAmountTotal.subtract(taxToTransAccount);
        fact.createLine(docLine, m_taxes[i].getAccount(DocTax.ACCTTYPE_TaxDue, as, conn),
            this.C_Currency_ID, taxToFinalAccount.toString(), "", Fact_Acct_Group_ID,
            nextSeqNo(SeqNo), DocumentType, conn);
      }
      // Revenue CR
      for (int i = 0; p_lines != null && i < p_lines.length; i++) {
        String amount = p_lines[i].getAmount();
        String amountCoverted = "";
        ConversionRateDoc conversionRateCurrentDoc = getConversionRateDoc(TABLEID_Invoice,
            Record_ID, C_Currency_ID, as.m_C_Currency_ID);
        if (conversionRateCurrentDoc != null) {
          amountCoverted = applyRate(new BigDecimal(p_lines[i].getAmount()),
              conversionRateCurrentDoc, true).setScale(2, RoundingMode.HALF_UP).toString();
        } else {
          amountCoverted = getConvertedAmt(p_lines[i].getAmount(), C_Currency_ID,
              as.m_C_Currency_ID, DateAcct, "", AD_Client_ID, AD_Org_ID, conn);
        }
        Account account = ((DocLine_Invoice) p_lines[i]).getAccount(ProductInfo.ACCTTYPE_P_Revenue,
            as, conn);
        if (((DocLine_Invoice) p_lines[i]).isDeferred()) {
          amount = createAccDefRevenueFact(
              fact, (DocLine_Invoice) p_lines[i], account, ((DocLine_Invoice) p_lines[i])
                  .getAccount(ProductInfo.ACCTTYPE_P_DefRevenue, as, conn),
              amountCoverted, as.m_C_Currency_ID, conn);
          fact.createLine(p_lines[i], account, as.m_C_Currency_ID, amount, "", Fact_Acct_Group_ID,
              nextSeqNo(SeqNo), DocumentType, conn);
        } else {
          fact.createLine(p_lines[i], account, this.C_Currency_ID, amount, "", Fact_Acct_Group_ID,
              nextSeqNo(SeqNo), DocumentType, conn);
        }
        // If revenue has been deferred
        if (((DocLine_Invoice) p_lines[i]).isDeferred() && !amount.equals(amountCoverted)) {
          amount = new BigDecimal(amountCoverted).subtract(new BigDecimal(amount)).toString();
          fact.createLine(p_lines[i],
              ((DocLine_Invoice) p_lines[i]).getAccount(ProductInfo.ACCTTYPE_P_DefRevenue, as,
                  conn),
              as.m_C_Currency_ID, amount, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType,
              conn);
        }
      }
      // Set Locations
      FactLine[] fLines = fact.getLines();
      for (int i = 0; fLines != null && i < fLines.length; i++) {
        if (fLines[i] != null) {
          fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(conn), true, conn); // from Loc
          fLines[i].setLocationFromBPartner(C_BPartner_Location_ID, false, conn); // to Loc
        }
      }
    }
    // API
    else if (this.DocumentType.equals(AcctServer.DOCTYPE_APInvoice)) {
      log4jDocInvoice.debug("Point 3");
      // Liability CR
      if (m_payments == null || m_payments.length == 0) {
        for (int i = 0; m_debt_payments != null && i < m_debt_payments.length; i++) {
          if (m_debt_payments[i].isReceipt.equals("Y")) {
            fact.createLine(m_debt_payments[i],
                getAccountBPartner(C_BPartner_ID, as, true, m_debt_payments[i].dpStatus, conn),
                this.C_Currency_ID,
                getConvertedAmt(m_debt_payments[i].Amount, m_debt_payments[i].C_Currency_ID_From,
                    this.C_Currency_ID, DateAcct, "", AD_Client_ID, AD_Org_ID, conn),
                "", Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
          } else {
            fact.createLine(m_debt_payments[i],
                getAccountBPartner(C_BPartner_ID, as, false, m_debt_payments[i].dpStatus, conn),
                this.C_Currency_ID, "",
                getConvertedAmt(m_debt_payments[i].Amount, m_debt_payments[i].C_Currency_ID_From,
                    this.C_Currency_ID, DateAcct, "", AD_Client_ID, AD_Org_ID, conn),
                Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
          }
        }
      } else {
        for (int i = 0; m_payments != null && i < m_payments.length; i++) {
          fact.createLine(m_payments[i], getAccountBPartner(C_BPartner_ID, as, false, false, conn),
              this.C_Currency_ID, "", m_payments[i].Amount, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
              DocumentType, conn);
          if (m_payments[i].C_Currency_ID_From.equals(as.m_C_Currency_ID)
              && new BigDecimal(m_payments[i].PrepaidAmount).compareTo(ZERO) != 0) {
            fact.createLine(m_payments[i], getAccountBPartner(C_BPartner_ID, as, false, true, conn),
                this.C_Currency_ID, "", m_payments[i].PrepaidAmount, Fact_Acct_Group_ID,
                nextSeqNo(SeqNo), DocumentType, conn);
          } else if (!m_payments[i].C_Currency_ID_From.equals(as.m_C_Currency_ID)) {
            try {
              DocInvoiceData[] prepayments = DocInvoiceData.selectPrepayments(connectionProvider,
                  m_payments[i].Line_ID, Record_ID);
              for (int j = 0; j < prepayments.length; j++) {
                BigDecimal prePaymentAmt = convertAmount(new BigDecimal(prepayments[j].prepaidamt),
                    false, DateAcct, TABLEID_Payment, prepayments[j].finPaymentId,
                    m_payments[i].C_Currency_ID_From, as.m_C_Currency_ID, m_payments[i], as, fact,
                    Fact_Acct_Group_ID, nextSeqNo(SeqNo), conn);
                fact.createLine(m_payments[i],
                    getAccountBPartner(C_BPartner_ID, as, false, true, conn),
                    m_payments[i].C_Currency_ID_From, "", prePaymentAmt.toString(),
                    Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
              }
            } catch (ServletException e) {
              log4jDocInvoice.warn(e);
            }
          }
        }
      }
      if ((m_payments == null || m_payments.length == 0)
          && (m_debt_payments == null || m_debt_payments.length == 0)) {
        BigDecimal grossamt = new BigDecimal(Amounts[AMTTYPE_Gross]);
        BigDecimal prepayment = new BigDecimal(prepaymentamt);
        BigDecimal difference = grossamt.subtract(prepayment);
        if (!prepaymentamt.equals("0")) {
          if (grossamt.abs().compareTo(prepayment.abs()) != 0) {
            if (IsReturn.equals("Y") || isReversedInvoice) {
              fact.createLine(null, getAccountBPartner(C_BPartner_ID, as, false, true, conn),
                  this.C_Currency_ID, prepaymentamt, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
                  DocumentType, conn);
              fact.createLine(null, getAccountBPartner(C_BPartner_ID, as, false, false, conn),
                  this.C_Currency_ID, difference.toString(), "", Fact_Acct_Group_ID,
                  nextSeqNo(SeqNo), DocumentType, conn);
            } else {
              fact.createLine(null, getAccountBPartner(C_BPartner_ID, as, false, true, conn),
                  this.C_Currency_ID, "", prepaymentamt, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
                  DocumentType, conn);
              fact.createLine(null, getAccountBPartner(C_BPartner_ID, as, false, false, conn),
                  this.C_Currency_ID, "", difference.toString(), Fact_Acct_Group_ID,
                  nextSeqNo(SeqNo), DocumentType, conn);
            }
          } else {
            if (IsReturn.equals("Y") || isReversedInvoice) {
              fact.createLine(null, getAccountBPartner(C_BPartner_ID, as, false, true, conn),
                  this.C_Currency_ID, prepaymentamt, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
                  DocumentType, conn);
            } else {
              fact.createLine(null, getAccountBPartner(C_BPartner_ID, as, false, true, conn),
                  this.C_Currency_ID, "", prepaymentamt, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
                  DocumentType, conn);
            }
          }
        } else {
          fact.createLine(null, getAccountBPartner(C_BPartner_ID, as, false, false, conn),
              this.C_Currency_ID, "", Amounts[AMTTYPE_Gross], Fact_Acct_Group_ID, nextSeqNo(SeqNo),
              DocumentType, conn);
        }

      }
      // Charge DR
      fact.createLine(null, getAccount(AcctServer.ACCTTYPE_Charge, as, conn), this.C_Currency_ID,
          getAmount(AcctServer.AMTTYPE_Charge), "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
          DocumentType, conn);
      BigDecimal grossamt = new BigDecimal(Amounts[AMTTYPE_Gross]);
      BigDecimal prepayment = new BigDecimal(prepaymentamt);
      // TaxCredit DR
      for (int i = 0; m_taxes != null && i < m_taxes.length; i++) {
        // New docLine created to assign C_Tax_ID value to the entry
        DocLine docLine = new DocLine(DocumentType, Record_ID, "");
        docLine.m_C_Tax_ID = m_taxes[i].m_C_Tax_ID;
        OBContext.setAdminMode(true);
        int precission = 0;
        try {
          Currency currency = OBDal.getInstance().get(Currency.class, C_Currency_ID);
          precission = currency.getStandardPrecision().intValue();
        } finally {
          OBContext.restorePreviousMode();
        }
        if (!m_taxes[i].m_isTaxUndeductable) {
          BigDecimal percentageFinalAccount = CashVATUtil._100;
          final BigDecimal taxesAmountTotal = new BigDecimal(
              StringUtils.isBlank(m_taxes[i].getAmount()) ? "0" : m_taxes[i].getAmount());
          BigDecimal taxToTransAccount = BigDecimal.ZERO;
          if (IsReversal.equals("Y")) {
            if (isCashVAT && m_taxes[i].m_isCashVAT) {
              if ((m_payments == null || m_payments.length == 0)
                  && (m_debt_payments == null || m_debt_payments.length == 0)
                  && (!prepaymentamt.equals("0"))) {
                percentageFinalAccount = ((prepayment.multiply(new BigDecimal(100)))
                    .divide(grossamt.abs(), precission, RoundingMode.HALF_UP));
                taxToTransAccount = CashVATUtil.calculatePercentageAmount(
                    CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
                    C_Currency_ID);
              } else {
                percentageFinalAccount = CashVATUtil
                    .calculatePrepaidPercentageForCashVATTax(m_taxes[i].m_C_Tax_ID, Record_ID);
                taxToTransAccount = CashVATUtil.calculatePercentageAmount(
                    CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
                    C_Currency_ID);
              }
              fact.createLine(docLine,
                  m_taxes[i].getAccount(DocTax.ACCTTYPE_TaxCredit_Trans, as, conn),
                  this.C_Currency_ID, "", taxToTransAccount.toString(), Fact_Acct_Group_ID,
                  nextSeqNo(SeqNo), DocumentType, conn);
            }
            final BigDecimal taxToFinalAccount = taxesAmountTotal.subtract(taxToTransAccount);
            fact.createLine(docLine, m_taxes[i].getAccount(DocTax.ACCTTYPE_TaxCredit, as, conn),
                this.C_Currency_ID, "", taxToFinalAccount.toString(), Fact_Acct_Group_ID,
                nextSeqNo(SeqNo), DocumentType, conn);
          } else {
            if (isCashVAT && m_taxes[i].m_isCashVAT) {
              if ((m_payments == null || m_payments.length == 0)
                  && (m_debt_payments == null || m_debt_payments.length == 0)
                  && (!prepaymentamt.equals("0"))) {
                percentageFinalAccount = ((prepayment.multiply(new BigDecimal(100)))
                    .divide(grossamt.abs(), precission, RoundingMode.HALF_UP));
                taxToTransAccount = CashVATUtil.calculatePercentageAmount(
                    CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
                    C_Currency_ID);
              } else {
                percentageFinalAccount = CashVATUtil
                    .calculatePrepaidPercentageForCashVATTax(m_taxes[i].m_C_Tax_ID, Record_ID);
                taxToTransAccount = CashVATUtil.calculatePercentageAmount(
                    CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
                    C_Currency_ID);
              }
              fact.createLine(docLine,
                  m_taxes[i].getAccount(DocTax.ACCTTYPE_TaxCredit_Trans, as, conn),
                  this.C_Currency_ID, taxToTransAccount.toString(), "", Fact_Acct_Group_ID,
                  nextSeqNo(SeqNo), DocumentType, conn);
            }
            final BigDecimal taxToFinalAccount = taxesAmountTotal.subtract(taxToTransAccount);
            fact.createLine(docLine, m_taxes[i].getAccount(DocTax.ACCTTYPE_TaxCredit, as, conn),
                this.C_Currency_ID, taxToFinalAccount.toString(), "", Fact_Acct_Group_ID,
                nextSeqNo(SeqNo), DocumentType, conn);
          }

        } else {
          DocLineInvoiceData[] data = null;
          try {
            data = DocLineInvoiceData.selectUndeductable(connectionProvider, Record_ID,
                m_taxes[i].m_C_Tax_ID);
          } catch (ServletException e) {
            log4jDocInvoice.warn(e);
          }

          BigDecimal cumulativeTaxLineAmount = new BigDecimal(0);
          BigDecimal taxAmount = new BigDecimal(0);
          for (int j = 0; data != null && j < data.length; j++) {
            DocLine docLine1 = new DocLine(DocumentType, Record_ID, "");
            docLine1.m_C_Tax_ID = data[j].cTaxId;
            docLine1.m_TrxLine_ID = data[j].cInvoicelineId;
            docLine1.m_C_BPartner_ID = data[j].cBpartnerId;
            docLine1.m_M_Product_ID = data[j].mProductId;
            docLine1.m_C_Costcenter_ID = data[j].cCostcenterId;
            docLine1.m_C_Project_ID = data[j].cProjectId;
            docLine1.m_User1_ID = data[j].user1id;
            docLine1.m_User2_ID = data[j].user2id;
            docLine1.m_C_Activity_ID = data[j].cActivityId;
            docLine1.m_C_Campaign_ID = data[j].cCampaignId;
            docLine1.m_A_Asset_ID = data[j].aAssetId;
            String strtaxAmount = null;

            try {
              DocInvoiceData[] dataEx = DocInvoiceData.selectProductAcct(conn,
                  as.getC_AcctSchema_ID(), m_taxes[i].m_C_Tax_ID, Record_ID,
                  docLine1.m_M_Product_ID);
              if (dataEx.length == 0) {
                dataEx = DocInvoiceData.selectGLItemAcctForTaxLine(conn, as.getC_AcctSchema_ID(),
                    m_taxes[i].m_C_Tax_ID, Record_ID);
              }
              strtaxAmount = m_taxes[i].getAmount();
              taxAmount = new BigDecimal(strtaxAmount.equals("") ? "0.00" : strtaxAmount);
              if (j == data.length - 1) {
                data[j].taxamt = taxAmount.subtract(cumulativeTaxLineAmount).toPlainString();
              }
              try {

                if (this.DocumentType.equals(AcctServer.DOCTYPE_APInvoice)) {
                  if (IsReversal.equals("Y")) {
                    fact.createLine(docLine1, Account.getAccount(conn, dataEx[0].pExpenseAcct),
                        this.C_Currency_ID, "", data[j].taxamt, Fact_Acct_Group_ID,
                        nextSeqNo(SeqNo), DocumentType, conn);

                  } else {
                    fact.createLine(docLine1, Account.getAccount(conn, dataEx[0].pExpenseAcct),
                        this.C_Currency_ID, data[j].taxamt, "", Fact_Acct_Group_ID,
                        nextSeqNo(SeqNo), DocumentType, conn);
                  }
                } else if (this.DocumentType.equals(AcctServer.DOCTYPE_APCredit)) {
                  fact.createLine(docLine1, Account.getAccount(conn, dataEx[0].pExpenseAcct),
                      this.C_Currency_ID, "", data[j].taxamt, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
                      DocumentType, conn);
                }
                cumulativeTaxLineAmount = cumulativeTaxLineAmount
                    .add(new BigDecimal(data[j].taxamt));
              } catch (ServletException e) {
                log4jDocInvoice.error("Exception in createLineForTaxUndeductable method: " + e);
              }
            } catch (ServletException e) {
              log4jDocInvoice.warn(e);
            }
          }
        }
      }
      // Expense DR
      for (int i = 0; p_lines != null && i < p_lines.length; i++) {
        String amount = p_lines[i].getAmount();
        String amountConverted = "";
        ConversionRateDoc conversionRateCurrentDoc = getConversionRateDoc(TABLEID_Invoice,
            Record_ID, C_Currency_ID, as.m_C_Currency_ID);
        if (conversionRateCurrentDoc != null) {
          amountConverted = applyRate(new BigDecimal(p_lines[i].getAmount()),
              conversionRateCurrentDoc, true).setScale(2, RoundingMode.HALF_UP).toString();
        } else {
          amountConverted = getConvertedAmt(p_lines[i].getAmount(), C_Currency_ID,
              as.m_C_Currency_ID, DateAcct, "", AD_Client_ID, AD_Org_ID, conn);
        }
        if (((DocLine_Invoice) p_lines[i]).isDeferred()) {
          amount = createAccDefExpenseFact(fact, (DocLine_Invoice) p_lines[i],
              ((DocLine_Invoice) p_lines[i]).getAccount(ProductInfo.ACCTTYPE_P_Expense, as, conn),
              ((DocLine_Invoice) p_lines[i]).getAccount(ProductInfo.ACCTTYPE_P_DefExpense, as,
                  conn),
              amountConverted, as.m_C_Currency_ID, conn);
          if (IsReversal.equals("Y")) {
            fact.createLine(p_lines[i],
                ((DocLine_Invoice) p_lines[i]).getAccount(ProductInfo.ACCTTYPE_P_Expense, as, conn),
                as.m_C_Currency_ID, "", amount, Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType,
                conn);
          } else {
            fact.createLine(p_lines[i],
                ((DocLine_Invoice) p_lines[i]).getAccount(ProductInfo.ACCTTYPE_P_Expense, as, conn),
                as.m_C_Currency_ID, amount, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType,
                conn);
          }
        } else {
          if (IsReversal.equals("Y")) {
            fact.createLine(p_lines[i],
                ((DocLine_Invoice) p_lines[i]).getAccount(ProductInfo.ACCTTYPE_P_Expense, as, conn),
                this.C_Currency_ID, "", amount, Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType,
                conn);
          } else {
            fact.createLine(p_lines[i],
                ((DocLine_Invoice) p_lines[i]).getAccount(ProductInfo.ACCTTYPE_P_Expense, as, conn),
                this.C_Currency_ID, amount, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType,
                conn);
          }
        }
        // If expense has been deferred
        if (((DocLine_Invoice) p_lines[i]).isDeferred() && !amount.equals(amountConverted)) {
          amount = new BigDecimal(amountConverted).subtract(new BigDecimal(amount)).toString();
          if (IsReversal.equals("Y")) {
            fact.createLine(p_lines[i],
                ((DocLine_Invoice) p_lines[i]).getAccount(ProductInfo.ACCTTYPE_P_DefExpense, as,
                    conn),
                as.m_C_Currency_ID, "", amount, Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType,
                conn);
          } else {
            fact.createLine(p_lines[i],
                ((DocLine_Invoice) p_lines[i]).getAccount(ProductInfo.ACCTTYPE_P_DefExpense, as,
                    conn),
                as.m_C_Currency_ID, amount, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType,
                conn);
          }
        }
      }
      // Set Locations
      FactLine[] fLines = fact.getLines();
      for (int i = 0; fLines != null && i < fLines.length; i++) {
        if (fLines[i] != null) {
          fLines[i].setLocationFromBPartner(C_BPartner_Location_ID, true, conn); // from Loc
          fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(conn), false, conn); // to Loc
        }
      }
      updateProductInfo(as.getC_AcctSchema_ID(), conn, con); // only API
    }
    // APC
    else if (this.DocumentType.equals(AcctServer.DOCTYPE_APCredit)) {
      log4jDocInvoice.debug("Point 4");
      // Liability DR
      if (m_payments == null || m_payments.length == 0) {
        for (int i = 0; m_debt_payments != null && i < m_debt_payments.length; i++) {
          BigDecimal amount = new BigDecimal(m_debt_payments[i].Amount);
          // BigDecimal ZERO = BigDecimal.ZERO;
          fact.createLine(m_debt_payments[i],
              getAccountBPartner(C_BPartner_ID, as, false, m_debt_payments[i].dpStatus, conn),
              this.C_Currency_ID,
              getConvertedAmt(((amount.negate())).toPlainString(),
                  m_debt_payments[i].C_Currency_ID_From, this.C_Currency_ID, DateAcct, "",
                  AD_Client_ID, AD_Org_ID, conn),
              "", Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
        }
      } else {
        for (int i = 0; m_payments != null && i < m_payments.length; i++) {
          BigDecimal amount = new BigDecimal(m_payments[i].Amount);
          BigDecimal prepaidAmount = new BigDecimal(m_payments[i].PrepaidAmount);
          fact.createLine(m_payments[i], getAccountBPartner(C_BPartner_ID, as, false, false, conn),
              this.C_Currency_ID, amount.negate().toString(), "", Fact_Acct_Group_ID,
              nextSeqNo(SeqNo), DocumentType, conn);
          // Pre-payment: Probably not needed as at this point we can not generate pre-payments
          // against APC. Amount is negated
          if (m_payments[i].C_Currency_ID_From.equals(as.m_C_Currency_ID)
              && prepaidAmount.compareTo(ZERO) != 0) {
            fact.createLine(m_payments[i], getAccountBPartner(C_BPartner_ID, as, false, true, conn),
                this.C_Currency_ID, prepaidAmount.negate().toString(), "", Fact_Acct_Group_ID,
                nextSeqNo(SeqNo), DocumentType, conn);
          } else if (!m_payments[i].C_Currency_ID_From.equals(as.m_C_Currency_ID)) {
            try {
              DocInvoiceData[] prepayments = DocInvoiceData.selectPrepayments(connectionProvider,
                  m_payments[i].Line_ID, Record_ID);
              for (int j = 0; j < prepayments.length; j++) {
                BigDecimal prePaymentAmt = convertAmount(
                    new BigDecimal(prepayments[j].prepaidamt).negate(), false, DateAcct,
                    TABLEID_Payment, prepayments[j].finPaymentId, m_payments[i].C_Currency_ID_From,
                    as.m_C_Currency_ID, m_payments[i], as, fact, Fact_Acct_Group_ID,
                    nextSeqNo(SeqNo), conn);
                fact.createLine(m_payments[i],
                    getAccountBPartner(C_BPartner_ID, as, false, true, conn),
                    m_payments[i].C_Currency_ID_From, prePaymentAmt.toString(), "",
                    Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
              }
            } catch (ServletException e) {
              log4jDocInvoice.warn(e);
            }
          }
        }
      }
      if ((m_payments == null || m_payments.length == 0)
          && (m_debt_payments == null || m_debt_payments.length == 0)) {
        fact.createLine(null, getAccountBPartner(C_BPartner_ID, as, false, false, conn),
            this.C_Currency_ID, Amounts[AMTTYPE_Gross], "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            DocumentType, conn);
      }
      // Charge CR
      fact.createLine(null, getAccount(AcctServer.ACCTTYPE_Charge, as, conn), this.C_Currency_ID,
          "", getAmount(AcctServer.AMTTYPE_Charge), Fact_Acct_Group_ID, nextSeqNo(SeqNo),
          DocumentType, conn);
      // TaxCredit CR
      for (int i = 0; m_taxes != null && i < m_taxes.length; i++) {
        // New docLine created to assign C_Tax_ID value to the entry
        DocLine docLine = new DocLine(DocumentType, Record_ID, "");
        docLine.m_C_Tax_ID = m_taxes[i].m_C_Tax_ID;
        if (m_taxes[i].m_isTaxUndeductable) {
          computeTaxUndeductableLine(conn, as, fact, docLine, Fact_Acct_Group_ID,
              m_taxes[i].m_C_Tax_ID, m_taxes[i].getAmount());

        } else {
          BigDecimal percentageFinalAccount = CashVATUtil._100;
          final BigDecimal taxesAmountTotal = new BigDecimal(
              StringUtils.isBlank(m_taxes[i].getAmount()) ? "0" : m_taxes[i].getAmount());
          BigDecimal taxToTransAccount = BigDecimal.ZERO;
          if (isCashVAT && m_taxes[i].m_isCashVAT) {
            percentageFinalAccount = CashVATUtil
                .calculatePrepaidPercentageForCashVATTax(m_taxes[i].m_C_Tax_ID, Record_ID);
            taxToTransAccount = CashVATUtil.calculatePercentageAmount(
                CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal, C_Currency_ID);
            fact.createLine(docLine,
                m_taxes[i].getAccount(DocTax.ACCTTYPE_TaxCredit_Trans, as, conn),
                this.C_Currency_ID, "", taxToTransAccount.toString(), Fact_Acct_Group_ID,
                nextSeqNo(SeqNo), DocumentType, conn);
          }
          final BigDecimal taxToFinalAccount = taxesAmountTotal.subtract(taxToTransAccount);
          fact.createLine(docLine, m_taxes[i].getAccount(DocTax.ACCTTYPE_TaxCredit, as, conn),
              this.C_Currency_ID, "", taxToFinalAccount.toString(), Fact_Acct_Group_ID,
              nextSeqNo(SeqNo), DocumentType, conn);
        }
      }
      // Expense CR
      for (int i = 0; p_lines != null && i < p_lines.length; i++) {
        String amount = p_lines[i].getAmount();
        String amountConverted = "";
        ConversionRateDoc conversionRateCurrentDoc = getConversionRateDoc(TABLEID_Invoice,
            Record_ID, C_Currency_ID, as.m_C_Currency_ID);
        if (conversionRateCurrentDoc != null) {
          amountConverted = applyRate(new BigDecimal(p_lines[i].getAmount()),
              conversionRateCurrentDoc, true).setScale(2, RoundingMode.HALF_UP).toString();
        } else {
          amountConverted = getConvertedAmt(p_lines[i].getAmount(), C_Currency_ID,
              as.m_C_Currency_ID, DateAcct, "", AD_Client_ID, AD_Org_ID, conn);
        }
        Account account = ((DocLine_Invoice) p_lines[i]).getAccount(ProductInfo.ACCTTYPE_P_Expense,
            as, conn);
        if (((DocLine_Invoice) p_lines[i]).isDeferred()) {
          amount = createAccDefExpenseFact(
              fact, (DocLine_Invoice) p_lines[i], account, ((DocLine_Invoice) p_lines[i])
                  .getAccount(ProductInfo.ACCTTYPE_P_DefExpense, as, conn),
              amountConverted, as.m_C_Currency_ID, conn);
          fact.createLine(p_lines[i], account, as.m_C_Currency_ID, "", amount, Fact_Acct_Group_ID,
              nextSeqNo(SeqNo), DocumentType, conn);
        } else {
          fact.createLine(p_lines[i], account, this.C_Currency_ID, "", amount, Fact_Acct_Group_ID,
              nextSeqNo(SeqNo), DocumentType, conn);
        }
        // If expense has been deferred
        if (((DocLine_Invoice) p_lines[i]).isDeferred() && !amount.equals(amountConverted)) {
          amount = new BigDecimal(amountConverted).subtract(new BigDecimal(amount)).toString();
          fact.createLine(p_lines[i],
              ((DocLine_Invoice) p_lines[i]).getAccount(ProductInfo.ACCTTYPE_P_DefExpense, as,
                  conn),
              as.m_C_Currency_ID, "", amount, Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType,
              conn);
        }

      }
      // Set Locations
      FactLine[] fLines = fact.getLines();
      for (int i = 0; fLines != null && i < fLines.length; i++) {
        if (fLines[i] != null) {
          fLines[i].setLocationFromBPartner(C_BPartner_Location_ID, true, conn); // from Loc
          fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(conn), false, conn); // to Loc
        }
      }
    } else {
      log4jDocInvoice.warn("Doc_Invoice - DocumentType unknown: " + this.DocumentType);
      fact = null;
    }
    SeqNo = "0";
    return fact;
  }// createFact

  public String createAccDefRevenueFact(Fact fact, DocLine_Invoice line, Account prodRevAccount,
      Account prodDefRevAccount, String lineAmount, String strCurrencyId, ConnectionProvider conn) {
    BigDecimal amount = new BigDecimal(lineAmount);
    String Fact_Acct_Group_ID = SequenceIdData.getUUID();
    ArrayList<HashMap<String, String>> plan = new ArrayList<HashMap<String, String>>();
    Period startingPeriod = OBDal.getInstance().get(Period.class, line.getStartingPeriodId());
    plan = calculateAccDefPlan(startingPeriod, line.getPeriodNumber(), amount, strCurrencyId);
    for (HashMap<String, String> planLine : plan) {
      DocLine planDocLine = new DocLine(DocumentType, Record_ID, line.m_TrxLine_ID);
      planDocLine.copyInfo(line);
      planDocLine.m_DateAcct = planLine.get("date");
      if (IsReversal.equals("Y")) {
        // Revenue Account
        fact.createLine(planDocLine, prodRevAccount, strCurrencyId, planLine.get("amount"), "",
            Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
        // Deferred Revenue Account
        fact.createLine(planDocLine, prodDefRevAccount, strCurrencyId, "", planLine.get("amount"),
            Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
      } else {
        // Deferred Revenue Account
        fact.createLine(planDocLine, prodDefRevAccount, strCurrencyId, planLine.get("amount"), "",
            Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
        // Revenue Account
        fact.createLine(planDocLine, prodRevAccount, strCurrencyId, "", planLine.get("amount"),
            Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
      }
      amount = amount.subtract(new BigDecimal(planLine.get("amount")));
      Fact_Acct_Group_ID = SequenceIdData.getUUID();
    }

    return amount.toString();
  }

  private ArrayList<HashMap<String, String>> calculateAccDefPlan(Period startingPeriod,
      int periodNumber, BigDecimal amount, String strCurrencyId) {
    Period period = startingPeriod;
    Date date = period.getEndingDate();
    ArrayList<HashMap<String, String>> plan = new ArrayList<HashMap<String, String>>();
    int i = 1;
    BigDecimal total = BigDecimal.ZERO;
    int stdPrecision = 0;
    OBContext.setAdminMode(true);
    try {
      stdPrecision = OBDal.getInstance()
          .get(Currency.class, this.C_Currency_ID)
          .getStandardPrecision()
          .intValue();
    } finally {
      OBContext.restorePreviousMode();
    }
    BigDecimal periodAmount = amount
        .divide(new BigDecimal(periodNumber), new MathContext(32, RoundingMode.HALF_UP))
        .setScale(stdPrecision, RoundingMode.HALF_UP);

    while (i <= periodNumber) {
      if (!OBDateUtils.formatDate(date).equals(DateAcct)) {
        HashMap<String, String> hm = new HashMap<String, String>();
        hm.put("date", OBDateUtils.formatDate(date));
        hm.put("amount",
            i == periodNumber ? amount.subtract(total).toString() : periodAmount.toString());
        plan.add(hm);
      }
      try {
        AcctServerData[] data = AcctServerData.periodOpen(connectionProvider, AD_Client_ID,
            DocumentType, AD_Org_ID, OBDateUtils.formatDate(period.getEndingDate()));
        if ("".equals(data[0].period)) {
          setStatus(STATUS_PeriodClosed);
          throw new OBException("@PeriodNotAvailable@");
        }
      } catch (ServletException e) {
        log4j.warn("DocInvoice - Error checking period open.", e);
        e.printStackTrace();
      }
      if (i < periodNumber) {
        period = AccDefUtility.getNextPeriod(period);
        date = period.getEndingDate();
      }
      total = total.add(periodAmount);
      i++;
    }
    return plan;
  }

  public String createAccDefExpenseFact(Fact fact, DocLine_Invoice line, Account prodExpAccount,
      Account prodDefExpAccount, String lineAmount, String strCurrencyId, ConnectionProvider conn) {
    BigDecimal amount = new BigDecimal(lineAmount);
    String Fact_Acct_Group_ID = SequenceIdData.getUUID();
    ArrayList<HashMap<String, String>> plan = new ArrayList<HashMap<String, String>>();
    Period startingPeriod = OBDal.getInstance().get(Period.class, line.getStartingPeriodId());
    plan = calculateAccDefPlan(startingPeriod, line.getPeriodNumber(), amount, strCurrencyId);
    for (HashMap<String, String> planLine : plan) {
      DocLine planDocLine = new DocLine(DocumentType, Record_ID, line.m_TrxLine_ID);
      planDocLine.copyInfo(line);
      planDocLine.m_DateAcct = planLine.get("date");
      if (IsReversal.equals("Y")) {
        // Expense Account
        fact.createLine(planDocLine, prodExpAccount, strCurrencyId, "", planLine.get("amount"),
            Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
        // Deferred Expense Account
        fact.createLine(planDocLine, prodDefExpAccount, strCurrencyId, planLine.get("amount"), "",
            Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
      } else {
        // Deferred Expense Account
        fact.createLine(planDocLine, prodDefExpAccount, strCurrencyId, "", planLine.get("amount"),
            Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
        // Expense Account
        fact.createLine(planDocLine, prodExpAccount, strCurrencyId, planLine.get("amount"), "",
            Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
      }
      amount = amount.subtract(new BigDecimal(planLine.get("amount")));
      Fact_Acct_Group_ID = SequenceIdData.getUUID();
    }
    return amount.toString();
  }

  /**
   * Update Product Info. - Costing (PriceLastInv) - PO (PriceLastInv)
   * 
   * @param C_AcctSchema_ID
   *          accounting schema
   */
  public void updateProductInfo(String C_AcctSchema_ID, ConnectionProvider conn, Connection con) {
    log4jDocInvoice.debug("updateProductInfo - C_Invoice_ID=" + this.Record_ID);

    /**
     * @todo Last.. would need to compare document/last updated date would need to maintain
     *       LastPriceUpdateDate on _PO and _Costing
     */

    // update Product PO info
    // should only be once, but here for every AcctSchema
    // ignores multiple lines with same product - just uses first
    int no = 0;
    try {
      no = DocInvoiceData.updateProductPO(con, conn, Record_ID);
      log4jDocInvoice.debug("M_Product_PO - Updated=" + no);

    } catch (ServletException e) {
      log4jDocInvoice.warn(e);
      if (e.getMessage().contains("@NoConversionRate@")) {
        setMessageResult(Utility.translateError(conn, RequestContext.get().getVariablesSecureApp(),
            OBContext.getOBContext().getLanguage().getId(), e.getMessage()));
        throw new IllegalStateException();
      }
    }
  } // updateProductInfo

  /**
   * Get Source Currency Balance - subtracts line and tax amounts from total - no rounding
   * 
   * @return positive amount, if total invoice is bigger than lines
   */
  @Override
  public BigDecimal getBalance() {
    // BigDecimal ZERO = new BigDecimal("0");
    BigDecimal retValue = ZERO;
    StringBuffer sb = new StringBuffer(" [");
    // Total
    retValue = retValue.add(new BigDecimal(getAmount(AcctServer.AMTTYPE_Gross)));
    sb.append(getAmount(AcctServer.AMTTYPE_Gross));
    // - Charge
    retValue = retValue.subtract(new BigDecimal(getAmount(AcctServer.AMTTYPE_Charge)));
    sb.append("-").append(getAmount(AcctServer.AMTTYPE_Charge));
    // - Tax
    for (int i = 0; i < m_taxes.length; i++) {
      retValue = retValue.subtract(new BigDecimal(m_taxes[i].getAmount()));
      sb.append("-").append(m_taxes[i].getAmount());
    }
    // - Lines
    for (int i = 0; p_lines != null && i < p_lines.length; i++) {
      retValue = retValue.subtract(new BigDecimal(p_lines[i].getAmount()));
      sb.append("-").append(p_lines[i].getAmount());
    }
    sb.append("]");
    //
    log4jDocInvoice.debug("Balance=" + retValue + sb.toString());
    return retValue;
  } // getBalance

  public String nextSeqNo(String oldSeqNo) {
    log4jDocInvoice.debug("DocInvoice - oldSeqNo = " + oldSeqNo);
    BigDecimal seqNo = new BigDecimal(oldSeqNo);
    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
    log4jDocInvoice.debug("DocInvoice - nextSeqNo = " + SeqNo);
    return SeqNo;
  }

  /**
   * Get the account for Accounting Schema
   * 
   * @param cBPartnerId
   *          business partner id
   * @param as
   *          accounting schema
   * @return Account
   */
  public final Account getAccountBPartner(String cBPartnerId, AcctSchema as, boolean isReceipt,
      String dpStatus, ConnectionProvider conn) {
    DocPaymentData[] data = null;
    try {
      if (log4j.isDebugEnabled()) {
        log4j.debug("DocInvoice - getAccountBPartner - DocumentType = " + DocumentType);
      }
      if (isReceipt) {
        data = DocPaymentData.selectBPartnerCustomerAcct(conn, cBPartnerId, as.getC_AcctSchema_ID(),
            dpStatus);
      } else {
        data = DocPaymentData.selectBPartnerVendorAcct(conn, cBPartnerId, as.getC_AcctSchema_ID(),
            dpStatus);
      }
    } catch (ServletException e) {
      log4j.warn(e);
    }
    // Get Acct
    String Account_ID = "";
    if (data != null && data.length != 0) {
      Account_ID = data[0].accountId;
    } else {
      return null;
    }
    // No account
    if (Account_ID.equals("")) {
      log4j.warn("DocInvoice - getAccountBPartner - NO account BPartner=" + cBPartnerId
          + ", Record=" + Record_ID + ", status " + dpStatus);
      return null;
    }
    // Return Account
    Account acct = null;
    try {
      acct = Account.getAccount(conn, Account_ID);
    } catch (ServletException e) {
      log4j.warn(e);
    }
    return acct;
  } // getAccount

  /**
   * @return the m_taxes
   */
  public DocTax[] getM_taxes() {
    return m_taxes;
  }

  /**
   * @param m_taxes
   *          the m_taxes to set
   */
  public void setM_taxes(DocTax[] m_taxes) {
    this.m_taxes = m_taxes;
  }

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

  public DocLine_FinPaymentSchedule[] getM_payments() {
    return m_payments;
  }

  public void setM_payments(DocLine_FinPaymentSchedule[] m_payments) {
    this.m_payments = m_payments;
  }

  public DocLine[] getP_lines_taxes() {
    return p_lines_taxes;
  }

  public void setP_lines_taxes(DocLine[] p_lines_taxes) {
    this.p_lines_taxes = p_lines_taxes;
  }

  public boolean isCashVAT() {
    return isCashVAT;
  }

  public void setCashVAT(boolean isCashVAT) {
    this.isCashVAT = isCashVAT;
  }

  public String getPrepaymentamt() {
    return prepaymentamt;
  }

  public void setPrepaymentamt(String prepaymentamt) {
    this.prepaymentamt = prepaymentamt;
  }

  /**
   * @return the serialVersionUID
   */
  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  /**
   * Get Document Confirmation
   * 
   */
  @Override
  public boolean getDocumentConfirmation(ConnectionProvider conn, String strRecordId) {
    DocInvoiceData[] data = null;
    FieldProvider dataFP[] = getObjectFieldProvider();
    if (ZERO.compareTo(new BigDecimal(dataFP[0].getField("GrandTotal"))) == 0) {
      Invoice invoice = OBDal.getInstance().get(Invoice.class, strRecordId);
      boolean zero = true;
      for (InvoiceLine invoiceline : invoice.getInvoiceLineList()) {
        if (ZERO.compareTo(invoiceline.getLineNetAmount()) != 0) {
          zero = false;
        }
      }
      for (InvoiceTax invoiceTax : invoice.getInvoiceTaxList()) {
        if (ZERO.compareTo(invoiceTax.getTaxAmount()) != 0) {
          zero = false;
        }
      }
      if (zero) {
        strMessage = "@TotalGrossIsZero@";
        setStatus(STATUS_DocumentDisabled);
        return false;
      }
    }
    try {
      data = DocInvoiceData.selectRegistro(conn, AD_Client_ID, strRecordId);
      AcctSchema[] m_acctSchemas = reloadLocalAcctSchemaArray(data[0].adOrgId);

      AcctSchema acct = null;
      for (int i = 0; i < m_acctSchemas.length; i++) {
        acct = m_acctSchemas[i];
        data = DocInvoiceData.selectFinInvCount(conn, strRecordId, acct.m_C_AcctSchema_ID);
        int countFinInv = Integer.parseInt(data[0].fininvcount);
        int countGLItemAcct = Integer.parseInt(data[0].finacctcount);
        // For any GL Item used in financial invoice lines debit/credit accounts must be defined
        if (countFinInv != 0 && (countFinInv != countGLItemAcct)) {
          log4jDocInvoice.debug("DocInvoice - getDocumentConfirmation - GL Item used in financial "
              + "invoice lines debit/credit accounts must be defined.");
          setStatus(STATUS_InvalidAccount);
          return false;
        }
      }
    } catch (Exception e) {
      log4jDocInvoice.error("Exception in getDocumentConfirmation method.", e);
    }

    return true;
  }

  private AcctSchema[] reloadLocalAcctSchemaArray(String adOrgId) throws ServletException {
    AcctSchema acct = null;
    ArrayList<Object> new_as = new ArrayList<Object>();
    // We reload again all the acct schemas of the client
    AcctSchema[] m_aslocal = AcctSchema.getAcctSchemaArray(connectionProvider, AD_Client_ID,
        adOrgId);
    // Filter the right acct schemas for the organization
    for (int i = 0; i < m_aslocal.length; i++) {
      acct = m_aslocal[i];
      if (AcctSchemaData.selectAcctSchemaTable(connectionProvider, acct.m_C_AcctSchema_ID,
          AD_Table_ID)) {
        new_as.add(new AcctSchema(connectionProvider, acct.m_C_AcctSchema_ID));
      }
    }
    AcctSchema[] retValue = new AcctSchema[new_as.size()];
    new_as.toArray(retValue);
    m_aslocal = retValue;
    return m_aslocal;
  }

  @Override
  public String getServletInfo() {
    return "Servlet for the accounting";
  } // end of getServletInfo() method

  public void computeTaxUndeductableLine(ConnectionProvider conn, AcctSchema as, Fact fact,
      DocLine docLine, String Fact_Acct_Group_ID, String taxId, String strTaxAmount) {
    int invoiceLineTaxCount = 0;
    int totalInvoiceLineTax = getTaxLineCount(conn, taxId);
    BigDecimal cumulativeTaxLineAmount = new BigDecimal(0);
    BigDecimal taxAmount = new BigDecimal(strTaxAmount.equals("") ? "0.00" : strTaxAmount);
    DocInvoiceData[] data = null;
    try {

      // We can have some lines from product or some lines from general ledger
      data = DocInvoiceData.selectProductAcct(conn, as.getC_AcctSchema_ID(), taxId, Record_ID,
          null);
      cumulativeTaxLineAmount = createLineForTaxUndeductable(invoiceLineTaxCount,
          totalInvoiceLineTax, cumulativeTaxLineAmount, taxAmount, data, conn, fact, docLine,
          Fact_Acct_Group_ID);
      invoiceLineTaxCount = data.length;
      // check whether gl item is selected instead of product in invoice line
      data = DocInvoiceData.selectGLItemAcctForTaxLine(conn, as.getC_AcctSchema_ID(), taxId,
          Record_ID);
      createLineForTaxUndeductable(invoiceLineTaxCount, totalInvoiceLineTax,
          cumulativeTaxLineAmount, taxAmount, data, conn, fact, docLine, Fact_Acct_Group_ID);
    } catch (ServletException e) {
      log4jDocInvoice.error("Exception in computeTaxUndeductableLine method: " + e);
    }
  }

  private int getTaxLineCount(ConnectionProvider conn, String taxId) {
    DocInvoiceData[] data = null;
    try {
      data = DocInvoiceData.getTaxLineCount(conn, taxId, Record_ID);
    } catch (ServletException e) {
      log4jDocInvoice.error("Exception in getTaxLineCount method: " + e);
    }
    if (data.length > 0) {
      return Integer.parseInt(data[0].totallines);
    }
    return 0;
  }

  private BigDecimal createLineForTaxUndeductable(int invoiceLineTaxCount, int totalInvoiceLineTax,
      BigDecimal cumulativeTaxLineAmount, BigDecimal taxAmount, DocInvoiceData[] data,
      ConnectionProvider conn, Fact fact, DocLine docLine, String Fact_Acct_Group_ID) {
    int localInvoiceLineTaxCount = invoiceLineTaxCount;
    BigDecimal localCumulativeTaxLineAmount = cumulativeTaxLineAmount;
    for (int j = 0; j < data.length; j++) {
      localInvoiceLineTaxCount++;
      // We have to adjust the amount in last line of tax
      if (localInvoiceLineTaxCount == totalInvoiceLineTax) {
        data[j].taxamt = taxAmount.subtract(localCumulativeTaxLineAmount).toPlainString();
      }
      try {
        // currently applicable for API and APC
        if (this.DocumentType.equals(AcctServer.DOCTYPE_APInvoice)) {
          if (IsReversal.equals("Y")) {
            fact.createLine(docLine, Account.getAccount(conn, data[j].pExpenseAcct),
                this.C_Currency_ID, "", data[j].taxamt, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
                DocumentType, conn);

          } else {
            fact.createLine(docLine, Account.getAccount(conn, data[j].pExpenseAcct),
                this.C_Currency_ID, data[j].taxamt, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
                DocumentType, conn);
          }
        } else if (this.DocumentType.equals(AcctServer.DOCTYPE_APCredit)) {
          fact.createLine(docLine, Account.getAccount(conn, data[j].pExpenseAcct),
              this.C_Currency_ID, "", data[j].taxamt, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
              DocumentType, conn);
        }
        localCumulativeTaxLineAmount = localCumulativeTaxLineAmount
            .add(new BigDecimal(data[j].taxamt));
      } catch (ServletException e) {
        log4jDocInvoice.error("Exception in createLineForTaxUndeductable method: " + e);
      }

    }

    return localCumulativeTaxLineAmount;
  }
}
