package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
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
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test class for SalesOrderAddPaymentDisplayLogics
 */
public class SalesOrderAddPaymentDisplayLogicsTest extends OBBaseTest {

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

  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    displayLogics = new SalesOrderAddPaymentDisplayLogics();

    // Setup static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    // Setup common mock behavior
    when(mockOBDal.get(eq(Order.class), anyString())).thenReturn(mockOrder);
    when(mockOrder.getBusinessPartner()).thenReturn(mockBusinessPartner);
    when(mockOrder.getOrganization()).thenReturn(mockOrganization);
    when(mockOrder.getCurrency()).thenReturn(mockCurrency);
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
    // WHEN
    long sequence = displayLogics.getSeq();

    // THEN
    assertEquals("The sequence should be 100", 100L, sequence);
  }

  @Test
  public void testGetOrganizationDisplayLogic() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();

    // WHEN
    boolean result = displayLogics.getOrganizationDisplayLogic(requestMap);

    // THEN
    assertFalse("Organization display logic should return false", result);
  }

  @Test
  public void testGetDocumentDisplayLogic() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();

    // WHEN
    boolean result = displayLogics.getDocumentDisplayLogic(requestMap);

    // THEN
    assertFalse("Document display logic should return false", result);
  }

  @Test
  public void testGetBankStatementLineDisplayLogic() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();

    // WHEN
    boolean result = displayLogics.getBankStatementLineDisplayLogic(requestMap);

    // THEN
    assertFalse("Bank statement line display logic should return false", result);
  }

  @Test
  public void testGetCreditToUseDisplayLogicWithPositiveCredit() throws Exception {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpcOrderId", "TEST_ORDER_ID");
    requestMap.put("context", context.toString());

    mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class,
        (mock, ctx) -> {
          when(mock.getCustomerCredit(any(BusinessPartner.class), anyBoolean(), any(Organization.class), any(Currency.class)))
              .thenReturn(new BigDecimal("100.00"));
        });

    // WHEN
    boolean result = displayLogics.getCreditToUseDisplayLogic(requestMap);

    // THEN
    assertTrue("Credit to use display logic should return true for positive credit", result);
  }

  @Test
  public void testGetCreditToUseDisplayLogicWithZeroCredit() throws Exception {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpcOrderId", "TEST_ORDER_ID");
    requestMap.put("context", context.toString());

    mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class,
        (mock, ctx) -> {
          when(mock.getCustomerCredit(any(BusinessPartner.class), anyBoolean(), any(Organization.class), any(Currency.class)))
              .thenReturn(BigDecimal.ZERO);
        });

    // WHEN
    boolean result = displayLogics.getCreditToUseDisplayLogic(requestMap);

    // THEN
    assertFalse("Credit to use display logic should return false for zero credit", result);
  }

  @Test
  public void testGetCreditToUseDisplayLogicWithNegativeCredit() throws Exception {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpcOrderId", "TEST_ORDER_ID");
    requestMap.put("context", context.toString());

    mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class,
        (mock, ctx) -> {
          when(mock.getCustomerCredit(any(BusinessPartner.class), anyBoolean(), any(Organization.class), any(Currency.class)))
              .thenReturn(new BigDecimal("-50.00"));
        });

    // WHEN
    boolean result = displayLogics.getCreditToUseDisplayLogic(requestMap);

    // THEN
    assertFalse("Credit to use display logic should return false for negative credit", result);
  }

  @Test
  public void testGetCreditToUseDisplayLogicWithNullBusinessPartner() throws Exception {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("inpcOrderId", "TEST_ORDER_ID");
    requestMap.put("context", context.toString());

    // Set business partner to null
    when(mockOrder.getBusinessPartner()).thenReturn(null);

    // WHEN
    boolean result = displayLogics.getCreditToUseDisplayLogic(requestMap);

    // THEN
    assertFalse("Credit to use display logic should return false for null business partner", result);
  }

  @Test
  public void testGetOverpaymentActionDisplayLogicWithPositiveDifference() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("difference", 50.0);
    requestMap.put("context", context.toString());

    // WHEN
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);

    // THEN
    assertTrue("Overpayment action display logic should return true for positive difference", result);
  }

  @Test
  public void testGetOverpaymentActionDisplayLogicWithZeroDifference() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("difference", 0.0);
    requestMap.put("context", context.toString());

    // WHEN
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);

    // THEN
    assertFalse("Overpayment action display logic should return false for zero difference", result);
  }

  @Test
  public void testGetOverpaymentActionDisplayLogicWithNegativeDifference() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("difference", -25.0);
    requestMap.put("context", context.toString());

    // WHEN
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);

    // THEN
    assertFalse("Overpayment action display logic should return false for negative difference", result);
  }

  @Test
  public void testGetOverpaymentActionDisplayLogicWithNullContext() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();

    // WHEN
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);

    // THEN
    assertFalse("Overpayment action display logic should return false for null context", result);
  }

  @Test
  public void testGetOverpaymentActionDisplayLogicWithoutDifference() throws JSONException {
    // GIVEN
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    // No difference key in the context
    requestMap.put("context", context.toString());

    // WHEN
    boolean result = displayLogics.getOverpaymentActionDisplayLogic(requestMap);

    // THEN
    assertFalse("Overpayment action display logic should return false when difference is not in context", result);
  }
}