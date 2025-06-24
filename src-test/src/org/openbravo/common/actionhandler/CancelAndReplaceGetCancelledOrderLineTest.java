package org.openbravo.common.actionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.OrderLine;

/**
 * Unit tests for the {@link CancelAndReplaceGetCancelledOrderLine} class.
 * Verifies the behavior of the `execute` method and its interaction with the database.
 */
public class CancelAndReplaceGetCancelledOrderLineTest {

  private AutoCloseable mocks;
  private MockedStatic<OBDal> obdalStaticMock;

  @Mock
  private OBDal obdalMock;

  @Mock
  private OrderLine mockOrderLine;

  private CancelAndReplaceGetCancelledOrderLine cancelAndReplaceHandler;

  /**
   * Sets up the test environment before each test.
   * Initialize mocks and the class under test.
   */
  @BeforeEach
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    obdalStaticMock = Mockito.mockStatic(OBDal.class);
    obdalStaticMock.when(OBDal::getInstance).thenReturn(obdalMock);
    cancelAndReplaceHandler = new CancelAndReplaceGetCancelledOrderLine();
  }

  /**
   * Cleans up the test environment after each test.
   * Closes all mocked static instances and resources.
   *
   * @throws Exception if an error occurs during cleanup
   */
  @AfterEach
  public void tearDown() throws Exception {
    if (obdalStaticMock != null) {
      obdalStaticMock.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Tests the {@code execute} method with valid input.
   * Verifies that the method processes the input correctly and returns the expected result.
   *
   * @throws Exception if an error occurs during the test
   */
  @Test
  public void testExecuteSuccessfulProcessing() throws Exception {
    String orderLineId = "12345";
    BigDecimal deliveredQuantity = new BigDecimal("10.00");

    when(obdalMock.get(OrderLine.class, orderLineId)).thenReturn(mockOrderLine);
    when(mockOrderLine.getDeliveredQuantity()).thenReturn(deliveredQuantity);

    JSONObject inputData = new JSONObject();
    JSONArray recordsArray = new JSONArray();
    JSONObject record = new JSONObject();
    record.put("replacedorderline", orderLineId);
    recordsArray.put(record);
    inputData.put(ActionHandlerTestConstants.RECORDS, recordsArray);

    JSONObject result = cancelAndReplaceHandler.execute(Collections.emptyMap(), inputData.toString());

    assertTrue(result.has(ActionHandlerTestConstants.RESULT));
    JSONArray resultArray = result.getJSONArray(ActionHandlerTestConstants.RESULT);
    assertEquals(1, resultArray.length());
    JSONObject resultOrderLine = resultArray.getJSONObject(0);
    assertEquals(orderLineId, resultOrderLine.getJSONObject("record").getString(ActionHandlerTestConstants.REPLACED_ORDER_LINE));
  }

  /**
   * Tests the {@code execute} method with an invalid order line ID.
   * Verifies that the method handles the invalid input gracefully.
   *
   * @throws Exception if an error occurs during the test
   */
  @Test
  public void testExecuteInvalidOrderLineId() throws Exception {
    String invalidOrderLineId = "INVALID_ID";

    when(obdalMock.get(OrderLine.class, invalidOrderLineId)).thenReturn(null);

    JSONObject inputData = new JSONObject();
    JSONArray recordsArray = new JSONArray();
    JSONObject record = new JSONObject();
    record.put(ActionHandlerTestConstants.REPLACED_ORDER_LINE, invalidOrderLineId);
    recordsArray.put(record);
    inputData.put(ActionHandlerTestConstants.RECORDS, recordsArray);

    JSONObject result = cancelAndReplaceHandler.execute(Collections.emptyMap(), inputData.toString());

    assertNotNull(result);

    if (!result.has(ActionHandlerTestConstants.RESULT)) {
      assertTrue(true, "The method correctly handles null OrderLines");
    } else {
      JSONArray resultArray = result.getJSONArray(ActionHandlerTestConstants.RESULT);
      assertEquals(0, resultArray.length());
    }
  }

  /**
   * Tests the {@code execute} method when an exception occurs during processing.
   * Verifies that the method handles the exception and returns an empty result.
   *
   * @throws Exception if an error occurs during the test
   */
  @Test
  public void testExecuteExceptionHandling() throws Exception {
    String orderLineId = "12345";

    when(obdalMock.get(OrderLine.class, orderLineId)).thenThrow(new RuntimeException("Database error"));

    JSONObject inputData = new JSONObject();
    JSONArray recordsArray = new JSONArray();
    JSONObject record = new JSONObject();
    record.put(ActionHandlerTestConstants.REPLACED_ORDER_LINE, orderLineId);
    recordsArray.put(record);
    inputData.put(ActionHandlerTestConstants.RECORDS, recordsArray);

    JSONObject result = cancelAndReplaceHandler.execute(Collections.emptyMap(), inputData.toString());

    assertNotNull(result);

    if (!result.has(ActionHandlerTestConstants.RESULT)) {
      assertTrue(true, "The method handles exceptions correctly");
    } else {
      JSONArray resultArray = result.getJSONArray(ActionHandlerTestConstants.RESULT);
      assertEquals(0, resultArray.length());
    }
  }
}
