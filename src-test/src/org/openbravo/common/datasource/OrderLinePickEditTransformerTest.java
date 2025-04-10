package org.openbravo.common.datasource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.util.Check;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

/**
 * Unit tests for the {@link OrderLinePickEditTransformer} class.
 * Verifies the behavior of the `transformHqlQuery` method under various scenarios,
 * including Sales Order Lines tab and Return From Customer Lines tab.
 */
@ExtendWith(MockitoExtension.class)
class OrderLinePickEditTransformerTest {

  @Mock
  private OBDal obDal;

  @Mock
  private Product mockProduct;

  @Mock
  private OrderLine mockOrderLine;

  @Mock
  private OrderLine mockOriginalOrderLine;

  @Mock
  private ShipmentInOutLine mockShipmentLine;

  @Mock
  private IsIDFilter mockIsIDFilter;

  @InjectMocks
  private OrderLinePickEditTransformer transformer;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<Check> mockedCheck;
  private Map<String, String> requestParameters;
  private Map<String, Object> queryNamedParameters;
  private String baseHqlQuery;

  /**
   * Sets up the test environment before each test.
   * Initializes mocked static methods, request parameters, and named parameters.
   */
  @BeforeEach
  void setUp() {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

    mockedCheck = mockStatic(Check.class);
    mockedCheck.when(() -> Check.isTrue(anyBoolean(), anyString())).then(invocation -> null);

    requestParameters = new HashMap<>();
    requestParameters.put("@Order.id@", "TEST_ORDER_ID");
    requestParameters.put("@Order.businessPartner@", "TEST_BP_ID");
    requestParameters.put("@OrderLine.id@", "TEST_ORDERLINE_ID");
    requestParameters.put("@OrderLine.product@", "TEST_PRODUCT_ID");

    queryNamedParameters = new HashMap<>();

    baseHqlQuery = "SELECT e FROM OrderLine e JOIN e.salesOrder o JOIN o.priceList pl WHERE @whereClause@ " + "AND @amountLeftClause@ > 0 " + "AND @relatedQuantityLeftClause@ @relatedQuantity@ > 0 " + "AND @returnQtyOtherRMLeftClause@ @returnQtyOtherRM@ > 0 " + "AND o.id = @Order.id@ ";

    when(mockProduct.getIncludedProductCategories()).thenReturn("X"); // Neither Y nor N
    when(mockProduct.getIncludedProducts()).thenReturn("X"); // Neither Y nor N
    when(obDal.get(eq(Product.class), anyString())).thenReturn(mockProduct);
  }

