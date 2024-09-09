package org.openbravo.test.invoice;

import static org.junit.Assert.assertFalse;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.advpaymentmngt.test.TestUtility;
import org.openbravo.advpaymentmngt.test.Value;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.financial.ResetAccounting;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.test.base.Issue;

/**
 * This class is used to test the invoice cloning process in the system.
 * It extends the WeldBaseTest class, which provides a base for all tests that use the Weld container.
 * The class contains several methods to set up the data, clone the invoice, check the output data, and clean up the data.
 * It also contains methods to delete an invoice and the payment configuration, and to reactivate an invoice.
 * The class uses the Apache Commons Lang library's DateUtils class to truncate dates to the day and compare them with the original dates.
 * It also uses the TestUtility class to perform various operations such as creating an invoice, processing an invoice, setting up the test context, and associating a payment method with a financial account.
 * The class uses the OBDal class to interact with the database.
 * It also uses the VariablesSecureApp class to set session values for the format output, group separator, and decimal separator for the general quantity edition.
 * The class is annotated with @Test, meaning it is a test class.
 */
public class CloneInvoiceTest extends WeldBaseTest {

  private static final Logger log = LogManager.getLogger();
  private static final String PRICELIST_ID = "8366EAF1EDF442A98377D74A199084A8"; // General Sales
  private static final String BPARTNER_ID = "2C4C71BC828B47A0AF2A79855FD3BA7A"; // Sleep Well Hotels, Co.
  private static final String PAYMENT_TERM_ID = "66BA1164A7394344BB9CD1A6ECEED05D"; // 30 days
  private static final String PRODUCT_ID = "34560A057833457D962F7A573F76F5BB"; // Ale Beer
  private static final String TAXRATE_ID = "3CCDACCCF02C4D209174159A8AF43127"; // NY Sales Tax
  private static final String DOCTYPE_ID = "61D7AC2360F0417C80237B5D2131BACD"; // AR Invoice
  private static final String PAYMENT_METHOD_ID = "1ECC7ADB9EA2442FA4E4DA566AFD806D"; // Cash
  private String financialAccountId;

