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
 * All portions are Copyright (C) 2013-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.event;

import java.math.BigDecimal;

import javax.enterprise.event.Observes;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.materialmgmt.transaction.ProductionLine;
import org.openbravo.model.materialmgmt.transaction.ProductionPlan;
import org.openbravo.service.db.DalConnectionProvider;

class ProductionLineEventHandler extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ProductionLine.ENTITY_NAME) };
  private static final String BOM_PRODUCTION = "321";
  private static final BigDecimal ZERO = new BigDecimal("0");

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    VariablesSecureApp vars = null;
    try {
      vars = RequestContext.get().getVariablesSecureApp();
    } catch (Exception e) {
      throw new OBException("Error: " + e.getMessage());
    }
    String currentTabId = vars.getStringParameter("tabId");
    if (BOM_PRODUCTION.equals(currentTabId)) {
      final Entity productionLineEntity = ModelProvider.getInstance()
          .getEntity(ProductionLine.ENTITY_NAME);
      final Property productionPlanProperty = productionLineEntity
          .getProperty(ProductionLine.PROPERTY_PRODUCTIONPLAN);
      final Property movementQtyProperty = productionLineEntity
          .getProperty(ProductionLine.PROPERTY_MOVEMENTQUANTITY);
      final ProductionPlan productionPlan = (ProductionPlan) event
          .getCurrentState(productionPlanProperty);
      final BigDecimal currentMovementQty = (BigDecimal) event.getCurrentState(movementQtyProperty);
      final BigDecimal previousMovementQty = (BigDecimal) event
          .getPreviousState(movementQtyProperty);
      OBCriteria<ProductionLine> productionLineCriteria = OBDal.getInstance()
          .createCriteria(ProductionLine.class);
      productionLineCriteria
          .add(Restrictions.eq(ProductionLine.PROPERTY_PRODUCTIONPLAN, productionPlan));
      productionLineCriteria.add(Restrictions.gt(ProductionLine.PROPERTY_MOVEMENTQUANTITY, ZERO));
      if (productionLineCriteria.count() > 0 && previousMovementQty != currentMovementQty) {
        if (currentMovementQty.compareTo(ZERO) == 1 && previousMovementQty.compareTo(ZERO) != 1) {
          String language = OBContext.getOBContext().getLanguage().getLanguage();
          ConnectionProvider conn = new DalConnectionProvider(false);
          throw new OBException(
              Utility.messageBD(conn, "@ConsumedProductWithPostiveQty@", language));
        } else if (currentMovementQty.compareTo(ZERO) == -1
            && previousMovementQty.compareTo(ZERO) != -1 && productionLineCriteria.count() == 1) {
          String language = OBContext.getOBContext().getLanguage().getLanguage();
          ConnectionProvider conn = new DalConnectionProvider(false);
          throw new OBException(
              Utility.messageBD(conn, "@ProducedProductWithNegativeQty@", language));
        }
      }
    }
  }
}
