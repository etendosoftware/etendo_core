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
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
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
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderlineServiceRelation;

@Dependent
public class ServiceOrderLineEventHandler extends EntityPersistenceEventObserver {
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

    final boolean relatedServices = hasRelatedServices(thisLine);
    if (relatedServices) {
      final Property lineNetAmountProperty = orderLineEntity
          .getProperty(OrderLine.PROPERTY_LINENETAMOUNT);
      final Property lineGrossAmountProperty = orderLineEntity
          .getProperty(OrderLine.PROPERTY_LINEGROSSAMOUNT);
      final Property orderedQtyProperty = orderLineEntity
          .getProperty(OrderLine.PROPERTY_ORDEREDQUANTITY);
      final Property unitPriceProperty = orderLineEntity.getProperty(OrderLine.PROPERTY_UNITPRICE);
      final Property grossUnitPriceProperty = orderLineEntity
          .getProperty(OrderLine.PROPERTY_GROSSUNITPRICE);

      BigDecimal currentLineNetAmount = (BigDecimal) event.getCurrentState(lineNetAmountProperty);
      BigDecimal oldLineNetAmount = (BigDecimal) event.getPreviousState(lineNetAmountProperty);
      BigDecimal currentLineGrossAmount = (BigDecimal) event
          .getCurrentState(lineGrossAmountProperty);
      BigDecimal oldLineGrossAmount = (BigDecimal) event.getPreviousState(lineGrossAmountProperty);
      BigDecimal currentOrderedQty = (BigDecimal) event.getCurrentState(orderedQtyProperty);
      BigDecimal oldOrderedQty = (BigDecimal) event.getPreviousState(orderedQtyProperty);
      BigDecimal currentUnitPrice = (BigDecimal) event.getCurrentState(unitPriceProperty);
      BigDecimal oldUnitPrice = (BigDecimal) event.getPreviousState(unitPriceProperty);
      BigDecimal currentGrossUnitPrice = (BigDecimal) event.getCurrentState(grossUnitPriceProperty);
      BigDecimal oldGrossUnitPrice = (BigDecimal) event.getPreviousState(grossUnitPriceProperty);

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
          && currentAmount.compareTo(oldAmount) == 0) {
        if (thisLine.getSalesOrder().isPriceIncludesTax()) {
          currentAmount = currentGrossUnitPrice.multiply(currentOrderedQty);
          oldAmount = oldGrossUnitPrice.multiply(oldOrderedQty);
        } else {
          currentAmount = currentUnitPrice.multiply(currentOrderedQty);
          oldAmount = oldUnitPrice.multiply(oldOrderedQty);
        }
      }

