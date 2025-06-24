package org.openbravo.costing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.service.db.DbUtility;

/**
 * Unit tests for the {@link CostAdjustmentProcessHandler} class.
 * Verifies the behavior of the `execute` method under various scenarios,
 * including successful execution, exceptions, and invalid input handling.
 */
@ExtendWith(MockitoExtension.class)
public class CostAdjustmentProcessHandlerTest {
  private static final String TEST_ID = "testId";
  private static final String COST_ADJUSTMENT_ID = "M_CostAdjustment_ID";
  private static final String SEVERITY = "severity";
  private static final String MESSAGE = "message";
  private static final String ERROR = "Error";
  private static final String ERROR_LOWERCASE = "error";
  private static final String UNEXPECTED_ERROR = "Unexpected Error";

  private CostAdjustmentProcessHandler classUnderTest;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<CostAdjustmentProcess> mockedCostAdjustmentProcess;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<DbUtility> mockedDbUtility;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private CostAdjustment mockCostAdjustment;

  /**
   * Sets up the test environment before each test.
   * Initializes the class under test and mocks static methods.
   */
  @BeforeEach
  public void setUp() {
    classUnderTest = new CostAdjustmentProcessHandler();

    mockedOBDal = mockStatic(OBDal.class);
    mockedCostAdjustmentProcess = mockStatic(CostAdjustmentProcess.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedDbUtility = mockStatic(DbUtility.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
  }

  /**
   * Cleans up the test environment after each test.
   * Closes all mocked static instances.
   */
  @AfterEach
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedCostAdjustmentProcess != null) {
      mockedCostAdjustmentProcess.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
    if (mockedDbUtility != null) {
      mockedDbUtility.close();
    }
  }

  /**
   * Tests the `execute` method for a successful cost adjustment process.
   * Verifies that the response contains a success message.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteSuccess() throws Exception {
    String costAdjustmentId = TEST_ID;
    JSONObject mockRequestContent = new JSONObject();
    mockRequestContent.put(COST_ADJUSTMENT_ID, costAdjustmentId);

    JSONObject mockResponseMessage = new JSONObject();
    mockResponseMessage.put(SEVERITY, "success");
    mockResponseMessage.put("text", "Cost Adjustment Processed");

    when(mockOBDal.get(CostAdjustment.class, costAdjustmentId)).thenReturn(mockCostAdjustment);

    mockedCostAdjustmentProcess.when(
        () -> CostAdjustmentProcess.doProcessCostAdjustment(mockCostAdjustment)).thenReturn(mockResponseMessage);

    JSONObject result = classUnderTest.execute(null, mockRequestContent.toString());

    assertNotNull(result);
    JSONObject message = result.getJSONObject(MESSAGE);
    assertEquals("success", message.get(SEVERITY));
    assertEquals("Cost Adjustment Processed", message.get("text"));

    verify(mockOBDal).get(CostAdjustment.class, costAdjustmentId);
    mockedCostAdjustmentProcess.verify(() -> CostAdjustmentProcess.doProcessCostAdjustment(mockCostAdjustment));
  }

  /**
   * Tests the `execute` method when an {@link OBException} is thrown.
   * Verifies that the response contains an error message with the exception details.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteWithOBException() throws Exception {
    String costAdjustmentId = TEST_ID;
    JSONObject mockRequestContent = new JSONObject();
    mockRequestContent.put(COST_ADJUSTMENT_ID, costAdjustmentId);

    OBException mockException = new OBException("Cost Adjustment Error");
    when(mockOBDal.get(CostAdjustment.class, costAdjustmentId)).thenThrow(mockException);

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(ERROR)).thenReturn(ERROR);

    JSONObject result = classUnderTest.execute(null, mockRequestContent.toString());

    assertNotNull(result);
    JSONObject message = result.getJSONObject(MESSAGE);
    assertEquals(ERROR_LOWERCASE, message.get(SEVERITY));
    assertEquals("Cost Adjustment Error", message.get("text"));

    verify(mockOBDal).rollbackAndClose();
  }

  /**
   * Tests the `execute` method when a {@link org.codehaus.jettison.json.JSONException} is thrown.
   * Verifies that the response contains an error message for invalid JSON input.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteWithJSONException() throws Exception {
    String invalidContent = "{ invalidJson }";

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(ERROR)).thenReturn(ERROR);

    JSONObject result = classUnderTest.execute(null, invalidContent);

    assertNotNull(result);
    JSONObject message = result.getJSONObject(MESSAGE);
    assertEquals(ERROR_LOWERCASE, message.get(SEVERITY));

    assertNotNull(message.get("text"));

    verify(mockOBDal).rollbackAndClose();
  }

  /**
   * Tests the `execute` method when a general exception is thrown.
   * Verifies that the response contains an error message with the exception details.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteWithGeneralException() throws Exception {
    String costAdjustmentId = TEST_ID;
    JSONObject mockRequestContent = new JSONObject();
    mockRequestContent.put(COST_ADJUSTMENT_ID, costAdjustmentId);

    NullPointerException mockException = new NullPointerException(UNEXPECTED_ERROR);
    when(mockOBDal.get(CostAdjustment.class, costAdjustmentId)).thenThrow(mockException);

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(ERROR)).thenReturn(ERROR);
    mockedOBMessageUtils.when(() -> OBMessageUtils.parseTranslation("@ErrorProcessingCostAdj@")).thenReturn(
        "Error Processing Cost Adjustment");

    mockedDbUtility.when(() -> DbUtility.getUnderlyingSQLException(mockException)).thenReturn(mockException);

    org.openbravo.erpCommon.utility.OBError translatedError = mock(org.openbravo.erpCommon.utility.OBError.class);
    when(translatedError.getMessage()).thenReturn(UNEXPECTED_ERROR);
    mockedOBMessageUtils.when(() -> OBMessageUtils.translateError(mockException.getMessage())).thenReturn(
        translatedError);

    JSONObject result = classUnderTest.execute(null, mockRequestContent.toString());

    assertNotNull(result);
    JSONObject message = result.getJSONObject(MESSAGE);
    assertEquals(ERROR_LOWERCASE, message.get(SEVERITY));
    assertEquals(UNEXPECTED_ERROR, message.get("text"));

    verify(mockOBDal).rollbackAndClose();
  }
}
