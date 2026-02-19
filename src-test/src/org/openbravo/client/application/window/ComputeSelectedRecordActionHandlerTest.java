package org.openbravo.client.application.window;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;

/**
 * Tests for {@link ComputeSelectedRecordActionHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ComputeSelectedRecordActionHandlerTest {

  private ComputeSelectedRecordActionHandler instance;

  @Mock
  private Window mockWindow;

  private MockedStatic<OBContext> obContextStatic;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(ComputeSelectedRecordActionHandler.class);
    obContextStatic = mockStatic(OBContext.class);
  }

  @After
  public void tearDown() {
    if (obContextStatic != null) obContextStatic.close();
  }

  @Test
  public void testGetTabReturnsMatchingTab() throws Exception {
    Tab activeTab = createMockTab("Entity1", true, true);
    Tab inactiveTab = createMockTab("Entity2", false, true);
    List<Tab> tabs = new ArrayList<>();
    tabs.add(activeTab);
    tabs.add(inactiveTab);
    when(mockWindow.getADTabList()).thenReturn(tabs);

    Method getTab = ComputeSelectedRecordActionHandler.class.getDeclaredMethod("getTab",
        Window.class, String.class);
    getTab.setAccessible(true);

    Tab result = (Tab) getTab.invoke(instance, mockWindow, "Entity1");

    assertNotNull(result);
    assertEquals(activeTab, result);
  }

  @Test
  public void testGetTabReturnsNullForInactiveTab() throws Exception {
    Tab inactiveTab = createMockTab("Entity1", false, true);
    List<Tab> tabs = new ArrayList<>();
    tabs.add(inactiveTab);
    when(mockWindow.getADTabList()).thenReturn(tabs);

    Method getTab = ComputeSelectedRecordActionHandler.class.getDeclaredMethod("getTab",
        Window.class, String.class);
    getTab.setAccessible(true);

    Tab result = (Tab) getTab.invoke(instance, mockWindow, "Entity1");

    assertNull(result);
  }

  @Test
  public void testGetTabReturnsNullForDisabledModule() throws Exception {
    Tab tab = createMockTab("Entity1", true, false);
    List<Tab> tabs = new ArrayList<>();
    tabs.add(tab);
    when(mockWindow.getADTabList()).thenReturn(tabs);

    Method getTab = ComputeSelectedRecordActionHandler.class.getDeclaredMethod("getTab",
        Window.class, String.class);
    getTab.setAccessible(true);

    Tab result = (Tab) getTab.invoke(instance, mockWindow, "Entity1");

    assertNull(result);
  }

  @Test
  public void testGetTabReturnsNullForNonMatchingEntity() throws Exception {
    Tab tab = createMockTab("Entity1", true, true);
    List<Tab> tabs = new ArrayList<>();
    tabs.add(tab);
    when(mockWindow.getADTabList()).thenReturn(tabs);

    Method getTab = ComputeSelectedRecordActionHandler.class.getDeclaredMethod("getTab",
        Window.class, String.class);
    getTab.setAccessible(true);

    Tab result = (Tab) getTab.invoke(instance, mockWindow, "NonExistentEntity");

    assertNull(result);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testExecuteWithParametersThrowsUnsupportedOperationException() throws Throwable {
    Method execute = ComputeSelectedRecordActionHandler.class.getDeclaredMethod("execute",
        Map.class, String.class);
    execute.setAccessible(true);
    try {
      execute.invoke(instance, null, null);
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw e.getCause();
    }
  }

  private Tab createMockTab(String entityName, boolean active, boolean moduleEnabled) {
    Tab tab = mock(Tab.class);
    Module module = mock(Module.class);
    org.openbravo.model.ad.datamodel.Table table = mock(
        org.openbravo.model.ad.datamodel.Table.class);

    when(tab.isActive()).thenReturn(active);
    when(tab.getModule()).thenReturn(module);
    lenient().when(module.isEnabled()).thenReturn(moduleEnabled);
    lenient().when(tab.getTable()).thenReturn(table);
    lenient().when(table.getName()).thenReturn(entityName);

    return tab;
  }
}
