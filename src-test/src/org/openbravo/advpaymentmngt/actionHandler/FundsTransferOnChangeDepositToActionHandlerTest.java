package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.openbravo.test.base.mock.MockitoStaticMockUtils.mockStaticSafely;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

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
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;

/**
 * Test class for FundsTransferOnChangeDepositToActionHandler.
 */
public class FundsTransferOnChangeDepositToActionHandlerTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private OBDal obDal;

  @Mock
  private FIN_FinancialAccount mockFinancialAccount;

  @Mock
  private Currency mockCurrency;

  @InjectMocks
  private FundsTransferOnChangeDepositToActionHandler handler;

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

    // Setup static mock for OBDal
    mockedOBDal = mockStaticSafely(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

    // Setup Currency mock
    when(mockCurrency.getId()).thenReturn(TestConstants.TEST_CURRENCY_ID);
    when(mockCurrency.getISOCode()).thenReturn(TestConstants.CURRENCY_IDENTIFIER);

    // Setup Financial Account mock
    when(mockFinancialAccount.getCurrency()).thenReturn(mockCurrency);

    // Setup OBDal mock
    when(obDal.get(FIN_FinancialAccount.class, TestConstants.TEST_ACCOUNT_ID)).thenReturn(mockFinancialAccount);
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
   * Tests the execute method with a valid account, expecting correct currency information.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteWithValidAccountReturnsCorrectCurrencyInfo() throws Exception {
    // Given
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = String.format("{\"accountID\":\"%s\"}", TestConstants.TEST_ACCOUNT_ID);

    // When
    JSONObject result = handler.execute(parameters, jsonData);

    // Then
    assertNotNull(TestConstants.RESULT_NOT_NULL_MESSAGE, result);
    assertEquals("Currency ID should match", TestConstants.TEST_CURRENCY_ID, result.getString("currencyID"));
    assertEquals("Currency ISO should match", TestConstants.CURRENCY_IDENTIFIER, result.getString("currencyISO"));
  }

  /**
   * Tests the execute method with a null account, expecting an empty result.
   */
  @Test
  public void testExecuteWithNullAccountReturnsEmptyResult() {
    // Given
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = "{\"accountID\":null}";

    // When
    JSONObject result = handler.execute(parameters, jsonData);

    // Then
    assertNotNull(TestConstants.RESULT_NOT_NULL_MESSAGE, result);
    assertEquals(TestConstants.RESULT_SHOULD_BE_EMPTY, 0, result.length());
  }

  /**
   * Tests the execute method with an invalid account, expecting an empty result.
   */
  @Test
  public void testExecuteWithInvalidAccountReturnsEmptyResult() {
    // Given
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = "{\"accountID\":\"INVALID_ID\"}";
    when(obDal.get(FIN_FinancialAccount.class, "INVALID_ID")).thenReturn(null);

    // When
    JSONObject result = handler.execute(parameters, jsonData);

    // Then
    assertNotNull(TestConstants.RESULT_NOT_NULL_MESSAGE, result);
    assertEquals(TestConstants.RESULT_SHOULD_BE_EMPTY, 0, result.length());
  }

  /**
   * Tests the execute method with invalid JSON, expecting an empty result.
   */
  @Test
  public void testExecuteWithInvalidJsonReturnsEmptyResult() {
    // Given
    Map<String, Object> parameters = new HashMap<>();
    String invalidJson = "invalid json";

    // When
    JSONObject result = handler.execute(parameters, invalidJson);

    // Then
    assertNotNull(TestConstants.RESULT_NOT_NULL_MESSAGE, result);
    assertEquals(TestConstants.RESULT_SHOULD_BE_EMPTY, 0, result.length());
  }
}
