/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.erpCommon.ad_process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
@RunWith(MockitoJUnitRunner.class)
public class CreateCashFlowStatementTest {

  private CreateCashFlowStatement instance;

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(CreateCashFlowStatement.class);

    // Set log4j field from HttpBaseServlet parent to avoid NPE
    Field log4jField = findField(instance.getClass(), "log4j");
    log4jField.setAccessible(true);
    log4jField.set(instance, LogManager.getLogger(CreateCashFlowStatement.class));
  }

  @Test
  public void testCalculateRatioNormalCase() throws Exception {
    String result = invokeCalculateRatio("100", "50");
    // 50/100 = 0.5
    assertTrue(result.startsWith("0.5"));
  }

  @Test
  public void testCalculateRatioReturnsZeroWhenTotalIsZero() throws Exception {
    String result = invokeCalculateRatio("0", "50");
    assertEquals("0", result);
  }

  @Test
  public void testCalculateRatioReturnsZeroWhenAmtIsZero() throws Exception {
    String result = invokeCalculateRatio("100", "0");
    assertEquals("0", result);
  }

  @Test
  public void testCalculateRatioReturnsZeroWhenTotalIsNull() throws Exception {
    String result = invokeCalculateRatio(null, "50");
    assertEquals("0", result);
  }

  @Test
  public void testCalculateRatioReturnsZeroWhenAmtIsNull() throws Exception {
    String result = invokeCalculateRatio("100", null);
    assertEquals("0", result);
  }

  @Test
  public void testCalculateRatioReturnsZeroWhenTotalIsEmpty() throws Exception {
    String result = invokeCalculateRatio("", "50");
    assertEquals("0", result);
  }

  @Test
  public void testCalculateRatioReturnsZeroWhenAmtIsEmpty() throws Exception {
    String result = invokeCalculateRatio("100", "");
    assertEquals("0", result);
  }

  @Test
  public void testCalculateRatioWithNegativeValues() throws Exception {
    String result = invokeCalculateRatio("100", "-50");
    // -50/100 = -0.5
    assertTrue(result.startsWith("-0.5"));
  }

  @Test
  public void testMultiplyNormalCase() throws Exception {
    String result = invokeMultiply("10", "5");
    // Result should contain 50 (with trailing zeros due to setScale(200))
    assertTrue(result.contains("50"));
  }

  @Test
  public void testMultiplyWithZero() throws Exception {
    String result = invokeMultiply("100", "0");
    // 100 * 0 = 0
    assertTrue(result.startsWith("0"));
  }

  @Test
  public void testMultiplyWithNegative() throws Exception {
    String result = invokeMultiply("10", "-3");
    assertTrue(result.contains("-30"));
  }

  @Test
  public void testMultiplyWithDecimals() throws Exception {
    String result = invokeMultiply("2.5", "4");
    // 2.5 * 4 = 10.0
    assertTrue(result.contains("10.0"));
  }

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet CreateCashFlowStatement", instance.getServletInfo());
  }

  private String invokeCalculateRatio(String strTotal, String strAmt) throws Exception {
    Method method = CreateCashFlowStatement.class.getDeclaredMethod("calculateRatio", String.class,
        String.class);
    method.setAccessible(true);
    return (String) method.invoke(instance, strTotal, strAmt);
  }

  private String invokeMultiply(String strOP1, String strOP2) throws Exception {
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
