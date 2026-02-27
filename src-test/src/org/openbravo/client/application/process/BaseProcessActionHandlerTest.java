package org.openbravo.client.application.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.client.application.Process;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.EntityAccessChecker;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;

/**
 * Tests for {@link BaseProcessActionHandler}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class BaseProcessActionHandlerTest {

  private static final String DO_REFRESH_PARENT = "doRefreshParent";
  private static final String REFRESH_PARENT = "refreshParent";
  private static final String OBUIAPP_PROCESS_FILE_UPLOAD_MAX_SIZE = "OBUIAPP_ProcessFileUploadMaxSize";
  private static final String IS_FILE_SIZE_WITHIN_LIMIT = "isFileSizeWithinLimit";

  private static final long BYTES_IN_A_MEGABYTE = 1024L * 1024L;

  @Mock
  private OBDal mockOBDal;
  @Mock
  private OBContext mockOBContext;
  @Mock
  private Process mockProcess;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<Preferences> preferencesStatic;
  private MockedStatic<EntityAccessChecker> accessCheckerStatic;

  private ConcreteProcessActionHandler instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obContextStatic = mockStatic(OBContext.class);
    lenient().when(OBContext.getOBContext()).thenReturn(mockOBContext);

    preferencesStatic = mockStatic(Preferences.class);

    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(ConcreteProcessActionHandler.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (preferencesStatic != null) preferencesStatic.close();
    if (accessCheckerStatic != null) accessCheckerStatic.close();
  }
  /**
   * Do refresh parent sets true.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoRefreshParentSetsTrue() throws Exception {
    JSONObject result = new JSONObject();

    Method method = BaseProcessActionHandler.class.getDeclaredMethod(DO_REFRESH_PARENT, JSONObject.class);
    method.setAccessible(true);

    JSONObject updated = (JSONObject) method.invoke(instance, result);
    assertTrue(updated.getBoolean(REFRESH_PARENT));
  }
  /**
   * Do refresh parent preserves true.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoRefreshParentPreservesTrue() throws Exception {
    JSONObject result = new JSONObject();
    result.put(REFRESH_PARENT, true);

    Method method = BaseProcessActionHandler.class.getDeclaredMethod(DO_REFRESH_PARENT, JSONObject.class);
    method.setAccessible(true);

    JSONObject updated = (JSONObject) method.invoke(instance, result);
    assertTrue(updated.getBoolean(REFRESH_PARENT));
  }
  /**
   * Do refresh parent when false.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoRefreshParentWhenFalse() throws Exception {
    JSONObject result = new JSONObject();
    result.put(REFRESH_PARENT, false);

    Method method = BaseProcessActionHandler.class.getDeclaredMethod(DO_REFRESH_PARENT, JSONObject.class);
    method.setAccessible(true);

    JSONObject updated = (JSONObject) method.invoke(instance, result);
    assertFalse(updated.getBoolean(REFRESH_PARENT));
  }
  /**
   * Is file size within limit under limit.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsFileSizeWithinLimitUnderLimit() throws Exception {
    preferencesStatic.when(() -> Preferences.getPreferenceValue(
        eq(OBUIAPP_PROCESS_FILE_UPLOAD_MAX_SIZE), eq(true), any(), any(), any(), any(), (String) any()))
        .thenReturn("10");

    Map<String, Object> fileParam = new HashMap<>();
    fileParam.put("size", 5L * BYTES_IN_A_MEGABYTE);

    Method method = BaseProcessActionHandler.class.getDeclaredMethod(IS_FILE_SIZE_WITHIN_LIMIT, Map.class);
    method.setAccessible(true);

    boolean result = (boolean) method.invoke(instance, fileParam);
    assertTrue(result);
  }
  /**
   * Is file size within limit over limit.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsFileSizeWithinLimitOverLimit() throws Exception {
    preferencesStatic.when(() -> Preferences.getPreferenceValue(
        eq(OBUIAPP_PROCESS_FILE_UPLOAD_MAX_SIZE), eq(true), any(), any(), any(), any(), (String) any()))
        .thenReturn("10");

    Map<String, Object> fileParam = new HashMap<>();
    fileParam.put("size", 15L * BYTES_IN_A_MEGABYTE);

    Method method = BaseProcessActionHandler.class.getDeclaredMethod(IS_FILE_SIZE_WITHIN_LIMIT, Map.class);
    method.setAccessible(true);

    boolean result = (boolean) method.invoke(instance, fileParam);
    assertFalse(result);
  }
  /**
   * Is file size exactly at limit.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsFileSizeExactlyAtLimit() throws Exception {
    preferencesStatic.when(() -> Preferences.getPreferenceValue(
        eq(OBUIAPP_PROCESS_FILE_UPLOAD_MAX_SIZE), eq(true), any(), any(), any(), any(), (String) any()))
        .thenReturn("10");

    Map<String, Object> fileParam = new HashMap<>();
    fileParam.put("size", 10L * BYTES_IN_A_MEGABYTE);

    Method method = BaseProcessActionHandler.class.getDeclaredMethod(IS_FILE_SIZE_WITHIN_LIMIT, Map.class);
    method.setAccessible(true);

    boolean result = (boolean) method.invoke(instance, fileParam);
    assertTrue(result);
  }
  /**
   * Is file size within limit when preference throws.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsFileSizeWithinLimitWhenPreferenceThrows() throws Exception {
    preferencesStatic.when(() -> Preferences.getPreferenceValue(
        eq(OBUIAPP_PROCESS_FILE_UPLOAD_MAX_SIZE), eq(true), any(), any(), any(), any(), (String) any()))
        .thenThrow(new RuntimeException("No preference"));

    Map<String, Object> fileParam = new HashMap<>();
    fileParam.put("size", 5L * BYTES_IN_A_MEGABYTE);

    Method method = BaseProcessActionHandler.class.getDeclaredMethod(IS_FILE_SIZE_WITHIN_LIMIT, Map.class);
    method.setAccessible(true);

    // When preference throws, default max is "10", so 5MB should be within limit
    boolean result = (boolean) method.invoke(instance, fileParam);
    assertTrue(result);
  }
  /**
   * Get response builder returns non null.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetResponseBuilderReturnsNonNull() throws Exception {
    Method method = BaseProcessActionHandler.class.getDeclaredMethod("getResponseBuilder");
    method.setAccessible(true);

    Object builder = method.invoke(null);
    assertNotNull(builder);
  }
  /** Bytes in megabyte constant. */

  @Test
  public void testBytesInMegabyteConstant() {
    assertEquals(1024L * 1024L, BaseProcessActionHandler.BYTES_IN_A_MEGABYTE);
  }

  /**
   * Concrete subclass for testing the abstract BaseProcessActionHandler.
   */
  private static class ConcreteProcessActionHandler extends BaseProcessActionHandler {
    @Override
    protected JSONObject doExecute(Map<String, Object> parameters, String content) {
      return new JSONObject();
    }
  }
}
