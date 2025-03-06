package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Criterion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.service.json.JsonUtils;

@RunWith(MockitoJUnitRunner.class)
public class PaymentMethodMulticurrencyActionHandlerTest {

  // ========== MOCKS ==========
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<FinancialUtils> mockedFinancialUtils;
  private MockedStatic<JsonUtils> mockedJsonUtils;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBCriteria<FinAccPaymentMethod> mockCriteria;

  @Mock
  private FIN_FinancialAccount mockFinancialAccount;

  @Mock
  private FIN_PaymentMethod mockPaymentMethod;

  @Mock
  private FinAccPaymentMethod mockFinAccPaymentMethod;

  @Mock
  private Currency mockCurrency;

  @Mock
  private Currency mockCurrencyTo;

  @Mock
  private Organization mockOrganization;

  @Mock
  private ConversionRate mockConversionRate;

  @Mock
  private SimpleDateFormat mockDateFormat;

  // ========== CLASS TO TEST ==========
  @InjectMocks
  private PaymentMethodMulticurrencyActionHandler handlerUnderTest;

  // ========== SETUP ==========
  @Before
  public void setUp() throws Exception {
    // Initialize static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedFinancialUtils = mockStatic(FinancialUtils.class);
    mockedJsonUtils = mockStatic(JsonUtils.class);

    // Setup OBDal mock
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    // Setup OBCriteria
    when(mockOBDal.createCriteria(FinAccPaymentMethod.class)).thenReturn(mockCriteria);
    when(mockCriteria.setFilterOnReadableOrganization(false)).thenReturn(mockCriteria);
    when(mockCriteria.setMaxResults(1)).thenReturn(mockCriteria);
    when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);

