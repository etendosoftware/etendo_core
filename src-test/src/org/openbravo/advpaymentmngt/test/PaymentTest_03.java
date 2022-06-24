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
 * All portions are Copyright (C) 2010-2017 Openbravo SLU
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
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.test.base.OBBaseTest;

/**
 * The PaymentTest_03 class used to test the payment document generation and reactivation with
 * write-off option.
 */
public class PaymentTest_03 extends OBBaseTest {

  private static final Logger log = LogManager.getLogger();

  private static final String MANUAL_EXECUTION = "M";
  private static final String CLEARED_ACCOUNT = "CLE";
  private static final String IN_TRANSIT_ACCOUNT = "INT";
  private static final String WITHDRAWN_ACCOUNT = "WIT";
  private static final String DEPOSIT_ACCOUNT = "DEP";
  private static final String CASH = "C";
  private static final String STANDARD_DESCRIPTION = "JUnit Test Payment_03";

  private String financialAccountId;

  /**
   * Initial Set up.
   * 
   * This before method is named setUpP03() to avoid overwriting the super setUp method that is
   * invoke automatically before this one.
   * 
   */
  @Before
  public void setUpP03() throws Exception {
    TestUtility.setTestContext();
  }

  @Test
  public void testRunPayment_03() {
    boolean exception = false;
    Invoice invoice = null;
    FIN_Payment payment = null;

    try {

      // DATA SETUP
      invoice = dataSetup();

      // PAY PARTIALLY THE INVOICE
      invoice = OBDal.getInstance().get(Invoice.class, invoice.getId());
      BigDecimal writeOffAmt = new BigDecimal("2.0");
      BigDecimal paymentAmount = invoice.getGrandTotalAmount().subtract(writeOffAmt);
      payment = TestUtility.addPaymentFromInvoice(invoice,
          OBDal.getInstance().get(FIN_FinancialAccount.class, financialAccountId), paymentAmount,
          true);

      // PROCESS THE PAYMENT
      TestUtility.processPayment(payment, "P");

      // CHECK OUTPUT DATA
      OBContext.setAdminMode();
      try {
        FIN_PaymentScheduleDetail psd = TestUtility.getOneInstance(FIN_PaymentScheduleDetail.class,
            new Value(FIN_PaymentScheduleDetail.PROPERTY_INVOICEPAYMENTSCHEDULE,
                invoice.getFINPaymentScheduleList().get(0)));

        assertTrue("Payment Schedule Outstanding Amount != 0",
            invoice.getFINPaymentScheduleList()
                .get(0)
                .getOutstandingAmount()
                .compareTo(psd.getAmount()) != 0);

        assertTrue("Payment Schedule Received Amount == Total Amount", invoice.getGrandTotalAmount()
            .compareTo(invoice.getFINPaymentScheduleList().get(0).getPaidAmount()) == 0);

        assertTrue("Payment Schedule Detail Amount != Total Amount",
            invoice.getGrandTotalAmount().compareTo(psd.getAmount()) != 0);

        assertTrue("Payment Schedule Detail Write-off Amount != 0",
            BigDecimal.ZERO.compareTo(psd.getWriteoffAmount()) != 0);

        assertTrue("Payment Amount != Total Amount",
            invoice.getGrandTotalAmount().compareTo(payment.getAmount()) != 0);
        assertTrue("Status == Payment Received", "RPR".equals(payment.getStatus()));
      } finally {
        OBContext.restorePreviousMode();
      }

      // REACTIVATE
      TestUtility.processPayment(payment, "R");

      // CHECK OUTPUT DATA AFTER REACTIVATION
      OBContext.setAdminMode();
      try {
        FIN_PaymentScheduleDetail psd = TestUtility.getOneInstance(FIN_PaymentScheduleDetail.class,
            new Value(FIN_PaymentScheduleDetail.PROPERTY_INVOICEPAYMENTSCHEDULE,
                invoice.getFINPaymentScheduleList().get(0)));

        assertTrue("Expected Amount != Total Amount", invoice.getGrandTotalAmount()
            .compareTo(invoice.getFINPaymentScheduleList().get(0).getAmount()) == 0);
        assertTrue("Outstanding Amount != Total Amount", invoice.getGrandTotalAmount()
            .compareTo(invoice.getFINPaymentScheduleList().get(0).getOutstandingAmount()) == 0);
        assertTrue("Received Amount != 0", BigDecimal.ZERO
            .compareTo(invoice.getFINPaymentScheduleList().get(0).getPaidAmount()) == 0);

        assertTrue("Payment Schedule Detail Amount != 0",
            invoice.getGrandTotalAmount().compareTo(psd.getAmount()) == 0);
        assertTrue("Payment Schedule Detail Write-off Amount == 0",
            BigDecimal.ZERO.compareTo(psd.getWriteoffAmount()) == 0);

        assertTrue("Payment Amount != 0", BigDecimal.ZERO.compareTo(payment.getAmount()) == 0);
        assertTrue("Status != Awaiting Payment", "RPAP".equals(payment.getStatus()));

        assertTrue("There are Payment Lines for this payment",
            TestUtility.getOneInstance(FIN_PaymentDetail.class,
                new Value(FIN_PaymentDetail.PROPERTY_FINPAYMENT, payment)) == null);
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
    String bpartnerId = "2C4C71BC828B47A0AF2A79855FD3BA7A"; // Sleep Well Hotels, Co.
    String priceListId = "8366EAF1EDF442A98377D74A199084A8"; // General Sales
    String paymentTermId = "66BA1164A7394344BB9CD1A6ECEED05D"; // 30 days
    String currencyId = EURO_ID;
    String productId = "34560A057833457D962F7A573F76F5BB"; // Ale Beer
    String taxId = "3CCDACCCF02C4D209174159A8AF43127"; // NY Sales Tax
    String docTypeId = "61D7AC2360F0417C80237B5D2131BACD"; // AR Invoice
    BigDecimal invoicedQuantity = new BigDecimal("5");
    BigDecimal netUnitPrice = new BigDecimal("2.04");
    BigDecimal netListPrice = new BigDecimal("2.04");
    BigDecimal lineNetAmount = new BigDecimal("10.20");
    BigDecimal priceLimit = new BigDecimal("1");

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

    FIN_FinancialAccount testAccount = TestUtility.insertFinancialAccount("APRM_FINACC_PAYMENT_01",
        STANDARD_DESCRIPTION, testCurrency, CASH, false,
        getOneInstance(org.openbravo.model.common.geography.Location.class), testBusinessPartner,
        null, null, null, null, null, null, null, null, null, BigDecimal.ZERO, BigDecimal.ZERO,
        null, true, true);

    FIN_PaymentMethod testPaymentMethod = TestUtility.insertPaymentMethod("APRM_PM_PAYMENT_01",
        STANDARD_DESCRIPTION, true, false, false, MANUAL_EXECUTION, null, false, IN_TRANSIT_ACCOUNT,
        DEPOSIT_ACCOUNT, CLEARED_ACCOUNT, true, false, false, MANUAL_EXECUTION, null, false,
        IN_TRANSIT_ACCOUNT, WITHDRAWN_ACCOUNT, CLEARED_ACCOUNT, true, true);

    FinAccPaymentMethod existAssociation = TestUtility.getOneInstance(FinAccPaymentMethod.class,
        new Value(FinAccPaymentMethod.PROPERTY_ACCOUNT, testAccount),
        new Value(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, testPaymentMethod));

    if (existAssociation == null) {
      TestUtility.associatePaymentMethod(testAccount, testPaymentMethod);
    }
    this.financialAccountId = testAccount.getId();

    Invoice invoice = TestUtility.createNewInvoice(OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), new Date(), new Date(), new Date(),
        testDocumentType, testBusinessPartner, location, testPriceList, testCurrency,
        testPaymentMethod, testPaymentTerm, testProduct, uom, invoicedQuantity, netUnitPrice,
        netListPrice, priceLimit, testTaxRate, lineNetAmount, true);

    TestUtility.processInvoice(invoice);

    return invoice;
  }
}
