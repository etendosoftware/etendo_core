package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.openbravo.test.base.mock.MockitoStaticMockUtils.mockStaticSafely;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

/**
 * Test for {@link PaymentOutAddPaymentDisplayLogics} class.
 */
@RunWith(MockitoJUnitRunner.class)
public class PaymentOutAddPaymentDisplayLogicsTest {

  @Mock
  private OBDal obDal;

  @Mock
  private FIN_Payment payment;

  private PaymentOutAddPaymentDisplayLogics logicsHandler;
  private Map<String, String> requestMap;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    logicsHandler = new PaymentOutAddPaymentDisplayLogics();

    requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    try {
      context.put("inpfinPaymentId", TestConstants.PAYMENT_ID);
      requestMap.put(TestConstants.CONTEXT, context.toString());
    } catch (JSONException e) {
      fail("Error setting up test: " + e.getMessage());
    }
  }

  /**
   * Tests the organization display logic.
   *
   * @throws JSONException
   *     if there is an error during JSON processing
   */
  @Test
  public void testOrganizationDisplayLogic() throws JSONException {
    boolean result = logicsHandler.getOrganizationDisplayLogic(requestMap);
    assertFalse("Organization display logic should be false", result);
  }

  /**
   * Tests the document display logic.
   *
   * @throws JSONException
   *     if there is an error during JSON processing
   */
  @Test
  public void testDocumentDisplayLogic() throws JSONException {
    boolean result = logicsHandler.getDocumentDisplayLogic(requestMap);
    assertFalse("Document display logic should be false", result);
  }

  /**
   * Tests the bank statement line display logic.
   *
   * @throws JSONException
   *     if there is an error during JSON processing
   */
  @Test
  public void testBankStatementLineDisplayLogic() throws JSONException {
    boolean result = logicsHandler.getBankStatementLineDisplayLogic(requestMap);
    assertFalse("Bank statement line display logic should be false", result);
  }

  /**
   * Tests the sequence retrieval.
   */
  @Test
  public void testGetSeq() {
    long result = logicsHandler.getSeq();
    assertEquals("Sequence should be 100", 100L, result);
  }

  /**
   * Tests the credit to use display logic when there is no business partner.
   *
   * @throws JSONException
   *     if there is an error during JSON processing
   */
  @Test
  public void testCreditToUseDisplayLogicNoBpartner() throws JSONException {
    try (MockedStatic<OBDal> obDalStatic = mockStaticSafely(OBDal.class)) {
      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.get(eq(FIN_Payment.class), eq(TestConstants.PAYMENT_ID))).thenReturn(payment);
      when(payment.getGeneratedCredit()).thenReturn(BigDecimal.ZERO);
      when(payment.getBusinessPartner()).thenReturn(null);

      boolean result = logicsHandler.getCreditToUseDisplayLogic(requestMap);
      assertFalse("Credit to use display logic should be false when business partner is null", result);
    }
  }

  /**
   * Tests the credit to use display logic when there is generated credit.
   *
   * @throws JSONException
   *     if there is an error during JSON processing
   */
  @Test
  public void testCreditToUseDisplayLogicWithGeneratedCredit() throws JSONException {
    try (MockedStatic<OBDal> obDalStatic = mockStaticSafely(OBDal.class)) {
      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.get(eq(FIN_Payment.class), eq(TestConstants.PAYMENT_ID))).thenReturn(payment);
      when(payment.getGeneratedCredit()).thenReturn(BigDecimal.TEN);

      boolean result = logicsHandler.getCreditToUseDisplayLogic(requestMap);
      assertFalse("Credit to use display logic should be false when generated credit is not zero", result);
    }
  }

  /**
   * Tests the retrieval of the payment object.
   *
   * @throws Exception
   *     if there is an error during reflection or JSON processing
   */
  @Test
  public void testGetPayment() throws Exception {
    try (MockedStatic<OBDal> obDalStatic = mockStaticSafely(OBDal.class)) {
      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.get(eq(FIN_Payment.class), eq(TestConstants.PAYMENT_ID))).thenReturn(payment);

      JSONObject context = new JSONObject();
      context.put("inpfinPaymentId", TestConstants.PAYMENT_ID);
      requestMap.put(TestConstants.CONTEXT, context.toString());

      java.lang.reflect.Method getPaymentMethod = PaymentOutAddPaymentDisplayLogics.class.getDeclaredMethod(
          "getPayment", Map.class);
      getPaymentMethod.setAccessible(true);

      FIN_Payment result = (FIN_Payment) getPaymentMethod.invoke(logicsHandler, requestMap);
      assertSame("Should return the payment object for inpfinPaymentId", payment, result);
      verify(obDal).get(FIN_Payment.class, TestConstants.PAYMENT_ID);

      context = new JSONObject();
      context.put("Fin_Payment_ID", TestConstants.PAYMENT_ID_2);
      requestMap.put(TestConstants.CONTEXT, context.toString());

      when(obDal.get(eq(FIN_Payment.class), eq(TestConstants.PAYMENT_ID_2))).thenReturn(payment);

      result = (FIN_Payment) getPaymentMethod.invoke(logicsHandler, requestMap);
      assertSame("Should return the payment object for Fin_Payment_ID", payment, result);
      verify(obDal).get(FIN_Payment.class, TestConstants.PAYMENT_ID_2);
    }
  }

  /**
   * Tests the retrieval of the generated credit.
   *
   * @throws Exception
   *     if there is an error during reflection or JSON processing
   */
  @Test
  public void testGetGeneratedCredit() throws Exception {
    BigDecimal creditAmount = BigDecimal.valueOf(123.45);
    try (MockedStatic<OBDal> obDalStatic = mockStaticSafely(OBDal.class)) {
      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.get(eq(FIN_Payment.class), eq(TestConstants.PAYMENT_ID))).thenReturn(payment);
      when(payment.getGeneratedCredit()).thenReturn(creditAmount);

      java.lang.reflect.Method getGeneratedCreditMethod = PaymentOutAddPaymentDisplayLogics.class.getDeclaredMethod(
          "getGeneratedCredit", Map.class);
      getGeneratedCreditMethod.setAccessible(true);

      BigDecimal result = (BigDecimal) getGeneratedCreditMethod.invoke(logicsHandler, requestMap);
      assertEquals("Should return the generated credit from payment", creditAmount, result);
    }
  }
}
