package org.openbravo.base.model.domaintype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link AbsoluteTimeDomainType}.
 */
public class AbsoluteTimeDomainTypeTest {

  private AbsoluteTimeDomainType domainType;

  @Before
  public void setUp() {
    domainType = new AbsoluteTimeDomainType();
  }

  @Test
  public void testGetPrimitiveTypeReturnsTimestamp() {
    assertEquals(Timestamp.class, domainType.getPrimitiveType());
  }

  @Test
  public void testConvertToStringWithNullReturnsEmpty() {
    String result = domainType.convertToString(null);
    assertEquals("", result);
  }

  @Test
  public void testConvertToStringWithTimestamp() {
    Timestamp ts = Timestamp.valueOf("2024-01-15 10:30:45.123");
    String result = domainType.convertToString(ts);
    assertNotNull(result);
    assertTrue(result.contains("2024"));
    assertTrue(result.contains("10:30:45"));
  }

  @Test
  public void testCreateFromStringWithNull() {
    Object result = domainType.createFromString(null);
    assertNull(result);
  }

  @Test
  public void testCreateFromStringWithEmptyString() {
    Object result = domainType.createFromString("");
    assertNull(result);
  }

  @Test
  public void testCreateFromStringWithWhitespace() {
    Object result = domainType.createFromString("   ");
    assertNull(result);
  }

  @Test
  public void testCreateFromStringWithValidDate() {
    String dateStr = "2024-01-15T10:30:45.0";
    Object result = domainType.createFromString(dateStr);
    assertNotNull(result);
    assertTrue(result instanceof Timestamp);
  }

  @Test
  public void testRoundTripConversion() {
    Timestamp original = Timestamp.valueOf("2024-06-20 14:25:30.0");
    String strValue = domainType.convertToString(original);
    Object parsed = domainType.createFromString(strValue);
    assertNotNull(parsed);
    assertTrue(parsed instanceof Timestamp);
    Timestamp result = (Timestamp) parsed;
    assertEquals(original.getTime(), result.getTime());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateFromStringWithInvalidFormat() {
    domainType.createFromString("not-a-date");
  }

  @Test
  public void testGetXMLSchemaType() {
    assertEquals("ob:dateTime", domainType.getXMLSchemaType());
  }

  @Test
  public void testConvertToStringFormatsCorrectly() {
    SimpleDateFormat expectedFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
    Timestamp ts = Timestamp.valueOf("2024-03-10 08:15:00.0");
    String result = domainType.convertToString(ts);
    String expected = expectedFormat.format(ts);
    assertEquals(expected, result);
  }
}
