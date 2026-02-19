package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

/**
 * Tests for {@link DocCash}.
 * Tests the public nextSeqNo, getBalance, and SeqNo getter/setter methods.
 */
@RunWith(MockitoJUnitRunner.class)
public class DocCashTest {

  private DocCash instance;

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(DocCash.class);
    // Initialize SeqNo field (defaults to "0" in constructor, but we bypass it)
    Field seqNoField = DocCash.class.getDeclaredField("SeqNo");
    seqNoField.setAccessible(true);
    seqNoField.set(instance, "0");
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
  public void testNextSeqNoFromLargeNumber() {
    String result = instance.nextSeqNo("990");
    assertEquals("1000", result);
  }

  @Test
  public void testNextSeqNoUpdatesInternalSeqNo() {
    instance.nextSeqNo("0");
    assertEquals("10", instance.getSeqNo());
  }

  @Test
  public void testNextSeqNoSequentialCalls() {
    instance.nextSeqNo("0");
    String second = instance.nextSeqNo(instance.getSeqNo());
    assertEquals("20", second);
  }

  // --- Tests for getSeqNo / setSeqNo ---

  @Test
  public void testGetSeqNoReturnsInitialValue() {
    assertEquals("0", instance.getSeqNo());
  }

  @Test
  public void testSetSeqNoUpdatesValue() {
    instance.setSeqNo("50");
    assertEquals("50", instance.getSeqNo());
  }

  // --- Tests for getDocumentConfirmation ---

  @Test
  public void testGetDocumentConfirmationReturnsTrue() {
    assertTrue(instance.getDocumentConfirmation(null, "anyId"));
  }

  // --- Tests for getServletInfo ---

  @Test
  public void testGetServletInfoReturnsExpectedString() {
    assertEquals("Servlet for the accounting", instance.getServletInfo());
  }

  // --- Tests for getSerialVersionUID ---

  @Test
  public void testGetSerialVersionUIDReturnsOne() {
    assertEquals(1L, DocCash.getSerialVersionUID());
  }

  private Field findField(Class<?> clazz, String fieldName) {
    Class<?> current = clazz;
    while (current != null) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new RuntimeException("Field not found: " + fieldName);
  }
}
