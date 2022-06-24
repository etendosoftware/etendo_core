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
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.test.base.OBBaseTest;

public class IsCompletelyInvoicedShipmentOneLineTest extends OBBaseTest {
  private static final Logger log = LogManager.getLogger();

  @Before
  public void setUpTestIsCompletelyInvoicedOneLine() throws Exception {
    ICIUtility.setTestContextSpain();
  }

  /**
   * Create a Sales Order, Goods Shipment and Sales Invoice
   */
  @Test
  public void testIsCompletelyInvoicedOneLine_01() {
    try {
      // Create Sales Order with one line
      Order order = ICIUtility.createNewSalesOrderWithDefaultParameters();
      OrderLine orderLine = ICIUtility.createNewSalesOrderLineWithDefaultParameters(order);

      ICIUtility.processOrder(order);

      // Create Shipment against the Sales Order
      ShipmentInOut shipment = ICIUtility.createNewShipmentWithDefaultParameters(order);
      ShipmentInOutLine shipmentLine = ICIUtility
          .createNewShipmentLineWithDefaultParameters(shipment);
      // Relate Shipment Line and Order Line
      shipmentLine.setSalesOrderLine(orderLine);
      OBDal.getInstance().flush();

      ICIUtility.processShipment(shipment);

      // Create Invoice against the order / shipment
      Invoice invoice = ICIUtility.createNewSalesInvoiceWithDefaultParameters();
      InvoiceLine invoiceLine = ICIUtility.createNewSalesInvoiceLineWithDefaultParameters(invoice);
      // Order is associated both to Invoice header and Invoice Lines
      invoiceLine.setSalesOrderLine(orderLine);
      invoice.setSalesOrder(order);
      // Shipment is associated in invoice line
      invoiceLine.setGoodsShipmentLine(shipmentLine);
      OBDal.getInstance().flush();

      ICIUtility.processInvoice(invoice);

      // Check isCompletlyInvoice flag is true
      shipment = ICIUtility.reloadAndRefreshShipment(shipment);
      assertEquals(true, shipment.isCompletelyInvoiced());
    } catch (Exception e) {
      log.error("Error in testIsCompletelyInvoicedOneLine_01", e);
    }
  }

  /**
   * Create a Sales Order, Goods Shipment and Sales Invoice, Reactivate Invoice
   */
  @Test
  public void testIsCompletelyInvoicedOneLine_02() {
    try {
      // Create a Sales Order with one line
      Order order = ICIUtility.createNewSalesOrderWithDefaultParameters();
      OrderLine orderLine = ICIUtility.createNewSalesOrderLineWithDefaultParameters(order);

      ICIUtility.processOrder(order);

      // Create Shipment against the order
      ShipmentInOut shipment = ICIUtility.createNewShipmentWithDefaultParameters(order);
      ShipmentInOutLine shipmentLine = ICIUtility
          .createNewShipmentLineWithDefaultParameters(shipment);
      // Relate Shipment Line and Order Line
      shipmentLine.setSalesOrderLine(orderLine);
      OBDal.getInstance().flush();

      ICIUtility.processShipment(shipment);

      Invoice invoice = ICIUtility.createNewSalesInvoiceWithDefaultParameters();
      InvoiceLine invoiceLine = ICIUtility.createNewSalesInvoiceLineWithDefaultParameters(invoice);
      // Order is associated both to Invoice header and Invoice lines
      invoiceLine.setSalesOrderLine(orderLine);
      invoice.setSalesOrder(order);
      // Associate Shipment line with Invoice line
      invoiceLine.setGoodsShipmentLine(shipmentLine);
      OBDal.getInstance().flush();

      ICIUtility.processInvoice(invoice);

      // Check isCompletlyInvoice flag is true
      shipment = ICIUtility.reloadAndRefreshShipment(shipment);
      assertEquals(true, shipment.isCompletelyInvoiced());

      // Reactivate Invoice
      ICIUtility.reactivateInvoice(invoice);
      OBDal.getInstance().flush();

      // Check isCompletlyInvoice flag is false
      shipment = ICIUtility.reloadAndRefreshShipment(shipment);
      assertEquals(false, shipment.isCompletelyInvoiced());
    } catch (Exception e) {
      log.error("Error in testIsCompletelyInvoicedOneLine_02", e);
    }
  }

  /**
   * Create Sales Order, Goods Shipment, Partial Sales Invoice
   */
  @Test
  public void testIsCompletelyInvoicedOneLine_03() {
    try {
      // Create Sales Order with one line
      Order order = ICIUtility.createNewSalesOrderWithDefaultParameters();
      OrderLine orderLine = ICIUtility.createNewSalesOrderLineWithDefaultParameters(order);

      ICIUtility.processOrder(order);

      // Create Shipment against Order
      ShipmentInOut shipment = ICIUtility.createNewShipmentWithDefaultParameters(order);
      ShipmentInOutLine shipmentLine = ICIUtility
          .createNewShipmentLineWithDefaultParameters(shipment);
      // Relate Shipment Line and Order Line
      shipmentLine.setSalesOrderLine(orderLine);
      OBDal.getInstance().flush();

      ICIUtility.processShipment(shipment);

      // Create Invoice with Partial Quantity
      Invoice invoice = ICIUtility.createNewSalesInvoiceWithDefaultParameters();

      ICIInvoiceLineParameters invoiceLineParameters = new ICIInvoiceLineParameters(invoice);
      // Subtract one unit to the invoiced quantity to have a partial invoice line
      invoiceLineParameters.setInvoicedQuantity(
          invoiceLineParameters.getInvoicedQuantity().subtract(BigDecimal.ONE));
      InvoiceLine invoiceLine = ICIUtility.createNewInvoiceLine(invoiceLineParameters);

      // Order is associated both to header and lines
      invoice.setSalesOrder(order);
      invoiceLine.setSalesOrderLine(orderLine);
      // Associate Shipment line with Invoice line
      invoiceLine.setGoodsShipmentLine(shipmentLine);
      OBDal.getInstance().flush();

      ICIUtility.processInvoice(invoice);

      // Check isCompletlyInvoice flag is false
      shipment = ICIUtility.reloadAndRefreshShipment(shipment);
      assertEquals(false, shipment.isCompletelyInvoiced());
    } catch (Exception e) {
      log.error("Error in testIsCompletelyInvoicedOneLine_03", e);
    }
  }
}
