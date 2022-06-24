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
package org.openbravo.role.inheritance;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.InheritedAccessEnabled;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.TriggerHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Role;

/**
 * Listens to delete, update and save events for all classes implementing the
 * {@link InheritedAccessEnabled} interface. This handler takes care of propagating the changes
 * according to the affected role inheritance settings.
 */
class InheritedAccessEnabledEventHandler extends EntityPersistenceEventObserver {
  private static final String SAVE = "save";
  private static final String UPDATE = "update";
  private static final String DELETE = "delete";
  private static Entity[] entities = {};

  @Inject
  private RoleInheritanceManager manager;

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  /**
   * Save event method launched when saving a class implementing the {@link InheritedAccessEnabled}
   * interface
   * 
   * @param event
   *          the new event
   */
  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    doAction(SAVE, event.getTargetInstance());
  }

  /**
   * Update event method launched when updating a class implementing the
   * {@link InheritedAccessEnabled} interface
   * 
   * @param event
   *          the update event
   */
  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    doAction(UPDATE, event.getTargetInstance());
  }

  /**
   * Delete event method launched when deleting a class implementing the
   * {@link InheritedAccessEnabled} interface
   * 
   * @param event
   *          the delete event
   */
  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    doAction(DELETE, event.getTargetInstance());
  }

  private void doAction(String action, BaseOBObject bob) {
    String entityClassName = ModelProvider.getInstance()
        .getEntity(bob.getEntity().getName())
        .getClassName();
    InheritedAccessEnabled access = (InheritedAccessEnabled) bob;
    Role role = manager.getRole(access, entityClassName);
    boolean isTemplate = role != null && role.isTemplate();
    if (SAVE.equals(action) && isTemplate) {
      manager.propagateNewAccess(role, access, entityClassName);
    } else if (UPDATE.equals(action) && isTemplate) {
      manager.propagateUpdatedAccess(role, access, entityClassName);
    } else if (DELETE.equals(action) && notDeletingParent(role)) {
      if (access.getInheritedFrom() != null) {
        Utility.throwErrorMessage("NotDeleteInheritedAccess");
      }
      if (isTemplate) {
        // Propagate access removal just for roles marked as template
        manager.propagateDeletedAccess(role, access, entityClassName);
        manager.removeReferenceInParentList(access, entityClassName);
      }
    }
  }

  @Override
  protected boolean isValidEvent(EntityPersistenceEvent event) {
    // Disable event handlers if data is being imported
    if (TriggerHandler.getInstance().isDisabled()) {
      return false;
    }
    return event.getTargetInstance() instanceof InheritedAccessEnabled;
  }

  private boolean notDeletingParent(Role role) {
    if (role == null) {
      return true;
    }
    return OBDal.getInstance().exists(Role.ENTITY_NAME, role.getId());
  }
}
