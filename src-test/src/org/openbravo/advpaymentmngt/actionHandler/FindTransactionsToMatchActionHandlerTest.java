package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.advpaymentmngt.utility.APRM_MatchingUtility;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.service.db.DbUtility;

@RunWith(MockitoJUnitRunner.class)
public class FindTransactionsToMatchActionHandlerTest {

    @InjectMocks
    private FindTransactionsToMatchActionHandler actionHandler;

    // Static mocks
    private MockedStatic<OBContext> mockedOBContext;
    private MockedStatic<OBDal> mockedOBDal;
    private MockedStatic<TransactionsDao> mockedTransactionsDao;
    private MockedStatic<APRM_MatchingUtility> mockedMatchingUtility;
    private MockedStatic<DbUtility> mockedDbUtility;

    // Instance mocks
    @Mock
    private OBDal obDal;
    @Mock
    private FIN_FinancialAccount financialAccount;
    @Mock
    private FIN_BankStatementLine bankStatementLine;
    @Mock
    private FIN_Reconciliation reconciliation;


    private AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);

        // Setup static mocks
        mockedOBContext = mockStatic(OBContext.class);
        mockedOBDal = mockStatic(OBDal.class);
        mockedTransactionsDao = mockStatic(TransactionsDao.class);
        mockedMatchingUtility = mockStatic(APRM_MatchingUtility.class);
        mockedDbUtility = mockStatic(DbUtility.class);

        // Configure static mocks
        mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
        mockedTransactionsDao.when(() -> TransactionsDao.getLastReconciliation(any(FIN_FinancialAccount.class), eq("N")))
            .thenReturn(reconciliation);

        // Configure instance mocks
        when(obDal.get(eq(FIN_FinancialAccount.class), anyString())).thenReturn(financialAccount);
        when(obDal.get(eq(FIN_BankStatementLine.class), anyString())).thenReturn(bankStatementLine);
    }

    @After
    public void tearDown() throws Exception {
        if (mockedOBContext != null) {
            mockedOBContext.close();
        }
        if (mockedOBDal != null) {
            mockedOBDal.close();
        }
        if (mockedTransactionsDao != null) {
            mockedTransactionsDao.close();
        }
        if (mockedMatchingUtility != null) {
            mockedMatchingUtility.close();
        }
        if (mockedDbUtility != null) {
            mockedDbUtility.close();
        }
        closeable.close();
    }

    @Test
    public void testExecute_withValidSelection() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        String data = createValidJsonData();
        List<String> selectedTransactionIds = new ArrayList<>();
        selectedTransactionIds.add("transaction1");

        mockedMatchingUtility.when(() -> APRM_MatchingUtility.matchBankStatementLine(
            any(FIN_BankStatementLine.class),
            anyList(),
            any(FIN_Reconciliation.class),
            any(),
            anyBoolean())).thenReturn(1);

        // When
        JSONObject result = actionHandler.execute(parameters, data);

        // Then
        // Verify OBContext methods were called
        mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
        mockedOBContext.verify(OBContext::restorePreviousMode, times(1));

        // Verify matchBankStatementLine was called
        mockedMatchingUtility.verify(() -> APRM_MatchingUtility.matchBankStatementLine(
            (FIN_BankStatementLine) eq(bankStatementLine),
            (List<String>) any(),
            eq(reconciliation),
            any(),
            eq(true)), times(1));

        // Verify result
        assertEquals(0, result.length());
    }

    @Test
    public void testExecute_withEmptySelection() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        String data = createEmptySelectionJsonData();
        JSONArray mockActions = new JSONArray();
        mockActions.put(new JSONObject());

        // Configure mocks for this test
        mockedMatchingUtility.when(() -> APRM_MatchingUtility.createMessageInProcessView(
            eq("@APRM_SELECT_RECORD_ERROR@"),
            eq("error"))).thenReturn(mockActions);

        // When
        JSONObject result = actionHandler.execute(parameters, data);

        // Then
        // Verify OBContext methods were called
        mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
        mockedOBContext.verify(OBContext::restorePreviousMode, times(1));

        // Verify createMessageInProcessView was called
        mockedMatchingUtility.verify(() -> APRM_MatchingUtility.createMessageInProcessView(
            eq("@APRM_SELECT_RECORD_ERROR@"),
            eq("error")), times(1));

        // Verify result
        assertTrue(result.has("responseActions"));
        assertTrue(result.has("retryExecution"));
        assertTrue(result.getBoolean("retryExecution"));
    }

    @Test
    public void testExecute_withException() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        String data = createValidJsonData();
        Exception mockException = new RuntimeException("Test exception");
        JSONArray mockActions = new JSONArray();
        mockActions.put(new JSONObject());

        // Configure mocks for this test
        when(obDal.get(eq(FIN_FinancialAccount.class), anyString())).thenThrow(mockException);
        mockedDbUtility.when(() -> DbUtility.getUnderlyingSQLException(any())).thenReturn(mockException);
        mockedMatchingUtility.when(() -> APRM_MatchingUtility.createMessageInProcessView(
            eq("Test exception"),
            eq("error"))).thenReturn(mockActions);

        // When
        JSONObject result = actionHandler.execute(parameters, data);

        // Then
        // Verify OBContext methods were called
        mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
        mockedOBContext.verify(OBContext::restorePreviousMode, times(1));

        // Verify rollbackAndClose was called
        verify(obDal, times(1)).rollbackAndClose();

        // Verify result
        assertTrue(result.has("responseActions"));
        assertTrue(result.has("retryExecution"));
        assertTrue(result.getBoolean("retryExecution"));
    }

    private String createValidJsonData() throws Exception {
        JSONObject jsonData = new JSONObject();
        jsonData.put("inpfinFinancialAccountId", "testAccountId");

        JSONObject params = new JSONObject();
        params.put("bankStatementLineId", "testBankLineId");

        JSONObject findTransactionToMatch = new JSONObject();
        JSONArray selection = new JSONArray();
        JSONObject transaction = new JSONObject();
        transaction.put("id", "transaction1");
        selection.put(transaction);
        findTransactionToMatch.put("_selection", selection);

        params.put("findtransactiontomatch", findTransactionToMatch);
        jsonData.put("_params", params);

        return jsonData.toString();
    }

    private String createEmptySelectionJsonData() throws Exception {
        JSONObject jsonData = new JSONObject();
        jsonData.put("inpfinFinancialAccountId", "testAccountId");

        JSONObject params = new JSONObject();
        params.put("bankStatementLineId", "testBankLineId");

        JSONObject findTransactionToMatch = new JSONObject();
        JSONArray selection = new JSONArray();
        findTransactionToMatch.put("_selection", selection);

        params.put("findtransactiontomatch", findTransactionToMatch);
        jsonData.put("_params", params);

        return jsonData.toString();
    }
}