package org.openbravo.client.application.window.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CalloutHttpServletResponseTest {

  @Mock
  private HttpServletResponse wrappedResponse;

  private CalloutHttpServletResponse response;

  @Before
  public void setUp() {
    response = new CalloutHttpServletResponse(wrappedResponse);
  }

  @Test
  public void testGetOutputFromWriterReturnsEmptyInitially() {
    String output = response.getOutputFromWriter();
    assertEquals("", output);
  }

  @Test
  public void testGetWriterReturnsPrintWriter() throws Exception {
    PrintWriter writer = response.getWriter();
    assertNotNull(writer);
  }

  @Test
  public void testWriterOutputCaptured() throws Exception {
    PrintWriter writer = response.getWriter();
    writer.print("test content");
    writer.flush();
    assertEquals("test content", response.getOutputFromWriter());
  }

  @Test
  public void testMultipleWritesCaptured() throws Exception {
    PrintWriter writer = response.getWriter();
    writer.print("first ");
    writer.print("second");
    writer.flush();
    assertEquals("first second", response.getOutputFromWriter());
  }

  @Test
  public void testSendRedirectDoesNothing() throws Exception {
    // sendRedirect is a no-op in this implementation
    response.sendRedirect("http://example.com");
    // No exception should be thrown, and no redirect should occur
    assertEquals("", response.getOutputFromWriter());
  }

  @Test
  public void testGetWriterReturnsSameInstance() throws Exception {
    PrintWriter writer1 = response.getWriter();
    PrintWriter writer2 = response.getWriter();
    assertEquals(writer1, writer2);
  }
}
