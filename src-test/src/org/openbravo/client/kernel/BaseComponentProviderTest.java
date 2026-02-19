package org.openbravo.client.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource.ComponentResourceType;

/**
 * Tests for {@link BaseComponentProvider} inner class {@link ComponentResource}
 * and static utility methods.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class BaseComponentProviderTest {

  private static final String TEST_PATH = "/web/js/test.js";
  private static final String APP_OB3 = ComponentResource.APP_OB3;
  private static final String APP_CLASSIC = ComponentResource.APP_CLASSIC;

  private ComponentResource resource;

  @Before
  public void setUp() {
    resource = new ComponentResource();
  }

  @Test
  public void testSetAndGetType() {
    resource.setType(ComponentResourceType.Static);
    assertEquals(ComponentResourceType.Static, resource.getType());
  }

  @Test
  public void testSetAndGetPath() {
    resource.setPath(TEST_PATH);
    assertEquals(TEST_PATH, resource.getPath());
  }

  @Test
  public void testToString() {
    resource.setType(ComponentResourceType.Static);
    resource.setPath(TEST_PATH);
    assertEquals("Static " + TEST_PATH, resource.toString());
  }

  @Test
  public void testToStringDynamic() {
    resource.setType(ComponentResourceType.Dynamic);
    resource.setPath("/web/dyn/component");
    assertEquals("Dynamic /web/dyn/component", resource.toString());
  }

  @Test
  public void testToStringStylesheet() {
    resource.setType(ComponentResourceType.Stylesheet);
    resource.setPath("/web/css/style.css");
    assertEquals("Stylesheet /web/css/style.css", resource.toString());
  }

  @Test
  public void testAddValidForApp() {
    resource.addValidForApp(APP_OB3);
    List<String> validApps = resource.getValidForAppList();
    assertEquals(1, validApps.size());
    assertTrue(validApps.contains(APP_OB3));
  }

  @Test
  public void testAddMultipleValidApps() {
    resource.addValidForApp(APP_OB3);
    resource.addValidForApp(APP_CLASSIC);
    List<String> validApps = resource.getValidForAppList();
    assertEquals(2, validApps.size());
    assertTrue(validApps.contains(APP_OB3));
    assertTrue(validApps.contains(APP_CLASSIC));
  }

  @Test
  public void testSetValidForAppList() {
    List<String> apps = Arrays.asList(APP_OB3, APP_CLASSIC);
    resource.setValidForAppList(apps);
    assertEquals(apps, resource.getValidForAppList());
  }

  @Test
  public void testIsValidForAppDirectMatch() {
    resource.addValidForApp(APP_OB3);
    assertTrue(resource.isValidForApp(APP_OB3));
  }

  @Test
  public void testIsValidForAppNotPresent() {
    resource.addValidForApp(APP_OB3);
    assertFalse(resource.isValidForApp(APP_CLASSIC));
  }

  @Test
  public void testIsValidForAppEmptyList() {
    assertFalse(resource.isValidForApp(APP_OB3));
  }

  @Test
  public void testIsValidForAppWithDependencies() {
    resource.addValidForApp(APP_OB3);
    BaseComponentProvider.setAppDependencies("CUSTOM_APP", Arrays.asList(APP_OB3));
    assertTrue(resource.isValidForApp("CUSTOM_APP"));
    // Clean up static state
    BaseComponentProvider.setAppDependencies("CUSTOM_APP", Collections.emptyList());
  }

  @Test
  public void testIsValidForAppWithNestedDependencies() {
    resource.addValidForApp(APP_OB3);
    BaseComponentProvider.setAppDependencies("LEVEL2", Arrays.asList(APP_OB3));
    BaseComponentProvider.setAppDependencies("LEVEL1", Arrays.asList("LEVEL2"));
    assertTrue(resource.isValidForApp("LEVEL1"));
    // Clean up static state
    BaseComponentProvider.setAppDependencies("LEVEL1", Collections.emptyList());
    BaseComponentProvider.setAppDependencies("LEVEL2", Collections.emptyList());
  }

  @Test
  public void testIsValidForAppDependencyNotMatching() {
    resource.addValidForApp(APP_CLASSIC);
    BaseComponentProvider.setAppDependencies("CUSTOM_APP", Arrays.asList(APP_OB3));
    assertFalse(resource.isValidForApp("CUSTOM_APP"));
    // Clean up static state
    BaseComponentProvider.setAppDependencies("CUSTOM_APP", Collections.emptyList());
  }

  @Test
  public void testDeprecatedIncludeInClassicMode() {
    resource.setIncludeAlsoInClassicMode(true);
    assertTrue(resource.isIncludeAlsoInClassicMode());
  }

  @Test
  public void testDeprecatedIncludeInClassicModeDefaultFalse() {
    assertFalse(resource.isIncludeAlsoInClassicMode());
  }

  @Test
  public void testDeprecatedIncludeInNewUIMode() {
    resource.setIncludeInNewUIMode(false);
    assertFalse(resource.isIncludeInNewUIMode());
  }

  @Test
  public void testDeprecatedIncludeInNewUIModeDefaultTrue() {
    assertTrue(resource.isIncludeInNewUIMode());
  }

  @Test
  public void testAllCoreAppsContainsBothApps() {
    assertNotNull(ComponentResource.ALL_CORE_APPS);
    assertEquals(2, ComponentResource.ALL_CORE_APPS.size());
    assertTrue(ComponentResource.ALL_CORE_APPS.contains(APP_OB3));
    assertTrue(ComponentResource.ALL_CORE_APPS.contains(APP_CLASSIC));
  }

  @Test
  public void testComponentResourceTypeValues() {
    ComponentResourceType[] types = ComponentResourceType.values();
    assertEquals(3, types.length);
    assertEquals(ComponentResourceType.Static, ComponentResourceType.valueOf("Static"));
    assertEquals(ComponentResourceType.Dynamic, ComponentResourceType.valueOf("Dynamic"));
    assertEquals(ComponentResourceType.Stylesheet, ComponentResourceType.valueOf("Stylesheet"));
  }

  @Test
  public void testGetValidForAppListInitiallyEmpty() {
    assertNotNull(resource.getValidForAppList());
    assertTrue(resource.getValidForAppList().isEmpty());
  }
}
