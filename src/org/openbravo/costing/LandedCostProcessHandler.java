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
 * All portions are Copyright (C) 2014-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.costing;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.cost.LandedCost;
import org.openbravo.service.db.DbUtility;

public class LandedCostProcessHandler extends BaseActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    try {
      JSONObject jsonContent = new JSONObject(content);
      final String strLandedCostId = jsonContent.getString("M_Landedcost_ID");
      LandedCost landedCost = OBDal.getInstance().get(LandedCost.class, strLandedCostId);
      // lock Landed Cost
      if (landedCost.isProcessNow()) {
        throw new OBException(OBMessageUtils.parseTranslation("@OtherProcessActive@"));
      }

      landedCost.setProcessNow(true);
      if (SessionHandler.isSessionHandlerPresent()) {
        SessionHandler.getInstance().commitAndStart();
      }
      JSONObject message = LandedCostProcess.doProcessLandedCost(landedCost);
      landedCost = OBDal.getInstance().get(LandedCost.class, strLandedCostId);
      landedCost.setProcessNow(false);

      jsonResponse.put("message", message);

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Error in LandedCostProcessHandler: " + e.getMessage(), e);
      try {
        JSONObject jsonContent = new JSONObject(content);
        final String strLandedCostId = jsonContent.getString("M_Landedcost_ID");
        final LandedCost landedCost = OBDal.getInstance().get(LandedCost.class, strLandedCostId);
        landedCost.setProcessNow(false);

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
