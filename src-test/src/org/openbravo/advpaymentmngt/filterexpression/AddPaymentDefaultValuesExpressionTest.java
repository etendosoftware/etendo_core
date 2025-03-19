package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Instance;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.client.application.OBBindingsConstants;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Unit tests for the AddPaymentDefaultValuesExpression class.
 */
@RunWith(MockitoJUnitRunner.class)
public class AddPaymentDefaultValuesExpressionTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private Instance<AddPaymentDefaultValuesHandler> addPaymentFilterExpressionHandlers;

  @InjectMocks
  private AddPaymentDefaultValuesExpression classUnderTest;

  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private AutoCloseable mocks;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
  }

  /**
   * Cleans up the test environment after each test.
   *
   * @throws Exception
   *     if an error occurs during teardown
   */
  @After
  public void tearDown() throws Exception {
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Tests the getExpression method with a handler for the actual payment parameter.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionWithHandlerActualPayment() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = createRequestMapWithWindowId(TestConstants.TEST_WINDOW_ID);
    requestMap.put(TestConstants.CURRENT_PARAM, "actual_payment");

    AddPaymentDefaultValuesHandler mockHandler = mock(AddPaymentDefaultValuesHandler.class);
    when(mockHandler.getDefaultActualAmount(requestMap)).thenReturn("100.00");

    Instance<AddPaymentDefaultValuesHandler> mockInstance = mock(Instance.class);
    when(addPaymentFilterExpressionHandlers.select(any(ComponentProvider.Selector.class))).thenReturn(mockInstance);
    when(mockInstance.iterator()).thenReturn(java.util.Collections.singletonList(mockHandler).iterator());

    // WHEN
    String result = classUnderTest.getExpression(requestMap);

    // THEN
    assertEquals("100.00", result);
    verify(mockHandler, times(1)).getDefaultActualAmount(requestMap);
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
  }

  /**
   * Tests the getExpression method with a handler for the expected payment parameter.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionWithHandlerExpectedPayment() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = createRequestMapWithWindowId(TestConstants.TEST_WINDOW_ID);
    requestMap.put(TestConstants.CURRENT_PARAM, "expected_payment");

    AddPaymentDefaultValuesHandler mockHandler = mock(AddPaymentDefaultValuesHandler.class);
    when(mockHandler.getDefaultExpectedAmount(requestMap)).thenReturn("200.00");

    Instance<AddPaymentDefaultValuesHandler> mockInstance = mock(Instance.class);
    when(addPaymentFilterExpressionHandlers.select(any(ComponentProvider.Selector.class))).thenReturn(mockInstance);
    when(mockInstance.iterator()).thenReturn(java.util.Collections.singletonList(mockHandler).iterator());

    // WHEN
    String result = classUnderTest.getExpression(requestMap);

    // THEN
    assertEquals("200.00", result);
    verify(mockHandler, times(1)).getDefaultExpectedAmount(requestMap);
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
  }

  /**
   * Tests the getExpression method with a handler for the document number parameter.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionWithHandlerDocumentNo() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = createRequestMapWithWindowId(TestConstants.TEST_WINDOW_ID);
    requestMap.put(TestConstants.CURRENT_PARAM, TestConstants.PAYMENT_DOCUMENT_NO);

    AddPaymentDefaultValuesHandler mockHandler = mock(AddPaymentDefaultValuesHandler.class);
    when(mockHandler.getDefaultDocumentNo(requestMap)).thenReturn(TestConstants.DOC_001);

    Instance<AddPaymentDefaultValuesHandler> mockInstance = mock(Instance.class);
    when(addPaymentFilterExpressionHandlers.select(any(ComponentProvider.Selector.class))).thenReturn(mockInstance);
    when(mockInstance.iterator()).thenReturn(java.util.Collections.singletonList(mockHandler).iterator());

    // WHEN
    String result = classUnderTest.getExpression(requestMap);

    // THEN
    assertEquals(TestConstants.DOC_001, result);
    verify(mockHandler, times(1)).getDefaultDocumentNo(requestMap);
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
  }

  /**
   * Tests the getExpression method with multiple handlers, selecting the one with the lowest sequence.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionMultipleHandlersSelectsLowestSeq() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = createRequestMapWithWindowId(TestConstants.TEST_WINDOW_ID);
    requestMap.put(TestConstants.CURRENT_PARAM, TestConstants.PAYMENT_DOCUMENT_NO);

    AddPaymentDefaultValuesHandler mockHandler1 = mock(AddPaymentDefaultValuesHandler.class);
    when(mockHandler1.getSeq()).thenReturn(20L);

    AddPaymentDefaultValuesHandler mockHandler2 = mock(AddPaymentDefaultValuesHandler.class);
    when(mockHandler2.getSeq()).thenReturn(10L);
    when(mockHandler2.getDefaultDocumentNo(requestMap)).thenReturn("DOC-002");

    Instance<AddPaymentDefaultValuesHandler> mockInstance = mock(Instance.class);
    when(addPaymentFilterExpressionHandlers.select(any(ComponentProvider.Selector.class))).thenReturn(mockInstance);
    when(mockInstance.iterator()).thenReturn(java.util.Arrays.asList(mockHandler1, mockHandler2).iterator());

    // WHEN
    String result = classUnderTest.getExpression(requestMap);

    // THEN
    assertEquals("DOC-002", result);
    verify(mockHandler2, times(1)).getDefaultDocumentNo(requestMap);
    verify(mockHandler1, times(0)).getDefaultDocumentNo(requestMap);
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
  }

  /**
   * Tests the getExpression method when the handler throws an exception.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionHandlerThrowsException() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = createRequestMapWithWindowId(TestConstants.TEST_WINDOW_ID);
    requestMap.put(TestConstants.CURRENT_PARAM, TestConstants.PAYMENT_DOCUMENT_NO);

    AddPaymentDefaultValuesHandler mockHandler = mock(AddPaymentDefaultValuesHandler.class);
    when(mockHandler.getDefaultDocumentNo(requestMap)).thenThrow(new RuntimeException("Test exception"));

    Instance<AddPaymentDefaultValuesHandler> mockInstance = mock(Instance.class);
    when(addPaymentFilterExpressionHandlers.select(any(ComponentProvider.Selector.class))).thenReturn(mockInstance);
    when(mockInstance.iterator()).thenReturn(java.util.Collections.singletonList(mockHandler).iterator());

    // WHEN
    String result = classUnderTest.getExpression(requestMap);

    // THEN
    assertNull("Result should be null when handler throws exception", result);
    verify(mockHandler, times(1)).getDefaultDocumentNo(requestMap);
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
  }

  /**
   * Tests the getExpression method when no window ID is provided.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionNoWindowId() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    requestMap.put(TestConstants.CURRENT_PARAM, TestConstants.PAYMENT_DOCUMENT_NO);

    AddPaymentDefaultValuesHandler mockHandler = mock(AddPaymentDefaultValuesHandler.class);
    when(mockHandler.getDefaultDocumentNo(requestMap)).thenReturn(TestConstants.DOC_001);

    Instance<AddPaymentDefaultValuesHandler> mockInstance = mock(Instance.class);
    when(addPaymentFilterExpressionHandlers.select(any(ComponentProvider.Selector.class))).thenReturn(mockInstance);
    when(mockInstance.iterator()).thenReturn(java.util.Collections.singletonList(mockHandler).iterator());

    // WHEN
    String result = classUnderTest.getExpression(requestMap);

    // THEN
    assertEquals(TestConstants.DOC_001, result);
    verify(mockHandler, times(1)).getDefaultDocumentNo(requestMap);
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
  }

  /**
   * Creates a request map with the given window ID.
   *
   * @param windowId
   *     the window ID to include in the request map
   * @return the created request map
   * @throws JSONException
   *     if a JSON error occurs
   */
  private Map<String, String> createRequestMapWithWindowId(String windowId) throws JSONException {
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(OBBindingsConstants.WINDOW_ID_PARAM, windowId);
    requestMap.put("context", context.toString());
    return requestMap;
  }
}
