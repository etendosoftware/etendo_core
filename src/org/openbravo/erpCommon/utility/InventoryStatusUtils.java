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

package org.openbravo.erpCommon.utility;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.common.hooks.InventoryStatusHookManager;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.materialmgmt.onhandquantity.InventoryStatus;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;

public class InventoryStatusUtils {

  private static final Logger log4j = LogManager.getLogger();

  /**
   * Changes the Inventory Status of the given Storage Bin
   * 
   * @param storageBinID
   *          ID of the Storage Bin that is going to change it's Inventory Status
   * @param inventoryStatusID
   *          ID of the new Inventory Status that is going to be set to the Storage Bin
   */
  public static void changeStatusOfStorageBin(String storageBinID, String inventoryStatusID) {
    Locator storageBin = OBDal.getInstance().get(Locator.class, storageBinID);
    String errorMessage = "";

    if (statusOfBinEquasGivenStatusID(storageBin, inventoryStatusID)) {
      return;
    }
    throwExceptionIfBinIsVirtual(storageBin);

    for (StorageDetail storageDetail : storageBin.getMaterialMgmtStorageDetailList()) {
      try {
        hooksToValidateStatusChangeInStorageDetail(inventoryStatusID, storageDetail);
      } catch (Exception e) {
        errorMessage = errorMessage.concat(e.getMessage()).concat("<br/>");
      }
    }
    if (errorsWhileChangingStatus(errorMessage)) {
      if (errorsAreOfWarningType(errorMessage)) {
        setNewStatusToBin(inventoryStatusID, storageBin);
        throw new OBException(errorMessage);
      } else {
        log4j.error(errorMessage);
        throw new OBException(errorMessage);
      }
    } else {
      setNewStatusToBin(inventoryStatusID, storageBin);
    }
  }

  private static boolean statusOfBinEquasGivenStatusID(Locator storageBin,
      String inventoryStatusID) {
    return StringUtils.equals(storageBin.getInventoryStatus().getId(), inventoryStatusID);
  }

  private static void throwExceptionIfBinIsVirtual(Locator storageBin) {
    if (storageBin.isVirtual()) {
      throw new OBException(
          OBMessageUtils.messageBD("M_VirtualBinCanNotChangeInvStatus").concat("<br/>"));
    }
  }

  private static void hooksToValidateStatusChangeInStorageDetail(String inventoryStatusID,
      StorageDetail storageDetail) throws Exception {
    WeldUtils.getInstanceFromStaticBeanManager(InventoryStatusHookManager.class)
        .executeValidationHooks(storageDetail,
            OBDal.getInstance().get(InventoryStatus.class, inventoryStatusID));
  }

  private static boolean errorsWhileChangingStatus(String errorMessage) {
    return !StringUtils.isEmpty(errorMessage);
  }

  private static boolean errorsAreOfWarningType(String errorMessage) {
    return StringUtils.startsWith(errorMessage, "WARNING");
  }

  private static void setNewStatusToBin(String inventoryStatusID, Locator storageBin) {
    storageBin
        .setInventoryStatus(OBDal.getInstance().get(InventoryStatus.class, inventoryStatusID));
    OBDal.getInstance().flush();
  }

  /**
   * Changes the Inventory Status of the given Storage Bin
   * 
   * @param storageBin
   *          Storage Bin that is going to change it's Inventory Status
   * @param inventoryStatusID
   *          ID of the new Inventory Status that is going to be set to the Storage Bin
   */
  public static void changeStatusOfStorageBin(Locator storageBin, String inventoryStatusID) {
    changeStatusOfStorageBin(storageBin.getId(), inventoryStatusID);
  }

  /**
   * Returns the number of Virtual Bins that are associated to the given Storage Bin
   */
  private static int getNumberOfVirtualBins(Locator storageBin, boolean active) {
    OBCriteria<Locator> obc = OBDal.getInstance().createCriteria(Locator.class);
    obc.add(Restrictions.eq(Locator.PROPERTY_ISVIRTUAL, true));
    obc.add(Restrictions.eq(Locator.PROPERTY_PARENTLOCATOR, storageBin));
    obc.setFilterOnActive(active);
    return obc.count();
  }

  /**
   * Returns the Total number of Virtual Bins created for the given Storage Bin
   */
  public static int getNumberOfTotalVirtualBins(Locator storageBin) {
    return getNumberOfVirtualBins(storageBin, false);
  }

  /**
   * Returns the Number of Virtual Bins created for the given Storage Bin that are active = 'Y'
   */
  public static int getNumberOfActiveVirtualBins(Locator storageBin) {
    return getNumberOfVirtualBins(storageBin, true);
  }

}
