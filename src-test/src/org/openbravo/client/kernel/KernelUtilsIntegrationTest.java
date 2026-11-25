/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.client.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.module.Module;
import org.openbravo.test.base.OBBaseTest;

/**
 * Integration tests for {@link KernelUtils}.
 * Requires a running database with the Etendo model loaded.
 */
public class KernelUtilsIntegrationTest extends OBBaseTest {

  @Test
  public void testGetInstanceReturnsNonNull() {
    setSystemAdministratorContext();
    assertNotNull(KernelUtils.getInstance());
  }

  @Test
  public void testGetModulesOrderedByDependencyReturnsNonEmpty() {
    setSystemAdministratorContext();
    List<Module> modules = KernelUtils.getInstance().getModulesOrderedByDependency();
    assertNotNull(modules);
    assertFalse("Module list should not be empty", modules.isEmpty());
  }

  @Test
  public void testCoreModuleIsFirst() {
    setSystemAdministratorContext();
    List<Module> modules = KernelUtils.getInstance().getModulesOrderedByDependency();
    assertEquals("Core module should be first", "0", modules.get(0).getId());
  }

  @Test
  public void testIsModulePresentForCorePackage() {
    setSystemAdministratorContext();
    assertTrue("org.openbravo should be present",
        KernelUtils.getInstance().isModulePresent("org.openbravo"));
  }

  @Test
  public void testIsModulePresentForNonExistentPackage() {
    setSystemAdministratorContext();
    assertFalse("non.existent.package should not be present",
        KernelUtils.getInstance().isModulePresent("non.existent.package"));
  }

  @Test
  public void testGetModuleForCorePackage() {
    setSystemAdministratorContext();
    Module module = KernelUtils.getInstance().getModule("org.openbravo");
    assertNotNull(module);
    assertEquals("0", module.getId());
  }

  @Test
  public void testGetPropertyFromColumnFindsProperty() {
    setSystemAdministratorContext();
    OBContext.setAdminMode(true);
    try {
      // AD_Client.Name column (ad_column_id = 208)
      Column nameColumn = OBDal.getInstance().get(Column.class, "208");
      assertNotNull("Name column of AD_Client should exist", nameColumn);
      Property prop = KernelUtils.getInstance().getPropertyFromColumn(nameColumn);
      assertNotNull("Should find property for Name column", prop);
      assertEquals("name", prop.getName());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCreateErrorJSONForSystemException() {
    setSystemAdministratorContext();
    Exception ex = new RuntimeException("Test error message");
    JSONObject errorJson = KernelUtils.getInstance().createErrorJSON(ex);
    assertNotNull(errorJson);
    assertEquals("system", errorJson.optString("type"));
    assertEquals("Test error message", errorJson.optString("message"));
  }

  @Test
  public void testCreateErrorJavaScriptContainsMessage() {
    setSystemAdministratorContext();
    Exception ex = new RuntimeException("Something went wrong");
    String js = KernelUtils.getInstance().createErrorJavaScript(ex);
    assertNotNull(js);
    assertTrue("Should contain the error message", js.contains("Something went wrong"));
    assertTrue("Should call handleSystemException",
        js.contains("OB.KernelUtilities.handleSystemException"));
  }

  @Test
  public void testGetVersionParametersFormat() {
    setSystemAdministratorContext();
    List<Module> modules = KernelUtils.getInstance().getModulesOrderedByDependency();
    Module coreModule = modules.get(0);
    String params = KernelUtils.getInstance().getVersionParameters(coreModule);
    assertNotNull(params);
    assertTrue("Should contain version parameter",
        params.contains(KernelConstants.RESOURCE_VERSION_PARAMETER + "="));
    assertTrue("Should contain language parameter",
        params.contains(KernelConstants.RESOURCE_LANGUAGE_PARAMETER + "="));
  }
}
