package com.etendoerp.common.hooks;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

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
