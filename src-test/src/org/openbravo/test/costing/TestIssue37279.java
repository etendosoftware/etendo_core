/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.costing;

import java.math.BigDecimal;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.materialmgmt.transaction.InternalConsumption;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.test.costing.assertclass.OrderToReceiptResult;
import org.openbravo.test.costing.utils.TestCostingConstants;
import org.openbravo.test.costing.utils.TestCostingUtils;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestIssue37279 extends TestCostingBase {

  private static final String INVENTORY_OPENING = "O";
  private static final String INVENTORY_NORMAL = "N";
  // RTV Shipment doctype id
  private static String RTV_SHIPMENT_DOCTYPE_ID = "4CBEA8CB77BB4208BCAD66235DC39AF2";

  /**
   * Test Price Difference Adjustment with a Goods Receipt not related to a Purchase Order
   * 
   * <ul>
   * <li>Create a new product with purchase price list of 3.00</li>
   * <li>Create and book a Purchase Order for 1 unit of product</li>
   * <li>Create and complete a Goods Receipt based on previous Purchase Order</li>
   * <li>Run Costing Background Process</li>
   * <li>Create and complete a new Goods Receipt for 10 units of product</li>
   * <li>Run Costing Background Process</li>
   * <li>Assert the product has 2 transactions</li>
   * <li>Assert one transaction has transaction cost of 3.00 and the other has transaction cost of
   * 30.00</li>
   * <li>Create a Purchase Invoice based on the first Goods Receipt created</li>
   * <li>Change the price to 20.00</li>
   * <li>Complete the Invoice</li>
   * <li>Run Price Difference Adjustment process</li>
   * <li>Assert for one transaction total cost is 20.00</li>
   * <li>Assert the other transaction has total cost 30.00</li>
   * </ul>
   */
  @Test
  public void testIssue37279_PriceDifferenceAdjustment_GoodsReceiptWithNoRelatedPurchaseOrder()
      throws Exception {
    try {
      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);
      OrderToReceiptResult results = TestCostingUtils.executeOrderToReceiptFlow("product37279-A",
          new BigDecimal("3.00"), new BigDecimal("1"));
      TestCostingUtils.runCostingBackground();
      // Add sleep to avoid assert errors
      Thread.sleep(1000);
      ShipmentInOut goodsReceipt = TestCostingUtils.cloneMovement(results.getProduct().getId(),
          false, new BigDecimal("10"), TestCostingConstants.LOCATOR_L01_ID, 0);
      TestCostingUtils.completeDocument(goodsReceipt);
      TestCostingUtils.runCostingBackground();
      TestCostingUtils.assertTransactionsCountIsTwo(results.getProduct().getId());
      TestCostingUtils.assertTransactionsCostsAre3And30(results.getProduct().getId());
      Invoice purchaseInvoice = TestCostingUtils.createInvoiceFromMovement(
          results.getGoodsReceipt().getId(), false, new BigDecimal("20"), new BigDecimal("1"), 0);
      TestCostingUtils.completeDocument(purchaseInvoice);
      TestCostingUtils.runPriceBackground();
      TestCostingUtils
          .assertTransactionCostsAdjustmentsForTestIssue37279GoodsReceiptWithNoRelatedPurchaseOrder(
              results.getProduct().getId());
    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Test Price Difference Adjustment with a Goods Shipment with negative values not related to a
   * Purchase Order
   * 
   * <ul>
   * <li>Create a new product with purchase price list of 3.00</li>
   * <li>Create and book a Purchase Order for 1 unit of product</li>
   * <li>Create and complete a Goods Receipt based on previous Purchase Order</li>
   * <li>Run Costing Background Process</li>
   * <li>Create and complete a Goods Shipment for -10 units of product</li>
   * <li>Run Costing Background Process</li>
   * <li>Assert the product has 2 transactions</li>
   * <li>Assert one transaction has transaction cost of 3.00 and the other has transaction cost of
   * 30.00</li>
   * <li>Create a Purchase Invoice based on the first Goods Receipt created</li>
   * <li>Change the price to 20.00</li>
   * <li>Complete the Invoice</li>
   * <li>Run Price Difference Adjustment process</li>
   * <li>Assert for one transaction total cost is 20.00</li>
   * <li>Assert the other transaction has total cost 200.00</li>
   * </ul>
   */
  @Test
  public void testIssue37279_PriceDifferenceAdjustment_ShippingNegativeWithNoRelatedPurchaseOrder()
      throws Exception {
    try {
      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);
      OrderToReceiptResult results = TestCostingUtils.executeOrderToReceiptFlow("product37279-B",
          new BigDecimal("3.00"), new BigDecimal("1"));
      TestCostingUtils.runCostingBackground();
      // Add sleep to avoid assert errors
      Thread.sleep(1000);
      ShipmentInOut goodsReceipt = TestCostingUtils.cloneMovement(results.getProduct().getId(),
          true, new BigDecimal("-10"), TestCostingConstants.LOCATOR_L01_ID, 0);
      TestCostingUtils.completeDocument(goodsReceipt);
      TestCostingUtils.runCostingBackground();
      TestCostingUtils.assertTransactionsCountIsTwo(results.getProduct().getId());
      TestCostingUtils.assertTransactionsCostsAre3And30(results.getProduct().getId());
      Invoice purchaseInvoice = TestCostingUtils.createInvoiceFromMovement(
          results.getGoodsReceipt().getId(), false, new BigDecimal("20"), new BigDecimal("1"), 0);
      TestCostingUtils.completeDocument(purchaseInvoice);
      TestCostingUtils.runPriceBackground();
      TestCostingUtils
          .assertTransactionCostsAdjustmentsForTestIssue37279(results.getProduct().getId());
    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Test Price Difference Adjustment with a Return to Vendor Shipment not related to a Purchase
   * Order
   * 
   * <ul>
   * <li>Create a new product with purchase price list of 3.00</li>
   * <li>Create and book a Purchase Order for 1 unit of product</li>
   * <li>Create and complete a Goods Receipt based on previous Purchase Order</li>
   * <li>Run Costing Background Process</li>
   * <li>Create and complete a Return to Vendor Shipment for 10 units of product</li>
   * <li>Run Costing Background Process</li>
   * <li>Assert the product has 2 transactions</li>
   * <li>Assert one transaction has transaction cost of 3.00 and the other has transaction cost of
   * 30.00</li>
   * <li>Create a Purchase Invoice based on the first Goods Receipt created</li>
   * <li>Change the price to 20.00</li>
   * <li>Complete the Invoice</li>
   * <li>Run Price Difference Adjustment process</li>
   * <li>Assert for one transaction total cost is 20.00</li>
   * <li>Assert the other transaction has total cost 200.00</li>
   * </ul>
   */
  @Test
  public void testIssue37279_PriceDifferenceAdjustment_ShipmentReturnWithNoRelatedPurchaseOrder()
      throws Exception {
    try {
      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);
      OrderToReceiptResult results = TestCostingUtils.executeOrderToReceiptFlow("product37279-C",
          new BigDecimal("3.00"), new BigDecimal("1"));
      TestCostingUtils.runCostingBackground();
      // Add sleep to avoid assert errors
      Thread.sleep(1000);
      ShipmentInOut returnToVendorShipment = TestCostingUtils.cloneMovement(
          results.getProduct().getId(), false, new BigDecimal("10"),
          TestCostingConstants.LOCATOR_L01_ID, 0);
      DocumentType rtvShipment = OBDal.getInstance()
          .get(DocumentType.class, RTV_SHIPMENT_DOCTYPE_ID);
      returnToVendorShipment.setDocumentType(rtvShipment);
      TestCostingUtils.completeDocument(returnToVendorShipment);
      TestCostingUtils.runCostingBackground();
      TestCostingUtils.assertTransactionsCountIsTwo(results.getProduct().getId());
      TestCostingUtils.assertTransactionsCostsAre3And30(results.getProduct().getId());
      Invoice purchaseInvoice = TestCostingUtils.createInvoiceFromMovement(
          results.getGoodsReceipt().getId(), false, new BigDecimal("20"), new BigDecimal("1"), 0);
      TestCostingUtils.completeDocument(purchaseInvoice);
      TestCostingUtils.runPriceBackground();
      TestCostingUtils
          .assertTransactionCostsAdjustmentsForTestIssue37279(results.getProduct().getId());
    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Test Price Difference Adjustment with an Internal Consumption
   * 
   * <ul>
   * <li>Create a new product with purchase price list of 3.00</li>
   * <li>Create and book a Purchase Order for 1 unit of product</li>
   * <li>Create and complete a Goods Receipt based on previous Purchase Order</li>
   * <li>Run Costing Background Process</li>
   * <li>Create and complete an Internal Consumption for 10 units of product</li>
   * <li>Run Costing Background Process</li>
   * <li>Assert the product has 2 transactions</li>
   * <li>Assert one transaction has transaction cost of 3.00 and the other has transaction cost of
   * 30.00</li>
   * <li>Create a Purchase Invoice based on the first Goods Receipt created</li>
   * <li>Change the price to 20.00</li>
   * <li>Complete the Invoice</li>
   * <li>Run Price Difference Adjustment process</li>
   * <li>Assert for one transaction total cost is 20.00</li>
   * <li>Assert the other transaction has total cost 200.00</li>
   * </ul>
   */
  @Test
  public void testIssue37279_PriceDifferenceAdjustment_InternalConsumptionWithNoRelatedPurchaseOrder()
      throws Exception {
    try {
      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);
      OrderToReceiptResult results = TestCostingUtils.executeOrderToReceiptFlow("product37279-D",
          new BigDecimal("3.00"), new BigDecimal("1"));
      TestCostingUtils.runCostingBackground();
      // Add sleep to avoid assert errors
      Thread.sleep(1000);
      InternalConsumption internalConsumption = TestCostingUtils.createInternalConsumption(
          results.getProduct().getId(), new BigDecimal("-10"), TestCostingConstants.LOCATOR_L01_ID,
          0);
      TestCostingUtils.completeDocument(internalConsumption,
          TestCostingConstants.PROCESSCONSUMPTION_PROCESS_ID);
      TestCostingUtils.runCostingBackground();
      TestCostingUtils.assertTransactionsCountIsTwo(results.getProduct().getId());
      TestCostingUtils.assertTransactionsCostsAre3And30(results.getProduct().getId());
      Invoice purchaseInvoice = TestCostingUtils.createInvoiceFromMovement(
          results.getGoodsReceipt().getId(), false, new BigDecimal("20"), new BigDecimal("1"), 0);
      TestCostingUtils.completeDocument(purchaseInvoice);
      TestCostingUtils.runPriceBackground();
      TestCostingUtils
          .assertTransactionCostsAdjustmentsForTestIssue37279(results.getProduct().getId());
    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Test Price Difference Adjustment with a NORMAL Physical Inventory
   * 
   * <ul>
   * <li>Create a new product with purchase price list of 3.00</li>
   * <li>Create and book a Purchase Order for 1 unit of product</li>
   * <li>Create and complete a Goods Receipt based on previous Purchase Order</li>
   * <li>Run Costing Background Process</li>
   * <li>Create and complete a NORMAL Physical Inventory to increase stock in 10 units of
   * product</li>
   * <li>Run Costing Background Process</li>
   * <li>Assert the product has 2 transactions</li>
   * <li>Assert one transaction has transaction cost of 3.00 and the other has transaction cost of
   * 30.00</li>
   * <li>Create a Purchase Invoice based on the first Goods Receipt created</li>
   * <li>Change the price to 20.00</li>
   * <li>Complete the Invoice</li>
   * <li>Run Price Difference Adjustment process</li>
   * <li>Assert for one transaction total cost is 20.00</li>
   * <li>Assert the other transaction has total cost 200.00</li>
   * </ul>
   */
  @Test
  public void testIssue37279_PriceDifferenceAdjustment_InventoryIncreaseWithNoRelatedPurchaseOrder()
      throws Exception {
    try {
      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);
      OrderToReceiptResult results = TestCostingUtils.executeOrderToReceiptFlow("product37279-E",
          new BigDecimal("3.00"), new BigDecimal("1"));
      TestCostingUtils.runCostingBackground();
      // Add sleep to avoid assert errors
      Thread.sleep(1000);
      InventoryCount physicalInventory = TestCostingUtils.createPhysicalInventory(
          "physicalInv37279-E", results.getProduct(), new BigDecimal("11"), INVENTORY_NORMAL, 0);
      TestCostingUtils.proessInventoryCount(physicalInventory);
      TestCostingUtils.runCostingBackground();
      TestCostingUtils.assertTransactionsCountIsTwo(results.getProduct().getId());
      TestCostingUtils.assertTransactionsCostsAre3And30(results.getProduct().getId());
      Invoice purchaseInvoice = TestCostingUtils.createInvoiceFromMovement(
          results.getGoodsReceipt().getId(), false, new BigDecimal("20"), new BigDecimal("1"), 0);
      TestCostingUtils.completeDocument(purchaseInvoice);
      TestCostingUtils.runPriceBackground();
      TestCostingUtils
          .assertTransactionCostsAdjustmentsForTestIssue37279(results.getProduct().getId());
    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Test Price Difference Adjustment with an OPENING Physical Inventory
   * 
   * <ul>
   * <li>Create a new product with purchase price list of 3.00</li>
   * <li>Create and book a Purchase Order for 1 unit of product</li>
   * <li>Create and complete a Goods Receipt based on previous Purchase Order</li>
   * <li>Run Costing Background Process</li>
   * <li>Create and complete an OPENING Physical Inventory to increase stock in 10 units of
   * product</li>
   * <li>Run Costing Background Process</li>
   * <li>Assert the product has 2 transactions</li>
   * <li>Assert one transaction has transaction cost of 3.00 and the other has transaction cost of
   * 30.00</li>
   * <li>Create a Purchase Invoice based on the first Goods Receipt created</li>
   * <li>Change the price to 20.00</li>
   * <li>Complete the Invoice</li>
   * <li>Run Price Difference Adjustment process</li>
   * <li>Assert for one transaction total cost is 20.00</li>
   * <li>Assert the other transaction has total cost 200.00</li>
   * </ul>
   */

  @Test
  public void testIssue37279_PriceDifferenceAdjustment_InventoryOpeningWithNoRelatedPurchaseOrder()
      throws Exception {
    try {
      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);
      OrderToReceiptResult results = TestCostingUtils.executeOrderToReceiptFlow("product37279-F",
          new BigDecimal("3.00"), new BigDecimal("1"));
      TestCostingUtils.runCostingBackground();
      // Add sleep to avoid assert errors
      Thread.sleep(2000);
      InventoryCount physicalInventory = TestCostingUtils.createPhysicalInventory(
          "physicalInv37279-F", results.getProduct(), new BigDecimal("11"), INVENTORY_OPENING, 0);
      TestCostingUtils.proessInventoryCount(physicalInventory);
      TestCostingUtils.runCostingBackground();
      TestCostingUtils.assertTransactionsCountIsTwo(results.getProduct().getId());
      TestCostingUtils.assertTransactionsCostsAre3And30(results.getProduct().getId());
      Invoice purchaseInvoice = TestCostingUtils.createInvoiceFromMovement(
          results.getGoodsReceipt().getId(), false, new BigDecimal("20"), new BigDecimal("1"), 0);
      TestCostingUtils.completeDocument(purchaseInvoice);
      TestCostingUtils.runPriceBackground();
      TestCostingUtils
          .assertTransactionCostsAdjustmentsForTestIssue37279(results.getProduct().getId());
    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
