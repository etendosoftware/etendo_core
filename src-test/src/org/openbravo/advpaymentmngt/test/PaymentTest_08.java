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
 * All portions are Copyright (C) 2011-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceDiscount;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentExecutionProcess;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.test.base.OBBaseTest;

public class PaymentTest_08 extends OBBaseTest {

  // 1) Create an order
  // 2) Add a partial payment but do not execute it
  // 3) Invoice the order partially using an automatic receipt payment method
  // 4)Review payment info

  private static final Logger log = LogManager.getLogger();

  private static final String MANUAL_EXECUTION = "M";
  private static final String AUTOMATIC_EXECUTION = "A";
  private static final String CLEARED_ACCOUNT = "CLE";
  private static final String IN_TRANSIT_ACCOUNT = "INT";
  private static final String WITHDRAWN_ACCOUNT = "WIT";
  private static final String DEPOSIT_ACCOUNT = "DEP";
  private static final String CASH = "C";
  private static final String STANDARD_DESCRIPTION = "JUnit Test Payment_08";

  private String financialAccountId;

  /**
   * Initial Set up.
   * 
   * This before method is named setUpP08() to avoid overwriting the super setUp method that is
   * invoke automatically before this one.
   * 
   */
  @Before
  public void setUpP08() throws Exception {
    TestUtility.setTestContextSpain();
  }

  @Test
  public void testRunPayment_08() {
    boolean exception = false;
    try {
      // DATA SETUP
      Invoice invoice = dataSetup();
      Order order = invoice.getSalesOrder();
      // CHECK OUTPUT DATA
      OBContext.setAdminMode();
      try {
        assertTrue("Invoice Payment Schedule Outstanding Amount == 0", BigDecimal.ZERO
            .compareTo(invoice.getFINPaymentScheduleList().get(0).getOutstandingAmount()) != 0);
        assertTrue("Invoice Payment Schedule  not Total Pending", invoice.getGrandTotalAmount()
            .compareTo(invoice.getFINPaymentScheduleList().get(0).getOutstandingAmount()) == 0);
        assertTrue("Invoice Payment Schedule Paid Amount != 0", BigDecimal.ZERO
            .compareTo(invoice.getFINPaymentScheduleList().get(0).getPaidAmount()) == 0);
        assertTrue("Order Payment Schedule Outstanding Amount == 0", BigDecimal.ZERO
            .compareTo(order.getFINPaymentScheduleList().get(0).getOutstandingAmount()) != 0);
        assertTrue("Order Payment Schedule not Total Pending", invoice.getGrandTotalAmount()
            .compareTo(invoice.getFINPaymentScheduleList().get(0).getOutstandingAmount()) == 0);
        assertTrue("Order Payment Schedule Paid Amount != 0", BigDecimal.ZERO
            .compareTo(order.getFINPaymentScheduleList().get(0).getPaidAmount()) == 0);

      } finally {
        OBContext.restorePreviousMode();
      }

    } catch (Exception e) {
      e.printStackTrace();
      log.error(FIN_Utility.getExceptionMessage(e));
      exception = true;
    }

    assertFalse(exception);

  }

