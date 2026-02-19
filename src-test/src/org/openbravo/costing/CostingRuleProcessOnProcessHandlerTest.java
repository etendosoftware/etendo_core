package org.openbravo.costing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.DbUtility;

@RunWith(MockitoJUnitRunner.class)
public class CostingRuleProcessOnProcessHandlerTest {

  private static final String RULE_ID = "TEST_RULE_001";

  private CostingRuleProcessOnProcessHandler instance;

  @Mock
  private RequestContext mockRequestContext;

  @Mock
  private VariablesSecureApp mockVars;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBContext mockOBContext;

  private MockedStatic<RequestContext> requestContextStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBMessageUtils> obMessageUtilsStatic;
  private MockedStatic<DbUtility> dbUtilityStatic;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(CostingRuleProcessOnProcessHandler.class);

    requestContextStatic = mockStatic(RequestContext.class);
    requestContextStatic.when(RequestContext::get).thenReturn(mockRequestContext);
    when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);

    obContextStatic = mockStatic(OBContext.class);
    obContextStatic.when(OBContext::getOBContext).thenReturn(mockOBContext);

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obMessageUtilsStatic = mockStatic(OBMessageUtils.class);
    dbUtilityStatic = mockStatic(DbUtility.class);

    lenient().when(mockVars.getLanguage()).thenReturn("en_US");
  }

  @After
  public void tearDown() {
    if (requestContextStatic != null) requestContextStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (obDalStatic != null) obDalStatic.close();
    if (obMessageUtilsStatic != null) obMessageUtilsStatic.close();
    if (dbUtilityStatic != null) dbUtilityStatic.close();
  }

  private JSONObject invokeExecute(Map<String, Object> parameters, String content) throws Exception {
    Method method = CostingRuleProcessOnProcessHandler.class.getDeclaredMethod("execute",
        Map.class, String.class);
    method.setAccessible(true);
    return (JSONObject) method.invoke(instance, parameters, content);
  }

  @Test
  public void testExecuteWithInvalidJsonReturnsError() throws Exception {
    OBError translatedError = new OBError();
    translatedError.setMessage("Invalid JSON");
    obMessageUtilsStatic.when(() -> OBMessageUtils.translateError(anyString()))
        .thenReturn(translatedError);
    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD("Error"))
        .thenReturn("Error");
    dbUtilityStatic.when(() -> DbUtility.getUnderlyingSQLException(any(Exception.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    Map<String, Object> parameters = new HashMap<>();

    JSONObject result = invokeExecute(parameters, "invalid json");

    assertNotNull(result);
    assertTrue(result.has("message"));
    JSONObject msg = result.getJSONObject("message");
    assertEquals("error", msg.getString("severity"));
  }

  @Test
  public void testExecuteWithMissingRuleIdReturnsError() throws Exception {
    OBError translatedError = new OBError();
    translatedError.setMessage("Missing ruleId");
    obMessageUtilsStatic.when(() -> OBMessageUtils.translateError(anyString()))
        .thenReturn(translatedError);
    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD("Error"))
        .thenReturn("Error");
    dbUtilityStatic.when(() -> DbUtility.getUnderlyingSQLException(any(Exception.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    Map<String, Object> parameters = new HashMap<>();
    JSONObject content = new JSONObject();

    JSONObject result = invokeExecute(parameters, content.toString());

    assertNotNull(result);
    assertTrue(result.has("message"));
    JSONObject msg = result.getJSONObject("message");
    assertEquals("error", msg.getString("severity"));
  }

  @Test
  public void testExecuteWithNullRuleReturnsError() throws Exception {
    OBError translatedError = new OBError();
    translatedError.setMessage("Null rule");
    obMessageUtilsStatic.when(() -> OBMessageUtils.translateError(any()))
        .thenReturn(translatedError);
    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD("Error"))
        .thenReturn("Error");

    NullPointerException npe = new NullPointerException("rule is null");
    dbUtilityStatic.when(() -> DbUtility.getUnderlyingSQLException(any(Exception.class)))
        .thenReturn(npe);

    when(mockOBDal.get(any(Class.class), anyString())).thenReturn(null);

    Map<String, Object> parameters = new HashMap<>();
    JSONObject content = new JSONObject();
    content.put("ruleId", RULE_ID);

    JSONObject result = invokeExecute(parameters, content.toString());

    assertNotNull(result);
    assertTrue(result.has("message"));
    JSONObject msg = result.getJSONObject("message");
    assertEquals("error", msg.getString("severity"));
  }
}
