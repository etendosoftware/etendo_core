package org.openbravo.common.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.pricing.pricelist.PriceList;

/**
 * Unit tests for the {@link InOutLinePEHQLTransformer} class.
 * Verifies the behavior of HQL transformation methods and additional filters.
 */
@ExtendWith(MockitoExtension.class)
public class InOutLinePEHQLTransformerTest {

  private static final String IS_SALES_TRANSACTION = "issotrx";
  private static final String SALES_TRANSACTION_PARAM = "@Invoice.salesTransaction@" ;


  @InjectMocks
  private InOutLinePEHQLTransformer transformer;

  @Mock
  private OBDal obDal;

  @Mock
  private Window window;

  @Mock
  private PriceList priceList;

  @Mock
  private OBContext obContext;

  @Mock
  private Client client;

  @Mock
  private Organization organization;

  @Mock
  private User user;

  @Mock
  private Role role;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<UOMUtil> uomUtilStatic;
  private MockedStatic<Preferences> preferencesStatic;

  private Map<String, String> requestParameters;
  private Map<String, Object> queryNamedParameters;
  private String baseHqlQuery;

  /**
   * Sets up the test environment before each test.
   * Mocks static methods and initializes required dependencies.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @BeforeEach
  public void setUp() throws Exception {
    obDalStatic = mockStatic(OBDal.class);
    obContextStatic = mockStatic(OBContext.class);
    uomUtilStatic = mockStatic(UOMUtil.class);
    preferencesStatic = mockStatic(Preferences.class);

    obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
    obContextStatic.when(OBContext::getOBContext).thenReturn(obContext);

    when(obContext.getCurrentClient()).thenReturn(client);
    when(obContext.getCurrentOrganization()).thenReturn(organization);
    when(obContext.getUser()).thenReturn(user);
    when(obContext.getRole()).thenReturn(role);

    requestParameters = new HashMap<>();
    queryNamedParameters = new HashMap<>();

    requestParameters.put("@Invoice.businessPartner@", "testBPartnerId");
    requestParameters.put("@Invoice.priceList@", "testPriceListId");
    requestParameters.put("@Invoice.currency@", "testCurrencyId");

    lenient().when(obDal.get(eq(Window.class), eq("E4524BA1D1354AAD8B31C290672D8417"))).thenReturn(window);
    lenient().when(obDal.get(eq(PriceList.class), anyString())).thenReturn(priceList);

    baseHqlQuery = "SELECT @selectClause@ FROM @fromClause@ WHERE 1=1 @whereClause@ GROUP BY @groupByClause@ HAVING @movementQuantity@ > 0";
  }

  /**
   * Cleans up the test environment after each test.
   * Closes mocked static methods to release resources.
   */
  @AfterEach
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (uomUtilStatic != null) uomUtilStatic.close();
    if (preferencesStatic != null) preferencesStatic.close();
  }

  /**
   * Tests the HQL transformation for a sales transaction.
   * Verifies that the query is transformed correctly and parameters are set as expected.
   */
  @Test
  @DisplayName("Test HQL transformation for sales transaction")
  public void testTransformHqlQueryForSalesTransaction() {
    requestParameters.put(SALES_TRANSACTION_PARAM, "true");
    when(priceList.isPriceIncludesTax()).thenReturn(true);
    uomUtilStatic.when(UOMUtil::isUomManagementEnabled).thenReturn(false);

    String transformedHql = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

    assertNotNull(transformedHql);
    assertTrue(transformedHql.contains("sh.salesTransaction = :issotrx"));
    assertTrue(transformedHql.contains("sh.completelyInvoiced = 'N'"));
    assertTrue(queryNamedParameters.containsKey(IS_SALES_TRANSACTION));
    assertTrue((Boolean) queryNamedParameters.get(IS_SALES_TRANSACTION));
    assertTrue((Boolean) queryNamedParameters.get("plIncTax"));
    assertEquals("testBPartnerId", queryNamedParameters.get("bp"));
    assertEquals("testCurrencyId", queryNamedParameters.get("cur"));
  }

  /**
   * Tests the HQL transformation for a purchase transaction.
   * Verifies that the query is transformed correctly and parameters are set as expected.
   */
  @Test
  @DisplayName("Test HQL transformation for purchase transaction")
  public void testTransformHqlQueryForPurchaseTransaction() {
    requestParameters.put(SALES_TRANSACTION_PARAM, "false");
    when(priceList.isPriceIncludesTax()).thenReturn(false);
    uomUtilStatic.when(UOMUtil::isUomManagementEnabled).thenReturn(false);

    String transformedHql = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

    assertNotNull(transformedHql);
    assertTrue(transformedHql.contains("sh.salesTransaction = :issotrx"));
    assertFalse(transformedHql.contains("sh.completelyInvoiced = 'N'"));
    assertTrue(queryNamedParameters.containsKey(IS_SALES_TRANSACTION));
    assertFalse((Boolean) queryNamedParameters.get(IS_SALES_TRANSACTION));
    assertFalse((Boolean) queryNamedParameters.get("plIncTax"));
  }

  /**
   * Tests the additional filters to change client and organization.
   * Verifies that the filters are correctly replaced in the query.
   */
  @Test
  @DisplayName("Testing additional filters to change client and organization")
  public void testAdditionalFilters() {
    requestParameters.put(SALES_TRANSACTION_PARAM, "true");
    when(priceList.isPriceIncludesTax()).thenReturn(true);
    String queryWithFilters = baseHqlQuery + " AND e.client.id in ('test') AND e.organization in ('test')";

    String transformedHql = transformer.transformHqlQuery(queryWithFilters, requestParameters, queryNamedParameters);

    assertNotNull(transformedHql);
    assertTrue(transformedHql.contains("sh.client.id in ('test')"));
    assertTrue(transformedHql.contains("sh.organization.id in ('test')"));
    assertFalse(transformedHql.contains("e.client.id in"));
    assertFalse(transformedHql.contains("e.organization in"));
  }

  /**
   * Tests the `getSinceHowManyDaysAgoInOutsShouldBeFiltered` method when a preference is defined.
   * Verifies that the method returns the correct value from the preference.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  @DisplayName("Test for the getSinceHowManyDaysAgoInOutsShouldBeFiltered method with defined preference")
  public void testGetSinceHowManyDaysAgoInOutsShouldBeFilteredWithPreference() throws Exception {
    preferencesStatic.when(
        () -> Preferences.getPreferenceValue(eq("FilterByDocumentsProcessedSinceNDaysAgo"), eq(true), any(Client.class),
            any(Organization.class), any(User.class), any(Role.class), any(Window.class))).thenReturn("30");

    InOutLinePEHQLTransformer spyTransformer = new InOutLinePEHQLTransformer();

    java.lang.reflect.Method method = InOutLinePEHQLTransformer.class.getDeclaredMethod(
        "getSinceHowManyDaysAgoInOutsShouldBeFiltered");
    method.setAccessible(true);
    String result = (String) method.invoke(spyTransformer);

    assertEquals("30", result);
  }

  /**
   * Tests the `getSinceHowManyDaysAgoInOutsShouldBeFiltered` method when no preference is defined.
   * Verifies that the method returns the default value of 365 days.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  @DisplayName("Test for the getSinceHowManyDaysAgoInOutsShouldBeFiltered method with no preference defined")
  public void testGetSinceHowManyDaysAgoInOutsShouldBeFilteredWithoutPreference() throws Exception {
    preferencesStatic.when(
        () -> Preferences.getPreferenceValue(anyString(), anyBoolean(), (Client) any(), any(), any(), any(),
            any())).thenThrow(new RuntimeException("Preference not found"));

    InOutLinePEHQLTransformer spyTransformer = new InOutLinePEHQLTransformer();

    java.lang.reflect.Method method = InOutLinePEHQLTransformer.class.getDeclaredMethod(
        "getSinceHowManyDaysAgoInOutsShouldBeFiltered");
    method.setAccessible(true);
    String result = (String) method.invoke(spyTransformer);

    assertEquals("365", result);
  }
}
