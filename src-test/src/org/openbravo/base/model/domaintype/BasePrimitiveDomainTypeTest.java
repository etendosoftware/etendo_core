package org.openbravo.base.model.domaintype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.model.Property;
import org.openbravo.base.validation.ValidationException;

/**
 * Tests for {@link BasePrimitiveDomainType}.
 */
@RunWith(MockitoJUnitRunner.class)
public class BasePrimitiveDomainTypeTest {

  private static final String HELLO = "hello";

  private TestPrimitiveDomainType instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    instance = new TestPrimitiveDomainType();
  }
  /** Get hibernate type returns primitive type. */

  @Test
  public void testGetHibernateTypeReturnsPrimitiveType() {
    assertEquals(String.class, instance.getHibernateType());
  }
  /** Get format id returns null. */

  @Test
  public void testGetFormatIdReturnsNull() {
    assertNull(instance.getFormatId());
  }
  /** Convert to string returns value to string. */

  @Test
  public void testConvertToStringReturnsValueToString() {
    assertEquals(HELLO, instance.convertToString(HELLO));
  }
  /** Convert to string returns empty string for null. */

  @Test
  public void testConvertToStringReturnsEmptyStringForNull() {
    assertEquals("", instance.convertToString(null));
  }
  /** Convert to string with number. */

  @Test
  public void testConvertToStringWithNumber() {
    // Using Integer primitive type for this test
    TestIntegerDomainType intInstance = new TestIntegerDomainType();
    assertEquals("42", intInstance.convertToString(42));
  }
  /** Check is valid value accepts null. */

  @Test
  public void testCheckIsValidValueAcceptsNull() {
    Property prop = mock(Property.class);
    // Should not throw
    instance.checkIsValidValue(prop, null);
  }
  /** Check is valid value accepts correct type. */

  @Test
  public void testCheckIsValidValueAcceptsCorrectType() {
    Property prop = mock(Property.class);
    // Should not throw - String is instance of String
    instance.checkIsValidValue(prop, "validValue");
  }
  /** Check is valid value rejects wrong type. */

  @Test(expected = ValidationException.class)
  public void testCheckIsValidValueRejectsWrongType() {
    Property prop = mock(Property.class);
    // Integer is not an instance of String
    instance.checkIsValidValue(prop, 42);
  }
  /** Create from string returns null for null. */

  @Test
  public void testCreateFromStringReturnsNullForNull() {
    assertNull(instance.createFromString(null));
  }
  /** Create from string returns null for empty. */

  @Test
  public void testCreateFromStringReturnsNullForEmpty() {
    assertNull(instance.createFromString(""));
  }
  /** Create from string creates object. */

  @Test
  public void testCreateFromStringCreatesObject() {
    // String has a constructor that takes String
    Object result = instance.createFromString(HELLO);
    assertEquals(HELLO, result);
  }
  /** Create from string with integer. */

  @Test
  public void testCreateFromStringWithInteger() {
    TestIntegerDomainType intInstance = new TestIntegerDomainType();
    Object result = intInstance.createFromString("123");
    assertEquals(123, result);
  }
  /** Create from string throws for invalid value. */

  @Test(expected = IllegalArgumentException.class)
  public void testCreateFromStringThrowsForInvalidValue() {
    TestIntegerDomainType intInstance = new TestIntegerDomainType();
    intInstance.createFromString("not_a_number");
  }

  /**
   * Concrete subclass returning String as primitive type.
   */
  private static class TestPrimitiveDomainType extends BasePrimitiveDomainType {
    /** Get primitive type. */
    @Override
    public Class<?> getPrimitiveType() {
      return String.class;
    }
    /** Get xml schema type. */

    @Override
    public String getXMLSchemaType() {
      return "string";
    }
  }

  /**
   * Concrete subclass returning Integer as primitive type.
   */
  private static class TestIntegerDomainType extends BasePrimitiveDomainType {
    /** Get primitive type. */
    @Override
    public Class<?> getPrimitiveType() {
      return Integer.class;
    }
    /** Get xml schema type. */

    @Override
    public String getXMLSchemaType() {
      return "integer";
    }
  }
}