  private Invoice dataSetup() throws Exception {

    // DATA SETUP
    String bpartnerId = "A6750F0D15334FB890C254369AC750A8"; // Alimentos y Supermercados, S.A.
    String priceListId = "AEE66281A08F42B6BC509B8A80A33C29"; // Tarifa de ventas
    String paymentTermId = "66BA1164A7394344BB9CD1A6ECEED05D"; // 30 days
    String currencyId = EURO_ID;
    String productId = "FDDBB89508D24D2C8D97A2A57BEA8788"; // Vino Tinto 0,75L
    String taxId = "4BF9470755AD4395AABCB77F5014CBE8"; // Ventas Exentas
    String docTypeId = "7FCD49652E104E6BB06C3A0D787412E3"; // AR Invoice
    String orderDocTypeId = "466AF4B0136A4A3F9F84129711DA8BD3"; // Standard Order
    String warehouseId = "B2D40D8A5D644DD89E329DC297309055"; // España Región Norte
    final String SIMPLE_EXECUTION_PROCESS = "301950D5D2F24F49916EDE06A473DF02"; // Simple Execution
                                                                                // Process
    // Process
    BigDecimal quantity = new BigDecimal("10");
    BigDecimal invoicedQuantity = new BigDecimal("5");
    BigDecimal netUnitPrice = new BigDecimal("10");
    BigDecimal netListPrice = new BigDecimal("10");
    BigDecimal lineNetAmount = new BigDecimal("100");
    BigDecimal invoiceLineNetAmount = new BigDecimal("50");
    BigDecimal priceLimit = new BigDecimal("10");

    PriceList testPriceList = OBDal.getInstance().get(PriceList.class, priceListId);
    BusinessPartner testBusinessPartner = OBDal.getInstance()
        .get(BusinessPartner.class, bpartnerId);
    Location location = TestUtility.getOneInstance(Location.class,
        new Value(Location.PROPERTY_BUSINESSPARTNER, testBusinessPartner));
    PaymentTerm testPaymentTerm = OBDal.getInstance().get(PaymentTerm.class, paymentTermId);
    Currency testCurrency = OBDal.getInstance().get(Currency.class, currencyId);
    Product testProduct = OBDal.getInstance().get(Product.class, productId);
    UOM uom = TestUtility.getOneInstance(UOM.class,
        new Value(UOM.PROPERTY_NAME, testProduct.getUOM().getName()));
    TaxRate testTaxRate = OBDal.getInstance().get(TaxRate.class, taxId);
    DocumentType testDocumentType = OBDal.getInstance().get(DocumentType.class, docTypeId);
    DocumentType testOrderDocumentType = OBDal.getInstance()
        .get(DocumentType.class, orderDocTypeId);
    FIN_FinancialAccount testAccount = TestUtility.insertFinancialAccount("APRM_FINACC_PAYMENT_08",
        STANDARD_DESCRIPTION, testCurrency, CASH, false,
        getOneInstance(org.openbravo.model.common.geography.Location.class), null, null, null, null,
        null, null, null, null, null, null, BigDecimal.ZERO, BigDecimal.ZERO, null, true, true);

    FIN_PaymentMethod testPaymentMethod = TestUtility.insertPaymentMethod("APRM_PM_PAYMENT_08",
        STANDARD_DESCRIPTION, true, true, false, MANUAL_EXECUTION, null, false, IN_TRANSIT_ACCOUNT,
        DEPOSIT_ACCOUNT, CLEARED_ACCOUNT, true, false, false, MANUAL_EXECUTION, null, false,
        IN_TRANSIT_ACCOUNT, WITHDRAWN_ACCOUNT, CLEARED_ACCOUNT, true, true);

    FinAccPaymentMethod existAssociation = TestUtility.getOneInstance(FinAccPaymentMethod.class,
        new Value(FinAccPaymentMethod.PROPERTY_ACCOUNT, testAccount),
        new Value(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, testPaymentMethod));

    if (existAssociation == null) {
      TestUtility.associatePaymentMethod(testAccount, testPaymentMethod);
    }
    this.financialAccountId = testAccount.getId();

    // UPDATE BPARTNER AND ASSOCIATED NEW FINANCIAL ACCOUNT
    testBusinessPartner.setAccount(testAccount);
    OBDal.getInstance().save(testBusinessPartner);

    Warehouse warehouse = OBDal.getInstance().get(Warehouse.class, warehouseId);
    Order order = TestUtility.createNewOrder(OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), new Date(), new Date(), new Date(),
        testOrderDocumentType, testBusinessPartner, location, warehouse, "I", testPriceList,
        testCurrency, testPaymentMethod, testPaymentTerm, testProduct, uom, quantity, netUnitPrice,
        netListPrice, priceLimit, testTaxRate, lineNetAmount, true);

    TestUtility.processOrder(order);
    order = OBDal.getInstance().get(Order.class, order.getId());

    // CHECK OUTPUT DATA FOR ORDER
    OBContext.setAdminMode();
    try {
      FIN_PaymentScheduleDetail psd = TestUtility.getOneInstance(FIN_PaymentScheduleDetail.class,
          new Value(FIN_PaymentScheduleDetail.PROPERTY_ORDERPAYMENTSCHEDULE,
              order.getFINPaymentScheduleList().get(0)));

      assertTrue("Payment Schedule Outstanding Amount == Order amount", order.getGrandTotalAmount()
          .compareTo(order.getFINPaymentScheduleList().get(0).getOutstandingAmount()) == 0);
      assertTrue("Payment Schedule Received Amount == 0",
          BigDecimal.ZERO.compareTo(order.getFINPaymentScheduleList().get(0).getPaidAmount()) == 0);

      assertTrue("Payment Schedule Deatail Amount == Total Amount",
          order.getGrandTotalAmount().compareTo(psd.getAmount()) == 0);
      assertTrue("Payment Schedule Detail Write-off Amount == 0",
          BigDecimal.ZERO.compareTo(psd.getWriteoffAmount()) == 0);
    } finally {
      OBContext.restorePreviousMode();
    }
    FIN_Payment payment = TestUtility.addPaymentFromOrder(order,
        OBDal.getInstance().get(FIN_FinancialAccount.class, financialAccountId),
        order.getGrandTotalAmount(), false);

    FIN_PaymentMethod awaitingExecutionPM = TestUtility.insertPaymentMethod(
        "APRM_PM_PAYMENT_08_AWE", STANDARD_DESCRIPTION, true, false, false, AUTOMATIC_EXECUTION,
        OBDal.getInstance().get(PaymentExecutionProcess.class, SIMPLE_EXECUTION_PROCESS), true,
        IN_TRANSIT_ACCOUNT, DEPOSIT_ACCOUNT, CLEARED_ACCOUNT, true, false, false, MANUAL_EXECUTION,
        null, false, IN_TRANSIT_ACCOUNT, WITHDRAWN_ACCOUNT, CLEARED_ACCOUNT, true, true);

