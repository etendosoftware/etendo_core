package org.openbravo.advpaymentmngt.test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.openbravo.test.costing.utils.TestCostingConstants.EURO_ID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.KernelInitializer;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalLayerInitializer;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.financial.ResetAccounting;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.invoice.Invoice;
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

/**
 * This class is used to test the reverse payment functionality in the system.
 * <p>
 * It includes methods to set up the data for an Invoice, pay the invoice completely, create a reverse payment for the processed payment,
 * process the reverse payment, check if all the payment schedule details of the invoice are not paid, and clean up the data related to the invoice and its payments.
 * It also includes methods to delete a Payment, reactivate an Invoice, delete an Invoice, and delete the payment configuration in the system.
 */
@EnableWeld
@ExtendWith(WeldJunit5Extension.class)
public class ReversePaymentTest {

  @Inject
  BeanManager beanManager;

  @Inject
  WeldUtils weldUtils;

  @Inject
  KernelInitializer kernelInitializer;

  private static boolean dalInitialized = false;
  private static final Logger log = LogManager.getLogger();
  private static final String MANUAL_EXECUTION = "M";
  private static final String AUTOMATIC_EXECUTION = "A";
  private static final String CLEARED_ACCOUNT = "CLE";
  private static final String IN_TRANSIT_ACCOUNT = "INT";
  private static final String WITHDRAWN_ACCOUNT = "WIT";
  private static final String DEPOSIT_ACCOUNT = "DEP";
  private static final String CASH = "C";
  private static final String STANDARD_DESCRIPTION = "JUnit Test Reverse Payment";
  private static final String REVERSE_PAYMENT_METHOD_NAME = "APRM_REVERSE_PAYMENT_METHOD";
  private String financialAccountId;
  private static final String REVERSE_FINACC_NAME = "APRM_REVERSE_PAYMENT_FINACC";
  private static final String BPARTNER_ID = "2C4C71BC828B47A0AF2A79855FD3BA7A"; // Sleep Well Hotels, Co.
  private static final String PRICELIST_ID = "8366EAF1EDF442A98377D74A199084A8"; // General Sales
  private static final String PAYMENT_TERM_ID = "66BA1164A7394344BB9CD1A6ECEED05D"; // 30 days
  private static final String PRODUCT_ID = "34560A057833457D962F7A573F76F5BB"; // Ale Beer
  private static final String TAXRATE_ID = "3CCDACCCF02C4D209174159A8AF43127"; // NY Sales Tax
  private static final String DOCTYPE_ID = "61D7AC2360F0417C80237B5D2131BACD"; // AR Invoice
  private static final String EXECUTION_PROCESS_ID = "301950D5D2F24F49916EDE06A473DF02"; // Simple Execution Process

  /**
   * Initial Set up.
   * <p>
   * This before method is named setUpRP() to avoid overwriting the super setUp method that is
   * invoke automatically before this one.
   */
  @BeforeEach
  void setUpRP() {
    if (!dalInitialized) {
      DalLayerInitializer.getInstance().setInitialized(false);
      DalLayerInitializer.getInstance().initialize(true);

      WeldUtils.setStaticInstanceBeanManager(beanManager);
      kernelInitializer.setInterceptor();
      weldUtils.setBeanManager(beanManager);

      dalInitialized = true;
    }

    TestUtility.setTestContext();
    VariablesSecureApp vsa = new VariablesSecureApp(
        OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId(),
        OBContext.getOBContext().getRole().getId()
    );
    RequestContext.get().setVariableSecureApp(vsa);
    vsa.setSessionValue("#FormatOutput|generalQtyEdition", "#0.######");
    vsa.setSessionValue("#GroupSeparator|generalQtyEdition", ",");
    vsa.setSessionValue("#DecimalSeparator|generalQtyEdition", ".");
  }


