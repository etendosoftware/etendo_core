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
 * All portions are Copyright (C) 2014-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.costing;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.cost.LCReceiptLineAmt;
import org.openbravo.model.materialmgmt.cost.LandedCostCost;
import org.openbravo.service.db.DbUtility;

public class LCMatchingCancelHandler extends BaseActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    try {
      JSONObject jsonContent = new JSONObject(content);
      JSONObject message = new JSONObject();

      final String strLCCostId = jsonContent.getString("M_LC_Cost_ID");
      LandedCostCost lcCost = OBDal.getInstance().get(LandedCostCost.class, strLCCostId);
      doChecks(lcCost);
      message = doCancelMatchingLandedCost(lcCost.getId());
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

  public static JSONObject doCancelMatchingLandedCost(String strLcCostId) throws JSONException {

    LandedCostCost lcCost = OBDal.getInstance().get(LandedCostCost.class, strLcCostId);
    lcCost.setProcessed(false);
    OBDal.getInstance().save(lcCost);
    OBDal.getInstance().flush();
    JSONObject message = new JSONObject();
    if (lcCost.getMatchingCostAdjustment() != null) {
      message = CancelCostAdjustment.doCancelCostAdjustment(lcCost.getMatchingCostAdjustment());
    } else {
      message.put("severity", "success");
      message.put("title", OBMessageUtils.messageBD("Success"));
    }
    lcCost = OBDal.getInstance().get(LandedCostCost.class, strLcCostId);
    doDeleteReceiptLineAmtMatched(lcCost);

    // Reload in case the cancel cost adjustment has cleared the session.
    lcCost = OBDal.getInstance().get(LandedCostCost.class, strLcCostId);
    lcCost.setMatchingCostAdjustment(null);
    lcCost.setMatched(false);
    lcCost.setMatchingAmount(null);
    OBDal.getInstance().save(lcCost);
    OBDal.getInstance().flush();

    return message;
  }

  private void doChecks(LandedCostCost lcCost) {
    if ("Y".equals(lcCost.getPosted())) {
      String errorMsg = OBMessageUtils.messageBD("DocumentPosted");
      log.error("Document Posted");
      throw new OBException(errorMsg);
    }
  }

  public static void doDeleteReceiptLineAmtMatched(LandedCostCost lcCost) {

    try {
      int i = 1;
      List<String> idList = OBDao.getIDListFromOBObject(lcCost.getLandedCostReceiptLineAmtList());

      for (String id : idList) {
        LCReceiptLineAmt lcrla = OBDal.getInstance().get(LCReceiptLineAmt.class, id);
        if (lcrla.isMatchingAdjustment()) {
          i++;
          lcCost.getLandedCostReceiptLineAmtList().remove(lcrla);
          OBDal.getInstance().remove(lcrla);
        }
        if (i % 100 == 0) {
          OBDal.getInstance().save(lcCost);
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
      }

      OBDal.getInstance().save(lcCost);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Error in DeleteReceiptLineAmtMatched: " + e.getMessage(), e);
      throw new OBException(e.getMessage());
    }
  }
}
