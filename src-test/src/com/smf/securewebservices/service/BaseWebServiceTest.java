package com.smf.securewebservices.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import com.smf.securewebservices.TestingConstants;
import com.smf.securewebservices.rsql.OBRestUtils;
import com.smf.securewebservices.utils.WSResult;
import com.smf.securewebservices.utils.WSResult.Status;

/**
 * Unit Test for BaseWebService class
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseWebServiceTest {

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private HttpServletResponse mockResponse;

  @Mock
  private WSResult mockResult;

  private MockedStatic<OBRestUtils> mockedOBRestUtils;

  private TestWebService webServiceUnderTest;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    webServiceUnderTest = spy(new TestWebService());
    mockedOBRestUtils = mockStatic(OBRestUtils.class);
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    if (mockedOBRestUtils != null) {
      mockedOBRestUtils.close();
    }
  }

  /**
   * Tests the doGet method.
   *
   * @throws Exception if an error occurs during execution
   */
  @Test
  public void testDoGet() throws Exception {
    // Arrange
    final String path = TestingConstants.TEST_PATH;
    final Map<String, String> params = new HashMap<>();
    params.put(TestingConstants.PARAM_1, TestingConstants.VALUE_1);

    mockedOBRestUtils.when(() -> OBRestUtils.requestParamsToMap(mockRequest)).thenReturn(params);

    WSResult expectedResult = new WSResult();
    expectedResult.setStatus(Status.OK);
    expectedResult.setMessage("GET success");

    doReturn(expectedResult).when(webServiceUnderTest).get(anyString(), anyMap());

    // Act
    webServiceUnderTest.doGet(path, mockRequest, mockResponse);

    // Assert
    verify(webServiceUnderTest).get(eq(path), eq(params));
    mockedOBRestUtils.verify(() -> OBRestUtils.writeWSResponse(expectedResult, mockResponse));
  }

  /**
   * Tests the doPost method.
   *
   * @throws Exception if an error occurs during execution
   */
  @Test
  public void testDoPost() throws Exception {
    // Arrange
    final String path = TestingConstants.TEST_PATH;
    final Map<String, String> params = new HashMap<>();
    params.put(TestingConstants.PARAM_1, TestingConstants.VALUE_1);
    final JSONObject bodyData = new JSONObject(TestingConstants.JSON_STRING);

    mockedOBRestUtils.when(() -> OBRestUtils.requestParamsToMap(mockRequest)).thenReturn(params);
    mockedOBRestUtils.when(() -> OBRestUtils.getBodyData(mockRequest)).thenReturn(bodyData);

    WSResult expectedResult = new WSResult();
    expectedResult.setStatus(Status.OK);
    expectedResult.setMessage("POST success");

    doReturn(expectedResult).when(webServiceUnderTest).post(anyString(), anyMap(), any(JSONObject.class));

    // Act
    webServiceUnderTest.doPost(path, mockRequest, mockResponse);

    // Assert
    verify(webServiceUnderTest).post(eq(path), eq(params), eq(bodyData));
    mockedOBRestUtils.verify(() -> OBRestUtils.writeWSResponse(expectedResult, mockResponse));
  }

  /**
   * Tests the doPut method.
   *
   * @throws Exception if an error occurs during execution
   */
  @Test
  public void testDoPut() throws Exception {
    // Arrange
    final String path = TestingConstants.TEST_PATH;
    final Map<String, String> params = new HashMap<>();
    params.put(TestingConstants.PARAM_1, TestingConstants.VALUE_1);
    final JSONObject bodyData = new JSONObject(TestingConstants.JSON_STRING);

    mockedOBRestUtils.when(() -> OBRestUtils.requestParamsToMap(mockRequest)).thenReturn(params);
    mockedOBRestUtils.when(() -> OBRestUtils.getBodyData(mockRequest)).thenReturn(bodyData);

    WSResult expectedResult = new WSResult();
    expectedResult.setStatus(Status.OK);
    expectedResult.setMessage("PUT success");

    doReturn(expectedResult).when(webServiceUnderTest).put(anyString(), anyMap(), any(JSONObject.class));

    // Act
    webServiceUnderTest.doPut(path, mockRequest, mockResponse);

    // Assert
    verify(webServiceUnderTest).put(eq(path), eq(params), eq(bodyData));
    mockedOBRestUtils.verify(() -> OBRestUtils.writeWSResponse(expectedResult, mockResponse));
  }

  /**
   * Tests the doDelete method.
   *
   * @throws Exception if an error occurs during execution
   */
  @Test
  public void testDoDelete() throws Exception {
    // Arrange
    final String path = TestingConstants.TEST_PATH;
    final Map<String, String> params = new HashMap<>();
    params.put(TestingConstants.PARAM_1, TestingConstants.VALUE_1);
    final JSONObject bodyData = new JSONObject(TestingConstants.JSON_STRING);

    mockedOBRestUtils.when(() -> OBRestUtils.requestParamsToMap(mockRequest)).thenReturn(params);
    mockedOBRestUtils.when(() -> OBRestUtils.getBodyData(mockRequest)).thenReturn(bodyData);

    WSResult expectedResult = new WSResult();
    expectedResult.setStatus(Status.OK);
    expectedResult.setMessage("DELETE success");

    doReturn(expectedResult).when(webServiceUnderTest).delete(anyString(), anyMap(), any(JSONObject.class));

    // Act
    webServiceUnderTest.doDelete(path, mockRequest, mockResponse);

    // Assert
    verify(webServiceUnderTest).delete(eq(path), eq(params), eq(bodyData));
    mockedOBRestUtils.verify(() -> OBRestUtils.writeWSResponse(expectedResult, mockResponse));
  }

  /**
   * Tests exception handling in doGet method.
   *
   * @throws Exception if an error occurs during execution
   */
  @Test
  public void testExceptionHandling() throws Exception {
    // Arrange
    final String path = TestingConstants.TEST_PATH;
    final Map<String, String> params = new HashMap<>();

    mockedOBRestUtils.when(() -> OBRestUtils.requestParamsToMap(mockRequest)).thenReturn(params);

    Exception expectedException = new RuntimeException("Test Exception");
    doThrow(expectedException).when(webServiceUnderTest).get(anyString(), anyMap());

    // Act & Assert
    try {
      webServiceUnderTest.doGet(path, mockRequest, mockResponse);
      fail("Expected exception was not thrown");
    } catch (Exception e) {
      assertEquals(expectedException, e);
    }
  }

  private static class TestWebService extends BaseWebService {
    @Override
    public WSResult get(String path, Map<String, String> parameters) {
      WSResult result = new WSResult();
      result.setStatus(Status.OK);
      result.setMessage("GET success");
      return result;
    }

    @Override
    public WSResult post(String path, Map<String, String> parameters, JSONObject body) throws Exception {
      WSResult result = new WSResult();
      result.setStatus(Status.OK);
      result.setMessage("POST success");
      return result;
    }

    @Override
    public WSResult put(String path, Map<String, String> parameters, JSONObject body) throws Exception {
      WSResult result = new WSResult();
      result.setStatus(Status.OK);
      result.setMessage("PUT success");
      return result;
    }

    @Override
    public WSResult delete(String path, Map<String, String> parameters, JSONObject body) throws Exception {
      WSResult result = new WSResult();
      result.setStatus(Status.OK);
      result.setMessage("DELETE success");
      return result;
    }
  }
}
