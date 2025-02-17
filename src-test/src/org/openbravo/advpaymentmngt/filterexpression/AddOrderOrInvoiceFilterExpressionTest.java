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

import javax.enterprise.inject.Instance;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.client.application.OBBindingsConstants;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;

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
  private static final String TEST_WINDOW_ID = "TEST_WINDOW";
  private static final String TEST_PAYMENT_METHOD_ID = "TEST_PAYMENT_METHOD";
  private static final String TEST_IDENTIFIER = "TEST_IDENTIFIER";

  private MockedStatic<OBDal> obdalMock;

  @Before
  public void setUp() throws Exception {
    requestMap = new HashMap<>();

    obdalMock = mockStatic(OBDal.class);
    obdalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
    when(mockOBDal.get(eq(FIN_PaymentMethod.class), anyString())).thenReturn(paymentMethod);

    JSONObject context = new JSONObject();
    context.put(OBBindingsConstants.WINDOW_ID_PARAM, TEST_WINDOW_ID);
    requestMap.put("context", context.toString());

    Instance<AddOrderOrInvoiceFilterExpressionHandler> mockInstance = mock(Instance.class);
    when(addOrderOrInvoiceFilterExpressionHandlers.select(any())).thenReturn(mockInstance);
    when(mockInstance.iterator()).thenAnswer(invocation ->
        java.util.Collections.singletonList(mockHandler).iterator()
    );

    when(paymentMethod.getIdentifier()).thenReturn(TEST_IDENTIFIER);
  }

  @After
  public void tearDown() {
    if (obdalMock != null) {
      obdalMock.close();
    }
  }

  @Test
  public void testGetExpressionWithPaymentMethod() throws JSONException {
    requestMap.put("filterExpressionColumnName", "paymentMethodName");
    when(mockHandler.getFilterExpression(requestMap)).thenReturn(TEST_PAYMENT_METHOD_ID);

    String result = filterExpression.getExpression(requestMap);

    assertEquals(TEST_IDENTIFIER, result);
    verify(mockHandler).getFilterExpression(requestMap);
    verify(paymentMethod).getIdentifier();
  }

  @Test
  public void testGetExpressionWithNullPaymentMethod() throws JSONException {
    requestMap.put("filterExpressionColumnName", "paymentMethodName");
    when(mockHandler.getFilterExpression(requestMap)).thenReturn(null);

    String result = filterExpression.getExpression(requestMap);

    assertEquals("", result);
    verify(mockHandler).getFilterExpression(requestMap);
  }

  @Test
  public void testGetExpressionWithEmptyPaymentMethod() throws JSONException {
    requestMap.put("filterExpressionColumnName", "paymentMethodName");
    when(mockHandler.getFilterExpression(requestMap)).thenReturn("");

    String result = filterExpression.getExpression(requestMap);

    assertEquals("", result);
    verify(mockHandler).getFilterExpression(requestMap);
  }

  @Test
  public void testGetExpressionWithInvalidJson() {
    requestMap.put("context", "invalid json");
    requestMap.put("filterExpressionColumnName", "paymentMethodName");

    String result = filterExpression.getExpression(requestMap);

    assertEquals("", result);
  }

  @Test
  public void testGetExpressionWithMultipleHandlers() throws JSONException {
    requestMap.put("filterExpressionColumnName", "paymentMethodName");

    Instance<AddOrderOrInvoiceFilterExpressionHandler> mockInstance = mock(Instance.class);
    AddOrderOrInvoiceFilterExpressionHandler handler1 = mockHandler;
    AddOrderOrInvoiceFilterExpressionHandler handler2 = mock(AddOrderOrInvoiceFilterExpressionHandler.class);

    when(addOrderOrInvoiceFilterExpressionHandlers.select(any())).thenReturn(mockInstance);
    when(mockInstance.iterator()).thenReturn(java.util.Arrays.asList(handler1, handler2).iterator());

    when(handler1.getFilterExpression(requestMap)).thenReturn(TEST_PAYMENT_METHOD_ID);

    String result = filterExpression.getExpression(requestMap);

    assertEquals(TEST_IDENTIFIER, result);
    verify(handler1).getFilterExpression(requestMap);
    verify(paymentMethod).getIdentifier();
  }
}
