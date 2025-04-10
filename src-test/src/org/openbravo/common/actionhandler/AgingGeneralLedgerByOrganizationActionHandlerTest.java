package org.openbravo.common.actionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBLedgerUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;

/**
 * Tests the {@code getExpression} method with a valid organization in the context.
 * Verifies that the correct organization ID is returned and the query is properly parameterized.
 */
public class AgingGeneralLedgerByOrganizationActionHandlerTest {

  private AgingGeneralLedgerByOrganizationActionHandler actionHandler;
  private MockedStatic<OBDal> obDalMock;
  private MockedStatic<OBLedgerUtils> obLedgerUtilsMock;
  private AutoCloseable closeable;

  @Mock
  private OBDal obDalInstance;

  @Mock
  private AcctSchema acctSchema;

  /**
   * Sets up the test environment before each test.
   * Initialize mocks and the action handler instance.
   */
  @BeforeEach
  public void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
    actionHandler = new AgingGeneralLedgerByOrganizationActionHandler();

    obDalMock = mockStatic(OBDal.class);
    obDalMock.when(OBDal::getInstance).thenReturn(obDalInstance);

    obLedgerUtilsMock = mockStatic(OBLedgerUtils.class);
  }

  /**
   * Cleans up the test environment after each test.
   * Closes all mocked static instances and resources.
   *
   * @throws Exception
   *     if an error occurs during cleanup
   */
  @AfterEach
  public void tearDown() throws Exception {
    obDalMock.close();
    obLedgerUtilsMock.close();
    closeable.close();
  }

  /**
   * Tests the {@code execute} method with valid input.
   * Verifies that the method returns the expected JSON response
   * and interacts correctly with mocked dependencies.
   *
   * @throws JSONException
   *     if an error occurs while creating or parsing JSON
   */
  @Test
  public void testExecuteValidInput() throws JSONException {
    String orgId = "testOrgId";
    String ledgerId = "testLedgerId";
    String ledgerName = "Test Ledger";

    JSONObject inputJson = new JSONObject();
    inputJson.put("organization", orgId);

    obLedgerUtilsMock.when(() -> OBLedgerUtils.getOrgLedger(orgId)).thenReturn(ledgerId);
    when(obDalInstance.get(AcctSchema.class, ledgerId)).thenReturn(acctSchema);
    when(acctSchema.getName()).thenReturn(ledgerName);

    Map<String, Object> parameters = new HashMap<>();

    JSONObject result = actionHandler.execute(parameters, inputJson.toString());

    assertTrue(result.has("response"));
    JSONObject response = result.getJSONObject("response");
    assertEquals(ledgerId, response.getString("value"));
    assertEquals(ledgerName, response.getString("identifier"));

    obLedgerUtilsMock.verify(() -> OBLedgerUtils.getOrgLedger(orgId));
    verify(obDalInstance).get(AcctSchema.class, ledgerId);
    verify(acctSchema).getName();
  }

  /**
   * Tests the {@code execute} method with invalid input.
   * Verifies that the method returns an error message in the JSON response.
   *
   * @throws JSONException
   *     if an error occurs while creating or parsing JSON
   */
  @Test
  public void testExecuteInvalidInput() throws JSONException {
    JSONObject inputJson = new JSONObject();
    Map<String, Object> parameters = new HashMap<>();

    JSONObject result = actionHandler.execute(parameters, inputJson.toString());

    assertTrue(result.has("message"));
    JSONObject message = result.getJSONObject("message");
    assertEquals("error", message.getString("severity"));
    assertEquals("Error", message.getString("title"));
    assertTrue(message.getString("text").contains("not found"));
  }
}
