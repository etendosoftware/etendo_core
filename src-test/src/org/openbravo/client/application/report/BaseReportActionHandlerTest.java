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

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (obMessageUtilsStatic != null) obMessageUtilsStatic.close();
  }

  @Test
  public void testGetSafeFilenameRemovesColons() throws Exception {
    Method method = BaseReportActionHandler.class.getDeclaredMethod("getSafeFilename", String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(null, "report:2024");
    assertEquals("report_2024", result);
  }

  @Test
  public void testGetSafeFilenameRemovesBackslashes() throws Exception {
    Method method = BaseReportActionHandler.class.getDeclaredMethod("getSafeFilename", String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(null, "report\\file");
    assertEquals("report_file", result);
  }

  @Test
  public void testGetSafeFilenameRemovesSlashes() throws Exception {
    Method method = BaseReportActionHandler.class.getDeclaredMethod("getSafeFilename", String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(null, "report/file");
    assertEquals("report_file", result);
  }

  @Test
  public void testGetSafeFilenameRemovesAsterisks() throws Exception {
    Method method = BaseReportActionHandler.class.getDeclaredMethod("getSafeFilename", String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(null, "report*file");
    assertEquals("report_file", result);
  }

  @Test
  public void testGetSafeFilenameRemovesQuestionMark() throws Exception {
    Method method = BaseReportActionHandler.class.getDeclaredMethod("getSafeFilename", String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(null, "report?file");
    assertEquals("report_file", result);
  }

  @Test
  public void testGetSafeFilenameRemovesPipeAndAngleBrackets() throws Exception {
    Method method = BaseReportActionHandler.class.getDeclaredMethod("getSafeFilename", String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(null, "report|<file>");
    assertEquals("report__file_", result);
  }

  @Test
  public void testGetSafeFilenameKeepsValidChars() throws Exception {
    Method method = BaseReportActionHandler.class.getDeclaredMethod("getSafeFilename", String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(null, "Sales Report-2024_01");
    assertEquals("Sales Report-2024_01", result);
  }

  @Test
  public void testPostActionsWithRecordIdReturnsEmptyActions() throws Exception {
    Map<String, Object> jrParams = new HashMap<>();
    jrParams.put("record_id", "someRecordId");
    JSONObject result = new JSONObject();

    instance.postActions(jrParams, result);

    JSONArray actions = result.getJSONArray("responseActions");
    assertEquals(0, actions.length());
  }

  @Test
  public void testPostActionsDefaultSuccessMessage() throws Exception {
    Map<String, Object> jrParams = new HashMap<>();
    JSONObject result = new JSONObject();

    instance.postActions(jrParams, result);

    JSONArray actions = result.getJSONArray("responseActions");
    assertEquals(1, actions.length());
    JSONObject action = actions.getJSONObject(0);
    JSONObject showMsg = action.getJSONObject("showMsgInProcessView");
    assertEquals("success", showMsg.getString("msgType"));
    assertEquals("Report generated successfully", showMsg.getString("msgText"));
  }

  @Test
  public void testPostActionsWithPostActionOverride() throws Exception {
    Map<String, Object> jrParams = new HashMap<>();
    OBError obError = new OBError();
    obError.setType("warning");
    obError.setMessage("Custom warning");
    jrParams.put("postAction", obError);
    JSONObject result = new JSONObject();

    instance.postActions(jrParams, result);

    JSONArray actions = result.getJSONArray("responseActions");
    assertEquals(1, actions.length());
    JSONObject action = actions.getJSONObject(0);
    JSONObject showMsg = action.getJSONObject("showMsgInProcessView");
    assertEquals("warning", showMsg.getString("msgType"));
    assertEquals("Custom warning", showMsg.getString("msgText"));
  }

  @Test(expected = OBException.class)
  public void testHandleCustomActionThrowsByDefault() throws Exception {
    obMessageUtilsStatic.when(() -> OBMessageUtils.getI18NMessage(
        eq("OBUIAPP_UnsupportedAction"), any(String[].class)))
        .thenReturn("Unsupported action: customAction");
    instance.handleCustomAction(new JSONObject(), new HashMap<>(), new JSONObject(), "customAction");
  }

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
