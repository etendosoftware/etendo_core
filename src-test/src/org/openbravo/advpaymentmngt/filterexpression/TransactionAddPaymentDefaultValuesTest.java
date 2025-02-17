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
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;

import org.openbravo.service.json.JsonUtils;

/**
 * Test class for TransactionAddPaymentDefaultValues
 */
public class TransactionAddPaymentDefaultValuesTest extends WeldBaseTest {

  private TransactionAddPaymentDefaultValues classUnderTest;
  private AutoCloseable mocks;
  
  @Mock
  private OBDal obDal;
  
  @Mock
  private FIN_FinancialAccount financialAccount;
  
  @Mock
  private Currency currency;
  
  @Mock
  private Organization organization;
  


  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBDateUtils> mockedOBDateUtils;
  private MockedStatic<JsonUtils> mockedJsonUtils;
  
  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    classUnderTest = new TransactionAddPaymentDefaultValues();
    
    // Setup static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
    
    mockedOBDateUtils = mockStatic(OBDateUtils.class);
    mockedJsonUtils = mockStatic(JsonUtils.class);
    
    // Setup common mocks
    when(currency.getStandardPrecision()).thenReturn(2L);
    when(currency.getId()).thenReturn("USD");
    when(financialAccount.getCurrency()).thenReturn(currency);
    when(financialAccount.getOrganization()).thenReturn(organization);
    when(organization.getId()).thenReturn("0");
    when(obDal.get(FIN_FinancialAccount.class, "TEST_FIN_ACCOUNT")).thenReturn(financialAccount);
  }
  
  @After
  public void tearDown() throws Exception {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBDateUtils != null) {
      mockedOBDateUtils.close();
    }
    if (mockedJsonUtils != null) {
      mockedJsonUtils.close();
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
    assertEquals(100L, result);
  }
  
  @Test
  public void testGetDefaultExpectedAmount() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    String result = classUnderTest.getDefaultExpectedAmount(requestMap);
    
    // Then
    assertEquals("0", result);
  }

  @Test
  public void testGetDefaultActualAmountSOTrxWithDepositAndWithdrawal() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("depositamt", "100.00");
    context.put("withdrawalamt", "20.00");
    context.put("trxtype", "BPD");
    requestMap.put("context", context.toString());

    // When
    String result = classUnderTest.getDefaultActualAmount(requestMap);

    // Then
    assertEquals("80.00", result);
  }

  @Test
  public void testGetDefaultActualAmountSOTrxWithInpDepositAndPayment() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpdepositamt", "100.00");
    context.put("inppaymentamt", "20.00");
    context.put("trxtype", "BPD");
    requestMap.put("context", context.toString());

    // When
    String result = classUnderTest.getDefaultActualAmount(requestMap);

    // Then
    assertEquals("80.00", result);
  }
  
  @Test
  public void testGetDefaultActualAmountNotSOTrx() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("trxtype", "BPW");
    requestMap.put("context", context.toString());
    
    // When
    String result = classUnderTest.getDefaultActualAmount(requestMap);
    
    // Then
    assertEquals("0", result);
  }
  
  @Test
  public void testConvertToSafeDecimalStringEuropeanFormat() throws Exception {
    // Given
    String europeanFormat = "1.234,56";
    
    // When
    // We need to use reflection to access the private method
    java.lang.reflect.Method method = TransactionAddPaymentDefaultValues.class.getDeclaredMethod(
        "convertToSafeDecimalString", String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(classUnderTest, europeanFormat);
    
    // Then
    assertEquals("1234.56", result);
  }
  
  @Test
  public void testConvertToSafeDecimalStringAngloSaxonFormat() throws Exception {
    // Given
    String angloSaxonFormat = "1,234.56";
    
    // When
    // We need to use reflection to access the private method
    java.lang.reflect.Method method = TransactionAddPaymentDefaultValues.class.getDeclaredMethod(
        "convertToSafeDecimalString", String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(classUnderTest, angloSaxonFormat);
    
    // Then
    assertEquals("1234.56", result);
  }
  
  @Test
  public void testConvertToSafeDecimalStringScientificNotation() throws Exception {
    // Given
    String scientificNotation = "1.214859023E7";
    
    // When
    // We need to use reflection to access the private method
    java.lang.reflect.Method method = TransactionAddPaymentDefaultValues.class.getDeclaredMethod(
        "convertToSafeDecimalString", String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(classUnderTest, scientificNotation);
    
    // Then
    assertEquals("1.214859023E7", result);
  }
  
  @Test
  public void testGetDefaultIsSOTrxBPD() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("trxtype", "BPD");
    requestMap.put("context", context.toString());
    
    // When
    String result = classUnderTest.getDefaultIsSOTrx(requestMap);
    
    // Then
    assertEquals("Y", result);
  }
  
  @Test
  public void testGetDefaultIsSOTrxBPW() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("trxtype", "BPW");
    requestMap.put("context", context.toString());
    
    // When
    String result = classUnderTest.getDefaultIsSOTrx(requestMap);
    
    // Then
    assertEquals("N", result);
  }
  
  @Test
  public void testGetDefaultIsSOTrxOther() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("trxtype", "OTHER");
    requestMap.put("context", context.toString());
    
    // When
    String result = classUnderTest.getDefaultIsSOTrx(requestMap);
    
    // Then
    assertEquals("", result);
  }
  
  @Test
  public void testGetDefaultTransactionType() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    String result = classUnderTest.getDefaultTransactionType(requestMap);
    
    // Then
    assertEquals("I", result);
  }
  
  @Test
  public void testGetDefaultPaymentType() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    String result = classUnderTest.getDefaultPaymentType(requestMap);
    
    // Then
    assertEquals("", result);
  }
  
  @Test
  public void testGetDefaultOrderType() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    String result = classUnderTest.getDefaultOrderType(requestMap);
    
    // Then
    assertEquals("", result);
  }
  
  @Test
  public void testGetDefaultInvoiceType() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    String result = classUnderTest.getDefaultInvoiceType(requestMap);
    
    // Then
    assertEquals("", result);
  }
  
  @Test
  public void testGetDefaultConversionRate() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    String result = classUnderTest.getDefaultConversionRate(requestMap);
    
    // Then
    assertEquals("1", result);
  }
  
  @Test
  public void testGetDefaultConvertedAmount() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    
    // When
    String result = classUnderTest.getDefaultConvertedAmount(requestMap);
    
    // Then
    assertEquals("0", result);
  }
  
  @Test
  public void testGetDefaultReceivedFromWithBPartner() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("c_bpartner_id", "TEST_BPARTNER");
    requestMap.put("context", context.toString());
    
    // When
    String result = classUnderTest.getDefaultReceivedFrom(requestMap);
    
    // Then
    assertEquals("TEST_BPARTNER", result);
  }
  
  @Test
  public void testGetDefaultReceivedFromWithInpBPartnerId() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpcBpartnerId", "TEST_BPARTNER_INP");
    requestMap.put("context", context.toString());
    
    // When
    String result = classUnderTest.getDefaultReceivedFrom(requestMap);
    
    // Then
    assertEquals("TEST_BPARTNER_INP", result);
  }
  
  @Test
  public void testGetDefaultReceivedFromNoBPartner() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    requestMap.put("context", context.toString());
    
    // When
    String result = classUnderTest.getDefaultReceivedFrom(requestMap);
    
    // Then
    assertEquals("", result);
  }
  
  @Test
  public void testGetDefaultStandardPrecision() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("Fin_Financial_Account_ID", "TEST_FIN_ACCOUNT");
    requestMap.put("context", context.toString());
    
    // When
    String result = classUnderTest.getDefaultStandardPrecision(requestMap);
    
    // Then
    assertEquals("2", result);
  }
  
  @Test
  public void testGetDefaultCurrency() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("Fin_Financial_Account_ID", "TEST_FIN_ACCOUNT");
    requestMap.put("context", context.toString());
    
    // When
    String result = classUnderTest.getDefaultCurrency(requestMap);
    
    // Then
    assertEquals("USD", result);
  }
  
  @Test
  public void testGetOrganizationFromContext() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("ad_org_id", "TEST_ORG");
    requestMap.put("context", context.toString());
    
    // When
    String result = classUnderTest.getOrganization(requestMap);
    
    // Then
    assertEquals("TEST_ORG", result);
  }
  
  @Test
  public void testGetOrganizationFromInpOrgId() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpadOrgId", "TEST_ORG_INP");
    requestMap.put("context", context.toString());
    
    // When
    String result = classUnderTest.getOrganization(requestMap);
    
    // Then
    assertEquals("TEST_ORG_INP", result);
  }
  
  @Test
  public void testGetOrganizationFromFinancialAccount() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("Fin_Financial_Account_ID", "TEST_FIN_ACCOUNT");
    requestMap.put("context", context.toString());
    
    // When
    String result = classUnderTest.getOrganization(requestMap);
    
    // Then
    assertEquals("0", result);
  }

  @Test
  public void testGetDefaultPaymentMethodNoSuitableMethod() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("trxtype", "BPD");
    context.put("Fin_Financial_Account_ID", "TEST_FIN_ACCOUNT");
    requestMap.put("context", context.toString());
    
    // Setup payment methods with no suitable method
    List<FinAccPaymentMethod> paymentMethods = new ArrayList<>();
    FinAccPaymentMethod unsuitable = mock(FinAccPaymentMethod.class);
    when(unsuitable.isActive()).thenReturn(true);
    when(unsuitable.isDefault()).thenReturn(false);
    when(unsuitable.isPayinAllow()).thenReturn(false);
    paymentMethods.add(unsuitable);
    
    when(financialAccount.getFinancialMgmtFinAccPaymentMethodList()).thenReturn(paymentMethods);
    
    // When
    String result = classUnderTest.getDefaultPaymentMethod(requestMap);
    
    // Then
    assertEquals("", result);
  }
  
  @Test
  public void testGetDefaultDocument_BPD() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("trxtype", "BPD");
    requestMap.put("context", context.toString());
    
    // When
    String result = classUnderTest.getDefaultDocument(requestMap);
    
    // Then
    assertEquals("RCIN", result);
  }
  
  @Test
  public void testGetDefaultDocument_BPW() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("trxtype", "BPW");
    requestMap.put("context", context.toString());
    
    // When
    String result = classUnderTest.getDefaultDocument(requestMap);
    
    // Then
    assertEquals("PDOUT", result);
  }
  
  @Test
  public void testGetDefaultDocument_Other() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("trxtype", "OTHER");
    requestMap.put("context", context.toString());
    
    // When
    String result = classUnderTest.getDefaultDocument(requestMap);
    
    // Then
    assertEquals("", result);
  }
  
  @Test
  public void testGetDefaultPaymentDateFromTrxDate() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("trxdate", "2023-01-15");
    requestMap.put("context", context.toString());
    
    // Setup date formatting
    Date testDate = new Date(123, 0, 15);
    mockedJsonUtils.when(JsonUtils::createDateFormat).thenReturn(new java.text.SimpleDateFormat("yyyy-MM-dd"));
    mockedOBDateUtils.when(() -> OBDateUtils.formatDate(testDate)).thenReturn("15-01-2023");
    
    // When
    String result = classUnderTest.getDefaultPaymentDate(requestMap);
    
    // Then
    assertEquals("15-01-2023", result);
  }
  
  @Test
  public void testGetDefaultPaymentDateFromStatementDate() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpstatementdate", "15-01-2023");
    requestMap.put("context", context.toString());
    
    // Setup date formatting
    Date testDate = new Date(123, 0, 15);
    mockedOBDateUtils.when(() -> OBDateUtils.getDate("15-01-2023")).thenReturn(testDate);
    mockedOBDateUtils.when(() -> OBDateUtils.formatDate(testDate)).thenReturn("15-01-2023");
    
    // When
    String result = classUnderTest.getDefaultPaymentDate(requestMap);
    
    // Then
    assertEquals("15-01-2023", result);
  }

  @Test
  public void testGetDefaultPaymentDateCurrentDate() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    requestMap.put("context", context.toString());

    mockedOBDateUtils.when(() -> OBDateUtils.formatDate(org.mockito.ArgumentMatchers.any(Date.class)))
        .thenReturn("15-01-2023");

    // When
    String result = classUnderTest.getDefaultPaymentDate(requestMap);

    // Then
    assertEquals("15-01-2023", result);
  }
  
  @Test
  public void testGetBankStatementLineAmountWithDepositAndWithdrawal() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("depositamt", "100.00");
    context.put("withdrawalamt", "20.00");
    requestMap.put("context", context.toString());
    
    // When
    String result = classUnderTest.getBankStatementLineAmount(requestMap);
    
    // Then
    assertEquals("80.00", result);
  }
  
  @Test
  public void testGetBankStatementLineAmountWithInpDepositAndPayment() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpdepositamt", "100.00");
    context.put("inppaymentamt", "20.00");
    requestMap.put("context", context.toString());
    
    // When
    String result = classUnderTest.getBankStatementLineAmount(requestMap);
    
    // Then
    assertEquals("80.00", result);
  }
  
  @Test
  public void testGetBankStatementLineAmountNoAmounts() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    requestMap.put("context", context.toString());
    
    // When
    String result = classUnderTest.getBankStatementLineAmount(requestMap);
    
    // Then
    assertEquals("0", result);
  }
  
  @Test
  public void testGetFinancialAccount() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("Fin_Financial_Account_ID", "TEST_FIN_ACCOUNT");
    requestMap.put("context", context.toString());
    
    // When
    // We need to use reflection to access the private method
    java.lang.reflect.Method method = TransactionAddPaymentDefaultValues.class.getDeclaredMethod(
        "getFinancialAccount", Map.class);
    method.setAccessible(true);
    FIN_FinancialAccount result = (FIN_FinancialAccount) method.invoke(classUnderTest, requestMap);
    
    // Then
    assertNotNull(result);
    assertEquals(financialAccount, result);
  }
  
  @Test
  public void testGetFinancialAccountFromInpFinFinancialAccountId() throws Exception {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpfinFinancialAccountId", "TEST_FIN_ACCOUNT");
    requestMap.put("context", context.toString());
    
    // When
    // We need to use reflection to access the private method
    java.lang.reflect.Method method = TransactionAddPaymentDefaultValues.class.getDeclaredMethod(
        "getFinancialAccount", Map.class);
    method.setAccessible(true);
    FIN_FinancialAccount result = (FIN_FinancialAccount) method.invoke(classUnderTest, requestMap);
    
    // Then
    assertNotNull(result);
    assertEquals(financialAccount, result);
  }
}