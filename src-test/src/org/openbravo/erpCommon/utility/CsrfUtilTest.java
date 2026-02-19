package org.openbravo.erpCommon.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.client.kernel.OBUserException;

/**
 * Tests for {@link CsrfUtil}.
 * Tests the public static methods: hasValidCsrfToken, getCsrfTokenFromRequestContent,
 * and checkCsrfToken.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.class)
public class CsrfUtilTest {

  private static final String ABC123 = "ABC123";
  private static final String CSRF_TOKEN = "#CSRF_TOKEN";
  private static final String TEST_URI = "/test/uri";
  private static final String SESSION_ID = "session-id";

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private HttpSession mockSession;

  // --- Tests for hasValidCsrfToken ---
  /** Has valid csrf token returns true when tokens match. */

  @Test
  public void testHasValidCsrfTokenReturnsTrueWhenTokensMatch() {
    assertTrue(CsrfUtil.hasValidCsrfToken(ABC123, ABC123));
  }
  /** Has valid csrf token returns false when tokens dont match. */

  @Test
  public void testHasValidCsrfTokenReturnsFalseWhenTokensDontMatch() {
    assertFalse(CsrfUtil.hasValidCsrfToken(ABC123, "DEF456"));
  }
  /** Has valid csrf token returns false when request token is null. */

  @Test
  public void testHasValidCsrfTokenReturnsFalseWhenRequestTokenIsNull() {
    assertFalse(CsrfUtil.hasValidCsrfToken(null, ABC123));
  }
  /** Has valid csrf token returns false when session token is null. */

  @Test
  public void testHasValidCsrfTokenReturnsFalseWhenSessionTokenIsNull() {
    assertFalse(CsrfUtil.hasValidCsrfToken(ABC123, null));
  }
  /** Has valid csrf token returns false when both tokens null. */

  @Test
  public void testHasValidCsrfTokenReturnsFalseWhenBothTokensNull() {
    assertFalse(CsrfUtil.hasValidCsrfToken(null, null));
  }
  /** Has valid csrf token returns false when request token is empty. */

  @Test
  public void testHasValidCsrfTokenReturnsFalseWhenRequestTokenIsEmpty() {
    assertFalse(CsrfUtil.hasValidCsrfToken("", ABC123));
  }
  /** Has valid csrf token returns false when session token is empty. */

  @Test
  public void testHasValidCsrfTokenReturnsFalseWhenSessionTokenIsEmpty() {
    assertFalse(CsrfUtil.hasValidCsrfToken(ABC123, ""));
  }
  /** Has valid csrf token returns false when both tokens empty. */

  @Test
  public void testHasValidCsrfTokenReturnsFalseWhenBothTokensEmpty() {
    assertFalse(CsrfUtil.hasValidCsrfToken("", ""));
  }

  // --- Tests for getCsrfTokenFromRequestContent ---
  /** Get csrf token from request content extracts token. */

  @Test
  public void testGetCsrfTokenFromRequestContentExtractsToken() {
    String content = "{\"csrfToken\":\"ABCDEF123456\",\"otherField\":\"value\"}";
    assertEquals("ABCDEF123456", CsrfUtil.getCsrfTokenFromRequestContent(content));
  }
  /** Get csrf token from request content returns empty when no token. */

  @Test
  public void testGetCsrfTokenFromRequestContentReturnsEmptyWhenNoToken() {
    String content = "{\"otherField\":\"value\"}";
    assertEquals("", CsrfUtil.getCsrfTokenFromRequestContent(content));
  }
  /** Get csrf token from request content returns empty for empty content. */

  @Test
  public void testGetCsrfTokenFromRequestContentReturnsEmptyForEmptyContent() {
    assertEquals("", CsrfUtil.getCsrfTokenFromRequestContent(""));
  }
  /** Get csrf token from request content extracts uppercase alphanumeric token. */

  @Test
  public void testGetCsrfTokenFromRequestContentExtractsUppercaseAlphanumericToken() {
    String content = "{\"csrfToken\":\"A1B2C3D4E5\"}";
    assertEquals("A1B2C3D4E5", CsrfUtil.getCsrfTokenFromRequestContent(content));
  }
  /** Get csrf token from request content returns empty for lowercase token. */

  @Test
  public void testGetCsrfTokenFromRequestContentReturnsEmptyForLowercaseToken() {
    // Pattern only matches uppercase letters and digits
    String content = "{\"csrfToken\":\"abcdef123\"}";
    assertEquals("", CsrfUtil.getCsrfTokenFromRequestContent(content));
  }
  /** Get csrf token from request content returns empty for empty token value. */

  @Test
  public void testGetCsrfTokenFromRequestContentReturnsEmptyForEmptyTokenValue() {
    String content = "{\"csrfToken\":\"\"}";
    assertEquals("", CsrfUtil.getCsrfTokenFromRequestContent(content));
  }

  // --- Tests for checkCsrfToken ---
  /** Check csrf token succeeds with valid tokens. */

  @Test
  public void testCheckCsrfTokenSucceedsWithValidTokens() {
    // Arrange
    when(mockRequest.getSession(false)).thenReturn(mockSession);
    when(mockSession.getAttribute(CSRF_TOKEN)).thenReturn("VALIDTOKEN123");

    // Act - should not throw
    CsrfUtil.checkCsrfToken("VALIDTOKEN123", mockRequest);
  }
  /** Check csrf token throws when tokens dont match. */

  @Test(expected = OBUserException.class)
  public void testCheckCsrfTokenThrowsWhenTokensDontMatch() {
    // Arrange
    when(mockRequest.getSession(false)).thenReturn(mockSession);
    when(mockSession.getAttribute(CSRF_TOKEN)).thenReturn("SESSIONTOKEN");
    when(mockRequest.getRequestURI()).thenReturn(TEST_URI);
    when(mockSession.getId()).thenReturn(SESSION_ID);

    // Act
    CsrfUtil.checkCsrfToken("WRONG_TOKEN", mockRequest);
  }
  /** Check csrf token throws when request token is null. */

  @Test(expected = OBUserException.class)
  public void testCheckCsrfTokenThrowsWhenRequestTokenIsNull() {
    // Arrange
    when(mockRequest.getSession(false)).thenReturn(mockSession);
    when(mockSession.getAttribute(CSRF_TOKEN)).thenReturn("SESSIONTOKEN");
    when(mockRequest.getRequestURI()).thenReturn(TEST_URI);
    when(mockSession.getId()).thenReturn(SESSION_ID);

    // Act
    CsrfUtil.checkCsrfToken(null, mockRequest);
  }
  /** Check csrf token throws when session token is null. */

  @Test(expected = OBUserException.class)
  public void testCheckCsrfTokenThrowsWhenSessionTokenIsNull() {
    // Arrange
    when(mockRequest.getSession(false)).thenReturn(mockSession);
    when(mockSession.getAttribute(CSRF_TOKEN)).thenReturn(null);
    when(mockRequest.getRequestURI()).thenReturn(TEST_URI);
    when(mockSession.getId()).thenReturn(SESSION_ID);

    // Act
    CsrfUtil.checkCsrfToken("REQUESTTOKEN", mockRequest);
  }
}
