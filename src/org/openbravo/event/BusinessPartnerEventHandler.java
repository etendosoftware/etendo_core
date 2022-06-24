/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
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
 *************************************************************************
 */
package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.model.common.businesspartner.BusinessPartner;

class BusinessPartnerEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(BusinessPartner.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final BusinessPartner bp = (BusinessPartner) event.getTargetInstance();
    setUpdateCurrency(event, bp);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final BusinessPartner bp = (BusinessPartner) event.getTargetInstance();
    setUpdateCurrency(event, bp);
  }

  private void setUpdateCurrency(EntityPersistenceEvent event, BusinessPartner bp) {
    if (bp.getCurrency() == null
        && (bp.getPriceList() != null || bp.getPurchasePricelist() != null)) {

      final Entity bpEntity = ModelProvider.getInstance().getEntity(BusinessPartner.ENTITY_NAME);
      final Property bpCurrencyProperty = bpEntity.getProperty(BusinessPartner.PROPERTY_CURRENCY);

      event.setCurrentState(bpCurrencyProperty,
          bp.getPriceList() != null ? bp.getPriceList().getCurrency()
              : bp.getPurchasePricelist().getCurrency());
    }
  }
}
