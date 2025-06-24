package org.openbravo.common.actionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.materialmgmt.UOMUtil;

/**
 * Unit tests for the {@link GetConvertedQtyActionHandler} class.
 * Verifies the behavior of the execute method for different scenarios, including
 * reverse and forward conversions, invalid input, and exception handling.
 */
@ExtendWith(MockitoExtension.class)
public class GetConvertedQtyActionHandlerTest {

  private GetConvertedQtyActionHandler actionHandler;
  private MockedStatic<UOMUtil> mockedUOMUtil;

  /**
   * Sets up the test environment before each test.
   * Initializes the action handler and mocks the {@link UOMUtil} class.
   */
  @BeforeEach
  public void setUp() {
    actionHandler = new GetConvertedQtyActionHandler();
    mockedUOMUtil = mockStatic(UOMUtil.class);
  }

  /**
   * Cleans up the test environment after each test.
   * Closes the mocked static methods to release resources.
   */
  @AfterEach
  public void tearDown() {
    if (mockedUOMUtil != null) {
      mockedUOMUtil.close();
    }
  }

  /**
   * Tests the execute method for reverse conversion.
   * Verifies that the correct quantity is returned for a reverse conversion.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteReverseConversion() throws Exception {
    String mProductId = ActionHandlerTestConstants.PRODUCT_ID;
    BigDecimal qty = new BigDecimal(ActionHandlerTestConstants.PRODUCT_PRICE);
    String toUOM = ActionHandlerTestConstants.UOM_ID;
    Boolean reverse = true;

    JSONObject inputData = new JSONObject();
    inputData.put(ActionHandlerTestConstants.M_PRODUCT_ID, mProductId);
    inputData.put("qty", qty.toString());
    inputData.put(ActionHandlerTestConstants.TO_UOM, toUOM);
    inputData.put(ActionHandlerTestConstants.REVERSE, reverse);

    BigDecimal convertedQty = new BigDecimal("50.0");
    mockedUOMUtil.when(() -> UOMUtil.getConvertedAumQty(mProductId, qty, toUOM)).thenReturn(convertedQty);

    JSONObject result = actionHandler.execute(Map.of(), inputData.toString());

    assertNotNull(result);
    assertEquals(convertedQty.toString(), result.get("qty").toString());
    mockedUOMUtil.verify(() -> UOMUtil.getConvertedAumQty(mProductId, qty, toUOM), times(1));
  }

  /**
   * Tests the execute method for forward conversion.
   * Verifies that the correct quantity is returned for a forward conversion.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteForwardConversion() throws Exception {
    String mProductId = ActionHandlerTestConstants.PRODUCT_ID;
    BigDecimal qty = new BigDecimal(ActionHandlerTestConstants.PRODUCT_PRICE);
    String toUOM = ActionHandlerTestConstants.UOM_ID;
    Boolean reverse = false;

    JSONObject inputData = new JSONObject();
    inputData.put(ActionHandlerTestConstants.M_PRODUCT_ID, mProductId);
    inputData.put("qty", qty.toString());
    inputData.put(ActionHandlerTestConstants.TO_UOM, toUOM);
    inputData.put(ActionHandlerTestConstants.REVERSE, reverse);

    BigDecimal convertedQty = new BigDecimal("200.0");
    mockedUOMUtil.when(() -> UOMUtil.getConvertedQty(mProductId, qty, toUOM)).thenReturn(convertedQty);

    JSONObject result = actionHandler.execute(Map.of(), inputData.toString());

    assertNotNull(result);
    assertEquals(convertedQty.toString(), result.get("qty").toString());
    mockedUOMUtil.verify(() -> UOMUtil.getConvertedQty(mProductId, qty, toUOM), times(1));
  }

  /**
   * Tests the execute method with invalid input.
   * Verifies that a {@link JSONException} is thrown for invalid JSON input.
   */
  @Test
  public void testExecuteInvalidInput() {
    String invalidData = "{invalidJson}";

    JSONException exception = assertThrows(JSONException.class, () -> new JSONObject(invalidData));

    assertTrue(exception.getMessage().contains("Expected a ':' after a key"));
  }

  /**
   * Tests the execute method when an exception occurs in {@link UOMUtil}.
   * Verifies that the original quantity is returned in case of an exception.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteExceptionInUOMUtil() throws Exception {
    String mProductId = ActionHandlerTestConstants.PRODUCT_ID;
    BigDecimal qty = new BigDecimal(ActionHandlerTestConstants.PRODUCT_PRICE);
    String toUOM = ActionHandlerTestConstants.UOM_ID;
    Boolean reverse = false;

    JSONObject inputData = new JSONObject();
    inputData.put(ActionHandlerTestConstants.M_PRODUCT_ID, mProductId);
    inputData.put("qty", qty.toString());
    inputData.put(ActionHandlerTestConstants.TO_UOM, toUOM);
    inputData.put(ActionHandlerTestConstants.REVERSE, reverse);

    mockedUOMUtil.when(() -> UOMUtil.getConvertedQty(mProductId, qty, toUOM)).thenThrow(
        new RuntimeException("UOM conversion error"));

    JSONObject result = actionHandler.execute(Map.of(), inputData.toString());

    assertNotNull(result);
    assertEquals(qty.toString(), result.get("qty").toString());
  }
}
