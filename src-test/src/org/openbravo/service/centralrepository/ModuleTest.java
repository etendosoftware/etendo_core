package org.openbravo.service.centralrepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;

/**
 * Unit tests for the {@link java.lang.Module} class.
 * Verifies the behavior of JSON deserialization, property setters and getters,
 * and other functionalities of the {@link java.lang.Module} class.
 */
@ExtendWith(MockitoExtension.class)
public class ModuleTest {

  private JSONObject validJsonModule;
  private JSONArray validJsonDependencies;
  private JSONArray validJsonIncludes;
  private JSONArray validJsonModules;
  private ModuleDependency[] mockDependencies;
  private ModuleDependency[] mockIncludes;

  /**
   * Sets up the test environment before each test.
   * Initializes valid JSON objects and mock dependencies for testing.
   *
   * @throws JSONException
   *     if an error occurs while creating JSON objects
   */
  @BeforeEach
  public void setUp() throws JSONException {
    validJsonDependencies = new JSONArray();
    validJsonDependencies.put(new JSONObject().put("moduleId", "dep1").put("versionStart", "1.0"));
    validJsonDependencies.put(new JSONObject().put("moduleId", "dep2").put("versionStart", "2.0"));

    validJsonIncludes = new JSONArray();
    validJsonIncludes.put(new JSONObject().put("moduleId", "inc1").put("versionStart", "1.0"));

    mockDependencies = new ModuleDependency[2];
    mockDependencies[0] = new ModuleDependency();
    mockDependencies[1] = new ModuleDependency();

    mockIncludes = new ModuleDependency[1];
    mockIncludes[0] = new ModuleDependency();

    validJsonModule = new JSONObject();
    validJsonModule.put("author", "Test Author");
    validJsonModule.put("dbPrefix", "TEST");
    validJsonModule.put("dependencies", validJsonDependencies);
    validJsonModule.put("description", "Test Description");
    validJsonModule.put("help", "Test Help");
    validJsonModule.put("includes", validJsonIncludes);
    validJsonModule.put("licenseAgreement", "Test License Agreement");
    validJsonModule.put("licenseType", "Test License Type");
    validJsonModule.put("moduleID", "12345");
    validJsonModule.put("moduleVersionID", "1.0.0");
    validJsonModule.put("name", "Test Module");
    validJsonModule.put("packageName", "org.test.module");
    validJsonModule.put("type", "Test Type");
    validJsonModule.put("updateDescription", "Test Update Description");
    validJsonModule.put("url", "http://test.example.com");
    validJsonModule.put("versionNo", "1.0");
    validJsonModule.put("isCommercial", true);

    JSONObject additionalInfo = new JSONObject();
    additionalInfo.put("key1", "value1");
    additionalInfo.put("key2", "value2");
    validJsonModule.put("additionalInfo", additionalInfo);

    validJsonModules = new JSONArray();
    validJsonModules.put(validJsonModule);
    validJsonModules.put(validJsonModule);
  }

  /**
   * Tests the {@code fromJson} method with valid JSON input.
   * Verifies that a {@link java.lang.Module} object is correctly created.
   */
  @Test
  public void testFromJsonWithValidJsonShouldCreateModule() {
    try (MockedStatic<ModuleDependency> mockedModuleDependency = mockStatic(ModuleDependency.class)) {
      mockedModuleDependency.when(() -> ModuleDependency.fromJson(validJsonDependencies)).thenReturn(mockDependencies);
      mockedModuleDependency.when(() -> ModuleDependency.fromJson(validJsonIncludes)).thenReturn(mockIncludes);

      Module module = Module.fromJson(validJsonModule);

      assertEquals("Test Author", module.getAuthor());
      assertEquals("TEST", module.getDbPrefix());
      assertEquals("Test Description", module.getDescription());
      assertEquals("Test Help", module.getHelp());
      assertEquals("Test License Agreement", module.getLicenseAgreement());
      assertEquals("Test License Type", module.getLicenseType());
      assertEquals("12345", module.getModuleID());
      assertEquals("1.0.0", module.getModuleVersionID());
      assertEquals("Test Module", module.getName());
      assertEquals("org.test.module", module.getPackageName());
      assertEquals("Test Type", module.getType());
      assertEquals("Test Update Description", module.getUpdateDescription());
      assertEquals("http://test.example.com", module.getUrl());
      assertEquals("1.0", module.getVersionNo());
      assertTrue(module.isIsCommercial());
      assertSame(mockDependencies, module.getDependencies());
      assertSame(mockIncludes, module.getIncludes());

      Map<String, Object> additionalInfo = module.getAdditionalInfo();
      assertNotNull(additionalInfo);
      assertEquals(2, additionalInfo.size());
      assertEquals("value1", additionalInfo.get("key1"));
      assertEquals("value2", additionalInfo.get("key2"));
    }
  }

  /**
   * Tests the {@code fromJson} method with missing required fields.
   * Verifies that an {@link OBException} is thrown.
   *
   * @throws JSONException
   *     if an error occurs while creating JSON objects
   */
  @Test
  public void testFromJsonWithMissingRequiredFieldShouldThrowOBException() throws JSONException {
    JSONObject invalidJson = new JSONObject();
    invalidJson.put("author", "Test Author");

    assertThrows(OBException.class, () -> Module.fromJson(invalidJson));
  }

