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
 * All portions are Copyright (C) 2015-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.event;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.ServicePriceUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderlineServiceRelation;
import org.openbravo.model.common.plm.Product;

class ServiceRelationEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(OrderlineServiceRelation.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final Entity orderLineRelationEntity = ModelProvider.getInstance()
        .getEntity(OrderlineServiceRelation.ENTITY_NAME);
    final Property solProperty = orderLineRelationEntity
        .getProperty(OrderlineServiceRelation.PROPERTY_SALESORDERLINE);
    final Property amountProperty = orderLineRelationEntity
        .getProperty(OrderlineServiceRelation.PROPERTY_AMOUNT);
    final Property quantityProperty = orderLineRelationEntity
        .getProperty(OrderlineServiceRelation.PROPERTY_QUANTITY);
    BigDecimal amount = (BigDecimal) event.getCurrentState(amountProperty);
    BigDecimal quantity = (BigDecimal) event.getCurrentState(quantityProperty);
    OrderLine orderLine = (OrderLine) event.getCurrentState(solProperty);
    if (orderLine.getSalesOrder().getCancelledorder() == null) {
      updateOrderLine(orderLine, amount, quantity, BigDecimal.ZERO, BigDecimal.ZERO);
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final Entity orderLineRelationEntity = ModelProvider.getInstance()
        .getEntity(OrderlineServiceRelation.ENTITY_NAME);
    final Property solProperty = orderLineRelationEntity
        .getProperty(OrderlineServiceRelation.PROPERTY_SALESORDERLINE);
    final Property amountProperty = orderLineRelationEntity
        .getProperty(OrderlineServiceRelation.PROPERTY_AMOUNT);
    final Property quantityProperty = orderLineRelationEntity
        .getProperty(OrderlineServiceRelation.PROPERTY_QUANTITY);
    BigDecimal currentAmount = (BigDecimal) event.getCurrentState(amountProperty);
    BigDecimal currentQuantity = (BigDecimal) event.getCurrentState(quantityProperty);
    BigDecimal oldAmount = (BigDecimal) event.getPreviousState(amountProperty);
    BigDecimal oldQuantity = (BigDecimal) event.getPreviousState(quantityProperty);
    OrderLine currentOrderLine = (OrderLine) event.getCurrentState(solProperty);
    if (currentOrderLine.getSalesOrder().getCancelledorder() == null) {
      updateOrderLine(currentOrderLine, currentAmount, currentQuantity, oldAmount, oldQuantity);
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final Entity orderLineRelationEntity = ModelProvider.getInstance()
        .getEntity(OrderlineServiceRelation.ENTITY_NAME);
    final Property solProperty = orderLineRelationEntity
        .getProperty(OrderlineServiceRelation.PROPERTY_SALESORDERLINE);
    final Property amountProperty = orderLineRelationEntity
        .getProperty(OrderlineServiceRelation.PROPERTY_AMOUNT);
    final Property quantityProperty = orderLineRelationEntity
        .getProperty(OrderlineServiceRelation.PROPERTY_QUANTITY);
    BigDecimal oldAmount = (BigDecimal) event.getCurrentState(amountProperty);
    BigDecimal oldQuantity = (BigDecimal) event.getCurrentState(quantityProperty);
    OrderLine orderLine = (OrderLine) event.getCurrentState(solProperty);
    if (orderLine.getSalesOrder().getCancelledorder() == null) {
      updateOrderLine(orderLine, BigDecimal.ZERO, BigDecimal.ZERO, oldAmount, oldQuantity);
    }
  }

  private void updateOrderLine(OrderLine currentOrderLine, BigDecimal currentAmount,
      BigDecimal currentqty, BigDecimal oldAmount, BigDecimal oldQuantity) {
    BigDecimal serviceQty = currentOrderLine.getOrderedQuantity();
    BigDecimal listPrice = BigDecimal.ZERO;
    Currency currency = currentOrderLine.getCurrency();
    HashMap<String, BigDecimal> dbValues = ServicePriceUtils
        .getRelatedAmountAndQty(currentOrderLine);
    BigDecimal dbAmount = dbValues.get("amount");
    BigDecimal dbQuantity = dbValues.get("quantity");
    BigDecimal dbPrice = dbValues.get("price");
    BigDecimal currentPrice = currentqty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
        : currentAmount.divide(currentqty, currency.getPricePrecision().intValue(),
            RoundingMode.HALF_UP);
    BigDecimal oldPrice = oldQuantity.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
        : oldAmount.divide(oldQuantity, currency.getPricePrecision().intValue(),
            RoundingMode.HALF_UP);
    BigDecimal baseProductPrice = ServicePriceUtils.getProductPrice(
        currentOrderLine.getSalesOrder().getOrderDate(),
        currentOrderLine.getSalesOrder().getPriceList(), currentOrderLine.getProduct());
    BigDecimal serviceAmount = ServicePriceUtils.getServiceAmount(currentOrderLine,
        dbAmount.add(currentAmount.subtract(oldAmount))
            .setScale(currency.getPricePrecision().intValue(), RoundingMode.HALF_UP),
        null,
        dbPrice.add(currentPrice.subtract(oldPrice))
            .setScale(currency.getPricePrecision().intValue(), RoundingMode.HALF_UP),
        dbQuantity.add(currentqty.subtract(oldQuantity)), null);
    Product service = currentOrderLine.getProduct();

    if (ServicePriceUtils.UNIQUE_QUANTITY.equals(service.getQuantityRule())) {
      if (currentqty.compareTo(BigDecimal.ZERO) > 0) {
        serviceQty = BigDecimal.ONE;
      } else if (currentqty.compareTo(BigDecimal.ZERO) < 0) {
        serviceQty = new BigDecimal("-1");
      } else {
        if (oldQuantity.compareTo(BigDecimal.ZERO) >= 0) {
          serviceQty = BigDecimal.ONE;
        } else {
          serviceQty = new BigDecimal("-1");
        }
      }
    } else {
      // TODO
      // Fix the issue of calling this event handler twice when modifying
      // OrderlineServiceRelation from ServiceOrderLineEventHandler. Investigate why it happens
      serviceQty = dbQuantity.add(currentqty).subtract(oldQuantity);
      serviceQty = serviceQty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : serviceQty;
    }
    currentOrderLine.setOrderedQuantity(serviceQty);

    BigDecimal servicePrice = baseProductPrice.add(serviceAmount.divide(serviceQty,
        currency.getPricePrecision().intValue(), RoundingMode.HALF_UP));
    serviceAmount = serviceAmount.add(baseProductPrice.multiply(serviceQty))
        .setScale(currency.getPricePrecision().intValue(), RoundingMode.HALF_UP);
    if (currentOrderLine.getSalesOrder().isPriceIncludesTax()) {
      currentOrderLine.setGrossUnitPrice(servicePrice);
      currentOrderLine.setLineGrossAmount(serviceAmount);
      currentOrderLine.setBaseGrossUnitPrice(servicePrice);
      listPrice = currentOrderLine.getGrossListPrice();
    } else {
      currentOrderLine.setUnitPrice(servicePrice);
      currentOrderLine.setLineNetAmount(serviceAmount);
      currentOrderLine.setStandardPrice(servicePrice);
      listPrice = currentOrderLine.getListPrice();
    }
    currentOrderLine.setTaxableAmount(serviceAmount);

    // Calculate discount
    BigDecimal discount = listPrice.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
        : listPrice.subtract(servicePrice)
            .multiply(new BigDecimal("100"))
            .divide(listPrice, currency.getPricePrecision().intValue(), RoundingMode.HALF_EVEN);
    currentOrderLine.setDiscount(discount);
    OBDal.getInstance().save(currentOrderLine);
  }
}
