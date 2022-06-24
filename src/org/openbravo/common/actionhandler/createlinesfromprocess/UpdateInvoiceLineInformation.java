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
 * All portions are Copyright (C) 2018-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.actionhandler.createlinesfromprocess;

import java.math.BigDecimal;

import javax.enterprise.context.Dependent;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.assetmgmt.Asset;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.project.Project;

@Dependent
@Qualifier(CreateLinesFromProcessHook.CREATE_LINES_FROM_PROCESS_HOOK_QUALIFIER)
class UpdateInvoiceLineInformation extends CreateLinesFromProcessHook {
  @Override
  public int getOrder() {
    return -60;
  }

  /**
   * Updates the Information of the new Invoice Line that is related with the Invoice Header and the
   * copied order line.
   */
  @Override
  public void exec() {
    linksInvoiceLineToOrderAndInOutLine();
    setClientFromInvoiceHeader();
    setDescriptionFromCopiedLine();
    setAcctDimensionsToLine();
    updateBOMParent();
    updateInvoicePrepaymentAmount();
    setOrderReferenceInInvoiceHeaderIfLinkedOnlyToTheSameOrderOrBlankIt();
  }

  private void linksInvoiceLineToOrderAndInOutLine() {
    if (isCopiedFromOrderLine()) {
      getInvoiceLine().setSalesOrderLine((OrderLine) getCopiedFromLine());
      getInvoiceLine()
          .setGoodsShipmentLine(CreateLinesFromUtil.getShipmentInOutLine(getPickExecJSONObject()));
    } else {
      ShipmentInOutLine shipInOutLine = (ShipmentInOutLine) getCopiedFromLine();
      getInvoiceLine().setGoodsShipmentLine(shipInOutLine);
      getInvoiceLine().setSalesOrderLine(shipInOutLine.getSalesOrderLine());
    }
  }

  private void setClientFromInvoiceHeader() {
    getInvoiceLine().setClient(getInvoice().getClient());
  }

  private void setDescriptionFromCopiedLine() {
    if (isCopiedFromOrderLine()) {
      getInvoiceLine().setDescription(((OrderLine) getCopiedFromLine()).getDescription());
    } else {
      getInvoiceLine().setDescription(((ShipmentInOutLine) getCopiedFromLine()).getDescription());
    }
  }

  private void setAcctDimensionsToLine() {
    getInvoiceLine().setOrganization(getOrganizationForNewLine());
    getInvoiceLine().setProject((Project) getPropertyFromCopiedFromLineOrHeader("project"));
    getInvoiceLine()
        .setCostcenter((Costcenter) getPropertyFromCopiedFromLineOrHeader("costcenter"));
    getInvoiceLine().setAsset((Asset) getPropertyFromCopiedFromLineOrHeader("asset"));
    getInvoiceLine()
        .setStDimension((UserDimension1) getPropertyFromCopiedFromLineOrHeader("stDimension"));
    getInvoiceLine()
        .setNdDimension((UserDimension2) getPropertyFromCopiedFromLineOrHeader("ndDimension"));
  }

  private Organization getOrganizationForNewLine() {
    return (Organization) getCopiedFromLine().get("organization");
  }

  private BaseOBObject getPropertyFromCopiedFromLineOrHeader(final String propertyName) {
    final BaseOBObject copiedFromProperty = (BaseOBObject) getCopiedFromLine().get(propertyName);
    if (copiedFromProperty == null) {
      // Parent property
      return isCopiedFromOrderLine()
          ? (BaseOBObject) ((OrderLine) getCopiedFromLine()).getSalesOrder().get(propertyName)
          : (BaseOBObject) ((ShipmentInOutLine) getCopiedFromLine()).getShipmentReceipt()
              .get(propertyName);
    }
    return copiedFromProperty;
  }

  private void updateBOMParent() {
    getInvoiceLine().setBOMParent(getInvoiceLineBOMParent());
  }

