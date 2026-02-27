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
/** Tests for {@link ApplicationDictionaryCachedStructures}. */
@SuppressWarnings("java:S112")

@RunWith(MockitoJUnitRunner.class)
public class ApplicationDictionaryCachedStructuresTest {

  private static final String IN_DEVELOPMENT_MODULES = "inDevelopmentModules";

  private static final String MODULE_ID_DEV = "DEV_MODULE_001";
  private static final String MODULE_ID_PROD = "PROD_MODULE_002";

  private ApplicationDictionaryCachedStructures instance;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(ApplicationDictionaryCachedStructures.class);
  }

  private void setPrivateField(Object target, String fieldName, Object value) throws IllegalAccessException, NoSuchFieldException {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
  /**
   * Use cache returns true.
   * @throws Exception if an error occurs
   */

  @Test
  public void testUseCacheReturnsTrue() throws Exception {
    setPrivateField(instance, "useCache", true);
    assertTrue(instance.useCache());
  }
  /**
   * Use cache returns false.
   * @throws Exception if an error occurs
   */

  @Test
  public void testUseCacheReturnsFalse() throws Exception {
    setPrivateField(instance, "useCache", false);
    assertFalse(instance.useCache());
  }
  /**
   * Is in development with dev modules.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsInDevelopmentWithDevModules() throws Exception {
    Set<String> devModules = new HashSet<>();
    devModules.add(MODULE_ID_DEV);
    setPrivateField(instance, IN_DEVELOPMENT_MODULES, devModules);

    assertTrue(instance.isInDevelopment());
  }
  /**
   * Is in development with no dev modules.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsInDevelopmentWithNoDevModules() throws Exception {
    Set<String> devModules = new HashSet<>();
    setPrivateField(instance, IN_DEVELOPMENT_MODULES, devModules);

    assertFalse(instance.isInDevelopment());
  }
  /**
   * Is in development by module id found.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsInDevelopmentByModuleIdFound() throws Exception {
    Set<String> devModules = new HashSet<>();
    devModules.add(MODULE_ID_DEV);
    setPrivateField(instance, IN_DEVELOPMENT_MODULES, devModules);

    assertTrue(instance.isInDevelopment(MODULE_ID_DEV));
  }
  /**
   * Is in development by module id not found.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsInDevelopmentByModuleIdNotFound() throws Exception {
    Set<String> devModules = new HashSet<>();
    devModules.add(MODULE_ID_DEV);
    setPrivateField(instance, IN_DEVELOPMENT_MODULES, devModules);

    assertFalse(instance.isInDevelopment(MODULE_ID_PROD));
  }
  /**
   * Is in development by module id empty set.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsInDevelopmentByModuleIdEmptySet() throws Exception {
    Set<String> devModules = new HashSet<>();
    setPrivateField(instance, IN_DEVELOPMENT_MODULES, devModules);

    assertFalse(instance.isInDevelopment(MODULE_ID_DEV));
  }
}
