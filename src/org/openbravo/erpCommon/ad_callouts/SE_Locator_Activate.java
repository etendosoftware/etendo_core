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
 * All portions are Copyright (C) 2012-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.onhandquantity.StoragePending;

public class SE_Locator_Activate extends SimpleCallout {

  private static final String STORAGEBIN_TAB = "178";
  private static final String WAREHOUSE_TAB = "177";

  @Override
  protected void execute(final CalloutInfo info) throws ServletException {

    final VariablesSecureApp vars = info.vars;
    final String active = vars.getStringParameter("inpisactive");
    final String strLocator = vars.getStringParameter("inpmLocatorId");
    final String tab = vars.getStringParameter("inpTabId");
    final String strWarehouse = vars.getStringParameter("inpmWarehouseId");

    if (active.equals("Y")) {
      return;
    }

    OBContext.setAdminMode(true);
    try {

      if (tab.equals(STORAGEBIN_TAB)) {
        deactivateStorageBin(info, strLocator);
      } else if (tab.equals(WAREHOUSE_TAB)) {
        deactivateWarehouse(info, strWarehouse);
      }

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void deactivateStorageBin(final CalloutInfo info, final String strLocator) {
    if (storageIsNotEmpty(strLocator)) {
      info.addResult("MESSAGE", OBMessageUtils.messageBD("M_STORAGE_ACTIVE_CHECK_FULL"));
    } else {
      final Locator locator = OBDal.getInstance().get(Locator.class, strLocator);
      if (numberOfActiveStorageBins(locator.getWarehouse()) == 1 && locator.isActive()) {
        // This means that the warehouse has only one active storage bin and it is this one
        info.addResult("MESSAGE", OBMessageUtils.messageBD("M_STORAGE_ACTIVE_CHECK_LAST"));
      }
    }
  }

  /**
   * This method returns true if the storage bin with the id passed as argument has stock inside.
   * This means that the storage bin should not be deactivated.
   */
  private boolean storageIsNotEmpty(final String strLocator) {
    //@formatter:off
    final String hql =
                  "as sd " +
                  " where sd.storageBin.id = :storageBinId " +
                  "   and (coalesce (sd.quantityOnHand,0) <> 0)" +
                  "   or (coalesce (sd.onHandOrderQuanity,0) <> 0)" +
                  "   or (coalesce (sd.quantityInDraftTransactions,0) <> 0)" +
                  "   or (coalesce (sd.quantityOrderInDraftTransactions,0) <> 0)";
    //@formatter:on
  
    return OBDal.getInstance()
        .createQuery(StorageDetail.class, hql)
        .setNamedParameter("storageBinId", strLocator)
        .setMaxResult(1)
        .uniqueResult() != null;
  }

  private void deactivateWarehouse(final CalloutInfo info, final String strWarehouse) {
    final Warehouse warehouse = OBDal.getInstance().get(Warehouse.class, strWarehouse);
    if (numberOfActiveStorageBins(warehouse) > 0) {
      info.addResult("MESSAGE", OBMessageUtils.messageBD("M_WAREHOUSE_ACTIVE_CHECK_ACTIVES"));
    } else if (warehouseWithPendingReceipts(warehouse.getId())) {
      info.addResult("MESSAGE", OBMessageUtils.messageBD("M_WAREHOUSE_ACTIVE_CHECK_ENTRIES"));
    }
  }

  /**
   * This method returns the number of Active Storage Bins a Warehouse has.
   */
  private int numberOfActiveStorageBins(final Warehouse warehouse) {
    int number = 0;
    for (Locator locator : warehouse.getLocatorList()) {
      if (locator.isActive()) {
        number++;
      }
    }
    return number;
  }

  /**
   * This method returns true if the warehouse has pending shipments or receipts.
   */
  private Boolean warehouseWithPendingReceipts(final String warehouseId) {
    //@formatter:off
    final String hql =
                  " as sp" +
                  "   left join sp.warehouse as w" +
                  "  where w.id = :warehouseId" +
                  "    and ((coalesce (sp.orderedQuantity,0) <> 0)" +
                  "    or (coalesce (sp.orderedQuantityOrder,0) <> 0)" +
                  "    or (coalesce (sp.reservedQuantity,0) <> 0)" +
                  "    or (coalesce (sp.reservedQuantityOrder,0) <> 0)) ";
    //@formatter:on

    return OBDal.getInstance()
        .createQuery(StoragePending.class, hql)
        .setNamedParameter("warehouseId", warehouseId)
        .setMaxResult(1)
        .uniqueResult() != null;
  }

}
