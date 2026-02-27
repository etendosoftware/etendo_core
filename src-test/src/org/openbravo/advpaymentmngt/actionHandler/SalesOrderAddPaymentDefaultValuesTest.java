package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.openbravo.test.base.mock.MockitoStaticMockUtils.mockStaticSafely;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.advpaymentmngt.filterexpression.SalesOrderAddPaymentDefaultValues;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;

/**
 * Unit tests for the SalesOrderAddPaymentDefaultValues class.
 */
public class SalesOrderAddPaymentDefaultValuesTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @InjectMocks
  private SalesOrderAddPaymentDefaultValues classUnderTest;

  @Mock
  private Order mockOrder;
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
    mocks = MockitoAnnotations.openMocks(this);
    mockedOBDal = mockStaticSafely(OBDal.class);

    OBDal mockOBDal = mock(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    // Setup basic mocks
    when(mockOrder.getBusinessPartner()).thenReturn(mockBusinessPartner);
    when(mockOrder.getCurrency()).thenReturn(mockCurrency);
    when(mockOrder.getOrganization()).thenReturn(mockOrganization);
    when(mockCurrency.getStandardPrecision()).thenReturn(2L);
    when(mockOBDal.get(eq(Order.class), anyString())).thenReturn(mockOrder);

    // Setup empty payment schedule list
    when(mockOrder.getFINPaymentScheduleList()).thenReturn(new ArrayList<FIN_PaymentSchedule>());
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
   * Tests the getDefaultExpectedAmount method for successful execution.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetDefaultExpectedAmountSuccess() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPC_ORDER_ID, TestConstants.TEST_ORDER_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultExpectedAmount(requestMap);

    // Then
    assertNotNull("Expected amount should not be null", result);
    assertEquals("0", result);
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
    assertEquals("Y", result);
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
    assertEquals("O", result);
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
    context.put(TestConstants.INPC_ORDER_ID, TestConstants.TEST_ORDER_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());
    String expectedBPartnerId = "TEST_BP_ID";
    when(mockBusinessPartner.getId()).thenReturn(expectedBPartnerId);

    // When
    String result = classUnderTest.getDefaultReceivedFrom(requestMap);

    // Then
    assertEquals(expectedBPartnerId, result);
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
    context.put(TestConstants.INPC_ORDER_ID, TestConstants.TEST_ORDER_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultStandardPrecision(requestMap);

    // Then
    assertEquals("2", result);
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
    context.put(TestConstants.INPC_ORDER_ID, TestConstants.TEST_ORDER_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());
    String expectedCurrencyId = "TEST_CURRENCY_ID";
    when(mockCurrency.getId()).thenReturn(expectedCurrencyId);

    // When
    String result = classUnderTest.getDefaultCurrency(requestMap);

    // Then
    assertEquals(expectedCurrencyId, result);
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
    context.put(TestConstants.INPC_ORDER_ID, TestConstants.TEST_ORDER_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());
    String expectedOrgId = "TEST_ORG_ID";
    when(mockOrganization.getId()).thenReturn(expectedOrgId);

    // When
    String result = classUnderTest.getOrganization(requestMap);

    // Then
    assertEquals(expectedOrgId, result);
  }

  /**
   * Tests the getDefaultPaymentType method for successful execution.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultPaymentTypeSuccess() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    String result = classUnderTest.getDefaultPaymentType(requestMap);

    // Then
    assertEquals("", result);
  }

  /**
   * Tests the getDefaultDocument method for successful execution.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultDocumentSuccess() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    String result = classUnderTest.getDefaultDocument(requestMap);

    // Then
    assertEquals("", result);
  }

  /**
   * Tests the getDefaultExpectedAmount method with malformed context, expecting a JSONException.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultExpectedAmountMalformedContext() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    requestMap.put(TestConstants.CONTEXT, "{ not a valid json }");

    // Then
    expectedException.expect(JSONException.class);

    // When
    classUnderTest.getDefaultExpectedAmount(requestMap);
    fail("Should have thrown JSONException");
  }

  /**
   * Tests the getDefaultExpectedAmount method with missing order ID, expecting a JSONException.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultExpectedAmountMissingOrderId() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    requestMap.put(TestConstants.CONTEXT, "{}");

    // Then
    expectedException.expect(JSONException.class);

    // When
    classUnderTest.getDefaultExpectedAmount(requestMap);
    fail("Should have thrown JSONException");
  }

  /**
   * Tests the getDefaultReceivedFrom method with a null business partner, expecting a NullPointerException.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetDefaultReceivedFromNullBusinessPartner() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPC_ORDER_ID, TestConstants.TEST_ORDER_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // Set up order with null business partner
    when(mockOrder.getBusinessPartner()).thenReturn(null);

    // Then
    expectedException.expect(NullPointerException.class);

    // When
    classUnderTest.getDefaultReceivedFrom(requestMap);
  }

  /**
   * Tests the getDefaultCurrency method with a null currency, expecting a NullPointerException.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetDefaultCurrencyNullCurrency() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPC_ORDER_ID, TestConstants.TEST_ORDER_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // Set up order with null currency
    when(mockOrder.getCurrency()).thenReturn(null);

    // Then
    expectedException.expect(NullPointerException.class);

    // When
    classUnderTest.getDefaultCurrency(requestMap);
  }

  /**
   * Tests the getOrganization method with a null organization, expecting a NullPointerException.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetOrganizationNullOrganization() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPC_ORDER_ID, TestConstants.TEST_ORDER_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // Set up order with null organization
    when(mockOrder.getOrganization()).thenReturn(null);

    // Then
    expectedException.expect(NullPointerException.class);

    // When
    classUnderTest.getOrganization(requestMap);
  }
}
