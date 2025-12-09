package org.openbravo.erpCommon.hook;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;

import com.smf.jobs.ActionResult;

/**
 * Hook interface for creating Shipment (Goods Shipment) lines from stock reservations.
 * <p>
 * Allows overriding the default logic to split lines using {@code M_Reservation}
 * and {@code M_Reservation_Stock}, preserving traceability and custom rules.
 * </p>
 */
public interface ShipmentLinesFromReservationHook {

  /**
   * Executes the custom logic for creating shipment lines.
   *
   * @param parameters
   *     input parameters (e.g. Shipment ID, Order Line ID)
   * @param isStopped
   *     flag to stop further processing if set to true
   * @return result of the execution
   */
  ActionResult execute(JSONObject parameters, MutableBoolean isStopped);

  /**
   * Defines the execution priority of the hook.
   * Lower values indicate higher priority.
   *
   * @return the priority value (default is 100)
   */
  default int getPriority() {
    return 100;
  }

  /**
   * Determines whether the hook is applicable for the given action type.
   *
   * @param actionType
   *     the type of action being executed
   * @return true if the hook should be applied; false otherwise
   */
  boolean isApplicable(String actionType);
}
