package org.openbravo.erpCommon.utility.poc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EmailAddressTypeTest {

  @Test
  public void testUndefinedValue() {
    assertEquals(0, EmailAddressType.UNDEFINED.getValue());
  }

  @Test
  public void testFromValue() {
    assertEquals(1, EmailAddressType.FROM.getValue());
  }

  @Test
  public void testToValue() {
    assertEquals(2, EmailAddressType.TO.getValue());
  }

  @Test
  public void testCcValue() {
    assertEquals(3, EmailAddressType.CC.getValue());
  }

  @Test
  public void testBccValue() {
    assertEquals(4, EmailAddressType.BCC.getValue());
  }

  @Test
  public void testGetStringValueUndefined() {
    assertEquals("0", EmailAddressType.UNDEFINED.getStringValue());
  }

  @Test
  public void testGetStringValueFrom() {
    assertEquals("1", EmailAddressType.FROM.getStringValue());
  }

  @Test
  public void testGetStringValueTo() {
    assertEquals("2", EmailAddressType.TO.getStringValue());
  }

  @Test
  public void testGetStringValueCc() {
    assertEquals("3", EmailAddressType.CC.getStringValue());
  }

  @Test
  public void testGetStringValueBcc() {
    assertEquals("4", EmailAddressType.BCC.getStringValue());
  }

  @Test
  public void testEnumValuesCount() {
    assertEquals(5, EmailAddressType.values().length);
  }

  @Test
  public void testValueOf() {
    assertEquals(EmailAddressType.FROM, EmailAddressType.valueOf("FROM"));
    assertEquals(EmailAddressType.TO, EmailAddressType.valueOf("TO"));
    assertEquals(EmailAddressType.BCC, EmailAddressType.valueOf("BCC"));
  }
}
