package org.openbravo.erpCommon.ad_process;

import static org.junit.Assert.assertEquals;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

/**
 * Tests for {@link CashBankOperations}.
 * Focuses on the private negate method which performs amount negation.
 */
@SuppressWarnings({"java:S120", "java:S112"})
@RunWith(MockitoJUnitRunner.class)
public class CashBankOperationsTest {

  private CashBankOperations instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(CashBankOperations.class);
  }
  /**
   * Negate positive amount.
   * @throws Exception if an error occurs
   */

  @Test
  public void testNegatePositiveAmount() throws Exception {
    String result = invokeNegate("100.00");
    BigDecimal expected = new BigDecimal("100.00").multiply(new BigDecimal("-1.0"));
    assertEquals(expected.toString(), result);
  }
  /**
   * Negate negative amount.
   * @throws Exception if an error occurs
   */

  @Test
  public void testNegateNegativeAmount() throws Exception {
    String result = invokeNegate("-50.00");
    BigDecimal expected = new BigDecimal("-50.00").multiply(new BigDecimal("-1.0"));
    assertEquals(expected.toString(), result);
  }
  /**
   * Negate zero.
   * @throws Exception if an error occurs
   */

  @Test
  public void testNegateZero() throws Exception {
    String result = invokeNegate("0");
    BigDecimal expected = new BigDecimal("0").multiply(new BigDecimal("-1.0"));
    assertEquals(expected.toString(), result);
  }
  /**
   * Negate decimal amount.
   * @throws Exception if an error occurs
   */

  @Test
  public void testNegateDecimalAmount() throws Exception {
    String result = invokeNegate("123.456");
    BigDecimal expected = new BigDecimal("123.456").multiply(new BigDecimal("-1.0"));
    assertEquals(expected.toString(), result);
  }
  /**
   * Negate small amount.
   * @throws Exception if an error occurs
   */

  @Test
  public void testNegateSmallAmount() throws Exception {
    String result = invokeNegate("0.01");
    BigDecimal expected = new BigDecimal("0.01").multiply(new BigDecimal("-1.0"));
    assertEquals(expected.toString(), result);
  }
  /**
   * Negate large amount.
   * @throws Exception if an error occurs
   */

  @Test
  public void testNegateLargeAmount() throws Exception {
    String result = invokeNegate("999999999.99");
    BigDecimal expected = new BigDecimal("999999999.99").multiply(new BigDecimal("-1.0"));
    assertEquals(expected.toString(), result);
  }
  /**
   * Negate invalid amount throws exception.
   * @throws Exception if an error occurs
   */

  @Test(expected = NumberFormatException.class)
  public void testNegateInvalidAmountThrowsException() throws Exception {
    try {
      invokeNegate("not_a_number");
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw (NumberFormatException) e.getCause();
    }
  }

  private String invokeNegate(String amount) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = CashBankOperations.class.getDeclaredMethod("negate", String.class);
    method.setAccessible(true);
    return (String) method.invoke(instance, amount);
  }
}
