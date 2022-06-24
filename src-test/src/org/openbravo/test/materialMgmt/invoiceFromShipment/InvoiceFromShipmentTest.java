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

package org.openbravo.test.materialMgmt.invoiceFromShipment;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.query.Query;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.common.actionhandler.createlinesfromprocess.CreateInvoiceLinesFromProcess;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.materialmgmt.InvoiceGeneratorFromGoodsShipment;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Test class for Automatic Invoice From Goods Shipment test cases
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class InvoiceFromShipmentTest extends WeldBaseTest {

  private static final Logger log = LogManager.getLogger();

  private static final String CLIENT_ID = "4028E6C72959682B01295A070852010D"; // QA Testing
  private static final String ORG_ID = "357947E87C284935AD1D783CF6F099A1"; // Spain
  private static final String USER_ID = "100"; // Openbravo
  private static final String ROLE_ID = "4028E6C72959682B01295A071429011E"; // QA Testing Admin

  private static final String GOODS_SHIPMENT_ID = "8BEAC8CAFFCE444FA15D0170F897B641";
  private static final String SALES_ORDER = "5B29AF263D004CD3830D4F9B23C17DFD";
  private static final String SALES_INVOICE = "B859F1D34CD348998F4D54B571010C8F";
  private static final String T_SHIRTS_PRODUCT_ID = "0CF7C882B8BD4D249F3BCC8727A736D1";
  private static final String DO_NOT_INVOICE = "N";
  private static final String IMMEDIATE = "I";
  private static final String AFTER_DELIVERY = "D";
  private static final String AFTER_ORDER_DELIVERY = "O";
  private static final String PRICE_LIST_SALES_ID = "4028E6C72959682B01295ADC1D55022B";

  @Before
  public void initialize() {
    log.info("Initializing Invoice From Shipment Tests ...");
    OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORG_ID);
    final OBContext obContext = OBContext.getOBContext();
    final VariablesSecureApp vars = new VariablesSecureApp(obContext.getUser().getId(),
        obContext.getCurrentClient().getId(), obContext.getCurrentOrganization().getId(),
        obContext.getRole().getId(), obContext.getLanguage().getLanguage());
    RequestContext.get().setVariableSecureApp(vars);
  }

  /**
   * Generating invoice from a Goods Shipment linked to a Sales Order with invoice term "After
   * Delivery"
   * <ol>
   * <li>Create a Sales Order with invoice term "After Delivery".</li>
   * <li>Adds two lines of product. Book the order.</li>
   * <li>Create a Goods Shipment.</li>
   * <li>Adds just one line from the previous order.</li>
   * <li>Complete the document invoicing if possible.</li>
   * <li>Verify an invoice was created and processed containing the line from Goods Shipment.</li>
   * </ol>
   */
  @Test
  public void invoiceFromShipment_001() {
    OBContext.setAdminMode();
    try {

      final Product product = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID,
          "InvoiceFromShipment_001");
      final Order salesOrder = createSalesOrderWithInvoiceTerm(SALES_ORDER,
          "InvoiceFromShipment_001", AFTER_DELIVERY);
      final OrderLine orderLine = getOrderLineByLineNo(salesOrder, 10L);
      setProductInOrderLine(orderLine, product);
      TestUtils.processOrder(salesOrder);

      final ShipmentInOut shipment = TestUtils.cloneReceiptShipment(GOODS_SHIPMENT_ID,
          "InvoiceFromShipment_001");
      final ShipmentInOutLine shipmentLine = getShipmentLineByLineNo(shipment, 10L);
      setProductInShipmentLine(shipmentLine, product);
      setOrderLineInShipmentLine(shipmentLine, orderLine);
      TestUtils.processShipmentReceipt(shipment);

      final Invoice invoice = new InvoiceGeneratorFromGoodsShipment(shipment.getId())
          .createInvoiceConsideringInvoiceTerms(true);
      assertGeneratedInvoiceLine(invoice, product, new BigDecimal(12));
      assertInvoiceDatePriceListAndStatus(invoice, shipment.getMovementDate(),
          salesOrder.getPriceList(), "CO");

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Generating invoice from a Goods Shipment linked to a Sales Order with invoice term "After Order
   * Delivery"
   * <ol>
   * <li>Create a Sales Order with invoice term "After Order Delivery".</li>
   * <li>Adds two lines of product. Book the order.</li>
   * <li>Create a Goods Shipment.</li>
   * <li>Adds the first line from the previous order.</li>
   * <li>Complete the document invoicing if possible.</li>
   * <li>Verify no invoice was created.</li>
   * <li>Create a Goods Shipment.</li>
   * <li>Adds the second line from the previous order.</li>
   * <li>Complete the document invoicing if possible.</li>
   * <li>Verify an invoice was created and processed containing the two lines from Sales Order.</li>
   * </ol>
   */

  @Test
  public void invoiceFromShipment_002() {
    OBContext.setAdminMode();
    try {

      final Product productOne = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID,
          "InvoiceFromShipment_002");
      final Product productTwo = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID,
          "InvoiceFromShipment_002");

      final Order salesOrder = createAfterOrderDeliveredOrder(SALES_ORDER,
          "InvoiceFromShipment_002");
      final OrderLine firstLine = getOrderLineByLineNo(salesOrder, 10L);
      setProductInOrderLine(firstLine, productOne);
      final OrderLine secondLine = getOrderLineByLineNo(salesOrder, 20L);
      setProductInOrderLine(secondLine, productTwo);
      TestUtils.processOrder(salesOrder);

      final ShipmentInOut shipment = TestUtils.cloneReceiptShipment(GOODS_SHIPMENT_ID,
          "InvoiceFromShipment_002");
      final ShipmentInOutLine shipmentLine = getShipmentLineByLineNo(shipment, 10L);
      setProductInShipmentLine(shipmentLine, productOne);
      setOrderLineInShipmentLine(shipmentLine, firstLine);
      TestUtils.processShipmentReceipt(shipment);
      OBDal.getInstance().refresh(salesOrder); // Necessary to update the delivery status

      final Invoice invoice = new InvoiceGeneratorFromGoodsShipment(shipment.getId())
          .createInvoiceConsideringInvoiceTerms(true);
      assertNoInvoiceWasGenerated(invoice);

      final ShipmentInOut secondShipment = TestUtils.cloneReceiptShipment(GOODS_SHIPMENT_ID,
          "InvoiceFromShipment_002");
      final ShipmentInOutLine secondShipmentLine = getShipmentLineByLineNo(secondShipment, 10L);
      setProductInShipmentLine(secondShipmentLine, productTwo);
      setOrderLineInShipmentLine(secondShipmentLine, secondLine);
      TestUtils.processShipmentReceipt(secondShipment);
      OBDal.getInstance().getSession().clear(); // Necessary to force DAL to refresh objects status

      final Invoice secondInvoice = new InvoiceGeneratorFromGoodsShipment(secondShipment.getId())
          .createInvoiceConsideringInvoiceTerms(true);
      assertGeneratedInvoiceLine(secondInvoice, productOne, new BigDecimal(12));
      assertGeneratedInvoiceLine(secondInvoice, productTwo, new BigDecimal(12));
      assertInvoiceDatePriceListAndStatus(secondInvoice, secondShipment.getMovementDate(),
          salesOrder.getPriceList(), "CO");
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Generating invoice from Goods Shipment not linked to a Sales Order
   * <ol>
   * <li>Create a Goods Shipment.</li>
   * <li>Adds two lines of product.</li>
   * <li>Complete the document invoicing if possible.</li>
   * <li>Verify an invoice was created and processed containing the lines from Goods Shipment.</li>
   * </ol>
   */

  @Test
  public void invoiceFromShipment_003() {
    OBContext.setAdminMode();
    try {

      final Product productOne = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID,
          "InvoiceFromShipment_003");
      final Product productTwo = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID,
          "InvoiceFromShipment_003");

      final ShipmentInOut shipment = createShipmentReceiptWithTwoLines(GOODS_SHIPMENT_ID,
          "InvoiceFromShipment_003");

      final ShipmentInOutLine firstShipmentLine = getShipmentLineByLineNo(shipment, 10L);
      setProductInShipmentLine(firstShipmentLine, productOne);
      ShipmentInOutLine secondShipmentLine = getShipmentLineByLineNo(shipment, 20L);
      setProductInShipmentLine(secondShipmentLine, productTwo);

      TestUtils.processShipmentReceipt(shipment);
      final PriceList priceList = shipment.getBusinessPartner().getPriceList();

      final Invoice invoice = new InvoiceGeneratorFromGoodsShipment(shipment.getId())
          .createInvoiceConsideringInvoiceTerms(true);
      assertGeneratedInvoiceLine(invoice, productOne, new BigDecimal(12));
      assertGeneratedInvoiceLine(invoice, productTwo, new BigDecimal(12));
      assertInvoiceDatePriceListAndStatus(invoice, shipment.getMovementDate(), priceList, "CO");
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Generating invoice from Goods Shipment with lines linked to Sales Order with invoice term
   * “After Delivery” and “After Order Delivery”.
   * <ol>
   * <li>Create a Sales Order with invoice term “After Delivery”.</li>
   * <li>Add one line of product. Book the order.</li>
   * <li>Create a second Sales Order with invoice term “After Order Delivery”.</li>
   * <li>Add two lines of product. Book the order.</li>
   * <li>Create a Goods Shipment</li>
   * <li>Add the line from the first order</li>
   * <li>Add the first line from the second order</li>
   * <li>Add a line manually</li>
   * <li>Complete the document invoicing if possible</li>
   * <li>Verify an invoice was created with the line from the first order and the one added
   * manually.</li>
   * <li>Create a second Goods Shipment</li>
   * <li>Add the second line from the second order</li>
   * <li>Complete the document invoicing if possible</li>
   * <li>Verify an invoice was created containing both lines from the second order.</li>
   * </ol>
   */

  @Test
  public void invoiceFromShipment_004() {
    OBContext.setAdminMode();
    try {

      final Product productOne = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID,
          "InvoiceFromShipment_004");
      final Product productTwo = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID,
          "InvoiceFromShipment_004");
      final Product productThree = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID,
          "InvoiceFromShipment_004");
      final Product productFour = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID,
          "InvoiceFromShipment_004");

      final Order firstSalesOrder = createSalesOrderWithInvoiceTerm(SALES_ORDER,
          "InvoiceFromShipment_004", AFTER_DELIVERY);
      final OrderLine firstLine = getOrderLineByLineNo(firstSalesOrder, 10L);
      setProductInOrderLine(firstLine, productOne);
      TestUtils.processOrder(firstSalesOrder);

      final Order secondSalesOrder = createAfterOrderDeliveredOrder(SALES_ORDER,
          "InvoiceFromShipment_004");
      final OrderLine secondLine = getOrderLineByLineNo(secondSalesOrder, 10L);
      setProductInOrderLine(secondLine, productTwo);
      final OrderLine thirdLine = getOrderLineByLineNo(secondSalesOrder, 20L);
      setProductInOrderLine(thirdLine, productThree);
      TestUtils.processOrder(secondSalesOrder);

      final ShipmentInOut shipment = createShipmentReceiptWithTwoLines(GOODS_SHIPMENT_ID,
          "InvoiceFromShipment_004");
      final ShipmentInOutLine firstShipmentLine = getShipmentLineByLineNo(shipment, 10L);
      setProductInShipmentLine(firstShipmentLine, productOne);
      setOrderLineInShipmentLine(firstShipmentLine, firstLine);
      final ShipmentInOutLine secondShipmentLine = getShipmentLineByLineNo(shipment, 20L);
      setProductInShipmentLine(secondShipmentLine, productTwo);
      setOrderLineInShipmentLine(secondShipmentLine, secondLine);
      final ShipmentInOutLine thirdShipmentLine = addLineToShipment(shipment);
      setProductInShipmentLine(thirdShipmentLine, productFour);
      setOrderLineInShipmentLine(thirdShipmentLine, null);

      TestUtils.processShipmentReceipt(shipment);
      OBDal.getInstance().refresh(firstSalesOrder); // Necessary to update the delivery status
      OBDal.getInstance().refresh(secondSalesOrder); // Necessary to update the delivery status

      final Invoice invoice = new InvoiceGeneratorFromGoodsShipment(shipment.getId())
          .createInvoiceConsideringInvoiceTerms(true);
      assertInvoiceLineNumber(invoice, 2);
      assertGeneratedInvoiceLine(invoice, productOne, new BigDecimal(12));
      assertGeneratedInvoiceLine(invoice, productFour, new BigDecimal(12));
      assertInvoiceDatePriceListAndStatus(invoice, shipment.getMovementDate(),
          firstSalesOrder.getPriceList(), "CO");

      final ShipmentInOut secondShipment = TestUtils.cloneReceiptShipment(GOODS_SHIPMENT_ID,
          "InvoiceFromShipment_004");
      final ShipmentInOutLine fourthShipmentLine = getShipmentLineByLineNo(secondShipment, 10L);
      setProductInShipmentLine(fourthShipmentLine, productThree);
      setOrderLineInShipmentLine(fourthShipmentLine, thirdLine);

      TestUtils.processShipmentReceipt(secondShipment);
      OBDal.getInstance().getSession().clear(); // Necessary to force DAL to refresh objects status

      final Invoice secondInvoice = new InvoiceGeneratorFromGoodsShipment(secondShipment.getId())
          .createInvoiceConsideringInvoiceTerms(true);
      assertInvoiceLineNumber(secondInvoice, 2);
      assertGeneratedInvoiceLine(secondInvoice, productTwo, new BigDecimal(12));
      assertGeneratedInvoiceLine(secondInvoice, productThree, new BigDecimal(12));
      assertInvoiceDatePriceListAndStatus(invoice, shipment.getMovementDate(),
          firstSalesOrder.getPriceList(), "CO");
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Generating invoice from Goods Shipment with incomplete delivery
   * <ol>
   * <li>Create a Sales Order win invoice term “After Delivery”.</li>
   * <li>Adds one line of product. Book the order.</li>
   * <li>Create a second Sales Order with invoice term “After Order Delivery”.</li>
   * <li>Adds one line of product. Book the order.</li>
   * <li>Create a Goods Shipment.</li>
   * <li>Adds the line from the first order. Set movement quantity less than the proposed
   * value.</li>
   * <li>Adds the line from the second order. Set movement quantity less than the proposed
   * value.</li>
   * <li>Complete the document invoicing if possible.</li>
   * <li>Verify an invoice was generated containing just the line from the first order</li>
   * </ol>
   */

  @Test
  public void invoiceFromShipment_005() {
    OBContext.setAdminMode();
    try {

      final Product productOne = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID,
          "InvoiceFromShipment_005");
      final Product productTwo = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID,
          "InvoiceFromShipment_005");

      final Order firstSalesOrder = createSalesOrderWithInvoiceTerm(SALES_ORDER,
          "InvoiceFromShipment_005", AFTER_DELIVERY);
      final OrderLine firstLine = getOrderLineByLineNo(firstSalesOrder, 10L);
      setProductInOrderLine(firstLine, productOne);
      TestUtils.processOrder(firstSalesOrder);

      final Order secondSalesOrder = createSalesOrderWithInvoiceTerm(SALES_ORDER,
          "InvoiceFromShipment_005", AFTER_ORDER_DELIVERY);
      final OrderLine secondLine = getOrderLineByLineNo(secondSalesOrder, 10L);
      setProductInOrderLine(secondLine, productTwo);
      TestUtils.processOrder(secondSalesOrder);

      final ShipmentInOut shipment = createShipmentReceiptWithTwoLines(GOODS_SHIPMENT_ID,
          "InvoiceFromShipment_005");
      final ShipmentInOutLine firstShipmentLine = getShipmentLineByLineNo(shipment, 10L);
      setProductInShipmentLine(firstShipmentLine, productOne);
      setOrderLineInShipmentLine(firstShipmentLine, firstLine);
      updateMovementQuantity(firstShipmentLine, BigDecimal.TEN);
      final ShipmentInOutLine secondShipmentLine = getShipmentLineByLineNo(shipment, 20L);
      setProductInShipmentLine(secondShipmentLine, productTwo);
      setOrderLineInShipmentLine(secondShipmentLine, secondLine);
      updateMovementQuantity(secondShipmentLine, BigDecimal.TEN);

      TestUtils.processShipmentReceipt(shipment);
      OBDal.getInstance().refresh(firstSalesOrder); // Necessary to update the delivery status
      OBDal.getInstance().refresh(secondSalesOrder); // Necessary to update the delivery status

      final Invoice invoice = new InvoiceGeneratorFromGoodsShipment(shipment.getId())
          .createInvoiceConsideringInvoiceTerms(true);
      assertInvoiceLineNumber(invoice, 1);
      assertGeneratedInvoiceLine(invoice, productOne, BigDecimal.TEN);
      assertInvoiceDatePriceListAndStatus(invoice, shipment.getMovementDate(),
          firstSalesOrder.getPriceList(), "CO");
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Generating invoice from Goods Shipment with lines linked to Sales Order with invoice terms
   * “Immediate” and “Do not invoice”
   * <ol>
   * <li>Create a Sales Order with invoice term “Immediate”</li>
   * <li>Add one line of product. Book the order.</li>
   * <li>Create a Sales Order with invoice term “Do not invoice”.</li>
   * <li>Add one line to the order. Book the order.</li>
   * <li>Create a Goods Shipment with the lines from the two orders.</li>
   * <li>Complete the document invoicing if possible.</li>
   * <li>Verify an invoice was created containing a line linked to the shipment line from
   * "Immediate" Sales Order</li>
   * </ol>
   */

  @Test
  public void invoiceFromShipment_006() {
    OBContext.setAdminMode();
    try {

      final Product productOne = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID,
          "InvoiceFromShipment_006");
      final Product productTwo = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID,
          "InvoiceFromShipment_006");

      final Order firstSalesOrder = createSalesOrderWithInvoiceTerm(SALES_ORDER,
          "InvoiceFromShipment_006", IMMEDIATE);
      final OrderLine firstLine = getOrderLineByLineNo(firstSalesOrder, 10L);
      setProductInOrderLine(firstLine, productOne);
      TestUtils.processOrder(firstSalesOrder);

      final Order secondSalesOrder = createSalesOrderWithInvoiceTerm(SALES_ORDER,
          "InvoiceFromShipment_006", DO_NOT_INVOICE);
      final OrderLine secondLine = getOrderLineByLineNo(secondSalesOrder, 10L);
      setProductInOrderLine(secondLine, productTwo);
      TestUtils.processOrder(secondSalesOrder);

      final ShipmentInOut shipment = createShipmentReceiptWithTwoLines(GOODS_SHIPMENT_ID,
          "InvoiceFromShipment_006");
      final ShipmentInOutLine firstShipmentLine = getShipmentLineByLineNo(shipment, 10L);
      setProductInShipmentLine(firstShipmentLine, productOne);
      setOrderLineInShipmentLine(firstShipmentLine, firstLine);
      final ShipmentInOutLine secondShipmentLine = getShipmentLineByLineNo(shipment, 20L);
      setProductInShipmentLine(secondShipmentLine, productTwo);
      setOrderLineInShipmentLine(secondShipmentLine, secondLine);

      TestUtils.processShipmentReceipt(shipment);

      final Invoice invoice = new InvoiceGeneratorFromGoodsShipment(shipment.getId())
          .createInvoiceConsideringInvoiceTerms(true);
      assertInvoiceLineNumber(invoice, 1);
      assertGeneratedInvoiceLine(invoice, productOne, new BigDecimal(12));
      assertInvoiceDatePriceListAndStatus(invoice, shipment.getMovementDate(),
          firstSalesOrder.getPriceList(), "CO");
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Generating invoice from Goods Shipment with lines partially invoiced from an order with invoice
   * term “After Order Delivery”
   * <ol>
   * <li>Create a Sales Order with invoice term “After Order Delivery”.</li>
   * <li>Add two lines of product. Book the order.</li>
   * <li>Create a Goods Shipment.</li>
   * <li>Add the first order line.</li>
   * <li>Complete the document without invoicing.</li>
   * <li>Create a second Goods Shipment.</li>
   * <li>Add the second order line.</li>
   * <li>Complete the document without invoicing.</li>
   * <li>Create a Sales Invoice.</li>
   * <li>Add the line for the first Goods Shipment.</li>
   * <li>Invoice a quantity less than the movement quantity.</li>
   * <li>Complete the document.</li>
   * <li>Select the second Goods Shipment in Goods Shipment window.</li>
   * <li>Generate invoice from shipment using the button.</li>
   * <li>Verify a new invoice was created containing:</li>
   * <ol>
   * <li>A line with the product of the first Goods Shipment, and invoiced quantity as the
   * difference between the movement quantity and the invoiced quantity of the Sales Invoice.</li>
   * <li>A line with the product of the second Goods Shipment with invoiced quantity as the movement
   * quantity</li>
   * </ol>
   * </ol>
   */
  @Test
  public void invoiceFromShipment_007() {
    OBContext.setAdminMode();
    try {

      final Product productOne = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID,
          "InvoiceFromShipment_007");
      final Product productTwo = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID,
          "InvoiceFromShipment_007");

      final Order salesOrder = createAfterOrderDeliveredOrder(SALES_ORDER,
          "InvoiceFromShipment_007");
      final OrderLine firstLine = getOrderLineByLineNo(salesOrder, 10L);
      setProductInOrderLine(firstLine, productOne);
      final OrderLine secondLine = getOrderLineByLineNo(salesOrder, 20L);
      setProductInOrderLine(secondLine, productTwo);
      TestUtils.processOrder(salesOrder);

      final ShipmentInOut firstShipment = TestUtils.cloneReceiptShipment(GOODS_SHIPMENT_ID,
          "InvoiceFromShipment_007");
      final ShipmentInOutLine firstShipmentLine = getShipmentLineByLineNo(firstShipment, 10L);
      setProductInShipmentLine(firstShipmentLine, productOne);
      setOrderLineInShipmentLine(firstShipmentLine, firstLine);

      TestUtils.processShipmentReceipt(firstShipment);

      final Invoice invoice = createInvoiceFromShipment(firstShipment, SALES_INVOICE);
      TestUtils.processInvoice(invoice);

      final ShipmentInOut secondShipment = TestUtils.cloneReceiptShipment(GOODS_SHIPMENT_ID,
          "InvoiceFromShipment_007");
      final ShipmentInOutLine secondShipmentLine = getShipmentLineByLineNo(secondShipment, 10L);
      setProductInShipmentLine(secondShipmentLine, productTwo);
      setOrderLineInShipmentLine(secondShipmentLine, secondLine);

      TestUtils.processShipmentReceipt(secondShipment);

      final Invoice secondInvoice = new InvoiceGeneratorFromGoodsShipment(secondShipment.getId())
          .createInvoiceConsideringInvoiceTerms(true);
      assertInvoiceLineNumber(secondInvoice, 2);
      assertGeneratedInvoiceLine(secondInvoice, productOne, new BigDecimal(2));
      assertGeneratedInvoiceLine(secondInvoice, productTwo, new BigDecimal(12));
      assertInvoiceDatePriceListAndStatus(secondInvoice, secondShipment.getMovementDate(),
          salesOrder.getPriceList(), "CO");
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Verify no invoice is generated when processing a Goods Shipment after being previously
   * processed
   * <ol>
   * <li>Create a Sales Order with invoice term "After Delivery".</li>
   * <li>Adds two lines of product. Book the order.</li>
   * <li>Create a Goods Shipment.</li>
   * <li>Adds just one line from the previous order.</li>
   * <li>Complete the document invoicing if possible.</li>
   * <li>Verify an invoice was created and processed containing the line from Goods Shipment.</li>
   * <li>Process the shipment a second time</li>
   * <li>Verify no invoice was generated</li>
   * </ol>
   */

  @Test
  public void invoiceFromShipment_008() {
    OBContext.setAdminMode();
    try {
      final Product product = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID,
          "InvoiceFromShipment_008");
      final Order salesOrder = createSalesOrderWithInvoiceTerm(SALES_ORDER,
          "InvoiceFromShipment_008", AFTER_DELIVERY);
      final OrderLine orderLine = getOrderLineByLineNo(salesOrder, 10L);
      setProductInOrderLine(orderLine, product);
      TestUtils.processOrder(salesOrder);

      final ShipmentInOut shipment = TestUtils.cloneReceiptShipment(GOODS_SHIPMENT_ID,
          "InvoiceFromShipment_008");
      final ShipmentInOutLine shipmentLine = getShipmentLineByLineNo(shipment, 10L);
      setProductInShipmentLine(shipmentLine, product);
      setOrderLineInShipmentLine(shipmentLine, orderLine);
      TestUtils.processShipmentReceipt(shipment);

      final Invoice invoice = new InvoiceGeneratorFromGoodsShipment(shipment.getId())
          .createInvoiceConsideringInvoiceTerms(true);
      assertInvoiceLineNumber(invoice, 1);
      assertGeneratedInvoiceLine(invoice, product, new BigDecimal(12));
      assertInvoiceDatePriceListAndStatus(invoice, shipment.getMovementDate(),
          salesOrder.getPriceList(), "CO");
      OBDal.getInstance().refresh(orderLine); // Necessary to force DAL to refresh invoiced qty

      final Invoice secondInvoice = new InvoiceGeneratorFromGoodsShipment(shipment.getId())
          .createInvoiceConsideringInvoiceTerms(false);
      assertNoInvoiceWasGenerated(secondInvoice);

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Generating invoice from a Goods Shipment linked to a Sales Order with invoice term "After
   * Delivery" and specific invoice date and price list
   * <ol>
   * <li>Create a Sales Order with invoice term "After Delivery".</li>
   * <li>Adds two lines of product. Book the order.</li>
   * <li>Create a Goods Shipment.</li>
   * <li>Adds just one line from the previous order.</li>
   * <li>Complete the document invoicing if possible with invoice date tomorrow and price list
   * "Sales"</li>
   * <li>Verify an invoice was created and processed containing the line from Goods Shipment with
   * invoice date tomorrow and price list "Sales".</li>
   * </ol>
   */
  @Test
  public void invoiceFromShipment_009() {
    OBContext.setAdminMode();
    try {

      final Product product = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID,
          "InvoiceFromShipment_009");
      final Order salesOrder = createSalesOrderWithInvoiceTerm(SALES_ORDER,
          "InvoiceFromShipment_009", AFTER_DELIVERY);
      final OrderLine orderLine = getOrderLineByLineNo(salesOrder, 10L);
      setProductInOrderLine(orderLine, product);
      TestUtils.processOrder(salesOrder);

      final ShipmentInOut shipment = TestUtils.cloneReceiptShipment(GOODS_SHIPMENT_ID,
          "InvoiceFromShipment_009");
      final ShipmentInOutLine shipmentLine = getShipmentLineByLineNo(shipment, 10L);
      setProductInShipmentLine(shipmentLine, product);
      setOrderLineInShipmentLine(shipmentLine, orderLine);
      TestUtils.processShipmentReceipt(shipment);

      Calendar calendar = Calendar.getInstance();
      calendar.add(Calendar.DAY_OF_MONTH, 1);
      Date invoiceDate = calendar.getTime();

      PriceList priceList = OBDal.getInstance().get(PriceList.class, PRICE_LIST_SALES_ID);

      final Invoice invoice = new InvoiceGeneratorFromGoodsShipment(shipment.getId(), invoiceDate,
          priceList).createInvoiceConsideringInvoiceTerms(true);
      assertGeneratedInvoiceLine(invoice, product, new BigDecimal(12));
      assertInvoiceDatePriceListAndStatus(invoice, invoiceDate, priceList, "CO");

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Generating invoice from a Goods Shipment linked to a Sales Order with invoice term "After
   * Delivery" and specific invoice date, price list and status draft
   * <ol>
   * <li>Create a Sales Order with invoice term "After Delivery".</li>
   * <li>Adds two lines of product. Book the order.</li>
   * <li>Create a Goods Shipment.</li>
   * <li>Adds just one line from the previous order.</li>
   * <li>Complete the document invoicing if possible with invoice date tomorrow, price list "Sales",
   * and not processing the invoice</li>
   * <li>Verify an invoice was created and processed containing the line from Goods Shipment with
   * invoice date tomorrow, price list "Sales" and document status draft</li>
   * </ol>
   */
  @Test
  public void invoiceFromShipment_010() {
    OBContext.setAdminMode();
    try {

      final Product product = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID,
          "InvoiceFromShipment_010");
      final Order salesOrder = createSalesOrderWithInvoiceTerm(SALES_ORDER,
          "InvoiceFromShipment_010", AFTER_DELIVERY);
      final OrderLine orderLine = getOrderLineByLineNo(salesOrder, 10L);
      setProductInOrderLine(orderLine, product);
      TestUtils.processOrder(salesOrder);

      final ShipmentInOut shipment = TestUtils.cloneReceiptShipment(GOODS_SHIPMENT_ID,
          "InvoiceFromShipment_010");
      final ShipmentInOutLine shipmentLine = getShipmentLineByLineNo(shipment, 10L);
      setProductInShipmentLine(shipmentLine, product);
      setOrderLineInShipmentLine(shipmentLine, orderLine);
      TestUtils.processShipmentReceipt(shipment);

      Calendar calendar = Calendar.getInstance();
      calendar.add(Calendar.DAY_OF_MONTH, 1);
      Date invoiceDate = calendar.getTime();

      PriceList priceList = OBDal.getInstance().get(PriceList.class, PRICE_LIST_SALES_ID);

      final Invoice invoice = new InvoiceGeneratorFromGoodsShipment(shipment.getId(), invoiceDate,
          priceList).createInvoiceConsideringInvoiceTerms(false);
      assertGeneratedInvoiceLine(invoice, product, new BigDecimal(12));
      assertInvoiceDatePriceListAndStatus(invoice, invoiceDate, priceList, "DR");

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private Order createSalesOrderWithInvoiceTerm(String salesOrderId, String docNno,
      String invoiceTerm) {
    final Order salesOrder = TestUtils.cloneOrder(salesOrderId, docNno);
    salesOrder.setInvoiceTerms(invoiceTerm);
    return salesOrder;
  }

  private Order createAfterOrderDeliveredOrder(String salesOrderWithOneLineId, String docNo) {
    final Order salesOrder = TestUtils.cloneOrder(salesOrderWithOneLineId, docNo);
    salesOrder.setInvoiceTerms(AFTER_ORDER_DELIVERY);
    final OrderLine orderLine = salesOrder.getOrderLineList().get(0);
    final OrderLine newOrderLine = TestUtils.cloneOrderLine(orderLine, salesOrder);
    newOrderLine.setLineNo(orderLine.getLineNo() + 10);
    OBDal.getInstance().flush();
    return salesOrder;
  }

  private void setProductInOrderLine(final OrderLine orderLine, final Product product) {
    orderLine.setProduct(product);
  }

  private ShipmentInOut createShipmentReceiptWithTwoLines(String goodsShipmentId, String docNo) {
    final ShipmentInOut shipment = TestUtils.cloneReceiptShipment(goodsShipmentId, docNo);
    final ShipmentInOutLine shipmentLine = shipment.getMaterialMgmtShipmentInOutLineList().get(0);
    final ShipmentInOutLine newShipmentLine = TestUtils.cloneReceiptShipmentLine(shipmentLine,
        shipment);
    newShipmentLine.setLineNo(shipmentLine.getLineNo() + 10);
    OBDal.getInstance().flush();
    return shipment;
  }

  private void setProductInShipmentLine(final ShipmentInOutLine shipmentLine,
      final Product product) {
    shipmentLine.setProduct(product);
  }

  private void setOrderLineInShipmentLine(final ShipmentInOutLine shipmentLine,
      final OrderLine orderLine) {
    shipmentLine.setSalesOrderLine(orderLine);
  }

  private void updateMovementQuantity(final ShipmentInOutLine shipmentLine,
      final BigDecimal movementQuantity) {
    shipmentLine.setMovementQuantity(movementQuantity);
  }

  private void assertInvoiceLineNumber(final Invoice invoice, final int numLines) {
    assertThat("Invoice should have " + numLines + " lines", invoice.getInvoiceLineList().size(),
        equalTo(numLines));
  }

  private void assertNoInvoiceWasGenerated(final Invoice invoice) {
    assertThat("Invoice should be null", invoice == null, equalTo(true));
  }

  private void assertGeneratedInvoiceLine(final Invoice invoice, final Product product,
      final BigDecimal quantity) {
    assertThat("Invoice should not be null", invoice == null, equalTo(false));
    final InvoiceLine invoiceLine = getInvoiceLineByProductQuantity(invoice, product, quantity);
    assertThat("Invoice Line should have the product " + product, invoiceLine == null,
        equalTo(false));
  }

  private ShipmentInOutLine addLineToShipment(final ShipmentInOut shipment) {
    int numberOfLines = shipment.getMaterialMgmtShipmentInOutLineList().size();
    final ShipmentInOutLine shipmentLine = shipment.getMaterialMgmtShipmentInOutLineList()
        .get(numberOfLines - 1);
    final ShipmentInOutLine newShipmentLine = TestUtils.cloneReceiptShipmentLine(shipmentLine,
        shipment);
    newShipmentLine.setLineNo(getMaxLineNo(shipment) + 10);
    OBDal.getInstance().flush();
    return newShipmentLine;
  }

  private Long getMaxLineNo(final ShipmentInOut shipment) {
    final String hql = "select coalesce(max(sl.lineNo), 0) from MaterialMgmtShipmentInOutLine as sl where sl.shipmentReceipt.id = :shipmentId";
    final Query<Long> query = OBDal.getInstance().getSession().createQuery(hql, Long.class);
    query.setParameter("shipmentId", shipment.getId());
    final Long maxLineNo = query.uniqueResult();
    if (maxLineNo != null) {
      return maxLineNo;
    }
    return 0L;
  }

  private OrderLine getOrderLineByLineNo(final Order salesOrder, long lineNo) {
    final String hql = "as ol where ol.salesOrder.id = :salesOrderId and ol.lineNo = :lineNo";
    final OBQuery<OrderLine> query = OBDal.getInstance().createQuery(OrderLine.class, hql);
    query.setNamedParameter("salesOrderId", salesOrder.getId());
    query.setNamedParameter("lineNo", lineNo);
    query.setMaxResult(1);
    return query.uniqueResult();
  }

  private ShipmentInOutLine getShipmentLineByLineNo(final ShipmentInOut shipment, long lineNo) {
    final String hql = "as sl where sl.shipmentReceipt.id = :shipmentId and sl.lineNo = :lineNo";
    final OBQuery<ShipmentInOutLine> query = OBDal.getInstance()
        .createQuery(ShipmentInOutLine.class, hql);
    query.setNamedParameter("shipmentId", shipment.getId());
    query.setNamedParameter("lineNo", lineNo);
    query.setMaxResult(1);
    return query.uniqueResult();
  }

  private InvoiceLine getInvoiceLineByProductQuantity(final Invoice invoice, final Product product,
      BigDecimal quantity) {
    final String hql = "as il where il.invoice.id = :invoiceId and il.product.id =:productId and il.invoicedQuantity = :invoicedQuantity";
    final OBQuery<InvoiceLine> query = OBDal.getInstance().createQuery(InvoiceLine.class, hql);
    query.setNamedParameter("invoiceId", invoice.getId());
    query.setNamedParameter("productId", product.getId());
    query.setNamedParameter("invoicedQuantity", quantity);
    query.setMaxResult(1);
    return query.uniqueResult();
  }

  private Invoice createInvoiceFromShipment(final ShipmentInOut shipment,
      final String salesInvoiceId) {
    final Invoice invoice = OBDal.getInstance().get(Invoice.class, salesInvoiceId);
    final Invoice newInvoice = (Invoice) DalUtil.copy(invoice, false);
    newInvoice.setId(SequenceIdData.getUUID());
    newInvoice.setNewOBObject(true);
    newInvoice.setAccountingDate(shipment.getAccountingDate());
    newInvoice.setInvoiceDate(shipment.getMovementDate());
    final String documentNo = Utility.getDocumentNo(OBDal.getInstance().getConnection(false),
        new DalConnectionProvider(false), RequestContext.get().getVariablesSecureApp(), "",
        Invoice.TABLE_NAME,
        newInvoice.getTransactionDocument() == null ? ""
            : newInvoice.getTransactionDocument().getId(),
        newInvoice.getDocumentType() == null ? "" : newInvoice.getDocumentType().getId(), false,
        true);
    newInvoice.setDocumentNo(documentNo);
    OBDal.getInstance().save(newInvoice);
    OBDal.getInstance().flush();

    final ShipmentInOutLine shipmentInOutLine = shipment.getMaterialMgmtShipmentInOutLineList()
        .get(0);
    WeldUtils.getInstanceFromStaticBeanManager(CreateInvoiceLinesFromProcess.class)
        .createInvoiceLinesFromDocumentLines(
            getShipmentLineToBeInvoiced(shipmentInOutLine, BigDecimal.TEN), newInvoice,
            ShipmentInOutLine.class);

    OBDal.getInstance().flush();
    return newInvoice;
  }

  private JSONArray getShipmentLineToBeInvoiced(final ShipmentInOutLine shipmentInOutLine,
      final BigDecimal invoicedQuantity) {

    final JSONArray lines = new JSONArray();
    try {
      final JSONObject line = new JSONObject();
      line.put("uOM", shipmentInOutLine.getUOM().getId());
      line.put("uOM$_identifier", shipmentInOutLine.getUOM().getIdentifier());
      line.put("product", shipmentInOutLine.getProduct().getId());
      line.put("product$_identifier", shipmentInOutLine.getProduct().getIdentifier());
      line.put("lineNo", shipmentInOutLine.getLineNo());
      line.put("movementQuantity", invoicedQuantity.toString());
      line.put("operativeQuantity",
          shipmentInOutLine.getOperativeQuantity() == null
              ? shipmentInOutLine.getMovementQuantity().toString()
              : shipmentInOutLine.getOperativeQuantity().toString());
      line.put("id", shipmentInOutLine.getId());
      line.put("operativeUOM",
          shipmentInOutLine.getOperativeUOM() == null ? shipmentInOutLine.getUOM().getId()
              : shipmentInOutLine.getOperativeUOM().getId());
      line.put("operativeUOM$_identifier",
          shipmentInOutLine.getOperativeUOM() == null ? shipmentInOutLine.getUOM().getIdentifier()
              : shipmentInOutLine.getOperativeUOM().getIdentifier());
      line.put("orderQuantity", "");
      lines.put(line);
    } catch (JSONException e) {
      log.error(e.getMessage());
    }
    return lines;
  }

  private void assertInvoiceDatePriceListAndStatus(Invoice invoice, Date invoiceDate,
      PriceList priceList, String status) {
    assertThat("Invoice status should have been " + status, invoice.getDocumentStatus(),
        equalTo(status));
    assertTrue("Invoice date should have been " + invoiceDate,
        DateUtils.truncatedEquals(invoice.getInvoiceDate(), invoiceDate, Calendar.DATE));
    assertThat("Price List should have been " + priceList.getId(), invoice.getPriceList().getId(),
        equalTo(priceList.getId()));
  }

}
