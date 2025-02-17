package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.advpaymentmngt.utility.APRM_MatchingUtility;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.service.json.JsonUtils;

public class UnMatchSelectedTransactionsActionHandlerTest extends WeldBaseTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private UnMatchSelectedTransactionsActionHandler handler;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<APRM_MatchingUtility> mockedMatchingUtility;
  private AutoCloseable mocks;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private FIN_BankStatementLine mockBankStatementLine;

  @Mock
  private FIN_FinaccTransaction mockTransaction;

  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    handler = new UnMatchSelectedTransactionsActionHandler();

    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedMatchingUtility = mockStatic(APRM_MatchingUtility.class);

    when(mockOBDal.get(eq(FIN_BankStatementLine.class), anyString())).thenReturn(mockBankStatementLine);
    when(mockBankStatementLine.getFinancialAccountTransaction()).thenReturn(mockTransaction);

    OBContext.setAdminMode();
  }

  @After
  public void tearDown() throws Exception {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
    if (mockedMatchingUtility != null) {
      mockedMatchingUtility.close();
    }
    if (mocks != null) {
      mocks.close();
    }

  }

  @Test
  public void testExecute_SuccessfulUnmatch() throws Exception {
    // Given
    Map<String, Object> parameters = new HashMap<>();

    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.MILLISECOND, 0);
    Date testDate = calendar.getTime();

    String formattedDate = JsonUtils.createJSTimeFormat().format(testDate);
    String jsonData = createJsonData(true, "testRef", "testId", formattedDate);

    when(mockBankStatementLine.getUpdated()).thenReturn(testDate);

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("Success")).thenReturn("Success");
    mockedOBMessageUtils.when(() -> OBMessageUtils.getI18NMessage(
        eq("APRM_UnmatchedRecords"),
        any(String[].class)
    )).thenReturn("1 records unmatched");

    mockedMatchingUtility.when(() -> APRM_MatchingUtility.unmatch(any(FIN_BankStatementLine.class))).thenAnswer(invocation -> null);

    MockedStatic<OBDateUtils> mockedOBDateUtils = mockStatic(OBDateUtils.class);
    mockedOBDateUtils.when(() -> OBDateUtils.convertDateToUTC(any(Date.class))).thenReturn(testDate);

    try {
      // When
      JSONObject result = handler.execute(parameters, jsonData);

      // Then
      JSONObject message = result.getJSONObject("message");
      assertEquals("success", message.getString("severity"));
      assertEquals("Success", message.getString("title"));

      // Verify
      verify(mockOBDal).get(eq(FIN_BankStatementLine.class), eq("testId"));
      mockedMatchingUtility.verify(() -> APRM_MatchingUtility.unmatch(mockBankStatementLine));
    } finally {
      mockedOBDateUtils.close();
    }
  }


  @Test
  public void testExecute_AlreadyClearedTransaction() throws Exception {
    // Given
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = createJsonData(false, "testRef", "testId", JsonUtils.createJSTimeFormat().format(new java.util.Date()));

    // When
    JSONObject result = handler.execute(parameters, jsonData);

    // Then
    JSONObject message = result.getJSONObject("message");
    assertEquals("error", message.getString("severity"));
    verify(mockOBDal, never()).get(eq(FIN_BankStatementLine.class), anyString());
  }

  @Test
  public void testExecute_StaleObjectException() throws Exception {
    // Given
    Map<String, Object> parameters = new HashMap<>();
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.MILLISECOND, 0);
    Date testDate = calendar.getTime();

    Calendar newerDate = Calendar.getInstance();
    newerDate.setTime(testDate);
    newerDate.add(Calendar.HOUR, 1);

    String formattedDate = JsonUtils.createJSTimeFormat().format(testDate);
    String jsonData = createJsonData(true, "testRef", "testId", formattedDate);

    when(mockBankStatementLine.getUpdated()).thenReturn(newerDate.getTime());

    MockedStatic<OBDateUtils> mockedOBDateUtils = mockStatic(OBDateUtils.class);
    mockedOBDateUtils.when(() -> OBDateUtils.convertDateToUTC(any(Date.class))).thenReturn(newerDate.getTime());

    try {
      // When
      JSONObject result = handler.execute(parameters, jsonData);

      // Then
      JSONObject message = result.getJSONObject("message");
      assertEquals("error", message.getString("severity"));
      verify(mockOBDal).get(eq(FIN_BankStatementLine.class), eq("testId"));
    } finally {
      mockedOBDateUtils.close();
    }
  }

  @Test
  public void testExecute_EmptyBankStatementLines() throws Exception {
    // Given
    Map<String, Object> parameters = new HashMap<>();
    JSONObject jsonData = new JSONObject();
    jsonData.put("bankStatementLineIds", new JSONArray());

    // When
    JSONObject result = handler.execute(parameters, jsonData.toString());

    // Then
    JSONObject message = result.getJSONObject("message");
    assertEquals("error", message.getString("severity"));
  }


  @Test
  public void testExecute_NullTransaction() throws Exception {
    // Given
    Map<String, Object> parameters = new HashMap<>();
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.MILLISECOND, 0);
    Date testDate = calendar.getTime();

    String formattedDate = JsonUtils.createJSTimeFormat().format(testDate);
    String jsonData = createJsonData(true, "testRef", "testId", formattedDate);

    when(mockBankStatementLine.getUpdated()).thenReturn(testDate);
    when(mockBankStatementLine.getFinancialAccountTransaction()).thenReturn(null);

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("Success")).thenReturn("Success");
    mockedOBMessageUtils.when(() -> OBMessageUtils.getI18NMessage(
        eq("APRM_UnmatchedRecords"),
        any(String[].class)
    )).thenReturn("1 records unmatched");

    MockedStatic<OBDateUtils> mockedOBDateUtils = mockStatic(OBDateUtils.class);
    mockedOBDateUtils.when(() -> OBDateUtils.convertDateToUTC(any(Date.class))).thenReturn(testDate);

    try {
      // When
      JSONObject result = handler.execute(parameters, jsonData);

      // Then
      JSONObject message = result.getJSONObject("message");
      assertEquals("success", message.getString("severity"));
      assertEquals("Success", message.getString("title"));
      verify(mockOBDal).get(eq(FIN_BankStatementLine.class), eq("testId"));
    } finally {
      mockedOBDateUtils.close();
    }
  }

  @Test
  public void testExecute_MultipleRecords() throws Exception {
    // Given
    Map<String, Object> parameters = new HashMap<>();
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.MILLISECOND, 0);
    Date testDate = calendar.getTime();

    JSONObject jsonData = new JSONObject();
    JSONArray bankStatementLineIds = new JSONArray();
    bankStatementLineIds.put(createBankStatementLineJSON(true, "ref1", "id1", testDate));
    bankStatementLineIds.put(createBankStatementLineJSON(true, "ref2", "id2", testDate));
    jsonData.put("bankStatementLineIds", bankStatementLineIds);

    when(mockBankStatementLine.getUpdated()).thenReturn(testDate);

    mockedMatchingUtility.when(() -> APRM_MatchingUtility.unmatch(any(FIN_BankStatementLine.class)))
        .thenAnswer(invocation -> null);

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("Success")).thenReturn("Success");
    mockedOBMessageUtils.when(() -> OBMessageUtils.getI18NMessage(
        eq("APRM_UnmatchedRecords"),
        any(String[].class)
    )).thenReturn("2 records unmatched");

    MockedStatic<OBDateUtils> mockedOBDateUtils = mockStatic(OBDateUtils.class);
    mockedOBDateUtils.when(() -> OBDateUtils.convertDateToUTC(any(Date.class))).thenReturn(testDate);

    try {
      // When
      JSONObject result = handler.execute(parameters, jsonData.toString());

      // Then
      JSONObject message = result.getJSONObject("message");
      assertEquals("success", message.getString("severity"));
      assertTrue(message.getString("text").contains("2 records"));
    } finally {
      mockedOBDateUtils.close();
    }
  }

  private JSONObject createBankStatementLineJSON(boolean cleared, String referenceNo, String id, Date updated) throws Exception {
    JSONObject bankStatementLine = new JSONObject();
    bankStatementLine.put("cleared", cleared);
    bankStatementLine.put("referenceNo", referenceNo);
    bankStatementLine.put("id", id);
    bankStatementLine.put("bslUpdated", JsonUtils.createJSTimeFormat().format(updated));
    return bankStatementLine;
  }


  private String createJsonData(boolean cleared, String referenceNo, String id, String updated) throws Exception {
    JSONObject jsonData = new JSONObject();
    JSONArray bankStatementLineIds = new JSONArray();
    JSONObject bankStatementLine = new JSONObject();
    bankStatementLine.put("cleared", cleared);
    bankStatementLine.put("referenceNo", referenceNo);
    bankStatementLine.put("id", id);
    bankStatementLine.put("bslUpdated", updated);
    bankStatementLineIds.put(bankStatementLine);
    jsonData.put("bankStatementLineIds", bankStatementLineIds);
    return jsonData.toString();
  }


}