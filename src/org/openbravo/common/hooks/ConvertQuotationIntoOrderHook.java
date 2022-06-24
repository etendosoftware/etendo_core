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
 * All portions are Copyright (C) 2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.hooks;

import org.openbravo.model.common.order.Order;

public abstract class ConvertQuotationIntoOrderHook {
  public static final String CONVERT_QUOTATION_INTO_ORDER_HOOK_QUALIFIER = "ConvertQuotationIntoOrderHookQualifier";

  /**
   * Returns the order when the concrete hook will be executed.
   */
  public abstract int getOrder();

  /**
   * Executes the hook logic on the ConvertQuotationIntoOrder process
   * 
   * @param order
   *          the order document with lines we are creating from Quotation
   */
  public abstract void exec(Order order);

}
