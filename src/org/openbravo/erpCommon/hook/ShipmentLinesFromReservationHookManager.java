package org.openbravo.erpCommon.hook;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;

import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;

/**
 * Manager responsible for executing all registered {@link ShipmentLinesFromReservationHook}
 * implementations when creating Shipment (Goods Shipment) lines from reservations.
 * <p>
 * Hooks are executed in order of their defined priority.
 * </p>
 */
public class ShipmentLinesFromReservationHookManager {

  @Inject
  @Any
  protected Instance<ShipmentLinesFromReservationHook> hooks;

  /**
   * Executes all applicable {@link ShipmentLinesFromReservationHook} instances in priority order.
   *
   * @param parameters
   *     the input parameters used during shipment line creation
   * @param isStopped
   *     flag to indicate if execution should be halted
   * @return the {@link ActionResult} from the last executed hook,
   *     or an error if no hooks are found
   */
  public ActionResult executeHooks(JSONObject parameters, MutableBoolean isStopped) {
    List<ShipmentLinesFromReservationHook> sortedHooks = sortHooksByPriority();
    ActionResult result = new ActionResult();

    List<ShipmentLinesFromReservationHook> applicableHooks = sortedHooks.stream().collect(Collectors.toList());

    if (applicableHooks.isEmpty()) {
      result.setType(Result.Type.ERROR);
      String errorMessage = "No hooks found";
      result.setMessage(errorMessage);
      return result;
    }

    for (ShipmentLinesFromReservationHook hook : applicableHooks) {
      result = hook.execute(parameters, isStopped);
    }

    return result;
  }

  /**
   * Sorts all available {@link ShipmentLinesFromReservationHook} implementations by priority.
   *
   * @return a list of hooks sorted in ascending order of priority
   */
  public List<ShipmentLinesFromReservationHook> sortHooksByPriority() {
    List<ShipmentLinesFromReservationHook> hookList = new ArrayList<>();
    hooks.forEach(hookList::add);
    hookList.sort((h1, h2) -> Integer.compare(h1.getPriority(), h2.getPriority()));
    return hookList;
  }

}
