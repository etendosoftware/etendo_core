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
 ***/
package org.openbravo.materialmgmt;

import java.util.Map;

import org.openbravo.client.application.FilterExpression;
import org.openbravo.common.actionhandler.InvoiceFromShipmentActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

/**
 * Class to filter the values in priceLists parameter in {@link InvoiceFromShipmentActionHandler}.
 * Considers the price list used by the orders linked to shipment lines.
 *
 */
public class InvoiceFromGoodsShipmentPriceListFilterExpression implements FilterExpression {

  @Override
  public String getExpression(Map<String, String> requestMap) {
    ShipmentInOut shipment = getShipmentFromContextData(requestMap);
    if (InvoiceFromGoodsShipmentUtil.shipmentLinesFromOrdersWithSamePriceList(shipment)) {
      return " e.id='" + InvoiceFromGoodsShipmentUtil.getPriceListFromOrder(shipment).get(0).getId()
          + "' ";
    }
    return " 1=1 ";
  }

  private ShipmentInOut getShipmentFromContextData(Map<String, String> requestMap) {
    String shipmentId = requestMap.get("inpmInoutId");
    return OBDal.getInstance().getProxy(ShipmentInOut.class, shipmentId);
  }

}
