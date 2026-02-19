package org.openbravo.base.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;

/**
 * Tests for {@link GenerateEntitiesTask}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class GenerateEntitiesTaskTest {

  private GenerateEntitiesTask instance;

  @Mock
  private ModelProvider mockModelProvider;

  private MockedStatic<ModelProvider> modelProviderStatic;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(GenerateEntitiesTask.class);
    modelProviderStatic = mockStatic(ModelProvider.class);
    modelProviderStatic.when(ModelProvider::getInstance).thenReturn(mockModelProvider);
  }

  @After
  public void tearDown() {
    if (modelProviderStatic != null) {
      modelProviderStatic.close();
    }
  }

  @Test
  public void testSetAndGetBasePath() {
    instance.setBasePath("/test/path");
    assertEquals("/test/path", instance.getBasePath());
  }

  @Test
  public void testSetAndGetSrcGenPath() {
    instance.setSrcGenPath("/test/src-gen");
    assertEquals("/test/src-gen", instance.getSrcGenPath());
  }

  @Test
  public void testSetAndGetPropertiesFile() {
    instance.setPropertiesFile("test.properties");
    assertEquals("test.properties", instance.getPropertiesFile());
  }

  @Test
  public void testShouldAddDeprecationBothFalse() throws Exception {
    Field genAllChild = GenerateEntitiesTask.class.getDeclaredField("generateAllChildProperties");
    genAllChild.setAccessible(true);
    genAllChild.set(instance, false);

    Field genDepr = GenerateEntitiesTask.class.getDeclaredField("generateDeprecatedProperties");
    genDepr.setAccessible(true);
    genDepr.set(instance, false);

    assertFalse(instance.shouldAddDeprecation());
  }

  @Test
  public void testShouldAddDeprecationWhenGenerateDeprecatedTrue() throws Exception {
    Field genAllChild = GenerateEntitiesTask.class.getDeclaredField("generateAllChildProperties");
    genAllChild.setAccessible(true);
    genAllChild.set(instance, false);

    Field genDepr = GenerateEntitiesTask.class.getDeclaredField("generateDeprecatedProperties");
    genDepr.setAccessible(true);
    genDepr.set(instance, true);

    assertTrue(instance.shouldAddDeprecation());
  }

  @Test
  public void testShouldAddDeprecationWhenGenerateAllChildTrue() throws Exception {
    Field genAllChild = GenerateEntitiesTask.class.getDeclaredField("generateAllChildProperties");
    genAllChild.setAccessible(true);
    genAllChild.set(instance, true);

    Field genDepr = GenerateEntitiesTask.class.getDeclaredField("generateDeprecatedProperties");
    genDepr.setAccessible(true);
    genDepr.set(instance, false);

    assertTrue(instance.shouldAddDeprecation());
  }

  @Test
  public void testIsDeprecatedEntityTrue() {
    Entity entity = mock(Entity.class);
    when(entity.isDeprecated()).thenReturn(true);
    assertTrue(instance.isDeprecated(entity));
  }

  @Test
  public void testIsDeprecatedEntityFalse() {
    Entity entity = mock(Entity.class);
    when(entity.isDeprecated()).thenReturn(false);
    assertFalse(instance.isDeprecated(entity));
  }

  @Test
  public void testIsDeprecatedEntityNull() {
    Entity entity = mock(Entity.class);
    when(entity.isDeprecated()).thenReturn(null);
    assertFalse(instance.isDeprecated(entity));
  }

  @Test
  public void testIsDeprecatedPropertyDirectlyDeprecated() {
    Property property = mock(Property.class);
    when(property.isDeprecated()).thenReturn(true);
    assertTrue(instance.isDeprecated(property));
  }

  @Test
  public void testIsDeprecatedPropertyTargetEntityDeprecated() {
    Property property = mock(Property.class);
    Entity targetEntity = mock(Entity.class);
    when(property.isDeprecated()).thenReturn(false);
    when(property.getTargetEntity()).thenReturn(targetEntity);
    when(targetEntity.isDeprecated()).thenReturn(true);
    assertTrue(instance.isDeprecated(property));
  }

  @Test
  public void testIsDeprecatedPropertyNotDeprecated() {
    Property property = mock(Property.class);
    when(property.isDeprecated()).thenReturn(false);
    when(property.getTargetEntity()).thenReturn(null);
    when(property.getReferencedProperty()).thenReturn(null);
    assertFalse(instance.isDeprecated(property));
  }

  @Test
  public void testGetDeprecationMessagePropertyDeprecated() {
    Property property = mock(Property.class);
    when(property.isDeprecated()).thenReturn(true);
    String msg = instance.getDeprecationMessage(property);
    assertEquals("Property marked as deprecated on field Development Status", msg);
  }

  @Test
  public void testGetDeprecationMessageTargetEntityDeprecated() {
    Property property = mock(Property.class);
    Entity targetEntity = mock(Entity.class);
    when(property.isDeprecated()).thenReturn(false);
    when(property.getTargetEntity()).thenReturn(targetEntity);
    when(targetEntity.isDeprecated()).thenReturn(true);
    when(targetEntity.getSimpleClassName()).thenReturn("MyEntity");
    String msg = instance.getDeprecationMessage(property);
    assertTrue(msg.contains("MyEntity"));
    assertTrue(msg.contains("deprecated"));
  }

  @Test
  public void testGetDeprecationMessageBackwardCompatibility() {
    Property property = mock(Property.class);
    when(property.isDeprecated()).thenReturn(false);
    when(property.getTargetEntity()).thenReturn(null);
    String msg = instance.getDeprecationMessage(property);
    assertTrue(msg.contains("backward compatibility"));
  }

  @Test
  public void testFormatSqlLogicNull() {
    assertNull(instance.formatSqlLogic(null));
  }

  @Test
  public void testFormatSqlLogicEscapesCommentEnd() {
    String result = instance.formatSqlLogic("SELECT */ FROM table");
    assertFalse(result.contains("*/"));
  }

  @Test
  public void testFormatSqlLogicShortString() {
    String result = instance.formatSqlLogic("SELECT 1");
    assertEquals("SELECT 1", result);
  }

  @Test
  public void testGeneratedDirConstant() {
    assertEquals("/../build/tmp/generated", GenerateEntitiesTask.GENERATED_DIR);
  }
}
