package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.advpaymentmngt.utility.APRM_MatchingUtility;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.model.sales.SalesRegion;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.json.JsonUtils;

/**
 * Unit tests for {@link AddTransactionActionHandler}.
 * Tests the action handler that creates and matches financial transactions
 * from bank statement lines with different transaction types (payment, GL item, bank fee).
 */
@SuppressWarnings({"java:S120", "java:S112"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class AddTransactionActionHandlerTest {

  private static final String VAL_100_00 = "100.00";
  private static final String VAL_50_00 = "50.00";
  private static final String VAL_200_00 = "200.00";
  private static final String APRM_BANK_FEE = "APRM_BankFee";
  private static final String BANK_FEE = "Bank Fee";
  private static final String RESPONSE_ACTIONS = "responseActions";
  private static final String RETRY_EXECUTION = "retryExecution";
  private static final String VAL_10_00 = "10.00";

  private static final String TEST_FINANCIAL_ACCOUNT_ID = "FA001";
  private static final String TEST_BSL_ID = "BSL001";
  private static final String TEST_TAB_ID = "TAB001";
  private static final String TEST_PAYMENT_ID = "PAY001";
  private static final String TEST_GLITEM_ID = "GLI001";
  private static final String TEST_ORG_ID = "ORG001";
  private static final String TEST_BP_ID = "BP001";
  private static final String TEST_PRODUCT_ID = "PROD001";
  private static final String TEST_PROJECT_ID = "PJ001";
  private static final String TEST_ACTIVITY_ID = "ACT001";
  private static final String TEST_SALESREGION_ID = "SR001";
  private static final String TEST_CAMPAIGN_ID = "MC001";
  private static final String TEST_USER1_ID = "U1001";
  private static final String TEST_USER2_ID = "U2001";
  private static final String TEST_COSTCENTER_ID = "CC001";

  private AddTransactionActionHandler handler;

  @Mock
  private OBDal mockOBDal;
  @Mock
  private RequestContext mockRequestContext;
  @Mock
  private VariablesSecureApp mockVars;
  @Mock
  private FIN_FinancialAccount mockAccount;
  @Mock
  private FIN_BankStatementLine mockBankStatementLine;
  @Mock
  private FIN_Payment mockPayment;
  @Mock
  private GLItem mockGLItem;
  @Mock
  private Organization mockOrganization;
  @Mock
  private BusinessPartner mockBusinessPartner;
  @Mock
  private Product mockProduct;
  @Mock
  private Project mockProject;
  @Mock
  private ABCActivity mockActivity;
  @Mock
  private SalesRegion mockSalesRegion;
  @Mock
  private Campaign mockCampaign;
  @Mock
  private UserDimension1 mockUser1;
  @Mock
  private UserDimension2 mockUser2;
  @Mock
  private Costcenter mockCostcenter;
  @Mock
  private Currency mockCurrency;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<RequestContext> requestContextStatic;
  private MockedStatic<APRM_MatchingUtility> matchingUtilityStatic;
  private MockedStatic<FIN_Utility> finUtilityStatic;
  private MockedStatic<OBMessageUtils> obMessageUtilsStatic;
  private MockedStatic<DbUtility> dbUtilityStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    handler = objenesis.newInstance(AddTransactionActionHandler.class);

    obDalStatic = mockStatic(OBDal.class);
    obContextStatic = mockStatic(OBContext.class);
    requestContextStatic = mockStatic(RequestContext.class);
    matchingUtilityStatic = mockStatic(APRM_MatchingUtility.class);
    finUtilityStatic = mockStatic(FIN_Utility.class);
    obMessageUtilsStatic = mockStatic(OBMessageUtils.class);
    dbUtilityStatic = mockStatic(DbUtility.class);

    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
    requestContextStatic.when(RequestContext::get).thenReturn(mockRequestContext);
    lenient().when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);

    setupDimensionMocks();
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (requestContextStatic != null) requestContextStatic.close();
    if (matchingUtilityStatic != null) matchingUtilityStatic.close();
    if (finUtilityStatic != null) finUtilityStatic.close();
    if (obMessageUtilsStatic != null) obMessageUtilsStatic.close();
    if (dbUtilityStatic != null) dbUtilityStatic.close();
  }

  /**
   * Tests doExecute with a payment transaction type.
   * Verifies that the payment path is taken when a valid payment ID is provided.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithPaymentTransaction() throws Exception {
    // Arrange
    String content = buildRequestContent("BPW", TEST_PAYMENT_ID, "null", VAL_100_00, "0.00",
        "Test payment description");

    lenient().when(mockOBDal.get(FIN_Payment.class, TEST_PAYMENT_ID)).thenReturn(mockPayment);
    when(mockPayment.isReceipt()).thenReturn(true);
    when(mockPayment.getFinancialTransactionAmount()).thenReturn(new BigDecimal(VAL_100_00));
    when(mockPayment.getDescription()).thenReturn("Payment desc");
    when(mockPayment.getCurrency()).thenReturn(mockCurrency);
    when(mockPayment.getFinancialTransactionConvertRate()).thenReturn(BigDecimal.ONE);
    when(mockPayment.getAmount()).thenReturn(new BigDecimal(VAL_100_00));

    finUtilityStatic.when(() -> FIN_Utility.getDepositAmount(eq(true), any(BigDecimal.class)))
        .thenReturn(new BigDecimal(VAL_100_00));
    finUtilityStatic.when(() -> FIN_Utility.getPaymentAmount(eq(true), any(BigDecimal.class)))
        .thenReturn(BigDecimal.ZERO);

    // Act
    JSONObject result = callDoExecute(content);

    // Assert
    assertNotNull(result);
    matchingUtilityStatic.verify(() -> APRM_MatchingUtility.createAndMatchFinancialTransaction(
        eq(TEST_FINANCIAL_ACCOUNT_ID), eq("BPW"), any(Date.class), eq(TEST_BSL_ID),
        any(), any(), any(), anyString(), any(), anyBoolean(),
        any(BigDecimal.class), any(BigDecimal.class), any(), any(), any(),
        any(), any(), any(), any(), any(), any(), any(), any(), any(),
        any(), any(), any(), anyBoolean()));
  }

  /**
   * Tests doExecute with a payment that has a blank description, using the payment's own description.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithPaymentFallbackDescription() throws Exception {
    // Arrange
    String content = buildRequestContent("BPW", TEST_PAYMENT_ID, "null", VAL_100_00, "0.00", "");

    lenient().when(mockOBDal.get(FIN_Payment.class, TEST_PAYMENT_ID)).thenReturn(mockPayment);
    when(mockPayment.isReceipt()).thenReturn(false);
    when(mockPayment.getFinancialTransactionAmount()).thenReturn(new BigDecimal(VAL_50_00));
    when(mockPayment.getDescription()).thenReturn("Original payment\ndescription");
    when(mockPayment.getCurrency()).thenReturn(mockCurrency);
    when(mockPayment.getFinancialTransactionConvertRate()).thenReturn(BigDecimal.ONE);
    when(mockPayment.getAmount()).thenReturn(new BigDecimal(VAL_50_00));

    finUtilityStatic.when(() -> FIN_Utility.getDepositAmount(eq(false), any(BigDecimal.class)))
        .thenReturn(BigDecimal.ZERO);
    finUtilityStatic.when(() -> FIN_Utility.getPaymentAmount(eq(false), any(BigDecimal.class)))
        .thenReturn(new BigDecimal(VAL_50_00));

    // Act
    JSONObject result = callDoExecute(content);

    // Assert
    assertNotNull(result);
    matchingUtilityStatic.verify(() -> APRM_MatchingUtility.createAndMatchFinancialTransaction(
        anyString(), anyString(), any(Date.class), anyString(),
        any(), any(), any(), eq("Original payment. description"), any(), anyBoolean(),
        any(BigDecimal.class), any(BigDecimal.class), any(), any(), any(),
        any(), any(), any(), any(), any(), any(), any(), any(), any(),
        any(), any(), any(), anyBoolean()));
  }

  /**
   * Tests doExecute with a payment that has a null/blank description.
   * Verifies that the description is empty when both the user input and payment description are blank.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithPaymentNullDescription() throws Exception {
    // Arrange
    String content = buildRequestContent("BPW", TEST_PAYMENT_ID, "null", VAL_200_00, "0.00", "");

    lenient().when(mockOBDal.get(FIN_Payment.class, TEST_PAYMENT_ID)).thenReturn(mockPayment);
    when(mockPayment.isReceipt()).thenReturn(true);
    when(mockPayment.getFinancialTransactionAmount()).thenReturn(new BigDecimal(VAL_200_00));
    when(mockPayment.getDescription()).thenReturn("");
    when(mockPayment.getCurrency()).thenReturn(mockCurrency);
    when(mockPayment.getFinancialTransactionConvertRate()).thenReturn(BigDecimal.ONE);
    when(mockPayment.getAmount()).thenReturn(new BigDecimal(VAL_200_00));

    finUtilityStatic.when(() -> FIN_Utility.getDepositAmount(eq(true), any(BigDecimal.class)))
        .thenReturn(new BigDecimal(VAL_200_00));
    finUtilityStatic.when(() -> FIN_Utility.getPaymentAmount(eq(true), any(BigDecimal.class)))
        .thenReturn(BigDecimal.ZERO);

    // Act
    JSONObject result = callDoExecute(content);

    // Assert
    assertNotNull(result);
    matchingUtilityStatic.verify(() -> APRM_MatchingUtility.createAndMatchFinancialTransaction(
        anyString(), anyString(), any(Date.class), anyString(),
        any(), any(), any(), eq(""), any(), anyBoolean(),
        any(BigDecimal.class), any(BigDecimal.class), any(), any(), any(),
        any(), any(), any(), any(), any(), any(), any(), any(), any(),
        any(), any(), any(), anyBoolean()));
  }

  /**
   * Tests doExecute with a GL item transaction type.
   * Verifies that the GL item path is taken when a valid GL item ID is provided.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithGLItemTransaction() throws Exception {
    // Arrange
    String content = buildRequestContent("BPW", "null", TEST_GLITEM_ID, "75.50", "0.00", "");

    lenient().when(mockOBDal.get(GLItem.class, TEST_GLITEM_ID)).thenReturn(mockGLItem);
    when(mockGLItem.getName()).thenReturn("Test GL Item");
    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD("APRM_GLItem"))
        .thenReturn("GL Item");

    // Act
    JSONObject result = callDoExecute(content);

    // Assert
    assertNotNull(result);
    matchingUtilityStatic.verify(() -> APRM_MatchingUtility.createAndMatchFinancialTransaction(
        anyString(), anyString(), any(Date.class), anyString(),
        any(), any(), eq(null), eq("GL Item: Test GL Item"), eq(mockGLItem), eq(true),
        any(BigDecimal.class), any(BigDecimal.class), eq(null), eq(null), eq(null),
        any(), any(), any(), any(), any(), any(), any(), any(), any(),
        any(), any(), any(), anyBoolean()));
  }

  /**
   * Tests doExecute with a GL item transaction where user provides a custom description.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithGLItemAndCustomDescription() throws Exception {
    // Arrange
    String content = buildRequestContent("BPW", "null", TEST_GLITEM_ID, VAL_50_00, "0.00",
        "Custom GL description");

    lenient().when(mockOBDal.get(GLItem.class, TEST_GLITEM_ID)).thenReturn(mockGLItem);

    // Act
    JSONObject result = callDoExecute(content);

    // Assert
    assertNotNull(result);
    matchingUtilityStatic.verify(() -> APRM_MatchingUtility.createAndMatchFinancialTransaction(
        anyString(), anyString(), any(Date.class), anyString(),
        any(), any(), eq(null), eq("Custom GL description"), eq(mockGLItem), eq(true),
        any(BigDecimal.class), any(BigDecimal.class), eq(null), eq(null), eq(null),
        any(), any(), any(), any(), any(), any(), any(), any(), any(),
        any(), any(), any(), anyBoolean()));
  }

  /**
   * Tests doExecute with a GL item where description equals "null" string.
   * Verifies that "null" string is treated as blank description.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithGLItemAndNullStringDescription() throws Exception {
    // Arrange
    String content = buildRequestContent("BPW", "null", TEST_GLITEM_ID, "0.00", "30.00", "null");

    lenient().when(mockOBDal.get(GLItem.class, TEST_GLITEM_ID)).thenReturn(mockGLItem);
    when(mockGLItem.getName()).thenReturn("Fee Item");
    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD("APRM_GLItem"))
        .thenReturn("GL Item");

    // Act
    JSONObject result = callDoExecute(content);

    // Assert
    assertNotNull(result);
    // withdrawal > deposit, so isReceipt should be false
    matchingUtilityStatic.verify(() -> APRM_MatchingUtility.createAndMatchFinancialTransaction(
        anyString(), anyString(), any(Date.class), anyString(),
        any(), any(), eq(null), eq("GL Item: Fee Item"), eq(mockGLItem), eq(false),
        any(BigDecimal.class), any(BigDecimal.class), eq(null), eq(null), eq(null),
        any(), any(), any(), any(), any(), any(), any(), any(), any(),
        any(), any(), any(), anyBoolean()));
  }

  /**
   * Tests doExecute with a bank fee transaction type (BF).
   * Verifies that both payment and GL item IDs are forced to "null".
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithBankFeeTransaction() throws Exception {
    // Arrange
    String content = buildRequestContent("BF", TEST_PAYMENT_ID, TEST_GLITEM_ID, "0.00", "25.00",
        "");

    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD(APRM_BANK_FEE))
        .thenReturn(BANK_FEE);

    // Act
    JSONObject result = callDoExecute(content);

    // Assert
    assertNotNull(result);
    // BF type forces both paymentId and glitemId to "null", so it takes the else branch
    matchingUtilityStatic.verify(() -> APRM_MatchingUtility.createAndMatchFinancialTransaction(
        anyString(), eq("BF"), any(Date.class), anyString(),
        any(), any(), eq(null), eq(BANK_FEE), eq(null), eq(false),
        any(BigDecimal.class), any(BigDecimal.class), eq(null), eq(null), eq(null),
        any(), any(), any(), any(), any(), any(), any(), any(), any(),
        any(), any(), any(), anyBoolean()));
  }

  /**
   * Tests doExecute with bank fee and a custom description.
   * Verifies the custom description takes precedence over the default bank fee message.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithBankFeeAndCustomDescription() throws Exception {
    // Arrange
    String content = buildRequestContent("BF", "null", "null", "0.00", "15.00",
        "Custom bank fee desc");

    // Act
    JSONObject result = callDoExecute(content);

    // Assert
    assertNotNull(result);
    matchingUtilityStatic.verify(() -> APRM_MatchingUtility.createAndMatchFinancialTransaction(
        anyString(), eq("BF"), any(Date.class), anyString(),
        any(), any(), eq(null), eq("Custom bank fee desc"), eq(null), eq(false),
        any(BigDecimal.class), any(BigDecimal.class), eq(null), eq(null), eq(null),
        any(), any(), any(), any(), any(), any(), any(), any(), any(),
        any(), any(), any(), anyBoolean()));
  }

  /**
   * Tests doExecute with a transaction where deposit equals withdrawal (both zero).
   * Verifies isReceipt is true when deposit >= withdrawal.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithEqualDepositAndWithdrawal() throws Exception {
    // Arrange
    String content = buildRequestContent("BF", "null", "null", VAL_50_00, VAL_50_00, "Equal amounts");

    // Act
    JSONObject result = callDoExecute(content);

    // Assert
    assertNotNull(result);
    // deposit == withdrawal so isReceipt = true (>=)
    matchingUtilityStatic.verify(() -> APRM_MatchingUtility.createAndMatchFinancialTransaction(
        anyString(), anyString(), any(Date.class), anyString(),
        any(), any(), eq(null), eq("Equal amounts"), eq(null), eq(true),
        any(BigDecimal.class), any(BigDecimal.class), eq(null), eq(null), eq(null),
        any(), any(), any(), any(), any(), any(), any(), any(), any(),
        any(), any(), any(), anyBoolean()));
  }

  /**
   * Tests doExecute when an exception occurs during processing.
   * Verifies the error handling path including rollback and error message creation.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithException() throws Exception {
    // Arrange
    String content = buildRequestContent("BPW", "null", "null", "INVALID", "0.00", "");
    // "INVALID" will cause NumberFormatException when creating BigDecimal

    RuntimeException underlyingEx = new RuntimeException("Number format error");
    dbUtilityStatic.when(() -> DbUtility.getUnderlyingSQLException(any(Exception.class)))
        .thenReturn(underlyingEx);
    matchingUtilityStatic.when(
            () -> APRM_MatchingUtility.createMessageInProcessView(anyString(), eq("error")))
        .thenReturn(new JSONArray());

    // Act
    JSONObject result = callDoExecute(content);

    // Assert
    assertNotNull(result);
    assertTrue(result.has(RESPONSE_ACTIONS));
    assertTrue(result.has(RETRY_EXECUTION));
    assertEquals(true, result.getBoolean(RETRY_EXECUTION));
  }

  /**
   * Tests doExecute when exception occurs and error message building also fails.
   * Verifies that the outer catch handles errors gracefully.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithExceptionAndMessageBuildFailure() throws Exception {
    // Arrange
    String content = buildRequestContent("BPW", "null", "null", "INVALID", "0.00", "");

    dbUtilityStatic.when(() -> DbUtility.getUnderlyingSQLException(any(Exception.class)))
        .thenThrow(new RuntimeException("Cannot get underlying exception"));

    // Act
    JSONObject result = callDoExecute(content);

    // Assert
    assertNotNull(result);
    // Result should not have responseActions since the inner catch failed
    assertTrue(!result.has(RESPONSE_ACTIONS));
  }

  /**
   * Tests doExecute when no optional params (fin_payment_id, c_glitem_id, description) are present.
   * Verifies that defaults are used.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithMissingOptionalParams() throws Exception {
    // Arrange
    JSONObject params = new JSONObject();
    params.put("bankStatementLineId", TEST_BSL_ID);
    params.put("trxtype", "BF");
    params.put("trxdate", "2024-01-15");
    params.put("depositamt", VAL_10_00);
    params.put("withdrawalamt", "0.00");
    // Intentionally not adding: fin_payment_id, c_glitem_id, description
    params.put("ad_org_id", TEST_ORG_ID);
    params.put("c_bpartner_id", TEST_BP_ID);
    params.put("m_product_id", TEST_PRODUCT_ID);
    params.put("c_project_id", TEST_PROJECT_ID);
    params.put("c_activity_id", TEST_ACTIVITY_ID);
    params.put("c_salesregion_id", TEST_SALESREGION_ID);
    params.put("c_campaign_id", TEST_CAMPAIGN_ID);
    params.put("user1_id", TEST_USER1_ID);
    params.put("user2_id", TEST_USER2_ID);
    params.put("c_costcenter_id", TEST_COSTCENTER_ID);

    JSONObject request = new JSONObject();
    request.put("_params", params);
    request.put("inpTabId", TEST_TAB_ID);
    request.put("Fin_Financial_Account_ID", TEST_FINANCIAL_ACCOUNT_ID);

    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD(APRM_BANK_FEE))
        .thenReturn(BANK_FEE);

    // Act
    JSONObject result = callDoExecute(request.toString());

    // Assert
    assertNotNull(result);
    matchingUtilityStatic.verify(() -> APRM_MatchingUtility.createAndMatchFinancialTransaction(
        anyString(), anyString(), any(Date.class), anyString(),
        any(), any(), eq(null), eq(BANK_FEE), eq(null), eq(true),
        any(BigDecimal.class), any(BigDecimal.class), eq(null), eq(null), eq(null),
        any(), any(), any(), any(), any(), any(), any(), any(), any(),
        any(), any(), any(), anyBoolean()));
  }

  /**
   * Tests doExecute with a deposit-only bank fee (no withdrawal).
   * Verifies isReceipt is true when deposit > 0 and withdrawal = 0.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithDepositOnlyBankFee() throws Exception {
    // Arrange
    String content = buildRequestContent("BF", "null", "null", VAL_100_00, "0.00", "Deposit only");

    // Act
    JSONObject result = callDoExecute(content);

    // Assert
    assertNotNull(result);
    matchingUtilityStatic.verify(() -> APRM_MatchingUtility.createAndMatchFinancialTransaction(
        anyString(), anyString(), any(Date.class), anyString(),
        any(), any(), eq(null), eq("Deposit only"), eq(null), eq(true),
        any(BigDecimal.class), any(BigDecimal.class), eq(null), eq(null), eq(null),
        any(), any(), any(), any(), any(), any(), any(), any(), any(),
        any(), any(), any(), anyBoolean()));
  }

  /**
   * Tests the createAndMatchTransaction private method directly via reflection
   * with bank fee transaction type, verifying OBContext admin mode is set and restored.
    * @throws Exception if an error occurs
   */
  @Test
  public void testCreateAndMatchTransactionSetsAdminMode() throws Exception {
    // Arrange
    String content = buildRequestContent("BF", "null", "null", VAL_10_00, "0.00", "Test");

    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD(APRM_BANK_FEE))
        .thenReturn(BANK_FEE);

    // Act
    callDoExecute(content);

    // Assert
    obContextStatic.verify(() -> OBContext.setAdminMode(true));
    obContextStatic.verify(() -> OBContext.restorePreviousMode());
  }

  /**
   * Tests that doExecute returns an empty JSONObject on success (no error).
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteReturnsEmptyResultOnSuccess() throws Exception {
    // Arrange
    String content = buildRequestContent("BF", "null", "null", VAL_10_00, "0.00", "Success test");

    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD(APRM_BANK_FEE))
        .thenReturn(BANK_FEE);

    // Act
    JSONObject result = callDoExecute(content);

    // Assert
    assertNotNull(result);
    assertTrue(!result.has(RESPONSE_ACTIONS));
    assertTrue(!result.has(RETRY_EXECUTION));
  }

  /**
   * Tests doExecute with GL item withdrawal greater than deposit.
   * Verifies isReceipt is false.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithGLItemWithdrawalGreaterThanDeposit() throws Exception {
    // Arrange
    String content = buildRequestContent("BPW", "null", TEST_GLITEM_ID, VAL_10_00, VAL_50_00,
        "Withdrawal GL");

    lenient().when(mockOBDal.get(GLItem.class, TEST_GLITEM_ID)).thenReturn(mockGLItem);

    // Act
    JSONObject result = callDoExecute(content);

    // Assert
    assertNotNull(result);
    matchingUtilityStatic.verify(() -> APRM_MatchingUtility.createAndMatchFinancialTransaction(
        anyString(), anyString(), any(Date.class), anyString(),
        any(), any(), eq(null), eq("Withdrawal GL"), eq(mockGLItem), eq(false),
        any(BigDecimal.class), any(BigDecimal.class), eq(null), eq(null), eq(null),
        any(), any(), any(), any(), any(), any(), any(), any(), any(),
        any(), any(), any(), anyBoolean()));
  }

  // --- Helper Methods ---

  /**
   * Invokes the protected doExecute method via reflection.
   */
  private JSONObject callDoExecute(String content) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method doExecuteMethod = AddTransactionActionHandler.class.getDeclaredMethod(
        "doExecute", Map.class, String.class);
    doExecuteMethod.setAccessible(true);
    Map<String, Object> parameters = new HashMap<>();
    return (JSONObject) doExecuteMethod.invoke(handler, parameters, content);
  }

  /**
   * Sets up all accounting dimension mocks for OBDal.get calls.
   */
  private void setupDimensionMocks() {
    lenient().when(mockOBDal.get(FIN_FinancialAccount.class, TEST_FINANCIAL_ACCOUNT_ID))
        .thenReturn(mockAccount);
    lenient().when(mockOBDal.get(FIN_BankStatementLine.class, TEST_BSL_ID))
        .thenReturn(mockBankStatementLine);
    lenient().when(mockOBDal.get(Organization.class, TEST_ORG_ID)).thenReturn(mockOrganization);
    lenient().when(mockOBDal.get(BusinessPartner.class, TEST_BP_ID))
        .thenReturn(mockBusinessPartner);
    lenient().when(mockOBDal.get(Product.class, TEST_PRODUCT_ID)).thenReturn(mockProduct);
    lenient().when(mockOBDal.get(Project.class, TEST_PROJECT_ID)).thenReturn(mockProject);
    lenient().when(mockOBDal.get(ABCActivity.class, TEST_ACTIVITY_ID)).thenReturn(mockActivity);
    lenient().when(mockOBDal.get(SalesRegion.class, TEST_SALESREGION_ID))
        .thenReturn(mockSalesRegion);
    lenient().when(mockOBDal.get(Campaign.class, TEST_CAMPAIGN_ID)).thenReturn(mockCampaign);
    lenient().when(mockOBDal.get(UserDimension1.class, TEST_USER1_ID)).thenReturn(mockUser1);
    lenient().when(mockOBDal.get(UserDimension2.class, TEST_USER2_ID)).thenReturn(mockUser2);
    lenient().when(mockOBDal.get(Costcenter.class, TEST_COSTCENTER_ID)).thenReturn(mockCostcenter);
  }

  /**
   * Builds a JSON request content string for the action handler.
   */
  private String buildRequestContent(String trxType, String paymentId, String glItemId,
      String depositAmt, String withdrawalAmt, String description) {
    try {
      JSONObject params = new JSONObject();
      params.put("bankStatementLineId", TEST_BSL_ID);
      params.put("trxtype", trxType);
      params.put("trxdate", "2024-01-15");
      params.put("depositamt", depositAmt);
      params.put("withdrawalamt", withdrawalAmt);
      params.put("fin_payment_id", paymentId);
      params.put("c_glitem_id", glItemId);
      params.put("description", description);
      params.put("ad_org_id", TEST_ORG_ID);
      params.put("c_bpartner_id", TEST_BP_ID);
      params.put("m_product_id", TEST_PRODUCT_ID);
      params.put("c_project_id", TEST_PROJECT_ID);
      params.put("c_activity_id", TEST_ACTIVITY_ID);
      params.put("c_salesregion_id", TEST_SALESREGION_ID);
      params.put("c_campaign_id", TEST_CAMPAIGN_ID);
      params.put("user1_id", TEST_USER1_ID);
      params.put("user2_id", TEST_USER2_ID);
      params.put("c_costcenter_id", TEST_COSTCENTER_ID);

      JSONObject request = new JSONObject();
      request.put("_params", params);
      request.put("inpTabId", TEST_TAB_ID);
      request.put("Fin_Financial_Account_ID", TEST_FINANCIAL_ACCOUNT_ID);
      return request.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
