package org.openbravo.common.actionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.util.Collections;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.CancelAndReplaceUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.DbUtility;

/**
 * Unit tests for the {@link CancelAndReplaceSalesOrder} class.
 * Verifies the behavior of the `doExecute` method and its interaction with mocked dependencies.
 */
public class CancelAndReplaceSalesOrderTest {

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<CancelAndReplaceUtils> mockedCancelAndReplaceUtils;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<DbUtility> mockedDbUtility;

  @Mock
  private OBDal obDal;

  @Mock
  private Connection connection;

  @Mock
  private Order mockOldOrder;

  @Mock
  private Order mockNewOrder;

  @Mock
  private DocumentType mockDocumentType;

  @Mock
  private OBError mockOBError;

  private CancelAndReplaceSalesOrder cancelAndReplaceSalesOrder;

  /**
   * Sets up the test environment before each test.
   * Initialize mocks and the class under test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    // Mock static dependencies
    mockedOBDal = mockStatic(OBDal.class);
    mockedCancelAndReplaceUtils = mockStatic(CancelAndReplaceUtils.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedDbUtility = mockStatic(DbUtility.class);

    // Set up mocked static OBDal instance
    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
    when(obDal.getConnection()).thenReturn(connection);

    // Setup DocumentType mock for all tests
    when(mockOldOrder.getDocumentType()).thenReturn(mockDocumentType);

    // Initialize the class under test
    cancelAndReplaceSalesOrder = spy(new CancelAndReplaceSalesOrder());
  }

  /**
   * Cleans up the test environment after each test.
   * Closes all mocked static instances and resources.
   */
  @AfterEach
  public void tearDown() {
    mockedOBDal.close();
    mockedCancelAndReplaceUtils.close();
    mockedOBMessageUtils.close();
    mockedDbUtility.close();
  }

  /**
   * Tests the {@code doExecute} method with valid input.
   * Verifies that the method processes the input correctly and returns the expected result.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoExecuteSuccess() throws Exception {
    String oldOrderId = "oldOrderId";
    String tabId = "tabId";
    JSONObject request = new JSONObject();
    request.put("inpcOrderId", oldOrderId);
    request.put("inpTabId", tabId);

    when(obDal.get(Order.class, oldOrderId)).thenReturn(mockOldOrder);
    when(mockDocumentType.getSOSubType()).thenReturn("SO");

    when(CancelAndReplaceUtils.createReplacementOrder(mockOldOrder)).thenReturn(mockNewOrder);
    when(mockNewOrder.getId()).thenReturn("newOrderId");
    when(mockNewOrder.getDocumentNo()).thenReturn("Order123");

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("Success")).thenReturn("Success Message");
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("OrderCreatedInTemporalStatus")).thenReturn(
        "Order Created Temporarily");
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("OrderInTemporalStatus")).thenReturn(
        "Order Temporarily Open");

    JSONObject result = cancelAndReplaceSalesOrder.doExecute(Collections.emptyMap(), request.toString());

    assertNotNull(result);
    JSONArray actions = result.getJSONArray("responseActions");
    assertEquals(3, actions.length());

    JSONObject successMessage = actions.getJSONObject(0).getJSONObject("showMsgInProcessView");
    assertEquals("success", successMessage.getString("msgType"));
    assertEquals("Success Message", successMessage.getString("msgTitle"));
    assertTrue(successMessage.getString("msgText").contains("Order123"));
  }

  /**
   * Tests the {@code doExecute} method when an exception occurs during processing.
   * Verifies that the method handles the exception and returns an error message.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoExecuteExceptionHandling() throws Exception {
    String oldOrderId = "oldOrderId";
    JSONObject request = new JSONObject();
    request.put("inpcOrderId", oldOrderId);
    request.put("inpTabId", "tabId");

    when(obDal.get(Order.class, oldOrderId)).thenReturn(mockOldOrder);
    when(mockDocumentType.getSOSubType()).thenReturn("SO");

    OBException mockedException = new OBException("Mocked Exception");
    when(CancelAndReplaceUtils.createReplacementOrder(mockOldOrder)).thenThrow(mockedException);

    mockedDbUtility.when(() -> DbUtility.getUnderlyingSQLException(any(Exception.class))).thenReturn(mockedException);

    when(mockOBError.getMessage()).thenReturn("Translated Exception");
    mockedOBMessageUtils.when(() -> OBMessageUtils.translateError(anyString())).thenReturn(mockOBError);

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("Error")).thenReturn("Error");

    JSONObject result = cancelAndReplaceSalesOrder.doExecute(Collections.emptyMap(), request.toString());

    assertNotNull(result);
    JSONObject message = result.getJSONObject("message");
    assertEquals("error", message.getString("severity"));
    assertEquals("Translated Exception", message.getString("text"));
  }
}
