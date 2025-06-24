package org.openbravo.common.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.pricing.pricelist.PriceList;

/**
 * Unit tests for the {@link ReturnToFromCustomerVendorOrphanHQLTransformer} class.
 * Verifies the behavior of the `transformHqlQuery` method under various scenarios,
 * including handling tax-inclusive and tax-exclusive price lists, and different query transformations.
 */
public class ReturnToFromCustomerVendorOrphanHQLTransformerTest {
  private static final String ORDER_ID_PARAMETER = "@Order.id@";
  private static final String DISTINCT_PARAMETER = "_distinct";

  private ReturnToFromCustomerVendorOrphanHQLTransformer transformer;

  @Mock
  private Order mockOrder;

  @Mock
  private PriceList mockPriceList;

  private MockedStatic<OBDal> obDalStaticMock;
  private AutoCloseable closeable;

  /**
   * Sets up the test environment before each test.
   * Initializes the transformer, mocks, and static utilities.
   */
  @BeforeEach
  public void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
    transformer = new ReturnToFromCustomerVendorOrphanHQLTransformer();

    obDalStaticMock = mockStatic(OBDal.class);
    OBDal mockOBDal = mock(OBDal.class);
    obDalStaticMock.when(OBDal::getInstance).thenReturn(mockOBDal);
    when(mockOBDal.get(Order.class, "100")).thenReturn(mockOrder);

    when(mockOrder.getPriceList()).thenReturn(mockPriceList);
  }

  /**
   * Cleans up the test environment after each test.
   * Closes all mocked static instances and resources.
   *
   * @throws Exception
   *     if an error occurs during cleanup
   */
  @AfterEach
  public void tearDown() throws Exception {
    if (obDalStaticMock != null) {
      obDalStaticMock.close();
    }

    if (closeable != null) {
      closeable.close();
    }
  }

  /**
   * Tests the `transformHqlQuery` method for a tax-inclusive price list.
   * Verifies that the query is transformed to use the `grossUnitPrice` property.
   */
  @Test
  public void testTransformHqlQueryIncludeTax() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(ORDER_ID_PARAMETER, "100");
    Map<String, Object> queryNamedParameters = new HashMap<>();

    when(mockPriceList.isPriceIncludesTax()).thenReturn(true);

    String hqlQuery = "SELECT @unitPriceProperty@ FROM Order";

    String result = transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

    assertEquals("SELECT grossUnitPrice FROM Order", result);
    assertEquals("100", queryNamedParameters.get("salesOrderId"));
  }

  /**
   * Tests the `transformHqlQuery` method for a tax-inclusive price list.
   * Verifies that the query is transformed to use the `grossUnitPrice` property.
   */
  @Test
  public void testTransformHqlQueryExcludeTax() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(ORDER_ID_PARAMETER, "100");
    Map<String, Object> queryNamedParameters = new HashMap<>();

    when(mockPriceList.isPriceIncludesTax()).thenReturn(false);

    String hqlQuery = "SELECT @unitPriceProperty@ FROM Order";

    String result = transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

    assertEquals("SELECT unitPrice FROM Order", result);
    assertEquals("100", queryNamedParameters.get("salesOrderId"));
  }

  /**
   * Tests the `transformHqlQuery` method for a tax-exclusive price list.
   * Verifies that the query is transformed to use the `unitPrice` property.
   */
  @Test
  public void testTransformHqlQueryReturnReasonCountQuery() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(ORDER_ID_PARAMETER, "100");
    requestParameters.put(DISTINCT_PARAMETER, "returnReason");
    requestParameters.put("_justCount", "true");
    Map<String, Object> queryNamedParameters = new HashMap<>();

    when(mockPriceList.isPriceIncludesTax()).thenReturn(false);

    String hqlQuery = "some query";

    String result = transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

    assertEquals(
        " select count(distinct e.name) from ReturnReason as e where exists (select distinct ol.returnReason from OrderLine as ol where ol.returnReason = e and ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine is null) ",
        result);
  }

  /**
   * Tests the `transformHqlQuery` method for a return reason data query.
   * Verifies that the query is transformed to retrieve distinct return reasons.
   */
  @Test
  public void testTransformHqlQueryReturnReasonDataQuery() {
    // GIVEN
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(ORDER_ID_PARAMETER, "100");
    requestParameters.put(DISTINCT_PARAMETER, "returnReason");
    Map<String, Object> queryNamedParameters = new HashMap<>();

    when(mockPriceList.isPriceIncludesTax()).thenReturn(false);

    String hqlQuery = "some query";

    String result = transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

    assertEquals(
        " select distinct e, e.name from ReturnReason as e where exists (select distinct ol.returnReason from OrderLine as ol where ol.returnReason = e and ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine is null) ",
        result);
  }

  /**
   * Tests the `transformHqlQuery` method for a default clause transformation.
   * Verifies that the query is transformed to include a default clause for return reasons.
   */
  @Test
  public void testTransformHqlQueryDefaultClause() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(ORDER_ID_PARAMETER, "100");
    requestParameters.put(DISTINCT_PARAMETER, "anotherProperty");
    Map<String, Object> queryNamedParameters = new HashMap<>();

    when(mockPriceList.isPriceIncludesTax()).thenReturn(false);

    String hqlQuery = "ORDER BY @returnReasonLeftClause@.id";

    String result = transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

    assertEquals(
        "ORDER BY  coalesce((select oli.returnReason.id from OrderLine as oli where oli.salesOrder.id = :salesOrderId  and oli.id=ol.id), '')",
        result);
  }
}
