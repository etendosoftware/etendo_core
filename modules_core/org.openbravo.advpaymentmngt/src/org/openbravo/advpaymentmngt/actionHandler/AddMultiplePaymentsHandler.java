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
 * All portions are Copyright (C) 2015-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.advpaymentmngt.actionHandler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.advpaymentmngt.process.FIN_TransactionProcess;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.json.JsonUtils;

public class AddMultiplePaymentsHandler extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();
  private static final SimpleDateFormat jsDateFormat = JsonUtils.createDateFormat();
  private static final String ACTION_PROCESS_TRANSACTION = "P";
  @Inject
  @Any
  private Instance<AddMultiplePaymentsProcessAfterProcessHook> afterHooks;

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String data) {
    try {
      final JSONObject jsonData = new JSONObject(data);
      final JSONObject jsonparams = jsonData.getJSONObject("_params");

      final JSONArray selectedPayments = jsonparams.getJSONObject("payments")
          .getJSONArray("_selection");
      final Date statementDate = jsDateFormat.parse(jsonparams.getString("statementDate"));
      final Date dateAcct = jsDateFormat.parse(jsonparams.getString("dateAcct"));
      final String strAccountId = jsonData.getString("Fin_Financial_Account_ID");

      int selectedPaymentsLength = selectedPayments.length();

      for (int i = 0; i < selectedPaymentsLength; i++) {
        final JSONObject paymentJS = selectedPayments.getJSONObject(i);
        createAndProcessTransactionFromPayment(paymentJS, statementDate, dateAcct, strAccountId);
        OBDal.getInstance().getSession().clear();
      }

      List<AddMultiplePaymentsProcessAfterProcessHook> hooksPriority = new ArrayList<AddMultiplePaymentsProcessAfterProcessHook>();
      for (AddMultiplePaymentsProcessAfterProcessHook hook : afterHooks) {
        hooksPriority.add(hook);
      }
      Collections.sort(hooksPriority, new Comparator<AddMultiplePaymentsProcessAfterProcessHook>() {
        @Override
        public int compare(AddMultiplePaymentsProcessAfterProcessHook o1,
            AddMultiplePaymentsProcessAfterProcessHook o2) {
          return (int) Math.signum(o2.getPriority() - o1.getPriority());
        }
      });
      for (AddMultiplePaymentsProcessAfterProcessHook hook : hooksPriority) {
        selectedPaymentsLength = selectedPaymentsLength + hook.executeHook(jsonData);
      }

      if (selectedPaymentsLength == 0) {
        // Validation error: No lines selected
        return getErrorMessage(OBMessageUtils.messageBD("APRM_NO_PAYMENTS_SELECTED"));
      }

      // Success Message
      return getSuccessMessage(String.format(
          OBMessageUtils.messageBD("APRM_MULTIPLE_TRANSACTIONS_ADDED"), selectedPaymentsLength));

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception creating multiple transactions from payments", e);

      try {
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        return getErrorMessage(message);
      } catch (Exception ignore) {
      }
    }

    return new JSONObject();
  }

  /**
   * Creates a new transaction from the payment and then it processes the transaction
   */
  private void createAndProcessTransactionFromPayment(final JSONObject paymentJS,
      final Date transactionDate, final Date acctDate, String strAccountId) throws JSONException {

    try {
      OBContext.setAdminMode(true);
      final String paymentId = paymentJS.getString("id");
      log.debug("Creating transaction for FIN_Payment_ID: " + paymentId);
      final FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, paymentId);
      FIN_FinancialAccount account = OBDal.getInstance()
          .get(FIN_FinancialAccount.class, strAccountId);

      if (payment != null) {
        final FIN_FinaccTransaction transaction = TransactionsDao.createFinAccTransaction(payment);
        transaction.setTransactionDate(transactionDate);
        transaction.setDateAcct(acctDate);
        transaction.setAccount(account);
        FIN_TransactionProcess.doTransactionProcess(ACTION_PROCESS_TRANSACTION, transaction);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns a JSONObject with the success message to be printed
   */
  private static JSONObject getSuccessMessage(final String msgText) {
    final JSONObject result = new JSONObject();
    try {
      final JSONArray actions = new JSONArray();
      final JSONObject msgInBPTab = new JSONObject();
      msgInBPTab.put("msgType", "success");
      msgInBPTab.put("msgTitle", OBMessageUtils.messageBD("success"));
      msgInBPTab.put("msgText", msgText);
      final JSONObject msgInBPTabAction = new JSONObject();
      msgInBPTabAction.put("showMsgInProcessView", msgInBPTab);
      actions.put(msgInBPTabAction);
      result.put("responseActions", actions);
    } catch (Exception e) {
      log.error(e);
    }

    return result;
  }

  /**
   * Returns a JSONObject with the error message to be printed and retry execution
   */
  private static JSONObject getErrorMessage(final String msgText) {
    final JSONObject result = new JSONObject();
    try {
      final JSONObject msg = new JSONObject();
      msg.put("severity", "error");
      msg.put("text", msgText);
      result.put("message", msg);
      result.put("retryExecution", true);
    } catch (Exception e) {
      log.error(e);
    }
    return result;
  }

}
