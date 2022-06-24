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
 * All portions are Copyright (C) 2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.createlinesfrom.data;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

/**
 * Data to check Create Lines From process
 * 
 * @author Andy Armaignac
 *
 */
public abstract class CreateLinesFromTestData {
  protected List<OrderLineData> orderLineData;
  protected List<InvoiceLineData> invoiceLineData;
  protected List<ShipmentInOutLineData> shipmentInOutLineData;

  public List<InvoiceLineData> getInvoiceLineData() {
    return invoiceLineData;
  }

  public abstract String getTestNumber();

  public abstract String getTestDescription();

  public abstract Boolean isSales();

  public Order createOrder() {
    Order order = OBDal.getInstance()
        .get(Order.class,
            isSales() ? CLFTestDataConstants.SALESORDER_ID : CLFTestDataConstants.PURCHASEORDER_ID);
    Order clonedOrder = (Order) DalUtil.copy(order, false);
    updateOrderHeader(clonedOrder);
    OBDal.getInstance().save(clonedOrder);

    OrderLine orderLine = order.getOrderLineList().get(0);

    for (int i = 0; i < orderLineData.size(); i++) {
      OrderLineData lineData = orderLineData.get(i);
      OrderLine clonedOrderLine = (OrderLine) DalUtil.copy(orderLine, false);

      clonedOrderLine.setLineNo((long) ((i + 1) * 10));
      updateOrderLine(clonedOrderLine, lineData);

      clonedOrderLine.setSalesOrder(clonedOrder);
      clonedOrder.getOrderLineList().add(clonedOrderLine);
      OBDal.getInstance().save(clonedOrderLine);
    }

    OBDal.getInstance().save(clonedOrder);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(clonedOrder);

    return clonedOrder;
  }

  public Invoice createInvoiceHeader() {
    Invoice invoice = OBDal.getInstance()
        .get(Invoice.class, isSales() ? CLFTestDataConstants.SALESINVOICE_ID
            : CLFTestDataConstants.PURCHASEINVOICE_ID);
    Invoice clonedInvoice = (Invoice) DalUtil.copy(invoice, false);
    String documentNo = FIN_Utility.getDocumentNo(clonedInvoice.getOrganization(), "ARI",
        Invoice.TABLE_NAME);
    clonedInvoice.setDocumentNo(documentNo);
    clonedInvoice.setDescription(getTestDescription());
    clonedInvoice.setSummedLineAmount(BigDecimal.ZERO);
    clonedInvoice.setGrandTotalAmount(BigDecimal.ZERO);
    Date date = new Date();
    clonedInvoice.setInvoiceDate(date);
    clonedInvoice.setAccountingDate(date);

    OBDal.getInstance().save(clonedInvoice);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(clonedInvoice);

    return clonedInvoice;
  }

  public void assertDraftOrder(Order order) {

    assertThat(getTestNumber() + ". Wrong Order document status = " + order.getDocumentStatus()
        + ". Was expected 'DR'", order.getDocumentStatus(), comparesEqualTo("DR"));

    for (int i = 0; i < orderLineData.size(); i++) {
      OrderLine orderLine = order.getOrderLineList().get(i);
      OrderLineData lineData = orderLineData.get(i);
      assertDraftOrderLines(orderLine, lineData);
    }
  }

  protected void assertDraftOrderLines(OrderLine orderLine, OrderLineData lineData) {
    assertThat(
        getTestNumber() + ". Wrong Order Line ordered quantity = " + orderLine.getOrderedQuantity()
            + ". Was expected " + lineData.getOrderedQuantity(),
        orderLine.getOrderedQuantity(), comparesEqualTo(lineData.getOrderedQuantity()));
  }

  public void assertCompletedOrder(Order order) {
    assertThat(getTestNumber() + ". Wrong Order document status = " + order.getDocumentStatus()
        + ". Was expected 'CO'", order.getDocumentStatus(), comparesEqualTo("CO"));

    for (int i = 0; i < orderLineData.size(); i++) {
      OrderLine orderLine = order.getOrderLineList().get(i);
      OrderLineData lineData = orderLineData.get(i);
      assertCompletedOrderLines(orderLine, lineData);
    }
  }

  protected void assertCompletedOrderLines(OrderLine orderLine, OrderLineData lineData) {
    assertThat(
        getTestNumber() + ". Wrong Ordered Quantity = " + orderLine.getOrderedQuantity()
            + ". Was expected " + lineData.getOrderedQuantity(),
        orderLine.getOrderedQuantity(), comparesEqualTo(lineData.getOrderedQuantity()));
  }

