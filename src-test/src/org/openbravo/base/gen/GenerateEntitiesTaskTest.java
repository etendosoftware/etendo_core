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

  private static final String GENERATE_ALL_CHILD_PROPERTIES = "generateAllChildProperties";
  private static final String GENERATE_DEPRECATED_PROPERTIES = "generateDeprecatedProperties";

  private GenerateEntitiesTask instance;

  @Mock
  private ModelProvider mockModelProvider;

  private MockedStatic<ModelProvider> modelProviderStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(GenerateEntitiesTask.class);
    modelProviderStatic = mockStatic(ModelProvider.class);
    modelProviderStatic.when(ModelProvider::getInstance).thenReturn(mockModelProvider);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (modelProviderStatic != null) {
      modelProviderStatic.close();
    }
  }
  /** Set and get base path. */

  @Test
  public void testSetAndGetBasePath() {
    instance.setBasePath("/test/path");
    assertEquals("/test/path", instance.getBasePath());
  }
  /** Set and get src gen path. */

  @Test
  public void testSetAndGetSrcGenPath() {
    instance.setSrcGenPath("/test/src-gen");
    assertEquals("/test/src-gen", instance.getSrcGenPath());
  }
  /** Set and get properties file. */

  @Test
  public void testSetAndGetPropertiesFile() {
    instance.setPropertiesFile("test.properties");
    assertEquals("test.properties", instance.getPropertiesFile());
  }
  /**
   * Should add deprecation both false.
   * @throws Exception if an error occurs
   */

  @Test
  public void testShouldAddDeprecationBothFalse() throws Exception {
    Field genAllChild = GenerateEntitiesTask.class.getDeclaredField(GENERATE_ALL_CHILD_PROPERTIES);
    genAllChild.setAccessible(true);
    genAllChild.set(instance, false);

    Field genDepr = GenerateEntitiesTask.class.getDeclaredField(GENERATE_DEPRECATED_PROPERTIES);
    genDepr.setAccessible(true);
    genDepr.set(instance, false);

    assertFalse(instance.shouldAddDeprecation());
  }
  /**
   * Should add deprecation when generate deprecated true.
   * @throws Exception if an error occurs
   */

  @Test
  public void testShouldAddDeprecationWhenGenerateDeprecatedTrue() throws Exception {
    Field genAllChild = GenerateEntitiesTask.class.getDeclaredField(GENERATE_ALL_CHILD_PROPERTIES);
    genAllChild.setAccessible(true);
    genAllChild.set(instance, false);

    Field genDepr = GenerateEntitiesTask.class.getDeclaredField(GENERATE_DEPRECATED_PROPERTIES);
    genDepr.setAccessible(true);
    genDepr.set(instance, true);

    assertTrue(instance.shouldAddDeprecation());
  }
  /**
   * Should add deprecation when generate all child true.
   * @throws Exception if an error occurs
   */

  @Test
  public void testShouldAddDeprecationWhenGenerateAllChildTrue() throws Exception {
    Field genAllChild = GenerateEntitiesTask.class.getDeclaredField(GENERATE_ALL_CHILD_PROPERTIES);
    genAllChild.setAccessible(true);
    genAllChild.set(instance, true);

    Field genDepr = GenerateEntitiesTask.class.getDeclaredField(GENERATE_DEPRECATED_PROPERTIES);
    genDepr.setAccessible(true);
    genDepr.set(instance, false);

    assertTrue(instance.shouldAddDeprecation());
  }
  /** Is deprecated entity true. */

  @Test
  public void testIsDeprecatedEntityTrue() {
    Entity entity = mock(Entity.class);
    when(entity.isDeprecated()).thenReturn(true);
    assertTrue(instance.isDeprecated(entity));
  }
  /** Is deprecated entity false. */

  @Test
  public void testIsDeprecatedEntityFalse() {
    Entity entity = mock(Entity.class);
    when(entity.isDeprecated()).thenReturn(false);
    assertFalse(instance.isDeprecated(entity));
  }
  /** Is deprecated entity null. */

  @Test
  public void testIsDeprecatedEntityNull() {
    Entity entity = mock(Entity.class);
    when(entity.isDeprecated()).thenReturn(null);
    assertFalse(instance.isDeprecated(entity));
  }
  /** Is deprecated property directly deprecated. */

  @Test
  public void testIsDeprecatedPropertyDirectlyDeprecated() {
    Property property = mock(Property.class);
    when(property.isDeprecated()).thenReturn(true);
    assertTrue(instance.isDeprecated(property));
  }
  /** Is deprecated property target entity deprecated. */

  @Test
  public void testIsDeprecatedPropertyTargetEntityDeprecated() {
    Property property = mock(Property.class);
    Entity targetEntity = mock(Entity.class);
    when(property.isDeprecated()).thenReturn(false);
    when(property.getTargetEntity()).thenReturn(targetEntity);
    when(targetEntity.isDeprecated()).thenReturn(true);
    assertTrue(instance.isDeprecated(property));
  }
  /** Is deprecated property not deprecated. */

  @Test
  public void testIsDeprecatedPropertyNotDeprecated() {
    Property property = mock(Property.class);
    when(property.isDeprecated()).thenReturn(false);
    when(property.getTargetEntity()).thenReturn(null);
    when(property.getReferencedProperty()).thenReturn(null);
    assertFalse(instance.isDeprecated(property));
  }
  /** Get deprecation message property deprecated. */

  @Test
  public void testGetDeprecationMessagePropertyDeprecated() {
    Property property = mock(Property.class);
    when(property.isDeprecated()).thenReturn(true);
    String msg = instance.getDeprecationMessage(property);
    assertEquals("Property marked as deprecated on field Development Status", msg);
  }
  /** Get deprecation message target entity deprecated. */

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
  /** Get deprecation message backward compatibility. */

  @Test
  public void testGetDeprecationMessageBackwardCompatibility() {
    Property property = mock(Property.class);
    when(property.isDeprecated()).thenReturn(false);
    when(property.getTargetEntity()).thenReturn(null);
    String msg = instance.getDeprecationMessage(property);
    assertTrue(msg.contains("backward compatibility"));
  }
  /** Format sql logic null. */

  @Test
  public void testFormatSqlLogicNull() {
    assertNull(instance.formatSqlLogic(null));
  }
  /** Format sql logic escapes comment end. */

  @Test
  public void testFormatSqlLogicEscapesCommentEnd() {
    String result = instance.formatSqlLogic("SELECT */ FROM table");
    assertFalse(result.contains("*/"));
  }
  /** Format sql logic short string. */

  @Test
  public void testFormatSqlLogicShortString() {
    String result = instance.formatSqlLogic("SELECT 1");
    assertEquals("SELECT 1", result);
  }
  /** Generated dir constant. */

  @Test
  public void testGeneratedDirConstant() {
    assertEquals("/../build/tmp/generated", GenerateEntitiesTask.GENERATED_DIR);
  }
}
