package com.smf.jobs.defaults;

import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;
import com.smf.jobs.Action;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.DbUtility;

/**
 * Action for processing orders.
 * Allows for the same actions available in the UI as part of a Job.
 */
public class ProcessOrders extends Action {
    Logger log = LogManager.getLogger();

    private static final String C_ORDER_POST_ID = "104";

    @Override
    protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
        var result = new ActionResult();
        var processMessages = new StringBuilder();
        int errors = 0;

        result.setType(Result.Type.SUCCESS);

        try {
            var input = getInputContents(getInputClass());
            var documentAction = parameters.getString("DocAction");

            log.debug("Process Invoice Action Parameters:");
            log.debug(parameters.toString());

            for (Order order : input) {
                var message = processOrder(order, documentAction);
                if (message.getType().equals("Error")) {
                    errors++;
                }
                if (message.getMessage().isBlank()) {
                    processMessages.append(order.getDocumentNo()).append(": ").append(message.getTitle()).append("\n");
                } else {
                    processMessages.append(order.getDocumentNo()).append(": ").append(message.getMessage()).append("\n");
                }
            }

            if (errors == input.size()) {
                result.setType(Result.Type.ERROR);
            } else if (errors > 0) {
                result.setType(Result.Type.WARNING);
            }

            if (input.size() > 1) {
                // Show the message in a pop up when more than one invoice was selected, for better readability.
                var jsonMessage = new JSONObject();
                jsonMessage.put("message", processMessages.toString().replaceAll("\n","<br>"));
                result.setResponseActionsBuilder(getResponseBuilder().addCustomResponseAction("smartclientSay", jsonMessage));
            }

            result.setMessage(processMessages.toString());
            result.setOutput(getInput());
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            result.setType(Result.Type.ERROR);
            result.setMessage(e.getMessage());
        }

        return result;
    }

    private OBError processOrder(Order order, String documentAction) {
        Process process;
        try {
            OBContext.setAdminMode(true);
            process = OBDal.getInstance().get(Process.class, C_ORDER_POST_ID);
        } finally {
            OBContext.restorePreviousMode();
        }

        try {
            order.setDocumentAction(documentAction);
            OBDal.getInstance().flush();

            final ProcessInstance pinstance = CallProcess.getInstance()
                    .call(process, order.getId(), null);

            return OBMessageUtils.getProcessInstanceMessage(pinstance);
        } catch (Exception e) {
            OBDal.getInstance().rollbackAndClose();
            return OBMessageUtils
                    .translateError(DbUtility.getUnderlyingSQLException(e).getMessage());
        }
    }

    @Override
    protected Class<Order> getInputClass() {
        return Order.class;
    }
}
