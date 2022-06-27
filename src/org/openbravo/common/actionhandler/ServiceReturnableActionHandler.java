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
 * All portions are Copyright (C) 2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.actionhandler;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.DalConnectionProvider;

public class ServiceReturnableActionHandler extends BaseActionHandler {
  private static final Logger log = LogManager.getLogger();
  private static final String PRODUCT = "@Product@ '";

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    OBContext.setAdminMode(true);
    try {
      JSONObject jsonRequest = new JSONObject(content);
      log.debug("{}", jsonRequest);

      final String strRFCOrderDate = jsonRequest.getString("rfcOrderDate");
      final Date rfcOrderDate = OBDateUtils.getDate(strRFCOrderDate);
      final ShipmentInOutLine shipmentLine = OBDal.getInstance()
          .get(ShipmentInOutLine.class, jsonRequest.getString("goodsShipmentId"));
      final Product serviceProduct = OBDal.getInstance()
          .get(Product.class, jsonRequest.getString("productId"));

      return new JSONObject().put("message", productReturnAllowedRFC(shipmentLine, serviceProduct, rfcOrderDate));

    } catch (Exception e) {
      log.error("Error in RFCServiceReturnableActionHandler Action Handler", e);
      try {
        String message = OBMessageUtils.parseTranslation(new DalConnectionProvider(false),
            RequestContext.get().getVariablesSecureApp(),
            OBContext.getOBContext().getLanguage().getLanguage(), e.getMessage());
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("title", "Error");
        errorMessage.put("text", message);
        return new JSONObject().put("message", errorMessage);
      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return null;
  }

  /**
   * Returns a warning message when a Return From Customer product is not Returnable or when the
   * return period is expired.
   */
  public static JSONObject productReturnAllowedRFC(ShipmentInOutLine shipmentLine, Product product,
      Date rfcOrderDate) {

    OBContext.setAdminMode(true);
    try {
      JSONObject result = null;
      if (!product.isReturnable()) {
        throw new OBException(
            PRODUCT + product.getIdentifier() + "' @ServiceIsNotReturnable@");
      }
      try {
        Date orderDate = null;
        if (shipmentLine != null && shipmentLine.getSalesOrderLine() != null) {
          orderDate = DateUtils.truncate(shipmentLine.getSalesOrderLine().getOrderDate(), Calendar.DAY_OF_MONTH);
        }
        Date returnDate = null;
        String message = null;
        Boolean isNullOverdueReturnDays = product.getOverdueReturnDays() != null;
        if (orderDate != null && isNullOverdueReturnDays) {
          returnDate = DateUtils.addDays(orderDate, product.getOverdueReturnDays().intValue());
        }
        if (isNullOverdueReturnDays && returnDate != null
            && rfcOrderDate.after(returnDate)) {
          message = PRODUCT + product.getIdentifier() + "' @ServiceReturnExpired@: "
              + OBDateUtils.formatDate(returnDate);
        }
        if (isNullOverdueReturnDays && returnDate == null) {
          message = PRODUCT + product.getIdentifier() + "' @ServiceMissingReturnDate@";
        }

        if (message != null) {
          message = OBMessageUtils.parseTranslation(new DalConnectionProvider(false),
              RequestContext.get().getVariablesSecureApp(),
              OBContext.getOBContext().getLanguage().getLanguage(), message);
          result = new JSONObject();
          result.put("severity", "warning");
          result.put("title", "Warning");
          result.put("text", message);
        }
      } catch (JSONException e) {
        log.error(e.getMessage(), e);
      }
      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
