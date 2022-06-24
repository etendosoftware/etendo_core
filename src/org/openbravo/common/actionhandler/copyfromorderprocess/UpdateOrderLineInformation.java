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
 * All portions are Copyright (C) 2017-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.actionhandler.copyfromorderprocess;

import java.util.Set;

import javax.enterprise.context.Dependent;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;

@Dependent
@Qualifier(CopyFromOrdersProcessImplementationInterface.COPY_FROM_ORDER_PROCESS_HOOK_QUALIFIER)
class UpdateOrderLineInformation implements CopyFromOrdersProcessImplementationInterface {
  private Order processingOrder;
  private OrderLine orderLine;
  private OrderLine newOrderLine;

  @Override
  public int getOrder() {
    return -50;
  }

  /**
   * Updates the Information of the new Order Line that is related with the old Order Line and Order
   * Header, like Organization, Warehouse, etc
   */
  @Override
  public void exec(final Order processingOrderParam, final OrderLine orderLineParam,
      OrderLine newOrderLineParam) {
    this.processingOrder = processingOrderParam;
    this.orderLine = orderLineParam;
    this.newOrderLine = newOrderLineParam;

    // Create to the new order line the reference to the order line from it is created
    updateOrderLineReference();

    // Information updated from order: Client, OrderDate, ScheduledDeliveryDate, Description,
    // Warehouse, Business Partner and BusinessPartner Address
    updateInformationFromOrder();

    // Information updated from orderLine: Organization, Currency, Project, CostCenter, Asset, User1
    // Dimension and User2 Dimension
    udpateInformationFromOrderLine();
  }

  /**
   * Creates to the new order line the reference to the order line from it is created.
   * 
   * @param orderLine
   *          The order line to be referenced
   * @param newOrderLine
   *          The new order line
   */
  private void updateOrderLineReference() {
    newOrderLine.setSOPOReference(orderLine);
    newOrderLine.setSelectOrderLine(Boolean.TRUE);
  }

  /**
   * Updates some order line information from the order it is created and itlinks the order line to
   * the order.
   * 
   * @param newOrderLine
   *          The order line to be updated
   */
  private void updateInformationFromOrder() {
    newOrderLine.setSalesOrder(processingOrder);
    newOrderLine.setClient(processingOrder.getClient());
    newOrderLine.setOrderDate(processingOrder.getOrderDate());
    newOrderLine.setScheduledDeliveryDate(processingOrder.getScheduledDeliveryDate());
    newOrderLine.setDescription(orderLine.getDescription());
    // If Warehouse of the Header is null retrieve it from the Context
    newOrderLine
        .setWarehouse(processingOrder.getWarehouse() != null ? processingOrder.getWarehouse()
            : OBContext.getOBContext().getWarehouse());
    newOrderLine.setBusinessPartner(processingOrder.getBusinessPartner());
    // If PartnerAddress of the Header is null retrieve it from the Business Partner
    newOrderLine.setPartnerAddress(
        processingOrder.getPartnerAddress() != null ? processingOrder.getPartnerAddress()
            : OBDal.getInstance()
                .getProxy(Location.class,
                    getMaxBusinessPartnerLocationId(processingOrder.getBusinessPartner())));
  }

  private void udpateInformationFromOrderLine() {
    newOrderLine.setOrganization(getOrganizationForNewLine());
    newOrderLine.setCurrency(orderLine.getCurrency());
    newOrderLine.setProject(orderLine.getProject());
    newOrderLine.setCostcenter(orderLine.getCostcenter());
    newOrderLine.setAsset(orderLine.getAsset());
    newOrderLine.setStDimension(orderLine.getStDimension());
    newOrderLine.setNdDimension(orderLine.getNdDimension());
  }

  private Organization getOrganizationForNewLine() {
    Organization organizationForNewLine = processingOrder.getOrganization();
    Set<String> parentOrgTree = new OrganizationStructureProvider()
        .getChildTree(organizationForNewLine.getId(), true);
    // If the Organization of the line that is being copied belongs to the child tree of the
    // Organization of the document header of the new line, use the organization of the line being
    // copied, else use the organization of the document header of the new line
    if (parentOrgTree.contains(orderLine.getOrganization().getId())) {
      organizationForNewLine = orderLine.getOrganization();
    }
    return organizationForNewLine;
  }

  /**
   * Returns the last business partner location ID
   * 
   * @param businessPartner
   *          The business partner where the location will be searched
   * @return the last business partner location ID
   */
  private String getMaxBusinessPartnerLocationId(final BusinessPartner businessPartner) {
    OBCriteria<Location> obc = OBDal.getInstance().createCriteria(Location.class);
    obc.add(Restrictions.eq(Location.PROPERTY_BUSINESSPARTNER, businessPartner));
    obc.add(Restrictions.eq(Location.PROPERTY_ACTIVE, true));
    obc.setProjection(Projections.max(Location.PROPERTY_ID));
    obc.setMaxResults(1);
    return (String) obc.uniqueResult();
  }

}
