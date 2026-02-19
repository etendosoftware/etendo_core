package com.smf.jobs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.structure.BaseOBObject;

/**
 * Unit tests for Action abstract class.
 * Tests concrete methods available in the abstract class.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ActionTest {

  private Action instance;
  private ObjenesisStd objenesis;

  /**
   * Concrete stub subclass for testing
   */
  private static class TestAction extends Action {

    @Override
    protected ActionResult action(JSONObject parameters, org.apache.commons.lang3.mutable.MutableBoolean isStopped) {
      return new ActionResult();
    }

    @Override
    protected Class<?> getInputClass() {
      return BaseOBObject.class;
    }
  }

  @Before
  public void setUp() throws Exception {
    objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(TestAction.class);
  }

  @Test
  public void testPreRunReturnsInput() throws Exception {
    // Arrange
    Data mockData = mock(Data.class);
    Field inputField = Action.class.getDeclaredField("input");
    inputField.setAccessible(true);
    inputField.set(instance, mockData);

    // Act
    Data result = instance.preRun(new JSONObject());

    // Assert
    assertSame(mockData, result);
  }

  @Test
  public void testPostRunReturnsResult() {
    ActionResult actionResult = new ActionResult();
    actionResult.setType(Result.Type.SUCCESS);
    actionResult.setMessage("Test message");

    ActionResult returned = instance.postRun(actionResult);
    assertSame(actionResult, returned);
  }

  @Test
  public void testSetAndGetParameters() throws Exception {
    JSONObject params = new JSONObject();
    params.put("key1", "value1");

    Method setMethod = Action.class.getDeclaredMethod("setParameters", JSONObject.class);
    setMethod.setAccessible(true);
    setMethod.invoke(instance, params);

    Field paramsField = Action.class.getDeclaredField("parameters");
    paramsField.setAccessible(true);
    JSONObject storedParams = (JSONObject) paramsField.get(instance);

    assertEquals("value1", storedParams.getString("key1"));
  }

  @Test
  public void testSetAndGetInput() throws Exception {
    Data mockData = mock(Data.class);

    Method setInput = Action.class.getDeclaredMethod("setInput", Data.class);
    setInput.setAccessible(true);
    setInput.invoke(instance, mockData);

    Method getInput = Action.class.getDeclaredMethod("getInput");
    getInput.setAccessible(true);
    Data result = (Data) getInput.invoke(instance);

    assertSame(mockData, result);
  }

  @Test
  public void testSetAndGetRequestParameters() throws Exception {
    Map<String, Object> params = new HashMap<>();
    params.put("paramA", "valueA");

    Method setMethod = Action.class.getDeclaredMethod("setRequestParameters", Map.class);
    setMethod.setAccessible(true);
    setMethod.invoke(instance, params);

    Method getMethod = Action.class.getDeclaredMethod("getRequestParameters");
    getMethod.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) getMethod.invoke(instance);

    assertEquals("valueA", result.get("paramA"));
  }

  @Test
  public void testGetOutputClassReturnsNull() throws Exception {
    Method method = Action.class.getDeclaredMethod("getOutputClass");
    method.setAccessible(true);
    Object result = method.invoke(instance);

    assertNull(result);
  }

  @Test
  public void testRunPreActionHooksWithNullHooks() throws Exception {
    // When preHooks is null, runPreActionHooks should return without error
    Field preHooksField = Action.class.getDeclaredField("preHooks");
    preHooksField.setAccessible(true);
    preHooksField.set(instance, null);

    Method runPreHooks = Action.class.getDeclaredMethod("runPreActionHooks", JSONObject.class);
    runPreHooks.setAccessible(true);

    // Should not throw
    runPreHooks.invoke(instance, new JSONObject());
  }

  @Test
  public void testRunPostActionHooksWithNullHooks() throws Exception {
    // When postHooks is null, runPostActionHooks should return without error
    Field postHooksField = Action.class.getDeclaredField("postHooks");
    postHooksField.setAccessible(true);
    postHooksField.set(instance, null);

    Method runPostHooks = Action.class.getDeclaredMethod("runPostActionHooks", JSONObject.class, ActionResult.class);
    runPostHooks.setAccessible(true);

    // Should not throw
    runPostHooks.invoke(instance, new JSONObject(), new ActionResult());
  }

  @Test
  public void testParamsConstant() {
    assertEquals("_params", Action.PARAMS);
  }
}
