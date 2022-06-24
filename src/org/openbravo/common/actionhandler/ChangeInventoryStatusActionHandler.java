/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
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
 *************************************************************************
 */

package org.openbravo.common.actionhandler;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder.MessageType;
import org.openbravo.erpCommon.utility.InventoryStatusUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;

public class ChangeInventoryStatusActionHandler extends BaseProcessActionHandler {

  private static final Logger log4j = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    try {
      final JSONObject request = new JSONObject(content);

      String mLocatorID = request.getString("M_Locator_ID");
      JSONObject params = request.getJSONObject("_params");
      String inventoryStatusID = params.getString("M_InventoryStatus_ID");

      InventoryStatusUtils.changeStatusOfStorageBin(mLocatorID, inventoryStatusID);
      return getResponseBuilder()
          .showMsgInView(MessageType.SUCCESS, OBMessageUtils.messageBD("Success"),
              OBMessageUtils.messageBD("Success"))
          .build();

    } catch (Exception e) {
      log4j.error(e.getMessage(), e);
      if (StringUtils.startsWith(e.getMessage(), "WARNING")) {
        return getResponseBuilder()
            .showMsgInView(MessageType.WARNING, OBMessageUtils.messageBD("Warning"),
                StringUtils.replaceOnce(e.getMessage(), "WARNING", ""))
            .build();
      }
      return getResponseBuilder()
          .showMsgInView(MessageType.ERROR, OBMessageUtils.messageBD("Error"), e.getMessage())
          .build();
    }
  }
}
