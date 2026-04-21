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
import java.lang.reflect.InvocationTargetException;

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
/** Tests for {@link CostingRuleProcessOnProcessHandler}. */
@SuppressWarnings("java:S112")

@RunWith(MockitoJUnitRunner.class)
public class CostingRuleProcessOnProcessHandlerTest {

  private static final String ERROR = "Error";
  private static final String MESSAGE = "message";
  private static final String SEVERITY = "severity";
  private static final String ERROR_2 = "error";

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
  /** Sets up test fixtures. */

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
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (requestContextStatic != null) requestContextStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (obDalStatic != null) obDalStatic.close();
    if (obMessageUtilsStatic != null) obMessageUtilsStatic.close();
    if (dbUtilityStatic != null) dbUtilityStatic.close();
  }

  private JSONObject invokeExecute(Map<String, Object> parameters, String content) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = CostingRuleProcessOnProcessHandler.class.getDeclaredMethod("execute",
        Map.class, String.class);
    method.setAccessible(true);
    return (JSONObject) method.invoke(instance, parameters, content);
  }
  /**
   * Execute with invalid json returns error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithInvalidJsonReturnsError() throws Exception {
    setupErrorMocks("Invalid JSON");
    dbUtilityStatic.when(() -> DbUtility.getUnderlyingSQLException(any(Exception.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    JSONObject result = invokeExecute(new HashMap<>(), "invalid json");

    assertErrorResult(result);
  }
  /**
   * Execute with missing rule id returns error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithMissingRuleIdReturnsError() throws Exception {
    setupErrorMocks("Missing ruleId");
    dbUtilityStatic.when(() -> DbUtility.getUnderlyingSQLException(any(Exception.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    JSONObject content = new JSONObject();

    JSONObject result = invokeExecute(new HashMap<>(), content.toString());

    assertErrorResult(result);
  }
  /**
   * Execute with null rule returns error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithNullRuleReturnsError() throws Exception {
    setupErrorMocks("Null rule");

    NullPointerException npe = new NullPointerException("rule is null");
    dbUtilityStatic.when(() -> DbUtility.getUnderlyingSQLException(any(Exception.class)))
        .thenReturn(npe);

    when(mockOBDal.get(any(Class.class), anyString())).thenReturn(null);

    JSONObject content = new JSONObject();
    content.put("ruleId", RULE_ID);

    JSONObject result = invokeExecute(new HashMap<>(), content.toString());

    assertErrorResult(result);
  }

  private void setupErrorMocks(String errorMessage) {
    OBError translatedError = new OBError();
    translatedError.setMessage(errorMessage);
    obMessageUtilsStatic.when(() -> OBMessageUtils.translateError(any()))
        .thenReturn(translatedError);
    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD(ERROR))
        .thenReturn(ERROR);
  }

  private void assertErrorResult(JSONObject result) throws Exception {
    assertNotNull(result);
    assertTrue(result.has(MESSAGE));
    JSONObject msg = result.getJSONObject(MESSAGE);
    assertEquals(ERROR_2, msg.getString(SEVERITY));
  }
}
