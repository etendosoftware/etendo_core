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

package org.openbravo.test.createlinesfrom;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.common.actionhandler.createlinesfromprocess.CreateInvoiceLinesFromProcess;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.test.createlinesfrom.data.CLFTestDataPO_02;
import org.openbravo.test.createlinesfrom.data.CreateLinesFromTestData;

/**
 * Checks how the Alternate Tax Base Amount is inherited by the invoice lines created with the
 * Create Lines From process when only part of the ordered quantity has been received.
 * <p>
 * The Alternate Tax Base Amount is a manual line level amount, so by default it must be inherited
 * from the purchase order line without any adjustment, no matter whether the order is still open or
 * it has already been closed. Only when the AlternateTaxBaseAmountProrating preference is enabled
 * it is prorated according to the invoiced quantity, again in both cases.
 * <p>
 * Closing the order overwrites the ordered quantity of its lines with the received one, so the
 * amount and the quantity of the source line are captured before closing it in order to assert the
 * same expectation in the four scenarios.
 */
public class AlternateTaxBaseAmountProratingTest extends WeldBaseTest {

  private static final String PREFERENCE_PROPERTY = "AlternateTaxBaseAmountProrating";
  // User Openbravo
  private static final String USER_ID = "100";
  // Client QA Testing
  private static final String CLIENT_ID = "4028E6C72959682B01295A070852010D";
  // Organization Spain
  private static final String ORGANIZATION_ID = "357947E87C284935AD1D783CF6F099A1";
  // Role QA Testing Admin
  private static final String ROLE_ID = "4028E6C72959682B01295A071429011E";
  private static final String ORDER_COMPLETE_PROCEDURE_NAME = "c_order_post1";
  private static final String SHIPMENT_INOUT_COMPLETE_PROCEDURE_NAME = "m_inout_post";

  private static final BigDecimal RECEIVED_QUANTITY = new BigDecimal("5");

  private CreateLinesFromTestData data;
  private Preference preference;

  @Before
  public void before() {
    OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
    data = new CLFTestDataPO_02();
  }

  @After
  public void after() {
    if (preference != null) {
      OBContext.setAdminMode();
      try {
        OBDal.getInstance().remove(preference);
        OBDal.getInstance().flush();
      } finally {
        OBContext.restorePreviousMode();
        preference = null;
      }
    }
  }

  @Test
  public void alternateTaxBaseAmountIsInheritedOnOpenOrder() {
    invoiceReceivedQuantity(false, false);
  }

  @Test
  public void alternateTaxBaseAmountIsInheritedOnClosedOrder() {
    invoiceReceivedQuantity(false, true);
  }

  @Test
  public void alternateTaxBaseAmountIsProratedOnOpenOrder() {
    invoiceReceivedQuantity(true, false);
  }

  @Test
  public void alternateTaxBaseAmountIsProratedOnClosedOrder() {
    invoiceReceivedQuantity(true, true);
  }

  /**
   * Creates a purchase order, receives part of the ordered quantity and invoices that partial
   * receipt, asserting the Alternate Tax Base Amount of the resulting invoice line.
   *
   * @param prorate
   *          whether the AlternateTaxBaseAmountProrating preference must be enabled
   * @param closeOrder
   *          whether the purchase order must be closed after the partial receipt
   */
  private void invoiceReceivedQuantity(final boolean prorate, final boolean closeOrder) {
    Order order = data.createOrder();
    order.setDocumentAction("CO");
    order = processOrder(order);
    OrderLine orderLine = order.getOrderLineList().get(0);

    ShipmentInOut receipt = data.createShipmentInOut();
    receipt.setSalesOrder(order);
    ShipmentInOutLine receiptLine = receipt.getMaterialMgmtShipmentInOutLineList().get(0);
    receiptLine.setSalesOrderLine(orderLine);
    receiptLine.setMovementQuantity(RECEIVED_QUANTITY);
    OBDal.getInstance().save(receiptLine);
    OBDal.getInstance().save(receipt);
    OBDal.getInstance().flush();
    receipt.setDocumentAction("CO");
    receipt = processShipmentInOut(receipt);

    OBDal.getInstance().refresh(orderLine);
    final BigDecimal sourceTaxBaseAmt = orderLine.getTaxableAmount();
    final BigDecimal sourceOrderedQty = orderLine.getOrderedQuantity();

    if (prorate) {
      createPreference();
    }
    if (closeOrder) {
      order.setDocumentAction("CL");
      order = processOrder(order);
    }

    OBDal.getInstance().refresh(orderLine);
    OBDal.getInstance().refresh(receipt);
    final String diagnostics = String.format(
        "source[taxBaseAmt=%s, qtyOrdered=%s], atInvoicing[taxBaseAmt=%s, qtyOrdered=%s, "
            + "lineNetAmt=%s], preference=%s, orderStatus=%s",
        sourceTaxBaseAmt, sourceOrderedQty, orderLine.getTaxableAmount(),
        orderLine.getOrderedQuantity(), orderLine.getLineNetAmount(),
        prorate ? Preferences.YES : "absent", order.getDocumentStatus());

    Invoice invoice = data.createInvoiceHeader();
    WeldUtils.getInstanceFromStaticBeanManager(CreateInvoiceLinesFromProcess.class)
        .createInvoiceLinesFromDocumentLines(selectedLinesFrom(receipt), invoice,
            ShipmentInOutLine.class);

    OBDal.getInstance().refresh(invoice);
    final InvoiceLine invoiceLine = invoice.getInvoiceLineList().get(0);
    final BigDecimal expected = expectedTaxBaseAmt(prorate, sourceTaxBaseAmt, sourceOrderedQty,
        invoice);

    assertEquals("Wrong Alternate Tax Base Amount in the invoice line. Expected " + expected
        + " but was " + invoiceLine.getTaxableAmount() + ". " + diagnostics, 0,
        expected.compareTo(invoiceLine.getTaxableAmount()));
  }

