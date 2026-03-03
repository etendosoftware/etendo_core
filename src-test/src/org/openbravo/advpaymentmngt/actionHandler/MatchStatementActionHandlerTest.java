package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.openbravo.test.base.mock.MockitoStaticMockUtils.mockStaticSafely;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.advpaymentmngt.utility.APRM_MatchingUtility;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;

/**
 * Unit tests for the MatchStatementActionHandler class.
 */
public class MatchStatementActionHandlerTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MatchStatementActionHandler actionHandler;
  private AutoCloseable mocks;

  // Static mocks
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<RequestContext> mockedRequestContext;
  private MockedStatic<APRM_MatchingUtility> mockedMatchingUtility;
  private MockedStatic<TransactionsDao> mockedTransactionsDao;
  private MockedStatic<DbUtility> mockedDbUtility;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<Utility> mockedUtility;

  // Mocks
  @Mock
  private OBDal obDal;
  @Mock
  private OBContext obContext;
  @Mock
  private RequestContext requestContext;
  @Mock
  private VariablesSecureApp vars;
  @Mock
  private FIN_FinancialAccount financialAccount;
  @Mock
  private FIN_Reconciliation reconciliation;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    actionHandler = new MatchStatementActionHandler();

    // Configure mocks
    when(requestContext.getVariablesSecureApp()).thenReturn(vars);
    when(vars.getLanguage()).thenReturn("en_US");

    // Setup static mocks - Order is important to avoid conflicts
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBDal = mockStaticSafely(OBDal.class);
    mockedRequestContext = mockStatic(RequestContext.class);
    mockedMatchingUtility = mockStatic(APRM_MatchingUtility.class);
    mockedTransactionsDao = mockStatic(TransactionsDao.class);
    mockedDbUtility = mockStatic(DbUtility.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedUtility = mockStatic(Utility.class);

    // Configure static mocks behavior
    mockedOBContext.when(() -> OBContext.setAdminMode(anyBoolean())).thenAnswer(invocation -> null);
    mockedOBContext.when(OBContext::restorePreviousMode).thenAnswer(invocation -> null);

    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
    mockedRequestContext.when(RequestContext::get).thenReturn(requestContext);
  }

  /**
   * Cleans up the test environment after each test.
   *
   * @throws Exception
   *     if an error occurs during teardown
   */
  @After
  public void tearDown() throws Exception {
    // Make sure to close all static mocks in reverse order of creation
    if (mockedUtility != null) {
      mockedUtility.close();
      mockedUtility = null;
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
      mockedOBMessageUtils = null;
    }
    if (mockedDbUtility != null) {
      mockedDbUtility.close();
      mockedDbUtility = null;
    }
    if (mockedTransactionsDao != null) {
      mockedTransactionsDao.close();
      mockedTransactionsDao = null;
    }
    if (mockedMatchingUtility != null) {
      mockedMatchingUtility.close();
      mockedMatchingUtility = null;
    }
    if (mockedRequestContext != null) {
      mockedRequestContext.close();
      mockedRequestContext = null;
    }
    if (mockedOBDal != null) {
      mockedOBDal.close();
      mockedOBDal = null;
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
      mockedOBContext = null;
    }
    if (mocks != null) {
      mocks.close();
      mocks = null;
    }
  }

  /**
   * Tests the execute method with the "OK" action.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteOKAction() throws Exception {
    // GIVEN
    String financialAccountId = TestConstants.TEST_FINANCIAL_ACCOUNT;

    JSONObject jsonContent = new JSONObject();
    jsonContent.put(TestConstants.FIN_FINANCIAL_ACCOUNT_ID, financialAccountId);
    jsonContent.put(TestConstants.BUTTON_VALUE, "OK");

    String content = jsonContent.toString();
    Map<String, Object> parameters = new HashMap<>();

    // WHEN
    JSONObject result = actionHandler.doExecute(parameters, content);

    // THEN
    assertNotNull(result);
    mockedTransactionsDao.verify(() -> TransactionsDao.getLastReconciliation(any(), any()), times(0));
    mockedMatchingUtility.verify(() -> APRM_MatchingUtility.updateReconciliation(any(), any(), eq(true)), times(0));

    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
  }

  /**
   * Tests the execute method with a successful match.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteSuccessfulMatch() throws Exception {
    // GIVEN
    String financialAccountId = TestConstants.TEST_FINANCIAL_ACCOUNT;
    String successMessage = "Success";

    // Mock JSON content
    JSONObject jsonContent = new JSONObject();
    jsonContent.put(TestConstants.FIN_FINANCIAL_ACCOUNT_ID, financialAccountId);
    jsonContent.put(TestConstants.BUTTON_VALUE, TestConstants.MATCH); // Any non-OK value

    String content = jsonContent.toString();
    Map<String, Object> parameters = new HashMap<>();

    // Mock getting the financial account
    when(obDal.get(FIN_FinancialAccount.class, financialAccountId)).thenReturn(financialAccount);

    // Mock getting the last reconciliation
    mockedTransactionsDao.when(() -> TransactionsDao.getLastReconciliation(financialAccount, "N")).thenReturn(
        reconciliation);

    // Mock updating reconciliation successfully
    mockedMatchingUtility.when(
        () -> APRM_MatchingUtility.updateReconciliation(reconciliation, financialAccount, true)).thenReturn(true);

    // Mock translation for success message
    mockedUtility.when(() -> Utility.parseTranslation(any(DalConnectionProvider.class), eq(vars), eq("en_US"),
        eq("@Success@"))).thenReturn(successMessage);

    // WHEN
    JSONObject result = actionHandler.doExecute(parameters, content);

    // THEN
    assertNotNull(result);
    assertTrue(result.has(TestConstants.RESPONSE_MESSAGE));

    JSONObject message = result.getJSONObject(TestConstants.RESPONSE_MESSAGE);
    assertEquals(TestConstants.RESULT_SUCCESS, message.getString("severity"));
    assertEquals("", message.getString("title"));
    assertEquals(successMessage, message.getString("text"));

    // Verify method calls
    verify(obDal, times(1)).get(FIN_FinancialAccount.class, financialAccountId);
    mockedTransactionsDao.verify(() -> TransactionsDao.getLastReconciliation(financialAccount, "N"), times(1));
    mockedMatchingUtility.verify(
        () -> APRM_MatchingUtility.updateReconciliation(reconciliation, financialAccount, true), times(1));

    // Modified verification to check setAdminMode with true parameter
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
  }

  /**
   * Tests the execute method when an exception occurs.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteException() throws Exception {
    // GIVEN
    String financialAccountId = TestConstants.TEST_FINANCIAL_ACCOUNT;
    String errorMessage = "Test exception message";
    Exception exception = new RuntimeException(TestConstants.TEST_EXCEPTION);

    // Mock JSON content
    JSONObject jsonContent = new JSONObject();
    jsonContent.put(TestConstants.FIN_FINANCIAL_ACCOUNT_ID, financialAccountId);
    jsonContent.put(TestConstants.BUTTON_VALUE, TestConstants.MATCH); // Any non-OK value

    String content = jsonContent.toString();
    Map<String, Object> parameters = new HashMap<>();

    // Mock getting the financial account to throw an exception
    when(obDal.get(FIN_FinancialAccount.class, financialAccountId)).thenThrow(exception);

    // Mock getting the underlying SQL exception
    mockedDbUtility.when(() -> DbUtility.getUnderlyingSQLException(exception)).thenReturn(exception);

    // Create a mock that provides the getMessage method
    OBError obError = mock(OBError.class);
    when(obError.getMessage()).thenReturn(errorMessage);
    mockedOBMessageUtils.when(() -> OBMessageUtils.translateError(any())).thenReturn(obError);

    // WHEN
    JSONObject result = actionHandler.doExecute(parameters, content);

    // THEN
    assertNotNull(result);
    assertTrue(result.has(TestConstants.RESPONSE_MESSAGE));

    JSONObject message = result.getJSONObject(TestConstants.RESPONSE_MESSAGE);
    assertEquals(TestConstants.ERROR, message.getString("severity"));
    assertEquals(errorMessage, message.getString("text"));

    // Verify rollback was called
    verify(obDal, times(1)).rollbackAndClose();

    // Modified verification to check setAdminMode with true parameter
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
  }
}
