package org.openbravo.client.application.window.hooks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.session.OBPropertiesProvider;

/**
 * Tests for {@link DataPoolSelectionWindowInjector}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DataPoolSelectionWindowInjectorTest {

  private static final String WINDOW_ID = "windowId";

  private static final String DATA_POOL_SEL_WINDOW_ID = "48B7215F9BF6458E813E6B280DEDB958";

  private DataPoolSelectionWindowInjector injector;

  @Mock
  private OBPropertiesProvider mockPropertiesProvider;

  private MockedStatic<OBPropertiesProvider> propertiesProviderStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    injector = new DataPoolSelectionWindowInjector();
    propertiesProviderStatic = mockStatic(OBPropertiesProvider.class);
    propertiesProviderStatic.when(OBPropertiesProvider::getInstance)
        .thenReturn(mockPropertiesProvider);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (propertiesProviderStatic != null) propertiesProviderStatic.close();
  }
  /**
   * Do add setting returns empty map for non matching window.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoAddSettingReturnsEmptyMapForNonMatchingWindow() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(WINDOW_ID, "SOME_OTHER_WINDOW_ID");

    Map<String, Object> result = injector.doAddSetting(parameters, new JSONObject());

    assertTrue(result.isEmpty());
  }
  /**
   * Do add setting returns settings when ro pool not available.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoAddSettingReturnsSettingsWhenROPoolNotAvailable() throws Exception {
    Properties properties = new Properties();
    when(mockPropertiesProvider.getOpenbravoProperties()).thenReturn(properties);

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(WINDOW_ID, DATA_POOL_SEL_WINDOW_ID);

    Map<String, Object> result = injector.doAddSetting(parameters, new JSONObject());

    assertFalse(result.isEmpty());
    assertEquals("OBUIAPP_ROPoolNotAvailable", result.get("messageKey"));
    assertEquals("D829B2F06F444694B7080C9BA19428E6", result.get("tabId"));
    assertNotNull(result.get("extraCallbacks"));
    List<?> callbacks = (List<?>) result.get("extraCallbacks");
    assertEquals(1, callbacks.size());
    assertEquals("OB.Utilities.ExtraWindowSettingsActions.showInfoMessage", callbacks.get(0));
  }
  /**
   * Do add setting returns empty map when ro pool available.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoAddSettingReturnsEmptyMapWhenROPoolAvailable() throws Exception {
    Properties properties = new Properties();
    properties.put("bbdd.readonly.url", "jdbc:postgresql://localhost:5432/readonly");
    when(mockPropertiesProvider.getOpenbravoProperties()).thenReturn(properties);

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(WINDOW_ID, DATA_POOL_SEL_WINDOW_ID);

    Map<String, Object> result = injector.doAddSetting(parameters, new JSONObject());

    assertTrue(result.isEmpty());
  }
  /**
   * Do add setting returns empty map when no window id.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoAddSettingReturnsEmptyMapWhenNoWindowId() throws Exception {
    Map<String, Object> parameters = new HashMap<>();

    Map<String, Object> result = injector.doAddSetting(parameters, new JSONObject());

    assertTrue(result.isEmpty());
  }
}
