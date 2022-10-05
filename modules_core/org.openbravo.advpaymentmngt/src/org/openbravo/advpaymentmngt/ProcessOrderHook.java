package org.openbravo.advpaymentmngt;

import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.order.Order;

/**
 * @author <a href="mailto:juan.baez@smfconsulting.es">Juan Baez</a>
 * 
 */
public interface ProcessOrderHook {

  /*
   * Returns an OBError when an error occurred and null if it succeed
   */
  public OBError preProcess(Order order, String strDocAction);

  /*
   * Returns an OBError when an error occurred and null if it succeed
   */
  public OBError postProcess(Order order, String strDocAction);
}
