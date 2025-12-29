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

package org.openbravo.common.actionhandler;

import java.util.Comparator;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder.MessageType;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.refinventory.DefaultProcessorProvider;
import org.openbravo.materialmgmt.refinventory.RefInvProcessorProvider;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryProcessor;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.service.db.DbUtility;

/**
 * Action handler for boxing storage details into a Referenced Inventory
 */
public class ReferencedInventoryBoxHandler extends BaseProcessActionHandler {
  private static final Logger logger = LogManager.getLogger();

  private static final String PARAM_GRID = "stock";
  private static final String PARAM_GRID_SELECTION = "_selection";
  private static final String PARAM_NEWSTORAGEBIN = "M_LocatorTo_ID";

  @Inject
  @Any
  private Instance<RefInvProcessorProvider> procProviders;

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    try {
      final JSONObject request = new JSONObject(content);
      OBContext.setAdminMode(true);

      final ReferencedInventory referencedInventory = getReferencedInventory(request);
      final JSONArray selectedStorageDetails = getSelectedStorageDetails(request);
      final String newStorageBinId = getNewStorageBin(request);

      createAndProcessGoodsMovement(referencedInventory, selectedStorageDetails, newStorageBinId);
    } catch (Exception e) {
      try {
        final Throwable ex = DbUtility.getUnderlyingSQLException(e);
        final String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();

        return getResponseBuilder()
            .showMsgInProcessView(MessageType.ERROR, OBMessageUtils.messageBD("Error"),
                StringUtils.isBlank(message) ? ex.toString() : message, true)
            .retryExecution()
            .build();
      } catch (Exception ignore) {
        logger.warn("Exception trying to build error message", ignore);
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    return getResponseBuilder()
        .showMsgInView(MessageType.SUCCESS, OBMessageUtils.messageBD("Success"),
            OBMessageUtils.messageBD("Success"))
        .build();
  }

  private ReferencedInventory getReferencedInventory(final JSONObject request)
      throws JSONException {
    final String refInventoryId = request.getString("inpmRefinventoryId");
    return OBDal.getInstance().getProxy(ReferencedInventory.class, refInventoryId);
  }

  private JSONArray getSelectedStorageDetails(final JSONObject request) throws JSONException {
    final JSONObject params = request.getJSONObject("_params");
    return params.getJSONObject(PARAM_GRID).getJSONArray(PARAM_GRID_SELECTION);
  }

  private String getNewStorageBin(final JSONObject request) {
    try {
      final JSONObject params = request.getJSONObject("_params");
      final String newStorageBinId = params.getString(PARAM_NEWSTORAGEBIN);
      return StringUtils.isBlank(newStorageBinId) || StringUtils.equals(newStorageBinId, "null")
          ? null
          : newStorageBinId;
    } catch (JSONException noParameterFound) {
      return null;
    }
  }

  /**
   * Creates and processes a goods movement for the selected storage details into the specified referenced inventory.
   * <p>
   * This method determines the appropriate processor provider based on the referenced inventory type, prioritizing
   * providers with higher priority. It then instantiates the corresponding box processor and delegates the creation
   * and processing of the goods movement to it. This encapsulates the logic for packing storage details into a referenced
   * inventory unit, such as a box or pallet, and moving them to a new storage bin if specified.
   *
   * @param referencedInventory
   *     The referenced inventory unit to which the goods will be packed.
   * @param selectedStorageDetails
   *     The storage details (as a JSONArray) to be packed into the referenced inventory.
   * @param newStorageBinId
   *     The ID of the new storage bin where the goods will be moved. Can be null.
   * @throws Exception
   *     If any error occurs during the creation or processing of the goods movement.
   */
  protected void createAndProcessGoodsMovement(final ReferencedInventory referencedInventory,
      final JSONArray selectedStorageDetails, final String newStorageBinId) throws Exception {
    String type = referencedInventory.getReferencedInventoryType().getId();
    RefInvProcessorProvider provider = procProviders.stream()
        .filter(p -> p.supports(type))
        .sorted(Comparator.comparingInt(RefInvProcessorProvider::getPriority).reversed())
        .findFirst()
        .orElse(new DefaultProcessorProvider());

    ReferencedInventoryProcessor bp = provider.getBoxProcessorClass()
        .getConstructor(ReferencedInventory.class, JSONArray.class, String.class)
        .newInstance(referencedInventory, selectedStorageDetails, newStorageBinId);

    bp.createAndProcessGoodsMovement();
  }
}
