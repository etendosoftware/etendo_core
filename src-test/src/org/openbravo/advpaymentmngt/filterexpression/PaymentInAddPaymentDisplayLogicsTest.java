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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

/**
 * Test for PaymentInAddPaymentDisplayLogics class
 */
public class PaymentInAddPaymentDisplayLogicsTest {

  private PaymentInAddPaymentDisplayLogics displayLogics;
  private AutoCloseable mocks;
  
  // Static mocks
  private MockedStatic<OBDal> mockedOBDal;

  private MockedConstruction<AdvPaymentMngtDao> mockedAdvPaymentMngtDao;

  // Mocks
  @Mock
  private OBDal mockOBDal;
  @Mock
  private FIN_Payment mockPayment;
  @Mock
  private BusinessPartner mockBusinessPartner;
  @Mock
  private Organization mockOrganization;
  @Mock
  private Currency mockCurrency;


  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    displayLogics = new PaymentInAddPaymentDisplayLogics();
    
    // Setup static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    
    // Setup mock payment
    when(mockPayment.getBusinessPartner()).thenReturn(mockBusinessPartner);
    when(mockPayment.getOrganization()).thenReturn(mockOrganization);
    when(mockPayment.getCurrency()).thenReturn(mockCurrency);
  }

  @After
  public void tearDown() throws Exception {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedAdvPaymentMngtDao != null) {
      mockedAdvPaymentMngtDao.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  @Test
  public void testGetSeq() {
    // When
    long sequence = displayLogics.getSeq();
    
    // Then
    assertEquals("Sequence should be 100", 100L, sequence);
  }

  @Test
  public void testGetOrganizationDisplayLogic() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    boolean result = displayLogics.getOrganizationDisplayLogic(requestMap);
    
    // Then
    assertFalse("Organization display logic should be false", result);
  }

  @Test
  public void testGetDocumentDisplayLogic() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    boolean result = displayLogics.getDocumentDisplayLogic(requestMap);
    
    // Then
    assertFalse("Document display logic should be false", result);
  }


  @Test
  public void testGetCreditToUseDisplayLogic_WithPositiveCredit() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpfinPaymentId", "TEST_PAYMENT_ID");
    requestMap.put("context", context.toString());

    // Setup mocks
    when(mockOBDal.get(FIN_Payment.class, "TEST_PAYMENT_ID")).thenReturn(mockPayment);

    mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class,
        (mock, context2) -> {
          when(mock.getCustomerCredit(any(BusinessPartner.class), eq(true), any(Organization.class), any(Currency.class)))
              .thenReturn(new BigDecimal("100.00"));
        });

    // When
    boolean result = displayLogics.getCreditToUseDisplayLogic(requestMap);

    // Then
    assertTrue("Credit to use display logic should be true when customer credit > 0", result);
  }

  @Test
  public void testGetCreditToUseDisplayLogic_WithZeroCredit() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpfinPaymentId", "TEST_PAYMENT_ID");
    requestMap.put("context", context.toString());

    // Setup mocks
    when(mockOBDal.get(FIN_Payment.class, "TEST_PAYMENT_ID")).thenReturn(mockPayment);

    mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class,
        (mock, context2) -> {
          when(mock.getCustomerCredit(any(BusinessPartner.class), eq(true), any(Organization.class), any(Currency.class)))
              .thenReturn(BigDecimal.ZERO);
        });

    // When
    boolean result = displayLogics.getCreditToUseDisplayLogic(requestMap);

    // Then
    assertFalse("Credit to use display logic should be false when customer credit = 0", result);
  }

  @Test
  public void testGetCreditToUseDisplayLogic_WithNullBusinessPartner() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpfinPaymentId", "TEST_PAYMENT_ID");
    requestMap.put("context", context.toString());
    
    // Setup mocks
    when(mockOBDal.get(FIN_Payment.class, "TEST_PAYMENT_ID")).thenReturn(mockPayment);
    when(mockPayment.getBusinessPartner()).thenReturn(null);
    
    // When
    boolean result = displayLogics.getCreditToUseDisplayLogic(requestMap);
    
    // Then
    assertFalse("Credit to use display logic should be false when business partner is null", result);
  }

  @Test
  public void testGetCreditToUseDisplayLogic_WithAlternativePaymentId() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("Fin_Payment_ID", "TEST_PAYMENT_ID");
    requestMap.put("context", context.toString());

    // Setup mocks
    when(mockOBDal.get(FIN_Payment.class, "TEST_PAYMENT_ID")).thenReturn(mockPayment);

    mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class,
        (mock, context2) -> {
          when(mock.getCustomerCredit(any(BusinessPartner.class), eq(true), any(Organization.class), any(Currency.class)))
              .thenReturn(new BigDecimal("100.00"));
        });

    // When
    boolean result = displayLogics.getCreditToUseDisplayLogic(requestMap);

    // Then
    assertTrue("Credit to use display logic should be true when using Fin_Payment_ID", result);
  }

  @Test
  public void testGetBankStatementLineDisplayLogic() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    boolean result = displayLogics.getBankStatementLineDisplayLogic(requestMap);
    
    // Then
    assertFalse("Bank statement line display logic should be false", result);
  }

  @Test
  public void testGetOverpaymentActionDisplayLogic_WithPositiveDifference() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("difference", 50.0);
    requestMap.put("context", context.toString());
    
    // When
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);
    
    // Then
    assertTrue("Overpayment action display logic should be true when difference > 0", result);
  }

  @Test
  public void testGetOverpaymentActionDisplayLogic_WithZeroDifference() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("difference", 0.0);
    requestMap.put("context", context.toString());
    
    // When
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);
    
    // Then
    assertFalse("Overpayment action display logic should be false when difference = 0", result);
  }

  @Test
  public void testGetOverpaymentActionDisplayLogic_WithNegativeDifference() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("difference", -10.0);
    requestMap.put("context", context.toString());
    
    // When
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);
    
    // Then
    assertFalse("Overpayment action display logic should be false when difference < 0", result);
  }

  @Test
  public void testGetOverpaymentActionDisplayLogic_WithNullContext() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);
    
    // Then
    assertFalse("Overpayment action display logic should be false when context is null", result);
  }

  @Test
  public void testGetOverpaymentActionDisplayLogic_WithoutDifference() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    requestMap.put("context", context.toString());
    
    // When
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);
    
    // Then
    assertFalse("Overpayment action display logic should be false when difference is not present", result);
  }

}
