package org.openbravo.erpCommon.utility.poc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for {@link EmailType}.
 */
public class EmailTypeTest {

  @Test
  public void testUndefinedValue() {
    assertEquals(0, EmailType.UNDEFINED.getValue());
  }

  @Test
  public void testIncommingValue() {
    assertEquals(1, EmailType.INCOMMING.getValue());
  }

  @Test
  public void testOutgoingValue() {
    assertEquals(2, EmailType.OUTGOING.getValue());
  }

  @Test
  public void testUndefinedStringValue() {
    assertEquals("0", EmailType.UNDEFINED.getStringValue());
  }

  @Test
  public void testIncommingStringValue() {
    assertEquals("1", EmailType.INCOMMING.getStringValue());
  }

  @Test
  public void testOutgoingStringValue() {
    assertEquals("2", EmailType.OUTGOING.getStringValue());
  }

  @Test
  public void testEnumValuesCount() {
    assertEquals(3, EmailType.values().length);
  }

  @Test
  public void testValueOf() {
    assertEquals(EmailType.OUTGOING, EmailType.valueOf("OUTGOING"));
  }
}
