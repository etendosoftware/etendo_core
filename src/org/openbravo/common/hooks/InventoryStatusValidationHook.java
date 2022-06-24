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

import org.openbravo.base.exception.OBException;
import org.openbravo.model.materialmgmt.onhandquantity.InventoryStatus;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;

public interface InventoryStatusValidationHook {

  public void exec(StorageDetail storageDetail, InventoryStatus newStatus) throws OBException;

}

// Example of a hook:

// package org.openbravo.warehouse.advancedwarehouseoperations.hooks;
//
// import javax.enterprise.context.ApplicationScoped;
//
// import org.openbravo.base.exception.OBException;
// import org.openbravo.common.hooks.InventoryStatusValidationHook;
// import org.openbravo.erpCommon.utility.OBMessageUtils;
// import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
// import org.openbravo.warehouse.advancedwarehouseoperations.utils.Utilities;
//
// @ApplicationScoped
// public class InventoryStatusValidatorHookImplementation implements InventoryStatusValidationHook
// {
//
// @Override
// public void exec(Locator locator, InventoryStatus newStatus) throws OBException {
// if (Utilities.existTasksForStorageDetail(locator)) {
// throw new OBException(OBMessageUtils.messageBD("OBAWO_StorageDetailWithTask"));
// // Hook stuff goes here
// }
// }
//
// }
