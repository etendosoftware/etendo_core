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
 * All portions are Copyright (C) 2012-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.costing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.query.Query;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder.MessageType;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.CallStoredProcedure;

public class ResetStockValuation extends BaseProcessActionHandler {

  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(final Map<String, Object> parameters, final String content) {
    try {
      final JSONObject request = new JSONObject(content);
      final JSONObject params = request.getJSONObject("_params");

      // Do validations on param values
      String strOrgID = null;
      if (params.get("AD_Org_ID") != JSONObject.NULL) {
        strOrgID = (String) params.get("AD_Org_ID");
      }

      return doResetStockValuation(strOrgID);

    } catch (JSONException e) {
      log.error("Error in process", e);
      return new JSONObject();
    }
  }

  public static JSONObject doResetStockValuation(final String strOrgID) {
    deleteOldRecords(strOrgID);
    return createNewResetedRecods(strOrgID);
  }

  private static void deleteOldRecords(final String strOrgID) {
    // delete existing records
    //@formatter:off
    String hql =
            "delete from  M_Stock_Valuation sv" +
            " where sv.client.id = :clientId";
    //@formatter:on
    if (strOrgID != null) {
      //@formatter:off
      hql +=
            "   and sv.organization.id = :orgId";
      //@formatter:on
    }

    @SuppressWarnings("rawtypes")
    final Query delQry = OBDal.getInstance()
        .getSession()
        .createQuery(hql)
        .setParameter("clientId", OBContext.getOBContext().getCurrentClient().getId());
    if (strOrgID != null) {
      delQry.setParameter("orgId", strOrgID);
    }
    delQry.executeUpdate();
  }

  private static JSONObject createNewResetedRecods(final String strOrgID) {
    final List<Object> storedProcedureParams = new ArrayList<>();
    storedProcedureParams.add(OBContext.getOBContext().getCurrentClient().getId());
    storedProcedureParams.add(strOrgID);
    storedProcedureParams.add(null);
    try {
      CallStoredProcedure.getInstance()
          .call("M_INITIALIZE_STOCK_VALUATION", storedProcedureParams, null, false, false);
    } catch (Exception e) {
      return getResponseBuilder()
          .showMsgInProcessView(MessageType.ERROR, OBMessageUtils.messageBD("Error"),
              OBMessageUtils.translateError(e.getMessage()).getMessage())
          .retryExecution()
          .build();
    }

    return getResponseBuilder()
        .showMsgInProcessView(MessageType.SUCCESS, OBMessageUtils.messageBD("Success"),
            OBMessageUtils.messageBD("Success"))
        .retryExecution()
        .build();
  }

}
