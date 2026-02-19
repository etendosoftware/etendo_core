package org.openbravo.client.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.dal.core.OBContext;

/**
 * Tests for {@link BaseTemplateComponent}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class BaseTemplateComponentTest {

  private BaseTemplateComponent instance;

  @Mock
  private Template mockTemplate;

  @Mock
  private TemplateProcessor mockProcessor;

  @Mock
  private TemplateProcessor.Registry mockRegistry;

  private MockedStatic<OBContext> obContextStatic;

  @Before
  public void setUp() throws Exception {
    obContextStatic = mockStatic(OBContext.class);

    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(BaseTemplateComponent.class);

    // Set the injected registry via reflection
    Field registryField = BaseTemplateComponent.class.getDeclaredField("templateProcessRegistry");
    registryField.setAccessible(true);
    registryField.set(instance, mockRegistry);

    // Set parameters map via reflection on BaseComponent
    Field parametersField = BaseComponent.class.getDeclaredField("parameters");
    parametersField.setAccessible(true);
    parametersField.set(instance, new HashMap<String, Object>());
  }

  @After
  public void tearDown() {
    if (obContextStatic != null) {
      obContextStatic.close();
    }
  }

  @Test
  public void testGenerateReturnsProcessedTemplate() throws Exception {
    String expectedOutput = "generated-js-code";
    when(mockTemplate.getTemplateLanguage()).thenReturn("FTL");
    when(mockRegistry.get("FTL")).thenReturn(mockProcessor);
    when(mockProcessor.process(any(Template.class), any(Map.class))).thenReturn(expectedOutput);

    // Set componentTemplate field
    Field templateField = BaseTemplateComponent.class.getDeclaredField("componentTemplate");
    templateField.setAccessible(true);
    templateField.set(instance, mockTemplate);

    String result = instance.generate();

    assertEquals(expectedOutput, result);
    obContextStatic.verify(() -> OBContext.setAdminMode());
    obContextStatic.verify(() -> OBContext.restorePreviousMode());
  }

  @Test
  public void testGenerateAddsDataParameterWhenNotNull() throws Exception {
    when(mockTemplate.getTemplateLanguage()).thenReturn("FTL");
    when(mockRegistry.get("FTL")).thenReturn(mockProcessor);
    when(mockProcessor.process(any(Template.class), any(Map.class))).thenReturn("output");

    Field templateField = BaseTemplateComponent.class.getDeclaredField("componentTemplate");
    templateField.setAccessible(true);
    templateField.set(instance, mockTemplate);

    instance.generate();

    // getData() returns 'this' so the DATA_PARAMETER should be set
    Map<String, Object> params = instance.getParameters();
    assertNotNull(params.get(BaseTemplateComponent.DATA_PARAMETER));
    assertEquals(instance, params.get(BaseTemplateComponent.DATA_PARAMETER));
  }

  @Test
  public void testGetDataReturnsThis() {
    Object data = instance.getData();
    assertEquals(instance, data);
  }

  @Test
  public void testSetAndGetComponentTemplate() {
    instance.setComponentTemplate(mockTemplate);
    assertEquals(mockTemplate, instance.getComponentTemplate());
  }

  @Test
  public void testConstants() {
    assertEquals("Base", BaseTemplateComponent.BASE_QUALIFIER);
    assertEquals("data", BaseTemplateComponent.DATA_PARAMETER);
  }
}
