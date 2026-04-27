package org.openbravo.erpCommon.utility.poc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for {@link EmailType}.
 */
@SuppressWarnings({"java:S120"})
public class EmailTypeTest {
  /** Undefined value. */

  @Test
  public void testUndefinedValue() {
    assertEquals(0, EmailType.UNDEFINED.getValue());
  }
  /** Incomming value. */

  @Test
  public void testIncommingValue() {
    assertEquals(1, EmailType.INCOMMING.getValue());
  }
  /** Outgoing value. */

  @Test
  public void testOutgoingValue() {
    assertEquals(2, EmailType.OUTGOING.getValue());
  }
  /** Undefined string value. */

  @Test
  public void testUndefinedStringValue() {
    assertEquals("0", EmailType.UNDEFINED.getStringValue());
  }
  /** Incomming string value. */

  @Test
  public void testIncommingStringValue() {
    assertEquals("1", EmailType.INCOMMING.getStringValue());
  }
  /** Outgoing string value. */

  @Test
  public void testOutgoingStringValue() {
    assertEquals("2", EmailType.OUTGOING.getStringValue());
  }
  /** Enum values count. */

  @Test
  public void testEnumValuesCount() {
    assertEquals(3, EmailType.values().length);
  }
  /** Value of. */

  @Test
  public void testValueOf() {
    assertEquals(EmailType.OUTGOING, EmailType.valueOf("OUTGOING"));
  }
}
