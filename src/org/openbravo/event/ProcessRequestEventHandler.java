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
package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.costing.CostingUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.ad.ui.ProcessGroup;
import org.openbravo.model.ad.ui.ProcessGroupList;
import org.openbravo.model.ad.ui.ProcessRequest;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Event Handler to check Process Group fields consistency
 */
class ProcessRequestEventHandler extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ProcessRequest.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkMandatoryFields(event);
    checkValidOrganizationCostingServer(event);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    checkValidOrganizationCostingServer(event);
  }

  private void checkMandatoryFields(EntityPersistenceEvent event) {

    final Entity processRequestEntity = ModelProvider.getInstance()
        .getEntity(ProcessRequest.ENTITY_NAME);
    final Property isGroupNameProperty = processRequestEntity
        .getProperty(ProcessRequest.PROPERTY_ISGROUP);
    final Property processProperty = processRequestEntity
        .getProperty(ProcessRequest.PROPERTY_PROCESS);
    final Property processGroupProperty = processRequestEntity
        .getProperty(ProcessRequest.PROPERTY_PROCESSGROUP);

    Boolean isGroup = (Boolean) event.getCurrentState(isGroupNameProperty);
    Process process = (Process) event.getCurrentState(processProperty);
    ProcessGroup processGroup = (ProcessGroup) event.getCurrentState(processGroupProperty);

    if (isGroup) {
      if (processGroup == null) {
        throw new OBException(OBMessageUtils.getI18NMessage("ProcessGroupMandatory", null));
      }
      if (process != null) {
        throw new OBException(OBMessageUtils.getI18NMessage("ProcessShouldBeNull", null));
      }
    } else {
      if (process == null) {
        throw new OBException(OBMessageUtils.getI18NMessage("ProcessMandatory", null));
      }
      if (processGroup != null) {
        throw new OBException(OBMessageUtils.getI18NMessage("ProcessGroupShouldBeNull", null));
      }
    }

  }

  /**
   * Returns and exception when the Costing Background Process has been scheduled for a child of a
   * legal entity organization.
   * 
   * This method checks a process request for a single process or for a group of processes
   * (iterating over the process group list)
   */
  private void checkValidOrganizationCostingServer(EntityPersistenceEvent event) {
    if (event != null && (event instanceof EntityNewEvent || event instanceof EntityUpdateEvent)) {
      final Entity processRequestEntity = ModelProvider.getInstance()
          .getEntity(ProcessRequest.ENTITY_NAME);

      final Property isGroupNameProperty = processRequestEntity
          .getProperty(ProcessRequest.PROPERTY_ISGROUP);
      final Property orgProperty = processRequestEntity
          .getProperty(ProcessRequest.PROPERTY_ORGANIZATION);

      final Boolean isGroup = (Boolean) event.getCurrentState(isGroupNameProperty);
      final Organization scheduledOrg = (Organization) event.getCurrentState(orgProperty);

      if (isGroup != null && isGroup.booleanValue()) {
        // Check for all the Processes defined in the group
        final Property processGroupProperty = processRequestEntity
            .getProperty(ProcessRequest.PROPERTY_PROCESSGROUP);
        final ProcessGroup processGroup = (ProcessGroup) event
            .getCurrentState(processGroupProperty);

        for (final ProcessGroupList pgl : processGroup.getProcessGroupListList()) {
          CostingUtils.checkValidOrganization(pgl.getProcess().getId(), scheduledOrg);
        }
      } else {
        final Property processProperty = processRequestEntity
            .getProperty(ProcessRequest.PROPERTY_PROCESS);
        final Process scheduledProcess = (Process) event.getCurrentState(processProperty);

        CostingUtils.checkValidOrganization(scheduledProcess.getId(), scheduledOrg);
      }
    }
  }

}
