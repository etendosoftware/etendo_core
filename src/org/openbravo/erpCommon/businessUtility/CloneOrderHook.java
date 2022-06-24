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

import org.openbravo.model.common.order.Order;

/**
 * Abstract class created to implement hooks inside Clone Order process. All hooks in that process
 * must extend this class so they implement exec process.
 * 
 * @author Andy Armaignac Collazo
 * 
 */
public abstract class CloneOrderHook {

  /**
   * Exec method that all hooks extending this abstract class must implement
   * 
   * @param order
   *          Order to be cloned
   * @throws Exception
   */
  public abstract void exec(Order order) throws Exception;
}
