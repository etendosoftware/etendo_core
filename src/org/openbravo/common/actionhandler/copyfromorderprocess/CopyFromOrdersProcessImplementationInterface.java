/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.actionhandler.copyfromorderprocess;

import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;

/**
 * Interface to be implemented by the hooks to be executed on the Copy From Orders process.
 * 
 * Example of a hook:
 * 
 * <pre>
 * import javax.enterprise.context.Dependent;
 * import org.openbravo.client.kernel.ComponentProvider.Qualifier;
 * import org.openbravo.common.actionhandler.copyfromorderprocess.CopyFromOrdersProcessImplementationInterface;
 * import org.openbravo.model.common.order.Order;
 * import org.openbravo.model.common.order.OrderLine;
 * 
 * &#64;Dependent
 * &#64;Qualifier(CopyFromOrdersProcessImplementationInterface.COPY_FROM_ORDER_PROCESS_HOOK_QUALIFIER)
 * public class TestHook implements CopyFromOrdersProcessImplementationInterface {
 * 
 *   &#64;Override
 *   public int getOrder() {
 *     return 10;
 *   }
 * 
 *   &#64;Override
 *   public void exec(Order processingOrder, OrderLine orderLine, OrderLine newOrderLine) {
 *     newOrderLine.setDescription("Test");
 *   }
 * }
 * </pre>
 *
 */
public interface CopyFromOrdersProcessImplementationInterface {
  public static final String COPY_FROM_ORDER_PROCESS_HOOK_QUALIFIER = "CopyFromOrderProcessHookQualifier";

  /**
   * Returns the order when the concrete hook will be implemented. A positive value will execute the
   * hook after the core's logic
   */
  public int getOrder();

  /**
   * Executes the hook logic on the Copy From Orders process
   * 
   * @param newOrder
   *          the new order we are creating
   * @param oldOrderLine
   *          the order line from which we are creating the line
   * @param newOrderLine
   *          the new line created within the newOrder
   */
  public void exec(final Order newOrder, final OrderLine oldOrderLine, OrderLine newOrderLine);
}
