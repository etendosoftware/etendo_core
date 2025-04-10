package org.openbravo.common.datasource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.pricing.pricelist.PriceList;

/**
 * Unit tests for the {@link OrderLinePEHQLTransformer} class.
 * Verifies the behavior of the HQL transformation methods under various scenarios,
 * including sales transactions, purchase transactions, and additional filters.
 */
@ExtendWith(MockitoExtension.class)
public class OrderLinePEHQLTransformerTest {

  private OrderLinePEHQLTransformer transformer;

  @Mock
  private OBDal mockDal;

  @Mock
  private OBContext mockContext;

  @Mock
  private PriceList mockPriceList;

  @Mock
  private Window mockWindow;

  private MockedStatic<OBDal> staticOBDal;
  private MockedStatic<OBContext> staticOBContext;
  private MockedStatic<UOMUtil> staticUOMUtil;
  private MockedStatic<Preferences> staticPreferences;

  private Map<String, String> requestParameters;
  private Map<String, Object> queryNamedParameters;
  private String baseHqlQuery;

  /**
   * Sets up the test environment before each test.
   * Initializes the transformer, mocks, and static utilities.
   */
  @BeforeEach
  public void setUp() {
    transformer = new OrderLinePEHQLTransformer();

    staticOBDal = Mockito.mockStatic(OBDal.class);
    staticOBContext = Mockito.mockStatic(OBContext.class);
    staticUOMUtil = Mockito.mockStatic(UOMUtil.class);
    staticPreferences = Mockito.mockStatic(Preferences.class);

    staticOBDal.when(OBDal::getInstance).thenReturn(mockDal);
    staticOBContext.when(OBContext::getOBContext).thenReturn(mockContext);

    requestParameters = new HashMap<>();
    queryNamedParameters = new HashMap<>();

    baseHqlQuery = "SELECT @selectClause@ FROM @fromClause@ WHERE 1=1 @whereClause@ " + "GROUP BY @groupByClause@ HAVING @filterByDocumentsProcessedSinceNDaysAgo@ " + "ORDER BY @orderByClause@";

    requestParameters.put("@Invoice.priceList@", "testPriceListId");
    requestParameters.put("@Invoice.businessPartner@", "testBusinessPartnerId");
    requestParameters.put("@Invoice.currency@", "testCurrencyId");
    requestParameters.put("@Invoice.id@", "testInvoiceId");

    when(mockDal.get(PriceList.class, "testPriceListId")).thenReturn(mockPriceList);
    when(mockPriceList.isPriceIncludesTax()).thenReturn(true);
  }

  /**
   * Cleans up the test environment after each test.
   * Closes all mocked static instances.
   */
  @AfterEach
  public void tearDown() {
    if (staticOBDal != null) staticOBDal.close();
    if (staticOBContext != null) staticOBContext.close();
    if (staticUOMUtil != null) staticUOMUtil.close();
    if (staticPreferences != null) staticPreferences.close();
  }

  /**
   * Tests the transformation of the HQL query for a sales transaction.
   * Verifies that the query and named parameters are correctly updated.
   */
  @Test
  public void testTransformHqlQuerySalesTransaction() {
    requestParameters.put("@Invoice.salesTransaction@", "true");
    staticUOMUtil.when(UOMUtil::isUomManagementEnabled).thenReturn(false);

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

    assertAll(() -> assertTrue(result.contains("InvoiceCandidateV ic")),
        () -> assertTrue(result.contains("ic.salesTransaction = :issotrx")),
        () -> assertTrue(result.contains("ic.documentNo desc")),
        () -> assertEquals(Boolean.TRUE, queryNamedParameters.get("issotrx")),
        () -> assertEquals("testBusinessPartnerId", queryNamedParameters.get("bp")),
        () -> assertEquals(Boolean.TRUE, queryNamedParameters.get("plIncTax")),
        () -> assertEquals("testCurrencyId", queryNamedParameters.get("cur")));

    assertFalse(queryNamedParameters.containsKey("invId"));
  }

