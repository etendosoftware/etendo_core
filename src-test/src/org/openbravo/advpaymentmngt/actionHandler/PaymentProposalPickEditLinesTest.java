package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentPropDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentProposal;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;

/**
 * Unit tests for the PaymentProposalPickEditLines class.
 */
@RunWith(MockitoJUnitRunner.class)
public class PaymentProposalPickEditLinesTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  // Static mocks
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBDao> mockedOBDao;
  private MockedStatic<OBProvider> mockedOBProvider;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;

  // Mocks
  @Mock
  private OBDal mockOBDal;

  @Mock
  private FIN_PaymentProposal mockPaymentProposal;

  @Mock
  private FIN_PaymentMethod mockPaymentMethod;

  @Mock
  private FIN_PaymentPropDetail mockPaymentPropDetail;

  @Mock
  private FIN_PaymentScheduleDetail mockPaymentScheduleDetail;

  @Mock
  private List<FIN_PaymentPropDetail> mockPaymentPropDetailList;

  @InjectMocks
  private PaymentProposalPickEditLines classUnderTest;

  private AutoCloseable mocks;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);

    // Setup static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBDao = mockStatic(OBDao.class);
    mockedOBProvider = mockStatic(OBProvider.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);

    // Configure static mocks
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    mockedOBContext.when(() -> OBContext.setAdminMode()).thenAnswer(invocation -> null);
    mockedOBContext.when(() -> OBContext.restorePreviousMode()).thenAnswer(invocation -> null);

    // Setup common mock behaviors
    when(mockPaymentProposal.getFINPaymentPropDetailList()).thenReturn(mockPaymentPropDetailList);
  }

  /**
   * Cleans up the test environment after each test.
   *
   * @throws Exception
   *     if an error occurs during cleanup
   */
  @After
  public void tearDown() throws Exception {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBDao != null) {
      mockedOBDao.close();
    }
    if (mockedOBProvider != null) {
      mockedOBProvider.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Tests the doExecute method with a different payment method.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testDoExecuteWithDifferentPaymentMethod() throws Exception {
    // GIVEN
    String paymentProposalId = TestConstants.TEST_PROPOSAL_ID;
    String paymentMethodId = TestConstants.TEST_METHOD_ID;
    String linePaymentMethodId = "DIFFERENT_METHOD_ID";

    // Create test content JSON with selection that will trigger different payment method warning
    String content = createTestContentJsonWithSelection(paymentProposalId, paymentMethodId, linePaymentMethodId);

    // Setup mocks
    when(mockOBDal.get(FIN_PaymentProposal.class, paymentProposalId)).thenReturn(mockPaymentProposal);
    when(mockOBDal.get(FIN_PaymentMethod.class, paymentMethodId)).thenReturn(mockPaymentMethod);

    FIN_PaymentMethod differentPaymentMethod = mock(FIN_PaymentMethod.class);
    when(mockOBDal.get(FIN_PaymentMethod.class, linePaymentMethodId)).thenReturn(differentPaymentMethod);

    List<String> idList = new ArrayList<>();
    mockedOBDao.when(() -> OBDao.getIDListFromOBObject(mockPaymentPropDetailList)).thenReturn(idList);

    // Mock payment prop detail
    FIN_PaymentPropDetail newPPD = mock(FIN_PaymentPropDetail.class);
    OBProvider provider = mock(OBProvider.class);
    mockedOBProvider.when(() -> OBProvider.getInstance()).thenReturn(provider);
    when(provider.get(FIN_PaymentPropDetail.class)).thenReturn(newPPD);

    // Mock payment schedule detail
    when(mockOBDal.get(FIN_PaymentScheduleDetail.class, TestConstants.TEST_PSD_ID)).thenReturn(
        mockPaymentScheduleDetail);

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(TestConstants.MESSAGE_SUCCESS)).thenReturn(
        TestConstants.MESSAGE_SUCCESS);
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("APRM_Different_PaymentMethod_Selected")).thenReturn(
        "Different payment method selected");

    Map<String, Object> parameters = new HashMap<>();

    // WHEN
    JSONObject result = classUnderTest.doExecute(parameters, content);

    // THEN
    assertNotNull(result);
    assertTrue(result.has(TestConstants.RESPONSE_MESSAGE));
    JSONObject message = result.getJSONObject(TestConstants.RESPONSE_MESSAGE);
    assertEquals("warning", message.getString(TestConstants.SEVERITY));
    assertEquals("Different payment method selected", message.getString("text"));
  }

  /**
   * Tests the doExecute method when an exception occurs.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testDoExecuteWithException() throws Exception {
    // GIVEN
    String paymentProposalId = TestConstants.TEST_PROPOSAL_ID;
    String paymentMethodId = TestConstants.TEST_METHOD_ID;

    // Create test content JSON
    String content = createTestContentJson(paymentProposalId, paymentMethodId, false);

    // Setup mocks to throw exception
    when(mockOBDal.get(FIN_PaymentProposal.class, paymentProposalId)).thenThrow(new OBException("Test exception"));

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("Test exception")).thenReturn("Test exception message");

    Map<String, Object> parameters = new HashMap<>();

    // WHEN
    JSONObject result = classUnderTest.doExecute(parameters, content);

    // THEN
    assertNotNull(result);
    assertTrue(result.has(TestConstants.RESPONSE_MESSAGE));
    JSONObject message = result.getJSONObject(TestConstants.RESPONSE_MESSAGE);
    assertEquals("error", message.getString(TestConstants.SEVERITY));
    assertEquals("Test exception message", message.getString("text"));

    // Verify rollback was called
    verify(mockOBDal).rollbackAndClose();
  }

  /**
   * Tests the doExecute method with no selected lines.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testDoExecuteNoSelectedLines() throws Exception {
    // GIVEN
    String paymentProposalId = TestConstants.TEST_PROPOSAL_ID;
    String paymentMethodId = TestConstants.PAYMENT_METHOD_ID;

    // Create test content JSON with no selected lines
    String content = createTestContentJson(paymentProposalId, paymentMethodId, true);

    // Setup mocks
    when(mockOBDal.get(FIN_PaymentProposal.class, paymentProposalId)).thenReturn(mockPaymentProposal);
    when(mockOBDal.get(FIN_PaymentMethod.class, paymentMethodId)).thenReturn(mockPaymentMethod);

    List<String> idList = new ArrayList<>();
    mockedOBDao.when(() -> OBDao.getIDListFromOBObject(mockPaymentPropDetailList)).thenReturn(idList);

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(TestConstants.MESSAGE_SUCCESS)).thenReturn(
        TestConstants.MESSAGE_SUCCESS);

    Map<String, Object> parameters = new HashMap<>();

    // WHEN
    JSONObject result = classUnderTest.doExecute(parameters, content);

    // THEN
    assertNotNull(result);
    assertTrue(result.has(TestConstants.RESPONSE_MESSAGE));
    JSONObject message = result.getJSONObject(TestConstants.RESPONSE_MESSAGE);
    assertEquals(TestConstants.RESULT_SUCCESS, message.getString(TestConstants.SEVERITY));
    assertEquals(TestConstants.MESSAGE_SUCCESS, message.getString("text"));
  }

  /**
   * Tests the removeNonSelectedLines method.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testRemoveNonSelectedLines() throws Exception {
    // GIVEN
    List<String> idList = new ArrayList<>();
    String detailId = "TEST_DETAIL_ID";
    idList.add(detailId);

    when(mockOBDal.get(FIN_PaymentPropDetail.class, detailId)).thenReturn(mockPaymentPropDetail);

    // WHEN
    // We need to use reflection to access the private method
    java.lang.reflect.Method method = PaymentProposalPickEditLines.class.getDeclaredMethod("removeNonSelectedLines",
        List.class, FIN_PaymentProposal.class);
    method.setAccessible(true);
    method.invoke(classUnderTest, idList, mockPaymentProposal);

    // THEN
    verify(mockPaymentPropDetailList).remove(mockPaymentPropDetail);
    verify(mockOBDal).remove(mockPaymentPropDetail);
    verify(mockOBDal).save(mockPaymentProposal);
    verify(mockOBDal).flush();
  }

  /**
   * Creates a test JSON content string.
   *
   * @param paymentProposalId
   *     the payment proposal ID
   * @param paymentMethodId
   *     the payment method ID
   * @param emptySelection
   *     whether the selection is empty
   * @return the test JSON content string
   * @throws JSONException
   *     if an error occurs during JSON creation
   */
  private String createTestContentJson(String paymentProposalId, String paymentMethodId,
      boolean emptySelection) throws JSONException {
    JSONObject jsonContent = new JSONObject();
    jsonContent.put("Fin_Payment_Proposal_ID", paymentProposalId);
    jsonContent.put("inpfinPaymentmethodId", paymentMethodId);

    JSONObject params = new JSONObject();
    JSONObject grid = new JSONObject();
    JSONArray selection = new JSONArray();

    if (!emptySelection) {
      // Add a sample selected line if needed
      JSONObject selectedLine = new JSONObject();
      selectedLine.put("id", "TEST_LINE_ID");
      selectedLine.put("payment", "100.00");
      selectedLine.put("paymentMethod", paymentMethodId);
      selectedLine.put("paymentScheduleDetail", TestConstants.TEST_PSD_ID);
      selectedLine.put("difference", "0.00");
      selectedLine.put("writeoff", "false");
      selection.put(selectedLine);
    }

    grid.put("_selection", selection);
    params.put("grid", grid);
    jsonContent.put("_params", params);

    return jsonContent.toString();
  }

  /**
   * Creates a test JSON content string with selection.
   *
   * @param paymentProposalId
   *     the payment proposal ID
   * @param paymentMethodId
   *     the payment method ID
   * @param linePaymentMethodId
   *     the line payment method ID
   * @return the test JSON content string with selection
   * @throws JSONException
   *     if an error occurs during JSON creation
   */
  private String createTestContentJsonWithSelection(String paymentProposalId, String paymentMethodId,
      String linePaymentMethodId) throws JSONException {
    JSONObject jsonContent = new JSONObject();
    jsonContent.put("Fin_Payment_Proposal_ID", paymentProposalId);
    jsonContent.put("inpfinPaymentmethodId", paymentMethodId);

    JSONObject params = new JSONObject();
    JSONObject grid = new JSONObject();
    JSONArray selection = new JSONArray();

    // Add a sample selected line with different payment method
    JSONObject selectedLine = new JSONObject();
    selectedLine.put("id", "TEST_LINE_ID");
    selectedLine.put("payment", "100.00");
    selectedLine.put("paymentMethod", linePaymentMethodId);
    selectedLine.put("paymentScheduleDetail", TestConstants.TEST_PSD_ID);
    selectedLine.put("difference", "0.00");
    selectedLine.put("writeoff", "false");
    selection.put(selectedLine);

    grid.put("_selection", selection);
    params.put("grid", grid);
    jsonContent.put("_params", params);

    return jsonContent.toString();
  }
}
