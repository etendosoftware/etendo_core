package org.openbravo.test.productStatus;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductStatus;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.test.base.OBBaseTest;

/**
 * Tests cases to check the PLM-Status development
 */
public class ProductStatusTest extends OBBaseTest {
  final static private Logger log = LogManager.getLogger();

  // User Openbravo
  private static final String USER_ID = "100";
  // Client QA Testing
  private static String CLIENT_ID = "4028E6C72959682B01295A070852010D";
  // Organization Spain
  private static final String ORGANIZATION_ID = "357947E87C284935AD1D783CF6F099A1";
  // Role QA Testing Admin
  private static final String ROLE_ID = "4028E6C72959682B01295A071429011E";
  // Sales Order: 50012
  private final String SALESORDER_ID = "8B53B7E6CF3B4D8D9BCF3A49EED6FCB4";
  // Ramp-Down status
  private final String RAMP_DOWN = "CFE2C57958CA41D0B6ED47D345AD276F";
  // Obsolete status
  private final String OBSOLETE = "7E4B33B5FB6444409E45D61668269FA3";
  // Mature status
  private final String MATURE = "4BB5DF9157424A418379C7708C5FF825";
  // costing Product 1
  private static final String COSTING_PRODUCT_1 = "A8B10A097DBD4BF5865BA3C844A2299C";

  private static final String ORDER_COMPLETE_PROCEDURE_NAME = "c_order_post1";
  private static final String LOCKED_ERROR = "@CannotUseLockedProduct@";
  private static final String DISCONTINUED_ERROR = "@CannotUseDiscontinuedProduct@";

  @Before
  public void executeBeforeTests() {
    OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
  }

  @AfterClass
  public static void executeAfterTests() {
    OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
    setProductStatus(COSTING_PRODUCT_1, null);
  }

  private Order createOrder(int testNum) {
    final Order order = OBDal.getInstance().get(Order.class, SALESORDER_ID);
    final Order testOrder = (Order) DalUtil.copy(order, false);
    testOrder.setDocumentNo("PLMStatus" + testNum);
    OBDal.getInstance().save(testOrder);

    for (final OrderLine orderLine : order.getOrderLineList()) {
      final OrderLine testOrderLine = (OrderLine) DalUtil.copy(orderLine, false);
      testOrderLine.setSalesOrder(testOrder);
      OBDal.getInstance().save(testOrderLine);
      testOrder.getOrderLineList().add(testOrderLine);
    }
    OBDal.getInstance().save(testOrder);

    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(testOrder);

    log.debug("Order Created:" + testOrder.getDocumentNo());

    return testOrder;
  }

  private static void setProductStatus(String productId, String statusId) {
    final Product product = OBDal.getInstance().get(Product.class, COSTING_PRODUCT_1);
    if (statusId != null) {
      final ProductStatus productStatus = OBDal.getInstance().get(ProductStatus.class, statusId);
      product.setProductStatus(productStatus);
    } else {
      product.setProductStatus(null);
    }
    OBDal.getInstance().save(product);
  }

  private Order completeOrder(Order testOrder) {
    testOrder.setDocumentAction("CO");
    return processOrder(testOrder);
  }

  private Order processOrder(Order testOrder) {
    final List<Object> params = new ArrayList<Object>();
    params.add(null);
    params.add(testOrder.getId());
    CallStoredProcedure.getInstance()
        .call(ORDER_COMPLETE_PROCEDURE_NAME, params, null, true, false);
    OBDal.getInstance().refresh(testOrder);
    return testOrder;
  }

  @Test
  public void testLocked() {
    boolean success = false;
    OBContext.setAdminMode(true);
    try {
      final Order testOrder = createOrder(1);
      setProductStatus(COSTING_PRODUCT_1, OBSOLETE);
      completeOrder(testOrder);
    } catch (Exception e) {
      success = e.getMessage().contains(LOCKED_ERROR);
    } finally {
      assertThat("The product costing Product 1 cannot be sold (locked)", success, equalTo(true));
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testDiscontinuedWithoutStock() {
    boolean success = false;
    OBContext.setAdminMode(true);
    try {
      final Order testOrder = createOrder(2);
      setProductStatus(COSTING_PRODUCT_1, RAMP_DOWN);
      final Product costingProduct1 = OBDal.getInstance().get(Product.class, COSTING_PRODUCT_1);
      final OBCriteria<StorageDetail> storageDetailCriteria = OBDal.getInstance()
          .createCriteria(StorageDetail.class);
      storageDetailCriteria.add(Restrictions.eq(StorageDetail.PROPERTY_PRODUCT, costingProduct1));
      final List<StorageDetail> storageDetailList = storageDetailCriteria.list();
      for (final StorageDetail storageDetail : storageDetailList) {
        storageDetail.setQuantityOnHand(BigDecimal.ZERO);
        OBDal.getInstance().save(storageDetail);
      }
      OBDal.getInstance().flush();
      completeOrder(testOrder);
    } catch (Exception e) {
      success = e.getMessage().contains(DISCONTINUED_ERROR);
    } finally {
      assertThat("The product costing Product 1 cannot be sold (not stock)", success,
          equalTo(true));
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testDiscontinuedWithStock() {
    boolean success = true;
    OBContext.setAdminMode(true);
    try {
      final Order testOrder = createOrder(3);
      setProductStatus(COSTING_PRODUCT_1, RAMP_DOWN);
      final Product costingProduct1 = OBDal.getInstance().get(Product.class, COSTING_PRODUCT_1);
      final OBCriteria<StorageDetail> storageDetailCriteria = OBDal.getInstance()
          .createCriteria(StorageDetail.class);
      storageDetailCriteria.add(Restrictions.eq(StorageDetail.PROPERTY_PRODUCT, costingProduct1));
      storageDetailCriteria.setMaxResults(1);
      final StorageDetail storageDetail = (StorageDetail) storageDetailCriteria.uniqueResult();
      final BigDecimal qtyOnHand = storageDetail.getQuantityOnHand() != null
          ? new BigDecimal(storageDetail.getQuantityOnHand().toString())
          : null;
      storageDetail.setQuantityOnHand(new BigDecimal(100));
      OBDal.getInstance().save(storageDetail);
      OBDal.getInstance().flush();
      completeOrder(testOrder);
      storageDetail.setQuantityOnHand(qtyOnHand);
      OBDal.getInstance().save(storageDetail);
      setProductStatus(COSTING_PRODUCT_1, null);
    } catch (Exception e) {
      success = false;
    } finally {
      assertThat("The product costing Product 1 can be sold (with stock)", success, equalTo(true));
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testNotLockedNorDiscontinued() {
    boolean success = true;
    OBContext.setAdminMode(true);
    try {
      final Order testOrder = createOrder(4);
      setProductStatus(COSTING_PRODUCT_1, MATURE);
      completeOrder(testOrder);
      setProductStatus(COSTING_PRODUCT_1, null);
    } catch (Exception e) {
      success = false;
    } finally {
      assertThat(
          "The product costing Product 1 can be sold without a locked or discontinued status",
          success, equalTo(true));
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testWithoutProductStatus() {
    boolean success = true;
    OBContext.setAdminMode(true);
    try {
      final Order testOrder = createOrder(5);
      setProductStatus(COSTING_PRODUCT_1, null);
      completeOrder(testOrder);
    } catch (Exception e) {
      success = false;
    } finally {
      assertThat("The product costing Product 1 can be sold without any status", success,
          equalTo(true));
      OBContext.restorePreviousMode();
    }
  }
}
