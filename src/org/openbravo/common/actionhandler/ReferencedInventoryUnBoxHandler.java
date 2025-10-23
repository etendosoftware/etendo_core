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

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONArray;
import org.openbravo.materialmgmt.refinventory.DefaultProcessorProvider;
import org.openbravo.materialmgmt.refinventory.RefInvProcessorProvider;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryProcessor;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;

/**
 * Action handler for unboxing storage details from a Referenced Inventory
 */
public class ReferencedInventoryUnBoxHandler extends ReferencedInventoryBoxHandler {
  @Inject
  @Any
  private Instance<RefInvProcessorProvider> procProviders;

  /**
   * Creates and processes a goods movement to unbox storage details from the specified referenced inventory.
   * <p>
   * This method determines the appropriate processor provider for the referenced inventory type, prioritizing
   * providers with higher priority. It then instantiates the corresponding unbox processor and delegates the
   * creation and processing of the goods movement to it. This encapsulates the logic for unboxing storage details
   * from a referenced inventory unit, such as a box or pallet.
   *
   * @param referencedInventory
   *     The referenced inventory unit from which the goods will be unboxed.
   * @param selectedStorageDetails
   *     The storage details (as a JSONArray) to be unboxed from the referenced inventory.
   * @param newStorageBinId
   *     The ID of the new storage bin (not used in unboxing, but kept for method signature compatibility).
   * @throws Exception
   *     If any error occurs during the creation or processing of the goods movement.
   */
  @Override
  protected void createAndProcessGoodsMovement(final ReferencedInventory referencedInventory,
      final JSONArray selectedStorageDetails, final String newStorageBinId) throws Exception {
    String type = referencedInventory.getReferencedInventoryType().getId();
    RefInvProcessorProvider provider = procProviders.stream()
        .filter(p -> p.supports(type))
        .sorted(Comparator.comparingInt(RefInvProcessorProvider::getPriority).reversed())
        .findFirst()
        .orElse(new DefaultProcessorProvider());

    ReferencedInventoryProcessor up = provider.getUnboxProcessorClass()
        .getConstructor(ReferencedInventory.class, JSONArray.class)
        .newInstance(referencedInventory, selectedStorageDetails);

    up.createAndProcessGoodsMovement();
  }
}
