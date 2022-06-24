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

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.externalbpartner.ExternalBusinessPartnerConfig;
import org.openbravo.model.externalbpartner.ExternalBusinessPartnerConfigProperty;

public class ExternalBusinessPartnerConfigPropertyEventHandler extends EntityPersistenceEventObserver {
  private static final Entity[] ENTITIES = {
    ModelProvider.getInstance().getEntity(ExternalBusinessPartnerConfigProperty.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return ENTITIES;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkDefaultEmailDuplicates(event);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkDefaultEmailDuplicates(event);
  }

  private void checkDefaultEmailDuplicates(EntityPersistenceEvent event) {
    final String id = event.getId();
    final ExternalBusinessPartnerConfigProperty property = (ExternalBusinessPartnerConfigProperty) event.getTargetInstance();
    final ExternalBusinessPartnerConfig currentExtBPConfig = property.getExternalBusinessPartnerIntegrationConfiguration();

    if (!property.isDefaultemail() || !property.isActive()) {
      return;
    }

    final OBCriteria<?> criteria = OBDal.getInstance()
      .createCriteria(event.getTargetInstance().getClass());
    criteria.add(Restrictions.eq(
      ExternalBusinessPartnerConfigProperty.PROPERTY_EXTERNALBUSINESSPARTNERINTEGRATIONCONFIGURATION,
      currentExtBPConfig));
    criteria
      .add(Restrictions.eq(ExternalBusinessPartnerConfigProperty.PROPERTY_ISDEFAULTEMAIL, true));
    criteria.add(Restrictions.eq(ExternalBusinessPartnerConfigProperty.PROPERTY_ACTIVE, true));
    criteria.add(Restrictions.ne(ExternalBusinessPartnerConfigProperty.PROPERTY_ID, id));

    criteria.setMaxResults(1);
    if (criteria.uniqueResult() != null) {
      throw new OBException("@DuplicatedCRMDefaultEmail@");
    }
  }
}
