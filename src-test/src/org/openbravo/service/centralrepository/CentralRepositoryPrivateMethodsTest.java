package org.openbravo.service.centralrepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;

/**
 * Unit tests for the private methods of the CentralRepository class.
 * Verifies the behavior of methods responsible for HTTP requests and response handling.
 */
@ExtendWith(MockitoExtension.class)
public class CentralRepositoryPrivateMethodsTest {

  private MockedStatic<HttpClients> httpClientsStatic;

  @Mock
  private CloseableHttpClient mockHttpClient;

  @Mock
  private CloseableHttpResponse mockResponse;

  @Mock
  private StatusLine mockStatusLine;

  @Mock
  private HttpEntity mockEntity;
   /**
   * Sets up the test environment by mocking the HttpClients class and its createDefault method.
   * This allows for controlled testing of the HTTP client behavior.
   */
  @BeforeEach
  public void setUp() {
    httpClientsStatic = mockStatic(HttpClients.class);
    httpClientsStatic.when(HttpClients::createDefault).thenReturn(mockHttpClient);
  }
   /**
   * Cleans up the test environment by closing the mocked HttpClients static instance.
   * This ensures that resources are released after each test.
   */
  @AfterEach
  public void tearDown() {
    if (httpClientsStatic != null) {
      httpClientsStatic.close();
    }
  }

  /**
   * Tests the getServiceRequest method with an invalid URI.
   * Verifies that an OBException is thrown when the URI is invalid.
   *
   * @throws Exception
   *     if reflection access or method invocation fails
   */
  @Test
  public void testGetServiceRequestGivenInvalidURIShouldThrowOBException() throws Exception {
    CentralRepository.Service service = CentralRepository.Service.MODULE_INFO;

    Method getServiceRequestMethod = CentralRepository.class.getDeclaredMethod("getServiceRequest",
        CentralRepository.Service.class, List.class);
    getServiceRequestMethod.setAccessible(true);

    List<String> invalidPath = Collections.singletonList("invalid path with spaces");

    try {
      getServiceRequestMethod.invoke(null, service, invalidPath);
      fail("An exception was expected but none was thrown.");
    } catch (Exception e) {
      Throwable cause = e.getCause();
      assertInstanceOf(OBException.class, cause);
    }
  }

