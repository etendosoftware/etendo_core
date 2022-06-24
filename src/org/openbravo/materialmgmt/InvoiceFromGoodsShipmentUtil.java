/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
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
 *************************************************************************
 */
package org.openbravo.materialmgmt;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.query.Query;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.pricing.pricelist.PriceList;

/**
 * Helper class for generate invoice from goods shipment process
 */
public class InvoiceFromGoodsShipmentUtil {

  private static final String STATUS_COMPLETED = "CO";

  private InvoiceFromGoodsShipmentUtil() {
    throw new UnsupportedOperationException("This class should not be instantiated");
  }

  /**
   * Returns if all lines in Goods Shipment are linked to Sales Orders sharing the same Price List
   * 
   * @param shipment
   *          The Goods Shipment
   * @return True if all lines in Goods Shipment come from orders sharing the same Price List, false
   *         otherwise
   */
  public static boolean shipmentLinesFromOrdersWithSamePriceList(final ShipmentInOut shipment) {
    String hql = "select distinct coalesce(so.priceList.id, '0') "//
        + " from MaterialMgmtShipmentInOutLine iol left join iol.salesOrderLine sol "//
        + " left join sol.salesOrder so "//
        + " where iol.shipmentReceipt.id = :shipmentId";

    final Query<String> query = OBDal.getInstance().getSession().createQuery(hql, String.class);
    query.setParameter("shipmentId", shipment.getId());
    query.setMaxResults(2);
    final List<String> priceLists = query.list();
    return priceLists.size() == 1 && !priceLists.get(0).equals("0");
  }

  /**
   * Returns the Price List used by the orders the shipment lines are linked to. Assumes all lines
   * are linked to Sales Orders sharing the same Price List
   * 
   * @param shipment
   *          The Goods Shipment
   * @return The Price List
   */
  public static List<PriceList> getPriceListFromOrder(final ShipmentInOut shipment) {
    return Arrays.asList(shipment.getMaterialMgmtShipmentInOutLineList()
        .get(0)
        .getSalesOrderLine()
        .getSalesOrder()
        .getPriceList());
  }

  /**
   * Returns the Price List used by the Business Partner from the Goods Shipment
   * 
   * @param shipment
   *          The Goods Shipment
   * @return The Price List Id, if exists, empty string otherwise
   */
  public static String getPriceListFromBusinessPartner(final ShipmentInOut shipment) {
    if (shipment.getBusinessPartner().getPriceList() != null) {
      return shipment.getBusinessPartner().getPriceList().getId();
    }
    return StringUtils.EMPTY;
  }

  /**
   * Returns the message based on invoice status
   * 
   * @param invoice
   *          The Invoice
   * @return The message
   */
  public static String getInvoiceStatus(final Invoice invoice) {
    if (STATUS_COMPLETED.equals(invoice.getDocumentStatus())) {
      return OBMessageUtils.messageBD("StatusCompleted");
    }
    return OBMessageUtils.messageBD("StatusDraft");
  }
}
