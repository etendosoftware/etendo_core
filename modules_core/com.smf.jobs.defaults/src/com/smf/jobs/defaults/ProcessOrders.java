package com.smf.jobs.defaults;

import java.text.ParseException;

import javax.inject.Inject;

import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;
import com.smf.jobs.Action;
import com.smf.jobs.defaults.Utils.ProcessUtils;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.ProcessOrderUtil;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Action for processing orders.
 * Allows for the same actions available in the UI as part of a Job.
 */
public class ProcessOrders extends Action {
  Logger log = LogManager.getLogger();

  @Inject
  private WeldUtils weldUtils;

  //private static final String C_ORDER_POST_ID = "104";

  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
    var result = new ActionResult();
    var errors = new MutableInt(0);
    var success = new MutableInt(0);
    OBError message = new OBError();

    result.setType(Result.Type.SUCCESS);

    try {
      var input = getInputContents(getInputClass());
      var documentAction = parameters.getString("DocAction");

      log.debug("Process Invoice Action Parameters:");
      log.debug(parameters.toString());

      for (Order order : input) {
        message = processOrder(order, documentAction);
        ProcessUtils.updateResult(message, errors, success);
      }

      ProcessUtils.massiveMessageHandler(result, message, input, errors, success, getInput());
    } catch (JSONException | ParseException e) {
      log.error(e.getMessage(), e);
      result.setType(Result.Type.ERROR);
      result.setMessage(e.getMessage());
    }

    return result;
  }

  private OBError processOrder(Order order, String docAction) throws ParseException {
    var processor = weldUtils.getInstance(ProcessOrderUtil.class);

    return processor.process(
        order.getId(),
        docAction,
        RequestContext.get().getVariablesSecureApp(),
        new DalConnectionProvider(false)
    );
  }

  @Override
  protected Class<Order> getInputClass() {
    return Order.class;
  }
}
