package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.openbravo.test.base.mock.MockitoStaticMockUtils.mockStaticSafely;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.service.json.JsonUtils;

/**
 * Test class for TransactionAddPaymentDefaultValues
 */
public class TransactionAddPaymentDefaultValuesTest {

  private TransactionAddPaymentDefaultValues classUnderTest;
  private AutoCloseable mocks;

  @Mock
  private OBDal obDal;

  @Mock
  private FIN_FinancialAccount financialAccount;

  @Mock
  private Currency currency;

  @Mock
  private Organization organization;


  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBDateUtils> mockedOBDateUtils;
  private MockedStatic<JsonUtils> mockedJsonUtils;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    classUnderTest = new TransactionAddPaymentDefaultValues();

    // Setup static mocks
    mockedOBDal = mockStaticSafely(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

    mockedOBDateUtils = mockStatic(OBDateUtils.class);
    mockedJsonUtils = mockStatic(JsonUtils.class);

    // Setup common mocks
    when(currency.getStandardPrecision()).thenReturn(2L);
    when(currency.getId()).thenReturn("USD");
    when(financialAccount.getCurrency()).thenReturn(currency);
    when(financialAccount.getOrganization()).thenReturn(organization);
    when(organization.getId()).thenReturn("0");
    when(obDal.get(FIN_FinancialAccount.class, TestConstants.TEST_FIN_ACCOUNT)).thenReturn(financialAccount);
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
    if (mockedJsonUtils != null) {
      mockedJsonUtils.close();
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
    assertEquals(100L, result);
  }

  /**
   * Tests the getDefaultExpectedAmount method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultExpectedAmount() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    String result = classUnderTest.getDefaultExpectedAmount(requestMap);

    // Then
    assertEquals("0", result);
  }

  /**
   * Tests the getDefaultActualAmount method for sales transactions with deposit and withdrawal amounts.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultActualAmountSOTrxWithDepositAndWithdrawal() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("depositamt", TestConstants.AMOUNT);
    context.put("withdrawalamt", TestConstants.WITHDRAWAL_AMOUNT);
    context.put(TestConstants.TRXTYPE, "BPD");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultActualAmount(requestMap);

    // Then
    assertEquals(TestConstants.DEPOSIT_AMOUNT, result);
  }

  /**
   * Tests the getDefaultActualAmount method for sales transactions with input deposit and payment amounts.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultActualAmountSOTrxWithInpDepositAndPayment() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpdepositamt", TestConstants.AMOUNT);
    context.put("inppaymentamt", TestConstants.WITHDRAWAL_AMOUNT);
    context.put(TestConstants.TRXTYPE, "BPD");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultActualAmount(requestMap);

    // Then
    assertEquals(TestConstants.DEPOSIT_AMOUNT, result);
  }

  /**
   * Tests the getDefaultActualAmount method for non-sales transactions.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultActualAmountNotSOTrx() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.TRXTYPE, "BPW");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultActualAmount(requestMap);

    // Then
    assertEquals("0", result);
  }

  /**
   * Tests the convertToSafeDecimalString method for European format.
   *
   * @throws Exception
   *     if an error occurs during reflection
   */
  @Test
  public void testConvertToSafeDecimalStringEuropeanFormat() throws Exception {
    // Given
    String europeanFormat = "1.234,56";

    // When
    // We need to use reflection to access the private method
    java.lang.reflect.Method method = TransactionAddPaymentDefaultValues.class.getDeclaredMethod(
        TestConstants.CONVERT_TO_SAFE_DECIMAL_STRING, String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(classUnderTest, europeanFormat);

    // Then
    assertEquals("1234.56", result);
  }

  /**
   * Tests the convertToSafeDecimalString method for Anglo-Saxon format.
   *
   * @throws Exception
   *     if an error occurs during reflection
   */
  @Test
  public void testConvertToSafeDecimalStringAngloSaxonFormat() throws Exception {
    // Given
    String angloSaxonFormat = "1,234.56";

    // When
    // We need to use reflection to access the private method
    java.lang.reflect.Method method = TransactionAddPaymentDefaultValues.class.getDeclaredMethod(
        TestConstants.CONVERT_TO_SAFE_DECIMAL_STRING, String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(classUnderTest, angloSaxonFormat);

    // Then
    assertEquals("1234.56", result);
  }

  /**
   * Tests the convertToSafeDecimalString method for scientific notation.
   *
   * @throws Exception
   *     if an error occurs during reflection
   */
  @Test
  public void testConvertToSafeDecimalStringScientificNotation() throws Exception {
    // Given
    String scientificNotation = "1.214859023E7";

    // When
    // We need to use reflection to access the private method
    java.lang.reflect.Method method = TransactionAddPaymentDefaultValues.class.getDeclaredMethod(
        TestConstants.CONVERT_TO_SAFE_DECIMAL_STRING, String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(classUnderTest, scientificNotation);

    // Then
    assertEquals("1.214859023E7", result);
  }

  /**
   * Tests the getDefaultIsSOTrx method for BPD transaction type.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultIsSOTrxBPD() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.TRXTYPE, "BPD");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultIsSOTrx(requestMap);

    // Then
    assertEquals("Y", result);
  }

  /**
   * Tests the getDefaultIsSOTrx method for BPW transaction type.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultIsSOTrxBPW() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.TRXTYPE, "BPW");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultIsSOTrx(requestMap);

    // Then
    assertEquals("N", result);
  }

  /**
   * Tests the getDefaultIsSOTrx method for other transaction types.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultIsSOTrxOther() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.TRXTYPE, "OTHER");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultIsSOTrx(requestMap);

    // Then
    assertEquals("", result);
  }

  /**
   * Tests the getDefaultTransactionType method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultTransactionType() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    String result = classUnderTest.getDefaultTransactionType(requestMap);

    // Then
    assertEquals("I", result);
  }

  /**
   * Tests the getDefaultPaymentType method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultPaymentType() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    String result = classUnderTest.getDefaultPaymentType(requestMap);

    // Then
    assertEquals("", result);
  }

  /**
   * Tests the getDefaultOrderType method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultOrderType() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    String result = classUnderTest.getDefaultOrderType(requestMap);

    // Then
    assertEquals("", result);
  }

  /**
   * Tests the getDefaultInvoiceType method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultInvoiceType() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    String result = classUnderTest.getDefaultInvoiceType(requestMap);

    // Then
    assertEquals("", result);
  }

  /**
   * Tests the getDefaultConversionRate method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultConversionRate() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    String result = classUnderTest.getDefaultConversionRate(requestMap);

    // Then
    assertEquals("1", result);
  }

  /**
   * Tests the getDefaultConvertedAmount method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultConvertedAmount() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    String result = classUnderTest.getDefaultConvertedAmount(requestMap);

    // Then
    assertEquals("0", result);
  }

  /**
   * Tests the getDefaultReceivedFrom method with a business partner.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultReceivedFromWithBPartner() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("c_bpartner_id", "TEST_BPARTNER");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultReceivedFrom(requestMap);

    // Then
    assertEquals("TEST_BPARTNER", result);
  }

  /**
   * Tests the getDefaultReceivedFrom method with input business partner ID.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultReceivedFromWithInpBPartnerId() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpcBpartnerId", "TEST_BPARTNER_INP");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultReceivedFrom(requestMap);

    // Then
    assertEquals("TEST_BPARTNER_INP", result);
  }

  /**
   * Tests the getDefaultReceivedFrom method with no business partner.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultReceivedFromNoBPartner() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultReceivedFrom(requestMap);

    // Then
    assertEquals("", result);
  }

  /**
   * Tests the getDefaultStandardPrecision method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultStandardPrecision() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.FIN_FINANCIAL_ACCOUNT_ID, TestConstants.TEST_FIN_ACCOUNT);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultStandardPrecision(requestMap);

    // Then
    assertEquals("2", result);
  }

  /**
   * Tests the getDefaultCurrency method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultCurrency() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.FIN_FINANCIAL_ACCOUNT_ID, TestConstants.TEST_FIN_ACCOUNT);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultCurrency(requestMap);

    // Then
    assertEquals("USD", result);
  }

  /**
   * Tests the getOrganization method from context.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOrganizationFromContext() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("ad_org_id", "TEST_ORG");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getOrganization(requestMap);

    // Then
    assertEquals("TEST_ORG", result);
  }

  /**
   * Tests the getOrganization method from input organization ID.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOrganizationFromInpOrgId() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpadOrgId", "TEST_ORG_INP");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getOrganization(requestMap);

    // Then
    assertEquals("TEST_ORG_INP", result);
  }

  /**
   * Tests the getOrganization method from financial account.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOrganizationFromFinancialAccount() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.FIN_FINANCIAL_ACCOUNT_ID, TestConstants.TEST_FIN_ACCOUNT);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getOrganization(requestMap);

    // Then
    assertEquals("0", result);
  }

  /**
   * Tests the getDefaultPaymentMethod method with no suitable method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultPaymentMethodNoSuitableMethod() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.TRXTYPE, "BPD");
    context.put(TestConstants.FIN_FINANCIAL_ACCOUNT_ID, TestConstants.TEST_FIN_ACCOUNT);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // Setup payment methods with no suitable method
    List<FinAccPaymentMethod> paymentMethods = new ArrayList<>();
    FinAccPaymentMethod unsuitable = mock(FinAccPaymentMethod.class);
    when(unsuitable.isActive()).thenReturn(true);
    when(unsuitable.isDefault()).thenReturn(false);
    when(unsuitable.isPayinAllow()).thenReturn(false);
    paymentMethods.add(unsuitable);

    when(financialAccount.getFinancialMgmtFinAccPaymentMethodList()).thenReturn(paymentMethods);

    // When
    String result = classUnderTest.getDefaultPaymentMethod(requestMap);

    // Then
    assertEquals("", result);
  }

  /**
   * Tests the getDefaultDocument method for BPD transaction type.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultDocumentBPD() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.TRXTYPE, "BPD");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultDocument(requestMap);

    // Then
    assertEquals("RCIN", result);
  }

  /**
   * Tests the getDefaultDocument method for BPW transaction type.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultDocumentBPW() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.TRXTYPE, "BPW");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultDocument(requestMap);

    // Then
    assertEquals("PDOUT", result);
  }

  /**
   * Tests the getDefaultDocument method for other transaction types.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultDocumentOther() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.TRXTYPE, "OTHER");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getDefaultDocument(requestMap);

    // Then
    assertEquals("", result);
  }

  /**
   * Tests the getDefaultPaymentDate method using transaction date.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetDefaultPaymentDateFromTrxDate() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("trxdate", "2023-01-15");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // Setup date formatting
    Date testDate = new Date(123, 0, 15);
    mockedJsonUtils.when(JsonUtils::createDateFormat).thenReturn(new java.text.SimpleDateFormat("yyyy-MM-dd"));
    mockedOBDateUtils.when(() -> OBDateUtils.formatDate(testDate)).thenReturn(TestConstants.TEST_DATE);

    // When
    String result = classUnderTest.getDefaultPaymentDate(requestMap);

    // Then
    assertEquals(TestConstants.TEST_DATE, result);
  }

  /**
   * Tests the getDefaultPaymentDate method using statement date.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetDefaultPaymentDateFromStatementDate() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpstatementdate", TestConstants.TEST_DATE);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // Setup date formatting
    Date testDate = new Date(123, 0, 15);
    mockedOBDateUtils.when(() -> OBDateUtils.getDate(TestConstants.TEST_DATE)).thenReturn(testDate);
    mockedOBDateUtils.when(() -> OBDateUtils.formatDate(testDate)).thenReturn(TestConstants.TEST_DATE);

    // When
    String result = classUnderTest.getDefaultPaymentDate(requestMap);

    // Then
    assertEquals(TestConstants.TEST_DATE, result);
  }

  /**
   * Tests the getDefaultPaymentDate method using the current date.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetDefaultPaymentDateCurrentDate() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    requestMap.put(TestConstants.CONTEXT, context.toString());

    mockedOBDateUtils.when(() -> OBDateUtils.formatDate(org.mockito.ArgumentMatchers.any(Date.class))).thenReturn(
        TestConstants.TEST_DATE);

    // When
    String result = classUnderTest.getDefaultPaymentDate(requestMap);

    // Then
    assertEquals(TestConstants.TEST_DATE, result);
  }

  /**
   * Tests the getBankStatementLineAmount method with deposit and withdrawal amounts.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetBankStatementLineAmountWithDepositAndWithdrawal() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("depositamt", TestConstants.AMOUNT);
    context.put("withdrawalamt", TestConstants.WITHDRAWAL_AMOUNT);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getBankStatementLineAmount(requestMap);

    // Then
    assertEquals(TestConstants.DEPOSIT_AMOUNT, result);
  }

  /**
   * Tests the getBankStatementLineAmount method with input deposit and payment amounts.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetBankStatementLineAmountWithInpDepositAndPayment() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpdepositamt", TestConstants.AMOUNT);
    context.put("inppaymentamt", TestConstants.WITHDRAWAL_AMOUNT);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getBankStatementLineAmount(requestMap);

    // Then
    assertEquals(TestConstants.DEPOSIT_AMOUNT, result);
  }

  /**
   * Tests the getBankStatementLineAmount method with no amounts.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetBankStatementLineAmountNoAmounts() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    String result = classUnderTest.getBankStatementLineAmount(requestMap);

    // Then
    assertEquals("0", result);
  }

  /**
   * Tests the getFinancialAccount method.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetFinancialAccount() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.FIN_FINANCIAL_ACCOUNT_ID, TestConstants.TEST_FIN_ACCOUNT);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    // We need to use reflection to access the private method
    java.lang.reflect.Method method = TransactionAddPaymentDefaultValues.class.getDeclaredMethod("getFinancialAccount",
        Map.class);
    method.setAccessible(true);
    FIN_FinancialAccount result = (FIN_FinancialAccount) method.invoke(classUnderTest, requestMap);

    // Then
    assertNotNull(result);
    assertEquals(financialAccount, result);
  }

  /**
   * Tests the getFinancialAccount method using input financial account ID.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetFinancialAccountFromInpFinFinancialAccountId() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpfinFinancialAccountId", TestConstants.TEST_FIN_ACCOUNT);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    // We need to use reflection to access the private method
    java.lang.reflect.Method method = TransactionAddPaymentDefaultValues.class.getDeclaredMethod("getFinancialAccount",
        Map.class);
    method.setAccessible(true);
    FIN_FinancialAccount result = (FIN_FinancialAccount) method.invoke(classUnderTest, requestMap);

    // Then
    assertNotNull(result);
    assertEquals(financialAccount, result);
  }
}
