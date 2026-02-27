package org.openbravo.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.erpCommon.utility.OBError;

/**
 * Tests for {@link AuthenticationException}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthenticationExceptionTest {

  private static final String TEST_MESSAGE = "Authentication failed";
  /** Constructor with message. */

  @Test
  public void testConstructorWithMessage() {
    AuthenticationException exception = new AuthenticationException(TEST_MESSAGE);

    assertEquals(TEST_MESSAGE, exception.getMessage());
    assertNull(exception.getOBError());
  }
  /** Constructor with message and log flag. */

  @Test
  public void testConstructorWithMessageAndLogFlag() {
    AuthenticationException exception = new AuthenticationException(TEST_MESSAGE, Boolean.TRUE);

    assertEquals(TEST_MESSAGE, exception.getMessage());
    assertNull(exception.getOBError());
  }
  /** Constructor with message and cause. */

  @Test
  public void testConstructorWithMessageAndCause() {
    Throwable cause = new RuntimeException("root cause");
    AuthenticationException exception = new AuthenticationException(TEST_MESSAGE, cause);

    assertEquals(TEST_MESSAGE, exception.getMessage());
    assertNotNull(exception.getCause());
    assertNull(exception.getOBError());
  }
  /** Constructor with message and ob error. */

  @Test
  public void testConstructorWithMessageAndOBError() {
    OBError error = new OBError();
    error.setType("Error");
    error.setTitle("Test Error");

    AuthenticationException exception = new AuthenticationException(TEST_MESSAGE, error);

    assertEquals(TEST_MESSAGE, exception.getMessage());
    assertSame(error, exception.getOBError());
  }
  /** Constructor with message ob error and log flag. */

  @Test
  public void testConstructorWithMessageOBErrorAndLogFlag() {
    OBError error = new OBError();
    error.setType("Error");
    error.setTitle("Test Error");

    AuthenticationException exception = new AuthenticationException(TEST_MESSAGE, error,
        Boolean.FALSE);

    assertEquals(TEST_MESSAGE, exception.getMessage());
    assertSame(error, exception.getOBError());
  }
  /** Get ob error returns null when not set. */

  @Test
  public void testGetOBErrorReturnsNullWhenNotSet() {
    AuthenticationException exception = new AuthenticationException(TEST_MESSAGE);

    assertNull(exception.getOBError());
  }
  /** Is runtime exception. */

  @Test
  public void testIsRuntimeException() {
    AuthenticationException exception = new AuthenticationException(TEST_MESSAGE);

    assertNotNull(exception);
    assertEquals(true, exception instanceof RuntimeException);
  }
}
