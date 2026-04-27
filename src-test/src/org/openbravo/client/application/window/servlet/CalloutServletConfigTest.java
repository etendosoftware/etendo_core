package org.openbravo.client.application.window.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.ServletContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
/** Tests for {@link CalloutServletConfig}. */

@RunWith(MockitoJUnitRunner.class)
public class CalloutServletConfigTest {

  private static final String PARAM1 = "param1";

  private static final String SERVLET_NAME = "TestServlet";

  @Mock
  private ServletContext mockContext;

  private CalloutServletConfig config;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    config = new CalloutServletConfig(SERVLET_NAME, mockContext);
  }
  /** Get servlet name. */

  @Test
  public void testGetServletName() {
    assertEquals(SERVLET_NAME, config.getServletName());
  }
  /** Get servlet context. */

  @Test
  public void testGetServletContext() {
    assertEquals(mockContext, config.getServletContext());
  }
  /** Get init parameter delegates to context. */

  @Test
  public void testGetInitParameterDelegatesToContext() {
    when(mockContext.getInitParameter(PARAM1)).thenReturn("value1");
    assertEquals("value1", config.getInitParameter(PARAM1));
  }
  /** Get init parameter returns null for unknown. */

  @Test
  public void testGetInitParameterReturnsNullForUnknown() {
    when(mockContext.getInitParameter("unknown")).thenReturn(null);
    assertNull(config.getInitParameter("unknown"));
  }
  /** Get init parameter names delegates to context. */

  @Test
  public void testGetInitParameterNamesDelegatesToContext() {
    ArrayList<String> names = new ArrayList<>(Arrays.asList(PARAM1, "param2"));
    when(mockContext.getInitParameterNames()).thenReturn(Collections.enumeration(names));

    Enumeration<String> result = config.getInitParameterNames();

    assertEquals(PARAM1, result.nextElement());
    assertEquals("param2", result.nextElement());
  }
  /** Get init parameter names empty. */

  @Test
  public void testGetInitParameterNamesEmpty() {
    when(mockContext.getInitParameterNames()).thenReturn(Collections.emptyEnumeration());
    Enumeration<String> result = config.getInitParameterNames();
    assertEquals(false, result.hasMoreElements());
  }
}
