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

@RunWith(MockitoJUnitRunner.class)
public class DalInitializingTaskTest {

  private DalInitializingTask instance;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(DalInitializingTask.class);
  }

  @Test
  public void testGetSetPropertiesFile() {
    assertNull(instance.getPropertiesFile());
    instance.setPropertiesFile("/tmp/Openbravo.properties");
    assertEquals("/tmp/Openbravo.properties", instance.getPropertiesFile());
  }

  @Test
  public void testGetSetUserId() {
    assertNull(instance.getUserId());
    instance.setUserId("100");
    assertEquals("100", instance.getUserId());
  }

  @Test
  public void testGetSetProviderConfigDirectory() {
    assertNull(instance.getProviderConfigDirectory());
    instance.setProviderConfigDirectory("/tmp/config");
    assertEquals("/tmp/config", instance.getProviderConfigDirectory());
  }

  @Test
  public void testIsSetAdminMode() {
    assertFalse(instance.isAdminMode());
    instance.setAdminMode(true);
    assertTrue(instance.isAdminMode());
  }

  @Test
  public void testIsSetReInitializeModel() {
    assertFalse(instance.isReInitializeModel());
    instance.setReInitializeModel(true);
    assertTrue(instance.isReInitializeModel());
  }

  @Test
  public void testDoExecuteDefaultIsNoOp() {
    // The default doExecute() does nothing - verify it doesn't throw
    instance.doExecute();
  }
}