  /**
   * Without the preference the manual amount is inherited as it is; with the preference it is
   * prorated according to the invoiced quantity. Both expectations hold whether the order is open
   * or closed at invoicing time.
   */
  private BigDecimal expectedTaxBaseAmt(final boolean prorate, final BigDecimal sourceTaxBaseAmt,
      final BigDecimal sourceOrderedQty, final Invoice invoice) {
    if (!prorate) {
      return sourceTaxBaseAmt;
    }
    return sourceTaxBaseAmt.multiply(RECEIVED_QUANTITY)
        .divide(sourceOrderedQty, invoice.getCurrency().getStandardPrecision().intValue(),
            RoundingMode.HALF_UP);
  }

  private void createPreference() {
    OBContext.setAdminMode();
    try {
      preference = Preferences.setPreferenceValue(PREFERENCE_PROPERTY, Preferences.YES, true,
          OBContext.getOBContext().getCurrentClient(), null, null, null, null, null);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private JSONArray selectedLinesFrom(final ShipmentInOut receipt) {
    JSONArray selectedLines = new JSONArray();
    for (ShipmentInOutLine receiptLine : receipt.getMaterialMgmtShipmentInOutLineList()) {
      try {
        JSONObject line = new JSONObject();
        line.put("uOM", receiptLine.getUOM().getId());
        line.put("uOM$_identifier", receiptLine.getUOM().getIdentifier());
        line.put("product", receiptLine.getProduct().getId());
        line.put("product$_identifier", receiptLine.getProduct().getIdentifier());
        line.put("lineNo", receiptLine.getLineNo());
        line.put("movementQuantity", receiptLine.getMovementQuantity().toString());
        line.put("operativeQuantity",
            receiptLine.getOperativeQuantity() == null
                ? receiptLine.getMovementQuantity().toString()
                : receiptLine.getOperativeQuantity().toString());
        line.put("id", receiptLine.getId());
        line.put("operativeUOM",
            receiptLine.getOperativeUOM() == null ? receiptLine.getUOM().getId()
                : receiptLine.getOperativeUOM().getId());
        line.put("operativeUOM$_identifier",
            receiptLine.getOperativeUOM() == null ? receiptLine.getUOM().getIdentifier()
                : receiptLine.getOperativeUOM().getIdentifier());
        line.put("orderQuantity", "");
        selectedLines.put(line);
      } catch (JSONException e) {
        throw new IllegalStateException(e);
      }
    }
    return selectedLines;
  }

  private Order processOrder(final Order order) {
    callProcedure(ORDER_COMPLETE_PROCEDURE_NAME, order.getId());
    OBDal.getInstance().refresh(order);
    return order;
  }

  private ShipmentInOut processShipmentInOut(final ShipmentInOut shipmentInOut) {
    callProcedure(SHIPMENT_INOUT_COMPLETE_PROCEDURE_NAME, shipmentInOut.getId());
    OBDal.getInstance().refresh(shipmentInOut);
    return shipmentInOut;
  }

  private void callProcedure(final String procedureName, final String recordId) {
    final List<Object> params = new ArrayList<>();
    params.add(null);
    params.add(recordId);
    CallStoredProcedure.getInstance().call(procedureName, params, null, true, false);
  }
}
