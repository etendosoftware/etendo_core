package org.openbravo.client.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
/** Tests for {@link GetLabelActionHandler}. */
@SuppressWarnings("java:S112")

@RunWith(MockitoJUnitRunner.Silent.class)
public class GetLabelActionHandlerTest {

  private static final String EXECUTE = "execute";

  private GetLabelActionHandler instance;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<KernelUtils> kernelUtilsStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    instance = new GetLabelActionHandler();
    obContextStatic = mockStatic(OBContext.class);
    kernelUtilsStatic = mockStatic(KernelUtils.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obContextStatic != null) {
      obContextStatic.close();
    }
    if (kernelUtilsStatic != null) {
      kernelUtilsStatic.close();
    }
  }
  /**
   * Execute throws when key parameter missing.
   * @throws IllegalAccessException if an error occurs
   * @throws InvocationTargetException if an error occurs
   * @throws NoSuchMethodException if an error occurs
   */

  @Test(expected = OBException.class)
  public void testExecuteThrowsWhenKeyParameterMissing() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();

    // Act
    Method executeMethod = GetLabelActionHandler.class.getDeclaredMethod(EXECUTE, Map.class, String.class);
    executeMethod.setAccessible(true);
    try {
      executeMethod.invoke(instance, parameters, null);
    } catch (java.lang.reflect.InvocationTargetException e) {
      if (e.getCause() instanceof OBException) {
        throw (OBException) e.getCause();
      }
      throw new RuntimeException(e);
    }
  }
  /**
   * Execute returns label when found.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteReturnsLabelWhenFound() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("key", "OBUIAPP_ActionButton");

    KernelUtils mockKernelUtils = mock(KernelUtils.class);
    kernelUtilsStatic.when(KernelUtils::getInstance).thenReturn(mockKernelUtils);
    when(mockKernelUtils.getI18N(eq("OBUIAPP_ActionButton"), any())).thenReturn("Action Button");

    // Act
    Method executeMethod = GetLabelActionHandler.class.getDeclaredMethod(EXECUTE, Map.class, String.class);
    executeMethod.setAccessible(true);
    JSONObject result = (JSONObject) executeMethod.invoke(instance, parameters, null);

    // Assert
    assertNotNull(result);
    assertEquals("Action Button", result.getString("label"));
  }
  /**
   * Execute returns empty json when label not found.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteReturnsEmptyJsonWhenLabelNotFound() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("key", "NONEXISTENT_KEY");

    KernelUtils mockKernelUtils = mock(KernelUtils.class);
    kernelUtilsStatic.when(KernelUtils::getInstance).thenReturn(mockKernelUtils);
    when(mockKernelUtils.getI18N(eq("NONEXISTENT_KEY"), any())).thenReturn(null);

    // Act
    Method executeMethod = GetLabelActionHandler.class.getDeclaredMethod(EXECUTE, Map.class, String.class);
    executeMethod.setAccessible(true);
    JSONObject result = (JSONObject) executeMethod.invoke(instance, parameters, null);

    // Assert
    assertNotNull(result);
    assertFalse(result.has("label"));
  }
  /**
   * Execute wraps exception from kernel utils.
   * @throws Exception if an error occurs
   */

  @Test(expected = OBException.class)
  public void testExecuteWrapsExceptionFromKernelUtils() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("key", "ERROR_KEY");

    KernelUtils mockKernelUtils = mock(KernelUtils.class);
    kernelUtilsStatic.when(KernelUtils::getInstance).thenReturn(mockKernelUtils);
    when(mockKernelUtils.getI18N(eq("ERROR_KEY"), any())).thenThrow(new RuntimeException("DB error"));

    // Act
    Method executeMethod = GetLabelActionHandler.class.getDeclaredMethod(EXECUTE, Map.class, String.class);
    executeMethod.setAccessible(true);
    try {
      executeMethod.invoke(instance, parameters, null);
    } catch (java.lang.reflect.InvocationTargetException e) {
      if (e.getCause() instanceof OBException) {
        throw (OBException) e.getCause();
      }
      throw new RuntimeException(e);
    }
  }
}
