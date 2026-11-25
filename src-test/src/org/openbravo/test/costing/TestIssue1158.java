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
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;

import org.hibernate.criterion.Restrictions;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.procurement.ReceiptInvoiceMatch;
import org.openbravo.test.costing.utils.TestCostingConstants;
import org.openbravo.test.costing.utils.TestCostingUtils;

/**
 * Regression test for ETP-5433 / https://github.com/etendosoftware/etendo_core/issues/1158
 *
 * <ul>
 * <li>Create and book a Purchase Order for 15 units</li>
 * <li>Create a Purchase Invoice from the order for only 7 units and complete it (the receipt does
 * not exist yet)</li>
 * <li>Create a Purchase Receipt from the order for the full 15 units and complete it</li>
 * <li>Assert the resulting Matched Purchase Invoice (M_Matched_Invoice) records the effectively
 * invoiced quantity (7), not the full receipt quantity (15)</li>
 * </ul>
 */
public class TestIssue1158 extends TestCostingBase {

  @Test
  public void testMatchedInvoiceQuantityWhenReceiptCompletedAfterInvoice() throws Exception {
    try {
      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      BigDecimal orderedQty = new BigDecimal("15");
      BigDecimal invoicedQty = new BigDecimal("7");
      BigDecimal price = new BigDecimal("10.00");

      Product product = TestCostingUtils.createProduct("productIssue1158", price);
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(product, price, orderedQty, 0);

      // Invoice the order for less than the ordered quantity while no receipt exists yet.
      Invoice purchaseInvoice = TestCostingUtils.createInvoiceFromOrder(purchaseOrder.getId(),
          false, price, invoicedQty, 0);
      InvoiceLine invoiceLine = purchaseInvoice.getInvoiceLineList().get(0);
      invoiceLine.setInvoicedQuantity(invoicedQty);
      invoiceLine.setLineNetAmount(invoicedQty.multiply(price));
      invoiceLine.setTaxableAmount(invoicedQty.multiply(price));
      OBDal.getInstance().save(invoiceLine);
      OBDal.getInstance().flush();
      TestCostingUtils.completeDocument(purchaseInvoice);

      // Create the receipt from the order for the full ordered quantity, completed afterwards.
      // "Create Lines From Order" (M_INOUT_CREATE) links the already-invoiced order line's
      // invoice line to the new receipt line (C_INVOICELINE.M_INOUTLINE_ID) before the receipt
      // is completed - replicate that linkage here as the UI action would.
      ShipmentInOut goodsReceipt = TestCostingUtils.createMovementFromOrder(purchaseOrder.getId(),
          false, orderedQty, TestCostingConstants.LOCATOR_L01_ID, 0);
      invoiceLine.setGoodsShipmentLine(
          goodsReceipt.getMaterialMgmtShipmentInOutLineList().get(0));
      OBDal.getInstance().save(invoiceLine);
      OBDal.getInstance().flush();

      TestCostingUtils.completeDocument(goodsReceipt);
      OBDal.getInstance().flush();

      OBCriteria<ReceiptInvoiceMatch> matchCriteria = OBDal.getInstance()
          .createCriteria(ReceiptInvoiceMatch.class);
      matchCriteria.add(Restrictions.eq(ReceiptInvoiceMatch.PROPERTY_INVOICELINE, invoiceLine));
      matchCriteria.setMaxResults(1);
      ReceiptInvoiceMatch match = (ReceiptInvoiceMatch) matchCriteria.uniqueResult();

      assertNotNull("No Matched Purchase Invoice record was created for the invoice line", match);
      assertEquals(
          "Matched invoice quantity must be capped to the effectively invoiced quantity (7), "
              + "not the full receipt quantity (15)",
          0, invoicedQty.compareTo(match.getQuantity()));
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
