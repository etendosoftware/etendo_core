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
 * All portions are Copyright (C) 2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ServicePriceRuleVersion;

public class ProductPriceRuleVersionEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ServicePriceRuleVersion.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final ServicePriceRuleVersion pcprv = (ServicePriceRuleVersion) event.getTargetInstance();
    linkProduct(event, pcprv);
  }

  private void linkProduct(EntityNewEvent event, ServicePriceRuleVersion pcprv) {
    final Product product = pcprv.getRelatedProductCategory() != null
        ? pcprv.getRelatedProductCategory().getProduct()
        : (pcprv.getRelatedProduct() != null ? pcprv.getRelatedProduct().getProduct() : null);

    if (product != null) {
      final Entity priceRuleVersionEntity = ModelProvider.getInstance()
          .getEntity(ServicePriceRuleVersion.ENTITY_NAME);
      final Property priceRuleVersionProductProperty = priceRuleVersionEntity
          .getProperty(ServicePriceRuleVersion.PROPERTY_PRODUCT);
      event.setCurrentState(priceRuleVersionProductProperty, product);
    }
  }
}
