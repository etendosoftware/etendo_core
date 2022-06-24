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
 * All portions are Copyright (C) 2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.externalbpartner.ExternalBusinessPartnerConfig;
import org.openbravo.model.externalbpartner.ExternalBusinessPartnerConfigProperty;

public class ExternalBusinessPartnerConfigColspan extends EntityPersistenceEventObserver {
  private static final Entity[] ENTITIES = {
      ModelProvider.getInstance().getEntity(ExternalBusinessPartnerConfig.ENTITY_NAME),
      ModelProvider.getInstance().getEntity(ExternalBusinessPartnerConfigProperty.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return ENTITIES;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkNoColspanIsGreaterThanMaxColumns(event);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkNoColspanIsGreaterThanMaxColumns(event);
  }

  private void checkNoColspanIsGreaterThanMaxColumns(final EntityPersistenceEvent event) {

    ExternalBusinessPartnerConfig config;
    Long result;
    final BaseOBObject targetInstance = event.getTargetInstance();
    if (targetInstance instanceof ExternalBusinessPartnerConfig) {
      config = (ExternalBusinessPartnerConfig) event.getTargetInstance();
      //@formatter:off
      String hql = "select count(1) "
          + "from C_ExtBP_Config as config, C_ExtBP_Config_Property as property "
          + "where property.detailColspan > :columnsdetailview "
          + "and config.id = :configId ";
      //@formatter:on
      result = (Long) OBDal.getInstance()
          .getSession()
          .createQuery(hql)
          .setParameter("configId", config.getId())
          .setParameter("columnsdetailview", config.getColumnsdetailview())
          .uniqueResult();
    } else {
      final ExternalBusinessPartnerConfigProperty property = (ExternalBusinessPartnerConfigProperty) event
          .getTargetInstance();
      if (!property.isActive()) {
        return;
      }
      config = property.getExternalBusinessPartnerIntegrationConfiguration();
      //@formatter:off
      String hql = "select count(1) "
          + "from C_ExtBP_Config as config, C_ExtBP_Config_Property as property "
          + "where :detailColspan > config.columnsdetailview "
          + "and config.id = :configId ";
      //@formatter:on
      result = (Long) OBDal.getInstance()
          .getSession()
          .createQuery(hql)
          .setParameter("configId", config.getId())
          .setParameter("detailColspan", property.getDetailColspan())
          .uniqueResult();
    }

    if (result != 0) {
      throw new OBException("@CRMColspanGreaterMaxCols@");
    }
  }

}
