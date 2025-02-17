/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2023 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

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
import org.openbravo.client.application.OBBindingsConstants;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;

@RunWith(MockitoJUnitRunner.class)
public class AddPaymentDefaultValuesExpressionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private Instance<AddPaymentDefaultValuesHandler> addPaymentFilterExpressionHandlers;

  @InjectMocks
  private AddPaymentDefaultValuesExpression classUnderTest;

  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private AutoCloseable mocks;

  @Before
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
  }

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

  @Test
  public void testGetExpressionWithHandlerActualPayment() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = createRequestMapWithWindowId("TEST_WINDOW");
    requestMap.put("currentParam", "actual_payment");
    
    AddPaymentDefaultValuesHandler mockHandler = mock(AddPaymentDefaultValuesHandler.class);
    when(mockHandler.getDefaultActualAmount(requestMap)).thenReturn("100.00");
    
    Instance<AddPaymentDefaultValuesHandler> mockInstance = mock(Instance.class);
    when(addPaymentFilterExpressionHandlers.select(any(ComponentProvider.Selector.class)))
        .thenReturn(mockInstance);
    when(mockInstance.iterator())
        .thenReturn(java.util.Collections.singletonList(mockHandler).iterator());
    
    // WHEN
    String result = classUnderTest.getExpression(requestMap);
    
    // THEN
    assertEquals("100.00", result);
    verify(mockHandler, times(1)).getDefaultActualAmount(requestMap);
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
  }

  @Test
  public void testGetExpressionWithHandlerExpectedPayment() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = createRequestMapWithWindowId("TEST_WINDOW");
    requestMap.put("currentParam", "expected_payment");
    
    AddPaymentDefaultValuesHandler mockHandler = mock(AddPaymentDefaultValuesHandler.class);
    when(mockHandler.getDefaultExpectedAmount(requestMap)).thenReturn("200.00");
    
    Instance<AddPaymentDefaultValuesHandler> mockInstance = mock(Instance.class);
    when(addPaymentFilterExpressionHandlers.select(any(ComponentProvider.Selector.class)))
        .thenReturn(mockInstance);
    when(mockInstance.iterator())
        .thenReturn(java.util.Collections.singletonList(mockHandler).iterator());
    
    // WHEN
    String result = classUnderTest.getExpression(requestMap);
    
    // THEN
    assertEquals("200.00", result);
    verify(mockHandler, times(1)).getDefaultExpectedAmount(requestMap);
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
  }

  @Test
  public void testGetExpressionWithHandlerDocumentNo() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = createRequestMapWithWindowId("TEST_WINDOW");
    requestMap.put("currentParam", "payment_documentno");
    
    AddPaymentDefaultValuesHandler mockHandler = mock(AddPaymentDefaultValuesHandler.class);
    when(mockHandler.getDefaultDocumentNo(requestMap)).thenReturn("DOC-001");
    
    Instance<AddPaymentDefaultValuesHandler> mockInstance = mock(Instance.class);
    when(addPaymentFilterExpressionHandlers.select(any(ComponentProvider.Selector.class)))
        .thenReturn(mockInstance);
    when(mockInstance.iterator())
        .thenReturn(java.util.Collections.singletonList(mockHandler).iterator());
    
    // WHEN
    String result = classUnderTest.getExpression(requestMap);
    
    // THEN
    assertEquals("DOC-001", result);
    verify(mockHandler, times(1)).getDefaultDocumentNo(requestMap);
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
  }

  @Test
  public void testGetExpressionMultipleHandlersSelectsLowestSeq() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = createRequestMapWithWindowId("TEST_WINDOW");
    requestMap.put("currentParam", "payment_documentno");
    
    AddPaymentDefaultValuesHandler mockHandler1 = mock(AddPaymentDefaultValuesHandler.class);
    when(mockHandler1.getSeq()).thenReturn(20L);
    
    AddPaymentDefaultValuesHandler mockHandler2 = mock(AddPaymentDefaultValuesHandler.class);
    when(mockHandler2.getSeq()).thenReturn(10L);
    when(mockHandler2.getDefaultDocumentNo(requestMap)).thenReturn("DOC-002");
    
    Instance<AddPaymentDefaultValuesHandler> mockInstance = mock(Instance.class);
    when(addPaymentFilterExpressionHandlers.select(any(ComponentProvider.Selector.class)))
        .thenReturn(mockInstance);
    when(mockInstance.iterator())
        .thenReturn(java.util.Arrays.asList(mockHandler1, mockHandler2).iterator());
    
    // WHEN
    String result = classUnderTest.getExpression(requestMap);
    
    // THEN
    assertEquals("DOC-002", result);
    verify(mockHandler2, times(1)).getDefaultDocumentNo(requestMap);
    verify(mockHandler1, times(0)).getDefaultDocumentNo(requestMap);
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
  }

  @Test
  public void testGetExpressionHandlerThrowsException() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = createRequestMapWithWindowId("TEST_WINDOW");
    requestMap.put("currentParam", "payment_documentno");
    
    AddPaymentDefaultValuesHandler mockHandler = mock(AddPaymentDefaultValuesHandler.class);
    when(mockHandler.getDefaultDocumentNo(requestMap)).thenThrow(new RuntimeException("Test exception"));
    
    Instance<AddPaymentDefaultValuesHandler> mockInstance = mock(Instance.class);
    when(addPaymentFilterExpressionHandlers.select(any(ComponentProvider.Selector.class)))
        .thenReturn(mockInstance);
    when(mockInstance.iterator())
        .thenReturn(java.util.Collections.singletonList(mockHandler).iterator());
    
    // WHEN
    String result = classUnderTest.getExpression(requestMap);
    
    // THEN
    assertNull("Result should be null when handler throws exception", result);
    verify(mockHandler, times(1)).getDefaultDocumentNo(requestMap);
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
  }

  @Test
  public void testGetExpressionNoWindowId() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    requestMap.put("currentParam", "payment_documentno");
    
    AddPaymentDefaultValuesHandler mockHandler = mock(AddPaymentDefaultValuesHandler.class);
    when(mockHandler.getDefaultDocumentNo(requestMap)).thenReturn("DOC-001");
    
    Instance<AddPaymentDefaultValuesHandler> mockInstance = mock(Instance.class);
    when(addPaymentFilterExpressionHandlers.select(any(ComponentProvider.Selector.class)))
        .thenReturn(mockInstance);
    when(mockInstance.iterator())
        .thenReturn(java.util.Collections.singletonList(mockHandler).iterator());
    
    // WHEN
    String result = classUnderTest.getExpression(requestMap);
    
    // THEN
    assertEquals("DOC-001", result);
    verify(mockHandler, times(1)).getDefaultDocumentNo(requestMap);
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
  }

  private Map<String, String> createRequestMapWithWindowId(String windowId) throws JSONException {
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(OBBindingsConstants.WINDOW_ID_PARAM, windowId);
    requestMap.put("context", context.toString());
    return requestMap;
  }
}
