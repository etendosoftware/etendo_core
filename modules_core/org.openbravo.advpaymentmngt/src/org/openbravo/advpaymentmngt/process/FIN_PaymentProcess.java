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
 * All portions are Copyright (C) 2010-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.advpaymentmngt.process;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.advpaymentmngt.exception.NoExecutionProcessFoundException;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentPropDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentProposal;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FIN_Payment_Credit;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentExecutionProcess;
import org.openbravo.scheduling.ProcessBundle;

public class FIN_PaymentProcess implements org.openbravo.scheduling.Process {
  private static AdvPaymentMngtDao dao;

  public BigDecimal ZERO = BigDecimal.ZERO;
  static Logger log4j = LogManager.getLogger();

  private static final String CREATED_BY_PROPERTY = "createdBy";
  private static final String CREATION_DATE_PROPERTY = "creationDate";

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(OBMessageUtils.messageBD("Success"));

    try {
      // retrieve custom params
      final String strAction = (String) bundle.getParams().get("action");
      final String comingFrom = (String) bundle.getParams().get("comingFrom");
      final String selectedCreditLineIds = (String) bundle.getParams().get("selectedCreditLineIds");
      // retrieve standard params
      final String recordID = (String) bundle.getParams().get("Fin_Payment_ID");
      final FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, recordID);
      final String paymentDate = (String) bundle.getParams().get("paymentdate");
      final boolean doFlush = bundle.getParams().get("doFlush") != null
          ? (Boolean) bundle.getParams().get("doFlush")
          : true;
      throwExceptionIfPaymentIsAlreadyReversed(strAction, payment);
      processPayment(payment, strAction, paymentDate, comingFrom, selectedCreditLineIds, doFlush);
      bundle.setResult(msg);
    } catch (Exception e) {
      log4j.debug(e.getMessage());
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD("Error"));
      msg.setMessage(FIN_Utility.getExceptionMessage(e));
      bundle.setResult(msg);
      OBDal.getInstance().getConnection().rollback();
    }
  }

  private void throwExceptionIfPaymentIsAlreadyReversed(final String strAction,
      final FIN_Payment payment) {
    if (StringUtils.equals(strAction, "RV") && payment.getReversedPayment() != null) {
      throw new OBException(OBMessageUtils.messageBD("APRM_PaymentAlreadyReversed"));
    }
  }

  // ProcessPayment without a return type
  public static void doProcessPayment(FIN_Payment payment, String strAction, String paymentDate,
      String comingFrom) throws OBException {
    FIN_PaymentProcess fpp = WeldUtils.getInstanceFromStaticBeanManager(FIN_PaymentProcess.class);
    fpp.processPayment(payment, strAction, paymentDate, comingFrom, null, true);
  }

  @Deprecated
  public static void doProcessPayment(FIN_Payment payment, String strAction, Boolean isPosOrder,
      String paymentDate, String comingFrom) throws OBException {
    doProcessPayment(payment, strAction, paymentDate, comingFrom);
  }

  private void processPayment(FIN_Payment payment, String strAction, String paymentDate,
      String comingFrom, String selectedCreditLineIds, boolean doFlush) throws OBException {
    dao = new AdvPaymentMngtDao();
    String msg = "";
    try {
      final boolean isReceipt = payment.isReceipt();
      if (strAction.equals("P") || strAction.equals("D")) {
        if (payment.getBusinessPartner() != null) {
          if (FIN_Utility.isBlockedBusinessPartner(payment.getBusinessPartner().getId(), isReceipt,
              4)) {
            // If the Business Partner is blocked for Payments, the Payment will not be completed.
            msg = OBMessageUtils.messageBD("ThebusinessPartner") + " "
                + payment.getBusinessPartner().getIdentifier() + " "
                + OBMessageUtils.messageBD("BusinessPartnerBlocked");
            throw new OBException(msg);
          }
        } else {
          OBContext.setAdminMode(true);
          try {
            for (FIN_PaymentDetail pd : payment.getFINPaymentDetailList()) {
              for (FIN_PaymentScheduleDetail psd : pd.getFINPaymentScheduleDetailList()) {
                BusinessPartner bPartner = null;
                if (psd.getInvoicePaymentSchedule() != null) {
                  bPartner = psd.getInvoicePaymentSchedule().getInvoice().getBusinessPartner();
                } else if (psd.getOrderPaymentSchedule() != null) {
                  bPartner = psd.getOrderPaymentSchedule().getOrder().getBusinessPartner();
                }
                if (bPartner != null && FIN_Utility.isBlockedBusinessPartner(bPartner.getId(),
                    payment.isReceipt(), 4)) {
                  // If the Business Partner is blocked for Payments, the Payment will not be
                  // completed.
                  msg = OBMessageUtils.messageBD("ThebusinessPartner") + " "
                      + bPartner.getIdentifier() + " "
                      + OBMessageUtils.messageBD("BusinessPartnerBlocked");
                  throw new OBException(msg);
                }
              }
            }
          } finally {
            OBContext.restorePreviousMode();
          }
        }
      }

      if (strAction.equals("P") || strAction.equals("D")) {
        // Guess if this is a refund payment
        boolean isRefund = false;
        OBContext.setAdminMode(false);
        try {
          List<FIN_PaymentDetail> paymentDetailList = payment.getFINPaymentDetailList();
          if (paymentDetailList.size() > 0) {
            for (FIN_PaymentDetail det : paymentDetailList) {
              if (det.isRefund()) {
                isRefund = true;
                break;
              }
            }
          }
        } finally {
          OBContext.restorePreviousMode();
        }
        if (!isRefund) {
          // Undo Used credit as it will be calculated again
          payment.setUsedCredit(BigDecimal.ZERO);
          OBDal.getInstance().save(payment);
        }

        boolean documentEnabled = getDocumentConfirmation(null, payment.getId());
        boolean periodNotAvailable = documentEnabled
            && !FIN_Utility.isPeriodOpen(payment.getClient().getId(),
                payment.getDocumentType().getDocumentCategory(), payment.getOrganization().getId(),
                OBDateUtils.formatDate(payment.getPaymentDate()))
            && FIN_Utility.periodControlOpened(FIN_Payment.TABLE_NAME, payment.getId(),
                FIN_Payment.TABLE_NAME + "_ID", "LE");
        if (periodNotAvailable) {
          msg = OBMessageUtils.messageBD("PeriodNotAvailable");
          throw new OBException(msg);
        }
        Set<String> documentOrganizations = OBContext.getOBContext()
            .getOrganizationStructureProvider(payment.getClient().getId())
            .getNaturalTree(payment.getOrganization().getId());
        if (!documentOrganizations.contains(payment.getAccount().getOrganization().getId())) {
          msg = OBMessageUtils.messageBD("APRM_FinancialAccountNotInNaturalTree");
          throw new OBException(msg);
        }
        Set<String> invoiceDocNos = new TreeSet<String>();
        Set<String> orderDocNos = new TreeSet<String>();
        Set<String> glitems = new TreeSet<String>();
        BigDecimal paymentAmount = BigDecimal.ZERO;
        BigDecimal paymentWriteOfAmount = BigDecimal.ZERO;

        // FIXME: added to access the FIN_PaymentSchedule and FIN_PaymentScheduleDetail tables to be
        // removed when new security implementation is done
        OBContext.setAdminMode();
        boolean flushDone = false;
        try {
          String strRefundCredit = "";
          // update payment schedule amount
          List<FIN_PaymentDetail> paymentDetails = payment.getFINPaymentDetailList();

          // Show error message when payment has no lines
          if (paymentDetails.size() == 0) {
            msg = OBMessageUtils.messageBD("APRM_PaymentNoLines");
            log4j.debug(msg);
            throw new OBException(msg, false);
          }
          for (FIN_PaymentDetail paymentDetail : paymentDetails) {
            for (FIN_PaymentScheduleDetail paymentScheduleDetail : paymentDetail
                .getFINPaymentScheduleDetailList()) {
              paymentAmount = paymentAmount.add(paymentScheduleDetail.getAmount());
              BigDecimal writeoff = paymentScheduleDetail.getWriteoffAmount();
              if (writeoff == null) {
                writeoff = BigDecimal.ZERO;
              }
              paymentWriteOfAmount = paymentWriteOfAmount.add(writeoff);
              if (paymentScheduleDetail.getInvoicePaymentSchedule() != null) {
                final Invoice invoice = paymentScheduleDetail.getInvoicePaymentSchedule()
                    .getInvoice();
                invoiceDocNos
                    .add(FIN_Utility.getDesiredDocumentNo(payment.getOrganization(), invoice));
              }
              if (paymentScheduleDetail.getOrderPaymentSchedule() != null) {
                orderDocNos.add(
                    paymentScheduleDetail.getOrderPaymentSchedule().getOrder().getDocumentNo());
              }
              if (paymentScheduleDetail.getInvoicePaymentSchedule() == null
                  && paymentScheduleDetail.getOrderPaymentSchedule() == null
                  && paymentScheduleDetail.getPaymentDetails().getGLItem() == null) {
                if (paymentDetail.isRefund()) {
                  strRefundCredit = OBMessageUtils.messageBD("APRM_RefundAmount");
                } else {
                  strRefundCredit = OBMessageUtils.messageBD("APRM_CreditAmount");
                  payment.setGeneratedCredit(paymentDetail.getAmount());
                }
                strRefundCredit += ": " + paymentDetail.getAmount().toString();
              }
            }
            if (paymentDetail.getGLItem() != null) {
              glitems.add(paymentDetail.getGLItem().getName());
            }
          }
          // Set description
          StringBuffer description = new StringBuffer();

          if (payment.getDescription() != null && !payment.getDescription().equals("")) {
            description.append(payment.getDescription()).append("\n");
          }
          if (!invoiceDocNos.isEmpty()) {
            description.append(OBMessageUtils.messageBD("InvoiceDocumentno"));
            description.append(": ")
                .append(
                    invoiceDocNos.toString().substring(1, invoiceDocNos.toString().length() - 1));
            description.append("\n");
          }
          if (!orderDocNos.isEmpty()) {
            description.append(OBMessageUtils.messageBD("OrderDocumentno"));
            description.append(": ")
                .append(orderDocNos.toString().substring(1, orderDocNos.toString().length() - 1));
            description.append("\n");
          }
          if (!glitems.isEmpty()) {
            description.append(OBMessageUtils.messageBD("APRM_GLItem"));
            description.append(": ")
                .append(glitems.toString().substring(1, glitems.toString().length() - 1));
            description.append("\n");
          }
          if (!"".equals(strRefundCredit)) {
            description.append(strRefundCredit).append("\n");
          }

          String truncateDescription = (description.length() > 255)
              ? description.substring(0, 251).concat("...").toString()
              : description.toString();
          payment.setDescription(truncateDescription);

          if (paymentAmount.compareTo(payment.getAmount()) != 0) {
            payment.setUsedCredit(paymentAmount.subtract(payment.getAmount()));
          }
          if (payment.getUsedCredit().compareTo(BigDecimal.ZERO) != 0) {
            updateUsedCredit(payment, selectedCreditLineIds);
          }

          payment.setWriteoffAmount(paymentWriteOfAmount);
          payment.setProcessed(true);
          payment.setAPRMProcessPayment("RE");
          if (payment.getGeneratedCredit() == null) {
            payment.setGeneratedCredit(BigDecimal.ZERO);
          }
          if (BigDecimal.ZERO.compareTo(payment.getUsedCredit()) != 0
              || BigDecimal.ZERO.compareTo(payment.getGeneratedCredit()) != 0) {
            BusinessPartner businessPartner = payment.getBusinessPartner();
            if (businessPartner == null) {
              msg = OBMessageUtils.messageBD("APRM_CreditWithoutBPartner");
              throw new OBException(msg);
            }
            String currency = null;
            if (businessPartner.getCurrency() == null) {
              currency = payment.getCurrency().getId();
              businessPartner.setCurrency(payment.getCurrency());
            } else {
              currency = businessPartner.getCurrency().getId();
            }
            if (!payment.getCurrency().getId().equals(currency)) {
              msg = String.format(OBMessageUtils.messageBD("APRM_CreditCurrency"),
                  businessPartner.getCurrency().getISOCode());
              throw new OBException(msg);
            }
          }
          // Execution Process
          if (dao.isAutomatedExecutionPayment(payment.getAccount(), payment.getPaymentMethod(),
              payment.isReceipt()) && payment.getAmount().compareTo(BigDecimal.ZERO) != 0) {
            try {
              payment.setStatus("RPAE");

              if (dao.hasNotDeferredExecutionProcess(payment.getAccount(),
                  payment.getPaymentMethod(), payment.isReceipt())) {
                PaymentExecutionProcess executionProcess = dao.getExecutionProcess(payment);
                if (dao.isAutomaticExecutionProcess(executionProcess)) {
                  final List<FIN_Payment> payments = new ArrayList<FIN_Payment>(1);
                  payments.add(payment);
                  FIN_ExecutePayment executePayment = new FIN_ExecutePayment();
                  executePayment.init("APP", executionProcess, payments, null,
                      payment.getOrganization());
                  executePayment.addInternalParameter("comingFrom", comingFrom);
                  OBError result = executePayment.execute();
                  if ("Error".equals(result.getType())) {
                    msg = OBMessageUtils.messageBD(result.getMessage());
                  } else if (!"".equals(result.getMessage())) {
                    String execProcessMsg = OBMessageUtils.messageBD(result.getMessage());
                    if (!"".equals(msg)) {
                      msg += "<br>";
                    }
                    msg += execProcessMsg;
                  }
                }
              }
            } catch (final NoExecutionProcessFoundException e) {
              msg = OBMessageUtils.messageBD("NoExecutionProcessFound");
              throw new OBException(msg);
            } catch (final Exception e) {
              msg = OBMessageUtils.messageBD("IssueOnExecutionProcess");
              throw new OBException(msg);
            }
          } else {
            BusinessPartner businessPartner = payment.getBusinessPartner();
            // When credit is used (consumed) we compensate so_creditused as this amount is already
            // included in the payment details. Credit consumed should not affect to so_creditused
            if (payment.getGeneratedCredit().compareTo(BigDecimal.ZERO) == 0
                && payment.getUsedCredit().compareTo(BigDecimal.ZERO) != 0) {
              if (isReceipt) {
                increaseCustomerCredit(businessPartner, payment.getUsedCredit());
              } else {
                decreaseCustomerCredit(businessPartner, payment.getUsedCredit());
              }
            }

            for (FIN_PaymentDetail paymentDetail : payment.getFINPaymentDetailList()) {

              List<FIN_PaymentScheduleDetail> orderPaymentScheduleDetails = new ArrayList<FIN_PaymentScheduleDetail>(
                  paymentDetail.getFINPaymentScheduleDetailList());

              // Get payment schedule detail list ordered by amount asc.
              // First negative if they exist and then positives
              if (orderPaymentScheduleDetails.size() > 1) {
                Collections.sort(orderPaymentScheduleDetails,
                    new Comparator<FIN_PaymentScheduleDetail>() {
                      @Override
                      public int compare(FIN_PaymentScheduleDetail o1,
                          FIN_PaymentScheduleDetail o2) {
                        // TODO Auto-generated method stub
                        return o1.getAmount().compareTo(o2.getAmount());
                      }
                    });
              }

              for (FIN_PaymentScheduleDetail paymentScheduleDetail : orderPaymentScheduleDetails) {
                BigDecimal amount = paymentScheduleDetail.getAmount()
                    .add(paymentScheduleDetail.getWriteoffAmount());
                // Do not restore paid amounts if the payment is awaiting execution.
                boolean invoicePaidAmounts = (FIN_Utility
                    .seqnumberpaymentstatus(isReceipt ? "RPR" : "PPM")) >= (FIN_Utility
                        .seqnumberpaymentstatus(FIN_Utility.invoicePaymentStatus(payment)));
                paymentScheduleDetail.setInvoicePaid(false);
                // Payment = 0 when the payment is generated by a invoice that consume credit
                if (invoicePaidAmounts
                    || (payment.getAmount().compareTo(new BigDecimal("0.00")) == 0)) {
                  if (paymentScheduleDetail.getInvoicePaymentSchedule() != null) {
                    // BP SO_CreditUsed
                    businessPartner = paymentScheduleDetail.getInvoicePaymentSchedule()
                        .getInvoice()
                        .getBusinessPartner();

                    // Payments update credit opposite to invoices
                    BigDecimal paidAmount = BigDecimal.ZERO;
                    Invoice invoiceForConversion = paymentScheduleDetail
                        .getInvoicePaymentSchedule() != null
                            ? paymentScheduleDetail.getInvoicePaymentSchedule().getInvoice()
                            : null;
                    paidAmount = BigDecimal.ZERO;
                    String fromCurrency = payment.getCurrency().getId();
                    if (businessPartner.getCurrency() == null) {
                      String errorMSG = OBMessageUtils.messageBD("InitBPCurrencyLnk", false);
                      msg = String.format(errorMSG, businessPartner.getId(),
                          businessPartner.getName());
                      throw new OBException(msg);
                    }
                    String toCurrency = businessPartner.getCurrency().getId();
                    if (!fromCurrency.equals(toCurrency)) {
                      BigDecimal exchangeRate = BigDecimal.ZERO;
                      // check at invoice document level
                      List<ConversionRateDoc> conversionRateDocumentForInvoice = getConversionRateDocumentForInvoice(
                          invoiceForConversion);
                      if (conversionRateDocumentForInvoice.size() > 0) {
                        exchangeRate = conversionRateDocumentForInvoice.get(0).getRate();
                      } else {
                        // global
                        exchangeRate = getConversionRate(payment.getOrganization().getId(),
                            fromCurrency, toCurrency,
                            invoiceForConversion != null ? invoiceForConversion.getInvoiceDate()
                                : payment.getPaymentDate());
                      }
                      if (exchangeRate == BigDecimal.ZERO) {
                        msg = OBMessageUtils.messageBD("NoCurrencyConversion");
                        throw new OBException(msg);
                      }
                      paidAmount = amount.multiply(exchangeRate);
                    } else {
                      paidAmount = amount;
                    }
                    if (isReceipt) {
                      decreaseCustomerCredit(businessPartner, paidAmount);
                    } else {
                      increaseCustomerCredit(businessPartner, paidAmount);
                    }
                    FIN_AddPayment.updatePaymentScheduleAmounts(paymentDetail,
                        paymentScheduleDetail.getInvoicePaymentSchedule(),
                        paymentScheduleDetail.getAmount(),
                        paymentScheduleDetail.getWriteoffAmount());
                    paymentScheduleDetail.setInvoicePaid(true);
                  }

                  if (paymentScheduleDetail.getOrderPaymentSchedule() != null) {
                    FIN_AddPayment.updatePaymentScheduleAmounts(paymentDetail,
                        paymentScheduleDetail.getOrderPaymentSchedule(),
                        paymentScheduleDetail.getAmount(),
                        paymentScheduleDetail.getWriteoffAmount());
                    paymentScheduleDetail.setInvoicePaid(true);
                  }
                  // when generating credit for a BP SO_CreditUsed is also updated
                  if (paymentScheduleDetail.getInvoicePaymentSchedule() == null
                      && paymentScheduleDetail.getOrderPaymentSchedule() == null
                      && paymentScheduleDetail.getPaymentDetails().getGLItem() == null
                      && !paymentDetail.isRefund()) {
                    // BP SO_CreditUsed
                    if (isReceipt) {
                      decreaseCustomerCredit(businessPartner, amount);
                    } else {
                      increaseCustomerCredit(businessPartner, amount);
                    }
                  }
                }
              }
            }
            payment.setStatus(isReceipt ? "RPR" : "PPM");

            if ((strAction.equals("D") || FIN_Utility.isAutomaticDepositWithdrawn(payment))
                && payment.getAmount().compareTo(BigDecimal.ZERO) != 0
                && !"TRANSACTION".equals(comingFrom)) {
              triggerAutomaticFinancialAccountTransaction(payment);
              flushDone = true;
            }
          }
          if (!payment.getAccount().getCurrency().equals(payment.getCurrency())
              && getConversionRateDocument(payment).size() == 0) {
            insertConversionRateDocument(payment);
            flushDone = true;
          }
        } finally {
          if (!flushDone && doFlush) {
            OBDal.getInstance().flush();
          }
          OBContext.restorePreviousMode();
        }

        // ***********************
        // Reverse Payment
        // ***********************
      } else if (strAction.equals("RV")) {
        FIN_Payment reversedPayment = (FIN_Payment) DalUtil.copy(payment, false);
        final Date now = new Date();
        updateAuditInformation(reversedPayment, now);
        OBContext.setAdminMode();
        try {
          if (BigDecimal.ZERO.compareTo(payment.getGeneratedCredit()) != 0
              && BigDecimal.ZERO.compareTo(payment.getUsedCredit()) != 0) {
            throw new OBException("@APRM_CreditConsumed@");
          } else if (BigDecimal.ZERO.compareTo(payment.getGeneratedCredit()) != 0
              && BigDecimal.ZERO.compareTo(payment.getUsedCredit()) == 0) {
            reversedPayment.setUsedCredit(payment.getGeneratedCredit());
            reversedPayment.setGeneratedCredit(BigDecimal.ZERO);
          } else {
            reversedPayment.setUsedCredit(BigDecimal.ZERO);
            reversedPayment.setGeneratedCredit(BigDecimal.ZERO);
          }
          reversedPayment.setDocumentNo(
              "*R*" + FIN_Utility.getDocumentNo(payment.getDocumentType(), "FIN_Payment"));
          reversedPayment.setPaymentDate(FIN_Utility.getDate(paymentDate));
          reversedPayment.setDescription("");
          reversedPayment.setProcessed(false);
          reversedPayment.setPosted("N");
          reversedPayment.setProcessNow(false);
          reversedPayment.setAPRMProcessPayment("P");
          reversedPayment.setStatus("RPAP");
          // Amounts
          reversedPayment.setAmount(payment.getAmount().negate());
          reversedPayment.setWriteoffAmount(payment.getWriteoffAmount().negate());
          reversedPayment
              .setFinancialTransactionAmount(payment.getFinancialTransactionAmount().negate());
          OBDal.getInstance().save(reversedPayment);

          List<FIN_PaymentDetail> reversedDetails = new ArrayList<FIN_PaymentDetail>();

          OBDal.getInstance().save(reversedPayment);
          List<FIN_Payment_Credit> credits = payment.getFINPaymentCreditList();

          for (FIN_PaymentDetail pd : payment.getFINPaymentDetailList()) {
            FIN_PaymentDetail reversedPaymentDetail = (FIN_PaymentDetail) DalUtil.copy(pd, false);
            updateAuditInformation(reversedPaymentDetail, now);
            reversedPaymentDetail.setFinPayment(reversedPayment);
            reversedPaymentDetail.setAmount(pd.getAmount().negate());
            reversedPaymentDetail.setWriteoffAmount(pd.getWriteoffAmount().negate());
            if (pd.isRefund()) {
              reversedPaymentDetail.setPrepayment(true);
              reversedPaymentDetail.setRefund(false);
              reversedPayment
                  .setGeneratedCredit(reversedPayment.getGeneratedCredit().add(pd.getAmount()));
              credits = new ArrayList<FIN_Payment_Credit>();
              OBDal.getInstance().save(reversedPayment);
            } else if (pd.isPrepayment()
                && pd.getFINPaymentScheduleDetailList().get(0).getOrderPaymentSchedule() == null) {
              reversedPaymentDetail.setPrepayment(true);
              reversedPaymentDetail.setRefund(true);
            } else if (pd.isPrepayment() && pd.getFINPaymentScheduleDetailList()
                .get(0)
                .getInvoicePaymentSchedule() != null) {
              reversedPaymentDetail.setPrepayment(false);
              reversedPaymentDetail.setRefund(false);
            }
            List<FIN_PaymentScheduleDetail> reversedSchedDetails = new ArrayList<FIN_PaymentScheduleDetail>();
            OBDal.getInstance().save(reversedPaymentDetail);
            // Create or update PSD of orders and invoices to set the new outstanding amount
            for (FIN_PaymentScheduleDetail psd : pd.getFINPaymentScheduleDetailList()) {
              if (psd.getInvoicePaymentSchedule() != null
                  || psd.getOrderPaymentSchedule() != null) {
                OBCriteria<FIN_PaymentScheduleDetail> unpaidSchedDet = OBDal.getInstance()
                    .createCriteria(FIN_PaymentScheduleDetail.class);
                if (psd.getInvoicePaymentSchedule() != null) {
                  unpaidSchedDet.add(
                      Restrictions.eq(FIN_PaymentScheduleDetail.PROPERTY_INVOICEPAYMENTSCHEDULE,
                          psd.getInvoicePaymentSchedule()));
                }
                if (psd.getOrderPaymentSchedule() != null) {
                  unpaidSchedDet
                      .add(Restrictions.eq(FIN_PaymentScheduleDetail.PROPERTY_ORDERPAYMENTSCHEDULE,
                          psd.getOrderPaymentSchedule()));
                }
                unpaidSchedDet
                    .add(Restrictions.isNull(FIN_PaymentScheduleDetail.PROPERTY_PAYMENTDETAILS));
                List<FIN_PaymentScheduleDetail> openPSDs = unpaidSchedDet.list();
                // If invoice/order not fully paid, update outstanding amount
                if (openPSDs.size() > 0) {
                  FIN_PaymentScheduleDetail openPSD = openPSDs.get(0);
                  BigDecimal openAmount = openPSD.getAmount()
                      .add(psd.getAmount().add(psd.getWriteoffAmount()));
                  if (openAmount.compareTo(BigDecimal.ZERO) == 0) {
                    OBDal.getInstance().remove(openPSD);
                  } else {
                    openPSD.setAmount(openAmount);
                  }
                } else {
                  // If invoice is fully paid create a new schedule detail.
                  FIN_PaymentScheduleDetail openPSD = (FIN_PaymentScheduleDetail) DalUtil.copy(psd,
                      false);
                  updateAuditInformation(openPSD, now);
                  openPSD.setPaymentDetails(null);
                  // Amounts
                  openPSD.setWriteoffAmount(BigDecimal.ZERO);
                  openPSD.setAmount(psd.getAmount().add(psd.getWriteoffAmount()));

                  openPSD.setCanceled(false);
                  OBDal.getInstance().save(openPSD);
                }
              }

              FIN_PaymentScheduleDetail reversedPaymentSchedDetail = (FIN_PaymentScheduleDetail) DalUtil
                  .copy(psd, false);
              updateAuditInformation(reversedPaymentSchedDetail, now);
              reversedPaymentSchedDetail.setPaymentDetails(reversedPaymentDetail);
              // Amounts
              reversedPaymentSchedDetail.setWriteoffAmount(psd.getWriteoffAmount().negate());
              reversedPaymentSchedDetail.setAmount(psd.getAmount().negate());
              reversedPaymentSchedDetail
                  .setDoubtfulDebtAmount(psd.getDoubtfulDebtAmount().negate());
              OBDal.getInstance().save(reversedPaymentSchedDetail);
              reversedSchedDetails.add(reversedPaymentSchedDetail);

              if ((FIN_Utility.invoicePaymentStatus(reversedPayment)
                  .equals(reversedPayment.getStatus()))) {
                reversedPaymentSchedDetail.setInvoicePaid(true);

              } else {
                reversedPaymentSchedDetail.setInvoicePaid(false);
              }
              OBDal.getInstance().save(reversedPaymentSchedDetail);

            }

            reversedPaymentDetail.setFINPaymentScheduleDetailList(reversedSchedDetails);
            OBDal.getInstance().save(reversedPaymentDetail);
            reversedDetails.add(reversedPaymentDetail);
          }
          reversedPayment.setFINPaymentDetailList(reversedDetails);
          OBDal.getInstance().save(reversedPayment);

          List<FIN_Payment_Credit> reversedCredits = new ArrayList<FIN_Payment_Credit>();
          for (FIN_Payment_Credit pc : credits) {
            FIN_Payment_Credit reversedPaymentCredit = (FIN_Payment_Credit) DalUtil.copy(pc, false);
            updateAuditInformation(reversedPaymentCredit, now);
            reversedPaymentCredit.setAmount(pc.getAmount().negate());
            reversedPaymentCredit.setCreditPaymentUsed(pc.getCreditPaymentUsed());
            pc.getCreditPaymentUsed()
                .setUsedCredit(
                    pc.getCreditPaymentUsed().getUsedCredit().add(pc.getAmount().negate()));
            reversedPaymentCredit.setPayment(reversedPayment);
            OBDal.getInstance().save(pc.getCreditPaymentUsed());
            OBDal.getInstance().save(reversedPaymentCredit);
            reversedCredits.add(reversedPaymentCredit);
          }

          reversedPayment.setFINPaymentCreditList(reversedCredits);
          OBDal.getInstance().save(reversedPayment);

          List<ConversionRateDoc> conversions = new ArrayList<ConversionRateDoc>();
          for (ConversionRateDoc cr : payment.getCurrencyConversionRateDocList()) {
            ConversionRateDoc reversedCR = (ConversionRateDoc) DalUtil.copy(cr, false);
            updateAuditInformation(reversedCR, now);
            reversedCR.setForeignAmount(cr.getForeignAmount().negate());
            reversedCR.setPayment(reversedPayment);
            OBDal.getInstance().save(reversedCR);
            conversions.add(reversedCR);
          }
          reversedPayment.setCurrencyConversionRateDocList(conversions);
          OBDal.getInstance().save(reversedPayment);

          OBDal.getInstance().flush();
        } finally {
          OBContext.restorePreviousMode();
        }

        payment.setReversedPayment(reversedPayment);
        OBDal.getInstance().save(payment);
        OBDal.getInstance().flush();

        String newStrAction = "P";
        FIN_PaymentProcess fpp = WeldUtils
            .getInstanceFromStaticBeanManager(FIN_PaymentProcess.class);
        fpp.processPayment(reversedPayment, newStrAction, paymentDate, comingFrom,
            selectedCreditLineIds, true);

        return;

        // ***********************
        // Reactivate Payment
        // ***********************
      } else if (strAction.equals("R") || strAction.equals("RE")) {
        // Already Posted Document
        if ("Y".equals(payment.getPosted())) {
          msg = OBMessageUtils.messageBD("PostedDocument: " + payment.getDocumentNo());
          throw new OBException(msg);
        }
        // Reversed Payment
        if (payment.getReversedPayment() != null) {
          msg = OBMessageUtils.messageBD("APRM_PaymentReversed");
          throw new OBException(msg);
        }
        // Reverse Payment
        if (strAction.equals("RE") && FIN_Utility.isReversePayment(payment)) {
          msg = OBMessageUtils.messageBD("APRM_ReversePayment");
          throw new OBException(msg);
        }

        // Do not reactive the payment if it is tax payment
        if (payment.getFinancialMgmtTaxPaymentList().size() != 0) {
          msg = OBMessageUtils.messageBD("APRM_TaxPaymentReactivation");
          throw new OBException(msg);
        }

        // Transaction exists
        if (hasTransaction(payment)) {
          msg = OBMessageUtils.messageBD("APRM_TransactionExists");
          throw new OBException(msg);
        }
        // Payment with generated credit already used on other payments.
        if (payment.getGeneratedCredit().compareTo(BigDecimal.ZERO) == 1
            && payment.getUsedCredit().compareTo(BigDecimal.ZERO) == 1) {
          msg = OBMessageUtils.messageBD("APRM_PaymentGeneratedCreditIsUsed");
          throw new OBException(msg);
        }

        if (FIN_Utility.invoicePaymentStatus(payment) == null) {
          msg = String.format(OBMessageUtils.messageBD("APRM_NoPaymentMethod"),
              payment.getPaymentMethod().getIdentifier(), payment.getDocumentNo(),
              payment.getAccount().getName());
          throw new OBException(msg);
        }
        // Do not restore paid amounts if the payment is awaiting execution.
        boolean restorePaidAmounts = (FIN_Utility
            .seqnumberpaymentstatus(payment.getStatus())) == (FIN_Utility
                .seqnumberpaymentstatus(FIN_Utility.invoicePaymentStatus(payment)));
        // Initialize amounts
        payment.setProcessed(false);
        OBDal.getInstance().save(payment);
        OBDal.getInstance().flush();
        payment.setWriteoffAmount(BigDecimal.ZERO);

        payment.setDescription("");

        // if all line are deleted then update amount to zero
        if (strAction.equals("R")) {
          payment.setAmount(BigDecimal.ZERO);
        }

        payment.setStatus("RPAP");
        payment.setAPRMProcessPayment("P");
        OBDal.getInstance().save(payment);
        OBDal.getInstance().flush();

        final List<FIN_PaymentDetail> removedPD = new ArrayList<FIN_PaymentDetail>();
        List<FIN_PaymentScheduleDetail> removedPDS = new ArrayList<FIN_PaymentScheduleDetail>();
        final List<String> removedPDIds = new ArrayList<String>();
        // FIXME: added to access the FIN_PaymentSchedule and FIN_PaymentScheduleDetail tables to be
        // removed when new security implementation is done
        OBContext.setAdminMode();
        try {
          BusinessPartner businessPartner = payment.getBusinessPartner();
          BigDecimal paidAmount = BigDecimal.ZERO;
          if (!(businessPartner == null)) {
            // When credit is used (consumed) we compensate so_creditused as this amount is already
            // included in the payment details. Credit consumed should not affect to so_creditused
            if (payment.getGeneratedCredit().compareTo(BigDecimal.ZERO) == 0
                && payment.getUsedCredit().compareTo(BigDecimal.ZERO) != 0) {
              if (isReceipt) {
                decreaseCustomerCredit(businessPartner, payment.getUsedCredit());
              } else {
                increaseCustomerCredit(businessPartner, payment.getUsedCredit());
              }
            }
          }
          List<FIN_PaymentDetail> paymentDetails = payment.getFINPaymentDetailList();
          List<ConversionRateDoc> conversionRates = payment.getCurrencyConversionRateDocList();
          Set<String> invoiceDocNos = new HashSet<String>();
          // Undo Reversed payment relationship
          List<FIN_Payment> revPayments = new ArrayList<FIN_Payment>();
          for (FIN_Payment reversedPayment : payment.getFINPaymentReversedPaymentList()) {
            reversedPayment.setReversedPayment(null);
            OBDal.getInstance().save(reversedPayment);
          }
          payment.setFINPaymentReversedPaymentList(revPayments);
          OBDal.getInstance().save(payment);
          for (FIN_PaymentDetail paymentDetail : paymentDetails) {
            removedPDS = new ArrayList<FIN_PaymentScheduleDetail>();
            for (FIN_PaymentScheduleDetail paymentScheduleDetail : paymentDetail
                .getFINPaymentScheduleDetailList()) {
              Boolean invoicePaidold = paymentScheduleDetail.isInvoicePaid();
              if (invoicePaidold | paymentScheduleDetail.getInvoicePaymentSchedule() == null) {
                BigDecimal psdWriteoffAmount = paymentScheduleDetail.getWriteoffAmount();
                BigDecimal psdAmount = paymentScheduleDetail.getAmount();
                BigDecimal amount = psdAmount.add(psdWriteoffAmount);
                if (paymentScheduleDetail.getInvoicePaymentSchedule() != null) {
                  // Remove invoice description related to the credit payments
                  final Invoice invoice = paymentScheduleDetail.getInvoicePaymentSchedule()
                      .getInvoice();
                  invoiceDocNos.add(invoice.getDocumentNo());
                  final String invDesc = invoice.getDescription();
                  if (invDesc != null) {
                    final String creditMsg = OBMessageUtils.messageBD("APRM_InvoiceDescUsedCredit");
                    if (creditMsg != null) {
                      StringBuffer newDesc = new StringBuffer();
                      for (final String line : invDesc.split("\n")) {
                        if (!line.startsWith(creditMsg.substring(0, creditMsg.lastIndexOf("%s")))) {
                          newDesc.append(line);
                          if (!"".equals(line)) {
                            newDesc.append("\n");
                          }
                        }
                      }
                      if (newDesc.length() > 255) {
                        newDesc = newDesc.delete(251, newDesc.length());
                        newDesc = newDesc.append("...\n");
                      }
                      invoice.setDescription(newDesc.toString());

                    }
                  }
                  if (restorePaidAmounts) {
                    FIN_AddPayment.updatePaymentScheduleAmounts(paymentDetail,
                        paymentScheduleDetail.getInvoicePaymentSchedule(), psdAmount.negate(),
                        psdWriteoffAmount.negate());
                    paymentScheduleDetail.setInvoicePaid(false);
                    OBDal.getInstance().save(paymentScheduleDetail);
                    // BP SO_CreditUsed
                    businessPartner = paymentScheduleDetail.getInvoicePaymentSchedule()
                        .getInvoice()
                        .getBusinessPartner();
                    Invoice invoiceForConversion = paymentScheduleDetail
                        .getInvoicePaymentSchedule() != null
                            ? paymentScheduleDetail.getInvoicePaymentSchedule().getInvoice()
                            : null;
                    paidAmount = BigDecimal.ZERO;
                    if (!(businessPartner == null)) {
                      final Currency fromCurrency = payment.getCurrency();
                      if (businessPartner.getCurrency() == null) {
                        String errorMSG = OBMessageUtils.messageBD("InitBPCurrencyLnk", false);
                        msg = String.format(errorMSG, businessPartner.getId(),
                            businessPartner.getName());
                        throw new OBException(msg);
                      }
                      final Currency toCurrency = businessPartner.getCurrency();
                      if (fromCurrency != null && toCurrency != null
                          && !fromCurrency.getId().equals(toCurrency.getId())) {
                        BigDecimal exchangeRate = BigDecimal.ZERO;
                        // check at invoice document level
                        List<ConversionRateDoc> conversionRateDocumentForInvoice = getConversionRateDocumentForInvoice(
                            invoiceForConversion);
                        if (conversionRateDocumentForInvoice.size() > 0) {
                          exchangeRate = conversionRateDocumentForInvoice.get(0).getRate();
                        } else {
                          // global
                          exchangeRate = getConversionRate(payment.getOrganization().getId(),
                              fromCurrency.getId(), toCurrency.getId(),
                              invoiceForConversion != null ? invoiceForConversion.getInvoiceDate()
                                  : payment.getPaymentDate());
                        }
                        if (exchangeRate == BigDecimal.ZERO) {
                          msg = OBMessageUtils.messageBD("NoCurrencyConversion");
                          throw new OBException(msg);
                        }
                        paidAmount = amount.multiply(exchangeRate);
                      } else {
                        paidAmount = amount;
                      }
                      if (isReceipt) {
                        increaseCustomerCredit(businessPartner, paidAmount);
                      } else {
                        decreaseCustomerCredit(businessPartner, paidAmount);
                      }
                    }
                  }
                }
                if (paymentScheduleDetail.getOrderPaymentSchedule() != null && restorePaidAmounts) {
                  FIN_AddPayment.updatePaymentScheduleAmounts(paymentDetail,
                      paymentScheduleDetail.getOrderPaymentSchedule(), psdAmount.negate(),
                      psdWriteoffAmount.negate());
                }
                if (restorePaidAmounts) {
                  // when generating credit for a BP SO_CreditUsed is also updated
                  if (paymentScheduleDetail.getInvoicePaymentSchedule() == null
                      && paymentScheduleDetail.getOrderPaymentSchedule() == null
                      && paymentScheduleDetail.getPaymentDetails().getGLItem() == null
                      && restorePaidAmounts && !paymentDetail.isRefund()) {
                    // BP SO_CreditUsed
                    if (isReceipt) {
                      increaseCustomerCredit(businessPartner, amount);
                    } else {
                      decreaseCustomerCredit(businessPartner, amount);
                    }
                  }
                }
              }

              if (strAction.equals("R") || (strAction.equals("RE")
                  && paymentScheduleDetail.getInvoicePaymentSchedule() == null
                  && paymentScheduleDetail.getOrderPaymentSchedule() == null
                  && paymentScheduleDetail.getPaymentDetails().getGLItem() == null)) {
                FIN_AddPayment.mergePaymentScheduleDetails(paymentScheduleDetail);
                removedPDS.add(paymentScheduleDetail);
              }

            }
            paymentDetail.getFINPaymentScheduleDetailList().removeAll(removedPDS);
            if (strAction.equals("R")) {
              OBDal.getInstance().getSession().refresh(paymentDetail);
            }
            // If there is any schedule detail with amount zero, those are deleted
            // Besides it removes the payment proposal lines linked to the PSD when
            // a) we are removing the PSD and
            // b) if we are reactivating a payment (deleting lines only) and we don't come from
            // payment proposal reactivation process
            for (FIN_PaymentScheduleDetail psd : removedPDS) {
              int proposalLinesRemoved = 0;
              if (BigDecimal.ZERO.compareTo(psd.getAmount()) == 0
                  && BigDecimal.ZERO.compareTo(psd.getWriteoffAmount()) == 0) {
                paymentDetail.getFINPaymentScheduleDetailList().remove(psd);
                OBDal.getInstance().getSession().refresh(paymentDetail);
                if (psd.getInvoicePaymentSchedule() != null) {
                  psd.getInvoicePaymentSchedule()
                      .getFINPaymentScheduleDetailInvoicePaymentScheduleList()
                      .remove(psd);
                }
                if (psd.getOrderPaymentSchedule() != null) {
                  psd.getOrderPaymentSchedule()
                      .getFINPaymentScheduleDetailOrderPaymentScheduleList()
                      .remove(psd);
                }

                // Before deleting the PSD, we must delete any payment proposal line linked to it
                proposalLinesRemoved = removePaymentProposalLines(psd);

                OBDal.getInstance().remove(psd);
              }

              // Delete any payment proposal line linked to the PSD if we are reactivating a payment
              // (deleting lines only), we haven't removed it in a previous step and we don't come
              // from payment proposal reactivation process
              if (strAction.equals("R") && proposalLinesRemoved == 0
                  && !StringUtils.equals(comingFrom,
                      FIN_PaymentProposalProcess.COMINGFROM_PAYMENTPROPOSALPROCESS)) {
                removePaymentProposalLines(psd);
              }
            }
            if (paymentDetail.getFINPaymentScheduleDetailList().size() == 0) {
              removedPD.add(paymentDetail);
              removedPDIds.add(paymentDetail.getId());
            }
            OBDal.getInstance().save(paymentDetail);
          }
          for (String pdToRm : removedPDIds) {
            OBDal.getInstance().remove(OBDal.getInstance().get(FIN_PaymentDetail.class, pdToRm));
          }
          payment.getFINPaymentDetailList().removeAll(removedPD);
          if (strAction.equals("R")) {
            payment.getCurrencyConversionRateDocList().removeAll(conversionRates);
            payment.setFinancialTransactionConvertRate(BigDecimal.ZERO);
          }
          OBDal.getInstance().save(payment);

          if (payment.getGeneratedCredit().compareTo(BigDecimal.ZERO) == 0
              && payment.getUsedCredit().compareTo(BigDecimal.ZERO) != 0) {
            undoUsedCredit(payment, invoiceDocNos);
          }

          List<FIN_Payment> creditPayments = new ArrayList<FIN_Payment>();
          for (final FIN_Payment_Credit pc : payment.getFINPaymentCreditList()) {
            creditPayments.add(pc.getCreditPaymentUsed());
          }
          for (final FIN_Payment creditPayment : creditPayments) {
            // Update Description
            final String payDesc = creditPayment.getDescription();
            if (payDesc != null) {
              final String invoiceDocNoMsg = OBMessageUtils.messageBD("APRM_CreditUsedinInvoice");
              if (invoiceDocNoMsg != null) {
                final StringBuffer newDesc = new StringBuffer();
                for (final String line : payDesc.split("\n")) {
                  boolean include = true;
                  if (line.startsWith(
                      invoiceDocNoMsg.substring(0, invoiceDocNoMsg.lastIndexOf("%s")))) {
                    for (final String docNo : invoiceDocNos) {
                      if (line.indexOf(docNo) > 0) {
                        include = false;
                        break;
                      }
                    }
                  }
                  if (include) {
                    newDesc.append(line);
                    if (!"".equals(line)) {
                      newDesc.append("\n");
                    }
                  }
                }
                // Truncate Description to keep length as 255
                creditPayment.setDescription(
                    newDesc.toString().length() > 255 ? newDesc.toString().substring(0, 255)
                        : newDesc.toString());
              }
            }
          }

          payment.getFINPaymentCreditList().clear();
          if (payment.isReceipt() || strAction.equals("R")) {
            payment.setGeneratedCredit(BigDecimal.ZERO);
          }
          if (strAction.equals("R")) {
            payment.setUsedCredit(BigDecimal.ZERO);
          }
        } finally {
          OBDal.getInstance().flush();
          OBContext.restorePreviousMode();
        }

      } else if (strAction.equals("V")) {
        // Void
        OBContext.setAdminMode();
        try {
          if (payment.isProcessed()) {
            // Already Posted Document
            if ("Y".equals(payment.getPosted())) {
              msg = OBMessageUtils.messageBD("PostedDocument: " + payment.getDocumentNo());
              throw new OBException(msg);
            }
            // Transaction exists
            if (hasTransaction(payment)) {
              msg = OBMessageUtils.messageBD("APRM_TransactionExists");
              throw new OBException(msg);
            }
            // Payment with generated credit already used on other payments.
            if (payment.getGeneratedCredit().compareTo(BigDecimal.ZERO) == 1
                && payment.getUsedCredit().compareTo(BigDecimal.ZERO) == 1) {
              msg = OBMessageUtils.messageBD("APRM_PaymentGeneratedCreditIsUsed");
              throw new OBException(msg);
            }
            // Payment not in Awaiting Execution
            boolean restorePaidAmounts = (FIN_Utility
                .seqnumberpaymentstatus(payment.getStatus())) < (FIN_Utility
                    .seqnumberpaymentstatus(FIN_Utility.invoicePaymentStatus(payment)));
            if (!restorePaidAmounts) {
              msg = OBMessageUtils.messageBD("APRM_PaymentNotRPAE_NotVoid");
              throw new OBException(msg);
            }

            /*
             * Void the payment
             */
            payment.setStatus("RPVOID");

            /*
             * Cancel all payment schedule details related to the payment
             */
            final List<FIN_PaymentScheduleDetail> removedPDS = new ArrayList<FIN_PaymentScheduleDetail>();
            Set<String> invoiceDocNos = new HashSet<String>();
            for (final FIN_PaymentDetail paymentDetail : payment.getFINPaymentDetailList()) {
              for (final FIN_PaymentScheduleDetail paymentScheduleDetail : paymentDetail
                  .getFINPaymentScheduleDetailList()) {
                Boolean invoicePaidold = paymentScheduleDetail.isInvoicePaid();
                if (invoicePaidold | paymentScheduleDetail.getInvoicePaymentSchedule() == null) {
                  paymentScheduleDetail.setInvoicePaid(false);
                }
                BigDecimal outStandingAmt = BigDecimal.ZERO;

                if (paymentScheduleDetail.getInvoicePaymentSchedule() != null) {
                  // Related to invoices
                  for (final FIN_PaymentScheduleDetail invScheDetail : paymentScheduleDetail
                      .getInvoicePaymentSchedule()
                      .getFINPaymentScheduleDetailInvoicePaymentScheduleList()) {
                    if (invScheDetail.isCanceled()) {
                      continue;
                    }
                    if (invScheDetail.getPaymentDetails() == null) {
                      outStandingAmt = outStandingAmt.add(invScheDetail.getAmount())
                          .add(invScheDetail.getWriteoffAmount());
                      removedPDS.add(invScheDetail);
                    } else if (invScheDetail.equals(paymentScheduleDetail)) {
                      outStandingAmt = outStandingAmt.add(invScheDetail.getAmount())
                          .add(invScheDetail.getWriteoffAmount());
                      paymentScheduleDetail.setCanceled(true);
                    }
                    invoiceDocNos.add(paymentScheduleDetail.getInvoicePaymentSchedule()
                        .getInvoice()
                        .getDocumentNo());
                  }
                  // Create merged Payment Schedule Detail with the pending to be paid amount
                  if (outStandingAmt.compareTo(BigDecimal.ZERO) != 0) {
                    final FIN_PaymentScheduleDetail mergedScheduleDetail = dao
                        .getNewPaymentScheduleDetail(
                            paymentScheduleDetail.getInvoicePaymentSchedule()
                                .getInvoice()
                                .getOrganization(),
                            outStandingAmt);
                    mergedScheduleDetail.setInvoicePaymentSchedule(
                        paymentScheduleDetail.getInvoicePaymentSchedule());
                    mergedScheduleDetail
                        .setOrderPaymentSchedule(paymentScheduleDetail.getOrderPaymentSchedule());
                    mergedScheduleDetail
                        .setBusinessPartner(paymentScheduleDetail.getBusinessPartner());
                    OBDal.getInstance().save(mergedScheduleDetail);
                  }
                } else if (paymentScheduleDetail.getOrderPaymentSchedule() != null) {
                  // Related to orders
                  for (final FIN_PaymentScheduleDetail ordScheDetail : paymentScheduleDetail
                      .getOrderPaymentSchedule()
                      .getFINPaymentScheduleDetailOrderPaymentScheduleList()) {
                    if (ordScheDetail.isCanceled()) {
                      continue;
                    }
                    if (ordScheDetail.getPaymentDetails() == null) {
                      outStandingAmt = outStandingAmt.add(ordScheDetail.getAmount())
                          .add(ordScheDetail.getWriteoffAmount());
                      removedPDS.add(ordScheDetail);
                    } else if (ordScheDetail.equals(paymentScheduleDetail)) {
                      outStandingAmt = outStandingAmt.add(ordScheDetail.getAmount())
                          .add(ordScheDetail.getWriteoffAmount());
                      paymentScheduleDetail.setCanceled(true);
                    }
                  }
                  // Create merged Payment Schedule Detail with the pending to be paid amount
                  if (outStandingAmt.compareTo(BigDecimal.ZERO) != 0) {
                    final FIN_PaymentScheduleDetail mergedScheduleDetail = dao
                        .getNewPaymentScheduleDetail(paymentScheduleDetail.getOrderPaymentSchedule()
                            .getOrder()
                            .getOrganization(), outStandingAmt);
                    mergedScheduleDetail
                        .setOrderPaymentSchedule(paymentScheduleDetail.getOrderPaymentSchedule());
                    mergedScheduleDetail
                        .setBusinessPartner(paymentScheduleDetail.getBusinessPartner());
                    OBDal.getInstance().save(mergedScheduleDetail);
                  }
                } else if (paymentDetail.getGLItem() != null) {
                  paymentScheduleDetail.setCanceled(true);
                } else if (paymentScheduleDetail.getOrderPaymentSchedule() == null
                    && paymentScheduleDetail.getInvoicePaymentSchedule() == null) {
                  // Credit payment
                  payment.setGeneratedCredit(
                      payment.getGeneratedCredit().subtract(paymentScheduleDetail.getAmount()));
                  removedPDS.add(paymentScheduleDetail);
                }

                OBDal.getInstance().save(payment);
                OBDal.getInstance().flush();
              }
              paymentDetail.getFINPaymentScheduleDetailList().removeAll(removedPDS);
              for (FIN_PaymentScheduleDetail removedPD : removedPDS) {
                if (removedPD.getOrderPaymentSchedule() != null) {
                  removedPD.getOrderPaymentSchedule()
                      .getFINPaymentScheduleDetailOrderPaymentScheduleList()
                      .remove(removedPD);
                  OBDal.getInstance().save(removedPD.getOrderPaymentSchedule());
                }
                if (removedPD.getInvoicePaymentSchedule() != null) {
                  removedPD.getInvoicePaymentSchedule()
                      .getFINPaymentScheduleDetailInvoicePaymentScheduleList()
                      .remove(removedPD);
                  OBDal.getInstance().save(removedPD.getInvoicePaymentSchedule());
                }
                OBDal.getInstance().remove(removedPD);
              }
              OBDal.getInstance().flush();
              removedPDS.clear();

            }
            if (payment.getGeneratedCredit().compareTo(BigDecimal.ZERO) == 0
                && payment.getUsedCredit().compareTo(BigDecimal.ZERO) == 1) {
              undoUsedCredit(payment, invoiceDocNos);
            }
            payment.getFINPaymentCreditList().clear();
            payment.setUsedCredit(BigDecimal.ZERO);
          }
          OBDal.getInstance().flush();
        } finally {
          OBContext.restorePreviousMode();
        }
      }
    } catch (final OBException obException) {
      throw obException;
    } catch (final Exception genericException) {
      log4j.error(genericException.getMessage(), genericException);
      msg = OBMessageUtils.translateError(FIN_Utility.getExceptionMessage(genericException))
          .getMessage();
      throw new OBException(msg);
    }
  }

  private void updateAuditInformation(BaseOBObject baseOBject, final Date now) {
    baseOBject.set(CREATION_DATE_PROPERTY, now);
    baseOBject.set(CREATED_BY_PROPERTY, OBContext.getOBContext().getUser());
  }

  /**
   * Method used to update the credit used when the user doing invoice processing or payment
   * processing
   * 
   * @param amount
   *          Payment amount
   */
  private void updateCustomerCredit(BusinessPartner businessPartner, BigDecimal amount,
      boolean add) {
    OBDal.getInstance().refresh(businessPartner);
    BigDecimal creditUsed = businessPartner.getCreditUsed();
    if (add) {
      creditUsed = creditUsed.add(amount);
    } else {
      creditUsed = creditUsed.subtract(amount);
    }
    businessPartner.setCreditUsed(creditUsed);
    OBDal.getInstance().save(businessPartner);
    OBDal.getInstance().flush();
  }

  private void increaseCustomerCredit(BusinessPartner businessPartner, BigDecimal amount) {
    updateCustomerCredit(businessPartner, amount, true);
  }

  private void decreaseCustomerCredit(BusinessPartner businessPartner, BigDecimal amount) {
    updateCustomerCredit(businessPartner, amount, false);
  }

  private void triggerAutomaticFinancialAccountTransaction(FIN_Payment payment) throws Exception {
    FIN_FinaccTransaction transaction = TransactionsDao.createFinAccTransaction(payment);
    processTransaction("P", transaction);
  }

  private static boolean hasTransaction(FIN_Payment payment) {
    OBCriteria<FIN_FinaccTransaction> transaction = OBDal.getInstance()
        .createCriteria(FIN_FinaccTransaction.class);
    transaction.add(Restrictions.eq(FIN_FinaccTransaction.PROPERTY_FINPAYMENT, payment));
    List<FIN_FinaccTransaction> list = transaction.list();
    if (list == null || list.size() == 0) {
      return false;
    }
    return true;
  }

  private void updateUsedCredit(FIN_Payment newPayment, String selectedCreditLineIds) {
    if (newPayment.getFINPaymentCreditList().isEmpty()) {
      // We process the payment from the Payment In/Out window (not from the Process Invoice flow)
      final BigDecimal usedAmount = newPayment.getUsedCredit();
      final BusinessPartner bp = newPayment.getBusinessPartner();
      final boolean isReceipt = newPayment.isReceipt();
      final Organization Org = newPayment.getOrganization();
      List<FIN_Payment> selectedCreditPayments = null;
      if (selectedCreditLineIds != null) {
        selectedCreditPayments = FIN_Utility.getOBObjectList(FIN_Payment.class,
            selectedCreditLineIds);
      }
      final OBCriteria<FIN_Payment> reversepayment = OBDal.getInstance()
          .createCriteria(FIN_Payment.class);
      reversepayment.add(Restrictions.eq(FIN_Payment.PROPERTY_REVERSEDPAYMENT, newPayment));
      final FIN_Payment reversepaymnt = (FIN_Payment) reversepayment.uniqueResult();

      List<FIN_Payment> creditPayments;
      if (reversepaymnt == null && selectedCreditLineIds == null) {
        // Normal scenario
        creditPayments = dao.getCustomerPaymentsWithCredit(Org, bp, isReceipt);
      } else if (selectedCreditLineIds != null) {
        creditPayments = selectedCreditPayments;
      } else {
        // If it is a reverse payment use its original payment
        creditPayments = new ArrayList<FIN_Payment>(1);
        creditPayments.add(reversepaymnt);
      }

      BigDecimal pendingToAllocateAmount = usedAmount;
      for (FIN_Payment creditPayment : creditPayments) {
        BigDecimal availableAmount = creditPayment.getGeneratedCredit()
            .subtract(creditPayment.getUsedCredit());
        if (pendingToAllocateAmount.compareTo(availableAmount) == 1) {
          creditPayment.setUsedCredit(creditPayment.getUsedCredit().add(availableAmount));
          pendingToAllocateAmount = pendingToAllocateAmount.subtract(availableAmount);
          linkCreditPayment(newPayment, availableAmount, creditPayment);
          OBDal.getInstance().save(creditPayment);
        } else {
          creditPayment.setUsedCredit(creditPayment.getUsedCredit().add(pendingToAllocateAmount));
          linkCreditPayment(newPayment, pendingToAllocateAmount, creditPayment);
          OBDal.getInstance().save(creditPayment);
          break;
        }
      }
    }
  }

  public static void linkCreditPayment(FIN_Payment newPayment, BigDecimal usedAmount,
      FIN_Payment creditPayment) {
    final FIN_Payment_Credit creditInfo = OBProvider.getInstance().get(FIN_Payment_Credit.class);
    creditInfo.setPayment(newPayment);
    creditInfo.setAmount(usedAmount);
    creditInfo.setCurrency(newPayment.getCurrency());
    creditInfo.setCreditPaymentUsed(creditPayment);
    creditInfo.setOrganization(newPayment.getOrganization());
    creditInfo.setClient(newPayment.getClient());
    newPayment.getFINPaymentCreditList().add(creditInfo);
  }

  private void undoUsedCredit(FIN_Payment myPayment, Set<String> invoiceDocNos) {
    final List<FIN_Payment> payments = new ArrayList<FIN_Payment>();
    for (final FIN_Payment_Credit pc : myPayment.getFINPaymentCreditList()) {
      final FIN_Payment creditPaymentUsed = pc.getCreditPaymentUsed();
      creditPaymentUsed.setUsedCredit(creditPaymentUsed.getUsedCredit().subtract(pc.getAmount()));
      payments.add(creditPaymentUsed);
    }

    for (final FIN_Payment payment : payments) {
      // Update Description
      final String payDesc = payment.getDescription();
      if (payDesc != null) {
        final String invoiceDocNoMsg = OBMessageUtils.messageBD("APRM_CreditUsedinInvoice");
        if (invoiceDocNoMsg != null) {
          final StringBuffer newDesc = new StringBuffer();
          for (final String line : payDesc.split("\n")) {
            boolean include = true;
            if (line.startsWith(invoiceDocNoMsg.substring(0, invoiceDocNoMsg.lastIndexOf("%s")))) {
              for (final String docNo : invoiceDocNos) {
                if (line.indexOf(docNo) > 0) {
                  include = false;
                  break;
                }
              }
            }
            if (include) {
              newDesc.append(line);
              if (!"".equals(line)) {
                newDesc.append("\n");
              }
            }
          }
          // Truncate Description to keep length as 255
          payment.setDescription(
              newDesc.toString().length() > 255 ? newDesc.toString().substring(0, 255)
                  : newDesc.toString());
        }
      }
    }
  }

  private List<ConversionRateDoc> getConversionRateDocument(FIN_Payment payment) {
    OBContext.setAdminMode();
    try {
      OBCriteria<ConversionRateDoc> obc = OBDal.getInstance()
          .createCriteria(ConversionRateDoc.class);
      obc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_CURRENCY, payment.getCurrency()));
      obc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_TOCURRENCY,
          payment.getAccount().getCurrency()));
      obc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_PAYMENT, payment));
      return obc.list();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private List<ConversionRateDoc> getConversionRateDocumentForInvoice(Invoice invoice) {
    OBContext.setAdminMode(true);
    try {
      OBCriteria<ConversionRateDoc> obc = OBDal.getInstance()
          .createCriteria(ConversionRateDoc.class);
      obc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_CURRENCY, invoice.getCurrency()));
      obc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_TOCURRENCY,
          invoice.getBusinessPartner().getCurrency()));
      obc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_INVOICE, invoice));
      return obc.list();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private ConversionRateDoc insertConversionRateDocument(FIN_Payment payment) {
    OBContext.setAdminMode();
    try {
      ConversionRateDoc newConversionRateDoc = OBProvider.getInstance()
          .get(ConversionRateDoc.class);
      newConversionRateDoc.setOrganization(payment.getOrganization());
      newConversionRateDoc.setCurrency(payment.getCurrency());
      newConversionRateDoc.setToCurrency(payment.getAccount().getCurrency());
      newConversionRateDoc.setRate(payment.getFinancialTransactionConvertRate());
      newConversionRateDoc.setForeignAmount(payment.getFinancialTransactionAmount());
      newConversionRateDoc.setPayment(payment);
      newConversionRateDoc.setClient(payment.getClient());
      OBDal.getInstance().save(newConversionRateDoc);
      OBDal.getInstance().flush();
      return newConversionRateDoc;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * It calls the Transaction Process for the given transaction and action.
   * 
   * @param strAction
   *          String with the action of the process. {P, D, R}
   * @param transaction
   *          FIN_FinaccTransaction that needs to be processed.
   * @throws OBException
   */
  private void processTransaction(String strAction, FIN_FinaccTransaction transaction)
      throws OBException {
    FIN_TransactionProcess.doTransactionProcess(strAction, transaction);
  }

  public boolean getDocumentConfirmation(ConnectionProvider conn, String strRecordId) {
    // Checks if this step is configured to generate accounting for the selected financial account
    boolean confirmation = false;
    OBContext.setAdminMode();
    try {
      FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, strRecordId);
      OBCriteria<FinAccPaymentMethod> obCriteria = OBDal.getInstance()
          .createCriteria(FinAccPaymentMethod.class);
      obCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, payment.getAccount()));
      obCriteria.add(
          Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, payment.getPaymentMethod()));
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
    } catch (Exception e) {
      // TODO no logging... ??
      return confirmation;
    } finally {
      OBContext.restorePreviousMode();
    }
    return confirmation;
  }

  private BigDecimal getConversionRate(String strOrgId, String strFromCurrencyId,
      String strToCurrencyId, Date conversionDate) {
    BigDecimal exchangeRate = BigDecimal.ZERO;
    // Apply default conversion rate
    int conversionRatePrecision = FIN_Utility
        .getConversionRatePrecision(RequestContext.get().getVariablesSecureApp());
    Organization organization = OBDal.getInstance().get(Organization.class, strOrgId);
    Currency fromCurrency = OBDal.getInstance().get(Currency.class, strFromCurrencyId);
    Currency toCurrency = OBDal.getInstance().get(Currency.class, strToCurrencyId);
    final ConversionRate conversionRate = FIN_Utility.getConversionRate(fromCurrency, toCurrency,
        conversionDate, organization);
    if (conversionRate != null) {
      exchangeRate = conversionRate.getMultipleRateBy()
          .setScale(conversionRatePrecision, RoundingMode.HALF_UP);
    } else {
      exchangeRate = BigDecimal.ZERO;
    }
    return exchangeRate;
  }

  /**
   * Removes the Payment Proposal Lines linked to the Payment Schedule Detail. If the payment
   * proposal is already processed, the record is unprocessed first, then affected lines are
   * deleted, and finally it gets back to processed status. This is done to avoid trigger validation
   * errors.
   * 
   * A common scenario where you want to delete a payment proposal line is when the payment schedule
   * detail has been deleted
   * 
   * @param psd
   *          Payment Schedule Detail
   * @return number of Payment Proposal Lines removed
   */
  public static int removePaymentProposalLines(FIN_PaymentScheduleDetail psd) {
    int proposalLinesRemoved = 0;
    for (FIN_PaymentPropDetail ppd : psd.getFINPaymentPropDetailList()) {
      final FIN_PaymentProposal paymentProposal = ppd.getFinPaymentProposal();
      if (paymentProposal.isProcessed()) {
        // Hack to delete Payment Proposal lines when you reactivate a payment linked to
        // a processed payment proposal.
        paymentProposal.setProcessed(false);
        OBDal.getInstance().save(paymentProposal);
        OBDal.getInstance().remove(ppd);
        OBDal.getInstance().flush();
        paymentProposal.setProcessed(true);
      } else {
        OBDal.getInstance().remove(ppd);
      }
      proposalLinesRemoved++;
    }
    return proposalLinesRemoved;
  }

}
