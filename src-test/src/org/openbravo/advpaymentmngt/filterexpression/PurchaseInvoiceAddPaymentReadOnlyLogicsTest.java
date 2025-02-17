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

/**
 * Test class for PurchaseInvoiceAddPaymentReadOnlyLogics
 */
@RunWith(MockitoJUnitRunner.class)
public class PurchaseInvoiceAddPaymentReadOnlyLogicsTest {

  @InjectMocks
  private PurchaseInvoiceAddPaymentReadOnlyLogics classUnderTest;

  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<Preferences> mockedPreferences;

  private AutoCloseable mocks;

  @Mock
  private OBContext mockOBContext;
  
  @Mock
  private OBDal mockOBDal;
  
  @Mock
  private Window mockWindow;

  @Before
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    
    // Setup static mocks
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);
    
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    
    mockedPreferences = mockStatic(Preferences.class);
  }

  @After
  public void tearDown() throws Exception {
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBDal != null) {
      mockedOBDal.close();
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
    // When
    long result = classUnderTest.getSeq();
    
    // Then
    assertEquals("Sequence should be 100", 100L, result);
  }

  @Test
  public void testGetPaymentDocumentNoReadOnlyLogic() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    boolean result = classUnderTest.getPaymentDocumentNoReadOnlyLogic(requestMap);
    
    // Then
    assertFalse("Payment document number should not be read-only", result);
  }

  @Test
  public void testGetReceivedFromReadOnlyLogic() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    boolean result = classUnderTest.getReceivedFromReadOnlyLogic(requestMap);
    
    // Then
    assertTrue("Received from field should be read-only", result);
  }

  @Test
  public void testGetPaymentMethodReadOnlyLogic() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    boolean result = classUnderTest.getPaymentMethodReadOnlyLogic(requestMap);
    
    // Then
    assertFalse("Payment method should not be read-only", result);
  }

  @Test
  public void testGetActualPaymentReadOnlyLogic() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    boolean result = classUnderTest.getActualPaymentReadOnlyLogic(requestMap);
    
    // Then
    assertTrue("Actual payment should be read-only", result);
  }

  @Test
  public void testGetPaymentDateReadOnlyLogic() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    boolean result = classUnderTest.getPaymentDateReadOnlyLogic(requestMap);
    
    // Then
    assertFalse("Payment date should not be read-only", result);
  }

  @Test
  public void testGetFinancialAccountReadOnlyLogic() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    boolean result = classUnderTest.getFinancialAccountReadOnlyLogic(requestMap);
    
    // Then
    assertFalse("Financial account should not be read-only", result);
  }

  @Test
  public void testGetCurrencyReadOnlyLogic() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    boolean result = classUnderTest.getCurrencyReadOnlyLogic(requestMap);
    
    // Then
    assertTrue("Currency should be read-only", result);
  }

  @Test
  public void testGetConvertedAmountReadOnlyLogic_NullContext() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    boolean result = classUnderTest.getConvertedAmountReadOnlyLogic(requestMap);
    
    // Then
    assertFalse("Converted amount should not be read-only when context is null", result);
  }

  @Test
  public void testGetConvertedAmountReadOnlyLogic_PreferenceYes() throws JSONException, PropertyNotFoundException, PropertyException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpwindowId", APRMConstants.PURCHASE_INVOICE_WINDOW_ID);
    requestMap.put("context", context.toString());
    
    when(mockOBDal.get(Window.class, APRMConstants.PURCHASE_INVOICE_WINDOW_ID)).thenReturn(mockWindow);
    
    mockedPreferences.when(() -> Preferences.getPreferenceValue("NotAllowChangeExchange", true, 
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), 
        mockOBContext.getUser(), mockOBContext.getRole(), mockWindow))
        .thenReturn(Preferences.YES);
    
    // When
    boolean result = classUnderTest.getConvertedAmountReadOnlyLogic(requestMap);
    
    // Then
    assertTrue("Converted amount should be read-only when preference is YES", result);
  }

  @Test
  public void testGetConvertedAmountReadOnlyLogic_PreferenceNo() throws JSONException, PropertyNotFoundException, PropertyException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpwindowId", APRMConstants.PURCHASE_INVOICE_WINDOW_ID);
    requestMap.put("context", context.toString());
    
    when(mockOBDal.get(Window.class, APRMConstants.PURCHASE_INVOICE_WINDOW_ID)).thenReturn(mockWindow);
    
    mockedPreferences.when(() -> Preferences.getPreferenceValue("NotAllowChangeExchange", true, 
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), 
        mockOBContext.getUser(), mockOBContext.getRole(), mockWindow))
        .thenReturn(Preferences.NO);
    
    // When
    boolean result = classUnderTest.getConvertedAmountReadOnlyLogic(requestMap);
    
    // Then
    assertFalse("Converted amount should not be read-only when preference is NO", result);
  }

  @Test
  public void testGetConvertedAmountReadOnlyLogic_PropertyNotFoundException() throws JSONException, PropertyNotFoundException, PropertyException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpwindowId", APRMConstants.PURCHASE_INVOICE_WINDOW_ID);
    requestMap.put("context", context.toString());
    
    when(mockOBDal.get(Window.class, APRMConstants.PURCHASE_INVOICE_WINDOW_ID)).thenReturn(mockWindow);
    
    mockedPreferences.when(() -> Preferences.getPreferenceValue("NotAllowChangeExchange", true, 
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), 
        mockOBContext.getUser(), mockOBContext.getRole(), mockWindow))
        .thenThrow(new PropertyNotFoundException());
    
    // When
    boolean result = classUnderTest.getConvertedAmountReadOnlyLogic(requestMap);
    
    // Then
    assertFalse("Converted amount should not be read-only when property is not found", result);
  }

  @Test
  public void testGetConvertedAmountReadOnlyLogic_PropertyException() throws JSONException, PropertyNotFoundException, PropertyException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpwindowId", APRMConstants.PURCHASE_INVOICE_WINDOW_ID);
    requestMap.put("context", context.toString());
    
    when(mockOBDal.get(Window.class, APRMConstants.PURCHASE_INVOICE_WINDOW_ID)).thenReturn(mockWindow);
    
    mockedPreferences.when(() -> Preferences.getPreferenceValue("NotAllowChangeExchange", true, 
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), 
        mockOBContext.getUser(), mockOBContext.getRole(), mockWindow))
        .thenThrow(new PropertyException());
    
    // When
    boolean result = classUnderTest.getConvertedAmountReadOnlyLogic(requestMap);
    
    // Then
    assertFalse("Converted amount should not be read-only when property exception occurs", result);
  }

  @Test
  public void testGetConversionRateReadOnlyLogic_NullContext() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    boolean result = classUnderTest.getConversionRateReadOnlyLogic(requestMap);
    
    // Then
    assertFalse("Conversion rate should not be read-only when context is null", result);
  }

  @Test
  public void testGetConversionRateReadOnlyLogic_PreferenceYes() throws JSONException, PropertyNotFoundException, PropertyException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpwindowId", APRMConstants.PURCHASE_INVOICE_WINDOW_ID);
    requestMap.put("context", context.toString());
    
    when(mockOBDal.get(Window.class, APRMConstants.PURCHASE_INVOICE_WINDOW_ID)).thenReturn(mockWindow);
    
    mockedPreferences.when(() -> Preferences.getPreferenceValue("NotAllowChangeExchange", true, 
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), 
        mockOBContext.getUser(), mockOBContext.getRole(), mockWindow))
        .thenReturn(Preferences.YES);
    
    // When
    boolean result = classUnderTest.getConversionRateReadOnlyLogic(requestMap);
    
    // Then
    assertTrue("Conversion rate should be read-only when preference is YES", result);
  }

  @Test
  public void testGetConversionRateReadOnlyLogic_PreferenceNo() throws JSONException, PropertyNotFoundException, PropertyException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpwindowId", APRMConstants.PURCHASE_INVOICE_WINDOW_ID);
    requestMap.put("context", context.toString());
    
    when(mockOBDal.get(Window.class, APRMConstants.PURCHASE_INVOICE_WINDOW_ID)).thenReturn(mockWindow);
    
    mockedPreferences.when(() -> Preferences.getPreferenceValue("NotAllowChangeExchange", true, 
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), 
        mockOBContext.getUser(), mockOBContext.getRole(), mockWindow))
        .thenReturn(Preferences.NO);
    
    // When
    boolean result = classUnderTest.getConversionRateReadOnlyLogic(requestMap);
    
    // Then
    assertFalse("Conversion rate should not be read-only when preference is NO", result);
  }

  @Test
  public void testGetConversionRateReadOnlyLogic_PropertyNotFoundException() throws JSONException, PropertyNotFoundException, PropertyException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpwindowId", APRMConstants.PURCHASE_INVOICE_WINDOW_ID);
    requestMap.put("context", context.toString());
    
    when(mockOBDal.get(Window.class, APRMConstants.PURCHASE_INVOICE_WINDOW_ID)).thenReturn(mockWindow);
    
    mockedPreferences.when(() -> Preferences.getPreferenceValue("NotAllowChangeExchange", true, 
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), 
        mockOBContext.getUser(), mockOBContext.getRole(), mockWindow))
        .thenThrow(new PropertyNotFoundException());
    
    // When
    boolean result = classUnderTest.getConversionRateReadOnlyLogic(requestMap);
    
    // Then
    assertFalse("Conversion rate should not be read-only when property is not found", result);
  }

  @Test
  public void testGetConversionRateReadOnlyLogic_PropertyException() throws JSONException, PropertyNotFoundException, PropertyException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpwindowId", APRMConstants.PURCHASE_INVOICE_WINDOW_ID);
    requestMap.put("context", context.toString());
    
    when(mockOBDal.get(Window.class, APRMConstants.PURCHASE_INVOICE_WINDOW_ID)).thenReturn(mockWindow);
    
    mockedPreferences.when(() -> Preferences.getPreferenceValue("NotAllowChangeExchange", true, 
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), 
        mockOBContext.getUser(), mockOBContext.getRole(), mockWindow))
        .thenThrow(new PropertyException());
    
    // When
    boolean result = classUnderTest.getConversionRateReadOnlyLogic(requestMap);
    
    // Then
    assertFalse("Conversion rate should not be read-only when property exception occurs", result);
  }
}