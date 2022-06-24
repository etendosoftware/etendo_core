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

package org.openbravo.common.actionhandler.createlinesfromprocess;

import java.util.List;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.invoice.InvoiceLineAccountingDimension;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderLineAccountingDimension;
import org.openbravo.model.materialmgmt.transaction.InOutLineAccountingDimension;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

/**
 * Inserts accounting dimension lines to the invoice line from the copied line
 * 
 */
class InsertAcctDimensions {
  private final InvoiceLine invoiceLine;
  private final BaseOBObject createdFromLine;

  InsertAcctDimensions(final InvoiceLine invoiceLine, final BaseOBObject createdFromLine) {
    this.invoiceLine = invoiceLine;
    this.createdFromLine = createdFromLine;
  }

  void createAndSaveAcctDimensionLines() {
    if (CreateLinesFromUtil.isOrderLine(createdFromLine)) {
      saveOrderLineAcctDimension();
    } else {
      saveInOutLineAcctDimension();
    }
  }

  private void saveOrderLineAcctDimension() {
    OrderLine orderLine = ((OrderLine) createdFromLine);
    List<OrderLineAccountingDimension> orderLineAccDimensions = orderLine
        .getOrderLineAccountingDimensionList();
    for (OrderLineAccountingDimension orderLineAccDimension : orderLineAccDimensions) {
      createInvoiceLineAccDimension(orderLineAccDimension, null);
      OBDal.getInstance().getSession().evict(orderLineAccDimension);
    }
  }

  private void saveInOutLineAcctDimension() {
    ShipmentInOutLine inOutLine = ((ShipmentInOutLine) createdFromLine);
    List<InOutLineAccountingDimension> inOutLineAccDimensions = inOutLine
        .getInOutLineAccountingDimensionList();
    for (InOutLineAccountingDimension inOutLineAccDimension : inOutLineAccDimensions) {
      createInvoiceLineAccDimension(null, inOutLineAccDimension);
      OBDal.getInstance().getSession().evict(inOutLineAccDimension);
    }
  }

  /**
   * Creates a new InvoiceLineAccountingDimension to an invoice line from an
   * OrderLineAccountingDimension if it is passed or from InOutLineAccountingDimension if not
   * 
   * @param orderLineAccDimension
   *          The order line accounting dimension is copied
   * @param inOutLineAccDimension
   *          The inout line accounting dimension is copied
   */
  private void createInvoiceLineAccDimension(OrderLineAccountingDimension orderLineAccDimension,
      InOutLineAccountingDimension inOutLineAccDimension) {
    InvoiceLineAccountingDimension invoiceLineAccDimension = OBProvider.getInstance()
        .get(InvoiceLineAccountingDimension.class);
    invoiceLineAccDimension
        .setActivity(orderLineAccDimension != null ? orderLineAccDimension.getActivity()
            : inOutLineAccDimension.getActivity());
    invoiceLineAccDimension
        .setAmount(orderLineAccDimension != null ? orderLineAccDimension.getAmount()
            : inOutLineAccDimension.getQuantity().multiply(invoiceLine.getStandardPrice()));
    invoiceLineAccDimension
        .setAsset(orderLineAccDimension != null ? orderLineAccDimension.getAsset()
            : inOutLineAccDimension.getAsset());
    invoiceLineAccDimension.setBusinessPartner(
        orderLineAccDimension != null ? orderLineAccDimension.getBusinessPartner()
            : inOutLineAccDimension.getBusinessPartner());
    invoiceLineAccDimension
        .setCostcenter(orderLineAccDimension != null ? orderLineAccDimension.getCostcenter()
            : inOutLineAccDimension.getCostcenter());
    invoiceLineAccDimension
        .setNdDimension(orderLineAccDimension != null ? orderLineAccDimension.getNdDimension()
            : inOutLineAccDimension.getNdDimension());
    invoiceLineAccDimension
        .setOrganization(orderLineAccDimension != null ? orderLineAccDimension.getOrganization()
            : inOutLineAccDimension.getOrganization());
    invoiceLineAccDimension
        .setProduct(orderLineAccDimension != null ? orderLineAccDimension.getProduct()
            : inOutLineAccDimension.getProduct());
    invoiceLineAccDimension
        .setProject(orderLineAccDimension != null ? orderLineAccDimension.getProject()
            : inOutLineAccDimension.getProject());
    invoiceLineAccDimension
        .setStDimension(orderLineAccDimension != null ? orderLineAccDimension.getStDimension()
            : inOutLineAccDimension.getStDimension());
    invoiceLineAccDimension.setInvoiceLine(invoiceLine);
    OBDal.getInstance().save(invoiceLineAccDimension);
  }

}
