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
 * All portions are Copyright (C) 2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.InheritedAccessEnabled;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.web.UserContextCache;

/**
 * Observers events in entities that affect {@link UserContextCache} to invalidate it. These
 * entities are that can modify access settings such as anyone implementing
 * {@link InheritedAccessEnabled} and the ones defining user/roles.
 */
public class UserCtxCacheObserver extends EntityPersistenceEventObserver {

  /** Entities defining user/role but not implementing {@link InheritedAccessEnabled} */
  private static Entity[] entities = { //
      ModelProvider.getInstance().getEntity(Organization.class), //
      ModelProvider.getInstance().getEntity(Role.class),
      ModelProvider.getInstance().getEntity(User.class),
      ModelProvider.getInstance().getEntity(UserRoles.class) };

  /** Invalidates {@link UserContextCache} if any entity affecting permissions is modified. */
  public void invalidateCacheOnAnyValidEvent(@Observes EntityPersistenceEvent event) {
    if (isValidEvent(event)) {
      UserContextCache.getInstance().invalidate();
    }
  }

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  @Override
  protected boolean isValidEvent(EntityPersistenceEvent event) {
    boolean valid = super.isValidEvent(event);
    return valid || (event.getTargetInstance() instanceof InheritedAccessEnabled
        && !(event.getTargetInstance() instanceof Preference));
  }
}
