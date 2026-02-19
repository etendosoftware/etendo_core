package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link DocDoubtfulDebt}.
 * Tests the public nextSeqNo, getBalance, and getDocumentConfirmation methods.
 */
@RunWith(MockitoJUnitRunner.class)
public class DocDoubtfulDebtTest {

  private DocDoubtfulDebt instance;

  @Before
  public void setUp() {
    // DocDoubtfulDebt has a no-arg constructor
    instance = new DocDoubtfulDebt();
  }

  // --- Tests for nextSeqNo ---

  @Test
  public void testNextSeqNoFromZero() {
    String result = instance.nextSeqNo("0");
    assertEquals("10", result);
  }

  @Test
  public void testNextSeqNoFromTen() {
    String result = instance.nextSeqNo("10");
    assertEquals("20", result);
  }

  @Test
  public void testNextSeqNoFromHundred() {
    String result = instance.nextSeqNo("100");
    assertEquals("110", result);
  }

  @Test
  public void testNextSeqNoFromLargeNumber() {
    String result = instance.nextSeqNo("990");
    assertEquals("1000", result);
  }

  @Test
  public void testNextSeqNoSequentialCalls() {
    String first = instance.nextSeqNo("0");
    assertEquals("10", first);
    String second = instance.nextSeqNo(first);
    assertEquals("20", second);
    String third = instance.nextSeqNo(second);
    assertEquals("30", third);
  }

  // --- Tests for getBalance ---

  @Test
  public void testGetBalanceReturnsZero() {
    BigDecimal result = instance.getBalance();
    assertTrue(BigDecimal.ZERO.compareTo(result) == 0);
  }

  // --- Tests for getDocumentConfirmation ---

  @Test
  public void testGetDocumentConfirmationReturnsTrue() {
    assertTrue(instance.getDocumentConfirmation(null, "anyId"));
  }

  @Test
  public void testGetDocumentConfirmationReturnsTrueWithNullId() {
    assertTrue(instance.getDocumentConfirmation(null, null));
  }
}
