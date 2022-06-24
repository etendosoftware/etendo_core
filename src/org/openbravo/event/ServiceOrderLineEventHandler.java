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
 * All portions are Copyright (C) 2015-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.event;

import java.math.BigDecimal;

import javax.enterprise.event.Observes;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.materialmgmt.ServicePriceUtils;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderlineServiceRelation;

class ServiceOrderLineEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(OrderLine.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final Entity orderLineEntity = ModelProvider.getInstance().getEntity(OrderLine.ENTITY_NAME);
    final OrderLine thisLine = (OrderLine) event.getTargetInstance();

    if (hasRelatedServices(thisLine)) {
      final Property lineNetAmountProperty = orderLineEntity
          .getProperty(OrderLine.PROPERTY_LINENETAMOUNT);
      final Property lineGrossAmountProperty = orderLineEntity
          .getProperty(OrderLine.PROPERTY_LINEGROSSAMOUNT);
      final Property orderedQtyProperty = orderLineEntity
          .getProperty(OrderLine.PROPERTY_ORDEREDQUANTITY);

      BigDecimal currentLineNetAmount = (BigDecimal) event.getCurrentState(lineNetAmountProperty);
      BigDecimal oldLineNetAmount = (BigDecimal) event.getPreviousState(lineNetAmountProperty);
      BigDecimal currentLineGrossAmount = (BigDecimal) event
          .getCurrentState(lineGrossAmountProperty);
      BigDecimal oldLineGrossAmount = (BigDecimal) event.getPreviousState(lineGrossAmountProperty);
      BigDecimal currentOrderedQty = (BigDecimal) event.getCurrentState(orderedQtyProperty);
      BigDecimal oldOrderedQty = (BigDecimal) event.getPreviousState(orderedQtyProperty);

      BigDecimal currentAmount = null;
      BigDecimal oldAmount = null;

      if (thisLine.getSalesOrder().isPriceIncludesTax()) {
        currentAmount = currentLineGrossAmount;
        oldAmount = oldLineGrossAmount;
      } else {
        currentAmount = currentLineNetAmount;
        oldAmount = oldLineNetAmount;
      }

      if (currentOrderedQty.compareTo(oldOrderedQty) != 0
          || currentAmount.compareTo(oldAmount) != 0) {
        Long lineNo = null;
        OBQuery<OrderlineServiceRelation> rol = relatedServices(thisLine);
        rol.setMaxResult(1000);
        final ScrollableResults scroller = rol.scroll(ScrollMode.FORWARD_ONLY);
        try {
          while (scroller.next()) {
            boolean changed = false;
            final OrderlineServiceRelation or = (OrderlineServiceRelation) scroller.get()[0];
            // Order Quantity has changed from positive to negative or backwards
            if (currentOrderedQty.signum() != 0 && oldOrderedQty.signum() != 0
                && currentOrderedQty.signum() != oldOrderedQty.signum()) {
              // Create new order line
              if (lineNo == null) {
                lineNo = ServicePriceUtils.getNewLineNo(thisLine.getSalesOrder().getId());
              } else {
                lineNo = lineNo + 10L;
              }
              OrderLine secondOrderline = (OrderLine) DalUtil.copy(or.getSalesOrderLine(), false);
              secondOrderline.setLineNo(lineNo);
              secondOrderline.setId(SequenceIdData.getUUID());
              secondOrderline.setNewOBObject(true);
              OBDal.getInstance().save(secondOrderline);

              // Delete relation line
              OBDal.getInstance().remove(or);

              // Create new relation line and relate to the new orderline
              OrderlineServiceRelation olsr = OBProvider.getInstance()
                  .get(OrderlineServiceRelation.class);
              olsr.setClient(thisLine.getClient());
              olsr.setOrganization(thisLine.getOrganization());
              olsr.setOrderlineRelated(thisLine);
              olsr.setSalesOrderLine(secondOrderline);
              olsr.setAmount(currentAmount);
              olsr.setQuantity(currentOrderedQty);
              OBDal.getInstance().save(olsr);
            } else {
              if (or.getQuantity().compareTo(currentOrderedQty) != 0) {
                or.setQuantity(currentOrderedQty);
                changed = true;
              }
              if (currentAmount.compareTo(oldAmount) != 0) {
                or.setAmount(currentAmount);
                changed = true;
              }
              if (changed) {
                OBDal.getInstance().save(or);
              }
            }
          }
        } catch (Exception e) {
          throw new OBException("Error in SalesOrderLineEventHandler" + e.getMessage(), true);
        } finally {
          if (scroller != null) {
            scroller.close();
          }
        }
      }
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final OrderLine thisLine = (OrderLine) event.getTargetInstance();

    if (hasRelatedServices(thisLine)) {
      OBQuery<OrderlineServiceRelation> rol = relatedServices(thisLine);
      rol.setMaxResult(1000);
      final ScrollableResults scroller = rol.scroll(ScrollMode.FORWARD_ONLY);
      while (scroller.next()) {
        final OrderlineServiceRelation or = (OrderlineServiceRelation) scroller.get()[0];
        OBDal.getInstance().remove(or);
      }
    }
    if (hasRelatedProducts(thisLine)) {
      OBQuery<OrderlineServiceRelation> rol = relatedProducts(thisLine);
      rol.setMaxResult(1000);
      final ScrollableResults scroller = rol.scroll(ScrollMode.FORWARD_ONLY);
      while (scroller.next()) {
        final OrderlineServiceRelation or = (OrderlineServiceRelation) scroller.get()[0];
        OBDal.getInstance().remove(or);
      }
    }
  }

  private boolean hasRelatedServices(OrderLine thisLine) {
    OBQuery<OrderlineServiceRelation> rol = relatedServices(thisLine);
    rol.setMaxResult(1);
    return rol.uniqueResult() != null;
  }

  private OBQuery<OrderlineServiceRelation> relatedServices(OrderLine thisLine) {
    //@formatter:off
    String hql = " as rol "
               + " where orderlineRelated.id = :orderLineId";
    //@formatter:on
    return OBDal.getInstance().createQuery(OrderlineServiceRelation.class, hql)
        .setNamedParameter("orderLineId", thisLine.getId());
  }

  private boolean hasRelatedProducts(OrderLine thisLine) {
    OBQuery<OrderlineServiceRelation> rol = relatedProducts(thisLine);
    rol.setMaxResult(1);
    return rol.uniqueResult() != null;
  }

  private OBQuery<OrderlineServiceRelation> relatedProducts(OrderLine thisLine) {
    //@formatter:off
    String hql = " as rol "
               + " where salesOrderLine.id = :orderLineId";
    //@formatter:on
    return OBDal.getInstance().createQuery(OrderlineServiceRelation.class, hql)
        .setNamedParameter("orderLineId", thisLine.getId());
  }
}
