package org.openbravo.base.secureApp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link DefaultValidationException}.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.class)
public class DefaultValidationExceptionTest {

  private static final String ERROR = "Error";
  private static final String FIELD = "field";
  /** Constructor sets message and field. */

  @Test
  public void testConstructorSetsMessageAndField() {
    DefaultValidationException exception = new DefaultValidationException("Invalid value",
        "roleId");

    assertEquals("Invalid value", exception.getMessage());
    assertEquals("roleId", exception.getDefaultField());
  }
  /** Get default field returns field name. */

  @Test
  public void testGetDefaultFieldReturnsFieldName() {
    DefaultValidationException exception = new DefaultValidationException(ERROR, "clientId");

    assertEquals("clientId", exception.getDefaultField());
  }
  /** Exception is instance of exception. */

  @Test
  public void testExceptionIsInstanceOfException() {
    DefaultValidationException exception = new DefaultValidationException("test", FIELD);
    assertNotNull(exception);
    assertEquals(Exception.class, exception.getClass().getSuperclass());
  }
  /** Constructor with null field. */

  @Test
  public void testConstructorWithNullField() {
    DefaultValidationException exception = new DefaultValidationException(ERROR, null);
    assertEquals(ERROR, exception.getMessage());
    assertEquals(null, exception.getDefaultField());
  }
  /** Constructor with empty message. */

  @Test
  public void testConstructorWithEmptyMessage() {
    DefaultValidationException exception = new DefaultValidationException("", FIELD);
    assertEquals("", exception.getMessage());
    assertEquals(FIELD, exception.getDefaultField());
  }
}
