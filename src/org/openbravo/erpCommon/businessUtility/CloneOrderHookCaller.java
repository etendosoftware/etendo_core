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

import java.util.Iterator;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.openbravo.model.common.order.Order;

/**
 * Public class implemented to execute all hooks of type CloneOrderHook. This class is called from
 * Clone order process.
 * 
 * @author Andy Armaignac Collazo
 *
 */
public class CloneOrderHookCaller {

  @Inject
  @Any
  private Instance<CloneOrderHook> cloneOrderHookProcess;

  /**
   * Method that executes all classes of type CloneOrderHook.
   * 
   * @param order
   *          Order to be clone
   * @throws Exception
   */
  public void executeHook(Order order) throws Exception {
    executeHooks(order);
  }

  private void executeHooks(Order order) throws Exception {
    for (Iterator<CloneOrderHook> processIterator = cloneOrderHookProcess
        .iterator(); processIterator.hasNext();) {
      CloneOrderHook process = processIterator.next();
      process.exec(order);
    }
  }
}
