package org.openbravo.materialmgmt;

import java.math.BigDecimal;
import java.sql.Connection;
import jakarta.servlet.ServletException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.ScrollMode;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.enterprise.WarehouseRule;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.onhandquantity.StockProposed;

/**
 * Unit tests for the StockUtils class.
 */
@RunWith(MockitoJUnitRunner.class)
public class StockUtilsTest {

  private static final String TEST_CLIENT_ID = "TEST_CLIENT_ID";
  private static final String TEST_PRODUCT_ID = "TEST_PRODUCT_ID";
  private static final String TEST_ORG_ID = "TEST_ORG_ID";
  private static final String TEST_USER_ID = "TEST_USER_ID";
  private static final String TEST_UOM_ID = "TEST_UOM_ID";
  private static final String TEST_WAREHOUSE_ID = "TEST_WAREHOUSE_ID";
  private static final String TEST_WAREHOUSE_RULE_ID = "TEST_WAREHOUSE_RULE_ID";
  private static final String SCROLLABLE_RESULTS_NOT_NULL = "ScrollableResults should not be null";

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<StockUtilsData> mockedStockUtilsData;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBContext mockOBContext;

  @Mock
  private OBCriteria<StockProposed> mockCriteria;

  @Mock
  private ScrollableResultsImplementor<StockProposed> mockScrollableResults;

  @Mock
  private Connection mockConnection;

  @Mock
  private OrderLine mockOrderLine;

  @Mock
  private Product mockProduct;

  @Mock
  private Organization mockOrganization;

  @Mock
  private AttributeSetInstance mockAttributeSetInstance;

  @Mock
  private User mockUser;

  @Mock
  private Client mockClient;

  @Mock
  private UOM mockUOM;

  @Mock
  private Warehouse mockWarehouse;

  /**
   * Sets up the mocks and static methods before each test.
   */
  @Before
  public void setUp() {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedStockUtilsData = mockStatic(StockUtilsData.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);

    when(mockOBDal.createCriteria(StockProposed.class)).thenReturn(mockCriteria);
    when(mockCriteria.add(Restrictions.eq(anyString(), any()))).thenReturn(mockCriteria);
    when(mockCriteria.addOrderBy(anyString(), anyBoolean())).thenReturn(mockCriteria);
    when(mockCriteria.scroll(ScrollMode.FORWARD_ONLY)).thenReturn(mockScrollableResults);

    when(mockOBDal.getConnection(anyBoolean())).thenReturn(mockConnection);

    CSResponseGetStockParam responseObject = new CSResponseGetStockParam();

    mockedStockUtilsData.when(
        () -> StockUtilsData.getStock(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any())).thenReturn(responseObject);

    when(mockOrderLine.getId()).thenReturn("TEST_ORDER_LINE_ID");
    when(mockOrderLine.getProduct()).thenReturn(mockProduct);
    when(mockProduct.getId()).thenReturn(TEST_PRODUCT_ID);
    when(mockOrderLine.getOrganization()).thenReturn(mockOrganization);
    when(mockOrganization.getId()).thenReturn(TEST_ORG_ID);
    when(mockOrderLine.getAttributeSetValue()).thenReturn(mockAttributeSetInstance);
    when(mockAttributeSetInstance.getId()).thenReturn("TEST_ATTRIBUTE_SET_INSTANCE_ID");
    when(mockOBContext.getUser()).thenReturn(mockUser);
    when(mockUser.getId()).thenReturn(TEST_USER_ID);
    when(mockOrderLine.getClient()).thenReturn(mockClient);
    when(mockClient.getId()).thenReturn(TEST_CLIENT_ID);
    when(mockOrderLine.getWarehouseRule()).thenReturn(null);
    when(mockOrderLine.getUOM()).thenReturn(mockUOM);
    when(mockUOM.getId()).thenReturn(TEST_UOM_ID);
    when(mockWarehouse.getId()).thenReturn(TEST_WAREHOUSE_ID);
  }

  /**
   * Tears down and closes static mocks after each test.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedStockUtilsData != null) {
      mockedStockUtilsData.close();
    }
  }

  /**
   * Tests the getStock method with default parameters.
   *
   * @throws ServletException
   *     if a servlet-related exception occurs.
   * @throws NoConnectionAvailableException
   *     if no connection is available.
   */
  @Test
  public void testGetStockDefaultParameters() throws ServletException, NoConnectionAvailableException {
    // GIVEN
    String uuid = "TEST_UUID";
    String recordId = "TEST_RECORD_ID";
    BigDecimal quantity = new BigDecimal("10.0");
    String mLocatorId = "TEST_LOCATOR_ID";
    String priorityWarehouseId = "TEST_PRIORITY_WAREHOUSE_ID";
    String mAttributeSetInstanceId = "TEST_ATTR_INSTANCE_ID";
    String productUomId = "TEST_PRODUCT_UOM_ID";
    String adTableId = "TEST_TABLE_ID";
    String auxId = "TEST_AUX_ID";
    Long lineNo = 1L;
    String processId = "TEST_PROCESS_ID";
    String mReservationId = "TEST_RESERVATION_ID";
    String calledFromApp = "Y";

    // WHEN
    CSResponseGetStockParam result = StockUtils.getStock(uuid, recordId, quantity, TEST_PRODUCT_ID, mLocatorId,
        TEST_WAREHOUSE_ID, priorityWarehouseId, TEST_ORG_ID, mAttributeSetInstanceId, TEST_USER_ID, TEST_CLIENT_ID,
        TEST_WAREHOUSE_RULE_ID, TEST_UOM_ID, productUomId, adTableId, auxId, lineNo, processId, mReservationId,
        calledFromApp);

    // THEN
    assertNotNull("Response should not be null", result);
  }

