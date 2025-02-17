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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.utility.APRMConstants;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test class for SalesOrderAddPaymentReadOnlyLogics
 */
@RunWith(MockitoJUnitRunner.class)
public class SalesOrderAddPaymentReadOnlyLogicsTest extends OBBaseTest {

  @InjectMocks
  private SalesOrderAddPaymentReadOnlyLogics logics;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<Preferences> mockedPreferences;

  @Mock
  private OBDal mockOBDal;
  
  @Mock
  private OBContext mockOBContext;
  
  @Mock
  private Window mockWindow;

  private AutoCloseable mocks;
  private Map<String, String> requestMap;

  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    
    // Initialize request map
    requestMap = new HashMap<>();
    
    // Setup static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);
    
    mockedPreferences = mockStatic(Preferences.class);
  }

  @After
  public void tearDown() throws Exception {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedPreferences != null) {
      mockedPreferences.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  @Test
  public void testGetSeq() {
    // WHEN
    long sequence = logics.getSeq();
    
    // THEN
    assertEquals("Sequence should be 100", 100L, sequence);
  }

  @Test
  public void testGetPaymentDocumentNoReadOnlyLogic() throws JSONException {
    // WHEN
    boolean result = logics.getPaymentDocumentNoReadOnlyLogic(requestMap);
    
    // THEN
    assertFalse("Payment document number should not be read-only", result);
  }

  @Test
  public void testGetReceivedFromReadOnlyLogic() throws JSONException {
    // WHEN
    boolean result = logics.getReceivedFromReadOnlyLogic(requestMap);
    
    // THEN
    assertTrue("Received from field should be read-only", result);
  }

  @Test
  public void testGetPaymentMethodReadOnlyLogic() throws JSONException {
    // WHEN
    boolean result = logics.getPaymentMethodReadOnlyLogic(requestMap);
    
    // THEN
    assertFalse("Payment method should not be read-only", result);
  }

  @Test
  public void testGetActualPaymentReadOnlyLogic() throws JSONException {
    // WHEN
    boolean result = logics.getActualPaymentReadOnlyLogic(requestMap);
    
    // THEN
    assertFalse("Actual payment should not be read-only", result);
  }

  @Test
  public void testGetPaymentDateReadOnlyLogic() throws JSONException {
    // WHEN
    boolean result = logics.getPaymentDateReadOnlyLogic(requestMap);
    
    // THEN
    assertFalse("Payment date should not be read-only", result);
  }

  @Test
  public void testGetFinancialAccountReadOnlyLogic() throws JSONException {
    // WHEN
    boolean result = logics.getFinancialAccountReadOnlyLogic(requestMap);
    
    // THEN
    assertFalse("Financial account should not be read-only", result);
  }

  @Test
  public void testGetCurrencyReadOnlyLogic() throws JSONException {
    // WHEN
    boolean result = logics.getCurrencyReadOnlyLogic(requestMap);
    
    // THEN
    assertTrue("Currency should be read-only", result);
  }

  @Test
  public void testGetConvertedAmountReadOnlyLogic_NoContext() throws JSONException {
    // WHEN
    boolean result = logics.getConvertedAmountReadOnlyLogic(requestMap);
    
    // THEN
    assertFalse("Converted amount should not be read-only when no context is provided", result);
  }

  @Test
  public void testGetConvertedAmountReadOnlyLogic_WithContext_PreferenceYes() throws JSONException, PropertyNotFoundException, PropertyException {
    // GIVEN
    JSONObject context = new JSONObject();
    context.put("inpwindowId", APRMConstants.SALES_ORDER_WINDOW_ID);
    requestMap.put("context", context.toString());
    
    when(mockOBDal.get(Window.class, APRMConstants.SALES_ORDER_WINDOW_ID)).thenReturn(mockWindow);
    
    mockedPreferences.when(() -> Preferences.getPreferenceValue("NotAllowChangeExchange", true, 
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), 
        mockOBContext.getUser(), mockOBContext.getRole(), mockWindow))
        .thenReturn(Preferences.YES);
    
    // WHEN
    boolean result = logics.getConvertedAmountReadOnlyLogic(requestMap);
    
    // THEN
    assertTrue("Converted amount should be read-only when preference is YES", result);
  }

  @Test
  public void testGetConvertedAmountReadOnlyLogic_WithContext_PreferenceNo() throws JSONException, PropertyNotFoundException, PropertyException {
    // GIVEN
    JSONObject context = new JSONObject();
    context.put("inpwindowId", APRMConstants.SALES_ORDER_WINDOW_ID);
    requestMap.put("context", context.toString());
    
    when(mockOBDal.get(Window.class, APRMConstants.SALES_ORDER_WINDOW_ID)).thenReturn(mockWindow);
    
    mockedPreferences.when(() -> Preferences.getPreferenceValue("NotAllowChangeExchange", true, 
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), 
        mockOBContext.getUser(), mockOBContext.getRole(), mockWindow))
        .thenReturn(Preferences.NO);
    
    // WHEN
    boolean result = logics.getConvertedAmountReadOnlyLogic(requestMap);
    
    // THEN
    assertFalse("Converted amount should not be read-only when preference is NO", result);
  }

  @Test
  public void testGetConvertedAmountReadOnlyLogic_WithContext_PropertyNotFoundException() throws JSONException, PropertyNotFoundException, PropertyException {
    // GIVEN
    JSONObject context = new JSONObject();
    context.put("inpwindowId", APRMConstants.SALES_ORDER_WINDOW_ID);
    requestMap.put("context", context.toString());
    
    when(mockOBDal.get(Window.class, APRMConstants.SALES_ORDER_WINDOW_ID)).thenReturn(mockWindow);
    
    mockedPreferences.when(() -> Preferences.getPreferenceValue("NotAllowChangeExchange", true, 
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), 
        mockOBContext.getUser(), mockOBContext.getRole(), mockWindow))
        .thenThrow(new PropertyNotFoundException());
    
    // WHEN
    boolean result = logics.getConvertedAmountReadOnlyLogic(requestMap);
    
    // THEN
    assertFalse("Converted amount should not be read-only when property is not found", result);
  }

  @Test
  public void testGetConvertedAmountReadOnlyLogic_WithContext_PropertyException() throws JSONException, PropertyNotFoundException, PropertyException {
    // GIVEN
    JSONObject context = new JSONObject();
    context.put("inpwindowId", APRMConstants.SALES_ORDER_WINDOW_ID);
    requestMap.put("context", context.toString());
    
    when(mockOBDal.get(Window.class, APRMConstants.SALES_ORDER_WINDOW_ID)).thenReturn(mockWindow);
    
    mockedPreferences.when(() -> Preferences.getPreferenceValue("NotAllowChangeExchange", true, 
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), 
        mockOBContext.getUser(), mockOBContext.getRole(), mockWindow))
        .thenThrow(new PropertyException());
    
    // WHEN
    boolean result = logics.getConvertedAmountReadOnlyLogic(requestMap);
    
    // THEN
    assertFalse("Converted amount should not be read-only when property exception occurs", result);
  }

  @Test
  public void testGetConversionRateReadOnlyLogic_NoContext() throws JSONException {
    // WHEN
    boolean result = logics.getConversionRateReadOnlyLogic(requestMap);
    
    // THEN
    assertFalse("Conversion rate should not be read-only when no context is provided", result);
  }

  @Test
  public void testGetConversionRateReadOnlyLogic_WithContext_PreferenceYes() throws JSONException, PropertyNotFoundException, PropertyException {
    // GIVEN
    JSONObject context = new JSONObject();
    context.put("inpwindowId", APRMConstants.SALES_ORDER_WINDOW_ID);
    requestMap.put("context", context.toString());
    
    when(mockOBDal.get(Window.class, APRMConstants.SALES_ORDER_WINDOW_ID)).thenReturn(mockWindow);
    
    mockedPreferences.when(() -> Preferences.getPreferenceValue("NotAllowChangeExchange", true, 
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), 
        mockOBContext.getUser(), mockOBContext.getRole(), mockWindow))
        .thenReturn(Preferences.YES);
    
    // WHEN
    boolean result = logics.getConversionRateReadOnlyLogic(requestMap);
    
    // THEN
    assertTrue("Conversion rate should be read-only when preference is YES", result);
  }

  @Test
  public void testGetConversionRateReadOnlyLogic_WithContext_PreferenceNo() throws JSONException, PropertyNotFoundException, PropertyException {
    // GIVEN
    JSONObject context = new JSONObject();
    context.put("inpwindowId", APRMConstants.SALES_ORDER_WINDOW_ID);
    requestMap.put("context", context.toString());
    
    when(mockOBDal.get(Window.class, APRMConstants.SALES_ORDER_WINDOW_ID)).thenReturn(mockWindow);
    
    mockedPreferences.when(() -> Preferences.getPreferenceValue("NotAllowChangeExchange", true, 
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), 
        mockOBContext.getUser(), mockOBContext.getRole(), mockWindow))
        .thenReturn(Preferences.NO);
    
    // WHEN
    boolean result = logics.getConversionRateReadOnlyLogic(requestMap);
    
    // THEN
    assertFalse("Conversion rate should not be read-only when preference is NO", result);
  }

  @Test
  public void testGetConversionRateReadOnlyLogic_WithContext_PropertyNotFoundException() throws JSONException, PropertyNotFoundException, PropertyException {
    // GIVEN
    JSONObject context = new JSONObject();
    context.put("inpwindowId", APRMConstants.SALES_ORDER_WINDOW_ID);
    requestMap.put("context", context.toString());
    
    when(mockOBDal.get(Window.class, APRMConstants.SALES_ORDER_WINDOW_ID)).thenReturn(mockWindow);
    
    mockedPreferences.when(() -> Preferences.getPreferenceValue("NotAllowChangeExchange", true, 
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), 
        mockOBContext.getUser(), mockOBContext.getRole(), mockWindow))
        .thenThrow(new PropertyNotFoundException());
    
    // WHEN
    boolean result = logics.getConversionRateReadOnlyLogic(requestMap);
    
    // THEN
    assertFalse("Conversion rate should not be read-only when property is not found", result);
  }

  @Test
  public void testGetConversionRateReadOnlyLogic_WithContext_PropertyException() throws JSONException, PropertyNotFoundException, PropertyException {
    // GIVEN
    JSONObject context = new JSONObject();
    context.put("inpwindowId", APRMConstants.SALES_ORDER_WINDOW_ID);
    requestMap.put("context", context.toString());
    
    when(mockOBDal.get(Window.class, APRMConstants.SALES_ORDER_WINDOW_ID)).thenReturn(mockWindow);
    
    mockedPreferences.when(() -> Preferences.getPreferenceValue("NotAllowChangeExchange", true, 
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), 
        mockOBContext.getUser(), mockOBContext.getRole(), mockWindow))
        .thenThrow(new PropertyException());
    
    // WHEN
    boolean result = logics.getConversionRateReadOnlyLogic(requestMap);
    
    // THEN
    assertFalse("Conversion rate should not be read-only when property exception occurs", result);
  }
}