package com.etendoerp.common.hooks;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@ApplicationScoped
public class HookManager {

  @Inject
  @Any
  private Instance<ChangeOrderLineQtyHook> orderLineQtyHooks;

  public void executeHooks(OrderLineQtyChangedHookObject hookObject) throws Exception {
    for (ChangeOrderLineQtyHook hook : orderLineQtyHooks) {
      hook.exec(hookObject);
    }
  }

}