  /**
   * Tests the getStock method with all parameters.
   *
   * @throws ServletException
   *     if a servlet-related exception occurs.
   * @throws NoConnectionAvailableException
   *     if no connection is available.
   */
  @Test
  public void testGetStockAllParameters() throws ServletException, NoConnectionAvailableException {
    // GIVEN
    String uuid = "TEST_UUID";
    String recordId = "TEST_RECORD_ID";
    BigDecimal quantity = new BigDecimal("10.0");
    String mLocatorId = "TEST_LOCATOR_ID";
    String priorityWarehouseId = "TEST_PRIORITY_WAREHOUSE_ID";
    String mAttributeSetInstanceId = "TEST_ATTR_INSTANCE_ID";
    String productUomId = "TEST_PRODUCT_UOM_ID";
    String adTableId = "TEST_TABLE_ID";
    String auxId = "TEST_AUX_ID";
    Long lineNo = 1L;
    String processId = "TEST_PROCESS_ID";
    String mReservationId = "TEST_RESERVATION_ID";
    String calledFromApp = "Y";
    String available = "N";
    String nettable = "N";
    String overIssue = "Y";

    // WHEN
    CSResponseGetStockParam result = StockUtils.getStock(uuid, recordId, quantity, TEST_PRODUCT_ID, mLocatorId,
        TEST_WAREHOUSE_ID, priorityWarehouseId, TEST_ORG_ID, mAttributeSetInstanceId, TEST_USER_ID, TEST_CLIENT_ID,
        TEST_WAREHOUSE_RULE_ID, TEST_UOM_ID, productUomId, adTableId, auxId, lineNo, processId, mReservationId,
        calledFromApp, available, nettable, overIssue);

    // THEN
    assertNotNull("Response should not be null", result);
  }

  /**
   * Test case for the `getStockProposed` method in the `StockUtils` class.
   * Verifies that the method returns non-null ScrollableResults and correctly
   * interacts with the mocked `OBDal` and `Criteria` classes.
   */
  @Test
  public void testGetStockProposed() {
    // GIVEN
    BigDecimal quantity = new BigDecimal("10.0");

    // WHEN
    ScrollableResultsImplementor<StockProposed> result = (ScrollableResultsImplementor<StockProposed>) StockUtils.getStockProposed(
        mockOrderLine, quantity, mockWarehouse);
    // THEN
    assertNotNull(SCROLLABLE_RESULTS_NOT_NULL, result);

    // Verify that createCriteria and its methods were called correctly
    verify(mockOBDal).createCriteria(StockProposed.class);
    verify(mockCriteria).add(Restrictions.eq(anyString(), any()));
    verify(mockCriteria).addOrderBy(anyString(), anyBoolean());
    verify(mockCriteria).scroll(ScrollMode.FORWARD_ONLY);
  }

  /**
   * Test case for the `getStockProposed` method in the `StockUtils` class when
   * the `AttributeSetValue` in the `OrderLine` is null. Ensures the method
   * handles this scenario gracefully and interacts with the mocked objects.
   */
  @Test
  public void testGetStockProposedNullAttributeSetValue() {
    // GIVEN
    BigDecimal quantity = new BigDecimal("10.0");
    when(mockOrderLine.getAttributeSetValue()).thenReturn(null);

    // WHEN
    ScrollableResultsImplementor<StockProposed> result = (ScrollableResultsImplementor<StockProposed>) StockUtils.getStockProposed(
        mockOrderLine, quantity, mockWarehouse);

    // THEN
    assertNotNull(SCROLLABLE_RESULTS_NOT_NULL, result);

    // Verify only that the method was called at least once
    verify(mockOrderLine, atLeastOnce()).getAttributeSetValue();
  }

  /**
   * Test case for the `getStockProposed` method in the `StockUtils` class when
   * the `OrderLine` has a `WarehouseRule`. Verifies that the method correctly
   * interacts with the `WarehouseRule` and uses its ID.
   */
  @Test
  public void testGetStockProposedWithWarehouseRule() {
    // GIVEN
    BigDecimal quantity = new BigDecimal("10.0");

    // Mock the WarehouseRule for OrderLine
    WarehouseRule mockWarehouseRule = mock(WarehouseRule.class);
    when(mockWarehouseRule.getId()).thenReturn(TEST_WAREHOUSE_RULE_ID);
    when(mockOrderLine.getWarehouseRule()).thenReturn(mockWarehouseRule);

    // WHEN
    ScrollableResultsImplementor<StockProposed> result = (ScrollableResultsImplementor<StockProposed>) StockUtils.getStockProposed(
        mockOrderLine, quantity, mockWarehouse);

    // THEN
    assertNotNull(SCROLLABLE_RESULTS_NOT_NULL, result);

    // Use atLeastOnce() since it's called more than once
    verify(mockOrderLine, atLeastOnce()).getWarehouseRule();
    verify(mockWarehouseRule).getId();
  }
}
