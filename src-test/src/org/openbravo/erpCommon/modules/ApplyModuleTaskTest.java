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
import org.openbravo.base.session.OBPropertiesProvider;
/** Tests for {@link ApplyModuleTask}. */
@SuppressWarnings({"java:S120"})

@RunWith(MockitoJUnitRunner.class)
public class ApplyModuleTaskTest {

  private static final String TEST_OB_DIR = "/test/openbravo";

  private ApplyModuleTask instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(ApplyModuleTask.class);
  }
  /**
   * Set ob dir.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSetObDir() throws Exception {
    instance.setObDir(TEST_OB_DIR);

    Field obDirField = ApplyModuleTask.class.getDeclaredField("obDir");
    obDirField.setAccessible(true);
    assertEquals(TEST_OB_DIR, obDirField.get(instance));
  }
  /**
   * Set force ref data.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSetForceRefData() throws Exception {
    instance.setForceRefData(true);

    Field forceField = ApplyModuleTask.class.getDeclaredField("forceRefData");
    forceField.setAccessible(true);
    assertTrue((Boolean) forceField.get(instance));
  }
  /**
   * Set force ref data false.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSetForceRefDataFalse() throws Exception {
    instance.setForceRefData(false);

    Field forceField = ApplyModuleTask.class.getDeclaredField("forceRefData");
    forceField.setAccessible(true);
    assertFalse((Boolean) forceField.get(instance));
  }
  /** Set friendly warnings. */

  @Test
  public void testSetFriendlyWarnings() {
    instance.setFriendlyWarnings(true);
    assertTrue(OBPropertiesProvider.isFriendlyWarnings());

    instance.setFriendlyWarnings(false);
    assertFalse(OBPropertiesProvider.isFriendlyWarnings());
  }
  /** Get friendly warnings. */

  @Test
  public void testGetFriendlyWarnings() {
    OBPropertiesProvider.setFriendlyWarnings(true);
    assertTrue(instance.getFriendlyWarnings());

    OBPropertiesProvider.setFriendlyWarnings(false);
    assertFalse(instance.getFriendlyWarnings());
  }
}
