package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.openbravo.test.base.mock.MockitoStaticMockUtils.mockStaticSafely;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
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
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;

/**
 * Unit tests for the PurchaseOrderAddPaymentDefaultValues class.
 */
@RunWith(MockitoJUnitRunner.class)
public class PurchaseOrderAddPaymentDefaultValuesTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  // Static mocks
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBDateUtils> mockedOBDateUtils;

  // Mocks
  @Mock
  private OBDal mockOBDal;

  @Mock
  private Order mockOrder;

  @Mock
  private BusinessPartner mockBusinessPartner;

  @Mock
  private Currency mockCurrency;

  @Mock
  private Organization mockOrganization;

  @InjectMocks
  private PurchaseOrderAddPaymentDefaultValues classUnderTest;

  private Map<String, String> requestMap;
  private JSONObject mockContext;
  private List<FIN_PaymentSchedule> paymentScheduleList;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    // Initialize static mocks
    mockedOBDal = mockStaticSafely(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    mockedOBDateUtils = mockStatic(OBDateUtils.class);

    // Setup mock context
    mockContext = new JSONObject();
    mockContext.put("inpcOrderId", TestConstants.TEST_ORDER_ID);

    // Setup request map
    requestMap = new HashMap<>();
    requestMap.put("context", mockContext.toString());

    // Setup order mock
    when(mockOBDal.get(Order.class, TestConstants.TEST_ORDER_ID)).thenReturn(mockOrder);

    // Setup business partner mock
    when(mockBusinessPartner.getId()).thenReturn("TEST_BP_ID");
    when(mockOrder.getBusinessPartner()).thenReturn(mockBusinessPartner);

    // Setup currency mock
    when(mockCurrency.getId()).thenReturn("TEST_CURRENCY_ID");
    when(mockCurrency.getStandardPrecision()).thenReturn(2L);
    when(mockOrder.getCurrency()).thenReturn(mockCurrency);

    // Setup organization mock
    when(mockOrganization.getId()).thenReturn("TEST_ORG_ID");
    when(mockOrder.getOrganization()).thenReturn(mockOrganization);

    // Setup payment schedule list
    paymentScheduleList = new ArrayList<>();
    when(mockOrder.getFINPaymentScheduleList()).thenReturn(paymentScheduleList);

    // Setup date formatting
    mockedOBDateUtils.when(() -> OBDateUtils.formatDate(any(Date.class))).thenReturn("2023-01-01");
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBDateUtils != null) {
      mockedOBDateUtils.close();
    }
  }

  /**
   * Tests the getDefaultExpectedAmount method, expecting the order pending amount to be returned.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultExpectedAmountReturnsOrderPendingAmount() throws JSONException {
    // GIVEN
    // Create mock payment schedule
    FIN_PaymentSchedule mockSchedule = mock(FIN_PaymentSchedule.class);

    // Mock the invoice as null to force using order payment schedule list
    when(mockSchedule.getInvoice()).thenReturn(null);

    // Create mock payment schedule details
    List<FIN_PaymentScheduleDetail> mockScheduleDetails = new ArrayList<>();
    FIN_PaymentScheduleDetail mockDetail = mock(FIN_PaymentScheduleDetail.class);

    // Set amount and null payment details to match expected behavior
    when(mockDetail.getAmount()).thenReturn(new BigDecimal(TestConstants.AMOUNT));
    when(mockDetail.getPaymentDetails()).thenReturn(null);

    mockScheduleDetails.add(mockDetail);

    // Link details to schedule
    when(mockSchedule.getFINPaymentScheduleDetailOrderPaymentScheduleList()).thenReturn(mockScheduleDetails);

    // Add to payment schedule list
    paymentScheduleList.add(mockSchedule);

    // WHEN
    String result = classUnderTest.getDefaultExpectedAmount(requestMap);

    // THEN
    assertEquals(TestConstants.AMOUNT, result);
    verify(mockOBDal).get(Order.class, TestConstants.TEST_ORDER_ID);
    verify(mockOrder).getFINPaymentScheduleList();
    verify(mockSchedule).getInvoice();
    verify(mockSchedule).getFINPaymentScheduleDetailOrderPaymentScheduleList();
    verify(mockDetail).getAmount();
    verify(mockDetail).getPaymentDetails();
  }

  /**
   * Tests the getDefaultActualAmount method, expecting the order pending amount to be returned.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultActualAmountReturnsOrderPendingAmount() throws JSONException {
    // GIVEN
    // Create mock payment schedule
    FIN_PaymentSchedule mockSchedule = mock(FIN_PaymentSchedule.class);

    // Mock the invoice as null to force using order payment schedule list
    when(mockSchedule.getInvoice()).thenReturn(null);

    // Create mock payment schedule details
    List<FIN_PaymentScheduleDetail> mockScheduleDetails = new ArrayList<>();
    FIN_PaymentScheduleDetail mockDetail = mock(FIN_PaymentScheduleDetail.class);

    // Set amount and null payment details to match expected behavior
    when(mockDetail.getAmount()).thenReturn(new BigDecimal(TestConstants.AMOUNT));
    when(mockDetail.getPaymentDetails()).thenReturn(null);

    mockScheduleDetails.add(mockDetail);

    // Link details to schedule
    when(mockSchedule.getFINPaymentScheduleDetailOrderPaymentScheduleList()).thenReturn(mockScheduleDetails);

    // Add to payment schedule list
    paymentScheduleList.add(mockSchedule);

    // WHEN
    String result = classUnderTest.getDefaultActualAmount(requestMap);

    // THEN
    assertEquals(TestConstants.AMOUNT, result);
    verify(mockOBDal).get(Order.class, TestConstants.TEST_ORDER_ID);
    verify(mockOrder).getFINPaymentScheduleList();
    verify(mockSchedule).getInvoice();
    verify(mockSchedule).getFINPaymentScheduleDetailOrderPaymentScheduleList();
    verify(mockDetail).getAmount();
    verify(mockDetail).getPaymentDetails();
  }

  /**
   * Tests the getDefaultIsSOTrx method, expecting "N" to be returned.
   */
  @Test
  public void testGetDefaultIsSOTrxReturnsN() {
    // WHEN
    String result = classUnderTest.getDefaultIsSOTrx(requestMap);

    // THEN
    assertEquals("N", result);
  }

  /**
   * Tests the getDefaultTransactionType method, expecting "O" to be returned.
   */
  @Test
  public void testGetDefaultTransactionTypeReturnsO() {
    // WHEN
    String result = classUnderTest.getDefaultTransactionType(requestMap);

    // THEN
    assertEquals("O", result);
  }

  /**
   * Tests the getDefaultPaymentType method, expecting an empty string to be returned.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultPaymentTypeReturnsEmptyString() throws JSONException {
    // WHEN
    String result = classUnderTest.getDefaultPaymentType(requestMap);

    // THEN
    assertEquals("", result);
  }

  /**
   * Tests the getDefaultOrderType method, expecting the order ID to be returned.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultOrderTypeReturnsOrderId() throws JSONException {
    // WHEN
    String result = classUnderTest.getDefaultOrderType(requestMap);

    // THEN
    assertEquals(TestConstants.TEST_ORDER_ID, result);
  }

  /**
   * Tests the getDefaultInvoiceType method, expecting an empty string to be returned.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultInvoiceTypeReturnsEmptyString() throws JSONException {
    // WHEN
    String result = classUnderTest.getDefaultInvoiceType(requestMap);

    // THEN
    assertEquals("", result);
  }

  /**
   * Tests the getDefaultConversionRate method, expecting an empty string to be returned.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultConversionRateReturnsEmptyString() throws JSONException {
    // WHEN
    String result = classUnderTest.getDefaultConversionRate(requestMap);

    // THEN
    assertEquals("", result);
  }

  /**
   * Tests the getDefaultConvertedAmount method, expecting an empty string to be returned.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultConvertedAmountReturnsEmptyString() throws JSONException {
    // WHEN
    String result = classUnderTest.getDefaultConvertedAmount(requestMap);

    // THEN
    assertEquals("", result);
  }

  /**
   * Tests the getDefaultReceivedFrom method, expecting the business partner ID to be returned.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultReceivedFromReturnsBusinessPartnerId() throws JSONException {
    // WHEN
    String result = classUnderTest.getDefaultReceivedFrom(requestMap);

    // THEN
    assertEquals("TEST_BP_ID", result);
    verify(mockOBDal).get(Order.class, TestConstants.TEST_ORDER_ID);
    verify(mockOrder).getBusinessPartner();
    verify(mockBusinessPartner).getId();
  }

  /**
   * Tests the getDefaultStandardPrecision method, expecting the currency precision to be returned.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultStandardPrecisionReturnsCurrencyPrecision() throws JSONException {
    // WHEN
    String result = classUnderTest.getDefaultStandardPrecision(requestMap);

    // THEN
    assertEquals("2", result);
    verify(mockOBDal).get(Order.class, TestConstants.TEST_ORDER_ID);
    verify(mockOrder).getCurrency();
    verify(mockCurrency).getStandardPrecision();
  }

  /**
   * Tests the getDefaultCurrency method, expecting the currency ID to be returned.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultCurrencyReturnsCurrencyId() throws JSONException {
    // WHEN
    String result = classUnderTest.getDefaultCurrency(requestMap);

    // THEN
    assertEquals("TEST_CURRENCY_ID", result);
    verify(mockOBDal).get(Order.class, TestConstants.TEST_ORDER_ID);
    verify(mockOrder).getCurrency();
    verify(mockCurrency).getId();
  }

  /**
   * Tests the getOrganization method, expecting the organization ID to be returned.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOrganizationReturnsOrganizationId() throws JSONException {
    // WHEN
    String result = classUnderTest.getOrganization(requestMap);

    // THEN
    assertEquals("TEST_ORG_ID", result);
    verify(mockOBDal).get(Order.class, TestConstants.TEST_ORDER_ID);
    verify(mockOrder).getOrganization();
    verify(mockOrganization).getId();
  }

  /**
   * Tests the getDefaultDocument method, expecting an empty string to be returned.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultDocumentReturnsEmptyString() throws JSONException {
    // WHEN
    String result = classUnderTest.getDefaultDocument(requestMap);

    // THEN
    assertEquals("", result);
  }

  /**
   * Tests the getOrder method, expecting the order to be returned from the context.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOrderReturnsOrderFromContext() throws JSONException {
    // WHEN
    Order result = classUnderTest.getOrder(mockContext);

    // THEN
    assertEquals(mockOrder, result);
    verify(mockOBDal).get(Order.class, TestConstants.TEST_ORDER_ID);
  }

  /**
   * Tests the getDefaultPaymentDate method, expecting the formatted current date to be returned.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultPaymentDateReturnsFormattedCurrentDate() throws JSONException {
    // WHEN
    String result = classUnderTest.getDefaultPaymentDate(requestMap);

    // THEN
    assertEquals("2023-01-01", result);
    mockedOBDateUtils.verify(() -> OBDateUtils.formatDate(any(Date.class)));
  }

  /**
   * Tests the getBankStatementLineAmount method, expecting an empty string to be returned.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetBankStatementLineAmountReturnsEmptyString() throws JSONException {
    // WHEN
    String result = classUnderTest.getBankStatementLineAmount(requestMap);

    // THEN
    assertEquals("", result);
  }

  /**
   * Tests the getSeq method, expecting 100 to be returned.
   */
  @Test
  public void testGetSeqReturns100() {
    // WHEN
    long result = classUnderTest.getSeq();

    // THEN
    assertEquals(100L, result);
  }
}
