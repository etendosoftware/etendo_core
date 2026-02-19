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
@RunWith(MockitoJUnitRunner.Silent.class)
public class BaseTemplateProcessorTest {

  private TestableTemplateProcessor instance;

  @Mock
  private Template mockTemplate;

  @Mock
  private Module mockModule;

  @Before
  public void setUp() throws Exception {
    instance = new TestableTemplateProcessor();
  }

  @Test
  public void testGetTemplateImplementationReturnsNullWhenIdIsNull() {
    when(mockTemplate.getId()).thenReturn(null);

    Object result = instance.getTemplateImplementation(mockTemplate);
    assertNull(result);
  }

  @Test
  public void testGetTemplateImplementationReturnsNullWhenNotCached() {
    when(mockTemplate.getId()).thenReturn("TEMPLATE_ID_001");

    Object result = instance.getTemplateImplementation(mockTemplate);
    assertNull(result);
  }

  @Test
  public void testGetTemplateImplementationReturnsCachedTemplate() throws Exception {
    String templateId = "TEMPLATE_ID_002";
    when(mockTemplate.getId()).thenReturn(templateId);

    // Put a value in the cache via reflection
    Field cacheField = BaseTemplateProcessor.class.getDeclaredField("templateCache");
    cacheField.setAccessible(true);
    ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    cache.put(templateId, "cachedTemplate");
    cacheField.set(instance, cache);

    Object result = instance.getTemplateImplementation(mockTemplate);
    assertEquals("cachedTemplate", result);
  }

  @Test
  public void testCreateSetFreeMarkerTemplateInCacheCachesWhenNotInDevelopment() throws Exception {
    String templateId = "TEMPLATE_CACHE_001";
    when(mockTemplate.getId()).thenReturn(templateId);
    when(mockTemplate.getModule()).thenReturn(mockModule);
    when(mockModule.isInDevelopment()).thenReturn(false);

    instance.setNextCreatedTemplate("compiledTemplate");

    String result = instance.createSetFreeMarkerTemplateInCache(mockTemplate, "source");

    assertEquals("compiledTemplate", result);

    // Verify it was cached
    Field cacheField = BaseTemplateProcessor.class.getDeclaredField("templateCache");
    cacheField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<String, String> cache = (Map<String, String>) cacheField.get(instance);
    assertEquals("compiledTemplate", cache.get(templateId));
  }

  @Test
  public void testCreateSetFreeMarkerTemplateInCacheDoesNotCacheWhenInDevelopment() throws Exception {
    when(mockTemplate.getId()).thenReturn("DEV_TEMPLATE");
    when(mockTemplate.getModule()).thenReturn(mockModule);
    when(mockModule.isInDevelopment()).thenReturn(true);

    instance.setNextCreatedTemplate("devTemplate");

    String result = instance.createSetFreeMarkerTemplateInCache(mockTemplate, "source");

    assertEquals("devTemplate", result);

    // Verify it was NOT cached
    Field cacheField = BaseTemplateProcessor.class.getDeclaredField("templateCache");
    cacheField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<String, String> cache = (Map<String, String>) cacheField.get(instance);
    assertNull(cache.get("DEV_TEMPLATE"));
  }

  @Test
  public void testCreateSetFreeMarkerTemplateInCacheDoesNotCacheWhenIdNull() throws Exception {
    when(mockTemplate.getId()).thenReturn(null);
    when(mockTemplate.getModule()).thenReturn(null);

    instance.setNextCreatedTemplate("noIdTemplate");

    String result = instance.createSetFreeMarkerTemplateInCache(mockTemplate, "source");

    assertEquals("noIdTemplate", result);
  }

  @Test
  public void testClearCacheResetsTemplateCache() throws Exception {
    // Put something in cache
    Field cacheField = BaseTemplateProcessor.class.getDeclaredField("templateCache");
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

    @Override
    public void validate(Template template) {
      // no-op
    }

    @Override
    public String getTemplateLanguage() {
      return "TEST";
    }
  }
}
