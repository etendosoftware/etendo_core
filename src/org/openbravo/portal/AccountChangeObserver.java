/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.portal;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.email.EmailEventManager;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;

/**
 * Observes cancellation (active is set to false) of users with access to portal and sends an email
 * to the user informing the account is expired.
 * 
 * @see AccountCancelledEmailGenerator
 * @author alostale
 * 
 */
public class AccountChangeObserver extends EntityPersistenceEventObserver {
  private final static Entity userEntity = ModelProvider.getInstance().getEntity(User.ENTITY_NAME);
  private final static Entity[] entities = { userEntity };
  private final static Logger log = LogManager.getLogger();
  public static final String EVT_ACCOUNT_CANCELLED = "accountCancelled";

  @Inject
  private EmailEventManager emailManager;

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdateActive(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    // account cancellation
    Boolean wasActive = (Boolean) event
        .getPreviousState(userEntity.getProperty(User.PROPERTY_ACTIVE));
    Boolean isActive = (Boolean) event
        .getCurrentState(userEntity.getProperty(User.PROPERTY_ACTIVE));
    if (wasActive && !isActive) {
      OBContext.setAdminMode(false);
      try {
        final User user = (User) event.getTargetInstance();

        // check if user has access to portal
        String where = "as r " + //
            "where forPortalUsers = true " + //
            "  and exists (from ADUserRoles ur " + //
            "              where ur.userContact = :user " + //
            "               and ur.role = r" + //
            "               and ur.active = true)";
        OBQuery<Role> qPortal = OBDal.getInstance().createQuery(Role.class, where);
        qPortal.setNamedParameter("user", user);
        if (StringUtils.isNotEmpty(user.getEmail()) && qPortal.count() > 0) {
          emailManager.sendEmail(EVT_ACCOUNT_CANCELLED, user.getEmail(), user);
        }
      } catch (Exception e) {
        // in case of fail, continue not to stop saving
        log.error("Error sending email for cancelled account", e);
      } finally {
        OBContext.restorePreviousMode();
      }
    }
  }
}
