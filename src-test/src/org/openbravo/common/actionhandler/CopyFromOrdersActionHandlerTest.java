package org.openbravo.common.actionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.common.actionhandler.copyfromorderprocess.CopyFromOrdersProcess;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.DbUtility;

/**
 * Unit tests for the {@link CopyFromOrdersActionHandler} class.
 * Verifies the behavior of the action handler when copying order lines.
 */
@ExtendWith(MockitoExtension.class)
public class CopyFromOrdersActionHandlerTest {

  private static final String TEST_ORDER_ID = "TEST_ORDER_ID";
  @InjectMocks
  private CopyFromOrdersActionHandler actionHandler;
  @Mock
  private CopyFromOrdersProcess copyFromOrdersProcess;
  @Mock
  private Order mockOrder;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<WeldUtils> mockedWeldUtils;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<DbUtility> mockedDbUtility;

  /**
   * Sets up the test environment before each test.
   * Mocks static methods and initializes dependencies.
   */
  @BeforeEach
  public void setUp() {
    mockedOBDal = mockStatic(OBDal.class);
    mockedWeldUtils = mockStatic(WeldUtils.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedDbUtility = mockStatic(DbUtility.class);

    OBDal mockOBDalInstance = mock(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDalInstance);
    when(mockOBDalInstance.get(Order.class, TEST_ORDER_ID)).thenReturn(mockOrder);

    mockedWeldUtils.when(() -> WeldUtils.getInstanceFromStaticBeanManager(CopyFromOrdersProcess.class)).thenReturn(
        copyFromOrdersProcess);

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("success")).thenReturn("Success");
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("RecordsCopied")).thenReturn("Records Copied: ");
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("OrderNotDefined")).thenReturn("Order not defined");

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
    if (mockedWeldUtils != null) {
      mockedWeldUtils.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
    if (mockedDbUtility != null) {
      mockedDbUtility.close();
    }
  }

  /**
   * Tests the {@code doExecute} method for a successful copy operation.
   * Verifies that the response contains a success message with the number of records copied.
   *
   * @throws Exception
   *     if an error occurs during the test execution
   */
  @Test
  public void testExecuteSuccessfulCopy() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    String content = createJsonContent("DONE", TEST_ORDER_ID, new String[]{ "ORDER1", "ORDER2" });

    when(copyFromOrdersProcess.copyOrderLines(any(Order.class), any(JSONArray.class))).thenReturn(5);

    // Act
    JSONObject result = actionHandler.doExecute(parameters, content);

    // Assert
    assertNotNull(result);
    assertTrue(result.has(ActionHandlerTestConstants.MESSAGE));
    JSONObject message = result.getJSONObject(ActionHandlerTestConstants.MESSAGE);
    assertEquals("success", message.getString("severity"));
    assertTrue(message.getString("text").contains("Records Copied: 5"));
  }

  /**
   * Tests the {@code doExecute} method when no orders are selected.
   * Verifies that the response does not contain a message.
   *
   * @throws Exception
   *     if an error occurs during the test execution
   */
  @Test
  public void testExecuteWithNoSelectedOrders() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String content = createJsonContent("DONE", TEST_ORDER_ID, new String[]{ });

    JSONObject result = actionHandler.doExecute(parameters, content);

    assertNotNull(result);
    assertFalse(result.has(ActionHandlerTestConstants.MESSAGE));
  }

  /**
   * Tests the {@code doExecute} method with a different action.
   * Verifies that the response does not contain a message.
   *
   * @throws Exception
   *     if an error occurs during the test execution
   */
  @Test
  public void testExecuteWithDifferentAction() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String content = createJsonContent("CANCEL", TEST_ORDER_ID, new String[]{ "ORDER1" });

    JSONObject result = actionHandler.doExecute(parameters, content);

    assertNotNull(result);
    assertFalse(result.has(ActionHandlerTestConstants.MESSAGE));
  }

  /**
   * Creates a JSON content string for testing purposes.
   * Simulates the input content for the {@code doExecute} method.
   *
   * @param action
   *     the action to be performed (e.g., "DONE", "CANCEL")
   * @param orderId
   *     the ID of the order
   * @param selectedOrderIds
   *     an array of selected order IDs
   * @return a JSON string representing the input content
   * @throws JSONException
   *     if an error occurs while creating the JSON content
   */
  private String createJsonContent(String action, String orderId, String[] selectedOrderIds) throws JSONException {
    JSONObject json = new JSONObject();
    json.put("C_Order_ID", orderId);
    json.put("_buttonValue", action);

    JSONObject params = new JSONObject();
    JSONObject grid = new JSONObject();
    JSONArray selection = new JSONArray();

    for (String selectedOrderId : selectedOrderIds) {
      JSONObject selectedOrder = new JSONObject();
      selectedOrder.put("id", selectedOrderId);
      selection.put(selectedOrder);
    }

    grid.put("_selection", selection);
    params.put("grid", grid);
    json.put("_params", params);

    return json.toString();
  }
}
