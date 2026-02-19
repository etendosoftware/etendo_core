/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
@RunWith(MockitoJUnitRunner.Silent.class)
public class AboutTest {

  private About about;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    about = objenesis.newInstance(About.class);
  }

  // --- getModuleHashMap tests ---

  @Test
  public void testGetModuleHashMapBasicFields() throws Exception {
    ModuleTreeData module = createModule("Test Module", "1.0.0", null, "Author1", "node1");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 1);

    assertNotNull(result);
    assertEquals("Test Module", result.get("moduleName"));
    assertEquals("1.0.0", result.get("moduleVersion"));
    assertEquals("Author1", result.get("moduleAuthor"));
  }

  @Test
  public void testGetModuleHashMapWithVersionLabel() throws Exception {
    ModuleTreeData module = createModule("Module A", "2.0.0", "beta", "Author2", "node2");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 1);

    assertEquals("2.0.0 - beta", result.get("moduleVersion"));
  }

  @Test
  public void testGetModuleHashMapWithEmptyVersionLabel() throws Exception {
    ModuleTreeData module = createModule("Module B", "3.0.0", "", "Author3", "node3");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 1);

    assertEquals("3.0.0", result.get("moduleVersion"));
  }

  @Test
  public void testGetModuleHashMapWithNullVersionLabel() throws Exception {
    ModuleTreeData module = createModule("Module C", "4.0.0", null, "Author4", "node4");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 1);

    assertEquals("4.0.0", result.get("moduleVersion"));
  }

  @Test
  public void testGetModuleHashMapWithNullAuthor() throws Exception {
    ModuleTreeData module = createModule("Module D", "1.0.0", null, null, "node5");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 1);

    assertEquals("-", result.get("moduleAuthor"));
  }

  @Test
  public void testGetModuleHashMapWithEmptyAuthor() throws Exception {
    ModuleTreeData module = createModule("Module E", "1.0.0", null, "", "node6");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 1);

    assertEquals("-", result.get("moduleAuthor"));
  }

  @Test
  public void testGetModuleHashMapLevelOneTab() throws Exception {
    ModuleTreeData module = createModule("Module F", "1.0.0", null, "Author", "node7");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 1);

    assertEquals("&nbsp;&nbsp;&nbsp;", result.get("modTab"));
  }

  @Test
  public void testGetModuleHashMapLevelTwoTab() throws Exception {
    ModuleTreeData module = createModule("Module G", "1.0.0", null, "Author", "node8");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 2);

    assertEquals("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", result.get("modTab"));
  }

  @Test
  public void testGetModuleHashMapLevelZeroTab() throws Exception {
    ModuleTreeData module = createModule("Module H", "1.0.0", null, "Author", "node9");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 0);

    assertEquals("", result.get("modTab"));
  }

  @Test
  public void testGetModuleHashMapLevelThreeTab() throws Exception {
    ModuleTreeData module = createModule("Module I", "1.0.0", null, "Author", "node10");

    HashMap<String, String> result = invokeGetModuleHashMap(module, 3);

    String expectedTab = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
    assertEquals(expectedTab, result.get("modTab"));
  }

  // --- getServletInfo test ---

  @Test
  public void testGetServletInfoReturnsExpectedString() {
    String info = about.getServletInfo();
    assertNotNull(info);
    assertEquals(true, info.contains("DebtPaymentUnapply"));
  }

  // --- Helper methods ---

  @SuppressWarnings("unchecked")
  private HashMap<String, String> invokeGetModuleHashMap(ModuleTreeData module, int level)
      throws Exception {
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