  /**
   * This test method is used to test the reverse payment functionality in the system.
   * <p>
   * It first sets up the data for an Invoice and saves it in the database.
   * Then it pays the invoice completely and processes the payment.
   * After that, it creates a reverse payment for the processed payment and processes the reverse payment.
   * Then it retrieves the invoice from the database and checks if all the payment schedule details of the invoice are not paid.
   * If any of the payment schedule details is paid, it sets the exception flag to true.
   * After that, it cleans up the data related to the invoice and its payments.
   * Finally, it asserts that the exception flag is false, which means that the test has passed.
   *
   * @throws Exception
   *     If there is an error during the test.
   */
  @Test
  public void testRunReversePayment() {
    // Given
    boolean exception = false;
    try {
      // DATA SETUP
      Invoice invoice = dataSetup();

      // PAY COMPLETELY THE INVOICE
      invoice = OBDal.getInstance().get(Invoice.class, invoice.getId());
      FIN_Payment payment = TestUtility.addPaymentFromInvoice(invoice,
          OBDal.getInstance().get(FIN_FinancialAccount.class, financialAccountId),
          invoice.getGrandTotalAmount(), false);

      OBDal.getInstance().save(invoice);
      OBDal.getInstance().save(payment);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(invoice);
      OBDal.getInstance().refresh(payment);

      // PROCESS THE PAYMENT
      TestUtility.processPayment(payment, "P");

      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      invoice = OBDal.getInstance().get(Invoice.class, invoice.getId());
      payment = OBDal.getInstance().get(FIN_Payment.class, payment.getId());

      FIN_Payment reversePayment = TestUtility.createReversePayment(payment);
      TestUtility.processPayment(reversePayment, "P");
      // CHECK OUTPUT DATA

      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      invoice = OBDal.getInstance().get(Invoice.class, invoice.getId());

      // When
      AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
      List<FIN_PaymentScheduleDetail> scheduleDetails = dao
          .getInvoicePendingScheduledPaymentDetails(invoice);

      for (FIN_PaymentScheduleDetail psd : scheduleDetails) {
        if (psd.isInvoicePaid()) {
          exception = true;
          break;
        }
      }

      dataClean(invoice.getId(), payment.getId(), reversePayment.getId());
    } catch (Exception e) {
      log.error(FIN_Utility.getExceptionMessage(e), e);
      exception = true;
    }

    assertFalse(exception);
  }

