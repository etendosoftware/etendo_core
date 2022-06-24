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
 * All portions are Copyright (C) 2017-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.actionhandler.copyfromorderprocess;

import java.io.IOException;

import javax.enterprise.context.Dependent;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Tax;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.service.db.DalConnectionProvider;

@Dependent
@Qualifier(CopyFromOrdersProcessImplementationInterface.COPY_FROM_ORDER_PROCESS_HOOK_QUALIFIER)
class UpdateTax implements CopyFromOrdersProcessImplementationInterface {
  private static final String ALTERNATE_TAX_BASE_AMOUNT_WITH_TAXAMOUNT = "TBATAX";
  private static final String ALTERNATE_TAX_BASE_AMOUNT = "TBA";
  private static final Logger log = LogManager.getLogger();

  @Override
  public int getOrder() {
    return -10;
  }

  /**
   * Update order line tax. Throws an exception if no taxes are found.
   * 
   * @param newOrderLine
   *          The order line where tax will be updated.
   */
  @Override
  public void exec(final Order processingOrder, final OrderLine orderLine, OrderLine newOrderLine) {
    TaxRate tax = OBDal.getInstance()
        .getProxy(TaxRate.class, getCurrentTaxId(newOrderLine.getProduct(), processingOrder));
    newOrderLine.setTax(tax);
    if (hasAlternateTaxAmount(tax) && hasAlternateTaxAmount(orderLine.getTax())) {
      newOrderLine.setTaxableAmount(orderLine.getTaxableAmount());
    }
  }

  private boolean hasAlternateTaxAmount(final TaxRate tax) {
    return tax.getBaseAmount().equals(ALTERNATE_TAX_BASE_AMOUNT)
        || tax.getBaseAmount().equals(ALTERNATE_TAX_BASE_AMOUNT_WITH_TAXAMOUNT);
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
  private String getCurrentTaxId(final Product product, final Order processingOrder) {
    String bpBillToLocationId = (processingOrder.getInvoiceAddress() != null)
        ? processingOrder.getInvoiceAddress().getId()
        : getMaxBusinessPartnerLocationId(processingOrder.getBusinessPartner());
    String bpLocationId = processingOrder.getPartnerAddress().getId();
    String orderWarehouseId = processingOrder.getWarehouse() != null
        ? processingOrder.getWarehouse().getId()
        : "";
    String orderProjectId = processingOrder.getProject() != null
        ? processingOrder.getProject().getId()
        : "";
    String strDatePromised = DateFormatUtils.format(processingOrder.getScheduledDeliveryDate(),
        OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("dateFormat.java"));

    String taxID;
    try {
      taxID = Tax.get(new DalConnectionProvider(), product.getId(), strDatePromised,
          processingOrder.getOrganization().getId(), orderWarehouseId, bpBillToLocationId,
          bpLocationId, orderProjectId, processingOrder.isSalesTransaction());
    } catch (IOException | ServletException e) {
      log.error("Error in CopyFromOrdersProcess while retrieving the TaxID for a Product", e);
      throw new OBException(e);
    }
    if (StringUtils.isEmpty(taxID)) {
      throw new OBException("@TaxNotFound@");
    }
    return taxID;
  }

  /**
   * Returns the last business partner location ID
   * 
   * @param businessPartner
   *          The business partner where the location will be searched
   * @return the last business partner location ID
   */
  private String getMaxBusinessPartnerLocationId(final BusinessPartner businessPartner) {
    OBCriteria<Location> obc = OBDal.getInstance().createCriteria(Location.class);
    obc.add(Restrictions.eq(Location.PROPERTY_BUSINESSPARTNER, businessPartner));
    obc.add(Restrictions.eq(Location.PROPERTY_ACTIVE, true));
    obc.setProjection(Projections.max(Location.PROPERTY_ID));
    obc.setMaxResults(1);
    return (String) obc.uniqueResult();
  }

}
