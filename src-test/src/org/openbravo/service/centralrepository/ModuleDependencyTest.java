package org.openbravo.service.centralrepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.openbravo.base.exception.OBException;

/**
 * Unit tests for the ModuleDependency class.
 * Verifies the behavior of JSON parsing and object manipulation methods.
 */
class ModuleDependencyTest {

  /**
   * Tests the fromJson method with a valid JSON array.
   * Verifies that the method correctly parses the array into ModuleDependency objects.
   *
   * @throws JSONException
   *     if an error occurs during JSON processing
   */
  @Test
  void testFromJsonValidJsonArray() throws JSONException {
    JSONArray jsonArray = new JSONArray();
    JSONObject jsonObj1 = new JSONObject();
    jsonObj1.put(CentralRepositoryTestConstants.MODULE_ID, CentralRepositoryTestConstants.MODULE_1);
    jsonObj1.put(CentralRepositoryTestConstants.MODULE_NAME, CentralRepositoryTestConstants.MODULE_1_NAME);
    jsonObj1.put(CentralRepositoryTestConstants.MODULE_VERSION_DEPENDENCY_ID, "1.0");
    jsonObj1.put(CentralRepositoryTestConstants.VERSION_START, CentralRepositoryTestConstants.VERSION_1_0_0);
    jsonObj1.put(CentralRepositoryTestConstants.VERSION_END, CentralRepositoryTestConstants.VERSION_2_0_0);
    jsonArray.put(jsonObj1);

    JSONObject jsonObj2 = new JSONObject();
    jsonObj2.put(CentralRepositoryTestConstants.MODULE_ID, "module2");
    jsonObj2.put(CentralRepositoryTestConstants.MODULE_NAME, "Module 2");
    jsonObj2.put(CentralRepositoryTestConstants.MODULE_VERSION_DEPENDENCY_ID, "1.1");
    jsonObj2.put(CentralRepositoryTestConstants.VERSION_START, CentralRepositoryTestConstants.VERSION_2_0_0);
    jsonObj2.put(CentralRepositoryTestConstants.VERSION_END, JSONObject.NULL);
    jsonArray.put(jsonObj2);

    ModuleDependency[] dependencies = ModuleDependency.fromJson(jsonArray);

    assertNotNull(dependencies);
    assertEquals(2, dependencies.length);
    assertEquals(CentralRepositoryTestConstants.MODULE_1, dependencies[0].getModuleID());
    assertEquals(CentralRepositoryTestConstants.VERSION_2_0_0, dependencies[0].getVersionEnd());
    assertEquals("module2", dependencies[1].getModuleID());
    assertNull(dependencies[1].getVersionEnd());
  }

  /**
   * Tests the fromJson method with an invalid JSON array.
   * Verifies that an OBException is thrown for invalid input.
   */
  @Test
  void testFromJsonInvalidJsonArray() {
    JSONArray jsonArray = new JSONArray();
    jsonArray.put("invalidEntry");

    assertThrows(OBException.class, () -> ModuleDependency.fromJson(jsonArray));
  }

  /**
   * Tests the fromJson method with a valid JSON object.
   * Verifies that the method correctly parses the object into a ModuleDependency instance.
   *
   * @throws JSONException
   *     if an error occurs during JSON processing
   */
  @Test
  void testFromJsonValidJsonObject() throws JSONException {
    // Given
    JSONObject jsonObj = new JSONObject();
    jsonObj.put(CentralRepositoryTestConstants.MODULE_ID, CentralRepositoryTestConstants.MODULE_1);
    jsonObj.put(CentralRepositoryTestConstants.MODULE_NAME, CentralRepositoryTestConstants.MODULE_1_NAME);
    jsonObj.put(CentralRepositoryTestConstants.MODULE_VERSION_DEPENDENCY_ID, "1.0");
    jsonObj.put(CentralRepositoryTestConstants.VERSION_START, CentralRepositoryTestConstants.VERSION_1_0_0);
    jsonObj.put(CentralRepositoryTestConstants.VERSION_END, CentralRepositoryTestConstants.VERSION_2_0_0);

    ModuleDependency dependency = ModuleDependency.fromJson(jsonObj);

    assertNotNull(dependency);
    assertEquals(CentralRepositoryTestConstants.MODULE_1, dependency.getModuleID());
    assertEquals(CentralRepositoryTestConstants.MODULE_1_NAME, dependency.getModuleName());
    assertEquals("1.0", dependency.getModuleVersionDependencyID());
    assertEquals(CentralRepositoryTestConstants.VERSION_1_0_0, dependency.getVersionStart());
    assertEquals(CentralRepositoryTestConstants.VERSION_2_0_0, dependency.getVersionEnd());
  }

  /**
   * Tests the fromJson method with an invalid JSON object.
   * Verifies that an OBException is thrown for invalid input.
   */
  @Test
  void testFromJsonInvalidJsonObject() {
    JSONObject jsonObj = new JSONObject();

    assertThrows(OBException.class, () -> ModuleDependency.fromJson(jsonObj));
  }

  /**
   * Tests the getters and setters of the ModuleDependency class.
   * Verifies that the properties are correctly set and retrieved.
   */
  @Test
  void testGettersAndSetters() {
    ModuleDependency dependency = new ModuleDependency();

    dependency.setModuleID(CentralRepositoryTestConstants.MODULE_1);
    dependency.setModuleName(CentralRepositoryTestConstants.MODULE_1_NAME);
    dependency.setModuleVersionDependencyID("1.0");
    dependency.setVersionStart(CentralRepositoryTestConstants.VERSION_1_0_0);
    dependency.setVersionEnd(null);

    assertEquals(CentralRepositoryTestConstants.MODULE_1, dependency.getModuleID());
    assertEquals(CentralRepositoryTestConstants.MODULE_1_NAME, dependency.getModuleName());
    assertEquals("1.0", dependency.getModuleVersionDependencyID());
    assertEquals(CentralRepositoryTestConstants.VERSION_1_0_0, dependency.getVersionStart());
    assertNull(dependency.getVersionEnd());
  }
}
