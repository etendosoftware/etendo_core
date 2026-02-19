package org.openbravo.costing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DbUtility;
/** Tests for {@link CostingRuleProcessActionHandler}. */
@SuppressWarnings("java:S112")

@RunWith(MockitoJUnitRunner.class)
public class CostingRuleProcessActionHandlerTest {

  private static final String ERROR = "Error";
  private static final String MESSAGE = "message";

  private static final String PROCESS_ID = "TEST_PROCESS_001";

  private CostingRuleProcessActionHandler instance;

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
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(CostingRuleProcessActionHandler.class);

    requestContextStatic = mockStatic(RequestContext.class);
    requestContextStatic.when(RequestContext::get).thenReturn(mockRequestContext);
    when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);

    obContextStatic = mockStatic(OBContext.class);

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obMessageUtilsStatic = mockStatic(OBMessageUtils.class);
    dbUtilityStatic = mockStatic(DbUtility.class);

    lenient().when(mockVars.getStringParameter("inpadOrgId")).thenReturn("0");
    lenient().when(mockVars.getStringParameter("inpadClientId")).thenReturn("0");
    lenient().when(mockVars.getLanguage()).thenReturn("en_US");
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (requestContextStatic != null) requestContextStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (obDalStatic != null) obDalStatic.close();
    if (obMessageUtilsStatic != null) obMessageUtilsStatic.close();
    if (dbUtilityStatic != null) dbUtilityStatic.close();
  }

  private JSONObject invokeDoExecute(Map<String, Object> parameters, String content) throws Exception {
    Method method = CostingRuleProcessActionHandler.class.getDeclaredMethod("doExecute",
        Map.class, String.class);
    method.setAccessible(true);
    return (JSONObject) method.invoke(instance, parameters, content);
  }
  /**
   * Do execute with exception returns error message.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoExecuteWithExceptionReturnsErrorMessage() throws Exception {
    OBError translatedError = new OBError();
    translatedError.setMessage("Translated error");
    obMessageUtilsStatic.when(() -> OBMessageUtils.translateError(anyString()))
        .thenReturn(translatedError);
    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD(ERROR))
        .thenReturn(ERROR);
    dbUtilityStatic.when(() -> DbUtility.getUnderlyingSQLException(any(Exception.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("processId", PROCESS_ID);

    // Invalid JSON will cause an exception
    JSONObject result = invokeDoExecute(parameters, "invalid json");

    assertNotNull(result);
    assertTrue(result.has(MESSAGE));
    JSONObject msg = result.getJSONObject(MESSAGE);
    assertEquals("error", msg.getString("severity"));
  }
  /**
   * Do execute with missing rule id returns error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoExecuteWithMissingRuleIdReturnsError() throws Exception {
    OBError translatedError = new OBError();
    translatedError.setMessage("Missing rule id");
    obMessageUtilsStatic.when(() -> OBMessageUtils.translateError(anyString()))
        .thenReturn(translatedError);
    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD(ERROR))
        .thenReturn(ERROR);
    dbUtilityStatic.when(() -> DbUtility.getUnderlyingSQLException(any(Exception.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("processId", PROCESS_ID);

    JSONObject content = new JSONObject();
    // Missing M_Costing_Rule_ID will cause JSONException

    JSONObject result = invokeDoExecute(parameters, content.toString());

    assertNotNull(result);
    assertTrue(result.has(MESSAGE));
    JSONObject msg = result.getJSONObject(MESSAGE);
    assertEquals("error", msg.getString("severity"));
  }
}
