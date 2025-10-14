package org.openbravo.advpaymentmngt;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_actionButton.ActionButtonUtility;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.reference.PInstanceProcessData;
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
import org.openbravo.model.financialmgmt.payment.*;
import org.openbravo.service.db.CallProcess;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;

/**
 * Utility with methods related to the processing of Invoices of any type.
 */
public class ProcessInvoiceUtil {
    private static final Logger log4j = LogManager.getLogger();
    private final AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
    private static final String VOID_DOCUMENT_ACTION = "RC";
    private static final String ERROR = "Error";

    @Inject
    @Any
    private Instance<ProcessInvoiceHook> hooks;

    /**
     * Processes an invoice.
     * This method uses the {@link ProcessInvoiceHook} type hooks to execute pre and post hooks.
     * @param strC_Invoice_ID Invoice ID
     * @param strdocaction Document Action
     * @param strVoidInvoiceDate Void date where applicable
     * @param strVoidInvoiceAcctDate Void accounting date where applicable
     * @param strSupplierReference Supplier reference associated with the invoice, used for tracking purposes.
     * @param vars {@link VariablesSecureApp} Used to obtain current language and by Payment Processes. Use {@link org.openbravo.client.kernel.RequestContext#getVariablesSecureApp()} outside of servlets.
     * @param conn {@link ConnectionProvider} Used to connect to the database. Use 'this' when in servlets.
     * @return an {@link OBError} with the message of the resulting operation. It can be a success.
     */
    public OBError process(String strC_Invoice_ID, String strdocaction, String strVoidInvoiceDate, String strVoidInvoiceAcctDate, String strSupplierReference, VariablesSecureApp vars, ConnectionProvider conn) {
        OBError myMessage = null;
        try {

            Invoice invoice = dao.getObject(Invoice.class, strC_Invoice_ID);
            invoice.setDocumentAction(strdocaction);
            OBDal.getInstance().save(invoice);
            OBDal.getInstance().flush();

            OBError msg = null;
            for (ProcessInvoiceHook hook : hooks) {
                msg = hook.preProcess(invoice, strdocaction);
                if (msg != null && StringUtils.equals(ERROR, msg.getType())) {
                    return msg;
                }
            }
            // check BP currency
            if ("CO".equals(strdocaction)) {
                // check BP currency
                if (invoice.getBusinessPartner().getCurrency() == null) {
                    String errorMSG = Utility.messageBD(conn, "InitBPCurrencyLnk", vars.getLanguage(),
                            false);
                    msg = new OBError();
                    msg.setType(ERROR);
                    msg.setTitle(Utility.messageBD(conn, ERROR, vars.getLanguage()));
                    msg.setMessage(String.format(errorMSG, invoice.getBusinessPartner().getId(),
                            invoice.getBusinessPartner().getName()));

                    return msg;
                }
            }

            OBContext.setAdminMode(true);
            Process process = null;
            try {
                process = dao.getObject(Process.class, "111");
            } finally {
                OBContext.restorePreviousMode();
            }

            Date voidDate = null;
            Date voidAcctDate = null;
            Map<String, String> parameters = new HashMap<>();
            if (!strVoidInvoiceDate.isEmpty() && !strVoidInvoiceAcctDate.isEmpty()) {
                try {
                    voidDate = OBDateUtils.getDate(strVoidInvoiceDate);
                    voidAcctDate = OBDateUtils.getDate(strVoidInvoiceAcctDate);
                } catch (ParseException pe) {
                    voidDate = new Date();
                    voidAcctDate = new Date();
                    log4j.error("Not possible to parse the following date: " + strVoidInvoiceDate, pe);
                    log4j.error("Not possible to parse the following date: " + strVoidInvoiceAcctDate, pe);
                }
                parameters.put("voidedDocumentDate", OBDateUtils.formatDate(voidDate, "yyyy-MM-dd"));
                parameters.put("voidedDocumentAcctDate",
                        OBDateUtils.formatDate(voidAcctDate, "yyyy-MM-dd"));
            }

            if (StringUtils.equals(VOID_DOCUMENT_ACTION, strdocaction) && !invoice.isSalesTransaction()) {
                parameters.put("supplierReference", strSupplierReference);
            }

            // In case of void a non paid invoice, create a dummy payment related to it with zero amount
            FIN_Payment dummyPayment = null;
            if (StringUtils.equals(VOID_DOCUMENT_ACTION, strdocaction) && !invoice.isPaymentComplete()
                    && invoice.getTotalPaid().compareTo(BigDecimal.ZERO) == 0) {
                try {
                    OBContext.setAdminMode(true);
                    final boolean isSOTrx = invoice.isSalesTransaction();
                    final DocumentType docType = FIN_Utility.getDocumentType(invoice.getOrganization(),
                            isSOTrx ? AcctServer.DOCTYPE_ARReceipt : AcctServer.DOCTYPE_APPayment);
                    final String strPaymentDocumentNo = FIN_Utility.getDocumentNo(docType,
                            docType.getTable() != null ? docType.getTable().getDBTableName() : StringUtils.EMPTY);

                    // Get default Financial Account as it is done in Add Payment
                    FIN_FinancialAccount bpFinAccount = null;
                    if (isSOTrx && invoice.getBusinessPartner().getAccount() != null
                            && FIN_Utility.getFinancialAccountPaymentMethod(invoice.getPaymentMethod().getId(),
                            invoice.getBusinessPartner().getAccount().getId(), isSOTrx,
                            invoice.getCurrency().getId(), invoice.getOrganization().getId()) != null) {
                        bpFinAccount = invoice.getBusinessPartner().getAccount();
                    } else if (!isSOTrx && invoice.getBusinessPartner().getPOFinancialAccount() != null
                            && FIN_Utility.getFinancialAccountPaymentMethod(invoice.getPaymentMethod().getId(),
                            invoice.getBusinessPartner().getPOFinancialAccount().getId(), isSOTrx,
                            invoice.getCurrency().getId(), invoice.getOrganization().getId()) != null) {
                        bpFinAccount = invoice.getBusinessPartner().getPOFinancialAccount();
                    } else {
                        FinAccPaymentMethod fpm = FIN_Utility.getFinancialAccountPaymentMethod(
                                invoice.getPaymentMethod().getId(), null, isSOTrx, invoice.getCurrency().getId(),
                                invoice.getOrganization().getId());
                        if (fpm != null) {
                            bpFinAccount = fpm.getAccount();
                        }
                    }

                    // If no Financial Account exists, show an Error
                    if (bpFinAccount == null) {
                        msg = new OBError();
                        msg.setType(ERROR);
                        msg.setTitle(Utility.messageBD(conn, ERROR, vars.getLanguage()));
                        msg.setMessage(OBMessageUtils.messageBD("APRM_NoFinancialAccountAvailable"));
                        return msg;
                    }

                    // If Invoice has a awaiting execution payment related, show an Error
                    StringBuilder fpHQLQuery = new StringBuilder(" as fp");
                    fpHQLQuery.append(" join fp.fINPaymentDetailList fpd");
                    fpHQLQuery.append(" join fpd.fINPaymentScheduleDetailList fpsd");
                    fpHQLQuery.append(" join fpsd.invoicePaymentSchedule fps");
                    fpHQLQuery.append(" where fps.invoice.id = :invoiceId");
                    fpHQLQuery.append(" and fp.status in ('RPAE', 'RPAP')");
                    OBQuery<FIN_Payment> paymentQuery = OBDal.getInstance()
                            .createQuery(FIN_Payment.class, fpHQLQuery.toString());
                    paymentQuery.setNamedParameter("invoiceId", invoice.getId());
                    paymentQuery.setMaxResult(1);
                    if (paymentQuery.uniqueResult() != null) {
                        msg = new OBError();
                        msg.setType(ERROR);
                        msg.setTitle(Utility.messageBD(conn, ERROR, vars.getLanguage()));
                        msg.setMessage(
                                OBMessageUtils.messageBD("APRM_InvoiceAwaitingExcutionPaymentRelated"));
                        return msg;
                    }

                    // Reversed invoice's date: voidDate in Purchase Invoice, new Date() in Sales Invoice
                    Date reversedDate = voidDate != null ? voidDate : new Date();

                    // Calculate Conversion Rate
                    BigDecimal rate = null;
                    if (!StringUtils.equals(invoice.getCurrency().getId(),
                            bpFinAccount.getCurrency().getId())) {
                        final ConversionRate conversionRate = FinancialUtils.getConversionRate(reversedDate,
                                invoice.getCurrency(), bpFinAccount.getCurrency(), invoice.getOrganization(),
                                invoice.getClient());
                        if (conversionRate != null) {
                            rate = conversionRate.getMultipleRateBy();
                        }
                    }

                    // Create dummy payment
                    dummyPayment = dao.getNewPayment(isSOTrx, invoice.getOrganization(), docType,
                            strPaymentDocumentNo, invoice.getBusinessPartner(), invoice.getPaymentMethod(),
                            bpFinAccount, "0", reversedDate, invoice.getDocumentNo(), invoice.getCurrency(),
                            rate, null);
                    OBDal.getInstance().save(dummyPayment);

                    List<FIN_PaymentDetail> paymentDetails = new ArrayList<FIN_PaymentDetail>();
                    List<FIN_PaymentScheduleDetail> paymentScheduleDetails = dao
                            .getInvoicePendingScheduledPaymentDetails(invoice);
                    for (FIN_PaymentScheduleDetail psd : paymentScheduleDetails) {
                        FIN_PaymentDetail pd = OBProvider.getInstance().get(FIN_PaymentDetail.class);
                        pd.setOrganization(psd.getOrganization());
                        pd.setFinPayment(dummyPayment);
                        pd.setAmount(psd.getAmount());
                        pd.setRefund(false);
                        OBDal.getInstance().save(pd);

                        paymentDetails.add(pd);
                        psd.setPaymentDetails(pd);
                        pd.getFINPaymentScheduleDetailList().add(psd);
                        OBDal.getInstance().save(psd);
                    }
                    dummyPayment.setFINPaymentDetailList(paymentDetails);

                    // Copy exchange rate from invoice
                    for (ConversionRateDoc conversionRateDoc : invoice.getCurrencyConversionRateDocList()) {
                        ConversionRateDoc newConversionRateDoc = OBProvider.getInstance()
                                .get(ConversionRateDoc.class);
                        newConversionRateDoc.setClient(conversionRateDoc.getClient());
                        newConversionRateDoc.setOrganization(conversionRateDoc.getOrganization());
                        newConversionRateDoc.setCurrency(conversionRateDoc.getCurrency());
                        newConversionRateDoc.setToCurrency(conversionRateDoc.getToCurrency());
                        newConversionRateDoc.setRate(conversionRateDoc.getRate());
                        newConversionRateDoc.setForeignAmount(BigDecimal.ZERO);
                        newConversionRateDoc.setPayment(dummyPayment);
                        dummyPayment.getCurrencyConversionRateDocList().add(newConversionRateDoc);
                        OBDal.getInstance().save(newConversionRateDoc);
                    }

                    OBDal.getInstance().save(dummyPayment);
                } catch (final Exception e) {
                    log4j.error(
                            "Exception while creating dummy payment for the invoice: " + strC_Invoice_ID, e);
                } finally {
                    OBContext.restorePreviousMode();
                }
            }

            boolean voidingPrepaidInvoice = StringUtils.equals(VOID_DOCUMENT_ACTION, strdocaction)
                    && invoice.getPrepaymentamt().compareTo(BigDecimal.ZERO) != 0;

            final ProcessInstance pinstance = CallProcess.getInstance()
                    .call(process, strC_Invoice_ID, parameters);

            OBDal.getInstance().getSession().refresh(invoice);
            invoice.setAPRMProcessinvoice(invoice.getDocumentAction());

            if (StringUtils.equals(VOID_DOCUMENT_ACTION, strdocaction) && pinstance.getResult() != 0L) {
                try {
                    OBContext.setAdminMode(true);

                    // Get reversed payment
                    OBCriteria<ReversedInvoice> revInvoiceCriteria = OBDal.getInstance()
                            .createCriteria(ReversedInvoice.class);
                    revInvoiceCriteria
                            .add(Restrictions.eq(ReversedInvoice.PROPERTY_REVERSEDINVOICE, invoice));
                    revInvoiceCriteria.setMaxResults(1);
                    ReversedInvoice revInvoice = (ReversedInvoice) revInvoiceCriteria.uniqueResult();

                    boolean processPayment = false;

                    if (voidingPrepaidInvoice) {
                        processPayment = true;

                        StringBuilder orderPaymentHQLQuery = new StringBuilder(" as fp");
                        orderPaymentHQLQuery.append(" join fp.fINPaymentDetailList fpd");
                        orderPaymentHQLQuery.append(" join fpd.fINPaymentScheduleDetailList fpsd");
                        orderPaymentHQLQuery.append(" join fpsd.invoicePaymentSchedule fps");
                        orderPaymentHQLQuery.append(" where fps.invoice.id = :invoiceId");
                        OBQuery<FIN_Payment> paymentQuery = OBDal.getInstance()
                                .createQuery(FIN_Payment.class, orderPaymentHQLQuery.toString());
                        paymentQuery.setNamedParameter("invoiceId", invoice.getId());
                        paymentQuery.setMaxResult(1);
                        FIN_Payment orderPayment = (FIN_Payment) paymentQuery.uniqueResult();

                        final DocumentType docType = FIN_Utility.getDocumentType(invoice.getOrganization(),
                                orderPayment.isReceipt() ? AcctServer.DOCTYPE_ARReceipt
                                        : AcctServer.DOCTYPE_APPayment);
                        final String strPaymentDocumentNo = FIN_Utility.getDocumentNo(docType,
                                docType.getTable() != null ? docType.getTable().getDBTableName() : StringUtils.EMPTY);

                        // Creating a dummy payment
                        dummyPayment = dao.getNewPayment(orderPayment.isReceipt(), invoice.getOrganization(),
                                orderPayment.getDocumentType(), strPaymentDocumentNo,
                                invoice.getBusinessPartner(), invoice.getPaymentMethod(),
                                orderPayment.getAccount(), "0", voidDate != null ? voidDate : new Date(),
                                invoice.getDocumentNo(), invoice.getCurrency(),
                                orderPayment.getFinancialTransactionConvertRate(), null);
                        OBDal.getInstance().save(dummyPayment);

                        invoice.setOutstandingAmount(BigDecimal.ZERO);

                        StringBuilder psdHQLQuery = new StringBuilder(" as fpsd");
                        psdHQLQuery.append(" join fpsd.invoicePaymentSchedule fps");
                        psdHQLQuery.append(" where fps.invoice.id = :invoiceId");
                        psdHQLQuery.append(" or fps.invoice.id = :revInvoiceId");
                        OBQuery<FIN_PaymentScheduleDetail> psdQuery = OBDal.getInstance()
                                .createQuery(FIN_PaymentScheduleDetail.class, psdHQLQuery.toString());
                        psdQuery.setNamedParameter("invoiceId", invoice.getId());
                        psdQuery.setNamedParameter("revInvoiceId", revInvoice.getInvoice().getId());

                        // Updating dummy payment lines with invoice and reverse invoice
                        for (FIN_PaymentScheduleDetail fpsd : psdQuery.list()) {

                            // Invoice payment detail associated to the order
                            FIN_PaymentDetail invoiceFPDOrder = fpsd.getPaymentDetails();
                            FIN_PaymentSchedule orderPaymentSchedule = fpsd.getOrderPaymentSchedule();

                            // Create a payment detail
                            FIN_PaymentDetail pd = OBProvider.getInstance().get(FIN_PaymentDetail.class);
                            pd.setOrganization(dummyPayment.getOrganization());
                            pd.setFinPayment(dummyPayment);
                            pd.setAmount(fpsd.getAmount());
                            pd.setRefund(false);
                            OBDal.getInstance().save(pd);

                            // Remove the reference to the order payment schedule
                            fpsd.setOrderPaymentSchedule(null);

                            fpsd.setPaymentDetails(pd);

                            pd.getFINPaymentScheduleDetailList().add(fpsd);
                            OBDal.getInstance().save(fpsd);

                            dummyPayment.getFINPaymentDetailList().add(pd);

                            if (invoiceFPDOrder != null) {
                                StringBuilder orderPSDHQLQuery = new StringBuilder();
                                orderPSDHQLQuery.append(" as fpsd");
                                orderPSDHQLQuery.append(" join fpsd.paymentDetails fpd");
                                orderPSDHQLQuery.append(" where fpd.finPayment.id = :paymentId");
                                orderPSDHQLQuery.append(" and fpsd.id <> :invoicePSDId");
                                orderPSDHQLQuery.append(" and fpsd.invoicePaymentSchedule is null");
                                OBQuery<FIN_PaymentScheduleDetail> orderPSDQuery = OBDal.getInstance()
                                        .createQuery(FIN_PaymentScheduleDetail.class, orderPSDHQLQuery.toString());
                                orderPSDQuery.setNamedParameter("paymentId",
                                        invoiceFPDOrder.getFinPayment().getId());
                                orderPSDQuery.setNamedParameter("invoicePSDId", fpsd.getId());
                                orderPSDQuery.setMaxResult(1);
                                FIN_PaymentScheduleDetail orderPSD = orderPSDQuery.uniqueResult();

                                if (orderPSD == null) {
                                    // Order with no payment schedule detail, create a new one
                                    orderPSD = OBProvider.getInstance().get(FIN_PaymentScheduleDetail.class);
                                    orderPSD.setOrganization(fpsd.getOrganization());
                                    orderPSD.setAmount(fpsd.getAmount());
                                    orderPSD.setBusinessPartner(fpsd.getBusinessPartner());
                                    orderPSD.setPaymentDetails(invoiceFPDOrder);
                                    orderPSD.setOrderPaymentSchedule(orderPaymentSchedule);
                                } else {
                                    // Update order received amount
                                    orderPSD.setAmount(orderPSD.getAmount().add(fpsd.getAmount()));
                                }
                                OBDal.getInstance().save(orderPSD);

                                // Update invoice payment schedule
                                FIN_PaymentSchedule ps = fpsd.getInvoicePaymentSchedule();
                                ps.setPaidAmount(BigDecimal.ZERO);
                                ps.setOutstandingAmount(fpsd.getAmount());
                                OBDal.getInstance().save(ps);

                                // Update invoice outstanding amount
                                invoice
                                        .setOutstandingAmount(invoice.getOutstandingAmount().add(fpsd.getAmount()));
                            }
                        }
                        OBDal.getInstance().save(dummyPayment);

                        revInvoice.getInvoice().setPrepaymentamt(BigDecimal.ZERO);

                        invoice.setTotalPaid(BigDecimal.ZERO);
                        invoice.setPrepaymentamt(BigDecimal.ZERO);

                        OBDal.getInstance().save(invoice);
                        OBDal.getInstance().save(revInvoice.getInvoice());
                    } else if (revInvoice != null && dummyPayment != null) {
                        processPayment = true;

                        List<FIN_PaymentDetail> paymentDetails = new ArrayList<FIN_PaymentDetail>();
                        List<FIN_PaymentScheduleDetail> paymentScheduleDetails = dao
                                .getInvoicePendingScheduledPaymentDetails(revInvoice.getInvoice());
                        for (FIN_PaymentScheduleDetail psd : paymentScheduleDetails) {
                            FIN_PaymentDetail pd = OBProvider.getInstance().get(FIN_PaymentDetail.class);
                            pd.setOrganization(psd.getOrganization());
                            pd.setFinPayment(dummyPayment);
                            pd.setAmount(psd.getAmount());
                            pd.setRefund(false);
                            OBDal.getInstance().save(pd);

                            paymentDetails.add(pd);
                            psd.setPaymentDetails(pd);
                            pd.getFINPaymentScheduleDetailList().add(psd);
                            OBDal.getInstance().save(psd);
                        }
                        dummyPayment.getFINPaymentDetailList().addAll(paymentDetails);
                        OBDal.getInstance().save(dummyPayment);
                    }

                    if (processPayment) {
                        // Process dummy payment related with both actual invoice and reversed invoice
                        OBError message = FIN_AddPayment.processPayment(vars, conn, "P", dummyPayment);
                        if (StringUtils.equals(ERROR, message.getType())) {
                            message.setMessage(
                                    OBMessageUtils.messageBD("PaymentError") + " " + message.getMessage());
                            return message;
                        }
                    }
                } catch (final Exception e) {
                    log4j.error(
                            "Exception while creating dummy payment for the invoice: " + strC_Invoice_ID, e);
                } finally {
                    OBContext.restorePreviousMode();
                }
            }

            // Remove invoice's used credit description
            if ("RE".equals(strdocaction) && pinstance.getResult() != 0L) {
                final String invDesc = invoice.getDescription();
                if (invDesc != null) {
                    final String creditMsg = Utility.messageBD(conn, "APRM_InvoiceDescUsedCredit",
                            vars.getLanguage());
                    if (creditMsg != null) {
                        final StringBuffer newDesc = new StringBuffer();
                        for (final String line : invDesc.split("\n")) {
                            if (!line.startsWith(creditMsg.substring(0, creditMsg.lastIndexOf("%s")))) {
                                newDesc.append(line);
                                if (!StringUtils.EMPTY.equals(line)) {
                                    newDesc.append("\n");
                                }
                            }
                        }
                        invoice.setDescription(newDesc.toString());
                    }
                }
            }
            OBDal.getInstance().save(invoice);
            OBDal.getInstance().flush();

            OBContext.setAdminMode();
            try {
                // on error close popup and rollback
                if (pinstance.getResult() == 0L) {
                    OBDal.getInstance().rollbackAndClose();
                    myMessage = Utility.translateError(conn, vars, vars.getLanguage(),
                            pinstance.getErrorMsg().replaceFirst("@ERROR=", StringUtils.EMPTY));
                    log4j.debug(myMessage.getMessage());

                    return myMessage;
                }
            } finally {
                OBContext.restorePreviousMode();
            }

            for (ProcessInvoiceHook hook : hooks) {
                msg = hook.postProcess(invoice, strdocaction);
                if (msg != null && StringUtils.equals(ERROR, msg.getType())) {
                    OBDal.getInstance().rollbackAndClose();
                    return msg;
                }
            }

            OBDal.getInstance().commitAndClose();
            final PInstanceProcessData[] pinstanceData = PInstanceProcessData.select(conn,
                    pinstance.getId());
            myMessage = Utility.getProcessInstanceMessage(conn, vars, pinstanceData);
            log4j.debug(myMessage.getMessage());

        } catch (ServletException ex) {
            myMessage = Utility.translateError(conn, vars, vars.getLanguage(), ex.getMessage());
            return myMessage;
        }

        return myMessage;
    }

    public static List<String> getDocumentActionList(String documentStatus, String documentAction, String isProcessing, String tableId, VariablesSecureApp vars, ConnectionProvider conn) {
        FieldProvider[] fields = ActionButtonUtility.docAction(conn, vars, documentAction, "135",
                documentStatus, isProcessing, tableId);

        List<String> actionList = new ArrayList<>();

        for (FieldProvider field : fields) {
            actionList.add(field.getField("ID"));
        }

        return actionList;
    }
}
