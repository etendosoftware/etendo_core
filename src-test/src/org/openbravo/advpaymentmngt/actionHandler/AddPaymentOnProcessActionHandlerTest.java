package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;

/**
 * Test class for {@link AddPaymentOnProcessActionHandler} which handles the processing of payment actions.
 * This test suite verifies the functionality of payment processing, including validation of business partners,
 * currencies, and financial accounts.
 *
 * <p>The test class uses Mockito to mock static dependencies:</p>
 * <ul>
 *   <li>{@link OBDal} - For database access operations</li>
 *   <li>{@link OBContext} - For context management</li>
 *   <li>{@link FIN_Utility} - For financial utilities</li>
 *   <li>{@link OBMessageUtils} - For message handling</li>
 * </ul>
 */
public class AddPaymentOnProcessActionHandlerTest {

  private static final String BUSINESS_PARTNER_ID = "TEST_BP_ID";
  private static final String FINANCIAL_ACCOUNT_ID = "TEST_FIN_ACC_ID";
  private static final String CURRENCY_ID = "TEST_CURR_ID";
  private static final String BP_NAME = "Test Business Partner";
  private static final String ISO_CODE = "USD";
  private static final BigDecimal WRITE_OFF_LIMIT = new BigDecimal("1000");

  /**
   * Rule for testing expected exceptions.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AddPaymentOnProcessActionHandler actionHandler;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<FIN_Utility> mockedFINUtility;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;

  @Mock
  private OBDal mockOBDal;
  @Mock
  private BusinessPartner mockBusinessPartner;
  @Mock
  private FIN_FinancialAccount mockFinancialAccount;
  @Mock
  private Currency mockCurrency;

  /**
   * Sets up the test environment before each test method.
   * Initializes mocks and configures default behavior.
   *
   * @throws Exception
   *     if any error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    actionHandler = new AddPaymentOnProcessActionHandler();

    // Setup static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedFINUtility = mockStatic(FIN_Utility.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);

    // Configure OBDal mock
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    when(mockOBDal.get(eq(FIN_FinancialAccount.class), anyString())).thenReturn(mockFinancialAccount);
    when(mockOBDal.get(eq(BusinessPartner.class), anyString())).thenReturn(mockBusinessPartner);

    // Configure BusinessPartner mock with base data
    setupBusinessPartnerMock();

    // Configure Financial Account mock with base data
    setupFinancialAccountMock();

    // Configure Currency mock with base data
    setupCurrencyMock();

    // Configure default FIN_Utility behavior
    mockedFINUtility.when(() -> FIN_Utility.isBlockedBusinessPartner(anyString(), anyBoolean(), anyInt())).thenReturn(
        false);
  }

  /**
   * Configures the mock BusinessPartner with default test data.
   */
  private void setupBusinessPartnerMock() {
    when(mockBusinessPartner.getId()).thenReturn(BUSINESS_PARTNER_ID);
    when(mockBusinessPartner.getName()).thenReturn(BP_NAME);
    when(mockBusinessPartner.getCurrency()).thenReturn(mockCurrency);
  }

  /**
   * Configures the mock FinancialAccount with default test data.
   */
  private void setupFinancialAccountMock() {
    when(mockFinancialAccount.getId()).thenReturn(FINANCIAL_ACCOUNT_ID);
    when(mockFinancialAccount.getWriteofflimit()).thenReturn(WRITE_OFF_LIMIT);
  }

  /**
   * Configures the mock Currency with default test data.
   */
  private void setupCurrencyMock() {
    when(mockCurrency.getId()).thenReturn(CURRENCY_ID);
    when(mockCurrency.getISOCode()).thenReturn(ISO_CODE);
  }

  /**
   * Cleans up resources after each test method.
   * Closes all static mocks to prevent memory leaks.
   */
  @After
  public void tearDown() {
    mockedOBDal.close();
    mockedOBContext.close();
    mockedFINUtility.close();
    mockedOBMessageUtils.close();
  }

