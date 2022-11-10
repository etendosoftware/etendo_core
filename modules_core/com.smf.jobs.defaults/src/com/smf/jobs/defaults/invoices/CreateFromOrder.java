package com.smf.jobs.defaults.invoices;

import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.ResponseActionsBuilder;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_forms.GenerateInvoicesHook;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonUtils;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateFromOrder extends Action {
  Logger log = LogManager.getLogger();

  private static final String ORDER_GRID_PARAM = "orderGrid";

  @Inject
  @Any
  private Instance<GenerateInvoicesHook> hooks;

  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
    var result = new ActionResult();
    result.setType(Result.Type.SUCCESS);

    try {
      if (parameters.has("_buttonValue") && "REFRESH".equals(parameters.getString("_buttonValue"))) {
        result.setResponseActionsBuilder(
            getResponseBuilder()
                .retryExecution()
                .refreshGridParameter(ORDER_GRID_PARAM)
        );
        return result;
      }

      var input = getInputContents(getInputClass());

      if (input.isEmpty()) {
        var gridSelection = parameters.getJSONObject(ORDER_GRID_PARAM).getJSONArray("_selection");
        for (int i = 0; i < gridSelection.length(); i++) {
          JSONObject gridRecord = gridSelection.getJSONObject(i);
          input.add(OBDal.getInstance().get(Order.class, gridRecord.getString("id")));
        }
      }

      if (!input.isEmpty()) {
        var selectedIds = input.stream().map(Order::getId).collect(Collectors.toList());
        initSelection(selectedIds);

        String invoiceDateStr = parameters.getString("invoiceDate");
        Date invoiceDate;

        if (StringUtils.equals("null", invoiceDateStr)) {
          invoiceDate = new Date();
        } else {
          invoiceDate = JsonUtils.createDateFormat().parse(invoiceDateStr);
        }

        final Map<String, Object> processParameters = new HashMap<>();
        processParameters.put("Selection",
            "Y"); // Selection is used to collect the orders marked by the initSelection method. See old GenerateInvoicesmanual.java
        processParameters.put("DateInvoiced", invoiceDate);

        var invoiceCreateProcess = OBDal.getInstance().get(Process.class, "134");
        final var processInstance = CallProcess.getInstance().callProcess(invoiceCreateProcess, null, processParameters,
            null);
        var processResult = Result.fromOBError(OBMessageUtils.getProcessInstanceMessage(processInstance));

        resetSelection(selectedIds);

        result.setType(processResult.getType());
        result.setMessage(processResult.getMessage());

        for (GenerateInvoicesHook hook : hooks) {
          var hookResult = hook.executeHook(new DalConnectionProvider().getConnection(), input,
              processResult.toOBError());
          if (hookResult != null && "Error".equals(hookResult.getType())) {
            throw new OBException(hookResult.getMessage());
          }
        }
      }

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      result.setType(Result.Type.ERROR);
      result.setMessage(e.getMessage());
      OBDal.getInstance().rollbackAndClose();
    }

    result.setResponseActionsBuilder(
        getResponseBuilder()
            .retryExecution()
            .refreshGridParameter(ORDER_GRID_PARAM)
            .showMsgInProcessView(ResponseActionsBuilder.MessageType.SUCCESS, result.getMessage())
    );

    return result;
  }

  private void initSelection(List<String> selection) {
    String deselectAllQueryString = "update Order set selected = false where selected = true";
    OBDal.getInstance().getSession().createQuery(deselectAllQueryString).executeUpdate();

    String selectQueryString = "update Order set selected = true where id in :selection";
    var selectQuery = OBDal.getInstance().getSession().createQuery(selectQueryString);
    selectQuery.setParameter("selection", selection);
    selectQuery.executeUpdate();
  }

  private void resetSelection(List<String> selection) {
    String resetSelectionQueryString = "update Order set selected = false where id in :selection";
    var resetSelectionQuery = OBDal.getInstance().getSession().createQuery(resetSelectionQueryString);
    resetSelectionQuery.setParameter("selection", selection);
    resetSelectionQuery.executeUpdate();
  }

  @Override
  protected Class<Order> getInputClass() {
    return Order.class;
  }
}