  /**
   * Cleans up the test environment after each test.
   * Closes mocked static methods to release resources.
   */
  @AfterEach
  void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedCheck != null) {
      mockedCheck.close();
    }
  }

  /**
   * Nested test class for Sales Order Lines tab scenarios.
   */
  @Nested
  @DisplayName("Sales Order Lines Tab Tests")
  class SalesOrderLinesTabTests {

    /**
     * Sets up the test environment for Sales Order Lines tab tests.
     */
    @BeforeEach
    void setUp() {
      requestParameters.put("buttonOwnerViewTabId", "187");
    }

    /**
     * Tests the `transformHqlQuery` method for the Sales Order Lines tab.
     * Verifies that the HQL query is transformed correctly.
     */
    @Test
    @DisplayName("Should correctly transform HQL for Sales Order Lines tab")
    void shouldTransformHqlForSalesOrderLinesTab() {
      String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

      assertAll(() -> assertTrue(result.contains("o.salesTransaction = true")),
          () -> assertTrue(result.contains("and (o.processed = true or o.id = :orderId)")),
          () -> assertTrue(result.contains("and o.businessPartner.id = :businessPartnerId")),
          () -> assertTrue(result.contains("and e.id <> :orderLineId")),
          () -> assertTrue(result.contains("and e.product.productType <> 'S'")),
          () -> assertEquals("TEST_ORDER_ID", queryNamedParameters.get("orderId")),
          () -> assertEquals("TEST_BP_ID", queryNamedParameters.get("businessPartnerId")),
          () -> assertEquals("TEST_ORDERLINE_ID", queryNamedParameters.get("orderLineId")),
          () -> assertEquals("TEST_PRODUCT_ID", queryNamedParameters.get("productId")));
    }

    /**
     * Tests that the HQL query includes category filtering when
     * `includedCategories` is set to "N". Verifies the presence of the
     * appropriate `exists` clause in the query.
     */
    @Test
    @DisplayName("Should include category filtering when includedCategories is N")
    void shouldIncludeCategoryFilteringWhenIncludedCategoriesIsN() {
      when(mockProduct.getIncludedProductCategories()).thenReturn("N");

      String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

      assertTrue(result.contains(
          "exists ( select 1 from ServiceProductCategory spc where spc.productCategory = e.product.productCategory and spc.product.id = :productId)"));
    }

    /**
     * Tests that the HQL query excludes category filtering when
     * `includedCategories` is set to "Y". Verifies the presence of the
     * appropriate `not exists` clause in the query.
     */
    @Test
    @DisplayName("Should exclude category filtering when includedCategories is Y")
    void shouldExcludeCategoryFilteringWhenIncludedCategoriesIsY() {
      when(mockProduct.getIncludedProductCategories()).thenReturn("Y");

      String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

      assertTrue(result.contains(
          "not exists ( select 1 from ServiceProductCategory spc where spc.productCategory = e.product.productCategory and spc.product.id = :productId)"));
    }

    /**
     * Tests that the HQL query includes product filtering when
     * `includedProducts` is set to "N". Verifies the presence of the
     * appropriate `exists` clause in the query.
     */
    @Test
    @DisplayName("Should include product filtering when includedProducts is N")
    void shouldIncludeProductFilteringWhenIncludedProductsIsN() {
      when(mockProduct.getIncludedProducts()).thenReturn("N");

      String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

      assertTrue(result.contains(
          "exists (select 1 from ServiceProduct p where p.relatedProduct.id = e.product.id and p.product.id = :productId)"));
    }

    /**
     * Tests that the HQL query excludes product filtering when
     * `includedProducts` is set to "Y". Verifies the presence of the
     * appropriate `not exists` clause in the query.
     */
    @Test
    @DisplayName("Should exclude product filtering when includedProducts is Y")
    void shouldExcludeProductFilteringWhenIncludedProductsIsY() {
      when(mockProduct.getIncludedProducts()).thenReturn("Y");

      String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

      assertTrue(result.contains(
          "not exists (select 1 from ServiceProduct p where p.relatedProduct.id = e.product.id and p.product.id = :productId)"));
    }

    /**
     * Verifies that the correct left join clauses are used in the HQL query
     * for the Sales Order Lines tab. Checks the presence of specific
     * conditions and calculations in the query.
     */
    @Test
    @DisplayName("Should use correct left clauses for Sales Order Lines tab")
    void shouldUseCorrectLeftClausesForSalesOrderLinesTab() {
      String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

      assertAll(() -> assertTrue(result.contains(
              "coalesce((select quantity from OrderlineServiceRelation osr where osr.salesOrderLine.id = :orderLineId and osr.orderlineRelated.id = e.id), 0)*1")),
          () -> assertTrue(result.contains(
              "abs(coalesce((select abs(amount) from OrderlineServiceRelation osr where osr.salesOrderLine.id = :orderLineId and osr.orderlineRelated.id = e.id)")));
    }
  }

  /**
   * Nested test class for Return From Customer Lines tab scenarios.
   */
  @Nested
  @DisplayName("Return From Customer Lines Tab Tests")
  class ReturnFromCustomerLinesTabTests {

    /**
     * Sets up the test environment for Return From Customer Lines tab tests.
     */
    @BeforeEach
    void setUp() {
      requestParameters.put("buttonOwnerViewTabId", "AF4090093D471431E040007F010048A5");
      when(obDal.get(eq(OrderLine.class), anyString())).thenReturn(mockOrderLine);
    }

    /**
     * Tests HQL transformation for the Return From Customer tab when no goods shipment is associated
     * with the order line. Verifies that specific conditions and clauses are correctly handled.
     */
    @Test
    @DisplayName("Should correctly transform HQL for Return From Customer tab when no goods shipment")
    void shouldTransformHqlForReturnFromCustomerTabNoGoodsShipment() {
      when(mockOrderLine.getGoodsShipmentLine()).thenReturn(null);

      String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

      assertAll(() -> assertFalse(result.contains(":originalOrderLineId")), () -> assertTrue(result.contains("abs")),
          () -> assertTrue(result.contains("*(-1)")), () -> assertTrue(result.contains(
              "(coalesce((select abs(amount) from OrderlineServiceRelation osr where osr.salesOrderLine.id = :orderLineId and osr.orderlineRelated.id = e.id)")));
    }

    /**
     * Tests HQL transformation for the Return From Customer tab when a goods shipment is associated
     * with the order line. Ensures correct handling of parameters and related clauses.
     */
    @Test
    @DisplayName("Should correctly transform HQL for Return From Customer tab with goods shipment")
    void shouldTransformHqlForReturnFromCustomerTabWithGoodsShipment() {
      when(mockOrderLine.getGoodsShipmentLine()).thenReturn(mockShipmentLine);
      when(mockShipmentLine.getSalesOrderLine()).thenReturn(mockOriginalOrderLine);
      when(mockOriginalOrderLine.getId()).thenReturn("ORIGINAL_ORDERLINE_ID");

      String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

      assertAll(() -> assertTrue(result.contains(
              "exists (select 1 from OrderlineServiceRelation osr where osr.salesOrderLine.id = :originalOrderLineId and osr.orderlineRelated.id = e.id)")),
          () -> assertTrue(result.contains(
              "exists (select 1 from OrderlineServiceRelation osr where osr.salesOrderLine.id = :orderLineId and osr.orderlineRelated.id = e.id)")),
          () -> assertEquals("ORIGINAL_ORDERLINE_ID", queryNamedParameters.get("originalOrderLineId")),
          () -> assertEquals("TEST_ORDERLINE_ID", queryNamedParameters.get("orderLineId")));
    }

    /**
     * Verifies that the correct left join clauses are used in the HQL query for the Return From Customer tab.
     * Checks that specific conditions and calculations are correctly handled in the query.
     */
    @Test
    @DisplayName("Should use correct left clauses for Return From Customer tab")
    void shouldUseCorrectLeftClausesForReturnFromCustomerTab() {
      String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

      assertAll(() -> assertTrue(result.contains(
              "coalesce((select quantity from OrderlineServiceRelation osr where osr.salesOrderLine.id = :orderLineId and osr.orderlineRelated.id = e.id), 0)*(-1)")),
          () -> assertTrue(result.contains(
              "(coalesce((select abs(amount) from OrderlineServiceRelation osr where osr.salesOrderLine.id = :orderLineId and osr.orderlineRelated.id = e.id)")));
    }
  }
}
