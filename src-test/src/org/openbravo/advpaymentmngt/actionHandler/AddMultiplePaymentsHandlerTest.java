package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Instance;

import org.codehaus.jettison.json.JSONArray;
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
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.advpaymentmngt.process.FIN_TransactionProcess;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.service.json.JsonUtils;

@RunWith(MockitoJUnitRunner.class)
public class AddMultiplePaymentsHandlerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  // Static mocks
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<TransactionsDao> mockedTransactionsDao;
  private MockedStatic<FIN_TransactionProcess> mockedFIN_TransactionProcess;
  private MockedStatic<JsonUtils> mockedJsonUtils;
  private MockedStatic<RequestContext> mockedRequestContext;

  // Mocks
  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBContext mockObContext;

  @Mock
  private Instance<AddMultiplePaymentsProcessAfterProcessHook> afterHooks;

  @Mock
  private FIN_Payment mockPayment;

  @Mock
  private FIN_FinancialAccount mockFinancialAccount;

  @Mock
  private FIN_FinaccTransaction mockTransaction;

  @Mock
  private SimpleDateFormat mockDateFormat;

  @Mock
  private RequestContext mockRequestContext;

  // Class under test
  @InjectMocks
  private AddMultiplePaymentsHandler classUnderTest;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    // Setup static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedTransactionsDao = mockStatic(TransactionsDao.class);
    mockedFIN_TransactionProcess = mockStatic(FIN_TransactionProcess.class);
    mockedJsonUtils = mockStatic(JsonUtils.class);
    mockedRequestContext = mockStatic(RequestContext.class);

    // Setup OBDal mock
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    // Configure OBContext mock
    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockObContext);

    // Configurar mock para setAdminMode
    mockedOBContext.when(() -> OBContext.setAdminMode(true)).thenAnswer(inv -> null);
    mockedOBContext.when(OBContext::restorePreviousMode).thenAnswer(inv -> null);

    // Setup RequestContext mock
    mockedRequestContext.when(RequestContext::get).thenReturn(mockRequestContext);

    // Setup JsonUtils mock
    mockedJsonUtils.when(JsonUtils::createDateFormat).thenReturn(mockDateFormat);

    // Mock empty afterHooks
    when(afterHooks.iterator()).thenReturn(new ArrayList<AddMultiplePaymentsProcessAfterProcessHook>().iterator());

    ArrayList<AddMultiplePaymentsProcessAfterProcessHook> emptyHooks = new ArrayList<>();
    when(afterHooks.iterator()).thenReturn(emptyHooks.iterator());

    org.hibernate.Session mockSession = mock(org.hibernate.Session.class);
    when(mockOBDal.getSession()).thenReturn(mockSession);
  }

  @After
  public void tearDown() throws Exception {
    // Close all static mocks
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
    if (mockedTransactionsDao != null) {
      mockedTransactionsDao.close();
    }
    if (mockedFIN_TransactionProcess != null) {
      mockedFIN_TransactionProcess.close();
    }
    if (mockedJsonUtils != null) {
      mockedJsonUtils.close();
    }
    if (mockedRequestContext != null) {
      mockedRequestContext.close();
    }
  }

  @Test
  public void testDoExecute_SuccessfulExecution() throws Exception {
    // GIVEN
    Map<String, Object> parameters = new HashMap<>();

    // Create JSON data
    String financialAccountId = "TEST_ACCOUNT_ID";
    Date testDate = new Date();
    String formattedDate = "2023-01-01";

    // Create JSON structure
    JSONObject jsonData = new JSONObject();
    jsonData.put("Fin_Financial_Account_ID", financialAccountId);

    JSONObject jsonParams = new JSONObject();
    jsonParams.put("statementDate", formattedDate);
    jsonParams.put("dateAcct", formattedDate);

    JSONObject paymentsObj = new JSONObject();
    JSONArray selectedPayments = new JSONArray();

    // Add a payment to the selection
    JSONObject paymentJS = new JSONObject();
    String paymentId = "TEST_PAYMENT_ID";
    paymentJS.put("id", paymentId);
    selectedPayments.put(paymentJS);

    paymentsObj.put("_selection", selectedPayments);
    jsonParams.put("payments", paymentsObj);
    jsonData.put("_params", jsonParams);

    // Mock OBDal.get to return our mock payment
    when(mockOBDal.get(FIN_Payment.class, paymentId)).thenReturn(mockPayment);
    when(mockOBDal.get(FIN_FinancialAccount.class, financialAccountId)).thenReturn(mockFinancialAccount);

    // Mock TransactionsDao.createFinAccTransaction
    mockedTransactionsDao.when(() -> TransactionsDao.createFinAccTransaction(mockPayment))
        .thenReturn(mockTransaction);

    // Mock success message
    String successMessage = "Success message";
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("APRM_MULTIPLE_TRANSACTIONS_ADDED"))
        .thenReturn(successMessage);
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("success"))
        .thenReturn("success");

    // Mock session.clear()
    org.hibernate.Session mockSession = mock(org.hibernate.Session.class);
    when(mockOBDal.getSession()).thenReturn(mockSession);

    // Mock afterHooks
    ArrayList<AddMultiplePaymentsProcessAfterProcessHook> emptyHooksList = new ArrayList<>();
    when(afterHooks.iterator()).thenReturn(emptyHooksList.iterator());

    // WHEN
    JSONObject result = classUnderTest.doExecute(parameters, jsonData.toString());

    // THEN
    assertNotNull("Result should not be null", result);
    assertTrue("Result should contain responseActions", result.has("responseActions"));

    // Verify transaction was created and processed
    verify(mockTransaction).setTransactionDate(any(Date.class));
    verify(mockTransaction).setDateAcct(any(Date.class));
    verify(mockTransaction).setAccount(mockFinancialAccount);

    // Verify transaction process was called
    mockedFIN_TransactionProcess.verify(
        () -> FIN_TransactionProcess.doTransactionProcess("P", mockTransaction), times(1));
  }

  @Test
  public void testDoExecute_NoPaymentsSelected() throws Exception {
    // GIVEN
    Map<String, Object> parameters = new HashMap<>();

    // Create JSON data
    String financialAccountId = "TEST_ACCOUNT_ID";

    // Create JSON structure
    JSONObject jsonData = new JSONObject();
    jsonData.put("Fin_Financial_Account_ID", financialAccountId);

    JSONObject jsonParams = new JSONObject();
    jsonParams.put("statementDate", "2023-01-01");
    jsonParams.put("dateAcct", "2023-01-01");

    JSONObject paymentsObj = new JSONObject();
    JSONArray selectedPayments = new JSONArray();
    // No payments added to selection

    paymentsObj.put("_selection", selectedPayments);
    jsonParams.put("payments", paymentsObj);
    jsonData.put("_params", jsonParams);

    // Mock error message
    String errorMessage = "No payments selected";
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("APRM_NO_PAYMENTS_SELECTED"))
        .thenReturn(errorMessage);

    // WHEN
    JSONObject result = classUnderTest.doExecute(parameters, jsonData.toString());

    // THEN
    assertNotNull("Result should not be null", result);
    assertTrue("Result should contain message", result.has("message"));
    JSONObject message = result.getJSONObject("message");
    assertEquals("Error severity should be set", "error", message.getString("severity"));
    assertEquals("Error text should match", errorMessage, message.getString("text"));
    assertTrue("Should retry execution", result.getBoolean("retryExecution"));
  }

  @Test
  public void testDoExecute_ExceptionHandling() throws Exception {
    // GIVEN
    Map<String, Object> parameters = new HashMap<>();

    // Create JSON data that will cause an exception
    JSONObject jsonData = new JSONObject();
    // Missing required fields to trigger exception

    // WHEN
    JSONObject result = classUnderTest.doExecute(parameters, jsonData.toString());

    // THEN
    assertNotNull("Result should not be null", result);
    // Should return an empty JSONObject when exception is not properly handled
    assertEquals("Should return empty JSONObject", 0, result.length());

    // Verify rollback was called
    verify(mockOBDal).rollbackAndClose();
  }

  @Test
  public void testCreateAndProcessTransactionFromPayment() throws Exception {
    // GIVEN
    String paymentId = "TEST_PAYMENT_ID";
    String accountId = "TEST_ACCOUNT_ID";
    Date testDate = new Date();

    // Create payment JSON
    JSONObject paymentJS = new JSONObject();
    paymentJS.put("id", paymentId);

    // Mock OBDal.get to return our mock payment and account
    when(mockOBDal.get(FIN_Payment.class, paymentId)).thenReturn(mockPayment);
    when(mockOBDal.get(FIN_FinancialAccount.class, accountId)).thenReturn(mockFinancialAccount);

    // Mock TransactionsDao.createFinAccTransaction
    mockedTransactionsDao.when(() -> TransactionsDao.createFinAccTransaction(mockPayment))
        .thenReturn(mockTransaction);

    // WHEN - Use reflection to access private method
    java.lang.reflect.Method method = AddMultiplePaymentsHandler.class.getDeclaredMethod(
        "createAndProcessTransactionFromPayment",
        JSONObject.class, Date.class, Date.class, String.class);
    method.setAccessible(true);
    method.invoke(classUnderTest, paymentJS, testDate, testDate, accountId);

    // THEN
    // Verify transaction was created and processed
    verify(mockTransaction).setTransactionDate(testDate);
    verify(mockTransaction).setDateAcct(testDate);
    verify(mockTransaction).setAccount(mockFinancialAccount);

    // Verify transaction process was called
    mockedFIN_TransactionProcess.verify(
        () -> FIN_TransactionProcess.doTransactionProcess("P", mockTransaction), times(1));

    // Verify setAdminMode and restorePreviousMode were called in balance
    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
  }

  @Test
  public void testGetSuccessMessage() throws Exception {
    // GIVEN
    String successText = "1 transaction processed successfully";

    // Mock success message
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("success"))
        .thenReturn("success");

    // WHEN - Use reflection to access private method
    java.lang.reflect.Method method = AddMultiplePaymentsHandler.class.getDeclaredMethod(
        "getSuccessMessage", String.class);
    method.setAccessible(true);
    JSONObject result = (JSONObject) method.invoke(null, successText);

    // THEN
    assertNotNull("Result should not be null", result);
    assertTrue("Result should contain responseActions", result.has("responseActions"));

    JSONArray actions = result.getJSONArray("responseActions");
    assertEquals("Should have one action", 1, actions.length());

    JSONObject action = actions.getJSONObject(0);
    assertTrue("Action should contain showMsgInProcessView", action.has("showMsgInProcessView"));

    JSONObject msg = action.getJSONObject("showMsgInProcessView");
    assertEquals("Message type should be success", "success", msg.getString("msgType"));
    assertEquals("Message title should be success", "success", msg.getString("msgTitle"));
    assertEquals("Message text should match", successText, msg.getString("msgText"));
  }

  @Test
  public void testGetErrorMessage() throws Exception {
    // GIVEN
    String errorText = "Error processing transactions";

    // WHEN - Use reflection to access private method
    java.lang.reflect.Method method = AddMultiplePaymentsHandler.class.getDeclaredMethod(
        "getErrorMessage", String.class);
    method.setAccessible(true);
    JSONObject result = (JSONObject) method.invoke(null, errorText);

    // THEN
    assertNotNull("Result should not be null", result);
    assertTrue("Result should contain message", result.has("message"));

    JSONObject message = result.getJSONObject("message");
    assertEquals("Error severity should be set", "error", message.getString("severity"));
    assertEquals("Error text should match", errorText, message.getString("text"));

    assertTrue("Should retry execution", result.getBoolean("retryExecution"));
  }
}