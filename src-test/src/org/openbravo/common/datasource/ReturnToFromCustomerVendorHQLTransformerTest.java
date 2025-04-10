package org.openbravo.common.datasource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.pricing.pricelist.PriceList;

/**
 * Unit tests for the {@link ReturnToFromCustomerVendorHQLTransformer} class.
 * Verifies the behavior of the HQL transformation methods under various scenarios,
 * including UOM management, distinct return reasons, and specific query transformations.
 */
@ExtendWith(MockitoExtension.class)
public class ReturnToFromCustomerVendorHQLTransformerTest {

  /**
   * Sample HQL query used in tests.
   */
  private static final String SAMPLE_HQL = "SELECT iol FROM MaterialMgmtShipmentInOutLine AS iol " + "WHERE iol.shipmentReceipt.processed = true " + "AND @returnedLeftClause@ " + "ORDER BY @orderNoLeftClause@";
  private static final String SALES_ORDER_ID = "TEST_ORDER_ID";
  private static final String BUSINESS_PARTNER_ID = "TEST_BP_ID";
  @InjectMocks
  private ReturnToFromCustomerVendorHQLTransformer transformer;
  @Mock
  private OBDal obDal;
  @Mock
  private Order mockOrder;
  @Mock
  private PriceList mockPriceList;
  private Map<String, String> requestParameters;
  private Map<String, Object> queryNamedParameters;

  /**
   * Sets up the test environment before each test.
   * Initializes request parameters, named parameters, and mocks.
   */
  @BeforeEach
  void setUp() {
    requestParameters = new HashMap<>();
    queryNamedParameters = new HashMap<>();

    requestParameters.put("@Order.id@", SALES_ORDER_ID);
    requestParameters.put("@Order.businessPartner@", BUSINESS_PARTNER_ID);
  }

  /**
   * Tests the {@code transformHqlQuery} method when UOM management is enabled.
   * Verifies that the correct HQL transformation is applied.
   */
  @Test
  void testTransformHqlQueryWithUomManagementEnabled() {
    try (MockedStatic<OBDal> obDalMock = Mockito.mockStatic(
        OBDal.class); MockedStatic<UOMUtil> uomUtilMock = Mockito.mockStatic(UOMUtil.class)) {

      obDalMock.when(OBDal::getInstance).thenReturn(obDal);
      uomUtilMock.when(UOMUtil::isUomManagementEnabled).thenReturn(true);

      when(obDal.get(eq(Order.class), eq(SALES_ORDER_ID))).thenReturn(mockOrder);
      when(mockOrder.getPriceList()).thenReturn(mockPriceList);
      when(mockPriceList.isPriceIncludesTax()).thenReturn(false);

      String result = transformer.transformHqlQuery(SAMPLE_HQL, requestParameters, queryNamedParameters);

      assertNotNull(result, "The result should not be null");
      assertTrue(result.contains("coalesce((select ol.operativeQuantity from OrderLine"),
          "It should use returnedLeftClauseAUM when UOM is enabled");

      verify(obDal).get(Order.class, SALES_ORDER_ID);
    }
  }

  /**
   * Tests the {@code transformHqlQuery} method with distinct return reasons.
   * Verifies that the correct query is used for counting or retrieving return reasons.
   */
  @Test
  void testTransformHqlQueryDistinctReturnReason() {
    try (MockedStatic<OBDal> obDalMock = Mockito.mockStatic(
        OBDal.class); MockedStatic<UOMUtil> uomUtilMock = Mockito.mockStatic(UOMUtil.class)) {

      obDalMock.when(OBDal::getInstance).thenReturn(obDal);
      uomUtilMock.when(UOMUtil::isUomManagementEnabled).thenReturn(false);

      when(obDal.get(eq(Order.class), eq(SALES_ORDER_ID))).thenReturn(mockOrder);
      when(mockOrder.getPriceList()).thenReturn(mockPriceList);
      when(mockPriceList.isPriceIncludesTax()).thenReturn(false);

      requestParameters.put("_distinct", "returnReason");
      requestParameters.put("_justCount", "true");

      String result = transformer.transformHqlQuery(SAMPLE_HQL, requestParameters, queryNamedParameters);

      assertNotNull(result, "The result should not be null");
      assertTrue(result.contains("select count(distinct e.name) from ReturnReason"),
          "It should use the count query for returnReason");

      requestParameters.put("_justCount", "false");
      result = transformer.transformHqlQuery(SAMPLE_HQL, requestParameters, queryNamedParameters);

      assertTrue(result.contains("select distinct e, e.name from ReturnReason"),
          "It should use the data query for returnReason");
    }
  }

  /**
   * Tests the {@code transformHqlQueryReturnToVendor} method.
   * Verifies that the HQL query is correctly transformed for the "Return to Vendor" scenario.
   */
  @Test
  void testTransformHqlQueryReturnToVendor() {
    String result = transformer.transformHqlQueryReturnToVendor(
        "SELECT @unitPriceLeftClause@ FROM Dummy WHERE @orderNoLeftClause@ AND @returnReasonLeftClause@.id",
        new HashMap<>(), SALES_ORDER_ID);

    assertNotNull(result);
    assertTrue(result.contains("(case when (select e.salesOrderLine.salesOrder.priceList.priceIncludesTax"));
    assertTrue(result.contains("coalesce ((select e.salesOrderLine.salesOrder.documentNo"));
    assertFalse(result.contains("@unitPriceLeftClause@"));
  }

  /**
   * Tests the {@code transformHqlQueryReturnFromCustomer} method.
   * Verifies that the HQL query is correctly transformed for the "Return from Customer" scenario.
   */
  @Test
  void testTransformHqlQueryReturnFromCustomer() {
    String result = transformer.transformHqlQueryReturnFromCustomer(
        "SELECT @unitPriceLeftClause@ FROM Dummy WHERE @orderNoLeftClause@ AND @returnReasonLeftClause@.name",
        new HashMap<>(), SALES_ORDER_ID);

    assertNotNull(result);
    assertTrue(result.contains("(case when (iol.salesOrderLine.salesOrder.priceList.priceIncludesTax)"));
    assertTrue(
        result.contains("coalesce((select e.salesOrderLine.salesOrder.documentNo from MaterialMgmtShipmentInOutLine"));
    assertFalse(result.contains("@returnReasonLeftClause@.name"));
  }
}