  /**
   * This method is used to set up the data for an Invoice in the system.
   * <p>
   * It first creates several BigDecimal objects representing the invoiced quantity, net unit price, net list price, line net amount, and price limit.
   * Then it retrieves several objects from the database, including a PriceList, BusinessPartner, Location, PaymentTerm, Currency, Product, UOM, TaxRate, and DocumentType.
   * It also creates a new FIN_FinancialAccount and associates it with a new FIN_PaymentMethod.
   * If the association between the financial account and the payment method does not exist, it creates one.
   * After that, it creates a new Invoice with the retrieved and created data, and processes the invoice.
   * Finally, it returns the created Invoice.
   *
   * @return Invoice The created Invoice object.
   * @throws Exception
   *     If there is an error during the data setup.
   */
  private Invoice dataSetup() throws Exception {
    // DATA SETUP
    BigDecimal invoicedQuantity = new BigDecimal("5");
    BigDecimal netUnitPrice = new BigDecimal("2.04");
    BigDecimal netListPrice = new BigDecimal("2.04");
    BigDecimal lineNetAmount = new BigDecimal("10.20");
    BigDecimal priceLimit = new BigDecimal("1");

    PriceList testPriceList = OBDal.getInstance().get(PriceList.class, PRICELIST_ID);
    BusinessPartner testBusinessPartner = OBDal.getInstance()
        .get(BusinessPartner.class, BPARTNER_ID);
    Location location = TestUtility.getOneInstance(Location.class,
        new Value(Location.PROPERTY_BUSINESSPARTNER, testBusinessPartner));
    PaymentTerm testPaymentTerm = OBDal.getInstance().get(PaymentTerm.class, PAYMENT_TERM_ID);
    Currency testCurrency = OBDal.getInstance().get(Currency.class, EURO_ID);
    Product testProduct = OBDal.getInstance().get(Product.class, PRODUCT_ID);
    UOM uom = TestUtility.getOneInstance(UOM.class,
        new Value(UOM.PROPERTY_NAME, testProduct.getUOM().getName()));
    TaxRate testTaxRate = OBDal.getInstance().get(TaxRate.class, TAXRATE_ID);
    DocumentType testDocumentType = OBDal.getInstance().get(DocumentType.class, DOCTYPE_ID);

    FIN_FinancialAccount testAccount = TestUtility.insertFinancialAccount(REVERSE_FINACC_NAME,
        STANDARD_DESCRIPTION, testCurrency, CASH, false,
        TestUtility.getOneInstance(org.openbravo.model.common.geography.Location.class), testBusinessPartner,
        null, null, null, null, null, null, null, null, null, BigDecimal.ZERO, BigDecimal.ZERO,
        null, true, true);

    PaymentExecutionProcess executionProcess = TestUtility.getOneInstance(
        PaymentExecutionProcess.class, new Value(PaymentExecutionProcess.PROPERTY_ID, EXECUTION_PROCESS_ID));

    FIN_PaymentMethod testPaymentMethod = TestUtility.insertPaymentMethod(REVERSE_PAYMENT_METHOD_NAME,
        STANDARD_DESCRIPTION, true, false, false, AUTOMATIC_EXECUTION, executionProcess, false, IN_TRANSIT_ACCOUNT,
        DEPOSIT_ACCOUNT, CLEARED_ACCOUNT, true, false, false, MANUAL_EXECUTION, null, false,
        IN_TRANSIT_ACCOUNT, WITHDRAWN_ACCOUNT, CLEARED_ACCOUNT, true, true);

    FinAccPaymentMethod existAssociation = TestUtility.getOneInstance(FinAccPaymentMethod.class,
        new Value(FinAccPaymentMethod.PROPERTY_ACCOUNT, testAccount),
        new Value(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, testPaymentMethod));

    if (existAssociation == null) {
      TestUtility.associatePaymentMethod(testAccount, testPaymentMethod);
    }
    this.financialAccountId = testAccount.getId();

    Invoice invoice = TestUtility.createInvoice(OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), new Date(), new Date(), new Date(),
        testDocumentType, testBusinessPartner, location, testPriceList, testCurrency,
        testPaymentMethod, testPaymentTerm, testProduct, uom, invoicedQuantity, netUnitPrice,
        netListPrice, priceLimit, testTaxRate, lineNetAmount, true);

    TestUtility.processInvoice(invoice);

