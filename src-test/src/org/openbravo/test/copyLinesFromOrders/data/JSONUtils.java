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
 * All portions are Copyright (C) 2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.copyLinesFromOrders.data;

import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.model.common.order.Order;

public class JSONUtils {

  public static JSONArray createJSONFromOrders(List<Order> ordersFrom) {
    JSONArray orders = new JSONArray();
    for (Order order : ordersFrom) {
      try {
        orders.put(getFormattedOrder(order));
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
    return orders;
  }

  private static JSONObject getFormattedOrder(Order order) throws JSONException {
    JSONObject formattedOrder = new JSONObject();
    formattedOrder.put("$ref", "Order" + order.getId());
    formattedOrder.put("id", order.getId());
    formattedOrder.put("client", order.getClient().getId());
    formattedOrder.put("organization", order.getOrganization().getId());
    formattedOrder.put("salesTransaction", order.isSalesTransaction());
    formattedOrder.put("businessPartner", order.getBusinessPartner().getId());
    formattedOrder.put("currency", order.getCurrency().getId());
    formattedOrder.put("paymentTerms", order.getPaymentTerms().getId());
    formattedOrder.put("warehouse", order.getWarehouse().getId());
    formattedOrder.put("priceList", order.getPriceList().getId());
    formattedOrder.put("priceIncludesTax", order.isPriceIncludesTax());
    return formattedOrder;
  }

}
