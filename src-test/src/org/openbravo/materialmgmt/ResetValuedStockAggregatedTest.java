package org.openbravo.materialmgmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.model.materialmgmt.onhandquantity.ValuedStockAggregated;

/**
 * Test class for {@link ResetValuedStockAggregated}.
 * <p>
 * This class includes unit tests for the behavior of the
 * ResetValuedStockAggregated process in different scenarios.
 */
public class ResetValuedStockAggregatedTest {

  private static final String SUCCESS = "Success";
  private static final String TEST_ORG_ID = "testOrgId";
  private static final String TEST_CLIENT_ID = "testClientId";
  private static final String DATE_2022_01_01 = "2022-01-01";

  // Mock static classes
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<OBDateUtils> mockedOBDateUtils;
  private MockedStatic<Utility> mockedUtility;
  private MockedStatic<GenerateValuedStockAggregatedData> mockedGenerateValuedStockAggregatedData;
  private AutoCloseable mocks;

  // Mock dependencies
  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBContext mockOBContext;

  @Mock
  private OBCriteria<ValuedStockAggregated> mockOBCriteria;

  @Mock
  private OBQuery<Period> mockOBQueryPeriod;

  @Mock
  private OBQuery<CostingRule> mockOBQueryCostingRule;

  @Mock
  private Session mockSession;

  @Mock
  private Query<Date> mockQuery;

  @Mock
  private OrganizationStructureProvider mockOSP;

  @Mock
  private Organization mockLegalEntity;

  @Mock
  private Client mockClient;

  @Mock
  private Currency mockCurrency;

  @Mock
  private Period mockPeriod;

  @Mock
  private CostingRule mockCostingRule;

  private ResetValuedStockAggregated processUnderTest;

