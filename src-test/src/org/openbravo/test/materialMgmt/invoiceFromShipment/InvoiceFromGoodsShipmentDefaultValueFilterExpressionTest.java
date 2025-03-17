package org.openbravo.test.materialMgmt.invoiceFromShipment;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.InvoiceFromGoodsShipmentDefaultValueFilterExpression;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.test.base.OBBaseTest;

/**
 * Integration test for {@link InvoiceFromGoodsShipmentDefaultValueFilterExpression}
 */
public class InvoiceFromGoodsShipmentDefaultValueFilterExpressionTest extends OBBaseTest {

  private static final String TEST_FILTER_EXPRESSION_SAME = "TestFilterExpression_Same";
  private static final String CONTEXT = "context";

  private static final String CLIENT_ID = "4028E6C72959682B01295A070852010D";
  private static final String ORG_ID = "357947E87C284935AD1D783CF6F099A1";
  private static final String USER_ID = "100";
  private static final String ROLE_ID = "4028E6C72959682B01295A071429011E";

  private static final String GOODS_SHIPMENT_ID = "8BEAC8CAFFCE444FA15D0170F897B641";
  private static final String SALES_ORDER = "5B29AF263D004CD3830D4F9B23C17DFD";
  private static final String T_SHIRTS_PRODUCT_ID = "0CF7C882B8BD4D249F3BCC8727A736D1";

  private InvoiceFromGoodsShipmentDefaultValueFilterExpression filterExpression;

  @Before
  public void setUp() {
    OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORG_ID);
    filterExpression = new InvoiceFromGoodsShipmentDefaultValueFilterExpression();
  }

  /**
   * Tests the filter expression with a real shipment that has lines from orders with the same price list
   */
  @Test
  public void testFilterExpressionWithSamePriceList() throws Exception {
    OBContext.setAdminMode(true);
    try {
      // Create test data
      final Product product = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID, TEST_FILTER_EXPRESSION_SAME);
      final Order salesOrder = TestUtils.cloneOrder(SALES_ORDER, TEST_FILTER_EXPRESSION_SAME);
      final OrderLine orderLine = salesOrder.getOrderLineList().get(0);
      orderLine.setProduct(product);
      OBDal.getInstance().save(orderLine);
      OBDal.getInstance().flush();

      // Create shipment with line linked to the order
      final ShipmentInOut shipment = TestUtils.cloneReceiptShipment(GOODS_SHIPMENT_ID, TEST_FILTER_EXPRESSION_SAME);
      final ShipmentInOutLine shipmentLine = shipment.getMaterialMgmtShipmentInOutLineList().get(0);
      shipmentLine.setProduct(product);
      shipmentLine.setSalesOrderLine(orderLine);
      OBDal.getInstance().save(shipmentLine);
      OBDal.getInstance().flush();

      // Prepare the request map
      Map<String, String> requestMap = new HashMap<>();
      JSONObject context = new JSONObject();
      context.put("inpmInoutId", shipment.getId());
      requestMap.put(CONTEXT, context.toString());

      // Execute the filter expression
      String result = filterExpression.getExpression(requestMap);

      // Verify that the result is the price list ID from the order
      PriceList expectedPriceList = salesOrder.getPriceList();
      assertThat("Price list ID should be from the order", result, equalTo(expectedPriceList.getId()));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Tests the filter expression with a shipment that has no lines from orders
   */
  @Test
  public void testFilterExpressionWithNoOrderLines() throws Exception {
    OBContext.setAdminMode(true);
    try {
      // Create test data - shipment with no order lines
      final Product product = TestUtils.cloneProduct(T_SHIRTS_PRODUCT_ID, "TestFilterExp_NoOrder");
      final ShipmentInOut shipment = TestUtils.cloneReceiptShipment(GOODS_SHIPMENT_ID, "TestFilterExp_NoOrder");
      final ShipmentInOutLine shipmentLine = shipment.getMaterialMgmtShipmentInOutLineList().get(0);
      shipmentLine.setProduct(product);
      shipmentLine.setSalesOrderLine(null);
      OBDal.getInstance().save(shipmentLine);
      OBDal.getInstance().flush();

      // Prepare the request map
      Map<String, String> requestMap = new HashMap<>();
      JSONObject context = new JSONObject();
      context.put("inpmInoutId", shipment.getId());
      requestMap.put(CONTEXT, context.toString());

      // Execute the filter expression
      String result = filterExpression.getExpression(requestMap);

      // Verify that the result is the price list ID from the business partner
      String expectedPriceListId = shipment.getBusinessPartner().getPriceList() != null ? shipment.getBusinessPartner().getPriceList().getId() : "";
      assertThat("Price list ID should be from the business partner", result, equalTo(expectedPriceListId));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Tests the filter expression with an invalid request map
   */
  @Test(expected = OBException.class)
  public void testFilterExpressionWithInvalidRequest() {
    // Prepare an invalid request map
    Map<String, String> requestMap = new HashMap<>();
    requestMap.put(CONTEXT, "{ invalid JSON }");

    // Execute the filter expression - should throw OBException
    filterExpression.getExpression(requestMap);
  }
}
