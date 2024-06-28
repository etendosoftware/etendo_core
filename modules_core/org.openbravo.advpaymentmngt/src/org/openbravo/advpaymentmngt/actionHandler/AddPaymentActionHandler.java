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
 * All portions are Copyright (C) 2014-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.advpaymentmngt.actionHandler;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.hook.PaymentProcessHook;
import org.openbravo.advpaymentmngt.hook.PaymentProcessOrderHook;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.advpaymentmngt.process.FIN_PaymentProcess;
import org.openbravo.advpaymentmngt.utility.APRMConstants;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.process.ResponseActionsBuilder;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.json.JsonUtils;

import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;

public class AddPaymentActionHandler extends Action {
  private static final Logger log = LogManager.getLogger();
  private static final String MESSAGE = "message";
  private static final String SEVERITY = "severity";
  private static final String ERROR = "error";
  private static final String TEXT = "text";
  private static final String RETRY_EXECUTION = "retryExecution";
  private static final String REFRESH_PARENT = "refreshParent";
  private static final String REFERENCE_NO = "reference_no";
  private static final String SELECTION = "_selection";
  private static final String TITLE = "title";
  private static final String RESPONSE_ACTION = "responseActions";
  private static final String DOCUMENT_ACTION = "document_action";
  private static final String FIN_PAYMENT = "FIN_Payment";
  private static final String PRE_PROCESS_METHOD = "preProcess";
  private static final String POST_PROCESS_METHOD = "posProcess";

  @Inject
  @Any
  private Instance<PaymentProcessHook> hooks;

