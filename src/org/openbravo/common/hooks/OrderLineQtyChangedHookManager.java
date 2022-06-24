package org.openbravo.common.hooks;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@ApplicationScoped
public class OrderLineQtyChangedHookManager {
  @Inject
  @Any
  private Instance<OrderLineQtyChangedHook> orderLineQtyChangedHooks;

  public void executeHooks(OrderLineQtyChangedHookObject hookObject) throws Exception {
    for (OrderLineQtyChangedHook hook : orderLineQtyChangedHooks) {
      hook.exec(hookObject);
    }
  }

}
