package org.openbravo.client.application.window;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.erpCommon.utility.OBError;

@RunWith(MockitoJUnitRunner.class)
public class GetTabMessageActionHandlerTest {

  private GetTabMessageActionHandler instance;

  @Mock
  private RequestContext mockRequestContext;

  private MockedStatic<RequestContext> rcStatic;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(GetTabMessageActionHandler.class);
    rcStatic = mockStatic(RequestContext.class);
    rcStatic.when(RequestContext::get).thenReturn(mockRequestContext);
  }

  @After
  public void tearDown() {
    if (rcStatic != null) {
      rcStatic.close();
    }
  }

  @Test
  public void testExecuteWithMessageInSession() throws Exception {
    String tabId = "TAB123";
    OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle("Done");
    msg.setMessage("Operation completed");

    when(mockRequestContext.getSessionAttribute(tabId + "|MESSAGE")).thenReturn(msg);

    Map<String, Object> params = new HashMap<>();
    String content = new JSONObject().put("tabId", tabId).toString();

    Method execute = GetTabMessageActionHandler.class.getDeclaredMethod("execute", Map.class, String.class);
    execute.setAccessible(true);
    JSONObject result = (JSONObject) execute.invoke(instance, params, content);

    assertEquals("TYPE_SUCCESS", result.getString("type"));
    assertEquals("Done", result.getString("title"));
    assertEquals("Operation completed", result.getString("text"));
    verify(mockRequestContext).removeSessionAttribute(tabId + "|MESSAGE");
  }

  @Test
  public void testExecuteWithNoMessageInSession() throws Exception {
    String tabId = "TAB456";
    when(mockRequestContext.getSessionAttribute(tabId + "|MESSAGE")).thenReturn(null);

    Map<String, Object> params = new HashMap<>();
    String content = new JSONObject().put("tabId", tabId).toString();

    Method execute = GetTabMessageActionHandler.class.getDeclaredMethod("execute", Map.class, String.class);
    execute.setAccessible(true);
    JSONObject result = (JSONObject) execute.invoke(instance, params, content);

    assertNotNull(result);
    assertFalse(result.has("type"));
  }

  @Test
  public void testExecuteWithInvalidJsonReturnsEmptyResult() throws Exception {
    Map<String, Object> params = new HashMap<>();
    String content = "not valid json";

    Method execute = GetTabMessageActionHandler.class.getDeclaredMethod("execute", Map.class, String.class);
    execute.setAccessible(true);
    JSONObject result = (JSONObject) execute.invoke(instance, params, content);

    assertNotNull(result);
    assertFalse(result.has("type"));
  }
}
