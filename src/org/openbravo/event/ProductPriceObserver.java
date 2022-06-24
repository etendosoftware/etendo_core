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

package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.model.pricing.pricelist.ProductPrice;

class ProductPriceObserver extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ProductPrice.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    overrideProductPriceOrganization(event);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    overrideProductPriceOrganization(event);
  }

  /**
   * Sets the Product Price Organization equal to the Price List organization
   */
  private void overrideProductPriceOrganization(EntityPersistenceEvent event) {
    if (event instanceof EntityNewEvent || event instanceof EntityUpdateEvent) {
      final Entity productPriceEntity = ModelProvider.getInstance()
          .getEntity(ProductPrice.ENTITY_NAME);
      final Property orgProperty = productPriceEntity
          .getProperty(ProductPrice.PROPERTY_ORGANIZATION);
      final Property plvProperty = productPriceEntity
          .getProperty(ProductPrice.PROPERTY_PRICELISTVERSION);

      final Organization org = (Organization) event.getCurrentState(orgProperty);
      final PriceListVersion plv = (PriceListVersion) event.getCurrentState(plvProperty);
      if (plv != null) {
        final Organization plOrg = plv.getOrganization();
        if (org != null && plOrg != null && !StringUtils.equals(org.getId(), plOrg.getId())) {
          event.setCurrentState(orgProperty, plOrg);
        }
      }
    }
  }
}
