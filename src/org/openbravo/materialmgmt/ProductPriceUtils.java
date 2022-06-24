/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.materialmgmt;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.DalConnectionProvider;

public class ProductPriceUtils {

  private static final Logger log = LogManager.getLogger();

  /**
   * Returns a warning message when a Return From Customer product is not Returnable or when the
   * return period is expired.
   */
  public static JSONObject productReturnAllowedRFC(ShipmentInOutLine shipmentLine, Product product,
      Date rfcOrderDate) {
    JSONObject result = null;
    OBContext.setAdminMode(true);
    try {
      if (!product.isReturnable()) {
        throw new OBException(
            "@Product@ '" + product.getIdentifier() + "' @ServiceIsNotReturnable@");
      } else {
        try {
          final Date orderDate = shipmentLine != null && shipmentLine.getSalesOrderLine() != null
              ? DateUtils.truncate(shipmentLine.getSalesOrderLine().getOrderDate(),
                  Calendar.DAY_OF_MONTH)
              : null;
          Date returnDate = null;
          String message = null;
          if (orderDate != null && product.getOverdueReturnDays() != null) {
            returnDate = DateUtils.addDays(orderDate, product.getOverdueReturnDays().intValue());
          }
          if (product.getOverdueReturnDays() != null && returnDate != null
              && rfcOrderDate.after(returnDate)) {
            message = "@Product@ '" + product.getIdentifier() + "' @ServiceReturnExpired@: "
                + OBDateUtils.formatDate(returnDate);
          }
          if (product.getOverdueReturnDays() != null && returnDate == null) {
            message = "@Product@ '" + product.getIdentifier() + "' @ServiceMissingReturnDate@";
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
      }
      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
