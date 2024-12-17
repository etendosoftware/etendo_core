package com.smf.jobs.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.ad_actionButton.ActionButtonUtility;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Unit test class for {@link ProcessOrdersDefaults}.
 * This class verifies the behavior of methods in the ProcessOrdersDefaults class.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessOrdersDefaultsTest {

  public static final String RESULT_SHOULD_NOT_BE_NULL = "Result should not be null";
  public static final String ACTIONS = "actions";


  @InjectMocks
  private ProcessOrdersDefaults processOrdersDefaults;

  @Mock
  private RequestContext mockRequestContext;

  @Mock
  private VariablesSecureApp mockVars;

  @Mock
  private DalConnectionProvider mockConnectionProvider;

  /**
   * Sets up the test environment before each test case.
   * Initializes common mocks and variables required across tests.
   */
  @Before
  public void setUp() {
    // Common setup if needed
  }

  /**
   * Tests the execution of the process with a single document status ("DR").
   * Mocks necessary components and verifies that the result contains the expected actions.
   *
   * @throws Exception If an error occurs during test execution.
   */
  @Test
  public void testExecuteWithSingleDocumentStatus() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String content = new JSONObject()
        .put("documentStatuses", new JSONArray().put("DR"))
        .put("isProcessing", "N")
        .put("tabId", "123")
        .toString();

    FieldProvider mockFieldProvider = mock(FieldProvider.class);
    when(mockFieldProvider.getField("ID")).thenReturn("CO");
    FieldProvider[] mockFields = new FieldProvider[]{mockFieldProvider};

    try (MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class);
         MockedStatic<ActionButtonUtility> actionButtonUtilityMock = mockStatic(ActionButtonUtility.class)) {

      requestContextMock.when(RequestContext::get).thenReturn(mockRequestContext);
      when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);

      actionButtonUtilityMock.when(() -> ActionButtonUtility.docAction(
          any(),
          any(),
          anyString(),
          eq(ProcessOrdersDefaults.ORDER_DOCUMENT_ACTION_REFERENCE_ID),
          eq("DR"),
          eq("N"),
          eq(ProcessOrdersDefaults.AD_TABLE_ID),
          eq("123")
      )).thenReturn(mockFields);

      JSONObject result = processOrdersDefaults.execute(parameters, content);

      assertNotNull(RESULT_SHOULD_NOT_BE_NULL, result);
      assertTrue("Result should contain actions", result.has(ACTIONS));
      JSONArray actions = result.getJSONArray(ACTIONS);
      assertEquals("Should have one action", 1, actions.length());
      assertEquals("Should have correct action", "CO", actions.getString(0));
    }
  }

  /**
   * Tests the {@link ProcessOrdersDefaults#execute(Map, String)} method when there are multiple document statuses.
   * Verifies the result structure and correctness of returned actions.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testExecuteWithMultipleDocumentStatuses() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String content = new JSONObject()
        .put("documentStatuses", new JSONArray().put("DR").put("CO"))
        .put("isProcessing", "N")
        .put("tabId", "123")
        .toString();

    FieldProvider mockFieldProvider1 = mock(FieldProvider.class);
    FieldProvider mockFieldProvider2 = mock(FieldProvider.class);
    when(mockFieldProvider1.getField("ID")).thenReturn("CO");
    when(mockFieldProvider2.getField("ID")).thenReturn("CL");

    FieldProvider[] mockFields1 = new FieldProvider[]{mockFieldProvider1};
    FieldProvider[] mockFields2 = new FieldProvider[]{mockFieldProvider2};

    try (MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class);
         MockedStatic<ActionButtonUtility> actionButtonUtilityMock = mockStatic(ActionButtonUtility.class)) {

      requestContextMock.when(RequestContext::get).thenReturn(mockRequestContext);
      when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);

      actionButtonUtilityMock.when(() -> ActionButtonUtility.docAction(
          any(),
          any(),
          anyString(),
          eq(ProcessOrdersDefaults.ORDER_DOCUMENT_ACTION_REFERENCE_ID),
          eq("DR"),
          eq("N"),
          eq(ProcessOrdersDefaults.AD_TABLE_ID),
          eq("123")
      )).thenReturn(mockFields1);

      actionButtonUtilityMock.when(() -> ActionButtonUtility.docAction(
          any(),
          any(),
          anyString(),
          eq(ProcessOrdersDefaults.ORDER_DOCUMENT_ACTION_REFERENCE_ID),
          eq("CO"),
          eq("N"),
          eq(ProcessOrdersDefaults.AD_TABLE_ID),
          eq("123")
      )).thenReturn(mockFields2);

      JSONObject result = processOrdersDefaults.execute(parameters, content);

      assertNotNull(RESULT_SHOULD_NOT_BE_NULL, result);
      assertTrue("Result should contain actions", result.has(ACTIONS));
      JSONArray actions = result.getJSONArray(ACTIONS);
      assertEquals("Should have two actions", 2, actions.length());
      assertEquals("First action should be CO", "CO", actions.getString(0));
      assertEquals("Second action should be CL", "CL", actions.getString(1));
    }
  }

  /**
   * Tests the {@link ProcessOrdersDefaults(String, String, String, VariablesSecureApp, DalConnectionProvider)}
   * method to ensure it correctly retrieves document actions based on input parameters.
   */
  @Test
  public void testGetDocumentActionList() {
    FieldProvider mockFieldProvider = mock(FieldProvider.class);
    when(mockFieldProvider.getField("ID")).thenReturn("CO");
    FieldProvider[] mockFields = new FieldProvider[]{mockFieldProvider};

    try (MockedStatic<ActionButtonUtility> actionButtonUtilityMock = mockStatic(ActionButtonUtility.class)) {
      actionButtonUtilityMock.when(() -> ActionButtonUtility.docAction(
          any(),
          any(),
          anyString(),
          anyString(),
          anyString(),
          anyString(),
          anyString(),
          anyString()
      )).thenReturn(mockFields);

      List<String> result = ProcessOrdersDefaults.getDocumentActionList(
          "DR",
          "N",
          "123",
          mockVars,
          mockConnectionProvider
      );

      assertNotNull(RESULT_SHOULD_NOT_BE_NULL, result);
      assertEquals("Should have one action", 1, result.size());
      assertEquals("Should have correct action", "CO", result.get(0));
    }
  }
}