  public void assertDraftInvoice(Invoice invoice) {
    assertThat(getTestNumber() + ". Wrong Order document status = " + invoice.getDocumentStatus()
        + ". Was expected 'DR'", invoice.getDocumentStatus(), comparesEqualTo("DR"));

    for (int i = 0; i < invoiceLineData.size(); i++) {
      InvoiceLine invoiceLine = invoice.getInvoiceLineList().get(i);
      InvoiceLineData lineData = invoiceLineData.get(i);
      assertDraftInvoiceLines(invoiceLine, lineData);
    }
  }

  protected void assertDraftInvoiceLines(InvoiceLine invoiceLine, InvoiceLineData lineData) {
    assertThat(getTestNumber() + ". Wrong Invoice Line invoiced quantity = "
        + invoiceLine.getInvoicedQuantity() + ". Was expected " + lineData.getInvoicedQuantity(),
        invoiceLine.getInvoicedQuantity(), comparesEqualTo(lineData.getInvoicedQuantity()));

    String orderLineIdentifier = invoiceLine.getSalesOrderLine() != null
        ? invoiceLine.getSalesOrderLine().getIdentifier()
        : "";
    assertThat(
        getTestNumber() + ". Wrong Invoice Line order line identifier = " + orderLineIdentifier
            + ". Was expected " + lineData.getOrderLineIdentifier(),
        orderLineIdentifier, comparesEqualTo(lineData.getOrderLineIdentifier()));
  }

  public void assertCompletedInvoice(Invoice invoice) {
    assertThat(getTestNumber() + ". Wrong Invoice document status = " + invoice.getDocumentStatus()
        + ". Was expected 'CO'", invoice.getDocumentStatus(), comparesEqualTo("CO"));

    for (int i = 0; i < invoiceLineData.size(); i++) {
      InvoiceLine orderLine = invoice.getInvoiceLineList().get(i);
      InvoiceLineData lineData = invoiceLineData.get(i);
      assertCompletedInvoiceLines(orderLine, lineData);
    }
  }

  protected void assertCompletedInvoiceLines(InvoiceLine invoiceLine, InvoiceLineData lineData) {
    assertThat(getTestNumber() + ". Wrong Invoice Line invoiced quantity = "
        + invoiceLine.getInvoicedQuantity() + ". Was expected " + lineData.getInvoicedQuantity(),
        invoiceLine.getInvoicedQuantity(), comparesEqualTo(lineData.getInvoicedQuantity()));

    // Check orderline identifier
    String orderLineIdentifier = invoiceLine.getSalesOrderLine() != null
        ? invoiceLine.getSalesOrderLine().getIdentifier()
        : "";
    assertThat(
        getTestNumber() + ". Wrong Invoice Line order line identifier = " + orderLineIdentifier
            + ". Was expected " + lineData.getOrderLineIdentifier(),
        orderLineIdentifier, comparesEqualTo(lineData.getOrderLineIdentifier()));

    // Check shipment/receipt identifier
    String shipmentInOutLineIdentifier = invoiceLine.getGoodsShipmentLine() != null
        ? invoiceLine.getGoodsShipmentLine().getIdentifier()
        : "";
    assertThat(
        getTestNumber() + ". Wrong Invoice Line shipment/receipt line identifier = "
            + shipmentInOutLineIdentifier + ". Was expected "
            + lineData.getShipmentInOutLineIdentifier(),
        shipmentInOutLineIdentifier, comparesEqualTo(lineData.getShipmentInOutLineIdentifier()));
  }

  protected void updateOrderHeader(Order order) {
    String documentNo = FIN_Utility.getDocumentNo(order.getOrganization(),
        order.getDocumentType().getDocumentCategory(), Order.TABLE_NAME);
    order.setDocumentNo(documentNo);
    order.setSummedLineAmount(BigDecimal.ZERO);
    order.setGrandTotalAmount(BigDecimal.ZERO);
  }

  protected void updateOrderLine(OrderLine orderLine, OrderLineData lineData) {
    Product product = OBDal.getInstance().get(Product.class, lineData.getProductId());

    orderLine.setProduct(product);
    orderLine.setUOM(product.getUOM());
    orderLine.setOrderedQuantity(lineData.getOrderedQuantity());
  }

