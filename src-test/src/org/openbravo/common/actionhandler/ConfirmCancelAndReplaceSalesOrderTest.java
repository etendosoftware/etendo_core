package org.openbravo.common.actionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.CancelAndReplaceUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Unit tests for the {@link ConfirmCancelAndReplaceSalesOrder} class.
 * Verifies the behavior of the `doExecute` method under various scenarios,
 * including successful execution, error handling, and invalid input.
 */
@ExtendWith(MockitoExtension.class)
public class ConfirmCancelAndReplaceSalesOrderTest {

  private static final String NEW_ORDER_ID = "TEST_ORDER_ID";
  private static final String TEST_ERROR_MSG = "Test error message";
  private static final String TRANSLATED_ERROR_MSG = "Translated error message";
  @InjectMocks
  private ConfirmCancelAndReplaceSalesOrder actionHandler;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<CancelAndReplaceUtils> mockedCancelAndReplaceUtils;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  @Mock
  private OBDal mockOBDal;

  /**
   * Sets up the test environment before each test.
   * Initializes mocked static methods and dependencies.
   */
  @BeforeEach
  public void setUp() {
    mockedOBDal = mockStatic(OBDal.class);
    mockedCancelAndReplaceUtils = mockStatic(CancelAndReplaceUtils.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(ActionHandlerTestConstants.SUCCESS)).thenReturn(ActionHandlerTestConstants.SUCCESS);
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(ActionHandlerTestConstants.ERROR_TITLE)).thenReturn(ActionHandlerTestConstants.ERROR_TITLE);
  }

  /**
   * Cleans up the test environment after each test.
   * Closes mocked static methods to release resources.
   */
  @AfterEach
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }

    if (mockedCancelAndReplaceUtils != null) {
      mockedCancelAndReplaceUtils.close();
    }

    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
  }

  /**
   * Tests the {@code doExecute} method with valid input.
   * Verifies that the method executes successfully and returns a success message.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoExecuteSuccess() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String content = createJsonContent(NEW_ORDER_ID);

    mockedCancelAndReplaceUtils.when(
        () -> CancelAndReplaceUtils.cancelAndReplaceOrder(eq(NEW_ORDER_ID), isNull(), eq(false))).thenAnswer(
        invocation -> null);

    JSONObject result = actionHandler.doExecute(parameters, content);

    assertNotNull(result);
    assertTrue(result.has(ActionHandlerTestConstants.MESSAGE));

    JSONObject message = result.getJSONObject(ActionHandlerTestConstants.MESSAGE);
    assertEquals("success", message.getString(ActionHandlerTestConstants.SEVERITY));
    assertEquals(ActionHandlerTestConstants.SUCCESS, message.getString(ActionHandlerTestConstants.TITLE));

    mockedCancelAndReplaceUtils.verify(
        () -> CancelAndReplaceUtils.cancelAndReplaceOrder(eq(NEW_ORDER_ID), isNull(), eq(false)), times(1));
  }

  /**
   * Tests the {@code doExecute} method when an error occurs in the cancel and replace process.
   * Verifies that the method returns an error message with translated text.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoExecuteCancelAndReplaceError() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String content = createJsonContent(NEW_ORDER_ID);

    OBException mockException = new OBException(TEST_ERROR_MSG);

    mockedCancelAndReplaceUtils.when(
        () -> CancelAndReplaceUtils.cancelAndReplaceOrder(eq(NEW_ORDER_ID), isNull(), eq(false))).thenThrow(
        mockException);

    OBError mockOBError = mock(OBError.class);
    when(mockOBError.getMessage()).thenReturn(TRANSLATED_ERROR_MSG);
    mockedOBMessageUtils.when(() -> OBMessageUtils.translateError(TEST_ERROR_MSG)).thenReturn(mockOBError);

    JSONObject result = actionHandler.doExecute(parameters, content);

    assertNotNull(result);
    assertTrue(result.has(ActionHandlerTestConstants.MESSAGE));

    JSONObject message = result.getJSONObject(ActionHandlerTestConstants.MESSAGE);
    assertEquals(ActionHandlerTestConstants.ERROR, message.getString(ActionHandlerTestConstants.SEVERITY));
    assertEquals(ActionHandlerTestConstants.ERROR_TITLE, message.getString(ActionHandlerTestConstants.TITLE));
    assertEquals(TRANSLATED_ERROR_MSG, message.getString("text"));

    verify(mockOBDal, times(1)).rollbackAndClose();
  }

  /**
   * Tests the {@code doExecute} method with invalid JSON input.
   * Verifies that the method handles the invalid input and returns an error message.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoExecuteInvalidJson() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String invalidContent = "invalid json";

    OBError mockOBError = mock(OBError.class);
    when(mockOBError.getMessage()).thenReturn(TRANSLATED_ERROR_MSG);
    mockedOBMessageUtils.when(() -> OBMessageUtils.translateError(anyString())).thenReturn(mockOBError);

    JSONObject result = actionHandler.doExecute(parameters, invalidContent);

    assertNotNull(result);
    assertTrue(result.has(ActionHandlerTestConstants.MESSAGE));

    JSONObject message = result.getJSONObject(ActionHandlerTestConstants.MESSAGE);
    assertEquals(ActionHandlerTestConstants.ERROR, message.getString(ActionHandlerTestConstants.SEVERITY));
    assertEquals(ActionHandlerTestConstants.ERROR_TITLE, message.getString(ActionHandlerTestConstants.TITLE));
    assertEquals(TRANSLATED_ERROR_MSG, message.getString("text"));

    verify(mockOBDal, times(1)).rollbackAndClose();
  }

  /**
   * Tests the {@code doExecute} method when an error occurs during error handling.
   * Verifies that the method still returns an error message.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoExecuteErrorInErrorHandling() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String content = createJsonContent(NEW_ORDER_ID);

    OBException primaryException = new OBException(TEST_ERROR_MSG);
    mockedCancelAndReplaceUtils.when(
        () -> CancelAndReplaceUtils.cancelAndReplaceOrder(eq(NEW_ORDER_ID), isNull(), eq(false))).thenThrow(
        primaryException);

    OBError mockOBError = mock(OBError.class);
    when(mockOBError.getMessage()).thenReturn(TRANSLATED_ERROR_MSG);
    mockedOBMessageUtils.when(() -> OBMessageUtils.translateError(TEST_ERROR_MSG)).thenReturn(mockOBError);

    JSONObject result = actionHandler.doExecute(parameters, content);

    assertNotNull(result);
    assertTrue(result.has(ActionHandlerTestConstants.MESSAGE));
    JSONObject message = result.getJSONObject(ActionHandlerTestConstants.MESSAGE);
    assertEquals(ActionHandlerTestConstants.ERROR, message.getString(ActionHandlerTestConstants.SEVERITY));
  }

  /**
   * Creates a JSON content string for testing.
   *
   * @param orderId
   *     the order ID to include in the JSON content
   * @return a JSON string containing the order ID
   */
  private String createJsonContent(String orderId) {
    return "{\"inpcOrderId\":\"" + orderId + "\"}";
  }
}
