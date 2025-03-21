package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.advpaymentmngt.utility.APRMConstants;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test class for TransactionAddPaymentReadOnlyLogics
 */
@RunWith(MockitoJUnitRunner.class)
public class TransactionAddPaymentReadOnlyLogicsTest extends OBBaseTest {

  private MockedStatic<OBDal> mockedOBDal;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private FIN_FinancialAccount mockFinancialAccount;

  @Mock
  private FIN_PaymentMethod mockPaymentMethod;

  @Mock
  private FinAccPaymentMethod mockFinAccPaymentMethod;

  @InjectMocks
  private TransactionAddPaymentReadOnlyLogics classUnderTest;

  private AutoCloseable mocks;
  private Map<String, String> requestMap;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);

    // Initialize static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    // Initialize request map with context
    requestMap = new HashMap<>();

    // Setup default behavior for mocks
    List<FinAccPaymentMethod> finAccPaymentMethodList = new ArrayList<>();
    finAccPaymentMethodList.add(mockFinAccPaymentMethod);
    when(mockFinancialAccount.getFinancialMgmtFinAccPaymentMethodList()).thenReturn(finAccPaymentMethodList);
    when(mockFinAccPaymentMethod.getPaymentMethod()).thenReturn(mockPaymentMethod);
    when(mockOBDal.get(FIN_FinancialAccount.class, TestConstants.TEST_FINANCIAL_ACCOUNT_ID)).thenReturn(
        mockFinancialAccount);
    when(mockOBDal.get(FIN_PaymentMethod.class, TestConstants.TEST_PAYMENT_METHOD)).thenReturn(mockPaymentMethod);
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
   * Test getPaymentDocumentNoReadOnlyLogic method
   */
  @Test
  public void testGetPaymentDocumentNoReadOnlyLogic() throws JSONException {
    // When
    boolean result = classUnderTest.getPaymentDocumentNoReadOnlyLogic(requestMap);

    // Then
    assertFalse("Payment document number should not be read-only", result);
  }

  /**
   * Test getReceivedFromReadOnlyLogic method
   */
  @Test
  public void testGetReceivedFromReadOnlyLogic() throws JSONException {
    // When
    boolean result = classUnderTest.getReceivedFromReadOnlyLogic(requestMap);

    // Then
    assertFalse("Received from field should not be read-only", result);
  }

  /**
   * Test getPaymentMethodReadOnlyLogic method
   */
  @Test
  public void testGetPaymentMethodReadOnlyLogic() throws JSONException {
    // When
    boolean result = classUnderTest.getPaymentMethodReadOnlyLogic(requestMap);

    // Then
    assertFalse("Payment method should not be read-only", result);
  }

  /**
   * Test getPaymentDateReadOnlyLogic method
   */
  @Test
  public void testGetPaymentDateReadOnlyLogic() throws JSONException {
    // When
    boolean result = classUnderTest.getPaymentDateReadOnlyLogic(requestMap);

    // Then
    assertFalse("Payment date should not be read-only", result);
  }

  /**
   * Test getFinancialAccountReadOnlyLogic method
   */
  @Test
  public void testGetFinancialAccountReadOnlyLogic() throws JSONException {
    // When
    boolean result = classUnderTest.getFinancialAccountReadOnlyLogic(requestMap);

    // Then
    assertTrue("Financial account should be read-only", result);
  }

  /**
   * Test getActualPaymentReadOnlyLogic with BPD document type
   */
  @Test
  public void testGetActualPaymentReadOnlyLogicBPD() throws JSONException {
    // Given
    JSONObject context = new JSONObject();
    context.put(TestConstants.TRXTYPE, "BPD");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    boolean result = classUnderTest.getActualPaymentReadOnlyLogic(requestMap);

    // Then
    assertFalse("Actual payment should not be read-only for BPD document type", result);
  }

  /**
   * Test getActualPaymentReadOnlyLogic with RCIN document type
   */
  @Test
  public void testGetActualPaymentReadOnlyLogicRCIN() throws JSONException {
    // Given
    JSONObject context = new JSONObject();
    context.put(TestConstants.TRXTYPE, "RCIN");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    boolean result = classUnderTest.getActualPaymentReadOnlyLogic(requestMap);

    // Then
    assertFalse("Actual payment should not be read-only for RCIN document type", result);
  }

  /**
   * Test getActualPaymentReadOnlyLogic with other document type
   */
  @Test
  public void testGetActualPaymentReadOnlyLogicOtherDocType() throws JSONException {
    // Given
    JSONObject context = new JSONObject();
    context.put(TestConstants.TRXTYPE, "OTHER");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    boolean result = classUnderTest.getActualPaymentReadOnlyLogic(requestMap);

    // Then
    assertTrue("Actual payment should be read-only for other document types", result);
  }

  /**
   * Test getActualPaymentReadOnlyLogic with transaction window
   */
  @Test
  public void testGetActualPaymentReadOnlyLogicTransactionWindow() throws JSONException {
    // Given
    JSONObject context = new JSONObject();
    context.put("inpwindowId", APRMConstants.TRANSACTION_WINDOW_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    boolean result = classUnderTest.getActualPaymentReadOnlyLogic(requestMap);

    // Then
    assertFalse("Actual payment should not be read-only for transaction window", result);
  }

  /**
   * Test getActualPaymentReadOnlyLogic with other window
   */
  @Test
  public void testGetActualPaymentReadOnlyLogicOtherWindow() throws JSONException {
    // Given
    JSONObject context = new JSONObject();
    context.put("inpwindowId", "OTHER_WINDOW_ID");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    boolean result = classUnderTest.getActualPaymentReadOnlyLogic(requestMap);

    // Then
    assertTrue("Actual payment should be read-only for other windows", result);
  }

  /**
   * Test getCurrencyReadOnlyLogic with RCIN document type and multicurrency payment method
   */
  @Test
  public void testGetCurrencyReadOnlyLogicRCINMulticurrency() throws JSONException {
    // Given
    JSONObject context = new JSONObject();
    context.put(TestConstants.TRXTYPE, "RCIN");
    context.put(TestConstants.FIN_FINANCIAL_ACCOUNT_ID, TestConstants.TEST_FINANCIAL_ACCOUNT_ID);
    context.put(TestConstants.FIN_PAYMENT_METHOD_ID, TestConstants.TEST_PAYMENT_METHOD);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockFinAccPaymentMethod.isPayinIsMulticurrency()).thenReturn(true);

    // When
    boolean result = classUnderTest.getCurrencyReadOnlyLogic(requestMap);

    // Then
    assertFalse("Currency should not be read-only for RCIN with multicurrency payment method", result);
  }

  /**
   * Test getCurrencyReadOnlyLogic with RCIN document type and non-multicurrency payment method
   */
  @Test
  public void testGetCurrencyReadOnlyLogicRCINNonMulticurrency() throws JSONException {
    // Given
    JSONObject context = new JSONObject();
    context.put(TestConstants.TRXTYPE, "RCIN");
    context.put(TestConstants.FIN_FINANCIAL_ACCOUNT_ID, TestConstants.TEST_FINANCIAL_ACCOUNT_ID);
    context.put(TestConstants.FIN_PAYMENT_METHOD_ID, TestConstants.TEST_PAYMENT_METHOD);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockFinAccPaymentMethod.isPayinIsMulticurrency()).thenReturn(false);

    // When
    boolean result = classUnderTest.getCurrencyReadOnlyLogic(requestMap);

    // Then
    assertTrue("Currency should be read-only for RCIN with non-multicurrency payment method", result);
  }

  /**
   * Test getCurrencyReadOnlyLogic with BPD document type and multicurrency payment method
   */
  @Test
  public void testGetCurrencyReadOnlyLogicBPDMulticurrency() throws JSONException {
    // Given
    JSONObject context = new JSONObject();
    context.put(TestConstants.TRXTYPE, "BPD");
    context.put(TestConstants.FIN_FINANCIAL_ACCOUNT_ID, TestConstants.TEST_FINANCIAL_ACCOUNT_ID);
    context.put(TestConstants.FIN_PAYMENT_METHOD_ID, TestConstants.TEST_PAYMENT_METHOD);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockFinAccPaymentMethod.isPayinIsMulticurrency()).thenReturn(true);

    // When
    boolean result = classUnderTest.getCurrencyReadOnlyLogic(requestMap);

    // Then
    assertFalse("Currency should not be read-only for BPD with multicurrency payment method", result);
  }

  /**
   * Test getCurrencyReadOnlyLogic with PDOUT document type and multicurrency payment method
   */
  @Test
  public void testGetCurrencyReadOnlyLogicPDOUTMulticurrency() throws JSONException {
    // Given
    JSONObject context = new JSONObject();
    context.put(TestConstants.TRXTYPE, "PDOUT");
    context.put(TestConstants.FIN_FINANCIAL_ACCOUNT_ID, TestConstants.TEST_FINANCIAL_ACCOUNT_ID);
    context.put(TestConstants.FIN_PAYMENT_METHOD_ID, TestConstants.TEST_PAYMENT_METHOD);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockFinAccPaymentMethod.isPayoutIsMulticurrency()).thenReturn(true);

    // When
    boolean result = classUnderTest.getCurrencyReadOnlyLogic(requestMap);

    // Then
    assertFalse("Currency should not be read-only for PDOUT with multicurrency payment method", result);
  }

  /**
   * Test getCurrencyReadOnlyLogic with BPW document type and multicurrency payment method
   */
  @Test
  public void testGetCurrencyReadOnlyLogicBPWMulticurrency() throws JSONException {
    // Given
    JSONObject context = new JSONObject();
    context.put(TestConstants.TRXTYPE, "BPW");
    context.put(TestConstants.FIN_FINANCIAL_ACCOUNT_ID, TestConstants.TEST_FINANCIAL_ACCOUNT_ID);
    context.put(TestConstants.FIN_PAYMENT_METHOD_ID, TestConstants.TEST_PAYMENT_METHOD);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockFinAccPaymentMethod.isPayoutIsMulticurrency()).thenReturn(true);

    // When
    boolean result = classUnderTest.getCurrencyReadOnlyLogic(requestMap);

    // Then
    assertFalse("Currency should not be read-only for BPW with multicurrency payment method", result);
  }

}
