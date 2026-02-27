package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.openbravo.test.base.mock.MockitoStaticMockUtils.mockStaticSafely;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;

/**
 * Test class for SalesOrderAddPaymentDisplayLogics.
 */
public class SalesOrderAddPaymentDisplayLogicsTest {

  private SalesOrderAddPaymentDisplayLogics displayLogics;
  private MockedStatic<OBDal> mockedOBDal;
  private AutoCloseable mocks;
  private MockedConstruction<AdvPaymentMngtDao> mockedAdvPaymentMngtDao;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private Order mockOrder;

  @Mock
  private BusinessPartner mockBusinessPartner;

  @Mock
  private Organization mockOrganization;

  @Mock
  private Currency mockCurrency;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    displayLogics = new SalesOrderAddPaymentDisplayLogics();

    // Setup static mocks
    mockedOBDal = mockStaticSafely(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    // Setup common mock behavior
    when(mockOBDal.get(eq(Order.class), anyString())).thenReturn(mockOrder);
    when(mockOrder.getBusinessPartner()).thenReturn(mockBusinessPartner);
    when(mockOrder.getOrganization()).thenReturn(mockOrganization);
    when(mockOrder.getCurrency()).thenReturn(mockCurrency);
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
   * Tests the getSeq method.
   */
  @Test
  public void testGetSeq() {
    // WHEN
    long sequence = displayLogics.getSeq();

    // THEN
    assertEquals("The sequence should be 100", 100L, sequence);
  }

  /**
   * Tests the getOrganizationDisplayLogic method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOrganizationDisplayLogic() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();

    // WHEN
    boolean result = displayLogics.getOrganizationDisplayLogic(requestMap);

    // THEN
    assertFalse("Organization display logic should return false", result);
  }

  /**
   * Tests the getDocumentDisplayLogic method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDocumentDisplayLogic() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();

    // WHEN
    boolean result = displayLogics.getDocumentDisplayLogic(requestMap);

    // THEN
    assertFalse("Document display logic should return false", result);
  }

  /**
   * Tests the getBankStatementLineDisplayLogic method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetBankStatementLineDisplayLogic() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();

    // WHEN
    boolean result = displayLogics.getBankStatementLineDisplayLogic(requestMap);

    // THEN
    assertFalse("Bank statement line display logic should return false", result);
  }

  /**
   * Tests the getCreditToUseDisplayLogic method with positive credit.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetCreditToUseDisplayLogicWithPositiveCredit() throws Exception {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPC_ORDER_ID, TestConstants.TEST_ORDER_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class, (mock, ctx) -> when(
        mock.getCustomerCredit(any(BusinessPartner.class), anyBoolean(), any(Organization.class),
            any(Currency.class))).thenReturn(new BigDecimal("100.00")));

    // WHEN
    boolean result = displayLogics.getCreditToUseDisplayLogic(requestMap);

    // THEN
    assertTrue("Credit to use display logic should return true for positive credit", result);
  }

  /**
   * Tests the getCreditToUseDisplayLogic method with zero credit.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetCreditToUseDisplayLogicWithZeroCredit() throws Exception {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPC_ORDER_ID, TestConstants.TEST_ORDER_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class, (mock, ctx) -> when(
        mock.getCustomerCredit(any(BusinessPartner.class), anyBoolean(), any(Organization.class),
            any(Currency.class))).thenReturn(BigDecimal.ZERO));

    // WHEN
    boolean result = displayLogics.getCreditToUseDisplayLogic(requestMap);

    // THEN
    assertFalse("Credit to use display logic should return false for zero credit", result);
  }

  /**
   * Tests the getCreditToUseDisplayLogic method with negative credit.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetCreditToUseDisplayLogicWithNegativeCredit() throws Exception {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPC_ORDER_ID, TestConstants.TEST_ORDER_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class, (mock, ctx) -> when(
        mock.getCustomerCredit(any(BusinessPartner.class), anyBoolean(), any(Organization.class),
            any(Currency.class))).thenReturn(new BigDecimal("-50.00")));

    // WHEN
    boolean result = displayLogics.getCreditToUseDisplayLogic(requestMap);

    // THEN
    assertFalse("Credit to use display logic should return false for negative credit", result);
  }

  /**
   * Tests the getCreditToUseDisplayLogic method with null business partner.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetCreditToUseDisplayLogicWithNullBusinessPartner() throws Exception {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPC_ORDER_ID, TestConstants.TEST_ORDER_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // Set business partner to null
    when(mockOrder.getBusinessPartner()).thenReturn(null);

    // WHEN
    boolean result = displayLogics.getCreditToUseDisplayLogic(requestMap);

    // THEN
    assertFalse("Credit to use display logic should return false for null business partner", result);
  }

  /**
   * Tests the getOverpaymentActionDisplayLogic method with positive difference.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOverpaymentActionDisplayLogicWithPositiveDifference() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.DIFFERENCE, 50.0);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // WHEN
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);

    // THEN
    assertTrue("Overpayment action display logic should return true for positive difference", result);
  }

  /**
   * Tests the getOverpaymentActionDisplayLogic method with zero difference.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOverpaymentActionDisplayLogicWithZeroDifference() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.DIFFERENCE, 0.0);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // WHEN
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);

    // THEN
    assertFalse("Overpayment action display logic should return false for zero difference", result);
  }

  /**
   * Tests the getOverpaymentActionDisplayLogic method with negative difference.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOverpaymentActionDisplayLogicWithNegativeDifference() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.DIFFERENCE, -25.0);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // WHEN
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);

    // THEN
    assertFalse("Overpayment action display logic should return false for negative difference", result);
  }

  /**
   * Tests the getOverpaymentActionDisplayLogic method with null context.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOverpaymentActionDisplayLogicWithNullContext() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();

    // WHEN
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);

    // THEN
    assertFalse("Overpayment action display logic should return false for null context", result);
  }

  /**
   * Tests the getOverpaymentActionDisplayLogic method without difference in context.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOverpaymentActionDisplayLogicWithoutDifference() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    // No difference key in the context
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // WHEN
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);

    // THEN
    assertFalse("Overpayment action display logic should return false when difference is not in context", result);
  }
}
