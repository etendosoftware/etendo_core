/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.common.hooks;

public interface OrderLineQtyChangedHook {

  public void exec(OrderLineQtyChangedHookObject hookObject) throws Exception;
}

// Example of a hook:
// import javax.enterprise.context.ApplicationScoped;
//
// import org.openbravo.model.common.invoice.Invoice;
// import org.openbravo.model.common.order.Order;
// import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
//
// @ApplicationScoped
// public class OrderLoaderHookTest implements OrderLoaderHook {
//
// @Override
// public void exec(JSONObject jsonorder, Order order, ShipmentInOut shipment, Invoice invoice)
// throws Exception {
// // Hook stuff goes here
// }
//
// }
