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

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleInheritance;

/**
 * Listens to delete, update and save events for the {@link RoleInheritance} entity. This handler
 * takes care of recalculating the access of the role affected by the changes on its inheritance.
 */
class RoleInheritanceEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(RoleInheritance.ENTITY_NAME) };

  @Inject
  private RoleInheritanceManager manager;

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  /**
   * Save event method launched when saving a {@link RoleInheritance} object
   * 
   * @param event
   *          the new event
   */
  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final RoleInheritance inheritance = (RoleInheritance) event.getTargetInstance();
    String roleId = inheritance.getRole().getId();
    String inheritFromId = inheritance.getInheritFrom().getId();
    // Check correct Inherit From
    if (!inheritance.getInheritFrom().isTemplate()) {
      Utility.throwErrorMessage("InheritFromNotTemplate");
    }
    // Check User Level
    if (!isSameUserLevel(inheritance.getRole(), inheritance.getInheritFrom())) {
      Utility.throwErrorMessage("DifferentUserLevelRoleInheritance");
    }
    // Check if new role to inherit from is an ancestor in the inheritance hierarchy
    checkAncestors(inheritance.getRole(), inheritance.getInheritFrom());
    // Check cycles
    if (roleId.equals(inheritFromId) || existCycles(inheritance.getRole(), inheritFromId)) {
      Utility.throwErrorMessage("CyclesInRoleInheritance");
    } else {
      manager.applyNewInheritance(inheritance);
    }
  }

  /**
   * Update event method launched when updating a {@link RoleInheritance} object
   * 
   * @param event
   *          the update event
   */
  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    Utility.throwErrorMessage("RoleInheritanceNotEdit");
  }

  /**
   * Delete event method launched when deleting a {@link RoleInheritance} object
   * 
   * @param event
   *          the delete event
   */
  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final RoleInheritance inheritance = (RoleInheritance) event.getTargetInstance();
    boolean notDeletingParent = OBDal.getInstance()
        .exists(Role.ENTITY_NAME, inheritance.getRole().getId());
    if (notDeletingParent) {
      manager.applyRemoveInheritance(inheritance);
    }
  }

  private boolean isSameUserLevel(Role role1, Role role2) {
    String roleAccessLevel = role1.getUserLevel();
    String inheritFromAccessLevel = role2.getUserLevel();
    return roleAccessLevel.equals(inheritFromAccessLevel);
  }

  private boolean existCycles(Role role, String roleIdToFind) {
    boolean result = false;
    for (RoleInheritance ri : role.getADRoleInheritanceInheritFromList()) {
      if (ri.isActive() && roleIdToFind.equals(ri.getRole().getId())) {
        return true;
      }
      if (!result) {
        result = existCycles(ri.getRole(), roleIdToFind);
      }
    }
    return result;
  }

  private void checkAncestors(Role role, Role parent) {
    Set<String> ancestorList = new HashSet<>();
    getAncestorList(parent, ancestorList);
    ancestorList.add(parent.getId());
    // check if the role is inheriting from the new parent role
    if (findAncestor(role, ancestorList)) {
      Utility.throwErrorMessage("RoleExistsInRoleInheritance");
    } else {
      // check any the role descendants is inheriting from the new parent role
      Set<String> descendantsAncestorList = new HashSet<>();
      getAncestorListWithChildrenAncestors(role, descendantsAncestorList);
      for (String ancestorId : ancestorList) {
        if (descendantsAncestorList.contains(ancestorId)) {
          Utility.throwErrorMessage("RoleExistsInDescentansInheritance");
        }
      }
    }
  }

  private void getAncestorListWithChildrenAncestors(Role role, Set<String> result) {
    // get descendants list of the current role
    for (RoleInheritance ri : role.getADRoleInheritanceInheritFromList()) {
      getAncestorList(ri.getRole(), result);
      getAncestorListWithChildrenAncestors(ri.getRole(), result);
    }
  }

  private void getAncestorList(Role role, Set<String> result) {
    for (RoleInheritance ri : role.getADRoleInheritanceList()) {
      result.add(ri.getInheritFrom().getId());
      getAncestorList(ri.getInheritFrom(), result);
    }
  }

  private boolean findAncestor(Role role, Set<String> roleIdsToFind) {
    boolean result = false;
    for (RoleInheritance ri : role.getADRoleInheritanceList()) {
      if (ri.isActive() && roleIdsToFind.contains(ri.getInheritFrom().getId())) {
        return true;
      }
      if (!result) {
        result = findAncestor(ri.getInheritFrom(), roleIdsToFind);
      }
    }
    return result;
  }
}
