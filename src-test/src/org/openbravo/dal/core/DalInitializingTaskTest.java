package org.openbravo.dal.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
/** Tests for {@link DalInitializingTask}. */

@RunWith(MockitoJUnitRunner.class)
public class DalInitializingTaskTest {

  private DalInitializingTask instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(DalInitializingTask.class);
  }
  /** Get set properties file. */

  @Test
  public void testGetSetPropertiesFile() {
    assertNull(instance.getPropertiesFile());
    instance.setPropertiesFile("/tmp/Openbravo.properties");
    assertEquals("/tmp/Openbravo.properties", instance.getPropertiesFile());
  }
  /** Get set user id. */

  @Test
  public void testGetSetUserId() {
    assertNull(instance.getUserId());
    instance.setUserId("100");
    assertEquals("100", instance.getUserId());
  }
  /** Get set provider config directory. */

  @Test
  public void testGetSetProviderConfigDirectory() {
    assertNull(instance.getProviderConfigDirectory());
    instance.setProviderConfigDirectory("/tmp/config");
    assertEquals("/tmp/config", instance.getProviderConfigDirectory());
  }
  /** Is set admin mode. */

  @Test
  public void testIsSetAdminMode() {
    assertFalse(instance.isAdminMode());
    instance.setAdminMode(true);
    assertTrue(instance.isAdminMode());
  }
  /** Is set re initialize model. */

  @Test
  public void testIsSetReInitializeModel() {
    assertFalse(instance.isReInitializeModel());
    instance.setReInitializeModel(true);
    assertTrue(instance.isReInitializeModel());
  }
  /** Do execute default is no op. */

  @Test
  public void testDoExecuteDefaultIsNoOp() {
    // The default doExecute() does nothing - verify it doesn't throw
    instance.doExecute();
  }
}