  /**
   * Tests the {@code fromJson} method with invalid JSON structure.
   * Verifies that an {@link OBException} is thrown.
   *
   * @throws JSONException
   *     if an error occurs while creating JSON objects
   */
  @Test
  public void testFromJsonWithInvalidJSONStructureShouldThrowOBException() throws JSONException {
    JSONObject invalidJson = mock(JSONObject.class);
    when(invalidJson.getString(anyString())).thenThrow(new JSONException("Invalid JSON"));

    assertThrows(OBException.class, () -> Module.fromJson(invalidJson));
  }

  /**
   * Tests the {@code fromJson} method with a valid JSON array.
   * Verifies that an array of {@link java.lang.Module} objects is correctly created.
   */
  @Test
  public void testFromJsonArrayWithValidArrayShouldCreateModuleArray() {
    try (MockedStatic<ModuleDependency> mockedModuleDependency = mockStatic(
        ModuleDependency.class); MockedStatic<Module> mockedModule = mockStatic(Module.class, invocation -> {
      if (invocation.getMethod().getName().equals("fromJson") && invocation.getArguments()[0] instanceof JSONArray) {
        return invocation.callRealMethod();
      }
      return mock(Module.class);
    })) {

      mockedModuleDependency.when(() -> ModuleDependency.fromJson(any(JSONArray.class))).thenReturn(mockDependencies);

      Module mockModule = mock(Module.class);
      mockedModule.when(() -> Module.fromJson(any(JSONObject.class))).thenReturn(mockModule);

      Module[] modules = Module.fromJson(validJsonModules);

      assertEquals(2, modules.length);
      assertSame(mockModule, modules[0]);
      assertSame(mockModule, modules[1]);
    }
  }

  /**
   * Tests the {@code fromJson} method with an invalid element in the JSON array.
   * Verifies that an {@link OBException} is thrown.
   */
  @Test
  public void testFromJsonArrayWithInvalidElementShouldThrowOBException() {
    JSONArray invalidArray = new JSONArray();
    invalidArray.put("Not a JSONObject");

    assertThrows(OBException.class, () -> Module.fromJson(invalidArray));
  }

  /**
   * Tests the setters and getters of the {@link java.lang.Module} class.
   * Verifies that all properties are correctly set and retrieved.
   */
  @Test
  public void testSetterAndGetters() {
    Module module = new Module();
    ModuleDependency[] dependencies = new ModuleDependency[1];
    ModuleDependency[] includes = new ModuleDependency[1];
    Map<String, Object> additionalInfo = Map.of("key", "value");

    module.setAuthor("New Author");
    module.setDbPrefix("NEW");
    module.setDependencies(dependencies);
    module.setDescription("New Description");
    module.setHelp("New Help");
    module.setIncludes(includes);
    module.setLicenseAgreement("New License Agreement");
    module.setLicenseType("New License Type");
    module.setModuleID("54321");
    module.setModuleVersionID("2.0.0");
    module.setName("New Module Name");
    module.setPackageName("org.new.module");
    module.setType("New Type");
    module.setUpdateDescription("New Update Description");
    module.setUrl("http://new.example.com");
    module.setVersionNo("2.0");
    module.setIsCommercial(false);
    module.setAdditionalInfo(additionalInfo);

    assertEquals("New Author", module.getAuthor());
    assertEquals("NEW", module.getDbPrefix());
    assertSame(dependencies, module.getDependencies());
    assertEquals("New Description", module.getDescription());
    assertEquals("New Help", module.getHelp());
    assertSame(includes, module.getIncludes());
    assertEquals("New License Agreement", module.getLicenseAgreement());
    assertEquals("New License Type", module.getLicenseType());
    assertEquals("54321", module.getModuleID());
    assertEquals("2.0.0", module.getModuleVersionID());
    assertEquals("New Module Name", module.getName());
    assertEquals("org.new.module", module.getPackageName());
    assertEquals("New Type", module.getType());
    assertEquals("New Update Description", module.getUpdateDescription());
    assertEquals("http://new.example.com", module.getUrl());
    assertEquals("2.0", module.getVersionNo());
    assertFalse(module.isIsCommercial());
    assertSame(additionalInfo, module.getAdditionalInfo());
  }

  /**
   * Tests the default constructor of the {@link java.lang.Module} class.
   * Verifies that all properties are initialized to {@code null} or default values.
   */
  @Test
  public void testDefaultConstructor() {
    Module module = new Module();

    assertNotNull(module);
    assertNull(module.getAuthor());
    assertNull(module.getDbPrefix());
    assertNull(module.getDependencies());
    assertNull(module.getDescription());
    assertNull(module.getHelp());
    assertNull(module.getIncludes());
    assertNull(module.getLicenseAgreement());
    assertNull(module.getLicenseType());
    assertNull(module.getModuleID());
    assertNull(module.getModuleVersionID());
    assertNull(module.getName());
    assertNull(module.getPackageName());
    assertNull(module.getType());
    assertNull(module.getUpdateDescription());
    assertNull(module.getUrl());
    assertNull(module.getVersionNo());
    assertNull(module.getAdditionalInfo());
  }
}
