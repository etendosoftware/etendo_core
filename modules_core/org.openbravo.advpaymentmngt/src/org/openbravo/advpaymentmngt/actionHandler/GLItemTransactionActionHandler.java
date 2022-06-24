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
 * All portions are Copyright (C) 2014-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.advpaymentmngt.actionHandler;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.utility.APRM_MatchingUtility;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.gl.GLItem;

public class GLItemTransactionActionHandler extends BaseActionHandler {

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    final JSONObject result = new JSONObject();

    try {
      OBContext.setAdminMode(true);
      final JSONObject jsonData = new JSONObject(data);
      String description = jsonData.getString("strDescription");
      final String glitemPrefix = OBMessageUtils.messageBD("APRM_GLItem");
      if (jsonData.isNull("strGLItemId")) {
        description = FIN_Utility.getFinAccTransactionDescription(description, "\n" + glitemPrefix,
            "");
        description = FIN_Utility.getFinAccTransactionDescription(description, glitemPrefix, "");
        result.put("description", description);
      } else {
        final String strGLItemId = jsonData.getString("strGLItemId");
        if (StringUtils.isNotBlank(strGLItemId)) {
          final GLItem glItem = OBDal.getInstance().get(GLItem.class, strGLItemId);
          if (glItem != null) {
            String glItemDescription = glitemPrefix + ": " + glItem.getName();
            description = FIN_Utility.getFinAccTransactionDescription(description, glitemPrefix,
                glItemDescription);
          }
        }
        result.put("description", description);
      }
    } catch (Exception e) {
      try {
        final JSONArray actions = APRM_MatchingUtility.createMessageInProcessView(e.getMessage(),
            "error");
        result.put("responseActions", actions);
        result.put("retryExecution", true);
      } catch (Exception ignore) {
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    return result;
  }
}
