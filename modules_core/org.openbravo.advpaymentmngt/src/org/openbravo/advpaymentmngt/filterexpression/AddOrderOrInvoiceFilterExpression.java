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
 * All portions are Copyright (C) 2014-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.advpaymentmngt.filterexpression;

import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.client.application.OBBindingsConstants;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;

public class AddOrderOrInvoiceFilterExpression implements FilterExpression {
  private static final Logger log = LogManager.getLogger();
  @Inject
  @Any
  private Instance<AddOrderOrInvoiceFilterExpressionHandler> addOrderOrInvoiceFilterExpressionHandlers;

  @Override
  public String getExpression(Map<String, String> requestMap) {
    try {
      final String strWindowId = getWindowId(requestMap);
      AddOrderOrInvoiceFilterExpressionHandler handler = getHandler(strWindowId);
      String strColumnName = requestMap.get("filterExpressionColumnName");
      Columns column = Columns.getColumn(strColumnName);
      switch (column) {
        case PaymentMethod:
          String paymentMethodId = handler.getFilterExpression(requestMap);
          if (!"".equals(paymentMethodId) && paymentMethodId != null) {
            return OBDal.getInstance()
                .get(FIN_PaymentMethod.class, paymentMethodId)
                .getIdentifier();
          } else {
            return "";
          }
      }
    } catch (Exception e) {
      log.error("Error calculating filter expression", e);
      return "";
    }
    return "";
  }

  private String getWindowId(Map<String, String> requestMap) throws JSONException {
    final String strContext = requestMap.get("context");
    JSONObject context = new JSONObject(strContext);
    return context.getString(OBBindingsConstants.WINDOW_ID_PARAM);
  }

  private AddOrderOrInvoiceFilterExpressionHandler getHandler(String strWindowId) {
    AddOrderOrInvoiceFilterExpressionHandler handler = null;
    for (AddOrderOrInvoiceFilterExpressionHandler nextHandler : addOrderOrInvoiceFilterExpressionHandlers
        .select(new ComponentProvider.Selector(strWindowId))) {
      if (handler == null) {
        handler = nextHandler;
      } else {
        log.warn("Trying to get handler for window with id {}, there are more than one instance",
            strWindowId);
      }
    }
    return handler;
  }

  private enum Columns {
    PaymentMethod("paymentMethodName");

    private String columnname;

    Columns(String columnname) {
      this.columnname = columnname;
    }

    public String getColumnName() {
      return this.columnname;
    }

    static Columns getColumn(String strColumnName) {
      for (Columns column : Columns.values()) {
        if (strColumnName.equals(column.getColumnName())) {
          return column;
        }
      }
      return null;
    }
  }
}
