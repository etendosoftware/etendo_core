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
package org.openbravo.erpCommon.businessUtility;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.enterprise.context.Dependent;

import org.apache.commons.lang.StringUtils;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderlineServiceRelation;
import org.openbravo.service.db.CallStoredProcedure;

/**
 * Process that creates a replacement order in temporary status in order to Cancel and Replace an
 * original order
 */
@Dependent
class CreateReplacementOrderExecutor extends CancelAndReplaceUtils {
  private Order oldOrder;
  private Map<Warehouse, Integer> warehouseMap;

  @SuppressWarnings("hiding")
  void init(final Order oldOrder, final Map<Warehouse, Integer> warehouseMap) {
    this.oldOrder = oldOrder;
    this.warehouseMap = warehouseMap;
  }

  List<Order> run() {
    final List<Order> replacementOrderList = new ArrayList<>();
    String documentNo = oldOrder.getDocumentNo();
    for (final Entry<Warehouse, Integer> warehouseEntry : warehouseMap.entrySet()) {
      for (int i = 0; i < warehouseEntry.getValue(); i++) {
        final Order replacementOrder = createReplacementOrder(warehouseEntry.getKey(), documentNo);
        documentNo = replacementOrder.getDocumentNo();
        replacementOrderList.add(replacementOrder);
      }
    }
    return replacementOrderList;
  }

  private Order createReplacementOrder(final Warehouse warehouse, final String documentNo) {
    // Create new Order header
    Order newOrder = (Order) DalUtil.copy(oldOrder, false, true);

    // Change order values
    final Organization org = warehouse.getOrganization();
    newOrder.setOrganization(org);
    newOrder.setWarehouse(warehouse);

    if (!oldOrder.getOrganization().getId().equals(org.getId())) {
      final Optional<DocumentType> docTypeOptional = getDocumentType(org,
          oldOrder.getDocumentType());
      if (docTypeOptional.isPresent()) {
        final DocumentType docType = docTypeOptional.get();
        newOrder.setDocumentType(docType);
        newOrder.setTransactionDocument(docType);
      }
    }

    newOrder.setProcessed(false);
    newOrder.setPosted("N");
    newOrder.setDocumentStatus("TMP");
    newOrder.setDocumentAction("CO");
    newOrder.setGrandTotalAmount(BigDecimal.ZERO);
    newOrder.setSummedLineAmount(BigDecimal.ZERO);
    newOrder.setOrderDate(new Date());
    newOrder.setReplacedorder(oldOrder);
    newOrder.setDocumentNo(getNextCancelDocNo(documentNo));
    OBDal.getInstance().save(newOrder);

    // Create new Order lines
    long i = 0;
    try (final ScrollableResults orderLines = getOrderLineList(oldOrder)) {
      while (orderLines.next()) {
        final OrderLine oldOrderLine = (OrderLine) orderLines.get(0);
        // Skip discount lines as they will be created when booking the replacement order
        if (oldOrderLine.getOrderDiscount() != null) {
          continue;
        }
        final OrderLine newOrderLine = (OrderLine) DalUtil.copy(oldOrderLine, false, true);
        newOrderLine.setOrganization(org);
        newOrderLine.setWarehouse(warehouse);
        newOrderLine.setDeliveredQuantity(BigDecimal.ZERO);
        newOrderLine.setReservedQuantity(BigDecimal.ZERO);
        newOrderLine.setInvoicedQuantity(BigDecimal.ZERO);
        newOrderLine.setSalesOrder(newOrder);
        newOrderLine.setReplacedorderline(oldOrderLine);
        newOrder.getOrderLineList().add(newOrderLine);
        OBDal.getInstance().save(newOrderLine);
        if ((++i % 100) == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
          newOrder = OBDal.getInstance().get(Order.class, newOrder.getId());
        }
      }
    }

    // Flush before updating Relations between Products and services to ensure all the Order Lines
    // have been calculated properly
    OBDal.getInstance().flush();
    updateRelationsBetweenOrderLinesProductsAndServices(newOrder);
    return newOrder;
  }

