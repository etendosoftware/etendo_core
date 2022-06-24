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
 * All portions are Copyright (C) 2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.common.actionhandler;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.CancelAndReplaceUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.DbUtility;

public class CancelAndReplaceSalesOrder extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {

    // Declare json to be returned
    JSONObject result = new JSONObject();
    JSONObject openDirectTab = new JSONObject();
    JSONObject showMsgInProcessView = new JSONObject();
    JSONObject showMsgInView = new JSONObject();
    try {

      // Get request parameters
      JSONObject request = new JSONObject(content);
      String oldOrderId = request.getString("inpcOrderId");
      String tabId = request.getString("inpTabId");

      // Get Order to be replaced and cancelled
      Order oldOrder = OBDal.getInstance().get(Order.class, oldOrderId);

      if (!"SO".equals(oldOrder.getDocumentType().getSOSubType())) {
        throw new OBException("@CancelAndReplaceNotAllowedForWRWP@");
      }

      // Get new Order
      Order newOrder = CancelAndReplaceUtils.createReplacementOrder(oldOrder);

      // Execute process and prepare an array with actions to be executed after execution
      JSONArray actions = new JSONArray();

      // Message in tab from where the process is executed
      showMsgInProcessView.put("msgType", "success");
      showMsgInProcessView.put("msgTitle", OBMessageUtils.messageBD("Success"));
      showMsgInProcessView.put("msgText", OBMessageUtils.messageBD("OrderCreatedInTemporalStatus")
          + " " + newOrder.getDocumentNo());
      showMsgInProcessView.put("wait", true);

      JSONObject showMsgInProcessViewAction = new JSONObject();
      showMsgInProcessViewAction.put("showMsgInProcessView", showMsgInProcessView);

      actions.put(showMsgInProcessViewAction);

      // New record info
      openDirectTab.put("tabId", tabId);
      openDirectTab.put("recordId", newOrder.getId());
      openDirectTab.put("wait", true);

      JSONObject openDirectTabAction = new JSONObject();
      openDirectTabAction.put("openDirectTab", openDirectTab);

      actions.put(openDirectTabAction);

      // Message of the new opened tab
      showMsgInView.put("msgType", "success");
      showMsgInView.put("msgTitle", OBMessageUtils.messageBD("Success"));
      showMsgInView.put("msgText", OBMessageUtils.messageBD("OrderInTemporalStatus"));

      JSONObject showMsgInViewAction = new JSONObject();
      showMsgInViewAction.put("showMsgInView", showMsgInView);

      actions.put(showMsgInViewAction);

      result.put("responseActions", actions);

    } catch (Exception e) {
      log.error("Error in process", e);
      try {
        OBDal.getInstance().getConnection().rollback();
        result = new JSONObject();
        JSONObject errorMessage = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        errorMessage.put("severity", "error");
        errorMessage.put("title", OBMessageUtils.messageBD("Error"));
        errorMessage.put("text", message);
        result.put("message", errorMessage);
      } catch (Exception ignore) {
      }
    }
    return result;
  }
}
