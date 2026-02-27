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
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.cost.InventoryAmountUpdate;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.test.costing.assertclass.CostAdjustmentAssert;
import org.openbravo.test.costing.assertclass.DocumentPostAssert;
import org.openbravo.test.costing.assertclass.ProductCostingAssert;
import org.openbravo.test.costing.assertclass.ProductTransactionAssert;
import org.openbravo.test.costing.utils.TestCostingConstants;
import org.openbravo.test.costing.utils.TestCostingUtils;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCostingSourceAdjustments extends TestCostingBase {

  // Storage Bin with name: L02
  private static String LOCATOR2_ID = "1A11102F318D4720957B52C8719A34F2";
  // Storage Bin with name: L03
  private static String LOCATOR3_ID = "FB4D5926A1B443E68CC2DB2BBAE3315D";

  @Test
  public void testCostingDDD() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("25.00");
    final BigDecimal price2 = new BigDecimal("20.00");
    final BigDecimal price3 = new BigDecimal("24.3103");
    final BigDecimal quantity1 = new BigDecimal("580");
    final BigDecimal quantity2 = new BigDecimal("80");
    final BigDecimal quantity3 = new BigDecimal("500");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingDDD", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create purchase invoice, post it and assert it
      Invoice purchaseInvoice = TestCostingUtils.createPurchaseInvoice(purchaseOrder, price1,
          quantity1, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseInvoice, price1,
          quantity2, day2);

      // Update purchase invoice line product price
      TestCostingUtils.updatePurchaseInvoice(purchaseInvoice, price2);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseInvoice, price1,
          quantity3, day3);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price2, price1, price2, quantity2));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(1), price2, price3, price2, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day1, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity3.multiply(price2).add(quantity3.multiply(price1).negate()), day1, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904",
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), null));
      CostAdjustment costAdjustment1 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment1, product.getId(),
          documentPostAssertList1);

      // Post cost adjustment 2 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904",
          quantity3.multiply(price1).add(quantity3.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity3.multiply(price1).add(quantity3.multiply(price2).negate()), null));
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
  public void testCostingV911() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("15.00");
    final BigDecimal price2 = new BigDecimal("25.00");
    final BigDecimal price3 = new BigDecimal("35.00");
    final BigDecimal quantity1 = new BigDecimal("500");
    final BigDecimal quantity2 = new BigDecimal("400");
    final BigDecimal quantity3 = new BigDecimal("200");
    final BigDecimal quantity4 = new BigDecimal("300");
    final String costType = "AVA";

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingV911", price1, price2, costType);

      // Create purchase invoice, post it and assert it
      Invoice purchaseInvoice = TestCostingUtils.createPurchaseInvoice(product, price1, quantity1,
          day0);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = TestCostingUtils.createGoodsShipment(product, price2, quantity2,
          day1);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseInvoice, price2,
          quantity3, day2);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseInvoice, price1,
          quantity4, day3);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price3, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(null, null, null, price2, null, costType));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price1, price2,
          price1, quantity2.negate().add(quantity3)));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), price1, null,
          price1, quantity2.negate().add(quantity3).add(quantity4)));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity3.multiply(price1).add(quantity3.multiply(price2).negate()), day0, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "NSC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day2, false, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(2), "NSC",
          BigDecimal.ZERO, day3, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904",
          quantity3.multiply(price2).add(quantity3.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity3.multiply(price2).add(quantity3.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("61000", BigDecimal.ZERO,
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), BigDecimal.ZERO,
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
  public void testCostingV10() throws Exception {

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
    final BigDecimal price1 = new BigDecimal("8.00");
    final BigDecimal price2 = new BigDecimal("9.00");
    final BigDecimal price3 = new BigDecimal("8.5493");
    final BigDecimal price4 = new BigDecimal("10.00");
    final BigDecimal price5 = new BigDecimal("8.6610");
    final BigDecimal price6 = new BigDecimal("9.3390");
    final BigDecimal price7 = new BigDecimal("9.4507");
    final BigDecimal price8 = new BigDecimal("9.0186");
    final BigDecimal quantity1 = new BigDecimal("1000");
    final BigDecimal quantity2 = new BigDecimal("150");
    final BigDecimal quantity3 = new BigDecimal("250");
    final BigDecimal quantity4 = new BigDecimal("10");
    final BigDecimal quantity5 = new BigDecimal("200");
    final BigDecimal quantity6 = new BigDecimal("120");
    final BigDecimal quantity7 = new BigDecimal("50");
    final BigDecimal quantity8 = new BigDecimal("280");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingV10", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity2, day1);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity3, day2);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = TestCostingUtils.createGoodsShipment(product, price1,
          quantity4, day3);

      // Create purchase invoice, post it and assert it
      List<ShipmentInOut> goodsReceiptList1 = new ArrayList<ShipmentInOut>();
      goodsReceiptList1.add(goodsReceipt1);
      goodsReceiptList1.add(goodsReceipt2);
      List<BigDecimal> priceList1 = new ArrayList<BigDecimal>();
      priceList1.add(price2);
      priceList1.add(price2);
      TestCostingUtils.createPurchaseInvoice(goodsReceiptList1, priceList1,
          quantity2.add(quantity3), day4);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt3 = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity5, day5);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt4 = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity6, day6);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = TestCostingUtils.createGoodsShipment(product, price3,
          quantity7, day7);

      // Create purchase invoice, post it and assert it
      List<ShipmentInOut> goodsReceiptList2 = new ArrayList<ShipmentInOut>();
      goodsReceiptList2.add(goodsReceipt3);
      goodsReceiptList2.add(goodsReceipt4);
      List<BigDecimal> priceList2 = new ArrayList<BigDecimal>();
      priceList2.add(price4);
      priceList2.add(price4);
      TestCostingUtils.createPurchaseInvoice(goodsReceiptList2, priceList2,
          quantity5.add(quantity6), day8);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt5 = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity8, day9);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt4.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price7));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt5.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price2, price1, price2, quantity2));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price2, price1,
          price2, quantity2.add(quantity3)));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(3), price4, price5,
          price6, quantity2.add(quantity3).add((quantity4).negate()).add(quantity5)));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(4), price4, price3, price7,
              quantity2.add(quantity3).add((quantity4).negate()).add(quantity5).add(quantity6)));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(6), price1, null, price8,
              quantity2.add(quantity3)
                  .add((quantity4).negate())
                  .add(quantity5)
                  .add(quantity6)
                  .add((quantity7).negate())
                  .add(quantity8)));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity3.multiply(price2).add(quantity3.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity4.multiply(price2).add(quantity4.multiply(price1).negate()), day4, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(3), "PDC",
          quantity5.multiply(price4).add(quantity5.multiply(price1).negate()), day8, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(4), "PDC",
          quantity6.multiply(price4).add(quantity6.multiply(price1).negate()), day8, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(5), "PDC",
          quantity7.multiply(price7).add(quantity7.multiply(price3).negate()), day8, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity3.multiply(price2).add(quantity3.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity3.multiply(price2).add(quantity3.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("99900",
          quantity4.multiply(price2).add(quantity4.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity4.multiply(price2).add(quantity4.multiply(price1).negate()), null));
      CostAdjustment costAdjustment1 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment1, product.getId(),
          documentPostAssertList1);

      // Post cost adjustment 2 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity5.multiply(price4).add(quantity5.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000",
          quantity5.multiply(price4).add(quantity5.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity6.multiply(price4).add(quantity6.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000",
          quantity6.multiply(price4).add(quantity6.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList2.add(new DocumentPostAssert("99900",
          quantity7.multiply(price7).add(quantity7.multiply(price3).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity7.multiply(price7).add(quantity7.multiply(price3).negate()), null));
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
  public void testCostingBD3() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final int day6 = 30;
    final int day7 = 35;
    final BigDecimal price1 = new BigDecimal("120.00");
    final BigDecimal price2 = new BigDecimal("135.00");
    final BigDecimal price3 = new BigDecimal("127.50");
    final BigDecimal price4 = new BigDecimal("128.0357");
    final BigDecimal price5 = new BigDecimal("132.50");
    final BigDecimal price6 = new BigDecimal("135.00");
    final BigDecimal price7 = new BigDecimal("133.00");
    final BigDecimal quantity1 = new BigDecimal("75");
    final BigDecimal quantity2 = new BigDecimal("10");
    final BigDecimal quantity3 = new BigDecimal("50");
    final BigDecimal quantity4 = new BigDecimal("25");
    final BigDecimal quantity5 = new BigDecimal("15");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingBD3", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product, price2, quantity1, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrder2, price2,
          quantity1, day6);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrder1, price1,
          quantity1, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = TestCostingUtils.createGoodsShipment(product, price3,
          quantity2, day3);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = TestCostingUtils.createGoodsShipment(product, price4,
          quantity3, day4);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment3 = TestCostingUtils.createGoodsShipment(product, price5,
          quantity4, day5);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment4 = TestCostingUtils.createGoodsShipment(product, price6,
          quantity5, day7);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price4, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price5, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price7, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment4.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price6, price6));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price1, price3, price1, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(4), price2, price2,
          price2, quantity1.subtract(quantity2)));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(
          new CostAdjustmentAssert(transactionList.get(0), "BDT", BigDecimal.ZERO, day2, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(1), "BDT",
          quantity2.multiply(price1).add(quantity2.multiply(price3).negate()), day3, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList3 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList3.add(new CostAdjustmentAssert(transactionList.get(2), "BDT",
          quantity3.multiply(price1).add(quantity3.multiply(price4).negate()), day4, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList3);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList4 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList4.add(new CostAdjustmentAssert(transactionList.get(3), "BDT",
          quantity4.multiply(price1).add(quantity4.multiply(price5).negate()), day5, true));
      costAdjustmentAssertLineList4.add(new CostAdjustmentAssert(transactionList.get(4), "NSC",
          quantity1.multiply(price7).add(quantity1.multiply(price2).negate()), day6, false, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList4);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99900", BigDecimal.ZERO,
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      CostAdjustment costAdjustment1 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(1).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment1, product.getId(),
          documentPostAssertList1);

      // Post cost adjustment 2 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(2));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99900", BigDecimal.ZERO,
          quantity3.multiply(price4).add(quantity3.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000",
          quantity3.multiply(price4).add(quantity3.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      CostAdjustment costAdjustment2 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(2).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment2, product.getId(),
          documentPostAssertList2);

      // Post cost adjustment 3 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(3));
      List<DocumentPostAssert> documentPostAssertList3 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList3.add(new DocumentPostAssert("99900", BigDecimal.ZERO,
          quantity4.multiply(price5).add(quantity4.multiply(price1).negate()), null));
      documentPostAssertList3.add(new DocumentPostAssert("35000",
          quantity4.multiply(price5).add(quantity4.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList3.add(new DocumentPostAssert("61000",
          quantity1.multiply(price2).add(quantity1.multiply(price7).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList3.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity1.multiply(price2).add(quantity1.multiply(price7).negate()), null));
      CostAdjustment costAdjustment3 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(3).getId());
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
  public void testCostingAAA() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final BigDecimal price1 = new BigDecimal("50.00");
    final BigDecimal price2 = new BigDecimal("70.00");
    final BigDecimal price3 = new BigDecimal("80.00");
    final BigDecimal price4 = new BigDecimal("62.00");
    final BigDecimal price5 = new BigDecimal("68.00");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("150");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingAAA", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrder1, price1,
          quantity1, day1);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt1, price1, quantity1, day2);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product, price2, quantity2, day3);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrder2, price2,
          quantity2, day4);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt2, price3, quantity2, day5);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price3));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price1, null, price1, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price3, price4,
          price5, quantity1.add(quantity2)));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price2).negate()), day5, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity2.multiply(price3).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList.add(new DocumentPostAssert("35000",
          quantity2.multiply(price3).add(quantity2.multiply(price2).negate()), BigDecimal.ZERO,
          null));
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
  public void testCostingCC() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("15.00");
    final BigDecimal price2 = new BigDecimal("25.00");
    final BigDecimal quantity1 = new BigDecimal("180");
    final BigDecimal quantity2 = new BigDecimal("80");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingCC", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity1, day1);

      // Create purchase invoice, post it and assert it
      Invoice purchaseInvoice = TestCostingUtils.createPurchaseInvoice(goodsReceipt, price1,
          quantity1, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = TestCostingUtils.createGoodsShipment(product, price1, quantity2,
          day3);

      // Update purchase order line product price
      TestCostingUtils.updatePurchaseOrder(purchaseOrder, price2);

      // Update purchase invoice line product price
      TestCostingUtils.updatePurchaseInvoice(purchaseInvoice, price2);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price2, price1, price2, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day2, true));
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day3, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
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
  public void testCostingC1() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("15.00");
    final BigDecimal price2 = new BigDecimal("25.00");
    final BigDecimal quantity1 = new BigDecimal("180");
    final BigDecimal quantity2 = new BigDecimal("80");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingC1", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity1, day1);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = TestCostingUtils.createGoodsShipment(product, price1, quantity2,
          day2);

      // Update purchase order line product price
      TestCostingUtils.updatePurchaseOrder(purchaseOrder, price2);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt, price2, quantity1, day3);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price2, price1, price2, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day3, true));
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day3, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
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
  public void testCostingE1() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("111.00");
    final BigDecimal price2 = new BigDecimal("110.00");
    final BigDecimal price3 = price1.add(price2).divide(new BigDecimal("2"));
    final BigDecimal quantity1 = new BigDecimal("250");
    final BigDecimal quantity2 = new BigDecimal("150");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingE1", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity1, day1);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create inventory amount update and run costing background
      InventoryAmountUpdate inventoryAmountUpdate = TestCostingUtils
          .createInventoryAmountUpdate(product, price1, price2, quantity1, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = TestCostingUtils.createGoodsShipment(product, price3, quantity2,
          day4);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt, price1, quantity1, day3);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InventoryAmountUpdate.class, inventoryAmountUpdate.getId())
          .getInventoryAmountUpdateLineList()
          .get(0), price3, price3, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InventoryAmountUpdate.class, inventoryAmountUpdate.getId())
          .getInventoryAmountUpdateLineList()
          .get(0), price2, price2, true));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price1, null, price1, quantity1));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(2), price2, null, price3, quantity1));
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
  public void testCostingE2() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final int day6 = 30;
    final BigDecimal price1 = new BigDecimal("4.00");
    final BigDecimal price2 = new BigDecimal("3.00");
    final BigDecimal price3 = new BigDecimal("3.3333");
    final BigDecimal price4 = new BigDecimal("8.00");
    final BigDecimal price5 = new BigDecimal("7.99");
    final BigDecimal price6 = new BigDecimal("3.50");
    final BigDecimal price7 = new BigDecimal("8.01");
    final BigDecimal quantity1 = new BigDecimal("1");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingE2", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrder1, price1,
          quantity1, TestCostingConstants.LOCATOR_L01_ID, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product, price2, quantity1, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrder2, price2,
          quantity1, LOCATOR2_ID, day1);

      // Create purchase order and book it
      Order purchaseOrder3 = TestCostingUtils.createPurchaseOrder(product, price2, quantity1, day2);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt3 = TestCostingUtils.createGoodsReceipt(purchaseOrder3, price2,
          quantity1, LOCATOR3_ID, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = TestCostingUtils.createGoodsShipment(product, price3,
          quantity1, TestCostingConstants.LOCATOR_L01_ID, day3);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = TestCostingUtils.createGoodsShipment(product, price3,
          quantity1, LOCATOR2_ID, day4);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment3 = TestCostingUtils.createGoodsShipment(product, price3,
          quantity1, LOCATOR3_ID, day5);

      // Create purchase order and book it
      Order purchaseOrder4 = TestCostingUtils.createPurchaseOrder(product, price4, quantity1, day6);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt4 = TestCostingUtils.createGoodsReceipt(purchaseOrder4, price4,
          quantity1, TestCostingConstants.LOCATOR_L01_ID, day6);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt4.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price4, price5, price4));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price1, null, price1, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price2, null,
          price6, quantity1.add(quantity1)));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), price2, null,
          price3, quantity1.add(quantity1).add(quantity1)));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(6), price4, price7, price4, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(6), "NSC",
          quantity1.multiply(price5).add(quantity1.multiply(price4).negate()), day6, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("61000",
          quantity1.multiply(price4).add(quantity1.multiply(price5).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity1.multiply(price4).add(quantity1.multiply(price5).negate()), null));
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
  public void testCostingE3() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final BigDecimal price1 = new BigDecimal("4.00");
    final BigDecimal price2 = new BigDecimal("3.00");
    final BigDecimal price3 = new BigDecimal("3.3333");
    final BigDecimal price4 = new BigDecimal("3.34");
    final BigDecimal price5 = new BigDecimal("8.00");
    final BigDecimal price6 = new BigDecimal("3.34");
    final BigDecimal price7 = new BigDecimal("3.50");
    final BigDecimal price8 = price3.add(price5).divide(new BigDecimal("2"));
    final BigDecimal price9 = new BigDecimal("5.67");
    final BigDecimal quantity1 = new BigDecimal("1");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingE3", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrder1, price1,
          quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product, price2, quantity1, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrder2, price2,
          quantity1, day1);

      // Create purchase order and book it
      Order purchaseOrder3 = TestCostingUtils.createPurchaseOrder(product, price2, quantity1, day2);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt3 = TestCostingUtils.createGoodsReceipt(purchaseOrder3, price2,
          quantity1, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = TestCostingUtils.createGoodsShipment(product, price3,
          quantity1, day3);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = TestCostingUtils.createGoodsShipment(product, price3,
          quantity1, day4);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create inventory amount update and run costing background
      InventoryAmountUpdate inventoryAmountUpdate = TestCostingUtils
          .createInventoryAmountUpdate(product, price3, price4, price5, quantity1, day5);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InventoryAmountUpdate.class, inventoryAmountUpdate.getId())
          .getInventoryAmountUpdateLineList()
          .get(0), price8, price8, price8));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InventoryAmountUpdate.class, inventoryAmountUpdate.getId())
          .getInventoryAmountUpdateLineList()
          .get(0), price5, price5, true));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price1, null, price1, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price2, null,
          price7, quantity1.add(quantity1)));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), price2, null,
          price3, quantity1.add(quantity1).add(quantity1)));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(6), price5, null, price9, quantity1));
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
  public void testCostingF2() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("1230.00");
    final BigDecimal price2 = new BigDecimal("1200.00");
    final BigDecimal price3 = new BigDecimal("1500.00");
    final BigDecimal price4 = new BigDecimal("1174.50");
    final BigDecimal price5 = price1.add(price2).divide(new BigDecimal("2"));
    final BigDecimal price6 = price2.add(price3).divide(new BigDecimal("2"));
    final BigDecimal price7 = new BigDecimal("1210.5263");
    final BigDecimal quantity1 = new BigDecimal("185");
    final BigDecimal quantity2 = new BigDecimal("85");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingF2", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity1, day1);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = TestCostingUtils.createGoodsShipment(product, price1, quantity2,
          day4);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create inventory amount update and run costing background
      InventoryAmountUpdate inventoryAmountUpdate = TestCostingUtils
          .createInventoryAmountUpdate(product, price1, price2, quantity1, day2);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt, price3, quantity1, day3);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InventoryAmountUpdate.class, inventoryAmountUpdate.getId())
          .getInventoryAmountUpdateLineList()
          .get(0), price5, price6, price6));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InventoryAmountUpdate.class, inventoryAmountUpdate.getId())
          .getInventoryAmountUpdateLineList()
          .get(0), price2, price2, true));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price3, price1, price3, quantity1));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(2), price2, price7, price6, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      assertEquals(3, costAdjustmentList.size());

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
  public void testCostingGG() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final BigDecimal price1 = new BigDecimal("15.00");
    final BigDecimal price2 = new BigDecimal("12.00");
    final BigDecimal price3 = new BigDecimal("13.50");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("200");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingGG", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product, price2, quantity1, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrder1, price1,
          quantity1, day2);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrder2, price2,
          quantity1, day3);

      // Create purchase invoice, post it and assert it
      List<ShipmentInOut> goodsReceiptList = new ArrayList<ShipmentInOut>();
      goodsReceiptList.add(goodsReceipt1);
      goodsReceiptList.add(goodsReceipt2);
      List<BigDecimal> priceList = new ArrayList<BigDecimal>();
      priceList.add(price1);
      priceList.add(price2);
      TestCostingUtils.createPurchaseInvoice(goodsReceiptList, priceList, quantity2, day5);

      // Update transaction total cost amount
      TestCostingUtils.manualCostAdjustment(
          TestCostingUtils.getProductTransactions(product.getId()).get(1),
          quantity1.multiply(price1), false, day4);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1, true, false));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price1, true, true));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price1, null, price1, quantity1));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(1), price1, price3, price1, quantity2));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "MCC",
          quantity1.multiply(price1).add(quantity1.multiply(price2).negate()), day4, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity1.multiply(price1).add(quantity1.multiply(price2).negate()), null));
      documentPostAssertList.add(new DocumentPostAssert("35000",
          quantity1.multiply(price1).add(quantity1.multiply(price2).negate()), BigDecimal.ZERO,
          null));
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
  public void testCostingH1() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final int day6 = 30;
    final BigDecimal price1 = new BigDecimal("105.00");
    final BigDecimal price2 = new BigDecimal("105.50");
    final BigDecimal price3 = new BigDecimal("105.25");
    final BigDecimal price4 = new BigDecimal("106.00");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("200");
    final BigDecimal quantity3 = new BigDecimal("150");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingH1", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product, price2, quantity1, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrder1, price1,
          quantity1, day2);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrder2, price2,
          quantity1, day3);

      // Create purchase invoice, post it and assert it
      List<ShipmentInOut> goodsReceiptList = new ArrayList<ShipmentInOut>();
      goodsReceiptList.add(goodsReceipt1);
      goodsReceiptList.add(goodsReceipt2);
      List<BigDecimal> priceList = new ArrayList<BigDecimal>();
      priceList.add(price1);
      priceList.add(price2);
      TestCostingUtils.createPurchaseInvoice(goodsReceiptList, priceList, quantity2, day4);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = TestCostingUtils.createGoodsShipment(product, price3, quantity3,
          day6);

      // Update transaction total cost amount
      TestCostingUtils.manualCostAdjustment(
          TestCostingUtils.getProductTransactions(product.getId()).get(1),
          quantity1.multiply(price4), false, day5);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1, true, false));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price4, true, true));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price2));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price1, null, price1, quantity1));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(1), price4, price3, price2, quantity2));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "MCC",
          quantity1.multiply(price4).add(quantity1.multiply(price2).negate()), day5, true));
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(2), "MCC",
          quantity3.multiply(price2).add(quantity3.multiply(price3).negate()), day6, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity1.multiply(price4).add(quantity1.multiply(price2).negate()), null));
      documentPostAssertList.add(new DocumentPostAssert("35000",
          quantity1.multiply(price4).add(quantity1.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList.add(new DocumentPostAssert("99900",
          quantity3.multiply(price2).add(quantity3.multiply(price3).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity3.multiply(price2).add(quantity3.multiply(price3).negate()), null));
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
  public void testCostingII() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int year = 1;
    final BigDecimal price1 = new BigDecimal("95.00");
    final BigDecimal price2 = new BigDecimal("100.00");
    final BigDecimal quantity1 = new BigDecimal("1500");
    final BigDecimal quantity2 = new BigDecimal("100");
    final String costType = "STA";

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingII", price1, price2, costType,
          year);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(product, price2, quantity1,
          day0);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = TestCostingUtils.createGoodsShipment(product, price2, quantity2,
          day2);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt, price1, quantity1, day1);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(null, null, null, price2, null, costType, year));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price1, price2, price1, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price1).add(quantity1.multiply(price2).negate()), day1, true));
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), day2, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99904",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList.add(new DocumentPostAssert("99900", BigDecimal.ZERO,
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), null));
      documentPostAssertList.add(new DocumentPostAssert("35000",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), BigDecimal.ZERO,
          null));
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
  public void testCostingJJ() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int year = -1;
    final BigDecimal price1 = new BigDecimal("95.00");
    final BigDecimal price2 = new BigDecimal("100.00");
    final BigDecimal price3 = new BigDecimal("195.00");
    final BigDecimal quantity1 = new BigDecimal("1500");
    final BigDecimal quantity2 = new BigDecimal("500");
    final String costType = "STA";

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingJJ", price1, price2, costType,
          year);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(product, price1, quantity1,
          day0);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = TestCostingUtils.createGoodsShipment(product, price1, quantity2,
          day2);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt, price3, quantity1, day1);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

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
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(null, null, null, price2, null, costType, year));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price3, price1, price3, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), day1, true));
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), day2, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList.add(new DocumentPostAssert("35000",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList.add(new DocumentPostAssert("99900",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), null));
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
  public void testCostingJJJ() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int year = -1;
    final BigDecimal price1 = new BigDecimal("95.00");
    final BigDecimal price2 = new BigDecimal("100.00");
    final BigDecimal price3 = new BigDecimal("195.00");
    final BigDecimal quantity1 = new BigDecimal("1500");
    final BigDecimal quantity2 = new BigDecimal("500");
    final BigDecimal quantity3 = new BigDecimal("50");
    final String costType = "STA";

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingJJJ", price1, price2, costType,
          year);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(product, price1, quantity1,
          day0);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = TestCostingUtils.createGoodsShipment(product, price1,
          quantity2, day1);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt, price3, quantity1, day2);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = TestCostingUtils.createGoodsShipment(product, price3,
          quantity3, day3);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(null, null, null, price2, null, costType, year));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price3, price1, price3, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), day2, true));
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), day2, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList.add(new DocumentPostAssert("35000",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList.add(new DocumentPostAssert("99900",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), null));
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
  public void testCostingK2() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int year = 1;
    final BigDecimal price1 = new BigDecimal("95.00");
    final BigDecimal price2 = new BigDecimal("100.00");
    final BigDecimal price3 = new BigDecimal("80.00");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("90");
    final BigDecimal quantity3 = new BigDecimal("45");
    final String costType = "STA";

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingK2", price1, price2, costType,
          year);

      // Create purchase order and book it
      TestCostingUtils.createPurchaseOrder(product, price3, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(product, price3, quantity2,
          day1);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = TestCostingUtils.createGoodsShipment(product, price3, quantity3,
          day2);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt, price1, quantity2, day2);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList1 = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1
          .add(new ProductCostingAssert(null, null, null, price2, null, costType, year));
      productCostingAssertList1.add(
          new ProductCostingAssert(transactionList1.get(0), price1, price3, price1, quantity2));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList1 = TestCostingUtils
          .getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList1.get(0), "PDC",
          quantity2.multiply(price1).add(quantity2.multiply(price3).negate()), day2, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList1.get(1), "PDC",
          quantity3.multiply(price1).add(quantity3.multiply(price3).negate()), day2, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList1);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList1.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity2.multiply(price1).add(quantity2.multiply(price3).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity2.multiply(price1).add(quantity2.multiply(price3).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("99900",
          quantity3.multiply(price1).add(quantity3.multiply(price3).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity3.multiply(price1).add(quantity3.multiply(price3).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList1.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList1);

      // Cancel Cost Adjustment
      TestCostingUtils.cancelCostAdjustment(costAdjustment);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList2);

      // Assert product costing
      List<MaterialTransaction> transactionList2 = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2
          .add(new ProductCostingAssert(null, null, null, price2, null, costType, year));
      productCostingAssertList2.add(
          new ProductCostingAssert(transactionList2.get(0), price3, price3, price3, quantity2));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList2);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList2 = TestCostingUtils
          .getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0), "PDC",
          quantity2.multiply(price1).add(quantity2.multiply(price3).negate()), day2, true, "VO"));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(1), "PDC",
          quantity3.multiply(price1).add(quantity3.multiply(price3).negate()), day2, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList22 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(0), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), day2, true, "VO"));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(1), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day2, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList22);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Post cost adjustment 1 and assert it
      List<DocumentPostAssert> documentPostAssertList21 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList21.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity2.multiply(price1).add(quantity2.multiply(price3).negate()), null));
      documentPostAssertList21.add(new DocumentPostAssert("35000",
          quantity2.multiply(price1).add(quantity2.multiply(price3).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList21.add(new DocumentPostAssert("99900",
          quantity3.multiply(price1).add(quantity3.multiply(price3).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList21.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity3.multiply(price1).add(quantity3.multiply(price3).negate()), null));
      CostAdjustment costAdjustment21 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList2.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment21, product.getId(),
          documentPostAssertList21);

      // Post cost adjustment 2 and assert it
      TestCostingUtils.postDocument(costAdjustmentList2.get(1));
      List<DocumentPostAssert> documentPostAssertList22 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList22.add(new DocumentPostAssert("99904",
          quantity2.multiply(price1).add(quantity2.multiply(price3).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList22.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity2.multiply(price1).add(quantity2.multiply(price3).negate()), null));
      documentPostAssertList22.add(new DocumentPostAssert("99900", BigDecimal.ZERO,
          quantity3.multiply(price1).add(quantity3.multiply(price3).negate()), null));
      documentPostAssertList22.add(new DocumentPostAssert("35000",
          quantity3.multiply(price1).add(quantity3.multiply(price3).negate()), BigDecimal.ZERO,
          null));
      CostAdjustment costAdjustment22 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList2.get(1).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment22, product.getId(),
          documentPostAssertList22);

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
  public void testCostingN0() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("10.00");
    final BigDecimal price2 = new BigDecimal("20.00");
    final BigDecimal price3 = new BigDecimal("30.00");
    final BigDecimal price4 = new BigDecimal("15.00");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("5");
    final BigDecimal quantity3 = new BigDecimal("10");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingN0", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrder1, price1,
          quantity1, day1);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = TestCostingUtils.createGoodsShipment(product, price1,
          quantity1, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = TestCostingUtils.createGoodsShipment(product, price1,
          quantity2, day3);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product, price2, quantity3, day4);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrder2, price2,
          quantity3, day4);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price4, price2));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price1, null, price1, quantity1));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(3), price2, price3, price2, quantity2));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(3), "NSC",
          quantity3.multiply(price4).add(quantity3.multiply(price2).negate()), day4, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("61000",
          quantity3.multiply(price2).add(quantity3.multiply(price4).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity3.multiply(price2).add(quantity3.multiply(price4).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList1);

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
  public void testCostingN1() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int year = 1;
    final BigDecimal price1 = new BigDecimal("20.00");
    final BigDecimal price2 = new BigDecimal("15.00");
    final BigDecimal price3 = new BigDecimal("10.00");
    final BigDecimal price4 = new BigDecimal("17.50");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("200");
    final String costType = "AVA";

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingN0", price1, price1, costType,
          year);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = TestCostingUtils.createGoodsShipment(product, price1, quantity1,
          day0);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price2, quantity2, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price2,
          quantity2, day1);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price4, price2));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(null, null, null, price1, null, costType, year));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(1), price2, price3, price2, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "NSC",
          quantity2.multiply(price4).add(quantity2.multiply(price2).negate()), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("61000", BigDecimal.ZERO,
          quantity2.multiply(price4).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity2.multiply(price4).add(quantity2.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      CostAdjustment costAdjustment = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList1);

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
  public void testCostingN2() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("5.00");
    final BigDecimal quantity1 = new BigDecimal("10");
    final BigDecimal quantity2 = new BigDecimal("8");
    final BigDecimal quantity3 = new BigDecimal("12");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingN2", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(product, price1, quantity1,
          day0);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = TestCostingUtils.createGoodsShipment(product, price1,
          quantity2, day2);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(product, price1, quantity1,
          day3);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = TestCostingUtils.createGoodsShipment(product, price1,
          quantity1, day1);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price1, null, price1, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(3), price1, null,
          price1, quantity3.subtract(quantity1)));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(
          new CostAdjustmentAssert(transactionList.get(1), "BDT", BigDecimal.ZERO, day1, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

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
  public void testCostingN5() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("5.00");
    final BigDecimal price2 = new BigDecimal("7.00");
    final BigDecimal price3 = new BigDecimal("6.6667");
    final BigDecimal price4 = new BigDecimal("5.40");
    final BigDecimal quantity1 = new BigDecimal("10");
    final BigDecimal quantity2 = new BigDecimal("8");
    final BigDecimal quantity3 = new BigDecimal("12");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingN5", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(product, price1, quantity1,
          day0);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = TestCostingUtils.createGoodsShipment(product, price1,
          quantity2, day2);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price2, quantity1, day3);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrder, price2,
          quantity1, day4);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = TestCostingUtils.createGoodsShipment(product, price3,
          quantity1, day1);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price4, price2));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price1, null, price1, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(3), price2, price3,
          price2, quantity3.subtract(quantity1)));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "BDT",
          quantity1.multiply(price1).add(quantity1.multiply(price3).negate()), day1, true));
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(3), "NSC",
          quantity1.multiply(price4).add(quantity1.multiply(price2).negate()), day4, false, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99900", BigDecimal.ZERO,
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("61000",
          quantity1.multiply(price2).add(quantity1.multiply(price4).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity1.multiply(price2).add(quantity1.multiply(price4).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList1);

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
  public void testCostingV11() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final BigDecimal price1 = new BigDecimal("20.00");
    final BigDecimal price2 = new BigDecimal("15.00");
    final BigDecimal price3 = new BigDecimal("16.6667");
    final BigDecimal price4 = new BigDecimal("25.00");
    final BigDecimal price5 = new BigDecimal("18.3333");
    final BigDecimal price6 = new BigDecimal("21.6667");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("200");
    final BigDecimal quantity3 = new BigDecimal("300");
    final BigDecimal quantity4 = new BigDecimal("150");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingV11", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product, price2, quantity2, day1);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList = new ArrayList<Order>();
      purchaseOrderList.add(purchaseOrder1);
      purchaseOrderList.add(purchaseOrder2);
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrderList, price3,
          quantity3, day2);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = TestCostingUtils.createGoodsShipment(product, price3, quantity4,
          day5);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(purchaseOrder1, price4, quantity1, day3);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(purchaseOrder2, price1, quantity2, day4);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price2, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price6));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(1), price1, price2, price1, quantity2));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price4, price3, price6, quantity3));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price4).add(quantity1.multiply(price1).negate()), day3, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity4.multiply(price5).add(quantity4.multiply(price3).negate()), day5, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), day4, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity4.multiply(price6).add(quantity4.multiply(price5).negate()), day5, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity1.multiply(price4).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity1.multiply(price4).add(quantity1.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("99900",
          quantity4.multiply(price5).add(quantity4.multiply(price3).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity4.multiply(price5).add(quantity4.multiply(price3).negate()), null));
      CostAdjustment costAdjustment1 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment1, product.getId(),
          documentPostAssertList1);

      // Post cost adjustment 2 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000",
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList2.add(new DocumentPostAssert("99900",
          quantity4.multiply(price6).add(quantity4.multiply(price5).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity4.multiply(price6).add(quantity4.multiply(price5).negate()), null));
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
  public void testCostingV221() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("15.00");
    final BigDecimal price2 = new BigDecimal("12.00");
    final BigDecimal price3 = new BigDecimal("20.00");
    final BigDecimal price4 = new BigDecimal("18.00");
    final BigDecimal price5 = new BigDecimal("19.80");
    final BigDecimal quantity1 = new BigDecimal("500");
    final BigDecimal quantity2 = new BigDecimal("700");
    final BigDecimal quantity3 = new BigDecimal("150");
    final BigDecimal quantity4 = new BigDecimal("600");
    final BigDecimal quantity5 = new BigDecimal("50");
    final BigDecimal amount1 = new BigDecimal("1350.00");
    final BigDecimal amount2 = new BigDecimal("-450.00");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingV221", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product, price2, quantity2, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrder1, price1,
          quantity3, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = TestCostingUtils.createGoodsShipment(product, price1, quantity4,
          day3);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrder2, price2,
          quantity1, day4);

      // Create purchase invoice, post it and assert it
      List<Order> purchaseOrderList = new ArrayList<Order>();
      purchaseOrderList.add(purchaseOrder1);
      purchaseOrderList.add(purchaseOrder2);
      List<BigDecimal> priceList = new ArrayList<BigDecimal>();
      priceList.add(price3);
      priceList.add(price4);
      List<BigDecimal> quantityList = new ArrayList<BigDecimal>();
      quantityList.add(quantity3);
      quantityList.add(quantity1);
      TestCostingUtils.createPurchaseInvoice(purchaseOrderList, priceList, quantityList, day4);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price5, price4));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price3, price1, price3, quantity3));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), price4,
          price1.negate(), price4, quantity5));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1
          .add(new CostAdjustmentAssert(transactionList.get(2), "NSC", amount1, day4, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity4.multiply(price3).add(quantity4.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity1.multiply(price4).add(quantity1.multiply(price2).negate()), day4, false));
      costAdjustmentAssertLineList2.add(
          new CostAdjustmentAssert(transactionList.get(2), "NSC", amount2, day4, false, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("61000", BigDecimal.ZERO, amount1, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", amount1, BigDecimal.ZERO, null));
      CostAdjustment costAdjustment1 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment1, product.getId(),
          documentPostAssertList1);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity4.multiply(price3).add(quantity4.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000",
          quantity4.multiply(price3).add(quantity4.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList2.add(new DocumentPostAssert("99900",
          quantity1.multiply(price4).add(quantity1.multiply(price2).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity1.multiply(price4).add(quantity1.multiply(price2).negate()), null));
      documentPostAssertList2
          .add(new DocumentPostAssert("61000", amount2.negate(), BigDecimal.ZERO, null));
      documentPostAssertList2
          .add(new DocumentPostAssert("35000", BigDecimal.ZERO, amount2.negate(), null));
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
  public void testCostingMC444() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("444.00");
    final BigDecimal price2 = new BigDecimal("355.20");
    final BigDecimal price3 = new BigDecimal("177.60");
    final BigDecimal quantity1 = BigDecimal.ONE;
    final BigDecimal rate = new BigDecimal("0.80");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingMC444", price1, DOLLAR_ID);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity1, day1);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt, price1, quantity1, rate, day2);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price2));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1
          .add(new ProductCostingAssert(transactionList.get(0), price2, price3, price2, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price2).add(quantity1.multiply(price3).negate()), day2, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity1.multiply(price2).add(quantity1.multiply(price3).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity1.multiply(price2).add(quantity1.multiply(price3).negate()), BigDecimal.ZERO,
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
  public void testCostingMC445() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final BigDecimal price1 = new BigDecimal("500.00");
    final BigDecimal price2 = new BigDecimal("600.00");
    final BigDecimal price3 = new BigDecimal("700.00");
    final BigDecimal price4 = new BigDecimal("1500.00");
    final BigDecimal price5 = new BigDecimal("1750.00");
    final BigDecimal price6 = new BigDecimal("1375.00");
    final BigDecimal quantity1 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingMC445", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrder1, price1,
          quantity1, day1);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt1, price1, quantity1, day2);

      // Change organization currency
      TestCostingUtils.changeOrganizationCurrency(TestCostingConstants.SPAIN_ORGANIZATION_ID,
          DOLLAR_ID);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product, price2, quantity1, day3);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrder2, price2,
          quantity1, day4);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(goodsReceipt2, price3, quantity1, day5);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), EURO_ID, price1, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), DOLLAR_ID, price4, price5));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1
          .add(new ProductCostingAssert(transactionList.get(0), price1, null, price1, quantity1));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(1), price5, price6,
          price4, quantity1.add(quantity1)));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), DOLLAR_ID,
          "PDC", quantity1.multiply(price5).add(quantity1.multiply(price4).negate()), day5, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity1.multiply(price3).add(quantity1.multiply(price2).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity1.multiply(price3).add(quantity1.multiply(price2).negate()), BigDecimal.ZERO,
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
      // Change organization currency
      TestCostingUtils.changeOrganizationCurrency(TestCostingConstants.SPAIN_ORGANIZATION_ID,
          EURO_ID);

      OBContext.restorePreviousMode();
    }
  }

}
