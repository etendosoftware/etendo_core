package com.etendoerp.common.hooks;

public interface ChangeOrderLineQtyHook {

  void exec(OrderLineQtyChangedHookObject hookObject) throws Exception;
}
