package com.smf.securewebservices.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import com.smf.securewebservices.TestingConstants;

/**
 * Unit tests for the JsonDalWebService class.
 */
public class JsonDalWebServiceTest {

  private JsonDalWebService jsonDalWebService;
  private HttpServletRequest mockRequest;
  private HttpServletResponse mockResponse;
  private PrintWriter mockPrintWriter;
  private StringWriter stringWriter;

  /**
   * Sets up the test environment before each test.
   * Initializes mock objects and the test subject.
   */
  @Before
  public void setUp() {
    jsonDalWebService = new JsonDalWebService();

    mockRequest = mock(HttpServletRequest.class);
    mockResponse = mock(HttpServletResponse.class);

    stringWriter = new StringWriter();
    mockPrintWriter = new PrintWriter(stringWriter);
    try {
      when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
    } catch (Exception e) {
      throw new RuntimeException("Error setting up test", e);
    }
  }

  /**
   * Tests the getParameterMap method with a single parameter.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetParameterMap() throws Exception {
    // Given
    when(mockRequest.getParameterNames()).thenReturn(
        Collections.enumeration(Collections.singletonList(TestingConstants.TEST_PARAM)));
    when(mockRequest.getParameter(TestingConstants.TEST_PARAM)).thenReturn("testValue");

    // When
    @SuppressWarnings("unchecked") Map<String, String> result = invokePrivateMethod(TestingConstants.GET_PARAMETER_MAP,
        mockRequest);

    // Then
    assertEquals(1, result.size());
    assertEquals("testValue", result.get(TestingConstants.TEST_PARAM));
  }

  /**
   * Tests the getParameterMap method with multiple parameters.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetParameterMapMultipleParameters() throws Exception {
    // Given
    when(mockRequest.getParameterNames()).thenReturn(Collections.enumeration(
        Arrays.asList(TestingConstants.PARAM_1, TestingConstants.PARAM_2, TestingConstants.PARAM_3)));
    when(mockRequest.getParameter(TestingConstants.PARAM_1)).thenReturn("value1");
    when(mockRequest.getParameter(TestingConstants.PARAM_2)).thenReturn("value2");
    when(mockRequest.getParameter(TestingConstants.PARAM_3)).thenReturn("value3");

    // When
    @SuppressWarnings("unchecked") Map<String, String> result = invokePrivateMethod(TestingConstants.GET_PARAMETER_MAP,
        mockRequest);

    // Then
    assertEquals(3, result.size());
    assertEquals("value1", result.get(TestingConstants.PARAM_1));
    assertEquals("value2", result.get(TestingConstants.PARAM_2));
    assertEquals("value3", result.get(TestingConstants.PARAM_3));
  }

  /**
   * Tests the getParameterMap method with no parameters.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetParameterMapEmptyParameters() throws Exception {
    // Given
    when(mockRequest.getParameterNames()).thenReturn(Collections.enumeration(Collections.emptyList()));

    // When
    @SuppressWarnings("unchecked") Map<String, String> result = invokePrivateMethod(TestingConstants.GET_PARAMETER_MAP,
        mockRequest);

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  /**
   * Tests the getRequestContent method with non-empty content.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetRequestContentWithContent() throws Exception {
    // Given
    String content = "Test content";
    BufferedReader reader = new BufferedReader(new StringReader(content));
    when(mockRequest.getReader()).thenReturn(reader);

    // When
    String result = invokePrivateMethod(TestingConstants.GET_REQUEST_CONTENT, mockRequest);

    // Then
    assertEquals(content, result);
  }

  /**
   * Tests the getRequestContent method with multiline content.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetRequestContentMultilineContent() throws Exception {
    // Given
    String content = "Line 1\nLine 2\nLine 3";
    BufferedReader reader = new BufferedReader(new StringReader(content));
    when(mockRequest.getReader()).thenReturn(reader);

    // When
    String result = invokePrivateMethod(TestingConstants.GET_REQUEST_CONTENT, mockRequest);

    // Then
    assertEquals(content, result);
  }

  /**
   * Tests the getRequestContent method with JSON content.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetRequestContentJsonContent() throws Exception {
    // Given
    String content = "{\"data\":{\"id\":\"123\",\"name\":\"Test Entity\"}}";
    BufferedReader reader = new BufferedReader(new StringReader(content));
    when(mockRequest.getReader()).thenReturn(reader);

    // When
    String result = invokePrivateMethod(TestingConstants.GET_REQUEST_CONTENT, mockRequest);

    // Then
    assertEquals(content, result);
  }

  /**
   * Tests the getRequestContent method with empty content.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetRequestContentEmptyContent() throws Exception {
    // Given
    when(mockRequest.getReader()).thenReturn(null);

    // When
    String result = invokePrivateMethod(TestingConstants.GET_REQUEST_CONTENT, mockRequest);

    // Then
    assertEquals("", result);
  }

  /**
   * Tests the writeResult method with JSON response.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testWriteResult() throws Exception {
    // Given
    String result = "{\"data\":{\"id\":\"123\"}}";

    // When
    invokePrivateMethod("writeResult", mockResponse, result);

    // Then
    verify(mockResponse).setContentType(TestingConstants.APPLICATION_JSON_UTF8);
    verify(mockResponse).setHeader("Content-Type", TestingConstants.APPLICATION_JSON_UTF8);

    mockPrintWriter.flush();
    assertEquals(result, stringWriter.toString());
  }

  /**
   * Tests the writeResult method with simple JSON response.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testWriteResultSimple() throws Exception {
    // Given
    String result = "{\"test\":\"value\"}";

    // When
    invokePrivateMethod("writeResult", mockResponse, result);

    // Then
    verify(mockResponse).setContentType(TestingConstants.APPLICATION_JSON_UTF8);
    verify(mockResponse).setHeader("Content-Type", TestingConstants.APPLICATION_JSON_UTF8);
    verify(mockResponse).getWriter();
  }

  /**
   * Tests the getParameterMap method with special parameter values (null, empty, space).
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testParameterMapWithSpecialValues() throws Exception {
    // Given
    when(mockRequest.getParameterNames()).thenReturn(
        Collections.enumeration(Arrays.asList("null", TestingConstants.EMPTY, TestingConstants.SPACE)));
    when(mockRequest.getParameter("null")).thenReturn(null);
    when(mockRequest.getParameter(TestingConstants.EMPTY)).thenReturn("");
    when(mockRequest.getParameter(TestingConstants.SPACE)).thenReturn(" ");

    // When
    @SuppressWarnings("unchecked") Map<String, String> result = invokePrivateMethod(TestingConstants.GET_PARAMETER_MAP,
        mockRequest);

    // Then
    assertEquals(3, result.size());
    assertNull(result.get("null"));
    assertEquals("", result.get(TestingConstants.EMPTY));
    assertEquals(" ", result.get(TestingConstants.SPACE));
  }

  /**
   * Tests the getRequestContent method with malformed JSON content.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetRequestContentMalformedJson() throws Exception {
    // Given
    String content = "{\"data\":{\"id\":\"123\",\"name\":\"Test Entity\"";
    BufferedReader reader = new BufferedReader(new StringReader(content));
    when(mockRequest.getReader()).thenReturn(reader);

    // When
    String result = invokePrivateMethod(TestingConstants.GET_REQUEST_CONTENT, mockRequest);

    // Then
    assertEquals(content, result);
  }

  /**
   * Tests the getRequestContent method with an empty reader.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetRequestContentEmptyReader() throws Exception {
    // Given
    BufferedReader reader = new BufferedReader(new StringReader(""));
    when(mockRequest.getReader()).thenReturn(reader);

    // When
    String result = invokePrivateMethod(TestingConstants.GET_REQUEST_CONTENT, mockRequest);

    // Then
    assertEquals("", result);
  }

  /**
   * Tests the getRequestContent method with large content.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetRequestContentLargeContent() throws Exception {
    StringBuilder largeContent = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      largeContent.append("X");
    }
    BufferedReader reader = new BufferedReader(new StringReader(largeContent.toString()));
    when(mockRequest.getReader()).thenReturn(reader);

    // When
    String result = invokePrivateMethod(TestingConstants.GET_REQUEST_CONTENT, mockRequest);

    // Then
    assertEquals(1000, result.length());
    assertEquals(largeContent.toString(), result);
  }


  /**
   * Helper method to invoke private methods of JsonDalWebService for testing.
   *
   * @param methodName
   *     the name of the method to invoke
   * @param args
   *     the arguments to pass to the method
   * @return the result of the method invocation
   * @throws Exception
   *     if an error occurs during method invocation
   */
  @SuppressWarnings("unchecked")
  private <T> T invokePrivateMethod(String methodName, Object... args) throws Exception {
    java.lang.reflect.Method method = null;

    for (java.lang.reflect.Method m : JsonDalWebService.class.getDeclaredMethods()) {
      if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
        method = m;
        break;
      }
    }

    if (method == null) {
      throw new NoSuchMethodException("Method " + methodName + " not found");
    }

    method.setAccessible(true);
    return (T) method.invoke(jsonDalWebService, args);
  }
}
