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
@RunWith(MockitoJUnitRunner.Silent.class)
public class AttachImplementationManagerTest {

  private static MockedStatic<ModelProvider> modelProviderStatic;

  private AttachImplementationManager instance;

  @Mock
  private Instance<AttachImplementation> attachImplementationHandlers;

  @Mock
  private ApplicationDictionaryCachedStructures adcs;

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

  @AfterClass
  public static void tearDownClass() {
    if (modelProviderStatic != null) {
      modelProviderStatic.close();
    }
  }

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(AttachImplementationManager.class);

    setPrivateField(instance, "attachImplementationHandlers", attachImplementationHandlers);
    setPrivateField(instance, "adcs", adcs);
  }

  @After
  public void tearDown() {
    // modelProviderStatic is managed at class level
  }

  @Test
  public void testGetJSONValueWithNonEmptyValue() throws Exception {
    Method method = AttachImplementationManager.class.getDeclaredMethod("getJSONValue",
        String.class);
    method.setAccessible(true);

    JSONObject result = (JSONObject) method.invoke(instance, "testValue");

    assertNotNull(result);
    assertEquals("testValue", result.getString("value"));
  }

  @Test
  public void testGetJSONValueWithEmptyString() throws Exception {
    Method method = AttachImplementationManager.class.getDeclaredMethod("getJSONValue",
        String.class);
    method.setAccessible(true);

    JSONObject result = (JSONObject) method.invoke(instance, "");

    assertNotNull(result);
    assertEquals("", result.getString("value"));
  }

  @Test
  public void testGetHandlerReturnsNullWhenNoImplementation() {
    Instance<AttachImplementation> selectedInstance = mock(Instance.class);
    when(attachImplementationHandlers.select(any(ComponentProvider.Selector.class)))
        .thenReturn(selectedInstance);
    when(selectedInstance.iterator()).thenReturn(Collections.emptyIterator());

    AttachImplementation result = instance.getHandler("Default");

    assertNull(result);
  }

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

  @Test
  public void testDeleteTempFileInTmpDir() throws Exception {
    File tempFile = File.createTempFile("test-attachment-", ".tmp");
    tempFile.deleteOnExit();

    Method method = AttachImplementationManager.class.getDeclaredMethod("deleteTempFile",
        File.class);
    method.setAccessible(true);
    method.invoke(instance, tempFile);

    // File should be deleted
    assertEquals(false, tempFile.exists());
  }

  @Test
  public void testDeleteTempFileInSubDirectory() throws Exception {
    Path tempDir = Files.createTempDirectory("test-attach-dir-");
    File tempFile = Files.createTempFile(tempDir, "test-attachment-", ".tmp").toFile();

    Method method = AttachImplementationManager.class.getDeclaredMethod("deleteTempFile",
        File.class);
    method.setAccessible(true);
    method.invoke(instance, tempFile);

    // File should be deleted
    assertEquals(false, tempFile.exists());
    // Parent dir should also be deleted (was empty)
    assertEquals(false, tempDir.toFile().exists());
  }

  @Test
  public void testDeleteTempFileInNonEmptySubDirectory() throws Exception {
    Path tempDir = Files.createTempDirectory("test-attach-dir-");
    File tempFile = Files.createTempFile(tempDir, "test-attachment-", ".tmp").toFile();
    File otherFile = Files.createTempFile(tempDir, "other-", ".tmp").toFile();
    otherFile.deleteOnExit();
    tempDir.toFile().deleteOnExit();

    Method method = AttachImplementationManager.class.getDeclaredMethod("deleteTempFile",
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

  @Test
  public void testReferenceListConstant() {
    assertEquals("17", AttachImplementationManager.REFERENCE_LIST);
  }

  @Test
  public void testReferenceSelectorConstant() {
    assertEquals("95E2A8B50A254B2AAE6774B8C2F28120",
        AttachImplementationManager.REFERENCE_SELECTOR_REFERENCE);
  }

  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
