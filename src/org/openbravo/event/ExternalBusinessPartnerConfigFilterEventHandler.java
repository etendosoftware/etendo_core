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
import org.openbravo.model.externalbpartner.ExternalBusinessPartnerConfigFilter;

public class ExternalBusinessPartnerConfigFilterEventHandler extends EntityPersistenceEventObserver {
  private static final Entity[] ENTITIES = {
    ModelProvider.getInstance().getEntity(ExternalBusinessPartnerConfigFilter.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return ENTITIES;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkScanFilterIsUnique(event);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkScanFilterIsUnique(event);
  }

  private void checkScanFilterIsUnique(final EntityPersistenceEvent event) {
    final String id = event.getId();
    final ExternalBusinessPartnerConfigFilter filter = (ExternalBusinessPartnerConfigFilter) event.getTargetInstance();
    final ExternalBusinessPartnerConfig currentExtBPConfig = filter.getExternalBusinessPartnerIntegrationConfiguration();

    if (!filter.isScanIdentifier() || !filter.isActive()) {
      return;
    }

    // Query the filters for the current config and check that there is 0 or 1 with the flag
    // isScanIdentifier=true.
    // Fail otherwise
    final OBCriteria<?> criteria = OBDal.getInstance()
      .createCriteria(event.getTargetInstance().getClass());
    criteria.add(Restrictions.eq(
      ExternalBusinessPartnerConfigFilter.PROPERTY_EXTERNALBUSINESSPARTNERINTEGRATIONCONFIGURATION,
      currentExtBPConfig));
    criteria
      .add(Restrictions.eq(ExternalBusinessPartnerConfigFilter.PROPERTY_ISSCANIDENTIFIER, true));
    criteria.add(Restrictions.eq(ExternalBusinessPartnerConfigFilter.PROPERTY_ACTIVE, true));
    criteria.add(Restrictions.ne(ExternalBusinessPartnerConfigFilter.PROPERTY_ID, id));

    criteria.setMaxResults(1);
    if (criteria.uniqueResult() != null) {
      throw new OBException("@DuplicatedCRMScanFilter@");
    }
  }
}
