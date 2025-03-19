package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.enterprise.util.TypeLiteral;

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
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.OBBindingsConstants;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Test class for AddPaymentReadOnlyLogicsExpression.
 */
@RunWith(MockitoJUnitRunner.class)
public class AddPaymentReadOnlyLogicsExpressionTest {

  @Mock
  private Instance<AddPaymentReadOnlyLogicsHandler> addPaymentFilterExpressionHandlers;

  @Mock
  private AddPaymentReadOnlyLogicsHandler mockHandler;

  @InjectMocks
  private AddPaymentReadOnlyLogicsExpression expressionUnderTest;

  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;

  private Map<String, String> requestMap;


  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);

    requestMap = createRequestMap(TestConstants.TEST_WINDOW_ID);

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("APRM_NOHANDLER")).thenReturn(TestConstants.ERROR_MSG);

    when(addPaymentFilterExpressionHandlers.select(any())).thenReturn(new FilteredInstanceMock<>(mockHandler));

    when(mockHandler.getSeq()).thenReturn(10L);
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
  }

  /**
   * Helper method to create a request map with the specified window ID.
   *
   * @param windowId
   *     the window ID
   * @return the request map
   * @throws JSONException
   *     if a JSON error occurs
   */
  private Map<String, String> createRequestMap(String windowId) throws JSONException {
    Map<String, String> map = new HashMap<>();
    if (windowId != null) {
      JSONObject contextJson = new JSONObject();
      contextJson.put(OBBindingsConstants.WINDOW_ID_PARAM, windowId);
      map.put("context", contextJson.toString());
    }
    return map;
  }

  /**
   * Helper method to set up the currentParam in the request map.
   *
   * @param paramName
   *     the parameter name
   */
  private void setCurrentParam(String paramName) {
    requestMap.put("currentParam", paramName);
  }

  /**
   * Tests the getExpression method for the PaymentDocumentNo parameter.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionPaymentDocumentNo() throws JSONException {
    setCurrentParam(TestConstants.PAYMENT_DOCUMENTNO_READONLY_LOGIC);
    when(mockHandler.getPaymentDocumentNoReadOnlyLogic(requestMap)).thenReturn(true);

    String result = expressionUnderTest.getExpression(requestMap);

    assertEquals("Y", result);
    verify(mockHandler).getPaymentDocumentNoReadOnlyLogic(requestMap);
    verifyNoMoreInteractions(mockHandler);
    mockedOBContext.verify(() -> OBContext.setAdminMode(true));
    mockedOBContext.verify(OBContext::restorePreviousMode);
  }

  /**
   * Tests the getExpression method for the ReceivedFrom parameter.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionReceivedFrom() throws JSONException {
    setCurrentParam("received_from_readonly_logic");
    when(mockHandler.getReceivedFromReadOnlyLogic(requestMap)).thenReturn(false);

    String result = expressionUnderTest.getExpression(requestMap);

    assertEquals("N", result);
    verify(mockHandler).getReceivedFromReadOnlyLogic(requestMap);
  }

  /**
   * Tests the getExpression method for the PaymentMethod parameter.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionPaymentMethod() throws JSONException {
    setCurrentParam("payment_method_readonly_logic");
    when(mockHandler.getPaymentMethodReadOnlyLogic(requestMap)).thenReturn(true);

    String result = expressionUnderTest.getExpression(requestMap);

    assertEquals("Y", result);
    verify(mockHandler).getPaymentMethodReadOnlyLogic(requestMap);
  }

  /**
   * Tests the getExpression method for the ActualPayment parameter.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionActualPayment() throws JSONException {
    setCurrentParam("actual_payment_readonly_logic");
    when(mockHandler.getActualPaymentReadOnlyLogic(requestMap)).thenReturn(false);

    String result = expressionUnderTest.getExpression(requestMap);

    assertEquals("N", result);
    verify(mockHandler).getActualPaymentReadOnlyLogic(requestMap);
  }

  /**
   * Tests the getExpression method for the ConvertedAmount parameter.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionConvertedAmount() throws JSONException {
    setCurrentParam("converted_amount_readonly_logic");
    when(mockHandler.getConvertedAmountReadOnlyLogic(requestMap)).thenReturn(true);

    String result = expressionUnderTest.getExpression(requestMap);

    assertEquals("Y", result);
    verify(mockHandler).getConvertedAmountReadOnlyLogic(requestMap);
  }

  /**
   * Tests the getExpression method for the PaymentDate parameter.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionPaymentDate() throws JSONException {
    setCurrentParam("payment_date_readonly_logic");
    when(mockHandler.getPaymentDateReadOnlyLogic(requestMap)).thenReturn(false);

    String result = expressionUnderTest.getExpression(requestMap);

    assertEquals("N", result);
    verify(mockHandler).getPaymentDateReadOnlyLogic(requestMap);
  }

  /**
   * Tests the getExpression method for the FinancialAccount parameter.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionFinancialAccount() throws JSONException {
    setCurrentParam("fin_financial_account_id_readonly_logic");
    when(mockHandler.getFinancialAccountReadOnlyLogic(requestMap)).thenReturn(true);

    String result = expressionUnderTest.getExpression(requestMap);

    assertEquals("Y", result);
    verify(mockHandler).getFinancialAccountReadOnlyLogic(requestMap);
  }

  /**
   * Tests the getExpression method for the ConversionRate parameter.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionConversionRate() throws JSONException {
    setCurrentParam("conversion_rate_readonly_logic");
    when(mockHandler.getConversionRateReadOnlyLogic(requestMap)).thenReturn(false);

    String result = expressionUnderTest.getExpression(requestMap);

    assertEquals("N", result);
    verify(mockHandler).getConversionRateReadOnlyLogic(requestMap);
  }

  /**
   * Tests the getExpression method for the Currency parameter.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionCurrency() throws JSONException {
    setCurrentParam("c_currency_id_readonly_logic");
    when(mockHandler.getCurrencyReadOnlyLogic(requestMap)).thenReturn(true);

    String result = expressionUnderTest.getExpression(requestMap);

    assertEquals("Y", result);
    verify(mockHandler).getCurrencyReadOnlyLogic(requestMap);
  }

  /**
   * Tests the getExpression method when no handler is found.
   */
  @Test
  public void testGetExpressionWithNoHandler() {
    setCurrentParam(TestConstants.PAYMENT_DOCUMENTNO_READONLY_LOGIC);
    when(addPaymentFilterExpressionHandlers.select(any())).thenReturn(new FilteredInstanceMock<>());

    OBException exception = assertThrows(OBException.class, () -> expressionUnderTest.getExpression(requestMap));

    assertEquals(TestConstants.ERROR_MSG, exception.getMessage());
    mockedOBMessageUtils.verify(() -> OBMessageUtils.messageBD("APRM_NOHANDLER"));
  }

  /**
   * Tests the getExpression method when the handler throws an exception.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionWithExceptionInHandler() throws JSONException {
    setCurrentParam(TestConstants.PAYMENT_DOCUMENTNO_READONLY_LOGIC);
    when(mockHandler.getPaymentDocumentNoReadOnlyLogic(requestMap)).thenThrow(new RuntimeException("Test exception"));

    String result = expressionUnderTest.getExpression(requestMap);

    assertNull(result);
    verify(mockHandler).getPaymentDocumentNoReadOnlyLogic(requestMap);
  }

  /**
   * Tests the handler selection when multiple handlers with different sequences are available.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testHandlerSelectionWithMultipleHandlersWithDifferentSequences() throws JSONException {
    setCurrentParam(TestConstants.PAYMENT_DOCUMENTNO_READONLY_LOGIC);

    AddPaymentReadOnlyLogicsHandler mockHandler2 = mock(AddPaymentReadOnlyLogicsHandler.class);
    when(mockHandler2.getSeq()).thenReturn(20L);

    when(mockHandler.getSeq()).thenReturn(10L);
    when(mockHandler.getPaymentDocumentNoReadOnlyLogic(requestMap)).thenReturn(true);

    when(addPaymentFilterExpressionHandlers.select(any())).thenReturn(
        new FilteredInstanceMock<>(mockHandler, mockHandler2));

    String result = expressionUnderTest.getExpression(requestMap);

    assertEquals("Y", result);
    verify(mockHandler).getPaymentDocumentNoReadOnlyLogic(requestMap);
    verify(mockHandler2, never()).getPaymentDocumentNoReadOnlyLogic(any());
  }

  /**
   * Tests the getExpression method when the request map does not contain context.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionWithoutContextInRequestMap() throws JSONException {
    requestMap = createRequestMap(null);
    setCurrentParam(TestConstants.PAYMENT_DOCUMENTNO_READONLY_LOGIC);

    when(mockHandler.getPaymentDocumentNoReadOnlyLogic(requestMap)).thenReturn(true);

    String result = expressionUnderTest.getExpression(requestMap);

    assertEquals("Y", result);
    verify(mockHandler).getPaymentDocumentNoReadOnlyLogic(requestMap);
  }

  /**
   * Helper class to simulate the behavior of Instance.select() method in CDI.
   */
  private static class FilteredInstanceMock<T> implements Instance<T> {
    private final T[] elements;

    @SafeVarargs
    public FilteredInstanceMock(T... elements) {
      this.elements = elements;
    }

    @Override
    public Instance<T> select(Annotation... annotations) {
      return null;
    }

    @Override
    public <U extends T> Instance<U> select(Class<U> aClass, Annotation... annotations) {
      return null;
    }

    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> typeLiteral, Annotation... annotations) {
      return null;
    }

    @Override
    public boolean isUnsatisfied() {
      return elements.length == 0;
    }

    @Override
    public boolean isAmbiguous() {
      return elements.length > 1;
    }

    @Override
    public void destroy(T instance) {
      // Do nothing
    }

    @Override
    public T get() {
      if (elements.length == 0) {
        throw new IllegalStateException("No instance available");
      }
      return elements[0];
    }

    @Override
    public java.util.Iterator<T> iterator() {
      return new java.util.Iterator<T>() {
        private int index = 0;

        @Override
        public boolean hasNext() {
          return index < elements.length;
        }

        @Override
        public T next() {
          return elements[index++];
        }
      };
    }
  }
}
