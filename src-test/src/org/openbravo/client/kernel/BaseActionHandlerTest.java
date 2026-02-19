package org.openbravo.client.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BaseActionHandlerTest {

  private BaseActionHandler instance;

  @Mock
  private HttpServletRequest mockRequest;

  @Before
  public void setUp() {
    // Create a concrete subclass for testing
    instance = new BaseActionHandler() {
      @Override
      protected JSONObject execute(Map<String, Object> parameters, String content) {
        return new JSONObject();
      }
    };
  }

  @Test
  public void testExtractParametersFromRequestSingleValues() {
    // Arrange
    when(mockRequest.getParameterNames()).thenReturn(
        Collections.enumeration(java.util.Arrays.asList("key1", "key2")));
    when(mockRequest.getParameterValues("key1")).thenReturn(new String[]{"value1"});
    when(mockRequest.getParameter("key1")).thenReturn("value1");
    when(mockRequest.getParameterValues("key2")).thenReturn(new String[]{"value2"});
    when(mockRequest.getParameter("key2")).thenReturn("value2");

    // Act
    Map<String, Object> result = instance.extractParametersFromRequest(mockRequest);

    // Assert
    assertEquals("value1", result.get("key1"));
    assertEquals("value2", result.get("key2"));
    assertEquals(2, result.size());
  }

  @Test
  public void testExtractParametersFromRequestMultipleValues() {
    // Arrange
    String[] multiValues = new String[]{"val1", "val2"};
    when(mockRequest.getParameterNames()).thenReturn(
        Collections.enumeration(Collections.singletonList("multi")));
    when(mockRequest.getParameterValues("multi")).thenReturn(multiValues);

    // Act
    Map<String, Object> result = instance.extractParametersFromRequest(mockRequest);

    // Assert
    assertTrue(result.get("multi") instanceof String[]);
    assertEquals(2, ((String[]) result.get("multi")).length);
  }

  @Test
  public void testExtractParametersFromRequestEmpty() {
    // Arrange
    when(mockRequest.getParameterNames()).thenReturn(
        Collections.enumeration(Collections.emptyList()));

    // Act
    Map<String, Object> result = instance.extractParametersFromRequest(mockRequest);

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void testExtractRequestContentWithContent() throws IOException {
    // Arrange
    String content = "test content line1\nline2";
    ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    ServletInputStream sis = createServletInputStream(bais);
    when(mockRequest.getInputStream()).thenReturn(sis);

    // Act
    String result = instance.extractRequestContent(mockRequest, new HashMap<>());

    // Assert
    assertNotNull(result);
    assertTrue(result.contains("test content line1"));
    assertTrue(result.contains("line2"));
  }

  @Test
  public void testExtractRequestContentEmpty() throws IOException {
    // Arrange
    ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
    ServletInputStream sis = createServletInputStream(bais);
    when(mockRequest.getInputStream()).thenReturn(sis);

    // Act
    String result = instance.extractRequestContent(mockRequest, new HashMap<>());

    // Assert
    assertNull(result);
  }

  @Test
  public void testFixRequestMapFiltersHttpKeys() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("normalKey", "normalValue");
    parameters.put(KernelConstants.HTTP_REQUEST, mockRequest);
    parameters.put(KernelConstants.HTTP_SESSION, mock(javax.servlet.http.HttpSession.class));

    // Act
    Map<String, String> result = instance.fixRequestMap(parameters, null);

    // Assert
    assertTrue(result.containsKey("normalKey"));
    assertFalse(result.containsKey(KernelConstants.HTTP_REQUEST));
    assertFalse(result.containsKey(KernelConstants.HTTP_SESSION));
  }

  @Test
  public void testFixRequestMapAddsContext() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("testKey", "testValue");

    // Act
    Map<String, String> result = instance.fixRequestMap(parameters, context);

    // Assert
    assertTrue(result.containsKey("context"));
    assertTrue(result.get("context").contains("testKey"));
  }

  @Test
  public void testFixRequestMapNullContext() {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("key1", "value1");

    // Act
    Map<String, String> result = instance.fixRequestMap(parameters, null);

    // Assert
    assertFalse(result.containsKey("context"));
    assertEquals("value1", result.get("key1"));
  }

  private ServletInputStream createServletInputStream(ByteArrayInputStream bais) {
    return new ServletInputStream() {
      @Override
      public int read() throws IOException {
        return bais.read();
      }

      @Override
      public boolean isFinished() {
        return bais.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
        // no-op
      }
    };
  }
}
