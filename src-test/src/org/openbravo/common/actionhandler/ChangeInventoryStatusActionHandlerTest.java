package org.openbravo.common.actionhandler;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.erpCommon.utility.InventoryStatusUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Tests for {@link ChangeInventoryStatusActionHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ChangeInventoryStatusActionHandlerTest {

  private static final String TEST_LOCATOR_ID = "LOC001";
  private static final String TEST_INVENTORY_STATUS_ID = "STATUS001";

  private ChangeInventoryStatusActionHandler instance;

  private MockedStatic<InventoryStatusUtils> inventoryStatusUtilsStatic;
  private MockedStatic<OBMessageUtils> obMessageUtilsStatic;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(ChangeInventoryStatusActionHandler.class);

    inventoryStatusUtilsStatic = mockStatic(InventoryStatusUtils.class);
    obMessageUtilsStatic = mockStatic(OBMessageUtils.class);
    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD(anyString())).thenReturn("Success");
  }

  @After
  public void tearDown() {
    if (inventoryStatusUtilsStatic != null) inventoryStatusUtilsStatic.close();
    if (obMessageUtilsStatic != null) obMessageUtilsStatic.close();
  }

  @Test
  public void testDoExecuteSuccessfulStatusChange() throws Exception {
    String content = buildRequestContent(TEST_LOCATOR_ID, TEST_INVENTORY_STATUS_ID);
    Map<String, Object> parameters = new HashMap<>();

    JSONObject result = invokeDoExecute(parameters, content);

    assertNotNull(result);
    inventoryStatusUtilsStatic.verify(
        () -> InventoryStatusUtils.changeStatusOfStorageBin(TEST_LOCATOR_ID,
            TEST_INVENTORY_STATUS_ID));
  }

  @Test
  public void testDoExecuteHandlesWarningException() throws Exception {
    String content = buildRequestContent(TEST_LOCATOR_ID, TEST_INVENTORY_STATUS_ID);
    Map<String, Object> parameters = new HashMap<>();

    inventoryStatusUtilsStatic.when(
        () -> InventoryStatusUtils.changeStatusOfStorageBin(anyString(), anyString()))
        .thenThrow(new RuntimeException("WARNING: Bin not empty"));

    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD("Warning")).thenReturn("Warning");

    JSONObject result = invokeDoExecute(parameters, content);

    assertNotNull(result);
  }

  @Test
  public void testDoExecuteHandlesGenericException() throws Exception {
    String content = buildRequestContent(TEST_LOCATOR_ID, TEST_INVENTORY_STATUS_ID);
    Map<String, Object> parameters = new HashMap<>();

    inventoryStatusUtilsStatic.when(
        () -> InventoryStatusUtils.changeStatusOfStorageBin(anyString(), anyString()))
        .thenThrow(new RuntimeException("Database connection failed"));

    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD("Error")).thenReturn("Error");

    JSONObject result = invokeDoExecute(parameters, content);

    assertNotNull(result);
  }

  private String buildRequestContent(String locatorId, String inventoryStatusId) throws Exception {
    JSONObject params = new JSONObject();
    params.put("M_InventoryStatus_ID", inventoryStatusId);

    JSONObject request = new JSONObject();
    request.put("M_Locator_ID", locatorId);
    request.put("_params", params);
    return request.toString();
  }

  private JSONObject invokeDoExecute(Map<String, Object> parameters, String content)
      throws Exception {
    java.lang.reflect.Method doExecute = ChangeInventoryStatusActionHandler.class.getDeclaredMethod(
        "doExecute", Map.class, String.class);
    doExecute.setAccessible(true);
    return (JSONObject) doExecute.invoke(instance, parameters, content);
  }
}
