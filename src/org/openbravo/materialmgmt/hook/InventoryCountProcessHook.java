package org.openbravo.materialmgmt.hook;

import org.openbravo.model.materialmgmt.transaction.InventoryCount;

public interface InventoryCountProcessHook {

  public void exec(InventoryCount inventory) throws Exception;
}
