package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.ParameterUtils;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.DbUtility;

/**
 * Test class for AddPaymentDisplayLogicActionHandler.
 */
public class AddPaymentDisplayLogicActionHandlerTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AddPaymentDisplayLogicActionHandler actionHandler;

  @Mock
  private Parameter mockParameter;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<DbUtility> mockedDbUtility;
  private MockedStatic<ParameterUtils> mockedParameterUtils;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    actionHandler = new AddPaymentDisplayLogicActionHandler();

    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedDbUtility = mockStatic(DbUtility.class);
    mockedParameterUtils = mockStatic(ParameterUtils.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mock(OBDal.class));
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) mockedOBDal.close();
    if (mockedOBContext != null) mockedOBContext.close();
    if (mockedOBMessageUtils != null) mockedOBMessageUtils.close();
    if (mockedDbUtility != null) mockedDbUtility.close();
    if (mockedParameterUtils != null) mockedParameterUtils.close();
  }

  /**
   * Tests the execute method for a successful execution.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteHappyPath() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    HttpSession mockSession = mock(HttpSession.class);
    parameters.put(KernelConstants.HTTP_SESSION, mockSession);

    String jsonData = createValidJsonData();

    when(OBDal.getInstance().get(Parameter.class, TestConstants.PARAM_ID_1)).thenReturn(mockParameter);
    when(mockParameter.getDBColumnName()).thenReturn("columnName1");
    when(mockParameter.getDefaultValue()).thenReturn("defaultValue1");

    mockedParameterUtils.when(() -> ParameterUtils.getJSExpressionResult(any(), any(), anyString())).thenReturn(
        "calculatedValue1");

    JSONObject result = actionHandler.execute(parameters, jsonData);

    assertNotNull(result);
    assertNotNull(result.getJSONObject("values"));
    assertEquals("calculatedValue1", result.getJSONObject("values").get("columnName1"));
  }

  /**
   * Tests the execute method for an error scenario.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteErrorScenario() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    HttpSession mockSession = mock(HttpSession.class);
    parameters.put(KernelConstants.HTTP_SESSION, mockSession);

    String jsonData = createValidJsonData();

    // Simulate an exception
    when(OBDal.getInstance().get(Parameter.class, TestConstants.PARAM_ID_1)).thenThrow(new RuntimeException("Test Error"));

    RuntimeException mockException = new RuntimeException("Underlying Error");
    mockedDbUtility.when(() -> DbUtility.getUnderlyingSQLException(any())).thenReturn(mockException);

    // Create and configure OBError mock
    OBError mockError = new OBError();
    mockError.setMessage("Error message");
    mockedOBMessageUtils.when(() -> OBMessageUtils.translateError(anyString())).thenReturn(mockError);

    JSONObject result = actionHandler.execute(parameters, jsonData);

    assertNotNull(result);
    assertNotNull(result.getJSONObject(TestConstants.RESPONSE_MESSAGE));
    assertEquals("error", result.getJSONObject(TestConstants.RESPONSE_MESSAGE).getString("severity"));
  }

  /**
   * Tests the fixRequestMap method.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testFixRequestMap() throws Exception {
    Map<String, Object> inputMap = new HashMap<>();
    inputMap.put("key1", "value1");
    inputMap.put("key2", "value2");
    inputMap.put(KernelConstants.HTTP_REQUEST, "requestObject");
    inputMap.put(KernelConstants.HTTP_SESSION, "sessionObject");

    // Cast Map to Map<String, Object> explicitly
    Map<String, String> result = invokePrivateMethod(actionHandler, "fixRequestMap", new Object[]{ inputMap },
        Map.class);

    assertEquals(2, result.size());
    assertEquals("value1", result.get("key1"));
    assertEquals("value2", result.get("key2"));
  }

  /**
   * Creates valid JSON data for testing.
   *
   * @return a valid JSON data string
   * @throws Exception
   *     if an error occurs during JSON creation
   */
  private String createValidJsonData() throws Exception {
    JSONObject jsonData = new JSONObject();
    jsonData.put("affectedParams", new JSONArray(Arrays.toString(new String[]{ TestConstants.PARAM_ID_1 })));
    jsonData.put("params", new JSONObject());
    return jsonData.toString();
  }

  /**
   * Invokes a private method using reflection.
   *
   * @param obj
   *     the object on which to invoke the method
   * @param methodName
   *     the name of the method to invoke
   * @param args
   *     the arguments to pass to the method
   * @param parameterTypes
   *     the parameter types of the method
   * @param <T>
   *     the return type of the method
   * @return the result of the method invocation
   * @throws Exception
   *     if an error occurs during method invocation
   */
  private <T> T invokePrivateMethod(Object obj, String methodName, Object[] args,
      Class<?>... parameterTypes) throws Exception {
    java.lang.reflect.Method method = obj.getClass().getDeclaredMethod(methodName, parameterTypes);
    method.setAccessible(true);
    return (T) method.invoke(obj, args);
  }
}
