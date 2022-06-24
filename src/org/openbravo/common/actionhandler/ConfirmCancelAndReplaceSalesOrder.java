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

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.CancelAndReplaceUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;

public class ConfirmCancelAndReplaceSalesOrder extends BaseProcessActionHandler {

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content)
      throws OBException {
    JSONObject result = new JSONObject();
    try {

      // Get request parameters
      JSONObject request = new JSONObject(content);
      String newOrderId = request.getString("inpcOrderId");

      CancelAndReplaceUtils.cancelAndReplaceOrder(newOrderId, null, false);

      JSONObject resultMessage = new JSONObject();
      resultMessage.put("severity", "success");
      resultMessage.put("title", OBMessageUtils.messageBD("Success"));
      result.put("message", resultMessage);

    } catch (Exception e1) {
      try {
        OBDal.getInstance().rollbackAndClose();
        JSONObject resultMessage = new JSONObject();
        resultMessage.put("severity", "error");
        resultMessage.put("title", OBMessageUtils.messageBD("Error"));
        resultMessage.put("text", OBMessageUtils.translateError(e1.getMessage()).getMessage());
        result.put("message", resultMessage);
      } catch (Exception e2) {
        throw new OBException(e2);
      }
    }
    return result;
  }
}
