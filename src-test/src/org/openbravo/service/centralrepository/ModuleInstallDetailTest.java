package org.openbravo.service.centralrepository;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link ModuleInstallDetail} class.
 * Verifies the behavior of JSON parsing, object manipulation, and validation methods.
 */
public class ModuleInstallDetailTest {

  /**
   * Tests the {@code fromJson} method with valid input.
   * Verifies that the method correctly parses the JSON object into a {@link ModuleInstallDetail} instance.
   *
   * @throws Exception
   *     if an error occurs during JSON processing
   */
  @Test
  public void testFromJsonValidInput() throws Exception {
    JSONObject mockJsonDetail = new JSONObject();
    mockJsonDetail.put("validConfiguration", true);
    mockJsonDetail.put("dependencyErrors", new JSONArray().put("Error1").put("Error2"));
    mockJsonDetail.put("modulesToInstall", new JSONArray());
    mockJsonDetail.put("modulesToUpdate", new JSONArray());

    JSONObject mockJsonResponse = new JSONObject();
    mockJsonResponse.put("response", mockJsonDetail);

    ModuleInstallDetail detail = ModuleInstallDetail.fromJson(mockJsonResponse);

    assertNotNull(detail);
    assertTrue(detail.isValidConfiguration());
    assertArrayEquals(new String[]{ "Error1", "Error2" }, detail.getDependencyErrors());
    assertEquals(0, detail.getModulesToInstall().length);
    assertEquals(0, detail.getModulesToUpdate().length);
  }

  /**
   * Tests the {@code fromJson} method with invalid input.
   * Verifies that an exception is thrown when the JSON object is missing required keys.
   */
  @Test
  public void testFromJsonInvalidInput() {
    JSONObject invalidJson = new JSONObject();

    Exception exception = assertThrows(Exception.class, () -> ModuleInstallDetail.fromJson(invalidJson));
    assertTrue(exception.getMessage().contains("JSONObject[\"response\"] not found"));
  }

  /**
   * Tests the setters and getters of the {@link ModuleInstallDetail} class.
   * Verifies that the properties are correctly set and retrieved.
   */
  @Test
  public void testSettersAndGetters() {
    ModuleInstallDetail detail = new ModuleInstallDetail(null, null, null, false);

    detail.setDependencyErrors(new String[]{ "ErrorA" });
    detail.setModulesToInstall(new Module[]{ });
    detail.setModulesToUpdate(new Module[]{ });
    detail.setValidConfiguration(true);

    assertArrayEquals(new String[]{ "ErrorA" }, detail.getDependencyErrors());
    assertEquals(0, detail.getModulesToInstall().length);
    assertEquals(0, detail.getModulesToUpdate().length);
    assertTrue(detail.isValidConfiguration());
  }
}
