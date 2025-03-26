package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
import org.openbravo.advpaymentmngt.utility.APRM_MatchingUtility;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.json.JsonUtils;

/**
 * Unit tests for the UnMatchSelectedTransactionsActionHandler class.
 */
@RunWith(MockitoJUnitRunner.class)
public class UnMatchSelectedTransactionsActionHandlerTest {

  /**
   * Rule to define and verify expected exceptions in test cases.
   * Ensures that tests can specify the type and message of expected exceptions.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @InjectMocks
  private UnMatchSelectedTransactionsActionHandler actionHandler;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private FIN_BankStatementLine mockBankStatementLine;

  @Mock
  private FIN_FinaccTransaction mockTransaction;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<APRM_MatchingUtility> mockedMatchingUtility;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<OBDateUtils> mockedOBDateUtils;
  private MockedStatic<JsonUtils> mockedJsonUtils;

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

    // Initialize static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedMatchingUtility = mockStatic(APRM_MatchingUtility.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedOBDateUtils = mockStatic(OBDateUtils.class);
    mockedJsonUtils = mockStatic(JsonUtils.class);

    // Setup common mock behaviors
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    // Mock JsonUtils.createJSTimeFormat()
    SimpleDateFormat mockDateFormat = mock(SimpleDateFormat.class);
    mockedJsonUtils.when(JsonUtils::createJSTimeFormat).thenReturn(mockDateFormat);

    // Configuración del mock para parse
    try {
      when(mockDateFormat.parse(any(String.class))).thenReturn(new Date());
    } catch (Exception e) {
      System.err.println("Error configuring parse mock: " + e.getMessage());
    }

    // Mock OBContext methods
    mockedOBContext.when(() -> OBContext.setAdminMode(true)).thenAnswer(invocation -> null);
    mockedOBContext.when(OBContext::restorePreviousMode).thenAnswer(invocation -> null);

    // Mock OBMessageUtils methods
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(TestConstants.MESSAGE_SUCCESS)).thenReturn(
        TestConstants.MESSAGE_SUCCESS);
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("Warning")).thenReturn("Warning");
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(TestConstants.USER_ERROR)).thenReturn(
        TestConstants.USER_ERROR);
    mockedOBMessageUtils.when(
        () -> OBMessageUtils.getI18NMessage("APRM_UnmatchedRecords", new String[]{ "1" })).thenReturn(
        "1 record(s) successfully unmatched");
    mockedOBMessageUtils.when(
        () -> OBMessageUtils.getI18NMessage("APRM_ErrorOnUnmatchingRecords", new String[]{ "1" })).thenReturn(
        "Error unmatching 1 record(s):");
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
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedMatchingUtility != null) {
      mockedMatchingUtility.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
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
   * Tests the execute method for successful unmatching of transactions.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteSuccessfulUnmatch() throws Exception {
    // GIVEN
    String bslId = TestConstants.BANK_STATEMENT_LINE_ID;
    String referenceNo = TestConstants.REFERENCE;

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    Date specificDate = format.parse("2023-01-01T00:00:00");

    // Mock del SimpleDateFormat
    SimpleDateFormat mockDateFormat = JsonUtils.createJSTimeFormat();
    when(mockDateFormat.parse(any(String.class))).thenReturn(specificDate);

    // Mock OBDateUtils
    mockedOBDateUtils.when(() -> OBDateUtils.convertDateToUTC(any())).thenReturn(specificDate);

    // Mock bank statement
    when(mockOBDal.get(FIN_BankStatementLine.class, bslId)).thenReturn(mockBankStatementLine);
    when(mockBankStatementLine.getUpdated()).thenReturn(specificDate);
    when(mockBankStatementLine.getFinancialAccountTransaction()).thenReturn(mockTransaction);

    // Mock APRM_MatchingUtility.unmatch
    mockedMatchingUtility.when(() -> APRM_MatchingUtility.unmatch(mockBankStatementLine)).thenAnswer(
        invocation -> null);

    Map<String, Object> parameters = new HashMap<>();
    String jsonData = "{\"bankStatementLineIds\":[{\"id\":\"" + bslId + "\",\"referenceNo\":\"" + referenceNo + "\",\"cleared\":true,\"bslUpdated\":\"2023-01-01T00:00:00\"}]}";

    // WHEN
    JSONObject result = actionHandler.execute(parameters, jsonData);

    // THEN
    verify(mockOBDal, times(1)).get(FIN_BankStatementLine.class, bslId);
    mockedMatchingUtility.verify(() -> APRM_MatchingUtility.unmatch(mockBankStatementLine), times(1));

    JSONObject message = result.getJSONObject(TestConstants.RESPONSE_MESSAGE);
    assertEquals(TestConstants.RESULT_SUCCESS, message.getString(TestConstants.SEVERITY));
    assertEquals(TestConstants.MESSAGE_SUCCESS, message.getString(TestConstants.TITLE));
    assertTrue(message.getString("text").contains("successfully unmatched"));
  }

  /**
   * Tests the execute method when a stale object exception occurs.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteStaleObjectException() throws Exception {
    // GIVEN
    String bslId = TestConstants.BANK_STATEMENT_LINE_ID;
    String referenceNo = TestConstants.REFERENCE;

    // Create dates with different timestamps
    Date serverDate = new Date(1672531201000L);

    // Mock OBDateUtils
    mockedOBDateUtils.when(() -> OBDateUtils.convertDateToUTC(any())).thenReturn(serverDate);

    // Mock bank statement line
    when(mockOBDal.get(FIN_BankStatementLine.class, bslId)).thenReturn(mockBankStatementLine);
    when(mockBankStatementLine.getUpdated()).thenReturn(serverDate);

    // Prepare test data
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = "{\"bankStatementLineIds\":[{\"id\":\"" + bslId + "\",\"referenceNo\":\"" + referenceNo + "\",\"cleared\":true,\"bslUpdated\":\"2023-01-01T00:00:00\"}]}";

    // WHEN
    JSONObject result = actionHandler.execute(parameters, jsonData);

    // THEN
    verify(mockOBDal, times(1)).get(FIN_BankStatementLine.class, bslId);

    JSONObject message = result.getJSONObject(TestConstants.RESPONSE_MESSAGE);
    assertEquals(TestConstants.ERROR, message.getString(TestConstants.SEVERITY));
    assertEquals(TestConstants.USER_ERROR, message.getString(TestConstants.TITLE));
    assertTrue(message.getString("text").contains("Error unmatching 1 record(s)"));
    assertTrue(message.getString("text").contains(referenceNo));
  }

  /**
   * Tests the execute method for handling underlying SQL exceptions.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteSQLExceptionHandling() throws Exception {
    // GIVEN
    String bslId = TestConstants.BANK_STATEMENT_LINE_ID;
    String referenceNo = TestConstants.REFERENCE;

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    Date specificDate = format.parse("2023-01-01T00:00:00");

    when(mockOBDal.get(FIN_BankStatementLine.class, bslId)).thenReturn(mockBankStatementLine);
    when(mockBankStatementLine.getUpdated()).thenReturn(specificDate);

    mockedMatchingUtility.when(() -> APRM_MatchingUtility.unmatch(mockBankStatementLine)).thenThrow(
        new RuntimeException("Error in unmatch operation"));

    // Prepare test data
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = "{\"bankStatementLineIds\":[{\"id\":\"" + bslId + "\",\"referenceNo\":\"" + referenceNo + "\",\"cleared\":true,\"bslUpdated\":\"2023-01-01T00:00:00\"}]}";

    // WHEN
    JSONObject result = actionHandler.execute(parameters, jsonData);

    // THEN
    JSONObject message = result.getJSONObject(TestConstants.RESPONSE_MESSAGE);
    assertEquals(TestConstants.ERROR, message.getString(TestConstants.SEVERITY));
    assertEquals(TestConstants.USER_ERROR, message.getString(TestConstants.TITLE));

    assertTrue(message.getString("text").contains(referenceNo));
    assertTrue(message.getString("text").contains("Error unmatching"));
  }

  /**
   * Tests the execute method for handling exceptions during error message construction.
   */
  @Test
  public void testExecuteErrorMessageConstructionException() {
    // GIVEN
    String bslId = TestConstants.BANK_STATEMENT_LINE_ID;
    String referenceNo = TestConstants.REFERENCE;

    MockedStatic<DbUtility> mockedDbUtility = mockStatic(DbUtility.class);
    try {
      mockedDbUtility.when(() -> DbUtility.getUnderlyingSQLException(any(Exception.class))).thenThrow(
          new NullPointerException("Error getting SQL exception"));

      when(mockOBDal.get(FIN_BankStatementLine.class, bslId)).thenThrow(new RuntimeException("Database error"));

      // Prepare test data
      Map<String, Object> parameters = new HashMap<>();
      String jsonData = "{\"bankStatementLineIds\":[{\"id\":\"" + bslId + "\",\"referenceNo\":\"" + referenceNo + "\",\"cleared\":true,\"bslUpdated\":\"2023-01-01T00:00:00\"}]}";

      // WHEN
      JSONObject result = actionHandler.execute(parameters, jsonData);

      // THEN
      assertNotNull(result);
    } finally {
      mockedDbUtility.close();
    }
  }

