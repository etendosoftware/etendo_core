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
 * All portions are Copyright (C) 2015 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.role.inheritance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.InheritedAccessEnabled;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleInheritance;
import org.openbravo.role.inheritance.access.AccessTypeInjector;

/**
 * This class contains all the methods required to manage the Role Inheritance functionality. It is
 * not intended to handle large volumes of objects (permissions) within this class. For this reason
 * it makes use of DAL lists instead of ScrollableResults.
 */
@ApplicationScoped
public class RoleInheritanceManager {

  private static final Logger log = LogManager.getLogger();
  private static final int ACCESS_NOT_CHANGED = 0;
  private static final int ACCESS_UPDATED = 1;
  private static final int ACCESS_CREATED = 2;

  @Inject
  @Any
  private Instance<AccessTypeInjector> accessTypeInjectors;

  /**
   * Returns the role which the access given as parameter is assigned to.
   * 
   * @param access
   *          An inheritable access
   * @param className
   *          the name of the class
   * 
   * @return the Role owner of the access
   */
  Role getRole(InheritedAccessEnabled access, String className) {
    AccessTypeInjector injector = getInjector(className);
    if (injector == null) {
      return null;
    }
    return injector.getRole(access);
  }

  /**
   * Returns the access with a particular secured element for a role.
   * 
   * @param role
   *          The role owner of the access to find
   * @param injector
   *          AccessTypeInjector used to retrieve the access information
   * @param access
   *          The access with the secured element to find
   * @return the searched access or null if not found
   */
  private InheritedAccessEnabled getAccess(Role role, AccessTypeInjector injector,
      InheritedAccessEnabled access) {
    String roleId = role.getId();
    try {
      return injector.findAccess(access, roleId);
    } catch (Exception ex) {
      log.error("Error getting access list of class {}", injector.getClassName(), ex);
      throw new OBException("Error getting access list of class " + injector.getClassName());
    }
  }

