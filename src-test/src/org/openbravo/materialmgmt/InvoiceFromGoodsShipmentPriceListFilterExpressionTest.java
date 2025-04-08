package org.openbravo.materialmgmt;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.pricing.pricelist.PriceList;

/**
 * Test cases for the {@link InvoiceFromGoodsShipmentPriceListFilterExpression} class.
 */
@RunWith(MockitoJUnitRunner.class)
public class InvoiceFromGoodsShipmentPriceListFilterExpressionTest {

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<InvoiceFromGoodsShipmentUtil> mockedInvoiceFromGoodsShipmentUtil;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private ShipmentInOut mockShipment;

  @Mock
  private PriceList mockPriceList;

  @InjectMocks
  private InvoiceFromGoodsShipmentPriceListFilterExpression filterExpression;

  /**
   * Sets up the test environment before each test case.
   * <p>
   * This method is executed before each test method annotated with @Test.
   * It initializes static mocks for the OBDal and InvoiceFromGoodsShipmentUtil classes.
   * It also configures the behavior of the OBDal mock to return a mocked instance.
   */
  @Before
  public void setUp() {
    mockedOBDal = mockStatic(OBDal.class);
    mockedInvoiceFromGoodsShipmentUtil = mockStatic(InvoiceFromGoodsShipmentUtil.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
  }

  /**
   * Cleans up the test environment after each test case.
   * <p>
   * This method is executed after each test method annotated with @Test.
   * It closes the static mocks to ensure no interference with other tests.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedInvoiceFromGoodsShipmentUtil != null) {
      mockedInvoiceFromGoodsShipmentUtil.close();
    }
  }

  /**
   * Test case for {@link InvoiceFromGoodsShipmentPriceListFilterExpression#getExpression} when all
   * shipment lines come from orders with the same price list.
   */
  @Test
  public void testGetExpressionWhenShipmentLinesFromOrdersWithSamePriceList() {
    // GIVEN
    final String shipmentId = "TEST_SHIPMENT_ID";
    final String priceListId = "TEST_PRICE_LIST_ID";
    Map<String, String> requestMap = new HashMap<>();
    requestMap.put("inpmInoutId", shipmentId);

    // Mock behavior for OBDal.getInstance().getProxy()
    when(mockOBDal.getProxy(ShipmentInOut.class, shipmentId)).thenReturn(mockShipment);

    // Mock behavior for InvoiceFromGoodsShipmentUtil methods
    mockedInvoiceFromGoodsShipmentUtil.when(
        () -> InvoiceFromGoodsShipmentUtil.shipmentLinesFromOrdersWithSamePriceList(mockShipment)).thenReturn(true);

    // Create a list with a mocked price list
    List<PriceList> priceLists = new ArrayList<>();
    when(mockPriceList.getId()).thenReturn(priceListId);
    priceLists.add(mockPriceList);

    mockedInvoiceFromGoodsShipmentUtil.when(
        () -> InvoiceFromGoodsShipmentUtil.getPriceListFromOrder(mockShipment)).thenReturn(priceLists);

    // WHEN
    String result = filterExpression.getExpression(requestMap);

    // THEN
    assertEquals(" e.id='" + priceListId + "' ", result);
    verify(mockOBDal).getProxy(ShipmentInOut.class, shipmentId);
    mockedInvoiceFromGoodsShipmentUtil.verify(
        () -> InvoiceFromGoodsShipmentUtil.shipmentLinesFromOrdersWithSamePriceList(mockShipment));
    mockedInvoiceFromGoodsShipmentUtil.verify(() -> InvoiceFromGoodsShipmentUtil.getPriceListFromOrder(mockShipment));
  }

  /**
   * Test case for {@link InvoiceFromGoodsShipmentPriceListFilterExpression#getExpression} when
   * shipment lines come from orders with different price lists.
   */
  @Test
  public void testGetExpressionWhenShipmentLinesFromOrdersWithDifferentPriceLists() {
    // GIVEN
    final String shipmentId = "TEST_SHIPMENT_ID";
    Map<String, String> requestMap = new HashMap<>();
    requestMap.put("inpmInoutId", shipmentId);

    // Mock behavior for OBDal.getInstance().getProxy()
    when(mockOBDal.getProxy(ShipmentInOut.class, shipmentId)).thenReturn(mockShipment);

    // Mock behavior for InvoiceFromGoodsShipmentUtil.shipmentLinesFromOrdersWithSamePriceList()
    mockedInvoiceFromGoodsShipmentUtil.when(
        () -> InvoiceFromGoodsShipmentUtil.shipmentLinesFromOrdersWithSamePriceList(mockShipment)).thenReturn(false);

    // WHEN
    String result = filterExpression.getExpression(requestMap);

    // THEN
    assertEquals(" 1=1 ", result);
    verify(mockOBDal).getProxy(ShipmentInOut.class, shipmentId);
    mockedInvoiceFromGoodsShipmentUtil.verify(
        () -> InvoiceFromGoodsShipmentUtil.shipmentLinesFromOrdersWithSamePriceList(mockShipment));
    // getPriceListFromOrder should not be called in this case
    mockedInvoiceFromGoodsShipmentUtil.verify(
        () -> InvoiceFromGoodsShipmentUtil.getPriceListFromOrder(any(ShipmentInOut.class)), never());
  }
}
