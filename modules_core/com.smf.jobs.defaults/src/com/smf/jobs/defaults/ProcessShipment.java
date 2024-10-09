package com.smf.jobs.defaults;

import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.ProcessShipmentUtil;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.service.db.DalConnectionProvider;

import java.text.ParseException;
import java.util.List;

import javax.inject.Inject;

/**
 * Action for processing shipment.
 * Allows for the same actions available in the UI as part of a Job.
 */
public class ProcessShipment extends Action {
  Logger log = LogManager.getLogger();

  @Inject
  private WeldUtils weldUtils;

  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
    var result = new ActionResult();

    try {
      var input = getInputContents(getInputClass());
      var documentAction = parameters.getString("DocAction");
      int errors = 0;
      int success = 0;

      result.setType(Result.Type.SUCCESS);

      log.debug("Process Shipment Action Parameters:");
      log.debug(parameters.toString());

      for (ShipmentInOut shipmentInOut : input) {
        var message = processShipment(shipmentInOut, documentAction);
        if (StringUtils.equalsIgnoreCase("error", message.getType())) {
          errors++;
        }
        if (StringUtils.equalsIgnoreCase("success", message.getType())) {
          success++;
        }
        result.setMessage(
            message.getTitle().isEmpty() ? message.getMessage() : message.getTitle().concat(
                ": ").concat(message.getMessage()));
      }

      massiveMessageHandler(result, input, errors, success);
    } catch (JSONException | ParseException e) {
      log.error(e.getMessage(), e);
      result.setType(Result.Type.ERROR);
      result.setMessage(e.getMessage());
    }

    return result;
  }

  private void massiveMessageHandler(ActionResult result, List<ShipmentInOut> inputs, int errors, int success) {
    if (inputs.size() > 1) {
      if (success == inputs.size()) {
        result.setType(Result.Type.SUCCESS);
      } else if (errors == inputs.size()) {
        result.setType(Result.Type.ERROR);
      } else {
        result.setType(Result.Type.WARNING);
      }
      result.setMessage(String.format(OBMessageUtils.messageBD("DJOBS_PostUnpostMessage"), success, errors));
      result.setOutput(getInput());
    }
  }

  private OBError processShipment(ShipmentInOut shipmentInOut, String docAction) throws ParseException {
    var processor = weldUtils.getInstance(ProcessShipmentUtil.class);

    return processor.process(
        shipmentInOut.getId(),
        docAction,
        RequestContext.get().getVariablesSecureApp(),
        new DalConnectionProvider(false)
    );
  }

  @Override
  protected Class<ShipmentInOut> getInputClass() {
    return ShipmentInOut.class;
  }
}
