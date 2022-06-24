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
 * All portions are Copyright (C) 2018-2019 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.costing;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.transaction.InternalConsumption;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ProductionTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.test.costing.assertclass.CostAdjustmentAssert;
import org.openbravo.test.costing.assertclass.DocumentPostAssert;
import org.openbravo.test.costing.assertclass.ProductCostingAssert;
import org.openbravo.test.costing.assertclass.ProductTransactionAssert;
import org.openbravo.test.costing.utils.TestCostingConstants;
import org.openbravo.test.costing.utils.TestCostingUtils;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCostingNoSourceAdjustments extends TestCostingBase {

  @Test
  public void testCostingGM11() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("11.00");
    final BigDecimal price2 = new BigDecimal("10.00");
    final BigDecimal price3 = new BigDecimal("15.00");
    final BigDecimal price4 = new BigDecimal("19.7619");
    final BigDecimal price5 = new BigDecimal("11.9524");
    final BigDecimal quantity1 = new BigDecimal("820");
    final BigDecimal quantity2 = new BigDecimal("400");
    final BigDecimal quantity3 = new BigDecimal("420");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingGM11", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity1, day1);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods movement, run costing background, post it and assert it
      InternalMovement goodsMovement = TestCostingUtils.createGoodsMovement(product, price1,
          quantity2, TestCostingConstants.LOCATOR_L01_ID, TestCostingConstants.LOCATOR_M01_ID,
          day3);

      // Update transaction total cost amount
      TestCostingUtils.manualCostAdjustment(
          TestCostingUtils.getProductTransactions(product.getId()).get(1),
          quantity2.multiply(price2), false, day4);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt, price3, quantity1, day2);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalMovement.class, goodsMovement.getId())
          .getMaterialMgmtInternalMovementLineList()
          .get(0), price1, price2, true));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalMovement.class, goodsMovement.getId())
          .getMaterialMgmtInternalMovementLineList()
          .get(0), price1, price2));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0),
          TestCostingConstants.SPAIN_WAREHOUSE_ID, price3, price1, price3, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1),
          TestCostingConstants.SPAIN_WAREHOUSE_ID, price2, price5, price4, quantity3));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2),
          TestCostingConstants.SPAIN_EAST_WAREHOUSE_ID, price2, price1, price2, quantity2));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "MCC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, true, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(2), "MCC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, false, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), day2, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("61000", BigDecimal.ZERO,
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("61000",
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), null));
      CostAdjustment costAdjustment1 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment1, product.getId(),
          documentPostAssertList1);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      CostAdjustment costAdjustment2 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(1).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment2, product.getId(),
          documentPostAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingGM12() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("11.00");
    final BigDecimal price2 = new BigDecimal("10.00");
    final BigDecimal price3 = new BigDecimal("15.00");
    final BigDecimal price4 = new BigDecimal("14.00");
    final BigDecimal price5 = new BigDecimal("15.9524");
    final BigDecimal price6 = new BigDecimal("11.9524");
    final BigDecimal quantity1 = new BigDecimal("820");
    final BigDecimal quantity2 = new BigDecimal("400");
    final BigDecimal quantity3 = new BigDecimal("420");
    final BigDecimal amount1 = new BigDecimal("-400");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingGM12", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity1, day1);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods movement, run costing background, post it and assert it
      InternalMovement goodsMovement = TestCostingUtils.createGoodsMovement(product, price1,
          quantity2, TestCostingConstants.LOCATOR_L01_ID, TestCostingConstants.LOCATOR_M01_ID,
          day3);

      // Update transaction total cost amount
      TestCostingUtils.manualCostAdjustment(
          TestCostingUtils.getProductTransactions(product.getId()).get(1), amount1, true, false,
          day4);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt, price3, quantity1, day2);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalMovement.class, goodsMovement.getId())
          .getMaterialMgmtInternalMovementLineList()
          .get(0), price1, price4, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalMovement.class, goodsMovement.getId())
          .getMaterialMgmtInternalMovementLineList()
          .get(0), price1, price4));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0),
          TestCostingConstants.SPAIN_WAREHOUSE_ID, price3, price1, price3, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1),
          TestCostingConstants.SPAIN_WAREHOUSE_ID, price4, price6, price5, quantity3));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2),
          TestCostingConstants.SPAIN_EAST_WAREHOUSE_ID, price4, price1, price4, quantity2));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "MCC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, true, false));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(2), "MCC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, false, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), day2, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), day3, false));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), day3, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("61000", BigDecimal.ZERO,
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("61000",
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), null));
      CostAdjustment costAdjustment1 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment1, product.getId(),
          documentPostAssertList1);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("61000",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList2.add(new DocumentPostAssert("61000", BigDecimal.ZERO,
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      CostAdjustment costAdjustment2 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(1).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment2, product.getId(),
          documentPostAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingGM13() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("11.00");
    final BigDecimal price2 = new BigDecimal("10.00");
    final BigDecimal price3 = new BigDecimal("15.00");
    final BigDecimal price4 = new BigDecimal("11.9524");
    final BigDecimal quantity1 = new BigDecimal("820");
    final BigDecimal quantity2 = new BigDecimal("400");
    final BigDecimal quantity3 = new BigDecimal("420");
    final BigDecimal amount1 = new BigDecimal("-400");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingGM13", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity1, day1);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods movement, run costing background, post it and assert it
      InternalMovement goodsMovement = TestCostingUtils.createGoodsMovement(product, price1,
          quantity2, TestCostingConstants.LOCATOR_L01_ID, TestCostingConstants.LOCATOR_M01_ID,
          day3);

      // Update transaction total cost amount
      TestCostingUtils.manualCostAdjustment(
          TestCostingUtils.getProductTransactions(product.getId()).get(1), amount1, true, true,
          day4);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt, price3, quantity1, day2);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalMovement.class, goodsMovement.getId())
          .getMaterialMgmtInternalMovementLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalMovement.class, goodsMovement.getId())
          .getMaterialMgmtInternalMovementLineList()
          .get(0), price1, price3));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0),
          TestCostingConstants.SPAIN_WAREHOUSE_ID, price3, price1, price3, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1),
          TestCostingConstants.SPAIN_WAREHOUSE_ID, price3, price4, price3, quantity3));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2),
          TestCostingConstants.SPAIN_EAST_WAREHOUSE_ID, price3, price1, price3, quantity2));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "MCC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, true, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(2), "MCC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, false, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), day2, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price2).negate()), day3, false));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price2).negate()), day3, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("61000", BigDecimal.ZERO,
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("61000",
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), null));
      CostAdjustment costAdjustment1 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment1, product.getId(),
          documentPostAssertList1);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity2.multiply(price3).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("61000",
          quantity2.multiply(price3).add(quantity2.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList2.add(new DocumentPostAssert("61000", BigDecimal.ZERO,
          quantity2.multiply(price3).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000",
          quantity2.multiply(price3).add(quantity2.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      CostAdjustment costAdjustment2 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(1).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment2, product.getId(),
          documentPostAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingGM5() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final int day6 = 30;
    final int day7 = 35;
    final int day8 = 40;
    final int day9 = 45;
    final BigDecimal price1 = new BigDecimal("120.00");
    final BigDecimal price2 = new BigDecimal("150.00");
    final BigDecimal price3 = new BigDecimal("100.00");
    final BigDecimal price4 = new BigDecimal("95.00");
    final BigDecimal price5 = new BigDecimal("5.00");
    final BigDecimal price6 = new BigDecimal("95.6897");
    final BigDecimal price7 = new BigDecimal("88.4921");
    final BigDecimal price8 = new BigDecimal("96.0317");
    final BigDecimal price9 = new BigDecimal("337.50");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("20");
    final BigDecimal quantity3 = new BigDecimal("30");
    final BigDecimal quantity4 = new BigDecimal("500");
    final BigDecimal quantity5 = new BigDecimal("50");
    final BigDecimal quantity6 = new BigDecimal("580");
    final BigDecimal quantity7 = new BigDecimal("630");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingGM5", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product, price2, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrder1, price1,
          quantity1, TestCostingConstants.LOCATOR_L01_ID, day1);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrder2, price2,
          quantity1, TestCostingConstants.LOCATOR_M01_ID, day2);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = TestCostingUtils.createGoodsShipment(product, price1,
          quantity2, TestCostingConstants.LOCATOR_L01_ID, day3);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = TestCostingUtils.createGoodsShipment(product, price2,
          quantity3, TestCostingConstants.LOCATOR_M01_ID, day4);

      // Create purchase invoice, post it and assert it
      List<ShipmentInOut> goodsReceiptList = new ArrayList<ShipmentInOut>();
      goodsReceiptList.add(goodsReceipt1);
      goodsReceiptList.add(goodsReceipt2);
      List<BigDecimal> priceList = new ArrayList<BigDecimal>();
      priceList.add(price3);
      priceList.add(price3);
      TestCostingUtils.createPurchaseInvoice(goodsReceiptList, priceList, quantity1.add(quantity1),
          day5);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt3 = TestCostingUtils.createGoodsReceipt(product, price3, quantity4,
          TestCostingConstants.LOCATOR_L01_ID, day6);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt3, price4, quantity4, day7);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods movement, run costing background, post it and assert it
      InternalMovement goodsMovement = TestCostingUtils.createGoodsMovement(product, price3,
          quantity5, TestCostingConstants.LOCATOR_M01_ID, TestCostingConstants.LOCATOR_L01_ID,
          day8);

      // Update transaction total cost amount
      TestCostingUtils.manualCostAdjustment(
          TestCostingUtils.getProductTransactions(product.getId()).get(5),
          quantity5.multiply(price5), false, day9);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price4));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalMovement.class, goodsMovement.getId())
          .getMaterialMgmtInternalMovementLineList()
          .get(0), price3, price5, true));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalMovement.class, goodsMovement.getId())
          .getMaterialMgmtInternalMovementLineList()
          .get(0), price3, price5));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0),
          TestCostingConstants.SPAIN_WAREHOUSE_ID, price3, price1, price3, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(4),
          TestCostingConstants.SPAIN_WAREHOUSE_ID, price4, price3, price6, quantity6));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(6),
          TestCostingConstants.SPAIN_WAREHOUSE_ID, price5, price8, price7, quantity7));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1),
          TestCostingConstants.SPAIN_EAST_WAREHOUSE_ID, price3, price2, price3, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(5),
          TestCostingConstants.SPAIN_EAST_WAREHOUSE_ID, price5, null, price9, quantity2));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), day5, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity1.multiply(price3).add(quantity1.multiply(price2).negate()), day5, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), day5, false));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(3), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price2).negate()), day5, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(4), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price3).negate()), day7, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList3 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList3.add(new CostAdjustmentAssert(transactionList.get(5), "MCC",
          quantity5.multiply(price5).add(quantity5.multiply(price3).negate()), day9, true));
      costAdjustmentAssertLineList3.add(new CostAdjustmentAssert(transactionList.get(6), "MCC",
          quantity5.multiply(price5).add(quantity5.multiply(price3).negate()), day9, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList3);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904",
          quantity1.multiply(price1).add(quantity1.multiply(price3).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity1.multiply(price1).add(quantity1.multiply(price3).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("99904",
          quantity1.multiply(price2).add(quantity1.multiply(price3).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity1.multiply(price2).add(quantity1.multiply(price3).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("99900", BigDecimal.ZERO,
          quantity2.multiply(price1).add(quantity2.multiply(price3).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity2.multiply(price1).add(quantity2.multiply(price3).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("99900", BigDecimal.ZERO,
          quantity3.multiply(price2).add(quantity3.multiply(price3).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity3.multiply(price2).add(quantity3.multiply(price3).negate()), BigDecimal.ZERO,
          null));
      CostAdjustment costAdjustment1 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment1, product.getId(),
          documentPostAssertList1);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904",
          quantity4.multiply(price3).add(quantity4.multiply(price4).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity4.multiply(price3).add(quantity4.multiply(price4).negate()), null));
      CostAdjustment costAdjustment2 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(1).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment2, product.getId(),
          documentPostAssertList2);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(2));
      List<DocumentPostAssert> documentPostAssertList3 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList3.add(new DocumentPostAssert("35000",
          quantity5.multiply(price3).add(quantity5.multiply(price5).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList3.add(new DocumentPostAssert("61000", BigDecimal.ZERO,
          quantity5.multiply(price3).add(quantity5.multiply(price5).negate()), null));
      documentPostAssertList3.add(new DocumentPostAssert("61000",
          quantity5.multiply(price3).add(quantity5.multiply(price5).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList3.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity5.multiply(price3).add(quantity5.multiply(price5).negate()), null));
      CostAdjustment costAdjustment3 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(2).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment3, product.getId(),
          documentPostAssertList3);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingIC4() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("35.00");
    final BigDecimal price2 = new BigDecimal("9.00");
    final BigDecimal price3 = new BigDecimal("5.00");
    final BigDecimal price4 = new BigDecimal("40.00");
    final BigDecimal quantity1 = new BigDecimal("1000");
    final BigDecimal quantity2 = new BigDecimal("250");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingIC4", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity1, day1);

      // Create internal consumption, run costing background, post it and assert
      // it
      InternalConsumption internalConsumtpion = TestCostingUtils.createInternalConsumption(product,
          price1, quantity2, day4);

      // Update transaction total cost amount
      TestCostingUtils.manualCostAdjustment(
          TestCostingUtils.getProductTransactions(product.getId()).get(0),
          quantity1.multiply(price2), false, day2);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2, true));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalConsumption.class, internalConsumtpion.getId())
          .getMaterialMgmtInternalConsumptionLineList()
          .get(0), price1, price2));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      List<MaterialTransaction> transactionList1 = TestCostingUtils
          .getProductTransactions(product.getId());
      productCostingAssertList1.add(
          new ProductCostingAssert(transactionList1.get(0), price2, price1, price2, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList1 = TestCostingUtils
          .getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList1.get(0), "MCC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day2, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList1.get(1), "MCC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList1);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList1.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904",
          quantity1.multiply(price1).add(quantity1.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity1.multiply(price1).add(quantity1.multiply(price2).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("61000", BigDecimal.ZERO,
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList1.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList1);

      // Cancel Cost Adjustment
      TestCostingUtils.cancelCostAdjustment(costAdjustment);

      // Update transaction total cost amount
      TestCostingUtils.manualCostAdjustment(
          TestCostingUtils.getProductTransactions(product.getId()).get(0),
          quantity1.multiply(price3), true, false, day3);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4, price1));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalConsumption.class, internalConsumtpion.getId())
          .getMaterialMgmtInternalConsumptionLineList()
          .get(0), price1, price4));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList2);

      // Assert product costing
      List<MaterialTransaction> transactionList2 = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(
          new ProductCostingAssert(transactionList2.get(0), price4, price1, price4, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList2);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList2 = TestCostingUtils
          .getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0), "MCC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day2, true, "VO"));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(1), "MCC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList22 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(0), "MCC",
          quantity1.multiply(price1).add(quantity1.multiply(price2).negate()), day2, true, "VO"));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(1), "MCC",
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), day4, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList22);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList3 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList3.add(new CostAdjustmentAssert(transactionList2.get(0), "MCC",
          quantity1.multiply(price4).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList3.add(new CostAdjustmentAssert(transactionList2.get(1), "MCC",
          quantity2.multiply(price4).add(quantity2.multiply(price1).negate()), day4, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList3);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Post cost adjustment 1 and assert it
      TestCostingUtils.postDocument(costAdjustmentList2.get(0));
      List<DocumentPostAssert> documentPostAssertList21 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList21.add(new DocumentPostAssert("99904",
          quantity1.multiply(price1).add(quantity1.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList21.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity1.multiply(price1).add(quantity1.multiply(price2).negate()), null));
      documentPostAssertList21.add(new DocumentPostAssert("35000",
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList21.add(new DocumentPostAssert("61000", BigDecimal.ZERO,
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), null));
      CostAdjustment costAdjustment21 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList2.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment21, product.getId(),
          documentPostAssertList21);

      // Post cost adjustment 2 and assert it
      TestCostingUtils.postDocument(costAdjustmentList2.get(1));
      List<DocumentPostAssert> documentPostAssertList22 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList22.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity1.multiply(price1).add(quantity1.multiply(price2).negate()), null));
      documentPostAssertList22.add(new DocumentPostAssert("35000",
          quantity1.multiply(price1).add(quantity1.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList22.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList22.add(new DocumentPostAssert("61000",
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      CostAdjustment costAdjustment22 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList2.get(1).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment22, product.getId(),
          documentPostAssertList22);

      // Post cost adjustment 3 and assert it
      TestCostingUtils.postDocument(costAdjustmentList2.get(2));
      List<DocumentPostAssert> documentPostAssertList3 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList3.add(new DocumentPostAssert("61000", BigDecimal.ZERO,
          quantity1.multiply(price4).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList3.add(new DocumentPostAssert("35000",
          quantity1.multiply(price4).add(quantity1.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList3.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity2.multiply(price4).add(quantity2.multiply(price1).negate()), null));
      documentPostAssertList3.add(new DocumentPostAssert("61000",
          quantity2.multiply(price4).add(quantity2.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      CostAdjustment costAdjustment3 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList2.get(2).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment3, product.getId(),
          documentPostAssertList3);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingR10() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final BigDecimal price1 = new BigDecimal("15.00");
    final BigDecimal price2 = new BigDecimal("40.00");
    final BigDecimal price3 = new BigDecimal("9.00");
    final BigDecimal quantity1 = new BigDecimal("180");
    final BigDecimal quantity2 = new BigDecimal("80");
    final BigDecimal quantity3 = new BigDecimal("40");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingR10", price1, price2);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity1, day1);

      // Create purchase order and book it
      Order saleseOrder = TestCostingUtils.createSalesOrder(product, price2, quantity2, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = TestCostingUtils.createGoodsShipment(saleseOrder, price1,
          quantity2, day3);

      // Update purchase order line product price
      TestCostingUtils.updatePurchaseOrder(purchaseOrder, price3);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt, price3, quantity1, day5);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Create return from customer, run costing background, post it and assert
      // it
      Order returnFromCustomer = TestCostingUtils.createReturnFromCustomer(goodsShipment, price2,
          quantity3, day3);

      // Create return material receipt, run costing background, post it and
      // assert it
      ShipmentInOut returnMaterialReceipt = TestCostingUtils
          .createReturnMaterialReceipt(returnFromCustomer, price3, quantity3, day4);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, returnMaterialReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price3, price1, price3, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), price3, null,
          price3, quantity1.add(quantity2.negate()).add(quantity3)));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), day1, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), day3, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      ;
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904",
          quantity1.multiply(price1).add(quantity1.multiply(price3).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity1.multiply(price1).add(quantity1.multiply(price3).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("99900", BigDecimal.ZERO,
          quantity2.multiply(price1).add(quantity2.multiply(price3).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity2.multiply(price1).add(quantity2.multiply(price3).negate()), BigDecimal.ZERO,
          null));
      CostAdjustment costAdjustment1 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment1, product.getId(),
          documentPostAssertList1);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingR2() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("15.00");
    final BigDecimal price2 = new BigDecimal("30.00");
    final BigDecimal quantity1 = new BigDecimal("180");
    final BigDecimal quantity2 = new BigDecimal("80");
    final BigDecimal half = new BigDecimal("0.5");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingR2", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity1, day1);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = TestCostingUtils.createGoodsShipment(product, price1,
          quantity2, day2);

      // Update purchase order line product price
      TestCostingUtils.updatePurchaseOrder(purchaseOrder, price2);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Cancel goods shipment
      ShipmentInOut goodsShipment2 = TestCostingUtils.cancelGoodsShipment(goodsShipment1,
          price2.multiply(half));

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2, true));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2.multiply(half), price2, true));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price2, price1, price2, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), price2,
          TestCostingUtils.getProductCostings(product.getId()).get(1).getOriginalCost(), price2,
          quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day1, true));
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day2, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day2, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList.add(new DocumentPostAssert("35000",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList.add(new DocumentPostAssert("99900",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingR22() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("25.00");
    final BigDecimal price2 = new BigDecimal("20.00");
    final BigDecimal quantity1 = new BigDecimal("330");
    final BigDecimal quantity2 = new BigDecimal("170");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingR22", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity1, day1);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = TestCostingUtils.createGoodsShipment(product, price1,
          quantity2, day3);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Cancel goods shipment
      ShipmentInOut goodsShipment2 = TestCostingUtils.cancelGoodsShipment(goodsShipment1, price1);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt, price2, quantity1, day2);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1, true));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1, true));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price2, price1, price2, quantity1));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(2), price1, price1, price2, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day2, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99904",
          quantity1.multiply(price1).add(quantity1.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity1.multiply(price1).add(quantity1.multiply(price2).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingR3() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final BigDecimal price1 = new BigDecimal("15.00");
    final BigDecimal price2 = new BigDecimal("7.50");
    final BigDecimal quantity1 = new BigDecimal("180");
    final BigDecimal two = new BigDecimal("2");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingR3", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity1, day1);

      // Update purchase order line product price
      TestCostingUtils.updatePurchaseOrder(purchaseOrder, price2);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Cancel goods receipt
      ShipmentInOut goodsReceipt2 = TestCostingUtils.cancelGoodsReceipt(goodsReceipt1,
          price2.multiply(two));

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2.multiply(two), price2, true));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2, true));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(1), price2, price1, price2, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0),
          price2.multiply(two), null, price2, BigDecimal.ZERO));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day1, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day1, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99904",
          quantity1.multiply(price1).add(quantity1.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity1.multiply(price1).add(quantity1.multiply(price2).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingIC3() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("35.00");
    final BigDecimal quantity1 = new BigDecimal("1000");
    final BigDecimal quantity2 = new BigDecimal("250");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingIC3", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity1, day1);

      // Create internal consumption, run costing background, post it and assert
      // it
      InternalConsumption internalConsumtpion = TestCostingUtils.createInternalConsumption(product,
          price1, quantity2, day2);

      // Cancel Cost Adjustment
      TestCostingUtils.cancelInternalConsumption(internalConsumtpion);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1, false));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalConsumption.class, internalConsumtpion.getId())
          .getMaterialMgmtInternalConsumptionLineList()
          .get(0), price1, price1, true));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalConsumption.class, internalConsumtpion.getId())
          .getMaterialMgmtInternalConsumptionLineList()
          .get(0)
          .getMaterialMgmtInternalConsumptionLineVoidedInternalConsumptionLineList()
          .get(0), price1, price1, true));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price1, null, price1, quantity1));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(2), price1, null, price1, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      assertEquals(TestCostingUtils.getCostAdjustment(product.getId()), null);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingMCC1() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final BigDecimal price1 = new BigDecimal("11.50");
    final BigDecimal price2 = new BigDecimal("15.00");
    final BigDecimal price3 = new BigDecimal("15.0714");
    final BigDecimal price4 = new BigDecimal("14.9375");
    final BigDecimal price5 = new BigDecimal("11.4375");
    final BigDecimal quantity1 = new BigDecimal("15");
    final BigDecimal quantity2 = new BigDecimal("7");
    final BigDecimal quantity3 = new BigDecimal("3");
    final BigDecimal amount1 = new BigDecimal("0.50");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingMCC1", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity1, day0);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = TestCostingUtils.createGoodsShipment(product, price1,
          quantity2, day2);

      // Update transaction total cost amount
      TestCostingUtils.manualCostAdjustment(
          TestCostingUtils.getProductTransactions(product.getId()).get(1), amount1, true, false,
          day3);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt, price2, quantity1, day4);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = TestCostingUtils.createGoodsShipment(product, price4,
          quantity3, day5);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price4, price4));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price2, price1, price2, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price3, price5,
          price4, quantity1.add(quantity2.negate())));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1
          .add(new CostAdjustmentAssert(transactionList.get(1), "MCC", amount1, day3, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99900", amount1, BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, amount1, null));
      CostAdjustment costAdjustment1 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment1, product.getId(),
          documentPostAssertList1);

      // Post cost adjustment 1 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList2.add(new DocumentPostAssert("99900",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), null));
      CostAdjustment costAdjustment2 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(1).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment2, product.getId(),
          documentPostAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingBOM() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("15.00");
    final BigDecimal price2 = new BigDecimal("25.00");
    final BigDecimal price3 = new BigDecimal("17.50");
    final BigDecimal price4 = new BigDecimal("31.50");
    final BigDecimal price5 = new BigDecimal("95.00");
    final BigDecimal price6 = new BigDecimal("115.50");
    final BigDecimal quantity1 = new BigDecimal("3");
    final BigDecimal quantity2 = new BigDecimal("2");
    final BigDecimal quantity3 = new BigDecimal("30");
    final BigDecimal quantity4 = new BigDecimal("20");
    final BigDecimal quantity5 = new BigDecimal("10");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = TestCostingUtils.createProduct("testCostingBOMA", price1);

      // Create a new product for the test
      Product product2 = TestCostingUtils.createProduct("testCostingBOMB", price2);

      // Create a new product for the test
      List<Product> productList = new ArrayList<Product>();
      productList.add(product1);
      productList.add(product2);
      List<BigDecimal> quantityList = new ArrayList<BigDecimal>();
      quantityList.add(quantity1);
      quantityList.add(quantity2);
      Product product3 = TestCostingUtils.createProduct("testCostingBOMC", productList,
          quantityList);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product1, price1, quantity3,
          day0);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product2, price2, quantity4,
          day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrder1, price1,
          quantity3, TestCostingConstants.LOCATOR_M01_ID, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrder2, price2,
          quantity4, TestCostingConstants.LOCATOR_M01_ID, day2);

      // Create bill of materials production, run costing background, post it and
      // assert it
      ProductionTransaction billOfMaterialsProduction = TestCostingUtils
          .createBillOfMaterialsProduction(product3, quantity5, TestCostingConstants.LOCATOR_L01_ID,
              day3);

      // Create purchase invoice, post it and assert it
      List<ShipmentInOut> goodsReceiptList = new ArrayList<ShipmentInOut>();
      goodsReceiptList.add(goodsReceipt1);
      goodsReceiptList.add(goodsReceipt2);
      List<BigDecimal> priceList = new ArrayList<BigDecimal>();
      priceList.add(price3);
      priceList.add(price4);
      TestCostingUtils.createPurchaseInvoice(goodsReceiptList, priceList, quantity3.add(quantity4),
          day4);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList1.add(new ProductTransactionAssert(
          TestCostingUtils.getProductionLines(billOfMaterialsProduction.getId()).get(0), price1,
          price3));
      TestCostingUtils.assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price4));
      productTransactionAssertList2.add(new ProductTransactionAssert(
          TestCostingUtils.getProductionLines(billOfMaterialsProduction.getId()).get(1), price2,
          price4));
      TestCostingUtils.assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product transactions 3
      List<ProductTransactionAssert> productTransactionAssertList3 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList3.add(new ProductTransactionAssert(
          TestCostingUtils.getProductionLines(billOfMaterialsProduction.getId()).get(2), price5,
          price6));
      TestCostingUtils.assertProductTransaction(product3.getId(), productTransactionAssertList3);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = TestCostingUtils
          .getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(0),
          TestCostingConstants.SPAIN_EAST_WAREHOUSE_ID, price3, price1, price3, quantity3));
      TestCostingUtils.assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = TestCostingUtils
          .getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(0),
          TestCostingConstants.SPAIN_EAST_WAREHOUSE_ID, price4, price2, price4, quantity4));
      TestCostingUtils.assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert product costing 3
      List<MaterialTransaction> transactionList3 = TestCostingUtils
          .getProductTransactions(product3.getId());
      List<ProductCostingAssert> productCostingAssertList3 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(0),
          TestCostingConstants.SPAIN_WAREHOUSE_ID, price6, price5, price6, quantity5));
      TestCostingUtils.assertProductCosting(product3.getId(), productCostingAssertList3);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = TestCostingUtils
          .getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), day4, true));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(1), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day4, false));
      BigDecimal previousAdjustmentAmount1 = quantity3.multiply(price3)
          .add(quantity3.multiply(price1).negate());
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList3.get(0), "PDC",
          quantity4.multiply(price4)
              .add(quantity4.multiply(price2).negate())
              .add(previousAdjustmentAmount1),
          day4, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(1), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), day4, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = TestCostingUtils
          .getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(0), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), day4, true));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(1), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day4, false));
      BigDecimal previousAdjustmentAmount2 = quantity3.multiply(price3)
          .add(quantity3.multiply(price1).negate());
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList3.get(0), "PDC",
          quantity4.multiply(price4)
              .add(quantity4.multiply(price2).negate())
              .add(previousAdjustmentAmount2),
          day4, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(1), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), day4, false));
      // costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList3.get(0), "PDC",
      // quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), day4, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Assert cost adjustment 3
      List<CostAdjustment> costAdjustmentList3 = TestCostingUtils
          .getCostAdjustment(product3.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList3 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList31 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList31.add(new CostAdjustmentAssert(transactionList1.get(0), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList31.add(new CostAdjustmentAssert(transactionList2.get(0), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), day4, true));
      costAdjustmentAssertLineList31.add(new CostAdjustmentAssert(transactionList1.get(1), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day4, false));
      BigDecimal previousAdjustmentAmount3 = quantity3.multiply(price3)
          .add(quantity3.multiply(price1).negate());
      costAdjustmentAssertLineList31.add(new CostAdjustmentAssert(transactionList3.get(0), "PDC",
          quantity4.multiply(price4)
              .add(quantity4.multiply(price2).negate())
              .add(previousAdjustmentAmount3),
          day4, false));
      costAdjustmentAssertLineList31.add(new CostAdjustmentAssert(transactionList2.get(1), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), day4, false));
      // costAdjustmentAssertLineList31.add(new CostAdjustmentAssert(transactionList3.get(0), "PDC",
      // quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), day4, false));
      costAdjustmentAssertList3.add(costAdjustmentAssertLineList31);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList3, costAdjustmentAssertList3);

      // Post cost adjustment 1 and assert it
      TestCostingUtils.postDocument(costAdjustmentList1.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert(product1.getId(), "99904", BigDecimal.ZERO,
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert(product1.getId(), "35000",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert(product2.getId(), "99904", BigDecimal.ZERO,
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert(product2.getId(), "35000",
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert(product1.getId(), "61000",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert(product1.getId(), "35000", BigDecimal.ZERO,
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), null));
      // documentPostAssertList1.add(new DocumentPostAssert(product3.getId(), "61000",
      // BigDecimal.ZERO, quantity3.multiply(price3).add(quantity3.multiply(price1).negate()),
      // null));
      // documentPostAssertList1.add(new DocumentPostAssert(product3.getId(), "35000", quantity3
      // .multiply(price3).add(quantity3.multiply(price1).negate()), BigDecimal.ZERO, null));
      BigDecimal previouslyAdjustedAmountPost = quantity3.multiply(price3)
          .add(quantity3.multiply(price1).negate());
      documentPostAssertList1.add(new DocumentPostAssert(product3.getId(), "61000", BigDecimal.ZERO,
          quantity4.multiply(price4)
              .add(quantity4.multiply(price2).negate())
              .add(previouslyAdjustedAmountPost),
          null));
      documentPostAssertList1.add(new DocumentPostAssert(product3.getId(), "35000",
          quantity4.multiply(price4)
              .add(quantity4.multiply(price2).negate())
              .add(previouslyAdjustedAmountPost),
          BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert(product2.getId(), "61000",
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert(product2.getId(), "35000", BigDecimal.ZERO,
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), null));
      // BigDecimal previouslyAdjustedAmountPost = quantity4.multiply(price4).add(
      // quantity4.multiply(price2).negate());
      // documentPostAssertList1.add(new DocumentPostAssert(product3.getId(), "61000",
      // BigDecimal.ZERO, quantity4.multiply(price4).add(
      // quantity4.multiply(price2).negate().add(previouslyAdjustedAmountPost)), null));
      // documentPostAssertList1.add(new DocumentPostAssert(product3.getId(), "35000", quantity4
      // .multiply(price4).add(
      // quantity4.multiply(price2).negate().add(previouslyAdjustedAmountPost)),
      // BigDecimal.ZERO, null));
      CostAdjustment costAdjustment1 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList1.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment1, null, documentPostAssertList1);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

}
