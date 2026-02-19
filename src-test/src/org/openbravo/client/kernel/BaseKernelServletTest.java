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

  @Test
  public void testRedirectTargetIsNullByDefault() {
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    KernelHttpServletResponse wrapper = new KernelHttpServletResponse(mockResponse);
    assertNull(wrapper.getRedirectTarget());
  }

  @Test
  public void testDoLogoutIsFalseByDefault() {
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    KernelHttpServletResponse wrapper = new KernelHttpServletResponse(mockResponse);
    assertFalse(wrapper.isDoLogout());
  }

  @Test
  public void testSetRedirectTarget() {
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    KernelHttpServletResponse wrapper = new KernelHttpServletResponse(mockResponse);
    wrapper.setRedirectTarget("http://example.com");
    assertEquals("http://example.com", wrapper.getRedirectTarget());
  }

  @Test
  public void testSetDoLogout() {
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    KernelHttpServletResponse wrapper = new KernelHttpServletResponse(mockResponse);
    wrapper.setDoLogout(true);
    assertTrue(wrapper.isDoLogout());
  }

  @Test
  public void testSendRedirectSetsRedirectTarget() throws IOException {
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    KernelHttpServletResponse wrapper = new KernelHttpServletResponse(mockResponse);
    wrapper.sendRedirect("http://redirect.example.com");
    assertEquals("http://redirect.example.com", wrapper.getRedirectTarget());
  }

  @Test
  public void testSendRedirectDoesNotCallSuperSendRedirect() throws IOException {
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    KernelHttpServletResponse wrapper = new KernelHttpServletResponse(mockResponse);
    wrapper.sendRedirect("http://redirect.example.com");
    // The original response's sendRedirect should NOT be called
    // because KernelHttpServletResponse overrides it to just store the target
    verify(mockResponse, org.mockito.Mockito.never()).sendRedirect(org.mockito.ArgumentMatchers.anyString());
  }

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
