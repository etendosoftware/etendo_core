package org.openbravo.service.centralrepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;

/**
 * Unit tests for the SimpleModule class.
 * Verifies the behavior of the SimpleModule class when interacting with JSON data.
 */
@ExtendWith(MockitoExtension.class)
public class SimpleModuleTest {
  public static final String VALUE_2 = "value2";

  private JSONObject validJsonModule;
  private JSONObject jsonModuleWithAdditionalInfo;

  /**
   * Sets up the test environment before each test.
   * Initializes valid JSON objects for testing.
   *
   * @throws JSONException
   *     if an error occurs while creating JSON objects
   */
  @BeforeEach
  public void setUp() throws JSONException {
    validJsonModule = new JSONObject();
    validJsonModule.put("author", CentralRepositoryTestConstants.TEST_AUTHOR);
    validJsonModule.put("description", "Test Description");
    validJsonModule.put("help", "Test Help");
    validJsonModule.put("licenseAgreement", "Test License Agreement");
    validJsonModule.put("licenseType", "Test License Type");
    validJsonModule.put("moduleID", "12345");
    validJsonModule.put("moduleVersionID", "1.0.0");
    validJsonModule.put("name", "Test Module");
    validJsonModule.put("type", "Test Type");
    validJsonModule.put("updateDescription", "Test Update Description");
    validJsonModule.put("url", "http://test.example.com");
    validJsonModule.put("versionNo", "1.0");
    validJsonModule.put("isCommercial", true);
    validJsonModule.put(CentralRepositoryTestConstants.ADDITIONAL_INFO, "{}");

    jsonModuleWithAdditionalInfo = new JSONObject(validJsonModule.toString());

    JSONObject additionalInfo = new JSONObject();
    additionalInfo.put("key1", CentralRepositoryTestConstants.VALUE_1);
    additionalInfo.put("key2", VALUE_2);
    jsonModuleWithAdditionalInfo.put(CentralRepositoryTestConstants.ADDITIONAL_INFO, additionalInfo);
  }

  /**
   * Tests the fromJson method with valid JSON input.
   * Verifies that a SimpleModule object is created correctly.
   */
  @Test
  public void testFromJsonWithValidJsonShouldCreateSimpleModule() {
    SimpleModule module = SimpleModule.fromJson(validJsonModule);

    assertEquals(CentralRepositoryTestConstants.TEST_AUTHOR, module.getAuthor());
    assertEquals("Test Description", module.getDescription());
    assertEquals("Test Help", module.getHelp());
    assertEquals("Test License Agreement", module.getLicenseAgreement());
    assertEquals("Test License Type", module.getLicenseType());
    assertEquals("12345", module.getModuleID());
    assertEquals("1.0.0", module.getModuleVersionID());
    assertEquals("Test Module", module.getName());
    assertEquals("Test Type", module.getType());
    assertEquals("Test Update Description", module.getUpdateDescription());
    assertEquals("http://test.example.com", module.getUrl());
    assertEquals("1.0", module.getVersionNo());
    assertTrue(module.isCommercial());
    assertNotNull(module.getAdditionalInfo());
    assertTrue(module.getAdditionalInfo().isEmpty());
  }

  /**
   * Tests the fromJson method with additionalInfo in the JSON.
   * Verifies that the additionalInfo is correctly parsed.
   */
  @Test
  public void testFromJsonWithAdditionalInfoShouldCreateSimpleModuleWithAdditionalInfo() {
    SimpleModule module = SimpleModule.fromJson(jsonModuleWithAdditionalInfo);

    Map<String, Object> additionalInfo = module.getAdditionalInfo();
    assertNotNull(additionalInfo);
    assertEquals(2, additionalInfo.size());
    assertEquals(CentralRepositoryTestConstants.VALUE_1, additionalInfo.get("key1"));
    assertEquals(VALUE_2, additionalInfo.get("key2"));
  }

  /**
   * Tests the fromJson method with missing required fields.
   * Verifies that an OBException is thrown.
   *
   * @throws JSONException
   *     if an error occurs while creating JSON objects
   */
  @Test
  public void testFromJsonWithMissingRequiredFieldShouldThrowOBException() throws JSONException {
    JSONObject invalidJson = new JSONObject();
    invalidJson.put("author", CentralRepositoryTestConstants.TEST_AUTHOR);

    assertThrows(OBException.class, () -> SimpleModule.fromJson(invalidJson));
  }

