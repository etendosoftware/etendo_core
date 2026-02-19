package org.openbravo.client.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.client.kernel.BaseKernelServlet.KernelHttpServletResponse;

/**
 * Tests for {@link BaseKernelServlet.KernelHttpServletResponse}.
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseKernelServletTest {

  private static final String HTTP_REDIRECT_EXAMPLE_COM = "http://redirect.example.com";
  /** Redirect target is null by default. */

  @Test
  public void testRedirectTargetIsNullByDefault() {
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    KernelHttpServletResponse wrapper = new KernelHttpServletResponse(mockResponse);
    assertNull(wrapper.getRedirectTarget());
  }
  /** Do logout is false by default. */

  @Test
  public void testDoLogoutIsFalseByDefault() {
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    KernelHttpServletResponse wrapper = new KernelHttpServletResponse(mockResponse);
    assertFalse(wrapper.isDoLogout());
  }
  /** Set redirect target. */

  @Test
  public void testSetRedirectTarget() {
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    KernelHttpServletResponse wrapper = new KernelHttpServletResponse(mockResponse);
    wrapper.setRedirectTarget("http://example.com");
    assertEquals("http://example.com", wrapper.getRedirectTarget());
  }
  /** Set do logout. */

  @Test
  public void testSetDoLogout() {
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    KernelHttpServletResponse wrapper = new KernelHttpServletResponse(mockResponse);
    wrapper.setDoLogout(true);
    assertTrue(wrapper.isDoLogout());
  }
  /**
   * Send redirect sets redirect target.
   * @throws IOException if an error occurs
   */

  @Test
  public void testSendRedirectSetsRedirectTarget() throws IOException {
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    KernelHttpServletResponse wrapper = new KernelHttpServletResponse(mockResponse);
    wrapper.sendRedirect(HTTP_REDIRECT_EXAMPLE_COM);
    assertEquals(HTTP_REDIRECT_EXAMPLE_COM, wrapper.getRedirectTarget());
  }
  /**
   * Send redirect does not call super send redirect.
   * @throws IOException if an error occurs
   */

  @Test
  public void testSendRedirectDoesNotCallSuperSendRedirect() throws IOException {
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    KernelHttpServletResponse wrapper = new KernelHttpServletResponse(mockResponse);
    wrapper.sendRedirect(HTTP_REDIRECT_EXAMPLE_COM);
    // The original response's sendRedirect should NOT be called
    // because KernelHttpServletResponse overrides it to just store the target
    verify(mockResponse, org.mockito.Mockito.never()).sendRedirect(org.mockito.ArgumentMatchers.anyString());
  }
  /** Set do logout back to false. */

  @Test
  public void testSetDoLogoutBackToFalse() {
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    KernelHttpServletResponse wrapper = new KernelHttpServletResponse(mockResponse);
    wrapper.setDoLogout(true);
    assertTrue(wrapper.isDoLogout());
    wrapper.setDoLogout(false);
    assertFalse(wrapper.isDoLogout());
  }
}
