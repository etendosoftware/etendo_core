package org.openbravo.service.centralrepository;

import static org.junit.jupiter.api.Assertions.*;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ModuleInstallDetail}
 */
public class ModuleInstallDetailTest {

  @Test
  public void testFromJson_ValidInput() throws Exception {
    // Given
    JSONObject mockJsonDetail = new JSONObject();
    mockJsonDetail.put("validConfiguration", true);
    mockJsonDetail.put("dependencyErrors", new JSONArray().put("Error1").put("Error2"));
    mockJsonDetail.put("modulesToInstall", new JSONArray());
    mockJsonDetail.put("modulesToUpdate", new JSONArray());

    JSONObject mockJsonResponse = new JSONObject();
    mockJsonResponse.put("response", mockJsonDetail);

    // When
    ModuleInstallDetail detail = ModuleInstallDetail.fromJson(mockJsonResponse);

    // Then
    assertNotNull(detail);
    assertTrue(detail.isValidConfiguration());
    assertArrayEquals(new String[] { "Error1", "Error2" }, detail.getDependencyErrors());
    assertEquals(0, detail.getModulesToInstall().length);
    assertEquals(0, detail.getModulesToUpdate().length);
  }

  @Test
  public void testFromJson_InvalidInput() {
    // Given
    JSONObject invalidJson = new JSONObject(); // Missing expected 'response' key

    // When & Then
    Exception exception = assertThrows(Exception.class, () -> {
      ModuleInstallDetail.fromJson(invalidJson);
    });
    assertTrue(exception.getMessage().contains("JSONObject[\"response\"] not found"));
  }

  @Test
  public void testSettersAndGetters() {
    // Given
    ModuleInstallDetail detail = new ModuleInstallDetail(null, null, null, false);

    // When
    detail.setDependencyErrors(new String[] { "ErrorA" });
    detail.setModulesToInstall(new Module[] {});
    detail.setModulesToUpdate(new Module[] {});
    detail.setValidConfiguration(true);

    // Then
    assertArrayEquals(new String[] { "ErrorA" }, detail.getDependencyErrors());
    assertEquals(0, detail.getModulesToInstall().length);
    assertEquals(0, detail.getModulesToUpdate().length);
    assertTrue(detail.isValidConfiguration());
  }
}