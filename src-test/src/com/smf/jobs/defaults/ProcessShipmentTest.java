package com.smf.jobs.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.ProcessShipmentUtil;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.service.db.DalConnectionProvider;

import com.smf.jobs.ActionResult;
import com.smf.jobs.Data;
import com.smf.jobs.Result;

/**
 * Unit tests for the {@code ProcessShipment} class.
 * Tests functionality related to shipment processing, covering:
 *  - Private methods using reflection.
 *  - Actions with successful and error outcomes.
 *  - Handling of invalid or edge-case inputs.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ProcessShipmentTest {

  /**
   * A testable subclass of {@code ProcessShipment} that exposes methods for testing.
   */
  public static class TestableProcessShipment extends ProcessShipment {

    /**
     * Overrides the {@code getInput} method to expose it for testing.
     * @return Data object representing the input for shipment processing.
     */
    @Override
    public Data getInput() {
      return super.getInput();
    }
  }

  @Spy
  @InjectMocks
  private TestableProcessShipment processShipment;

  @Mock
  private WeldUtils mockWeldUtils;

  @Mock
  private ProcessShipmentUtil mockProcessShipmentUtil;

  @Mock
  private RequestContext mockRequestContext;

  @Mock
  private VariablesSecureApp mockVars;

  @Mock
  private ShipmentInOut mockShipment;

  @Mock
  private Data mockData;

  private Method processShipmentMethod;

  /**
   * Initializes test dependencies and configurations, such as static mocks
   * for utility methods and reflection access to private methods.
   * @throws Exception If setup fails.
   */
  @Before
  public void setUp() throws Exception {
    processShipmentMethod = ProcessShipment.class.getDeclaredMethod(
        "processShipment",
        ShipmentInOut.class,
        String.class
    );
    processShipmentMethod.setAccessible(true);

    try (MockedStatic<OBMessageUtils> messageMock = mockStatic(OBMessageUtils.class)) {
      messageMock.when(() -> OBMessageUtils.messageBD(anyString()))
          .thenReturn("Test message");
      messageMock.when(() -> OBMessageUtils.parseTranslation(anyString(), any()))
          .thenReturn("Test message");
    }

  }

  /**
   * Tests the {@code processShipment} private method to ensure it processes
   * a shipment with valid data and returns a successful result.
   * @throws Exception If invocation of the private method fails.
   */
  @Test
  public void testProcessShipmentPrivateMethod() throws Exception {
    String shipmentId = "test-shipment-id";
    String docAction = "CO";
    OBError expectedResult = new OBError();
    expectedResult.setType("Success");
    expectedResult.setMessage("Shipment processed successfully");

    try (MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class)) {
      requestContextMock.when(RequestContext::get).thenReturn(mockRequestContext);
      when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);
      when(mockWeldUtils.getInstance(ProcessShipmentUtil.class)).thenReturn(mockProcessShipmentUtil);
      when(mockShipment.getId()).thenReturn(shipmentId);
      when(mockProcessShipmentUtil.process(
          eq(shipmentId),
          eq(docAction),
          any(VariablesSecureApp.class),
          any(DalConnectionProvider.class)
      )).thenReturn(expectedResult);

      OBError result = (OBError) processShipmentMethod.invoke(
          processShipment,
          mockShipment,
          docAction
      );

      assertNotNull("Result should not be null", result);
      assertEquals("Should return success type", "Success", result.getType());
      assertEquals(
          "Should return correct message",
          "Shipment processed successfully",
          result.getMessage()
      );
    }
  }

  /**
   * Verifies the successful execution of the {@code action} method when
   * valid shipments and parameters are provided.
   * @throws Exception If processing fails unexpectedly.
   */
  @Test
  public void testActionWithSuccessfulProcessing() throws Exception {
    JSONObject parameters = new JSONObject();
    parameters.put("DocAction", "CO");
    MutableBoolean isStopped = new MutableBoolean(false);

    List<ShipmentInOut> mockShipments = List.of(mockShipment);
    OBError successResult = new OBError();
    successResult.setType("Success");
    successResult.setMessage("Shipment processed successfully");

    try (MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class)) {
      requestContextMock.when(RequestContext::get).thenReturn(mockRequestContext);
      when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);
      when(mockWeldUtils.getInstance(ProcessShipmentUtil.class)).thenReturn(mockProcessShipmentUtil);
      when(mockShipment.getId()).thenReturn("testId");
      when(mockProcessShipmentUtil.process(
          anyString(),
          anyString(),
          any(VariablesSecureApp.class),
          any(DalConnectionProvider.class)
      )).thenReturn(successResult);

      doReturn(mockShipments).when(processShipment).getInputContents(any());
      doReturn(mockData).when(processShipment).getInput();

      ActionResult result = processShipment.action(parameters, isStopped);

      assertNotNull("Result should not be null", result);
      assertEquals("Should return success type", Result.Type.SUCCESS, result.getType());
      verify(mockProcessShipmentUtil, times(1)).process(
          anyString(),
          anyString(),
          any(VariablesSecureApp.class),
          any(DalConnectionProvider.class)
      );
    }
  }

  /**
   * Tests the {@code action} method to ensure it handles errors appropriately
   * when an unexpected condition occurs during shipment processing.
   */
  @Test
  public void testActionWithError() {
    JSONObject parameters = new JSONObject();
    MutableBoolean isStopped = new MutableBoolean(false);

    List<ShipmentInOut> mockShipments = List.of(mockShipment);

    doReturn(mockShipments).when(processShipment).getInputContents(any());

    ActionResult result = processShipment.action(parameters, isStopped);

    assertEquals("Should return error type", Result.Type.ERROR, result.getType());
  }

  /**
   * Checks that the {@code action} method can handle cases where input is empty,
   * returning a successful result without performing any actions.
   * @throws Exception If processing fails.
   */
  @Test
  public void testActionWithEmptyInput() throws Exception {
    JSONObject parameters = new JSONObject();
    parameters.put("DocAction", "CO");
    MutableBoolean isStopped = new MutableBoolean(false);

    List<ShipmentInOut> emptyShipments = Collections.emptyList();
    doReturn(emptyShipments).when(processShipment).getInputContents(any());
    doReturn(mockData).when(processShipment).getInput();

    ActionResult result = processShipment.action(parameters, isStopped);

    assertNotNull("Result should not be null", result);
    assertEquals("Should return success type for empty input", Result.Type.SUCCESS, result.getType());
  }

  /**
   * Tests the {@code processShipment} private method to ensure it processes
   * a valid shipment and returns an OBError with the expected success result.
   * Validates that the returned OBError contains the correct type and message.
   *
   * @throws Exception If invocation of the private method fails or an unexpected exception occurs.
   */
  @Test
  public void testProcessShipment() throws Exception {
    String shipmentId = "test-shipment-id";
    String docAction = "CO";
    OBError expectedResult = new OBError();
    expectedResult.setType("Success");
    expectedResult.setMessage("Shipment processed successfully");

    try (MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class)) {
      requestContextMock.when(RequestContext::get).thenReturn(mockRequestContext);
      when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);
      when(mockWeldUtils.getInstance(ProcessShipmentUtil.class)).thenReturn(mockProcessShipmentUtil);
      when(mockShipment.getId()).thenReturn(shipmentId);
      when(mockProcessShipmentUtil.process(
          eq(shipmentId),
          eq(docAction),
          any(VariablesSecureApp.class),
          any(DalConnectionProvider.class)
      )).thenReturn(expectedResult);

      OBError result = (OBError) processShipmentMethod.invoke(
          processShipment,
          mockShipment,
          docAction
      );

      assertNotNull("Result should not be null", result);
      assertEquals("Should return success type", "Success", result.getType());
    }
  }

  /**
   * Validates the {@code processShipment} private method when the input shipment is null,
   * ensuring a {@code NullPointerException} is thrown as expected.
   * @throws Exception If invocation fails.
   */
  @Test
  public void testProcessShipmentWithNullShipment() throws Exception {
    try (MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class)) {
      requestContextMock.when(RequestContext::get).thenReturn(mockRequestContext);
      when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);

      try {
        processShipmentMethod.invoke(processShipment, null, "CO");
        fail("Should throw NullPointerException");
      } catch (InvocationTargetException e) {
        assertTrue(e.getCause() instanceof NullPointerException);
      }
    }
  }

  /**
   * Ensures the {@code action} method identifies and handles invalid document actions,
   * returning an appropriate error result.
   * @throws Exception If an unexpected exception occurs.
   */
  @Test
  public void testActionWithInvalidDocAction() throws Exception {
    JSONObject parameters = new JSONObject();
    parameters.put("DocAction", "INVALID_ACTION");
    MutableBoolean isStopped = new MutableBoolean(false);

    List<ShipmentInOut> mockShipments = List.of(mockShipment);
    OBError errorResult = new OBError();
    errorResult.setType("Error");
    errorResult.setMessage("Invalid document action");

    try (MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class);
         MockedStatic<OBMessageUtils> messageMock = mockStatic(OBMessageUtils.class)) {

      requestContextMock.when(RequestContext::get).thenReturn(mockRequestContext);
      when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);
      when(mockWeldUtils.getInstance(ProcessShipmentUtil.class)).thenReturn(mockProcessShipmentUtil);
      when(mockShipment.getId()).thenReturn("testId");
      when(mockProcessShipmentUtil.process(
          anyString(),
          eq("INVALID_ACTION"),
          any(VariablesSecureApp.class),
          any(DalConnectionProvider.class)
      )).thenReturn(errorResult);

      messageMock.when(() -> OBMessageUtils.messageBD(anyString()))
          .thenReturn("Test message");

      doReturn(mockShipments).when(processShipment).getInputContents(any());
      doReturn(mockData).when(processShipment).getInput();

      ActionResult result = processShipment.action(parameters, isStopped);

      assertEquals("Should return error type for invalid action", Result.Type.ERROR, result.getType());
    }
  }

  /**
   * Tests processing of multiple shipments through the {@code action} method,
   * ensuring all shipments are processed individually and successfully.
   * @throws Exception If processing fails unexpectedly.
   */
  @Test
  public void testActionWithMultipleShipments() throws Exception {
    JSONObject parameters = new JSONObject();
    parameters.put("DocAction", "CO");
    MutableBoolean isStopped = new MutableBoolean(false);

    ShipmentInOut mockShipment2 = mock(ShipmentInOut.class);
    ShipmentInOut mockShipment3 = mock(ShipmentInOut.class);
    List<ShipmentInOut> multipleShipments = Arrays.asList(mockShipment, mockShipment2, mockShipment3);

    OBError successResult = new OBError();
    successResult.setType("Success");

    try (MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class);
         MockedStatic<OBMessageUtils> messageMock = mockStatic(OBMessageUtils.class)) {

      requestContextMock.when(RequestContext::get).thenReturn(mockRequestContext);
      when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);
      when(mockWeldUtils.getInstance(ProcessShipmentUtil.class)).thenReturn(mockProcessShipmentUtil);

      when(mockShipment.getId()).thenReturn("testId1");
      when(mockShipment2.getId()).thenReturn("testId2");
      when(mockShipment3.getId()).thenReturn("testId3");

      when(mockProcessShipmentUtil.process(
          anyString(),
          anyString(),
          any(VariablesSecureApp.class),
          any(DalConnectionProvider.class)
      )).thenReturn(successResult);

      messageMock.when(() -> OBMessageUtils.messageBD(anyString()))
          .thenReturn("Test message");

      doReturn(multipleShipments).when(processShipment).getInputContents(any());
      doReturn(mockData).when(processShipment).getInput();

      ActionResult result = processShipment.action(parameters, isStopped);

      assertNotNull("Result should not be null", result);
      assertEquals("Should return success type", Result.Type.SUCCESS, result.getType());
      verify(mockProcessShipmentUtil, times(3)).process(
          anyString(),
          anyString(),
          any(VariablesSecureApp.class),
          any(DalConnectionProvider.class)
      );
    }
  }

  /**
   * Verifies the correct input class type is returned by the {@code getInputClass} method.
   */
  @Test
  public void testGetInputClass() {
    assertEquals("Should return ShipmentInOut.class", ShipmentInOut.class, processShipment.getInputClass());
  }


}