    return invoice;
  }

  /**
   * This method is used to clean up the data related to an invoice and its payments in the system.
   * <p>
   * It first deletes the reverse payment associated with the invoice using the provided reversePaymentId.
   * Then it deletes the original payment associated with the invoice using the provided paymentId.
   * After that, it deletes the invoice itself using the provided invoiceId.
   * Finally, it deletes the payment configuration associated with the invoice.
   *
   * @param invoiceId
   *     The String representing the ID of the Invoice to be deleted.
   * @param paymentId
   *     The String representing the ID of the Payment to be deleted.
   * @param reversePaymentId
   *     The String representing the ID of the Reverse Payment to be deleted.
   */
  private void dataClean(String invoiceId, String paymentId, String reversePaymentId) {
    deletePayment(reversePaymentId);
    deletePayment(paymentId);
    deleteInvoice(invoiceId);
    deletePaymentConfiguration();
  }

  /**
   * This method is used to delete a Payment in the system.
   * <p>
   * It first retrieves the FIN_Payment object with the provided paymentId.
   * Then it calls the ResetAccounting.delete method to delete the accounting entries related to the payment.
   * After that, it processes the payment with the status "R" (Reversed) and flushes the changes to the database.
   * Then it retrieves the FIN_Payment object again and removes it from the database.
   * Finally, it commits the changes and closes the session.
   *
   * @param paymentId
   *     The String representing the ID of the Payment to be deleted.
   * @throws OBException
   *     If there is an error during the deletion of the Payment.
   */
  private void deletePayment(String paymentId) {
    try {
      FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, paymentId);
      ResetAccounting.delete(payment.getClient().getId(),
          payment.getOrganization().getId(),
          payment.getEntity().getTableId(), paymentId, OBDateUtils.formatDate(payment.getPaymentDate()), null);
      TestUtility.processPayment(payment, "R");
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

      payment = OBDal.getInstance().get(FIN_Payment.class, paymentId);
      OBDal.getInstance().remove(payment);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * This method is used to reactivate an Invoice in the system.
   * <p>
   * It first sets the document status of the invoice to "CO" (Complete),
   * the document action to "RE" (Reactivate), and the posted status to "N" (No).
   * Then it flushes the changes to the database.
   * Finally, it calls the TestUtility.processInvoice method to process the invoice.
   *
   * @param invoice
   *     The Invoice object to be reactivated.
   * @return boolean Returns true if the invoice is processed successfully, false otherwise.
   * @throws OBException
   *     If there is an error during the reactivation of the Invoice.
   */
  private boolean reactivateInvoice(Invoice invoice) {
    try {
      invoice.setDocumentStatus("CO");
      invoice.setDocumentAction("RE");
      invoice.setPosted("N");
      OBDal.getInstance().flush();
      return TestUtility.processInvoice(invoice);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * This method is used to delete an Invoice in the system.
   * <p>
   * It first retrieves the Invoice object with the provided invoiceId.
   * Then it calls the ResetAccounting.delete method to delete the accounting entries related to the invoice.
   * After that, it reactivates the invoice and flushes the changes to the database.
   * Then it retrieves the Invoice object again and removes it from the database.
   * Finally, it flushes the changes and closes the session.
   *
   * @param invoiceId
   *     The String representing the ID of the Invoice to be deleted.
   * @throws OBException
   *     If there is an error during the deletion of the Invoice.
   */
  private void deleteInvoice(String invoiceId) {
    try {
      Invoice invoice = OBDal.getInstance().get(Invoice.class, invoiceId);
      ResetAccounting.delete(invoice.getClient().getId(),
          invoice.getOrganization().getId(),
          invoice.getEntity().getTableId(), invoice.getId(), OBDateUtils.formatDate(invoice.getAccountingDate()), null);
      reactivateInvoice(invoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

      invoice = OBDal.getInstance().get(Invoice.class, invoiceId);
      OBDal.getInstance().remove(invoice);
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * This method is used to delete the payment configuration in the system.
   * <p>
   * It first retrieves the payment method with the name REVERSE_PAYMENT_METHOD_NAME.
   * Then it retrieves the financial account with the id stored in the instance variable financialAccountId.
   * It also retrieves the association between the payment method and the financial account.
   * After that, it removes the association, the financial account, and the payment method from the database.
   * Finally, it flushes the changes and closes the session.
   *
   * @throws OBException
   *     If there is an error during the deletion of the payment configuration.
   */
  private void deletePaymentConfiguration() {
    try {
      final OBCriteria<FIN_PaymentMethod> paymentMethodOBCriteria = OBDal.getInstance()
          .createCriteria(FIN_PaymentMethod.class);
      paymentMethodOBCriteria.addEqual(FIN_PaymentMethod.PROPERTY_NAME, REVERSE_PAYMENT_METHOD_NAME);
      FIN_PaymentMethod paymentMethod = (FIN_PaymentMethod) paymentMethodOBCriteria.setMaxResults(1).uniqueResult();
      FIN_FinancialAccount financialAccount = OBDal.getInstance().get(FIN_FinancialAccount.class, financialAccountId);
      FinAccPaymentMethod finAccPaymentMethod = TestUtility.getOneInstance(FinAccPaymentMethod.class,
          new Value(FinAccPaymentMethod.PROPERTY_ACCOUNT, financialAccount),
          new Value(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, paymentMethod));

      OBDal.getInstance().remove(finAccPaymentMethod);
      OBDal.getInstance().remove(financialAccount);
      OBDal.getInstance().remove(paymentMethod);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }
}
