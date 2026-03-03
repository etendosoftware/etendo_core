/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.erpCommon.modules.ModuleTreeData;

/**
 * Tests for {@link About}.
 */
@SuppressWarnings({"java:S120", "java:S112"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class AboutTest {

  private static final String VAL_1_0_0 = "1.0.0";
  private static final String MODULE_VERSION = "moduleVersion";
  private static final String MODULE_AUTHOR = "moduleAuthor";
  private static final String AUTHOR = "Author";
  private static final String MOD_TAB = "modTab";

  private About about;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    about = objenesis.newInstance(About.class);
  }

  // --- getModuleHashMap tests ---
  /**
   * Get module hash map basic fields.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetModuleHashMapBasicFields() throws Exception {
    ModuleTreeData module = createModule("Test Module", VAL_1_0_0, null, "Author1", "node1");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 1);

    assertNotNull(result);
    assertEquals("Test Module", result.get("moduleName"));
    assertEquals(VAL_1_0_0, result.get(MODULE_VERSION));
    assertEquals("Author1", result.get(MODULE_AUTHOR));
  }
  /**
   * Get module hash map with version label.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetModuleHashMapWithVersionLabel() throws Exception {
    ModuleTreeData module = createModule("Module A", "2.0.0", "beta", "Author2", "node2");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 1);

    assertEquals("2.0.0 - beta", result.get(MODULE_VERSION));
  }
  /**
   * Get module hash map with empty version label.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetModuleHashMapWithEmptyVersionLabel() throws Exception {
    ModuleTreeData module = createModule("Module B", "3.0.0", "", "Author3", "node3");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 1);

    assertEquals("3.0.0", result.get(MODULE_VERSION));
  }
  /**
   * Get module hash map with null version label.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetModuleHashMapWithNullVersionLabel() throws Exception {
    ModuleTreeData module = createModule("Module C", "4.0.0", null, "Author4", "node4");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 1);

    assertEquals("4.0.0", result.get(MODULE_VERSION));
  }
  /**
   * Get module hash map with null author.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetModuleHashMapWithNullAuthor() throws Exception {
    ModuleTreeData module = createModule("Module D", VAL_1_0_0, null, null, "node5");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 1);

    assertEquals("-", result.get(MODULE_AUTHOR));
  }
  /**
   * Get module hash map with empty author.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetModuleHashMapWithEmptyAuthor() throws Exception {
    ModuleTreeData module = createModule("Module E", VAL_1_0_0, null, "", "node6");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 1);

    assertEquals("-", result.get(MODULE_AUTHOR));
  }
  /**
   * Get module hash map level one tab.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetModuleHashMapLevelOneTab() throws Exception {
    ModuleTreeData module = createModule("Module F", VAL_1_0_0, null, AUTHOR, "node7");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 1);

    assertEquals("&nbsp;&nbsp;&nbsp;", result.get(MOD_TAB));
  }
  /**
   * Get module hash map level two tab.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetModuleHashMapLevelTwoTab() throws Exception {
    ModuleTreeData module = createModule("Module G", VAL_1_0_0, null, AUTHOR, "node8");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 2);

    assertEquals("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", result.get(MOD_TAB));
  }
  /**
   * Get module hash map level zero tab.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetModuleHashMapLevelZeroTab() throws Exception {
    ModuleTreeData module = createModule("Module H", VAL_1_0_0, null, AUTHOR, "node9");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 0);

    assertEquals("", result.get(MOD_TAB));
  }
  /**
   * Get module hash map level three tab.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetModuleHashMapLevelThreeTab() throws Exception {
    ModuleTreeData module = createModule("Module I", VAL_1_0_0, null, AUTHOR, "node10");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 3);

    String expectedTab = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
    assertEquals(expectedTab, result.get(MOD_TAB));
  }

  // --- getServletInfo test ---
  /** Get servlet info returns expected string. */

  @Test
  public void testGetServletInfoReturnsExpectedString() {
    String info = about.getServletInfo();
    assertNotNull(info);
    assertEquals(true, info.contains("DebtPaymentUnapply"));
  }

  // --- Helper methods ---

  @SuppressWarnings("unchecked")
  private HashMap<String, String> invokeGetModuleHashMap(ModuleTreeData module, int level)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = About.class.getDeclaredMethod("getModuleHashMap", ModuleTreeData.class,
        int.class);
    method.setAccessible(true);
    return (HashMap<String, String>) method.invoke(about, module, level);
  }

  private ModuleTreeData createModule(String name, String version, String versionLabel,
      String author, String nodeId) {
    ObjenesisStd objenesis = new ObjenesisStd();
    ModuleTreeData module = objenesis.newInstance(ModuleTreeData.class);
    module.modulename = name;
    module.version = version;
    module.versionLabel = versionLabel;
    module.author = author;
    module.nodeId = nodeId;
    return module;
  }
}
