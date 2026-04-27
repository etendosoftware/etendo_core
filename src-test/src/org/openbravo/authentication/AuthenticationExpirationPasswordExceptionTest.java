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
 * Tests for {@link AuthenticationExpirationPasswordException}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthenticationExpirationPasswordExceptionTest {

  private static final String TEST_MESSAGE = "Password has expired";
  /** Constructor with message. */

  @Test
  public void testConstructorWithMessage() {
    AuthenticationExpirationPasswordException exception =
        new AuthenticationExpirationPasswordException(TEST_MESSAGE);

    assertEquals(TEST_MESSAGE, exception.getMessage());
    assertNull(exception.getOBError());
  }
  /** Constructor with message and ob error. */

  @Test
  public void testConstructorWithMessageAndOBError() {
    OBError error = new OBError();
    error.setType("Error");
    error.setTitle("Password Expired");

    AuthenticationExpirationPasswordException exception =
        new AuthenticationExpirationPasswordException(TEST_MESSAGE, error);

    assertEquals(TEST_MESSAGE, exception.getMessage());
    assertSame(error, exception.getOBError());
  }
  /** Constructor with message and cause. */

  @Test
  public void testConstructorWithMessageAndCause() {
    Throwable cause = new RuntimeException("underlying cause");

    AuthenticationExpirationPasswordException exception =
        new AuthenticationExpirationPasswordException(TEST_MESSAGE, cause);

    assertEquals(TEST_MESSAGE, exception.getMessage());
    assertNotNull(exception.getCause());
    assertNull(exception.getOBError());
  }
  /** Extends authentication exception. */

  @Test
  public void testExtendsAuthenticationException() {
    AuthenticationExpirationPasswordException exception =
        new AuthenticationExpirationPasswordException(TEST_MESSAGE);

    assertEquals(true, exception instanceof AuthenticationException);
  }
  /** Constructor with ob error and password expiration. */

  @Test
  public void testConstructorWithOBErrorAndPasswordExpiration() {
    OBError error = new OBError();
    error.setType("Error");
    error.setTitle("Password Expired Title");

    AuthenticationExpirationPasswordException exception =
        new AuthenticationExpirationPasswordException(TEST_MESSAGE, error, true);

    assertEquals(TEST_MESSAGE, exception.getMessage());
    assertSame(error, exception.getOBError());
  }
}
