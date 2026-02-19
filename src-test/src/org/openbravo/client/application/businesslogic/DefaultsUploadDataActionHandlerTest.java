package org.openbravo.client.application.businesslogic;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.dal.core.OBContext;

/**
 * Tests for {@link DefaultsUploadDataActionHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultsUploadDataActionHandlerTest {

  private DefaultsUploadDataActionHandler handler;

  private MockedStatic<OBContext> obContextStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    handler = objenesis.newInstance(DefaultsUploadDataActionHandler.class);
    obContextStatic = mockStatic(OBContext.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obContextStatic != null) obContextStatic.close();
  }
  /**
   * Execute returns json with defaults.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteReturnsJsonWithDefaults() throws Exception {
    Map<String, Object> parameters = new HashMap<>();

    Method execute = DefaultsUploadDataActionHandler.class.getDeclaredMethod("execute",
        Map.class, String.class);
    execute.setAccessible(true);
    JSONObject result = (JSONObject) execute.invoke(handler, parameters, "{}");

    assertNotNull(result);
    assertTrue(result.has("defaults"));
  }
  /**
   * Execute returns empty defaults.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteReturnsEmptyDefaults() throws Exception {
    Map<String, Object> parameters = new HashMap<>();

    Method execute = DefaultsUploadDataActionHandler.class.getDeclaredMethod("execute",
        Map.class, String.class);
    execute.setAccessible(true);
    JSONObject result = (JSONObject) execute.invoke(handler, parameters, "{}");

    assertNotNull(result);
    JSONObject defaults = result.getJSONObject("defaults");
    assertEquals(0, defaults.length());
  }

  private void assertEquals(int expected, int actual) {
    org.junit.Assert.assertEquals(expected, actual);
  }
}
