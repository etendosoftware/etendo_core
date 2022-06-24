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

package org.openbravo.client.application.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;

/**
 * Fills in the {@link Field#setColumn(org.openbravo.model.ad.datamodel.Column)} of the
 * {@link Field} if the {@link Field#getProperty()} is set.
 * 
 * @author mtaal
 */
class FieldHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Field.ENTITY_NAME) };

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    setColumn(event);
    setIgnoreInWad(event);
  }

  public void onNew(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    setColumn(event);
    setIgnoreInWad(event);
  }

  // must be called after setcolumn
  private void setIgnoreInWad(EntityPersistenceEvent event) {
    final Column column = (Column) event.getCurrentState(getColumnProperty());
    if (column != null && column.getSqllogic() != null) {
      event.setCurrentState(getIgnoreInWadProperty(), true);
    }
  }

  private void setColumn(EntityPersistenceEvent event) {
    final String propertyPath = (String) event.getCurrentState(getPropertyProperty());
    final String clientClass = (String) event.getCurrentState(getClientClassProperty());
    if (propertyPath == null || propertyPath.trim().length() == 0) {
      if (event.getCurrentState(getColumnProperty()) == null && clientClass == null) {
        throw new OBException(OBMessageUtils.messageBD("OBUIAPP_ColumnMustBeSet"));
      }
      return;
    }
    final Tab tab = (Tab) event.getCurrentState(getTabProperty());
    final String tableId = tab.getTable().getId();
    final Entity entity = ModelProvider.getInstance().getEntityByTableId(tableId);
    final Property property = DalUtil.getPropertyFromPath(entity, propertyPath);
    if (property == null) {
      throw new OBException(OBMessageUtils.messageBD("OBUIAPP_PropertyNotFound"));
    }

    if (propertyPath.contains(DalUtil.DOT)) {
      event.setCurrentState(getIgnoreInWadProperty(), true);
    }

    final Column column = OBDal.getInstance().get(Column.class, property.getColumnId());
    event.setCurrentState(getColumnProperty(), column);
  }

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Property getColumnProperty() {
    return entities[0].getProperty(Field.PROPERTY_COLUMN);
  }

  private Property getPropertyProperty() {
    return entities[0].getProperty(Field.PROPERTY_PROPERTY);
  }

  private Property getIgnoreInWadProperty() {
    return entities[0].getProperty(Field.PROPERTY_IGNOREINWAD);
  }

  private Property getTabProperty() {
    return entities[0].getProperty(Field.PROPERTY_TAB);
  }

  private Property getClientClassProperty() {
    return entities[0].getProperty(Field.PROPERTY_CLIENTCLASS);
  }
}
