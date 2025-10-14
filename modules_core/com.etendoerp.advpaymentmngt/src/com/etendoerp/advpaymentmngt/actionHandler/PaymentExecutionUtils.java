package com.etendoerp.advpaymentmngt.actionHandler;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.exception.NoExecutionProcessFoundException;
import org.openbravo.advpaymentmngt.process.FIN_ExecutePayment;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.*;

import java.util.*;

public class PaymentExecutionUtils {

  static final String PROCESS_PARAMETER_ID = "id";
  static final String PROCESS_PARAMETER_NAME = "name";
  static final String PROCESS_PARAMETER_INPUT_TYPE = "inputType";
  static final String PROCESS_PARAMETER_DEFAULT_TEXT = "defaultText";
  static final String PROCESS_PARAMETER_DEFAULT_CHECK = "defaultCheck";

  static PaymentExecutionProcess getExecutionProcess(String paymentId) {
    AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
    final FIN_Payment payment = dao.getObject(FIN_Payment.class, paymentId);
    return dao.getExecutionProcess(payment);
  }

  static JSONArray getProcessParameters(PaymentExecutionProcess executionProcess)
      throws JSONException {
    JSONArray jsonArrayParameters = new JSONArray();
    AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
    OBContext.setAdminMode();
    try {
      List<PaymentExecutionProcessParameter> parameters = dao.getInPaymentExecutionParameters(
          executionProcess);
      if (parameters != null && parameters.size() > 0) {
        for (PaymentExecutionProcessParameter parameter : parameters) {
          JSONObject parameterData = new JSONObject();
          parameterData.put(PROCESS_PARAMETER_ID, parameter.getId());
          parameterData.put(PROCESS_PARAMETER_NAME, parameter.getName());
          parameterData.put(PROCESS_PARAMETER_INPUT_TYPE, parameter.getInputType());
          parameterData.put(PROCESS_PARAMETER_DEFAULT_TEXT,
              Optional.ofNullable(parameter.getDefaultTextValue()).orElse(""));
          parameterData.put(PROCESS_PARAMETER_DEFAULT_CHECK,
              Optional.ofNullable(parameter.getDefaultValueForFlag()).orElse(""));
          jsonArrayParameters.put(parameterData);
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonArrayParameters;
  }

  public static <T extends BaseOBObject> T getObject(Class<T> t, String strId) {
    return OBDal.getInstance().get(t, strId);
  }

  public static JSONObject processAndClose(PaymentExecutionProcess executionProcess,
      String strPayments, Organization organization, Map<String, String> processParametersValues) {
    AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
    OBError result = new OBError();
    OBContext.setAdminMode();
    JSONObject jsonResult = new JSONObject();
    try {
      List<PaymentExecutionProcessParameter> executionProcessInParameters = dao.getInPaymentExecutionParameters(
          executionProcess);
      HashMap<String, String> parameters = null;
      if (executionProcessInParameters != null && executionProcessInParameters.size() > 0) {
        parameters = new HashMap<String, String>();
        for (PaymentExecutionProcessParameter parameter : executionProcessInParameters) {
          String strValue = processParametersValues.getOrDefault(parameter.getId(), "");
          parameters.put(parameter.getSearchKey(), strValue);
        }
      }

      List<FIN_Payment> payments = FIN_Utility.getOBObjectList(FIN_Payment.class, strPayments);
      Set<FIN_Payment> paymentSet = new HashSet<>(payments);
      FIN_ExecutePayment executePayment = new FIN_ExecutePayment();
      executePayment.init("MF", executionProcess, payments, parameters, organization);
      result = executePayment.execute();
      String paymentsDocNo = "";
      if ("Success".equals(result.getType())) {
        String message = OBMessageUtils.messageBD(
            PaymentExecutionActionHandler.PAYMENTS_CREATED_MESSAGE) + collectPaymentDocumentsNo(
            paymentSet);
        jsonResult = PaymentExecutionProcessActionHandler.buildResponse(true, message, "");
      } else {
        jsonResult = PaymentExecutionProcessActionHandler.buildResponse(false, "",
            result.getMessage());
      }
    } catch (NoExecutionProcessFoundException e) {
      jsonResult = PaymentExecutionProcessActionHandler.buildResponse(false, "", e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonResult;
  }

  static String collectPaymentDocumentsNo(Set<FIN_Payment> paymentSet) {
    String paymentsDocNo = "";
    for (FIN_Payment payment : paymentSet) {
      paymentsDocNo = paymentsDocNo.concat(", ").concat(payment.getDocumentNo());
    }
    return paymentsDocNo.replaceFirst(",", "");
  }

  static FinAccPaymentMethod getFinancialAccountPaymentMethod(FIN_FinancialAccount account,
      FIN_PaymentMethod paymentMethod) {
    final OBCriteria<FinAccPaymentMethod> obc = OBDal.getInstance()
        .createCriteria(FinAccPaymentMethod.class);
    obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, account));
    obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, paymentMethod));
    obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACTIVE, true));
    obc.setFilterOnReadableClients(false);
    obc.setFilterOnReadableOrganization(false);
    try {
      return obc.list().get(0);
    } catch (Exception e) {
      return null;
    }
  }
}
