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
 * All portions are Copyright (C) 2013-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

class MInOutLineEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ShipmentInOutLine.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    ShipmentInOutLine shipmentInOutLine = (ShipmentInOutLine) event.getTargetInstance();

    if (shipmentInOutLine.getProduct() == null
        && (shipmentInOutLine.getMovementQuantity().doubleValue() != 0)) {
      throw new OBException(OBMessageUtils.messageBD("ProductNullAndMovementQtyGreaterZero"));
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    ShipmentInOutLine shipmentInOutLine = (ShipmentInOutLine) event.getTargetInstance();

    if (shipmentInOutLine.getProduct() == null
        && (shipmentInOutLine.getMovementQuantity().doubleValue() != 0)) {
      throw new OBException(OBMessageUtils.messageBD("ProductNullAndMovementQtyGreaterZero"));
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkShipmentOrderRelation((ShipmentInOutLine) event.getTargetInstance());
  }

  private void checkShipmentOrderRelation(ShipmentInOutLine shipmentInOutLine) {
    OBCriteria<ShipmentInOutLine> criteria = OBDal.getInstance()
        .createCriteria(ShipmentInOutLine.class);
    criteria.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT,
        shipmentInOutLine.getShipmentReceipt()));

    if (criteria.count() == 1) {
      ShipmentInOut shipmentInOut = OBDal.getInstance()
          .get(ShipmentInOut.class, shipmentInOutLine.getShipmentReceipt().getId());
      if (shipmentInOut != null) {
        shipmentInOut.setSalesOrder(null);
        OBDal.getInstance().save(shipmentInOut);
      }
    }
  }
}
