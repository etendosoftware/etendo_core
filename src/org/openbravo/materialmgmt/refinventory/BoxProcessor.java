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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;

/**
 * Process of boxing storage details into a concrete referenced inventory
 */
public class BoxProcessor extends ReferencedInventoryProcessor {
  private String newStorageBinId;
  // StorageDetailId:NewAttributeSetInstanceId created by this object
  private final Map<String, String> storageDetailNewAttributeIdMap;

  public BoxProcessor(final ReferencedInventory referencedInventory,
      final JSONArray selectedStorageDetails, final String newStorageBinId) throws JSONException {
    super(referencedInventory);
    super.setSelectedStorageDetailsAndValidateThem(selectedStorageDetails);
    checkStorageDetailsNotAlreadyInReferencedInventory(selectedStorageDetails);
    setAndValidateNewStorageBinId(newStorageBinId);
    storageDetailNewAttributeIdMap = new HashMap<>(selectedStorageDetails.length());
  }

  private void checkStorageDetailsNotAlreadyInReferencedInventory(
      final JSONArray selectedStorageDetails) throws JSONException {
    for (int i = 0; i < selectedStorageDetails.length(); i++) {
      final JSONObject storageDetailJS = selectedStorageDetails.getJSONObject(i);
      final StorageDetail storageDetail = getStorageDetail(storageDetailJS);
      final ReferencedInventory previousReferencedInventory = storageDetail
          .getReferencedInventory();
      if (previousReferencedInventory != null) {
        throw new OBException(String.format(
            OBMessageUtils.messageBD("StorageDetailAlreadyLinkedToPreviousReferencedInventory"),
            storageDetail.getIdentifier(), previousReferencedInventory.getIdentifier()));
      }
    }
  }

  private void setAndValidateNewStorageBinId(final String newStorageBinId) {
    if (StringUtils.isBlank(newStorageBinId)) {
      throw new OBException(OBMessageUtils.messageBD("NewStorageBinParameterMandatory"));
    } else {
      this.newStorageBinId = newStorageBinId;
    }
  }

  @Override
  protected AttributeSetInstance getAttributeSetInstanceTo(final StorageDetail storageDetail) {
    // Attribute previously created in this box execution
    if (storageDetailNewAttributeIdMap.containsKey(storageDetail.getId())) {
      return OBDal.getInstance()
          .getProxy(AttributeSetInstance.class,
              storageDetailNewAttributeIdMap.get(storageDetail.getId()));
    }

    // Attribute previously created in other box executions for this refInventory
    final AttributeSetInstance previouslyClonedAttributeSetInstance = ReferencedInventoryUtil
        .getAlreadyClonedAttributeSetInstance(storageDetail.getAttributeSetValue(),
            getReferencedInventory());
    if (previouslyClonedAttributeSetInstance == null) {
      final AttributeSetInstance newAttributeSetInstance = ReferencedInventoryUtil
          .cloneAttributeSetInstance(storageDetail.getAttributeSetValue(),
              getReferencedInventory());
      storageDetailNewAttributeIdMap.put(storageDetail.getId(), newAttributeSetInstance.getId());
      return newAttributeSetInstance;
    } else {
      storageDetailNewAttributeIdMap.put(storageDetail.getId(),
          previouslyClonedAttributeSetInstance.getId());
      return previouslyClonedAttributeSetInstance;
    }
  }

  @Override
  protected String generateInternalMovementName() {
    return OBDateUtils.formatDateTime(new Date()) + "_" + OBMessageUtils.messageBD("BOX");
  }

  @Override
  protected String getNewStorageBinId(JSONObject storageDetailJS) {
    return newStorageBinId;
  }

}