  /**
   * Tests the executeRequest method with a successful response.
   * Verifies that the response contains success and the expected data.
   *
   * @throws Exception
   *     if reflection access or method invocation fails
   */
  @Test
  public void testExecuteRequestGivenSuccessfulResponseShouldReturnJsonWithSuccess() throws Exception {
    CentralRepository.Service service = CentralRepository.Service.VERSION_INFO;
    List<String> path = Collections.emptyList();
    JSONObject payload = null;

    String jsonResponse = "{\"data\":\"test data\"}";
    when(mockStatusLine.getStatusCode()).thenReturn(200);
    when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
    when(mockResponse.getEntity()).thenReturn(mockEntity);
    when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));
    when(mockHttpClient.execute(any(HttpRequestBase.class))).thenReturn(mockResponse);

    Method executeRequestMethod = CentralRepository.class.getDeclaredMethod(CentralRepositoryTestConstants.EXECUTE_REQUEST,
        CentralRepository.Service.class, List.class, JSONObject.class);
    executeRequestMethod.setAccessible(true);

    JSONObject result = (JSONObject) executeRequestMethod.invoke(null, service, path, payload);

    assertTrue(result.getBoolean(CentralRepositoryTestConstants.SUCCESS));
    assertEquals(200, result.getInt(CentralRepositoryTestConstants.RESPONSE_CODE));
    assertEquals("test data", result.getJSONObject(CentralRepositoryTestConstants.RESPONSE).getString("data"));

    verify(mockHttpClient).execute(any(HttpRequestBase.class));
  }

  /**
   * Tests the executeRequest method with an error response.
   * Verifies that the response contains the appropriate error information.
   *
   * @throws Exception
   *     if reflection access or method invocation fails
   */
  @Test
  public void testExecuteRequestGivenErrorResponseShouldReturnJsonWithErrorInfo() throws Exception {
    CentralRepository.Service service = CentralRepository.Service.MODULE_INFO;
    List<String> path = Collections.singletonList("123");
    JSONObject payload = null;

    String jsonResponse = "{\"error\":\"not found\"}";
    when(mockStatusLine.getStatusCode()).thenReturn(404);
    when(mockStatusLine.getReasonPhrase()).thenReturn("Not Found");
    when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
    when(mockResponse.getEntity()).thenReturn(mockEntity);
    when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));
    when(mockHttpClient.execute(any(HttpRequestBase.class))).thenReturn(mockResponse);

    Method executeRequestMethod = CentralRepository.class.getDeclaredMethod(CentralRepositoryTestConstants.EXECUTE_REQUEST,
        CentralRepository.Service.class, List.class, JSONObject.class);
    executeRequestMethod.setAccessible(true);

    JSONObject result = (JSONObject) executeRequestMethod.invoke(null, service, path, payload);

    assertFalse(result.getBoolean(CentralRepositoryTestConstants.SUCCESS));
    assertEquals(404, result.getInt(CentralRepositoryTestConstants.RESPONSE_CODE));
    assertEquals("not found", result.getJSONObject(CentralRepositoryTestConstants.RESPONSE).getString("error"));
  }

  /**
   * Tests the executeRequest method when a connection exception occurs.
   * Verifies that the response contains the appropriate error message.
   *
   * @throws Exception
   *     if reflection access or method invocation fails
   * @throws IOException
   *     if an I/O error occurs during the HTTP request
   */
  @Test
  public void testExecuteRequestGivenConnectionExceptionShouldReturnJsonWithErrorMessage() throws Exception {
    CentralRepository.Service service = CentralRepository.Service.SEARCH_MODULES;
    List<String> path = Collections.emptyList();
    JSONObject payload = new JSONObject("{\"query\":\"test\"}");

    when(mockHttpClient.execute(any(HttpRequestBase.class))).thenThrow(new IOException("Connection timeout"));

    Method executeRequestMethod = CentralRepository.class.getDeclaredMethod(CentralRepositoryTestConstants.EXECUTE_REQUEST,
        CentralRepository.Service.class, List.class, JSONObject.class);
    executeRequestMethod.setAccessible(true);

    JSONObject result = (JSONObject) executeRequestMethod.invoke(null, service, path, payload);

    assertFalse(result.getBoolean(CentralRepositoryTestConstants.SUCCESS));
    assertEquals(500, result.getInt(CentralRepositoryTestConstants.RESPONSE_CODE));
    assertEquals("Connection timeout", result.getJSONObject(CentralRepositoryTestConstants.RESPONSE).getString("msg"));
  }

  /**
   * Tests the executeRequest method with an invalid JSON response.
   * Verifies that the method handles the invalid JSON gracefully.
   *
   * @throws Exception
   *     if reflection access or method invocation fails
   */
  @Test
  public void testExecuteRequestGivenInvalidJsonResponseShouldHandleJsonException() throws Exception {
    CentralRepository.Service service = CentralRepository.Service.VERSION_INFO;
    List<String> path = Collections.emptyList();
    JSONObject payload = null;

    String invalidJsonResponse = "This is not valid JSON";
    when(mockStatusLine.getStatusCode()).thenReturn(200);
    when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
    when(mockResponse.getEntity()).thenReturn(mockEntity);
    when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(invalidJsonResponse.getBytes()));
    when(mockHttpClient.execute(any(HttpRequestBase.class))).thenReturn(mockResponse);

    Method executeRequestMethod = CentralRepository.class.getDeclaredMethod(CentralRepositoryTestConstants.EXECUTE_REQUEST,
        CentralRepository.Service.class, List.class, JSONObject.class);
    executeRequestMethod.setAccessible(true);

    JSONObject result = (JSONObject) executeRequestMethod.invoke(null, service, path, payload);

    assertTrue(result.getBoolean(CentralRepositoryTestConstants.SUCCESS));
    assertEquals(200, result.getInt(CentralRepositoryTestConstants.RESPONSE_CODE));

    JSONObject responseObj = result.getJSONObject(CentralRepositoryTestConstants.RESPONSE);
    assertEquals(0, responseObj.length());
  }
}
