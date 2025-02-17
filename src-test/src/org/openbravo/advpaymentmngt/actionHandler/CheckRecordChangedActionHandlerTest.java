package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
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
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.service.json.JsonUtils;

/**
 * Test for the CheckRecordChangedActionHandler class
 */
@RunWith(MockitoJUnitRunner.class)
public class CheckRecordChangedActionHandlerTest {

  private static final String BANK_STATEMENT_LINE_ID = "TEST_BSL_ID";
  private static final String STALE_DATE_MESSAGE = "APRM_StaleDate";

  @Mock
  private FIN_BankStatementLine mockBankStatementLine;

  @Mock
  private OBDal mockOBDal;

  @InjectMocks
  private CheckRecordChangedActionHandler actionHandler;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<OBDateUtils> mockedOBDateUtils;
  private MockedStatic<JsonUtils> mockedJsonUtils;
  private AutoCloseable mocks;

  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);

    // Setup static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedOBDateUtils = mockStatic(OBDateUtils.class);
    mockedJsonUtils = mockStatic(JsonUtils.class);

    // Configure default mock behavior
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    when(mockOBDal.get(FIN_BankStatementLine.class, BANK_STATEMENT_LINE_ID)).thenReturn(mockBankStatementLine);

    // Mock the message bundle
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(STALE_DATE_MESSAGE))
        .thenReturn("Record has been modified by another user");

    // mock OBDateUtils.convertDateToUTC
    mockedOBDateUtils.when(() -> OBDateUtils.convertDateToUTC(any(Date.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @After
  public void tearDown() throws Exception {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
    if (mockedOBDateUtils != null) {
      mockedOBDateUtils.close();
    }
    if (mockedJsonUtils != null) {
      mockedJsonUtils.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Test case for when the record has not been changed (dates match)
   */
  @Test
  public void testExecute_DatesMatch() throws Exception {
    // GIVEN
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = createJsonData("2023-01-01T12:00:00Z");

    // Setup matching dates
    Calendar calendar = Calendar.getInstance();
    calendar.set(2023, 0, 1, 12, 0, 0); // 2023-01-01 12:00:00
    calendar.set(Calendar.MILLISECOND, 0);
    Date testDate = calendar.getTime();

    when(mockBankStatementLine.getUpdated()).thenReturn(testDate);

    // Mock date parsing to return the same date
    SimpleDateFormat mockDateFormat = mock(SimpleDateFormat.class);
    mockedJsonUtils.when(JsonUtils::createJSTimeFormat).thenReturn(mockDateFormat);
    when(mockDateFormat.parse("2023-01-01T12:00:00Z")).thenReturn(testDate);

    // WHEN
    JSONObject result = actionHandler.execute(parameters, jsonData);

    // THEN
    assertNull("No error message should be returned when dates match", result.optJSONObject("message"));
  }

  /**
   * Test case for when the record has been changed (dates don't match)
   */
  @Test
  public void testExecute_DatesMismatch() throws Exception {
    // GIVEN
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = createJsonData("2023-01-01T12:00:00Z");

    // Setup different dates
    // DB date (one hour later than UI date)
    Calendar dbCalendar = Calendar.getInstance();
    dbCalendar.set(2023, 0, 1, 13, 0, 0);
    dbCalendar.set(Calendar.MILLISECOND, 0);
    Date dbDate = dbCalendar.getTime();

    // UI date
    Calendar uiCalendar = Calendar.getInstance();
    uiCalendar.set(2023, 0, 1, 12, 0, 0);
    uiCalendar.set(Calendar.MILLISECOND, 0);
    Date uiDate = uiCalendar.getTime();

    when(mockBankStatementLine.getUpdated()).thenReturn(dbDate);

    // Mock date parsing to return the UI date
    SimpleDateFormat mockDateFormat = mock(SimpleDateFormat.class);
    mockedJsonUtils.when(JsonUtils::createJSTimeFormat).thenReturn(mockDateFormat);
    when(mockDateFormat.parse("2023-01-01T12:00:00Z")).thenReturn(uiDate);

    // WHEN
    JSONObject result = actionHandler.execute(parameters, jsonData);

    // THEN
    JSONObject message = result.getJSONObject("message");
    assertNotNull("Error message should be returned when dates don't match", message);
    assertEquals("Error message should have severity 'error'", "error", message.getString("severity"));
    assertEquals("Error message should have title 'Error'", "Error", message.getString("title"));
    assertEquals("Error message should contain stale date message",
        "Record has been modified by another user", message.getString("text"));
  }

  /**
   * Test case for handling JSON parsing exceptions
   */
  @Test
  public void testExecute_JsonException() throws Exception {
    // GIVEN
    Map<String, Object> parameters = new HashMap<>();
    String invalidJsonData = "{invalid json";

    // WHEN
    JSONObject result = actionHandler.execute(parameters, invalidJsonData);

    // THEN
    JSONObject message = result.getJSONObject("message");
    assertNotNull("Error message should be returned for invalid JSON", message);
    assertEquals("Error message should have severity 'error'", "error", message.getString("severity"));
    assertEquals("Error message should have title 'Error'", "Error", message.getString("title"));
    assertEquals("Error message should be empty for JSON exception", "", message.getString("text"));
  }

  /**
   * Helper method to create JSON data for testing
   */
  private String createJsonData(String dateStr) throws JSONException {
    JSONObject jsonData = new JSONObject();
    jsonData.put("bankStatementLineId", BANK_STATEMENT_LINE_ID);
    jsonData.put("updated", dateStr);
    return jsonData.toString();
  }
}