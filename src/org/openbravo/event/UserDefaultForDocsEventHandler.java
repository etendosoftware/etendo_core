/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright © 2021–2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.event;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.businesspartner.BusinessPartner;

/**
 * Validates that only one {@link User} per Business Partner is marked as the default for
 * documents ({@code isdefaultfordocs = true}).
 */
@ApplicationScoped
public class UserDefaultForDocsEventHandler extends EntityPersistenceEventObserver {

  private static final Entity[] ENTITIES = {
      ModelProvider.getInstance().getEntity(User.ENTITY_NAME) };

  /**
   * Returns the entities observed by this handler.
   * @return an array containing {@link User}
   */
  @Override
  protected Entity[] getObservedEntities() {
    return ENTITIES;
  }

  /**
   * Handles the creation of a new {@link User} record. If the new record is marked as the default
   * for documents and another user in the same Business Partner already has that flag, an
   * {@link OBException} is thrown and the save is aborted.
   * @param event the entity creation event fired by the persistence framework
   */
  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    validate((User) event.getTargetInstance());
  }

  /**
   * Handles updates to an existing {@link User} record. If the updated record is marked as the
   * default for documents and another user in the same Business Partner already has that flag, an
   * {@link OBException} is thrown and the update is aborted.
   * @param event the entity update event fired by the persistence framework
   */
  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    validate((User) event.getTargetInstance());
  }

  /**
   * If the user is marked as default for docs and belongs to a Business Partner, ensures no other
   * user in that Business Partner already holds the same flag.
   * @param user the {@link User} being persisted
   * @throws OBException if another user in the same Business Partner is already the default
   */
  protected void validate(User user) {
    if (!user.isDefaultfordocs()) {
      return;
    }
    BusinessPartner bp = user.getBusinessPartner();
    if (bp == null) {
      return;
    }
    OBCriteria<User> criteria = OBDal.getInstance().createCriteria(User.class);
    criteria.add(Restrictions.eq(User.PROPERTY_BUSINESSPARTNER, bp));
    criteria.add(Restrictions.eq(User.PROPERTY_ISDEFAULTFORDOCS, true));
    criteria.add(Restrictions.ne(User.PROPERTY_ID, user.getId()));
    criteria.setMaxResults(1);
    if (criteria.uniqueResult() != null) {
      throw new OBException(OBMessageUtils.messageBD("OnlyOneDefaultPerUserBP"));
    }
  }
}
