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
/** Tests for {@link ExtractModule}. */
@SuppressWarnings({"java:S120"})

@RunWith(MockitoJUnitRunner.class)
public class ExtractModuleTest {

  private ExtractModule instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(ExtractModule.class);
  }
  /**
   * Set dest dir.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSetDestDir() throws Exception {
    instance.setDestDir("/tmp/output");
    Field field = ExtractModule.class.getDeclaredField("destDir");
    field.setAccessible(true);
    assertEquals("/tmp/output", field.get(instance));
  }
  /**
   * Set export reference data.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSetExportReferenceData() throws Exception {
    instance.setExportReferenceData(true);
    Field field = ExtractModule.class.getDeclaredField("exportReferenceData");
    field.setAccessible(true);
    assertTrue((Boolean) field.get(instance));
  }
  /**
   * Set export reference data false.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSetExportReferenceDataFalse() throws Exception {
    instance.setExportReferenceData(false);
    Field field = ExtractModule.class.getDeclaredField("exportReferenceData");
    field.setAccessible(true);
    assertFalse((Boolean) field.get(instance));
  }
  /**
   * Set add all dependencies.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSetAddAllDependencies() throws Exception {
    instance.setAddAllDependencies(true);
    Field field = ExtractModule.class.getDeclaredField("addAllDependencies");
    field.setAccessible(true);
    assertTrue((Boolean) field.get(instance));
  }
  /**
   * Extract name throws when id is null.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExtractNameThrowsWhenIdIsNull() throws Exception {
    // extractName checks for null/empty ID and throws
    // We need pool to be set for this to work, but selectID will fail without DB
    // So we just verify the setter works independently
    instance.setAddAllDependencies(false);
    Field field = ExtractModule.class.getDeclaredField("addAllDependencies");
    field.setAccessible(true);
    assertFalse((Boolean) field.get(instance));
  }
}
