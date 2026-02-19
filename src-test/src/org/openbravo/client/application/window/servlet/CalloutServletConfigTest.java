package org.openbravo.client.application.window.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import javax.servlet.ServletContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CalloutServletConfigTest {

  private static final String SERVLET_NAME = "TestServlet";

  @Mock
  private ServletContext mockContext;

  private CalloutServletConfig config;

  @Before
  public void setUp() {
    config = new CalloutServletConfig(SERVLET_NAME, mockContext);
  }

  @Test
  public void testGetServletName() {
    assertEquals(SERVLET_NAME, config.getServletName());
  }

  @Test
  public void testGetServletContext() {
    assertEquals(mockContext, config.getServletContext());
  }

  @Test
  public void testGetInitParameterDelegatesToContext() {
    when(mockContext.getInitParameter("param1")).thenReturn("value1");
    assertEquals("value1", config.getInitParameter("param1"));
  }

  @Test
  public void testGetInitParameterReturnsNullForUnknown() {
    when(mockContext.getInitParameter("unknown")).thenReturn(null);
    assertNull(config.getInitParameter("unknown"));
  }

  @Test
  public void testGetInitParameterNamesDelegatesToContext() {
    Vector<String> names = new Vector<>();
    names.add("param1");
    names.add("param2");
    when(mockContext.getInitParameterNames()).thenReturn(names.elements());

    Enumeration<String> result = config.getInitParameterNames();

    assertEquals("param1", result.nextElement());
    assertEquals("param2", result.nextElement());
  }

  @Test
  public void testGetInitParameterNamesEmpty() {
    when(mockContext.getInitParameterNames()).thenReturn(Collections.emptyEnumeration());
    Enumeration<String> result = config.getInitParameterNames();
    assertEquals(false, result.hasMoreElements());
  }
}
