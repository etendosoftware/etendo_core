package org.openbravo.client.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.query.NativeQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.service.json.JsonConstants;

/**
 * Tests for {@link ADAlertDatasourceService}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ADAlertDatasourceServiceTest {

  private static final String AD_TABLE_ID = "594";
  private static final String ALERT_STATUS_KEY = "_alertStatus";
  private static final String TEST_USER_ID = "100";
  private static final String TEST_ROLE_ID = "200";

  private ADAlertDatasourceService instance;

  @Mock
  private OBDal mockOBDal;
  @Mock
  private OBContext mockOBContext;
  @Mock
  private ModelProvider mockModelProvider;
  @Mock
  private Entity mockEntity;
  @Mock
  private Session mockSession;
  @Mock
  private RequestContext mockRequestContext;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<ModelProvider> modelProviderStatic;
  private MockedStatic<RequestContext> requestContextStatic;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = spy(objenesis.newInstance(ADAlertDatasourceService.class));

    obDalStatic = mockStatic(OBDal.class);
    obContextStatic = mockStatic(OBContext.class);
    modelProviderStatic = mockStatic(ModelProvider.class);
    requestContextStatic = mockStatic(RequestContext.class);

    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
    obContextStatic.when(OBContext::getOBContext).thenReturn(mockOBContext);
    modelProviderStatic.when(ModelProvider::getInstance).thenReturn(mockModelProvider);
    requestContextStatic.when(RequestContext::get).thenReturn(mockRequestContext);

    lenient().when(mockOBDal.getSession()).thenReturn(mockSession);

    User mockUser = mock(User.class);
    Role mockRole = mock(Role.class);
    lenient().when(mockOBContext.getUser()).thenReturn(mockUser);
    lenient().when(mockOBContext.getRole()).thenReturn(mockRole);
    lenient().when(mockUser.getId()).thenReturn(TEST_USER_ID);
    lenient().when(mockRole.getId()).thenReturn(TEST_ROLE_ID);
    lenient().when(mockOBContext.getReadableClients()).thenReturn(new String[] { "0", "1000000" });
    lenient().when(mockOBContext.getReadableOrganizations())
        .thenReturn(new String[] { "0", "1000001" });
  }

  @After
  public void tearDown() {
    if (obDalStatic != null)
      obDalStatic.close();
    if (obContextStatic != null)
      obContextStatic.close();
    if (modelProviderStatic != null)
      modelProviderStatic.close();
    if (requestContextStatic != null)
      requestContextStatic.close();
  }

  // --- getEntity() tests ---

  @Test
  public void testGetEntityReturnsEntityByTableId() {
    when(mockModelProvider.getEntityByTableId(AD_TABLE_ID)).thenReturn(mockEntity);

    Entity result = instance.getEntity();

    assertEquals(mockEntity, result);
    verify(mockModelProvider).getEntityByTableId(AD_TABLE_ID);
  }

  // --- checkFetchDatasourceAccess() tests ---

  @Test
  public void testCheckFetchDatasourceAccessDoesNotThrow() {
    Map<String, String> params = new HashMap<>();
    instance.checkFetchDatasourceAccess(params);
    // Should complete without exception - method is intentionally empty
  }

  // --- getAlertRulesGroupedByFilterClause() tests (private method via reflection) ---

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testGetAlertRulesGroupedByFilterClauseWithResults() throws Exception {
    NativeQuery mockAlertRulesQuery = mock(NativeQuery.class);

    Object[] row1 = new Object[] { "RULE001", "filterA" };
    Object[] row2 = new Object[] { "RULE002", "filterA" };
    Object[] row3 = new Object[] { "RULE003", null };

    List resultList = Arrays.asList(row1, row2, row3);
    when(mockAlertRulesQuery.list()).thenReturn(resultList);

    Method method = ADAlertDatasourceService.class.getDeclaredMethod(
        "getAlertRulesGroupedByFilterClause", NativeQuery.class);
    method.setAccessible(true);

    Map<String, List<String>> result = (Map<String, List<String>>) method.invoke(instance,
        mockAlertRulesQuery);

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(Arrays.asList("RULE001", "RULE002"), result.get("filterA"));
    assertEquals(Collections.singletonList("RULE003"), result.get(""));
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testGetAlertRulesGroupedByFilterClauseEmptyResults() throws Exception {
    NativeQuery mockAlertRulesQuery = mock(NativeQuery.class);
    when(mockAlertRulesQuery.list()).thenReturn(Collections.emptyList());

    Method method = ADAlertDatasourceService.class.getDeclaredMethod(
        "getAlertRulesGroupedByFilterClause", NativeQuery.class);
    method.setAccessible(true);

    Map<String, List<String>> result = (Map<String, List<String>>) method.invoke(instance,
        mockAlertRulesQuery);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testGetAlertRulesGroupedByFilterClauseHandlesSQLGrammarException() throws Exception {
    NativeQuery mockAlertRulesQuery = mock(NativeQuery.class);
    when(mockAlertRulesQuery.list())
        .thenThrow(new SQLGrammarException("Bad SQL", new SQLException("syntax error")));

    Method method = ADAlertDatasourceService.class.getDeclaredMethod(
        "getAlertRulesGroupedByFilterClause", NativeQuery.class);
    method.setAccessible(true);

    Map<String, List<String>> result = (Map<String, List<String>>) method.invoke(instance,
        mockAlertRulesQuery);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testGetAlertRulesGroupedByFilterClauseMultipleFilterClauses() throws Exception {
    NativeQuery mockAlertRulesQuery = mock(NativeQuery.class);

    Object[] row1 = new Object[] { "RULE001", "clause1" };
    Object[] row2 = new Object[] { "RULE002", "clause2" };
    Object[] row3 = new Object[] { "RULE003", "clause1" };
    Object[] row4 = new Object[] { "RULE004", "clause3" };

    List resultList = Arrays.asList(row1, row2, row3, row4);
    when(mockAlertRulesQuery.list()).thenReturn(resultList);

    Method method = ADAlertDatasourceService.class.getDeclaredMethod(
        "getAlertRulesGroupedByFilterClause", NativeQuery.class);
    method.setAccessible(true);

    Map<String, List<String>> result = (Map<String, List<String>>) method.invoke(instance,
        mockAlertRulesQuery);

    assertEquals(3, result.size());
    assertEquals(Arrays.asList("RULE001", "RULE003"), result.get("clause1"));
    assertEquals(Collections.singletonList("RULE002"), result.get("clause2"));
    assertEquals(Collections.singletonList("RULE004"), result.get("clause3"));
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testGetAlertRulesGroupedByFilterClauseNullFilterClauseGroupedAsEmpty()
      throws Exception {
    NativeQuery mockAlertRulesQuery = mock(NativeQuery.class);

    Object[] row1 = new Object[] { "RULE001", null };
    Object[] row2 = new Object[] { "RULE002", null };

    List resultList = Arrays.asList(row1, row2);
    when(mockAlertRulesQuery.list()).thenReturn(resultList);

    Method method = ADAlertDatasourceService.class.getDeclaredMethod(
        "getAlertRulesGroupedByFilterClause", NativeQuery.class);
    method.setAccessible(true);

    Map<String, List<String>> result = (Map<String, List<String>>) method.invoke(instance,
        mockAlertRulesQuery);

    assertEquals(1, result.size());
    assertEquals(Arrays.asList("RULE001", "RULE002"), result.get(""));
  }

  // --- getWhereAndFilterClause() tests (protected, via reflection) ---

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testGetWhereAndFilterClauseEmptyAlertList() throws Exception {
    // Mock the full chain so getAlertIds returns empty
    NativeQuery mockNativeQuery = mock(NativeQuery.class);
    when(mockSession.createNativeQuery(anyString())).thenReturn(mockNativeQuery);
    when(mockNativeQuery.setParameter(anyString(), any())).thenReturn(mockNativeQuery);
    when(mockNativeQuery.setParameterList(anyString(), any(String[].class)))
        .thenReturn(mockNativeQuery);
    when(mockNativeQuery.setParameterList(anyString(), any(java.util.Collection.class)))
        .thenReturn(mockNativeQuery);
    when(mockNativeQuery.list()).thenReturn(Collections.emptyList());

    Method method = ADAlertDatasourceService.class.getDeclaredMethod("getWhereAndFilterClause",
        Map.class);
    method.setAccessible(true);

    Map<String, String> parameters = new HashMap<>();
    String result = (String) method.invoke(instance, parameters);

    assertEquals("1 = 2", result);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testGetWhereAndFilterClauseSmallList() throws Exception {
    // Mock so getAlertIds returns a small list (< 1000)
    NativeQuery mockNativeQuery = mock(NativeQuery.class);
    when(mockSession.createNativeQuery(anyString())).thenReturn(mockNativeQuery);
    when(mockNativeQuery.setParameter(anyString(), any())).thenReturn(mockNativeQuery);
    when(mockNativeQuery.setParameterList(anyString(), any(String[].class)))
        .thenReturn(mockNativeQuery);
    when(mockNativeQuery.setParameterList(anyString(), any(java.util.Collection.class)))
        .thenReturn(mockNativeQuery);

    // First query returns alert rules, second returns alert IDs
    Object[] alertRule = new Object[] { "RULE001", "" };
    List alertRulesList = Collections.singletonList(alertRule);
    List<String> alertIds = Arrays.asList("'A1'", "'A2'", "'A3'");

    VariablesSecureApp mockVars = mock(VariablesSecureApp.class);
    when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);

    when(mockNativeQuery.list())
        .thenReturn(alertRulesList)
        .thenReturn(alertIds);

    Method method = ADAlertDatasourceService.class.getDeclaredMethod("getWhereAndFilterClause",
        Map.class);
    method.setAccessible(true);

    Map<String, String> parameters = new HashMap<>();
    String result = (String) method.invoke(instance, parameters);

    assertTrue(result.startsWith("e.id in ("));
  }

  // --- getAlertIdsFromAlertRules() tests ---

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testGetAlertIdsFromAlertRulesEmptyMap() throws Exception {
    Map<String, List<String>> emptyMap = new HashMap<>();

    Method method = ADAlertDatasourceService.class.getDeclaredMethod(
        "getAlertIdsFromAlertRules", Map.class, String.class);
    method.setAccessible(true);

    List<String> result = (List<String>) method.invoke(instance, emptyMap, "NEW");

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testGetAlertIdsFromAlertRulesHandlesSQLGrammarException() throws Exception {
    Map<String, List<String>> alertRulesMap = new HashMap<>();
    alertRulesMap.put("", Arrays.asList("RULE001"));

    NativeQuery mockNativeQuery = mock(NativeQuery.class);
    when(mockSession.createNativeQuery(anyString())).thenReturn(mockNativeQuery);
    when(mockNativeQuery.setParameter(anyString(), any())).thenReturn(mockNativeQuery);
    when(mockNativeQuery.setParameterList(anyString(), any(String[].class)))
        .thenReturn(mockNativeQuery);
    when(mockNativeQuery.setParameterList(anyString(), any(java.util.Collection.class)))
        .thenReturn(mockNativeQuery);

    // Mock UsedByLink
    VariablesSecureApp mockVars = mock(
        VariablesSecureApp.class);
    when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);

    when(mockNativeQuery.list())
        .thenThrow(new SQLGrammarException("Bad SQL", new SQLException("error")));

    Method method = ADAlertDatasourceService.class.getDeclaredMethod(
        "getAlertIdsFromAlertRules", Map.class, String.class);
    method.setAccessible(true);

    List<String> result = (List<String>) method.invoke(instance, alertRulesMap, "NEW");

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testGetAlertIdsFromAlertRulesReturnsAlertIds() throws Exception {
    Map<String, List<String>> alertRulesMap = new HashMap<>();
    alertRulesMap.put("", Arrays.asList("RULE001"));

    NativeQuery mockNativeQuery = mock(NativeQuery.class);
    when(mockSession.createNativeQuery(anyString())).thenReturn(mockNativeQuery);
    when(mockNativeQuery.setParameter(anyString(), any())).thenReturn(mockNativeQuery);
    when(mockNativeQuery.setParameterList(anyString(), any(String[].class)))
        .thenReturn(mockNativeQuery);
    when(mockNativeQuery.setParameterList(anyString(), any(java.util.Collection.class)))
        .thenReturn(mockNativeQuery);

    VariablesSecureApp mockVars = mock(
        VariablesSecureApp.class);
    when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);

    List<String> expectedAlerts = Arrays.asList("ALERT001", "ALERT002");
    when(mockNativeQuery.list()).thenReturn(expectedAlerts);

    Method method = ADAlertDatasourceService.class.getDeclaredMethod(
        "getAlertIdsFromAlertRules", Map.class, String.class);
    method.setAccessible(true);

    List<String> result = (List<String>) method.invoke(instance, alertRulesMap, "NEW");

    assertEquals(2, result.size());
    assertTrue(result.contains("ALERT001"));
    assertTrue(result.contains("ALERT002"));
  }
}
