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
 * All portions are Copyright (C) 2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.erpCommon.businessUtility;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.model.common.order.Order;

/**
 * Interface created to implement hooks inside Cancel and Replace process in createPayments method.
 * All hooks in that process must implement this class so they implement exec process.
 * 
 */
public interface CancelLayawayPaymentsHook {
  /**
   * Exec method that all hooks implementing this interface must implement.
   * 
   * @param inverseOrder
   *          New Sales Order equal to old sales order but with negative quantities. Cancellation
   *          Order.
   * @param jsonorder
   *          JSONObject with information of the order coming with values when the hook is invoked
   *          because the Cancel and Replace has been executed from Web POS.
   */
  public void exec(JSONObject jsonorder, Order inverseOrder) throws Exception;
}
