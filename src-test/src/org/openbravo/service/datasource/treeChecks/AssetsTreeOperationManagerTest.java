package org.openbravo.service.datasource.treeChecks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.assetmgmt.Asset;
import org.openbravo.service.datasource.CheckTreeOperationManager;
/** Tests for {@link AssetsTreeOperationManager}. */
@SuppressWarnings({"java:S112", "java:S120"})

@RunWith(MockitoJUnitRunner.Silent.class)
public class AssetsTreeOperationManagerTest {

  private static final String TEST_NODE_ID = "NODE_001";
  private static final String TEST_PARENT_ID = "PARENT_001";
  private AssetsTreeOperationManager instance;

  @Mock
  private OBDal mockOBDal;
  @Mock
  private Asset mockAsset;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBMessageUtils> obMessageUtilsStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    instance = new AssetsTreeOperationManager();

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obMessageUtilsStatic = mockStatic(OBMessageUtils.class);
    lenient().when(OBMessageUtils.messageBD(anyString())).thenAnswer(inv -> inv.getArgument(0));

    when(mockOBDal.get(eq(Asset.class), eq(TEST_NODE_ID))).thenReturn(mockAsset);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obMessageUtilsStatic != null) obMessageUtilsStatic.close();
  }
  /**
   * Check node movement static asset returns false.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCheckNodeMovementStaticAssetReturnsFalse() throws Exception {
    when(mockAsset.isStatic()).thenReturn(true);
    Map<String, String> params = new HashMap<>();

    Object response = instance.checkNodeMovement(params, TEST_NODE_ID, TEST_PARENT_ID, null, null);

    assertFalse(getSuccess(response));
    assertEquals("error", getMessageType(response));
  }
  /**
   * Check node movement non static asset returns true.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCheckNodeMovementNonStaticAssetReturnsTrue() throws Exception {
    when(mockAsset.isStatic()).thenReturn(false);
    Map<String, String> params = new HashMap<>();

    Object response = instance.checkNodeMovement(params, TEST_NODE_ID, TEST_PARENT_ID, null, null);

    assertTrue(getSuccess(response));
  }

  private boolean getSuccess(Object actionResponse) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = actionResponse.getClass().getMethod("isSuccess");
    return (boolean) method.invoke(actionResponse);
  }

  private String getMessageType(Object actionResponse) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = actionResponse.getClass().getMethod("getMessageType");
    return (String) method.invoke(actionResponse);
  }
}
