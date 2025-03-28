package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
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
import org.openbravo.advpaymentmngt.utility.APRM_MatchingUtility;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.gl.GLItem;

/**
 * Test class for the GLItemTransactionActionHandler functionality.
 */
public class GLItemTransactionActionHandlerTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private GLItemTransactionActionHandler actionHandler;
  private AutoCloseable mocks;

  // Static mocks
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<APRM_MatchingUtility> mockedMatchingUtility;
  private MockedStatic<FIN_Utility> mockedFinUtility;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;

  // Mocks
  @Mock
  private OBDal obDal;

  @Mock
  private GLItem glItem;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    actionHandler = new GLItemTransactionActionHandler();

    // Setup static mocks - Order is important to avoid conflicts
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBDal = mockStatic(OBDal.class);
    mockedMatchingUtility = mockStatic(APRM_MatchingUtility.class);
    mockedFinUtility = mockStatic(FIN_Utility.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);

    // Configure static mocks
    mockedOBContext.when(() -> OBContext.setAdminMode(true)).thenAnswer(invocation -> null);
    mockedOBContext.when(OBContext::restorePreviousMode).thenAnswer(invocation -> null);

    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

    // Configure OBMessageUtils
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("APRM_GLItem")).thenReturn(TestConstants.GL_ITEM);
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
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
      mockedOBMessageUtils = null;
    }
    if (mockedFinUtility != null) {
      mockedFinUtility.close();
      mockedFinUtility = null;
    }
    if (mockedMatchingUtility != null) {
      mockedMatchingUtility.close();
      mockedMatchingUtility = null;
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
   * Test scenario where GLItem ID is null, and description should be modified
   * by removing any occurrence of the GLItem prefix.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteNullGLItem() throws Exception {
    // GIVEN
    String originalDescription = "GL Item: Test Description\nSome other text GL Item: other text";
    JSONObject jsonData = new JSONObject();
    jsonData.put(TestConstants.STR_DESCRIPTION, originalDescription);

    String data = jsonData.toString();
    Map<String, Object> parameters = new HashMap<>();

    // Mock utility methods
    mockedFinUtility.when(
        () -> FIN_Utility.getFinAccTransactionDescription(eq(originalDescription), eq("\nGL Item"), eq(""))).thenReturn(
        "Some other text GL Item: other text");
    mockedFinUtility.when(() -> FIN_Utility.getFinAccTransactionDescription(eq("Some other text GL Item: other text"),
        eq(TestConstants.GL_ITEM), eq(""))).thenReturn("Some other text");

    // WHEN
    JSONObject result = actionHandler.execute(parameters, data);

    // THEN
    assertNotNull(result);
    assertEquals("Some other text", result.getString(TestConstants.DESCRIPTION));

    // Verify methods were called
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
    mockedFinUtility.verify(
        () -> FIN_Utility.getFinAccTransactionDescription(eq(originalDescription), eq("\nGL Item"), eq("")), times(1));
    mockedFinUtility.verify(
        () -> FIN_Utility.getFinAccTransactionDescription(anyString(), eq(TestConstants.GL_ITEM), eq("")), times(1));
  }

  /**
   * Test scenario where GLItem ID is present but blank (empty string).
   * According to the implementation, when strGLItemId is an empty string (not null),
   * the description should remain unchanged since it doesn't enter the null block
   * and doesn't qualify for StringUtils.isNotBlank(strGLItemId).
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteEmptyGLItemId() throws Exception {
    // GIVEN
    String originalDescription = "GL Item: Test Description\nSome other text GL Item: other text";
    JSONObject jsonData = new JSONObject();
    jsonData.put(TestConstants.STR_DESCRIPTION, originalDescription);
    jsonData.put(TestConstants.STR_GL_ITEM_ID, ""); // Empty string, not null

    String data = jsonData.toString();
    Map<String, Object> parameters = new HashMap<>();

    // WHEN
    JSONObject result = actionHandler.execute(parameters, data);

    // THEN
    assertNotNull(result);
    // The description should remain unchanged since strGLItemId is empty string, not null
    assertEquals(originalDescription, result.getString(TestConstants.DESCRIPTION));

    // Verify methods were called
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));

    // No calls to FIN_Utility.getFinAccTransactionDescription should occur for empty strGLItemId
    mockedFinUtility.verify(() -> FIN_Utility.getFinAccTransactionDescription(anyString(), anyString(), anyString()),
        times(0));
  }

  /**
   * Test scenario where a valid GLItem ID is provided, and the description
   * should be updated with the GLItem name.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteValidGLItem() throws Exception {
    // GIVEN
    String originalDescription = TestConstants.ORIGINAL_DESCRIPTION;
    String glItemId = "TEST_GLITEM_ID";
    String glItemName = "Test GL Item";

    JSONObject jsonData = new JSONObject();
    jsonData.put(TestConstants.STR_DESCRIPTION, originalDescription);
    jsonData.put(TestConstants.STR_GL_ITEM_ID, glItemId);

    String data = jsonData.toString();
    Map<String, Object> parameters = new HashMap<>();

    // Mock GLItem
    when(obDal.get(GLItem.class, glItemId)).thenReturn(glItem);
    when(glItem.getName()).thenReturn(glItemName);

    // Mock utility methods
    String expectedGLItemDesc = "GL Item: " + glItemName;
    mockedFinUtility.when(
        () -> FIN_Utility.getFinAccTransactionDescription(eq(originalDescription), eq(TestConstants.GL_ITEM),
            eq(expectedGLItemDesc))).thenReturn("Original Description GL Item: Test GL Item");

    // WHEN
    JSONObject result = actionHandler.execute(parameters, data);

    // THEN
    assertNotNull(result);
    assertEquals("Original Description GL Item: Test GL Item", result.getString(TestConstants.DESCRIPTION));

    // Verify methods were called
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
    verify(obDal, times(1)).get(GLItem.class, glItemId);
    mockedFinUtility.verify(
        () -> FIN_Utility.getFinAccTransactionDescription(eq(originalDescription), eq(TestConstants.GL_ITEM),
            eq(expectedGLItemDesc)), times(1));
  }

  /**
   * Test scenario where a valid GLItem ID is provided but the GLItem is not found.
   * The description should remain unchanged.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteNonExistentGLItem() throws Exception {
    // GIVEN
    String originalDescription = TestConstants.ORIGINAL_DESCRIPTION;
    String glItemId = "NONEXISTENT_GLITEM_ID";

    JSONObject jsonData = new JSONObject();
    jsonData.put(TestConstants.STR_DESCRIPTION, originalDescription);
    jsonData.put(TestConstants.STR_GL_ITEM_ID, glItemId);

    String data = jsonData.toString();
    Map<String, Object> parameters = new HashMap<>();

    // Mock non-existent GLItem
    when(obDal.get(GLItem.class, glItemId)).thenReturn(null);

    // WHEN
    JSONObject result = actionHandler.execute(parameters, data);

    // THEN
    assertNotNull(result);
    assertEquals(originalDescription, result.getString(TestConstants.DESCRIPTION));

    // Verify methods were called
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
    verify(obDal, times(1)).get(GLItem.class, glItemId);
  }

  /**
   * Test scenario where an exception occurs during processing.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteException() throws Exception {
    // GIVEN
    String originalDescription = TestConstants.ORIGINAL_DESCRIPTION;
    String glItemId = "TEST_GLITEM_ID";

    JSONObject jsonData = new JSONObject();
    jsonData.put(TestConstants.STR_DESCRIPTION, originalDescription);
    jsonData.put(TestConstants.STR_GL_ITEM_ID, glItemId);

    String data = jsonData.toString();
    Map<String, Object> parameters = new HashMap<>();

    // Mock exception
    RuntimeException testException = new RuntimeException(TestConstants.TEST_EXCEPTION);
    when(obDal.get(GLItem.class, glItemId)).thenThrow(testException);

    // Mock error handling
    JSONArray errorActions = new JSONArray();
    errorActions.put("Test error action");
    mockedMatchingUtility.when(
        () -> APRM_MatchingUtility.createMessageInProcessView(TestConstants.TEST_EXCEPTION, "error")).thenReturn(
        errorActions);

    // WHEN
    JSONObject result = actionHandler.execute(parameters, data);

    // THEN
    assertNotNull(result);
    assertEquals(errorActions, result.getJSONArray("responseActions"));
    assertTrue(result.getBoolean("retryExecution"));

    // Verify methods were called
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
    verify(obDal, times(1)).get(GLItem.class, glItemId);
    mockedMatchingUtility.verify(
        () -> APRM_MatchingUtility.createMessageInProcessView(TestConstants.TEST_EXCEPTION, "error"), times(1));
  }
}
