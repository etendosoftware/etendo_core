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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.model.common.order.Order;

@ApplicationScoped
public class ConvertQuotationIntoOrderHookManager {
  @Inject
  @Any
  private Instance<ConvertQuotationIntoOrderHook> convertQuotationIntoOrderHooks;

  /**
   * Executes hook list
   * 
   * @param order
   *          the order document with lines we are creating from Quotation
   */
  public void executeHooks(Order order) {
    if (convertQuotationIntoOrderHooks != null) {
      final List<ConvertQuotationIntoOrderHook> hooks = new ArrayList<>();
      for (ConvertQuotationIntoOrderHook hook : convertQuotationIntoOrderHooks
          .select(new ComponentProvider.Selector(
              ConvertQuotationIntoOrderHook.CONVERT_QUOTATION_INTO_ORDER_HOOK_QUALIFIER))) {
        if (hook != null) {
          hooks.add(hook);
        }
      }

      Collections.sort(hooks, new ConvertQuotationIntoOrderHookComparator());
      for (ConvertQuotationIntoOrderHook hook : hooks) {
        hook.exec(order);
      }
    }
  }

  private class ConvertQuotationIntoOrderHookComparator
      implements Comparator<ConvertQuotationIntoOrderHook> {
    @Override
    public int compare(final ConvertQuotationIntoOrderHook a,
        final ConvertQuotationIntoOrderHook b) {
      if (a.getOrder() < b.getOrder()) {
        return -1;
      } else if (a.getOrder() == b.getOrder()) {
        return 0;
      } else {
        return 1;
      }
    }
  }
}