  /**
   * Creates a new access by copying from the one introduced as parameter. In addition, it sets the
   * Inherit From field with the corresponding role.
   * 
   * @param parentAccess
   *          The access to be copied
   * @param role
   *          The role used to set the parent of the new access
   * @param injector
   *          an AccessTypeInjector object used to retrieve and set the access information
   */
  private void copyRoleAccess(InheritedAccessEnabled parentAccess, Role role,
      AccessTypeInjector injector) {
    try {
      OBContext.setAdminMode(false);
      // copy the new access
      final InheritedAccessEnabled newAccess = (InheritedAccessEnabled) DalUtil
          .copy((BaseOBObject) parentAccess, false);
      injector.setParent(newAccess, parentAccess, role);
      newAccess.setInheritedFrom(injector.getRole(parentAccess));
      OBDal.getInstance().save(newAccess);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  void removeReferenceInParentList(InheritedAccessEnabled access, String className) {
    AccessTypeInjector injector = getInjector(className);
    if (injector != null) {
      injector.removeReferenceInParentList(access);
    }
  }

  /**
   * Deletes all accesses which are inheriting from a particular role.
   * 
   * @param inheritFromToDelete
   *          The role which the accesses about to delete are inherited from
   * @param roleAccessList
   *          The list of accesses to remove from
   * @param injector
   *          An AccessTypeInjector used to retrieve the access elements
   */
  private void deleteRoleAccess(Role inheritFromToDelete,
      List<? extends InheritedAccessEnabled> roleAccessList, AccessTypeInjector injector) {
    try {
      OBContext.setAdminMode(false);
      String inheritFromId = inheritFromToDelete.getId();
      List<InheritedAccessEnabled> iaeToDelete = new ArrayList<InheritedAccessEnabled>();
      for (InheritedAccessEnabled ih : roleAccessList) {
        String inheritedFromId = ih.getInheritedFrom() != null ? ih.getInheritedFrom().getId() : "";
        if (!StringUtils.isEmpty(inheritedFromId) && inheritFromId.equals(inheritedFromId)) {
          iaeToDelete.add(ih);
        }
      }
      for (InheritedAccessEnabled iae : iaeToDelete) {
        iae.setInheritedFrom(null);
        roleAccessList.remove(iae);
        Role owner = injector.getRole(iae);
        if (!owner.isTemplate()) {
          // Perform this operation for not template roles, because for template roles is already
          // done
          // in the event handler
          injector.removeReferenceInParentList(iae);
        }
        OBDal.getInstance().remove(iae);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Updates the fields of an access with the values of the access introduced as parameter. In
   * addition, it sets the Inherit From field with the corresponding role.
   * 
   * @param access
   *          The access to be updated
   * @param inherited
   *          The access with the values to update
   * @param injector
   *          an AccessTypeInjector used to retrieve the access information
   */
  private void updateRoleAccess(InheritedAccessEnabled access, InheritedAccessEnabled inherited,
      AccessTypeInjector injector) {
    try {
      OBContext.setAdminMode(false);
      final InheritedAccessEnabled updatedAccess = (InheritedAccessEnabled) DalUtil.copyToTarget(
          (BaseOBObject) inherited, (BaseOBObject) access, false, injector.getSkippedProperties());
      // update the inherit from field, to indicate from which role we are inheriting now
      updatedAccess.setInheritedFrom(injector.getRole(inherited));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Applies all type of accesses based on the inheritance passed as parameter
   * 
   * @param inheritance
   *          The inheritance used to calculate the possible new accesses
   */
  void applyNewInheritance(RoleInheritance inheritance) {
    long t = System.currentTimeMillis();
    List<RoleInheritance> inheritanceList = getUpdatedRoleInheritancesList(inheritance, false);
    List<String> inheritanceRoleIdList = getRoleInheritancesInheritFromIdList(inheritanceList);
    List<RoleInheritance> newInheritanceList = new ArrayList<RoleInheritance>();
    newInheritanceList.add(inheritance);
    for (AccessTypeInjector accessType : getAccessTypeOrderByPriority(true)) {
      calculateAccesses(newInheritanceList, inheritanceRoleIdList, accessType, false);
    }
    log.debug("add new inheritance time: {}", (System.currentTimeMillis() - t));
  }

  /**
   * Calculates all type of accesses after the removal of the inheritance passed as parameter
   * 
   * @param inheritance
   *          The inheritance being removed
   */
  void applyRemoveInheritance(RoleInheritance inheritance) {
    long t = System.currentTimeMillis();
    List<RoleInheritance> inheritanceList = getUpdatedRoleInheritancesList(inheritance, true);
    List<String> inheritanceRoleIdList = getRoleInheritancesInheritFromIdList(inheritanceList);
    for (AccessTypeInjector accessType : getAccessTypeOrderByPriority(false)) {
      // We need to retrieve the access types ordered descending by their priority, to force to
      // handle first 'child' accesses like TabAccess or ChildAccess which have a
      // priority number higher than their parent, WindowAccess. This way, child instances will be
      // deleted first when it applies.
      calculateAccesses(inheritanceList, inheritanceRoleIdList, inheritance, accessType, false);
    }
    log.debug("remove inheritance time: {}", (System.currentTimeMillis() - t));
  }

  /**
   * Recalculates all accesses for those roles using as template the role passed as parameter
   * 
   * @param template
   *          The template role used by the roles whose accesses will be recalculated
   * @return a set of the child roles which have accesses that have been updated or created
   */
  public Set<Role> recalculateAllAccessesFromTemplate(Role template) {
    long t = System.currentTimeMillis();
    Set<Role> updatedRoles = new HashSet<Role>();
    for (RoleInheritance ri : template.getADRoleInheritanceInheritFromList()) {
      if (ri.isActive()) {
        Map<String, CalculationResult> result = recalculateAllAccessesForRole(ri.getRole());
        for (String accessClassName : result.keySet()) {
          CalculationResult counters = result.get(accessClassName);
          if (counters.getUpdated() > 0 || counters.getCreated() > 0) {
            updatedRoles.add(ri.getRole());
          }
        }
      }
    }
    log.debug("recalculate all accesses from template {} time: {}", template,
        (System.currentTimeMillis() - t));
    return updatedRoles;
  }

  /**
   * @see RoleInheritanceManager#recalculateAllAccessesForRole(Role, boolean)
   * @param role
   *          The role whose accesses will be recalculated
   * @return a map with the number of accesses updated and created for every access type
   * 
   */
  public Map<String, CalculationResult> recalculateAllAccessesForRole(Role role) {
    return recalculateAllAccessesForRole(role, false);
  }

  /**
   * Recalculates all accesses for a given role
   * 
   * @param role
   *          The role whose accesses will be recalculated
   * @param delete
   *          A flag to indicate if all the role accesses should be deleted before the recalculation
   * @return a map with the number of accesses updated and created for every access type
   */
  Map<String, CalculationResult> recalculateAllAccessesForRole(Role role, boolean delete) {
    long t = System.currentTimeMillis();
    if (delete) {
      deleteAllAccesses(role);
    }
    Map<String, CalculationResult> result = new HashMap<String, CalculationResult>();
    List<RoleInheritance> inheritanceList = getRoleInheritancesList(role);
    List<String> inheritanceRoleIdList = getRoleInheritancesInheritFromIdList(inheritanceList);
    for (AccessTypeInjector accessType : getAccessTypeOrderByPriority(true)) {
      CalculationResult counters = calculateAccesses(inheritanceList, inheritanceRoleIdList,
          accessType, delete);
      result.put(accessType.getClassName(), counters);
    }
    log.debug("recalculate all accesses for role {} time: {}", role,
        (System.currentTimeMillis() - t));
    return result;
  }

  /**
   * Deletes all the permissions for a role
   * 
   * @param role
   *          The role whose accesses will be deleted
   */

  void deleteAllAccesses(Role role) {
    for (AccessTypeInjector accessType : getAccessTypeOrderByPriority(true)) {
      List<? extends InheritedAccessEnabled> roleAccessList = accessType.getAccessList(role);
      List<InheritedAccessEnabled> iaeToDelete = new ArrayList<InheritedAccessEnabled>();
      for (InheritedAccessEnabled iae : roleAccessList) {
        // Not inherited accesses should not be deleted
        if (iae.getInheritedFrom() != null) {
          iaeToDelete.add(iae);
        }
      }
      for (InheritedAccessEnabled iae : iaeToDelete) {
        accessType.clearInheritFromFieldInChilds(iae, true);
        iae.setInheritedFrom(null);
        roleAccessList.remove(iae);
        OBDal.getInstance().remove(iae);
      }
    }
    OBDal.getInstance().commitAndClose();
  }

  /**
   * Recalculates the accesses of a particular class, for those roles using as template the role
   * passed as parameter
   * 
   * @param template
   *          The template role used by the roles whose accesses will be recalculated * @param
   * @param classCanonicalName
   *          the name of the class
   */
  public void recalculateAccessFromTemplate(Role template, String classCanonicalName) {
    long t = System.currentTimeMillis();
    AccessTypeInjector injector = getInjector(classCanonicalName);
    if (injector == null) {
      return;
    }
    for (RoleInheritance ri : template.getADRoleInheritanceInheritFromList()) {
      if (ri.isActive()) {
        recalculateAccessForRole(ri.getRole(), injector);
      }
    }
    log.debug("recalculate access from template {} time: {}", template,
        (System.currentTimeMillis() - t));
  }

  /**
   * Recalculates the accesses of a particular class for a given role
   * 
   * @param role
   *          The role whose accesses will be recalculated
   * @param injector
   *          An AccessTypeInjector used to retrieve the access elements
   * 
   */
  void recalculateAccessForRole(Role role, AccessTypeInjector injector) {
    long t = System.currentTimeMillis();
    List<RoleInheritance> inheritanceList = getRoleInheritancesList(role);
    List<String> inheritanceRoleIdList = getRoleInheritancesInheritFromIdList(inheritanceList);
    CalculationResult counters = calculateAccesses(inheritanceList, inheritanceRoleIdList, injector,
        false);
    log.debug("recalculate access for role {} time: {}", role, (System.currentTimeMillis() - t));
    log.debug("accesses created: {}, accesses updated: {}", counters.getCreated(),
        counters.getUpdated());
  }

  /**
   * Propagates a new access assigned to a template role
   * 
   * @param role
   *          The template role whose new access will be propagated
   * @param access
   *          The new access to be propagated
   * @param classCanonicalName
   *          the name of the class
   */
  void propagateNewAccess(Role role, InheritedAccessEnabled access, String classCanonicalName) {
    long t = System.currentTimeMillis();
    AccessTypeInjector injector = getInjector(classCanonicalName);
    if (injector == null) {
      return;
    }
    injector.checkAccessExistence(access);
    if (!injector.isInheritable(access)) {
      return;
    }
    for (RoleInheritance ri : role.getADRoleInheritanceInheritFromList()) {
      if (ri.isActive()) {
        List<RoleInheritance> inheritanceList = getRoleInheritancesList(ri.getRole());
        List<String> inheritanceRoleIdList = getRoleInheritancesInheritFromIdList(inheritanceList);
        handleAccess(ri, access, inheritanceRoleIdList, injector);
      }
    }
    log.debug("propagate new access from template {} time: {}", role,
        +(System.currentTimeMillis() - t));
  }

  /**
   * Propagates an updated access of a template role
   * 
   * @param role
   *          The template role whose updated access will be propagated
   * @param access
   *          The updated access with the changes to propagate
   * @param classCanonicalName
   *          the name of the class
   */
  void propagateUpdatedAccess(Role role, InheritedAccessEnabled access, String classCanonicalName) {
    long t = System.currentTimeMillis();
    AccessTypeInjector injector = getInjector(classCanonicalName);
    if (injector == null) {
      return;
    }
    if (!injector.isInheritable(access)) {
      return;
    }
    for (RoleInheritance ri : role.getADRoleInheritanceInheritFromList()) {
      if (ri.isActive()) {
        InheritedAccessEnabled childAccess = findInheritedAccess(ri.getRole(), access, injector);
        if (childAccess != null) {
          updateRoleAccess(childAccess, access, injector);
        }
      }
    }
    log.debug("propagate updated access from template {} time: {}", role,
        (System.currentTimeMillis() - t));
  }

  /**
   * Propagates a deleted access of a template role
   * 
   * @param role
   *          The template role whose deleted access will be propagated
   * @param access
   *          The removed access to be propagated
   * @param classCanonicalName
   *          the name of the class
   */
  void propagateDeletedAccess(Role role, InheritedAccessEnabled access, String classCanonicalName) {
    long t = System.currentTimeMillis();
    AccessTypeInjector injector = getInjector(classCanonicalName);
    if (injector == null) {
      return;
    }
    if (!injector.isInheritable(access)) {
      return;
    }
    for (RoleInheritance ri : role.getADRoleInheritanceInheritFromList()) {
      if (ri.isActive()) {
        Role childRole = ri.getRole();
        InheritedAccessEnabled iaeToDelete = findInheritedAccess(childRole, access, injector);
        if (iaeToDelete != null) {
          // need to recalculate, look for this access in other inheritances
          boolean updated = false;
          // retrieve the list of templates, ordered by sequence number descending, to update the
          // access with the first one available (highest sequence number)
          List<Role> inheritFromList = getRoleInheritancesInheritFromList(childRole, role, false);
          for (Role inheritFrom : inheritFromList) {
            InheritedAccessEnabled inheritFromAccess = getAccess(inheritFrom, injector,
                iaeToDelete);
            if (inheritFromAccess != null) {
              updateRoleAccess(iaeToDelete, inheritFromAccess, injector);
              updated = true;
              break;
            }
          }
          if (!updated) {
            // Access not present in other inheritances, remove it
            injector.clearInheritFromFieldInChilds(iaeToDelete, false);
            iaeToDelete.setInheritedFrom(null);
            Role owner = injector.getRole(iaeToDelete);
            if (!owner.isTemplate()) {
              // Perform this operation for not template roles, because for template roles is
              // already done in the event handler
              injector.removeReferenceInParentList(iaeToDelete);
            }
            OBDal.getInstance().remove(iaeToDelete);
          }
        }
      }
    }
    log.debug("propagate deleted access from template {} time: {}", role,
        (System.currentTimeMillis() - t));
  }

  /**
   * Looks for a particular access of a role
   * 
   * @param role
   *          The role owner of the access to look for
   * @param access
   *          The access to be found
   * @param injector
   *          An AccessTypeInjector used to retrieve the access elements
   * @return the access being searched or null if not found
   */
  private InheritedAccessEnabled findInheritedAccess(Role role, InheritedAccessEnabled access,
      AccessTypeInjector injector) {
    String accessRole = injector.getRole(access).getId();
    InheritedAccessEnabled iae = getAccess(role, injector, access);
    if (iae != null) {
      String inheritFromRole = iae.getInheritedFrom() != null ? iae.getInheritedFrom().getId() : "";
      if (accessRole.equals(inheritFromRole)) {
        return iae;
      }
    }
    return null;
  }

  /**
   * @see RoleInheritanceManager#calculateAccesses(List, List, RoleInheritance, AccessTypeInjector,
   *      boolean)
   */
  private CalculationResult calculateAccesses(List<RoleInheritance> inheritanceList,
      List<String> inheritanceInheritFromIdList, AccessTypeInjector injector, boolean doFlush) {
    return calculateAccesses(inheritanceList, inheritanceInheritFromIdList, null, injector,
        doFlush);
  }

  /**
   * Calculate the inheritable accesses according to the inheritance list passed as parameter.
   * 
   * @param inheritanceList
   *          The list of inheritances used to calculate the accesses
   * @param inheritanceInheritFromIdList
   *          A list of template role ids. The position of the ids in this list determines the
   *          priority when applying their related inheritances.
   * @param roleInheritanceToDelete
   *          If not null, the accesses introduced by this inheritance will be removed
   * @param injector
   *          An AccessTypeInjector used to retrieve the access elements
   * @param doFlush
   *          a flag to indicate if a flush must be done after applying every inheritance
   * @return a list with two Integers containing the number of accesses updated and created
   *         respectively.
   */
  private CalculationResult calculateAccesses(List<RoleInheritance> inheritanceList,
      List<String> inheritanceInheritFromIdList, RoleInheritance roleInheritanceToDelete,
      AccessTypeInjector injector, boolean doFlush) {
    int[] counters = new int[] { 0, 0, 0 };
    for (RoleInheritance roleInheritance : inheritanceList) {
      for (InheritedAccessEnabled inheritedAccess : injector
          .getAccessList(roleInheritance.getInheritFrom())) {
        if (!injector.isInheritable(inheritedAccess)) {
          continue;
        }
        int res = handleAccess(roleInheritance, inheritedAccess, inheritanceInheritFromIdList,
            injector);
        counters[res]++;
      }
      if (doFlush) {
        OBDal.getInstance().flush();
      }
    }
    if (roleInheritanceToDelete != null) {
      // delete accesses not inherited anymore
      deleteRoleAccess(roleInheritanceToDelete.getInheritFrom(),
          injector.getAccessList(roleInheritanceToDelete.getRole()), injector);
    }
    CalculationResult result = new CalculationResult(counters[ACCESS_UPDATED],
        counters[ACCESS_CREATED]);
    return result;
  }

  /**
   * Determines if a access candidate to be inherited should be created, not created or updated.
   * 
   * @param roleInheritance
   *          Inheritance with the role information
   * @param inheritedAccess
   *          An existing access candidate to be overridden
   * @param inheritanceInheritFromIdList
   *          A list of template role ids which determines the priority of the template roles
   * @param injector
   *          An AccessTypeInjector used to retrieve the access elements
   * @return an integer that indicates the final action done with the access: not changed
   *         (ACCESS_NOT_CHANGED), updated (ACCESS_UPDATED) or created (ACCESS_CREATED).
   */
  private int handleAccess(RoleInheritance roleInheritance, InheritedAccessEnabled inheritedAccess,
      List<String> inheritanceInheritFromIdList, AccessTypeInjector injector) {
    String newInheritedFromId = roleInheritance.getInheritFrom().getId();
    Role role = roleInheritance.getRole();
    InheritedAccessEnabled access = getAccess(role, injector, inheritedAccess);
    if (access != null) {
      String currentInheritedFromId = access.getInheritedFrom() != null
          ? access.getInheritedFrom().getId()
          : "";
      if (!StringUtils.isEmpty(currentInheritedFromId) && isPrecedent(inheritanceInheritFromIdList,
          currentInheritedFromId, newInheritedFromId)) {
        updateRoleAccess(access, inheritedAccess, injector);
        log.debug("Updated access of class {} for role {}", injector.getClassName(), role);
        return ACCESS_UPDATED;
      }
      return ACCESS_NOT_CHANGED;
    }
    copyRoleAccess(inheritedAccess, roleInheritance.getRole(), injector);
    log.debug("Created access of class {} for role {}", injector.getClassName(), role);
    return ACCESS_CREATED;
  }

  /**
   * Utility method used to determine the precedence between two roles according to the given
   * priority list.
   * 
   * @param inheritanceInheritFromIdList
   *          A list of template role ids which determines the priority of the template roles
   * @param role1
   *          The first role to check its priority
   * @param role2
   *          The second role to check its priority
   * @return true if the first role is precedent to the second role, false otherwise
   */
  private boolean isPrecedent(List<String> inheritanceInheritFromIdList, String role1,
      String role2) {
    if (inheritanceInheritFromIdList.indexOf(role1) == -1) {
      // Not found, need to override (this can happen on delete or on update)
      return true;
    }
    if (inheritanceInheritFromIdList.indexOf(role1) < inheritanceInheritFromIdList.indexOf(role2)) {
      return true;
    }
    return false;
  }

  /**
   * @see RoleInheritanceManager#getRoleInheritancesList(Role, boolean)
   */
  List<RoleInheritance> getRoleInheritancesList(Role role) {
    return getRoleInheritancesList(role, true);
  }

  /**
   * @see RoleInheritanceManager#getRoleInheritancesList(Role, Role, boolean)
   */
  List<RoleInheritance> getRoleInheritancesList(Role role, boolean seqNoAscending) {
    return getRoleInheritancesList(role, null, true);
  }

  /**
   * Returns the list of inheritances of a role
   * 
   * @param role
   *          The role whose inheritance list will be retrieved
   * @param excludedInheritFrom
   *          A template role whose inheritance will be excluded from the returned list
   * @param seqNoAscending
   *          Determines of the list is returned by sequence number ascending (true) or descending
   * @return the list of inheritances of the role
   */
  List<RoleInheritance> getRoleInheritancesList(Role role, Role excludedInheritFrom,
      boolean seqNoAscending) {
    final OBCriteria<RoleInheritance> obCriteria = OBDal.getInstance()
        .createCriteria(RoleInheritance.class);
    obCriteria.add(Restrictions.eq(RoleInheritance.PROPERTY_ROLE, role));
    if (excludedInheritFrom != null) {
      obCriteria.add(Restrictions.ne(RoleInheritance.PROPERTY_INHERITFROM, excludedInheritFrom));
    }
    obCriteria.addOrderBy(RoleInheritance.PROPERTY_SEQUENCENUMBER, seqNoAscending);
    return obCriteria.list();
  }

  /**
   * Returns the list of template roles which a particular role is using.
   * 
   * @param role
   *          The role whose parent template role list will be retrieved
   * @param excludedInheritFrom
   *          A template role that can be excluded from the list
   * @param seqNoAscending
   *          Determines of the list is returned by sequence number ascending (true) or descending
   * @return the list of template roles used by role
   */
  List<Role> getRoleInheritancesInheritFromList(Role role, Role excludedInheritFrom,
      boolean seqNoAscending) {
    List<RoleInheritance> inheritancesList = getRoleInheritancesList(role, excludedInheritFrom,
        seqNoAscending);
    final List<Role> inheritFromList = new ArrayList<Role>();
    for (RoleInheritance ri : inheritancesList) {
      inheritFromList.add(ri.getInheritFrom());
    }
    return inheritFromList;
  }

  /**
   * Returns the list of inheritances of the role owner of the inheritance passed as parameter. It
   * also verifies if this inheritance fulfills the unique constraints, before adding it to the
   * list.
   * 
   * @param inheritance
   *          inheritance that contains the role information
   * @param deleting
   *          a flag which determines whether the inheritance passed as parameter should be included
   *          in the returned list or not.
   * @return the list of role inheritances
   */
  private List<RoleInheritance> getUpdatedRoleInheritancesList(RoleInheritance inheritance,
      boolean deleting) {
    final ArrayList<RoleInheritance> roleInheritancesList = new ArrayList<RoleInheritance>();
    final OBCriteria<RoleInheritance> obCriteria = OBDal.getInstance()
        .createCriteria(RoleInheritance.class);
    obCriteria.add(Restrictions.eq(RoleInheritance.PROPERTY_ROLE, inheritance.getRole()));
    obCriteria.add(Restrictions.ne(RoleInheritance.PROPERTY_ID, inheritance.getId()));
    obCriteria.addOrderBy(RoleInheritance.PROPERTY_SEQUENCENUMBER, true);
    boolean added = false;
    for (RoleInheritance rh : obCriteria.list()) {
      String inheritFromId = rh.getInheritFrom().getId();
      String inheritanceInheritFromId = inheritance.getInheritFrom().getId();
      if (inheritFromId.equals(inheritanceInheritFromId)) {
        Utility.throwErrorMessage("RoleInheritanceInheritFromDuplicated");
      } else if (rh.getSequenceNumber().equals(inheritance.getSequenceNumber())) {
        Utility.throwErrorMessage("RoleInheritanceSequenceNumberDuplicated");
      }
      if (!deleting && !added
          && rh.getSequenceNumber().longValue() > inheritance.getSequenceNumber().longValue()) {
        roleInheritancesList.add(inheritance);
        added = true;
      }
      roleInheritancesList.add(rh);
    }
    if (!deleting && !added) {
      roleInheritancesList.add(inheritance);
    }
    return roleInheritancesList;
  }

  /**
   * Returns the list of role template ids from an inheritance list.
   * 
   * @param roleInheritanceList
   *          a list of inheritances
   * @return the list of template role ids
   */
  private List<String> getRoleInheritancesInheritFromIdList(
      List<RoleInheritance> roleInheritanceList) {
    final ArrayList<String> roleIdsList = new ArrayList<String>();
    for (RoleInheritance roleInheritance : roleInheritanceList) {
      roleIdsList.add(roleInheritance.getInheritFrom().getId());
    }
    return roleIdsList;
  }

  /**
   * Returns the list of access types ordered by their priority value
   * 
   * @param ascending
   *          determines the sorting of the list, ascending (true) or descending (false)
   * 
   * @return the list of template access types
   */
  private List<AccessTypeInjector> getAccessTypeOrderByPriority(boolean ascending) {
    List<AccessTypeInjector> list = new ArrayList<AccessTypeInjector>();
    for (AccessTypeInjector injector : accessTypeInjectors) {
      list.add(injector);
    }
    Collections.sort(list);
    if (!ascending) {
      Collections.reverse(list);
    }
    return list;
  }

  /**
   * Returns the injector for the access type related to the canonical name of the class entered as
   * parameter
   * 
   * @param classCanonicalName
   *          the name of the class to identify the injector
   * 
   * @return the AccessTypeInjector used to retrieve the access type to be handled by the manager
   */
  private AccessTypeInjector getInjector(String classCanonicalName) {
    try {
      for (AccessTypeInjector injector : accessTypeInjectors
          .select(new AccessTypeInjector.Selector(classCanonicalName))) {
        return injector;
      }
    } catch (Exception e) {
      log.error("No access type injector found for class name: {}", classCanonicalName, e);
    }
    return null;
  }

  /**
   * Returns true if there exists an injector for the access type related to the canonical name of
   * the class entered as parameter
   * 
   * @param classCanonicalName
   *          the name of the class to identify the injector
   * 
   * @return true if exists an injector for the entered class name, false otherwise
   */
  boolean existsInjector(String classCanonicalName) {
    if (getInjector(classCanonicalName) == null) {
      return false;
    }
    return true;
  }

  /**
   * A class used to hold the results of the recalculation done for a particular access of a role
   */
  class CalculationResult {
    private int updated;
    private int created;

    /**
     * Basic constructor
     * 
     * @param updated
     *          An integer that represents the number of accesses updated during an access
     *          recalculation
     * @param created
     *          An integer that represents the number of new accesses created during an access
     *          recalculation
     */
    public CalculationResult(int updated, int created) {
      this.updated = updated;
      this.created = created;
    }

    /**
     * Returns the updated number
     * 
     * @return the value of the updated field
     */
    public int getUpdated() {
      return updated;
    }

    /**
     * Returns the created number
     * 
     * @return the value of the created field
     */
    public int getCreated() {
      return created;
    }
  }
}
