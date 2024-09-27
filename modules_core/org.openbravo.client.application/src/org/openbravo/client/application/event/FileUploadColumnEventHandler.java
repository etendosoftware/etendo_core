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
 * All portions are Copyright (C) 2022 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.datamodel.Column;

/**
 * This event handler prevents to add File upload references in standard windows. These references
 * are available for ProcessDefinition only
 */
public class FileUploadColumnEventHandler extends EntityPersistenceEventObserver {
  private static final String FILE_UPLOAD_REFERENCE_ID = "715C53D4FEA74B28B74F14AE65BC5C16";
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Column.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    this.checkReferenceIsNotFileUpload(event);
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    this.checkReferenceIsNotFileUpload(event);
  }

  private void checkReferenceIsNotFileUpload(EntityPersistenceEvent event) {
    Column instanceColumn = (Column) event.getTargetInstance();

    if (instanceColumn.getReference().getId().equals(FILE_UPLOAD_REFERENCE_ID)) {
      throw new OBException(OBMessageUtils.getI18NMessage("OBUIAPP_CannotAddFileUploadReference"));
    }
  }
}