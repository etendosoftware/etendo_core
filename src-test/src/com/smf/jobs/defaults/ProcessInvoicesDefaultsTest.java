package com.smf.jobs.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.ProcessInvoiceUtil;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Unit tests for ProcessInvoicesDefaults class.
 * Tests the invoice processing functionality and document action handling.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessInvoicesDefaultsTest {

  public static final String TABLE_ID = "\"tableId\": \"318\"";
  public static final String ACTIONS = "actions";
  public static final String IS_PROCESSING = "\"isProcessing\": \"N\",";
  /**
   * Rule for expecting exceptions in test methods.
   * Allows verification of exception types thrown during test execution.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private ProcessInvoicesDefaults actionHandler;
  private Map<String, Object> parameters;
  private AutoCloseable closeable;
  @Mock
  private RequestContext mockRequestContext;
  @Mock
  private VariablesSecureApp mockVars;
  private MockedStatic<RequestContext> mockedRequestContext;
  private MockedStatic<ProcessInvoiceUtil> mockedProcessInvoiceUtil;

  /**
   * Sets up the test environment before test.
   * Initialize mocks and configures basic mock behavior.
   *
   * @throws Exception
   *     if an error occurs during initialization
   */
  @Before
  public void setUp() throws Exception {
    closeable = MockitoAnnotations.openMocks(this);
    actionHandler = new ProcessInvoicesDefaults();
    parameters = new HashMap<>();

    mockedRequestContext = mockStatic(RequestContext.class);
    mockedProcessInvoiceUtil = mockStatic(ProcessInvoiceUtil.class);

    mockedRequestContext.when(RequestContext::get).thenReturn(mockRequestContext);
    when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);
  }

  /**
   * Cleans up resources after each test.
   * Closes static mocks to prevent memory leaks.
   *
   * @throws Exception
   *     if an error occurs while closing resources
   */
  @After
  public void tearDown() throws Exception {
    if (mockedRequestContext != null) {
      mockedRequestContext.close();
    }
    if (mockedProcessInvoiceUtil != null) {
      mockedProcessInvoiceUtil.close();
    }
    closeable.close();
  }

  /**
   * Tests successful execution of the process with multiple document statuses.
   * p
   * This test verifies that when the process is executed with different document statuses
   * (e.g., "CO" and "DR"), the expected actions are returned in the result.
   * It mocks the behavior of {@link ProcessInvoiceUtil} to provide predefined actions
   * and asserts that the response contains the correct actions.
   *
   * @throws Exception
   *     if an unexpected error occurs during execution
   */
  @Test
  public void testExecuteSuccess() throws Exception {
    // Prepare test data
    String content = "{" + "\"documentStatuses\": [\"CO\", \"DR\"]," + IS_PROCESSING + TABLE_ID + "}";

    JSONArray coActionsArray = new JSONArray();
    coActionsArray.put(new JSONObject("{\"id\":\"CL\",\"name\":\"Close\"}"));
    coActionsArray.put(new JSONObject("{\"id\":\"RE\",\"name\":\"Reactivate\"}"));

    JSONArray drActionsArray = new JSONArray();
    drActionsArray.put(new JSONObject("{\"id\":\"CO\",\"name\":\"Complete\"}"));
    drActionsArray.put(new JSONObject("{\"id\":\"VO\",\"name\":\"Void\"}"));

    List<String> coActions = new ArrayList<>();
    coActions.add("{\"id\":\"CL\",\"name\":\"Close\"}");
    coActions.add("{\"id\":\"RE\",\"name\":\"Reactivate\"}");

    List<String> drActions = new ArrayList<>();
    drActions.add("{\"id\":\"CO\",\"name\":\"Complete\"}");
    drActions.add("{\"id\":\"VO\",\"name\":\"Void\"}");

    configureMockProcessInvoiceUtil(coActions, drActions);

    JSONObject result = actionHandler.execute(parameters, content);

    assertNotNull("Result should not be null", result);
    assertTrue("Result should contain 'actions' key", result.has(ACTIONS));

    JSONArray actions = result.getJSONArray(ACTIONS);
    assertEquals("Should have 4 actions in total", 4, actions.length());
    verifyActions(actions);
  }

  /**
   * Tests execution with empty document statuses.
   * p
   * This test verifies that when the process is executed with an empty list of document statuses,
   * the result contains an empty list of actions. It ensures that the system correctly handles
   * cases where no document statuses are provided.
   *
   * @throws Exception
   *     if an unexpected error occurs during execution
   */
  @Test
  public void testExecuteEmptyDocumentStatuses() throws Exception {
    String content = "{" + "\"documentStatuses\": []," + IS_PROCESSING + TABLE_ID + "}";

    JSONObject result = actionHandler.execute(parameters, content);

    assertNotNull("Result should not be null", result);
    assertTrue("Result should contain 'actions' key", result.has(ACTIONS));

    JSONArray actions = result.getJSONArray(ACTIONS);
    assertEquals("Should have no actions", 0, actions.length());
  }

  /**
   * Tests handling of invalid JSON content.
   * Verifies that an OBException is thrown when an invalid JSON
   * format is provided as input to the execute method.
   */
  @Test
  public void testExecuteException() {
    expectedException.expect(OBException.class);
    actionHandler.execute(parameters, "invalidJson");
  }

  /**
   * Tests handling of ProcessInvoiceUtil exceptions.
   * Verifies that exceptions are properly wrapped and thrown.
   */
  @Test
  public void testExecuteProcessInvoiceUtilThrowsException() {
    String content = "{" + "\"documentStatuses\": [\"CO\"]," + IS_PROCESSING + TABLE_ID + "}";

    mockedProcessInvoiceUtil.when(
        () -> ProcessInvoiceUtil.getDocumentActionList(anyString(), anyString(), anyString(), anyString(),
            any(VariablesSecureApp.class), any(DalConnectionProvider.class))).thenThrow(
        new RuntimeException("Test error"));

    expectedException.expect(OBException.class);
    actionHandler.execute(parameters, content);
  }

  /**
   * Configures mock behavior for ProcessInvoiceUtil.
   */
  private void configureMockProcessInvoiceUtil(List<String> coActions, List<String> drActions) {
    mockedProcessInvoiceUtil.when(
        () -> ProcessInvoiceUtil.getDocumentActionList(eq("CO"), anyString(), eq("N"), eq("318"),
            any(VariablesSecureApp.class), any(DalConnectionProvider.class))).thenReturn(coActions);

    mockedProcessInvoiceUtil.when(
        () -> ProcessInvoiceUtil.getDocumentActionList(eq("DR"), anyString(), eq("N"), eq("318"),
            any(VariablesSecureApp.class), any(DalConnectionProvider.class))).thenReturn(drActions);
  }

  /**
   * Verifies that all expected actions are present in the result.
   */
  private void verifyActions(JSONArray actions) throws Exception {
    boolean foundClose = false;
    boolean foundReactivate = false;
    boolean foundComplete = false;
    boolean foundVoid = false;

    for (int i = 0; i < actions.length(); i++) {
      String actionStr = actions.getString(i);
      if (actionStr.contains("\"id\":\"CL\"")) foundClose = true;
      if (actionStr.contains("\"id\":\"RE\"")) foundReactivate = true;
      if (actionStr.contains("\"id\":\"CO\"")) foundComplete = true;
      if (actionStr.contains("\"id\":\"VO\"")) foundVoid = true;
    }

    assertTrue("Should contain Close action", foundClose);
    assertTrue("Should contain Reactivate action", foundReactivate);
    assertTrue("Should contain Complete action", foundComplete);
    assertTrue("Should contain Void action", foundVoid);
  }
}
