package org.openbravo.client.application.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;

@RunWith(MockitoJUnitRunner.class)
public class GridExampleActionHandlerTest {

  private GridExampleActionHandler instance;
  private MockedStatic<OBContext> obContextStatic;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(GridExampleActionHandler.class);
    obContextStatic = mockStatic(OBContext.class);
  }

  @After
  public void tearDown() {
    if (obContextStatic != null) {
      obContextStatic.close();
    }
  }

  @Test(expected = OBException.class)
  public void testExecuteWithoutCommandParamThrows() throws Throwable {
    Map<String, Object> params = new HashMap<>();
    // No _command parameter
    Method execute = GridExampleActionHandler.class.getDeclaredMethod("execute", Map.class, String.class);
    execute.setAccessible(true);
    try {
      execute.invoke(instance, params, "{}");
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw e.getCause();
    }
  }

  @Test(expected = OBException.class)
  public void testExecuteWithUnknownCommandThrows() throws Throwable {
    Map<String, Object> params = new HashMap<>();
    params.put("_command", "unknown");
    Method execute = GridExampleActionHandler.class.getDeclaredMethod("execute", Map.class, String.class);
    execute.setAccessible(true);
    try {
      execute.invoke(instance, params, "{}");
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw e.getCause();
    }
  }

  @Test
  public void testDoExecuteCommandWithSelectedRecords() throws Exception {
    JSONObject record1 = new JSONObject();
    record1.put("id", "ID1");
    record1.put("name", "Module A");

    JSONObject record2 = new JSONObject();
    record2.put("id", "ID2");
    record2.put("name", "Module B");

    JSONArray selectedRecords = new JSONArray();
    selectedRecords.put(record1);
    selectedRecords.put(record2);

    JSONObject content = new JSONObject();
    content.put("selectedRecords", selectedRecords);

    Method doExecuteCommand = GridExampleActionHandler.class.getDeclaredMethod(
        "doExecuteCommand", Map.class, String.class);
    doExecuteCommand.setAccessible(true);

    Map<String, Object> params = new HashMap<>();
    JSONObject result = (JSONObject) doExecuteCommand.invoke(instance, params, content.toString());

    assertNotNull(result);
    String message = result.getString("message");
    assertTrue(message.contains("Processed 2 records"));
    assertTrue(message.contains("Module A"));
    assertTrue(message.contains("Module B"));
  }

  @Test
  public void testDoExecuteCommandWithEmptySelection() throws Exception {
    JSONObject content = new JSONObject();
    content.put("selectedRecords", new JSONArray());

    Method doExecuteCommand = GridExampleActionHandler.class.getDeclaredMethod(
        "doExecuteCommand", Map.class, String.class);
    doExecuteCommand.setAccessible(true);

    Map<String, Object> params = new HashMap<>();
    JSONObject result = (JSONObject) doExecuteCommand.invoke(instance, params, content.toString());

    assertNotNull(result);
    String message = result.getString("message");
    assertTrue(message.contains("Processed 0 records"));
  }
}
