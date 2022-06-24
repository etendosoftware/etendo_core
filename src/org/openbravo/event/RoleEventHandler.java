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

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.common.enterprise.Organization;

class RoleEventHandler extends EntityPersistenceEventObserver {
  private static final String InitialOrgSetup_CLASSNAME = "org.openbravo.erpCommon.businessUtility.InitialOrgSetup";
  private static final String InitialClientSetup_CLASSNAME = "org.openbravo.erpCommon.businessUtility.InitialClientSetup";

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Role.ENTITY_NAME) };

  private static final Logger logger = LogManager.getLogger();

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onNew(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final Entity roleEntity = ModelProvider.getInstance().getEntity(Role.class);
    final Property roleProperty = roleEntity.getProperty(Role.PROPERTY_ADROLEORGANIZATIONLIST);
    final Role role = (Role) event.getTargetInstance();

    populateOrgAccess(event, role, roleProperty);
  }

  /**
   * Creates the necessary Org Access records only when the role is set Manual=N and when we don't
   * come from the Initial Client/Organization setup
   */
  private void populateOrgAccess(EntityNewEvent event, Role role, Property roleProperty) {
    // Create org access for new automatic role
    try {
      if (!role.isManual() && !isComingFromInitialClientOrganizationSetup()) {
        List<RoleOrganization> roleOrganizationList = getRoleOrganizationList(role);
        @SuppressWarnings("unchecked")
        final List<Object> roleOrganizations = (List<Object>) event.getCurrentState(roleProperty);
        roleOrganizations.addAll(roleOrganizationList);
      }
    } catch (Exception e) {
      logger
          .error("Error in RoleEventHandler while inserting Org Access to role " + role.getName());
    }
  }

  // Get org access list
  private List<RoleOrganization> getRoleOrganizationList(Role role) throws Exception {
    List<RoleOrganization> roleOrganizationList = new ArrayList<>();

    // Client or System level: Only * [isOrgAdmin=N]
    if (StringUtils.equals(role.getUserLevel(), " C")
        || StringUtils.equals(role.getUserLevel(), "S")) {
      roleOrganizationList
          .add(getRoleOrganization(role, OBDal.getInstance().get(Organization.class, "0"), false));
      logger.debug("Added organization * to role " + role.getName());
    }

    // Client/Organization level: * [isOrgAdmin=N], other Orgs (but *) [isOrgAdmin=Y]
    else if (StringUtils.equals(role.getUserLevel(), " CO")) {
      roleOrganizationList
          .add(getRoleOrganization(role, OBDal.getInstance().get(Organization.class, "0"), false));
      logger.debug("Added organization * to role " + role.getName());

      OBCriteria<Organization> criteria = OBDal.getInstance().createCriteria(Organization.class);
      criteria.setFilterOnActive(false);
      criteria.add(Restrictions.eq(Organization.PROPERTY_CLIENT, role.getClient()));
      criteria.add(Restrictions.ne(Organization.PROPERTY_ID, "0"));
      ScrollableResults scroll = criteria.scroll(ScrollMode.FORWARD_ONLY);
      try {
        while (scroll.next()) {
          final Organization organization = (Organization) scroll.get()[0];
          roleOrganizationList.add(getRoleOrganization(role, organization, true));
          logger
              .debug("Added organization " + organization.getName() + " to role " + role.getName());
        }
      } finally {
        scroll.close();
      }
    }

    // Organization level: Orgs (but *) [isOrgAdmin=Y]
    else if (StringUtils.equals(role.getUserLevel(), "  O")) {
      OBCriteria<Organization> criteria = OBDal.getInstance().createCriteria(Organization.class);
      criteria.add(Restrictions.eq(Organization.PROPERTY_CLIENT, role.getClient()));
      criteria.setFilterOnActive(false);
      ScrollableResults scroll = criteria.scroll(ScrollMode.FORWARD_ONLY);
      try {
        while (scroll.next()) {
          final Organization organization = (Organization) scroll.get()[0];
          roleOrganizationList.add(getRoleOrganization(role, organization, true));
          logger
              .debug("Added organization " + organization.getName() + " to role " + role.getName());
        }
      } finally {
        scroll.close();
      }
    }

    return roleOrganizationList;
  }

  // Get org access
  private RoleOrganization getRoleOrganization(Role role, Organization orgProvided,
      boolean isOrgAdmin) throws Exception {
    final RoleOrganization newRoleOrganization = OBProvider.getInstance()
        .get(RoleOrganization.class);
    newRoleOrganization.setClient(role.getClient());
    newRoleOrganization.setOrganization(orgProvided);
    newRoleOrganization.setRole(role);
    newRoleOrganization.setOrgAdmin(isOrgAdmin);
    return newRoleOrganization;
  }

  /**
   * Returns true if the Initial Client/Organization Setup is in the stack trace
   */
  private boolean isComingFromInitialClientOrganizationSetup() {
    boolean comeFrom_ICS_IOS = false;
    for (final StackTraceElement ste : Thread.currentThread().getStackTrace()) {
      final String clazz = ste.getClassName();
      if (StringUtils.equals(clazz, InitialOrgSetup_CLASSNAME)
          || StringUtils.equals(clazz, InitialClientSetup_CLASSNAME)) {
        comeFrom_ICS_IOS = true;
        logger.debug(
            "Coming from Initial Client/Organization Setup. RoleEventHandler will not insert Org Access records");
        break;
      }
    }
    return comeFrom_ICS_IOS;
  }

}
