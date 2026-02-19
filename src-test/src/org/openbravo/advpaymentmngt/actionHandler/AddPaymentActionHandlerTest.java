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
@RunWith(MockitoJUnitRunner.Silent.class)
public class AddPaymentActionHandlerTest {

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

  @Test
  public void testGetInputClassReturnsFINPayment() {
    assertEquals(FIN_Payment.class, handler.getInputClass());
  }

  // ===== Tests for executeHooks =====

  @Test
  public void testExecuteHooksPreProcessReturnsNullWhenNoErrors() throws Exception {
    List<PaymentProcessHook> hookList = new ArrayList<>();
    PaymentProcessHook mockHook = mock(PaymentProcessHook.class);
    hookList.add(mockHook);
    paymentProcessOrderHookStatic.when(() -> PaymentProcessOrderHook.sortHooksByPriority(any()))
        .thenReturn(hookList);

    when(mockHook.preProcess(any())).thenReturn(null);

    JSONObject params = new JSONObject();
    JSONObject result = handler.executeHooks(params, "preProcess");

    assertNull(result);
  }

  @Test
  public void testExecuteHooksPreProcessReturnsResultOnError() throws Exception {
    List<PaymentProcessHook> hookList = new ArrayList<>();
    PaymentProcessHook mockHook = mock(PaymentProcessHook.class);
    hookList.add(mockHook);
    paymentProcessOrderHookStatic.when(() -> PaymentProcessOrderHook.sortHooksByPriority(any()))
        .thenReturn(hookList);

    JSONObject hookResult = new JSONObject();
    JSONObject message = new JSONObject();
    message.put("severity", "error");
    hookResult.put("message", message);
    when(mockHook.preProcess(any())).thenReturn(hookResult);

    JSONObject params = new JSONObject();
    JSONObject result = handler.executeHooks(params, "preProcess");

    assertNotNull(result);
    assertTrue(result.has("message"));
  }

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

  @Test
  public void testExecuteHooksPostProcessReturnsResultOnError() throws Exception {
    List<PaymentProcessHook> hookList = new ArrayList<>();
    PaymentProcessHook mockHook = mock(PaymentProcessHook.class);
    hookList.add(mockHook);
    paymentProcessOrderHookStatic.when(() -> PaymentProcessOrderHook.sortHooksByPriority(any()))
        .thenReturn(hookList);

    JSONObject hookResult = new JSONObject();
    JSONObject message = new JSONObject();
    message.put("severity", "error");
    hookResult.put("message", message);
    when(mockHook.posProcess(any())).thenReturn(hookResult);

    JSONObject params = new JSONObject();
    JSONObject result = handler.executeHooks(params, "posProcess");

    assertNotNull(result);
  }

  @Test
  public void testExecuteHooksWithEmptyHookList() throws Exception {
    List<PaymentProcessHook> hookList = new ArrayList<>();
    paymentProcessOrderHookStatic.when(() -> PaymentProcessOrderHook.sortHooksByPriority(any()))
        .thenReturn(hookList);

    JSONObject params = new JSONObject();
    JSONObject result = handler.executeHooks(params, "preProcess");

    assertNull(result);
  }

  @Test
  public void testExecuteHooksWithNonErrorSeverity() throws Exception {
    List<PaymentProcessHook> hookList = new ArrayList<>();
    PaymentProcessHook mockHook = mock(PaymentProcessHook.class);
    hookList.add(mockHook);
    paymentProcessOrderHookStatic.when(() -> PaymentProcessOrderHook.sortHooksByPriority(any()))
        .thenReturn(hookList);

    JSONObject hookResult = new JSONObject();
    JSONObject message = new JSONObject();
    message.put("severity", "success");
    hookResult.put("message", message);
    when(mockHook.preProcess(any())).thenReturn(hookResult);

    JSONObject params = new JSONObject();
    JSONObject result = handler.executeHooks(params, "preProcess");

    assertNull(result);
  }

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
    message.put("severity", "error");
    hookResult.put("message", message);
    when(mockHook1.preProcess(any())).thenReturn(hookResult);

    JSONObject params = new JSONObject();
    JSONObject result = handler.executeHooks(params, "preProcess");

