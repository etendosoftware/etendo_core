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
 * All portions are Copyright (C) 2017-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.materialmgmt.refinventory;

import java.util.Date;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.util.Check;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;

/**
 * Process of unboxing storage details out of a referenced inventory
 */
public class UnboxProcessor extends ReferencedInventoryProcessor {

  public UnboxProcessor(final ReferencedInventory referencedInventory,
      final JSONArray selectedStorageDetails) throws JSONException {
    super(referencedInventory);
    setSelectedStorageDetailsAndValidateThem(selectedStorageDetails);
  }

  @Override
  protected void setSelectedStorageDetailsAndValidateThem(final JSONArray selectedStorageDetails)
      throws JSONException {
    checkStorageDetailsHaveReferencedInventory(selectedStorageDetails);
    super.setSelectedStorageDetailsAndValidateThem(selectedStorageDetails);
  }

  private void checkStorageDetailsHaveReferencedInventory(final JSONArray selectedStorageDetails)
      throws JSONException {
    for (int i = 0; i < selectedStorageDetails.length(); i++) {
      final JSONObject storageDetailJS = selectedStorageDetails.getJSONObject(i);
      final StorageDetail storageDetail = getStorageDetail(storageDetailJS);
      Check.isNotNull(storageDetail.getReferencedInventory(),
          String.format(OBMessageUtils.messageBD("StorageDetailNotLinkedToReferencedInventory"),
              storageDetail.getIdentifier()));
    }
  }

  @Override
  protected AttributeSetInstance getAttributeSetInstanceTo(StorageDetail storageDetail) {
    return ReferencedInventoryUtil.getParentAttributeSetInstance(storageDetail);
  }

  @Override
  protected String generateInternalMovementName() {
    return OBDateUtils.formatDateTime(new Date()) + "_" + OBMessageUtils.messageBD("UNBOX");
  }

  @Override
  protected String getNewStorageBinId(JSONObject storageDetailJS) {
    try {
      return storageDetailJS.getString("storageBin");
    } catch (JSONException e) {
      throw new OBException("Error getting new storage bin for storage detail: " + storageDetailJS,
          e);
    }
  }

}
