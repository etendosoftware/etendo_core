package org.openbravo.common.actionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Date;
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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.financial.ResetAccounting;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.json.JsonUtils;

/**
 * Unit tests for the {@link ResetAccountingHandler} class.
 * Verifies the behavior of the `doExecute` method under various scenarios,
 * including successful delete and restore operations.
 */
@ExtendWith(MockitoExtension.class)
public class ResetAccountingHandlerTest {

  @InjectMocks
  private ResetAccountingHandler handler;

  private MockedStatic<ResetAccounting> mockedResetAccounting;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<OBDateUtils> mockedOBDateUtils;
  private MockedStatic<DbUtility> mockedDbUtility;
  private MockedStatic<JsonUtils> mockedJsonUtils;

  /**
   * Mocks static methods and initializes required dependencies before each test.
   */
  @BeforeEach
  void setUp() {
    mockedResetAccounting = mockStatic(ResetAccounting.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedOBDateUtils = mockStatic(OBDateUtils.class);
    mockedDbUtility = mockStatic(DbUtility.class);
    mockedJsonUtils = mockStatic(JsonUtils.class);
  }

  /**
   * Closes mocked static methods and releases resources after each test.
   */
  @AfterEach
  void tearDown() {
    mockedResetAccounting.close();
    mockedOBMessageUtils.close();
    mockedOBDateUtils.close();
    mockedDbUtility.close();
    mockedJsonUtils.close();
  }

  /**
   * Tests the `doExecute` method for a successful delete operation.
   * Verifies that the method processes the input correctly and returns the expected result.
   *
   * @throws Exception if an error occurs during the test
   */
  @Test
  @DisplayName("Test successful delete operation")
  void testDeletePostingSuccess() throws Exception {
    String clientId = "testClientId";
    String orgId = "testOrgId";
    String dateFrom = "2023-01-01";
    String dateTo = "2023-12-31";
    String formattedDateFrom = "01-01-2023";
    String formattedDateTo = "31-12-2023";

    Map<String, Object> parameters = new HashMap<>();
    String content = createJsonContent(clientId, orgId, true, dateFrom, dateTo, new String[]{"259", "260"});

    SimpleDateFormat mockFormat = new SimpleDateFormat("yyyy-MM-dd");
    when(JsonUtils.createDateFormat()).thenReturn(mockFormat);

    mockedOBDateUtils.when(() -> OBDateUtils.formatDate(any(Date.class)))
        .thenReturn(formattedDateFrom)
        .thenReturn(formattedDateTo);

    HashMap<String, Integer> resetResult = new HashMap<>();
    resetResult.put("updated", 10);
    resetResult.put("deleted", 5);

    mockedResetAccounting.when(() -> ResetAccounting.delete(
            eq(clientId),
            eq(orgId),
            anyList(),
            eq(formattedDateFrom),
            eq(formattedDateTo)))
        .thenReturn(resetResult);

    mockedOBMessageUtils.when(() -> OBMessageUtils.parseTranslation(anyString()))
        .thenReturn("Unposted Documents = 10, Deleted Entries = 5");

    JSONObject result = handler.doExecute(parameters, content);

    assertNotNull(result);
    assertTrue(result.has(ActionHandlerTestConstants.MESSAGE));

    JSONObject message = result.getJSONObject(ActionHandlerTestConstants.MESSAGE);
    assertEquals("success", message.getString("severity"));
    assertEquals("Unposted Documents = 10, Deleted Entries = 5", message.getString("text"));

    mockedResetAccounting.verify(() -> ResetAccounting.delete(
        eq(clientId),
        eq(orgId),
        anyList(),
        eq(formattedDateFrom),
        eq(formattedDateTo)));
  }

  /**
   * Tests the `doExecute` method for a successful restore operation.
   * Verifies that the method processes the input correctly and returns the expected result.
   *
   * @throws Exception if an error occurs during the test
   */
  @Test
  @DisplayName("Test successful restore operation")
  void testRestorePostingSuccess() throws Exception {
    String clientId = "testClientId";
    String orgId = "testOrgId";

    Map<String, Object> parameters = new HashMap<>();
    String content = createJsonContent(clientId, orgId, false, "null", "null", new String[]{"259"});

    HashMap<String, Integer> resetResult = new HashMap<>();
    resetResult.put("updated", 15);
    resetResult.put("deleted", 0);

    mockedResetAccounting.when(() -> ResetAccounting.restore(
            eq(clientId),
            eq(orgId),
            anyList(),
            eq(""),
            eq("")))
        .thenReturn(resetResult);

    mockedOBMessageUtils.when(() -> OBMessageUtils.parseTranslation(anyString()))
        .thenReturn("Unposted Documents = 15, Deleted Entries = 0");

    JSONObject result = handler.doExecute(parameters, content);

    assertNotNull(result);
    assertTrue(result.has(ActionHandlerTestConstants.MESSAGE));

    JSONObject message = result.getJSONObject(ActionHandlerTestConstants.MESSAGE);
    assertEquals("success", message.getString("severity"));
    assertEquals("Unposted Documents = 15, Deleted Entries = 0", message.getString("text"));

    mockedResetAccounting.verify(() -> ResetAccounting.restore(
        eq(clientId),
        eq(orgId),
        anyList(),
        eq(""),
        eq("")));
  }

  /**
   * Creates a JSON content string for testing.
   * The JSON includes client ID, organization ID, posting type, date range, and table IDs.
   *
   * @param clientId the client ID to include in the JSON content
   * @param orgId the organization ID to include in the JSON content
   * @param deletePosting whether the operation is a delete posting
   * @param dateFrom the start date for the operation
   * @param dateTo the end date for the operation
   * @param tableIds the table IDs to include in the JSON content
   * @return a JSON string containing test data
   * @throws JSONException if an error occurs while creating the JSON content
   */
  private String createJsonContent(String clientId, String orgId, boolean deletePosting,
      String dateFrom, String dateTo, String[] tableIds) throws JSONException {

    JSONObject params = new JSONObject();
    params.put("AD_Client_ID", clientId);
    params.put("AD_Org_ID", orgId);
    params.put("DeletePosting", deletePosting ? "true" : "false");
    params.put("datefrom", dateFrom);
    params.put("dateto", dateTo);

    JSONArray tableIdsArray = new JSONArray();
    for (String tableId : tableIds) {
      tableIdsArray.put(tableId);
    }
    params.put("AD_Table_ID", tableIdsArray);

    JSONObject request = new JSONObject();
    request.put("_params", params);

    return request.toString();
  }
}
