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
package org.openbravo.client.application.attachment;

import javax.enterprise.event.Observes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.utility.AttachmentConfig;

/**
 * Event Handler on AttachmentConfig entity that manages the changes on the Attachment Method used
 * on each Client. It also ensures that there are not more than 1 record active for each client. It
 * listens to Insert, Update or Delete events to update the cached "clientConfigs" Map in
 * AttachmentUtils.
 */
class AttachmentConfigEventHandler extends EntityPersistenceEventObserver {
  private static final Logger logger = LogManager.getLogger();

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(AttachmentConfig.ENTITY_NAME) };
  private static Property propActive = entities[0].getProperty(AttachmentConfig.PROPERTY_ACTIVE);

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  /**
   * It checks that there is not already an active config for the client and updates the cached
   * client config. If the record is being deactivated it sends an empty id to reset the
   * configuration.
   */
  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final AttachmentConfig newAttConfig = (AttachmentConfig) event.getTargetInstance();

    isAnyActivated(event);
    String clientId = newAttConfig.getClient().getId();
    if ((Boolean) event.getCurrentState(propActive)) {
      AttachmentUtils.setAttachmentConfig(clientId, event.getId());
    } else if ((Boolean) event.getPreviousState(propActive)) {
      // Deactivating a config reset AttachmentUtils state
      AttachmentUtils.setAttachmentConfig(clientId, null);
    }
  }

  /**
   * It checks that there is not already an active config for the client and updates the cached
   * client config.
   */
  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final AttachmentConfig newAttConfig = (AttachmentConfig) event.getTargetInstance();

    isAnyActivated(event);
    String clientId = newAttConfig.getClient().getId();
    if ((Boolean) event.getCurrentState(propActive)) {
      AttachmentUtils.setAttachmentConfig(clientId, newAttConfig.getId());
    }
  }

  /**
   * It updates the cached client config by sending a empty id.
   */
  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final AttachmentConfig deletedAttachmentConfig = (AttachmentConfig) event.getTargetInstance();
    String clientId = deletedAttachmentConfig.getClient().getId();
    if (deletedAttachmentConfig.isActive()) {
      // The active config of the client is deleted. Update AttachmentUtils with an empty config
      AttachmentUtils.setAttachmentConfig(clientId, null);
    }

  }

  private void isAnyActivated(EntityPersistenceEvent event) {
    final AttachmentConfig newAttachmentConfig = (AttachmentConfig) event.getTargetInstance();
    if (!newAttachmentConfig.isActive()) {
      return;
    }
    final OBQuery<AttachmentConfig> attachmentConfigQuery = OBDal.getInstance()
        .createQuery(AttachmentConfig.class, "id!=:id and client.id=:clientId");
    attachmentConfigQuery.setNamedParameter("id", newAttachmentConfig.getId());
    attachmentConfigQuery.setNamedParameter("clientId", newAttachmentConfig.getClient().getId());
    // Ensure that filtering by client and active is done.
    attachmentConfigQuery.setFilterOnReadableClients(true);
    attachmentConfigQuery.setFilterOnActive(true);

    if (!attachmentConfigQuery.list().isEmpty()) {
      logger.error("Error saving, more than one active config detected.");
      throw new OBException(OBMessageUtils.messageBD("AD_EnabledAttachmentMethod"));
    }
  }

}
