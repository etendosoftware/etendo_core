package com.etendoerp.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for the {@link PaymentExecutionProcessActionHandler} class.
 */
@RunWith(MockitoJUnitRunner.class)
public class PaymentExecutionProcessActionHandlerTest {

  private static final String PARAM_ID = "id";
  private static final String PARAM_VALUE = "value";
  private static final String PARAM_INPUT_TYPE = "inputType";
  private static final String ERROR_MESSAGE = "Should have 2 parameters";
  private static final String CHECKBOX1 = "checkbox1";
  private static final String CHECKBOX_TYPE = "CHECK";
  private static final String PARAM_1 = "param1";

  /**
   * Tests that {@code generateProcessParametersMap} correctly processes normal parameters
   * with STRING and NUMBER input types.
   *
   * @throws Exception
   *     if an error occurs during JSON processing
   */
  @Test
  public void testGenerateProcessParametersMapWithNormalParameters() throws Exception {
    // Given
    JSONArray paramArray = new JSONArray();

    JSONObject param1 = new JSONObject();
    param1.put(PARAM_ID, PARAM_1);
    param1.put(PARAM_VALUE, "value1");
    param1.put(PARAM_INPUT_TYPE, "STRING");
    paramArray.put(param1);

    JSONObject param2 = new JSONObject();
    param2.put(PARAM_ID, "param2");
    param2.put(PARAM_VALUE, "100");
    param2.put(PARAM_INPUT_TYPE, "NUMBER");
    paramArray.put(param2);

    // When
    Map<String, String> result = PaymentExecutionProcessActionHandler.generateProcessParametersMap(paramArray);

    // Then
    assertEquals(ERROR_MESSAGE, 2, result.size());
    assertEquals("value1", result.get(PARAM_1));
    assertEquals("100", result.get("param2"));
  }

  /**
   * Tests that checkbox-type parameters are correctly converted to "Y" or "N".
   *
   * @throws Exception
   *     if an error occurs during JSON processing
   */
  @Test
  public void testGenerateProcessParametersMapWithCheckboxParameters() throws Exception {
    // Given
    JSONArray paramArray = new JSONArray();

    JSONObject checkParam1 = new JSONObject();
    checkParam1.put(PARAM_ID, CHECKBOX1);
    checkParam1.put(PARAM_VALUE, "true");
    checkParam1.put(PARAM_INPUT_TYPE, CHECKBOX_TYPE);
    paramArray.put(checkParam1);

    JSONObject checkParam2 = new JSONObject();
    checkParam2.put(PARAM_ID, "checkbox2");
    checkParam2.put(PARAM_VALUE, "false");
    checkParam2.put(PARAM_INPUT_TYPE, CHECKBOX_TYPE);
    paramArray.put(checkParam2);

    // When
    Map<String, String> result = PaymentExecutionProcessActionHandler.generateProcessParametersMap(paramArray);

    // Then
    assertEquals(ERROR_MESSAGE, 2, result.size());
    assertEquals("Y", result.get(CHECKBOX1));
    assertEquals("N", result.get("checkbox2"));
  }

  /**
   * Tests that a mix of normal and checkbox parameters is handled correctly.
   *
   * @throws Exception
   *     if an error occurs during JSON processing
   */
  @Test
  public void testGenerateProcessParametersMapWithMixedParameters() throws Exception {
    // Given
    JSONArray paramArray = new JSONArray();

    JSONObject normalParam = new JSONObject();
    normalParam.put(PARAM_ID, PARAM_1);
    normalParam.put(PARAM_VALUE, "normalValue");
    normalParam.put(PARAM_INPUT_TYPE, "STRING");
    paramArray.put(normalParam);

    JSONObject checkParam = new JSONObject();
    checkParam.put(PARAM_ID, CHECKBOX1);
    checkParam.put(PARAM_VALUE, "true");
    checkParam.put(PARAM_INPUT_TYPE, CHECKBOX_TYPE);
    paramArray.put(checkParam);

    // When
    Map<String, String> result = PaymentExecutionProcessActionHandler.generateProcessParametersMap(paramArray);

    // Then
    assertEquals(ERROR_MESSAGE, 2, result.size());
    assertEquals("normalValue", result.get(PARAM_1));
    assertEquals("Y", result.get(CHECKBOX1));
  }

  /**
   * Tests that an empty JSON array results in an empty parameter map.
   *
   * @throws Exception
   *     if an error occurs during JSON processing
   */
  @Test
  public void testGenerateProcessParametersMapWithEmptyArray() throws Exception {
    // Given
    JSONArray paramArray = new JSONArray();

    // When
    Map<String, String> result = PaymentExecutionProcessActionHandler.generateProcessParametersMap(paramArray);

    // Then
    assertTrue("Result map should be empty", result.isEmpty());
  }

  /**
   * Tests that an invalid JSON structure throws a JSONException.
   *
   * @throws Exception
   *     if an error occurs during JSON processing
   */
  @Test(expected = JSONException.class)
  public void testGenerateProcessParametersMapWithInvalidJSON() throws Exception {
    // Given
    JSONArray paramArray = new JSONArray();
    JSONObject invalidParam = new JSONObject();
    // Not setting required fields
    paramArray.put(invalidParam);

    // When
    PaymentExecutionProcessActionHandler.generateProcessParametersMap(paramArray);
    // Then: expect JSONException
  }
}
