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
import static org.junit.Assert.assertTrue;

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
import org.openbravo.model.materialmgmt.cost.LandedCost;
import org.openbravo.model.materialmgmt.cost.LandedCostCost;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.test.costing.assertclass.CostAdjustmentAssert;
import org.openbravo.test.costing.assertclass.DocumentPostAssert;
import org.openbravo.test.costing.assertclass.ProductCostingAssert;
import org.openbravo.test.costing.assertclass.ProductTransactionAssert;
import org.openbravo.test.costing.utils.TestCostingConstants;
import org.openbravo.test.costing.utils.TestCostingUtils;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCostingLandedCost extends TestCostingBase {

  @Test
  public void testCostingLC100LC200() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("150.00");
    final BigDecimal price2 = new BigDecimal("185.00");
    final BigDecimal price3 = new BigDecimal("535.00");
    final BigDecimal price4 = new BigDecimal("105.00");
    final BigDecimal price5 = new BigDecimal("151.8462");
    final BigDecimal price6 = new BigDecimal("187.2769");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("200");
    final BigDecimal quantity3 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = TestCostingUtils.createProduct("testCostingLC100LC200A", price1);

      // Create a new product for the test
      Product product2 = TestCostingUtils.createProduct("testCostingLC100LC200B", price2);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product1, price1, quantity1,
          day0);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product2, price2, quantity2,
          day0);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_FEES_ID, price3, quantity3, day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID, price4, quantity3, day2);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList = new ArrayList<Order>();
      purchaseOrderList.add(purchaseOrder1);
      purchaseOrderList.add(purchaseOrder2);
      List<Invoice> invoiceList = new ArrayList<Invoice>();
      invoiceList.add(purchaseInvoiceLandedCost1);
      invoiceList.add(purchaseInvoiceLandedCost2);
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrderList, null,
          quantity1.add(quantity2), day3, invoiceList);

      // Post landed cost and assert it
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      TestCostingUtils.postLandedCost(OBDal.getInstance()
          .get(LandedCost.class,
              goodsReceipt.getLandedCostCostList().get(0).getLandedCost().getId()));

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price5, price1));
      TestCostingUtils.assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price2, price6, price2));
      TestCostingUtils.assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = TestCostingUtils
          .getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(
          new ProductCostingAssert(transactionList1.get(0), price5, price1, price5, quantity1));
      TestCostingUtils.assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = TestCostingUtils
          .getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(
          new ProductCostingAssert(transactionList2.get(0), price6, price2, price6, quantity2));
      TestCostingUtils.assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = TestCostingUtils
          .getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price5).add(quantity1.multiply(price1).negate()), day0, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price6).add(quantity2.multiply(price2).negate()), day0, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = TestCostingUtils
          .getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price5).add(quantity1.multiply(price1).negate()), day0, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price6).add(quantity2.multiply(price2).negate()), day0, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

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
  public void testCostingLC101LC201() throws Exception {

    final int day0 = 0;
    final BigDecimal price1 = new BigDecimal("150.00");
    final BigDecimal price2 = new BigDecimal("185.00");
    final BigDecimal price3 = new BigDecimal("535.00");
    final BigDecimal price4 = new BigDecimal("105.00");
    final BigDecimal price5 = new BigDecimal("20.00");
    final BigDecimal price6 = new BigDecimal("151.8693");
    final BigDecimal price7 = new BigDecimal("187.30535");
    final BigDecimal price8 = new BigDecimal("149.9423");
    final BigDecimal price9 = new BigDecimal("184.92885");
    final BigDecimal price10 = new BigDecimal("148.1538");
    final BigDecimal price11 = new BigDecimal("182.7231");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("200");
    final BigDecimal quantity3 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = TestCostingUtils.createProduct("testCostingLC101LC201A", price1);

      // Create a new product for the test
      Product product2 = TestCostingUtils.createProduct("testCostingLC101LC201B", price2);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product1, price1, quantity1,
          day0);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product2, price2, quantity2,
          day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList = new ArrayList<Order>();
      purchaseOrderList.add(purchaseOrder1);
      purchaseOrderList.add(purchaseOrder2);
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrderList, null,
          quantity1.add(quantity2), day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity3.multiply(price3));
      amountList.add(quantity3.multiply(price4));
      amountList.add(quantity3.multiply(price5));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList, receiptList, null, day0);

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      TestCostingUtils.assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price2, price7, price2));
      TestCostingUtils.assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = TestCostingUtils
          .getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(
          new ProductCostingAssert(transactionList1.get(0), price6, price1, price6, quantity1));
      TestCostingUtils.assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = TestCostingUtils
          .getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(
          new ProductCostingAssert(transactionList2.get(0), price7, price2, price7, quantity2));
      TestCostingUtils.assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = TestCostingUtils
          .getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0),
          DOLLAR_ID, "LC", quantity1.multiply(price1).add(quantity1.multiply(price8).negate()),
          day0, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), EURO_ID,
          "LC", quantity1.multiply(price1).add(quantity1.multiply(price10).negate()), day0, true,
          false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0),
          DOLLAR_ID, "LC", quantity2.multiply(price2).add(quantity2.multiply(price9).negate()),
          day0, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), EURO_ID,
          "LC", quantity2.multiply(price2).add(quantity2.multiply(price11).negate()), day0, true,
          false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = TestCostingUtils
          .getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(0),
          DOLLAR_ID, "LC", quantity1.multiply(price1).add(quantity1.multiply(price8).negate()),
          day0, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(0), EURO_ID,
          "LC", quantity1.multiply(price1).add(quantity1.multiply(price10).negate()), day0, true,
          false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0),
          DOLLAR_ID, "LC", quantity2.multiply(price2).add(quantity2.multiply(price9).negate()),
          day0, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0), EURO_ID,
          "LC", quantity2.multiply(price2).add(quantity2.multiply(price11).negate()), day0, true,
          false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

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
  public void testCostingLC400() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final BigDecimal price1 = new BigDecimal("150.00");
    final BigDecimal price2 = new BigDecimal("400.00");
    final BigDecimal price3 = new BigDecimal("535.00");
    final BigDecimal price4 = new BigDecimal("105.00");
    final BigDecimal price5 = new BigDecimal("20.00");
    final BigDecimal price6 = new BigDecimal("600.00");
    final BigDecimal price7 = new BigDecimal("150.00");
    final BigDecimal price8 = new BigDecimal("25.00");
    final BigDecimal price9 = new BigDecimal("153.7377");
    final BigDecimal price10 = new BigDecimal("160.00");
    final BigDecimal price11 = new BigDecimal("163.9868");
    final BigDecimal quantity1 = new BigDecimal("50");
    final BigDecimal quantity2 = new BigDecimal("150");
    final BigDecimal quantity3 = BigDecimal.ONE;
    final BigDecimal amount1 = new BigDecimal("5.25");
    final BigDecimal amount2 = new BigDecimal("167.87");
    final BigDecimal amount3 = new BigDecimal("14.75");
    final BigDecimal amount4 = new BigDecimal("472.13");
    final BigDecimal amount5 = new BigDecimal("17.05");
    final BigDecimal amount6 = new BigDecimal("47.95");
    final BigDecimal amount7 = new BigDecimal("11.80");
    final BigDecimal amount8 = new BigDecimal("33.20");
    final BigDecimal amount9 = new BigDecimal("1.31");
    final BigDecimal amount10 = new BigDecimal("3.69");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = TestCostingUtils.createProduct("testCostingLC400A", price1, EURO_ID);

      // Create a new product for the test
      Product product2 = TestCostingUtils.createProduct("testCostingLC400B", price2, DOLLAR_ID);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product2, price2, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrder, price2,
          quantity1, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(product1, price1, quantity2,
          day2);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity3.multiply(price3));
      amountList.add(quantity3.multiply(price4));
      amountList.add(quantity3.multiply(price5));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt1);
      receiptList.add(goodsReceipt2);
      LandedCost landedCost = TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList,
          receiptList, null, day3);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_FEES_ID, price6, quantity3, day4);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(0), true);

      // Post landed cost cost and assert it
      TestCostingUtils.postLandedCostLine(landedCost.getLandedCostCostList().get(0),
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0));

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID, price7, quantity3, day4);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost2.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(1), true);

      // Post landed cost cost and assert it
      TestCostingUtils.postLandedCostLine(landedCost.getLandedCostCostList().get(1),
          purchaseInvoiceLandedCost2.getInvoiceLineList().get(0));

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost3 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID, price8, quantity3, day5);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost3.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(2), true);

      // Post landed cost cost and assert it
      TestCostingUtils.postLandedCostLine(landedCost.getLandedCostCostList().get(2),
          purchaseInvoiceLandedCost3.getInvoiceLineList().get(0));

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price9, price1));
      TestCostingUtils.assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price10, price11, price10));
      TestCostingUtils.assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = TestCostingUtils
          .getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(
          new ProductCostingAssert(transactionList1.get(0), price9, price1, price9, quantity2));
      TestCostingUtils.assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = TestCostingUtils
          .getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(
          new ProductCostingAssert(transactionList2.get(0), price11, price10, price11, quantity1));
      TestCostingUtils.assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = TestCostingUtils
          .getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0),
          DOLLAR_ID, "LC", amount1, day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), EURO_ID,
          "LC", amount2, day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0),
          DOLLAR_ID, "LC", amount3, day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), EURO_ID,
          "LC", amount4, day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList12 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(0), EURO_ID,
          "LC", amount5, day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(0), EURO_ID,
          "LC", amount6, day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList12);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList13 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList2.get(0), EURO_ID,
          "LC", amount7, day3, true, false));
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList1.get(0), EURO_ID,
          "LC", amount8, day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList13);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList14 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList14.add(new CostAdjustmentAssert(transactionList2.get(0),
          DOLLAR_ID, "LC", amount9, day3, true, false));
      costAdjustmentAssertLineList14.add(new CostAdjustmentAssert(transactionList1.get(0),
          DOLLAR_ID, "LC", amount10, day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList14);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = TestCostingUtils
          .getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0),
          DOLLAR_ID, "LC", amount1, day3, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0), EURO_ID,
          "LC", amount2, day3, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(0),
          DOLLAR_ID, "LC", amount3, day3, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(0), EURO_ID,
          "LC", amount4, day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList22 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(0), EURO_ID,
          "LC", amount5, day3, true, false));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList1.get(0), EURO_ID,
          "LC", amount6, day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList22);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList23 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList23.add(new CostAdjustmentAssert(transactionList2.get(0), EURO_ID,
          "LC", amount7, day3, true, false));
      costAdjustmentAssertLineList23.add(new CostAdjustmentAssert(transactionList1.get(0), EURO_ID,
          "LC", amount8, day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList23);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList24 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList24.add(new CostAdjustmentAssert(transactionList2.get(0),
          DOLLAR_ID, "LC", amount9, day3, true, false));
      costAdjustmentAssertLineList24.add(new CostAdjustmentAssert(transactionList1.get(0),
          DOLLAR_ID, "LC", amount10, day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList24);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

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
  public void testCostingLC300() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("300.00");
    final BigDecimal price2 = new BigDecimal("27.50");
    final BigDecimal price3 = new BigDecimal("105.00");
    final BigDecimal price4 = new BigDecimal("326.50");
    final BigDecimal quantity1 = new BigDecimal("5");
    final BigDecimal quantity2 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingLC300", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(product, price1, quantity1,
          day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity2.multiply(price2));
      amountList.add(quantity2.multiply(price3));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList,
          receiptList, null, day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_FEES_ID, price2, quantity2, day2);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(0), true);

      // Post landed cost cost and assert it
      TestCostingUtils.postLandedCostLine(landedCost.getLandedCostCostList().get(0),
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0));

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID, price3, quantity2, day2);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost2.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(1), true);

      // Post landed cost cost and assert it
      TestCostingUtils.postLandedCostLine(landedCost.getLandedCostCostList().get(1),
          purchaseInvoiceLandedCost2.getInvoiceLineList().get(0));

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1
          .add(new ProductCostingAssert(transactionList.get(0), price4, price1, price4, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity1.multiply(price4).add(quantity1.multiply(price1).negate()), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
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
  public void testCostingLC500() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("500.00");
    final BigDecimal price2 = new BigDecimal("210.00");
    final BigDecimal price3 = new BigDecimal("300.00");
    final BigDecimal price4 = new BigDecimal("520.40");
    final BigDecimal quantity1 = new BigDecimal("25");
    final BigDecimal quantity2 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingLC500", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(product, price1, quantity1,
          day0);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID, price2, quantity2, day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_FEES_ID, price3, quantity2, day1);

      // Create Landed Cost
      List<Invoice> invoiceList = new ArrayList<Invoice>();
      invoiceList.add(purchaseInvoiceLandedCost1);
      invoiceList.add(purchaseInvoiceLandedCost2);
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      TestCostingUtils.createLandedCost(invoiceList, receiptList, day2);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1
          .add(new ProductCostingAssert(transactionList.get(0), price4, price1, price4, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity1.multiply(price4).add(quantity1.multiply(price1).negate()), day2, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
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
  public void testCostingLC600() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("600.00");
    final BigDecimal price2 = new BigDecimal("315.00");
    final BigDecimal price3 = new BigDecimal("1110.00");
    final BigDecimal price4 = new BigDecimal("350.00");
    final BigDecimal price5 = new BigDecimal("1500.00");
    final BigDecimal price6 = new BigDecimal("643.1818");
    final BigDecimal quantity1 = new BigDecimal("33");
    final BigDecimal quantity2 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingLC600", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(product, price1, quantity1,
          day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity2.multiply(price2));
      amountList.add(quantity2.multiply(price3));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList,
          receiptList, null, day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID, price4, quantity2, day2);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(0), false);

      // Post landed cost cost and assert it
      TestCostingUtils.postLandedCostLine(landedCost.getLandedCostCostList().get(0),
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0));

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_FEES_ID, price5, quantity2, day2);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost2.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(1), false);

      // Post landed cost cost and assert it
      TestCostingUtils.postLandedCostLine(landedCost.getLandedCostCostList().get(1),
          purchaseInvoiceLandedCost2.getInvoiceLineList().get(0));

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1
          .add(new ProductCostingAssert(transactionList.get(0), price6, price1, price6, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
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
  public void testCostingLC701() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("700.00");
    final BigDecimal price2 = new BigDecimal("315.00");
    final BigDecimal price3 = new BigDecimal("1110.00");
    final BigDecimal price4 = new BigDecimal("250.00");
    final BigDecimal price5 = new BigDecimal("1000.00");
    final BigDecimal price6 = new BigDecimal("757.00");
    final BigDecimal quantity1 = new BigDecimal("25");
    final BigDecimal quantity2 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingLC701", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(product, price1, quantity1,
          day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity2.multiply(price2));
      amountList.add(quantity2.multiply(price3));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList,
          receiptList, null, day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID, price4, quantity2, day2);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(0), false);

      // Post landed cost cost and assert it
      TestCostingUtils.postLandedCostLine(landedCost.getLandedCostCostList().get(0),
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0));

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_FEES_ID, price5, quantity2, day2);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost2.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(1), false);

      // Post landed cost cost and assert it
      TestCostingUtils.postLandedCostLine(landedCost.getLandedCostCostList().get(1),
          purchaseInvoiceLandedCost2.getInvoiceLineList().get(0));

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1
          .add(new ProductCostingAssert(transactionList.get(0), price6, price1, price6, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
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
  public void testCostingLC801() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("800.00");
    final BigDecimal price2 = new BigDecimal("315.00");
    final BigDecimal price3 = new BigDecimal("1110.00");
    final BigDecimal price4 = new BigDecimal("250.00");
    final BigDecimal price5 = new BigDecimal("1000.00");
    final BigDecimal price6 = new BigDecimal("850.00");
    final BigDecimal quantity1 = new BigDecimal("25");
    final BigDecimal quantity2 = BigDecimal.ONE;
    final BigDecimal amount1 = new BigDecimal("1425.00");
    final BigDecimal amount2 = new BigDecimal("-65.00");
    final BigDecimal amount3 = new BigDecimal("-110.00");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingLC801", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(product, price1, quantity1,
          day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity2.multiply(price2));
      amountList.add(quantity2.multiply(price3));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList,
          receiptList, null, day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID, price4, quantity2, day2);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(0), true);

      // Post landed cost cost and assert it
      TestCostingUtils.postLandedCostLine(landedCost.getLandedCostCostList().get(0),
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0));

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_FEES_ID, price5, quantity2, day2);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost2.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(1), true);

      // Post landed cost cost and assert it
      TestCostingUtils.postLandedCostLine(landedCost.getLandedCostCostList().get(1),
          purchaseInvoiceLandedCost2.getInvoiceLineList().get(0));

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1
          .add(new ProductCostingAssert(transactionList.get(0), price6, price1, price6, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1
          .add(new CostAdjustmentAssert(transactionList.get(0), "LC", amount1, day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2
          .add(new CostAdjustmentAssert(transactionList.get(0), "LC", amount2, day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList3 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList3
          .add(new CostAdjustmentAssert(transactionList.get(0), "LC", amount3, day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList3);
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
  public void testCostingLC9() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final int day6 = 30;
    final BigDecimal price1 = new BigDecimal("120.00");
    final BigDecimal price2 = new BigDecimal("210.00");
    final BigDecimal price3 = new BigDecimal("120.21");
    final BigDecimal price4 = new BigDecimal("150.00");
    final BigDecimal price5 = new BigDecimal("120.30");
    final BigDecimal price6 = new BigDecimal("120.3250");
    final BigDecimal price7 = new BigDecimal("120.2938");
    final BigDecimal quantity1 = new BigDecimal("1000");
    final BigDecimal quantity2 = BigDecimal.ONE;
    final BigDecimal quantity3 = new BigDecimal("200");
    final BigDecimal quantity4 = new BigDecimal("2");
    final BigDecimal amount1 = new BigDecimal("5.00");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingLC9", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity1, day1);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = TestCostingUtils.createGoodsShipment(product, price1,
          quantity3, day2);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity2.multiply(price2));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList,
          receiptList, null, day3);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = TestCostingUtils.createGoodsShipment(product, price3,
          quantity3, day4);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID, price4, quantity4, day5);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(purchaseInvoiceLandedCost.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(0), true);

      // Post landed cost cost and assert it
      TestCostingUtils.postLandedCostLine(landedCost.getLandedCostCostList().get(0),
          purchaseInvoiceLandedCost.getInvoiceLineList().get(0));

      // Update transaction total cost amount
      TestCostingUtils.manualCostAdjustment(
          TestCostingUtils.getProductTransactions(product.getId()).get(1), amount1, true, true,
          day6);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price5, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price7));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1
          .add(new ProductCostingAssert(transactionList.get(0), price5, price1, price5, quantity1));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(1), price6, null,
          price7, quantity1.add(quantity3.negate())));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity2.multiply(price2), day3, true, false));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day3, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity4.multiply(price4).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity3.multiply(price5).add(quantity3.multiply(price3).negate()), day3, false));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity3.multiply(price5).add(quantity3.multiply(price3).negate()), day4, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList3 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList3
          .add(new CostAdjustmentAssert(transactionList.get(1), "MCC", amount1, day6, true));
      costAdjustmentAssertLineList3.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity3.multiply(price7).add(quantity3.multiply(price5).negate()), day6, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList3);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99900",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), null));
      CostAdjustment costAdjustment1 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(0).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment1, product.getId(),
          documentPostAssertList1);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99900",
          quantity3.multiply(price5).add(quantity3.multiply(price3).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity3.multiply(price5).add(quantity3.multiply(price3).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("99900",
          quantity3.multiply(price5).add(quantity3.multiply(price3).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity3.multiply(price5).add(quantity3.multiply(price3).negate()), null));
      CostAdjustment costAdjustment2 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(1).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment2, product.getId(),
          documentPostAssertList2);

      // Post cost adjustment and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(2));
      List<DocumentPostAssert> documentPostAssertList3 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList3.add(new DocumentPostAssert("99900", amount1, BigDecimal.ZERO, null));
      documentPostAssertList3.add(new DocumentPostAssert("35000", BigDecimal.ZERO, amount1, null));
      documentPostAssertList3.add(new DocumentPostAssert("99900", BigDecimal.ZERO,
          quantity3.multiply(price5).add(quantity3.multiply(price7).negate()), null));
      documentPostAssertList3.add(new DocumentPostAssert("35000",
          quantity3.multiply(price5).add(quantity3.multiply(price7).negate()), BigDecimal.ZERO,
          null));
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
  public void testCostingLC1000() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int year = -1;
    final BigDecimal price1 = new BigDecimal("1000.00");
    final BigDecimal price2 = new BigDecimal("650.00");
    final BigDecimal price3 = new BigDecimal("500.00");
    final BigDecimal price4 = new BigDecimal("1022.2222");
    final BigDecimal quantity1 = new BigDecimal("45");
    final BigDecimal quantity2 = new BigDecimal("7");
    final BigDecimal quantity3 = BigDecimal.ONE;
    final String productType = "S";
    final String costType = "STA";

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = TestCostingUtils.createProduct("testCostingLC1000A", price1);

      // Create a new product for the test
      Product product2 = TestCostingUtils.createProduct("testCostingLC1000B", productType, price2,
          price3, costType, year);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(product1, price1, quantity1,
          day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(product2, price3, quantity2,
          day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity3.multiply(price1));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt1);
      receiptList.add(goodsReceipt2);
      TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList, receiptList, null, day1);

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4, price1));
      TestCostingUtils.assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      assertTrue(TestCostingUtils.getProductTransactions(product2.getId()).isEmpty());

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = TestCostingUtils
          .getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(
          new ProductCostingAssert(transactionList1.get(0), price4, price1, price4, quantity1));
      TestCostingUtils.assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2
          .add(new ProductCostingAssert(null, null, null, price3, null, costType, year));
      TestCostingUtils.assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = TestCostingUtils
          .getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price4).add(quantity1.multiply(price1).negate()), day1, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      assertEquals(TestCostingUtils.getCostAdjustment(product2.getId()), null);

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
  public void testCostingLC802() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("800.00");
    final BigDecimal price2 = new BigDecimal("315.00");
    final BigDecimal price3 = new BigDecimal("1110.00");
    final BigDecimal price4 = new BigDecimal("250.00");
    final BigDecimal price5 = new BigDecimal("1000.00");
    final BigDecimal price6 = new BigDecimal("850.00");
    final BigDecimal quantity1 = new BigDecimal("25");
    final BigDecimal quantity2 = BigDecimal.ONE;
    final BigDecimal amount1 = new BigDecimal("1425.00");
    final BigDecimal amount2 = new BigDecimal("-65.00");
    final BigDecimal amount3 = new BigDecimal("-110.00");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingLC802", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(product, price1, quantity1,
          day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity2.multiply(price2));
      amountList.add(quantity2.multiply(price3));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList,
          receiptList, null, day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID, price4, quantity2, day2);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(0), true);

      // Post landed cost cost and assert it
      TestCostingUtils.postLandedCostLine(landedCost.getLandedCostCostList().get(0),
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0));

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_FEES_ID, price5, quantity2, day2);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost2.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(1), true);

      // Post landed cost cost and assert it
      TestCostingUtils.postLandedCostLine(landedCost.getLandedCostCostList().get(1),
          purchaseInvoiceLandedCost2.getInvoiceLineList().get(0));

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1
          .add(new ProductCostingAssert(transactionList.get(0), price6, price1, price6, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1
          .add(new CostAdjustmentAssert(transactionList.get(0), "LC", amount1, day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2
          .add(new CostAdjustmentAssert(transactionList.get(0), "LC", amount2, day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList3 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList3
          .add(new CostAdjustmentAssert(transactionList.get(0), "LC", amount3, day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList3);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Reactivate landed cost
      TestCostingUtils.reactivateLandedCost(landedCost.getId(), "This document is posted");

      // Unpost landed cost
      TestCostingUtils.unpostDocument(landedCost);

      // Reactivate landed cost
      TestCostingUtils.reactivateLandedCost(landedCost.getId(),
          "This document is posted: tab Cost - line 10");

      // Unpost landed cost cost
      TestCostingUtils.unpostDocument(landedCost.getLandedCostCostList().get(0));

      // Reactivate landed cost
      TestCostingUtils.reactivateLandedCost(landedCost.getId(),
          "This document is posted: tab Cost - line 20");

      // Unpost landed cost cost
      TestCostingUtils.unpostDocument(landedCost.getLandedCostCostList().get(1));

      // Reactivate landed cost
      TestCostingUtils.reactivateLandedCost(landedCost.getId(), null);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList2);

      // Assert product costing
      List<MaterialTransaction> transactionList2 = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(
          new ProductCostingAssert(transactionList2.get(0), price1, price1, price1, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList2);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList2 = TestCostingUtils
          .getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          amount1, day1, true, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList22 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          amount2, day1, true, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList22);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList23 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList23.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          amount3, day1, true, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList23);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList24 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList24.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          amount1.negate(), day1, true, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList24);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList25 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList25.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          amount2.negate(), day1, true, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList25);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList26 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList26.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          amount3.negate(), day1, true, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList26);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

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
  public void testCostingLC702() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("700.00");
    final BigDecimal price2 = new BigDecimal("315.00");
    final BigDecimal price3 = new BigDecimal("1110.00");
    final BigDecimal price4 = new BigDecimal("250.00");
    final BigDecimal price5 = new BigDecimal("1000.00");
    final BigDecimal price6 = new BigDecimal("757.00");
    final BigDecimal price7 = new BigDecimal("750.00");
    final BigDecimal quantity1 = new BigDecimal("25");
    final BigDecimal quantity2 = BigDecimal.ONE;
    final BigDecimal amount1 = new BigDecimal("-65.00");
    final BigDecimal amount2 = new BigDecimal("-110.00");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingLC702", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(product, price1, quantity1,
          day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity2.multiply(price2));
      amountList.add(quantity2.multiply(price3));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList,
          receiptList, null, day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID, price4, quantity2, day2);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(0), false);

      // Post landed cost cost and assert it
      TestCostingUtils.postLandedCostLine(landedCost.getLandedCostCostList().get(0),
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0));

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_FEES_ID, price5, quantity2, day2);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost2.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(1), false);

      // Post landed cost cost and assert it
      TestCostingUtils.postLandedCostLine(landedCost.getLandedCostCostList().get(1),
          purchaseInvoiceLandedCost2.getInvoiceLineList().get(0));

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1
          .add(new ProductCostingAssert(transactionList.get(0), price6, price1, price6, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Cancel landed cost cost
      TestCostingUtils.cancelLandedCostCost(landedCost.getLandedCostCostList().get(0).getId(),
          "This document is posted");

      // Unpost landed cost cost
      TestCostingUtils.unpostDocument(landedCost.getLandedCostCostList().get(0));

      // Cancel landed cost cost
      TestCostingUtils.cancelLandedCostCost(landedCost.getLandedCostCostList().get(0).getId(),
          null);

      // Cancel landed cost cost
      TestCostingUtils.cancelLandedCostCost(landedCost.getLandedCostCostList().get(1).getId(),
          "This document is posted");

      // Unpost landed cost cost
      TestCostingUtils.unpostDocument(landedCost.getLandedCostCostList().get(1));

      // Cancel landed cost cost
      TestCostingUtils.cancelLandedCostCost(landedCost.getLandedCostCostList().get(1).getId(),
          null);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(0),
          OBDal.getInstance()
              .get(LandedCostCost.class, landedCost.getLandedCostCostList().get(0).getId())
              .getLandedCostMatchedList()
              .get(0),
          true);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost2.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(1),
          OBDal.getInstance()
              .get(LandedCostCost.class, landedCost.getLandedCostCostList().get(1).getId())
              .getLandedCostMatchedList()
              .get(0),
          true);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price7, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList2);

      // Assert product costing
      List<MaterialTransaction> transactionList2 = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(
          new ProductCostingAssert(transactionList2.get(0), price7, price1, price7, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList2);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList2 = TestCostingUtils
          .getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day1, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList22 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList22
          .add(new CostAdjustmentAssert(transactionList2.get(0), "LC", amount1, day1, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList22);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList23 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList23
          .add(new CostAdjustmentAssert(transactionList2.get(0), "LC", amount2, day1, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList23);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

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
  public void testCostingLC5551() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("555.00");
    final BigDecimal price2 = new BigDecimal("100.00");
    final BigDecimal price3 = new BigDecimal("645.00");
    final BigDecimal price4 = new BigDecimal("125.00");
    final BigDecimal quantity1 = BigDecimal.ONE;
    final BigDecimal rate = new BigDecimal("0.90");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingLC5551", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(product, price1, quantity1,
          day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity1.multiply(price2));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList,
          receiptList, null, day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID, price2, quantity1, rate, day2);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(0), true);

      // Post landed cost cost and assert it
      TestCostingUtils.postLandedCostLine(landedCost.getLandedCostCostList().get(0),
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0));

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1
          .add(new ProductCostingAssert(transactionList.get(0), price3, price1, price3, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), DOLLAR_ID,
          "LC", quantity1.multiply(price2), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), DOLLAR_ID,
          "LC", quantity1.multiply(price4), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
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
  public void testCostingLC5552() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("555.00");
    final BigDecimal price2 = new BigDecimal("100.00");
    final BigDecimal price3 = new BigDecimal("645.00");
    final BigDecimal price4 = new BigDecimal("125.00");
    final BigDecimal quantity1 = BigDecimal.ONE;
    final BigDecimal rate = new BigDecimal("0.90");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingLC5552", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(product, price1, quantity1,
          day0);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID, price2, quantity1, rate, day1);

      // Create Landed Cost
      List<Invoice> invoiceList = new ArrayList<Invoice>();
      invoiceList.add(purchaseInvoiceLandedCost1);
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      TestCostingUtils.createLandedCost(invoiceList, receiptList, day2);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1
          .add(new ProductCostingAssert(transactionList.get(0), price3, price1, price3, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), DOLLAR_ID,
          "LC", quantity1.multiply(price4), day2, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), DOLLAR_ID,
          "LC", quantity1.multiply(price2), day2, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
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
  public void testCostingLC5553() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("555.00");
    final BigDecimal price2 = new BigDecimal("100.00");
    final BigDecimal price3 = new BigDecimal("585.00");
    final BigDecimal price4 = new BigDecimal("-25.00");
    final BigDecimal quantity1 = BigDecimal.ONE;
    final BigDecimal rate = new BigDecimal("0.30");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingLC5553", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(product, price1, quantity1,
          day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity1.multiply(price2));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList,
          receiptList, null, day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID, price2, quantity1, rate, day2);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(0), true);

      // Post landed cost cost and assert it
      TestCostingUtils.postLandedCostLine(landedCost.getLandedCostCostList().get(0),
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0));

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1
          .add(new ProductCostingAssert(transactionList.get(0), price3, price1, price3, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), DOLLAR_ID,
          "LC", quantity1.multiply(price2), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), DOLLAR_ID,
          "LC", quantity1.multiply(price4), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
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
  public void testCostingLC5554() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("555.00");
    final BigDecimal price2 = new BigDecimal("100.00");
    final BigDecimal price3 = new BigDecimal("585.00");
    final BigDecimal price4 = new BigDecimal("-25.00");
    final BigDecimal quantity1 = BigDecimal.ONE;
    final BigDecimal rate = new BigDecimal("0.30");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingLC5554", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(product, price1, quantity1,
          day0);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID, price2, quantity1, rate, day1);

      // Create Landed Cost
      List<Invoice> invoiceList = new ArrayList<Invoice>();
      invoiceList.add(purchaseInvoiceLandedCost1);
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      TestCostingUtils.createLandedCost(invoiceList, receiptList, day2);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1
          .add(new ProductCostingAssert(transactionList.get(0), price3, price1, price3, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), DOLLAR_ID,
          "LC", quantity1.multiply(price4), day2, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), DOLLAR_ID,
          "LC", quantity1.multiply(price2), day2, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
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
  public void testCostingLC1() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("135.00");
    final BigDecimal price2 = new BigDecimal("145.00");
    final BigDecimal price3 = new BigDecimal("80.00");
    final BigDecimal price4 = new BigDecimal("105.00");
    final BigDecimal price5 = new BigDecimal("145.37");
    final BigDecimal quantity1 = new BigDecimal("500");
    final BigDecimal quantity2 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingLC1", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity1, day1);

      // Create purchase invoice, post it and assert it
      TestCostingUtils.createPurchaseInvoice(purchaseOrder, price2, quantity1, day2);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_FEES_ID, price3, quantity2, day2);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID, price4, quantity2, day2);

      // Create Landed Cost
      List<Invoice> invoiceList = new ArrayList<Invoice>();
      invoiceList.add(purchaseInvoiceLandedCost1);
      invoiceList.add(purchaseInvoiceLandedCost2);
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      TestCostingUtils.createLandedCost(invoiceList, receiptList, day2);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price5, price2));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price5, price1, price5, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day2, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity2.multiply(price3).add(quantity2.multiply(price4)), day2, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), BigDecimal.ZERO,
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
  public void testCostingLC2() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("120.00");
    final BigDecimal price2 = new BigDecimal("95.00");
    final BigDecimal price3 = new BigDecimal("105.00");
    final BigDecimal price4 = new BigDecimal("130.00");
    final BigDecimal price5 = new BigDecimal("130.75");
    final BigDecimal quantity1 = new BigDecimal("1500");
    final BigDecimal quantity2 = new BigDecimal("320");
    final BigDecimal quantity3 = new BigDecimal("180");
    final BigDecimal quantity4 = new BigDecimal("300");
    final BigDecimal quantity5 = new BigDecimal("3");

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingLC2", price1);

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

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt3 = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1,
          quantity4, day3);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_FEES_ID, price2, quantity5, day0);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID, price3, quantity5, day0);

      // Create Landed Cost
      List<Invoice> invoiceList = new ArrayList<Invoice>();
      invoiceList.add(purchaseInvoiceLandedCost1);
      invoiceList.add(purchaseInvoiceLandedCost2);
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt1);
      receiptList.add(goodsReceipt2);
      receiptList.add(goodsReceipt3);
      LandedCost landedCost = TestCostingUtils.createLandedCost(invoiceList, receiptList, day0);

      // Create purchase invoice, post it and assert it
      List<BigDecimal> priceList = new ArrayList<BigDecimal>();
      priceList.add(price4);
      priceList.add(price4);
      priceList.add(price4);
      TestCostingUtils.createPurchaseInvoice(receiptList, priceList,
          quantity2.add(quantity3).add(quantity4), day4);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price5, price4));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price5, price4));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price5, price4));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price5, price1, price5, quantity2));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price5, price1,
          price5, quantity2.add(quantity3)));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), price5, price1,
          price5, quantity2.add(quantity3).add(quantity4)));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity2.multiply(price5).add(quantity2.multiply(price4).negate()), day0, true, false));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "LC",
          quantity3.multiply(price5).add(quantity3.multiply(price4).negate()), day0, true, false));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(2), "LC",
          quantity4.multiply(price5).add(quantity4.multiply(price4).negate()), day0, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity2.multiply(price4).add(quantity2.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity3.multiply(price4).add(quantity3.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price1).negate()), day4, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      TestCostingUtils.postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity2.multiply(price4).add(quantity2.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity2.multiply(price4).add(quantity2.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity3.multiply(price4).add(quantity3.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity3.multiply(price4).add(quantity3.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
          quantity4.multiply(price4).add(quantity4.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000",
          quantity4.multiply(price4).add(quantity4.multiply(price1).negate()), BigDecimal.ZERO,
          null));
      CostAdjustment costAdjustment1 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList.get(1).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment1, product.getId(),
          documentPostAssertList1);

      // Reactivate landed cost
      TestCostingUtils.cancelLandedCost(landedCost);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList2);

      // Assert product costing
      List<MaterialTransaction> transactionList2 = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(
          new ProductCostingAssert(transactionList2.get(0), price4, price1, price4, quantity2));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(1), price4,
          price1, price4, quantity2.add(quantity3)));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(2), price4,
          price1, price4, quantity2.add(quantity3).add(quantity4)));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList2);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList2 = TestCostingUtils
          .getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price5).add(quantity2.multiply(price4).negate()), day0, true, false,
          "VO"));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price5).add(quantity3.multiply(price4).negate()), day0, true, false,
          "VO"));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(2), "LC",
          quantity4.multiply(price5).add(quantity4.multiply(price4).negate()), day0, true, false,
          "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList22 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(0), "PDC",
          quantity2.multiply(price4).add(quantity2.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(1), "PDC",
          quantity3.multiply(price4).add(quantity3.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(2), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price1).negate()), day4, true));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList22);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList23 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList23.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price4).add(quantity2.multiply(price5).negate()), day0, true, false,
          "VO"));
      costAdjustmentAssertLineList23.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price4).add(quantity3.multiply(price5).negate()), day0, true, false,
          "VO"));
      costAdjustmentAssertLineList23.add(new CostAdjustmentAssert(transactionList2.get(2), "LC",
          quantity4.multiply(price4).add(quantity4.multiply(price5).negate()), day0, true, false,
          "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList23);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

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
  public void testCostingLC3LC4() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final int day6 = 30;
    final BigDecimal price1 = new BigDecimal("100.00");
    final BigDecimal price2 = new BigDecimal("110.00");
    final BigDecimal price3 = new BigDecimal("1500.00");
    final BigDecimal price4 = new BigDecimal("210.00");
    final BigDecimal price5 = new BigDecimal("124.4582");
    final BigDecimal price6 = new BigDecimal("139.5180");
    final BigDecimal price7 = new BigDecimal("159.8375");
    final BigDecimal price8 = new BigDecimal("136.9043");
    final BigDecimal price9 = new BigDecimal("173.4708");
    final BigDecimal price10 = new BigDecimal("195.1500");
    final BigDecimal price11 = new BigDecimal("133.1465");
    final BigDecimal price12 = new BigDecimal("165.4719");
    final BigDecimal price13 = new BigDecimal("178.1911");
    final BigDecimal price14 = new BigDecimal("84.9400");
    final BigDecimal price15 = new BigDecimal("93.4338");
    final BigDecimal quantity1 = new BigDecimal("11");
    final BigDecimal quantity2 = new BigDecimal("7");
    final BigDecimal quantity3 = new BigDecimal("15");
    final BigDecimal quantity4 = new BigDecimal("25");
    final BigDecimal quantity5 = new BigDecimal("12");
    final BigDecimal quantity6 = new BigDecimal("24");
    final BigDecimal quantity7 = BigDecimal.ONE;
    final BigDecimal quantity8 = new BigDecimal("3");
    final BigDecimal amount1 = new BigDecimal("500");
    final BigDecimal unitPrice = new BigDecimal("124.4580");
    final BigDecimal unitPrice2 = new BigDecimal("133.1466");
    final BigDecimal unitPrice3 = new BigDecimal("156.9044");
    final BigDecimal unitPrice4 = new BigDecimal("165.4721");
    final BigDecimal costingAssertFinalCost = new BigDecimal("141.5753");
    final BigDecimal costAdjustmentAssertLineAmount = new BigDecimal("225.90");
    final BigDecimal costAdjustmentAssertLineAmount2 = new BigDecimal("284.98");
    final BigDecimal costAdjustmentAssertLineAmount3 = new BigDecimal("414.14");
    final BigDecimal costAdjustmentAssertLineAmount4 = new BigDecimal("708.19");
    final BigDecimal costAdjustmentAssertLineAmount5 = new BigDecimal("140.97");
    final BigDecimal costAdjustmentAssertLineAmount6 = new BigDecimal("112.78");
    final BigDecimal costAdjustmentAssertLineAmount7 = new BigDecimal("258.47");
    final BigDecimal costAdjustmentAssertLineAmount8 = new BigDecimal("248.14");
    final BigDecimal costAdjustmentAssertLineAmount9 = new BigDecimal("375.00");
    final BigDecimal costAdjustmentAssertLinePrice = new BigDecimal("90.6018");
    final BigDecimal costAdjustmentAssertLinePrice2 = new BigDecimal("99.6614");
    final BigDecimal costAdjustmentAssertLinePrice3 = new BigDecimal("88.3692");
    final BigDecimal costAdjustmentAssertLinePrice4 = new BigDecimal("96.8883");
    final String costType = "AVA";

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = TestCostingUtils.createProduct("testCostingLC3LC4A", price1, price1,
          costType);

      // Create a new product for the test
      Product product2 = TestCostingUtils.createProduct("testCostingLC3LC4B", price2, price2,
          costType);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt11 = TestCostingUtils.createGoodsReceipt(product1, price1,
          quantity1, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt21 = TestCostingUtils.createGoodsReceipt(product2, price2,
          quantity2, day1);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt12 = TestCostingUtils.createGoodsReceipt(product1, price1,
          quantity3, day2);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt22 = TestCostingUtils.createGoodsReceipt(product2, price2,
          quantity4, day2);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt13 = TestCostingUtils.createGoodsReceipt(product1, price1,
          quantity5, day3);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt23 = TestCostingUtils.createGoodsReceipt(product2, price2,
          quantity6, day3);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_FEES_ID, price3, quantity7, day0);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID, price4, quantity8, day4);

      // Create Landed Cost
      List<Invoice> invoiceList1 = new ArrayList<Invoice>();
      invoiceList1.add(purchaseInvoiceLandedCost1);
      List<ShipmentInOut> receiptList1 = new ArrayList<ShipmentInOut>();
      receiptList1.add(goodsReceipt11);
      receiptList1.add(goodsReceipt21);
      receiptList1.add(goodsReceipt12);
      receiptList1.add(goodsReceipt22);
      receiptList1.add(goodsReceipt13);
      receiptList1.add(goodsReceipt23);
      TestCostingUtils.createLandedCost(invoiceList1, receiptList1, day0);

      // Create Landed Cost
      List<Invoice> invoiceList2 = new ArrayList<Invoice>();
      invoiceList2.add(purchaseInvoiceLandedCost2);
      List<ShipmentInOut> receiptList2 = new ArrayList<ShipmentInOut>();
      receiptList2.add(goodsReceipt11);
      receiptList2.add(goodsReceipt21);
      receiptList2.add(goodsReceipt13);
      receiptList2.add(goodsReceipt23);
      TestCostingUtils.createLandedCost(invoiceList2, receiptList2, day5);

      // Update transaction total cost amount
      TestCostingUtils.manualCostAdjustment(
          TestCostingUtils.getProductTransactions(product2.getId()).get(1), amount1, true, true,
          day6);

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt11.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price5, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt12.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, unitPrice));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt13.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price7, unitPrice2));
      TestCostingUtils.assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt21.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price8, price2));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt22.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price9, unitPrice3));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt23.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price10, unitPrice4));
      TestCostingUtils.assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = TestCostingUtils
          .getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1
          .add(new ProductCostingAssert(null, null, null, price1, null, costType));
      productCostingAssertList1.add(
          new ProductCostingAssert(transactionList1.get(0), price5, price1, price5, quantity5));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(1), price6,
          price1, price11, quantity1.add(quantity3)));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(2), price7,
          price1, costingAssertFinalCost, quantity1.add(quantity3).add(quantity5)));
      TestCostingUtils.assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = TestCostingUtils
          .getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2
          .add(new ProductCostingAssert(null, null, null, price2, null, costType));
      productCostingAssertList2.add(
          new ProductCostingAssert(transactionList2.get(0), price8, price2, price8, quantity2));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(1), price9,
          price2, price12, quantity2.add(quantity4)));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(2), price10,
          price2, price13, quantity2.add(quantity4).add(quantity6)));
      TestCostingUtils.assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = TestCostingUtils
          .getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price1).add(quantity1.multiply(price14).negate()), day0, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price2).add(quantity2.multiply(price15).negate()), day0, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(1), "LC",
          quantity3.multiply(price1).add(quantity3.multiply(price14).negate()), day0, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity4.multiply(price2).add(quantity4.multiply(price15).negate()), day0, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(2), "LC",
          quantity5.multiply(price1).add(quantity5.multiply(price14).negate()), day0, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(2), "LC",
          quantity6.multiply(price2).add(quantity6.multiply(price15).negate()), day0, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(1), "LC",
          costAdjustmentAssertLineAmount, day2, false, true, true));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(2), "LC",
          costAdjustmentAssertLineAmount2, day3, false, true, true));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          costAdjustmentAssertLineAmount3, day2, false, true, true));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(2), "LC",
          costAdjustmentAssertLineAmount4, day3, false, true, true));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList12 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList12
          .add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
              quantity1.multiply(price1)
                  .add(quantity1.multiply(costAdjustmentAssertLinePrice).negate()),
              day5, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price2)
              .add(quantity2.multiply(costAdjustmentAssertLinePrice2).negate()),
          day5, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(2), "LC",
          quantity5.multiply(price1)
              .add(quantity5.multiply(costAdjustmentAssertLinePrice3).negate()),
          day5, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(2), "LC",
          quantity6.multiply(price2)
              .add(quantity6.multiply(costAdjustmentAssertLinePrice4).negate()),
          day5, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(1), "LC",
          costAdjustmentAssertLineAmount5, day5, false, true, true));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(2), "LC",
          costAdjustmentAssertLineAmount6, day5, false, true, true));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          costAdjustmentAssertLineAmount7, day5, false, true, true));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(2), "LC",
          costAdjustmentAssertLineAmount8, day5, false, true, true));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList12);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = TestCostingUtils
          .getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price1).add(quantity1.multiply(price14).negate()), day0, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price2).add(quantity2.multiply(price15).negate()), day0, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(1), "LC",
          quantity3.multiply(price1).add(quantity3.multiply(price14).negate()), day0, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity4.multiply(price2).add(quantity4.multiply(price15).negate()), day0, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(2), "LC",
          quantity5.multiply(price1).add(quantity5.multiply(price14).negate()), day0, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(2), "LC",
          quantity6.multiply(price2).add(quantity6.multiply(price15).negate()), day0, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(1), "LC",
          costAdjustmentAssertLineAmount, day2, false, true, true));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(2), "LC",
          costAdjustmentAssertLineAmount2, day3, false, true, true));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          costAdjustmentAssertLineAmount3, day2, false, true, true));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(2), "LC",
          costAdjustmentAssertLineAmount4, day3, false, true, true));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList22 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList22
          .add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
              quantity1.multiply(price1)
                  .add(quantity1.multiply(costAdjustmentAssertLinePrice).negate()),
              day5, true, false));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price2)
              .add(quantity2.multiply(costAdjustmentAssertLinePrice2).negate()),
          day5, true, false));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList1.get(2), "LC",
          quantity5.multiply(price1)
              .add(quantity5.multiply(costAdjustmentAssertLinePrice3).negate()),
          day5, true, false));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(2), "LC",
          quantity6.multiply(price2)
              .add(quantity6.multiply(costAdjustmentAssertLinePrice4).negate()),
          day5, true, false));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList1.get(1), "LC",
          costAdjustmentAssertLineAmount5, day5, false, true, true));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList1.get(2), "LC",
          costAdjustmentAssertLineAmount6, day5, false, true, true));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          costAdjustmentAssertLineAmount7, day5, false, true, true));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(2), "LC",
          costAdjustmentAssertLineAmount8, day5, false, true, true));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList22);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList23 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList23
          .add(new CostAdjustmentAssert(transactionList2.get(1), "MCC", amount1, day6, true));
      costAdjustmentAssertLineList23.add(new CostAdjustmentAssert(transactionList2.get(2), "MCC",
          costAdjustmentAssertLineAmount9, day6, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList23);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Post cost adjustment 3 and assert it
      TestCostingUtils.postDocument(costAdjustmentList2.get(2));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO, amount1, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", amount1, BigDecimal.ZERO, null));
      documentPostAssertList1.add(
          new DocumentPostAssert("99904", BigDecimal.ZERO, costAdjustmentAssertLineAmount9, null));
      documentPostAssertList1.add(
          new DocumentPostAssert("35000", costAdjustmentAssertLineAmount9, BigDecimal.ZERO, null));
      CostAdjustment costAdjustment1 = OBDal.getInstance()
          .get(CostAdjustment.class, costAdjustmentList2.get(2).getId());
      TestCostingUtils.assertDocumentPost(costAdjustment1, product2.getId(),
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
  public void testCostingLC900() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("900.00");
    final BigDecimal price2 = new BigDecimal("315.00");
    final BigDecimal price3 = new BigDecimal("1110.00");
    final BigDecimal price4 = new BigDecimal("250.00");
    final BigDecimal price5 = new BigDecimal("1000.00");
    final BigDecimal price6 = new BigDecimal("940.7143");
    final BigDecimal quantity1 = new BigDecimal("35");
    final BigDecimal quantity2 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingLC900", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(product, price1, quantity1,
          day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity2.multiply(price2));
      amountList.add(quantity2.multiply(price3));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList,
          receiptList, null, day1);

      // Create purchase invoice with landed cost, post it and assert it
      TestCostingUtils.createPurchaseInvoiceLandedCost(
          TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID, price4, quantity2, day2);

      // Create purchase invoice with landed cost, post it and assert it
      TestCostingUtils.createPurchaseInvoiceLandedCost(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID,
          price5, quantity2, day2);

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(landedCost.getLandedCostCostList().get(0), true,
          "The Landed Cost Cost does not have any matching available.");

      // Match invoice landed cost
      TestCostingUtils.matchInvoiceLandedCost(landedCost.getLandedCostCostList().get(1), true,
          "The Landed Cost Cost does not have any matching available.");

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1
          .add(new ProductCostingAssert(transactionList.get(0), price6, price1, price6, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
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
  public void testCostingLC1111() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("111.00");
    final BigDecimal price2 = new BigDecimal("222.00");
    final BigDecimal price3 = new BigDecimal("333.00");
    final BigDecimal price4 = new BigDecimal("325.00");
    final BigDecimal price5 = new BigDecimal("425.00");
    final BigDecimal price6 = new BigDecimal("113.1428");
    final BigDecimal price7 = new BigDecimal("111.9740");
    final BigDecimal price8 = new BigDecimal("226.2857");
    final BigDecimal price9 = new BigDecimal("224.8572");
    final BigDecimal quantity1 = new BigDecimal("50");
    final BigDecimal quantity2 = new BigDecimal("150");
    final BigDecimal quantity3 = new BigDecimal("75");
    final BigDecimal quantity4 = new BigDecimal("125");
    final BigDecimal quantity5 = new BigDecimal("80");
    final BigDecimal quantity6 = new BigDecimal("60");
    final BigDecimal quantity7 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = TestCostingUtils.createProduct("testCostingLC1111A", price1);

      // Create a new product for the test
      Product product2 = TestCostingUtils.createProduct("testCostingLC1111B", price2);

      // Create a new product for the test
      Product product3 = TestCostingUtils.createProduct("testCostingLC1111C", price3);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product1, price1, quantity1,
          day0);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product2, price2, quantity2,
          day0);

      // Create purchase order and book it
      Order purchaseOrder3 = TestCostingUtils.createPurchaseOrder(product2, price2, quantity3,
          day0);

      // Create purchase order and book it
      Order purchaseOrder4 = TestCostingUtils.createPurchaseOrder(product3, price3, quantity4,
          day0);

      // Create purchase order and book it
      Order purchaseOrder5 = TestCostingUtils.createPurchaseOrder(product3, price3, quantity5,
          day0);

      // Create purchase order and book it
      Order purchaseOrder6 = TestCostingUtils.createPurchaseOrder(product1, price1, quantity6,
          day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList1 = new ArrayList<Order>();
      purchaseOrderList1.add(purchaseOrder1);
      purchaseOrderList1.add(purchaseOrder2);
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrderList1, null,
          quantity1.add(quantity2), day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList2 = new ArrayList<Order>();
      purchaseOrderList2.add(purchaseOrder3);
      purchaseOrderList2.add(purchaseOrder4);
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrderList2, null,
          quantity3.add(quantity4), day1);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList3 = new ArrayList<Order>();
      purchaseOrderList3.add(purchaseOrder5);
      purchaseOrderList3.add(purchaseOrder6);
      ShipmentInOut goodsReceipt3 = TestCostingUtils.createGoodsReceipt(purchaseOrderList3, null,
          quantity5.add(quantity6), day2);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity7.multiply(price4));
      amountList.add(quantity7.multiply(price5));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt1);
      TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList, receiptList, null, day3);

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price1, price1));
      TestCostingUtils.assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price2, price8, price2));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price2));
      TestCostingUtils.assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product transactions 3
      List<ProductTransactionAssert> productTransactionAssertList3 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price3, price3));
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      TestCostingUtils.assertProductTransaction(product3.getId(), productTransactionAssertList3);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = TestCostingUtils
          .getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(
          new ProductCostingAssert(transactionList1.get(0), price6, price1, price6, quantity1));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(1), price1,
          price1, price7, quantity1.add(quantity6)));
      TestCostingUtils.assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = TestCostingUtils
          .getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(
          new ProductCostingAssert(transactionList2.get(0), price8, price2, price8, quantity2));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(1), price2,
          price2, price9, quantity2.add(quantity3)));
      TestCostingUtils.assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert product costing 3
      List<MaterialTransaction> transactionList3 = TestCostingUtils
          .getProductTransactions(product3.getId());
      List<ProductCostingAssert> productCostingAssertList3 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList3
          .add(new ProductCostingAssert(transactionList3.get(0), price3, null, price3, quantity4));
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(1), price3, null,
          price3, quantity4.add(quantity5)));
      TestCostingUtils.assertProductCosting(product3.getId(), productCostingAssertList3);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = TestCostingUtils
          .getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = TestCostingUtils
          .getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList12 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList12);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Assert cost adjustment 3
      assertEquals(TestCostingUtils.getCostAdjustment(product3.getId()), null);

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
  public void testCostingLC1112() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("111.00");
    final BigDecimal price2 = new BigDecimal("222.00");
    final BigDecimal price3 = new BigDecimal("333.00");
    final BigDecimal price4 = new BigDecimal("325.00");
    final BigDecimal price5 = new BigDecimal("425.00");
    final BigDecimal price6 = new BigDecimal("113.1428");
    final BigDecimal price7 = new BigDecimal("111.9740");
    final BigDecimal price8 = new BigDecimal("226.2857");
    final BigDecimal price9 = new BigDecimal("224.8572");
    final BigDecimal quantity1 = new BigDecimal("50");
    final BigDecimal quantity2 = new BigDecimal("150");
    final BigDecimal quantity3 = new BigDecimal("75");
    final BigDecimal quantity4 = new BigDecimal("125");
    final BigDecimal quantity5 = new BigDecimal("80");
    final BigDecimal quantity6 = new BigDecimal("60");
    final BigDecimal quantity7 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = TestCostingUtils.createProduct("testCostingLC1112A", price1);

      // Create a new product for the test
      Product product2 = TestCostingUtils.createProduct("testCostingLC1112B", price2);

      // Create a new product for the test
      Product product3 = TestCostingUtils.createProduct("testCostingLC1112C", price3);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product1, price1, quantity1,
          day0);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product2, price2, quantity2,
          day0);

      // Create purchase order and book it
      Order purchaseOrder3 = TestCostingUtils.createPurchaseOrder(product2, price2, quantity3,
          day0);

      // Create purchase order and book it
      Order purchaseOrder4 = TestCostingUtils.createPurchaseOrder(product3, price3, quantity4,
          day0);

      // Create purchase order and book it
      Order purchaseOrder5 = TestCostingUtils.createPurchaseOrder(product3, price3, quantity5,
          day0);

      // Create purchase order and book it
      Order purchaseOrder6 = TestCostingUtils.createPurchaseOrder(product1, price1, quantity6,
          day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList1 = new ArrayList<Order>();
      purchaseOrderList1.add(purchaseOrder1);
      purchaseOrderList1.add(purchaseOrder2);
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrderList1, null,
          quantity1.add(quantity2), day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList2 = new ArrayList<Order>();
      purchaseOrderList2.add(purchaseOrder3);
      purchaseOrderList2.add(purchaseOrder4);
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrderList2, null,
          quantity3.add(quantity4), day1);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList3 = new ArrayList<Order>();
      purchaseOrderList3.add(purchaseOrder5);
      purchaseOrderList3.add(purchaseOrder6);
      ShipmentInOut goodsReceipt3 = TestCostingUtils.createGoodsReceipt(purchaseOrderList3, null,
          quantity5.add(quantity6), day2);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity7.multiply(price4));
      amountList.add(quantity7.multiply(price5));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt1);
      receiptList.add(goodsReceipt1);
      List<ShipmentInOutLine> receiptLineList = new ArrayList<ShipmentInOutLine>();
      receiptLineList.add(goodsReceipt1.getMaterialMgmtShipmentInOutLineList().get(0));
      receiptLineList.add(goodsReceipt1.getMaterialMgmtShipmentInOutLineList().get(1));
      TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList, receiptList,
          receiptLineList, day3);

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price1, price1));
      TestCostingUtils.assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price2, price8, price2));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price2));
      TestCostingUtils.assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product transactions 3
      List<ProductTransactionAssert> productTransactionAssertList3 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price3, price3));
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      TestCostingUtils.assertProductTransaction(product3.getId(), productTransactionAssertList3);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = TestCostingUtils
          .getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(
          new ProductCostingAssert(transactionList1.get(0), price6, price1, price6, quantity1));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(1), price1,
          price1, price7, quantity1.add(quantity6)));
      TestCostingUtils.assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = TestCostingUtils
          .getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(
          new ProductCostingAssert(transactionList2.get(0), price8, price2, price8, quantity2));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(1), price2,
          price2, price9, quantity2.add(quantity3)));
      TestCostingUtils.assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert product costing 3
      List<MaterialTransaction> transactionList3 = TestCostingUtils
          .getProductTransactions(product3.getId());
      List<ProductCostingAssert> productCostingAssertList3 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList3
          .add(new ProductCostingAssert(transactionList3.get(0), price3, null, price3, quantity4));
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(1), price3, null,
          price3, quantity4.add(quantity5)));
      TestCostingUtils.assertProductCosting(product3.getId(), productCostingAssertList3);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = TestCostingUtils
          .getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = TestCostingUtils
          .getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList12 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList12);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Assert cost adjustment 3
      assertEquals(TestCostingUtils.getCostAdjustment(product3.getId()), null);

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
  public void testCostingLC1113() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("111.00");
    final BigDecimal price2 = new BigDecimal("222.00");
    final BigDecimal price3 = new BigDecimal("333.00");
    final BigDecimal price4 = new BigDecimal("325.00");
    final BigDecimal price5 = new BigDecimal("425.00");
    final BigDecimal price6 = new BigDecimal("112.50");
    final BigDecimal price7 = new BigDecimal("111.6818");
    final BigDecimal price8 = new BigDecimal("225.00");
    final BigDecimal quantity1 = new BigDecimal("50");
    final BigDecimal quantity2 = new BigDecimal("150");
    final BigDecimal quantity3 = new BigDecimal("75");
    final BigDecimal quantity4 = new BigDecimal("125");
    final BigDecimal quantity5 = new BigDecimal("80");
    final BigDecimal quantity6 = new BigDecimal("60");
    final BigDecimal quantity7 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = TestCostingUtils.createProduct("testCostingLC1113A", price1);

      // Create a new product for the test
      Product product2 = TestCostingUtils.createProduct("testCostingLC1113B", price2);

      // Create a new product for the test
      Product product3 = TestCostingUtils.createProduct("testCostingLC1113C", price3);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product1, price1, quantity1,
          day0);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product2, price2, quantity2,
          day0);

      // Create purchase order and book it
      Order purchaseOrder3 = TestCostingUtils.createPurchaseOrder(product2, price2, quantity3,
          day0);

      // Create purchase order and book it
      Order purchaseOrder4 = TestCostingUtils.createPurchaseOrder(product3, price3, quantity4,
          day0);

      // Create purchase order and book it
      Order purchaseOrder5 = TestCostingUtils.createPurchaseOrder(product3, price3, quantity5,
          day0);

      // Create purchase order and book it
      Order purchaseOrder6 = TestCostingUtils.createPurchaseOrder(product1, price1, quantity6,
          day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList1 = new ArrayList<Order>();
      purchaseOrderList1.add(purchaseOrder1);
      purchaseOrderList1.add(purchaseOrder2);
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrderList1, null,
          quantity1.add(quantity2), day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList2 = new ArrayList<Order>();
      purchaseOrderList2.add(purchaseOrder3);
      purchaseOrderList2.add(purchaseOrder4);
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrderList2, null,
          quantity3.add(quantity4), day1);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList3 = new ArrayList<Order>();
      purchaseOrderList3.add(purchaseOrder5);
      purchaseOrderList3.add(purchaseOrder6);
      ShipmentInOut goodsReceipt3 = TestCostingUtils.createGoodsReceipt(purchaseOrderList3, null,
          quantity5.add(quantity6), day2);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity7.multiply(price4));
      amountList.add(quantity7.multiply(price5));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt1);
      receiptList.add(goodsReceipt2);
      List<ShipmentInOutLine> receiptLineList = new ArrayList<ShipmentInOutLine>();
      receiptLineList.add(null);
      receiptLineList.add(goodsReceipt2.getMaterialMgmtShipmentInOutLineList().get(0));
      TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList, receiptList,
          receiptLineList, day3);

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price1, price1));
      TestCostingUtils.assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price2, price8, price2));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price8, price2));
      TestCostingUtils.assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product transactions 3
      List<ProductTransactionAssert> productTransactionAssertList3 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price3, price3));
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      TestCostingUtils.assertProductTransaction(product3.getId(), productTransactionAssertList3);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = TestCostingUtils
          .getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(
          new ProductCostingAssert(transactionList1.get(0), price6, price1, price6, quantity1));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(1), price1,
          price1, price7, quantity1.add(quantity6)));
      TestCostingUtils.assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = TestCostingUtils
          .getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(
          new ProductCostingAssert(transactionList2.get(0), price8, price2, price8, quantity2));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(1), price8,
          price2, price8, quantity2.add(quantity3)));
      TestCostingUtils.assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert product costing 3
      List<MaterialTransaction> transactionList3 = TestCostingUtils
          .getProductTransactions(product3.getId());
      List<ProductCostingAssert> productCostingAssertList3 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList3
          .add(new ProductCostingAssert(transactionList3.get(0), price3, null, price3, quantity4));
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(1), price3, null,
          price3, quantity4.add(quantity5)));
      TestCostingUtils.assertProductCosting(product3.getId(), productCostingAssertList3);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = TestCostingUtils
          .getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = TestCostingUtils
          .getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList12 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList12);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Assert cost adjustment 3
      assertEquals(TestCostingUtils.getCostAdjustment(product3.getId()), null);

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
  public void testCostingLC1114() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("111.00");
    final BigDecimal price2 = new BigDecimal("222.00");
    final BigDecimal price3 = new BigDecimal("333.00");
    final BigDecimal price4 = new BigDecimal("325.00");
    final BigDecimal price5 = new BigDecimal("425.00");
    final BigDecimal price6 = new BigDecimal("112.50");
    final BigDecimal price7 = new BigDecimal("111.6818");
    final BigDecimal price8 = new BigDecimal("225.00");
    final BigDecimal quantity1 = new BigDecimal("50");
    final BigDecimal quantity2 = new BigDecimal("150");
    final BigDecimal quantity3 = new BigDecimal("75");
    final BigDecimal quantity4 = new BigDecimal("125");
    final BigDecimal quantity5 = new BigDecimal("80");
    final BigDecimal quantity6 = new BigDecimal("60");
    final BigDecimal quantity7 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = TestCostingUtils.createProduct("testCostingLC1114A", price1);

      // Create a new product for the test
      Product product2 = TestCostingUtils.createProduct("testCostingLC1114B", price2);

      // Create a new product for the test
      Product product3 = TestCostingUtils.createProduct("testCostingLC1114C", price3);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product1, price1, quantity1,
          day0);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product2, price2, quantity2,
          day0);

      // Create purchase order and book it
      Order purchaseOrder3 = TestCostingUtils.createPurchaseOrder(product2, price2, quantity3,
          day0);

      // Create purchase order and book it
      Order purchaseOrder4 = TestCostingUtils.createPurchaseOrder(product3, price3, quantity4,
          day0);

      // Create purchase order and book it
      Order purchaseOrder5 = TestCostingUtils.createPurchaseOrder(product3, price3, quantity5,
          day0);

      // Create purchase order and book it
      Order purchaseOrder6 = TestCostingUtils.createPurchaseOrder(product1, price1, quantity6,
          day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList1 = new ArrayList<Order>();
      purchaseOrderList1.add(purchaseOrder1);
      purchaseOrderList1.add(purchaseOrder2);
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrderList1, null,
          quantity1.add(quantity2), day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList2 = new ArrayList<Order>();
      purchaseOrderList2.add(purchaseOrder3);
      purchaseOrderList2.add(purchaseOrder4);
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrderList2, null,
          quantity3.add(quantity4), day1);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList3 = new ArrayList<Order>();
      purchaseOrderList3.add(purchaseOrder5);
      purchaseOrderList3.add(purchaseOrder6);
      ShipmentInOut goodsReceipt3 = TestCostingUtils.createGoodsReceipt(purchaseOrderList3, null,
          quantity5.add(quantity6), day2);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity7.multiply(price4));
      amountList.add(quantity7.multiply(price5));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt1);
      receiptList.add(goodsReceipt1);
      receiptList.add(goodsReceipt2);
      List<ShipmentInOutLine> receiptLineList = new ArrayList<ShipmentInOutLine>();
      receiptLineList.add(goodsReceipt1.getMaterialMgmtShipmentInOutLineList().get(0));
      receiptLineList.add(goodsReceipt1.getMaterialMgmtShipmentInOutLineList().get(1));
      receiptLineList.add(goodsReceipt2.getMaterialMgmtShipmentInOutLineList().get(0));
      TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList, receiptList,
          receiptLineList, day3);

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price1, price1));
      TestCostingUtils.assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price2, price8, price2));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price8, price2));
      TestCostingUtils.assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product transactions 3
      List<ProductTransactionAssert> productTransactionAssertList3 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price3, price3));
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      TestCostingUtils.assertProductTransaction(product3.getId(), productTransactionAssertList3);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = TestCostingUtils
          .getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(
          new ProductCostingAssert(transactionList1.get(0), price6, price1, price6, quantity1));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(1), price1,
          price1, price7, quantity1.add(quantity6)));
      TestCostingUtils.assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = TestCostingUtils
          .getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(
          new ProductCostingAssert(transactionList2.get(0), price8, price2, price8, quantity2));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(1), price8,
          price2, price8, quantity2.add(quantity3)));
      TestCostingUtils.assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert product costing 3
      List<MaterialTransaction> transactionList3 = TestCostingUtils
          .getProductTransactions(product3.getId());
      List<ProductCostingAssert> productCostingAssertList3 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList3
          .add(new ProductCostingAssert(transactionList3.get(0), price3, null, price3, quantity4));
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(1), price3, null,
          price3, quantity4.add(quantity5)));
      TestCostingUtils.assertProductCosting(product3.getId(), productCostingAssertList3);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = TestCostingUtils
          .getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = TestCostingUtils
          .getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList12 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList12);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Assert cost adjustment 3
      assertEquals(TestCostingUtils.getCostAdjustment(product3.getId()), null);

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
  public void testCostingLC1115() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("111.00");
    final BigDecimal price2 = new BigDecimal("222.00");
    final BigDecimal price3 = new BigDecimal("333.00");
    final BigDecimal price4 = new BigDecimal("325.00");
    final BigDecimal price5 = new BigDecimal("425.00");
    final BigDecimal price6 = new BigDecimal("111.6726");
    final BigDecimal price7 = new BigDecimal("111.3057");
    final BigDecimal price8 = new BigDecimal("223.345267");
    final BigDecimal price9 = new BigDecimal("335.0179");
    final BigDecimal price10 = new BigDecimal("335.0180");
    final BigDecimal quantity1 = new BigDecimal("50");
    final BigDecimal quantity2 = new BigDecimal("150");
    final BigDecimal quantity3 = new BigDecimal("75");
    final BigDecimal quantity4 = new BigDecimal("125");
    final BigDecimal quantity5 = new BigDecimal("80");
    final BigDecimal quantity6 = new BigDecimal("60");
    final BigDecimal quantity7 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = TestCostingUtils.createProduct("testCostingLC1115A", price1);

      // Create a new product for the test
      Product product2 = TestCostingUtils.createProduct("testCostingLC1115B", price2);

      // Create a new product for the test
      Product product3 = TestCostingUtils.createProduct("testCostingLC1115C", price3);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product1, price1, quantity1,
          day0);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product2, price2, quantity2,
          day0);

      // Create purchase order and book it
      Order purchaseOrder3 = TestCostingUtils.createPurchaseOrder(product2, price2, quantity3,
          day0);

      // Create purchase order and book it
      Order purchaseOrder4 = TestCostingUtils.createPurchaseOrder(product3, price3, quantity4,
          day0);

      // Create purchase order and book it
      Order purchaseOrder5 = TestCostingUtils.createPurchaseOrder(product3, price3, quantity5,
          day0);

      // Create purchase order and book it
      Order purchaseOrder6 = TestCostingUtils.createPurchaseOrder(product1, price1, quantity6,
          day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList1 = new ArrayList<Order>();
      purchaseOrderList1.add(purchaseOrder1);
      purchaseOrderList1.add(purchaseOrder2);
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrderList1, null,
          quantity1.add(quantity2), day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList2 = new ArrayList<Order>();
      purchaseOrderList2.add(purchaseOrder3);
      purchaseOrderList2.add(purchaseOrder4);
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrderList2, null,
          quantity3.add(quantity4), day1);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList3 = new ArrayList<Order>();
      purchaseOrderList3.add(purchaseOrder5);
      purchaseOrderList3.add(purchaseOrder6);
      ShipmentInOut goodsReceipt3 = TestCostingUtils.createGoodsReceipt(purchaseOrderList3, null,
          quantity5.add(quantity6), day2);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity7.multiply(price4));
      amountList.add(quantity7.multiply(price5));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt1);
      receiptList.add(goodsReceipt2);
      receiptList.add(goodsReceipt2);
      receiptList.add(goodsReceipt3);
      List<ShipmentInOutLine> receiptLineList = new ArrayList<ShipmentInOutLine>();
      receiptLineList.add(null);
      receiptLineList.add(goodsReceipt2.getMaterialMgmtShipmentInOutLineList().get(0));
      receiptLineList.add(goodsReceipt2.getMaterialMgmtShipmentInOutLineList().get(1));
      receiptLineList.add(goodsReceipt3.getMaterialMgmtShipmentInOutLineList().get(0));
      TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList, receiptList,
          receiptLineList, day3);

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price1, price1));
      TestCostingUtils.assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price2, price8, price2));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price8, price2));
      TestCostingUtils.assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product transactions 3
      List<ProductTransactionAssert> productTransactionAssertList3 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price3, price9, price3));
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price10, price3));
      TestCostingUtils.assertProductTransaction(product3.getId(), productTransactionAssertList3);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = TestCostingUtils
          .getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(
          new ProductCostingAssert(transactionList1.get(0), price6, price1, price6, quantity1));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(1), price1,
          price1, price7, quantity1.add(quantity6)));
      TestCostingUtils.assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = TestCostingUtils
          .getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(
          new ProductCostingAssert(transactionList2.get(0), price8, price2, price8, quantity2));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(1), price8,
          price2, price8, quantity2.add(quantity3)));
      TestCostingUtils.assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert product costing 3
      List<MaterialTransaction> transactionList3 = TestCostingUtils
          .getProductTransactions(product3.getId());
      List<ProductCostingAssert> productCostingAssertList3 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList3.add(
          new ProductCostingAssert(transactionList3.get(0), price9, price3, price9, quantity4));
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(1), price10,
          price3, price10, quantity4.add(quantity5)));
      TestCostingUtils.assertProductCosting(product3.getId(), productCostingAssertList3);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = TestCostingUtils
          .getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList3.get(0), "LC",
          quantity4.multiply(price9).add(quantity4.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList3.get(1), "LC",
          quantity5.multiply(price10).add(quantity5.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = TestCostingUtils
          .getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList12 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList3.get(0), "LC",
          quantity4.multiply(price9).add(quantity4.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList3.get(1), "LC",
          quantity5.multiply(price10).add(quantity5.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList12);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Assert cost adjustment 3
      List<CostAdjustment> costAdjustmentList3 = TestCostingUtils
          .getCostAdjustment(product3.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList3 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList13 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList3.get(0), "LC",
          quantity4.multiply(price9).add(quantity4.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList3.get(1), "LC",
          quantity5.multiply(price10).add(quantity5.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertList3.add(costAdjustmentAssertLineList13);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList3, costAdjustmentAssertList3);

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
  public void testCostingLC1116() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("111.00");
    final BigDecimal price2 = new BigDecimal("222.00");
    final BigDecimal price3 = new BigDecimal("333.00");
    final BigDecimal price4 = new BigDecimal("325.00");
    final BigDecimal price5 = new BigDecimal("425.00");
    final BigDecimal price6 = new BigDecimal("111.6726");
    final BigDecimal price7 = new BigDecimal("111.3057");
    final BigDecimal price8 = new BigDecimal("223.345267");
    final BigDecimal price9 = new BigDecimal("335.0179");
    final BigDecimal price10 = new BigDecimal("335.0180");
    final BigDecimal quantity1 = new BigDecimal("50");
    final BigDecimal quantity2 = new BigDecimal("150");
    final BigDecimal quantity3 = new BigDecimal("75");
    final BigDecimal quantity4 = new BigDecimal("125");
    final BigDecimal quantity5 = new BigDecimal("80");
    final BigDecimal quantity6 = new BigDecimal("60");
    final BigDecimal quantity7 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = TestCostingUtils.createProduct("testCostingLC1116A", price1);

      // Create a new product for the test
      Product product2 = TestCostingUtils.createProduct("testCostingLC1116B", price2);

      // Create a new product for the test
      Product product3 = TestCostingUtils.createProduct("testCostingLC1116C", price3);

      // Create purchase order and book it
      Order purchaseOrder1 = TestCostingUtils.createPurchaseOrder(product1, price1, quantity1,
          day0);

      // Create purchase order and book it
      Order purchaseOrder2 = TestCostingUtils.createPurchaseOrder(product2, price2, quantity2,
          day0);

      // Create purchase order and book it
      Order purchaseOrder3 = TestCostingUtils.createPurchaseOrder(product2, price2, quantity3,
          day0);

      // Create purchase order and book it
      Order purchaseOrder4 = TestCostingUtils.createPurchaseOrder(product3, price3, quantity4,
          day0);

      // Create purchase order and book it
      Order purchaseOrder5 = TestCostingUtils.createPurchaseOrder(product3, price3, quantity5,
          day0);

      // Create purchase order and book it
      Order purchaseOrder6 = TestCostingUtils.createPurchaseOrder(product1, price1, quantity6,
          day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList1 = new ArrayList<Order>();
      purchaseOrderList1.add(purchaseOrder1);
      purchaseOrderList1.add(purchaseOrder2);
      ShipmentInOut goodsReceipt1 = TestCostingUtils.createGoodsReceipt(purchaseOrderList1, null,
          quantity1.add(quantity2), day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList2 = new ArrayList<Order>();
      purchaseOrderList2.add(purchaseOrder3);
      purchaseOrderList2.add(purchaseOrder4);
      ShipmentInOut goodsReceipt2 = TestCostingUtils.createGoodsReceipt(purchaseOrderList2, null,
          quantity3.add(quantity4), day1);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList3 = new ArrayList<Order>();
      purchaseOrderList3.add(purchaseOrder5);
      purchaseOrderList3.add(purchaseOrder6);
      ShipmentInOut goodsReceipt3 = TestCostingUtils.createGoodsReceipt(purchaseOrderList3, null,
          quantity5.add(quantity6), day2);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity7.multiply(price4));
      amountList.add(quantity7.multiply(price5));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt1);
      receiptList.add(goodsReceipt2);
      receiptList.add(goodsReceipt3);
      List<ShipmentInOutLine> receiptLineList = new ArrayList<ShipmentInOutLine>();
      receiptLineList.add(null);
      receiptLineList.add(null);
      receiptLineList.add(goodsReceipt3.getMaterialMgmtShipmentInOutLineList().get(0));
      TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList, receiptList,
          receiptLineList, day3);

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price1, price1));
      TestCostingUtils.assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price2, price8, price2));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price8, price2));
      TestCostingUtils.assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product transactions 3
      List<ProductTransactionAssert> productTransactionAssertList3 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price3, price9, price3));
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price10, price3));
      TestCostingUtils.assertProductTransaction(product3.getId(), productTransactionAssertList3);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = TestCostingUtils
          .getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(
          new ProductCostingAssert(transactionList1.get(0), price6, price1, price6, quantity1));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(1), price1,
          price1, price7, quantity1.add(quantity6)));
      TestCostingUtils.assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = TestCostingUtils
          .getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(
          new ProductCostingAssert(transactionList2.get(0), price8, price2, price8, quantity2));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(1), price8,
          price2, price8, quantity2.add(quantity3)));
      TestCostingUtils.assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert product costing 3
      List<MaterialTransaction> transactionList3 = TestCostingUtils
          .getProductTransactions(product3.getId());
      List<ProductCostingAssert> productCostingAssertList3 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList3.add(
          new ProductCostingAssert(transactionList3.get(0), price9, price3, price9, quantity4));
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(1), price10,
          price3, price10, quantity4.add(quantity5)));
      TestCostingUtils.assertProductCosting(product3.getId(), productCostingAssertList3);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = TestCostingUtils
          .getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList3.get(0), "LC",
          quantity4.multiply(price9).add(quantity4.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList3.get(1), "LC",
          quantity5.multiply(price10).add(quantity5.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = TestCostingUtils
          .getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList12 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList3.get(0), "LC",
          quantity4.multiply(price9).add(quantity4.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList3.get(1), "LC",
          quantity5.multiply(price10).add(quantity5.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList12);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Assert cost adjustment 3
      List<CostAdjustment> costAdjustmentList3 = TestCostingUtils
          .getCostAdjustment(product3.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList3 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList13 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList3.get(0), "LC",
          quantity4.multiply(price9).add(quantity4.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList3.get(1), "LC",
          quantity5.multiply(price10).add(quantity5.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertList3.add(costAdjustmentAssertLineList13);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList3, costAdjustmentAssertList3);

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
