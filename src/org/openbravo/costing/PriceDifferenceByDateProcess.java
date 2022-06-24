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
 * All portions are Copyright (C) 2014-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.costing;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.json.JsonUtils;

public class PriceDifferenceByDateProcess extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    String message = new String();
    OBContext.setAdminMode(true);
    try {
      jsonRequest = new JSONObject(content);
      JSONObject params = jsonRequest.getJSONObject("_params");
      log.debug("{}", jsonRequest);
      JSONArray productIds = params.getJSONArray("M_Product_ID");
      String mvdate = params.getString("movementdate");
      String orgId = params.getString("ad_org_id");
      productIds.toString();
      Date movementdate = JsonUtils.createDateFormat().parse(mvdate);
      int transactionsProcessed = 0;

      List<Organization> legalOrganizations = PriceDifferenceUtil.getLegalOrganizationList(orgId);
      List<String> selectedProductsId = getProductsIdListFromProductsParameter(productIds);
      for (Organization legalOrganization : legalOrganizations) {
        doChecks(legalOrganization.getId(), movementdate);
        PriceDifferenceUtil.setTransactionsReadyForPriceAdjustment(selectedProductsId, movementdate,
            legalOrganization);
        JSONObject msg = new JSONObject();
        msg = PriceDifferenceProcess.processPriceDifference(legalOrganization);
        transactionsProcessed += msg.getInt("transactionsProcessed");
      }
      JSONObject msg = getResutlMessage(transactionsProcessed);
      jsonRequest.put("message", msg);
      jsonRequest.put("retryExecution", true);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Error Process Price Correction", e);

      try {
        jsonRequest = new JSONObject();
        jsonRequest.put("retryExecution", true);
        if (message.isEmpty()) {
          Throwable ex = DbUtility.getUnderlyingSQLException(e);
          message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        }
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonRequest.put("message", errorMessage);
        return jsonRequest;
      } catch (Exception ignore) {
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonRequest;
  }

  private JSONObject getResutlMessage(int transactionsProcessed) throws JSONException {
    String messageText = OBMessageUtils.messageBD("PriceDifferenceChecked");
    Map<String, String> map = new HashMap<String, String>();
    map.put("trxsNumber", Integer.toString(transactionsProcessed));

    JSONObject message = new JSONObject();
    message = new JSONObject();
    message.put("severity", "success");
    message.put("title", OBMessageUtils.messageBD("Success"));
    message.put("text", OBMessageUtils.parseTranslation(messageText, map));
    return message;
  }

  private List<String> getProductsIdListFromProductsParameter(JSONArray productIds)
      throws JSONException {
    List<String> productsIdList = new ArrayList<>();
    for (int i = 0; i < productIds.length(); i++) {
      productsIdList.add(productIds.getString(i));
    }
    return productsIdList;
  }

  private void doChecks(String orgId, Date movementdate) {
    try {
      Organization org = OBDal.getInstance().get(Organization.class, orgId);
      Date maxDate = CostingUtils.getMaxTransactionDate(org);
      Period periodClosed = CostingUtils.periodClosed(org, movementdate, maxDate, "CAD");
      if (periodClosed != null) {
        String errorMsg = OBMessageUtils.getI18NMessage("DocumentTypePeriodClosed",
            new String[] { "CAD", periodClosed.getIdentifier() });
        throw new OBException(errorMsg);
      }
    } catch (ServletException e) {
      throw new OBException(e.getMessage());
    }
  }
}
