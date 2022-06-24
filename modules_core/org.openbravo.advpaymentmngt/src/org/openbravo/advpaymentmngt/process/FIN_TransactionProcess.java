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
 * All portions are Copyright (C) 2010-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.advpaymentmngt.process;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.APRM_FinaccTransactionV;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.scheduling.ProcessBundle;

public class FIN_TransactionProcess implements org.openbravo.scheduling.Process {
  /** Transaction type - Financial Account */
  public static final String TRXTYPE_BPDeposit = "BPD";
  public static final String TRXTYPE_BPWithdrawal = "BPW";
  public static final String TRXTYPE_BankFee = "BF";
  static Logger log4j = LogManager.getLogger();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(OBMessageUtils.messageBD("Success"));

    try {
      // retrieve custom params
      final String strAction = (String) bundle.getParams().get("action");

      // retrieve standard params
      String recordID = (String) bundle.getParams().get("Fin_FinAcc_Transaction_ID");
      if (recordID == null) {
        recordID = (String) bundle.getParams().get("Fin_Finacc_Transaction_ID");
      }
      final FIN_FinaccTransaction transaction = OBDal.getInstance()
          .get(FIN_FinaccTransaction.class, recordID);
      transactionProcess(strAction, transaction);
      bundle.setResult(msg);
    } catch (Exception e) {
      log4j.error(e.getMessage());
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD("Error"));
      msg.setMessage(FIN_Utility.getExceptionMessage(e));
      bundle.setResult(msg);
      OBDal.getInstance().getConnection().rollback();
    }
  }

  public static void doTransactionProcess(String strAction, FIN_FinaccTransaction transaction)
      throws OBException {
    FIN_TransactionProcess ftp = WeldUtils
        .getInstanceFromStaticBeanManager(FIN_TransactionProcess.class);
    ftp.transactionProcess(strAction, transaction);
  }

  private void transactionProcess(String strAction, FIN_FinaccTransaction transaction)
      throws OBException {
    String msg = "";
    try {
      OBContext.setAdminMode(false);
      if (strAction.equals("P") && !transaction.isProcessed()) {
        // ***********************
        // Process Transaction
        // ***********************

        boolean orgLegalWithAccounting = FIN_Utility.periodControlOpened(
            FIN_FinaccTransaction.TABLE_NAME, transaction.getId(),
            FIN_FinaccTransaction.TABLE_NAME + "_ID", "LE");
        boolean documentEnabled = getDocumentConfirmation(transaction.getId());
        if (documentEnabled && !FIN_Utility.isPeriodOpen(transaction.getClient().getId(),
            AcctServer.DOCTYPE_FinAccTransaction, transaction.getOrganization().getId(),
            OBDateUtils.formatDate(transaction.getDateAcct())) && orgLegalWithAccounting) {
          msg = OBMessageUtils.messageBD("PeriodNotAvailable");
          throw new OBException(msg);
        }
        transaction.setProcessed(true);
        FIN_Payment payment = transaction.getFinPayment();
        if (payment != null) {
          if (transaction.getBusinessPartner() == null) {
            transaction.setBusinessPartner(payment.getBusinessPartner());
          }
          AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
          if (StringUtils.equals(payment.getStatus(), dao.PAYMENT_STATUS_AWAITING_EXECUTION)
              && dao.isAutomatedExecutionPayment(transaction.getAccount(),
                  payment.getPaymentMethod(), payment.isReceipt())) {
            msg = OBMessageUtils.messageBD("APRM_AutomaticExecutionProcess");
            throw new OBException(msg);
          }

          payment.setStatus(payment.isReceipt() ? "RDNC" : "PWNC");
          transaction.setStatus(payment.isReceipt() ? "RDNC" : "PWNC");
          if (transaction.getPaymentAmount().compareTo(BigDecimal.ZERO) > 0) {
            transaction.setTransactionType(TRXTYPE_BPWithdrawal);
          } else {
            transaction.setTransactionType(TRXTYPE_BPDeposit);
          }
          OBDal.getInstance().save(payment);
          if (transaction.getDescription() == null || "".equals(transaction.getDescription())) {
            transaction.setDescription(payment.getDescription());
          }
          Boolean invoicePaidold = false;
          for (FIN_PaymentDetail pd : payment.getFINPaymentDetailList()) {
            for (FIN_PaymentScheduleDetail psd : pd.getFINPaymentScheduleDetailList()) {
              invoicePaidold = psd.isInvoicePaid();
              if (!invoicePaidold) {
                if ((FIN_Utility.invoicePaymentStatus(payment).equals(payment.getStatus()))) {
                  psd.setInvoicePaid(true);
                }
                if (psd.isInvoicePaid()) {
                  FIN_Utility.updatePaymentAmounts(psd);
                  FIN_Utility.updateBusinessPartnerCredit(payment);
                }
                OBDal.getInstance().save(psd);
              }
            }
          }

          if (!StringUtils.equals(transaction.getCurrency().getId(),
              payment.getCurrency().getId())) {
            transaction.setForeignCurrency(payment.getCurrency());
            transaction.setForeignConversionRate(payment.getFinancialTransactionConvertRate());
            transaction.setForeignAmount(payment.getAmount());
          }

        } else {
          transaction.setStatus(
              transaction.getDepositAmount().compareTo(transaction.getPaymentAmount()) > 0 ? "RDNC"
                  : "PWNC");
        }
        if (transaction.getForeignCurrency() != null
            && !transaction.getCurrency().equals(transaction.getForeignCurrency())
            && getConversionRateDocument(transaction).size() == 0) {
          insertConversionRateDocument(transaction);
        }

        FIN_FinancialAccount financialAccount = OBDal.getInstance()
            .getObjectLockForNoKeyUpdate(transaction.getAccount());
        financialAccount.setCurrentBalance(financialAccount.getCurrentBalance()
            .add(transaction.getDepositAmount().subtract(transaction.getPaymentAmount())));
        transaction.setAprmProcessed("R");
        OBDal.getInstance().save(transaction);

      } else if (strAction.equals("R") && transaction.isProcessed()) {
        // ***********************
        // Reactivate Transaction
        // ***********************
        // Already Posted Document
        if ("Y".equals(transaction.getPosted())) {
          msg = OBMessageUtils.messageBD("PostedDocument: " + transaction.getIdentifier());
          throw new OBException(msg);
        }
        // Already Reconciled
        if (transaction.getReconciliation() != null || "RPPC".equals(transaction.getStatus())) {
          msg = OBMessageUtils.messageBD("APRM_ReconciledDocument: " + transaction.getIdentifier());
          throw new OBException(msg);
        }
        // Payment Method associated to payment not longer in financial account
        final FIN_Payment payment = transaction.getFinPayment();
        if (payment != null && FIN_Utility.invoicePaymentStatus(payment) == null) {
          msg = String.format(OBMessageUtils.messageBD("APRM_NoPaymentMethod"),
              payment.getPaymentMethod().getIdentifier(), payment.getDocumentNo(),
              payment.getAccount().getName());
          throw new OBException(msg);
        }
        // Remove conversion rate at document level for the given transaction
        OBContext.setAdminMode();
        try {
          OBCriteria<ConversionRateDoc> obc = OBDal.getInstance()
              .createCriteria(ConversionRateDoc.class);
          obc.add(
              Restrictions.eq(ConversionRateDoc.PROPERTY_FINANCIALACCOUNTTRANSACTION, transaction));
          boolean dataRemoved = false;
          for (ConversionRateDoc conversionRateDoc : obc.list()) {
            dataRemoved = true;
            OBDal.getInstance().remove(conversionRateDoc);
          }
          if (dataRemoved) {
            OBDal.getInstance().flush();
          }
        } finally {
          OBContext.restorePreviousMode();
        }
        transaction.setProcessed(false);

        OBDal.getInstance().save(transaction);
        if (payment != null) {
          Boolean invoicePaidold = false;
          for (FIN_PaymentDetail pd : payment.getFINPaymentDetailList()) {
            for (FIN_PaymentScheduleDetail psd : pd.getFINPaymentScheduleDetailList()) {
              invoicePaidold = psd.isInvoicePaid();
              if (invoicePaidold) {
                boolean restore = (FIN_Utility
                    .seqnumberpaymentstatus(payment.getStatus())) == (FIN_Utility
                        .seqnumberpaymentstatus(FIN_Utility.invoicePaymentStatus(payment)));
                if (restore) {
                  FIN_Utility.restorePaidAmounts(psd);
                }
              }
            }
          }
          payment.setStatus(payment.isReceipt() ? "RPR" : "PPM");
          transaction.setStatus(payment.isReceipt() ? "RPR" : "PPM");
          OBDal.getInstance().save(payment);
        } else {
          transaction.setStatus(
              transaction.getDepositAmount().compareTo(transaction.getPaymentAmount()) > 0 ? "RPR"
                  : "PPM");
        }
        FIN_FinancialAccount financialAccount = OBDal.getInstance()
            .getObjectLockForNoKeyUpdate(transaction.getAccount());
        financialAccount.setCurrentBalance(financialAccount.getCurrentBalance()
            .subtract(transaction.getDepositAmount())
            .add(transaction.getPaymentAmount()));
        transaction.setAprmProcessed("P");
        OBDal.getInstance().save(transaction);
      }
    } finally {
      OBDal.getInstance().flush();
      OBContext.restorePreviousMode();
    }
  }

  private List<ConversionRateDoc> getConversionRateDocument(FIN_FinaccTransaction transaction) {
    OBContext.setAdminMode();
    try {
      OBCriteria<ConversionRateDoc> obc = OBDal.getInstance()
          .createCriteria(ConversionRateDoc.class);
      obc.add(
          Restrictions.eq(ConversionRateDoc.PROPERTY_CURRENCY, transaction.getForeignCurrency()));
      obc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_TOCURRENCY, transaction.getCurrency()));
      obc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_FINANCIALACCOUNTTRANSACTION, transaction));
      return obc.list();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private ConversionRateDoc insertConversionRateDocument(FIN_FinaccTransaction transaction) {
    OBContext.setAdminMode();
    try {
      ConversionRateDoc newConversionRateDoc = OBProvider.getInstance()
          .get(ConversionRateDoc.class);
      newConversionRateDoc.setOrganization(transaction.getOrganization());
      newConversionRateDoc.setCurrency(transaction.getForeignCurrency());
      newConversionRateDoc.setToCurrency(transaction.getCurrency());
      newConversionRateDoc.setRate(transaction.getForeignConversionRate());
      newConversionRateDoc.setForeignAmount(transaction.getForeignAmount());
      newConversionRateDoc.setFinancialAccountTransaction(
          OBDal.getInstance().get(APRM_FinaccTransactionV.class, transaction.getId()));
      OBDal.getInstance().save(newConversionRateDoc);
      OBDal.getInstance().flush();
      return newConversionRateDoc;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /*
   * Checks if this step is configured to generate accounting for the selected financial account
   */
  public boolean getDocumentConfirmation(ConnectionProvider conn, String strRecordId) {
    return getDocumentConfirmation(strRecordId);
  }

  private boolean getDocumentConfirmation(String strRecordId) {
    boolean confirmation = false;
    OBContext.setAdminMode();
    try {
      FIN_FinaccTransaction transaction = OBDal.getInstance()
          .get(FIN_FinaccTransaction.class, strRecordId);
      List<FIN_FinancialAccountAccounting> accounts = transaction.getAccount()
          .getFINFinancialAccountAcctList();
      FIN_Payment payment = transaction.getFinPayment();
      if (payment != null) {
        OBCriteria<FinAccPaymentMethod> obCriteria = OBDal.getInstance()
            .createCriteria(FinAccPaymentMethod.class);
        obCriteria
            .add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, transaction.getAccount()));
        obCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD,
            payment.getPaymentMethod()));
        obCriteria.setFilterOnReadableClients(false);
        obCriteria.setFilterOnReadableOrganization(false);
        List<FinAccPaymentMethod> lines = obCriteria.list();
        for (FIN_FinancialAccountAccounting account : accounts) {
          if (confirmation) {
            return confirmation;
          }
          if (payment.isReceipt()) {
            if (("INT").equals(lines.get(0).getUponDepositUse())
                && account.getInTransitPaymentAccountIN() != null) {
              confirmation = true;
            } else if (("DEP").equals(lines.get(0).getUponDepositUse())
                && account.getDepositAccount() != null) {
              confirmation = true;
            } else if (("CLE").equals(lines.get(0).getUponDepositUse())
                && account.getClearedPaymentAccount() != null) {
              confirmation = true;
            } else if (null == (lines.get(0).getUponDepositUse())
                && null == (lines.get(0).getINUponClearingUse())
                && transaction.getAccount() != payment.getAccount()) {
              confirmation = true;
            }
          } else {
            if (("INT").equals(lines.get(0).getUponWithdrawalUse())
                && account.getFINOutIntransitAcct() != null) {
              confirmation = true;
            } else if (("WIT").equals(lines.get(0).getUponWithdrawalUse())
                && account.getWithdrawalAccount() != null) {
              confirmation = true;
            } else if (("CLE").equals(lines.get(0).getUponWithdrawalUse())
                && account.getClearedPaymentAccountOUT() != null) {
              confirmation = true;
            }
          }
        }

      } else {
        for (FIN_FinancialAccountAccounting account : accounts) {
          if (confirmation) {
            return confirmation;
          }
          if ((TRXTYPE_BPDeposit.equals(transaction.getTransactionType())
              && account.getDepositAccount() != null)
              || (TRXTYPE_BPWithdrawal.equals(transaction.getTransactionType())
                  && account.getWithdrawalAccount() != null)
              || (TRXTYPE_BankFee.equals(transaction.getTransactionType())
                  && account.getWithdrawalAccount() != null)) {
            confirmation = true;
          }
        }
      }
    } catch (Exception e) {
      return confirmation;
    } finally {
      OBContext.restorePreviousMode();
    }
    return confirmation;
  }
}