      if (currentOrderedQty.compareTo(oldOrderedQty) != 0
          || currentAmount.compareTo(oldAmount) != 0) {
        Long lineNo = null;
        final Set<String> affectedServiceLineIds = new HashSet<>();
        OBQuery<OrderlineServiceRelation> rol = relatedServices(thisLine);
        rol.setMaxResult(1000);
        final ScrollableResults scroller = rol.scroll(ScrollMode.FORWARD_ONLY);
        try {
          while (scroller.next()) {
            boolean changed = false;
            final OrderlineServiceRelation or = (OrderlineServiceRelation) scroller.get();
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
              affectedServiceLineIds.add(secondOrderline.getId());
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
              affectedServiceLineIds.add(or.getSalesOrderLine().getId());
            }
          }
          for (String serviceLineId : affectedServiceLineIds) {
            recomputeServiceLine(OBDal.getInstance().get(OrderLine.class, serviceLineId));
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
        final OrderlineServiceRelation or = (OrderlineServiceRelation) scroller.get();
        OBDal.getInstance().remove(or);
      }
    }
    if (hasRelatedProducts(thisLine)) {
      OBQuery<OrderlineServiceRelation> rol = relatedProducts(thisLine);
      rol.setMaxResult(1000);
      final ScrollableResults scroller = rol.scroll(ScrollMode.FORWARD_ONLY);
      while (scroller.next()) {
        final OrderlineServiceRelation or = (OrderlineServiceRelation) scroller.get();
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

  private void recomputeServiceLine(OrderLine serviceOrderLine) {
    BigDecimal relatedAmount = BigDecimal.ZERO;
    BigDecimal relatedQty = BigDecimal.ZERO;
    BigDecimal relatedPrice = BigDecimal.ZERO;
    final Currency currency = serviceOrderLine.getCurrency();
    final List<OrderlineServiceRelation> relations = OBDal.getInstance()
        .createQuery(OrderlineServiceRelation.class, " as e where e.salesOrderLine.id = :serviceLineId")
        .setNamedParameter("serviceLineId", serviceOrderLine.getId())
        .list();
    final JSONObject relatedInfo = new JSONObject();
    final JSONArray relatedLines = new JSONArray();
    final JSONArray relatedAmounts = new JSONArray();
    final JSONArray relatedDiscounts = new JSONArray();
    final JSONArray relatedPrices = new JSONArray();
    final JSONArray relatedQuantities = new JSONArray();
    final JSONArray relatedUnitDiscounts = new JSONArray();

    try {
      for (OrderlineServiceRelation relation : relations) {
        relatedAmount = relatedAmount.add(relation.getAmount());
        relatedQty = relatedQty.add(relation.getQuantity());
        final BigDecimal linePrice = relation.getQuantity().compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO
            : relation.getAmount().divide(relation.getQuantity(), currency.getPricePrecision().intValue(),
                RoundingMode.HALF_UP);
        relatedPrice = relatedPrice.add(linePrice);

        relatedLines.put(relation.getOrderlineRelated().getId());
        relatedAmounts.put(relation.getAmount());
        relatedDiscounts.put(JSONObject.NULL);
        relatedPrices.put(linePrice);
        relatedQuantities.put(relation.getQuantity());
        relatedUnitDiscounts.put(JSONObject.NULL);
      }
      relatedInfo.put("relatedLines", relatedLines);
      relatedInfo.put("lineAmount", relatedAmounts);
      relatedInfo.put("lineDiscounts", relatedDiscounts);
      relatedInfo.put("linePriceamount", relatedPrices);
      relatedInfo.put("lineRelatedqty", relatedQuantities);
      relatedInfo.put("lineUnitdiscountsamt", relatedUnitDiscounts);
    } catch (JSONException e) {
      throw new OBException("Error in SalesOrderLineEventHandler" + e.getMessage(), true);
    }
    BigDecimal serviceQty = relatedQty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : relatedQty;
    if (ServicePriceUtils.UNIQUE_QUANTITY.equals(serviceOrderLine.getProduct().getQuantityRule())) {
      serviceQty = relatedQty.compareTo(BigDecimal.ZERO) < 0 ? new BigDecimal("-1") : BigDecimal.ONE;
    }

    final BigDecimal basePrice = ServicePriceUtils.getProductPrice(serviceOrderLine.getOrderDate(),
        serviceOrderLine.getSalesOrder().getPriceList(), serviceOrderLine.getProduct());
    final BigDecimal variableAmount = ServicePriceUtils.getServiceAmount(serviceOrderLine, relatedAmount,
        null, relatedPrice, relatedQty, null, relatedInfo);
    final BigDecimal servicePrice = basePrice.add(variableAmount.divide(serviceQty,
        currency.getPricePrecision().intValue(), RoundingMode.HALF_UP));
    final BigDecimal serviceAmount = variableAmount.add(basePrice.multiply(serviceQty))
        .setScale(currency.getPricePrecision().intValue(), RoundingMode.HALF_UP);

    serviceOrderLine.setOrderedQuantity(serviceQty);
    if (serviceOrderLine.getSalesOrder().isPriceIncludesTax()) {
      serviceOrderLine.setGrossUnitPrice(servicePrice);
      serviceOrderLine.setLineGrossAmount(serviceAmount);
      serviceOrderLine.setTaxableAmount(serviceAmount);
    } else {
      serviceOrderLine.setUnitPrice(servicePrice);
      serviceOrderLine.setLineNetAmount(serviceAmount);
      serviceOrderLine.setTaxableAmount(serviceAmount);
    }
    OBDal.getInstance().save(serviceOrderLine);
  }
}
