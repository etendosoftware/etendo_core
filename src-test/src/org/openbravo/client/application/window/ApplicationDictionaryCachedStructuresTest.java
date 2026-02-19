package org.openbravo.client.application.window;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationDictionaryCachedStructuresTest {

  private static final String MODULE_ID_DEV = "DEV_MODULE_001";
  private static final String MODULE_ID_PROD = "PROD_MODULE_002";

  private ApplicationDictionaryCachedStructures instance;

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(ApplicationDictionaryCachedStructures.class);
  }

  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  @Test
  public void testUseCacheReturnsTrue() throws Exception {
    setPrivateField(instance, "useCache", true);
    assertTrue(instance.useCache());
  }

  @Test
  public void testUseCacheReturnsFalse() throws Exception {
    setPrivateField(instance, "useCache", false);
    assertFalse(instance.useCache());
  }

  @Test
  public void testIsInDevelopmentWithDevModules() throws Exception {
    Set<String> devModules = new HashSet<>();
    devModules.add(MODULE_ID_DEV);
    setPrivateField(instance, "inDevelopmentModules", devModules);

    assertTrue(instance.isInDevelopment());
  }

  @Test
  public void testIsInDevelopmentWithNoDevModules() throws Exception {
    Set<String> devModules = new HashSet<>();
    setPrivateField(instance, "inDevelopmentModules", devModules);

    assertFalse(instance.isInDevelopment());
  }

  @Test
  public void testIsInDevelopmentByModuleIdFound() throws Exception {
    Set<String> devModules = new HashSet<>();
    devModules.add(MODULE_ID_DEV);
    setPrivateField(instance, "inDevelopmentModules", devModules);

    assertTrue(instance.isInDevelopment(MODULE_ID_DEV));
  }

  @Test
  public void testIsInDevelopmentByModuleIdNotFound() throws Exception {
    Set<String> devModules = new HashSet<>();
    devModules.add(MODULE_ID_DEV);
    setPrivateField(instance, "inDevelopmentModules", devModules);

    assertFalse(instance.isInDevelopment(MODULE_ID_PROD));
  }

  @Test
  public void testIsInDevelopmentByModuleIdEmptySet() throws Exception {
    Set<String> devModules = new HashSet<>();
    setPrivateField(instance, "inDevelopmentModules", devModules);

    assertFalse(instance.isInDevelopment(MODULE_ID_DEV));
  }
}
