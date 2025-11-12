package com.etendoerp.advpaymentmngt.actionHandler;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.PaymentExecutionProcess;

import jakarta.enterprise.context.Dependent;

@Dependent
public class PaymentExecutionProcessActionHandler extends BaseActionHandler {

    private static final Logger log = Logger.getLogger(PaymentExecutionProcessActionHandler.class);

    @Override
    protected JSONObject execute(Map<String, Object> parameters, String content) {
        try {
            final JSONObject request = new JSONObject(content);
            // Get the passed parameters
            JSONObject params = request.getJSONObject("_params");
            String executionProcessId = params.getString(PaymentExecutionActionHandler.EXECUTION_PROCESS_ID);
            String selectedPaymentsIds = params.getString(PaymentExecutionActionHandler.SELECTED_PAYMENTS_IDS);
            String organizationId = params.getString(PaymentExecutionActionHandler.ORGANIZATION_ID);

            // Get the process parameters values set by the user
            JSONArray processParametersValues = request.getJSONArray(PaymentExecutionActionHandler.PROCESS_PARAMETERS);

            return PaymentExecutionUtils.processAndClose(
                    PaymentExecutionUtils.getObject(PaymentExecutionProcess.class, executionProcessId),
                    selectedPaymentsIds,
                    PaymentExecutionUtils.getObject(Organization.class, organizationId),
                    generateProcessParametersMap(processParametersValues)
            );
        } catch (Exception e) {
            return buildResponse(false, "", e.getMessage());
        }
    }

    /**
     * Build a JSONObjet with a custom response.
     * @param isSuccess
     * @param message
     * @param error
     * @return
     */
    static JSONObject buildResponse(boolean isSuccess, String message, String error) {
        JSONObject result = new JSONObject();
        try {
            result.put("success", isSuccess);
            result.put("message", message);
            result.put("error", error);
        } catch (Exception e) {
            log.error("Error building response: " + e.getMessage());
        }
        return result;
    }

    /**
     * parses a JSONArray with the process parameters values to a HashMap.
     * @param processParametersValues A JSONArray with the values of the process parameters set by the user.
     * @return HashMap of the parsed values.
     * @throws JSONException
     */
    static Map<String, String> generateProcessParametersMap(JSONArray processParametersValues) throws JSONException {
        Map<String, String > map = new HashMap<>();
        for (int i = 0; i < processParametersValues.length(); i++) {
            String paramId = processParametersValues.getJSONObject(i).getString("id");
            String paramValue = processParametersValues.getJSONObject(i).getString("value");
            String paramType = processParametersValues.getJSONObject(i).getString("inputType");

            if (paramType.equalsIgnoreCase("CHECK")) {
                paramValue = (paramValue.equalsIgnoreCase("true")) ? "Y" : "N";
            }
            map.put(paramId, paramValue);
        }
        return map;
    }

}
