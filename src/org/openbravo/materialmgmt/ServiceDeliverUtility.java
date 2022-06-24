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
 * All portions are Copyright (C) 2019-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.materialmgmt;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Tuple;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

/**
 * 
 * Delivers service type products. Adds shipment lines in selected shipment for service type
 * products; the quantity delivered is computed considering the quantity rule of the product.
 *
 */
public class ServiceDeliverUtility {

  private static final String UNIQUE_QUANTITY = "UQ";
  private static final String AS_PER_PRODUCT = "PP";

  public static void deliverServices(final ShipmentInOut shipment) {
    Map<String, BigDecimal> serviceToDeliver = getShipmentServiceOrderlinesAndQtyToDeliver(
        shipment);
    serviceToDeliver.entrySet()
        .stream()
        .filter(map -> map.getValue().compareTo(BigDecimal.ZERO) != 0)
        .forEach(s -> addShipmentLine(shipment, s.getKey(), s.getValue()));
  }

  private static Map<String, BigDecimal> getShipmentServiceOrderlinesAndQtyToDeliver(
      ShipmentInOut shipment) {
    Map<String, BigDecimal> servicesAndQtyToDeliver = new HashMap<>();
    Map<String, BigDecimal> orderlineDeliveredQty = new HashMap<>();
    List<Tuple> serviceRelated = getServiceRelated(shipment);
    for (Tuple service : serviceRelated) {

      BigDecimal serviceOrderedQuantity = (BigDecimal) service.get("serviceOrderedQuantity");
      BigDecimal serviceDeliveredQuantity = (BigDecimal) service.get("serviceDeliveredQuantity");
      BigDecimal movementQuantity = (BigDecimal) service.get("movementQuantity");
      String serviceOrderLineId = (String) service.get("serviceOrderLineId");
      String orderlineAndServiceId = (String) service.get("id");
      if (UNIQUE_QUANTITY.equals(service.get("quantityRule"))
          && serviceOrderedQuantity.compareTo(serviceDeliveredQuantity) != 0) {
        servicesAndQtyToDeliver.put(serviceOrderLineId, BigDecimal.ONE);
        addOrderlineQtyDelivered(orderlineDeliveredQty, (String) service.get("id"), BigDecimal.ONE);
      }
      if (AS_PER_PRODUCT.equals(service.get("quantityRule"))) {
        BigDecimal partialQuantity = servicesAndQtyToDeliver.get(serviceOrderLineId) == null
            ? BigDecimal.ZERO
            : servicesAndQtyToDeliver.get(serviceOrderLineId);
        servicesAndQtyToDeliver
            .put(serviceOrderLineId,
                getAsPerProductQuantity(orderlineAndServiceId, orderlineDeliveredQty,
                    movementQuantity, serviceOrderedQuantity.subtract(serviceDeliveredQuantity))
                        .add(partialQuantity));
      }
    }

    return servicesAndQtyToDeliver;
  }

  private static BigDecimal getAsPerProductQuantity(String orderlineAndServiceId,
      Map<String, BigDecimal> orderlineDeliveredQty, BigDecimal movementQuantity,
      BigDecimal servicePendingQty) {
    BigDecimal previouslyDelivered = orderlineDeliveredQty.get(orderlineAndServiceId) != null
        ? orderlineDeliveredQty.get(orderlineAndServiceId)
        : BigDecimal.ZERO;
    BigDecimal pendingQty = movementQuantity.subtract(previouslyDelivered);
    orderlineDeliveredQty.put(orderlineAndServiceId, previouslyDelivered
        .add(pendingQty.compareTo(servicePendingQty) > 0 ? servicePendingQty : pendingQty));
    return pendingQty.compareTo(servicePendingQty) > 0 ? servicePendingQty : pendingQty;

  }

  private static void addOrderlineQtyDelivered(Map<String, BigDecimal> orderlineDeliveredQty,
      String orderlineId, BigDecimal qtyToAdd) {
    if (orderlineDeliveredQty.get(orderlineId) == null) {
      orderlineDeliveredQty.put(orderlineId, qtyToAdd);
    } else {
      orderlineDeliveredQty.put(orderlineId, orderlineDeliveredQty.get(orderlineId).add(qtyToAdd));
    }
  }

  private static List<Tuple> getServiceRelated(ShipmentInOut shipment) {
    //@formatter:off
    String hql = "select ol.id || serv.id as id, "
               + "       iol.movementQuantity as movementQuantity, "
               + "       ol.orderedQuantity as orderedQuantity, "
               + "       ol.deliveredQuantity as deliveredQuantity, "
               + "       sol.id as serviceOrderLineId, "
               + "       serv.quantityRule as quantityRule, "
               + "       sol.orderedQuantity as serviceOrderedQuantity, "
               + "       sol.deliveredQuantity as serviceDeliveredQuantity "
               + "from MaterialMgmtShipmentInOutLine iol "
               + "join iol.salesOrderLine ol "
               + "join ol.orderlineServiceRelationCOrderlineRelatedIDList srol "
               + "join srol.salesOrderLine sol "
               + "join sol.product serv "
               + "where iol.shipmentReceipt.id = :shipmentId ";
    //@formatter:on
    return OBDal.getInstance()
        .getSession()
        .createQuery(hql, Tuple.class)
        .setParameter("shipmentId", shipment.getId())
        .list();
  }

  private static ShipmentInOutLine addShipmentLine(ShipmentInOut shipment, String orderlineId,
      BigDecimal qtyToDeliver) {
    ShipmentInOutLine shipmentLine = OBProvider.getInstance().get(ShipmentInOutLine.class);
    OrderLine orderLine = OBDal.getInstance().get(OrderLine.class, orderlineId);
    shipmentLine.setOrganization(shipment.getOrganization());
    shipmentLine.setShipmentReceipt(shipment);
    shipmentLine.setSalesOrderLine(orderLine);
    Long lineNo = (shipment.getMaterialMgmtShipmentInOutLineList().size() + 1) * 10L;
    shipmentLine.setLineNo(lineNo);
    shipmentLine.setProduct(orderLine.getProduct());
    shipmentLine.setUOM(orderLine.getUOM());
    shipmentLine.setMovementQuantity(qtyToDeliver);
    String description = orderLine.getDescription();
    if (description != null && description.length() > 255) {
      description = description.substring(0, 254);
    }
    shipmentLine.setDescription(description);
    if (orderLine.getBOMParent() != null) {
      OBCriteria<ShipmentInOutLine> obc = OBDal.getInstance()
          .createCriteria(ShipmentInOutLine.class);
      obc.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT, shipment));
      obc.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SALESORDERLINE, orderLine.getBOMParent()));
      obc.setMaxResults(1);
      shipmentLine.setBOMParent((ShipmentInOutLine) obc.uniqueResult());
    }
    OBDal.getInstance().save(shipmentLine);
    shipment.getMaterialMgmtShipmentInOutLineList().add(shipmentLine);
    OBDal.getInstance().save(shipment);
    return shipmentLine;
  }
}