  /**
   * This method is used to set up the context for the invoice cloning test.
   * It is annotated with @Before, meaning it is run before each test method in this class.
   * <p>
   * It first calls the setUp method of the superclass to perform any setup operations defined there.
   * Then it calls the TestUtility.setTestContext method to set up the test context.
   * <p>
   * A VariablesSecureApp object is created with the IDs of the current user, client, organization, and role.
   * The RequestContext's VariableSecureApp is then set to this object.
   * <p>
   * The session values for the format output, group separator, and decimal separator for the general quantity edition are set.
   * These values are used to format the output of quantities in the system.
   *
   * @throws Exception
   *     If there is an error during the setup.
   */
  @Before
  public void setUpCloneInvoice() throws Exception {
    super.setUp();
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
   * This method is used to test the invoice cloning process.
   * It first sets up the data by creating an invoice.
   * Then it clones the invoice.
   * It checks if the invoice date and accounting date of the cloned invoice are not truncated to the day.
   * If they are not, it sets the exception flag to true and sets the error message.
   * It then cleans up the data by deleting the original and cloned invoices.
   * If any exception occurs during the process, it logs the error message and sets the exception flag to true.
   * Finally, it asserts that no exception occurred during the process.
   *
   * @throws AssertionError
   *     If there is an error during the test.
   */
  @Test
  @Issue("#345")
  public void testRunCloneInvoice() throws AssertionError {
    // Given
    boolean exception = false;
    String errorMessage = "";
    try {
      // DATA SETUP
      Invoice invoice = dataSetup();

      // CLONE THE INVOICE
      Invoice cloneInvoice = cloneInvoice(invoice);
      // When
      if (isNotTruncateDate(cloneInvoice.getInvoiceDate())) {
        exception = true;
        errorMessage = "Invalid Invoice Date cloned.";
      }
      if (!exception && isNotTruncateDate(cloneInvoice.getAccountingDate())) {
        exception = true;
        errorMessage = "Invalid Accounting Date cloned.";
      }
      dataClean(invoice.getId(), cloneInvoice.getId());
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      exception = true;
    }

    assertFalse(errorMessage, exception);
  }

  /**
   * This method checks if the provided date is not truncated to the day.
   * It uses the Apache Commons Lang library's DateUtils class to truncate the date to the day and compare it with the original date.
   * If the original date and the truncated date are not the same instant, it means that the original date was not truncated to the day.
   * This method was created to address issue #345.
   *
   * @param dateToCheck
   *     The Date object to be checked.
   * @return boolean Returns true if the date is not truncated to the day, false otherwise.
   */
  private boolean isNotTruncateDate(Date dateToCheck) {
    return !DateUtils.isSameInstant(dateToCheck, DateUtils.truncate(dateToCheck, Calendar.DATE));
  }

  /**
   * This method is used to clone an Invoice in the system.
   * It first retrieves the Invoice using its ID.
   * A list of BaseOBObject is created and the retrieved invoice is added to it.
   * The CallCloneInvoice.callCloneRecordsJob method is then called with the list of invoices and the Invoice class as parameters.
   * The ID of the cloned invoice is retrieved from the callCloneRecordsJob method.
   * The cloned invoice is then retrieved using its ID.
   * If the original invoice is processed and the document status of the cloned invoice is "DR" (Draft) and the document action is "CO" (Complete),
   * the cloned invoice is processed using the TestUtility.processInvoice method.
   * The cloned invoice is then refreshed to ensure that any changes made during the process are reflected in the object.
   *
   * @param invoice
   *     The Invoice object to be cloned.
   * @return Invoice The cloned Invoice object.
   * @throws Exception
   *     If there is an error during the cloning of the Invoice.
   */
  private Invoice cloneInvoice(Invoice invoice) throws Exception {
    invoice = OBDal.getInstance().get(Invoice.class, invoice.getId());

    List<BaseOBObject> invoices = new LinkedList<>();
    invoices.add(invoice);

    String clonedInvoiceId = CallCloneInvoice.callCloneRecordsJob(invoices, Invoice.class);
    Invoice clonedInvoice = OBDal.getInstance().get(Invoice.class, clonedInvoiceId);

    if (invoice.isProcessed() && StringUtils.equals("DR", clonedInvoice.getDocumentStatus())
        && StringUtils.equals("CO", clonedInvoice.getDocumentAction())) {
      TestUtility.processInvoice(clonedInvoice);
    }
    OBDal.getInstance().refresh(clonedInvoice);
    return clonedInvoice;
  }

  /**
   * This method is used to set up the data for the invoice cloning test.
   * It creates an invoice with specific details and processes it.
   * <p>
   * It first creates several BigDecimal objects to represent the invoiced quantity, net unit price, net list price, line net amount, and price limit.
   * Then it retrieves several objects from the database using their IDs, including the price list, business partner, location, payment term, currency, product, tax rate, document type, and payment method.
   * It also creates a financial account and associates it with the payment method if it is not already associated.
   * The ID of the financial account is stored in the financialAccountId field.
   * An invoice is then created using the retrieved and created data, and it is processed.
   *
   * @return Invoice The created and processed invoice.
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
    TaxRate testTaxRate = OBDal.getInstance().get(TaxRate.class, TAXRATE_ID);
    DocumentType testDocumentType = OBDal.getInstance().get(DocumentType.class, DOCTYPE_ID);
    FIN_PaymentMethod testPaymentMethod = OBDal.getInstance().get(FIN_PaymentMethod.class, PAYMENT_METHOD_ID);
    FIN_FinancialAccount testAccount = TestUtility.insertFinancialAccount("APRM_CLONE_INVOICE_FINACC",
        "JUnit Test Reverse Payment", testCurrency, "C", false,
        getOneInstance(org.openbravo.model.common.geography.Location.class), testBusinessPartner,
        null, null, null, null, null, null, null, null, null, BigDecimal.ZERO, BigDecimal.ZERO,
        null, true, true);
    FinAccPaymentMethod existAssociation = TestUtility.getOneInstance(FinAccPaymentMethod.class,
        new Value(FinAccPaymentMethod.PROPERTY_ACCOUNT, testAccount),
        new Value(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, testPaymentMethod));

    if (existAssociation == null) {
      TestUtility.associatePaymentMethod(testAccount, testPaymentMethod);
    }
    if (testAccount != null) {
      this.financialAccountId = testAccount.getId();
    }
    Invoice invoice = TestUtility.createInvoice(OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), new Date(), new Date(), new Date(),
        testDocumentType, testBusinessPartner, location, testPriceList, testCurrency,
        testPaymentMethod, testPaymentTerm, testProduct, testProduct.getUOM(), invoicedQuantity, netUnitPrice,
        netListPrice, priceLimit, testTaxRate, lineNetAmount, true);

    TestUtility.processInvoice(invoice);

    return invoice;
  }

  /**
   * This method is used to clean up the data after the invoice cloning test.
   * It deletes the original invoice, the cloned invoice, and the payment configuration.
   * <p>
   * It first calls the deleteInvoice method with the ID of the original invoice.
   * Then it calls the deleteInvoice method again with the ID of the cloned invoice.
   * Finally, it calls the deletePaymentConfiguration method to delete the payment configuration.
   *
   * @param invoiceId
   *     The ID of the original Invoice to be deleted.
   * @param clonedInvoiceId
   *     The ID of the cloned Invoice to be deleted.
   */
  private void dataClean(String invoiceId, String clonedInvoiceId) {
    deleteInvoice(invoiceId);
    deleteInvoice(clonedInvoiceId);
    deletePaymentConfiguration();
  }

  /**
   * This method is used to reactivate an Invoice in the system.
   * It first sets the document status of the invoice to "CO" (Complete),
   * the document action to "RE" (Reactivate), and the posted status to "N" (No).
   * Then it flushes the changes to the database.
   * Finally, it calls the TestUtility.processInvoice method to process the invoice.
   *
   * @param invoice
   *     The Invoice object to be reactivated.
   * @throws OBException
   *     If there is an error during the reactivation of the Invoice.
   */
  private void reactivateInvoice(Invoice invoice) {
    try {
      invoice.setDocumentStatus("CO");
      invoice.setDocumentAction("RE");
      invoice.setPosted("N");
      OBDal.getInstance().flush();
      TestUtility.processInvoice(invoice);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * This method is used to delete an Invoice in the system.
   * It first retrieves the Invoice using its ID.
   * If the Invoice exists, it calls the ResetAccounting.delete method to delete the accounting entries related to the Invoice.
   * The Invoice is then refreshed and if it is processed, it is reactivated.
   * All changes are flushed to the database and the transaction is committed.
   * The Invoice is then retrieved again using its ID and removed from the database.
   * All changes are flushed to the database and the transaction is committed.
   * If any exception occurs during the process, it is caught and rethrown as an OBException.
   *
   * @param invoiceId
   *     The ID of the Invoice to be deleted.
   * @throws OBException
   *     if any exception occurs during the process.
   */
  private void deleteInvoice(String invoiceId) {
    try {
      Invoice invoice = OBDal.getInstance().get(Invoice.class, invoiceId);
      if (invoice != null) {
        ResetAccounting.delete(invoice.getClient().getId(),
            invoice.getOrganization().getId(),
            invoice.getEntity().getTableId(), invoice.getId(), OBDateUtils.formatDate(invoice.getAccountingDate()),
            null);

        OBDal.getInstance().refresh(invoice);
        if (invoice.isProcessed()) {
          reactivateInvoice(invoice);
        }
        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();

        invoice = OBDal.getInstance().get(Invoice.class, invoiceId);
        OBDal.getInstance().remove(invoice);
        OBDal.getInstance().commitAndClose();
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * This method is used to delete the payment configuration.
   * It first retrieves the payment method and financial account using their respective IDs.
   * If the financial account exists, it retrieves the associated payment method in financial account.
   * If the associated payment method in financial account exists, it is removed.
   * The financial account is then removed.
   * All changes are flushed to the database and the transaction is committed.
   * If any exception occurs during the process, it is caught and rethrown as an OBException.
   *
   * @throws OBException
   *     if any exception occurs during the process.
   */
  private void deletePaymentConfiguration() {
    try {
      FIN_PaymentMethod paymentMethod = OBDal.getInstance().get(FIN_PaymentMethod.class, PAYMENT_METHOD_ID);
      FIN_FinancialAccount financialAccount = OBDal.getInstance().get(FIN_FinancialAccount.class, financialAccountId);
      if (financialAccount != null) {
        FinAccPaymentMethod finAccPaymentMethod = TestUtility.getOneInstance(FinAccPaymentMethod.class,
            new Value(FinAccPaymentMethod.PROPERTY_ACCOUNT, financialAccount),
            new Value(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, paymentMethod));
        if (finAccPaymentMethod != null) {
          OBDal.getInstance().remove(finAccPaymentMethod);
        }
        OBDal.getInstance().remove(financialAccount);
        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }
}