    assertNotNull(result);
    // Second hook should not be called since first returned error
  }

  // ===== Tests for checkID (private) =====

  @Test
  public void testCheckIDWithValidId() throws Exception {
    Method checkIDMethod = AddPaymentActionHandler.class.getDeclaredMethod("checkID", String.class);
    checkIDMethod.setAccessible(true);

    // Valid hex ID
    checkIDMethod.invoke(handler, "ABC123DEF456789012345678ABCDEF01");
  }

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

  @Test
  public void testGetSelectedCreditLinesIdsSingleRow() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getSelectedCreditLinesIds", JSONArray.class);
    method.setAccessible(true);

    JSONArray selection = new JSONArray();
    JSONObject row = new JSONObject();
    row.put("id", "pay001");
    selection.put(row);

    String result = (String) method.invoke(handler, selection);

    assertEquals("(pay001)", result);
  }

  @Test
  public void testGetSelectedCreditLinesIdsMultipleRows() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getSelectedCreditLinesIds", JSONArray.class);
    method.setAccessible(true);

    JSONArray selection = new JSONArray();
    JSONObject row1 = new JSONObject();
    row1.put("id", "pay001");
    JSONObject row2 = new JSONObject();
    row2.put("id", "pay002");
    selection.put(row1);
    selection.put(row2);

    String result = (String) method.invoke(handler, selection);

    assertEquals("(pay001,pay002)", result);
  }

  // ===== Tests for getSelectedCreditLinesAndAmount (private) =====

  @Test
  public void testGetSelectedCreditLinesAndAmount() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getSelectedCreditLinesAndAmount", JSONArray.class, List.class);
    method.setAccessible(true);

    FIN_Payment creditPayment = mock(FIN_Payment.class);
    when(creditPayment.getId()).thenReturn("pay001");

    JSONArray selection = new JSONArray();
    JSONObject row = new JSONObject();
    row.put("id", "pay001");
    row.put("paymentAmount", "100.00");
    selection.put(row);

    List<FIN_Payment> payments = new ArrayList<>();
    payments.add(creditPayment);

    @SuppressWarnings("unchecked")
    HashMap<String, BigDecimal> result = (HashMap<String, BigDecimal>) method.invoke(
        handler, selection, payments);

    assertNotNull(result);
    assertEquals(new BigDecimal("100.00"), result.get("pay001"));
  }

  @Test
  public void testGetSelectedCreditLinesAndAmountNoMatch() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getSelectedCreditLinesAndAmount", JSONArray.class, List.class);
    method.setAccessible(true);

    FIN_Payment creditPayment = mock(FIN_Payment.class);
    when(creditPayment.getId()).thenReturn("pay001");

    JSONArray selection = new JSONArray();
    JSONObject row = new JSONObject();
    row.put("id", "pay999");
    row.put("paymentAmount", "100.00");
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

  @Test
  public void testIsFullyPaidTrue() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "isFullyPaid", List.class, BigDecimal.class);
    method.setAccessible(true);

    FIN_PaymentScheduleDetail psd1 = mock(FIN_PaymentScheduleDetail.class);
    FIN_PaymentScheduleDetail psd2 = mock(FIN_PaymentScheduleDetail.class);
    when(psd1.getAmount()).thenReturn(new BigDecimal("60.00"));
    when(psd2.getAmount()).thenReturn(new BigDecimal("40.00"));

    List<FIN_PaymentScheduleDetail> psds = Arrays.asList(psd1, psd2);

    boolean result = (boolean) method.invoke(handler, psds, new BigDecimal("100.00"));

    assertTrue(result);
  }

  @Test
  public void testIsFullyPaidFalse() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "isFullyPaid", List.class, BigDecimal.class);
    method.setAccessible(true);

    FIN_PaymentScheduleDetail psd1 = mock(FIN_PaymentScheduleDetail.class);
    when(psd1.getAmount()).thenReturn(new BigDecimal("60.00"));

    List<FIN_PaymentScheduleDetail> psds = Collections.singletonList(psd1);

    boolean result = (boolean) method.invoke(handler, psds, new BigDecimal("100.00"));

    assertFalse(result);
  }

  @Test
  public void testIsFullyPaidEmptyList() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "isFullyPaid", List.class, BigDecimal.class);
    method.setAccessible(true);

    List<FIN_PaymentScheduleDetail> psds = new ArrayList<>();

    boolean result = (boolean) method.invoke(handler, psds, new BigDecimal("100.00"));

    assertFalse(result);
  }

  // ===== Tests for hasNegativeLines (private) =====

  @Test
  public void testHasNegativeLinesTrue() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "hasNegativeLines", List.class);
    method.setAccessible(true);

    FIN_PaymentScheduleDetail psd1 = mock(FIN_PaymentScheduleDetail.class);
    FIN_PaymentScheduleDetail psd2 = mock(FIN_PaymentScheduleDetail.class);
    when(psd1.getAmount()).thenReturn(new BigDecimal("100.00"));
    when(psd2.getAmount()).thenReturn(new BigDecimal("-50.00"));

    List<FIN_PaymentScheduleDetail> psds = Arrays.asList(psd1, psd2);

    boolean result = (boolean) method.invoke(handler, psds);

    assertTrue(result);
  }

  @Test
  public void testHasNegativeLinesFalse() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "hasNegativeLines", List.class);
    method.setAccessible(true);

    FIN_PaymentScheduleDetail psd1 = mock(FIN_PaymentScheduleDetail.class);
    when(psd1.getAmount()).thenReturn(new BigDecimal("100.00"));

    List<FIN_PaymentScheduleDetail> psds = Collections.singletonList(psd1);

    boolean result = (boolean) method.invoke(handler, psds);

    assertFalse(result);
  }

  @Test
  public void testHasNegativeLinesEmpty() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "hasNegativeLines", List.class);
    method.setAccessible(true);

    List<FIN_PaymentScheduleDetail> psds = new ArrayList<>();

    boolean result = (boolean) method.invoke(handler, psds);

    assertFalse(result);
  }

  // ===== Tests for getAccountDimension (private) =====

  @Test
  public void testGetAccountDimensionWithValidDimension() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getAccountDimension", JSONObject.class, String.class, Class.class);
    method.setAccessible(true);

    BusinessPartner mockBP = mock(BusinessPartner.class);
    String bpId = "0A1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D";

    // Need to set up IsIDFilter to accept the ID
    IsIDFilter mockFilter = mock(IsIDFilter.class);
    when(mockFilter.accept(anyString())).thenReturn(true);

    JSONObject glItem = new JSONObject();
    glItem.put("businessPartner", bpId);

    lenient().when(mockOBDal.get(BusinessPartner.class, bpId)).thenReturn(mockBP);

    Object result = method.invoke(handler, glItem, "businessPartner", BusinessPartner.class);

    assertEquals(mockBP, result);
  }

  @Test
  public void testGetAccountDimensionWithNullValue() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getAccountDimension", JSONObject.class, String.class, Class.class);
    method.setAccessible(true);

    JSONObject glItem = new JSONObject();
    glItem.put("businessPartner", JSONObject.NULL);

    Object result = method.invoke(handler, glItem, "businessPartner", BusinessPartner.class);

    assertNull(result);
  }

  @Test
  public void testGetAccountDimensionWithMissingKey() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getAccountDimension", JSONObject.class, String.class, Class.class);
    method.setAccessible(true);

    JSONObject glItem = new JSONObject();

    Object result = method.invoke(handler, glItem, "businessPartner", BusinessPartner.class);

    assertNull(result);
  }

  // ===== Tests for getDocumentConfirmation (private) =====

  @Test
  public void testGetDocumentConfirmationNoAccounting() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getDocumentConfirmation", FIN_FinancialAccount.class,
        FIN_PaymentMethod.class, boolean.class, String.class, boolean.class);
    method.setAccessible(true);

    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    OBCriteria<FinAccPaymentMethod> mockCriteria = mock(OBCriteria.class);
    lenient().when(mockOBDal.createCriteria(FinAccPaymentMethod.class)).thenReturn(mockCriteria);
    lenient().when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);

    FinAccPaymentMethod mockFinAccPM = mock(FinAccPaymentMethod.class);
    lenient().when(mockCriteria.uniqueResult()).thenReturn(mockFinAccPM);
    lenient().when(mockFinAccPM.getUponReceiptUse()).thenReturn("INT");

    // Empty accounting list
    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(new ArrayList<>());

    boolean result = (boolean) method.invoke(handler, finAccount, paymentMethod, true, "100", true);

    assertFalse(result);
  }

  @Test
  public void testGetDocumentConfirmationZeroAmount() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getDocumentConfirmation", FIN_FinancialAccount.class,
        FIN_PaymentMethod.class, boolean.class, String.class, boolean.class);
    method.setAccessible(true);

    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    OBCriteria<FinAccPaymentMethod> mockCriteria = mock(OBCriteria.class);
    lenient().when(mockOBDal.createCriteria(FinAccPaymentMethod.class)).thenReturn(mockCriteria);
    lenient().when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);

    FinAccPaymentMethod mockFinAccPM = mock(FinAccPaymentMethod.class);
    lenient().when(mockCriteria.uniqueResult()).thenReturn(mockFinAccPM);
    lenient().when(mockFinAccPM.getUponReceiptUse()).thenReturn("INT");

    org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting mockAcct =
        mock(org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting.class);
    List<org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting> acctList =
        new ArrayList<>();
    acctList.add(mockAcct);
    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(acctList);

    // Zero amount payment - should set confirmation true
    boolean result = (boolean) method.invoke(handler, finAccount, paymentMethod, true, "0", true);

    assertTrue(result);
  }

  @Test
  public void testGetDocumentConfirmationExceptionReturnsDefault() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getDocumentConfirmation", FIN_FinancialAccount.class,
        FIN_PaymentMethod.class, boolean.class, String.class, boolean.class);
    method.setAccessible(true);

    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    // Make createCriteria throw to trigger the catch block
    lenient().when(mockOBDal.createCriteria(FinAccPaymentMethod.class))
        .thenThrow(new RuntimeException("DB error"));

    boolean result = (boolean) method.invoke(handler, finAccount, paymentMethod, true, "100", true);

    assertFalse(result);
  }

  @Test
  public void testGetDocumentConfirmationPaymentOut() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getDocumentConfirmation", FIN_FinancialAccount.class,
        FIN_PaymentMethod.class, boolean.class, String.class, boolean.class);
    method.setAccessible(true);

    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    OBCriteria<FinAccPaymentMethod> mockCriteria = mock(OBCriteria.class);
    lenient().when(mockOBDal.createCriteria(FinAccPaymentMethod.class)).thenReturn(mockCriteria);
    lenient().when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);

    FinAccPaymentMethod mockFinAccPM = mock(FinAccPaymentMethod.class);
    lenient().when(mockCriteria.uniqueResult()).thenReturn(mockFinAccPM);
    // isReceipt = false, isPayment = true => should use getUponPaymentUse
    lenient().when(mockFinAccPM.getUponPaymentUse()).thenReturn("INT");

    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(new ArrayList<>());

    boolean result = (boolean) method.invoke(handler, finAccount, paymentMethod, false, "100", true);

    assertFalse(result);
  }

  @Test
  public void testGetDocumentConfirmationDepositUse() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getDocumentConfirmation", FIN_FinancialAccount.class,
        FIN_PaymentMethod.class, boolean.class, String.class, boolean.class);
    method.setAccessible(true);

    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    OBCriteria<FinAccPaymentMethod> mockCriteria = mock(OBCriteria.class);
    lenient().when(mockOBDal.createCriteria(FinAccPaymentMethod.class)).thenReturn(mockCriteria);
    lenient().when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);

    FinAccPaymentMethod mockFinAccPM = mock(FinAccPaymentMethod.class);
    lenient().when(mockCriteria.uniqueResult()).thenReturn(mockFinAccPM);
    // isReceipt = true, isPayment = false => should use getUponDepositUse
    lenient().when(mockFinAccPM.getUponDepositUse()).thenReturn("DEP");

    org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting mockAcct =
        mock(org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting.class);
    lenient().when(mockAcct.getDepositAccount()).thenReturn(mock(AccountingCombination.class));
    List<org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting> acctList =
        new ArrayList<>();
    acctList.add(mockAcct);
    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(acctList);

    boolean result = (boolean) method.invoke(handler, finAccount, paymentMethod, true, "100", false);

    assertTrue(result);
  }

  @Test
  public void testGetDocumentConfirmationWithdrawalUse() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getDocumentConfirmation", FIN_FinancialAccount.class,
        FIN_PaymentMethod.class, boolean.class, String.class, boolean.class);
    method.setAccessible(true);

    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    OBCriteria<FinAccPaymentMethod> mockCriteria = mock(OBCriteria.class);
    lenient().when(mockOBDal.createCriteria(FinAccPaymentMethod.class)).thenReturn(mockCriteria);
    lenient().when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);

    FinAccPaymentMethod mockFinAccPM = mock(FinAccPaymentMethod.class);
    lenient().when(mockCriteria.uniqueResult()).thenReturn(mockFinAccPM);
    // isReceipt = false, isPayment = false => getUponWithdrawalUse
    lenient().when(mockFinAccPM.getUponWithdrawalUse()).thenReturn("WIT");

    org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting mockAcct =
        mock(org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting.class);
    lenient().when(mockAcct.getWithdrawalAccount()).thenReturn(mock(AccountingCombination.class));
    List<org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting> acctList =
        new ArrayList<>();
    acctList.add(mockAcct);
    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(acctList);

    boolean result = (boolean) method.invoke(handler, finAccount, paymentMethod, false, "100", false);

    assertTrue(result);
  }

  // ===== Tests for massiveMessageHandler (private) =====

  @Test
  public void testMassiveMessageHandlerAllSuccess() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "massiveMessageHandler", ActionResult.class, List.class, int.class, int.class);
    method.setAccessible(true);

    ActionResult result = new ActionResult();
    FIN_Payment p1 = mock(FIN_Payment.class);
    FIN_Payment p2 = mock(FIN_Payment.class);
    List<FIN_Payment> payments = Arrays.asList(p1, p2);

    // Set up input on handler
    setFieldOnSuperclass(handler, "input", mockData);

    method.invoke(handler, result, payments, 0, 2);

    assertEquals(Result.Type.SUCCESS, result.getType());
  }

  @Test
  public void testMassiveMessageHandlerAllErrors() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "massiveMessageHandler", ActionResult.class, List.class, int.class, int.class);
    method.setAccessible(true);

    ActionResult result = new ActionResult();
    FIN_Payment p1 = mock(FIN_Payment.class);
    FIN_Payment p2 = mock(FIN_Payment.class);
    List<FIN_Payment> payments = Arrays.asList(p1, p2);

    setFieldOnSuperclass(handler, "input", mockData);

    method.invoke(handler, result, payments, 2, 0);

    assertEquals(Result.Type.ERROR, result.getType());
  }

  @Test
  public void testMassiveMessageHandlerMixed() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "massiveMessageHandler", ActionResult.class, List.class, int.class, int.class);
    method.setAccessible(true);

    ActionResult result = new ActionResult();
    FIN_Payment p1 = mock(FIN_Payment.class);
    FIN_Payment p2 = mock(FIN_Payment.class);
    List<FIN_Payment> payments = Arrays.asList(p1, p2);

    setFieldOnSuperclass(handler, "input", mockData);

    method.invoke(handler, result, payments, 1, 1);

    assertEquals(Result.Type.WARNING, result.getType());
  }

  @Test
  public void testMassiveMessageHandlerSingleRecord() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "massiveMessageHandler", ActionResult.class, List.class, int.class, int.class);
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

  @Test
  public void testOldProcessPaymentHandlerWebServicePaymentAlreadyProcessed() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    JSONObject content = new JSONObject();
    content.put("document_action", "PRP");
    String paymentId = "PAY001";

    FIN_Payment payment = mock(FIN_Payment.class);
    when(payment.isProcessed()).thenReturn(true);
    when(mockOBDal.get(FIN_Payment.class, paymentId)).thenReturn(payment);

    JSONObject result = handler.oldProcessPaymentHandler(parameters, content, paymentId, true);

    assertNotNull(result);
    assertTrue(result.has("message"));
    JSONObject msg = result.getJSONObject("message");
    assertEquals("error", msg.getString("severity"));
    assertTrue(result.getBoolean("retryExecution"));
  }

  @Test
  public void testOldProcessPaymentHandlerWebServicePaymentNotProcessed() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    JSONObject content = new JSONObject();
    content.put("document_action", "PRP");
    String paymentId = "PAY001";

    FIN_Payment payment = mock(FIN_Payment.class);
    when(payment.isProcessed()).thenReturn(false);
    when(mockOBDal.get(FIN_Payment.class, paymentId)).thenReturn(payment);

    // Mock processMultiPayment - it's static, so we need to mock it differently
    // processMultiPayment calls DalConnectionProvider and RequestContext internally
    // We'll use doReturn on the spy to bypass it
    org.openbravo.erpCommon.utility.OBError mockError = new org.openbravo.erpCommon.utility.OBError();
    mockError.setType("success");
    mockError.setMessage("Payment processed");
    mockError.setTitle("Success");

    finAddPaymentStatic.when(() -> FIN_AddPayment.processPayment(
        any(VariablesSecureApp.class), any(), anyString(), any(FIN_Payment.class), any()))
        .thenReturn(mockError);

    JSONObject result = handler.oldProcessPaymentHandler(parameters, content, paymentId, true);

    assertNotNull(result);
    assertTrue(result.has("message"));
    assertTrue(result.getBoolean("retryExecution"));
  }

  // ===== Tests for oldProcessPaymentHandler (non-WebService path with hooks) =====

  @Test
  public void testOldProcessPaymentHandlerPreHookReturnsError() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("windowId", "someWindowId");

    JSONObject jsonParams = new JSONObject();
    jsonParams.put("issotrx", true);
    jsonParams.put("document_action", "actionId");
    jsonParams.put("c_currency_id", "currId");
    jsonParams.put("received_from", "bpId");
    jsonParams.put("actual_payment", "100");
    jsonParams.put("payment_date", "2024-01-15");
    jsonParams.put("fin_payment_id", "PAY001");

    JSONObject content = new JSONObject();
    content.put("_params", jsonParams);
    content.put("_entityName", "FIN_Payment");

    // Setup hook to return an error
    JSONObject hookErrorResult = new JSONObject();
    JSONObject hookMessage = new JSONObject();
    hookMessage.put("severity", "error");
    hookMessage.put("text", "Hook error");
    hookErrorResult.put("message", hookMessage);

    List<PaymentProcessHook> hookList = new ArrayList<>();
    PaymentProcessHook mockHook = mock(PaymentProcessHook.class);
    hookList.add(mockHook);
    paymentProcessOrderHookStatic.when(() -> PaymentProcessOrderHook.sortHooksByPriority(any()))
        .thenReturn(hookList);
    when(mockHook.preProcess(any())).thenReturn(hookErrorResult);

    JSONObject result = handler.oldProcessPaymentHandler(parameters, content, "PAY001", false);

    assertNotNull(result);
    assertTrue(result.has("message"));
  }

  // ===== Tests for action method (the main entry point) =====

  @Test
  public void testActionWithOrderMultipleRecordsReturnsError() throws Exception {
    // Set up input data
    JSONObject rawData = new JSONObject();
    rawData.put("_entityName", "Order");

    Data data = mock(Data.class);
    when(data.getRawData()).thenReturn(rawData);

    List<FIN_Payment> paymentList = new ArrayList<>();
    paymentList.add(mock(FIN_Payment.class));
    paymentList.add(mock(FIN_Payment.class));
    when(data.getContents(FIN_Payment.class)).thenReturn(paymentList);

    setFieldOnSuperclass(handler, "input", data);

    Method actionMethod = AddPaymentActionHandler.class.getDeclaredMethod(
        "action", JSONObject.class, MutableBoolean.class);
    actionMethod.setAccessible(true);

    ActionResult result = (ActionResult) actionMethod.invoke(handler, new JSONObject(), new MutableBoolean(false));

    assertEquals(Result.Type.ERROR, result.getType());
  }

  @Test
  public void testActionWithInvoiceMultipleRecordsReturnsError() throws Exception {
    JSONObject rawData = new JSONObject();
    rawData.put("_entityName", "Invoice");

    Data data = mock(Data.class);
    when(data.getRawData()).thenReturn(rawData);

    List<FIN_Payment> paymentList = new ArrayList<>();
    paymentList.add(mock(FIN_Payment.class));
    paymentList.add(mock(FIN_Payment.class));
    when(data.getContents(FIN_Payment.class)).thenReturn(paymentList);

    setFieldOnSuperclass(handler, "input", data);

    Method actionMethod = AddPaymentActionHandler.class.getDeclaredMethod(
        "action", JSONObject.class, MutableBoolean.class);
    actionMethod.setAccessible(true);

    ActionResult result = (ActionResult) actionMethod.invoke(handler, new JSONObject(), new MutableBoolean(false));

    assertEquals(Result.Type.ERROR, result.getType());
  }

  @Test
  public void testActionWithPaymentMultipleRecordsNotWebServiceReturnsError() throws Exception {
    JSONObject rawData = new JSONObject();
    rawData.put("_entityName", "FIN_Payment");

    Data data = mock(Data.class);
    when(data.getRawData()).thenReturn(rawData);

    List<FIN_Payment> paymentList = new ArrayList<>();
    paymentList.add(mock(FIN_Payment.class));
    paymentList.add(mock(FIN_Payment.class));
    when(data.getContents(FIN_Payment.class)).thenReturn(paymentList);

    setFieldOnSuperclass(handler, "input", data);

    Method actionMethod = AddPaymentActionHandler.class.getDeclaredMethod(
        "action", JSONObject.class, MutableBoolean.class);
    actionMethod.setAccessible(true);

    ActionResult result = (ActionResult) actionMethod.invoke(handler, new JSONObject(), new MutableBoolean(false));

    assertEquals(Result.Type.ERROR, result.getType());
  }

  @Test
  public void testActionWithNonPaymentEntityCallsOldProcess() throws Exception {
    JSONObject rawData = new JSONObject();
    rawData.put("_entityName", "Invoice");

    JSONObject params = new JSONObject();
    params.put("fin_payment_id", "PAY001");
    rawData.put("_params", params);

    Data data = mock(Data.class);
    when(data.getRawData()).thenReturn(rawData);

    List<FIN_Payment> paymentList = new ArrayList<>();
    paymentList.add(mock(FIN_Payment.class));
    when(data.getContents(FIN_Payment.class)).thenReturn(paymentList);

    setFieldOnSuperclass(handler, "input", data);

    // Mock oldProcessPaymentHandler via spy - returns result with message
    JSONObject processResult = new JSONObject();
    JSONObject msg = new JSONObject();
    msg.put("severity", "success");
    msg.put("text", "OK");
    processResult.put("message", msg);

    doReturn(processResult).when(handler).oldProcessPaymentHandler(any(), any(), anyString(), anyBoolean());

    // Inject a mockResponseBuilder into the action via reflection on callPaymentProcess
    // callPaymentProcess calls getResponseBuilder which is protected.
    // We set up the action result's responseActionsBuilder before the call so orElse won't be needed.
    // Actually this is internal to callPaymentProcess which creates a new ActionResult.
    // The simplest approach: verify oldProcessPaymentHandler is called.
    Method actionMethod = AddPaymentActionHandler.class.getDeclaredMethod(
        "action", JSONObject.class, MutableBoolean.class);
    actionMethod.setAccessible(true);

    try {
      actionMethod.invoke(handler, new JSONObject(), new MutableBoolean(false));
    } catch (java.lang.reflect.InvocationTargetException e) {
      // The call to getResponseBuilder may fail since it's a mock context,
      // but we can verify oldProcessPaymentHandler was called
    }

    verify(handler).oldProcessPaymentHandler(any(), any(), eq("PAY001"), eq(false));
  }

  @Test
  public void testActionWithPaymentWebServiceDocumentActionMissing() throws Exception {
    JSONObject rawData = new JSONObject();
    rawData.put("_entityName", "FIN_Payment");
    rawData.put("processByWebService", true);
    // No document_action

    Data data = mock(Data.class);
    when(data.getRawData()).thenReturn(rawData);

    FIN_Payment mockPay = mock(FIN_Payment.class);
    when(mockPay.getId()).thenReturn("PAY001");
    List<FIN_Payment> paymentList = new ArrayList<>();
    paymentList.add(mockPay);
    when(data.getContents(FIN_Payment.class)).thenReturn(paymentList);

    setFieldOnSuperclass(handler, "input", data);

    Method actionMethod = AddPaymentActionHandler.class.getDeclaredMethod(
        "action", JSONObject.class, MutableBoolean.class);
    actionMethod.setAccessible(true);

    ActionResult result = (ActionResult) actionMethod.invoke(handler, new JSONObject(), new MutableBoolean(false));

    assertEquals(Result.Type.ERROR, result.getType());
  }

  @Test
  public void testActionWithPaymentWebServiceWithDocumentAction() throws Exception {
    JSONObject rawData = new JSONObject();
    rawData.put("_entityName", "FIN_Payment");
    rawData.put("processByWebService", true);
    rawData.put("document_action", "PRP");

    Data data = mock(Data.class);
    when(data.getRawData()).thenReturn(rawData);

    FIN_Payment mockPay = mock(FIN_Payment.class);
    when(mockPay.getId()).thenReturn("PAY001");
    List<FIN_Payment> paymentList = new ArrayList<>();
    paymentList.add(mockPay);
    when(data.getContents(FIN_Payment.class)).thenReturn(paymentList);

    setFieldOnSuperclass(handler, "input", data);

    // Mock oldProcessPaymentHandler to return success
    JSONObject processResult = new JSONObject();
    JSONObject msg = new JSONObject();
    msg.put("severity", "success");
    msg.put("text", "OK");
    processResult.put("message", msg);
    processResult.put("retryExecution", true);

    doReturn(processResult).when(handler).oldProcessPaymentHandler(any(), any(), anyString(), anyBoolean());

    Method actionMethod = AddPaymentActionHandler.class.getDeclaredMethod(
        "action", JSONObject.class, MutableBoolean.class);
    actionMethod.setAccessible(true);

    try {
      actionMethod.invoke(handler, new JSONObject(), new MutableBoolean(false));
    } catch (java.lang.reflect.InvocationTargetException e) {
      // callPaymentProcess may fail on getResponseBuilder, that's OK
    }

    verify(handler).oldProcessPaymentHandler(any(), any(), eq("PAY001"), eq(true));
  }

  @Test
  public void testActionExceptionRollsBackAndReturnsError() throws Exception {
    JSONObject rawData = new JSONObject();
    rawData.put("_entityName", "FIN_Payment");

    Data data = mock(Data.class);
    when(data.getRawData()).thenReturn(rawData);
    when(data.getContents(FIN_Payment.class)).thenThrow(new RuntimeException("test exception"));

    setFieldOnSuperclass(handler, "input", data);

    Method actionMethod = AddPaymentActionHandler.class.getDeclaredMethod(
        "action", JSONObject.class, MutableBoolean.class);
    actionMethod.setAccessible(true);

    ActionResult result = (ActionResult) actionMethod.invoke(handler, new JSONObject(), new MutableBoolean(false));

    assertEquals(Result.Type.ERROR, result.getType());
    assertEquals("test exception", result.getMessage());
  }

  // ===== Tests for removeNotSelectedPaymentDetails (private) =====

  @Test
  public void testRemoveNotSelectedPaymentDetailsEmptyList() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "removeNotSelectedPaymentDetails", FIN_Payment.class, List.class);
    method.setAccessible(true);

    FIN_Payment payment = mock(FIN_Payment.class);
    List<String> pdToRemove = new ArrayList<>();

    // Should complete without error
    method.invoke(handler, payment, pdToRemove);
  }

  @Test
  public void testRemoveNotSelectedPaymentDetailsWithGLItem() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "removeNotSelectedPaymentDetails", FIN_Payment.class, List.class);
    method.setAccessible(true);

    FIN_Payment payment = mock(FIN_Payment.class);
    FIN_PaymentDetail pd = mock(FIN_PaymentDetail.class);
    FIN_PaymentScheduleDetail psd = mock(FIN_PaymentScheduleDetail.class);
    org.openbravo.model.financialmgmt.gl.GLItem glItem =
        mock(org.openbravo.model.financialmgmt.gl.GLItem.class);

    when(mockOBDal.get(FIN_PaymentDetail.class, "PD001")).thenReturn(pd);
    when(pd.getGLItem()).thenReturn(glItem);

    List<FIN_PaymentScheduleDetail> psdList = new ArrayList<>();
    psdList.add(psd);
    when(pd.getFINPaymentScheduleDetailList()).thenReturn(psdList);

    obDaoStatic.when(() -> OBDao.getIDListFromOBObject(any())).thenReturn(
        new ArrayList<>(Collections.singletonList("PSD001")));
    when(mockOBDal.get(FIN_PaymentScheduleDetail.class, "PSD001")).thenReturn(psd);

    List<FIN_PaymentDetail> paymentDetailList = new ArrayList<>();
    paymentDetailList.add(pd);
    when(payment.getFINPaymentDetailList()).thenReturn(paymentDetailList);

    List<String> pdToRemove = new ArrayList<>();
    pdToRemove.add("PD001");

    method.invoke(handler, payment, pdToRemove);

    verify(mockOBDal).remove(psd);
    verify(mockOBDal).remove(pd);
  }

  @Test
  public void testRemoveNotSelectedPaymentDetailsWithoutGLItemAndWithOutstanding() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "removeNotSelectedPaymentDetails", FIN_Payment.class, List.class);
    method.setAccessible(true);

    FIN_Payment payment = mock(FIN_Payment.class);
    FIN_PaymentDetail pd = mock(FIN_PaymentDetail.class);
    FIN_PaymentScheduleDetail psd = mock(FIN_PaymentScheduleDetail.class);
    FIN_PaymentScheduleDetail outstandingPsd = mock(FIN_PaymentScheduleDetail.class);

    when(mockOBDal.get(FIN_PaymentDetail.class, "PD001")).thenReturn(pd);
    when(pd.getGLItem()).thenReturn(null);
    when(psd.getAmount()).thenReturn(new BigDecimal("100.00"));
    when(psd.getWriteoffAmount()).thenReturn(BigDecimal.ZERO);
    when(psd.getDoubtfulDebtAmount()).thenReturn(BigDecimal.ZERO);

    List<FIN_PaymentScheduleDetail> psdList = new ArrayList<>();
    psdList.add(psd);
    when(pd.getFINPaymentScheduleDetailList()).thenReturn(psdList);

    obDaoStatic.when(() -> OBDao.getIDListFromOBObject(any())).thenReturn(
        new ArrayList<>(Collections.singletonList("PSD001")));
    when(mockOBDal.get(FIN_PaymentScheduleDetail.class, "PSD001")).thenReturn(psd);

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
    pdToRemove.add("PD001");

    method.invoke(handler, payment, pdToRemove);

    // Should update existing outstanding PSD
    verify(outstandingPsd).setAmount(new BigDecimal("150.00"));
    verify(mockOBDal).save(outstandingPsd);
    verify(mockOBDal).remove(psd);
    verify(mockOBDal).remove(pd);
  }

  // ===== Tests for CLE accounting path in getDocumentConfirmation =====

  @Test
  public void testGetDocumentConfirmationCLEReceiptPath() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getDocumentConfirmation", FIN_FinancialAccount.class,
        FIN_PaymentMethod.class, boolean.class, String.class, boolean.class);
    method.setAccessible(true);

    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    OBCriteria<FinAccPaymentMethod> mockCriteria = mock(OBCriteria.class);
    lenient().when(mockOBDal.createCriteria(FinAccPaymentMethod.class)).thenReturn(mockCriteria);
    lenient().when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);

    FinAccPaymentMethod mockFinAccPM = mock(FinAccPaymentMethod.class);
    lenient().when(mockCriteria.uniqueResult()).thenReturn(mockFinAccPM);
    lenient().when(mockFinAccPM.getUponReceiptUse()).thenReturn("CLE");

    org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting mockAcct =
        mock(org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting.class);
    lenient().when(mockAcct.getClearedPaymentAccount()).thenReturn(mock(AccountingCombination.class));
    List<org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting> acctList =
        new ArrayList<>();
    acctList.add(mockAcct);
    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(acctList);

    boolean result = (boolean) method.invoke(handler, finAccount, paymentMethod, true, "100", true);

    assertTrue(result);
  }

  @Test
  public void testGetDocumentConfirmationCLEPaymentOutPath() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getDocumentConfirmation", FIN_FinancialAccount.class,
        FIN_PaymentMethod.class, boolean.class, String.class, boolean.class);
    method.setAccessible(true);

    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    OBCriteria<FinAccPaymentMethod> mockCriteria = mock(OBCriteria.class);
    lenient().when(mockOBDal.createCriteria(FinAccPaymentMethod.class)).thenReturn(mockCriteria);
    lenient().when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);

    FinAccPaymentMethod mockFinAccPM = mock(FinAccPaymentMethod.class);
    lenient().when(mockCriteria.uniqueResult()).thenReturn(mockFinAccPM);
    lenient().when(mockFinAccPM.getUponPaymentUse()).thenReturn("CLE");

    org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting mockAcct =
        mock(org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting.class);
    lenient().when(mockAcct.getClearedPaymentAccountOUT()).thenReturn(mock(AccountingCombination.class));
    List<org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting> acctList =
        new ArrayList<>();
    acctList.add(mockAcct);
    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(acctList);

    boolean result = (boolean) method.invoke(handler, finAccount, paymentMethod, false, "100", true);

    assertTrue(result);
  }

  // ===== Tests for INT path in payment out for getDocumentConfirmation =====

  @Test
  public void testGetDocumentConfirmationINTPaymentOutPath() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getDocumentConfirmation", FIN_FinancialAccount.class,
        FIN_PaymentMethod.class, boolean.class, String.class, boolean.class);
    method.setAccessible(true);

    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    OBCriteria<FinAccPaymentMethod> mockCriteria = mock(OBCriteria.class);
    lenient().when(mockOBDal.createCriteria(FinAccPaymentMethod.class)).thenReturn(mockCriteria);
    lenient().when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);

    FinAccPaymentMethod mockFinAccPM = mock(FinAccPaymentMethod.class);
    lenient().when(mockCriteria.uniqueResult()).thenReturn(mockFinAccPM);
    lenient().when(mockFinAccPM.getUponPaymentUse()).thenReturn("INT");

    org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting mockAcct =
        mock(org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting.class);
    lenient().when(mockAcct.getFINOutIntransitAcct()).thenReturn(mock(AccountingCombination.class));
    List<org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting> acctList =
        new ArrayList<>();
    acctList.add(mockAcct);
    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(acctList);

    boolean result = (boolean) method.invoke(handler, finAccount, paymentMethod, false, "100", true);

    assertTrue(result);
  }

  // ===== Tests for INT receipt path in getDocumentConfirmation =====

  @Test
  public void testGetDocumentConfirmationINTReceiptPath() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getDocumentConfirmation", FIN_FinancialAccount.class,
        FIN_PaymentMethod.class, boolean.class, String.class, boolean.class);
    method.setAccessible(true);

    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    OBCriteria<FinAccPaymentMethod> mockCriteria = mock(OBCriteria.class);
    lenient().when(mockOBDal.createCriteria(FinAccPaymentMethod.class)).thenReturn(mockCriteria);
    lenient().when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);

    FinAccPaymentMethod mockFinAccPM = mock(FinAccPaymentMethod.class);
    lenient().when(mockCriteria.uniqueResult()).thenReturn(mockFinAccPM);
    lenient().when(mockFinAccPM.getUponReceiptUse()).thenReturn("INT");

    org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting mockAcct =
        mock(org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting.class);
    lenient().when(mockAcct.getInTransitPaymentAccountIN()).thenReturn(mock(AccountingCombination.class));
    List<org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting> acctList =
        new ArrayList<>();
    acctList.add(mockAcct);
    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(acctList);

    boolean result = (boolean) method.invoke(handler, finAccount, paymentMethod, true, "100", true);

    assertTrue(result);
  }

  // ===== Tests for DEP receipt path =====

  @Test
  public void testGetDocumentConfirmationDEPReceiptPath() throws Exception {
    Method method = AddPaymentActionHandler.class.getDeclaredMethod(
        "getDocumentConfirmation", FIN_FinancialAccount.class,
        FIN_PaymentMethod.class, boolean.class, String.class, boolean.class);
    method.setAccessible(true);

    FIN_FinancialAccount finAccount = mock(FIN_FinancialAccount.class);
    FIN_PaymentMethod paymentMethod = mock(FIN_PaymentMethod.class);

    OBCriteria<FinAccPaymentMethod> mockCriteria = mock(OBCriteria.class);
    lenient().when(mockOBDal.createCriteria(FinAccPaymentMethod.class)).thenReturn(mockCriteria);
    lenient().when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);

    FinAccPaymentMethod mockFinAccPM = mock(FinAccPaymentMethod.class);
    lenient().when(mockCriteria.uniqueResult()).thenReturn(mockFinAccPM);
    lenient().when(mockFinAccPM.getUponReceiptUse()).thenReturn("DEP");

    org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting mockAcct =
        mock(org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting.class);
    lenient().when(mockAcct.getDepositAccount()).thenReturn(mock(AccountingCombination.class));
    List<org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting> acctList =
        new ArrayList<>();
    acctList.add(mockAcct);
    lenient().when(finAccount.getFINFinancialAccountAcctList()).thenReturn(acctList);

    boolean result = (boolean) method.invoke(handler, finAccount, paymentMethod, true, "100", true);

    assertTrue(result);
  }

  // ===== Helper Methods =====

  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = findField(target.getClass(), fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private void setFieldOnSuperclass(Object target, String fieldName, Object value) throws Exception {
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