  /*
   * @param parameters
   *     Parameters (if any) defined in dictionary
   * @param isStopped
   *     true when the Job that runs the action was signaled to be stopped.
   *     Use when performing long or intensive task inside the action, and stop when the value switches to true.
   * @return an ActionResult which will contain the message to the user, and optionally Data to pass to another Action.
   */
  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
    var actionResult = new ActionResult();
    actionResult.setType(Result.Type.SUCCESS);
    try {
      var input = getInputContents(getInputClass());
      JSONObject content = getInput().getRawData();
      String entityName = content.getString("_entityName");
      boolean isOrderWithMoreThanOneRecord = StringUtils.equals("Order", entityName) && input.size() > 1;
      boolean isInvoiceWithMoreThanOneRecord = StringUtils.equals("Invoice", entityName) && input.size() > 1;
      boolean isPaymentWithMoreThanOneRecord = StringUtils.equals(FIN_PAYMENT, entityName) && input.size() > 1;
      boolean isWebService = content.has("processByWebService") && content.getBoolean("processByWebService");

      if (isOrderWithMoreThanOneRecord || isInvoiceWithMoreThanOneRecord || (isPaymentWithMoreThanOneRecord && !isWebService)) {
        actionResult.setType(Result.Type.ERROR);
        actionResult.setMessage(OBMessageUtils.messageBD("JS13"));
        return actionResult;
      }

      boolean isDocumentActionMissing = isWebService && !content.has(DOCUMENT_ACTION);
      // this is for Invoices and Order
      if (!StringUtils.equals(entityName, FIN_PAYMENT)) {
        callPaymentProcess(actionResult, content, content.getJSONObject("_params").getString("fin_payment_id"),
            isWebService);
        return actionResult;
      }

      // this is for payments, from WS or from Payment in/out windows
      if (!isDocumentActionMissing) {
        var processMessages = new StringBuilder();
        int errors = 0;
        int success = 0;
        for (FIN_Payment payment : input) {
          callPaymentProcess(actionResult, content, payment.getId(), isWebService);
          if (StringUtils.equals(Result.Type.ERROR.toString(), actionResult.getType().toString())) {
            errors++;
            OBDal.getInstance().rollbackAndClose();
          } else {
            success++;
            SessionHandler.getInstance().commitAndStart();
          }
          processMessages.append(actionResult.getMessage());
        }
        actionResult.setMessage(processMessages.toString());
        massiveMessageHandler(actionResult, input, errors, success);
        actionResult.setResponseActionsBuilder(null);
        return actionResult;
      }
      actionResult.setType(Result.Type.ERROR);
      actionResult.setMessage(OBMessageUtils.messageBD("APRM_payment_action_is_required"));
      return actionResult;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(e.getMessage(), e);
      actionResult.setType(Result.Type.ERROR);
      actionResult.setMessage(e.getMessage());
      return actionResult;
    }
  }

  private void massiveMessageHandler(ActionResult result, List<FIN_Payment> registers, int errors, int success) {
    if (registers.size() > 1) {
      if (success == registers.size()) {
        result.setType(Result.Type.SUCCESS);
      } else if (errors == registers.size()) {
        result.setType(Result.Type.ERROR);
      } else {
        result.setType(Result.Type.WARNING);
      }
      result.setMessage(String.format(OBMessageUtils.messageBD("DJOBS_PostUnpostMessage"), success, errors));
      result.setOutput(getInput());
    }
  }

  private void callPaymentProcess(ActionResult actionResult, JSONObject content,
      String paymentId, boolean isWebService) throws Exception {
    // process payments
    JSONObject resultProcess = oldProcessPaymentHandler(getRequestParameters(), content, paymentId, isWebService);
    JSONObject resultMessage = resultProcess.has(MESSAGE) ? resultProcess.getJSONObject(MESSAGE) : new JSONObject();
    String severity = resultMessage.has(SEVERITY) ? resultMessage.getString(SEVERITY) : "";
    String text = resultMessage.has(TEXT) ? resultMessage.getString(TEXT) : "";
    actionResult.setType(Result.Type.valueOf(severity.toUpperCase()));
    actionResult.setMessage(text);
    ResponseActionsBuilder responseActions = actionResult.getResponseActionsBuilder().orElse(getResponseBuilder());
    responseActions.showMsgInProcessView(ResponseActionsBuilder.MessageType.valueOf(severity.toUpperCase()), text);
    JSONObject actions = resultProcess.has(RESPONSE_ACTION) ? resultProcess.getJSONObject(
        RESPONSE_ACTION) : new JSONObject();
    Iterator<?> keys = actions.keys();
    while (keys.hasNext()) {
      String key = (String) keys.next();
      if (actions.get(key) instanceof JSONObject) {
        responseActions.addCustomResponseAction(key, actions.getJSONObject(key));
      }
    }
    if (resultProcess.has(RETRY_EXECUTION) && resultProcess.getBoolean(RETRY_EXECUTION)) {
      responseActions.retryExecution();
    }
    if (resultProcess.has(REFRESH_PARENT)) {
      responseActions.setRefreshParent(resultProcess.getBoolean(REFRESH_PARENT));
    }
    actionResult.setResponseActionsBuilder(responseActions);
    actionResult.setOutput(getInput());
  }

  protected JSONObject oldProcessPaymentHandler(Map<String, Object> parameters, JSONObject content, String paymentId,
      boolean isWebService) throws Exception {
    JSONObject jsonResponse = new JSONObject();
    boolean openedFromMenu = false;
    String comingFrom = null;
    try {
      OBContext.setAdminMode(true);
      if (isWebService) {
        FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, paymentId);
        if (payment.isProcessed()) {
          JSONObject message = new JSONObject();
          message.put(SEVERITY, ERROR);
          message.put(TEXT, OBMessageUtils.messageBD("APRM_payment_is_processed"));
          jsonResponse.put(RETRY_EXECUTION, true);
          jsonResponse.put(MESSAGE, message);
        } else {
          OBError msg = processMultiPayment(payment, content.getString(DOCUMENT_ACTION));
          JSONObject message = new JSONObject();
          message.put(SEVERITY, msg.getType());
          message.put(TEXT, !msg.getMessage().isEmpty() ? msg.getMessage() : msg.getTitle());
          jsonResponse.put(RETRY_EXECUTION, true);
          jsonResponse.put(MESSAGE, message);
        }
        return jsonResponse;
      } else {
        VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
        JSONObject jsonParams = content.getJSONObject("_params");

        JSONObject resultPre = executeHooks(jsonParams, PRE_PROCESS_METHOD);
        if (resultPre != null) {
          return resultPre;
        }

        // Get Params
        final String WINDOW_ID = "inpwindowId";
        if (content.has(WINDOW_ID) && content.get(WINDOW_ID) != JSONObject.NULL) {
          if (APRMConstants.TRANSACTION_WINDOW_ID.equals(content.getString(WINDOW_ID))) {
            comingFrom = "TRANSACTION";
          }
        } else {
          openedFromMenu = parameters.containsKey("windowId") && "null".equals(parameters.get("windowId").toString());
        }
        String strOrgId = null;
        final String AD_ORG_ID = "ad_org_id";
        final String INP_AD_ORG_ID = "inpadOrgId";
        String orgId1 = jsonParams.optString(AD_ORG_ID, "");
        String orgId2 = content.optString(INP_AD_ORG_ID, "");
        strOrgId = !orgId1.isEmpty() ? orgId1 : orgId2;
        Organization org = OBDal.getInstance().get(Organization.class, strOrgId);
        boolean isReceipt = jsonParams.getBoolean("issotrx");

        // Action to do
        final String strActionId = jsonParams.getString(DOCUMENT_ACTION);
        final org.openbravo.model.ad.domain.List actionList = OBDal.getInstance()
            .get(org.openbravo.model.ad.domain.List.class, strActionId);
        final String strAction = actionList.getSearchKey();

        final String strCurrencyId = jsonParams.getString("c_currency_id");
        Currency currency = OBDal.getInstance().get(Currency.class, strCurrencyId);
        final String strBPartnerID = jsonParams.getString("received_from");
        BusinessPartner businessPartner = OBDal.getInstance()
            .get(BusinessPartner.class, strBPartnerID);
        String strActualPayment = jsonParams.getString("actual_payment");

        // Format Date
        String strPaymentDate = jsonParams.getString("payment_date");
        Date paymentDate = JsonUtils.createDateFormat().parse(strPaymentDate);

        // OverPayment action
        String strDifferenceAction = "";
        BigDecimal differenceAmount = BigDecimal.ZERO;

        String difference = jsonParams.optString("difference", "");
        if (!difference.isEmpty() && !"null".equals(difference)) {
          differenceAmount = new BigDecimal(difference);
          strDifferenceAction = jsonParams.getString("overpayment_action");
          strDifferenceAction = "RE".equals(strDifferenceAction) ? "refund" : "credit";
        }

        BigDecimal exchangeRate = BigDecimal.ZERO;
        BigDecimal convertedAmount = BigDecimal.ZERO;
        if (jsonParams.get("conversion_rate") != JSONObject.NULL) {
          exchangeRate = new BigDecimal(jsonParams.getString("conversion_rate"));
        }
        if (jsonParams.get("converted_amount") != JSONObject.NULL) {
          convertedAmount = new BigDecimal(jsonParams.getString("converted_amount"));
        }

        List<String> pdToRemove = new ArrayList<>();
        FIN_Payment payment;
        if (!StringUtils.equals(paymentId, "null")) {
          // Payment is already created. Load it.
          payment = OBDal.getInstance().get(FIN_Payment.class, paymentId);
          String strReferenceNo = "";
          if (jsonParams.get(REFERENCE_NO) != JSONObject.NULL) {
            strReferenceNo = jsonParams.getString(REFERENCE_NO);
          }
          payment.setReferenceNo(strReferenceNo);
          // Load existing lines to be deleted.
          pdToRemove = OBDao.getIDListFromOBObject(payment.getFINPaymentDetailList());
        } else {
            payment = createNewPayment(jsonParams, isReceipt, org, businessPartner, paymentDate,
                currency, exchangeRate, convertedAmount, strActualPayment);
        }
        payment.setAmount(new BigDecimal(strActualPayment));
        FIN_AddPayment.setFinancialTransactionAmountAndRate(vars, payment, exchangeRate,
            convertedAmount);
        OBDal.getInstance().save(payment);

        addCredit(payment, jsonParams, differenceAmount);
        addSelectedPSDs(payment, jsonParams, pdToRemove);
        addGLItems(payment, jsonParams);

        removeNotSelectedPaymentDetails(payment, pdToRemove);

        if (strAction.equals("PRP") || strAction.equals("PPP") || strAction.equals("PRD")
            || strAction.equals("PPW")) {

          OBError message = processPayment(payment, strAction, strDifferenceAction, differenceAmount,
              exchangeRate, jsonParams, comingFrom);
          JSONObject errorMessage = new JSONObject();
          errorMessage.put(SEVERITY, message.getType().toLowerCase());
          errorMessage.put(TITLE, message.getTitle());
          errorMessage.put(TEXT, message.getMessage());
          jsonResponse.put(RETRY_EXECUTION, openedFromMenu);
          jsonResponse.put(MESSAGE, errorMessage);
          jsonResponse.put(REFRESH_PARENT, false);
          if (!"TRANSACTION".equals(comingFrom)) {
            jsonResponse.put(REFRESH_PARENT, true);
          }
          JSONObject setSelectorValueFromRecord = new JSONObject();
          JSONObject jsonRecord = new JSONObject();
          JSONObject responseActions = new JSONObject();
          jsonRecord.put("value", payment.getId());
          jsonRecord.put("map", payment.getIdentifier());
          setSelectorValueFromRecord.put("record", jsonRecord);
          responseActions.put("setSelectorValueFromRecord", setSelectorValueFromRecord);
          if (openedFromMenu) {
            responseActions.put("reloadParameters", setSelectorValueFromRecord);
          }
          jsonResponse.put(RESPONSE_ACTION, responseActions);

        }
        JSONObject resultPost = executeHooks(jsonParams, POST_PROCESS_METHOD);
        if(resultPost != null) {
          return resultPost;
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonResponse;
  }

  /**
   * Override this and return a Class<T> in case your action supports only one entity (T will be your entity type).
   * In case your action supports multiple entities, return a BaseOBObject.class.
   * Use with {@link #getInputContents(Class)} to obtain a typed list with the class of your choice.
   *
   * @return a Class of the type your action supports (example: Class<\Invoice> Invoice.class)
   */
  @Override
  protected Class<FIN_Payment> getInputClass() {
    return FIN_Payment.class;
  }

  private FIN_Payment createNewPayment(JSONObject jsonParams, boolean isReceipt, Organization org,
      BusinessPartner bPartner, Date paymentDate, Currency currency, BigDecimal conversionRate,
      BigDecimal convertedAmt, String strActualPayment)
      throws OBException, JSONException, SQLException {

    String strPaymentDocumentNo = jsonParams.getString("payment_documentno");
    String strReferenceNo = "";
    if (jsonParams.get(REFERENCE_NO) != JSONObject.NULL) {
      strReferenceNo = jsonParams.getString(REFERENCE_NO);
    }
    String strFinancialAccountId = jsonParams.getString("fin_financial_account_id");
    FIN_FinancialAccount finAccount = OBDal.getInstance()
        .get(FIN_FinancialAccount.class, strFinancialAccountId);
    String strPaymentMethodId = jsonParams.getString("fin_paymentmethod_id");
    FIN_PaymentMethod paymentMethod = OBDal.getInstance()
        .get(FIN_PaymentMethod.class, strPaymentMethodId);

    boolean paymentDocumentEnabled = getDocumentConfirmation(finAccount, paymentMethod, isReceipt,
        strActualPayment, true);
    boolean documentEnabled;
    if (FIN_Utility.isAutomaticDepositWithdrawn(finAccount, paymentMethod,
        isReceipt) && new BigDecimal(strActualPayment).signum() != 0) {
      documentEnabled = paymentDocumentEnabled
          || getDocumentConfirmation(finAccount, paymentMethod, isReceipt, strActualPayment, false);
    } else {
      documentEnabled = paymentDocumentEnabled;
    }

    DocumentType documentType = FIN_Utility.getDocumentType(org, isReceipt ? "ARR" : "APP");
    String strDocBaseType = documentType != null ? documentType.getDocumentCategory() : "";

    OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(OBContext.getOBContext().getCurrentClient().getId());
    boolean orgLegalWithAccounting = osp.getLegalEntityOrBusinessUnit(org)
        .getOrganizationType()
        .isLegalEntityWithAccounting();
    if (documentEnabled
        && !FIN_Utility.isPeriodOpen(OBContext.getOBContext().getCurrentClient().getId(),
        strDocBaseType, org.getId(), OBDateUtils.formatDate(paymentDate))
        && orgLegalWithAccounting) {
      String message = OBMessageUtils.messageBD("PeriodNotAvailable");
      log.debug(message);
      throw new OBException(message, false);
    }

    String strPaymentAmount = "0";
    if (strPaymentDocumentNo.startsWith("<")) {
      // get DocumentNo
      strPaymentDocumentNo = FIN_Utility.getDocumentNo(documentType, FIN_PAYMENT);
    }

    try {
      OBContext.setAdminMode(false);
      FIN_Payment payment = (new AdvPaymentMngtDao()).getNewPayment(isReceipt, org, documentType,
          strPaymentDocumentNo, bPartner, paymentMethod, finAccount, strPaymentAmount, paymentDate,
          strReferenceNo, currency, conversionRate, convertedAmt);
      OBDal.getInstance().getConnection(true).commit();
      return payment;
    } catch (Exception e) {
      final Throwable ex = DbUtility.getUnderlyingSQLException(e.getCause() != null ? e.getCause() : e);
      final String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
      throw new OBException(message);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void addSelectedPSDs(FIN_Payment payment, JSONObject jsonParams, List<String> pdToRemove)
      throws JSONException {
    JSONObject orderInvoiceGrid = jsonParams.getJSONObject("order_invoice");
    JSONArray selectedPSDs = orderInvoiceGrid.getJSONArray(SELECTION);
    for (int i = 0; i < selectedPSDs.length(); i++) {
      JSONObject psdRow = selectedPSDs.getJSONObject(i);
      String strPSDIds = psdRow.getString("id");
      String strPaidAmount = psdRow.getString("amount");
      BigDecimal paidAmount = new BigDecimal(strPaidAmount);

      boolean isWriteOff = psdRow.getBoolean("writeoff");
      // psdIds can be grouped
      List<String> psdIds = Arrays.asList(strPSDIds.replace(" ", "").split(","));
      List<FIN_PaymentScheduleDetail> psds = getOrderedPaymentScheduleDetails(psdIds);
      BigDecimal outstandingAmount;
      BigDecimal remainingAmount = paidAmount;
      boolean isFullPaydAndHasNegativeLines = isFullyPaid(psds, paidAmount)
          && hasNegativeLines(psds);
      for (FIN_PaymentScheduleDetail psd : psds) {
        BigDecimal assignAmount;

        if (psd.getPaymentDetails() != null) {
          // This schedule detail comes from an edited payment so outstanding amount needs to be
          // properly calculated
          List<FIN_PaymentScheduleDetail> outStandingPSDs = FIN_AddPayment.getOutstandingPSDs(psd);
          if (!outStandingPSDs.isEmpty()) {
            outstandingAmount = psd.getAmount().add(outStandingPSDs.get(0).getAmount());
          } else {
            outstandingAmount = psd.getAmount();
          }
          pdToRemove.remove(psd.getPaymentDetails().getId());
        } else {
          outstandingAmount = psd.getAmount();
        }
        // Manage negative amounts
        if ((remainingAmount.signum() > 0 && remainingAmount.compareTo(outstandingAmount) >= 0)
            || (remainingAmount.signum() < 0
            && remainingAmount.compareTo(outstandingAmount) <= 0)) {
          assignAmount = outstandingAmount;
          if (!isFullPaydAndHasNegativeLines) {
            remainingAmount = remainingAmount.subtract(outstandingAmount);
          }
        } else {
          if (isFullPaydAndHasNegativeLines) {
            assignAmount = outstandingAmount;
          } else {
            assignAmount = remainingAmount;
            remainingAmount = BigDecimal.ZERO;
          }
        }
        FIN_AddPayment.updatePaymentDetail(psd, payment, assignAmount, isWriteOff);
      }
    }
  }

  private boolean isFullyPaid(final List<FIN_PaymentScheduleDetail> psds,
      final BigDecimal paidAmount) {
    final Optional<BigDecimal> sumOfAmounts = psds.stream()
        .map(FIN_PaymentScheduleDetail::getAmount)
        .reduce(BigDecimal::add);
    return sumOfAmounts.isPresent() && sumOfAmounts.get().compareTo(paidAmount) == 0;
  }

  private boolean hasNegativeLines(final List<FIN_PaymentScheduleDetail> psds) {
    final List<FIN_PaymentScheduleDetail> negativePsd = psds.stream()
        .filter(t -> t.getAmount().signum() < 0)
        .collect(Collectors.toList());
    return !negativePsd.isEmpty();
  }

  private void addCredit(FIN_Payment payment, JSONObject jsonParams, BigDecimal differenceAmount)
      throws JSONException {
    // Credit to Use Grid
    JSONObject creditToUseGrid = jsonParams.getJSONObject("credit_to_use");
    JSONArray selectedCreditLines = creditToUseGrid.getJSONArray(SELECTION);
    BigDecimal remainingRefundAmt = differenceAmount;
    String strSelectedCreditLinesIds;
    if (selectedCreditLines.length() > 0) {
      strSelectedCreditLinesIds = getSelectedCreditLinesIds(selectedCreditLines);
      List<FIN_Payment> selectedCreditPayment = FIN_Utility.getOBObjectList(FIN_Payment.class,
          strSelectedCreditLinesIds);
      HashMap<String, BigDecimal> selectedCreditPaymentAmounts = getSelectedCreditLinesAndAmount(
          selectedCreditLines, selectedCreditPayment);

      for (final FIN_Payment creditPayment : selectedCreditPayment) {
        BusinessPartner businessPartner = creditPayment.getBusinessPartner();
        if (businessPartner == null) {
          throw new OBException(OBMessageUtils.messageBD("APRM_CreditWithoutBPartner"));
        }
        String currency;
        if (businessPartner.getCurrency() == null) {
          currency = creditPayment.getCurrency().getId();
          businessPartner.setCurrency(creditPayment.getCurrency());
        } else {
          currency = businessPartner.getCurrency().getId();
        }
        if (!creditPayment.getCurrency().getId().equals(currency)) {
          throw new OBException(String.format(OBMessageUtils.messageBD("APRM_CreditCurrency"),
              businessPartner.getCurrency().getISOCode()));
        }
        BigDecimal usedCreditAmt = selectedCreditPaymentAmounts.get(creditPayment.getId());

        // Reset usedCredit by traversing through each credit payment
        if (remainingRefundAmt.compareTo(usedCreditAmt) > 0) {
          remainingRefundAmt = remainingRefundAmt.subtract(usedCreditAmt);
          usedCreditAmt = BigDecimal.ZERO;
        } else {
          usedCreditAmt = usedCreditAmt.subtract(remainingRefundAmt);
          remainingRefundAmt = BigDecimal.ZERO;
        }

        // Set Used Credit = Amount + Previous used credit introduced by the user
        creditPayment.setUsedCredit(usedCreditAmt.add(creditPayment.getUsedCredit()));

        if (usedCreditAmt.compareTo(BigDecimal.ZERO) > 0) {
          // Set Credit description only when it is actually used
          final StringBuilder description = new StringBuilder();
          if (creditPayment.getDescription() != null
              && !creditPayment.getDescription().equals("")) {
            description.append(creditPayment.getDescription()).append("\n");
          }
          description.append(String.format(OBMessageUtils.messageBD("APRM_CreditUsedPayment"),
              payment.getDocumentNo()));
          String truncateDescription = (description.length() > 255)
              ? description.substring(0, 251).concat("...")
              : description.toString();
          creditPayment.setDescription(truncateDescription);
          FIN_PaymentProcess.linkCreditPayment(payment, usedCreditAmt, creditPayment);
        }
        OBDal.getInstance().save(creditPayment);
      }
    }
  }

  private void addGLItems(FIN_Payment payment, JSONObject jsonParams)
      throws JSONException, ServletException {
    // Add GL Item lines
    JSONObject gLItemsGrid = jsonParams.getJSONObject("glitem");
    JSONArray addedGLITemsArray = gLItemsGrid.getJSONArray("_allRows");
    boolean isReceipt = payment.isReceipt();
    for (int i = 0; i < addedGLITemsArray.length(); i++) {
      JSONObject glItem = addedGLITemsArray.getJSONObject(i);
      BigDecimal glItemOutAmt = BigDecimal.ZERO;
      BigDecimal glItemInAmt = BigDecimal.ZERO;

      final String PAID_OUT = "paidOut";
      if (glItem.has(PAID_OUT) && glItem.get(PAID_OUT) != JSONObject.NULL) {
        glItemOutAmt = new BigDecimal(glItem.getString(PAID_OUT));
      }
      final String RECEIVED_IN = "receivedIn";
      if (glItem.has(RECEIVED_IN) && glItem.get(RECEIVED_IN) != JSONObject.NULL) {
        glItemInAmt = new BigDecimal(glItem.getString(RECEIVED_IN));
      }

      BigDecimal glItemAmt;
      if (isReceipt) {
        glItemAmt = glItemInAmt.subtract(glItemOutAmt);
      } else {
        glItemAmt = glItemOutAmt.subtract(glItemInAmt);
      }
      String strGLItemId = null;
      final String GL_ITEM = "gLItem";
      if (glItem.has(GL_ITEM) && glItem.get(GL_ITEM) != JSONObject.NULL) {
        strGLItemId = glItem.getString(GL_ITEM);
        checkID(strGLItemId);
      }

      // Accounting Dimensions
      BusinessPartner businessPartnerGLItem = (BusinessPartner) getAccountDimension(glItem,
          "businessPartner", BusinessPartner.class);
      Product product = (Product) getAccountDimension(glItem, "product", Product.class);
      Project project = (Project) getAccountDimension(glItem, "project", Project.class);
      ABCActivity activity = (ABCActivity) getAccountDimension(glItem, "cActivityDim",
          ABCActivity.class);
      Costcenter costCenter = (Costcenter) getAccountDimension(glItem, "costCenter",
          Costcenter.class);
      Campaign campaign = (Campaign) getAccountDimension(glItem, "cCampaignDim", Campaign.class);
      UserDimension1 user1 = (UserDimension1) getAccountDimension(glItem, "stDimension",
          UserDimension1.class);
      UserDimension2 user2 = (UserDimension2) getAccountDimension(glItem, "ndDimension",
          UserDimension2.class);

      FIN_AddPayment.saveGLItem(payment, glItemAmt,
          OBDal.getInstance().get(GLItem.class, strGLItemId), businessPartnerGLItem, product,
          project, campaign, activity, null, costCenter, user1, user2);
    }
  }

  private BaseOBObject getAccountDimension(final JSONObject glItem, final String dimension,
      final Class<?> clazz) throws JSONException, ServletException {
    if (glItem.has(dimension) && glItem.get(dimension) != JSONObject.NULL) {
      final String dimensionId = glItem.getString(dimension);
      checkID(dimensionId);
      return (BaseOBObject) OBDal.getInstance().get(clazz, dimensionId);
    }
    return null;
  }

  private void removeNotSelectedPaymentDetails(FIN_Payment payment, List<String> pdToRemove) {
    for (String pdId : pdToRemove) {
      FIN_PaymentDetail pd = OBDal.getInstance().get(FIN_PaymentDetail.class, pdId);

      List<String> pdsIds = OBDao.getIDListFromOBObject(pd.getFINPaymentScheduleDetailList());
      for (String strPDSId : pdsIds) {
        FIN_PaymentScheduleDetail psd = OBDal.getInstance()
            .get(FIN_PaymentScheduleDetail.class, strPDSId);

        if (pd.getGLItem() == null) {
          List<FIN_PaymentScheduleDetail> outStandingPSDs = FIN_AddPayment.getOutstandingPSDs(psd);
          if (outStandingPSDs.isEmpty()) {
            FIN_PaymentScheduleDetail newOutstanding = (FIN_PaymentScheduleDetail) DalUtil.copy(psd,
                false);
            newOutstanding.setPaymentDetails(null);
            newOutstanding.setWriteoffAmount(BigDecimal.ZERO);
            newOutstanding.setAmount(psd.getAmount().add(psd.getWriteoffAmount()));
            OBDal.getInstance().save(newOutstanding);
          } else {
            FIN_PaymentScheduleDetail outStandingPSD = outStandingPSDs.get(0);
            // First make sure outstanding amount is not equal zero
            if (outStandingPSD.getAmount()
                .add(psd.getAmount())
                .add(psd.getWriteoffAmount())
                .signum() == 0) {
              OBDal.getInstance().remove(outStandingPSD);
            } else {
              // update existing PD with difference
              outStandingPSD.setAmount(
                  outStandingPSD.getAmount().add(psd.getAmount()).add(psd.getWriteoffAmount()));
              outStandingPSD.setDoubtfulDebtAmount(
                  outStandingPSD.getDoubtfulDebtAmount().add(psd.getDoubtfulDebtAmount()));
              OBDal.getInstance().save(outStandingPSD);
            }
          }
        }

        FIN_PaymentProcess.removePaymentProposalLines(psd);

        pd.getFINPaymentScheduleDetailList().remove(psd);
        OBDal.getInstance().save(pd);
        OBDal.getInstance().remove(psd);
      }
      payment.getFINPaymentDetailList().remove(pd);
      OBDal.getInstance().save(payment);
      OBDal.getInstance().remove(pd);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(payment);
    }
  }

  private OBError processPayment(FIN_Payment payment, String strAction, String strDifferenceAction,
      BigDecimal refundAmount, BigDecimal exchangeRate, JSONObject jsonParams, String comingFrom)
      throws Exception {
    ConnectionProvider conn = new DalConnectionProvider(true);
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

    AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
    BigDecimal assignedAmount = BigDecimal.ZERO;
    for (FIN_PaymentDetail paymentDetail : payment.getFINPaymentDetailList()) {
      assignedAmount = assignedAmount.add(paymentDetail.getAmount());
    }

    if (assignedAmount.compareTo(payment.getAmount()) < 0) {
      FIN_PaymentScheduleDetail refundScheduleDetail = dao.getNewPaymentScheduleDetail(
          payment.getOrganization(), payment.getAmount().subtract(assignedAmount));
      dao.getNewPaymentDetail(payment, refundScheduleDetail,
          payment.getAmount().subtract(assignedAmount), BigDecimal.ZERO, false, null);
    }

    String strAction1 = (strAction.equals("PRP") || strAction.equals("PPP")) ? "P" : "D";
    OBError message = FIN_AddPayment.processPayment(vars, conn,
        strAction1, payment, comingFrom);
    String strNewPaymentMessage = OBMessageUtils
        .parseTranslation("@PaymentCreated@" + " " + payment.getDocumentNo()) + ".";
    if (!"Error".equalsIgnoreCase(message.getType())) {
      message.setMessage(strNewPaymentMessage + " " + message.getMessage());
      message.setType(message.getType().toLowerCase());
    }
    if (!strDifferenceAction.equals("refund")) {
      return message;
    }
    boolean newPayment = !payment.getFINPaymentDetailList().isEmpty();
    JSONObject creditToUseGrid = jsonParams.getJSONObject("credit_to_use");
    JSONArray selectedCreditLines = creditToUseGrid.getJSONArray(SELECTION);
    String strSelectedCreditLinesIds = null;
    if (selectedCreditLines.length() > 0) {
      strSelectedCreditLinesIds = getSelectedCreditLinesIds(selectedCreditLines);
    }
    FIN_Payment refundPayment = FIN_AddPayment.createRefundPayment(conn, vars, payment,
        refundAmount.negate(), exchangeRate);

    // If refunded credit is generated in the same payment, add payment id to
    // strSelectedCreditLinesIds
    BigDecimal actualPayment = new BigDecimal(jsonParams.getString("actual_payment"));
    if (actualPayment.compareTo(BigDecimal.ZERO) != 0) {
      if (!StringUtils.isEmpty(strSelectedCreditLinesIds)) {
        strSelectedCreditLinesIds = "(" + payment.getId() + ", "
            + strSelectedCreditLinesIds.substring(1);
      } else {
        strSelectedCreditLinesIds = "(" + payment.getId() + ")";
      }
    }

    OBError auxMessage = FIN_AddPayment.processPayment(vars, conn,
        strAction1, refundPayment, comingFrom,
        strSelectedCreditLinesIds);
    if (newPayment && !"Error".equalsIgnoreCase(auxMessage.getType())) {
      final String strNewRefundPaymentMessage = OBMessageUtils
          .parseTranslation("@APRM_RefundPayment@" + ": " + refundPayment.getDocumentNo()) + ".";
      message.setMessage(strNewRefundPaymentMessage + " " + message.getMessage());
      if (payment.getGeneratedCredit().compareTo(BigDecimal.ZERO) != 0) {
        payment.setDescription(payment.getDescription() + strNewRefundPaymentMessage + "\n");
        OBDal.getInstance().save(payment);
        OBDal.getInstance().flush();
      }
    } else {
      message = auxMessage;
    }

    return message;
  }

  private List<FIN_PaymentScheduleDetail> getOrderedPaymentScheduleDetails(List<String> psdSet) {
    //@formatter:off
    String hql =
        "as psd" +
            " where psd.id in (:psdSet)" +
            " order by psd.paymentDetails, abs(psd.amount)";
    //@formatter:on

    return OBDal.getInstance()
        .createQuery(FIN_PaymentScheduleDetail.class, hql)
        .setNamedParameter("psdSet", psdSet)
        .list();
  }

  private void checkID(final String id) throws ServletException {
    if (!IsIDFilter.instance.accept(id)) {
      log.error("Input: {} not accepted by filter: IsIDFilter", id);
      throw new ServletException("Input: " + id + " is not an accepted input");
    }
  }

  /**
   * @param allSelection
   *     Selected Rows in Credit to use grid
   * @return a String with the concatenation of the selected rows ids
   */
  private String getSelectedCreditLinesIds(JSONArray allSelection) throws JSONException {
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    for (int i = 0; i < allSelection.length(); i++) {
      JSONObject selectedRow = allSelection.getJSONObject(i);
      sb.append(selectedRow.getString("id")).append(",");
    }
    sb.replace(sb.lastIndexOf(","), sb.lastIndexOf(",") + 1, ")");
    return sb.toString();
  }

  private HashMap<String, BigDecimal> getSelectedCreditLinesAndAmount(final JSONArray allSelection,
      final List<FIN_Payment> selectedCreditPayments) throws JSONException {
    final HashMap<String, BigDecimal> selectedCreditLinesAmounts = new HashMap<>();

    for (final FIN_Payment creditPayment : selectedCreditPayments) {
      for (int i = 0; i < allSelection.length(); i++) {
        final JSONObject selectedRow = allSelection.getJSONObject(i);
        if (selectedRow.getString("id").equals(creditPayment.getId())) {
          selectedCreditLinesAmounts.put(creditPayment.getId(),
              new BigDecimal(selectedRow.getString("paymentAmount")));
        }
      }
    }
    return selectedCreditLinesAmounts;
  }

  private boolean getDocumentConfirmation(FIN_FinancialAccount finAccount,
      FIN_PaymentMethod finPaymentMethod, boolean isReceipt, String strPaymentAmount,
      boolean isPayment) {
    // Checks if this step is configured to generate accounting for the selected financial account
    boolean confirmation = false;
    OBContext.setAdminMode(true);
    try {
      OBCriteria<FinAccPaymentMethod> obCriteria = OBDal.getInstance()
          .createCriteria(FinAccPaymentMethod.class);
      obCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, finAccount));
      obCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, finPaymentMethod));
      obCriteria.setFilterOnReadableClients(false);
      obCriteria.setFilterOnReadableOrganization(false);
      obCriteria.setMaxResults(1);
      FinAccPaymentMethod finAccPayMethod = (FinAccPaymentMethod) obCriteria.uniqueResult();
      String uponUse;
      if (isPayment) {
        if (isReceipt) {
          uponUse = finAccPayMethod.getUponReceiptUse();
        } else {
          uponUse = finAccPayMethod.getUponPaymentUse();
        }
      } else {
        if (isReceipt) {
          uponUse = finAccPayMethod.getUponDepositUse();
        } else {
          uponUse = finAccPayMethod.getUponWithdrawalUse();
        }
      }
      for (FIN_FinancialAccountAccounting account : finAccount.getFINFinancialAccountAcctList()) {
        if (confirmation) {
          return true;
        }
        if (isReceipt) {
          if ("INT".equals(uponUse) && account.getInTransitPaymentAccountIN() != null || "DEP".equals(
              uponUse) && account.getDepositAccount() != null
              || "CLE".equals(uponUse) && account.getClearedPaymentAccount() != null) {
            confirmation = true;
          }
        } else {
          if ("INT".equals(uponUse) && account.getFINOutIntransitAcct() != null
              || "WIT".equals(uponUse) && account.getWithdrawalAccount() != null
              || "CLE".equals(uponUse) && account.getClearedPaymentAccountOUT() != null) {
            confirmation = true;
          }
        }
        // For payments with Amount ZERO always create an entry as no transaction will be created
        if (isPayment) {
          BigDecimal amount = new BigDecimal(strPaymentAmount);
          if (amount.signum() == 0) {
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

  public static OBError processMultiPayment(FIN_Payment payment,
      String strAction) throws Exception {
    ConnectionProvider conn = new DalConnectionProvider(true);
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    try {
      String strAction1 = (strAction.equals("PRP") || strAction.equals("PPP")) ? "P" : "D";
      return FIN_AddPayment.processPayment(vars, conn, strAction1, payment, null);
    } catch (Exception e) {
      log.info(e);
      throw e;
    }
  }

  protected JSONObject executeHooks(JSONObject jsonParams, String methodName) throws JSONException {
    List<PaymentProcessHook> hookList = PaymentProcessOrderHook.sortHooksByPriority(hooks);
    for (PaymentProcessHook hook : hookList) {
      JSONObject resultHook = null;
      if (StringUtils.equals(methodName, PRE_PROCESS_METHOD)) {
        resultHook = hook.preProcess(jsonParams);
      } else if (StringUtils.equals(methodName, POST_PROCESS_METHOD)) {
        resultHook = hook.posProcess(jsonParams);
      }

      JSONObject message = (resultHook != null && resultHook.has(MESSAGE)) ? resultHook.getJSONObject(MESSAGE) : null;
      if (message != null && message.has(SEVERITY)
          && StringUtils.equalsIgnoreCase(ERROR, message.getString(SEVERITY))) {
        return resultHook;
      }
    }
    return null;
  }
}