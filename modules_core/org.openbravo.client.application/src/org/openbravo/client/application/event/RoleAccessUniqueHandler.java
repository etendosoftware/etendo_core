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
 * All portions are Copyright (C) 2014-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.event;

import javax.enterprise.event.Observes;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.ProcessAccess;
import org.openbravo.client.application.ViewRoleAccess;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.client.myob.WidgetClassAccess;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;

/**
 * Ensures there are no duplicates entries in access tabs for Widgets, Process Definitions and View
 * Implementations.
 * 
 * Implemented as <code>EntityPersistenceEventObserver</code> instead of DB constraint to maintain
 * backwards compatibility. See issue https://issues.openbravo.com/view.php?id=27074
 * 
 * @author alostale
 *
 */
class RoleAccessUniqueHandler extends EntityPersistenceEventObserver {
  private static final String WIDGET_CLASS_ACCESS_TABLE_ID = "D1829E5F3A8441BF85DDBC06D49C1074";
  private static final String PROCESS_DEF_ACCESS_TABLE_ID = "FF80818132D85DB50132D860924E0004";
  private static final String VIEW_ACCESS_TALBLE_ID = "E6F29F8A30BC4603B1D1195051C4F3A6";

  private static final Entity WIDGET_CLASS_ACCESS_ENTITY = ModelProvider.getInstance()
      .getEntityByTableId(WIDGET_CLASS_ACCESS_TABLE_ID);
  private static final Entity PROCESS_DEF_ACCESS_ENTITY = ModelProvider.getInstance()
      .getEntityByTableId(PROCESS_DEF_ACCESS_TABLE_ID);
  private static final Entity VIEW_ACCESS_ENTITY = ModelProvider.getInstance()
      .getEntityByTableId(VIEW_ACCESS_TALBLE_ID);

  private static final Entity[] entities = { WIDGET_CLASS_ACCESS_ENTITY, PROCESS_DEF_ACCESS_ENTITY,
      VIEW_ACCESS_ENTITY };

  public void onInsert(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    checkUniqueness(event);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkUniqueness(event);
  }

  private void checkUniqueness(EntityPersistenceEvent event) {
    Entity entity = event.getTargetInstance().getEntity();

    Role newRole = (Role) event.getCurrentState(entity.getProperty("role"));

    // securedObjectProperty is the property (different based on the observed entity) that links to
    // the secured object. This property together with role must be unique
    Property securedObjectProperty;
    if (entity.equals(WIDGET_CLASS_ACCESS_ENTITY)) {
      securedObjectProperty = WIDGET_CLASS_ACCESS_ENTITY
          .getProperty(WidgetClassAccess.PROPERTY_WIDGETCLASS);
    } else if (entity.equals(PROCESS_DEF_ACCESS_ENTITY)) {
      securedObjectProperty = PROCESS_DEF_ACCESS_ENTITY
          .getProperty(ProcessAccess.PROPERTY_OBUIAPPPROCESS);
    } else {
      securedObjectProperty = VIEW_ACCESS_ENTITY
          .getProperty(ViewRoleAccess.PROPERTY_VIEWIMPLEMENTATION);
    }

    OBCriteria<BaseOBObject> q = OBDal.getInstance().createCriteria(entity.getName());

    q.add(Restrictions.eq("role", newRole));
    q.add(Restrictions.eq(securedObjectProperty.getName(),
        event.getCurrentState(securedObjectProperty)));

    if (event instanceof EntityUpdateEvent) {
      // do not count itself when updating
      q.add(Restrictions.ne("id", event.getId()));
    }

    if (q.count() > 0) {
      throw new OBException(OBMessageUtils.getI18NMessage("OBUIAPP_DuplicateAccess",
          new String[] {
              ((BaseOBObject) event.getCurrentState(securedObjectProperty)).getIdentifier(),
              newRole.getName() }));
    }
  }

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }
}