  private InvoiceLine getInvoiceLineBOMParent() {
    if (!isCopiedFromOrderLine()
        && ((ShipmentInOutLine) getCopiedFromLine()).getBOMParent() == null) {
      return null;
    }

    OBCriteria<InvoiceLine> obc = OBDal.getInstance().createCriteria(InvoiceLine.class);
    obc.add(Restrictions.eq(InvoiceLine.PROPERTY_INVOICE, getInvoice()));
    if (isCopiedFromOrderLine()) {
      obc.add(Restrictions.eq(InvoiceLine.PROPERTY_SALESORDERLINE,
          ((OrderLine) getCopiedFromLine()).getBOMParent()));
    } else {
      obc.add(Restrictions.eq(InvoiceLine.PROPERTY_GOODSSHIPMENTLINE,
          ((ShipmentInOutLine) getCopiedFromLine()).getBOMParent()));
    }
    obc.setMaxResults(1);
    return (InvoiceLine) obc.uniqueResult();
  }

  private void updateInvoicePrepaymentAmount() {
    if ((isCopiedFromOrderLine()
        || CreateLinesFromUtil.hasRelatedOrderLine((ShipmentInOutLine) getCopiedFromLine()))
        && !thereAreInvoiceLinesLinkedToTheOrderCopiedFromLine()) {
      BigDecimal invoicePrepaymentAmt = getInvoice().getPrepaymentamt();
      getInvoice().setPrepaymentamt(invoicePrepaymentAmt.add(getOrderPrepaymentAmt()));
    }
  }

  private boolean thereAreInvoiceLinesLinkedToTheOrderCopiedFromLine() {
    Order order = isCopiedFromOrderLine() ? ((OrderLine) getCopiedFromLine()).getSalesOrder()
        : ((ShipmentInOutLine) getCopiedFromLine()).getShipmentReceipt().getSalesOrder();
    if (order != null) {
      //@formatter:off
      String hql =
              "as o" +
              " where o.id = :orderId" +
              "   and exists (" +
              "     select 1 from InvoiceLine as il" +
              "       join il.salesOrderLine as ol" +
              "      where ol.salesOrder.id = o.id)";
      //@formatter:on

      return OBDal.getInstance()
          .createQuery(Order.class, hql)
          .setNamedParameter("orderId", order.getId())
          .setMaxResult(1)
          .uniqueResult() != null;
    }
    return false;
  }

  private BigDecimal getOrderPrepaymentAmt() {
    try {
      final OBCriteria<FIN_PaymentSchedule> obc = OBDal.getInstance()
          .createCriteria(FIN_PaymentSchedule.class);
      obc.add(
          Restrictions.eq(FIN_PaymentSchedule.PROPERTY_ORDER + ".id", getRelatedOrder().getId()));
      obc.setMaxResults(1);
      return obc.list().get(0).getPaidAmount();
    } catch (Exception noOrderFoundOrNoPaymentScheduleFound) {
      return BigDecimal.ZERO;
    }
  }

  private void setOrderReferenceInInvoiceHeaderIfLinkedOnlyToTheSameOrderOrBlankIt() {
    Order processingOrder = getRelatedOrder();
    if (processingOrder != null) {
      boolean isMultiOrderInvoice = existsOtherOrdersLinkedToThisInvoice(processingOrder);
      getInvoice().setSalesOrder(isMultiOrderInvoice ? null : processingOrder);
    }
  }

  private Order getRelatedOrder() {
    Order processingOrder = null;
    if (isCopiedFromOrderLine()) {
      processingOrder = ((OrderLine) getCopiedFromLine()).getSalesOrder();
    } else if (((ShipmentInOutLine) getCopiedFromLine()).getSalesOrderLine() != null) {
      processingOrder = ((ShipmentInOutLine) getCopiedFromLine()).getSalesOrderLine()
          .getSalesOrder();
    }
    return processingOrder;
  }

  private boolean existsOtherOrdersLinkedToThisInvoice(Order processingOrder) {
    //@formatter:off
    String hql =
            "as il " +
            " where il.invoice.id = :invId" +
            "   and il.salesOrderLine.salesOrder.id <> :ordId";
    //@formatter:on

    return OBDal.getInstance()
        .createQuery(InvoiceLine.class, hql)
        .setNamedParameter("invId", getInvoice().getId())
        .setNamedParameter("ordId", processingOrder.getId())
        .setMaxResult(1)
        .uniqueResult() != null;
  }
}
