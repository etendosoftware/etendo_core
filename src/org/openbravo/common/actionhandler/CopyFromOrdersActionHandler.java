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

package org.openbravo.common.actionhandler;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.common.actionhandler.copyfromorderprocess.CopyFromOrdersProcess;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.DbUtility;

import javax.servlet.ServletException;

/**
 * Action Handler to manage the Copy From Orders process
 * 
 * @author Mark
 *
 */
public class CopyFromOrdersActionHandler extends BaseProcessActionHandler {

  private static final Logger log = LogManager.getLogger();

  private static final String MESSAGE = "message";
  private static final String MESSAGE_SEVERITY = "severity";
  private static final String MESSAGE_TEXT = "text";
  private static final String MESSAGE_RECORDS_COPIED = "RecordsCopied";
  private static final String MESSAGE_SUCCESS = "success";
  private static final String MESSAGE_ERROR = "error";

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    try {
      // Request Parameters
      jsonRequest = new JSONObject(content);
      final String requestedAction = getRequestedAction(jsonRequest);
      JSONArray selectedOrders = getSelectedOrders(jsonRequest);
      Order processingOrder = getProcessingOrder(jsonRequest);

      if (processingOrder == null)
        throw new OBException(OBMessageUtils.messageBD("OrderNotDefined"));

      if (requestedActionIsDoneAndThereAreSelectedOrders(requestedAction, selectedOrders)) {
        // CopyFromOrdersProcess is instantiated using Weld so it can use Dependency Injection
        CopyFromOrdersProcess copyFromOrdersProcess = WeldUtils
            .getInstanceFromStaticBeanManager(CopyFromOrdersProcess.class);
        int createdOrderLinesCount = copyFromOrdersProcess.copyOrderLines(processingOrder,
            selectedOrders);
        jsonRequest.put(MESSAGE, getSuccessMessage(createdOrderLinesCount));
      }
    } catch (Exception e) {
      log.error("Error in CopyFromOrders Action Handler", e);

      try {
        if (jsonRequest != null) {
          jsonRequest.put(MESSAGE, getErrorMessage(e));
        }
      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
      }
    }
    return jsonRequest;
  }

  private String getRequestedAction(final JSONObject jsonRequest) throws JSONException {
    return jsonRequest.getString(ApplicationConstants.BUTTON_VALUE);
  }

  private JSONArray getSelectedOrders(final JSONObject jsonRequest) throws JSONException {
    return jsonRequest.getJSONObject("_params").getJSONObject("grid").getJSONArray("_selection");
  }

  private Order getProcessingOrder(final JSONObject jsonRequest) throws JSONException {
    return OBDal.getInstance().get(Order.class, jsonRequest.getString("C_Order_ID"));
  }

  private boolean requestedActionIsDoneAndThereAreSelectedOrders(final String requestedAction,
      final JSONArray selectedOrders) {
    return StringUtils.equals(requestedAction, "DONE") && selectedOrders.length() > 0;
  }

  private JSONObject getSuccessMessage(final int recordsCopiedCount) throws JSONException {
    JSONObject errorMessage = new JSONObject();
    errorMessage.put(MESSAGE_SEVERITY, MESSAGE_SUCCESS);
    errorMessage.put(MESSAGE_TEXT, OBMessageUtils.messageBD(MESSAGE_SUCCESS) + "<br/>"
        + OBMessageUtils.messageBD(MESSAGE_RECORDS_COPIED) + recordsCopiedCount);
    return errorMessage;
  }

  private JSONObject getErrorMessage(final Exception e) throws JSONException {
    Throwable ex = DbUtility.getUnderlyingSQLException(e);
    String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
    JSONObject errorMessage = new JSONObject();
    errorMessage.put(MESSAGE_SEVERITY, MESSAGE_ERROR);
    errorMessage.put(MESSAGE_TEXT, message);
    return errorMessage;
  }
}
