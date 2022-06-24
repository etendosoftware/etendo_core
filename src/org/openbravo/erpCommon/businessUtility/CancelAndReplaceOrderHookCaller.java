/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2016-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.erpCommon.businessUtility;

import java.util.Iterator;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.model.common.order.Order;

/**
 * Public class implemented to execute all hooks of type CancelAndReplaceOrderHook. This class is
 * called from Cancel and Replace process.
 * 
 */
public class CancelAndReplaceOrderHookCaller {
  @Inject
  @Any
  private Instance<CancelAndReplaceOrderHook> cancelAndReplaceOrderHookProcesses;

  /**
   * Method that executes all classes of type CancelAndReplaceOrderHook.
   * 
   * @param replaceOrder
   *          boolean that informs if the old sales order has been replaced or not.
   * @param triggersDisabled
   *          boolean that informs if triggers have been disabled previous to execute this hook.
   * @param oldOrder
   *          Sales Order that has been replaced.
   * @param newOrder
   *          New Sales Order that has replaced the old Sales Order
   * @param inverseOrder
   *          New Sales Order equal to old sales order but with negative quantities. Cancellation
   *          Order.
   * @param jsonorder
   *          JSONObject with information of the order coming with values when the hook is invoked
   *          because the Cancel and Replace has been executed from Web POS.
   */
  public void executeHook(boolean replaceOrder, boolean triggersDisabled, Order oldOrder,
      Order newOrder, Order inverseOrder, JSONObject jsonorder) throws Exception {
    for (Iterator<CancelAndReplaceOrderHook> processIterator = cancelAndReplaceOrderHookProcesses
        .iterator(); processIterator.hasNext();) {
      CancelAndReplaceOrderHook process = processIterator.next();
      process.exec(replaceOrder, triggersDisabled, oldOrder, newOrder, inverseOrder, jsonorder);
    }
  }

  /**
   * Method that executes all classes of type CancelAndReplaceOrderHook.
   * 
   * @param replaceOrder
   *          boolean that informs if the old sales order has been replaced or not.
   * @param triggersDisabled
   *          boolean that informs if triggers have been disabled previous to execute this hook.
   * @param oldOrder
   *          Sales Order that has been replaced.
   * @param newOrders
   *          New Sales Order that has replaced the old Sales Order
   * @param inverseOrder
   *          New Sales Order equal to old sales order but with negative quantities. Cancellation
   *          Order.
   * @param jsonorder
   *          JSONObject with information of the order coming with values when the hook is invoked
   *          because the Cancel and Replace has been executed from Web POS.
   */
  public void executeHook(boolean replaceOrder, boolean triggersDisabled, Order oldOrder,
      List<Order> newOrders, Order inverseOrder, JSONObject jsonorder) throws Exception {
    for (Iterator<CancelAndReplaceOrderHook> processIterator = cancelAndReplaceOrderHookProcesses
        .iterator(); processIterator.hasNext();) {
      CancelAndReplaceOrderHook process = processIterator.next();
      process.exec(replaceOrder, triggersDisabled, oldOrder, newOrders, inverseOrder, jsonorder);
    }
  }
}