  private Optional<DocumentType> getDocumentType(final Organization org,
      final DocumentType docType) {
    final List<Object> parameters = new ArrayList<>();
    parameters.add(org.getClient().getId());
    parameters.add(org.getId());
    parameters.add(docType.getDocumentCategory());
    parameters.add(docType.getSOSubType());
    final String docTypeId = (String) CallStoredProcedure.getInstance()
        .call("AD_GET_DOCTYPE", parameters, null, false);

    if (StringUtils.isEmpty(docTypeId)) {
      return Optional.empty();
    }
    return Optional.of(OBDal.getInstance().get(DocumentType.class, docTypeId));
  }

  /**
   * Update the relationships between the services and products in the new order lines. After all
   * the lines are created, it is needed to update relations taking into account if those relations
   * were present in the lines they are replacing
   * 
   * @param order
   *          The new created order where the lines will be updated
   */
  private void updateRelationsBetweenOrderLinesProductsAndServices(final Order order) {
    int i = 0;
    try (
        final ScrollableResults newOrderLines = getOrderLinesListWithReplacedLineWithRelatedService(
            order)) {
      while (newOrderLines.next()) {
        updateOrderLineRelatedServices((OrderLine) newOrderLines.get(0));

        if ((++i % 100) == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
      }
    }
  }

  private ScrollableResults getOrderLinesListWithReplacedLineWithRelatedService(final Order order) {
    //@formatter:off
    final String hql = 
                  "select ol " +
                  "  from OrderLine ol" +
                  "    join ol.replacedorderline rol " + // Explicit join to avoid null values
                  " where rol.orderlineServiceRelationList is not empty" +
                  "   and ol.salesOrder.id = :orderId";
    //@formatter:on

    return OBDal.getInstance()
        .getSession()
        .createQuery(hql, OrderLine.class)
        .setParameter("orderId", order.getId())
        .scroll(ScrollMode.FORWARD_ONLY);
  }

  private void updateOrderLineRelatedServices(final OrderLine orderLine) {
    final Order order = orderLine.getSalesOrder();
    final OrderLine replacedOrderLine = orderLine.getReplacedorderline();

    for (final OrderlineServiceRelation replacedRelatedService : replacedOrderLine
        .getOrderlineServiceRelationList()) {
      final OrderLine replacedRelatedOrderLine = replacedRelatedService.getOrderlineRelated();
      final OrderLine orderLineReplacingRelatedOrderLine = getOrderLineReplacingRelatedOrderLine(
          order, replacedRelatedOrderLine);
      addNewOrderLineServiceRelation(orderLine, orderLineReplacingRelatedOrderLine);
    }
  }

  /**
   * Method returns the order line of an order that is replacing an specific order line
   * 
   * @param order
   *          The order where the order line will be searched
   * @param replacedOrderLine
   *          The replaced order line that is searching for
   * @return The order line that is replacing the one passed as parameter
   */
  private OrderLine getOrderLineReplacingRelatedOrderLine(final Order order,
      final OrderLine replacedOrderLine) {
    return (OrderLine) OBDal.getInstance()
        .createCriteria(OrderLine.class)
        .add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, order))
        .add(Restrictions.eq(OrderLine.PROPERTY_REPLACEDORDERLINE, replacedOrderLine))
        .setMaxResults(1)
        .uniqueResult();
  }

  private void addNewOrderLineServiceRelation(final OrderLine orderLine,
      final OrderLine orderLineRelated) {
    final OrderlineServiceRelation newOrderLineServiceRelation = getNewOrderLineServiceRelation(
        orderLine, orderLineRelated);
    orderLine.getOrderlineServiceRelationList().add(newOrderLineServiceRelation);
  }

  private OrderlineServiceRelation getNewOrderLineServiceRelation(final OrderLine orderLine,
      final OrderLine orderLineRelated) {
    final OrderlineServiceRelation newOrderLineServiceRelation = OBProvider.getInstance()
        .get(OrderlineServiceRelation.class);
    newOrderLineServiceRelation.setClient(orderLine.getClient());
    newOrderLineServiceRelation.setOrganization(orderLine.getOrganization());
    newOrderLineServiceRelation.setAmount(orderLine.getLineGrossAmount());
    newOrderLineServiceRelation.setOrderlineRelated(orderLineRelated);
    newOrderLineServiceRelation.setQuantity(orderLine.getOrderedQuantity());
    newOrderLineServiceRelation.setSalesOrderLine(orderLine);
    OBDal.getInstance().save(newOrderLineServiceRelation);
    return newOrderLineServiceRelation;
  }

}
