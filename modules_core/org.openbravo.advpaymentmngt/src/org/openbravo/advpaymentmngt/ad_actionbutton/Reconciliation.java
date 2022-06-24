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

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.DateUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.APRM_FinaccTransactionV;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.model.financialmgmt.payment.FIN_ReconciliationLine_v;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.xmlEngine.XmlDocument;

public class Reconciliation extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private AdvPaymentMngtDao dao;
  Set<FIN_FinaccTransaction> transactionsToBePosted = new HashSet<FIN_FinaccTransaction>();
  private static final String BP_WITHDRAWAL = "BPW";

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId", "Reconciliation|Org");
      String strWindowId = vars.getRequestGlobalVariable("inpwindowId", "Reconciliation|windowId");
      String strTabId = vars.getRequestGlobalVariable("inpTabId", "Reconciliation|tabId");
      String strFinancialAccountId = vars.getStringParameter("inpfinFinancialAccountId");
      final int accesslevel = 3;

      if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(),
          strTabId))
          || !(Utility.isElementInList(
              Utility.getContext(this, vars, "#User_Client", strWindowId, accesslevel),
              vars.getClient())
              && Utility.isElementInList(
                  Utility.getContext(this, vars, "#User_Org", strWindowId, accesslevel),
                  strOrgId))) {
        OBError myError = Utility.translateError(this, vars, vars.getLanguage(),
            Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
        vars.setMessage(strTabId, myError);
        printPageClosePopUp(response, vars);
      } else {
        printPage(response, vars, strOrgId, strWindowId, strTabId, strFinancialAccountId, null,
            null);
      }

    } else if (vars.commandIn("GRID")) {
      String strFinancialAccountId = vars.getStringParameter("inpFinFinancialAccountId", "");
      String strStatementDate = vars.getStringParameter("inpStatementDate");
      boolean strAfterDate = "Y".equals(vars.getStringParameter("inpAfterDate"));
      String selectedTransactionsIds = vars.getInParameter("inpTransactionId", IsIDFilter.instance);
      String strCurrentlyCleared = vars.getNumericParameter("inpCalcCurrentlyCleared");
      String strTotalPayment = vars.getNumericParameter("inpCalcTotalPayment");
      String strTotalDeposit = vars.getNumericParameter("inpCalcTotalDeposit");

      printGrid(response, strFinancialAccountId, strStatementDate, strAfterDate,
          selectedTransactionsIds, strCurrentlyCleared, strTotalPayment, strTotalDeposit);

    } else if (vars.commandIn("SAVE") || vars.commandIn("PROCESS")) {
      String strTabId = vars.getGlobalVariable("inpTabId", "Reconciliation|tabId");
      String strFinancialAccountId = vars.getStringParameter("inpFinFinancialAccountId", "");
      String strDifference = vars.getNumericParameter("inpCalcDifference");
      String strStatementDate = vars.getStringParameter("inpStatementDate");
      String strBeginBalance = vars.getNumericParameter("inpBeginBalance");
      String strEndBalance = vars.getNumericParameter("inpEndBalance");
      String strGlItemDiff = vars.getNumericParameter("inpGlDifference");

      boolean process = vars.commandIn("PROCESS");
      processReconciliation(response, vars, strTabId, strFinancialAccountId, strDifference,
          strStatementDate, strBeginBalance, strEndBalance, strGlItemDiff, process);

    } else if (vars.commandIn("UPDATESTATUS")) {
      String strFinancialAccountId = vars.getStringParameter("inpFinFinancialAccountId", "");
      String strSelectedTransId = vars.getStringParameter("inpCurrentTransIdSelected", "");
      boolean isChecked = "true".equals(vars.getStringParameter("inpIsCurrentTransSelected"));

      updateTransactionStatus(response, strFinancialAccountId, strSelectedTransId, isChecked);
    }

  }

  private void updateTransactionStatus(HttpServletResponse response, String strFinancialAccountId,
      String strSelectedTransId, boolean isChecked) {

    try {
      OBContext.setAdminMode();
      if (strSelectedTransId != "") {
        FIN_FinaccTransaction trans = OBDal.getInstance()
            .get(FIN_FinaccTransaction.class, strSelectedTransId);
        String newStatus = "RPPC";
        if (!isChecked) {
          newStatus = (trans.getPaymentAmount().compareTo(trans.getDepositAmount()) >= 0) ? "PWNC"
              : "RDNC";
          trans.setReconciliation(null);
          if (trans.getFinPayment() != null) {
            trans.getFinPayment().setStatus((trans.getFinPayment().isReceipt()) ? "RDNC" : "PWNC");
          }
        } else {
          FIN_FinancialAccount account = OBDal.getInstance()
              .get(FIN_FinancialAccount.class, strFinancialAccountId);
          FIN_Reconciliation reconciliation = TransactionsDao.getLastReconciliation(account, "N");
          trans.setReconciliation(reconciliation);
          if (trans.getFinPayment() != null) {
            trans.getFinPayment().setStatus("RPPC");
          }
        }

        trans.setStatus(newStatus);
        OBDal.getInstance().save(trans);
        OBDal.getInstance().flush();
        // Force flush because eventhandler is executed in the first flush and data updated in
        // enventhandler needs to be flush in a admin mode block
        OBDal.getInstance().flush();
      }
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      out.println("");
      out.close();
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private void processReconciliation(HttpServletResponse response, VariablesSecureApp vars,
      String strTabId, String strFinancialAccountId, String strDifference, String strStatementDate,
      String strBeginBalance, String strEndBalance, String strGlItemDiff, boolean process)
      throws IOException, ServletException {

    log4j.debug(
        "Output: Process or Save button pressed on Financial Account || Transaction || Reconciliation manual window");

    dao = new AdvPaymentMngtDao();
    OBError msg = new OBError();
    OBContext.setAdminMode();
    try {

      FIN_FinancialAccount account = OBDal.getInstance()
          .get(FIN_FinancialAccount.class, strFinancialAccountId);

      FIN_Reconciliation reconciliation = TransactionsDao.getLastReconciliation(account, "N");

      FIN_Reconciliation lastProcessedReconciliation = TransactionsDao
          .getLastReconciliation(account, "Y");

      reconciliation.setEndingBalance(new BigDecimal(strEndBalance));
      reconciliation.setTransactionDate(FIN_Utility.getDate(strStatementDate));
      reconciliation.setEndingDate(FIN_Utility.getDate(strStatementDate));
      reconciliation.setDocumentStatus("DR");
      reconciliation.setProcessed(false);
      reconciliation.setAPRMProcessReconciliation("P");
      reconciliation.setAprmProcessRec("P");
      OBDal.getInstance().save(reconciliation);
      OBDal.getInstance().flush();

      if (process) { // Validations
        String strMessage = "";
        boolean raiseException = false;

        if ((new BigDecimal(strDifference).subtract(new BigDecimal(strGlItemDiff)))
            .compareTo(BigDecimal.ZERO) != 0) {
          strMessage = "@APRM_ReconciliationDiscrepancy@" + " " + strDifference;
          raiseException = true;
        }

        Calendar calCurrent = Calendar.getInstance();
        calCurrent.setTime(FIN_Utility.getDateTime(strStatementDate));

        if (lastProcessedReconciliation != null) {
          Calendar calLast = Calendar.getInstance();
          calLast.setTime(lastProcessedReconciliation.getEndingDate());
          if (calCurrent.before(calLast)) {
            strMessage = "@APRM_ReconcileInFutureOrPast@";
            raiseException = true;
          }
        }

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);
        tomorrow.setTime(DateUtils.truncate(tomorrow.getTime(), Calendar.DATE));
        if (calCurrent.after(tomorrow)) {
          strMessage = "@APRM_ReconcileInFutureOrPast@";
          raiseException = true;
        }
        if (raiseException) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
          msg.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), strMessage));
          vars.setMessage(strTabId, msg);
          msg = null;
          printPageClosePopUpAndRefreshParent(response, vars);
          return;
        }

        boolean orgLegalWithAccounting = FIN_Utility.periodControlOpened(
            FIN_Reconciliation.TABLE_NAME, reconciliation.getId(),
            FIN_Reconciliation.TABLE_NAME + "_ID", "LE");
        boolean documentEnabled = getDocumentConfirmation(this, reconciliation.getId());
        if (documentEnabled && !FIN_Utility.isPeriodOpen(reconciliation.getClient().getId(),
            AcctServer.DOCTYPE_Reconciliation, reconciliation.getOrganization().getId(),
            strStatementDate) && orgLegalWithAccounting) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
          msg.setMessage(
              Utility.parseTranslation(this, vars, vars.getLanguage(), "@PeriodNotAvailable@"));
          vars.setMessage(strTabId, msg);
          msg = null;
          printPageClosePopUpAndRefreshParent(response, vars);
          return;
        }

        if (documentEnabled && orgLegalWithAccounting) {
          String identifier = linesInNotAvailablePeriod(reconciliation.getId());
          if (!identifier.equalsIgnoreCase("")) {
            msg.setType("Error");
            msg.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
            msg.setMessage(String.format(
                Utility.messageBD(this, "APRM_PeriodNotAvailableClearedItem", vars.getLanguage()),
                identifier));
            vars.setMessage(strTabId, msg);
            msg = null;
            printPageClosePopUpAndRefreshParent(response, vars);
            return;
          }
        }

        // Difference transaction
        LineNumberUtil lineNoUtil = new LineNumberUtil();
        BigDecimal bdGlItemDiff = new BigDecimal(strGlItemDiff).negate();

        // Do no create a difference transaction if the amount is 0.00
        // If transaction is created with amount 0, a constratint is violated
        if (bdGlItemDiff.compareTo(BigDecimal.ZERO) != 0) {
          createTransaction(account, BP_WITHDRAWAL, reconciliation.getTransactionDate(),
              account.getAprmGlitemDiff(), bdGlItemDiff, lineNoUtil, null, "GL Item: Differences",
              reconciliation);
        }

        for (APRM_FinaccTransactionV finacctrxv : reconciliation.getAPRMFinaccTransactionVList()) {
          if (reconciliation.getEndingDate()
              .compareTo(finacctrxv.getFinancialAccountTransaction().getTransactionDate()) < 0) {
            FIN_FinaccTransaction trans = finacctrxv.getFinancialAccountTransaction();
            // We set processed to false before changing dates to avoid trigger exception
            boolean posted = "Y".equals(trans.getPosted());
            if (posted) {
              trans.setPosted("N");
              OBDal.getInstance().save(trans);
              OBDal.getInstance().flush();
            }
            trans.setProcessed(false);
            OBDal.getInstance().save(trans);
            OBDal.getInstance().flush();
            trans.setTransactionDate(reconciliation.getEndingDate());
            trans.setDateAcct(reconciliation.getEndingDate());
            OBDal.getInstance().save(trans);
            OBDal.getInstance().flush();
            // We set processed to true afterwards
            trans.setProcessed(true);
            OBDal.getInstance().save(trans);
            OBDal.getInstance().flush();
            if (posted) {
              trans.setPosted("Y");
              OBDal.getInstance().save(trans);
              OBDal.getInstance().flush();
            }
            // Changing dates for accounting entries as well
            TransactionsDao.updateAccountingDate(trans);
          }
          Boolean invoicePaidold = false;
          if (finacctrxv.getPayment() != null) {
            for (FIN_PaymentDetail pd : finacctrxv.getPayment().getFINPaymentDetailList()) {
              for (FIN_PaymentScheduleDetail psd : pd.getFINPaymentScheduleDetailList()) {
                invoicePaidold = psd.isInvoicePaid();
                if (!invoicePaidold) {
                  if ((FIN_Utility
                      .invoicePaymentStatus(finacctrxv.getPayment().getPaymentMethod(),
                          reconciliation.getAccount(), finacctrxv.getPayment().isReceipt())
                      .equals(finacctrxv.getPayment().getStatus()))) {
                    psd.setInvoicePaid(true);
                  }
                  if (psd.isInvoicePaid()) {
                    FIN_Utility.updatePaymentAmounts(psd);
                  }
                }
              }
              FIN_Utility.updateBusinessPartnerCredit(finacctrxv.getPayment());
            }
          }
        }

        reconciliation.setDocumentStatus("CO");
        reconciliation.setProcessed(true);
        reconciliation.setAPRMProcessReconciliation("R");
        reconciliation.setAprmProcessRec("R");
        OBDal.getInstance().save(reconciliation);
        OBDal.getInstance().flush();

      }

      String strMessage = "@APRM_ReconciliationNo@" + ": " + reconciliation.getDocumentNo();
      msg.setType("Success");
      msg.setTitle(Utility.messageBD(this, "Success", vars.getLanguage()));
      msg.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), strMessage));
      vars.setMessage(strTabId, msg);
      msg = null;
      printPageClosePopUpAndRefreshParent(response, vars);

    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strOrgId,
      String strWindowId, String strTabId, String strFinancialAccountId, String strStatementDate,
      String strEndBalance) throws IOException, ServletException {

    log4j.debug("Output: Reconcile button pressed on Financial Account || Transaction tab");

    dao = new AdvPaymentMngtDao();
    String dateFormat = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("dateFormat.java");
    SimpleDateFormat dateFormater = new SimpleDateFormat(dateFormat);

    FIN_Reconciliation currentReconciliation = null;
    OBContext.setAdminMode();
    try {
      FIN_FinancialAccount account = OBDal.getInstance()
          .get(FIN_FinancialAccount.class, strFinancialAccountId);

      FIN_Reconciliation lastProcessedReconciliation = TransactionsDao
          .getLastReconciliation(account, "Y");
      currentReconciliation = TransactionsDao.getLastReconciliation(account, "N");
      if (isAutomaticReconciliation(currentReconciliation)) {
        OBDal.getInstance().rollbackAndClose();
        OBError message = Utility.translateError(this, vars, vars.getLanguage(),
            Utility.parseTranslation(this, vars, vars.getLanguage(), "@APRM_ReconciliationMixed@"));
        vars.setMessage(strTabId, message);
        printPageClosePopUp(response, vars, Utility.getTabURL(strTabId, "R", true));
        return;
      }

      XmlDocument xmlDocument = xmlEngine
          .readXmlTemplate("org/openbravo/advpaymentmngt/ad_actionbutton/Reconciliation")
          .createXmlDocument();

      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("theme", vars.getTheme());

      xmlDocument.setParameter("dateDisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      xmlDocument.setParameter("mainDate", DateTimeData.today(this));
      xmlDocument.setParameter("windowId", strWindowId);
      xmlDocument.setParameter("tabId", strTabId);
      xmlDocument.setParameter("orgId", strOrgId);
      xmlDocument.setParameter("finFinancialAccountId", strFinancialAccountId);

      BigDecimal currentEndBalance = BigDecimal.ZERO;
      if (vars.commandIn("PROCESS")) {
        xmlDocument.setParameter("statementDate", strStatementDate);
        xmlDocument.setParameter("endBalance", strEndBalance);
        xmlDocument.setParameter("calcEndingBalance", strEndBalance);

      } else {
        String currentStatementDate = DateTimeData.today(this);
        if (currentReconciliation != null) {
          currentStatementDate = dateFormater.format(currentReconciliation.getTransactionDate());
          currentEndBalance = currentReconciliation.getEndingBalance();
        }
        xmlDocument.setParameter("statementDate", currentStatementDate);
        xmlDocument.setParameter("endBalance", currentEndBalance.toString());
        xmlDocument.setParameter("calcEndingBalance", currentEndBalance.toString());
      }

      BigDecimal beginBalance = (lastProcessedReconciliation == null) ? account.getInitialBalance()
          : lastProcessedReconciliation.getEndingBalance();

      xmlDocument.setParameter("account", account.getName());
      xmlDocument.setParameter("beginBalance", beginBalance.toString());

      // Hidden inputs
      xmlDocument.setParameter("calcBeginningBalance", beginBalance.toString());
      xmlDocument.setParameter("calcTotalPayment", BigDecimal.ZERO.toString());
      xmlDocument.setParameter("calcTotalDeposit", BigDecimal.ZERO.toString());
      xmlDocument.setParameter("calcDifferenceToClear",
          currentEndBalance.subtract(beginBalance).toString());
      xmlDocument.setParameter("calcCurrentlyCleared",
          TransactionsDao.getCurrentlyClearedAmt(account.getId()).toString());
      xmlDocument.setParameter("calcDifference",
          currentEndBalance
              .subtract(beginBalance.add(TransactionsDao.getCurrentlyClearedAmt(account.getId())))
              .toString());

      OBContext.setAdminMode();
      try {
        xmlDocument.setParameter("precision",
            account.getCurrency().getStandardPrecision().toString());

        if (currentReconciliation == null) {
          DocumentType docType = FIN_Utility.getDocumentType(account.getOrganization(), "REC");
          if (docType == null) {
            OBError msg = new OBError();
            String strMessage = "@APRM_DocumentTypeNotFound@";
            msg.setType("Error");
            msg.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
            msg.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), strMessage));
            vars.setMessage(strTabId, msg);
            msg = null;
            printPageClosePopUpAndRefreshParent(response, vars);
            return;
          }
          String docNumber = FIN_Utility.getDocumentNo(account.getOrganization(), "REC",
              "DocumentNo_FIN_Reconciliation");

          dao.getNewReconciliation(account.getOrganization(), account, docNumber, docType,
              new Date(), new Date(), beginBalance, BigDecimal.ZERO, "DR");
        }
      } finally {
        OBContext.restorePreviousMode();
      }

      OBError myMessage = vars.getMessage(strWindowId);
      vars.removeMessage(strWindowId);
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }

      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      out.println(xmlDocument.print());
      out.close();
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void printGrid(HttpServletResponse response, String strFinancialAccountId,
      String strStatmentDate, boolean afterDate, String selectedTransactionsIds,
      String strCurrentlyCleared, String strTotalPayment, String strTotalDeposit)
      throws IOException, ServletException {

    log4j.debug("Output: Grid on Financial Account || Transaction tab || Reconciliation window");

    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/advpaymentmngt/ad_actionbutton/ReconciliationGrid")
        .createXmlDocument();

    FIN_FinancialAccount account = OBDal.getInstance()
        .get(FIN_FinancialAccount.class, strFinancialAccountId);

    Map<String, String> map = FIN_Utility.getMapFromStringList(selectedTransactionsIds);

    FieldProvider[] data = TransactionsDao.getTransactionsFiltered(account,
        FIN_Utility.getDate(DateTimeData.nDaysAfter(this, strStatmentDate, "1")), afterDate);

    BigDecimal currentlyCleared = new BigDecimal(strCurrentlyCleared);
    BigDecimal totalPayment = new BigDecimal(strTotalPayment);
    BigDecimal totalDeposit = new BigDecimal(strTotalDeposit);

    for (FieldProvider fp : data) {

      if (!map.containsKey(fp.getField("transactionId"))
          && !fp.getField("markSelectedId").isEmpty()) {
        BigDecimal payAmt = new BigDecimal(fp.getField("paymentAmount"));
        BigDecimal depAmt = new BigDecimal(fp.getField("depositAmount"));
        currentlyCleared = currentlyCleared.add(payAmt).subtract(depAmt);
        totalPayment = totalPayment.add(payAmt);
        totalDeposit = totalDeposit.add(depAmt);
      }
    }

    xmlDocument.setParameter("calcTotalPayment", totalPayment.toString());
    xmlDocument.setParameter("caclTotalDeposit", totalDeposit.toString());
    xmlDocument.setParameter("calcCurrentlyCleared", currentlyCleared.toString());
    xmlDocument.setData("structure", data);

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private boolean isAutomaticReconciliation(FIN_Reconciliation reconciliation) {
    OBContext.setAdminMode();
    try {
      final OBCriteria<FIN_ReconciliationLine_v> obc = OBDal.getInstance()
          .createCriteria(FIN_ReconciliationLine_v.class);
      obc.add(Restrictions.eq(FIN_ReconciliationLine_v.PROPERTY_RECONCILIATION, reconciliation));
      obc.add(Restrictions.isNotNull(FIN_ReconciliationLine_v.PROPERTY_BANKSTATEMENTLINE));
      obc.setMaxResults(1);
      final List<FIN_ReconciliationLine_v> rec = obc.list();
      return (rec.size() != 0);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Override
  public String getServletInfo() {
    return "This servlet manages manual transactions reconciliations.";
  }

  private String linesInNotAvailablePeriod(String reconciliationId) {
    //@formatter:off
    final String hql = " as rl "
    + " where rl.reconciliation.id = :reconciliationId"
    + "   and c_chk_open_period(rl.organization, rl.transactionDate, 'REC', null) = 0 "
    + " order by rl.transactionDate";

    //@formatter:on
    final OBQuery<FIN_ReconciliationLine_v> obqRL = OBDal.getInstance()
        .createQuery(FIN_ReconciliationLine_v.class, hql);
    obqRL.setNamedParameter("reconciliationId", reconciliationId);
    obqRL.setMaxResult(1);

    List<FIN_ReconciliationLine_v> obqRLlist = obqRL.list();

    if (obqRLlist.size() == 0) {
      return "";
    } else {
      return obqRLlist.get(0).getIdentifier();
    }

  }

  public List<FIN_FinaccTransaction> getTransactionList(FIN_Reconciliation reconciliation) {
    OBContext.setAdminMode();
    List<FIN_FinaccTransaction> transactions = null;
    try {
      OBCriteria<FIN_FinaccTransaction> trans = OBDal.getInstance()
          .createCriteria(FIN_FinaccTransaction.class);
      trans.add(Restrictions.eq(FIN_FinaccTransaction.PROPERTY_RECONCILIATION, reconciliation));
      trans.setFilterOnReadableClients(false);
      trans.setFilterOnReadableOrganization(false);
      transactions = trans.list();
    } finally {
      OBContext.restorePreviousMode();
    }
    return transactions;
  }

  /*
   * Checks if this step (Reconciliation) is configured to generate accounting for the selected
   * financial account
   */
  public boolean getDocumentConfirmation(ConnectionProvider conn, String strRecordId) {
    OBContext.setAdminMode();
    try {
      FIN_Reconciliation reconciliation = OBDal.getInstance()
          .get(FIN_Reconciliation.class, strRecordId);
      List<FIN_FinaccTransaction> transactions = getTransactionList(reconciliation);
      List<FIN_FinancialAccountAccounting> accounts = reconciliation.getAccount()
          .getFINFinancialAccountAcctList();
      for (FIN_FinaccTransaction transaction : transactions) {
        FIN_Payment payment = transaction.getFinPayment();
        // If payment exists, check Payment Method + financial Account Configuration
        if (payment != null) {
          OBCriteria<FinAccPaymentMethod> obCriteria = OBDal.getInstance()
              .createCriteria(FinAccPaymentMethod.class);
          obCriteria.add(
              Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, reconciliation.getAccount()));
          obCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD,
              payment.getPaymentMethod()));
          obCriteria.setFilterOnReadableClients(false);
          obCriteria.setFilterOnReadableOrganization(false);
          List<FinAccPaymentMethod> lines = obCriteria.list();
          for (FIN_FinancialAccountAccounting account : accounts) {
            if (payment.isReceipt()) {
              if (("INT").equals(lines.get(0).getINUponClearingUse())
                  && account.getInTransitPaymentAccountIN() != null) {
                transactionsToBePosted.add(transaction);
              } else if (("DEP").equals(lines.get(0).getINUponClearingUse())
                  && account.getDepositAccount() != null) {
                transactionsToBePosted.add(transaction);
              } else if (("CLE").equals(lines.get(0).getINUponClearingUse())
                  && account.getClearedPaymentAccount() != null) {
                transactionsToBePosted.add(transaction);
              }
            } else {
              if (("INT").equals(lines.get(0).getOUTUponClearingUse())
                  && account.getFINOutIntransitAcct() != null) {
                transactionsToBePosted.add(transaction);
              } else if (("WIT").equals(lines.get(0).getOUTUponClearingUse())
                  && account.getWithdrawalAccount() != null) {
                transactionsToBePosted.add(transaction);
              } else if (("CLE").equals(lines.get(0).getOUTUponClearingUse())
                  && account.getClearedPaymentAccountOUT() != null) {
                transactionsToBePosted.add(transaction);
              }
            }
          }
        } else if (transaction.getGLItem() != null) {
          for (FIN_FinancialAccountAccounting account : accounts) {
            if ("BPD".equals(transaction.getTransactionType())
                && account.getClearedPaymentAccount() != null) {
              transactionsToBePosted.add(transaction);
            } else if ("BPW".equals(transaction.getTransactionType())
                && account.getClearedPaymentAccountOUT() != null) {
              transactionsToBePosted.add(transaction);
            }
          }
        } else {
          for (FIN_FinancialAccountAccounting account : accounts) {
            if ("BF".equals(transaction.getTransactionType())
                && account.getClearedPaymentAccountOUT() != null) {
              transactionsToBePosted.add(transaction);
            }
          }
        }
      }
    } catch (Exception e) {
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
    if (transactionsToBePosted.size() == 0) {
      return false;
    }
    return true;
  }

  private static FIN_FinaccTransaction createTransaction(FIN_FinancialAccount account,
      String trxType, Date trxDate, GLItem glitem, BigDecimal amount, LineNumberUtil lineNoUtil,
      FIN_FinaccTransaction sourceTrx, String description, FIN_Reconciliation reconciliation) {
    FIN_FinaccTransaction trx = OBProvider.getInstance().get(FIN_FinaccTransaction.class);

    trx.setAccount(account);
    trx.setTransactionType(trxType);
    trx.setTransactionDate(trxDate);
    trx.setDateAcct(trxDate);
    trx.setGLItem(glitem);
    trx.setCurrency(account.getCurrency());
    // if (BP_DEPOSIT.equalsIgnoreCase(trxType)) {
    // trx.setDepositAmount(amount);
    // } else {
    trx.setPaymentAmount(amount);
    // }
    // If the user has access to the Organization of the Financial Account, the Transaction is
    // created for it. If not, the Organization of the context is used instead
    if (OBContext.getOBContext()
        .getWritableOrganizations()
        .contains(account.getOrganization().getId())) {
      trx.setOrganization(account.getOrganization());
    } else {
      trx.setOrganization(OBContext.getOBContext().getCurrentOrganization());
    }

    Long line = lineNoUtil.getNextLineNumber(account);
    trx.setLineNo(line);

    trx.setAprmFinaccTransOrigin(sourceTrx);
    trx.setDescription(description);
    trx.setReconciliation(reconciliation);

    OBDal.getInstance().save(trx);
    OBDal.getInstance().flush();

    return trx;
  }

  private static class LineNumberUtil {
    private HashMap<FIN_FinancialAccount, Long> lastLineNo = new HashMap<FIN_FinancialAccount, Long>();

    protected Long getNextLineNumber(FIN_FinancialAccount account) {
      Long lineNo = lastLineNo.get(account);

      if (lineNo == null) {
        lineNo = TransactionsDao.getTransactionMaxLineNo(account);
      }
      lineNo += 10;
      lastLineNo.put(account, lineNo);

      return lineNo;
    }
  }

}
