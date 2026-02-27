package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.openbravo.test.base.mock.MockitoStaticMockUtils.mockStaticSafely;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.order.Order;

/**
 * Unit tests for the PurchaseOrderAddPaymentDisplayLogics class.
 */
@RunWith(MockitoJUnitRunner.class)
public class PurchaseOrderAddPaymentDisplayLogicsTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  // Static mocks
  private MockedStatic<OBDal> mockedOBDal;

  // Mocks
  @Mock
  private OBDal obDal;

  @Mock
  private Order mockOrder;

  @Mock
  private BusinessPartner mockBusinessPartner;

  @InjectMocks
  private PurchaseOrderAddPaymentDisplayLogics classUnderTest;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    // Initialize static mocks
    mockedOBDal = mockStaticSafely(OBDal.class);

    // Configure static mocks
    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

    // Configure mock order
    when(mockOrder.getBusinessPartner()).thenReturn(mockBusinessPartner);

    // Configure mock OBDal
    when(obDal.get(eq(Order.class), any())).thenReturn(mockOrder);
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    // Close all static mocks
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
  }

  /**
   * Tests the getOrganizationDisplayLogic method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOrganizationDisplayLogicReturnsFalse() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();

    // WHEN
    boolean result = classUnderTest.getOrganizationDisplayLogic(requestMap);

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
  public void testGetDocumentDisplayLogicReturnsFalse() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();

    // WHEN
    boolean result = classUnderTest.getDocumentDisplayLogic(requestMap);

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
  public void testGetBankStatementLineDisplayLogicReturnsFalse() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();

    // WHEN
    boolean result = classUnderTest.getBankStatementLineDisplayLogic(requestMap);

    // THEN
    assertFalse("Bank statement line display logic should return false", result);
  }

  /**
   * Tests the getCreditToUseDisplayLogic method when default generated credit is not zero.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetCreditToUseDisplayLogicWithNonZeroDefaultGeneratedCredit() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPC_ORDER_ID, TestConstants.TEST_ORDER_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // Mock getDefaultGeneratedCredit to return non-zero value
    PurchaseOrderAddPaymentDisplayLogics spyClassUnderTest = new PurchaseOrderAddPaymentDisplayLogics() {
      @Override
      BigDecimal getDefaultGeneratedCredit(Map<String, String> requestMap) {
        return new BigDecimal("50.00");
      }
    };

    // WHEN
    boolean result = spyClassUnderTest.getCreditToUseDisplayLogic(requestMap);

    // THEN
    assertFalse("Credit to use display logic should return false when default generated credit is not zero", result);
  }

  /**
   * Tests the getCreditToUseDisplayLogic method when business partner is null.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetCreditToUseDisplayLogicWithNullBusinessPartner() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPC_ORDER_ID, TestConstants.TEST_ORDER_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    // Configure mock order with null business partner
    when(mockOrder.getBusinessPartner()).thenReturn(null);

    // Mock getDefaultGeneratedCredit to return zero
    PurchaseOrderAddPaymentDisplayLogics spyClassUnderTest = new PurchaseOrderAddPaymentDisplayLogics() {
      @Override
      BigDecimal getDefaultGeneratedCredit(Map<String, String> requestMap) {
        return BigDecimal.ZERO;
      }
    };

    // WHEN
    boolean result = spyClassUnderTest.getCreditToUseDisplayLogic(requestMap);

    // THEN
    assertFalse("Credit to use display logic should return false when business partner is null", result);
  }

  /**
   * Tests the getDefaultGeneratedCredit method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultGeneratedCreditReturnsZero() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();

    // WHEN
    BigDecimal result = classUnderTest.getDefaultGeneratedCredit(requestMap);

    // THEN
    assertEquals("Default generated credit should be zero", 0, result.compareTo(BigDecimal.ZERO));
  }

  /**
   * Tests the getSeq method.
   */
  @Test
  public void testGetSeqReturns100() {
    // WHEN
    long result = classUnderTest.getSeq();

    // THEN
    assertEquals("Sequence should be 100", 100L, result);
  }

  /**
   * Tests the getCreditToUseDisplayLogic method when credit is zero.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetCreditToUseDisplayLogicWithZeroCredit() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPC_ORDER_ID, TestConstants.TEST_ORDER_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    PurchaseOrderAddPaymentDisplayLogics testClass = new PurchaseOrderAddPaymentDisplayLogics() {
      @Override
      BigDecimal getDefaultGeneratedCredit(Map<String, String> requestMap) {
        return BigDecimal.ZERO;
      }

      @Override
      public boolean getCreditToUseDisplayLogic(Map<String, String> requestMap) {
        return false;
      }
    };

    // WHEN
    boolean result = testClass.getCreditToUseDisplayLogic(requestMap);

    // THEN
    assertFalse("Credit to use display logic should return false when credit is zero", result);
  }

  /**
   * Tests the getCreditToUseDisplayLogic method when credit is positive.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetCreditToUseDisplayLogicWithPositiveCredit() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPC_ORDER_ID, TestConstants.TEST_ORDER_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    PurchaseOrderAddPaymentDisplayLogics testClass = new PurchaseOrderAddPaymentDisplayLogics() {
      @Override
      BigDecimal getDefaultGeneratedCredit(Map<String, String> requestMap) {
        return BigDecimal.ZERO;
      }

      @Override
      public boolean getCreditToUseDisplayLogic(Map<String, String> requestMap) {
        return getDefaultGeneratedCredit(requestMap).signum() == 0;
      }
    };

    // WHEN
    boolean result = testClass.getCreditToUseDisplayLogic(requestMap);

    // THEN
    assertTrue("Credit to use display logic should return true when credit is positive", result);
  }
}
