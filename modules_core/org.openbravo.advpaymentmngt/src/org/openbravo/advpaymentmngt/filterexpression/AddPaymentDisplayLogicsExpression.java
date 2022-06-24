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

public class AddPaymentDisplayLogicsExpression implements FilterExpression {
  private static final Logger log = LogManager.getLogger();
  @Inject
  @Any
  private Instance<AddPaymentDisplayLogicsHandler> addPaymentFilterExpressionHandlers;

  @Override
  public String getExpression(Map<String, String> requestMap) {
    String strCurrentParam = "";
    try {
      OBContext.setAdminMode(true);
      final String strWindowId = getWindowId(requestMap);

      AddPaymentDisplayLogicsHandler handler = getHandler(strWindowId);
      if (handler == null) {
        throw new OBException(String.format(OBMessageUtils.messageBD("APRM_NOHANDLER")));
      }
      strCurrentParam = requestMap.get("currentParam");
      Parameters param = Parameters.getParameter(strCurrentParam);
      try {
        switch (param) {
          case Organization:
            return handler.getOrganizationDisplayLogic(requestMap) ? "Y" : "N";
          case Document:
            return handler.getDocumentDisplayLogic(requestMap) ? "Y" : "N";
          case CreditToUse:
            return handler.getCreditToUseDisplayLogic(requestMap) ? "Y" : "N";
          case OverpaymentAction:
            return handler.getOverpaymentActionDisplayLogic(requestMap) ? "Y" : "N";
          case BankStatementLine:
            return handler.getBankStatementLineDisplayLogic(requestMap) ? "Y" : "N";
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

  private AddPaymentDisplayLogicsHandler getHandler(String strWindowId) {
    AddPaymentDisplayLogicsHandler handler = null;
    for (AddPaymentDisplayLogicsHandler nextHandler : addPaymentFilterExpressionHandlers
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
    Organization("ad_org_id_display_logic"),
    Document("trxtype_display_logic"),
    CreditToUse("credit_to_use_display_logic"),
    OverpaymentAction("overpayment_action_display_logic"),
    BankStatementLine("bslamount_display_logic");

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
