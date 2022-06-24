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
 * All portions are Copyright (C) 2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.common.hooks;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.openbravo.model.materialmgmt.onhandquantity.InventoryStatus;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;

@ApplicationScoped
public class InventoryStatusHookManager {
  @Inject
  @Any
  private Instance<InventoryStatusValidationHook> inventoryStatusValidationHooks;

  /**
   * Execute Validation Hooks over the given Storage Detail
   * 
   * @param storageDetail
   * @throws Exception
   */
  public void executeValidationHooks(StorageDetail storageDetail, InventoryStatus newStatus)
      throws Exception {
    for (InventoryStatusValidationHook hook : inventoryStatusValidationHooks) {
      hook.exec(storageDetail, newStatus);
    }
  }
}
