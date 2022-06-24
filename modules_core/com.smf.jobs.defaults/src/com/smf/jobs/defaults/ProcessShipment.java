package com.smf.jobs.defaults;

import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;
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
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.DbUtility;

import java.text.ParseException;

/**
 * Action for processing shipment.
 * Allows for the same actions available in the UI as part of a Job.
 */
public class ProcessShipment extends Action {
     Logger log = LogManager.getLogger();

    private static final String M_INOUT_POST_ID = "109";

    @Override
    protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
        var result = new ActionResult();

        try {
            var input = getInputContents(getInputClass());
            var documentAction = parameters.getString("DocAction");
            var processMessages = new StringBuilder();
            int errors = 0;

            result.setType(Result.Type.SUCCESS);

            log.debug("Process Shipment Action Parameters:");
            log.debug(parameters.toString());

            for (ShipmentInOut shipmentInOut : input) {
                var message = processShipment(shipmentInOut, documentAction);
                if (message.getType().equals("Error")) {
                    errors++;
                }
                if (message.getMessage().isBlank()) {
                    processMessages.append(shipmentInOut.getDocumentNo()).append(": ").append(message.getTitle()).append("\n");
                } else {
                    processMessages.append(shipmentInOut.getDocumentNo()).append(": ").append(message.getMessage()).append("\n");
                }
            }

            if (errors == input.size()) {
                result.setType(Result.Type.ERROR);
            } else if (errors > 0) {
                result.setType(Result.Type.WARNING);
            }

            if (input.size() > 1) {
                // Show the message in a pop up when more than one shipment was selected, for better readability.
                var jsonMessage = new JSONObject();
                jsonMessage.put("message", processMessages.toString().replaceAll("\n","<br>"));
                result.setResponseActionsBuilder(getResponseBuilder().addCustomResponseAction("smartclientSay", jsonMessage));
            }

            result.setMessage(processMessages.toString());
            result.setOutput(getInput());


        } catch (JSONException | ParseException e) {
            log.error(e.getMessage(), e);
            result.setType(Result.Type.ERROR);
            result.setMessage(e.getMessage());
        }

        return result;
    }

    private OBError processShipment(ShipmentInOut shipmentInOut, String docAction) throws ParseException {

        Process process;
        try {
            OBContext.setAdminMode(true);
            process = OBDal.getInstance().get(Process.class, M_INOUT_POST_ID);
        } finally {
            OBContext.restorePreviousMode();
        }

        try {
            shipmentInOut.setDocumentAction(docAction);
            OBDal.getInstance().flush();

            final ProcessInstance pinstance = CallProcess.getInstance()
                    .call(process, shipmentInOut.getId(), null);

            return OBMessageUtils.getProcessInstanceMessage(pinstance);
        } catch (Exception e) {
            OBDal.getInstance().rollbackAndClose();
            return OBMessageUtils
                    .translateError(DbUtility.getUnderlyingSQLException(e).getMessage());
        }
    }

    @Override
    protected Class<ShipmentInOut> getInputClass() {
        return ShipmentInOut.class;
    }
}
