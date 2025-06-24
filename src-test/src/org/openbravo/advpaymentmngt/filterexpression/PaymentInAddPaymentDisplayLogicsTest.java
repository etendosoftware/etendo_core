package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

/**
 * Unit tests for the PaymentInAddPaymentDisplayLogics class.
 */
public class PaymentInAddPaymentDisplayLogicsTest {

  private PaymentInAddPaymentDisplayLogics displayLogics;
  private AutoCloseable mocks;

  // Static mocks
  private MockedStatic<OBDal> mockedOBDal;

  private MockedConstruction<AdvPaymentMngtDao> mockedAdvPaymentMngtDao;

  // Mocks
  @Mock
  private OBDal mockOBDal;
  @Mock
  private FIN_Payment mockPayment;
  @Mock
  private BusinessPartner mockBusinessPartner;
  @Mock
  private Organization mockOrganization;
  @Mock
  private Currency mockCurrency;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    displayLogics = new PaymentInAddPaymentDisplayLogics();

    // Setup static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    // Setup mock payment
    when(mockPayment.getBusinessPartner()).thenReturn(mockBusinessPartner);
    when(mockPayment.getOrganization()).thenReturn(mockOrganization);
    when(mockPayment.getCurrency()).thenReturn(mockCurrency);
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
    if (mockedAdvPaymentMngtDao != null) {
      mockedAdvPaymentMngtDao.close();
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
    long sequence = displayLogics.getSeq();

    // Then
    assertEquals("Sequence should be 100", 100L, sequence);
  }

  /**
   * Tests the getOrganizationDisplayLogic method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOrganizationDisplayLogic() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    boolean result = displayLogics.getOrganizationDisplayLogic(requestMap);

    // Then
    assertFalse("Organization display logic should be false", result);
  }

  /**
   * Tests the getDocumentDisplayLogic method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDocumentDisplayLogic() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    boolean result = displayLogics.getDocumentDisplayLogic(requestMap);

    // Then
    assertFalse("Document display logic should be false", result);
  }

  /**
   * Tests the getCreditToUseDisplayLogic method with positive customer credit.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetCreditToUseDisplayLogicWithPositiveCredit() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPFIN_PAYMENT_ID, TestConstants.PAYMENT_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // Setup mocks
    when(mockOBDal.get(FIN_Payment.class, TestConstants.PAYMENT_ID)).thenReturn(mockPayment);

    mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class, (mock, context2) -> when(
        mock.getCustomerCredit(any(BusinessPartner.class), eq(true), any(Organization.class),
            any(Currency.class))).thenReturn(new BigDecimal("100.00")));

    // When
    boolean result = displayLogics.getCreditToUseDisplayLogic(requestMap);

    // Then
    assertTrue("Credit to use display logic should be true when customer credit > 0", result);
  }

  /**
   * Tests the getCreditToUseDisplayLogic method with zero customer credit.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetCreditToUseDisplayLogicWithZeroCredit() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPFIN_PAYMENT_ID, TestConstants.PAYMENT_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // Setup mocks
    when(mockOBDal.get(FIN_Payment.class, TestConstants.PAYMENT_ID)).thenReturn(mockPayment);

    mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class, (mock, context2) -> when(
        mock.getCustomerCredit(any(BusinessPartner.class), eq(true), any(Organization.class),
            any(Currency.class))).thenReturn(BigDecimal.ZERO));

    // When
    boolean result = displayLogics.getCreditToUseDisplayLogic(requestMap);

    // Then
    assertFalse("Credit to use display logic should be false when customer credit = 0", result);
  }

  /**
   * Tests the getCreditToUseDisplayLogic method with a null business partner.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetCreditToUseDisplayLogicWithNullBusinessPartner() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPFIN_PAYMENT_ID, TestConstants.PAYMENT_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // Setup mocks
    when(mockOBDal.get(FIN_Payment.class, TestConstants.PAYMENT_ID)).thenReturn(mockPayment);
    when(mockPayment.getBusinessPartner()).thenReturn(null);

    // When
    boolean result = displayLogics.getCreditToUseDisplayLogic(requestMap);

    // Then
    assertFalse("Credit to use display logic should be false when business partner is null", result);
  }

  /**
   * Tests the getCreditToUseDisplayLogic method with an alternative payment ID.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetCreditToUseDisplayLogicWithAlternativePaymentId() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("Fin_Payment_ID", TestConstants.PAYMENT_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // Setup mocks
    when(mockOBDal.get(FIN_Payment.class, TestConstants.PAYMENT_ID)).thenReturn(mockPayment);

    mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class, (mock, context2) -> when(
        mock.getCustomerCredit(any(BusinessPartner.class), eq(true), any(Organization.class),
            any(Currency.class))).thenReturn(new BigDecimal("100.00")));

    // When
    boolean result = displayLogics.getCreditToUseDisplayLogic(requestMap);

    // Then
    assertTrue("Credit to use display logic should be true when using Fin_Payment_ID", result);
  }

  /**
   * Tests the getBankStatementLineDisplayLogic method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetBankStatementLineDisplayLogic() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    boolean result = displayLogics.getBankStatementLineDisplayLogic(requestMap);

    // Then
    assertFalse("Bank statement line display logic should be false", result);
  }

  /**
   * Tests the getOverpaymentActionDisplayLogic method with a positive difference.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOverpaymentActionDisplayLogicWithPositiveDifference() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.DIFFERENCE, 50.0);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);

    // Then
    assertTrue("Overpayment action display logic should be true when difference > 0", result);
  }

  /**
   * Tests the getOverpaymentActionDisplayLogic method with a zero difference.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOverpaymentActionDisplayLogicWithZeroDifference() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.DIFFERENCE, 0.0);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);

    // Then
    assertFalse("Overpayment action display logic should be false when difference = 0", result);
  }

  /**
   * Tests the getOverpaymentActionDisplayLogic method with a negative difference.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOverpaymentActionDisplayLogicWithNegativeDifference() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.DIFFERENCE, -10.0);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);

    // Then
    assertFalse("Overpayment action display logic should be false when difference < 0", result);
  }

  /**
   * Tests the getOverpaymentActionDisplayLogic method with a null context.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOverpaymentActionDisplayLogicWithNullContext() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);

    // Then
    assertFalse("Overpayment action display logic should be false when context is null", result);
  }

  /**
   * Tests the getOverpaymentActionDisplayLogic method without a difference.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOverpaymentActionDisplayLogicWithoutDifference() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);

    // Then
    assertFalse("Overpayment action display logic should be false when difference is not present", result);
  }
}
