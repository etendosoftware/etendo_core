package com.smf.jobs.defaults;

import java.text.ParseException;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;
import com.smf.jobs.Action;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.ProcessInvoiceHook;
import org.openbravo.advpaymentmngt.ProcessInvoiceUtil;
import org.openbravo.advpaymentmngt.ProcessOrderHook;
import org.openbravo.advpaymentmngt.ProcessOrderUtil;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.json.JsonUtils;

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
        if (StringUtils.equals("Error", message.getType())) {
          errors++;
        }
        if (StringUtils.isBlank(message.getMessage())) {
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
        jsonMessage.put("message", processMessages.toString().replaceAll("\n", "<br>"));
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
