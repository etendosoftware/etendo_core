package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.test.base.OBBaseTest;

public class AddTransactionFilterExpressionTest extends OBBaseTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AddTransactionFilterExpression filterExpression;
  private Map<String, String> requestMap;

  @Mock
  private FIN_BankStatementLine mockBankStatementLine;
  @Mock
  private FIN_FinancialAccount mockFinancialAccount;
  @Mock
  private Currency mockCurrency;
  @Mock
  private Organization mockOrganization;

  @Mock
  private OBDal mockOBDal;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.openMocks(this);
    filterExpression = new AddTransactionFilterExpression();
    requestMap = new HashMap<>();

    try {
      JSONObject context = new JSONObject();
      context.put("bankStatementLineId", "testLineId");
      requestMap.put("context", context.toString());

      setPrivateField(filterExpression, "requestMap", requestMap);
    } catch (JSONException e) {
      throw new RuntimeException("Error initializing context in setUp", e);
    }
  }

  @Test
  public void testGetDefaultDocumentWithdrawalAmount() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getDramount()).thenReturn(BigDecimal.TEN);

      JSONObject context = new JSONObject();
      context.put("bankStatementLineId", "testLineId");
      requestMap.put("context", context.toString());

      // When
      String result = filterExpression.getDefaultDocument(requestMap);

      // Then
      assertEquals("BPW", result);
    }
  }

  @Test
  public void testGetDefaultDocumentDepositAmount() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getDramount()).thenReturn(BigDecimal.ZERO);

      JSONObject context = new JSONObject();
      context.put("bankStatementLineId", "testLineId");
      requestMap.put("context", context.toString());

      // When
      String result = filterExpression.getDefaultDocument(requestMap);

      // Then
      assertEquals("BPD", result);
    }
  }


  @Test
  public void testGetDefaultCurrency() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_FinancialAccount.class), anyString()))
          .thenReturn(mockFinancialAccount);
      when(mockFinancialAccount.getCurrency()).thenReturn(mockCurrency);
      when(mockCurrency.getId()).thenReturn("currencyId");

      JSONObject context = new JSONObject();
      context.put("inpfinFinancialAccountId", "finAccountId");
      requestMap.put("context", context.toString());

      // When
      String result = filterExpression.getDefaultCurrency(requestMap);

      // Then
      assertEquals("currencyId", result);
    }
  }

  @Test
  public void testGetOrganization() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_FinancialAccount.class), anyString()))
          .thenReturn(mockFinancialAccount);
      when(mockFinancialAccount.getOrganization()).thenReturn(mockOrganization);
      when(mockOrganization.getId()).thenReturn("orgId");

      JSONObject context = new JSONObject();
      context.put("inpfinFinancialAccountId", "finAccountId");
      requestMap.put("context", context.toString());

      // When
      String result = filterExpression.getOrganization(requestMap);

      // Then
      assertEquals("orgId", result);
    }
  }

  @Test
  public void testGetDefaultDocumentCategory() {
    // When
    String result = filterExpression.getDefaulDocumentCategory();

    // Then
    assertEquals("FAT", result);
  }

  @Test
  public void testGetDefaultDescriptionOnlyDescription() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getBpartnername()).thenReturn("");
      when(mockBankStatementLine.getDescription()).thenReturn("Test Description");

      JSONObject context = new JSONObject();
      context.put("bankStatementLineId", "testLineId");
      requestMap.put("context", context.toString());

      setPrivateField(filterExpression, "requestMap", requestMap);

      // When
      String result = filterExpression.getDefaulDescription();

      // Then
      assertEquals("Test Description", result);
    }
  }

  @Test
  public void testGetDefaultBusinessPartnerNoBusinessPartner() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getBusinessPartner()).thenReturn(null);

      JSONObject context = new JSONObject();
      context.put("bankStatementLineId", "testLineId");
      requestMap.put("context", context.toString());

      setPrivateField(filterExpression, "requestMap", requestMap);

      // When
      String result = filterExpression.getDefaulBusinessPartner();

      // Then
      assertNull(result);
    }
  }

  @Test
  public void testGetDefaultGLItemNoGLItem() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getGLItem()).thenReturn(null);

      JSONObject context = new JSONObject();
      context.put("bankStatementLineId", "testLineId");
      requestMap.put("context", context.toString());

      setPrivateField(filterExpression, "requestMap", requestMap);

      // When
      String result = filterExpression.getDefaulGLItem();

      // Then
      assertNull(result);
    }
  }

  @Test
  public void testGetDefaultTransactionDate() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<OBDateUtils> dateUtilsMock = mockStatic(OBDateUtils.class)) {
      // Given
      Date testDate = new Date();
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getTransactionDate()).thenReturn(testDate);
      dateUtilsMock.when(() -> OBDateUtils.formatDate(testDate)).thenReturn("2023-05-15");

      JSONObject context = new JSONObject();
      context.put("bankStatementLineId", "testLineId");
      requestMap.put("context", context.toString());

      setPrivateField(filterExpression, "requestMap", requestMap);

      // When
      String result = filterExpression.getDefaultTransactionDate();

      // Then
      assertEquals("2023-05-15", result);
    }
  }

  @Test
  public void testGetDefaultDepositAmountWithDramount() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getDramount()).thenReturn(new BigDecimal("100.00"));

      JSONObject context = new JSONObject();
      context.put("bankStatementLineId", "testLineId");
      requestMap.put("context", context.toString());
      setPrivateField(filterExpression, "requestMap", requestMap);

      // When
      String result = filterExpression.getDefaultDepositAmout();

      // Then
      assertEquals("0.00", result);
    }
  }

  @Test
  public void testGetDefaultDepositAmountWithCramount() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getDramount()).thenReturn(BigDecimal.ZERO);
      when(mockBankStatementLine.getCramount()).thenReturn(new BigDecimal("50.00"));

      JSONObject context = new JSONObject();
      context.put("bankStatementLineId", "testLineId");
      requestMap.put("context", context.toString());
      setPrivateField(filterExpression, "requestMap", requestMap);

      // When
      String result = filterExpression.getDefaultDepositAmout();

      // Then
      assertEquals("50.00", result);
    }
  }

  @Test
  public void testGetDefaultWithdrawalAmountWithDramount() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getDramount()).thenReturn(new BigDecimal("75.00"));

      JSONObject context = new JSONObject();
      context.put("bankStatementLineId", "testLineId");
      requestMap.put("context", context.toString());
      setPrivateField(filterExpression, "requestMap", requestMap);

      // When
      String result = filterExpression.getDefaulWithdrawalAmount();

      // Then
      assertEquals("75.00", result);
    }
  }

  @Test
  public void testGetDefaultWithdrawalAmountWithZeroDramount() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getDramount()).thenReturn(BigDecimal.ZERO);

      JSONObject context = new JSONObject();
      context.put("bankStatementLineId", "testLineId");
      requestMap.put("context", context.toString());
      setPrivateField(filterExpression, "requestMap", requestMap);

      // When
      String result = filterExpression.getDefaulWithdrawalAmount();

      // Then
      assertEquals("0.00", result);
    }
  }

  @Test
  public void testGetExpressionValidParameter() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      // Given
      Map<String, String> testMap = new HashMap<>();
      JSONObject context = new JSONObject();
      context.put("bankStatementLineId", "testLineId");
      testMap.put("context", context.toString());
      testMap.put("currentParam", "DOCBASETYPE");

      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);

      // When
      String result = filterExpression.getExpression(testMap);

      // Then
      assertEquals("FAT", result);
    }
  }


  @Test
  public void testParametersGetParameter() throws Exception {
    Class<?> parametersClass = Class.forName("org.openbravo.advpaymentmngt.filterexpression.AddTransactionFilterExpression$Parameters");
    Method getParameterMethod = parametersClass.getDeclaredMethod("getParameter", String.class);
    getParameterMethod.setAccessible(true);
    Method getColumnNameMethod = parametersClass.getDeclaredMethod("getColumnName");
    Object[] enumConstants = parametersClass.getEnumConstants();

    for (Object enumConstant : enumConstants) {
      // When
      String columnName = (String) getColumnNameMethod.invoke(enumConstant);
      Object result = getParameterMethod.invoke(null, columnName);

      // Then
      assertEquals("Error getting parameter " + enumConstant,
          enumConstant, result);
    }
  }

  @Test
  public void testParametersGetParameterInvalidColumn() throws Exception {
    Class<?> parametersClass = Class.forName("org.openbravo.advpaymentmngt.filterexpression.AddTransactionFilterExpression$Parameters");
    Method getParameterMethod = parametersClass.getDeclaredMethod("getParameter", String.class);
    getParameterMethod.setAccessible(true);

    // When
    Object result = getParameterMethod.invoke(null, "invalid_column");

    // Then
    assertNull(result);
  }

  @Test
  public void testParametersGetColumnName() throws Exception {
    Class<?> parametersClass = Class.forName("org.openbravo.advpaymentmngt.filterexpression.AddTransactionFilterExpression$Parameters");
    Method getColumnNameMethod = parametersClass.getDeclaredMethod("getColumnName");

    Map<String, String> expectedColumnNames = new HashMap<>();
    expectedColumnNames.put("TransactionType", "trxtype");
    expectedColumnNames.put("Currency", "c_currency_id");
    expectedColumnNames.put("Organization", "ad_org_id");
    expectedColumnNames.put("DocumentCategory", "DOCBASETYPE");

    for (Object enumConstant : parametersClass.getEnumConstants()) {
      String enumName = ((Enum<?>) enumConstant).name();
      if (expectedColumnNames.containsKey(enumName)) {
        String expectedColumnName = expectedColumnNames.get(enumName);
        String actualColumnName = (String) getColumnNameMethod.invoke(enumConstant);
        assertEquals("Incorrect column for " + enumName,
            expectedColumnName, actualColumnName);
      }
    }
  }

  private void setPrivateField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Error setting private field: " + fieldName, e);
    }
  }

}
