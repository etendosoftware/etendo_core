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
@RunWith(MockitoJUnitRunner.class)
public class DocAmortizationTest {

  private DocAmortization instance;

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(DocAmortization.class);

    // Set ZERO field from AcctServer parent
    Field zeroField = findField(instance.getClass(), "ZERO");
    zeroField.setAccessible(true);
    zeroField.set(instance, BigDecimal.ZERO);

    // Set SeqNo field
    Field seqNoField = DocAmortization.class.getDeclaredField("SeqNo");
    seqNoField.setAccessible(true);
    seqNoField.set(instance, "0");
  }

  @Test
  public void testGetBalanceReturnsZero() {
    BigDecimal balance = instance.getBalance();
    assertEquals(BigDecimal.ZERO, balance);
  }

  @Test
  public void testNextSeqNoIncrementsBy10() {
    String result = instance.nextSeqNo("0");
    assertEquals("10", result);
  }

  @Test
  public void testNextSeqNoFromNonZero() {
    String result = instance.nextSeqNo("10");
    assertEquals("20", result);
  }

  @Test
  public void testNextSeqNoUpdatesInternalSeqNo() {
    instance.nextSeqNo("0");
    assertEquals("10", instance.getSeqNo());
  }

  @Test
  public void testNextSeqNoMultipleCalls() {
    instance.nextSeqNo("0");
    String second = instance.nextSeqNo(instance.getSeqNo());
    assertEquals("20", second);
  }

  @Test
  public void testGetSetSeqNo() {
    instance.setSeqNo("50");
    assertEquals("50", instance.getSeqNo());
  }

  @Test
  public void testGetACCTTYPE_Depreciation() {
    assertEquals("1", DocAmortization.getACCTTYPE_Depreciation());
  }

  @Test
  public void testGetACCTTYPE_AccumDepreciation() {
    assertEquals("2", DocAmortization.getACCTTYPE_AccumDepreciation());
  }

  @Test
  public void testGetDocumentConfirmationReturnsTrue() {
    assertTrue(instance.getDocumentConfirmation(null, null));
  }

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet for the accounting", instance.getServletInfo());
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
