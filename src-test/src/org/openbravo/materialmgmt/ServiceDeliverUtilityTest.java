package org.openbravo.materialmgmt;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Tuple;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.Criterion;
import org.hibernate.query.Query;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

/**
 * Test class for the ServiceDeliverUtility class.
 * <p>
 * This class contains unit tests for the functionality provided by the ServiceDeliverUtility class.
 * It uses Mockito for mocking dependencies and JUnit for test execution.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceDeliverUtilityTest {

  private static final String TEST_ORDER_LINE_ID = "TEST_ORDER_LINE_ID";


  @Mock
  private OBDal mockOBDal;

  @Mock
  private ShipmentInOut mockShipment;

  @Mock
  private OrderLine mockOrderLine;

  @Mock
  private ShipmentInOutLine mockShipmentLine;

  @Mock
  private Product mockProduct;

  @Mock
  private UOM mockUOM;

  @Mock
  private Organization mockOrganization;

  @Mock
  private OBCriteria<ShipmentInOutLine> mockCriteria;

  @Mock
  private OBProvider mockOBProvider;

  @Mock
  private Query<Tuple> mockQuery;

  @Mock
  private org.hibernate.Session mockHibernateSession;

  /**
   * Tests the deliverServices method with the "unique quantity" rule.
   * <p>
   * Verifies that a ShipmentInOutLine is created with the correct quantity when the rule specifies unique quantity.
   *
   * @throws Exception
   *     if there is an error during the test
   */
  @Test
  public void testDeliverServicesWithUniqueQuantity() {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(
        OBDal.class); MockedStatic<OBProvider> mockedOBProvider = mockStatic(OBProvider.class)) {

      // GIVEN
      String testShipmentId = "TEST_SHIPMENT_ID";
      String testOrderLineId = TEST_ORDER_LINE_ID;

      mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.getSession()).thenReturn(mockHibernateSession);
      when(mockHibernateSession.createQuery(anyString(), eq(Tuple.class))).thenReturn(mockQuery);
      when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);

      // Mock shipment
      when(mockShipment.getId()).thenReturn(testShipmentId);
      when(mockShipment.getOrganization()).thenReturn(mockOrganization);
      List<ShipmentInOutLine> shipmentLines = new ArrayList<>();
      when(mockShipment.getMaterialMgmtShipmentInOutLineList()).thenReturn(shipmentLines);

      // Mock query results for unique quantity rule
      List<Tuple> queryResults = new ArrayList<>();
      Tuple mockTuple = createMockTupleWithUniqueQuantity("id1", BigDecimal.ONE, testOrderLineId, "UQ", BigDecimal.ONE,
          BigDecimal.ZERO);
      queryResults.add(mockTuple);
      when(mockQuery.list()).thenReturn(queryResults);

      // Mock OrderLine retrieval
      when(mockOBDal.get(OrderLine.class, testOrderLineId)).thenReturn(mockOrderLine);
      when(mockOrderLine.getProduct()).thenReturn(mockProduct);
      when(mockOrderLine.getUOM()).thenReturn(mockUOM);

      // Mock ShipmentInOutLine creation
      mockedOBProvider.when(() -> OBProvider.getInstance()).thenReturn(mockOBProvider);
      when(mockOBProvider.get(ShipmentInOutLine.class)).thenReturn(mockShipmentLine);

      // WHEN
      ServiceDeliverUtility.deliverServices(mockShipment);

      // THEN
      // Verify the shipment line was added with the correct quantity
      verify(mockShipmentLine).setOrganization(mockOrganization);
      verify(mockShipmentLine).setShipmentReceipt(mockShipment);
      verify(mockShipmentLine).setSalesOrderLine(mockOrderLine);
      verify(mockShipmentLine).setProduct(mockProduct);
      verify(mockShipmentLine).setUOM(mockUOM);
      verify(mockShipmentLine).setMovementQuantity(BigDecimal.ONE); // Unique quantity = 1
      verify(mockOBDal).save(mockShipmentLine);
      verify(mockOBDal).save(mockShipment);
      assertEquals(1, shipmentLines.size());
    }
  }

  /**
   * Tests the deliverServices method with the "as per product quantity" rule.
   * <p>
   * Verifies that a ShipmentInOutLine is created with the correct quantity when the rule specifies quantity based on the product.
   *
   * @throws Exception
   *     if there is an error during the test
   */
  @Test
  public void testDeliverServicesWithAsPerProductQuantity() throws Exception {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(
        OBDal.class); MockedStatic<OBProvider> mockedOBProvider = mockStatic(OBProvider.class)) {

      // GIVEN
      String testShipmentId = "TEST_SHIPMENT_ID";
      String testOrderLineId = TEST_ORDER_LINE_ID;

      mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.getSession()).thenReturn(mockHibernateSession);
      when(mockHibernateSession.createQuery(anyString(), eq(Tuple.class))).thenReturn(mockQuery);
      when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);

      // Mock shipment
      when(mockShipment.getId()).thenReturn(testShipmentId);
      when(mockShipment.getOrganization()).thenReturn(mockOrganization);
      List<ShipmentInOutLine> shipmentLines = new ArrayList<>();
      when(mockShipment.getMaterialMgmtShipmentInOutLineList()).thenReturn(shipmentLines);

      // Mock query results for as per product quantity rule
      List<Tuple> queryResults = new ArrayList<>();
      Tuple mockTuple = createMockTupleWithUniqueQuantity("id1", BigDecimal.valueOf(5), testOrderLineId, "PP",
          BigDecimal.valueOf(10), BigDecimal.ZERO);
      queryResults.add(mockTuple);
      when(mockQuery.list()).thenReturn(queryResults);

      // Mock OrderLine retrieval
      when(mockOBDal.get(OrderLine.class, testOrderLineId)).thenReturn(mockOrderLine);
      when(mockOrderLine.getProduct()).thenReturn(mockProduct);
      when(mockOrderLine.getUOM()).thenReturn(mockUOM);

      // Mock ShipmentInOutLine creation
      mockedOBProvider.when(() -> OBProvider.getInstance()).thenReturn(mockOBProvider);
      when(mockOBProvider.get(ShipmentInOutLine.class)).thenReturn(mockShipmentLine);

      // WHEN
      ServiceDeliverUtility.deliverServices(mockShipment);

      // THEN
      verify(mockShipmentLine).setOrganization(mockOrganization);
      verify(mockShipmentLine).setShipmentReceipt(mockShipment);
      verify(mockShipmentLine).setSalesOrderLine(mockOrderLine);
      verify(mockShipmentLine).setProduct(mockProduct);
      verify(mockShipmentLine).setUOM(mockUOM);
      verify(mockShipmentLine).setMovementQuantity(BigDecimal.valueOf(5)); // As per product = movement quantity
      verify(mockOBDal).save(mockShipmentLine);
      verify(mockOBDal).save(mockShipment);
      assertEquals(1, shipmentLines.size());
    }
  }

  /**
   * Tests the addShipmentLine method to handle descriptions exceeding 255 characters.
   * <p>
   * Verifies that the description is truncated correctly and saved in the database.
   *
   * @throws Exception
   *     if there is an error during the test
   */
  @Test
  public void testAddShipmentLineWithLongDescription() throws Exception {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(
        OBDal.class); MockedStatic<OBProvider> mockedOBProvider = mockStatic(OBProvider.class)) {

      // GIVEN
      String testOrderLineId = TEST_ORDER_LINE_ID;
      String longDescription = "This is a very long description that exceeds 255 characters. " + "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor " + "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud " + "exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit.";

      mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

      // Mock shipment
      when(mockShipment.getOrganization()).thenReturn(mockOrganization);
      List<ShipmentInOutLine> shipmentLines = new ArrayList<>();
      when(mockShipment.getMaterialMgmtShipmentInOutLineList()).thenReturn(shipmentLines);

      // Mock OrderLine with long description
      when(mockOBDal.get(OrderLine.class, testOrderLineId)).thenReturn(mockOrderLine);
      when(mockOrderLine.getProduct()).thenReturn(mockProduct);
      when(mockOrderLine.getUOM()).thenReturn(mockUOM);
      when(mockOrderLine.getDescription()).thenReturn(longDescription);
      when(mockOrderLine.getBOMParent()).thenReturn(null);

      // Mock ShipmentInOutLine creation
      mockedOBProvider.when(() -> OBProvider.getInstance()).thenReturn(mockOBProvider);
      when(mockOBProvider.get(ShipmentInOutLine.class)).thenReturn(mockShipmentLine);

      Method addShipmentLineMethod = ServiceDeliverUtility.class.getDeclaredMethod("addShipmentLine",
          ShipmentInOut.class, String.class, BigDecimal.class);
      addShipmentLineMethod.setAccessible(true);

      // WHEN
      addShipmentLineMethod.invoke(null, mockShipment, testOrderLineId, BigDecimal.ONE);

      String safeLongDescription = StringUtils.defaultString(longDescription);
      verify(mockShipmentLine).setDescription(
          safeLongDescription.substring(0, Math.min(safeLongDescription.length(), 254)));
      verify(mockOBDal).save(mockShipmentLine);
      verify(mockOBDal).save(mockShipment);
    }
  }

  /**
   * Tests the addOrderlineQtyDelivered method to add quantities correctly to the delivered map.
   * <p>
   * Verifies that quantities are summed properly for the specified order line ID.
   *
   * @throws Exception
   *     if there is an error during the test
   */
  @Test
  public void testAddOrderlineQtyDelivered() throws Exception {
    Method addOrderlineQtyDeliveredMethod = ServiceDeliverUtility.class.getDeclaredMethod("addOrderlineQtyDelivered",
        Map.class, String.class, BigDecimal.class);
    addOrderlineQtyDeliveredMethod.setAccessible(true);

    // GIVEN
    Map<String, BigDecimal> orderlineDeliveredQty = new HashMap<>();
    String orderlineId = "TEST_ID";
    BigDecimal qtyToAdd1 = BigDecimal.valueOf(5);
    BigDecimal qtyToAdd2 = BigDecimal.valueOf(3);

    // WHEN
    addOrderlineQtyDeliveredMethod.invoke(null, orderlineDeliveredQty, orderlineId, qtyToAdd1);

    addOrderlineQtyDeliveredMethod.invoke(null, orderlineDeliveredQty, orderlineId, qtyToAdd2);

    // THEN
    assertEquals(0, BigDecimal.valueOf(8).compareTo(orderlineDeliveredQty.get(orderlineId)));
  }

  /**
   * Tests the addShipmentLine method to handle BOM parent.
   * <p>
   * Verifies that the BOM parent is set correctly and saved in the database.
   *
   * @throws Exception
   *     if there is an error during the test
   */
  @Test
  public void testAddShipmentLineWithBOMParent() throws Exception {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(
        OBDal.class); MockedStatic<OBProvider> mockedOBProvider = mockStatic(OBProvider.class)) {

      // GIVEN
      String testOrderLineId = TEST_ORDER_LINE_ID;

      mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

      // Mock shipment
      when(mockShipment.getOrganization()).thenReturn(mockOrganization);
      List<ShipmentInOutLine> shipmentLines = new ArrayList<>();
      when(mockShipment.getMaterialMgmtShipmentInOutLineList()).thenReturn(shipmentLines);

      // Mock OrderLine with BOM parent
      when(mockOBDal.get(OrderLine.class, testOrderLineId)).thenReturn(mockOrderLine);
      when(mockOrderLine.getProduct()).thenReturn(mockProduct);
      when(mockOrderLine.getUOM()).thenReturn(mockUOM);
      OrderLine mockBOMParent = mock(OrderLine.class);
      when(mockOrderLine.getBOMParent()).thenReturn(mockBOMParent);

      // Mock criteria for BOM parent
      when(mockOBDal.createCriteria(ShipmentInOutLine.class)).thenReturn(mockCriteria);

      when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);
      when(mockCriteria.setMaxResults(1)).thenReturn(mockCriteria);

      ShipmentInOutLine mockParentShipmentLine = mock(ShipmentInOutLine.class);
      when(mockCriteria.uniqueResult()).thenReturn(mockParentShipmentLine);

      // Mock ShipmentInOutLine creation
      mockedOBProvider.when(OBProvider::getInstance).thenReturn(mockOBProvider);
      when(mockOBProvider.get(ShipmentInOutLine.class)).thenReturn(mockShipmentLine);

      // Access the private method via reflection
      Method addShipmentLineMethod = ServiceDeliverUtility.class.getDeclaredMethod("addShipmentLine",
          ShipmentInOut.class, String.class, BigDecimal.class);
      addShipmentLineMethod.setAccessible(true);

      // WHEN
      addShipmentLineMethod.invoke(null, mockShipment, testOrderLineId, BigDecimal.ONE);

      // THEN
      verify(mockShipmentLine).setBOMParent(mockParentShipmentLine);
      verify(mockOBDal).save(mockShipmentLine);
      verify(mockOBDal).save(mockShipment);
    }
  }

  /**
   * Tests the getAsPerProductQuantity method to calculate the quantity based on the product.
   * <p>
   * Verifies that the quantity is calculated correctly based on the product and delivered quantities.
   *
   * @throws Exception
   *     if there is an error during the test
   */
  @Test
  public void testGetAsPerProductQuantity() throws Exception {
    // Use reflection to access private method
    Method getAsPerProductQuantityMethod = ServiceDeliverUtility.class.getDeclaredMethod("getAsPerProductQuantity",
        String.class, Map.class, BigDecimal.class, BigDecimal.class);
    getAsPerProductQuantityMethod.setAccessible(true);

    // GIVEN
    String orderlineId = "TEST_ID";
    Map<String, BigDecimal> orderlineDeliveredQty = new HashMap<>();
    BigDecimal movementQuantity = BigDecimal.valueOf(10);
    BigDecimal servicePendingQty = BigDecimal.valueOf(5);

    // WHEN
    BigDecimal result1 = (BigDecimal) getAsPerProductQuantityMethod.invoke(null, orderlineId, orderlineDeliveredQty,
        movementQuantity, servicePendingQty);

    // THEN
    assertEquals(0, BigDecimal.valueOf(5).compareTo(result1));

    assertEquals(0, BigDecimal.valueOf(5).compareTo(orderlineDeliveredQty.get(orderlineId)));

    // WHEN
    BigDecimal result3 = (BigDecimal) getAsPerProductQuantityMethod.invoke(null, "DIFFERENT_ID", orderlineDeliveredQty,
        BigDecimal.valueOf(0), BigDecimal.valueOf(5));

    // THEN
    assertEquals(0, BigDecimal.ZERO.compareTo(result3));
  }

  /**
   * Helper method to create a mock Tuple for testing
   */
  private Tuple createMockTupleWithUniqueQuantity(String id, BigDecimal movementQuantity, String serviceOrderLineId,
      String quantityRule, BigDecimal serviceOrderedQuantity, BigDecimal serviceDeliveredQuantity) {

    Tuple mockTuple = mock(Tuple.class);
    when(mockTuple.get("id")).thenReturn(id);
    when(mockTuple.get("movementQuantity")).thenReturn(movementQuantity);
    when(mockTuple.get("serviceOrderLineId")).thenReturn(serviceOrderLineId);
    when(mockTuple.get("quantityRule")).thenReturn(quantityRule);
    when(mockTuple.get("serviceOrderedQuantity")).thenReturn(serviceOrderedQuantity);
    when(mockTuple.get("serviceDeliveredQuantity")).thenReturn(serviceDeliveredQuantity);

    return mockTuple;
  }
}
