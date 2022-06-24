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

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * It is not possible to have two different metadatas configured for an Attachment Method with the
 * same column name. This event handlers checks that no new metadatas with an existing columnname
 * are added or edited.
 */
class AttachmentMetadataEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Parameter.class) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onInsert(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    doChecks(event);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    doChecks(event);
  }

  private void doChecks(EntityPersistenceEvent event) {
    Parameter parameter = (Parameter) event.getTargetInstance();
    if (parameter.getAttachmentMethod() == null) {
      return;
    }
    if (checkDuplicates(parameter)) {
      // If there is a duplicated Attachment Metadata throw an exception.
      throw new OBException(OBMessageUtils.messageBD("OBUIAPP_DuplicatedDBColumnname"));
    }
  }

  /**
   * Checks if exists another Parameter (Attachment Metadata) with the same DB Column Name in the
   * same Attachment Method.
   * 
   * @param parameter
   *          The new Attachment Metadata.
   * @return true if exists another parameter.
   */
  private boolean checkDuplicates(Parameter parameter) {
    OBCriteria<Parameter> critParam = OBDal.getInstance().createCriteria(Parameter.class);
    critParam
        .add(Restrictions.eq(Parameter.PROPERTY_ATTACHMENTMETHOD, parameter.getAttachmentMethod()));
    critParam.add(Restrictions.eq(Parameter.PROPERTY_DBCOLUMNNAME, parameter.getDBColumnName()));
    critParam.add(Restrictions.ne(Parameter.PROPERTY_ID, parameter.getId()));
    critParam.setMaxResults(1);
    return critParam.uniqueResult() != null;
  }

}
