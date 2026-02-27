package org.openbravo.erpCommon.utility.poc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
/** Tests for {@link EmailAddressType}. */
@SuppressWarnings({"java:S120"})

@RunWith(MockitoJUnitRunner.class)
public class EmailAddressTypeTest {
  /** Undefined value. */

  @Test
  public void testUndefinedValue() {
    assertEquals(0, EmailAddressType.UNDEFINED.getValue());
  }
  /** From value. */

  @Test
  public void testFromValue() {
    assertEquals(1, EmailAddressType.FROM.getValue());
  }
  /** To value. */

  @Test
  public void testToValue() {
    assertEquals(2, EmailAddressType.TO.getValue());
  }
  /** Cc value. */

  @Test
  public void testCcValue() {
    assertEquals(3, EmailAddressType.CC.getValue());
  }
  /** Bcc value. */

  @Test
  public void testBccValue() {
    assertEquals(4, EmailAddressType.BCC.getValue());
  }
  /** Get string value undefined. */

  @Test
  public void testGetStringValueUndefined() {
    assertEquals("0", EmailAddressType.UNDEFINED.getStringValue());
  }
  /** Get string value from. */

  @Test
  public void testGetStringValueFrom() {
    assertEquals("1", EmailAddressType.FROM.getStringValue());
  }
  /** Get string value to. */

  @Test
  public void testGetStringValueTo() {
    assertEquals("2", EmailAddressType.TO.getStringValue());
  }
  /** Get string value cc. */

  @Test
  public void testGetStringValueCc() {
    assertEquals("3", EmailAddressType.CC.getStringValue());
  }
  /** Get string value bcc. */

  @Test
  public void testGetStringValueBcc() {
    assertEquals("4", EmailAddressType.BCC.getStringValue());
  }
  /** Enum values count. */

  @Test
  public void testEnumValuesCount() {
    assertEquals(5, EmailAddressType.values().length);
  }
  /** Value of. */

  @Test
  public void testValueOf() {
    assertEquals(EmailAddressType.FROM, EmailAddressType.valueOf("FROM"));
    assertEquals(EmailAddressType.TO, EmailAddressType.valueOf("TO"));
    assertEquals(EmailAddressType.BCC, EmailAddressType.valueOf("BCC"));
  }
}
