/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.erpCommon.ad_process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

/**
 * Tests for {@link CreateCashFlowStatement} private utility methods.
 */
@SuppressWarnings({"java:S120", "java:S112"})
@RunWith(MockitoJUnitRunner.class)
public class CreateCashFlowStatementTest {

  private CreateCashFlowStatement instance;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(CreateCashFlowStatement.class);

    // Set log4j field from HttpBaseServlet parent to avoid NPE
    Field log4jField = findField(instance.getClass(), "log4j");
    log4jField.setAccessible(true);
    log4jField.set(instance, LogManager.getLogger(CreateCashFlowStatement.class));
  }
  /**
   * Calculate ratio normal case.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCalculateRatioNormalCase() throws Exception {
    String result = invokeCalculateRatio("100", "50");
    // 50/100 = 0.5
    assertTrue(result.startsWith("0.5"));
  }
  /**
   * Calculate ratio returns zero when total is zero.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCalculateRatioReturnsZeroWhenTotalIsZero() throws Exception {
    String result = invokeCalculateRatio("0", "50");
    assertEquals("0", result);
  }
  /**
   * Calculate ratio returns zero when amt is zero.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCalculateRatioReturnsZeroWhenAmtIsZero() throws Exception {
    String result = invokeCalculateRatio("100", "0");
    assertEquals("0", result);
  }
  /**
   * Calculate ratio returns zero when total is null.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCalculateRatioReturnsZeroWhenTotalIsNull() throws Exception {
    String result = invokeCalculateRatio(null, "50");
    assertEquals("0", result);
  }
  /**
   * Calculate ratio returns zero when amt is null.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCalculateRatioReturnsZeroWhenAmtIsNull() throws Exception {
    String result = invokeCalculateRatio("100", null);
    assertEquals("0", result);
  }
  /**
   * Calculate ratio returns zero when total is empty.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCalculateRatioReturnsZeroWhenTotalIsEmpty() throws Exception {
    String result = invokeCalculateRatio("", "50");
    assertEquals("0", result);
  }
  /**
   * Calculate ratio returns zero when amt is empty.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCalculateRatioReturnsZeroWhenAmtIsEmpty() throws Exception {
    String result = invokeCalculateRatio("100", "");
    assertEquals("0", result);
  }
  /**
   * Calculate ratio with negative values.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCalculateRatioWithNegativeValues() throws Exception {
    String result = invokeCalculateRatio("100", "-50");
    // -50/100 = -0.5
    assertTrue(result.startsWith("-0.5"));
  }
  /**
   * Multiply normal case.
   * @throws Exception if an error occurs
   */

  @Test
  public void testMultiplyNormalCase() throws Exception {
    String result = invokeMultiply("10", "5");
    // Result should contain 50 (with trailing zeros due to setScale(200))
    assertTrue(result.contains("50"));
  }
  /**
   * Multiply with zero.
   * @throws Exception if an error occurs
   */

  @Test
  public void testMultiplyWithZero() throws Exception {
    String result = invokeMultiply("100", "0");
    // 100 * 0 = 0
    assertTrue(result.startsWith("0"));
  }
  /**
   * Multiply with negative.
   * @throws Exception if an error occurs
   */

  @Test
  public void testMultiplyWithNegative() throws Exception {
    String result = invokeMultiply("10", "-3");
    assertTrue(result.contains("-30"));
  }
  /**
   * Multiply with decimals.
   * @throws Exception if an error occurs
   */

  @Test
  public void testMultiplyWithDecimals() throws Exception {
    String result = invokeMultiply("2.5", "4");
    // 2.5 * 4 = 10.0
    assertTrue(result.contains("10.0"));
  }
  /** Get servlet info. */

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet CreateCashFlowStatement", instance.getServletInfo());
  }

  private String invokeCalculateRatio(String strTotal, String strAmt) throws Exception{
    Method method = CreateCashFlowStatement.class.getDeclaredMethod("calculateRatio", String.class,
        String.class);
    method.setAccessible(true);
    return (String) method.invoke(instance, strTotal, strAmt);
  }

  private String invokeMultiply(String strOP1, String strOP2) throws Exception{
    Method method = CreateCashFlowStatement.class.getDeclaredMethod("multiply", String.class,
        String.class);
    method.setAccessible(true);
    return (String) method.invoke(instance, strOP1, strOP2);
  }

  private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    Class<?> current = clazz;
    while (current != null) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new NoSuchFieldException(fieldName);
  }
}
