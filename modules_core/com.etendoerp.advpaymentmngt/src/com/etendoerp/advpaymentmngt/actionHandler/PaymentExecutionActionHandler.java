package com.etendoerp.advpaymentmngt.actionHandler;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.apache.log4j.Logger;
import org.openbravo.client.application.process.ResponseActionsBuilder;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.payment.*;

import java.util.Map;
import java.util.Objects;

public class PaymentExecutionActionHandler extends BaseProcessActionHandler {

  private static final Logger log = Logger.getLogger(PaymentExecutionActionHandler.class);

  static final String PROCESS_BUTTON = "ProcessPayment";
  static final String GRID_PARAMETER = "payment_pick";
  static final String ORG_PARAMETER = "ad_org_id";
  static final String PAYMENT_METHOD_PARAMETER = "payment_method";
  static final String FINANCIAL_ACCOUNT_PARAMETER = "financial_account";

  static final String EXECUTION_PROCESS_ID = "executionProcessId";
  static final String EXECUTION_PROCESS_NAME = "executionProcessName";
  static final String SELECTED_PAYMENTS_IDS = "selectedPaymentsIds";
  static final String ORGANIZATION_ID = "organizationId";
  static final String PROCESS_PARAMETERS = "processParameters";
  static final String POPUP_ACTION_NAME = "EAPM_Popup";

  // Messages
  static final String GRID_NOT_DEFINED_MESSAGE = "EAPM_GridNotDefined";
  static final String LINES_NOT_SELECTED_MESSAGE = "EAPM_LinesNotSelected";
  static final String PAYMENTS_CREATED_MESSAGE = "EAPM_PaymentsCreated";
  static final String NO_EXECUTION_PROCESS_MESSAGE = "EAPM_NoExecutionProcessDefined";
  static final String FINACCPAY_NOT_DEFINED_MESSAGE = "EAPM_FinAccPaymentMethodNotDefined";
  static final String PROCESS_TYPE_MESSAGE = "EAPM_ProcessTypeNotAutomatic";

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    try {
      JSONObject request = new JSONObject(content);
      JSONObject params = request.getJSONObject("_params");
      String buttonValue = request.getString("_buttonValue");

      if (buttonValue.equals(PROCESS_BUTTON)) {
        JSONObject paymentPickGrid = params.getJSONObject(GRID_PARAMETER);
        if (paymentPickGrid == null) {
          throw new PaymentExecutionException(OBMessageUtils.messageBD(GRID_NOT_DEFINED_MESSAGE));
        }

        JSONArray selectedPayments = paymentPickGrid.getJSONArray(
            ApplicationConstants.SELECTION_PROPERTY);
        if (selectedPayments == null || selectedPayments.length() == 0) {
          return getResponseBuilder().showMsgInProcessView(
              ResponseActionsBuilder.MessageType.WARNING,
              OBMessageUtils.messageBD(LINES_NOT_SELECTED_MESSAGE)).retryExecution().build();
        }

        String paymentId = selectedPayments.getJSONObject(0).getString("id");
        String organizationId = params.getString(ORG_PARAMETER);

        // Search the process
        PaymentExecutionProcess executionProcess = getExecutionProcess(paymentId);

        verifyExecutionType(params.getString(PAYMENT_METHOD_PARAMETER),
            params.getString(FINANCIAL_ACCOUNT_PARAMETER), paymentId);

        String selectedPaymentsIds = getSelectedPaymentsIds(selectedPayments);
        return generateActionPopup(executionProcess, selectedPaymentsIds, organizationId);
      } else {
        return getResponseBuilder().refreshGrid()
            .refreshGridParameter(GRID_PARAMETER)
            .retryExecution()
            .build();
      }
    } catch (PaymentExecutionException p) {
      return getResponseBuilder().showMsgInProcessView(ResponseActionsBuilder.MessageType.ERROR,
          p.getMessage()).retryExecution().build();
    } catch (Exception e) {
      log.error("Error in process", e);
      return getResponseBuilder().showMsgInProcessView(ResponseActionsBuilder.MessageType.ERROR,
          e.getMessage()).build();
    }
  }

  static JSONObject generateActionPopup(PaymentExecutionProcess executionProcess,
      String selectedPaymentsIds, String organizationId) throws JSONException {
    JSONObject actionData = new JSONObject();
    actionData.put(EXECUTION_PROCESS_ID, executionProcess.getId());
    actionData.put(EXECUTION_PROCESS_NAME, executionProcess.getName());
    actionData.put(SELECTED_PAYMENTS_IDS, selectedPaymentsIds);
    actionData.put(ORGANIZATION_ID, organizationId);
    actionData.put(PROCESS_PARAMETERS,
        PaymentExecutionUtils.getProcessParameters(executionProcess));

    return getResponseBuilder().addCustomResponseAction(POPUP_ACTION_NAME, actionData)
        .retryExecution()
        .build();
  }

  static PaymentExecutionProcess getExecutionProcess(String paymentId) {
    PaymentExecutionProcess executionProcess;
    try {
      OBContext.setAdminMode(true);
      executionProcess = PaymentExecutionUtils.getExecutionProcess(paymentId);
    } finally {
      OBContext.restorePreviousMode();
    }
    if (executionProcess == null) {
      throw new PaymentExecutionException(OBMessageUtils.messageBD(NO_EXECUTION_PROCESS_MESSAGE));
    }
    return executionProcess;
  }

  /**
   * Verifies that the process execution type is set to automatic ('A')
   *
   * @param paymentMethodId
   * @param financialAccountId
   * @param paymentId
   */
  static void verifyExecutionType(String paymentMethodId, String financialAccountId,
      String paymentId) {
    FIN_Payment finPayment = OBDal.getInstance().get(FIN_Payment.class, paymentId);
    FIN_PaymentMethod finPaymentMethod = OBDal.getInstance()
        .get(FIN_PaymentMethod.class, paymentMethodId);
    FIN_FinancialAccount finFinancialAccount = OBDal.getInstance()
        .get(FIN_FinancialAccount.class, financialAccountId);
    FinAccPaymentMethod finAccPaymentMethod = PaymentExecutionUtils.getFinancialAccountPaymentMethod(
        finFinancialAccount, finPaymentMethod);

    if (finAccPaymentMethod == null) {
      throw new PaymentExecutionException(OBMessageUtils.messageBD(FINACCPAY_NOT_DEFINED_MESSAGE));
    }

    boolean isReceipt = finPayment.isReceipt();
    if (isReceipt && !Objects.equals(finAccPaymentMethod.getPayinExecutionType(),
        "A") || !isReceipt && !Objects.equals(finAccPaymentMethod.getPayoutExecutionType(), "A")) {
      throw new PaymentExecutionException(OBMessageUtils.messageBD(PROCESS_TYPE_MESSAGE));
    }
  }

  /**
   * Obtains a separated comma list of Payments Ids from the 'selectedPayments' grid.
   *
   * @param selectedPayments
   *     The pick grid values
   * @return String of selected payments ids
   * @throws JSONException
   */
  static String getSelectedPaymentsIds(JSONArray selectedPayments) throws JSONException {
    StringBuilder paymentsIds = new StringBuilder();
    for (int i = 0; i < selectedPayments.length(); i++) {
      paymentsIds.append(",").append(selectedPayments.getJSONObject(i).getString("id"));
    }
    return paymentsIds.toString().replaceFirst(",", "");
  }

}
