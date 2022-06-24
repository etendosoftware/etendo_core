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
 * All portions are Copyright (C) 2014 Openbravo SLU
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
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.client.application.OBBindingsConstants;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;

public class AddPaymentReadOnlyLogicsExpression implements FilterExpression {
  private static final Logger log = LogManager.getLogger();
  @Inject
  @Any
  private Instance<AddPaymentReadOnlyLogicsHandler> addPaymentFilterExpressionHandlers;

  @Override
  public String getExpression(Map<String, String> requestMap) {
    String strCurrentParam = "";
    try {
      OBContext.setAdminMode(true);
      final String strWindowId = getWindowId(requestMap);

      AddPaymentReadOnlyLogicsHandler handler = getHandler(strWindowId);
      if (handler == null) {
        throw new OBException(String.format(OBMessageUtils.messageBD("APRM_NOHANDLER")));
      }
      strCurrentParam = requestMap.get("currentParam");
      Parameters param = Parameters.getParameter(strCurrentParam);
      try {
        switch (param) {
          case PaymentDocumentNo:
            return handler.getPaymentDocumentNoReadOnlyLogic(requestMap) ? "Y" : "N";
          case ReceivedFrom:
            return handler.getReceivedFromReadOnlyLogic(requestMap) ? "Y" : "N";
          case PaymentMethod:
            return handler.getPaymentMethodReadOnlyLogic(requestMap) ? "Y" : "N";
          case ActualPayment:
            return handler.getActualPaymentReadOnlyLogic(requestMap) ? "Y" : "N";
          case ConvertedAmount:
            return handler.getConvertedAmountReadOnlyLogic(requestMap) ? "Y" : "N";
          case PaymentDate:
            return handler.getPaymentDateReadOnlyLogic(requestMap) ? "Y" : "N";
          case FinancialAccount:
            return handler.getFinancialAccountReadOnlyLogic(requestMap) ? "Y" : "N";
          case ConversionRate:
            return handler.getConversionRateReadOnlyLogic(requestMap) ? "Y" : "N";
          case Currency:
            return handler.getCurrencyReadOnlyLogic(requestMap) ? "Y" : "N";
        }
      } catch (Exception e) {
        log.error("Error trying to get default value of " + strCurrentParam + " " + e.getMessage(),
            e);
        return null;
      }
    } catch (JSONException ignore) {
    } finally {
      OBContext.restorePreviousMode();
    }
    log.error("No default value found for param: " + strCurrentParam);
    return null;
  }

  private String getWindowId(Map<String, String> requestMap) throws JSONException {
    final String strContext = requestMap.get("context");
    if (strContext != null) {
      JSONObject context = new JSONObject(strContext);
      if (context != null && context.has(OBBindingsConstants.WINDOW_ID_PARAM)) {
        return context.getString(OBBindingsConstants.WINDOW_ID_PARAM);
      }
    }
    return "NULLWINDOWID";
  }

  private AddPaymentReadOnlyLogicsHandler getHandler(String strWindowId) {
    AddPaymentReadOnlyLogicsHandler handler = null;
    for (AddPaymentReadOnlyLogicsHandler nextHandler : addPaymentFilterExpressionHandlers
        .select(new ComponentProvider.Selector(strWindowId))) {
      if (handler == null) {
        handler = nextHandler;
      } else if (nextHandler.getSeq() < handler.getSeq()) {
        handler = nextHandler;
      } else if (nextHandler.getSeq() == handler.getSeq()) {
        log.warn(
            "Trying to get handler for window with id {}, there are more than one instance with the same sequence",
            strWindowId);
      }
    }
    return handler;
  }

  private enum Parameters {
    PaymentDocumentNo("payment_documentno_readonly_logic"),
    ReceivedFrom("received_from_readonly_logic"),
    PaymentMethod("payment_method_readonly_logic"),
    ActualPayment("actual_payment_readonly_logic"),
    ConvertedAmount("converted_amount_readonly_logic"),
    PaymentDate("payment_date_readonly_logic"),
    FinancialAccount("fin_financial_account_id_readonly_logic"),
    ConversionRate("conversion_rate_readonly_logic"),
    Currency("c_currency_id_readonly_logic");

    private String columnname;

    Parameters(String columnname) {
      this.columnname = columnname;
    }

    public String getColumnName() {
      return this.columnname;
    }

    static Parameters getParameter(String strColumnName) {
      for (Parameters parameter : Parameters.values()) {
        if (strColumnName.equals(parameter.getColumnName())) {
          return parameter;
        }
      }
      return null;
    }
  }

}
