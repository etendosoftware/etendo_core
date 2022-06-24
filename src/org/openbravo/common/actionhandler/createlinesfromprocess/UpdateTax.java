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
 * All portions are Copyright (C) 2018-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.actionhandler.createlinesfromprocess;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import javax.enterprise.context.Dependent;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Tax;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.project.Project;
import org.openbravo.service.db.DalConnectionProvider;

@Dependent
@Qualifier(CreateLinesFromProcessHook.CREATE_LINES_FROM_PROCESS_HOOK_QUALIFIER)
class UpdateTax extends CreateLinesFromProcessHook {
  private static final Logger log = LogManager.getLogger();

  @Override
  public int getOrder() {
    return -10;
  }

  /**
   * Update order line tax, taxable amount and tax amount. Throws an exception if no taxes are
   * found.
   */
  @Override
  public void exec() {
    updateTaxRate();
    updateTaxableAmount();
  }

  private void updateTaxRate() {
    TaxRate tax = OBDal.getInstance()
        .getProxy(TaxRate.class, getCurrentTaxId(getInvoiceLine().getProduct()));
    getInvoiceLine().setTax(tax);
  }

  /**
   * Gets the current tax according order information and selected product. If any tax is found an
   * exception is thrown.
   * 
   * @param product
   *          The product where taxes are searching for
   * @return The Tax ID or an exception if it is not found
   * @throws IOException
   * @throws ServletException
   */
  private String getCurrentTaxId(final Product product) {
    String taxID = "";
    if (isCopiedFromOrderLine()
        || CreateLinesFromUtil.hasRelatedOrderLine((ShipmentInOutLine) getCopiedFromLine())) {
      if (isCopiedFromOrderLine()) {
        taxID = ((OrderLine) getCopiedFromLine()).getTax().getId();
      } else {
        taxID = ((ShipmentInOutLine) getCopiedFromLine()).getSalesOrderLine().getTax().getId();
      }
    } else {
      ShipmentInOutLine copiedFromIOLine = (ShipmentInOutLine) getCopiedFromLine();
      Warehouse warehouse = copiedFromIOLine.getShipmentReceipt().getWarehouse();
      Project project = copiedFromIOLine.getProject();
      Organization organization = copiedFromIOLine.getOrganization();
      boolean isSalesTransaction = copiedFromIOLine.getShipmentReceipt().isSalesTransaction();
      Date scheduledDeliveryDate = copiedFromIOLine.getShipmentReceipt().getMovementDate();

      String bpLocationId = getInvoice().getPartnerAddress().getId();
      String strDatePromised = DateFormatUtils.format(scheduledDeliveryDate,
          OBPropertiesProvider.getInstance()
              .getOpenbravoProperties()
              .getProperty("dateFormat.java"));

      try {
        taxID = Tax.get(new DalConnectionProvider(), product.getId(), strDatePromised,
            organization.getId(), (warehouse != null) ? warehouse.getId() : "", bpLocationId,
            bpLocationId, (project != null) ? project.getId() : "", isSalesTransaction);
      } catch (IOException | ServletException e) {
        log.error("Error in CopyFromProcess while retrieving the TaxID for a Product", e);
        throw new OBException(e);
      }
      if (StringUtils.isEmpty(taxID)) {
        throw new OBException("@TaxNotFound@");
      }
    }
    return taxID;
  }

  private void updateTaxableAmount() {
    BigDecimal taxBaseAmt = getInvoiceLine().getLineNetAmount();
    if (isCopiedFromOrderLine()
        || CreateLinesFromUtil.hasRelatedOrderLine((ShipmentInOutLine) getCopiedFromLine())) {
      taxBaseAmt = calculateAlternateTaxBaseAmtProrating(taxBaseAmt);
    }
    getInvoiceLine().setTaxableAmount(taxBaseAmt);
  }

  private BigDecimal calculateAlternateTaxBaseAmtProrating(final BigDecimal invoiceLineNetAmt) {
    BigDecimal taxBaseAmt = invoiceLineNetAmt;
    final OrderLine originalOrderLine = (isCopiedFromOrderLine() ? (OrderLine) getCopiedFromLine()
        : ((ShipmentInOutLine) getCopiedFromLine()).getSalesOrderLine());
    if (originalOrderLine.getTaxableAmount() != null) {
      BigDecimal originalOrderedQuantity = originalOrderLine.getOrderedQuantity();
      BigDecimal qtyOrdered = CreateLinesFromUtil.getOrderedQuantity(getPickExecJSONObject());
      taxBaseAmt = originalOrderLine.getTaxableAmount();
      if (originalOrderedQuantity.compareTo(BigDecimal.ZERO) != 0) {
        taxBaseAmt = taxBaseAmt.multiply(qtyOrdered)
            .divide(originalOrderedQuantity,
                getInvoice().getCurrency().getStandardPrecision().intValue(), RoundingMode.HALF_UP);
      }
    }
    return taxBaseAmt;
  }
}
