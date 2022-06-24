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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.materialMgmt.iscompletelyinvoicedshipment;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.test.base.OBBaseTest;

/**
 * Create Sales Order with 2 Products, Create shipments against each order line, Create Sales
 * Invoice with links to both Shipment's line, both line fully invoiced
 */

public class IsCompletelyInvoicedShipmentMultiLineTest extends OBBaseTest {

  private static final Logger log = LogManager.getLogger();

  @Before
  public void setUpTestIsCompletelyInvoicedMultiLine() throws Exception {
    ICIUtility.setTestContextSpain();
  }

  @Test
  public void testIsCompletelyInvoicedMultiLine_01() {
    try {
      Order order = ICIUtility.createNewSalesOrderWithDefaultParameters();
      OrderLine orderline = ICIUtility.createNewSalesOrderLineWithDefaultParameters(order);
      Product secondProduct = OBDal.getInstance().get(Product.class, ICIConstants.PRODUCT_2_ID);
      OrderLine orderLine2 = createSecondOrderLine(order, secondProduct);

      ICIUtility.processOrder(order);

      // Create first Shipment against first order line
      ShipmentInOut shipment = ICIUtility.createNewShipmentWithDefaultParameters(order);
      ShipmentInOutLine shipmentLine = ICIUtility
          .createNewShipmentLineWithDefaultParameters(shipment);
      // Relate Shipment Line with Order Line
      shipmentLine.setSalesOrderLine(orderline);
      OBDal.getInstance().flush();

      // Process Shipment 1
      ICIUtility.processShipment(shipment);

      // Create second Shipment against second order line
      ShipmentInOut shipment2 = ICIUtility.createNewShipmentWithDefaultParameters(order);
      ICIShipmentLineParameters shipmentLine2Parameters = new ICIShipmentLineParameters(shipment2);
      shipmentLine2Parameters.setProduct(secondProduct);
      ShipmentInOutLine shipmentLine2 = ICIUtility.createShipmentLine(shipmentLine2Parameters);
      // Fully Relate second Shipment Line with second Order Line
      shipmentLine2.setSalesOrderLine(orderLine2);
      shipmentLine2.setMovementQuantity(new BigDecimal("20"));
      OBDal.getInstance().flush();

      // Process second Shipment
      ICIUtility.processShipment(shipment2);

      // Create Sales Invoice including both Shipment's Line
      Invoice invoice = ICIUtility.createNewSalesInvoiceWithDefaultParameters();

      InvoiceLine invoiceLine = ICIUtility.createNewSalesInvoiceLineWithDefaultParameters(invoice);
      // Order is associated both to header and lines
      invoice.setSalesOrder(order);
      invoiceLine.setSalesOrderLine(orderline);
      // Invoice Line is related with Shipment Line
      invoiceLine.setGoodsShipmentLine(shipmentLine);

      InvoiceLine invoiceLine2 = createSecondInvoiceLine(secondProduct, invoice);
      // second Order and second Shipment are related to second Invoice Line
      invoiceLine2.setSalesOrderLine(orderLine2);
      invoiceLine2.setGoodsShipmentLine(shipmentLine2);
      OBDal.getInstance().flush();

      // Process Invoice
      ICIUtility.processInvoice(invoice);

      // Check isCompletlyInvoice flag is true for first shipment
      shipment = ICIUtility.reloadAndRefreshShipment(shipment);
      assertEquals(true, shipment.isCompletelyInvoiced());

      // Check isCompletlyInvoice flag is true for second shipment
      shipment2 = ICIUtility.reloadAndRefreshShipment(shipment2);
      assertEquals(true, shipment2.isCompletelyInvoiced());
    } catch (Exception e) {
      log.error("Error in testIsCompletelyInvoicedMultiLine_01", e);
    }
  }

  /**
   * Create Sales Order with 2 Products, Create shipments against each order line, Create Sales
   * Invoice with links to both Shipment's line, one line fully invoiced, second line partially
   * invoiced
   */

