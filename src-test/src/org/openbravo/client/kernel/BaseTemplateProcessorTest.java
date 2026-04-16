package org.openbravo.client.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.model.ad.module.Module;

/**
 * Tests for {@link BaseTemplateProcessor}.
 */
@SuppressWarnings("java:S112")
@RunWith(MockitoJUnitRunner.Silent.class)
public class BaseTemplateProcessorTest {

  private static final String TEMPLATE_CACHE = "templateCache";
  private static final String COMPILED_TEMPLATE = "compiledTemplate";
  private static final String SOURCE = "source";

  private TestableTemplateProcessor instance;

  @Mock
  private Template mockTemplate;

  @Mock
  private Module mockModule;
  /**
   * Sets up test fixtures.
   * @throws IllegalAccessException if an error occurs
   * @throws NoSuchFieldException if an error occurs
   */

  @Before
  public void setUp() throws IllegalAccessException, NoSuchFieldException {
    instance = new TestableTemplateProcessor();
  }
  /** Get template implementation returns null when id is null. */

  @Test
  public void testGetTemplateImplementationReturnsNullWhenIdIsNull() {
    when(mockTemplate.getId()).thenReturn(null);

    Object result = instance.getTemplateImplementation(mockTemplate);
    assertNull(result);
  }
  /** Get template implementation returns null when not cached. */

  @Test
  public void testGetTemplateImplementationReturnsNullWhenNotCached() {
    when(mockTemplate.getId()).thenReturn("TEMPLATE_ID_001");

    Object result = instance.getTemplateImplementation(mockTemplate);
    assertNull(result);
  }
  /**
   * Get template implementation returns cached template.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetTemplateImplementationReturnsCachedTemplate() throws Exception {
    String templateId = "TEMPLATE_ID_002";
    when(mockTemplate.getId()).thenReturn(templateId);

    // Put a value in the cache via reflection
    Field cacheField = BaseTemplateProcessor.class.getDeclaredField(TEMPLATE_CACHE);
    cacheField.setAccessible(true);
    ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    cache.put(templateId, "cachedTemplate");
    cacheField.set(instance, cache);

    Object result = instance.getTemplateImplementation(mockTemplate);
    assertEquals("cachedTemplate", result);
  }
  /**
   * Create set free marker template in cache caches when not in development.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCreateSetFreeMarkerTemplateInCacheCachesWhenNotInDevelopment() throws Exception {
    String templateId = "TEMPLATE_CACHE_001";
    when(mockTemplate.getId()).thenReturn(templateId);
    when(mockTemplate.getModule()).thenReturn(mockModule);
    when(mockModule.isInDevelopment()).thenReturn(false);

    instance.setNextCreatedTemplate(COMPILED_TEMPLATE);

    String result = instance.createSetFreeMarkerTemplateInCache(mockTemplate, SOURCE);

    assertEquals(COMPILED_TEMPLATE, result);

    // Verify it was cached
    Field cacheField = BaseTemplateProcessor.class.getDeclaredField(TEMPLATE_CACHE);
    cacheField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<String, String> cache = (Map<String, String>) cacheField.get(instance);
    assertEquals(COMPILED_TEMPLATE, cache.get(templateId));
  }
  /**
   * Create set free marker template in cache does not cache when in development.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCreateSetFreeMarkerTemplateInCacheDoesNotCacheWhenInDevelopment() throws Exception {
    when(mockTemplate.getId()).thenReturn("DEV_TEMPLATE");
    when(mockTemplate.getModule()).thenReturn(mockModule);
    when(mockModule.isInDevelopment()).thenReturn(true);

    instance.setNextCreatedTemplate("devTemplate");

    String result = instance.createSetFreeMarkerTemplateInCache(mockTemplate, SOURCE);

    assertEquals("devTemplate", result);

    // Verify it was NOT cached
    Field cacheField = BaseTemplateProcessor.class.getDeclaredField(TEMPLATE_CACHE);
    cacheField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<String, String> cache = (Map<String, String>) cacheField.get(instance);
    assertNull(cache.get("DEV_TEMPLATE"));
  }
  /**
   * Create set free marker template in cache does not cache when id null.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCreateSetFreeMarkerTemplateInCacheDoesNotCacheWhenIdNull() throws Exception {
    when(mockTemplate.getId()).thenReturn(null);
    when(mockTemplate.getModule()).thenReturn(null);

    instance.setNextCreatedTemplate("noIdTemplate");

    String result = instance.createSetFreeMarkerTemplateInCache(mockTemplate, SOURCE);

    assertEquals("noIdTemplate", result);
  }
  /**
   * Clear cache resets template cache.
   * @throws Exception if an error occurs
   */

  @Test
  public void testClearCacheResetsTemplateCache() throws Exception {
    // Put something in cache
    Field cacheField = BaseTemplateProcessor.class.getDeclaredField(TEMPLATE_CACHE);
    cacheField.setAccessible(true);
    ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    cache.put("key", "value");
    cacheField.set(instance, cache);

    instance.clearCache();

    @SuppressWarnings("unchecked")
    Map<String, String> newCache = (Map<String, String>) cacheField.get(instance);
    assertEquals(0, newCache.size());
  }

  /**
   * Concrete subclass of BaseTemplateProcessor for testing.
   */
  private static class TestableTemplateProcessor extends BaseTemplateProcessor<String> {

    private String nextCreatedTemplate = "default";
    /** Set next created template. */

    public void setNextCreatedTemplate(String template) {
      this.nextCreatedTemplate = template;
    }

    @Override
    protected String processTemplate(String templateImplementation, Map<String, Object> data) {
      return templateImplementation;
    }

    @Override
    protected String createTemplateImplementation(Template template, String source) {
      return nextCreatedTemplate;
    }
    /** Validate. */

    @Override
    public void validate(Template template) {
      // no-op
    }
    /** Get template language. */

    @Override
    public String getTemplateLanguage() {
      return "TEST";
    }
  }
}
