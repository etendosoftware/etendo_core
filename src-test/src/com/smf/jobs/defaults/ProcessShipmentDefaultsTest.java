package com.smf.jobs.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_actionButton.ActionButtonUtility;

/**
 * Unit tests for the ProcessShipmentDefaults class.
 * Tests the functionality of the execute method and the getDocumentActionList method.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessShipmentDefaultsTest {

  private ProcessShipmentDefaults processShipmentDefaults;
  private MockedStatic<RequestContext> mockedRequestContext;
  private MockedStatic<ActionButtonUtility> mockedActionButtonUtility;

  @Mock
  private RequestContext mockRequestContext;

  @Mock
  private VariablesSecureApp mockVariablesSecureApp;

  /**
   * Sets up the test environment before each test.
   * Initialize mocks and configures common behavior.
   */
  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    processShipmentDefaults = new ProcessShipmentDefaults();

    mockedRequestContext = mockStatic(RequestContext.class);
    mockedActionButtonUtility = mockStatic(ActionButtonUtility.class);

    // Setup RequestContext.get() to return our mock
    mockedRequestContext.when(RequestContext::get).thenReturn(mockRequestContext);
    when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVariablesSecureApp);
  }

  /**
   * Cleans up resources after each test.
   * Closes the static mocks to prevent memory leaks.
   */
  @After
  public void tearDown() {
    mockedRequestContext.close();
    mockedActionButtonUtility.close();
  }

  /**
   * Tests the execute method with a single document status.
   * Verifies that the method returns the expected actions.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testExecuteSingleDocumentStatus() throws Exception {
    // Given
    String content = "{\"documentStatuses\":[\"CO\"], \"isProcessing\":\"N\", \"tableId\":\"319\"}";
    Map<String, Object> parameters = new HashMap<>();

    // Mock ActionButtonUtility.docAction
    FieldProvider mockField1 = createMockFieldProvider(Utility.DRAFT_STATUS);
    FieldProvider mockField2 = createMockFieldProvider(Utility.COMPLETE);

    FieldProvider[] mockFields = new FieldProvider[]{mockField1, mockField2};

    mockedActionButtonUtility.when(() -> ActionButtonUtility.docAction(
            any(ConnectionProvider.class),
            eq(mockVariablesSecureApp),
            eq(""),
            eq("135"),
            eq(Utility.COMPLETE),
            eq("N"),
            eq("319")))
        .thenReturn(mockFields);

    // When
    JSONObject result = processShipmentDefaults.execute(parameters, content);

    // Then
    JSONArray actions = result.getJSONArray(Utility.ACTIONS);
    assertEquals(2, actions.length());
    assertEquals(Utility.DRAFT_STATUS, actions.getString(0));
    assertEquals(Utility.COMPLETE, actions.getString(1));
  }

  /**
   * Tests the execute method with multiple document statuses.
   * Verifies that the method returns the expected actions.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testExecuteMultipleDocumentStatuses() throws Exception {
    // Given
    String content = "{\"documentStatuses\":[\"DR\", \"CO\"], \"isProcessing\":\"N\", \"tableId\":\"319\"}";
    Map<String, Object> parameters = new HashMap<>();

    // Mock ActionButtonUtility.docAction for "DR" status
    FieldProvider mockFieldDR = createMockFieldProvider(Utility.COMPLETE);
    FieldProvider[] mockFieldsDR = new FieldProvider[]{mockFieldDR};

    mockedActionButtonUtility.when(() -> ActionButtonUtility.docAction(
            any(ConnectionProvider.class),
            eq(mockVariablesSecureApp),
            eq(""),
            eq("135"),
            eq(Utility.DRAFT_STATUS),
            eq("N"),
            eq("319")))
        .thenReturn(mockFieldsDR);

    // Mock ActionButtonUtility.docAction for "CO" status
    FieldProvider mockFieldCO = createMockFieldProvider("CL");
    FieldProvider[] mockFieldsCO = new FieldProvider[]{mockFieldCO};

    mockedActionButtonUtility.when(() -> ActionButtonUtility.docAction(
            any(ConnectionProvider.class),
            eq(mockVariablesSecureApp),
            eq(""),
            eq("135"),
            eq(Utility.COMPLETE),
            eq("N"),
            eq("319")))
        .thenReturn(mockFieldsCO);

    // When
    JSONObject result = processShipmentDefaults.execute(parameters, content);

    // Then
    JSONArray actions = result.getJSONArray(Utility.ACTIONS);
    assertEquals(2, actions.length());
    assertEquals(Utility.COMPLETE, actions.getString(0));
    assertEquals("CL", actions.getString(1));
  }

  /**
   * Tests the execute method when no actions are returned.
   * Verifies that the method returns an empty actions array.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testExecuteNoActions() throws Exception {
    // Given
    String content = "{\"documentStatuses\":[\"XX\"], \"isProcessing\":\"Y\", \"tableId\":\"319\"}";
    Map<String, Object> parameters = new HashMap<>();

    // Mock ActionButtonUtility.docAction to return empty array
    FieldProvider[] emptyFields = new FieldProvider[]{};

    mockedActionButtonUtility.when(() -> ActionButtonUtility.docAction(
            any(ConnectionProvider.class),
            any(VariablesSecureApp.class),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString()))
        .thenReturn(emptyFields);

    // When
    JSONObject result = processShipmentDefaults.execute(parameters, content);

    // Then
    JSONArray actions = result.getJSONArray(Utility.ACTIONS);
    assertEquals(0, actions.length());
  }

  /**
   * Tests the execute method with invalid JSON content.
   * Verifies that the method throws an OBException.
   */
  @Test(expected = OBException.class)
  public void testExecuteInvalidJson() {
    // Given
    String invalidContent = "{invalid json";
    Map<String, Object> parameters = new HashMap<>();

    // When/Then
    processShipmentDefaults.execute(parameters, invalidContent);
    // Should throw OBException
  }

  /**
   * Tests the getDocumentActionList method.
   * Verifies that the method returns the expected list of actions.
   */
  @Test
  public void testGetDocumentActionList() {
    // Given
    ConnectionProvider mockConn = mock(ConnectionProvider.class);

    // Mock ActionButtonUtility.docAction
    FieldProvider mockField1 = createMockFieldProvider(Utility.DRAFT_STATUS);
    FieldProvider mockField2 = createMockFieldProvider(Utility.COMPLETE);
    FieldProvider[] mockFields = new FieldProvider[]{mockField1, mockField2};

    mockedActionButtonUtility.when(() -> ActionButtonUtility.docAction(
            eq(mockConn),
            eq(mockVariablesSecureApp),
            eq(""),
            eq("135"),
            eq(Utility.DRAFT_STATUS),
            eq("N"),
            eq("319")))
        .thenReturn(mockFields);

    // When
    List<String> actionList = ProcessShipmentDefaults.getDocumentActionList(Utility.DRAFT_STATUS, "N", mockVariablesSecureApp, mockConn);

    // Then
    assertEquals(2, actionList.size());
    assertTrue(actionList.containsAll(Arrays.asList(Utility.DRAFT_STATUS, Utility.COMPLETE)));
  }

  /**
   * Creates a mock FieldProvider with the specified ID.
   *
   * @param id the ID to be returned by the mock FieldProvider
   * @return the mock FieldProvider
   */
  private FieldProvider createMockFieldProvider(String id) {
    FieldProvider mockField = mock(FieldProvider.class);
    try {
      when(mockField.getField("ID")).thenReturn(id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return mockField;
  }
}
