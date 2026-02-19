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

  private TestPrimitiveDomainType instance;

  @Before
  public void setUp() {
    instance = new TestPrimitiveDomainType();
  }

  @Test
  public void testGetHibernateTypeReturnsPrimitiveType() {
    assertEquals(String.class, instance.getHibernateType());
  }

  @Test
  public void testGetFormatIdReturnsNull() {
    assertNull(instance.getFormatId());
  }

  @Test
  public void testConvertToStringReturnsValueToString() {
    assertEquals("hello", instance.convertToString("hello"));
  }

  @Test
  public void testConvertToStringReturnsEmptyStringForNull() {
    assertEquals("", instance.convertToString(null));
  }

  @Test
  public void testConvertToStringWithNumber() {
    // Using Integer primitive type for this test
    TestIntegerDomainType intInstance = new TestIntegerDomainType();
    assertEquals("42", intInstance.convertToString(42));
  }

  @Test
  public void testCheckIsValidValueAcceptsNull() {
    Property prop = mock(Property.class);
    // Should not throw
    instance.checkIsValidValue(prop, null);
  }

  @Test
  public void testCheckIsValidValueAcceptsCorrectType() {
    Property prop = mock(Property.class);
    // Should not throw - String is instance of String
    instance.checkIsValidValue(prop, "validValue");
  }

  @Test(expected = ValidationException.class)
  public void testCheckIsValidValueRejectsWrongType() {
    Property prop = mock(Property.class);
    // Integer is not an instance of String
    instance.checkIsValidValue(prop, 42);
  }

  @Test
  public void testCreateFromStringReturnsNullForNull() {
    assertNull(instance.createFromString(null));
  }

  @Test
  public void testCreateFromStringReturnsNullForEmpty() {
    assertNull(instance.createFromString(""));
  }

  @Test
  public void testCreateFromStringCreatesObject() {
    // String has a constructor that takes String
    Object result = instance.createFromString("hello");
    assertEquals("hello", result);
  }

  @Test
  public void testCreateFromStringWithInteger() {
    TestIntegerDomainType intInstance = new TestIntegerDomainType();
    Object result = intInstance.createFromString("123");
    assertEquals(123, result);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateFromStringThrowsForInvalidValue() {
    TestIntegerDomainType intInstance = new TestIntegerDomainType();
    intInstance.createFromString("not_a_number");
  }

  /**
   * Concrete subclass returning String as primitive type.
   */
  private static class TestPrimitiveDomainType extends BasePrimitiveDomainType {
    @Override
    public Class<?> getPrimitiveType() {
      return String.class;
    }

    @Override
    public String getXMLSchemaType() {
      return "string";
    }
  }

  /**
   * Concrete subclass returning Integer as primitive type.
   */
  private static class TestIntegerDomainType extends BasePrimitiveDomainType {
    @Override
    public Class<?> getPrimitiveType() {
      return Integer.class;
    }

    @Override
    public String getXMLSchemaType() {
      return "integer";
    }
  }
}
