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
/** Tests for {@link BaseActionHandler}. */

@RunWith(MockitoJUnitRunner.Silent.class)
public class BaseActionHandlerTest {

  private static final String VALUE1 = "value1";
  private static final String VALUE2 = "value2";
  private static final String MULTI = "multi";
  private static final String CONTEXT = "context";

  private BaseActionHandler instance;

  @Mock
  private HttpServletRequest mockRequest;
  /** Sets up test fixtures. */

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
  /** Extract parameters from request single values. */

  @Test
  public void testExtractParametersFromRequestSingleValues() {
    // Arrange
    when(mockRequest.getParameterNames()).thenReturn(
        Collections.enumeration(java.util.Arrays.asList("key1", "key2")));
    when(mockRequest.getParameterValues("key1")).thenReturn(new String[]{VALUE1});
    when(mockRequest.getParameter("key1")).thenReturn(VALUE1);
    when(mockRequest.getParameterValues("key2")).thenReturn(new String[]{VALUE2});
    when(mockRequest.getParameter("key2")).thenReturn(VALUE2);

    // Act
    Map<String, Object> result = instance.extractParametersFromRequest(mockRequest);

    // Assert
    assertEquals(VALUE1, result.get("key1"));
    assertEquals(VALUE2, result.get("key2"));
    assertEquals(2, result.size());
  }
  /** Extract parameters from request multiple values. */

  @Test
  public void testExtractParametersFromRequestMultipleValues() {
    // Arrange
    String[] multiValues = new String[]{"val1", "val2"};
    when(mockRequest.getParameterNames()).thenReturn(
        Collections.enumeration(Collections.singletonList(MULTI)));
    when(mockRequest.getParameterValues(MULTI)).thenReturn(multiValues);

    // Act
    Map<String, Object> result = instance.extractParametersFromRequest(mockRequest);

    // Assert
    assertTrue(result.get(MULTI) instanceof String[]);
    assertEquals(2, ((String[]) result.get(MULTI)).length);
  }
  /** Extract parameters from request empty. */

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
  /**
   * Extract request content with content.
   * @throws IOException if an error occurs
   */

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
  /**
   * Extract request content empty.
   * @throws IOException if an error occurs
   */

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
  /**
   * Fix request map filters http keys.
   * @throws Exception if an error occurs
   */

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
  /**
   * Fix request map adds context.
   * @throws Exception if an error occurs
   */

  @Test
  public void testFixRequestMapAddsContext() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put("testKey", "testValue");

    // Act
    Map<String, String> result = instance.fixRequestMap(parameters, context);

    // Assert
    assertTrue(result.containsKey(CONTEXT));
    assertTrue(result.get(CONTEXT).contains("testKey"));
  }
  /** Fix request map null context. */

  @Test
  public void testFixRequestMapNullContext() {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("key1", VALUE1);

    // Act
    Map<String, String> result = instance.fixRequestMap(parameters, null);

    // Assert
    assertFalse(result.containsKey(CONTEXT));
    assertEquals(VALUE1, result.get("key1"));
  }

  private ServletInputStream createServletInputStream(ByteArrayInputStream bais) {
    return new ServletInputStream() {
      /**
       * Read.
       * @throws IOException if an error occurs
       */
      @Override
      public int read() throws IOException {
        return bais.read();
      }
      /** Is finished. */

      @Override
      public boolean isFinished() {
        return bais.available() == 0;
      }
      /** Is ready. */

      @Override
      public boolean isReady() {
        return true;
      }
      /** Set read listener. */

      @Override
      public void setReadListener(ReadListener readListener) {
        // no-op
      }
    };
  }
}
