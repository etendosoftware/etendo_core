package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.openbravo.test.base.mock.MockitoStaticMockUtils.mockStaticSafely;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

/**
 * Unit tests for the PaymentOutAddPaymentDefaultValues class.
 */
@RunWith(MockitoJUnitRunner.class)
public class PaymentOutAddPaymentDefaultValuesTest {

  /**
   * The class under test.
   */
  @InjectMocks
  private PaymentOutAddPaymentDefaultValues classUnderTest;

  @Mock
  private FIN_Payment mockPayment;

  @Mock
  private BusinessPartner mockBusinessPartner;

  @Mock
  private Currency mockCurrency;

  @Mock
  private Organization mockOrganization;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBDateUtils> mockedOBDateUtils;
  private Map<String, String> requestMap;
  private JSONObject context;
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

    // Setup static mocks
    OBDal mockOBDal = mock(OBDal.class);
    mockedOBDal = mockStaticSafely(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    mockedOBDateUtils = mockStatic(OBDateUtils.class);

    // Setup mock payment
    when(mockPayment.getAmount()).thenReturn(TestConstants.PAYMENT_AMOUNT);
    when(mockPayment.getGeneratedCredit()).thenReturn(TestConstants.GENERATED_CREDIT);
    when(mockPayment.getDocumentNo()).thenReturn(TestConstants.DOCUMENT_NO);
    when(mockPayment.getFinancialTransactionConvertRate()).thenReturn(TestConstants.CONVERSION_RATE);
    when(mockPayment.getFinancialTransactionAmount()).thenReturn(TestConstants.CONVERTED_AMOUNT);
    when(mockPayment.getBusinessPartner()).thenReturn(mockBusinessPartner);
    when(mockPayment.getCurrency()).thenReturn(mockCurrency);
    when(mockPayment.getOrganization()).thenReturn(mockOrganization);
    when(mockPayment.getPaymentDate()).thenReturn(new Date());

    // Setup mock business partner
    when(mockBusinessPartner.getId()).thenReturn(TestConstants.BUSINESS_PARTNER_ID);

    // Setup mock currency
    when(mockCurrency.getId()).thenReturn(TestConstants.CURRENCY_ID);
    when(mockCurrency.getStandardPrecision()).thenReturn(
        Long.valueOf(String.valueOf(TestConstants.STANDARD_PRECISION)));

    // Setup mock organization
    when(mockOrganization.getId()).thenReturn(TestConstants.ORGANIZATION_ID);

    // Setup request map with context
    requestMap = new HashMap<>();
    context = new JSONObject();
    context.put("inpfinPaymentId", TestConstants.PAYMENT_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // Setup OBDal to return mock payment
    when(mockOBDal.get(FIN_Payment.class, TestConstants.PAYMENT_ID)).thenReturn(mockPayment);

    // Setup date formatting
    mockedOBDateUtils.when(() -> OBDateUtils.formatDate(mockPayment.getPaymentDate())).thenReturn("2023-01-01");
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
    if (mockedOBDateUtils != null) {
      mockedOBDateUtils.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Tests the getSeq method.
   */
  @Test
  public void testGetSeq() {
    // When
    long result = classUnderTest.getSeq();

    // Then
    assertEquals("Sequence should be 100", 100L, result);
  }

  /**
   * Tests the getDefaultExpectedAmount method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultExpectedAmount() throws JSONException {
    // When
    String result = classUnderTest.getDefaultExpectedAmount(requestMap);

    // Then
    assertEquals("Expected amount should match payment amount", TestConstants.PAYMENT_AMOUNT.toPlainString(), result);
  }

  /**
   * Tests the getDefaultActualAmount method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultActualAmount() throws JSONException {
    // When
    String result = classUnderTest.getDefaultActualAmount(requestMap);

    // Then
    assertEquals("Actual amount should match payment amount", TestConstants.PAYMENT_AMOUNT.toPlainString(), result);
  }

  /**
   * Tests the getDefaultGeneratedCredit method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultGeneratedCredit() throws JSONException {
    // When
    String result = classUnderTest.getDefaultGeneratedCredit(requestMap);

    // Then
    assertEquals("Generated credit should match payment generated credit",
        TestConstants.GENERATED_CREDIT.toPlainString(), result);
  }

  /**
   * Tests the getDefaultGeneratedCredit method when the generated credit is null.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultGeneratedCreditNull() throws JSONException {
    // Given
    when(mockPayment.getGeneratedCredit()).thenReturn(null);

    // When
    String result = classUnderTest.getDefaultGeneratedCredit(requestMap);

    // Then
    assertEquals("Generated credit should be zero when null", BigDecimal.ZERO.toPlainString(), result);
  }

  /**
   * Tests the getDefaultIsSOTrx method.
   */
  @Test
  public void testGetDefaultIsSOTrx() {
    // When
    String result = classUnderTest.getDefaultIsSOTrx(requestMap);

    // Then
    assertEquals("IsSOTrx should be N", "N", result);
  }

  /**
   * Tests the getDefaultTransactionType method.
   */
  @Test
  public void testGetDefaultTransactionType() {
    // When
    String result = classUnderTest.getDefaultTransactionType(requestMap);

    // Then
    assertEquals("Transaction type should be I", "I", result);
  }

  /**
   * Tests the getDefaultPaymentType method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultPaymentType() throws JSONException {
    // When
    String result = classUnderTest.getDefaultPaymentType(requestMap);

    // Then
    assertEquals("Payment type should match payment ID", TestConstants.PAYMENT_ID, result);
  }

  /**
   * Tests the getDefaultOrderType method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultOrderType() throws JSONException {
    // When
    String result = classUnderTest.getDefaultOrderType(requestMap);

    // Then
    assertEquals("Order type should be empty", "", result);
  }

  /**
   * Tests the getDefaultInvoiceType method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultInvoiceType() throws JSONException {
    // When
    String result = classUnderTest.getDefaultInvoiceType(requestMap);

    // Then
    assertEquals("Invoice type should be empty", "", result);
  }

  /**
   * Tests the getDefaultDocumentNo method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultDocumentNo() throws JSONException {
    // When
    String result = classUnderTest.getDefaultDocumentNo(requestMap);

    // Then
    assertEquals("Document number should match payment document number", TestConstants.DOCUMENT_NO, result);
  }

  /**
   * Tests the getDefaultDocumentNo method with inpfinPaymentId.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetPaymentWithInpfinPaymentId() throws JSONException {
    // Given
    context = new JSONObject();
    context.put("inpfinPaymentId", TestConstants.PAYMENT_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultDocumentNo(requestMap);

    // Then
    assertEquals("Document number should match when using inpfinPaymentId", TestConstants.DOCUMENT_NO, result);
  }

  /**
   * Tests the getDefaultDocumentNo method with Fin_Payment_ID.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetPaymentWithFinPaymentID() throws JSONException {
    // Given
    context = new JSONObject();
    context.put("Fin_Payment_ID", TestConstants.PAYMENT_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultDocumentNo(requestMap);

    // Then
    assertEquals("Document number should match when using Fin_Payment_ID", TestConstants.DOCUMENT_NO, result);
  }

  /**
   * Tests the getDefaultConversionRate method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultConversionRate() throws JSONException {
    // When
    String result = classUnderTest.getDefaultConversionRate(requestMap);

    // Then
    assertEquals("Conversion rate should match payment conversion rate", TestConstants.CONVERSION_RATE.toPlainString(),
        result);
  }

  /**
   * Tests the getDefaultConvertedAmount method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultConvertedAmount() throws JSONException {
    // When
    String result = classUnderTest.getDefaultConvertedAmount(requestMap);

    // Then
    assertEquals("Converted amount should match payment converted amount",
        TestConstants.CONVERTED_AMOUNT.toPlainString(), result);
  }

  /**
   * Tests the getDefaultReceivedFrom method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultReceivedFrom() throws JSONException {
    // When
    String result = classUnderTest.getDefaultReceivedFrom(requestMap);

    // Then
    assertEquals("Received from should match business partner ID", TestConstants.BUSINESS_PARTNER_ID, result);
  }

  /**
   * Tests the getDefaultReceivedFrom method when the business partner is null.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultReceivedFromNullBusinessPartner() throws JSONException {
    // Given
    when(mockPayment.getBusinessPartner()).thenReturn(null);

    // When
    String result = classUnderTest.getDefaultReceivedFrom(requestMap);

    // Then
    assertEquals("Received from should be empty when business partner is null", "", result);
  }

  /**
   * Tests the getDefaultStandardPrecision method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultStandardPrecision() throws JSONException {
    // When
    String result = classUnderTest.getDefaultStandardPrecision(requestMap);

    // Then
    assertEquals("Standard precision should match currency standard precision",
        TestConstants.STANDARD_PRECISION.toString(), result);
  }

  /**
   * Tests the getDefaultCurrency method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultCurrency() throws JSONException {
    // When
    String result = classUnderTest.getDefaultCurrency(requestMap);

    // Then
    assertEquals("Currency should match payment currency ID", TestConstants.CURRENCY_ID, result);
  }

  /**
   * Tests the getOrganization method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOrganization() throws JSONException {
    // When
    String result = classUnderTest.getOrganization(requestMap);

    // Then
    assertEquals("Organization should match payment organization ID", TestConstants.ORGANIZATION_ID, result);
  }

  /**
   * Tests the getDefaultPaymentDate method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultPaymentDate() throws JSONException {
    // When
    String result = classUnderTest.getDefaultPaymentDate(requestMap);

    // Then
    assertEquals("Payment date should be formatted correctly", "2023-01-01", result);
  }

  /**
   * Tests the getDefaultDocument method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultDocument() throws JSONException {
    // When
    String result = classUnderTest.getDefaultDocument(requestMap);

    // Then
    assertEquals("Default document should be empty", "", result);
  }

  /**
   * Tests the getBankStatementLineAmount method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetBankStatementLineAmount() throws JSONException {
    // When
    String result = classUnderTest.getBankStatementLineAmount(requestMap);

    // Then
    assertEquals("Bank statement line amount should be empty", "", result);
  }
}
