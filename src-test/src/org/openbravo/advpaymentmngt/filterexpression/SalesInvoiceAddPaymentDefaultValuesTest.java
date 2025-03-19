package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.test.base.TestConstants;

/**
 * Unit tests for the SalesInvoiceAddPaymentDefaultValues class.
 */
public class SalesInvoiceAddPaymentDefaultValuesTest extends OBBaseTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @InjectMocks
  private SalesInvoiceAddPaymentDefaultValues classUnderTest;
  @Mock
  private Invoice mockInvoice;
  @Mock
  private BusinessPartner mockBusinessPartner;
  @Mock
  private Currency mockCurrency;
  @Mock
  private Organization mockOrganization;
  private MockedStatic<OBDal> mockedOBDal;
  private AutoCloseable mocks;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    // Initialize mocks
    mocks = MockitoAnnotations.openMocks(this);

    // Set up OBContext
    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN, TestConstants.Clients.FB_GRP,
        TestConstants.Orgs.ESP_NORTE);

    // Set up static mocks
    mockedOBDal = mockStatic(OBDal.class);
    OBDal mockOBDal = mock(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    // Configure mock invoice
    when(mockInvoice.getBusinessPartner()).thenReturn(mockBusinessPartner);
    when(mockInvoice.getCurrency()).thenReturn(mockCurrency);
    when(mockInvoice.getOrganization()).thenReturn(mockOrganization);
    when(mockBusinessPartner.getId()).thenReturn(org.openbravo.advpaymentmngt.TestConstants.BUSINESS_PARTNER_ID);
    when(mockCurrency.getId()).thenReturn(org.openbravo.advpaymentmngt.TestConstants.CURRENCY_ID);
    when(mockCurrency.getStandardPrecision()).thenReturn(2L);
    when(mockOrganization.getId()).thenReturn(org.openbravo.advpaymentmngt.TestConstants.ORGANIZATION_ID);

    // Configure OBDal mock to return our mock invoice
    when(mockOBDal.get(Invoice.class, org.openbravo.advpaymentmngt.TestConstants.INVOICE_ID)).thenReturn(mockInvoice);
  }

  /**
   * Cleans up the test environment after each test.
   *
   * @throws Exception
   *     if an error occurs during teardown
   */
  @After
  public void tearDown() throws Exception {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Tests the getDefaultReceivedFrom method for successful execution.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetDefaultReceivedFromSuccess() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(org.openbravo.advpaymentmngt.TestConstants.INPC_INVOICE_ID, org.openbravo.advpaymentmngt.TestConstants.INVOICE_ID);
    requestMap.put(org.openbravo.advpaymentmngt.TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultReceivedFrom(requestMap);

    // Then
    assertEquals("Should return the business partner ID", org.openbravo.advpaymentmngt.TestConstants.BUSINESS_PARTNER_ID, result);
  }

  /**
   * Tests the getDefaultCurrency method for successful execution.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetDefaultCurrencySuccess() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(org.openbravo.advpaymentmngt.TestConstants.INPC_INVOICE_ID, org.openbravo.advpaymentmngt.TestConstants.INVOICE_ID);
    requestMap.put(org.openbravo.advpaymentmngt.TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultCurrency(requestMap);

    // Then
    assertEquals("Should return the currency ID", org.openbravo.advpaymentmngt.TestConstants.CURRENCY_ID, result);
  }

  /**
   * Tests the getDefaultStandardPrecision method for successful execution.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetDefaultStandardPrecisionSuccess() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(org.openbravo.advpaymentmngt.TestConstants.INPC_INVOICE_ID, org.openbravo.advpaymentmngt.TestConstants.INVOICE_ID);
    requestMap.put(org.openbravo.advpaymentmngt.TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultStandardPrecision(requestMap);

    // Then
    assertEquals("Should return the standard precision", "2", result);
  }

  /**
   * Tests the getOrganization method for successful execution.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetOrganizationSuccess() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(org.openbravo.advpaymentmngt.TestConstants.INPC_INVOICE_ID, org.openbravo.advpaymentmngt.TestConstants.INVOICE_ID);
    requestMap.put(org.openbravo.advpaymentmngt.TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getOrganization(requestMap);

    // Then
    assertEquals("Should return the organization ID", org.openbravo.advpaymentmngt.TestConstants.ORGANIZATION_ID,
        result);
  }

  /**
   * Tests the getDefaultIsSOTrx method for successful execution.
   */
  @Test
  public void testGetDefaultIsSOTrxSuccess() {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    String result = classUnderTest.getDefaultIsSOTrx(requestMap);

    // Then
    assertEquals("Should return Y for sales transaction", "Y", result);
  }

  /**
   * Tests the getDefaultTransactionType method for successful execution.
   */
  @Test
  public void testGetDefaultTransactionTypeSuccess() {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    String result = classUnderTest.getDefaultTransactionType(requestMap);

    // Then
    assertEquals("Should return I for invoice transaction", "I", result);
  }

  /**
   * Tests the getDefaultExpectedAmount method for successful execution.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetPendingAmountSuccess() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(org.openbravo.advpaymentmngt.TestConstants.INPC_INVOICE_ID, org.openbravo.advpaymentmngt.TestConstants.INVOICE_ID);
    requestMap.put(org.openbravo.advpaymentmngt.TestConstants.CONTEXT, context.toString());

    List<FIN_PaymentSchedule> schedules = new ArrayList<>();
    when(mockInvoice.getFINPaymentScheduleList()).thenReturn(schedules);

    // When
    String result = classUnderTest.getDefaultExpectedAmount(requestMap);

    // Then
    assertNotNull("Should return a non-null amount", result);
    assertEquals("Should return 0 for empty schedule", "0", result);
  }

  /**
   * Tests the getDefaultPaymentDate method for successful execution.
   */
  @Test
  public void testGetDefaultPaymentDateSuccess() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    String result = classUnderTest.getDefaultPaymentDate(requestMap);

    // Then
    assertNotNull("Should return a non-null date", result);
  }
}
