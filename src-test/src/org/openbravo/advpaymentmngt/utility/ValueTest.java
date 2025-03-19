package org.openbravo.advpaymentmngt.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.openbravo.advpaymentmngt.TestConstants;

/**
 * Test cases for the Value utility class.
 */
public class ValueTest {

  /**
   * Tests the constructor with two parameters.
   * Verifies that the field and value are set correctly and the default operator is '=='.
   */
  @Test
  public void testConstructorWithTwoParameters() {
    // GIVEN
    String field = "testField";
    String testValue = "testValue";

    // WHEN
    Value value = new Value(field, testValue);

    // THEN
    assertNotNull(TestConstants.VALUE_INSTANCE_NOT_NULL, value);
    assertEquals(TestConstants.FIELD_MATCH_CONSTRUCTOR_PARAMETER, field, value.getField());
    assertEquals(TestConstants.VALUE_MATCH_CONSTRUCTOR_PARAMETER, testValue, value.getValue());
    assertEquals("Default operator should be '=='", "==", value.getOperator());
  }

  /**
   * Tests the constructor with three parameters.
   * Verifies that the field, value, and operator are set correctly.
   */
  @Test
  public void testConstructorWithThreeParameters() {
    // GIVEN
    String field = "testField";
    Integer testValue = 100;
    String operator = ">=";

    // WHEN
    Value value = new Value(field, testValue, operator);

    // THEN
    assertNotNull(TestConstants.VALUE_INSTANCE_NOT_NULL, value);
    assertEquals(TestConstants.FIELD_MATCH_CONSTRUCTOR_PARAMETER, field, value.getField());
    assertEquals(TestConstants.VALUE_MATCH_CONSTRUCTOR_PARAMETER, testValue, value.getValue());
    assertEquals("Operator should match the constructor parameter", operator, value.getOperator());
  }

  /**
   * Tests the constructor with null values.
   * Verifies that the field, value, and operator are set to null.
   */
  @Test
  public void testConstructorWithNullValues() {
    // WHEN
    Value value = new Value(null, null, null);

    // THEN
    assertNotNull(TestConstants.VALUE_INSTANCE_NOT_NULL, value);
    assertNull("Field should be null", value.getField());
    assertNull("Value should be null", value.getValue());
    assertNull("Operator should be null", value.getOperator());
  }

  /**
   * Tests the constructor with different value types.
   * Verifies that the field, value, and operator are set correctly for different value types.
   */
  @Test
  public void testDifferentValueTypes() {
    // GIVEN
    String field = "amountField";
    Double amount = 150.75;
    String operator = ">";

    // WHEN
    Value value = new Value(field, amount, operator);

    // THEN
    assertNotNull(TestConstants.VALUE_INSTANCE_NOT_NULL, value);
    assertEquals(TestConstants.FIELD_MATCH_CONSTRUCTOR_PARAMETER, field, value.getField());
    assertEquals(TestConstants.VALUE_MATCH_CONSTRUCTOR_PARAMETER, amount, value.getValue());
    assertEquals("Operator should match the constructor parameter", operator, value.getOperator());
  }
}
