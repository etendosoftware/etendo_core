package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.inject.Instance;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.client.application.OBBindingsConstants;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;

/**
 * Unit tests for the AddOrderOrInvoiceFilterExpression class.
 */
@RunWith(MockitoJUnitRunner.class)
public class AddOrderOrInvoiceFilterExpressionTest {

  @InjectMocks
  private AddOrderOrInvoiceFilterExpression filterExpression;

  @Mock
  private Instance<AddOrderOrInvoiceFilterExpressionHandler> addOrderOrInvoiceFilterExpressionHandlers;

  @Mock
  private AddOrderOrInvoiceFilterExpressionHandler mockHandler;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private FIN_PaymentMethod paymentMethod;

  private Map<String, String> requestMap;

  private MockedStatic<OBDal> obdalMock;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    requestMap = new HashMap<>();

    obdalMock = mockStatic(OBDal.class);
    obdalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
    when(mockOBDal.get(eq(FIN_PaymentMethod.class), anyString())).thenReturn(paymentMethod);

    JSONObject context = new JSONObject();
    context.put(OBBindingsConstants.WINDOW_ID_PARAM, TestConstants.TEST_WINDOW_ID);
    requestMap.put("context", context.toString());

    Instance<AddOrderOrInvoiceFilterExpressionHandler> mockInstance = mock(Instance.class);
    when(addOrderOrInvoiceFilterExpressionHandlers.select(any())).thenReturn(mockInstance);
    when(mockInstance.iterator()).thenAnswer(invocation -> java.util.Collections.singletonList(mockHandler).iterator());

    when(paymentMethod.getIdentifier()).thenReturn(TestConstants.TEST_IDENTIFIER);
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    if (obdalMock != null) {
      obdalMock.close();
    }
  }

  /**
   * Tests the getExpression method with a valid payment method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionWithPaymentMethod() throws JSONException {
    requestMap.put(TestConstants.FILTER_EXPRESSION_COLUMN_NAME, TestConstants.PAYMENT_METHOD_NAME);
    when(mockHandler.getFilterExpression(requestMap)).thenReturn(TestConstants.TEST_PAYMENT_METHOD_ID);

    String result = filterExpression.getExpression(requestMap);

    assertEquals(TestConstants.TEST_IDENTIFIER, result);
    verify(mockHandler).getFilterExpression(requestMap);
    verify(paymentMethod).getIdentifier();
  }

  /**
   * Tests the getExpression method with a null payment method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionWithNullPaymentMethod() throws JSONException {
    requestMap.put(TestConstants.FILTER_EXPRESSION_COLUMN_NAME, TestConstants.PAYMENT_METHOD_NAME);
    when(mockHandler.getFilterExpression(requestMap)).thenReturn(null);

    String result = filterExpression.getExpression(requestMap);

    assertEquals("", result);
    verify(mockHandler).getFilterExpression(requestMap);
  }

  /**
   * Tests the getExpression method with an empty payment method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionWithEmptyPaymentMethod() throws JSONException {
    requestMap.put(TestConstants.FILTER_EXPRESSION_COLUMN_NAME, TestConstants.PAYMENT_METHOD_NAME);
    when(mockHandler.getFilterExpression(requestMap)).thenReturn("");

    String result = filterExpression.getExpression(requestMap);

    assertEquals("", result);
    verify(mockHandler).getFilterExpression(requestMap);
  }

  /**
   * Tests the getExpression method with invalid JSON in the context.
   */
  @Test
  public void testGetExpressionWithInvalidJson() {
    requestMap.put("context", "invalid json");
    requestMap.put(TestConstants.FILTER_EXPRESSION_COLUMN_NAME, TestConstants.PAYMENT_METHOD_NAME);

    String result = filterExpression.getExpression(requestMap);

    assertEquals("", result);
  }

  /**
   * Tests the getExpression method with multiple handlers.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionWithMultipleHandlers() throws JSONException {
    requestMap.put(TestConstants.FILTER_EXPRESSION_COLUMN_NAME, TestConstants.PAYMENT_METHOD_NAME);

    Instance<AddOrderOrInvoiceFilterExpressionHandler> mockInstance = mock(Instance.class);
    AddOrderOrInvoiceFilterExpressionHandler handler1 = mockHandler;
    AddOrderOrInvoiceFilterExpressionHandler handler2 = mock(AddOrderOrInvoiceFilterExpressionHandler.class);

    when(addOrderOrInvoiceFilterExpressionHandlers.select(any())).thenReturn(mockInstance);
    when(mockInstance.iterator()).thenReturn(java.util.Arrays.asList(handler1, handler2).iterator());

    when(handler1.getFilterExpression(requestMap)).thenReturn(TestConstants.TEST_PAYMENT_METHOD_ID);

    String result = filterExpression.getExpression(requestMap);

    assertEquals(TestConstants.TEST_IDENTIFIER, result);
    verify(handler1).getFilterExpression(requestMap);
    verify(paymentMethod).getIdentifier();
  }
}
