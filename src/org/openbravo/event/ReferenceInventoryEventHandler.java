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
 * All portions are Copyright (C) 2017-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventoryType;

/**
 * Event Observer over {@link ReferencedInventory} entity
 */
class ReferenceInventoryEventHandler extends EntityPersistenceEventObserver {

  private static final Entity[] ENTITIES = {
      ModelProvider.getInstance().getEntity(ReferencedInventory.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return ENTITIES;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    recomputeValueFromSequenceIfAutomaticallySet(event);
  }

  public void onSave(@Observes EntityNewEvent event) {
    recomputeValueFromSequenceIfAutomaticallySet(event);
  }

  /**
   * If value is automatic (when value starts with "<"), it recompute its value using the associated
   * sequence (if found) and updates the next sequence number in database
   */
  private void recomputeValueFromSequenceIfAutomaticallySet(EntityPersistenceEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final Property valueProperty = ENTITIES[0].getProperty(ReferencedInventory.PROPERTY_SEARCHKEY);
    final String value = (String) event.getCurrentState(valueProperty);

    if (isValueAutomaticallySet(value)) {
      final Property refInvTypeProperty = ENTITIES[0]
          .getProperty(ReferencedInventory.PROPERTY_REFERENCEDINVENTORYTYPE);
      final ReferencedInventoryType refInvType = (ReferencedInventoryType) event
          .getCurrentState(refInvTypeProperty);

      final String documentNo = ReferencedInventoryUtil
          .getProposedValueFromSequenceOrNull(refInvType.getId(), true);
      event.setCurrentState(valueProperty, documentNo);
    }
  }

  private boolean isValueAutomaticallySet(final String value) {
    return StringUtils.startsWith(value, "<");
  }

}
