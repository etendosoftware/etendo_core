/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
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
 *************************************************************************
 */
package org.openbravo.advpaymentmngt.ad_actionbutton;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.openbravo.advpaymentmngt.ProcessInvoiceHook;
import org.openbravo.advpaymentmngt.ProcessInvoiceUtil;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.advpaymentmngt.process.FIN_PaymentProcess;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.ad_actionButton.ActionButtonUtility;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.reference.PInstanceProcessData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.FieldProviderFactory;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.ReversedInvoice;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetailV;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.service.db.CallProcess;
import org.openbravo.xmlEngine.XmlDocument;

public class ProcessInvoice extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  private List<FIN_Payment> creditPayments = new ArrayList<>();
  private final AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
  private static final String PURCHASE_INVOICE_WINDOW_ID = "183";
  private static final String C_INVOICE_ID = "|C_Invoice_ID";
  private static final String INP_KEY = "inpKey";

  @Inject
  private WeldUtils weldUtils;

  @Inject
  @Any
  private Instance<ProcessInvoiceHook> hooks;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      final String strWindowId = vars.getGlobalVariable("inpwindowId", "ProcessInvoice|Window_ID",
          IsIDFilter.instance);
      final String strTabId = vars.getGlobalVariable("inpTabId", "ProcessInvoice|Tab_ID",
          IsIDFilter.instance);

      final String strC_Invoice_ID = vars.getGlobalVariable("inpcInvoiceId",
          strWindowId + C_INVOICE_ID, StringUtils.EMPTY, IsIDFilter.instance);

      final String strdocaction = vars.getStringParameter("inpdocaction");
      final String strProcessing = vars.getStringParameter("inpprocessing", "Y");
      final String strOrg = vars.getRequestGlobalVariable("inpadOrgId", "ProcessInvoice|Org_ID",
          IsIDFilter.instance);
      final String strClient = vars.getStringParameter("inpadClientId", IsIDFilter.instance);

      final String strdocstatus = vars.getRequiredStringParameter("inpdocstatus");
      final String stradTableId = "318";
      final int accesslevel = 1;

      if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(),
          strTabId))
          || !(Utility.isElementInList(
              Utility.getContext(this, vars, "#User_Client", strWindowId, accesslevel), strClient)
              && Utility.isElementInList(
                  Utility.getContext(this, vars, "#User_Org", strWindowId, accesslevel), strOrg))) {
        OBError myError = Utility.translateError(this, vars, vars.getLanguage(),
            Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
        vars.setMessage(strTabId, myError);
        printPageClosePopUp(response, vars);
      } else {
        printPageDocAction(response, vars, strC_Invoice_ID, strdocaction, strProcessing,
            strdocstatus, stradTableId, strWindowId);
      }
    } else if (vars.commandIn("SAVE_BUTTONDocAction111")) {
      final String strWindowId = vars.getGlobalVariable("inpwindowId", "ProcessInvoice|Window_ID",
          IsIDFilter.instance);
      final String strTabId = vars.getGlobalVariable("inpTabId", "ProcessInvoice|Tab_ID",
          IsIDFilter.instance);
      final String strC_Invoice_ID = vars.getGlobalVariable(INP_KEY, strWindowId + C_INVOICE_ID,
          StringUtils.EMPTY);
      final String strdocaction = vars.getStringParameter("inpdocaction");
      final String strVoidInvoiceDate = vars.getStringParameter("inpVoidedDocumentDate");
      final String strVoidInvoiceAcctDate = vars.getStringParameter("inpVoidedDocumentAcctDate");
      final String strSupplierReference = vars.getStringParameter("inpSupplierReference");
      final String strOrg = vars.getGlobalVariable("inpadOrgId", "ProcessInvoice|Org_ID",
          IsIDFilter.instance);

      OBError myMessage = null;
      try {

        Invoice invoice = dao.getObject(Invoice.class, strC_Invoice_ID);

        myMessage = weldUtils.getInstance(ProcessInvoiceUtil.class).process(strC_Invoice_ID, strdocaction, strVoidInvoiceDate, strVoidInvoiceAcctDate, strSupplierReference, vars, this);
        log4j.debug(myMessage.getMessage());
        vars.setMessage(strTabId, myMessage);

        OBContext.setAdminMode();
        try {
          if (!"CO".equals(strdocaction)) {
            String strWindowPath = Utility.getTabURL(strTabId, "R", true);
            if (StringUtils.isEmpty(strWindowPath)) {
              strWindowPath = strDefaultServlet;
            }
            printPageClosePopUp(response, vars, strWindowPath);
            return;
          }
        } finally {
          OBContext.restorePreviousMode();
        }

        if ("CO".equals(strdocaction)) {
          // Need to refresh the invoice again from the db
          invoice = dao.getObject(Invoice.class, strC_Invoice_ID);
          OBContext.setAdminMode(false);
          String invoiceDocCategory = StringUtils.EMPTY;
          try {
            invoiceDocCategory = invoice.getDocumentType().getDocumentCategory();

            /*
             * Print a grid popup in case of credit payment
             */
            // If the invoice grand total is ZERO or already has payments (due to
            // payment method automation) or the business partner does not have a default financial
            // account defined or invoice's payment method is not inside BP's financial
            // account or the business partner's currency is not equal to the invoice's currency do
            // not cancel credit
            if (BigDecimal.ZERO.compareTo(invoice.getGrandTotalAmount()) != 0
                && isPaymentMethodConfigured(invoice) && !isInvoiceWithPayments(invoice)
                && (AcctServer.DOCTYPE_ARInvoice.equals(invoiceDocCategory)
                    || AcctServer.DOCTYPE_APInvoice.equals(invoiceDocCategory))
                && (invoice.getBusinessPartner().getCurrency() != null
                    && StringUtils.equals(invoice.getCurrency().getId(),
                        invoice.getBusinessPartner().getCurrency().getId()))) {
              creditPayments = dao.getCustomerPaymentsWithCredit(invoice.getOrganization(),
                  invoice.getBusinessPartner(), invoice.isSalesTransaction(),
                  invoice.getCurrency());
              if (creditPayments != null && !creditPayments.isEmpty()) {
                printPageCreditPaymentGrid(response, vars, strC_Invoice_ID, strWindowId, strTabId,
                    invoice.getInvoiceDate(), strOrg);
              }
            }
          } finally {
            OBContext.restorePreviousMode();
          }

          executePayments(response, vars, strWindowId, strTabId, strC_Invoice_ID, strOrg);
        }

      } catch (ServletException ex) {
        myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
        if (!myMessage.isConnectionAvailable()) {
          bdErrorConnection(response);
          return;
        } else {
          vars.setMessage(strTabId, myMessage);
        }
      }

    } else if (vars.commandIn("GRIDLIST")) {
      final String strWindowId = vars.getGlobalVariable("inpwindowId", "ProcessInvoice|Window_ID",
          IsIDFilter.instance);
      final String strC_Invoice_ID = vars.getGlobalVariable(INP_KEY, strWindowId + C_INVOICE_ID,
          StringUtils.EMPTY, IsIDFilter.instance);

      printGrid(response, vars, strC_Invoice_ID);
    } else if (vars.commandIn("USECREDITPAYMENTS") || vars.commandIn("CANCEL_USECREDITPAYMENTS")) {
      final String strWindowId = vars.getGlobalVariable("inpwindowId", "ProcessInvoice|Window_ID",
          IsIDFilter.instance);
      final String strTabId = vars.getGlobalVariable("inpTabId", "ProcessInvoice|Tab_ID",
          IsIDFilter.instance);
      final String strC_Invoice_ID = vars.getGlobalVariable(INP_KEY, strWindowId + C_INVOICE_ID,
          StringUtils.EMPTY);
      final String strPaymentDate = vars.getRequiredStringParameter("inpPaymentDate");
      final String strOrg = vars.getGlobalVariable("inpadOrgId", "ProcessInvoice|Org_ID",
          IsIDFilter.instance);

      final String strCreditPaymentIds;
      if (vars.commandIn("CANCEL_USECREDITPAYMENTS")) {
        strCreditPaymentIds = null;
      } else {
        strCreditPaymentIds = vars.getInParameter("inpCreditPaymentId", IsIDFilter.instance);
      }

      /*
       * Use credit logic
       */
      if (strCreditPaymentIds != null && !strCreditPaymentIds.isEmpty()) {
        List<FIN_Payment> selectedCreditPayment = FIN_Utility.getOBObjectList(FIN_Payment.class,
            strCreditPaymentIds);
        HashMap<String, BigDecimal> selectedCreditPaymentAmounts = FIN_AddPayment
            .getSelectedBaseOBObjectAmount(vars, selectedCreditPayment, "inpPaymentAmount");
        try {
          OBContext.setAdminMode(true);
          final Invoice invoice = OBDal.getInstance().get(Invoice.class, strC_Invoice_ID);

          final StringBuilder creditPaymentsIdentifiers = new StringBuilder();
          BigDecimal totalUsedCreditAmt = BigDecimal.ZERO;
          for (final FIN_Payment creditPayment : selectedCreditPayment) {
            final BigDecimal usedCreditAmt = selectedCreditPaymentAmounts
                .get(creditPayment.getId());
            // Set Used Credit = Amount + Previous used credit introduced by the user
            creditPayment.setUsedCredit(usedCreditAmt.add(creditPayment.getUsedCredit()));
            final StringBuilder description = new StringBuilder();
            if (creditPayment.getDescription() != null
                && !creditPayment.getDescription().equals(StringUtils.EMPTY)) {
              description.append(creditPayment.getDescription()).append("\n");
            }
            description.append(String.format(
                Utility.messageBD(this, "APRM_CreditUsedinInvoice", vars.getLanguage()),
                invoice.getDocumentNo()));
            String truncateDescription = (description.length() > 255)
                ? description.substring(0, 251).concat("...").toString()
                : description.toString();
            creditPayment.setDescription(truncateDescription);
            totalUsedCreditAmt = totalUsedCreditAmt.add(usedCreditAmt);
            creditPaymentsIdentifiers.append(creditPayment.getDocumentNo());
            creditPaymentsIdentifiers.append(", ");
          }
          creditPaymentsIdentifiers.delete(creditPaymentsIdentifiers.length() - 2,
              creditPaymentsIdentifiers.length());
          creditPaymentsIdentifiers.append("\n");

          final List<FIN_PaymentScheduleDetail> paymentScheduleDetails = new ArrayList<FIN_PaymentScheduleDetail>();
          final HashMap<String, BigDecimal> paymentScheduleDetailsAmounts = new HashMap<String, BigDecimal>();
          BigDecimal allocatedAmt = BigDecimal.ZERO;
          for (final FIN_PaymentScheduleDetail paymentScheduleDetail : dao
              .getInvoicePendingScheduledPaymentDetails(invoice)) {
            if (totalUsedCreditAmt.compareTo(allocatedAmt) > 0) {
              final BigDecimal pendingToAllocate = totalUsedCreditAmt.subtract(allocatedAmt);
              paymentScheduleDetails.add(paymentScheduleDetail);

              final BigDecimal psdAmt = paymentScheduleDetail.getAmount();
              if (psdAmt.compareTo(pendingToAllocate) <= 0) {
                paymentScheduleDetailsAmounts.put(paymentScheduleDetail.getId(), psdAmt);
                allocatedAmt = allocatedAmt.add(psdAmt);
              } else {
                paymentScheduleDetailsAmounts.put(paymentScheduleDetail.getId(), pendingToAllocate);
                allocatedAmt = allocatedAmt.add(pendingToAllocate);
              }
            }
          }

          // Create new Payment
          final boolean isSalesTransaction = invoice.isSalesTransaction();
          final DocumentType docType = FIN_Utility.getDocumentType(invoice.getOrganization(),
              isSalesTransaction ? AcctServer.DOCTYPE_ARReceipt : AcctServer.DOCTYPE_APPayment);
          final String strPaymentDocumentNo = FIN_Utility.getDocumentNo(docType,
              docType.getTable() != null ? docType.getTable().getDBTableName() : StringUtils.EMPTY);
          final FIN_FinancialAccount bpFinAccount = isSalesTransaction
              ? invoice.getBusinessPartner().getAccount()
              : invoice.getBusinessPartner().getPOFinancialAccount();
          // Calculate Conversion Rate
          final ConversionRate conversionRate = StringUtils.equals(invoice.getCurrency().getId(),
              bpFinAccount.getCurrency().getId())
                  ? null
                  : FinancialUtils.getConversionRate(FIN_Utility.getDate(strPaymentDate),
                      invoice.getCurrency(), bpFinAccount.getCurrency(), invoice.getOrganization(),
                      invoice.getClient());
          final FIN_Payment newPayment = FIN_AddPayment.savePayment(null, isSalesTransaction,
              docType, strPaymentDocumentNo, invoice.getBusinessPartner(),
              invoice.getPaymentMethod(), bpFinAccount, "0", FIN_Utility.getDate(strPaymentDate),
              invoice.getOrganization(), invoice.getDocumentNo(), paymentScheduleDetails,
              paymentScheduleDetailsAmounts, false, false, invoice.getCurrency(),
              conversionRate != null ? conversionRate.getMultipleRateBy() : null, null);
          newPayment.setAmount(BigDecimal.ZERO);
          newPayment.setGeneratedCredit(BigDecimal.ZERO);
          newPayment.setUsedCredit(totalUsedCreditAmt);

          // Link new Payment with the credit payments used
          for (final FIN_Payment creditPayment : selectedCreditPayment) {
            final BigDecimal usedCreditAmt = selectedCreditPaymentAmounts
                .get(creditPayment.getId());
            FIN_PaymentProcess.linkCreditPayment(newPayment, usedCreditAmt, creditPayment);
          }

          // Process the new payment
          OBError message = FIN_AddPayment.processPayment(vars, this, "P", newPayment);
          if ("Success".equals(message.getType())) {
            // Update Invoice's description
            final StringBuilder invDesc = new StringBuilder();
            if (invoice.getDescription() != null) {
              invDesc.append(invoice.getDescription());
              invDesc.append("\n");
            }
            invDesc.append(String.format(
                Utility.messageBD(this, "APRM_InvoiceDescUsedCredit", vars.getLanguage()),
                creditPaymentsIdentifiers.toString()));
            invoice.setDescription(invDesc.toString());
          } else {
            message
                .setMessage(OBMessageUtils.messageBD("PaymentError") + " " + message.getMessage());
            vars.setMessage(strTabId, message);
          }

        } catch (final Exception e) {
          log4j.error("Exception while canceling the credit in the invoice: " + strC_Invoice_ID, e);
        } finally {
          OBContext.restorePreviousMode();
        }
      }
      executePayments(response, vars, strWindowId, strTabId, strC_Invoice_ID, strOrg);
    }
  }

  private void executePayments(HttpServletResponse response, VariablesSecureApp vars,
      final String strWindowId, final String strTabId, final String strC_Invoice_ID,
      final String strOrg) throws IOException, ServletException {
    OBError myMessage = new OBError();

    List<FIN_Payment> payments = null;
    try {
      OBContext.setAdminMode(true);
      payments = dao.getPendingExecutionPayments(strC_Invoice_ID);
    } finally {
      OBContext.restorePreviousMode();
    }
    if (payments != null && !payments.isEmpty()) {
      vars.setSessionValue("ExecutePayments|Window_ID", strWindowId);
      vars.setSessionValue("ExecutePayments|Tab_ID", strTabId);
      vars.setSessionValue("ExecutePayments|Org_ID", strOrg);
      vars.setSessionValue("ExecutePayments|payments", Utility.getInStrList(payments));

      vars.setMessage("ExecutePayments|message", myMessage);
      response.sendRedirect(
          strDireccion + "/org.openbravo.advpaymentmngt.ad_actionbutton/ExecutePayments.html");
    } else {
      String strWindowPath = Utility.getTabURL(strTabId, "R", true);
      if (StringUtils.isEmpty(strWindowPath)) {
        strWindowPath = strDefaultServlet;
      }
      printPageClosePopUp(response, vars, strWindowPath);
    }

    vars.removeSessionValue("ProcessInvoice|Window_ID");
    vars.removeSessionValue("ProcessInvoice|Tab_ID");
    vars.removeSessionValue("ProcessInvoice|Org_ID");
  }

  void printPageDocAction(HttpServletResponse response, VariablesSecureApp vars,
      String strCInvoiceID, String strdocaction, String strProcessing, String strdocstatus,
      String stradTableId, String strWindowId) throws IOException, ServletException {
    log4j.debug("Output: Button process 111");
    String[] discard = { "newDiscard" };
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/DocAction", discard)
        .createXmlDocument();
    xmlDocument.setParameter("key", strCInvoiceID);
    xmlDocument.setParameter("processing", strProcessing);
    xmlDocument.setParameter("form", "ProcessInvoice.html");
    xmlDocument.setParameter("window", strWindowId);
    xmlDocument.setParameter("css", vars.getTheme());
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("dateDisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("processId", "111");
    xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
    xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));

    OBError myMessage = vars.getMessage("111");
    vars.removeMessage("111");
    if (myMessage != null) {
      xmlDocument.setParameter("messageType", myMessage.getType());
      xmlDocument.setParameter("messageTitle", myMessage.getTitle());
      xmlDocument.setParameter("messageMessage", myMessage.getMessage());
    }

    xmlDocument.setParameter("docstatus", strdocstatus);
    if (strWindowId.equals(PURCHASE_INVOICE_WINDOW_ID)) {
      // VOID action: Reverse sales/purchase invoice by default takes today as document date and
      // accounting date.
      xmlDocument.setParameter("voidedDocumentDate", DateTimeData.today(this));
      xmlDocument.setParameter("voidedDocumentAcctDate", DateTimeData.today(this));
      Invoice invoice = (Invoice) OBDal.getInstance().getProxy(Invoice.ENTITY_NAME, strCInvoiceID);
      xmlDocument.setParameter("documentDate", OBDateUtils.formatDate(invoice.getInvoiceDate()));
      xmlDocument.setParameter("documentAcctDate",
          OBDateUtils.formatDate(invoice.getAccountingDate()));
    }
    xmlDocument.setParameter("adTableId", stradTableId);
    xmlDocument.setParameter("processId", "111");
    xmlDocument.setParameter("processDescription", "Process Invoice");
    xmlDocument.setParameter("docaction", (strdocaction.equals("--") ? "CL" : strdocaction));
    FieldProvider[] dataDocAction = ActionButtonUtility.docAction(this, vars, strdocaction, "135",
        strdocstatus, strProcessing, stradTableId);
    xmlDocument.setData("reportdocaction", "liststructure", dataDocAction);
    StringBuilder dact = new StringBuilder();
    if (dataDocAction != null) {
      dact.append("var arrDocAction = new Array(\n");
      for (int i = 0; i < dataDocAction.length; i++) {
        dact.append("new Array(\"" + dataDocAction[i].getField("id") + "\", \""
            + dataDocAction[i].getField("name") + "\", \""
            + dataDocAction[i].getField("description") + "\")\n");
        if (i < dataDocAction.length - 1) {
          dact.append(",\n");
        }
      }
      dact.append(");");
    } else {
      dact.append("var arrDocAction = null");
    }
    xmlDocument.setParameter("array", dact.toString());

    out.println(xmlDocument.print());
    out.close();

  }

  void printPageCreditPaymentGrid(HttpServletResponse response, VariablesSecureApp vars,
      String strCInvoiceID, String strWindowId, String strTabId, Date invoiceDate, String strOrg)
      throws IOException, ServletException {
    log4j.debug("Output: Credit Payment Grid popup");
    String[] discard = { StringUtils.EMPTY };
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/CreditPaymentGrid", discard)
        .createXmlDocument();
    xmlDocument.setParameter("css", vars.getTheme());
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("window", strWindowId);
    xmlDocument.setParameter("tab", strTabId);
    xmlDocument.setParameter("adOrgId", strOrg);

    xmlDocument.setParameter("messageType", "SUCCESS");
    xmlDocument.setParameter("messageTitle",
        Utility.messageBD(this, "InvoiceComplete", vars.getLanguage()));

    xmlDocument.setParameter("invoiceGrossAmt",
        dao.getObject(Invoice.class, strCInvoiceID).getGrandTotalAmount().toString());

    OBError myMessage = vars.getMessage("ProcessInvoice|CreditPaymentGrid");
    vars.removeMessage("ProcessInvoice|CreditPaymentGrid");
    if (myMessage != null) {
      xmlDocument.setParameter("messageType", myMessage.getType());
      xmlDocument.setParameter("messageTitle", myMessage.getTitle());
      xmlDocument.setParameter("messageMessage", myMessage.getMessage());
    }

    xmlDocument.setParameter("dateDisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("paymentDate",
        Utility.formatDate(invoiceDate, vars.getJavaDateFormat()));

    out.println(xmlDocument.print());
    out.close();

  }

  private void printGrid(HttpServletResponse response, VariablesSecureApp vars, String invoiceId)
      throws IOException, ServletException {
    log4j.debug("Output: Grid with credit payments");

    final Invoice invoice = dao.getObject(Invoice.class, invoiceId);

    String[] discard = {};
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/AddCreditPaymentGrid", discard)
        .createXmlDocument();

    xmlDocument.setData("structure", getCreditPayments(invoice));

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private FieldProvider[] getCreditPayments(Invoice invoice) {
    FieldProvider[] data = FieldProviderFactory.getFieldProviderArray(creditPayments);
    String dateFormat = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("dateFormat.java");
    SimpleDateFormat dateFormater = new SimpleDateFormat(dateFormat);

    BigDecimal pendingToPay = invoice.getGrandTotalAmount();
    try {
      OBContext.setAdminMode(true);
      for (int i = 0; i < data.length; i++) {
        FieldProviderFactory.setField(data[i], "finCreditPaymentId", creditPayments.get(i).getId());
        FieldProviderFactory.setField(data[i], "documentNo", creditPayments.get(i).getDocumentNo());
        FieldProviderFactory.setField(data[i], "paymentDescription",
            creditPayments.get(i).getDescription());
        if (creditPayments.get(i).getPaymentDate() != null) {
          FieldProviderFactory.setField(data[i], "documentDate",
              dateFormater.format(creditPayments.get(i).getPaymentDate()));
        }

        final BigDecimal outStandingAmt = creditPayments.get(i)
            .getGeneratedCredit()
            .subtract(creditPayments.get(i).getUsedCredit());
        FieldProviderFactory.setField(data[i], "outstandingAmount", outStandingAmt.toString());

        FieldProviderFactory.setField(data[i], "paymentAmount",
            pendingToPay.compareTo(outStandingAmt) > 0 ? outStandingAmt.toString()
                : (pendingToPay.compareTo(BigDecimal.ZERO) > 0 ? pendingToPay.toString() : StringUtils.EMPTY));
        pendingToPay = pendingToPay.subtract(outStandingAmt);

        FieldProviderFactory.setField(data[i], "finSelectedCreditPaymentId",
            StringUtils.EMPTY.equals(data[i].getField("paymentAmount")) ? StringUtils.EMPTY : creditPayments.get(i).getId());
        FieldProviderFactory.setField(data[i], "rownum", String.valueOf(i));
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    return data;
  }

  private boolean isInvoiceWithPayments(Invoice invoice) {
    // Crear OBCriteria para FIN_PaymentSchedule
    OBCriteria<FIN_PaymentSchedule> psCriteria = OBDal.getInstance().createCriteria(FIN_PaymentSchedule.class);
    psCriteria.add(Restrictions.eq(FIN_PaymentSchedule.PROPERTY_INVOICE, invoice));
    
    for (FIN_PaymentSchedule ps : psCriteria.list()) {
      // Crear OBCriteria para FIN_PaymentDetailV
      OBCriteria<FIN_PaymentDetailV> pdvCriteria = OBDal.getInstance().createCriteria(FIN_PaymentDetailV.class);
      pdvCriteria.add(Restrictions.eq(FIN_PaymentDetailV.PROPERTY_PAYMENTPLANINVOICE, ps));
      
      for (FIN_PaymentDetailV pdv : pdvCriteria.list()) {
        if (pdv.getPayment() != null && !"RPVOID".equals(pdv.getPayment().getStatus())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Checks if the invoice business partner has defined a default financial account and if the
   * payment method selected in the invoice belongs to the default financial account.
   * 
   * @param invoice
   *          Invoice.
   * @return True if the invoice business partner has defined a default financial account and the
   *         payment method selected in the invoice belongs to the default financial account. False
   *         in other cases.
   */
  private boolean isPaymentMethodConfigured(Invoice invoice) {
    final FIN_FinancialAccount bpFinAccount = invoice.isSalesTransaction()
        ? invoice.getBusinessPartner().getAccount()
        : invoice.getBusinessPartner().getPOFinancialAccount();
    if (bpFinAccount != null) {
      for (final FinAccPaymentMethod bpFinAccPaymentMethod : bpFinAccount
          .getFinancialMgmtFinAccPaymentMethodList()) {
        if (bpFinAccPaymentMethod.getPaymentMethod().equals(invoice.getPaymentMethod())) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public String getServletInfo() {
    return "Servlet to Process Invoice";
  }
}
