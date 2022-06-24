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
 * All portions are Copyright (C) 2015-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.TableNavigation;
import org.openbravo.model.ad.ui.Field;

/**
 * Event Handler for AD_Table_Navigation
 * 
 * @author airaceburu
 * 
 */
class ADTableNavigationEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(TableNavigation.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    updateTableId(event);
  }

  private void updateTableId(EntityPersistenceEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    TableNavigation tableNavigation = (TableNavigation) event.getTargetInstance();
    Field field = tableNavigation.getField();
    if (field != null) {
      Property fieldProperty = KernelUtils.getProperty(field);
      Entity targetEntity = fieldProperty.getTargetEntity();
      String tableId = targetEntity.getTableId();
      Table table = OBDal.getInstance().get(Table.class, tableId);

      final Entity tableNavigationEntity = ModelProvider.getInstance()
          .getEntity(TableNavigation.ENTITY_NAME);
      final Property tableNavigationTableProperty = tableNavigationEntity
          .getProperty(TableNavigation.PROPERTY_TABLE);
      event.setCurrentState(tableNavigationTableProperty, table);
    }
  }
}
