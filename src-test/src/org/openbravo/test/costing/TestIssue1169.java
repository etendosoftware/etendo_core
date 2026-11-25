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
 * All portions are Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.costing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.test.costing.utils.TestCostingConstants;
import org.openbravo.test.costing.utils.TestCostingUtils;

/**
 * Regression test for ETP-5626 / https://github.com/etendosoftware/etendo_core/issues/1169
 *
 * <ul>
 * <li>Create and book a Purchase Order for 50 units</li>
 * <li>Create a draft Goods Receipt with two lines (30 + 30) linked to the same order line</li>
 * <li>Assert the receipt cannot be completed, as the lines add up to more than the ordered quantity</li>
 * <li>Assert the receipt can be completed when the lines add up to exactly the ordered quantity
 * (30 + 20), updating the delivered quantity of the order line</li>
 * </ul>
 */
public class TestIssue1169 extends TestCostingBase {

  private static final BigDecimal ORDERED_QTY = new BigDecimal("50");
  private static final BigDecimal FIRST_LINE_QTY = new BigDecimal("30");
  private static final BigDecimal PRICE = new BigDecimal("10.00");

  @Test
  public void testReceiptLinesExceedingOrderedQuantityCannotBeCompleted() throws Exception {
    try {
      setContext();
      ShipmentInOut goodsReceipt = createReceiptWithTwoLines("productIssue1169a",
          new BigDecimal("30"));

      try {
        TestCostingUtils.completeDocument(goodsReceipt);
        fail("The receipt must not be completed when its lines exceed the ordered quantity");
      } catch (OBException e) {
        assertTrue("Unexpected error: " + e.getMessage(), hasMessage(e, "MovementQtyCheck")
            || hasMessage(e, "greater than the Ordered Quantity"));
      }
    } finally {
      OBDal.getInstance().rollbackAndClose();
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testReceiptLinesMatchingOrderedQuantityAreCompleted() throws Exception {
    try {
      setContext();
      ShipmentInOut goodsReceipt = createReceiptWithTwoLines("productIssue1169b",
          new BigDecimal("20"));

      TestCostingUtils.completeDocument(goodsReceipt);
      OBDal.getInstance().flush();

      OrderLine orderLine = goodsReceipt.getSalesOrder().getOrderLineList().get(0);
      OBDal.getInstance().refresh(orderLine);
      assertEquals("Delivered quantity must be the sum of both receipt lines", 0,
          ORDERED_QTY.compareTo(orderLine.getDeliveredQuantity()));
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void setContext() {
    OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
        TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
        TestCostingConstants.SPAIN_ORGANIZATION_ID);
    OBContext.setAdminMode(true);
  }

  /**
   * Creates a draft Goods Receipt from a booked Purchase Order, with a first line of 30 units and
   * a second line of secondLineQty units, both linked to the same order line.
   */
  private ShipmentInOut createReceiptWithTwoLines(String productName, BigDecimal secondLineQty) {
    Product product = TestCostingUtils.createProduct(productName, PRICE);
    Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, PRICE, ORDERED_QTY, 0);
    OrderLine orderLine = purchaseOrder.getOrderLineList().get(0);

    ShipmentInOut goodsReceipt = TestCostingUtils.createMovementFromOrder(purchaseOrder.getId(),
        false, FIRST_LINE_QTY, TestCostingConstants.LOCATOR_L01_ID, 0);

    ShipmentInOutLine secondLine = (ShipmentInOutLine) DalUtil
        .copy(goodsReceipt.getMaterialMgmtShipmentInOutLineList().get(0), false);
    TestCostingUtils.setGeneralData(secondLine);
    secondLine.setShipmentReceipt(goodsReceipt);
    secondLine.setLineNo(20L);
    secondLine.setMovementQuantity(secondLineQty);
    secondLine.setSalesOrderLine(orderLine);
    goodsReceipt.getMaterialMgmtShipmentInOutLineList().add(secondLine);

    OBDal.getInstance().save(goodsReceipt);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(goodsReceipt);
    return goodsReceipt;
  }

  private static boolean hasMessage(Throwable throwable, String text) {
    for (Throwable t = throwable; t != null; t = t.getCause()) {
      if (t.getMessage() != null && t.getMessage().contains(text)) {
        return true;
      }
    }
    return false;
  }
}
