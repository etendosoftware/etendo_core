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
@RunWith(MockitoJUnitRunner.class)
public class CsrfUtilTest {

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private HttpSession mockSession;

  // --- Tests for hasValidCsrfToken ---

  @Test
  public void testHasValidCsrfTokenReturnsTrueWhenTokensMatch() {
    assertTrue(CsrfUtil.hasValidCsrfToken("ABC123", "ABC123"));
  }

  @Test
  public void testHasValidCsrfTokenReturnsFalseWhenTokensDontMatch() {
    assertFalse(CsrfUtil.hasValidCsrfToken("ABC123", "DEF456"));
  }

  @Test
  public void testHasValidCsrfTokenReturnsFalseWhenRequestTokenIsNull() {
    assertFalse(CsrfUtil.hasValidCsrfToken(null, "ABC123"));
  }

  @Test
  public void testHasValidCsrfTokenReturnsFalseWhenSessionTokenIsNull() {
    assertFalse(CsrfUtil.hasValidCsrfToken("ABC123", null));
  }

  @Test
  public void testHasValidCsrfTokenReturnsFalseWhenBothTokensNull() {
    assertFalse(CsrfUtil.hasValidCsrfToken(null, null));
  }

  @Test
  public void testHasValidCsrfTokenReturnsFalseWhenRequestTokenIsEmpty() {
    assertFalse(CsrfUtil.hasValidCsrfToken("", "ABC123"));
  }

  @Test
  public void testHasValidCsrfTokenReturnsFalseWhenSessionTokenIsEmpty() {
    assertFalse(CsrfUtil.hasValidCsrfToken("ABC123", ""));
  }

  @Test
  public void testHasValidCsrfTokenReturnsFalseWhenBothTokensEmpty() {
    assertFalse(CsrfUtil.hasValidCsrfToken("", ""));
  }

  // --- Tests for getCsrfTokenFromRequestContent ---

  @Test
  public void testGetCsrfTokenFromRequestContentExtractsToken() {
    String content = "{\"csrfToken\":\"ABCDEF123456\",\"otherField\":\"value\"}";
    assertEquals("ABCDEF123456", CsrfUtil.getCsrfTokenFromRequestContent(content));
  }

  @Test
  public void testGetCsrfTokenFromRequestContentReturnsEmptyWhenNoToken() {
    String content = "{\"otherField\":\"value\"}";
    assertEquals("", CsrfUtil.getCsrfTokenFromRequestContent(content));
  }

  @Test
  public void testGetCsrfTokenFromRequestContentReturnsEmptyForEmptyContent() {
    assertEquals("", CsrfUtil.getCsrfTokenFromRequestContent(""));
  }

  @Test
  public void testGetCsrfTokenFromRequestContentExtractsUppercaseAlphanumericToken() {
    String content = "{\"csrfToken\":\"A1B2C3D4E5\"}";
    assertEquals("A1B2C3D4E5", CsrfUtil.getCsrfTokenFromRequestContent(content));
  }

  @Test
  public void testGetCsrfTokenFromRequestContentReturnsEmptyForLowercaseToken() {
    // Pattern only matches uppercase letters and digits
    String content = "{\"csrfToken\":\"abcdef123\"}";
    assertEquals("", CsrfUtil.getCsrfTokenFromRequestContent(content));
  }

  @Test
  public void testGetCsrfTokenFromRequestContentReturnsEmptyForEmptyTokenValue() {
    String content = "{\"csrfToken\":\"\"}";
    assertEquals("", CsrfUtil.getCsrfTokenFromRequestContent(content));
  }

  // --- Tests for checkCsrfToken ---

  @Test
  public void testCheckCsrfTokenSucceedsWithValidTokens() {
    // Arrange
    when(mockRequest.getSession(false)).thenReturn(mockSession);
    when(mockSession.getAttribute("#CSRF_TOKEN")).thenReturn("VALIDTOKEN123");

    // Act - should not throw
    CsrfUtil.checkCsrfToken("VALIDTOKEN123", mockRequest);
  }

  @Test(expected = OBUserException.class)
  public void testCheckCsrfTokenThrowsWhenTokensDontMatch() {
    // Arrange
    when(mockRequest.getSession(false)).thenReturn(mockSession);
    when(mockSession.getAttribute("#CSRF_TOKEN")).thenReturn("SESSIONTOKEN");
    when(mockRequest.getRequestURI()).thenReturn("/test/uri");
    when(mockSession.getId()).thenReturn("session-id");

    // Act
    CsrfUtil.checkCsrfToken("WRONG_TOKEN", mockRequest);
  }

  @Test(expected = OBUserException.class)
  public void testCheckCsrfTokenThrowsWhenRequestTokenIsNull() {
    // Arrange
    when(mockRequest.getSession(false)).thenReturn(mockSession);
    when(mockSession.getAttribute("#CSRF_TOKEN")).thenReturn("SESSIONTOKEN");
    when(mockRequest.getRequestURI()).thenReturn("/test/uri");
    when(mockSession.getId()).thenReturn("session-id");

    // Act
    CsrfUtil.checkCsrfToken(null, mockRequest);
  }

  @Test(expected = OBUserException.class)
  public void testCheckCsrfTokenThrowsWhenSessionTokenIsNull() {
    // Arrange
    when(mockRequest.getSession(false)).thenReturn(mockSession);
    when(mockSession.getAttribute("#CSRF_TOKEN")).thenReturn(null);
    when(mockRequest.getRequestURI()).thenReturn("/test/uri");
    when(mockSession.getId()).thenReturn("session-id");

    // Act
    CsrfUtil.checkCsrfToken("REQUESTTOKEN", mockRequest);
  }
}