    // Setup date format
    mockDateFormat = mock(SimpleDateFormat.class);
    mockedJsonUtils.when(JsonUtils::createDateFormat).thenReturn(mockDateFormat);
  }

  @After
  public void tearDown() {
    // Close all static mocks
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedFinancialUtils != null) {
      mockedFinancialUtils.close();
    }
    if (mockedJsonUtils != null) {
      mockedJsonUtils.close();
    }
  }

  // ========== TESTS ==========

  /**
   * Test the execute method with a financial account that has the same currency as the payment
   */
  @Test
  public void testExecute_SameCurrency() throws Exception {
    // GIVEN
    String paymentMethodId = "PM1";
    String financialAccountId = "FA1";
    String currencyId = "C1";
    boolean isSOTrx = true;
    String strPaymentDate = "2023-01-01";
    Date paymentDate = new Date();
    String orgId = "ORG1";

    // Setup JSON data
    String jsonData = "{" + "\"paymentMethodId\":\"" + paymentMethodId + "\"," + "\"financialAccountId\":\"" + financialAccountId + "\"," + "\"isSOTrx\":" + isSOTrx + "," + "\"currencyId\":\"" + currencyId + "\"," + "\"paymentDate\":\"" + strPaymentDate + "\"," + "\"orgId\":\"" + orgId + "\"" + "}";

    // Setup mock objects
    when(mockOBDal.get(FIN_FinancialAccount.class, financialAccountId)).thenReturn(mockFinancialAccount);
    when(mockOBDal.get(FIN_PaymentMethod.class, paymentMethodId)).thenReturn(mockPaymentMethod);
    when(mockDateFormat.parse(strPaymentDate)).thenReturn(paymentDate);

    when(mockFinancialAccount.getCurrency()).thenReturn(mockCurrency);
    when(mockCurrency.getId()).thenReturn(currencyId);
    when(mockCurrency.getIdentifier()).thenReturn("Currency 1");

    when(mockCriteria.uniqueResult()).thenReturn(mockFinAccPaymentMethod);
    when(mockFinAccPaymentMethod.getAccount()).thenReturn(mockFinancialAccount);
    when(mockFinAccPaymentMethod.isPayinIsMulticurrency()).thenReturn(true);
    when(mockFinAccPaymentMethod.isPayinAllow()).thenReturn(true);

    Map<String, Object> parameters = new HashMap<>();

    // WHEN
    JSONObject result = handlerUnderTest.execute(parameters, jsonData);

    // THEN
    assertNotNull("Result should not be null", result);
    assertEquals("Conversion rate should be 1 for same currency", 1, result.getInt("conversionrate"));
    assertTrue("Is multicurrency should be true", result.getBoolean("isPayIsMulticurrency"));
    assertFalse("Wrong financial account should be false", result.getBoolean("isWrongFinancialAccount"));
    assertEquals("CurrencyToId should match", currencyId, result.getString("currencyToId"));

    // Verify interactions - Note: We're not verifying exact call counts for getAccount() and isPayinIsMulticurrency()
    // as they're called multiple times internally, which was causing the test failures
    verify(mockCriteria).uniqueResult();
    // Make sure these methods are called at least once, but don't specify exact count
    verify(mockFinAccPaymentMethod, atLeastOnce()).isPayinIsMulticurrency();
    verify(mockFinAccPaymentMethod, atLeastOnce()).getAccount();
    verify(mockFinancialAccount, atLeastOnce()).getCurrency();
  }

  /**
   * Test the execute method with a different currency requiring conversion
   */
  @Test
  public void testExecute_DifferentCurrency() throws Exception {
    // GIVEN
    String paymentMethodId = "PM1";
    String financialAccountId = "FA1";
    String currencyId = "C1";
    String currencyToId = "C2";
    boolean isSOTrx = true;
    String strPaymentDate = "2023-01-01";
    Date paymentDate = new Date();
    String orgId = "ORG1";
    BigDecimal conversionRate = new BigDecimal("1.25");

    // Setup JSON data
    String jsonData = "{" + "\"paymentMethodId\":\"" + paymentMethodId + "\"," + "\"financialAccountId\":\"" + financialAccountId + "\"," + "\"isSOTrx\":" + isSOTrx + "," + "\"currencyId\":\"" + currencyId + "\"," + "\"paymentDate\":\"" + strPaymentDate + "\"," + "\"orgId\":\"" + orgId + "\"" + "}";

    // Setup mock objects
    when(mockOBDal.get(FIN_FinancialAccount.class, financialAccountId)).thenReturn(mockFinancialAccount);
    when(mockOBDal.get(FIN_PaymentMethod.class, paymentMethodId)).thenReturn(mockPaymentMethod);
    when(mockOBDal.get(Currency.class, currencyId)).thenReturn(mockCurrency);
    when(mockOBDal.get(Organization.class, orgId)).thenReturn(mockOrganization);
    when(mockDateFormat.parse(strPaymentDate)).thenReturn(paymentDate);

    when(mockFinancialAccount.getCurrency()).thenReturn(mockCurrencyTo);
    when(mockCurrencyTo.getId()).thenReturn(currencyToId);
    when(mockCurrencyTo.getIdentifier()).thenReturn("Currency 2");

    when(mockCriteria.uniqueResult()).thenReturn(mockFinAccPaymentMethod);
    when(mockFinAccPaymentMethod.getAccount()).thenReturn(mockFinancialAccount);
    when(mockFinAccPaymentMethod.isPayinIsMulticurrency()).thenReturn(true);

    when(mockOrganization.getClient()).thenReturn(null);

    // Setup conversion rate
    mockedFinancialUtils.when(
        () -> FinancialUtils.getConversionRate(eq(paymentDate), eq(mockCurrency), eq(mockCurrencyTo),
            eq(mockOrganization), any())).thenReturn(mockConversionRate);
    when(mockConversionRate.getMultipleRateBy()).thenReturn(conversionRate);

    Map<String, Object> parameters = new HashMap<>();

    // WHEN
    JSONObject result = handlerUnderTest.execute(parameters, jsonData);

    // THEN
    assertNotNull("Result should not be null", result);
    assertEquals("Conversion rate should match", conversionRate.toString(), result.get("conversionrate").toString());
    assertTrue("Is multicurrency should be true", result.getBoolean("isPayIsMulticurrency"));
    assertFalse("Wrong financial account should be false", result.getBoolean("isWrongFinancialAccount"));
    assertEquals("CurrencyToId should match", currencyToId, result.getString("currencyToId"));

    // Verify interactions - Using atLeastOnce() instead of specific counts
    verify(mockCriteria).uniqueResult();
    verify(mockFinAccPaymentMethod, atLeastOnce()).isPayinIsMulticurrency();
    verify(mockFinAccPaymentMethod, atLeastOnce()).getAccount();
    verify(mockFinancialAccount, atLeastOnce()).getCurrency();
    mockedFinancialUtils.verify(() -> FinancialUtils.getConversionRate(any(), any(), any(), any(), any()));
  }

  /**
   * Test the execution when no conversion rate is found
   */
  @Test
  public void testExecute_NoConversionRate() throws Exception {
    // GIVEN
    String paymentMethodId = "PM1";
    String financialAccountId = "FA1";
    String currencyId = "C1";
    String currencyToId = "C2";
    boolean isSOTrx = true;
    String strPaymentDate = "2023-01-01";
    Date paymentDate = new Date();
    String orgId = "ORG1";

    // Setup JSON data
    String jsonData = "{" + "\"paymentMethodId\":\"" + paymentMethodId + "\"," + "\"financialAccountId\":\"" + financialAccountId + "\"," + "\"isSOTrx\":" + isSOTrx + "," + "\"currencyId\":\"" + currencyId + "\"," + "\"paymentDate\":\"" + strPaymentDate + "\"," + "\"orgId\":\"" + orgId + "\"" + "}";

    // Setup mock objects
    when(mockOBDal.get(FIN_FinancialAccount.class, financialAccountId)).thenReturn(mockFinancialAccount);
    when(mockOBDal.get(FIN_PaymentMethod.class, paymentMethodId)).thenReturn(mockPaymentMethod);
    when(mockOBDal.get(Currency.class, currencyId)).thenReturn(mockCurrency);
    when(mockOBDal.get(Organization.class, orgId)).thenReturn(mockOrganization);
    when(mockDateFormat.parse(strPaymentDate)).thenReturn(paymentDate);

    when(mockFinancialAccount.getCurrency()).thenReturn(mockCurrencyTo);
    when(mockCurrencyTo.getId()).thenReturn(currencyToId);
    when(mockCurrencyTo.getIdentifier()).thenReturn("Currency 2");

    when(mockCriteria.uniqueResult()).thenReturn(mockFinAccPaymentMethod);
    when(mockFinAccPaymentMethod.getAccount()).thenReturn(mockFinancialAccount);
    when(mockFinAccPaymentMethod.isPayinIsMulticurrency()).thenReturn(true);

    when(mockOrganization.getClient()).thenReturn(null);

    // Set up null conversion rate
    mockedFinancialUtils.when(
        () -> FinancialUtils.getConversionRate(eq(paymentDate), eq(mockCurrency), eq(mockCurrencyTo),
            eq(mockOrganization), any())).thenReturn(null);

    Map<String, Object> parameters = new HashMap<>();

    // WHEN
    JSONObject result = handlerUnderTest.execute(parameters, jsonData);

    // THEN
    assertNotNull("Result should not be null", result);
    assertEquals("Conversion rate should be empty", "", result.getString("conversionrate"));
    assertEquals("Converted amount should be empty", "", result.getString("convertedamount"));
    assertTrue("Is multicurrency should be true", result.getBoolean("isPayIsMulticurrency"));
    assertFalse("Wrong financial account should be false", result.getBoolean("isWrongFinancialAccount"));
    assertEquals("CurrencyToId should match", currencyToId, result.getString("currencyToId"));

    // Verify interactions - Using atLeastOnce() instead of specific counts
    verify(mockCriteria).uniqueResult();
    verify(mockFinAccPaymentMethod, atLeastOnce()).isPayinIsMulticurrency();
    verify(mockFinAccPaymentMethod, atLeastOnce()).getAccount();
    verify(mockFinancialAccount, atLeastOnce()).getCurrency();
    mockedFinancialUtils.verify(() -> FinancialUtils.getConversionRate(any(), any(), any(), any(), any()));
  }

  /**
   * Test when null currency is provided and financial account is used to determine currency
   */
  @Test
  public void testExecute_NullCurrency() throws Exception {
    // GIVEN
    String paymentMethodId = "PM1";
    String financialAccountId = "FA1";
    String currencyId = "C1";
    boolean isSOTrx = true;
    String strPaymentDate = "2023-01-01";
    Date paymentDate = new Date();
    String orgId = "ORG1";

    // Setup JSON data
    String jsonData = "{" + "\"paymentMethodId\":\"" + paymentMethodId + "\"," + "\"financialAccountId\":\"" + financialAccountId + "\"," + "\"isSOTrx\":" + isSOTrx + "," + "\"currencyId\":\"null\"," + "\"paymentDate\":\"" + strPaymentDate + "\"," + "\"orgId\":\"" + orgId + "\"" + "}";

    // Setup mock objects
    when(mockOBDal.get(FIN_FinancialAccount.class, financialAccountId)).thenReturn(mockFinancialAccount);
    when(mockOBDal.get(FIN_PaymentMethod.class, paymentMethodId)).thenReturn(mockPaymentMethod);
    when(mockDateFormat.parse(strPaymentDate)).thenReturn(paymentDate);

    when(mockFinancialAccount.getCurrency()).thenReturn(mockCurrency);
    when(mockCurrency.getId()).thenReturn(currencyId);
    when(mockCurrency.getIdentifier()).thenReturn("Currency 1");

    when(mockCriteria.uniqueResult()).thenReturn(mockFinAccPaymentMethod);
    when(mockFinAccPaymentMethod.getAccount()).thenReturn(mockFinancialAccount);
    when(mockFinAccPaymentMethod.isPayinIsMulticurrency()).thenReturn(true);
    when(mockFinAccPaymentMethod.isPayinAllow()).thenReturn(true);

    Map<String, Object> parameters = new HashMap<>();

    // WHEN
    JSONObject result = handlerUnderTest.execute(parameters, jsonData);

    // THEN
    assertNotNull("Result should not be null", result);
    assertEquals("Conversion rate should be 1 for same currency", 1, result.getInt("conversionrate"));
    assertTrue("Is multicurrency should be true", result.getBoolean("isPayIsMulticurrency"));
    assertFalse("Wrong financial account should be false", result.getBoolean("isWrongFinancialAccount"));
    assertEquals("CurrencyId should match", currencyId, result.getString("currencyId"));
    assertEquals("CurrencyIdIdentifier should match", "Currency 1", result.getString("currencyIdIdentifier"));

    // Verify interactions - Using atLeastOnce() instead of specific counts
    verify(mockCriteria).uniqueResult();
    verify(mockFinAccPaymentMethod, atLeastOnce()).isPayinIsMulticurrency();
    verify(mockFinAccPaymentMethod, atLeastOnce()).getAccount();
    verify(mockFinancialAccount, atLeastOnce()).getCurrency();
  }

  /**
   * Test the exception handling of the execute method
   */
  @Test(expected = OBException.class)
  public void testExecute_Exception() throws Exception {
    // GIVEN
    String invalidJsonData = "{invalid json";
    Map<String, Object> parameters = new HashMap<>();

    // WHEN
    handlerUnderTest.execute(parameters, invalidJsonData);

    // THEN: Exception should be thrown
  }

  /**
   * Test when the FinAccPaymentMethod is null
   */
  @Test
  public void testExecute_NullFinAccPaymentMethod() throws Exception {
    // GIVEN
    String paymentMethodId = "PM1";
    String financialAccountId = "FA1";
    String currencyId = "C1";
    boolean isSOTrx = true;
    String strPaymentDate = "2023-01-01";
    Date paymentDate = new Date();
    String orgId = "ORG1";

    // Setup JSON data
    String jsonData = "{" + "\"paymentMethodId\":\"" + paymentMethodId + "\"," + "\"financialAccountId\":\"" + financialAccountId + "\"," + "\"isSOTrx\":" + isSOTrx + "," + "\"currencyId\":\"" + currencyId + "\"," + "\"paymentDate\":\"" + strPaymentDate + "\"," + "\"orgId\":\"" + orgId + "\"" + "}";

    // Setup mock objects
    when(mockOBDal.get(FIN_FinancialAccount.class, financialAccountId)).thenReturn(mockFinancialAccount);
    when(mockDateFormat.parse(strPaymentDate)).thenReturn(paymentDate);

    when(mockCriteria.uniqueResult()).thenReturn(null);

    Map<String, Object> parameters = new HashMap<>();

    // WHEN
    JSONObject result = handlerUnderTest.execute(parameters, jsonData);

    // THEN
    assertNotNull("Result should not be null", result);
    assertFalse("Is multicurrency should be false", result.getBoolean("isPayIsMulticurrency"));
    assertTrue("Wrong financial account should be true", result.getBoolean("isWrongFinancialAccount"));
    assertEquals("Conversion rate should be 1", 1, result.getInt("conversionrate"));
    assertEquals("CurrencyToId should match", currencyId, result.getString("currencyToId"));

    // Verify interactions
    verify(mockCriteria).uniqueResult();
  }

  /**
   * Test for isValidFinancialAccount method using reflection
   */
  @Test
  public void testIsValidFinancialAccount() throws Exception {
    // GIVEN
    String currencyId = "C1";
    boolean isSOTrx = true;

    // Setup mock objects
    when(mockFinAccPaymentMethod.getAccount()).thenReturn(mockFinancialAccount);
    when(mockFinancialAccount.getCurrency()).thenReturn(mockCurrency);
    when(mockCurrency.getId()).thenReturn(currencyId);
    when(mockFinAccPaymentMethod.isPayinAllow()).thenReturn(true);

    // Access private method using reflection
    Method isValidFinancialAccountMethod = PaymentMethodMulticurrencyActionHandler.class.getDeclaredMethod(
        "isValidFinancialAccount", FinAccPaymentMethod.class, String.class, boolean.class);
    isValidFinancialAccountMethod.setAccessible(true);

    // WHEN - Same currency with payin allowed
    boolean result1 = (boolean) isValidFinancialAccountMethod.invoke(handlerUnderTest, mockFinAccPaymentMethod,
        currencyId, isSOTrx);

    // THEN
    assertTrue("Should be valid with same currency and payin allowed", result1);

    // WHEN - Same currency with payin not allowed
    when(mockFinAccPaymentMethod.isPayinAllow()).thenReturn(false);
    boolean result2 = (boolean) isValidFinancialAccountMethod.invoke(handlerUnderTest, mockFinAccPaymentMethod,
        currencyId, isSOTrx);

    // THEN
    assertFalse("Should be invalid with same currency and payin not allowed", result2);

    // WHEN - Different currency with multicurrency allowed
    String differentCurrencyId = "C2";
    when(mockFinAccPaymentMethod.isPayinIsMulticurrency()).thenReturn(true);
    boolean result3 = (boolean) isValidFinancialAccountMethod.invoke(handlerUnderTest, mockFinAccPaymentMethod,
        differentCurrencyId, isSOTrx);

    // THEN
    assertTrue("Should be valid with different currency and multicurrency allowed", result3);

    // WHEN - Different currency with multicurrency not allowed
    when(mockFinAccPaymentMethod.isPayinIsMulticurrency()).thenReturn(false);
    boolean result4 = (boolean) isValidFinancialAccountMethod.invoke(handlerUnderTest, mockFinAccPaymentMethod,
        differentCurrencyId, isSOTrx);

    // THEN
    assertFalse("Should be invalid with different currency and multicurrency not allowed", result4);

    // WHEN - Null FinAccPaymentMethod
    boolean result5 = (boolean) isValidFinancialAccountMethod.invoke(handlerUnderTest, null, currencyId, isSOTrx);

    // THEN
    assertFalse("Should be invalid with null FinAccPaymentMethod", result5);
  }
}
