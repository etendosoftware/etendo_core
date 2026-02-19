/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

/**
 * Tests for {@link DocAmortization}.
 */
@SuppressWarnings({"java:S100", "java:S120"})
@RunWith(MockitoJUnitRunner.class)
public class DocAmortizationTest {

  private DocAmortization instance;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(DocAmortization.class);

    setFieldValue(instance, "ZERO", BigDecimal.ZERO);
    setFieldValue(instance, "SeqNo", "0");
  }
  /** Get balance returns zero. */

  @Test
  public void testGetBalanceReturnsZero() {
    BigDecimal balance = instance.getBalance();
    assertEquals(BigDecimal.ZERO, balance);
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
    String result = instance.nextSeqNo("10");
    assertEquals("20", result);
  }
  /** Next seq no updates internal seq no. */

  @Test
  public void testNextSeqNoUpdatesInternalSeqNo() {
    instance.nextSeqNo("0");
    assertEquals("10", instance.getSeqNo());
  }
  /** Next seq no multiple calls. */

  @Test
  public void testNextSeqNoMultipleCalls() {
    instance.nextSeqNo("0");
    String second = instance.nextSeqNo(instance.getSeqNo());
    assertEquals("20", second);
  }
  /** Get set seq no. */

  @Test
  public void testGetSetSeqNo() {
    instance.setSeqNo("50");
    assertEquals("50", instance.getSeqNo());
  }
  /** Get accttype_depreciation. */

  @Test
  public void testGetACCTTYPE_Depreciation() {
    assertEquals("1", DocAmortization.getACCTTYPE_Depreciation());
  }
  /** Get accttype_accum depreciation. */

  @Test
  public void testGetACCTTYPE_AccumDepreciation() {
    assertEquals("2", DocAmortization.getACCTTYPE_AccumDepreciation());
  }
  /** Get document confirmation returns true. */

  @Test
  public void testGetDocumentConfirmationReturnsTrue() {
    assertTrue(instance.getDocumentConfirmation(null, null));
  }
  /** Get servlet info. */

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet for the accounting", instance.getServletInfo());
  }

  @SuppressWarnings("java:S112")
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
