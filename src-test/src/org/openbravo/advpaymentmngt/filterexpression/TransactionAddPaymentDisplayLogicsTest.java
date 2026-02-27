package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.openbravo.test.base.mock.MockitoStaticMockUtils.mockStaticSafely;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Unit tests for {@link TransactionAddPaymentDisplayLogics}
 */
public class TransactionAddPaymentDisplayLogicsTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private OBDal mockOBDal;

  private TransactionAddPaymentDisplayLogics classUnderTest;

  @Mock
  private BusinessPartner mockBusinessPartner;

  @Mock
  private Organization mockOrganization;

  @Mock
  private Currency mockCurrency;

  private MockedStatic<OBDal> mockedOBDal;

  private MockedConstruction<AdvPaymentMngtDao> mockedAdvPaymentMngtDao;

  private AutoCloseable mocks;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);

    classUnderTest = new TransactionAddPaymentDisplayLogics();

    mockedOBDal = mockStaticSafely(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
  }

  /**
   * Cleans up the test environment after each test.
   *
   * @throws Exception
   *     if an error occurs during teardown
   */
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

  /**
   * Tests the getSeq method to ensure it returns the expected value.
   */
  @Test
  public void testGetSeqReturnsExpectedValue() {
    // When
    long result = classUnderTest.getSeq();

    // Then
    assertEquals(100L, result);
  }

  /**
   * Tests the getDocumentDisplayLogic method to ensure it always returns true.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDocumentDisplayLogicAlwaysReturnsTrue() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    boolean result = classUnderTest.getDocumentDisplayLogic(requestMap);

    // Then
    assertTrue("Document display logic should always return true", result);
  }

  /**
   * Tests the getOrganizationDisplayLogic method to ensure it always returns false.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOrganizationDisplayLogicAlwaysReturnsFalse() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    boolean result = classUnderTest.getOrganizationDisplayLogic(requestMap);

    // Then
    assertFalse("Organization display logic should always return false", result);
  }

  /**
   * Tests the getCreditToUseDisplayLogic method with no business partner to ensure it returns false.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetCreditToUseDisplayLogicWithNoBusinessPartnerReturnsFalse() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    boolean result = classUnderTest.getCreditToUseDisplayLogic(requestMap);

    // Then
    assertFalse("Credit to use display logic should return false when no business partner is provided", result);
  }

  /**
   * Tests the getCreditToUseDisplayLogic method with an empty business partner to ensure it returns false.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetCreditToUseDisplayLogicWithEmptyBusinessPartnerReturnsFalse() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.RECEIVED_FROM, "");
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    boolean result = classUnderTest.getCreditToUseDisplayLogic(requestMap);

    // Then
    assertFalse("Credit to use display logic should return false when business partner is empty", result);
  }

  /**
   * Tests the getCreditToUseDisplayLogic method with a business partner and zero credit to ensure it returns false.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetCreditToUseDisplayLogicWithBusinessPartnerAndZeroCreditReturnsFalse() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.RECEIVED_FROM, TestConstants.TEST_BPARTNER_ID);
    context.put(TestConstants.TRXTYPE, TestConstants.PAYMENT_TYPE);
    context.put(TestConstants.AD_ORG_ID, TestConstants.TEST_ORG_ID);
    context.put(TestConstants.C_CURRENCY, TestConstants.C_CURRENCY_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // Mock OBDal.get calls
    when(mockOBDal.get(BusinessPartner.class, TestConstants.TEST_BPARTNER_ID)).thenReturn(mockBusinessPartner);
    when(mockOBDal.get(Organization.class, TestConstants.TEST_ORG_ID)).thenReturn(mockOrganization);
    when(mockOBDal.get(Currency.class, TestConstants.C_CURRENCY_ID)).thenReturn(mockCurrency);

    mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class, (mock, ctx) -> when(
        mock.getCustomerCredit(mockBusinessPartner, false, mockOrganization, mockCurrency)).thenReturn(
        BigDecimal.ZERO));

    TransactionAddPaymentDisplayLogics spyClassUnderTest = new TransactionAddPaymentDisplayLogics() {
      @Override
      BigDecimal getDefaultGeneratedCredit(Map<String, String> requestMap) {
        return BigDecimal.ZERO;
      }
    };

    // When
    boolean result = spyClassUnderTest.getCreditToUseDisplayLogic(requestMap);

    // Then
    assertFalse("Credit to use display logic should return false when customer credit is zero", result);
  }

  /**
   * Tests the getCreditToUseDisplayLogic method with a business partner and positive credit to ensure it returns true.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetCreditToUseDisplayLogicWithBusinessPartnerAndPositiveCreditReturnsTrue() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.RECEIVED_FROM, TestConstants.TEST_BPARTNER_ID);
    context.put(TestConstants.TRXTYPE, TestConstants.PAYMENT_TYPE);
    context.put(TestConstants.AD_ORG_ID, TestConstants.TEST_ORG_ID);
    context.put(TestConstants.C_CURRENCY, TestConstants.C_CURRENCY_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // Mock OBDal.get calls
    when(mockOBDal.get(BusinessPartner.class, TestConstants.TEST_BPARTNER_ID)).thenReturn(mockBusinessPartner);
    when(mockOBDal.get(Organization.class, TestConstants.TEST_ORG_ID)).thenReturn(mockOrganization);
    when(mockOBDal.get(Currency.class, TestConstants.C_CURRENCY_ID)).thenReturn(mockCurrency);

    mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class, (mock, ctx) -> when(
        mock.getCustomerCredit(mockBusinessPartner, false, mockOrganization, mockCurrency)).thenReturn(
        new BigDecimal(TestConstants.AMOUNT)));

    TransactionAddPaymentDisplayLogics spyClassUnderTest = new TransactionAddPaymentDisplayLogics() {
      @Override
      BigDecimal getDefaultGeneratedCredit(Map<String, String> requestMap) {
        return BigDecimal.ZERO;
      }
    };

    // When
    boolean result = spyClassUnderTest.getCreditToUseDisplayLogic(requestMap);

    // Then
    assertTrue("Credit to use display logic should return true when customer credit is positive", result);
  }

  /**
   * Tests the getCreditToUseDisplayLogic method with RCIN document type to ensure it checks credit.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetCreditToUseDisplayLogicWithRCINDocumentTypeChecksCredit() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.RECEIVED_FROM, TestConstants.TEST_BPARTNER_ID);
    context.put(TestConstants.TRXTYPE, "RCIN");
    context.put(TestConstants.AD_ORG_ID, TestConstants.TEST_ORG_ID);
    context.put(TestConstants.C_CURRENCY, TestConstants.C_CURRENCY_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // Mock OBDal.get calls
    when(mockOBDal.get(BusinessPartner.class, TestConstants.TEST_BPARTNER_ID)).thenReturn(mockBusinessPartner);
    when(mockOBDal.get(Organization.class, TestConstants.TEST_ORG_ID)).thenReturn(mockOrganization);
    when(mockOBDal.get(Currency.class, TestConstants.C_CURRENCY_ID)).thenReturn(mockCurrency);

    AdvPaymentMngtDao mockDao = mock(AdvPaymentMngtDao.class);
    when(mockDao.getCustomerCredit(mockBusinessPartner, true, mockOrganization, mockCurrency)).thenReturn(
        new BigDecimal(TestConstants.AMOUNT));

    mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class, (mock, ctx) -> when(
        mock.getCustomerCredit(mockBusinessPartner, true, mockOrganization, mockCurrency)).thenReturn(
        new BigDecimal(TestConstants.AMOUNT)));

    // When
    boolean result = classUnderTest.getCreditToUseDisplayLogic(requestMap);

    // Then
    assertTrue("Credit to use display logic should return true for RCIN document with positive credit", result);
  }

  /**
   * Tests the getCreditToUseDisplayLogic method with inpreceivedFrom parameter to ensure it uses the alternative parameter.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetCreditToUseDisplayLogicWithInpreceivedFromUsesAlternativeParameter() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpreceivedFrom", TestConstants.TEST_BPARTNER_ID);
    context.put("inptrxtype", TestConstants.PAYMENT_TYPE);
    context.put(TestConstants.AD_ORG_ID, TestConstants.TEST_ORG_ID);
    context.put(TestConstants.C_CURRENCY, TestConstants.C_CURRENCY_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // Mock OBDal.get calls
    when(mockOBDal.get(BusinessPartner.class, TestConstants.TEST_BPARTNER_ID)).thenReturn(mockBusinessPartner);
    when(mockOBDal.get(Organization.class, TestConstants.TEST_ORG_ID)).thenReturn(mockOrganization);
    when(mockOBDal.get(Currency.class, TestConstants.C_CURRENCY_ID)).thenReturn(mockCurrency);

    mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class, (mock, ctx) -> when(
        mock.getCustomerCredit(mockBusinessPartner, false, mockOrganization, mockCurrency)).thenReturn(
        new BigDecimal(TestConstants.AMOUNT)));

    TransactionAddPaymentDisplayLogics spyClassUnderTest = new TransactionAddPaymentDisplayLogics() {
      @Override
      BigDecimal getDefaultGeneratedCredit(Map<String, String> requestMap) {
        return BigDecimal.ZERO;
      }
    };

    // When
    boolean result = spyClassUnderTest.getCreditToUseDisplayLogic(requestMap);

    // Then
    assertTrue("Credit to use display logic should return true when using inpreceivedFrom parameter", result);
  }

  /**
   * Tests the getDefaultGeneratedCredit method to ensure it returns zero.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultGeneratedCreditReturnsZero() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    BigDecimal result = classUnderTest.getDefaultGeneratedCredit(requestMap);

    // Then
    assertEquals("Default generated credit should be zero", BigDecimal.ZERO, result);
  }

  /**
   * Tests the getBankStatementLineDisplayLogic method with trxtype to ensure it returns true.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetBankStatementLineDisplayLogicWithTrxtypeReturnsTrue() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.TRXTYPE, TestConstants.PAYMENT_TYPE);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    boolean result = classUnderTest.getBankStatementLineDisplayLogic(requestMap);

    // Then
    assertTrue("Bank statement line display logic should return true when trxtype is present", result);
  }

  /**
   * Tests the getBankStatementLineDisplayLogic method without trxtype to ensure it returns false.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetBankStatementLineDisplayLogicWithoutTrxtypeReturnsFalse() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // When
    boolean result = classUnderTest.getBankStatementLineDisplayLogic(requestMap);

    // Then
    assertFalse("Bank statement line display logic should return false when trxtype is not present", result);
  }
}
