package org.openbravo.erpCommon.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
/** Tests for {@link ExtractModuleTask}. */
@SuppressWarnings({"java:S120"})

@RunWith(MockitoJUnitRunner.class)
public class ExtractModuleTaskTest {

  private ExtractModuleTask instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(ExtractModuleTask.class);
  }
  /**
   * Set dest dir.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSetDestDir() throws Exception {
    instance.setDestDir("/tmp/dest");
    Field field = ExtractModuleTask.class.getDeclaredField("destDir");
    field.setAccessible(true);
    assertEquals("/tmp/dest", field.get(instance));
  }
  /**
   * Set module name.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSetModuleName() throws Exception {
    instance.setModuleName("com.example.module");
    Field field = ExtractModuleTask.class.getDeclaredField("moduleName");
    field.setAccessible(true);
    assertEquals("com.example.module", field.get(instance));
  }
  /**
   * Set module id.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSetModuleID() throws Exception {
    instance.setModuleID("ABC123");
    Field field = ExtractModuleTask.class.getDeclaredField("moduleID");
    field.setAccessible(true);
    assertEquals("ABC123", field.get(instance));
  }
  /**
   * Set ob dir.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSetObDir() throws Exception {
    instance.setObDir("/opt/openbravo");
    Field field = ExtractModuleTask.class.getDeclaredField("obDir");
    field.setAccessible(true);
    assertEquals("/opt/openbravo", field.get(instance));
  }
  /**
   * Set export rd.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSetExportRD() throws Exception {
    instance.setExportRD(true);
    Field field = ExtractModuleTask.class.getDeclaredField("exportRD");
    field.setAccessible(true);
    assertTrue((Boolean) field.get(instance));
  }
  /**
   * Set add all dependencies.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSetAddAllDependencies() throws Exception {
    instance.setAddAllDependencies(true);
    Field field = ExtractModuleTask.class.getDeclaredField("addAllDependencies");
    field.setAccessible(true);
    assertTrue((Boolean) field.get(instance));
  }
  /**
   * Export rd default is false.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExportRDDefaultIsFalse() throws Exception {
    Field field = ExtractModuleTask.class.getDeclaredField("exportRD");
    field.setAccessible(true);
    assertFalse((Boolean) field.get(instance));
  }
}
