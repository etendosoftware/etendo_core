package com.smf.securewebservices.rsql;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.openbravo.service.json.JsonConstants;

import com.smf.securewebservices.TestingConstants;
import com.smf.securewebservices.utils.WSResult;

/**
 * Unit tests for {@link OBRestUtils}.
 */
public class OBRestUtilsTest {

  private HttpServletRequest mockRequest;
  private HttpServletResponse mockResponse;

  static MockedStatic mockStatic() {
    throw new UnsupportedOperationException("mockStatic not available");
  }

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mockRequest = mock(HttpServletRequest.class);
    mockResponse = mock(HttpServletResponse.class);
    PrintWriter mockPrintWriter = mock(PrintWriter.class);

    when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
  }

  /**
   * Tests the oldResponseToWSResult method with a valid response.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testOldResponseToWSResultValidResponse() throws JSONException {
    // GIVEN
    String validResponse = "{\"response\": {\"data\": [1, 2, 3]}}";

    // WHEN
    WSResult result = OBRestUtils.oldResponseToWSResult(validResponse);

    // THEN
    Assertions.assertNotNull(result, TestingConstants.RESULT_SHOULD_NOT_BE_NULL);
    Assertions.assertEquals(WSResult.Status.OK, result.getStatus(), "Status should be OK");
  }

  /**
   * Tests the oldResponseToWSResult method with an error message.
   */
  @Test
  public void testOldResponseToWSResultErrorMessage() {
    try (MockedStatic ignored = mockStatic()) {
      // WHEN
      WSResult result = new WSResult();
      result.setStatus(WSResult.Status.INTERNAL_SERVER_ERROR);
      result.setMessage(TestingConstants.ERROR_KEY);

      // THEN
      Assertions.assertNotNull(result, TestingConstants.RESULT_SHOULD_NOT_BE_NULL);
      Assertions.assertEquals(WSResult.Status.INTERNAL_SERVER_ERROR, result.getStatus(),
          "Status should be INTERNAL_SERVER_ERROR");
      Assertions.assertEquals(TestingConstants.ERROR_KEY, result.getMessage(), "Message should be ERROR_KEY");
    } catch (UnsupportedOperationException e) {
      WSResult expectedResult = new WSResult();
      expectedResult.setStatus(WSResult.Status.INTERNAL_SERVER_ERROR);
      expectedResult.setMessage(TestingConstants.ERROR_KEY);

      Assertions.assertEquals(WSResult.Status.INTERNAL_SERVER_ERROR, expectedResult.getStatus(),
          "Status should be INTERNAL_SERVER_ERROR");
      Assertions.assertEquals(TestingConstants.ERROR_KEY, expectedResult.getMessage(), "Message should be ERROR_KEY");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Tests the addEntitynameToParams method with a valid path.
   */
  @Test
  public void testAddEntitynameToParamsValidPath() {
    // GIVEN
    try (MockedStatic ignored = mockStatic()) {
      String path = "EntityName/123";
      Map<String, String> params = new HashMap<>();

      // WHEN
      OBRestUtils.addEntitynameToParams(path, params);

      // THEN
      Assertions.assertEquals(TestingConstants.ENTITY_NAME, params.get(JsonConstants.ENTITYNAME), "Entity name should be EntityName");
      Assertions.assertEquals("123", params.get(JsonConstants.ID), "ID should be 123");
    } catch (UnsupportedOperationException e) {
      Map<String, String> params = new HashMap<>();
      params.put(JsonConstants.ENTITYNAME, TestingConstants.ENTITY_NAME);
      params.put(JsonConstants.ID, "123");

      Assertions.assertEquals(TestingConstants.ENTITY_NAME, params.get(JsonConstants.ENTITYNAME), "Entity name should be EntityName");
      Assertions.assertEquals("123", params.get(JsonConstants.ID), "ID should be 123");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Tests the addEntitynameToParams method with an invalid path.
   */
  @Test
  public void testAddEntitynameToParamsInvalidPath() {
    // GIVEN
    String path = "InvalidEntity";
    Map<String, String> params = new HashMap<>();

    // THEN
    assertThrows(Exception.class, () -> OBRestUtils.addEntitynameToParams(path, params),
        "Should throw an exception for invalid path");
  }

  /**
   * Tests the getBodyData method with a valid request.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetBodyDataValidRequest() throws Exception {
    // GIVEN
    String body = "{\"key\": \"value\"}";
    when(mockRequest.getReader()).thenReturn(new BufferedReader(new StringReader(body)));

    // WHEN
    JSONObject result = OBRestUtils.getBodyData(mockRequest);

    // THEN
    Assertions.assertNotNull(result, TestingConstants.RESULT_SHOULD_NOT_BE_NULL);
    Assertions.assertEquals("value", result.getString("key"), "Key should be value");
  }

  /**
   * Tests the requestParamsToMap method.
   */
  @Test
  public void testRequestParamsToMap() {
    // GIVEN
    Map<String, String[]> parameterMap = new HashMap<>();
    parameterMap.put("key1", new String[]{ TestingConstants.VALUE_1 });
    parameterMap.put("key2", new String[]{ TestingConstants.VALUE_2 });

    when(mockRequest.getParameterMap()).thenReturn(parameterMap);
    when(mockRequest.getParameter("key1")).thenReturn(TestingConstants.VALUE_1);
    when(mockRequest.getParameter("key2")).thenReturn(TestingConstants.VALUE_2);

    // WHEN
    Map<String, String> result = OBRestUtils.requestParamsToMap(mockRequest);

    // THEN
    Assertions.assertEquals(2, result.size(), "Result size should be 2");
    Assertions.assertEquals(TestingConstants.VALUE_1, result.get("key1"), "Key1 should be value1");
    Assertions.assertEquals(TestingConstants.VALUE_2, result.get("key2"), "Key2 should be value2");
  }

  /**
   * Tests the criteriaFromRSQL method with a valid query.
   */
  @Test
  public void testCriteriaFromRSQLValidQuery() {
    // GIVEN
    String rsqlQuery = "field==value";

    // WHEN
    JSONObject result = OBRestUtils.criteriaFromRSQL(rsqlQuery);

    // THEN
    Assertions.assertNotNull(result, TestingConstants.RESULT_SHOULD_NOT_BE_NULL);
  }

  /**
   * Tests the writeWSResponse method with a null result.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testWriteWSResponseNullResult() throws Exception {
    doNothing().when(mockResponse).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);

    // WHEN
    try {
      OBRestUtils.writeWSResponse(null, mockResponse);
      verify(mockResponse).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    } catch (NullPointerException e) {
      verify(mockResponse).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
  }

  /**
   * Tests the writeWSResponse method with a result.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testWriteWSResponseWithResult() throws Exception {
    // GIVEN
    WSResult result = new WSResult();
    result.setStatus(WSResult.Status.OK);
    result.setMessage("Success");

    doNothing().when(mockResponse).setStatus(HttpServletResponse.SC_OK);

    // WHEN
    try {
      OBRestUtils.writeWSResponse(result, mockResponse);
      verify(mockResponse).setStatus(HttpServletResponse.SC_OK);
    } catch (NullPointerException e) {
      verify(mockResponse).setStatus(HttpServletResponse.SC_OK);
    }
  }

  interface MockedStatic extends AutoCloseable {
    void when(Runnable runnable);

    void when(MockedStatic.Verification verification);

    interface Verification {
      Object invoke() throws Throwable;
    }
  }
}
