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
 * All portions are Copyright (C) 2019 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.costing;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.cost.LandedCost;
import org.openbravo.model.materialmgmt.transaction.ProductionLine;
import org.openbravo.model.materialmgmt.transaction.ProductionTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.test.costing.assertclass.ProductTransactionAssert;
import org.openbravo.test.costing.utils.TestCostingConstants;
import org.openbravo.test.costing.utils.TestCostingUtils;

public class TestIssue39888 extends TestCostingBase {

  private static final String UNDEF_OVERISSUE_STATUS = "0";

  @Test
  public void testIssue39888() throws Exception {

    final int day0 = 0;
    final BigDecimal salesPrice = new BigDecimal("10.00");
    final BigDecimal purchasePrice = new BigDecimal("10.00");
    final BigDecimal cost = new BigDecimal("10.00");
    final BigDecimal bomQuantity = new BigDecimal("2.00");

    final BigDecimal shipReceiveQuantity = new BigDecimal("1000.00");
    final BigDecimal orginalPrice = new BigDecimal("10.00");
    final BigDecimal bomProductionQty = BigDecimal.ONE;
    final BigDecimal cost1 = new BigDecimal("20.00");
    final BigDecimal cost2 = new BigDecimal("120.00");
    final BigDecimal cost3 = new BigDecimal("180.00");

    final BigDecimal totalCost = new BigDecimal("-9980.00");
    final BigDecimal totalCostAfterLC1 = new BigDecimal("-109880.00");
    final BigDecimal totalCostAfterLC2 = new BigDecimal("-169820.00");

    final BigDecimal amount1 = new BigDecimal("50000.00");
    final BigDecimal amount2 = new BigDecimal("30000.00");

    ProductionLine productionLine1;
    try {

      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);

      OBContext.setAdminMode(true);

      Product rawProduct = TestCostingUtils.addProductPriceCost("Raw Material", "I", purchasePrice,
          null, cost, "AVA", 0, TestCostingConstants.EURO_ID);

      List<Product> productList = new ArrayList<Product>();
      List<BigDecimal> quantityList = new ArrayList<BigDecimal>();
      Product finalProduct = TestCostingUtils.addProductPriceCost("Final Product", "I", null,
          salesPrice, cost, "AVA", 0, TestCostingConstants.EURO_ID);
      productList.add(rawProduct);
      quantityList.add(bomQuantity);
      finalProduct = TestCostingUtils.addBOMProducts(finalProduct, productList, quantityList);

      Locator storageBin = OBDal.getInstance()
          .get(Locator.class, TestCostingConstants.LOCATOR_M01_ID);
      assertThat("Inventory Status of Storage Bin must be 'Undefined Over-Issue': ",
          storageBin.getInventoryStatus().getId(), equalTo(UNDEF_OVERISSUE_STATUS));
      // Goods Shipment
      ShipmentInOut goodsShipment = TestCostingUtils.createGoodsShipment(finalProduct, salesPrice,
          shipReceiveQuantity, TestCostingConstants.LOCATOR_M01_ID, day0);

      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), TestCostingConstants.EURO_ID, orginalPrice, orginalPrice));

      TestCostingUtils.assertProductTransaction(finalProduct.getId(), productTransactionAssertList);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Goods Receipt
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(rawProduct, purchasePrice,
          shipReceiveQuantity, TestCostingConstants.LOCATOR_M01_ID, day0);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      List<ProductTransactionAssert> productTransactionReceiptAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionReceiptAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), TestCostingConstants.EURO_ID, orginalPrice, orginalPrice));

      TestCostingUtils.assertProductTransaction(rawProduct.getId(),
          productTransactionReceiptAssertList);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Bills Of Material Production I
      ProductionTransaction billOfMaterialsProduction = TestCostingUtils
          .createBillOfMaterialsProduction(finalProduct, bomProductionQty,
              TestCostingConstants.LOCATOR_M01_ID, day0, false, true);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      productionLine1 = OBDal.getInstance()
          .get(ProductionTransaction.class, billOfMaterialsProduction.getId())
          .getMaterialMgmtProductionPlanList()
          .get(0)
          .getManufacturingProductionLineList()
          .get(0);

      productTransactionAssertList
          .add(new ProductTransactionAssert(productionLine1, cost1, totalCost, cost1));

      TestCostingUtils.assertProductTransaction(finalProduct.getId(), productTransactionAssertList,
          true);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Bills Of Material Production II
      ProductionTransaction billOfMaterialsProduction2 = TestCostingUtils
          .createBillOfMaterialsProduction(finalProduct, bomProductionQty,
              TestCostingConstants.LOCATOR_M01_ID, day0, false, true);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ProductionTransaction.class, billOfMaterialsProduction2.getId())
          .getMaterialMgmtProductionPlanList()
          .get(0)
          .getManufacturingProductionLineList()
          .get(0), cost1, cost1));

      TestCostingUtils.assertProductTransaction(finalProduct.getId(), productTransactionAssertList,
          true);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Bills Of Material Production III
      ProductionTransaction billOfMaterialsProduction3 = TestCostingUtils
          .createBillOfMaterialsProduction(finalProduct, bomProductionQty,
              TestCostingConstants.LOCATOR_M01_ID, day0, false, true);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ProductionTransaction.class, billOfMaterialsProduction3.getId())
          .getMaterialMgmtProductionPlanList()
          .get(0)
          .getManufacturingProductionLineList()
          .get(0), cost1, cost1));

      TestCostingUtils.assertProductTransaction(finalProduct.getId(), productTransactionAssertList,
          true);

      // Create Landed Cost I
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(amount1);
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      List<ShipmentInOutLine> receiptLineList = new ArrayList<ShipmentInOutLine>();
      receiptList.add(goodsReceipt);
      receiptLineList.add(goodsReceipt.getMaterialMgmtShipmentInOutLineList().get(0));

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      LandedCost landedCost1 = TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList,
          receiptList, receiptLineList, day0);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      productTransactionAssertList.remove(1);
      productTransactionAssertList.add(1,
          new ProductTransactionAssert(OBDal.getInstance()
              .get(ProductionTransaction.class, billOfMaterialsProduction.getId())
              .getMaterialMgmtProductionPlanList()
              .get(0)
              .getManufacturingProductionLineList()
              .get(0), cost1, totalCostAfterLC1, cost2));
      productTransactionAssertList.remove(2);
      productTransactionAssertList.add(2,
          new ProductTransactionAssert(OBDal.getInstance()
              .get(ProductionTransaction.class, billOfMaterialsProduction2.getId())
              .getMaterialMgmtProductionPlanList()
              .get(0)
              .getManufacturingProductionLineList()
              .get(0), cost1, cost2, cost2));
      productTransactionAssertList.remove(3);
      productTransactionAssertList.add(3,
          new ProductTransactionAssert(OBDal.getInstance()
              .get(ProductionTransaction.class, billOfMaterialsProduction3.getId())
              .getMaterialMgmtProductionPlanList()
              .get(0)
              .getManufacturingProductionLineList()
              .get(0), cost1, cost2, cost2));

      TestCostingUtils.assertProductTransaction(finalProduct.getId(), productTransactionAssertList,
          true);

      // Assert Transaction Cost after Landed Cost I
      assertTrue(TestCostingUtils.existsProductTransactionCostByCostIsUnitCostCurrency(
          TestCostingUtils.getProductTransactionsForProductionLine(productionLine1), true,
          TestCostingConstants.EURO_ID, new BigDecimal(100)));
      assertTrue(TestCostingUtils.existsProductTransactionCostByCostIsUnitCostCurrency(
          TestCostingUtils.getProductTransactionsForProductionLine(productionLine1), false,
          TestCostingConstants.EURO_ID, new BigDecimal(-100000)));

      // Create Landed Cost II

      amountList = new ArrayList<BigDecimal>();
      amountList.add(amount2);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      LandedCost landedCost2 = TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList,
          receiptList, receiptLineList, day0);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      productTransactionAssertList.remove(1);
      productTransactionAssertList.add(1,
          new ProductTransactionAssert(OBDal.getInstance()
              .get(ProductionTransaction.class, billOfMaterialsProduction.getId())
              .getMaterialMgmtProductionPlanList()
              .get(0)
              .getManufacturingProductionLineList()
              .get(0), cost1, totalCostAfterLC2, cost3));
      productTransactionAssertList.remove(2);
      productTransactionAssertList.add(2,
          new ProductTransactionAssert(OBDal.getInstance()
              .get(ProductionTransaction.class, billOfMaterialsProduction2.getId())
              .getMaterialMgmtProductionPlanList()
              .get(0)
              .getManufacturingProductionLineList()
              .get(0), cost1, cost3, cost3));
      productTransactionAssertList.remove(3);
      productTransactionAssertList.add(3,
          new ProductTransactionAssert(OBDal.getInstance()
              .get(ProductionTransaction.class, billOfMaterialsProduction3.getId())
              .getMaterialMgmtProductionPlanList()
              .get(0)
              .getManufacturingProductionLineList()
              .get(0), cost1, cost3, cost3));

      TestCostingUtils.assertProductTransaction(finalProduct.getId(), productTransactionAssertList,
          true);

      // Assert Transaction Cost after Landed Cost I
      assertTrue(TestCostingUtils.existsProductTransactionCostByCostIsUnitCostCurrency(
          TestCostingUtils.getProductTransactionsForProductionLine(productionLine1), true,
          TestCostingConstants.EURO_ID, new BigDecimal(60)));
      assertTrue(TestCostingUtils.existsProductTransactionCostByCostIsUnitCostCurrency(
          TestCostingUtils.getProductTransactionsForProductionLine(productionLine1), false,
          TestCostingConstants.EURO_ID, new BigDecimal(-60000)));

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Reactivate Landed Cost II
      TestCostingUtils.cancelLandedCost(landedCost2);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      productTransactionAssertList.remove(1);
      productTransactionAssertList.add(1,
          new ProductTransactionAssert(OBDal.getInstance()
              .get(ProductionTransaction.class, billOfMaterialsProduction.getId())
              .getMaterialMgmtProductionPlanList()
              .get(0)
              .getManufacturingProductionLineList()
              .get(0), cost1, totalCostAfterLC1, cost2));
      productTransactionAssertList.remove(2);
      productTransactionAssertList.add(2,
          new ProductTransactionAssert(OBDal.getInstance()
              .get(ProductionTransaction.class, billOfMaterialsProduction2.getId())
              .getMaterialMgmtProductionPlanList()
              .get(0)
              .getManufacturingProductionLineList()
              .get(0), cost1, cost2, cost2));
      productTransactionAssertList.remove(3);
      productTransactionAssertList.add(3,
          new ProductTransactionAssert(OBDal.getInstance()
              .get(ProductionTransaction.class, billOfMaterialsProduction3.getId())
              .getMaterialMgmtProductionPlanList()
              .get(0)
              .getManufacturingProductionLineList()
              .get(0), cost1, cost2, cost2));

      TestCostingUtils.assertProductTransaction(finalProduct.getId(), productTransactionAssertList,
          true);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Reactivate Landed Cost I
      TestCostingUtils.cancelLandedCost(landedCost1);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      productTransactionAssertList.remove(1);
      productTransactionAssertList.add(1,
          new ProductTransactionAssert(OBDal.getInstance()
              .get(ProductionTransaction.class, billOfMaterialsProduction.getId())
              .getMaterialMgmtProductionPlanList()
              .get(0)
              .getManufacturingProductionLineList()
              .get(0), cost1, totalCost, cost1));
      productTransactionAssertList.remove(2);
      productTransactionAssertList.add(2,
          new ProductTransactionAssert(OBDal.getInstance()
              .get(ProductionTransaction.class, billOfMaterialsProduction2.getId())
              .getMaterialMgmtProductionPlanList()
              .get(0)
              .getManufacturingProductionLineList()
              .get(0), cost1, cost1, cost1));
      productTransactionAssertList.remove(3);
      productTransactionAssertList.add(3,
          new ProductTransactionAssert(OBDal.getInstance()
              .get(ProductionTransaction.class, billOfMaterialsProduction3.getId())
              .getMaterialMgmtProductionPlanList()
              .get(0)
              .getManufacturingProductionLineList()
              .get(0), cost1, cost1, cost1));

      TestCostingUtils.assertProductTransaction(finalProduct.getId(), productTransactionAssertList,
          true);

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
