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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.test.costing.assertclass.CostAdjustmentAssert;
import org.openbravo.test.costing.utils.TestCostingConstants;
import org.openbravo.test.costing.utils.TestCostingUtils;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestIssue39616 extends TestCostingBase {

  /**
   * Test for issue 39616.
   * 
   * <ul>
   * <li>Create Landed Cost Type</li>
   * <li>Create Landed Cost</li>
   * <li>Create records in Cost Tab with USD Currency</li>
   * <li>Create records in Cost Tab with EUR Currency</li>
   * <li>Create records in Receipt Tab</li>
   * <li>Process Landed Cost</li>
   * <li>Assert Cost Adjustment for USD Currency</li>
   * <li>Assert Cost Adjustment for EUR Currency</li>
   */
  @Test
  public void testIssue39616() throws Exception {
    final int day0 = 0;
    final int day1 = 5;
    final BigDecimal price = new BigDecimal("10.00");
    final BigDecimal amount1 = new BigDecimal("10.00");
    final BigDecimal amount2 = new BigDecimal("20.00");
    final BigDecimal amount3 = new BigDecimal("30.00");
    final BigDecimal amount4 = new BigDecimal("40.00");
    final BigDecimal quantity = new BigDecimal("5");

    try {
      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testCostingLC39616", price);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(product, price, quantity,
          day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID);
      landedCostTypeIdList.add(TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(amount1);
      amountList.add(amount2);
      amountList.add(amount3);
      amountList.add(amount4);
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      List<ShipmentInOutLine> receiptLineList = new ArrayList<ShipmentInOutLine>();
      receiptList.add(goodsReceipt);
      receiptLineList.add(goodsReceipt.getMaterialMgmtShipmentInOutLineList().get(0));
      TestCostingUtils.createLandedCost(landedCostTypeIdList, amountList, receiptList,
          receiptLineList, day1);

      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());

      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());

      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0),
          TestCostingConstants.EURO_ID, "LC", amount1.add(amount3), day1, true, false));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0),
          TestCostingConstants.DOLLAR_ID, "LC", amount2.add(amount4), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
