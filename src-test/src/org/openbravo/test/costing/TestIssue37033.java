package org.openbravo.test.costing;

import java.math.BigDecimal;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.test.costing.utils.TestCostingConstants;
import org.openbravo.test.costing.utils.TestCostingUtils;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestIssue37033 extends TestCostingBase {

  /**
   * Test for issue 37033. Test Plan No. 1, No Cost Adjustment
   * 
   * <ul>
   * <li>Create a product with purchase price 10.00</li>
   * <li>Create and process a purchase order for one unit of product</li>
   * <li>Create and process a goods receipt for one unit of product from purchase order</li>
   * <li>Create an invoice from goods receipt</li>
   * <li>Create a second invoice from same goods receipt</li>
   * <li>Complete the second invoice</li>
   * <li>Complete the first invoice</li>
   * <li>Set "Enable Automatic Price Difference Correction" preference</li>
   * <li>Run Costing Background process</li>
   * <li>Asserts the original transaction cost is 10.00</li>
   * <li>Asserts the total cost is 10.00</li>
   * <li>Asserts the unit cost is 10.00</li>
   * </ul>
   */
  @Test
  public void testIssue37033_NoCostAdjustment() throws Exception {
    try {
      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      Product costingProduct = TestCostingUtils.createProduct("product37033-A",
          new BigDecimal("10.00"));

      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(costingProduct,
          new BigDecimal("10.00"), BigDecimal.ONE, 0);
      ShipmentInOut goodsReceipt = TestCostingUtils.createMovementFromOrder(purchaseOrder.getId(),
          false, BigDecimal.ONE, TestCostingConstants.LOCATOR_L01_ID, 0);
      TestCostingUtils.completeDocument(goodsReceipt);

      Invoice firstInvoice = TestCostingUtils.createInvoiceFromMovement(goodsReceipt.getId(), false,
          new BigDecimal("10.00"), BigDecimal.ONE, 0);
      Invoice secondInvoice = TestCostingUtils.createInvoiceFromMovement(goodsReceipt.getId(),
          false, new BigDecimal("10.00"), BigDecimal.ONE, 0);

      secondInvoice = OBDal.getInstance().get(Invoice.class, secondInvoice.getId());
      OBDal.getInstance().refresh(secondInvoice);
      TestCostingUtils.completeDocument(secondInvoice);

      firstInvoice = OBDal.getInstance().get(Invoice.class, firstInvoice.getId());
      OBDal.getInstance().refresh(firstInvoice);
      TestCostingUtils.completeDocument(firstInvoice);

      TestCostingUtils.markProductTransactionsForPriceDifference(costingProduct);
      TestCostingUtils.enableAutomaticPriceDifferenceCorrectionPreference();
      TestCostingUtils.runCostingBackground();
      TestCostingUtils.disableAutomaticPriceDifferenceCorrectionPreference();

      costingProduct = OBDal.getInstance().get(Product.class, costingProduct.getId());
      OBDal.getInstance().refresh(costingProduct);

      TestCostingUtils.assertOriginalTotalAndUnitCostOfProductTransaction(costingProduct, 10, 10,
          10);

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Test for issue 37033. Test Plan No. 2, Cost adjustment to increase cost
   * 
   * <ul>
   * <li>Create a product with purchase price 10.00</li>
   * <li>Create and process a purchase order for one unit of product</li>
   * <li>Create and process a goods receipt for one unit of product from purchase order</li>
   * <li>Create an invoice from goods receipt</li>
   * <li>Create a second invoice from same goods receipt</li>
   * <li>Set unit price 20.00 in invoice line</li>
   * <li>Complete the second invoice</li>
   * <li>Complete the first invoice</li>
   * <li>Set "Enable Automatic Price Difference Correction" preference</li>
   * <li>Run Costing Background process</li>
   * <li>Asserts the original transaction cost is 10.00</li>
   * <li>Asserts the total cost is 15.00</li>
   * <li>Asserts the unit cost is 15.00</li>
   * </ul>
   */
  @Test
  public void testIssue37033_CostAdjustmentToIncreaseCost() throws Exception {
    try {
      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      Product costingProduct = TestCostingUtils.createProduct("product37033-B",
          new BigDecimal("10.00"));

      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(costingProduct,
          new BigDecimal("10.00"), BigDecimal.ONE, 0);
      ShipmentInOut goodsReceipt = TestCostingUtils.createMovementFromOrder(purchaseOrder.getId(),
          false, BigDecimal.ONE, TestCostingConstants.LOCATOR_L01_ID, 0);
      TestCostingUtils.completeDocument(goodsReceipt);

      Invoice firstInvoice = TestCostingUtils.createInvoiceFromMovement(goodsReceipt.getId(), false,
          new BigDecimal("10.00"), BigDecimal.ONE, 0);
      Invoice secondInvoice = TestCostingUtils.createInvoiceFromMovement(goodsReceipt.getId(),
          false, new BigDecimal("20.00"), BigDecimal.ONE, 0);

      secondInvoice = OBDal.getInstance().get(Invoice.class, secondInvoice.getId());
      OBDal.getInstance().refresh(secondInvoice);
      TestCostingUtils.completeDocument(secondInvoice);

      firstInvoice = OBDal.getInstance().get(Invoice.class, firstInvoice.getId());
      OBDal.getInstance().refresh(firstInvoice);
      TestCostingUtils.completeDocument(firstInvoice);

      TestCostingUtils.markProductTransactionsForPriceDifference(costingProduct);
      TestCostingUtils.enableAutomaticPriceDifferenceCorrectionPreference();
      TestCostingUtils.runCostingBackground();
      TestCostingUtils.disableAutomaticPriceDifferenceCorrectionPreference();

      costingProduct = OBDal.getInstance().get(Product.class, costingProduct.getId());
      OBDal.getInstance().refresh(costingProduct);

      TestCostingUtils.assertOriginalTotalAndUnitCostOfProductTransaction(costingProduct, 10, 15,
          15);

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Test for issue 37033. Test Plan No. 3, Cost adjustment to decrease cost
   * 
   * <ul>
   * <li>Create a product with purchase price 10.00</li>
   * <li>Create and process a purchase order for one unit of product</li>
   * <li>Create and process a goods receipt for one unit of product from purchase order</li>
   * <li>Create an invoice from goods receipt</li>
   * <li>Create a second invoice from same goods receipt</li>
   * <li>Set unit price 5.00 in invoice line</li>
   * <li>Complete the second invoice</li>
   * <li>Complete the first invoice</li>
   * <li>Set "Enable Automatic Price Difference Correction" preference</li>
   * <li>Run Costing Background process</li>
   * <li>Asserts the original transaction cost is 10.00</li>
   * <li>Asserts the total cost is 7.50</li>
   * <li>Asserts the unit cost is 7.50</li>
   * </ul>
   */
  @Test
  public void testIssue37033_CostAdjustmentToDecreaseCost() throws Exception {
    try {
      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      Product costingProduct = TestCostingUtils.createProduct("product37033-C",
          new BigDecimal("10.00"));

      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(costingProduct,
          new BigDecimal("10.00"), BigDecimal.ONE, 0);
      ShipmentInOut goodsReceipt = TestCostingUtils.createMovementFromOrder(purchaseOrder.getId(),
          false, BigDecimal.ONE, TestCostingConstants.LOCATOR_L01_ID, 0);
      TestCostingUtils.completeDocument(goodsReceipt);

      Invoice firstInvoice = TestCostingUtils.createInvoiceFromMovement(goodsReceipt.getId(), false,
          new BigDecimal("10.00"), BigDecimal.ONE, 0);
      Invoice secondInvoice = TestCostingUtils.createInvoiceFromMovement(goodsReceipt.getId(),
          false, new BigDecimal("5.00"), BigDecimal.ONE, 0);

      secondInvoice = OBDal.getInstance().get(Invoice.class, secondInvoice.getId());
      OBDal.getInstance().refresh(secondInvoice);
      TestCostingUtils.completeDocument(secondInvoice);

      firstInvoice = OBDal.getInstance().get(Invoice.class, firstInvoice.getId());
      OBDal.getInstance().refresh(firstInvoice);
      TestCostingUtils.completeDocument(firstInvoice);

      TestCostingUtils.markProductTransactionsForPriceDifference(costingProduct);
      TestCostingUtils.enableAutomaticPriceDifferenceCorrectionPreference();
      TestCostingUtils.runCostingBackground();
      TestCostingUtils.disableAutomaticPriceDifferenceCorrectionPreference();

      costingProduct = OBDal.getInstance().get(Product.class, costingProduct.getId());
      OBDal.getInstance().refresh(costingProduct);

      TestCostingUtils.assertOriginalTotalAndUnitCostOfProductTransaction(costingProduct, 10, 7, 7);

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Test for issue 37033. Test Plan No. 4, Cost adjustment to increase cost with receipt partially
   * invoiced
   * 
   * <ul>
   * <li>Create a product with purchase price 10.00</li>
   * <li>Create and process a purchase order for two unit of product</li>
   * <li>Create and process a goods receipt for two unit of product from purchase order</li>
   * <li>Create an invoice from goods receipt</li>
   * <li>Set unit price 11.00 and quantity to 1 in invoice line</li>
   * <li>Complete the invoice</li>
   * <li>Set "Enable Automatic Price Difference Correction" preference</li>
   * <li>Run Costing Background process</li>
   * <li>Asserts the original transaction cost is 20.00</li>
   * <li>Asserts the total cost is 21</li>
   * <li>Asserts the unit cost is 21</li>
   * </ul>
   */
  @Test
  public void testIssue37033_CostAdjustmentToIncreaseCostWithReceiptPartiallyInvoiced()
      throws Exception {
    try {
      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      Product costingProduct = TestCostingUtils.createProduct("product37033-D",
          new BigDecimal("10.00"));

      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(costingProduct,
          new BigDecimal("10.00"), new BigDecimal("2"), 0);
      ShipmentInOut goodsReceipt = TestCostingUtils.createMovementFromOrder(purchaseOrder.getId(),
          false, new BigDecimal("2"), TestCostingConstants.LOCATOR_L01_ID, 0);
      TestCostingUtils.completeDocument(goodsReceipt);

      Invoice purchaseInvoice = TestCostingUtils.createInvoiceFromMovement(goodsReceipt.getId(),
          false, new BigDecimal("11.00"), BigDecimal.ONE, 0);
      purchaseInvoice.getInvoiceLineList().get(0).setInvoicedQuantity(BigDecimal.ONE);
      OBDal.getInstance().flush();
      TestCostingUtils.completeDocument(purchaseInvoice);

      TestCostingUtils.markProductTransactionsForPriceDifference(costingProduct);
      TestCostingUtils.enableAutomaticPriceDifferenceCorrectionPreference();
      TestCostingUtils.runCostingBackground();
      TestCostingUtils.disableAutomaticPriceDifferenceCorrectionPreference();

      costingProduct = OBDal.getInstance().get(Product.class, costingProduct.getId());
      OBDal.getInstance().refresh(costingProduct);

      TestCostingUtils.assertOriginalTotalAndUnitCostOfProductTransaction(costingProduct, 20, 21,
          21);

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Test for issue 37033. Test Plan No. 5, Cost adjustment to decrease cost with receipt partially
   * invoiced
   * 
   * <ul>
   * <li>Create a product with purchase price 10.00</li>
   * <li>Create and process a purchase order for two unit of product</li>
   * <li>Create and process a goods receipt for two unit of product from purchase order</li>
   * <li>Create an invoice from goods receipt</li>
   * <li>Set unit price 9.00 and quantity to 1 in invoice line</li>
   * <li>Complete the invoice</li>
   * <li>Set "Enable Automatic Price Difference Correction" preference</li>
   * <li>Run Costing Background process</li>
   * <li>Asserts the original transaction cost is 20.00</li>
   * <li>Asserts the total cost is 19</li>
   * <li>Asserts the unit cost is 19</li>
   * </ul>
   */
  @Test
  public void testIssue37033_CostAdjustmentToDecreaseCostWithReceiptPartiallyInvoiced()
      throws Exception {
    try {
      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      Product costingProduct = TestCostingUtils.createProduct("product37033-E",
          new BigDecimal("10.00"));

      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(costingProduct,
          new BigDecimal("10.00"), new BigDecimal("2"), 0);
      ShipmentInOut goodsReceipt = TestCostingUtils.createMovementFromOrder(purchaseOrder.getId(),
          false, new BigDecimal("2"), TestCostingConstants.LOCATOR_L01_ID, 0);
      TestCostingUtils.completeDocument(goodsReceipt);

      Invoice purchaseInvoice = TestCostingUtils.createInvoiceFromMovement(goodsReceipt.getId(),
          false, new BigDecimal("9.00"), BigDecimal.ONE, 0);
      purchaseInvoice.getInvoiceLineList().get(0).setInvoicedQuantity(BigDecimal.ONE);
      OBDal.getInstance().flush();
      TestCostingUtils.completeDocument(purchaseInvoice);

      TestCostingUtils.markProductTransactionsForPriceDifference(costingProduct);
      TestCostingUtils.enableAutomaticPriceDifferenceCorrectionPreference();
      TestCostingUtils.runCostingBackground();
      TestCostingUtils.disableAutomaticPriceDifferenceCorrectionPreference();

      costingProduct = OBDal.getInstance().get(Product.class, costingProduct.getId());
      OBDal.getInstance().refresh(costingProduct);

      TestCostingUtils.assertOriginalTotalAndUnitCostOfProductTransaction(costingProduct, 20, 19,
          19);

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
