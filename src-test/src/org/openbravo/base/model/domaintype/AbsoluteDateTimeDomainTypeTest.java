package org.openbravo.base.model.domaintype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for AbsoluteDateTimeDomainType.
 */
@RunWith(MockitoJUnitRunner.class)
public class AbsoluteDateTimeDomainTypeTest {

  private AbsoluteDateTimeDomainType domainType;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    domainType = new AbsoluteDateTimeDomainType();
  }
  /** Get primitive type returns date. */

  @Test
  public void testGetPrimitiveTypeReturnsDate() {
    assertEquals(Date.class, domainType.getPrimitiveType());
  }
  /** Convert to string with null returns empty string. */

  @Test
  public void testConvertToStringWithNullReturnsEmptyString() {
    String result = domainType.convertToString(null);
    assertEquals("", result);
  }
  /** Convert to string formats date. */

  @Test
  public void testConvertToStringFormatsDate() {
    // Arrange
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
    Date date = new Date(0); // epoch

    // Act
    String result = domainType.convertToString(date);

    // Assert
    String expected = sdf.format(date);
    assertEquals(expected, result);
  }
  /** Create from string with null. */

  @Test
  public void testCreateFromStringWithNull() {
    Object result = domainType.createFromString(null);
    assertNull(result);
  }
  /** Create from string with empty string. */

  @Test
  public void testCreateFromStringWithEmptyString() {
    Object result = domainType.createFromString("");
    assertNull(result);
  }
  /** Create from string with whitespace. */

  @Test
  public void testCreateFromStringWithWhitespace() {
    Object result = domainType.createFromString("   ");
    assertNull(result);
  }
  /** Create from string parses valid date. */

  @Test
  public void testCreateFromStringParsesValidDate() {
    // Arrange
    String dateStr = "2024-01-15T10:30:00.0";

    // Act
    Object result = domainType.createFromString(dateStr);

    // Assert
    assertNotNull(result);
    assertTrue(result instanceof Date);
  }
  /** Convert to string and create from string round trip. */

  @Test
  public void testConvertToStringAndCreateFromStringRoundTrip() {
    // Arrange
    Date original = new Date();

    // Act
    String str = domainType.convertToString(original);
    Date parsed = (Date) domainType.createFromString(str);

    // Assert
    assertNotNull(parsed);
    // Millisecond precision may vary, so compare the string representations
    assertEquals(str, domainType.convertToString(parsed));
  }
  /** Create from string with invalid date throws exception. */

  @Test(expected = IllegalArgumentException.class)
  public void testCreateFromStringWithInvalidDateThrowsException() {
    domainType.createFromString("not-a-date");
  }
  /** Get xml schema type. */

  @Test
  public void testGetXMLSchemaType() {
    assertEquals("ob:dateTime", domainType.getXMLSchemaType());
  }
}
