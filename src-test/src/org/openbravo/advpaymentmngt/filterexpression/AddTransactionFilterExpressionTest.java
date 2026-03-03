package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.openbravo.test.base.mock.MockitoStaticMockUtils.mockStaticSafely;
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
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;

/**
 * Unit tests for the AddTransactionFilterExpression class.
 */
public class AddTransactionFilterExpressionTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
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

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    filterExpression = new AddTransactionFilterExpression();
    requestMap = new HashMap<>();

    try {
      JSONObject context = new JSONObject();
      context.put(TestConstants.BANK_STATEMENT_LINE, TestConstants.TEST_LINE_ID);
      requestMap.put(TestConstants.CONTEXT, context.toString());

      setPrivateField(filterExpression, TestConstants.REQUEST_MAP, requestMap);
    } catch (JSONException e) {
      throw new RuntimeException("Error initializing context in setUp", e);
    }
  }

  /**
   * Tests the getDefaultDocumentWithdrawalAmount method.
   *
   * @throws JSONException if a JSON error occurs
   */
  @Test
  public void testGetDefaultDocumentWithdrawalAmount() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStaticSafely(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getDramount()).thenReturn(BigDecimal.TEN);

      JSONObject context = new JSONObject();
      context.put(TestConstants.BANK_STATEMENT_LINE, TestConstants.TEST_LINE_ID);
      requestMap.put(TestConstants.CONTEXT, context.toString());

      // When
      String result = filterExpression.getDefaultDocument(requestMap);

      // Then
      assertEquals("BPW", result);
    }
  }

  /**
   * Tests the getDefaultDocumentDepositAmount method.
   *
   * @throws JSONException if a JSON error occurs
   */
  @Test
  public void testGetDefaultDocumentDepositAmount() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStaticSafely(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getDramount()).thenReturn(BigDecimal.ZERO);

      JSONObject context = new JSONObject();
      context.put(TestConstants.BANK_STATEMENT_LINE, TestConstants.TEST_LINE_ID);
      requestMap.put(TestConstants.CONTEXT, context.toString());

      // When
      String result = filterExpression.getDefaultDocument(requestMap);

      // Then
      assertEquals("BPD", result);
    }
  }

  /**
   * Tests the getDefaultCurrency method.
   *
   * @throws JSONException if a JSON error occurs
   */
  @Test
  public void testGetDefaultCurrency() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStaticSafely(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_FinancialAccount.class), anyString()))
          .thenReturn(mockFinancialAccount);
      when(mockFinancialAccount.getCurrency()).thenReturn(mockCurrency);
      when(mockCurrency.getId()).thenReturn("currencyId");

      JSONObject context = new JSONObject();
      context.put("inpfinFinancialAccountId", "finAccountId");
      requestMap.put(TestConstants.CONTEXT, context.toString());

      // When
      String result = filterExpression.getDefaultCurrency(requestMap);

      // Then
      assertEquals("currencyId", result);
    }
  }

  /**
   * Tests the getOrganization method.
   *
   * @throws JSONException if a JSON error occurs
   */
  @Test
  public void testGetOrganization() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStaticSafely(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_FinancialAccount.class), anyString()))
          .thenReturn(mockFinancialAccount);
      when(mockFinancialAccount.getOrganization()).thenReturn(mockOrganization);
      when(mockOrganization.getId()).thenReturn("orgId");

      JSONObject context = new JSONObject();
      context.put("inpfinFinancialAccountId", "finAccountId");
      requestMap.put(TestConstants.CONTEXT, context.toString());

      // When
      String result = filterExpression.getOrganization(requestMap);

      // Then
      assertEquals("orgId", result);
    }
  }

  /**
   * Tests the getDefaulDocumentCategory method.
   */
  @Test
  public void testGetDefaultDocumentCategory() {
    // When
    String result = filterExpression.getDefaulDocumentCategory();

    // Then
    assertEquals("FAT", result);
  }

  /**
   * Tests the getDefaultDescriptionOnlyDescription method.
   *
   * @throws JSONException if a JSON error occurs
   */
  @Test
  public void testGetDefaultDescriptionOnlyDescription() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStaticSafely(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getBpartnername()).thenReturn("");
      when(mockBankStatementLine.getDescription()).thenReturn("Test Description");

      JSONObject context = new JSONObject();
      context.put(TestConstants.BANK_STATEMENT_LINE, TestConstants.TEST_LINE_ID);
      requestMap.put(TestConstants.CONTEXT, context.toString());

      setPrivateField(filterExpression, TestConstants.REQUEST_MAP, requestMap);

      // When
      String result = filterExpression.getDefaulDescription();

      // Then
      assertEquals("Test Description", result);
    }
  }

  /**
   * Tests the getDefaultBusinessPartnerNoBusinessPartner method.
   *
   * @throws JSONException if a JSON error occurs
   */
  @Test
  public void testGetDefaultBusinessPartnerNoBusinessPartner() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStaticSafely(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getBusinessPartner()).thenReturn(null);

      JSONObject context = new JSONObject();
      context.put(TestConstants.BANK_STATEMENT_LINE, TestConstants.TEST_LINE_ID);
      requestMap.put(TestConstants.CONTEXT, context.toString());

      setPrivateField(filterExpression, TestConstants.REQUEST_MAP, requestMap);

      // When
      String result = filterExpression.getDefaulBusinessPartner();

      // Then
      assertNull(result);
    }
  }

  /**
   * Tests the getDefaultGLItemNoGLItem method.
   *
   * @throws JSONException if a JSON error occurs
   */
  @Test
  public void testGetDefaultGLItemNoGLItem() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStaticSafely(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getGLItem()).thenReturn(null);

      JSONObject context = new JSONObject();
      context.put(TestConstants.BANK_STATEMENT_LINE, TestConstants.TEST_LINE_ID);
      requestMap.put(TestConstants.CONTEXT, context.toString());

      setPrivateField(filterExpression, TestConstants.REQUEST_MAP, requestMap);

      // When
      String result = filterExpression.getDefaulGLItem();

      // Then
      assertNull(result);
    }
  }

  /**
   * Tests the getDefaultTransactionDate method.
   *
   * @throws JSONException if a JSON error occurs
   */
  @Test
  public void testGetDefaultTransactionDate() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStaticSafely(OBDal.class);
         MockedStatic<OBDateUtils> dateUtilsMock = mockStatic(OBDateUtils.class)) {
      // Given
      Date testDate = new Date();
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getTransactionDate()).thenReturn(testDate);
      dateUtilsMock.when(() -> OBDateUtils.formatDate(testDate)).thenReturn("2023-05-15");

      JSONObject context = new JSONObject();
      context.put(TestConstants.BANK_STATEMENT_LINE, TestConstants.TEST_LINE_ID);
      requestMap.put(TestConstants.CONTEXT, context.toString());

      setPrivateField(filterExpression, TestConstants.REQUEST_MAP, requestMap);

      // When
      String result = filterExpression.getDefaultTransactionDate();

      // Then
      assertEquals("2023-05-15", result);
    }
  }

  /**
   * Tests the getDefaultDepositAmountWithDramount method.
   *
   * @throws JSONException if a JSON error occurs
   */
  @Test
  public void testGetDefaultDepositAmountWithDramount() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStaticSafely(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getDramount()).thenReturn(new BigDecimal("100.00"));

      JSONObject context = new JSONObject();
      context.put(TestConstants.BANK_STATEMENT_LINE, TestConstants.TEST_LINE_ID);
      requestMap.put(TestConstants.CONTEXT, context.toString());
      setPrivateField(filterExpression, TestConstants.REQUEST_MAP, requestMap);

      // When
      String result = filterExpression.getDefaultDepositAmout();

      // Then
      assertEquals("0.00", result);
    }
  }

  /**
   * Tests the getDefaultDepositAmountWithCramount method.
   *
   * @throws JSONException if a JSON error occurs
   */
  @Test
  public void testGetDefaultDepositAmountWithCramount() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStaticSafely(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getDramount()).thenReturn(BigDecimal.ZERO);
      when(mockBankStatementLine.getCramount()).thenReturn(new BigDecimal("50.00"));

      JSONObject context = new JSONObject();
      context.put(TestConstants.BANK_STATEMENT_LINE, TestConstants.TEST_LINE_ID);
      requestMap.put(TestConstants.CONTEXT, context.toString());
      setPrivateField(filterExpression, TestConstants.REQUEST_MAP, requestMap);

      // When
      String result = filterExpression.getDefaultDepositAmout();

      // Then
      assertEquals("50.00", result);
    }
  }

  /**
   * Tests the getDefaultWithdrawalAmountWithDramount method.
   *
   * @throws JSONException if a JSON error occurs
   */
  @Test
  public void testGetDefaultWithdrawalAmountWithDramount() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStaticSafely(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getDramount()).thenReturn(new BigDecimal("75.00"));

      JSONObject context = new JSONObject();
      context.put(TestConstants.BANK_STATEMENT_LINE, TestConstants.TEST_LINE_ID);
      requestMap.put(TestConstants.CONTEXT, context.toString());
      setPrivateField(filterExpression, TestConstants.REQUEST_MAP, requestMap);

      // When
      String result = filterExpression.getDefaulWithdrawalAmount();

      // Then
      assertEquals("75.00", result);
    }
  }

  /**
   * Tests the getDefaultWithdrawalAmountWithZeroDramount method.
   *
   * @throws JSONException if a JSON error occurs
   */
  @Test
  public void testGetDefaultWithdrawalAmountWithZeroDramount() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStaticSafely(OBDal.class)) {
      // Given
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString()))
          .thenReturn(mockBankStatementLine);
      when(mockBankStatementLine.getDramount()).thenReturn(BigDecimal.ZERO);

      JSONObject context = new JSONObject();
      context.put(TestConstants.BANK_STATEMENT_LINE, TestConstants.TEST_LINE_ID);
      requestMap.put(TestConstants.CONTEXT, context.toString());
      setPrivateField(filterExpression, TestConstants.REQUEST_MAP, requestMap);

      // When
      String result = filterExpression.getDefaulWithdrawalAmount();

      // Then
      assertEquals("0.00", result);
    }
  }

  /**
   * Tests the getExpression method with a valid parameter.
   *
   * @throws JSONException if a JSON error occurs
   */
  @Test
  public void testGetExpressionValidParameter() throws JSONException {
    try (MockedStatic<OBDal> obDalMock = mockStaticSafely(OBDal.class)) {
      // Given
      Map<String, String> testMap = new HashMap<>();
      JSONObject context = new JSONObject();
      context.put(TestConstants.BANK_STATEMENT_LINE, TestConstants.TEST_LINE_ID);
      testMap.put(TestConstants.CONTEXT, context.toString());
      testMap.put("currentParam", "DOCBASETYPE");

      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);

      // When
      String result = filterExpression.getExpression(testMap);

      // Then
      assertEquals("FAT", result);
    }
  }

  /**
   * Tests the Parameters.getParameter method.
   *
   * @throws Exception if an error occurs during reflection
   */
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

  /**
   * Tests the Parameters.getParameter method with an invalid column.
   *
   * @throws Exception if an error occurs during reflection
   */
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

  /**
   * Tests the Parameters.getColumnName method.
   *
   * @throws Exception if an error occurs during reflection
   */
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

  /**
   * Sets a private field using reflection.
   *
   * @param target the target object
   * @param fieldName the name of the field
   * @param value the value to set
   */
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