  /**
   * Tests successful execution of a valid payment process.
   * Verifies that the handler returns a success message when all conditions are met.
   *
   * @throws Exception
   *     if any error occurs during test execution
   */
  @Test
  public void testExecuteValidPaymentSuccess() throws Exception {
    // Prepare test data
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = createJsonData(false, false);

    // Execute
    JSONObject result = actionHandler.execute(parameters, jsonData);

    // Verify
    assertTrue(result.has(TestConstants.RESPONSE_MESSAGE));
    JSONObject message = result.getJSONObject(TestConstants.RESPONSE_MESSAGE);
    assertEquals(TestConstants.RESULT_SUCCESS, message.getString(TestConstants.SEVERITY));
    assertEquals("Ok", message.getString("text"));
  }

  /**
   * Tests execution with a blocked business partner.
   * Verifies that the handler returns an error message when the business partner is blocked.
   *
   * @throws Exception
   *     if any error occurs during test execution
   */
  @Test
  public void testExecuteBlockedBusinessPartnerReturnsError() throws Exception {
    // Prepare test data
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = createJsonData(false, false);

    // Configure mocks for blocked BP scenario
    mockedFINUtility.when(
        () -> FIN_Utility.isBlockedBusinessPartner(eq(BUSINESS_PARTNER_ID), eq(true), eq(4))).thenReturn(true);
    mockedOBMessageUtils.when(() -> OBMessageUtils.parseTranslation("@ThebusinessPartner@")).thenReturn(
        "The Business Partner");
    mockedOBMessageUtils.when(() -> OBMessageUtils.parseTranslation("@BusinessPartnerBlocked@")).thenReturn(
        "is blocked");

    // Execute
    JSONObject result = actionHandler.execute(parameters, jsonData);

    // Verify
    assertTrue(result.has(TestConstants.RESPONSE_MESSAGE));
    JSONObject message = result.getJSONObject(TestConstants.RESPONSE_MESSAGE);
    assertEquals("error", message.getString(TestConstants.SEVERITY));
    assertTrue(message.getString("text").contains(BP_NAME));
    assertTrue(message.getString("text").contains("is blocked"));
  }

  /**
   * Tests execution with a business partner that has no currency configured.
   * Verifies that the handler returns an error message when the business partner lacks currency configuration.
   *
   * @throws Exception
   *     if any error occurs during test execution
   */
  @Test
  public void testExecuteBusinessPartnerWithoutCurrencyReturnsError() throws Exception {
    // Prepare test data
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = createJsonData(false, false);

    // Configure BP without currency
    when(mockBusinessPartner.getCurrency()).thenReturn(null);

    // Configure message mock
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("InitBPCurrencyLnk", false)).thenReturn(
        "Business Partner %s (%s) does not have a currency");

    // Execute
    JSONObject result = actionHandler.execute(parameters, jsonData);

    // Verify
    assertTrue(result.has(TestConstants.RESPONSE_MESSAGE));
    JSONObject message = result.getJSONObject(TestConstants.RESPONSE_MESSAGE);
    assertEquals("error", message.getString(TestConstants.SEVERITY));
    assertTrue(message.getString("text").contains("Business Partner"));
  }

  /**
   * Creates a JSON string representing payment data for testing.
   *
   * @param usesCredit
   *     whether the payment uses credit
   * @param generatesCredit
   *     whether the payment generates credit
   * @return JSON string containing payment data
   */
  private String createJsonData(boolean usesCredit, boolean generatesCredit) {
    return createJsonData(usesCredit, generatesCredit, CURRENCY_ID);
  }

  /**
   * Creates a JSON string representing payment data for testing with a specific currency.
   *
   * @param usesCredit
   *     whether the payment uses credit
   * @param generatesCredit
   *     whether the payment generates credit
   * @param currencyId
   *     the ID of the currency to use
   * @return JSON string containing payment data
   */
  private String createJsonData(boolean usesCredit, boolean generatesCredit, String currencyId) {
    StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append("{")
        .append("\"issotrx\": \"true\",")
        .append("\"finFinancialAccount\": \"").append(FINANCIAL_ACCOUNT_ID).append("\",")
        .append("\"receivedFrom\": \"").append(BUSINESS_PARTNER_ID).append("\",")
        .append("\"currencyId\": \"").append(currencyId).append("\",")
        .append("\"usesCredit\": ").append(usesCredit).append(",")
        .append("\"generatesCredit\": ").append(generatesCredit)
        .append("}");

    return jsonBuilder.toString();
  }
}
