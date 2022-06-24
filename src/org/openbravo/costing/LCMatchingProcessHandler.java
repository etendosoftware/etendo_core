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
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.costing;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.cost.LandedCostCost;
import org.openbravo.service.db.DbUtility;

public class LCMatchingProcessHandler extends BaseActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    try {
      JSONObject jsonContent = new JSONObject(content);
      JSONObject params = jsonContent.getJSONObject("_params");
      final String strLCCostId = jsonContent.getString("M_LC_Cost_ID");
      final LandedCostCost lcCost = OBDal.getInstance().get(LandedCostCost.class, strLCCostId);
      final boolean isMatchingAdjusted = params.getBoolean("IsMatchingAdjusted");
      lcCost.setMatchingAdjusted(isMatchingAdjusted);
      OBDal.getInstance().save(lcCost);

      JSONObject message = LCMatchingProcess.doProcessLCMatching(lcCost);
      jsonResponse.put("message", message);

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Error in LCMatchingProcessHandler: " + e.getMessage(), e);
      try {
        JSONObject message = new JSONObject();
        message.put("severity", "error");
        message.put("title", OBMessageUtils.messageBD("Error"));
        message.put("text", e.getMessage());
        jsonResponse.put("message", message);
      } catch (JSONException ignore) {
      }
    } catch (JSONException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Error parsing JSONObject: " + e.getMessage(), e);
      try {
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("title", OBMessageUtils.messageBD("Error"));
        errorMessage.put("text", e.getMessage());
        jsonResponse.put("message", errorMessage);
      } catch (JSONException ignore) {
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Error in LCMatchingProcessHandler: " + e.getMessage(), e);
      try {
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String strMessage = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("title", OBMessageUtils.messageBD("Error"));
        errorMessage.put("text", strMessage);
        jsonResponse.put("message", errorMessage);
      } catch (Exception ignore) {
      }
    }
    return jsonResponse;
  }
}