    existAssociation = TestUtility.getOneInstance(FinAccPaymentMethod.class,
        new Value(FinAccPaymentMethod.PROPERTY_ACCOUNT, testAccount),
        new Value(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, awaitingExecutionPM));

    if (existAssociation == null) {
      TestUtility.associatePaymentMethod(testAccount, awaitingExecutionPM);
    }

    // SET NEW PAYMENT METHOD FOR CREATED PAYMENT: AWAITING EXECUTION
    // NO PAYMENT INFO SHOULD BE UPDATED AS PAYMENT HAS NOT BEEN EXECUTED YET
    payment.setPaymentMethod(awaitingExecutionPM);
    OBDal.getInstance().save(payment);
    // PROCESS THE PAYMENT
    TestUtility.processPayment(payment, "P");
    payment = OBDal.getInstance().get(FIN_Payment.class, payment.getId());
    order = OBDal.getInstance().get(Order.class, order.getId());

    // CHECK OUTPUT DATA FOR ORDER
    OBContext.setAdminMode();
    try {
      assertTrue("Status != Awaiting Execution", "RPAE".equals(payment.getStatus()));
      // CHECK OUTPUT DATA FOR ORDER AGAIN NO CHANGE SHOULD HAPPEN AS PAYMENT REMAINS AWE
      FIN_PaymentScheduleDetail psd = TestUtility.getOneInstance(FIN_PaymentScheduleDetail.class,
          new Value(FIN_PaymentScheduleDetail.PROPERTY_ORDERPAYMENTSCHEDULE,
              order.getFINPaymentScheduleList().get(0)));
      assertTrue("Payment Schedule Outstanding Amount == Order amount", order.getGrandTotalAmount()
          .compareTo(order.getFINPaymentScheduleList().get(0).getOutstandingAmount()) == 0);
      assertTrue("Payment Schedule Received Amount == 0",
          BigDecimal.ZERO.compareTo(order.getFINPaymentScheduleList().get(0).getPaidAmount()) == 0);

      assertTrue("Payment Schedule Deatail Amount == Total Amount",
          order.getGrandTotalAmount().compareTo(psd.getAmount()) == 0);
      assertTrue("Payment Schedule Detail Write-off Amount == 0",
          BigDecimal.ZERO.compareTo(psd.getWriteoffAmount()) == 0);
    } finally {
      OBContext.restorePreviousMode();
    }
    // This created order is PARTIALLY invoiced

    Invoice invoice = TestUtility.createNewInvoice(OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), new Date(), new Date(), new Date(),
        testDocumentType, testBusinessPartner, location, testPriceList, testCurrency,
        testPaymentMethod, testPaymentTerm, testProduct, uom, invoicedQuantity, netUnitPrice,
        netListPrice, priceLimit, testTaxRate, invoiceLineNetAmount, true);
    OBDal.getInstance().refresh(invoice);
    // Order is associated both to header and lines
    InvoiceLine invoiceLine = invoice.getInvoiceLineList().get(0);
    invoiceLine.setSalesOrderLine(order.getOrderLineList().get(0));

    OBDal.getInstance().save(invoiceLine);

    invoice.setSalesOrder(order);

    OBDal.getInstance().save(invoice);

    // Add invoice Discount
    InvoiceDiscount invoiceDiscount = OBProvider.getInstance().get(InvoiceDiscount.class);
    invoiceDiscount.setInvoice(invoice);
    invoiceDiscount.setLineNo(10l);
    invoiceDiscount.setDiscount(order.getOrderDiscountList().get(0).getDiscount());
    OBDal.getInstance().save(invoiceDiscount);

    TestUtility.processInvoice(invoice);
    invoice = OBDal.getInstance().get(Invoice.class, invoice.getId());
    // CHECK OUTPUT DATA FOR INVOICE
    OBContext.setAdminMode();
    try {
      // CHECK OUTPUT DATA FOR INVOICE
      FIN_PaymentScheduleDetail psd = TestUtility.getOneInstance(FIN_PaymentScheduleDetail.class,
          new Value(FIN_PaymentScheduleDetail.PROPERTY_INVOICEPAYMENTSCHEDULE,
              invoice.getFINPaymentScheduleList().get(0)));
      assertTrue("Payment Schedule Outstanding Amount == Order amount",
          invoice.getGrandTotalAmount()
              .compareTo(invoice.getFINPaymentScheduleList().get(0).getOutstandingAmount()) == 0);
      assertTrue("Payment Schedule Received Amount == 0", BigDecimal.ZERO
          .compareTo(invoice.getFINPaymentScheduleList().get(0).getPaidAmount()) == 0);

      assertTrue("Payment Schedule Deatail Amount == Total Amount",
          invoice.getGrandTotalAmount().compareTo(psd.getAmount()) == 0);
      assertTrue("Payment Schedule Detail Write-off Amount == 0",
          BigDecimal.ZERO.compareTo(psd.getWriteoffAmount()) == 0);
    } finally {
      OBContext.restorePreviousMode();
    }
    return invoice;
  }
}
