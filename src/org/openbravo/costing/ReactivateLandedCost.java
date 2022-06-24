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
 * All portions are Copyright (C) 2014-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.costing;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.cost.LCDistributionAlgorithm;
import org.openbravo.model.materialmgmt.cost.LandedCost;
import org.openbravo.model.materialmgmt.cost.LandedCostCost;
import org.openbravo.service.db.DbUtility;

public class ReactivateLandedCost extends BaseActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject result = new JSONObject();
    JSONObject errorMessage = new JSONObject();
    try {
      final JSONObject jsonData = new JSONObject(data);
      String lcId = jsonData.getString("inpmLandedcostId");
      LandedCost landedCost = OBDal.getInstance().get(LandedCost.class, lcId);
      doChecks(landedCost);
      landedCost.setProcessNow(true);
      if (SessionHandler.isSessionHandlerPresent()) {
        SessionHandler.getInstance().commitAndStart();
      }
      JSONObject message = doReactivateLandedCost(landedCost);
      landedCost = OBDal.getInstance().get(LandedCost.class, lcId);
      landedCost.setProcessNow(false);
      result.put("message", message);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(e.getMessage(), e);
      try {
        JSONObject jsonContent = new JSONObject(data);
        final String strLandedCostId = jsonContent.getString("inpmLandedcostId");
        final LandedCost landedCost = OBDal.getInstance().get(LandedCost.class, strLandedCostId);
        landedCost.setProcessNow(false);

        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("title", OBMessageUtils.messageBD("Error"));
        errorMessage.put("text", message);
        result.put("message", errorMessage);
      } catch (JSONException ignore) {
      }
    }
    return result;
  }

  public static JSONObject doReactivateLandedCost(LandedCost landedCost)
      throws OBException, JSONException {
    LandedCost localLandedCost = landedCost;
    String strLCostId = localLandedCost.getId();
    JSONObject message = null;

    if (StringUtils.equals(localLandedCost.getDocumentStatus(), "DR")
        || !localLandedCost.isProcessed()) {
      message = new JSONObject();
      message.put("severity", "error");
      message.put("title", OBMessageUtils.messageBD("DocumentProcessed"));
      return message;
    }
    // Cancel cost adjustment only if exists
    if (localLandedCost.getCostAdjustment() != null) {
      message = CancelCostAdjustment.doCancelCostAdjustment(localLandedCost.getCostAdjustment());
      if (!"success".equals(message.get("severity"))) {
        return message;
      }
    } else {
      message = new JSONObject();
      message.put("severity", "success");
      message.put("title", OBMessageUtils.messageBD("Success"));
    }

    String strPartialMessage = "";
    // Reload in case the cancel cost adjustment has cleared the session.
    localLandedCost = OBDal.getInstance().get(LandedCost.class, strLCostId);
    localLandedCost.setProcessed(Boolean.FALSE);
    OBDal.getInstance().save(localLandedCost);
    OBDal.getInstance().flush();
    for (LandedCostCost lcc : localLandedCost.getLandedCostCostList()) {
      if (lcc.isMatched()) {
        message = LCMatchingCancelHandler.doCancelMatchingLandedCost(lcc.getId());
      }
      if (message.has("severity") && !message.get("severity").equals("success")) {
        return message;
      } else {
        if (message.has("text")) {
          strPartialMessage = strPartialMessage + "- " + message.get("text");
        }
      }
      lcc = OBDal.getInstance().get(LandedCostCost.class, lcc.getId());
      LandedCostDistributionAlgorithm lcDistAlg = getDistributionAlgorithm(
          lcc.getLandedCostDistributionAlgorithm());

      message = lcDistAlg.cancelDistributeAmount(lcc);
      if (message.has("severity") && !message.get("severity").equals("success")) {
        return message;
      }

      // Remove related Matched Amount records
      lcc.getLandedCostMatchedList().clear();
    }

    // Reload in case the cancel cost adjustment has cleared the session.
    localLandedCost = OBDal.getInstance().get(LandedCost.class, strLCostId);
    localLandedCost.setDocumentStatus("DR");
    localLandedCost.setCostAdjustment(null);
    OBDal.getInstance().save(localLandedCost);

    message.put("title", OBMessageUtils.messageBD("Success"));
    message.put("text", strPartialMessage);
    return message;
  }

  private static LandedCostDistributionAlgorithm getDistributionAlgorithm(
      LCDistributionAlgorithm lcDistAlg) {
    OBContext.setAdminMode(true);
    LandedCostDistributionAlgorithm lcDistAlgInstance;
    try {
      Class<?> clz = null;
      clz = OBClassLoader.getInstance().loadClass(lcDistAlg.getJavaClassName());
      lcDistAlgInstance = (LandedCostDistributionAlgorithm) WeldUtils
          .getInstanceFromStaticBeanManager(clz);
    } catch (Exception e) {
      log.error("Error loading distribution algorithm: " + lcDistAlg.getJavaClassName(), e);
      String strError = OBMessageUtils.messageBD("LCDistributionAlgorithmNotFound");
      Map<String, String> map = new HashMap<String, String>();
      map.put("distalg", lcDistAlg.getIdentifier());
      throw new OBException(OBMessageUtils.parseTranslation(strError, map));
    } finally {
      OBContext.restorePreviousMode();
    }
    return lcDistAlgInstance;
  }

  private void doChecks(LandedCost landedCost) {
    if ("Y".equals(landedCost.getPosted())) {
      String errorMsg = OBMessageUtils.messageBD("DocumentPosted");
      log.error("Document Posted");
      throw new OBException(errorMsg);
    }
    // lock Landed Cost
    if (landedCost.isProcessNow()) {
      throw new OBException(OBMessageUtils.parseTranslation("@OtherProcessActive@"));
    }
    for (LandedCostCost lcc : landedCost.getLandedCostCostList()) {
      if ("Y".equals(lcc.getPosted())) {
        String errorMsg = OBMessageUtils.messageBD("DocumentPosted");
        log.error("Document Posted");
        throw new OBException(
            errorMsg + ": " + OBMessageUtils.messageBD("COST_LINE") + lcc.getLineNo());
      }
    }
  }
}
