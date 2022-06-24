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
 * Contributor(s):______________________________________.
 *************************************************************************
 */
package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.CharacteristicSubsetValue;
import org.openbravo.model.common.plm.ProductCharacteristic;
import org.openbravo.model.common.plm.ProductCharacteristicConf;

class SubsetValueEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(CharacteristicSubsetValue.class) };
  private static ThreadLocal<String> chsubsetvalueUpdated = new ThreadLocal<>();

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final CharacteristicSubsetValue chsubsetv = (CharacteristicSubsetValue) event
        .getTargetInstance();
    chsubsetvalueUpdated.set(chsubsetv.getId());
    // Update all product characteristics configurations with updated code of the Subset Value.
    final Entity chsubsetValue = ModelProvider.getInstance()
        .getEntity(CharacteristicSubsetValue.ENTITY_NAME);
    final Property codeProperty = chsubsetValue
        .getProperty(CharacteristicSubsetValue.PROPERTY_CODE);
    if (event.getCurrentState(codeProperty) != event.getPreviousState(codeProperty)) {
      OBCriteria<ProductCharacteristic> productCharacteristic = OBDal.getInstance()
          .createCriteria(ProductCharacteristic.class);
      productCharacteristic.add(Restrictions.eq(ProductCharacteristic.PROPERTY_CHARACTERISTICSUBSET,
          chsubsetv.getCharacteristicSubset()));
      if (productCharacteristic.count() > 0) {
        for (ProductCharacteristic productCh : productCharacteristic.list()) {
          OBCriteria<ProductCharacteristicConf> productCharateristicsConf = OBDal.getInstance()
              .createCriteria(ProductCharacteristicConf.class);
          productCharateristicsConf
              .add(Restrictions.eq(ProductCharacteristicConf.PROPERTY_CHARACTERISTICVALUE,
                  chsubsetv.getCharacteristicValue()));
          productCharateristicsConf.add(Restrictions
              .eq(ProductCharacteristicConf.PROPERTY_CHARACTERISTICOFPRODUCT, productCh));
          if (productCharateristicsConf.count() > 0) {
            for (ProductCharacteristicConf conf : productCharateristicsConf.list()) {
              if (chsubsetv.getCode() != conf.getCode()) {
                conf.setCode(chsubsetv.getCode());
                OBDal.getInstance().save(conf);
              }
            }
          }
        }
      }
    }
  }
}