  /**
   * Initializes mocks and static classes before each test.
   *
   * @throws Exception
   *     if there is an error during the setup process
   */
  @Before
  public void setUp() throws Exception {
    Mockito.framework().clearInlineMocks();
    mocks = MockitoAnnotations.openMocks(this);
    processUnderTest = new ResetValuedStockAggregated();

    // Initialize static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedOBDateUtils = mockStatic(OBDateUtils.class);
    mockedUtility = mockStatic(Utility.class);
    mockedGenerateValuedStockAggregatedData = mockStatic(GenerateValuedStockAggregatedData.class);

    // Configure static mocks
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(SUCCESS)).thenReturn(SUCCESS);
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("ErrorAggregatingData")).thenReturn(
        "Error Aggregating Data");
    mockedOBDateUtils.when(() -> OBDateUtils.formatDate(any(Date.class))).thenReturn(DATE_2022_01_01);

    // Configure mockOBDal
    when(mockOBDal.getConnection()).thenReturn(null);
    when(mockOBDal.get(Organization.class, TEST_ORG_ID)).thenReturn(mockLegalEntity);
    when(mockOBDal.getSession()).thenReturn(mockSession);
    when(mockOBDal.createCriteria(ValuedStockAggregated.class)).thenReturn(mockOBCriteria);
    when(mockOBDal.createQuery(eq(Period.class), anyString())).thenReturn(mockOBQueryPeriod);
    when(mockOBDal.createQuery(eq(CostingRule.class), anyString())).thenReturn(mockOBQueryCostingRule);

    // Configure criteria and query
    when(mockOBCriteria.add(any())).thenReturn(mockOBCriteria);
    when(mockOBCriteria.uniqueResult()).thenReturn(null);
    when(mockOBCriteria.list()).thenReturn(new ArrayList<>());

    when(mockOBQueryPeriod.setNamedParameter(anyString(), any())).thenReturn(mockOBQueryPeriod);
    when(mockOBQueryCostingRule.setNamedParameter(anyString(), any())).thenReturn(mockOBQueryCostingRule);

    // Configure session and query
    when(mockSession.createQuery(anyString(), eq(Date.class))).thenReturn(mockQuery);
    when(mockSession.createQuery(anyString())).thenReturn(mockQuery);
    when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
    when(mockQuery.setParameterList(anyString(), any(Collection.class))).thenReturn(mockQuery);
    when(mockQuery.setMaxResults(anyInt())).thenReturn(mockQuery);
    when(mockQuery.executeUpdate()).thenReturn(0);

    List<Date> dateList = new ArrayList<>();
    DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    dateList.add(formatter.parse("01-01-9999"));
    when(mockQuery.list()).thenReturn(dateList);

    // Configure mock organization structure provider
    when(mockOBContext.getOrganizationStructureProvider(anyString())).thenReturn(mockOSP);
    Set<String> orgIds = new HashSet<>();
    orgIds.add(TEST_ORG_ID);
    when(mockOSP.getNaturalTree(anyString())).thenReturn(orgIds);
    when(mockOSP.getLegalEntity(any(Organization.class))).thenReturn(mockLegalEntity);

    // Configure mock utility
    mockedUtility.when(() -> Utility.getInStrSet(any())).thenReturn("('testOrgId')");

    // Configure mock legal entity and client
    when(mockLegalEntity.getId()).thenReturn(TEST_ORG_ID);
    when(mockLegalEntity.getClient()).thenReturn(mockClient);
    when(mockLegalEntity.getCurrency()).thenReturn(mockCurrency);
    when(mockClient.getId()).thenReturn(TEST_CLIENT_ID);
    when(mockCurrency.getId()).thenReturn("testCurrencyId");

    // Configure mock period
    when(mockPeriod.getId()).thenReturn("testPeriodId");
    Date testDate = formatter.parse("01-01-2022");
    when(mockPeriod.getStartingDate()).thenReturn(testDate);
    when(mockPeriod.getEndingDate()).thenReturn(testDate);

    // Configure mock costing rule
    when(mockCostingRule.getId()).thenReturn("testCostingRuleId");
    when(mockCostingRule.getStartingDate()).thenReturn(null);
    when(mockCostingRule.getEndingDate()).thenReturn(null);
  }

  /**
   * Closes mocked static instances after each test.
   */
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
    if (mockedUtility != null) {
      mockedUtility.close();
    }
    if (mockedGenerateValuedStockAggregatedData != null) {
      mockedGenerateValuedStockAggregatedData.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Tests the doExecute method successfully completes and returns a success message.
   *
   * @throws Exception
   *     if there is an error during the test
   */
  @Test
  public void testDoExecuteSuccess() throws Exception {
    // GIVEN
    Map<String, Object> parameters = new HashMap<>();
    String content = "{\"_params\": {\"ad_org_id\": \"testOrgId\"}}";

    List<Period> periods = new ArrayList<>();
    periods.add(mockPeriod);
    when(mockOBQueryPeriod.list()).thenReturn(periods);

    // Mock costing rules
    List<CostingRule> costingRules = new ArrayList<>();
    costingRules.add(mockCostingRule);
    when(mockOBQueryCostingRule.list()).thenReturn(costingRules);

    mockedGenerateValuedStockAggregatedData.when(
        () -> GenerateValuedStockAggregatedData.insertData(any(), any(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString())).thenAnswer(invocation -> null);

    Method noAggregatedDataForPeriodMethod = ResetValuedStockAggregated.class.getDeclaredMethod(
        "noAggregatedDataForPeriod", Period.class);
    noAggregatedDataForPeriodMethod.setAccessible(true);

    Method costingRuleDefindedForPeriodMethod = ResetValuedStockAggregated.class.getDeclaredMethod(
        "costingRuleDefindedForPeriod", Organization.class, Period.class);
    costingRuleDefindedForPeriodMethod.setAccessible(true);

    // WHEN
    JSONObject result = processUnderTest.doExecute(parameters, content);

    // THEN
    assertTrue(result.getBoolean("retryExecution"));
    JSONObject message = result.getJSONObject("message");
    assertEquals("success", message.getString("severity"));
    assertEquals(SUCCESS, message.getString("text"));
  }

  /**
   * Tests the doExecute method when an exception occurs during execution.
   *
   * @throws Exception
   *     if there is an error during the test
   */
  @Test
  public void testDoExecuteWithException() throws Exception {
    // GIVEN
    Map<String, Object> parameters = new HashMap<>();
    String content = "{\"_params\": {\"ad_org_id\": \"testOrgId\"}}";

    // Force an exception during execution
    when(mockOBDal.get(Organization.class, TEST_ORG_ID)).thenThrow(new RuntimeException("Test exception"));

    // WHEN
    JSONObject result = processUnderTest.doExecute(parameters, content);

    // THEN
    JSONObject message = result.getJSONObject("message");
    assertEquals("error", message.getString("severity"));
    assertEquals("Error Aggregating Data", message.getString("text"));

  }

  /**
   * Tests the costingRuleDefindedForPeriod method returns true when a costing rule exists for the period.
   */
  @Test
  public void testCostingRuleDefinedForPeriodWithRule() {
    // GIVEN
    List<CostingRule> costingRules = new ArrayList<>();
    costingRules.add(mockCostingRule);
    when(mockOBQueryCostingRule.list()).thenReturn(costingRules);

    // WHEN
    boolean result = ResetValuedStockAggregated.costingRuleDefindedForPeriod(mockLegalEntity, mockPeriod);

    // THEN
    assertTrue(result);
  }

  /**
   * Tests the costingRuleDefindedForPeriod method returns false when no costing rule exists for the period.
   */
  @Test
  public void testCostingRuleDefinedForPeriodWithoutRule() {
    // GIVEN
    when(mockOBQueryCostingRule.list()).thenReturn(new ArrayList<>());

    // WHEN
    boolean result = ResetValuedStockAggregated.costingRuleDefindedForPeriod(mockLegalEntity, mockPeriod);

    // THEN
    assertFalse(result);
  }

  /**
   * Tests the noAggregatedDataForPeriod method returns true when no aggregated data exists for the period.
   */
  @Test
  public void testNoAggregatedDataForPeriodWithoutData() {
    // GIVEN
    when(mockOBCriteria.list()).thenReturn(new ArrayList<>());

    // WHEN
  boolean result = ResetValuedStockAggregated.noAggregatedDataForPeriod(mockPeriod);

    // THEN
    assertTrue(result);
  }

  /**
   * Tests the noAggregatedDataForPeriod method returns false when aggregated data exists for the period.
   */
  @Test
  public void testNoAggregatedDataForPeriodWithData() {
    // GIVEN
    List<ValuedStockAggregated> aggregatedData = new ArrayList<>();
    aggregatedData.add(mock(ValuedStockAggregated.class));
    when(mockOBCriteria.list()).thenReturn(aggregatedData);

    // WHEN
  boolean result = ResetValuedStockAggregated.noAggregatedDataForPeriod(mockPeriod);

    // THEN
    assertFalse(result);
  }

  /**
   * Tests the getClosedPeriodsToAggregate method returns a list of periods.
   */
  @Test
  public void testGetClosedPeriodsToAggregate() {
    // GIVEN
    Date endDate = new Date();
    List<Period> periods = new ArrayList<>();
    periods.add(mockPeriod);
    when(mockOBQueryPeriod.list()).thenReturn(periods);

    // Set up the mock OBContext to return a real legal entity
    when(mockOSP.getLegalEntity(any())).thenReturn(mockLegalEntity);

    // Configure the query to return a date for first not closed period
    List<Date> dateList = new ArrayList<>();
    dateList.add(new Date(System.currentTimeMillis() + 100000000L)); // Future date
    when(mockQuery.list()).thenReturn(dateList);

    // WHEN
    List<Period> result = ResetValuedStockAggregated.getClosedPeriodsToAggregate(endDate, TEST_CLIENT_ID, TEST_ORG_ID);

    // THEN
    assertEquals(periods, result);
  }

  /**
   * Tests the insertValuesIntoValuedStockAggregated method for a period with costing rules.
   *
   * @throws Exception
   *     if there is an error during the test
   */
  @Test
  public void testInsertValuesIntoValuedStockAggregated() throws Exception {
    // GIVEN
    Date startingDate = new Date();
    List<CostingRule> costingRules = new ArrayList<>();
    costingRules.add(mockCostingRule);

    when(mockOBQueryCostingRule.list()).thenReturn(costingRules);

    Method getCostingRulesMethod = ResetValuedStockAggregated.class.getDeclaredMethod("getCostingRules",
        Organization.class, Date.class, Date.class);
    getCostingRulesMethod.setAccessible(true);

    // WHEN
    ResetValuedStockAggregated.insertValuesIntoValuedStockAggregated(mockLegalEntity, mockPeriod, startingDate);

    // THEN
    mockedGenerateValuedStockAggregatedData.verify(
        () -> GenerateValuedStockAggregatedData.insertData(any(), any(), eq(TEST_ORG_ID), eq("testPeriodId"),
            eq(DATE_2022_01_01), eq(DATE_2022_01_01), eq("testCurrencyId"), eq("testCostingRuleId"),
            eq(DATE_2022_01_01), eq(null), eq(null), eq(TEST_CLIENT_ID), eq("('testOrgId')"), eq(TEST_ORG_ID)),
        times(1));
  }

  /**
   * Tests the deleteAggregatedValuesFromDate method with a null date.
   *
   * @throws Exception
   *     if there is an error during the test
   */
  @Test
  public void testDeleteAggregatedValuesFromDate() throws Exception {
    // GIVEN
    Date testDate = null;

    ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
    ArgumentCaptor<Set<String>> orgIdsCaptor = ArgumentCaptor.forClass((Class) Set.class);

    when(mockQuery.executeUpdate()).thenReturn(5);

    // WHEN
    Method deleteAggregatedValuesFromDateMethod = ResetValuedStockAggregated.class.getDeclaredMethod(
        "deleteAggregatedValuesFromDate", Date.class, Organization.class);
    deleteAggregatedValuesFromDateMethod.setAccessible(true);
    deleteAggregatedValuesFromDateMethod.invoke(processUnderTest, testDate, mockLegalEntity);

    // THEN
    verify(mockQuery).setParameter(eq("dateFrom"), dateCaptor.capture());
    verify(mockQuery).setParameterList(eq("orgIds"), orgIdsCaptor.capture());
    verify(mockQuery).executeUpdate();

    assertTrue(dateCaptor.getValue().before(new Date()));

    Set<String> capturedOrgIds = orgIdsCaptor.getValue();
    assertEquals(1, capturedOrgIds.size());
    assertTrue(capturedOrgIds.contains(TEST_ORG_ID));
  }

}
