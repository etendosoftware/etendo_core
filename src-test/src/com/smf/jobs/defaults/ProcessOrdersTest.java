package com.smf.jobs.defaults;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.ProcessOrderUtil;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.DalConnectionProvider;

import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;

/**
 * Unit tests for the {@code ProcessOrders} class.
 * Verifies the functionality for processing orders, including:
 *  - Reflection-based tests for private methods.
 *  - Handling of successful and erroneous scenarios during order processing.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessOrdersTest {

  @Spy
  @InjectMocks
  private ProcessOrders processOrders;

  @Mock
  private WeldUtils mockWeldUtils;

  @Mock
  private ProcessOrderUtil mockOrderUtil;

  @Mock
  private RequestContext mockRequestContext;

  @Mock
  private VariablesSecureApp mockVars;

  @Mock
  private Order mockOrder;

  private Method processOrderMethod;

  /**
   * Sets up the test environment, initializing mock dependencies and
   * preparing the {@code processOrder} method for reflection-based testing.
   *
   * @throws Exception If setup fails or reflection setup encounters issues.
   */
  @Before
  public void setUp() throws Exception {
    processOrderMethod = ProcessOrders.class.getDeclaredMethod(
        "processOrder",
        Order.class,
        String.class
    );
    processOrderMethod.setAccessible(true);
  }

  /**
   * Tests the {@code processOrder} private method using reflection to ensure
   * an order is successfully processed and the correct result is returned.
   *
   * @throws Exception If invocation of the private method fails.
   */
  @Test
  public void testProcessOrder() throws Exception {
    String orderId = "test-order-id";
    String docAction = "CO";
    OBError expectedResult = new OBError();
    expectedResult.setType("Success");
    expectedResult.setMessage("Order processed successfully");

    try (MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class)) {
      requestContextMock.when(RequestContext::get).thenReturn(mockRequestContext);
      when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);
      when(mockWeldUtils.getInstance(ProcessOrderUtil.class)).thenReturn(mockOrderUtil);
      when(mockOrder.getId()).thenReturn(orderId);
      when(mockOrderUtil.process(
          eq(orderId),
          eq(docAction),
          any(VariablesSecureApp.class),
          any(DalConnectionProvider.class)
      )).thenReturn(expectedResult);

      OBError result = (OBError) processOrderMethod.invoke(
          processOrders,
          mockOrder,
          docAction
      );

      assertEquals("Should return success type", "Success", result.getType());
      assertEquals(
          "Should return correct message",
          "Order processed successfully",
          result.getMessage()
      );
    }
  }

  /**
   * Verifies the successful execution of the {@code action} method when
   * valid orders and parameters are provided.
   *
   * @throws Exception If the processing fails unexpectedly.
   */
  @Test
  public void testActionWithSuccessfulProcessing() throws Exception {
    JSONObject parameters = new JSONObject();
    parameters.put("DocAction", "CO");
    MutableBoolean isStopped = new MutableBoolean(false);

    List<Order> mockOrders = Arrays.asList(mockOrder);
    OBError successResult = new OBError();
    successResult.setType("Success");

    doReturn(mockOrders).when(processOrders).getInputContents(any());

    try (MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class)) {
      requestContextMock.when(RequestContext::get).thenReturn(mockRequestContext);
      when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);
      when(mockWeldUtils.getInstance(ProcessOrderUtil.class)).thenReturn(mockOrderUtil);
      when(mockOrder.getId()).thenReturn("testId");
      when(mockOrderUtil.process(
          anyString(),
          anyString(),
          any(VariablesSecureApp.class),
          any(DalConnectionProvider.class)
      )).thenReturn(successResult);

      ActionResult result = processOrders.action(parameters, isStopped);

      assertEquals("Should return success type", Result.Type.SUCCESS, result.getType());
    }
  }

  /**
   * Tests the {@code action} method to ensure it correctly handles a scenario
   * where no valid input orders are provided, returning an error result.
   */
  @Test
  public void testActionWithError() {
    JSONObject parameters = new JSONObject();
    MutableBoolean isStopped = new MutableBoolean(false);

    doReturn(List.of()).when(processOrders).getInputContents(any());

    ActionResult result = processOrders.action(parameters, isStopped);

    assertEquals("Should return error type", Result.Type.ERROR, result.getType());
  }

  /**
   * Validates the correct input class type returned by the {@code getInputClass} method.
   * Ensures it matches the expected {@code Order.class} type.
   */
  @Test
  public void testGetInputClass() {
    assertEquals("Should return Order.class", Order.class, processOrders.getInputClass());
  }
}