  @Test
  public void testIsCompletelyInvoicedMultiLine_02() {
    try {
      Order order = ICIUtility.createNewSalesOrderWithDefaultParameters();
      OrderLine orderLine = ICIUtility.createNewSalesOrderLineWithDefaultParameters(order);
      Product secondProduct = OBDal.getInstance().get(Product.class, ICIConstants.PRODUCT_2_ID);
      OrderLine orderLine2 = createSecondOrderLine(order, secondProduct);

      ICIUtility.processOrder(order);

      // Create first Shipment against first order line
      ShipmentInOut shipment = ICIUtility.createNewShipmentWithDefaultParameters(order);
      ShipmentInOutLine shipmentLine = ICIUtility
          .createNewShipmentLineWithDefaultParameters(shipment);
      // Relate Goods Shipment Line with Sales Order Line
      shipmentLine.setSalesOrderLine(orderLine);
      OBDal.getInstance().flush();

      ICIUtility.processShipment(shipment);

      // Create second Shipment against second order line
      ShipmentInOut shipment2 = ICIUtility.createNewShipmentWithDefaultParameters(order);
      ICIShipmentLineParameters shipmentLine2Parameters = new ICIShipmentLineParameters(shipment2);
      shipmentLine2Parameters.setProduct(secondProduct);
      ShipmentInOutLine shipmentLine2 = ICIUtility.createShipmentLine(shipmentLine2Parameters);
      // Associate second Shipment Line with second Sales Order Line
      shipmentLine2.setSalesOrderLine(orderLine2);
      shipmentLine2.setMovementQuantity(new BigDecimal("20"));
      OBDal.getInstance().flush();

      ICIUtility.processShipment(shipment2);

      // Create Sales Invoice including both Shipment's Line
      Invoice invoice = ICIUtility.createNewSalesInvoiceWithDefaultParameters();

      InvoiceLine invoiceLine = ICIUtility.createNewSalesInvoiceLineWithDefaultParameters(invoice);
      // Order is associated both to header and lines
      invoice.setSalesOrder(order);
      invoiceLine.setSalesOrderLine(orderLine);
      // Invoice Line is related with Shipment Line
      invoiceLine.setGoodsShipmentLine(shipmentLine);

      InvoiceLine invoiceLine2 = createSecondInvoiceLine(secondProduct, invoice);
      // second Order and second Shipment are related to second Invoice Line
      invoiceLine2.setSalesOrderLine(orderLine2);
      invoiceLine2.setGoodsShipmentLine(shipmentLine2);
      // Remove some Invoice Quantity to make it a partial invoice
      invoiceLine2.setInvoicedQuantity(invoiceLine2.getInvoicedQuantity().subtract(BigDecimal.ONE));
      OBDal.getInstance().flush();

      ICIUtility.processInvoice(invoice);

      // Check isCompletlyInvoice flag is true for first shipment
      shipment = ICIUtility.reloadAndRefreshShipment(shipment);
      assertEquals(true, shipment.isCompletelyInvoiced());

      // Check isCompletlyInvoice flag is false for second shipment
      shipment2 = ICIUtility.reloadAndRefreshShipment(shipment2);
      assertEquals(false, shipment2.isCompletelyInvoiced());
    } catch (Exception e) {
      log.error("Error in testIsCompletelyInvoicedMultiLine_02", e);
    }
  }

  private OrderLine createSecondOrderLine(Order order, Product secondProduct) {
    ICIOrderLineParameters orderLine2Parameters = new ICIOrderLineParameters(order);
    orderLine2Parameters.setProduct(secondProduct);
    orderLine2Parameters.setUom(secondProduct.getUOM());
    orderLine2Parameters.setOrderedQuantity(new BigDecimal("20"));
    orderLine2Parameters.setNetUnitPrice(new BigDecimal("20"));
    orderLine2Parameters.setNetListPrice(new BigDecimal("20"));
    orderLine2Parameters.setPriceLimit(new BigDecimal("20"));
    orderLine2Parameters.setLineNetAmount(new BigDecimal("200"));
    orderLine2Parameters.setLineNo(20L);
    OrderLine orderLine2 = ICIUtility.createNewOrderLine(orderLine2Parameters);
    return orderLine2;
  }

  private InvoiceLine createSecondInvoiceLine(Product secondProduct, Invoice invoice) {
    ICIInvoiceLineParameters invoiceLine2Parameters = new ICIInvoiceLineParameters(invoice);
    invoiceLine2Parameters.setLineNo(20L);
    invoiceLine2Parameters.setProduct(secondProduct);
    invoiceLine2Parameters.setUom(secondProduct.getUOM());
    invoiceLine2Parameters.setInvoicedQuantity(new BigDecimal("20"));
    invoiceLine2Parameters.setNetUnitPrice(new BigDecimal("20"));
    invoiceLine2Parameters.setNetListPrice(new BigDecimal("20"));
    invoiceLine2Parameters.setPriceLimit(new BigDecimal("20"));
    invoiceLine2Parameters.setLineNetAmount(new BigDecimal("20"));
    InvoiceLine invoiceLine2 = ICIUtility.createNewInvoiceLine(invoiceLine2Parameters);
    return invoiceLine2;
  }
}