  /**
   * Tests the fromJson method with invalid JSON structure.
   * Verifies that an OBException is thrown.
   *
   * @throws JSONException
   *     if an error occurs while mocking JSON objects
   */
  @Test
  public void testFromJsonWithInvalidJSONStructureShouldThrowOBException() throws JSONException {
    JSONObject invalidJson = mock(JSONObject.class);
    when(invalidJson.getString(anyString())).thenThrow(new JSONException("Invalid JSON"));

    assertThrows(OBException.class, () -> SimpleModule.fromJson(invalidJson));
  }

  /**
   * Tests the setter and getter methods of the SimpleModule class.
   * Verifies that the values are correctly updated and retrieved.
   */
  @Test
  public void testSetterAndGetters() {
    SimpleModule module = SimpleModule.fromJson(validJsonModule);

    module.setAuthor("New Author");
    module.setDescription("New Description");
    module.setHelp("New Help");
    module.setLicenseAgreement("New License Agreement");
    module.setLicenseType("New License Type");
    module.setModuleID("54321");
    module.setModuleVersionID("2.0.0");
    module.setName("New Module Name");
    module.setType("New Type");
    module.setUpdateDescription("New Update Description");
    module.setUrl("http://new.example.com");
    module.setVersionNo("2.0");
    module.setCommercial(false);

    assertEquals("New Author", module.getAuthor());
    assertEquals("New Description", module.getDescription());
    assertEquals("New Help", module.getHelp());
    assertEquals("New License Agreement", module.getLicenseAgreement());
    assertEquals("New License Type", module.getLicenseType());
    assertEquals("54321", module.getModuleID());
    assertEquals("2.0.0", module.getModuleVersionID());
    assertEquals("New Module Name", module.getName());
    assertEquals("New Type", module.getType());
    assertEquals("New Update Description", module.getUpdateDescription());
    assertEquals("http://new.example.com", module.getUrl());
    assertEquals("2.0", module.getVersionNo());
    assertFalse(module.isCommercial());
  }

  /**
   * Tests the setAdditionalInfo method.
   * Verifies that the additionalInfo map is correctly updated.
   */
  @Test
  public void testAdditionalInfoSetter() {
    SimpleModule module = SimpleModule.fromJson(validJsonModule);
    Map<String, Object> newAdditionalInfo = Map.of("key1", CentralRepositoryTestConstants.VALUE_1, "key2", VALUE_2, "key3", "value3");

    module.setAdditionalInfo(newAdditionalInfo);

    assertEquals(newAdditionalInfo, module.getAdditionalInfo());
    assertEquals(3, module.getAdditionalInfo().size());
  }

  /**
   * Tests the fromJson method with null additionalInfo.
   * Verifies that an empty map is created.
   *
   * @throws JSONException
   *     if an error occurs while creating JSON objects
   */
  @Test
  public void testFromJsonWithNullAdditionalInfoShouldCreateEmptyMap() throws JSONException {
    JSONObject jsonWithNullAddInfo = new JSONObject(validJsonModule.toString());
    jsonWithNullAddInfo.put(CentralRepositoryTestConstants.ADDITIONAL_INFO, JSONObject.NULL);

    SimpleModule module = SimpleModule.fromJson(jsonWithNullAddInfo);

    assertNotNull(module.getAdditionalInfo());
    assertTrue(module.getAdditionalInfo().isEmpty());
  }

  /**
   * Tests the fromJson method with an array as additionalInfo.
   * Verifies that the additionalInfo is handled gracefully.
   *
   * @throws JSONException
   *     if an error occurs while creating JSON objects
   */
  @Test
  public void testFromJsonWithArrayAsAdditionalInfoShouldHandleGracefully() throws JSONException {
    JSONObject jsonWithArrayAddInfo = new JSONObject(validJsonModule.toString());
    JSONArray array = new JSONArray();
    array.put(CentralRepositoryTestConstants.VALUE_1);
    array.put(VALUE_2);
    jsonWithArrayAddInfo.put(CentralRepositoryTestConstants.ADDITIONAL_INFO, array);

    SimpleModule module = SimpleModule.fromJson(jsonWithArrayAddInfo);
    assertNotNull(module.getAdditionalInfo());
    assertTrue(module.getAdditionalInfo().isEmpty());
  }
}
