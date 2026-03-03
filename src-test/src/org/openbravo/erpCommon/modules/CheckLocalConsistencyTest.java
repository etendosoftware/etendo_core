package org.openbravo.erpCommon.modules;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;

import org.apache.tools.ant.BuildException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.service.centralrepository.Module;
/** Tests for {@link CheckLocalConsistency}. */
@SuppressWarnings({"java:S120", "java:S112"})

@RunWith(MockitoJUnitRunner.class)
public class CheckLocalConsistencyTest {

  private CheckLocalConsistency instance;

  private MockedStatic<VersionUtility> versionUtilityStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(CheckLocalConsistency.class);
    versionUtilityStatic = mockStatic(VersionUtility.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (versionUtilityStatic != null) {
      versionUtilityStatic.close();
    }
  }

  private void invokeDoExecute() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = CheckLocalConsistency.class.getDeclaredMethod("doExecute");
    method.setAccessible(true);
    method.invoke(instance);
  }
  /**
   * Do execute with satisfied dependencies.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoExecuteWithSatisfiedDependencies() throws Exception {
    versionUtilityStatic.when(() -> VersionUtility.checkLocal(
        any(VariablesSecureApp.class),
        any(Module[].class),
        any(Module[].class),
        any(Module[].class),
        any(OBError.class)
    )).thenReturn(true);

    invokeDoExecute();
    // Should complete without exception
  }
  /**
   * Do execute with unsatisfied dependencies.
   * @throws Exception if an error occurs
   */

  @Test(expected = BuildException.class)
  public void testDoExecuteWithUnsatisfiedDependencies() throws Exception {
    versionUtilityStatic.when(() -> VersionUtility.checkLocal(
        any(VariablesSecureApp.class),
        any(Module[].class),
        any(Module[].class),
        any(Module[].class),
        any(OBError.class)
    )).thenReturn(false);

    try {
      invokeDoExecute();
    } catch (java.lang.reflect.InvocationTargetException e) {
      if (e.getCause() instanceof BuildException) {
        throw (BuildException) e.getCause();
      }
      throw new RuntimeException(e);
    }
  }
  /**
   * Do execute with check local throwing exception.
   * @throws Exception if an error occurs
   */

  @Test(expected = BuildException.class)
  public void testDoExecuteWithCheckLocalThrowingException() throws Exception {
    versionUtilityStatic.when(() -> VersionUtility.checkLocal(
        any(VariablesSecureApp.class),
        any(Module[].class),
        any(Module[].class),
        any(Module[].class),
        any(OBError.class)
    )).thenThrow(new RuntimeException("DB connection failed"));

    try {
      invokeDoExecute();
    } catch (java.lang.reflect.InvocationTargetException e) {
      if (e.getCause() instanceof BuildException) {
        throw (BuildException) e.getCause();
      }
      throw new RuntimeException(e);
    }
  }
}
