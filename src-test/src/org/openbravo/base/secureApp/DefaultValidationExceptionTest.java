package org.openbravo.base.secureApp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link DefaultValidationException}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultValidationExceptionTest {

  @Test
  public void testConstructorSetsMessageAndField() {
    DefaultValidationException exception = new DefaultValidationException("Invalid value",
        "roleId");

    assertEquals("Invalid value", exception.getMessage());
    assertEquals("roleId", exception.getDefaultField());
  }

  @Test
  public void testGetDefaultFieldReturnsFieldName() {
    DefaultValidationException exception = new DefaultValidationException("Error", "clientId");

    assertEquals("clientId", exception.getDefaultField());
  }

  @Test
  public void testExceptionIsInstanceOfException() {
    DefaultValidationException exception = new DefaultValidationException("test", "field");
    assertNotNull(exception);
    assertEquals(Exception.class, exception.getClass().getSuperclass());
  }

  @Test
  public void testConstructorWithNullField() {
    DefaultValidationException exception = new DefaultValidationException("Error", null);
    assertEquals("Error", exception.getMessage());
    assertEquals(null, exception.getDefaultField());
  }

  @Test
  public void testConstructorWithEmptyMessage() {
    DefaultValidationException exception = new DefaultValidationException("", "field");
    assertEquals("", exception.getMessage());
    assertEquals("field", exception.getDefaultField());
  }
}
