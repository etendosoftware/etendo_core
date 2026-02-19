package org.openbravo.client.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BaseComponentTest {

  private BaseComponent instance;

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(TestableBaseComponent.class);

    // Initialize fields that Objenesis skips
    Field parametersField = BaseComponent.class.getDeclaredField("parameters");
    parametersField.setAccessible(true);
    parametersField.set(instance, new HashMap<String, Object>());

    Field dependenciesField = BaseComponent.class.getDeclaredField("dependencies");
    dependenciesField.setAccessible(true);
    dependenciesField.set(instance, new ArrayList<Component>());

    // Reset the static contextUrl field to null for clean tests
    Field contextUrlField = BaseComponent.class.getDeclaredField("contextUrl");
    contextUrlField.setAccessible(true);
    contextUrlField.set(null, null);
  }

  @Test
  public void testStripHostWithHttpUrl() throws Exception {
    // Arrange
    String url = "http://example.com/etendo/";

    // Act
    java.lang.reflect.Method stripHost = BaseComponent.class.getDeclaredMethod("stripHost", String.class);
    stripHost.setAccessible(true);
    String result = (String) stripHost.invoke(instance, url);

    // Assert
    assertEquals("/etendo/", result);
  }

  @Test
  public void testStripHostWithHttpsUrl() throws Exception {
    // Arrange
    String url = "https://example.com/etendo/";

    // Act
    java.lang.reflect.Method stripHost = BaseComponent.class.getDeclaredMethod("stripHost", String.class);
    stripHost.setAccessible(true);
    String result = (String) stripHost.invoke(instance, url);

    // Assert
    assertEquals("/etendo/", result);
  }

  @Test
  public void testStripHostWithRelativeUrl() throws Exception {
    // Arrange
    String url = "/etendo/";

    // Act
    java.lang.reflect.Method stripHost = BaseComponent.class.getDeclaredMethod("stripHost", String.class);
    stripHost.setAccessible(true);
    String result = (String) stripHost.invoke(instance, url);

    // Assert
    assertEquals("/etendo/", result);
  }

  @Test
  public void testStripHostWithHttpNoPath() throws Exception {
    // Arrange - URL like "http://example.com" with no path after host
    String url = "http://example.com";

    // Act
    java.lang.reflect.Method stripHost = BaseComponent.class.getDeclaredMethod("stripHost", String.class);
    stripHost.setAccessible(true);
    String result = (String) stripHost.invoke(instance, url);

    // Assert - no slash after host, so it returns original
    assertEquals("http://example.com", result);
  }

  @Test
  public void testGetIdAndSetId() {
    // Act
    instance.setId("testId123");

    // Assert
    assertEquals("testId123", instance.getId());
  }

  @Test
  public void testGetParameterExisting() {
    // Arrange
    Map<String, Object> params = new HashMap<>();
    params.put("key1", "value1");
    instance.setParameters(params);

    // Act & Assert
    assertEquals("value1", instance.getParameter("key1"));
  }

  @Test
  public void testGetParameterMissing() {
    // Act & Assert
    assertEquals("", instance.getParameter("nonexistent"));
  }

  @Test
  public void testHasParameter() {
    // Arrange
    Map<String, Object> params = new HashMap<>();
    params.put("existing", "value");
    instance.setParameters(params);

    // Act & Assert
    assertTrue(instance.hasParameter("existing"));
    assertFalse(instance.hasParameter("missing"));
  }

  @Test
  public void testGetParameterNamesOnlyStrings() {
    // Arrange
    Map<String, Object> params = new HashMap<>();
    params.put("stringKey", "stringValue");
    params.put("intKey", 42);
    params.put("anotherString", "anotherValue");
    instance.setParameters(params);

    // Act
    List<String> names = instance.getParameterNames();

    // Assert
    assertTrue(names.contains("stringKey"));
    assertTrue(names.contains("anotherString"));
    assertFalse(names.contains("intKey"));
    assertEquals(2, names.size());
  }

  @Test
  public void testSetAndGetDependencies() {
    // Arrange
    List<Component> deps = new ArrayList<>();

    // Act
    instance.setDependencies(deps);

    // Assert
    assertEquals(deps, instance.getDependencies());
  }

  @Test
  public void testGetSafeValueWithNull() throws Exception {
    // Act
    java.lang.reflect.Method getSafeValue = BaseComponent.class.getDeclaredMethod("getSafeValue", Object.class);
    getSafeValue.setAccessible(true);
    String result = (String) getSafeValue.invoke(instance, (Object) null);

    // Assert
    assertEquals("", result);
  }

  @Test
  public void testGetSafeValueWithValue() throws Exception {
    // Act
    java.lang.reflect.Method getSafeValue = BaseComponent.class.getDeclaredMethod("getSafeValue", Object.class);
    getSafeValue.setAccessible(true);
    String result = (String) getSafeValue.invoke(instance, "hello");

    // Assert
    assertEquals("hello", result);
  }

  @Test
  public void testGetContentType() {
    // Act & Assert
    assertEquals(KernelConstants.JAVASCRIPT_CONTENTTYPE, instance.getContentType());
  }

  @Test
  public void testIsJavaScriptComponent() {
    // Act & Assert
    assertTrue(instance.isJavaScriptComponent());
  }

  @Test
  public void testBypassAuthentication() {
    // Act & Assert
    assertFalse(instance.bypassAuthentication());
  }

  @Test
  public void testGetContextUrlEmptyWhenNoParameter() {
    // Act
    String result = instance.getContextUrl();

    // Assert
    assertEquals("", result);
  }

  @Test
  public void testGetContextUrlFromParameter() throws Exception {
    // Arrange
    Map<String, Object> params = new HashMap<>();
    params.put(KernelConstants.CONTEXT_URL, "/myapp");
    instance.setParameters(params);

    // Act
    String result = instance.getContextUrl();

    // Assert
    assertEquals("/myapp/", result);
  }

  @Test
  public void testGetContextUrlStripsHttpHost() throws Exception {
    // Arrange
    Map<String, Object> params = new HashMap<>();
    params.put(KernelConstants.CONTEXT_URL, "http://localhost:8080/etendo");
    instance.setParameters(params);

    // Act
    String result = instance.getContextUrl();

    // Assert
    assertEquals("/etendo/", result);
  }

  @Test
  public void testGetApplicationNameFromAppNameParam() {
    // Arrange
    Map<String, Object> params = new HashMap<>();
    params.put(KernelConstants.APP_NAME_PARAMETER, "MyApp");
    instance.setParameters(params);

    // Act
    String result = instance.getApplicationName();

    // Assert
    assertEquals("MyApp", result);
  }

  @Test
  public void testGetApplicationNameClassicMode() {
    // Arrange
    Map<String, Object> params = new HashMap<>();
    params.put(KernelConstants.MODE_PARAMETER, KernelConstants.MODE_PARAMETER_CLASSIC);
    instance.setParameters(params);

    // Act
    String result = instance.getApplicationName();

    // Assert
    assertEquals(ComponentResource.APP_CLASSIC, result);
  }

  @Test
  public void testGetApplicationName300Mode() {
    // Arrange
    Map<String, Object> params = new HashMap<>();
    params.put(KernelConstants.MODE_PARAMETER, KernelConstants.MODE_PARAMETER_300);
    instance.setParameters(params);

    // Act
    String result = instance.getApplicationName();

    // Assert
    assertEquals(ComponentResource.APP_OB3, result);
  }

  @Test
  public void testGetApplicationNameEmptyModeDefaultsToClassic() {
    // Act - no mode parameter set, defaults to empty string
    String result = instance.getApplicationName();

    // Assert
    assertEquals(ComponentResource.APP_CLASSIC, result);
  }

  @Test
  public void testIsClassicModeTrue() {
    // Arrange - no params, defaults to classic
    // Act & Assert
    assertTrue(instance.isClassicMode());
  }

  @Test
  public void testIsClassicModeFalse() {
    // Arrange
    Map<String, Object> params = new HashMap<>();
    params.put(KernelConstants.MODE_PARAMETER, KernelConstants.MODE_PARAMETER_300);
    instance.setParameters(params);

    // Act & Assert
    assertFalse(instance.isClassicMode());
  }

  @Test
  public void testGetLastModified() {
    // Act
    Date before = new Date();
    Date result = instance.getLastModified();
    Date after = new Date();

    // Assert
    assertTrue(result.getTime() >= before.getTime());
    assertTrue(result.getTime() <= after.getTime());
  }

  @Test
  public void testNullifyModuleCache() throws Exception {
    // Arrange - set a value in moduleVersionHash
    Field hashField = BaseComponent.class.getDeclaredField("moduleVersionHash");
    hashField.setAccessible(true);
    hashField.set(null, "someHash");

    // Act
    BaseComponent.nullifyModuleCache();

    // Assert
    assertNull(hashField.get(null));
  }

  // Concrete subclass for testing abstract BaseComponent
  private static class TestableBaseComponent extends BaseComponent {
    @Override
    public String generate() {
      return "";
    }

    @Override
    public Object getData() {
      return null;
    }
  }

  private void assertNull(Object value) {
    assertEquals(null, value);
  }
}
