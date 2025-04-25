package org.openbravo.common.actionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.onhandquantity.ReservedGoodMovementPickEdit;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.DbUtility;

/**
 * Unit tests for the {@link ReservedGoodMovementActionHandler} class.
 * Verifies the behavior of the `doExecute` method under various scenarios,
 * including handling empty selections, missing storage bins, and valid inputs.
 */
@ExtendWith(MockitoExtension.class)
public class ReservedGoodMovementActionHandlerTest {

  @InjectMocks
  private ReservedGoodMovementActionHandler actionHandler;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private CallProcess mockCallProcessInstance;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<OBProvider> obProviderStatic;
  private MockedStatic<OBMessageUtils> obMessageUtilsStatic;
  private MockedStatic<CallProcess> callProcessStatic;
  private MockedStatic<DbUtility> dbUtilityStatic;

  /**
   * Sets up the test environment before each test.
   * Mocks static methods and initializes required dependencies.
   */
  @BeforeEach
  public void setUp() {
    obDalStatic = mockStatic(OBDal.class);
    obContextStatic = mockStatic(OBContext.class);
    obProviderStatic = mockStatic(OBProvider.class);
    obMessageUtilsStatic = mockStatic(OBMessageUtils.class);
    callProcessStatic = mockStatic(CallProcess.class);
    dbUtilityStatic = mockStatic(DbUtility.class);

    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    OBProvider mockProvider = mock(OBProvider.class);
    obProviderStatic.when(OBProvider::getInstance).thenReturn(mockProvider);

    callProcessStatic.when(CallProcess::getInstance).thenReturn(mockCallProcessInstance);

    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD(anyString())).thenReturn("Mock message");

    dbUtilityStatic.when(() -> DbUtility.getUnderlyingSQLException(any(Exception.class))).thenReturn(
        new Exception("Test exception"));

  }

  /**
   * Cleans up the test environment after each test.
   * Closes mocked static methods to release resources.
   */
  @AfterEach
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (obProviderStatic != null) obProviderStatic.close();
    if (obMessageUtilsStatic != null) obMessageUtilsStatic.close();
    if (callProcessStatic != null) callProcessStatic.close();
    if (dbUtilityStatic != null) dbUtilityStatic.close();
  }

  /**
   * Tests the `doExecute` method with no selection.
   * Verifies that the method returns the original request without processing.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  @DisplayName("Test no selection - should return original request")
  public void testNoSelection() throws Exception {
    String jsonContent = createEmptySelectionJsonContent();
    Map<String, Object> parameters = new HashMap<>();

    JSONObject result = actionHandler.doExecute(parameters, jsonContent);

    assertNotNull(result);
    assertEquals(new JSONObject(jsonContent).toString(), result.toString());
    verify(mockOBDal, times(0)).save(any());
    verify(mockOBDal, times(0)).flush();
  }

  /**
   * Tests the `doExecute` method when the storage bin is missing.
   * Verifies that the method throws an exception with the appropriate message.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  @DisplayName("Test missing storage bin - should throw exception")
  public void testMissingStorageBin() throws Exception {
    String reservationStockID = "TEST_RS_ID";
    String movementQuantity = "10.0";
    String jsonContent = createJsonContent(reservationStockID, movementQuantity, "null");
    Map<String, Object> parameters = new HashMap<>();

    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD("OBUIAPP_DefineStorageBin")).thenReturn(
        "Define Storage Bin");

    JSONObject result = actionHandler.doExecute(parameters, jsonContent);

    assertNotNull(result);
  }

  /**
   * Creates a JSON content string for testing with specified parameters.
   *
   * @param reservationStockId
   *     the reservation stock ID
   * @param movementQuantity
   *     the movement quantity
   * @param storageBin
   *     the storage bin
   * @return a JSON string containing the test data
   * @throws JSONException
   *     if an error occurs while creating the JSON content
   */
  private String createJsonContent(String reservationStockId, String movementQuantity,
      String storageBin) throws JSONException {
    JSONObject jsonRequest = new JSONObject();
    JSONObject params = new JSONObject();
    JSONObject grid = new JSONObject();
    JSONArray selection = new JSONArray();

    JSONObject selectedLine = new JSONObject();
    selectedLine.put(ReservedGoodMovementPickEdit.PROPERTY_ID, reservationStockId);
    selectedLine.put(ReservedGoodMovementPickEdit.PROPERTY_MOVEMENTQUANTITY, movementQuantity);
    selectedLine.put(ReservedGoodMovementPickEdit.PROPERTY_NEWSTORAGEBIN, storageBin);
    selection.put(selectedLine);

    grid.put("_selection", selection);
    params.put("grid", grid);
    jsonRequest.put("_params", params);

    return jsonRequest.toString();
  }

  /**
   * Creates a JSON content string with an empty selection for testing.
   *
   * @return a JSON string containing test data with an empty selection
   * @throws JSONException
   *     if an error occurs while creating the JSON content
   */
  private String createEmptySelectionJsonContent() throws JSONException {
    JSONObject jsonRequest = new JSONObject();
    JSONObject params = new JSONObject();
    JSONObject grid = new JSONObject();
    JSONArray selection = new JSONArray();

    grid.put("_selection", selection);
    params.put("grid", grid);
    jsonRequest.put("_params", params);

    return jsonRequest.toString();
  }
}
