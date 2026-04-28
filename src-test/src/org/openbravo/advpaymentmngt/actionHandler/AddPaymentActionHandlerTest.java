package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.servlet.ServletException;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Criterion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.advpaymentmngt.hook.PaymentProcessHook;
import org.openbravo.advpaymentmngt.hook.PaymentProcessOrderHook;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.advpaymentmngt.process.FIN_PaymentProcess;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.process.ResponseActionsBuilder;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;

import com.smf.jobs.ActionResult;
import com.smf.jobs.Data;
import com.smf.jobs.Result;

/**
 * Unit tests for {@link AddPaymentActionHandler}.
 * Tests the payment action handler logic including payment processing,
 * credit handling, GL item addition, and hook execution.
 */
@SuppressWarnings({"java:S120", "java:S1448", "java:S4144", "java:S112"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class AddPaymentActionHandlerTest {

  private static final String PRE_PROCESS = "preProcess";
  private static final String SEVERITY = "severity";
  private static final String ERROR = "error";
  private static final String MESSAGE = "message";
  private static final String SUCCESS = "success";
  private static final String PAY001 = "pay001";
  private static final String VAL_100_00 = "100.00";
  private static final String IS_FULLY_PAID = "isFullyPaid";
  private static final String HAS_NEGATIVE_LINES = "hasNegativeLines";
  private static final String GET_ACCOUNT_DIMENSION = "getAccountDimension";
  private static final String BUSINESS_PARTNER = "businessPartner";
  private static final String GET_DOCUMENT_CONFIRMATION = "getDocumentConfirmation";
  private static final String MASSIVE_MESSAGE_HANDLER = "massiveMessageHandler";
  private static final String INPUT = "input";
  private static final String DOCUMENT_ACTION = "document_action";
  private static final String PAY001_2 = "PAY001";
  private static final String RETRY_EXECUTION = "retryExecution";
  private static final String FIN_PAYMENT = "FIN_Payment";
  private static final String ENTITY_NAME = "_entityName";
  private static final String ACTION = "action";
  private static final String REMOVE_NOT_SELECTED_PAYMENT_DETAILS = "removeNotSelectedPaymentDetails";
  private static final String PD001 = "PD001";
  private static final String PSD001 = "PSD001";

  private AddPaymentActionHandler handler;

  @Mock
  private OBDal mockOBDal;
  @Mock
  private OBContext mockOBContext;
  @Mock
  private RequestContext mockRequestContext;
  @Mock
  private VariablesSecureApp mockVars;
  @Mock
  private SessionHandler mockSessionHandler;
  @Mock
  private ResponseActionsBuilder mockResponseBuilder;
  @Mock
  private Instance<PaymentProcessHook> mockHooksInstance;
  @Mock
  private FIN_Payment mockPayment;
  @Mock
  private Data mockData;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<RequestContext> requestContextStatic;
  private MockedStatic<OBMessageUtils> obMessageUtilsStatic;
  private MockedStatic<SessionHandler> sessionHandlerStatic;
  private MockedStatic<FIN_AddPayment> finAddPaymentStatic;
  private MockedStatic<FIN_PaymentProcess> finPaymentProcessStatic;
  private MockedStatic<PaymentProcessOrderHook> paymentProcessOrderHookStatic;
  private MockedStatic<FIN_Utility> finUtilityStatic;
  private MockedStatic<OBDao> obDaoStatic;
  private MockedStatic<IsIDFilter> isIDFilterStatic;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    handler = spy(objenesis.newInstance(AddPaymentActionHandler.class));

    obDalStatic = mockStatic(OBDal.class);
    obContextStatic = mockStatic(OBContext.class);
    requestContextStatic = mockStatic(RequestContext.class);
    obMessageUtilsStatic = mockStatic(OBMessageUtils.class);
    sessionHandlerStatic = mockStatic(SessionHandler.class);
    finAddPaymentStatic = mockStatic(FIN_AddPayment.class);
    finPaymentProcessStatic = mockStatic(FIN_PaymentProcess.class);
    paymentProcessOrderHookStatic = mockStatic(PaymentProcessOrderHook.class);
    finUtilityStatic = mockStatic(FIN_Utility.class);
    obDaoStatic = mockStatic(OBDao.class);

    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
    obContextStatic.when(OBContext::getOBContext).thenReturn(mockOBContext);
    requestContextStatic.when(RequestContext::get).thenReturn(mockRequestContext);
    lenient().when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);
    sessionHandlerStatic.when(SessionHandler::getInstance).thenReturn(mockSessionHandler);
    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD(anyString())).thenReturn("test message");

    // Set the hooks field
    setPrivateField(handler, "hooks", mockHooksInstance);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (requestContextStatic != null) requestContextStatic.close();
    if (obMessageUtilsStatic != null) obMessageUtilsStatic.close();
    if (sessionHandlerStatic != null) sessionHandlerStatic.close();
    if (finAddPaymentStatic != null) finAddPaymentStatic.close();
    if (finPaymentProcessStatic != null) finPaymentProcessStatic.close();
    if (paymentProcessOrderHookStatic != null) paymentProcessOrderHookStatic.close();
    if (finUtilityStatic != null) finUtilityStatic.close();
    if (obDaoStatic != null) obDaoStatic.close();
    if (isIDFilterStatic != null) isIDFilterStatic.close();
  }

  // ===== Tests for getInputClass =====
  /** Get input class returns fin payment. */

  @Test
  public void testGetInputClassReturnsFINPayment() {
    assertEquals(FIN_Payment.class, handler.getInputClass());
  }

  // ===== Tests for executeHooks =====
  /**
   * Execute hooks pre process returns null when no errors.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteHooksPreProcessReturnsNullWhenNoErrors() throws Exception {
    List<PaymentProcessHook> hookList = new ArrayList<>();
    PaymentProcessHook mockHook = mock(PaymentProcessHook.class);
    hookList.add(mockHook);
    paymentProcessOrderHookStatic.when(() -> PaymentProcessOrderHook.sortHooksByPriority(any()))
        .thenReturn(hookList);

    when(mockHook.preProcess(any())).thenReturn(null);

    JSONObject params = new JSONObject();
    JSONObject result = handler.executeHooks(params, PRE_PROCESS);

    assertNull(result);
  }
  /**
   * Execute hooks pre process returns result on error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteHooksPreProcessReturnsResultOnError() throws Exception {
    List<PaymentProcessHook> hookList = new ArrayList<>();
    PaymentProcessHook mockHook = mock(PaymentProcessHook.class);
    hookList.add(mockHook);
    paymentProcessOrderHookStatic.when(() -> PaymentProcessOrderHook.sortHooksByPriority(any()))
        .thenReturn(hookList);

    JSONObject hookResult = new JSONObject();
    JSONObject message = new JSONObject();
    message.put(SEVERITY, ERROR);
    hookResult.put(MESSAGE, message);
    when(mockHook.preProcess(any())).thenReturn(hookResult);

    JSONObject params = new JSONObject();
    JSONObject result = handler.executeHooks(params, PRE_PROCESS);

    assertNotNull(result);
    assertTrue(result.has(MESSAGE));
  }
  /**
   * Execute hooks post process returns null when no errors.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteHooksPostProcessReturnsNullWhenNoErrors() throws Exception {
    List<PaymentProcessHook> hookList = new ArrayList<>();
    PaymentProcessHook mockHook = mock(PaymentProcessHook.class);
    hookList.add(mockHook);
    paymentProcessOrderHookStatic.when(() -> PaymentProcessOrderHook.sortHooksByPriority(any()))
        .thenReturn(hookList);

    when(mockHook.posProcess(any())).thenReturn(null);

    JSONObject params = new JSONObject();
    JSONObject result = handler.executeHooks(params, "posProcess");

    assertNull(result);
  }
  /**
   * Execute hooks post process returns result on error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteHooksPostProcessReturnsResultOnError() throws Exception {
    List<PaymentProcessHook> hookList = new ArrayList<>();
    PaymentProcessHook mockHook = mock(PaymentProcessHook.class);
    hookList.add(mockHook);
    paymentProcessOrderHookStatic.when(() -> PaymentProcessOrderHook.sortHooksByPriority(any()))
        .thenReturn(hookList);

    JSONObject hookResult = new JSONObject();
    JSONObject message = new JSONObject();
    message.put(SEVERITY, ERROR);
    hookResult.put(MESSAGE, message);
    when(mockHook.posProcess(any())).thenReturn(hookResult);

    JSONObject params = new JSONObject();
    JSONObject result = handler.executeHooks(params, "posProcess");

    assertNotNull(result);
  }
  /**
   * Execute hooks with empty hook list.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteHooksWithEmptyHookList() throws Exception {
    List<PaymentProcessHook> hookList = new ArrayList<>();
    paymentProcessOrderHookStatic.when(() -> PaymentProcessOrderHook.sortHooksByPriority(any()))
        .thenReturn(hookList);

    JSONObject params = new JSONObject();
    JSONObject result = handler.executeHooks(params, PRE_PROCESS);

    assertNull(result);
  }
  /**
   * Execute hooks with non error severity.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteHooksWithNonErrorSeverity() throws Exception {
    List<PaymentProcessHook> hookList = new ArrayList<>();
    PaymentProcessHook mockHook = mock(PaymentProcessHook.class);
    hookList.add(mockHook);
    paymentProcessOrderHookStatic.when(() -> PaymentProcessOrderHook.sortHooksByPriority(any()))
        .thenReturn(hookList);

    JSONObject hookResult = new JSONObject();
    JSONObject message = new JSONObject();
    message.put(SEVERITY, SUCCESS);
    hookResult.put(MESSAGE, message);
    when(mockHook.preProcess(any())).thenReturn(hookResult);

    JSONObject params = new JSONObject();
    JSONObject result = handler.executeHooks(params, PRE_PROCESS);

    assertNull(result);
  }
  /**
   * Execute hooks with unknown method.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteHooksWithUnknownMethod() throws Exception {
    List<PaymentProcessHook> hookList = new ArrayList<>();
    PaymentProcessHook mockHook = mock(PaymentProcessHook.class);
    hookList.add(mockHook);
    paymentProcessOrderHookStatic.when(() -> PaymentProcessOrderHook.sortHooksByPriority(any()))
        .thenReturn(hookList);

    JSONObject params = new JSONObject();
    JSONObject result = handler.executeHooks(params, "unknownMethod");

    assertNull(result);
  }
  /**
   * Execute hooks multiple hooks first errors.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteHooksMultipleHooksFirstErrors() throws Exception {
    List<PaymentProcessHook> hookList = new ArrayList<>();
    PaymentProcessHook mockHook1 = mock(PaymentProcessHook.class);
    PaymentProcessHook mockHook2 = mock(PaymentProcessHook.class);
    hookList.add(mockHook1);
    hookList.add(mockHook2);
    paymentProcessOrderHookStatic.when(() -> PaymentProcessOrderHook.sortHooksByPriority(any()))
        .thenReturn(hookList);

    JSONObject hookResult = new JSONObject();
    JSONObject message = new JSONObject();
    message.put(SEVERITY, ERROR);
    hookResult.put(MESSAGE, message);
    when(mockHook1.preProcess(any())).thenReturn(hookResult);

    JSONObject params = new JSONObject();
    JSONObject result = handler.executeHooks(params, PRE_PROCESS);

    assertNotNull(result);
    // Second hook should not be called since first returned error
  }

  // ===== Tests for checkID (private) =====
  /**
   * Check id with valid id.
   * @throws IllegalAccessException if an error occurs
   * @throws InvocationTargetException if an error occurs
   * @throws NoSuchMethodException if an error occurs
   */

  @Test
  public void testCheckIDWithValidId() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method checkIDMethod = AddPaymentActionHandler.class.getDeclaredMethod("checkID", String.class);
    checkIDMethod.setAccessible(true);

    // Valid hex ID
    checkIDMethod.invoke(handler, "ABC123DEF456789012345678ABCDEF01");
  }
  /**
   * Check id with invalid id.
   * @throws Exception if an error occurs
   */

  @Test(expected = Exception.class)
  public void testCheckIDWithInvalidId() throws Exception {
    Method checkIDMethod = AddPaymentActionHandler.class.getDeclaredMethod("checkID", String.class);
    checkIDMethod.setAccessible(true);

    // Invalid ID with special characters
    try {
      checkIDMethod.invoke(handler, "<script>alert('xss')</script>");
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw (Exception) e.getCause();
    }
  }

  // ===== Tests for getSelectedCreditLinesIds (private) =====
  /**
   * Get selected credit lines ids single row.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetSelectedCreditLinesIdsSingleRow() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getSelectedCreditLinesIds", JSONArray.class);
    method.setAccessible(true);

    JSONArray selection = new JSONArray();
    JSONObject row = new JSONObject();
    row.put("id", PAY001);
    selection.put(row);

    String result = (String) method.invoke(handler, selection);

    assertEquals("(pay001)", result);
  }
  /**
   * Get selected credit lines ids multiple rows.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetSelectedCreditLinesIdsMultipleRows() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getSelectedCreditLinesIds", JSONArray.class);
    method.setAccessible(true);

    JSONArray selection = new JSONArray();
    JSONObject row1 = new JSONObject();
    row1.put("id", PAY001);
    JSONObject row2 = new JSONObject();
    row2.put("id", "pay002");
    selection.put(row1);
    selection.put(row2);

    String result = (String) method.invoke(handler, selection);

    assertEquals("(pay001,pay002)", result);
  }

  // ===== Tests for getSelectedCreditLinesAndAmount (private) =====
  /**
   * Get selected credit lines and amount.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetSelectedCreditLinesAndAmount() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getSelectedCreditLinesAndAmount", JSONArray.class, List.class);
    method.setAccessible(true);

    FIN_Payment creditPayment = mock(FIN_Payment.class);
    when(creditPayment.getId()).thenReturn(PAY001);

    JSONArray selection = new JSONArray();
    JSONObject row = new JSONObject();
    row.put("id", PAY001);
    row.put("paymentAmount", VAL_100_00);
    selection.put(row);

    List<FIN_Payment> payments = new ArrayList<>();
    payments.add(creditPayment);

    @SuppressWarnings("unchecked")
    HashMap<String, BigDecimal> result = (HashMap<String, BigDecimal>) method.invoke(
        handler, selection, payments);

    assertNotNull(result);
    assertEquals(new BigDecimal(VAL_100_00), result.get(PAY001));
  }
  /**
   * Get selected credit lines and amount no match.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetSelectedCreditLinesAndAmountNoMatch() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getSelectedCreditLinesAndAmount", JSONArray.class, List.class);
    method.setAccessible(true);

    FIN_Payment creditPayment = mock(FIN_Payment.class);
    when(creditPayment.getId()).thenReturn(PAY001);

    JSONArray selection = new JSONArray();
    JSONObject row = new JSONObject();
    row.put("id", "pay999");
    row.put("paymentAmount", VAL_100_00);
    selection.put(row);

    List<FIN_Payment> payments = new ArrayList<>();
    payments.add(creditPayment);

    @SuppressWarnings("unchecked")
    HashMap<String, BigDecimal> result = (HashMap<String, BigDecimal>) method.invoke(
        handler, selection, payments);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  // ===== Tests for isFullyPaid (private) =====
  /**
   * Is fully paid true.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsFullyPaidTrue() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        IS_FULLY_PAID, List.class, BigDecimal.class);
    method.setAccessible(true);

    FIN_PaymentScheduleDetail psd1 = mock(FIN_PaymentScheduleDetail.class);
    FIN_PaymentScheduleDetail psd2 = mock(FIN_PaymentScheduleDetail.class);
    when(psd1.getAmount()).thenReturn(new BigDecimal("60.00"));
    when(psd2.getAmount()).thenReturn(new BigDecimal("40.00"));

    List<FIN_PaymentScheduleDetail> psds = Arrays.asList(psd1, psd2);

    boolean result = (boolean) method.invoke(handler, psds, new BigDecimal(VAL_100_00));

    assertTrue(result);
  }
  /**
   * Is fully paid false.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsFullyPaidFalse() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        IS_FULLY_PAID, List.class, BigDecimal.class);
    method.setAccessible(true);

    FIN_PaymentScheduleDetail psd1 = mock(FIN_PaymentScheduleDetail.class);
    when(psd1.getAmount()).thenReturn(new BigDecimal("60.00"));

    List<FIN_PaymentScheduleDetail> psds = Collections.singletonList(psd1);

    boolean result = (boolean) method.invoke(handler, psds, new BigDecimal(VAL_100_00));

    assertFalse(result);
  }
  /**
   * Is fully paid empty list.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsFullyPaidEmptyList() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        IS_FULLY_PAID, List.class, BigDecimal.class);
    method.setAccessible(true);

    List<FIN_PaymentScheduleDetail> psds = new ArrayList<>();

    boolean result = (boolean) method.invoke(handler, psds, new BigDecimal(VAL_100_00));

    assertFalse(result);
  }

  // ===== Tests for hasNegativeLines (private) =====
  /**
   * Has negative lines true.
   * @throws Exception if an error occurs
   */

  @Test
  public void testHasNegativeLinesTrue() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        HAS_NEGATIVE_LINES, List.class);
    method.setAccessible(true);

    FIN_PaymentScheduleDetail psd1 = mock(FIN_PaymentScheduleDetail.class);
    FIN_PaymentScheduleDetail psd2 = mock(FIN_PaymentScheduleDetail.class);
    when(psd1.getAmount()).thenReturn(new BigDecimal(VAL_100_00));
    when(psd2.getAmount()).thenReturn(new BigDecimal("-50.00"));

    List<FIN_PaymentScheduleDetail> psds = Arrays.asList(psd1, psd2);

    boolean result = (boolean) method.invoke(handler, psds);

    assertTrue(result);
  }
  /**
   * Has negative lines false.
   * @throws Exception if an error occurs
   */

  @Test
  public void testHasNegativeLinesFalse() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        HAS_NEGATIVE_LINES, List.class);
    method.setAccessible(true);

    FIN_PaymentScheduleDetail psd1 = mock(FIN_PaymentScheduleDetail.class);
    when(psd1.getAmount()).thenReturn(new BigDecimal(VAL_100_00));

    List<FIN_PaymentScheduleDetail> psds = Collections.singletonList(psd1);

    boolean result = (boolean) method.invoke(handler, psds);

    assertFalse(result);
  }
  /**
   * Has negative lines empty.
   * @throws Exception if an error occurs
   */

  @Test
  public void testHasNegativeLinesEmpty() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        HAS_NEGATIVE_LINES, List.class);
    method.setAccessible(true);

    List<FIN_PaymentScheduleDetail> psds = new ArrayList<>();

    boolean result = (boolean) method.invoke(handler, psds);

    assertFalse(result);
  }

  // ===== Tests for getAccountDimension (private) =====
  /**
   * Get account dimension with valid dimension.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetAccountDimensionWithValidDimension() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        GET_ACCOUNT_DIMENSION, JSONObject.class, String.class, Class.class);
    method.setAccessible(true);

    BusinessPartner mockBP = mock(BusinessPartner.class);
    String bpId = "0A1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D";

    // Need to set up IsIDFilter to accept the ID
    IsIDFilter mockFilter = mock(IsIDFilter.class);
    when(mockFilter.accept(anyString())).thenReturn(true);

    JSONObject glItem = new JSONObject();
    glItem.put(BUSINESS_PARTNER, bpId);

    lenient().when(mockOBDal.get(BusinessPartner.class, bpId)).thenReturn(mockBP);

    Object result = method.invoke(handler, glItem, BUSINESS_PARTNER, BusinessPartner.class);

    assertEquals(mockBP, result);
  }
  /**
   * Get account dimension with null value.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetAccountDimensionWithNullValue() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        GET_ACCOUNT_DIMENSION, JSONObject.class, String.class, Class.class);
    method.setAccessible(true);

    JSONObject glItem = new JSONObject();
    glItem.put(BUSINESS_PARTNER, JSONObject.NULL);

    Object result = method.invoke(handler, glItem, BUSINESS_PARTNER, BusinessPartner.class);

    assertNull(result);
  }
  /**
   * Get account dimension with missing key.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetAccountDimensionWithMissingKey() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        GET_ACCOUNT_DIMENSION, JSONObject.class, String.class, Class.class);
    method.setAccessible(true);

    JSONObject glItem = new JSONObject();

    Object result = method.invoke(handler, glItem, BUSINESS_PARTNER, BusinessPartner.class);

    assertNull(result);
  }

  // ===== Tests for getDocumentConfirmation (private) =====
  /**
   * Get document confirmation no accounting.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDocumentConfirmationNoAccounting() throws Exception {
    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    FinAccPaymentMethod mockFinAccPM = setupFinAccPaymentMethodCriteria();
    lenient().when(mockFinAccPM.getUponReceiptUse()).thenReturn("INT");

    // Empty accounting list
    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(new ArrayList<>());

    boolean result = invokeGetDocumentConfirmation(finAccount, paymentMethod, true, "100", true);

    assertFalse(result);
  }
  /**
   * Get document confirmation zero amount.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDocumentConfirmationZeroAmount() throws Exception {
    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    FinAccPaymentMethod mockFinAccPM = setupFinAccPaymentMethodCriteria();
    lenient().when(mockFinAccPM.getUponReceiptUse()).thenReturn("INT");

    org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting mockAcct =
        mock(org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting.class);
    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(
        createAcctList(mockAcct));

    // Zero amount payment - should set confirmation true
    boolean result = invokeGetDocumentConfirmation(finAccount, paymentMethod, true, "0", true);

    assertTrue(result);
  }
  /**
   * Get document confirmation exception returns default.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDocumentConfirmationExceptionReturnsDefault() throws Exception {
    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    // Make createCriteria throw to trigger the catch block
    lenient().when(mockOBDal.createCriteria(FinAccPaymentMethod.class))
        .thenThrow(new RuntimeException("DB error"));

    boolean result = invokeGetDocumentConfirmation(finAccount, paymentMethod, true, "100", true);

    assertFalse(result);
  }
  /**
   * Get document confirmation payment out.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDocumentConfirmationPaymentOut() throws Exception {
    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    FinAccPaymentMethod mockFinAccPM = setupFinAccPaymentMethodCriteria();
    // isReceipt = false, isPayment = true => should use getUponPaymentUse
    lenient().when(mockFinAccPM.getUponPaymentUse()).thenReturn("INT");

    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(new ArrayList<>());

    boolean result = invokeGetDocumentConfirmation(finAccount, paymentMethod, false, "100", true);

    assertFalse(result);
  }
  /**
   * Get document confirmation deposit use.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDocumentConfirmationDepositUse() throws Exception {
    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    FinAccPaymentMethod mockFinAccPM = setupFinAccPaymentMethodCriteria();
    // isReceipt = true, isPayment = false => should use getUponDepositUse
    lenient().when(mockFinAccPM.getUponDepositUse()).thenReturn("DEP");

    org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting mockAcct =
        mock(org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting.class);
    lenient().when(mockAcct.getDepositAccount()).thenReturn(mock(AccountingCombination.class));
    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(
        createAcctList(mockAcct));

    boolean result = invokeGetDocumentConfirmation(finAccount, paymentMethod, true, "100", false);

    assertTrue(result);
  }
  /**
   * Get document confirmation withdrawal use.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDocumentConfirmationWithdrawalUse() throws Exception {
    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    FinAccPaymentMethod mockFinAccPM = setupFinAccPaymentMethodCriteria();
    // isReceipt = false, isPayment = false => getUponWithdrawalUse
    lenient().when(mockFinAccPM.getUponWithdrawalUse()).thenReturn("WIT");

    org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting mockAcct =
        mock(org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting.class);
    lenient().when(mockAcct.getWithdrawalAccount()).thenReturn(mock(AccountingCombination.class));
    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(
        createAcctList(mockAcct));

    boolean result = invokeGetDocumentConfirmation(finAccount, paymentMethod, false, "100", false);

    assertTrue(result);
  }

  // ===== Tests for massiveMessageHandler (private) =====
  /**
   * Massive message handler all success.
   * @throws Exception if an error occurs
   */

  @Test
  public void testMassiveMessageHandlerAllSuccess() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        MASSIVE_MESSAGE_HANDLER, ActionResult.class, List.class, int.class, int.class);
    method.setAccessible(true);

    ActionResult result = new ActionResult();
    FIN_Payment p1 = mock(FIN_Payment.class);
    FIN_Payment p2 = mock(FIN_Payment.class);
    List<FIN_Payment> payments = Arrays.asList(p1, p2);

    // Set up input on handler
    setFieldOnSuperclass(handler, INPUT, mockData);

    method.invoke(handler, result, payments, 0, 2);

    assertEquals(Result.Type.SUCCESS, result.getType());
  }
  /**
   * Massive message handler all errors.
   * @throws Exception if an error occurs
   */

  @Test
  public void testMassiveMessageHandlerAllErrors() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        MASSIVE_MESSAGE_HANDLER, ActionResult.class, List.class, int.class, int.class);
    method.setAccessible(true);

    ActionResult result = new ActionResult();
    FIN_Payment p1 = mock(FIN_Payment.class);
    FIN_Payment p2 = mock(FIN_Payment.class);
    List<FIN_Payment> payments = Arrays.asList(p1, p2);

    setFieldOnSuperclass(handler, INPUT, mockData);

    method.invoke(handler, result, payments, 2, 0);

    assertEquals(Result.Type.ERROR, result.getType());
  }
  /**
   * Massive message handler mixed.
   * @throws Exception if an error occurs
   */

  @Test
  public void testMassiveMessageHandlerMixed() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        MASSIVE_MESSAGE_HANDLER, ActionResult.class, List.class, int.class, int.class);
    method.setAccessible(true);

    ActionResult result = new ActionResult();
    FIN_Payment p1 = mock(FIN_Payment.class);
    FIN_Payment p2 = mock(FIN_Payment.class);
    List<FIN_Payment> payments = Arrays.asList(p1, p2);

    setFieldOnSuperclass(handler, INPUT, mockData);

    method.invoke(handler, result, payments, 1, 1);

    assertEquals(Result.Type.WARNING, result.getType());
  }
  /**
   * Massive message handler single record.
   * @throws Exception if an error occurs
   */

  @Test
  public void testMassiveMessageHandlerSingleRecord() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        MASSIVE_MESSAGE_HANDLER, ActionResult.class, List.class, int.class, int.class);
    method.setAccessible(true);

    ActionResult result = new ActionResult();
    result.setType(Result.Type.SUCCESS);
    result.setMessage("original");
    FIN_Payment p1 = mock(FIN_Payment.class);
    List<FIN_Payment> payments = Collections.singletonList(p1);

    method.invoke(handler, result, payments, 0, 1);

    // Single record should not change the message
    assertEquals("original", result.getMessage());
  }

  // ===== Tests for oldProcessPaymentHandler (WebService path) =====
  /**
   * Old process payment handler web service payment already processed.
   * @throws Exception if an error occurs
   */

  @Test
  public void testOldProcessPaymentHandlerWebServicePaymentAlreadyProcessed() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    JSONObject content = new JSONObject();
    content.put(DOCUMENT_ACTION, "PRP");
    String paymentId = PAY001_2;

    FIN_Payment payment = mock(FIN_Payment.class);
    when(payment.isProcessed()).thenReturn(true);
    when(mockOBDal.get(FIN_Payment.class, paymentId)).thenReturn(payment);

    JSONObject result = handler.oldProcessPaymentHandler(parameters, content, paymentId, true);

    assertNotNull(result);
    assertTrue(result.has(MESSAGE));
    JSONObject msg = result.getJSONObject(MESSAGE);
    assertEquals(ERROR, msg.getString(SEVERITY));
    assertTrue(result.getBoolean(RETRY_EXECUTION));
  }
  /**
   * Old process payment handler web service payment not processed.
   * @throws Exception if an error occurs
   */

  @Test
  public void testOldProcessPaymentHandlerWebServicePaymentNotProcessed() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    JSONObject content = new JSONObject();
    content.put(DOCUMENT_ACTION, "PRP");
    String paymentId = PAY001_2;

    FIN_Payment payment = mock(FIN_Payment.class);
    when(payment.isProcessed()).thenReturn(false);
    when(mockOBDal.get(FIN_Payment.class, paymentId)).thenReturn(payment);

    // Mock processMultiPayment - it's static, so we need to mock it differently
    // processMultiPayment calls DalConnectionProvider and RequestContext internally
    // We'll use doReturn on the spy to bypass it
    org.openbravo.erpCommon.utility.OBError mockError = new org.openbravo.erpCommon.utility.OBError();
    mockError.setType(SUCCESS);
    mockError.setMessage("Payment processed");
    mockError.setTitle("Success");

    finAddPaymentStatic.when(() -> FIN_AddPayment.processPayment(
        any(VariablesSecureApp.class), any(), anyString(), any(FIN_Payment.class), any()))
        .thenReturn(mockError);

    JSONObject result = handler.oldProcessPaymentHandler(parameters, content, paymentId, true);

    assertNotNull(result);
    assertTrue(result.has(MESSAGE));
    assertTrue(result.getBoolean(RETRY_EXECUTION));
  }

  // ===== Tests for oldProcessPaymentHandler (non-WebService path with hooks) =====
  /**
   * Old process payment handler pre hook returns error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testOldProcessPaymentHandlerPreHookReturnsError() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("windowId", "someWindowId");

    JSONObject jsonParams = new JSONObject();
    jsonParams.put("issotrx", true);
    jsonParams.put(DOCUMENT_ACTION, "actionId");
    jsonParams.put("c_currency_id", "currId");
    jsonParams.put("received_from", "bpId");
    jsonParams.put("actual_payment", "100");
    jsonParams.put("payment_date", "2024-01-15");
    jsonParams.put("fin_payment_id", PAY001_2);

    JSONObject content = new JSONObject();
    content.put("_params", jsonParams);
    content.put(ENTITY_NAME, FIN_PAYMENT);

    // Setup hook to return an error
    JSONObject hookErrorResult = new JSONObject();
    JSONObject hookMessage = new JSONObject();
    hookMessage.put(SEVERITY, ERROR);
    hookMessage.put("text", "Hook error");
    hookErrorResult.put(MESSAGE, hookMessage);

    List<PaymentProcessHook> hookList = new ArrayList<>();
    PaymentProcessHook mockHook = mock(PaymentProcessHook.class);
    hookList.add(mockHook);
    paymentProcessOrderHookStatic.when(() -> PaymentProcessOrderHook.sortHooksByPriority(any()))
        .thenReturn(hookList);
    when(mockHook.preProcess(any())).thenReturn(hookErrorResult);

    JSONObject result = handler.oldProcessPaymentHandler(parameters, content, PAY001_2, false);

    assertNotNull(result);
    assertTrue(result.has(MESSAGE));
  }

  // ===== Tests for action method (the main entry point) =====
  /**
   * Action with order multiple records returns error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testActionWithOrderMultipleRecordsReturnsError() throws Exception {
    // Set up input data
    JSONObject rawData = new JSONObject();
    rawData.put(ENTITY_NAME, "Order");

    Data data = mock(Data.class);
    when(data.getRawData()).thenReturn(rawData);

    List<FIN_Payment> paymentList = new ArrayList<>();
    paymentList.add(mock(FIN_Payment.class));
    paymentList.add(mock(FIN_Payment.class));
    when(data.getContents(FIN_Payment.class)).thenReturn(paymentList);

    setFieldOnSuperclass(handler, INPUT, data);

    Method actionMethod = AddPaymentActionHandler.class.getDeclaredMethod(
        ACTION, JSONObject.class, MutableBoolean.class);
    actionMethod.setAccessible(true);

    ActionResult result = (ActionResult) actionMethod.invoke(handler, new JSONObject(), new MutableBoolean(false));

    assertEquals(Result.Type.ERROR, result.getType());
  }
  /**
   * Action with invoice multiple records returns error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testActionWithInvoiceMultipleRecordsReturnsError() throws Exception {
    JSONObject rawData = new JSONObject();
    rawData.put(ENTITY_NAME, "Invoice");

    Data data = mock(Data.class);
    when(data.getRawData()).thenReturn(rawData);

    List<FIN_Payment> paymentList = new ArrayList<>();
    paymentList.add(mock(FIN_Payment.class));
    paymentList.add(mock(FIN_Payment.class));
    when(data.getContents(FIN_Payment.class)).thenReturn(paymentList);

    setFieldOnSuperclass(handler, INPUT, data);

    Method actionMethod = AddPaymentActionHandler.class.getDeclaredMethod(
        ACTION, JSONObject.class, MutableBoolean.class);
    actionMethod.setAccessible(true);

    ActionResult result = (ActionResult) actionMethod.invoke(handler, new JSONObject(), new MutableBoolean(false));

    assertEquals(Result.Type.ERROR, result.getType());
  }
  /**
   * Action with payment multiple records not web service returns error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testActionWithPaymentMultipleRecordsNotWebServiceReturnsError() throws Exception {
    JSONObject rawData = new JSONObject();
    rawData.put(ENTITY_NAME, FIN_PAYMENT);

    Data data = mock(Data.class);
    when(data.getRawData()).thenReturn(rawData);

    List<FIN_Payment> paymentList = new ArrayList<>();
    paymentList.add(mock(FIN_Payment.class));
    paymentList.add(mock(FIN_Payment.class));
    when(data.getContents(FIN_Payment.class)).thenReturn(paymentList);

    setFieldOnSuperclass(handler, INPUT, data);

    Method actionMethod = AddPaymentActionHandler.class.getDeclaredMethod(
        ACTION, JSONObject.class, MutableBoolean.class);
    actionMethod.setAccessible(true);

    ActionResult result = (ActionResult) actionMethod.invoke(handler, new JSONObject(), new MutableBoolean(false));

    assertEquals(Result.Type.ERROR, result.getType());
  }
  /**
   * Action with non payment entity calls old process.
   * @throws Exception if an error occurs
   */

  @Test
  public void testActionWithNonPaymentEntityCallsOldProcess() throws Exception {
    JSONObject rawData = new JSONObject();
    rawData.put(ENTITY_NAME, "Invoice");

    JSONObject params = new JSONObject();
    params.put("fin_payment_id", PAY001_2);
    rawData.put("_params", params);

    Data data = mock(Data.class);
    when(data.getRawData()).thenReturn(rawData);

    List<FIN_Payment> paymentList = new ArrayList<>();
    paymentList.add(mock(FIN_Payment.class));
    when(data.getContents(FIN_Payment.class)).thenReturn(paymentList);

    setFieldOnSuperclass(handler, INPUT, data);

    // Mock oldProcessPaymentHandler via spy - returns result with message
    JSONObject processResult = new JSONObject();
    JSONObject msg = new JSONObject();
    msg.put(SEVERITY, SUCCESS);
    msg.put("text", "OK");
    processResult.put(MESSAGE, msg);

    doReturn(processResult).when(handler).oldProcessPaymentHandler(any(), any(), anyString(), anyBoolean());

    // Inject a mockResponseBuilder into the action via reflection on callPaymentProcess
    // callPaymentProcess calls getResponseBuilder which is protected.
    // We set up the action result's responseActionsBuilder before the call so orElse won't be needed.
    // Actually this is internal to callPaymentProcess which creates a new ActionResult.
    // The simplest approach: verify oldProcessPaymentHandler is called.
    Method actionMethod = AddPaymentActionHandler.class.getDeclaredMethod(
        ACTION, JSONObject.class, MutableBoolean.class);
    actionMethod.setAccessible(true);

    try {
      actionMethod.invoke(handler, new JSONObject(), new MutableBoolean(false));
    } catch (java.lang.reflect.InvocationTargetException e) {
      // The call to getResponseBuilder may fail since it's a mock context,
      // but we can verify oldProcessPaymentHandler was called
    }

    verify(handler).oldProcessPaymentHandler(any(), any(), eq(PAY001_2), eq(false));
  }
  /**
   * Action with payment web service document action missing.
   * @throws Exception if an error occurs
   */

  @Test
  public void testActionWithPaymentWebServiceDocumentActionMissing() throws Exception {
    JSONObject rawData = new JSONObject();
    rawData.put(ENTITY_NAME, FIN_PAYMENT);
    rawData.put("processByWebService", true);
    // No document_action

    Data data = mock(Data.class);
    when(data.getRawData()).thenReturn(rawData);

    FIN_Payment mockPay = mock(FIN_Payment.class);
    when(mockPay.getId()).thenReturn(PAY001_2);
    List<FIN_Payment> paymentList = new ArrayList<>();
    paymentList.add(mockPay);
    when(data.getContents(FIN_Payment.class)).thenReturn(paymentList);

    setFieldOnSuperclass(handler, INPUT, data);

    Method actionMethod = AddPaymentActionHandler.class.getDeclaredMethod(
        ACTION, JSONObject.class, MutableBoolean.class);
    actionMethod.setAccessible(true);

    ActionResult result = (ActionResult) actionMethod.invoke(handler, new JSONObject(), new MutableBoolean(false));

    assertEquals(Result.Type.ERROR, result.getType());
  }
  /**
   * Action with payment web service with document action.
   * @throws Exception if an error occurs
   */

  @Test
  public void testActionWithPaymentWebServiceWithDocumentAction() throws Exception {
    JSONObject rawData = new JSONObject();
    rawData.put(ENTITY_NAME, FIN_PAYMENT);
    rawData.put("processByWebService", true);
    rawData.put(DOCUMENT_ACTION, "PRP");

    Data data = mock(Data.class);
    when(data.getRawData()).thenReturn(rawData);

    FIN_Payment mockPay = mock(FIN_Payment.class);
    when(mockPay.getId()).thenReturn(PAY001_2);
    List<FIN_Payment> paymentList = new ArrayList<>();
    paymentList.add(mockPay);
    when(data.getContents(FIN_Payment.class)).thenReturn(paymentList);

    setFieldOnSuperclass(handler, INPUT, data);

    // Mock oldProcessPaymentHandler to return success
    JSONObject processResult = new JSONObject();
    JSONObject msg = new JSONObject();
    msg.put(SEVERITY, SUCCESS);
    msg.put("text", "OK");
    processResult.put(MESSAGE, msg);
    processResult.put(RETRY_EXECUTION, true);

    doReturn(processResult).when(handler).oldProcessPaymentHandler(any(), any(), anyString(), anyBoolean());

    Method actionMethod = AddPaymentActionHandler.class.getDeclaredMethod(
        ACTION, JSONObject.class, MutableBoolean.class);
    actionMethod.setAccessible(true);

    try {
      actionMethod.invoke(handler, new JSONObject(), new MutableBoolean(false));
    } catch (java.lang.reflect.InvocationTargetException e) {
      // callPaymentProcess may fail on getResponseBuilder, that's OK
    }

    verify(handler).oldProcessPaymentHandler(any(), any(), eq(PAY001_2), eq(true));
  }
  /**
   * Action exception rolls back and returns error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testActionExceptionRollsBackAndReturnsError() throws Exception {
    JSONObject rawData = new JSONObject();
    rawData.put(ENTITY_NAME, FIN_PAYMENT);

    Data data = mock(Data.class);
    when(data.getRawData()).thenReturn(rawData);
    when(data.getContents(FIN_Payment.class)).thenThrow(new RuntimeException("test exception"));

    setFieldOnSuperclass(handler, INPUT, data);

    Method actionMethod = AddPaymentActionHandler.class.getDeclaredMethod(
        ACTION, JSONObject.class, MutableBoolean.class);
    actionMethod.setAccessible(true);

    ActionResult result = (ActionResult) actionMethod.invoke(handler, new JSONObject(), new MutableBoolean(false));

    assertEquals(Result.Type.ERROR, result.getType());
    assertEquals("test exception", result.getMessage());
  }

  // ===== Tests for removeNotSelectedPaymentDetails (private) =====
  /**
   * Remove not selected payment details empty list.
   * @throws Exception if an error occurs
   */

  @Test
  public void testRemoveNotSelectedPaymentDetailsEmptyList() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        REMOVE_NOT_SELECTED_PAYMENT_DETAILS, FIN_Payment.class, List.class);
    method.setAccessible(true);

    FIN_Payment payment = mock(FIN_Payment.class);
    List<String> pdToRemove = new ArrayList<>();

    // Should complete without error
    method.invoke(handler, payment, pdToRemove);
  }
  /**
   * Remove not selected payment details with gl item.
   * @throws Exception if an error occurs
   */

  @Test
  public void testRemoveNotSelectedPaymentDetailsWithGLItem() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        REMOVE_NOT_SELECTED_PAYMENT_DETAILS, FIN_Payment.class, List.class);
    method.setAccessible(true);

    FIN_Payment payment = mock(FIN_Payment.class);
    FIN_PaymentDetail pd = mock(FIN_PaymentDetail.class);
    FIN_PaymentScheduleDetail psd = mock(FIN_PaymentScheduleDetail.class);
    org.openbravo.model.financialmgmt.gl.GLItem glItem =
        mock(org.openbravo.model.financialmgmt.gl.GLItem.class);

    when(mockOBDal.get(FIN_PaymentDetail.class, PD001)).thenReturn(pd);
    when(pd.getGLItem()).thenReturn(glItem);

    List<FIN_PaymentScheduleDetail> psdList = new ArrayList<>();
    psdList.add(psd);
    when(pd.getFINPaymentScheduleDetailList()).thenReturn(psdList);

    obDaoStatic.when(() -> OBDao.getIDListFromOBObject(any())).thenReturn(
        new ArrayList<>(Collections.singletonList(PSD001)));
    when(mockOBDal.get(FIN_PaymentScheduleDetail.class, PSD001)).thenReturn(psd);

    List<FIN_PaymentDetail> paymentDetailList = new ArrayList<>();
    paymentDetailList.add(pd);
    when(payment.getFINPaymentDetailList()).thenReturn(paymentDetailList);

    List<String> pdToRemove = new ArrayList<>();
    pdToRemove.add(PD001);

    method.invoke(handler, payment, pdToRemove);

    verify(mockOBDal).remove(psd);
    verify(mockOBDal).remove(pd);
  }
  /**
   * Remove not selected payment details without gl item and with outstanding.
   * @throws Exception if an error occurs
   */

  @Test
  public void testRemoveNotSelectedPaymentDetailsWithoutGLItemAndWithOutstanding() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        REMOVE_NOT_SELECTED_PAYMENT_DETAILS, FIN_Payment.class, List.class);
    method.setAccessible(true);

    FIN_Payment payment = mock(FIN_Payment.class);
    FIN_PaymentDetail pd = mock(FIN_PaymentDetail.class);
    FIN_PaymentScheduleDetail psd = mock(FIN_PaymentScheduleDetail.class);
    FIN_PaymentScheduleDetail outstandingPsd = mock(FIN_PaymentScheduleDetail.class);

    when(mockOBDal.get(FIN_PaymentDetail.class, PD001)).thenReturn(pd);
    when(pd.getGLItem()).thenReturn(null);
    when(psd.getAmount()).thenReturn(new BigDecimal(VAL_100_00));
    when(psd.getWriteoffAmount()).thenReturn(BigDecimal.ZERO);
    when(psd.getDoubtfulDebtAmount()).thenReturn(BigDecimal.ZERO);

    List<FIN_PaymentScheduleDetail> psdList = new ArrayList<>();
    psdList.add(psd);
    when(pd.getFINPaymentScheduleDetailList()).thenReturn(psdList);

    obDaoStatic.when(() -> OBDao.getIDListFromOBObject(any())).thenReturn(
        new ArrayList<>(Collections.singletonList(PSD001)));
    when(mockOBDal.get(FIN_PaymentScheduleDetail.class, PSD001)).thenReturn(psd);

    // getOutstandingPSDs returns one existing outstanding PSD
    List<FIN_PaymentScheduleDetail> outstandingList = new ArrayList<>();
    outstandingList.add(outstandingPsd);
    finAddPaymentStatic.when(() -> FIN_AddPayment.getOutstandingPSDs(any()))
        .thenReturn(outstandingList);

    // outstandingPSD.getAmount() + psd.getAmount() + psd.getWriteoffAmount() != 0
    when(outstandingPsd.getAmount()).thenReturn(new BigDecimal("50.00"));
    when(outstandingPsd.getDoubtfulDebtAmount()).thenReturn(BigDecimal.ZERO);

    List<FIN_PaymentDetail> paymentDetailList = new ArrayList<>();
    paymentDetailList.add(pd);
    when(payment.getFINPaymentDetailList()).thenReturn(paymentDetailList);

    List<String> pdToRemove = new ArrayList<>();
    pdToRemove.add(PD001);

    method.invoke(handler, payment, pdToRemove);

    // Should update existing outstanding PSD
    verify(outstandingPsd).setAmount(new BigDecimal("150.00"));
    verify(mockOBDal).save(outstandingPsd);
    verify(mockOBDal).remove(psd);
    verify(mockOBDal).remove(pd);
  }

  // ===== Tests for CLE accounting path in getDocumentConfirmation =====
  /**
   * Get document confirmation cle receipt path.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDocumentConfirmationCLEReceiptPath() throws Exception {
    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    FinAccPaymentMethod mockFinAccPM = setupFinAccPaymentMethodCriteria();
    lenient().when(mockFinAccPM.getUponReceiptUse()).thenReturn("CLE");

    org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting mockAcct =
        mock(org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting.class);
    lenient().when(mockAcct.getClearedPaymentAccount()).thenReturn(mock(AccountingCombination.class));
    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(
        createAcctList(mockAcct));

    boolean result = invokeGetDocumentConfirmation(finAccount, paymentMethod, true, "100", true);

    assertTrue(result);
  }
  /**
   * Get document confirmation cle payment out path.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDocumentConfirmationCLEPaymentOutPath() throws Exception {
    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    FinAccPaymentMethod mockFinAccPM = setupFinAccPaymentMethodCriteria();
    lenient().when(mockFinAccPM.getUponPaymentUse()).thenReturn("CLE");

    org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting mockAcct =
        mock(org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting.class);
    lenient().when(mockAcct.getClearedPaymentAccountOUT()).thenReturn(mock(AccountingCombination.class));
    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(
        createAcctList(mockAcct));

    boolean result = invokeGetDocumentConfirmation(finAccount, paymentMethod, false, "100", true);

    assertTrue(result);
  }

  // ===== Tests for INT path in payment out for getDocumentConfirmation =====
  /**
   * Get document confirmation int payment out path.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDocumentConfirmationINTPaymentOutPath() throws Exception {
    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    FinAccPaymentMethod mockFinAccPM = setupFinAccPaymentMethodCriteria();
    lenient().when(mockFinAccPM.getUponPaymentUse()).thenReturn("INT");

    org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting mockAcct =
        mock(org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting.class);
    lenient().when(mockAcct.getFINOutIntransitAcct()).thenReturn(mock(AccountingCombination.class));
    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(
        createAcctList(mockAcct));

    boolean result = invokeGetDocumentConfirmation(finAccount, paymentMethod, false, "100", true);

    assertTrue(result);
  }

  // ===== Tests for INT receipt path in getDocumentConfirmation =====
  /**
   * Get document confirmation int receipt path.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDocumentConfirmationINTReceiptPath() throws Exception {
    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    FinAccPaymentMethod mockFinAccPM = setupFinAccPaymentMethodCriteria();
    lenient().when(mockFinAccPM.getUponReceiptUse()).thenReturn("INT");

    org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting mockAcct =
        mock(org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting.class);
    lenient().when(mockAcct.getInTransitPaymentAccountIN()).thenReturn(mock(AccountingCombination.class));
    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(
        createAcctList(mockAcct));

    boolean result = invokeGetDocumentConfirmation(finAccount, paymentMethod, true, "100", true);

    assertTrue(result);
  }

  // ===== Tests for DEP receipt path =====
  /**
   * Get document confirmation dep receipt path.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDocumentConfirmationDEPReceiptPath() throws Exception {
    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    FinAccPaymentMethod mockFinAccPM = setupFinAccPaymentMethodCriteria();
    lenient().when(mockFinAccPM.getUponReceiptUse()).thenReturn("DEP");

    org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting mockAcct =
        mock(org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting.class);
    lenient().when(mockAcct.getDepositAccount()).thenReturn(mock(AccountingCombination.class));
    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(
        createAcctList(mockAcct));

    boolean result = invokeGetDocumentConfirmation(finAccount, paymentMethod, true, "100", true);

    assertTrue(result);
  }

  // ===== Helper Methods =====

  @SuppressWarnings("unchecked")
  private FinAccPaymentMethod setupFinAccPaymentMethodCriteria() {
    OBCriteria<FinAccPaymentMethod> mockCriteria = mock(OBCriteria.class);
    lenient().when(mockOBDal.createCriteria(FinAccPaymentMethod.class)).thenReturn(mockCriteria);
    lenient().when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);

    FinAccPaymentMethod mockFinAccPM = mock(FinAccPaymentMethod.class);
    lenient().when(mockCriteria.uniqueResult()).thenReturn(mockFinAccPM);
    return mockFinAccPM;
  }

  private List<org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting> createAcctList(
      org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting... accts) {
    List<org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting> acctList =
        new ArrayList<>();
    for (org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting acct : accts) {
      acctList.add(acct);
    }
    return acctList;
  }

  private boolean invokeGetDocumentConfirmation(FIN_FinancialAccount finAccount,
      FIN_PaymentMethod paymentMethod, boolean isReceipt, String amount,
      boolean isPayment) throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        GET_DOCUMENT_CONFIRMATION, FIN_FinancialAccount.class,
        FIN_PaymentMethod.class, boolean.class, String.class, boolean.class);
    method.setAccessible(true);
    return (boolean) method.invoke(handler, finAccount, paymentMethod, isReceipt, amount, isPayment);
  }

  private void setPrivateField(Object target, String fieldName, Object value) throws IllegalAccessException, NoSuchFieldException {
    Field field = findField(target.getClass(), fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private void setFieldOnSuperclass(Object target, String fieldName, Object value) throws IllegalAccessException, NoSuchFieldException {
    Field field = findField(target.getClass(), fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    Class<?> current = clazz;
    while (current != null) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new NoSuchFieldException(fieldName);
  }
}
