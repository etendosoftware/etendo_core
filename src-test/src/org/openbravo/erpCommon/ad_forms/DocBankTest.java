/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

/**
 * Tests for {@link DocBank}.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.class)
public class DocBankTest {

  private DocBank instance;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(DocBank.class);

    setFieldValue(instance, "ZERO", BigDecimal.ZERO);
    setFieldValue(instance, "SeqNo", "0");

    String[] amounts = new String[10];
    for (int i = 0; i < amounts.length; i++) {
      amounts[i] = "0";
    }
    setFieldValue(instance, "Amounts", amounts);
    setFieldValue(instance, "p_lines", new DocLine[0]);
  }
  /** Next seq no increments by10. */

  @Test
  public void testNextSeqNoIncrementsBy10() {
    String result = instance.nextSeqNo("0");
    assertEquals("10", result);
  }
  /** Next seq no from non zero. */

  @Test
  public void testNextSeqNoFromNonZero() {
    String result = instance.nextSeqNo("30");
    assertEquals("40", result);
  }
  /** Next seq no updates internal seq no. */

  @Test
  public void testNextSeqNoUpdatesInternalSeqNo() {
    instance.nextSeqNo("0");
    assertEquals("10", instance.getSeqNo());
  }
  /** Get set seq no. */

  @Test
  public void testGetSetSeqNo() {
    instance.setSeqNo("100");
    assertEquals("100", instance.getSeqNo());
  }
  /** Get balance with no lines. */

  @Test
  public void testGetBalanceWithNoLines() {
    BigDecimal balance = instance.getBalance();
    assertEquals(0, BigDecimal.ZERO.compareTo(balance));
  }
  /** Get servlet info. */

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet for the accounting", instance.getServletInfo());
  }
  /** Get serial version uid. */

  @Test
  public void testGetSerialVersionUID() {
    assertEquals(1L, DocBank.getSerialVersionUID());
  }

  private void setFieldValue(Object target, String fieldName, Object value) throws Exception {
    Field field = findField(target.getClass(), fieldName);
    field.setAccessible(true);
    field.set(target, value);
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
