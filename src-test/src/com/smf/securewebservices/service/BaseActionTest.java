package com.smf.securewebservices.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.smf.securewebservices.utils.WSResult;

/**
 * Unit tests for the BaseAction class.
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseActionTest {

  private JSONObject testResponse;
  private Map<String, String> testParameters;
  private String testPath;
  private TestAction classUnderTest;

  /**
   * Sets up the test environment before each test.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Before
  public void setUp() throws JSONException {
    testResponse = new JSONObject();
    testResponse.put("success", true);
    testResponse.put("message", "Test response");

    testParameters = new HashMap<>();
    testParameters.put("param1", "value1");
    testParameters.put("param2", "value2");

    testPath = "/test/path";

    classUnderTest = new TestAction(testResponse);
  }

  /**
   * Tests the post method for a successful execution.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testPostHappyPath() throws Exception {
    // GIVEN

    // WHEN
    WSResult result = classUnderTest.post(testPath, testParameters, new JSONObject());

    // THEN
    assertEquals(WSResult.Status.OK, result.getStatus());
    assertEquals(WSResult.ResultType.SINGLE, result.getResultType());
    assertNotNull(result.getJSONResponse());
    assertEquals(testResponse, result.getJSONResponse().getJSONObject("action"));
  }

  /**
   * Tests the post method when an exception is thrown.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testPostWithException() throws Exception {
    // GIVEN
    classUnderTest.setThrowException(true);

    // WHEN
    WSResult result = classUnderTest.post(testPath, testParameters, new JSONObject());

    // THEN
    assertEquals(WSResult.Status.INTERNAL_SERVER_ERROR, result.getStatus());
  }

  /**
   * Tests the get method to ensure it calls the post method correctly.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testGetCallsPostCorrectly() throws Exception {
    // GIVEN
    TestAction spyAction = spy(classUnderTest);

    // WHEN
    WSResult result = spyAction.get(testPath, testParameters);

    // THEN
    verify(spyAction).post(eq(testPath), eq(testParameters), any(JSONObject.class));
    assertEquals(WSResult.Status.OK, result.getStatus());
  }

  private static class TestAction extends BaseAction {
    private final JSONObject responseObject;
    private boolean throwException = false;

    public TestAction(JSONObject responseObject) {
      this.responseObject = responseObject;
    }

    public void setThrowException(boolean throwException) {
      this.throwException = throwException;
    }

    @Override
    public JSONObject execute(JSONObject params) throws Exception {
      if (throwException) {
        throw new Exception("Test exception");
      }
      return responseObject;
    }
  }
}
