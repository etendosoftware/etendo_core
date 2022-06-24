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
 * All portions are Copyright (C) 2015-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.actionhandler;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.ServicePriceUtils;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.service.db.DalConnectionProvider;

public class ServiceRelatedLinePriceActionHandler extends BaseActionHandler {
  private static final Logger log = LogManager.getLogger();
  private static final String RFC_ORDERLINE_TAB_ID = "AF4090093D471431E040007F010048A5";

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    OBContext.setAdminMode(true);
    JSONObject errorMessage = new JSONObject();
    JSONObject result = new JSONObject();
    try {
      jsonRequest = new JSONObject(content);
      log.debug("{}", jsonRequest);

      final OrderLine serviceOrderline = OBDal.getInstance()
          .get(OrderLine.class, jsonRequest.getString("orderlineId"));
      BigDecimal amount = new BigDecimal(jsonRequest.getString("amount"));
      BigDecimal discounts = new BigDecimal(jsonRequest.getString("discounts"));
      BigDecimal priceamount = new BigDecimal(jsonRequest.getString("priceamount"));
      BigDecimal relatedQty = new BigDecimal(jsonRequest.getString("relatedqty"));
      BigDecimal unitDiscountsAmt = new BigDecimal(jsonRequest.getString("unitdiscountsamt"));
      JSONObject relatedInfo = jsonRequest.optJSONObject("relatedLinesInfo");
      final OrderLine orderLineToRelate = OBDal.getInstance()
          .get(OrderLine.class, jsonRequest.getString("orderLineToRelateId"));
      String tabId = jsonRequest.getString("tabId");
      boolean state = false;
      if (jsonRequest.has("state") && jsonRequest.get("state") != JSONObject.NULL) {
        state = jsonRequest.getBoolean("state");
      }
      JSONObject deferredSale = null;
      BigDecimal serviceTotalAmount = ServicePriceUtils.getServiceAmount(serviceOrderline, amount,
          discounts, priceamount, relatedQty, unitDiscountsAmt, relatedInfo);
      if (jsonRequest.has("orderLineToRelateId")
          && jsonRequest.get("orderLineToRelateId") != JSONObject.NULL
          && !RFC_ORDERLINE_TAB_ID.equals(tabId) && state) {
        deferredSale = ServicePriceUtils.deferredSaleAllowed(serviceOrderline, orderLineToRelate);
      }
      result.put("amount", serviceTotalAmount);
      result.put("message", deferredSale);
    } catch (Exception e) {
      log.error("Error in ServiceRelatedLinePriceActionHandler Action Handler", e);
      try {
        result = new JSONObject();
        String message = OBMessageUtils.parseTranslation(new DalConnectionProvider(false),
            RequestContext.get().getVariablesSecureApp(),
            OBContext.getOBContext().getLanguage().getLanguage(), e.getMessage());
        errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("title", "Error");
        errorMessage.put("text", message);
        result.put("message", errorMessage);

      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }
}
