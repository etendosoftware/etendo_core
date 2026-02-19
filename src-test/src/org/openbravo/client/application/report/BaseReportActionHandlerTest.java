package org.openbravo.client.application.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.ReportDefinition;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Tests for {@link BaseReportActionHandler}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class BaseReportActionHandlerTest {

  private static final String GET_SAFE_FILENAME = "getSafeFilename";
  private static final String REPORT_FILE = "report_file";
  private static final String RESPONSE_ACTIONS = "responseActions";

  @Mock
  private OBDal mockOBDal;
  @Mock
  private OBContext mockOBContext;
  @Mock
  private ReportDefinition mockReport;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<OBMessageUtils> obMessageUtilsStatic;

  private BaseReportActionHandler instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obContextStatic = mockStatic(OBContext.class);
    lenient().when(OBContext.getOBContext()).thenReturn(mockOBContext);

    obMessageUtilsStatic = mockStatic(OBMessageUtils.class);
    lenient().when(OBMessageUtils.messageBD("ReportGenerated")).thenReturn("Report generated successfully");
    lenient().when(OBMessageUtils.messageBD("CustomActionDetected")).thenReturn("Custom action detected: %s");

    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(ConcreteReportHandler.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (obMessageUtilsStatic != null) obMessageUtilsStatic.close();
  }
  /**
   * Get safe filename removes colons.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetSafeFilenameRemovesColons() throws Exception {
    Method method = BaseReportActionHandler.class.getDeclaredMethod(GET_SAFE_FILENAME, String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(null, "report:2024");
    assertEquals("report_2024", result);
  }
  /**
   * Get safe filename removes backslashes.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetSafeFilenameRemovesBackslashes() throws Exception {
    Method method = BaseReportActionHandler.class.getDeclaredMethod(GET_SAFE_FILENAME, String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(null, "report\\file");
    assertEquals(REPORT_FILE, result);
  }
  /**
   * Get safe filename removes slashes.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetSafeFilenameRemovesSlashes() throws Exception {
    Method method = BaseReportActionHandler.class.getDeclaredMethod(GET_SAFE_FILENAME, String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(null, "report/file");
    assertEquals(REPORT_FILE, result);
  }
  /**
   * Get safe filename removes asterisks.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetSafeFilenameRemovesAsterisks() throws Exception {
    Method method = BaseReportActionHandler.class.getDeclaredMethod(GET_SAFE_FILENAME, String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(null, "report*file");
    assertEquals(REPORT_FILE, result);
  }
  /**
   * Get safe filename removes question mark.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetSafeFilenameRemovesQuestionMark() throws Exception {
    Method method = BaseReportActionHandler.class.getDeclaredMethod(GET_SAFE_FILENAME, String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(null, "report?file");
    assertEquals(REPORT_FILE, result);
  }
  /**
   * Get safe filename removes pipe and angle brackets.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetSafeFilenameRemovesPipeAndAngleBrackets() throws Exception {
    Method method = BaseReportActionHandler.class.getDeclaredMethod(GET_SAFE_FILENAME, String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(null, "report|<file>");
    assertEquals("report__file_", result);
  }
  /**
   * Get safe filename keeps valid chars.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetSafeFilenameKeepsValidChars() throws Exception {
    Method method = BaseReportActionHandler.class.getDeclaredMethod(GET_SAFE_FILENAME, String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(null, "Sales Report-2024_01");
    assertEquals("Sales Report-2024_01", result);
  }
  /**
   * Post actions with record id returns empty actions.
   * @throws Exception if an error occurs
   */

  @Test
  public void testPostActionsWithRecordIdReturnsEmptyActions() throws Exception {
    Map<String, Object> jrParams = new HashMap<>();
    jrParams.put("record_id", "someRecordId");
    JSONObject result = new JSONObject();

    instance.postActions(jrParams, result);

    JSONArray actions = result.getJSONArray(RESPONSE_ACTIONS);
    assertEquals(0, actions.length());
  }
  /**
   * Post actions default success message.
   * @throws Exception if an error occurs
   */

  @Test
  public void testPostActionsDefaultSuccessMessage() throws Exception {
    Map<String, Object> jrParams = new HashMap<>();
    JSONObject result = new JSONObject();

    instance.postActions(jrParams, result);

    JSONArray actions = result.getJSONArray(RESPONSE_ACTIONS);
    assertEquals(1, actions.length());
    JSONObject action = actions.getJSONObject(0);
    JSONObject showMsg = action.getJSONObject("showMsgInProcessView");
    assertEquals("success", showMsg.getString("msgType"));
    assertEquals("Report generated successfully", showMsg.getString("msgText"));
  }
  /**
   * Post actions with post action override.
   * @throws Exception if an error occurs
   */

  @Test
  public void testPostActionsWithPostActionOverride() throws Exception {
    Map<String, Object> jrParams = new HashMap<>();
    OBError obError = new OBError();
    obError.setType("warning");
    obError.setMessage("Custom warning");
    jrParams.put("postAction", obError);
    JSONObject result = new JSONObject();

    instance.postActions(jrParams, result);

    JSONArray actions = result.getJSONArray(RESPONSE_ACTIONS);
    assertEquals(1, actions.length());
    JSONObject action = actions.getJSONObject(0);
    JSONObject showMsg = action.getJSONObject("showMsgInProcessView");
    assertEquals("warning", showMsg.getString("msgType"));
    assertEquals("Custom warning", showMsg.getString("msgText"));
  }
  /**
   * Handle custom action throws by default.
   * @throws Exception if an error occurs
   */

  @Test(expected = OBException.class)
  public void testHandleCustomActionThrowsByDefault() throws Exception {
    obMessageUtilsStatic.when(() -> OBMessageUtils.getI18NMessage(
        eq("OBUIAPP_UnsupportedAction"), any(String[].class)))
        .thenReturn("Unsupported action: customAction");
    instance.handleCustomAction(new JSONObject(), new HashMap<>(), new JSONObject(), "customAction");
  }
  /** Is compiling subreports returns true. */

  @Test
  public void testIsCompilingSubreportsReturnsTrue() {
    assertTrue(instance.isCompilingSubreports());
  }

  /**
   * Concrete subclass for testing the abstract BaseReportActionHandler.
   */
  private static class ConcreteReportHandler extends BaseReportActionHandler {
    @Override
    protected JSONObject doExecute(Map<String, Object> parameters, String content) {
      return new JSONObject();
    }
  }
}