  public ShipmentInOut createShipmentInOut() {
    ShipmentInOut shipmentInOut = OBDal.getInstance()
        .get(ShipmentInOut.class, isSales() ? CLFTestDataConstants.GOODS_SHIPMENT_ID
            : CLFTestDataConstants.GOODS_RECEIPT_ID);
    ShipmentInOut clonedShipmentInOut = (ShipmentInOut) DalUtil.copy(shipmentInOut, false);
    updateShipmentHeader(clonedShipmentInOut);
    OBDal.getInstance().save(clonedShipmentInOut);

    ShipmentInOutLine shipmentInOutLine = shipmentInOut.getMaterialMgmtShipmentInOutLineList()
        .get(0);

    for (int i = 0; i < shipmentInOutLineData.size(); i++) {
      ShipmentInOutLineData lineData = shipmentInOutLineData.get(i);
      ShipmentInOutLine clonedShipmentInOutLine = (ShipmentInOutLine) DalUtil
          .copy(shipmentInOutLine, false);

      clonedShipmentInOutLine.setLineNo((long) ((i + 1) * 10));
      updateShipmentLine(clonedShipmentInOutLine, lineData);

      clonedShipmentInOutLine.setShipmentReceipt(clonedShipmentInOut);
      clonedShipmentInOut.getMaterialMgmtShipmentInOutLineList().add(clonedShipmentInOutLine);
      OBDal.getInstance().save(clonedShipmentInOutLine);
    }

    OBDal.getInstance().save(clonedShipmentInOut);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(clonedShipmentInOut);

    return clonedShipmentInOut;
  }

  private void updateShipmentHeader(ShipmentInOut shipmentInOut) {
    String documentNo = FIN_Utility.getDocumentNo(shipmentInOut.getOrganization(), "MMS",
        ShipmentInOut.TABLE_NAME);
    shipmentInOut.setDocumentNo(documentNo);

    Date shipmentDate = new Date();
    shipmentInOut.setAccountingDate(shipmentDate);
    shipmentInOut.setMovementDate(shipmentDate);
  }

  protected void updateShipmentLine(ShipmentInOutLine shipmentInOutLine,
      ShipmentInOutLineData lineData) {

    Product product = OBDal.getInstance().get(Product.class, lineData.getProductId());

    shipmentInOutLine.setProduct(product);
    shipmentInOutLine.setUOM(product.getUOM());
    shipmentInOutLine.setMovementQuantity(lineData.getMovementQuantity());

    AttributeSetInstance instance = OBDal.getInstance()
        .get(AttributeSetInstance.class, CLFTestDataConstants.RMA_ATTRIBUTESET);
    shipmentInOutLine.setAttributeSetValue(instance);
  }

  public void assertDraftShipmentInOut(ShipmentInOut shipmentInOut) {
    assertThat(
        getTestNumber() + ". Wrong ShipmentInOut document status = "
            + shipmentInOut.getDocumentStatus() + ". Was expected 'DR'",
        shipmentInOut.getDocumentStatus(), comparesEqualTo("DR"));

    for (int i = 0; i < shipmentInOutLineData.size(); i++) {
      ShipmentInOutLine shipmentLine = shipmentInOut.getMaterialMgmtShipmentInOutLineList().get(i);
      ShipmentInOutLineData lineData = shipmentInOutLineData.get(i);
      assertDraftShipmentLines(shipmentLine, lineData);
    }
  }

  protected void assertDraftShipmentLines(ShipmentInOutLine shipmentInOutLine,
      ShipmentInOutLineData lineData) {
    assertThat(
        getTestNumber() + ". Wrong ShipmentInOut Line Movement Quantity = "
            + shipmentInOutLine.getMovementQuantity() + ". Was expected "
            + lineData.getMovementQuantity(),
        shipmentInOutLine.getMovementQuantity(), comparesEqualTo(lineData.getMovementQuantity()));
  }

  public void assertCompletedShipmentInOut(ShipmentInOut shipmentInOut) {
    assertThat(
        getTestNumber() + ". Wrong ShipmentInOut document status = "
            + shipmentInOut.getDocumentStatus() + ". Was expected 'CO'",
        shipmentInOut.getDocumentStatus(), comparesEqualTo("CO"));

    for (int i = 0; i < shipmentInOutLineData.size(); i++) {
      ShipmentInOutLine shipmentLine = shipmentInOut.getMaterialMgmtShipmentInOutLineList().get(i);
      ShipmentInOutLineData lineData = shipmentInOutLineData.get(i);
      assertCompletedShipmentInOutLines(shipmentLine, lineData);
    }

  }

  protected void assertCompletedShipmentInOutLines(ShipmentInOutLine shipmentInOutLine,
      ShipmentInOutLineData lineData) {
    assertThat(
        getTestNumber() + ". Wrong ShipmentInOut Line Movement Quantity = "
            + shipmentInOutLine.getMovementQuantity() + ". Was expected "
            + lineData.getMovementQuantity(),
        shipmentInOutLine.getMovementQuantity(), comparesEqualTo(lineData.getMovementQuantity()));
  }
}
