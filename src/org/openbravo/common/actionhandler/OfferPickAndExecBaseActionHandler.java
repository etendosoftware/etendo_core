/*
 ************************************************************************************
 * Copyright (C) 2019 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.common.actionhandler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;
import org.openbravo.service.db.DbUtility;

public abstract class OfferPickAndExecBaseActionHandler extends BaseProcessActionHandler {

  private static final Logger log = Logger.getLogger(OfferPickAndExecBaseActionHandler.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    OBContext.setAdminMode(true);
    JSONObject errorMessage = new JSONObject();
    try {
      jsonRequest = new JSONObject(content);
      JSONArray selectedLines = jsonRequest.getJSONObject("_params").getJSONObject(getJSONName())
          .getJSONArray("_selection");
      if (selectedLines.length() == 0) {
        errorMessage.put("severity", "error");
        errorMessage.put("title", OBMessageUtils.messageBD("NotSelected"));
        jsonRequest.put("message", errorMessage);
        return jsonRequest;
      }

      final String offerId = jsonRequest.getString("inpmOfferId");
      final PriceAdjustment offer = (PriceAdjustment) OBDal.getInstance().getProxy(
          PriceAdjustment.ENTITY_NAME, offerId);
      final Client offerClient = (Client) OBDal.getInstance().getProxy(Client.ENTITY_NAME,
          jsonRequest.getString("inpadClientId"));
      final Organization offerOrg = (Organization) OBDal.getInstance().getProxy(
          Organization.ENTITY_NAME, jsonRequest.getString("inpadOrgId"));

      doPickAndExecute(offerId, offer, offerClient, offerOrg, selectedLines);

      errorMessage.put("severity", "success");
      errorMessage.put("title", OBMessageUtils.messageBD("Success"));
      jsonRequest.put("message", errorMessage);
    } catch (Exception e) {
      log.error("Error in RelateProductsToServiceProduct Action Handler", e);
      OBDal.getInstance().rollbackAndClose();
      try {
        jsonRequest = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonRequest.put("message", errorMessage);
      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonRequest;
  }

  abstract protected String getJSONName();

  abstract protected void doPickAndExecute(String offerId, PriceAdjustment priceAdjustment,
      Client client, Organization org, JSONArray selectedLines) throws JSONException;
}
