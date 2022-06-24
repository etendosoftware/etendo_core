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
 * All portions are Copyright (C) 2012-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.userinterface.selector;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;

/**
 * Observes Select Field entity modifications to ensure it is not set as centrally maintained if its
 * parent Selector is Custom Query.
 * 
 * @author alostale
 * 
 */
class SelectorFieldHandler extends EntityPersistenceEventObserver {
  private static final String SELECTOR_FIELD_TABLE_ID = "A2F880F9981349E2A6A57BD58267EBCE";
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntityByTableId(SELECTOR_FIELD_TABLE_ID) };

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkCentralMaintenance(event);
  }

  public void onNew(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkCentralMaintenance(event);
  }

  private void checkCentralMaintenance(EntityPersistenceEvent event) {
    Selector sel = (Selector) event
        .getCurrentState(entities[0].getPropertyByColumnName("Obuisel_Selector_ID"));

    if (sel.isCustomQuery()) {
      event.setCurrentState(entities[0].getPropertyByColumnName("Iscentrallymaintained"), false);
    }
  }

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

}
