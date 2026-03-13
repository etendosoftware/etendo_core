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
package org.openbravo.email.event;

import java.util.List;

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
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Validates that only one {@link EmailServerConfiguration} record per scope is marked as the
 * default configuration ({@code ISDEFAULTCONFIGURATION = 'Y'}).
 */
public class DefaultSmtpConfigEventHandler extends EntityPersistenceEventObserver {
  private static final Entity[] ENTITIES = {
      ModelProvider.getInstance().getEntity(EmailServerConfiguration.ENTITY_NAME) };

  /**
   * Returns the entities observed by this handler.
   * @return an array containing {@link EmailServerConfiguration}
   */
  @Override
  protected Entity[] getObservedEntities() {
    return ENTITIES;
  }

  /**
   * Handles the creation of a new {@link EmailServerConfiguration} record. If the new record is
   * marked as the default configuration and another record in the same scope already has that flag,
   * an {@link OBException} is thrown and the save is aborted.
   * @param event the entity creation event fired by the persistence framework
   */
  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    handleDefaultConfigChange((EmailServerConfiguration) event.getTargetInstance());
  }

  /**
   * Handles updates to an existing {@link EmailServerConfiguration} record. If the updated record
   * is marked as the default configuration and another record in the same scope already has that
   * flag, an {@link OBException} is thrown and the update is aborted.
   * @param event the entity update event fired by the persistence framework
   */
  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    handleDefaultConfigChange((EmailServerConfiguration) event.getTargetInstance());
  }

  /**
   * Core validation logic. If {@code saved} is marked as the default configuration, searches for
   * other records in the same scope that already hold that flag. If any conflict is found, throws
   * an {@link OBException} identifying the conflicting record and aborts the save.
   * @param saved the {@link EmailServerConfiguration} being persisted
   * @throws OBException if another record in the same scope is already marked as default
   */
  protected static void handleDefaultConfigChange(EmailServerConfiguration saved) {
    if (!isMarkedAsDefault(saved)) {
      return;
    }
    List<EmailServerConfiguration> conflicts = findOtherDefaultsInScope(saved);
    if (!conflicts.isEmpty()) {
      throw new OBException(OBMessageUtils.messageBD("SmtpDuplicateDefaultConfig"));
    }
  }

  /**
   * Returns {@code true} if the given configuration is marked as the default.
   * @param config the configuration to inspect
   * @return {@code true} if {@code defaultConfiguration} is {@code true}
   */
  protected static boolean isMarkedAsDefault(EmailServerConfiguration config) {
    return config.isDefaultConfiguration();
  }

  /**
   * Returns {@code true} if the given configuration is scoped to a specific user, i.e. its
   * {@code userContact} property is not {@code null}.
   * @param config the configuration to inspect
   * @return {@code true} for user-level configurations; {@code false} for org/client-level ones
   */
  protected static boolean isUserLevelConfig(EmailServerConfiguration config) {
    return config.getUserContact() != null;
  }

  /**
   * Finds all {@link EmailServerConfiguration} records in the same scope as {@code saved} that are
   * currently marked as the default configuration, excluding {@code saved} itself.
   * @param saved the configuration that triggered the event
   * @return list of other configurations currently marked as default within the same scope;
   *   never {@code null}
   */
  protected static List<EmailServerConfiguration> findOtherDefaultsInScope(EmailServerConfiguration saved) {
    if (isUserLevelConfig(saved)) {
      return findOtherUserDefaults(saved.getUserContact(), saved.getId());
    }
    return findOtherOrgDefaults(saved.getOrganization(), saved.getId());
  }

  /**
   * Queries for {@link EmailServerConfiguration} records that belong to the given {@code user}
   * (user-level scope), are marked as the default configuration, and are not the record identified
   * by {@code excludedId}.
   * @param user the user whose scope is being inspected
   * @param excludedId the ID of the record that triggered the event, excluded from results
   * @return list of conflicting default configurations at user level; never {@code null}
   */
  protected static List<EmailServerConfiguration> findOtherUserDefaults(User user, String excludedId) {
    OBCriteria<EmailServerConfiguration> criteria = buildDefaultConfigCriteria(excludedId);
    criteria.add(Restrictions.eq(EmailServerConfiguration.PROPERTY_USERCONTACT, user));
    return criteria.list();
  }

  /**
   * Queries for {@link EmailServerConfiguration} records that belong to the given
   * {@code organization} (org/client-level scope), have no linked user
   * ({@code userContact IS NULL}), are marked as the default configuration, and are not the record
   * identified by {@code excludedId}.
   * @param organization the organization whose scope is being inspected
   * @param excludedId the ID of the record that triggered the event, excluded from results
   * @return list of conflicting default configurations at organization/client level;
   *   never {@code null}
   */
  protected static List<EmailServerConfiguration> findOtherOrgDefaults(Organization organization,
      String excludedId) {
    OBCriteria<EmailServerConfiguration> criteria = buildDefaultConfigCriteria(excludedId);
    criteria.add(Restrictions.eq(EmailServerConfiguration.PROPERTY_ORGANIZATION, organization));
    criteria.add(Restrictions.isNull(EmailServerConfiguration.PROPERTY_USERCONTACT));
    return criteria.list();
  }

  /**
   * Builds a base {@link OBCriteria} for {@link EmailServerConfiguration} pre-filtered to records
   * that are currently marked as the default configuration and do not match {@code excludedId}.
   * @param excludedId the ID of the triggering record, excluded from results to avoid self-detection
   * @return a pre-configured {@link OBCriteria} instance; never {@code null}
   */
  protected static OBCriteria<EmailServerConfiguration> buildDefaultConfigCriteria(String excludedId) {
    OBCriteria<EmailServerConfiguration> criteria = OBDal.getInstance()
        .createCriteria(EmailServerConfiguration.class);
    criteria.add(Restrictions.eq(EmailServerConfiguration.PROPERTY_DEFAULTCONFIGURATION, true));
    criteria.add(Restrictions.ne(EmailServerConfiguration.PROPERTY_ID, excludedId));
    return criteria;
  }
}