  /**
   * Tests the execute method for handling exceptions at the start of processing.
   */
  @Test
  public void testExecuteInitialException() {
    // GIVEN
    String invalidJsonData = "{\"bankStatementLineIds\":[{malformed json...";

    // Prepare test data
    Map<String, Object> parameters = new HashMap<>();

    // WHEN
    JSONObject result = actionHandler.execute(parameters, invalidJsonData);

    assertNotNull(result);
  }

  /**
   * Tests the execute method for handling nested exceptions during error processing.
   */
  @Test
  public void testExecuteNestedExceptionInErrorHandling() {
    // GIVEN
    SQLException sqlException = mock(SQLException.class);
    when(sqlException.getMessage()).thenReturn("SQL Error");

    MockedStatic<DbUtility> mockedDbUtility = mockStatic(DbUtility.class);

    try {
      mockedDbUtility.when(() -> DbUtility.getUnderlyingSQLException(any(Exception.class))).thenReturn(sqlException);

      mockedOBMessageUtils.when(() -> OBMessageUtils.translateError(any())).thenThrow(
          new NullPointerException("Error in message translation"));

      mockedJsonUtils.when(() -> JsonUtils.createJSTimeFormat()).thenThrow(new RuntimeException("Error in JsonUtils"));

      // Prepare test data
      Map<String, Object> parameters = new HashMap<>();
      String jsonData = "{\"bankStatementLineIds\":[{\"id\":\"TEST_ID\",\"referenceNo\":\"REF123\",\"cleared\":true,\"bslUpdated\":\"2023-01-01T00:00:00\"}]}";

      // WHEN
      JSONObject result = actionHandler.execute(parameters, jsonData);

      // THEN
      assertNotNull(result);
    } finally {
      mockedDbUtility.close();
    }
  }
}