  /**
   * Tests the transformation of the HQL query for a purchase transaction.
   * Verifies that the query and named parameters are correctly updated.
   */
  @Test
  public void testTransformHqlQueryPurchaseTransaction() {
    requestParameters.put("@Invoice.salesTransaction@", "false");
    staticUOMUtil.when(UOMUtil::isUomManagementEnabled).thenReturn(false);

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

    assertAll(() -> assertTrue(result.contains("OrderLine e")),
        () -> assertTrue(result.contains("join e.salesOrder o")),
        () -> assertTrue(result.contains("o.salesTransaction = :issotrx")),
        () -> assertTrue(result.contains("o.documentNo desc")),
        () -> assertEquals(Boolean.FALSE, queryNamedParameters.get("issotrx")),
        () -> assertEquals("testBusinessPartnerId", queryNamedParameters.get("bp")),
        () -> assertEquals(Boolean.TRUE, queryNamedParameters.get("plIncTax")),
        () -> assertEquals("testCurrencyId", queryNamedParameters.get("cur")),
        () -> assertEquals("testInvoiceId", queryNamedParameters.get("invId")));
  }

  /**
   * Tests the transformation of the HQL query with the "FilterByDocumentsProcessedSinceNDaysAgo" preference.
   * Verifies that the query includes the correct date filter.
   */
  @Test
  public void testTransformHqlQueryWithFilterByDaysPreference() {
    requestParameters.put("@Invoice.salesTransaction@", "true");
    staticUOMUtil.when(UOMUtil::isUomManagementEnabled).thenReturn(false);

    when(mockDal.get(Window.class, "D0E067F649AC457D9EA2CDAC2E8571D7")).thenReturn(mockWindow);

    when(mockContext.getCurrentClient()).thenReturn(null);
    when(mockContext.getCurrentOrganization()).thenReturn(null);
    when(mockContext.getUser()).thenReturn(null);
    when(mockContext.getRole()).thenReturn(null);

    staticPreferences.when(
        () -> Preferences.getPreferenceValue(eq("FilterByDocumentsProcessedSinceNDaysAgo"), eq(true), any(), any(),
            any(), any(), eq(mockWindow))).thenReturn("30");

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

    assertTrue(result.contains("ic.orderDate >= (now()-30)"));
  }

  /**
   * Tests the transformation of the HQL query with additional filters for a sales transaction.
   * Verifies that the filters are correctly updated in the query.
   */
  @Test
  public void testTransformHqlQueryAdditionalFilters() {
    requestParameters.put("@Invoice.salesTransaction@", "true");
    staticUOMUtil.when(UOMUtil::isUomManagementEnabled).thenReturn(false);

    String queryWithFilters = baseHqlQuery + " AND e.client.id in ('1') AND e.organization in ('1')";

    String result = transformer.transformHqlQuery(queryWithFilters, requestParameters, queryNamedParameters);

    assertTrue(result.contains("ic.client.id in ('1')"));
    assertTrue(result.contains("ic.organization.id in ('1')"));
    assertFalse(result.contains("e.client.id in"));
    assertFalse(result.contains("e.organization in"));
  }

  /**
   * Tests the transformation of the HQL query with additional filters for a purchase transaction.
   * Verifies that the filters are correctly updated in the query.
   */
  @Test
  public void testTransformHqlQueryAdditionalFiltersPurchaseTransaction() {
    requestParameters.put("@Invoice.salesTransaction@", "false");
    staticUOMUtil.when(UOMUtil::isUomManagementEnabled).thenReturn(false);

    String queryWithFilters = baseHqlQuery + " AND e.client.id in ('1') AND e.organization in ('1')";

    String result = transformer.transformHqlQuery(queryWithFilters, requestParameters, queryNamedParameters);

    assertTrue(result.contains("o.client.id in ('1')"));
    assertTrue(result.contains("o.organization.id in ('1')"));
    assertFalse(result.contains("e.client.id in"));
    assertFalse(result.contains("e.organization in"));
  }
}
