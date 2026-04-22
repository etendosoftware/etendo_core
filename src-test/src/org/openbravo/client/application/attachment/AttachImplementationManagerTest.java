package org.openbravo.client.application.attachment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import javax.enterprise.inject.Instance;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.kernel.ComponentProvider;

/**
 * Tests for AttachImplementationManager.
 */
@SuppressWarnings({"java:S4042", "java:S899", "java:S112"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class AttachImplementationManagerTest {

  private static final String TEST_ATTACHMENT = "test-attachment-";
  private static final String DELETE_TEMP_FILE = "deleteTempFile";

  private static MockedStatic<ModelProvider> modelProviderStatic;

  private AttachImplementationManager instance;

  @Mock
  private Instance<AttachImplementation> attachImplementationHandlers;

  @Mock
  private ApplicationDictionaryCachedStructures adcs;
  /** Sets up test fixtures. */

  @BeforeClass
  public static void setUpClass() {
    ModelProvider mockModelProvider = mock(ModelProvider.class);
    Entity mockEntity = mock(Entity.class);
    Property mockProperty = mock(Property.class);
    when(mockProperty.getFieldLength()).thenReturn(200);
    when(mockEntity.getProperty(anyString())).thenReturn(mockProperty);
    when(mockModelProvider.getEntity(any(Class.class))).thenReturn(mockEntity);
    when(mockModelProvider.getEntityByTableId(anyString())).thenReturn(mockEntity);
    modelProviderStatic = mockStatic(ModelProvider.class);
    modelProviderStatic.when(ModelProvider::getInstance).thenReturn(mockModelProvider);
  }
  /** Tears down test fixtures. */

  @AfterClass
  public static void tearDownClass() {
    if (modelProviderStatic != null) {
      modelProviderStatic.close();
    }
  }
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(AttachImplementationManager.class);

    setPrivateField(instance, "attachImplementationHandlers", attachImplementationHandlers);
    setPrivateField(instance, "adcs", adcs);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    // modelProviderStatic is managed at class level
  }
  /**
   * Get json value with non empty value.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetJSONValueWithNonEmptyValue() throws Exception {
    Method method = AttachImplementationManager.class.getDeclaredMethod("getJSONValue",
        String.class);
    method.setAccessible(true);

    JSONObject result = (JSONObject) method.invoke(instance, "testValue");

    assertNotNull(result);
    assertEquals("testValue", result.getString("value"));
  }
  /**
   * Get json value with empty string.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetJSONValueWithEmptyString() throws Exception {
    Method method = AttachImplementationManager.class.getDeclaredMethod("getJSONValue",
        String.class);
    method.setAccessible(true);

    JSONObject result = (JSONObject) method.invoke(instance, "");

    assertNotNull(result);
    assertEquals("", result.getString("value"));
  }
  /** Get handler returns null when no implementation. */

  @Test
  public void testGetHandlerReturnsNullWhenNoImplementation() {
    Instance<AttachImplementation> selectedInstance = mock(Instance.class);
    when(attachImplementationHandlers.select(any(ComponentProvider.Selector.class)))
        .thenReturn(selectedInstance);
    when(selectedInstance.iterator()).thenReturn(Collections.emptyIterator());

    AttachImplementation result = instance.getHandler("Default");

    assertNull(result);
  }
  /** Get handler returns single implementation. */

  @Test
  public void testGetHandlerReturnsSingleImplementation() {
    AttachImplementation mockHandler = mock(AttachImplementation.class);
    Instance<AttachImplementation> selectedInstance = mock(Instance.class);
    when(attachImplementationHandlers.select(any(ComponentProvider.Selector.class)))
        .thenReturn(selectedInstance);
    when(selectedInstance.iterator()).thenReturn(
        Collections.singletonList(mockHandler).iterator());

    AttachImplementation result = instance.getHandler("Default");

    assertNotNull(result);
    assertEquals(mockHandler, result);
  }
  /**
   * Delete temp file in tmp dir.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDeleteTempFileInTmpDir() throws Exception {
    File tempFile = File.createTempFile(TEST_ATTACHMENT, ".tmp");
    tempFile.deleteOnExit();

    Method method = AttachImplementationManager.class.getDeclaredMethod(DELETE_TEMP_FILE,
        File.class);
    method.setAccessible(true);
    method.invoke(instance, tempFile);

    // File should be deleted
    assertEquals(false, tempFile.exists());
  }
  /**
   * Delete temp file in sub directory.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDeleteTempFileInSubDirectory() throws Exception {
    Path tempDir = Files.createTempDirectory("test-attach-dir-");
    File tempFile = Files.createTempFile(tempDir, TEST_ATTACHMENT, ".tmp").toFile();

    Method method = AttachImplementationManager.class.getDeclaredMethod(DELETE_TEMP_FILE,
        File.class);
    method.setAccessible(true);
    method.invoke(instance, tempFile);

    // File should be deleted
    assertEquals(false, tempFile.exists());
    // Parent dir should also be deleted (was empty)
    assertEquals(false, tempDir.toFile().exists());
  }
  /**
   * Delete temp file in non empty sub directory.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDeleteTempFileInNonEmptySubDirectory() throws Exception {
    Path tempDir = Files.createTempDirectory("test-attach-dir-");
    File tempFile = Files.createTempFile(tempDir, TEST_ATTACHMENT, ".tmp").toFile();
    File otherFile = Files.createTempFile(tempDir, "other-", ".tmp").toFile();
    otherFile.deleteOnExit();
    tempDir.toFile().deleteOnExit();

    Method method = AttachImplementationManager.class.getDeclaredMethod(DELETE_TEMP_FILE,
        File.class);
    method.setAccessible(true);
    method.invoke(instance, tempFile);

    // File should be deleted
    assertEquals(false, tempFile.exists());
    // Parent dir should NOT be deleted (has other files)
    assertEquals(true, tempDir.toFile().exists());

    // Cleanup
    otherFile.delete();
    tempDir.toFile().delete();
  }
  /** Reference list constant. */

  @Test
  public void testReferenceListConstant() {
    assertEquals("17", AttachImplementationManager.REFERENCE_LIST);
  }
  /** Reference selector constant. */

  @Test
  public void testReferenceSelectorConstant() {
    assertEquals("95E2A8B50A254B2AAE6774B8C2F28120",
        AttachImplementationManager.REFERENCE_SELECTOR_REFERENCE);
  }

  private void setPrivateField(Object target, String fieldName, Object value) throws IllegalAccessException, NoSuchFieldException {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
