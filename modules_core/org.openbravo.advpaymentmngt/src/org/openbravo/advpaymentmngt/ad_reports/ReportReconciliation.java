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
package org.openbravo.advpaymentmngt.ad_reports;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import jakarta.persistence.criteria.JoinType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.advpaymentmngt.dao.MatchTransactionDao;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatement;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;

public class ReportReconciliation extends HttpSecureAppServlet {
  final static String DETAIL = "DETAIL";
  final static String SUMMARY = "SUMMARY";
  private static final long serialVersionUID = 1L;
  private static final Logger log = LogManager.getLogger();

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strFinReconciliationID = vars.getGlobalVariable("inpfinReconciliationId", "");
      String strFinFinancialAccountId = vars.getGlobalVariable("inpfinFinancialAccountId", "");
      String strLastFieldChanged = vars.getGlobalVariable("inpLastFieldChanged", "");
      String strDateTo = vars.getGlobalVariable("inpdateto", "");
      printPageDataPDF(request, response, vars, strFinReconciliationID,
          OBDal.getInstance().get(FIN_FinancialAccount.class, strFinFinancialAccountId).getName(),
          strDateTo, strLastFieldChanged.toLowerCase().contains("detail") ? DETAIL : SUMMARY);
    }
  }

  public void post(HttpServletRequest request, HttpServletResponse response, String strReportType)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strFinReconciliationID = vars.getGlobalVariable("inpfinReconciliationId", "");
      String strFinFinancialAccountId = vars.getGlobalVariable("inpfinFinancialAccountId", "");
      String strDateTo = vars.getGlobalVariable("inpdateto", "");
      printPageDataPDF(request, response, vars, strFinReconciliationID,
          OBDal.getInstance().get(FIN_FinancialAccount.class, strFinFinancialAccountId).getName(),
          strDateTo, strReportType);
    }
  }

  private void printPageDataPDF(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strFinReconciliationId, String strFinFinancialAccountName,
      String strDateTo, String strReportType) throws IOException, ServletException {
    log4j.debug("Output: Reconciliation PDF report");

    String reportType = (strReportType == null) ? "DETAIL" : strReportType;

    String strMainReportName = "@basedesign@/org/openbravo/advpaymentmngt/ad_reports/ReportReconciliation.jrxml";
    FIN_Reconciliation reconciliation = null;
    OBContext.setAdminMode(true);
    try {
      reconciliation = OBDal.getInstance().get(FIN_Reconciliation.class, strFinReconciliationId);
    } finally {
      OBContext.restorePreviousMode();
    }
    HashMap<String, Object> parameters = new HashMap<String, Object>();

    String strLanguage = vars.getLanguage();
    String strBaseDesign = getBaseDesignPath(strLanguage);

    JasperReport subReportOutstandingPayment;
    JasperReport subReportOutstandingDeposit;
    JasperReport subReportUnreconciledBankStatementLines;
    try {
      subReportOutstandingPayment = ReportingUtils.getTranslatedJasperReport(this,
          strBaseDesign + "/org/openbravo/advpaymentmngt/ad_reports/OutstandingPayment.jrxml",
          vars.getLanguage());

      subReportOutstandingDeposit = ReportingUtils.getTranslatedJasperReport(this,
          strBaseDesign + "/org/openbravo/advpaymentmngt/ad_reports/OutstandingDeposit.jrxml",
          vars.getLanguage());

      subReportUnreconciledBankStatementLines = ReportingUtils.getTranslatedJasperReport(this,
          strBaseDesign
              + "/org/openbravo/advpaymentmngt/ad_reports/UnreconciledBankStatement.jrxml",
          vars.getLanguage());

    } catch (JRException e) {
      throw new ServletException(e.getMessage());
    }

    String dateFormat = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("dateFormat.java");
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    List<BigDecimal> amtPayDep = getOutstandingPaymentAndDepositTotal(reconciliation);
    BigDecimal accountBalanceOB = getBalanceOfAccount(reconciliation);
    BigDecimal totalOutPayment = amtPayDep.get(0);
    BigDecimal totalOutDeposit = amtPayDep.get(1);
    BigDecimal totalUnreconciledBs = getUnreconciledBankStatmentLinesTotal(reconciliation);

    // Parameters
    parameters.put("REPORT_TYPE", reportType);

    // Common
    parameters.put("FINACCOUNT_INFO", strFinFinancialAccountName);
    parameters.put("DATEFORMAT", sdf);
    parameters.put("RECONCILIATION_ID", strFinReconciliationId);
    parameters.put("ACC_BAL_OB", accountBalanceOB);
    parameters.put("ADJ_ACC_BAL_OB",
        accountBalanceOB.add(totalOutPayment).subtract(totalOutDeposit).add(totalUnreconciledBs));

    parameters.put("SHOW_BAND_PAYMENT", (totalOutPayment.compareTo(BigDecimal.ZERO) != 0));
    parameters.put("SHOW_BAND_DEPOSIT", (totalOutDeposit.compareTo(BigDecimal.ZERO) != 0));
    parameters.put("SHOW_BAND_BSLINE", (totalUnreconciledBs.compareTo(BigDecimal.ZERO) != 0));

    // Summary
    parameters.put("AMT_OUT_PAYMENT", totalOutPayment);
    parameters.put("AMT_OUT_DEPOSIT", totalOutDeposit);
    parameters.put("AMT_UNREC_BSLINE", totalUnreconciledBs);

    OBContext.setAdminMode(true);
    try {
      parameters.put("DATE", reconciliation.getEndingDate());
      parameters.put("END_BALANCE", reconciliation.getEndingBalance());
    } finally {
      OBContext.restorePreviousMode();
    }

    parameters.put("SUBREPORT_OUTPAYMENT", subReportOutstandingPayment);
    parameters.put("SUBREPORT_OUTDEPOSIT", subReportOutstandingDeposit);
    parameters.put("SUBREPORT_UNRECBS", subReportUnreconciledBankStatementLines);

    response.setContentType("text/html; charset=UTF-8");
    renderJR(vars, response, strMainReportName, "pdf", parameters, null, null);
  }

  /**
   * Calculates the sum of outstanding payments/deposits applying the following filters:
   * <p>
   * - They belong to the financial account of the reconciliation.
   * <p>
   * - The transaction date must be lower than the ending date of the reconciliation.
   * <p>
   * - They do not belong to any reconciliation.
   *
   * @param recon
   *     Reconciliation
   * @return List with 2 values. The first one is the sum of outstanding payments (transactions) and
   *     the second is the sum of outstanding deposits (transactions).
   */
  private List<BigDecimal> getOutstandingPaymentAndDepositTotal(FIN_Reconciliation recon) {
    List<BigDecimal> outList = new ArrayList<BigDecimal>();
    OBContext.setAdminMode(true);
    try {
      OBCriteria<FIN_FinaccTransaction> obcTrans = OBDal.getInstance()
          .createCriteria(FIN_FinaccTransaction.class);
      obcTrans.addEqual(FIN_FinaccTransaction.PROPERTY_ACCOUNT, recon.getAccount());
      obcTrans.addEqual(FIN_FinaccTransaction.PROPERTY_PROCESSED, true);
      obcTrans.addLessOrEqual(FIN_FinaccTransaction.PROPERTY_TRANSACTIONDATE, recon.getEndingDate());
      List<FIN_Reconciliation> afterReconciliations = MatchTransactionDao
          .getReconciliationListAfterDate(recon);
      if (!afterReconciliations.isEmpty()) {
        obcTrans.addOr((cb, obc) -> cb.isNull(obc.getPath(FIN_FinaccTransaction.PROPERTY_RECONCILIATION)),
            (cb, obc) -> cb.in(obc.getPath(FIN_FinaccTransaction.PROPERTY_RECONCILIATION)).value(afterReconciliations));
      } else {
        obcTrans.addIsNull(FIN_FinaccTransaction.PROPERTY_RECONCILIATION);
      }

      // Usar nueva funcionalidad de múltiples proyecciones
      obcTrans.setMultipleSums(
          FIN_FinaccTransaction.PROPERTY_PAYMENTAMOUNT,
          FIN_FinaccTransaction.PROPERTY_DEPOSITAMOUNT
      );

      @SuppressWarnings("rawtypes")
      List o = obcTrans.list();
      if (o != null && !o.isEmpty()) {
        Object[] resultSet = (Object[]) o.get(0);
        BigDecimal paymentAmt = (resultSet[0] != null) ? (BigDecimal) resultSet[0]
            : BigDecimal.ZERO;
        BigDecimal depositAmt = (resultSet[1] != null) ? (BigDecimal) resultSet[1]
            : BigDecimal.ZERO;
        outList.add(paymentAmt);
        outList.add(depositAmt);
      }

    } finally {
      OBContext.restorePreviousMode();
    }

    return outList;
  }

  /**
   * Calculates the sum of un-reconciled bank statement lines applying the following filters:
   * <p>
   * - They belong to the financial account of the reconciliation.
   * <p>
   * - The transaction date must be lower than the ending date of the reconciliation.
   * <p>
   * - They are not matched with any transaction.
   *
   * @param recon
   *     Reconciliation
   * @return Sum of the un-reconciled bank statement lines.
   */
  private BigDecimal getUnreconciledBankStatmentLinesTotal(FIN_Reconciliation recon) {
    BigDecimal total = BigDecimal.ZERO;
    OBContext.setAdminMode(true);
    try {
      OBCriteria<FIN_BankStatementLine> obcBsl = OBDal.getInstance()
          .createCriteria(FIN_BankStatementLine.class);
      obcBsl.createAlias(FIN_BankStatementLine.PROPERTY_BANKSTATEMENT, "bs");
      obcBsl.createAlias(FIN_BankStatementLine.PROPERTY_FINANCIALACCOUNTTRANSACTION, "tr",
          JoinType.LEFT);
      obcBsl.addLessOrEqual(FIN_BankStatementLine.PROPERTY_TRANSACTIONDATE, recon.getEndingDate());
      List<FIN_Reconciliation> afterReconciliations = MatchTransactionDao
          .getReconciliationListAfterDate(recon);
      if (!afterReconciliations.isEmpty()) {
        obcBsl.addOr((cb, obc) -> cb.isNull(obc.getPath(FIN_BankStatementLine.PROPERTY_FINANCIALACCOUNTTRANSACTION)),
            (cb, obc) -> cb.in(obc.getPath("tr." + FIN_FinaccTransaction.PROPERTY_RECONCILIATION)).value(
                afterReconciliations));
      } else {
        obcBsl.addIsNull(FIN_BankStatementLine.PROPERTY_FINANCIALACCOUNTTRANSACTION);
      }
      obcBsl.addEqual("bs." + FIN_BankStatement.PROPERTY_ACCOUNT, recon.getAccount());
      obcBsl.addEqual("bs." + FIN_BankStatement.PROPERTY_PROCESSED, true);
      obcBsl.setMultipleSums(
          FIN_BankStatementLine.PROPERTY_CRAMOUNT,
          FIN_BankStatementLine.PROPERTY_DRAMOUNT
      );

      @SuppressWarnings("rawtypes")
      List o = obcBsl.list();
      if (o != null && !o.isEmpty()) {
        Object[] resultSet = (Object[]) o.get(0);
        BigDecimal credit = (resultSet[0] != null) ? (BigDecimal) resultSet[0] : BigDecimal.ZERO;
        BigDecimal debit = (resultSet[1] != null) ? (BigDecimal) resultSet[1] : BigDecimal.ZERO;
        total = credit.subtract(debit);
      }

    } finally {
      OBContext.restorePreviousMode();
    }

    return total;
  }

  /**
   * Calculates the sum of all the transactions in a higher date than the end date of the given
   * reconciliation.
   *
   * @param recon
   *     Reconciliation.
   * @return Sum of all the transactions in a higher date than the end date of the given
   *     reconciliation.
   */
  private BigDecimal getTransactionsTotalAfterReconciliationEndDate(FIN_Reconciliation recon) {
    BigDecimal balance = BigDecimal.ZERO;
    OBContext.setAdminMode(true);
    try {
      OBCriteria<FIN_FinaccTransaction> obcTrans = OBDal.getInstance()
          .createCriteria(FIN_FinaccTransaction.class);
      obcTrans.addEqual(FIN_FinaccTransaction.PROPERTY_ACCOUNT, recon.getAccount());
      obcTrans.addEqual(FIN_FinaccTransaction.PROPERTY_PROCESSED, true);
      obcTrans.addGreaterThan(FIN_FinaccTransaction.PROPERTY_TRANSACTIONDATE, recon.getEndingDate());
      obcTrans.setMultipleSums(
          FIN_FinaccTransaction.PROPERTY_PAYMENTAMOUNT,
          FIN_FinaccTransaction.PROPERTY_DEPOSITAMOUNT
      );

      @SuppressWarnings("rawtypes")
      List o = obcTrans.list();
      if (o != null && !o.isEmpty()) {
        Object[] resultSet = (Object[]) o.get(0);
        BigDecimal paymentAmt = (resultSet[0] != null) ? (BigDecimal) resultSet[0]
            : BigDecimal.ZERO;
        BigDecimal depositAmt = (resultSet[1] != null) ? (BigDecimal) resultSet[1]
            : BigDecimal.ZERO;
        balance = depositAmt.subtract(paymentAmt);
      }

    } finally {
      OBContext.restorePreviousMode();
    }

    return balance;
  }

  /**
   * Calculates the balance of the financial account at the day of the reconciliation.
   *
   * @param recon
   *     Reconciliation
   * @return Balance of the financial account at the day of the reconciliation.
   */
  private BigDecimal getBalanceOfAccount(FIN_Reconciliation recon) {
    BigDecimal balance = BigDecimal.ZERO;

    OBContext.setAdminMode(true);
    try {
      BigDecimal totals = getTransactionsTotalAfterReconciliationEndDate(recon);
      balance = recon.getAccount().getCurrentBalance().subtract(totals);
    } catch (Exception e) {
      log.error("Error during getBalanceOfAccount", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return balance;
  }
}
