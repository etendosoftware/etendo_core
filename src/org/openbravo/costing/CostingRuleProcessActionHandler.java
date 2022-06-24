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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;

public class CostingRuleProcessActionHandler extends BaseProcessActionHandler {
  private static final Logger log4j = LogManager.getLogger();
  private static final String costingRuleTabId = "6868B706DA8340158DE353A6C252A564";

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    JSONObject msg = new JSONObject();
    final VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    ConnectionProvider conn = new DalConnectionProvider(true);
    try {
      OBContext.setAdminMode(true);
      JSONObject jsonRequest = new JSONObject(content);
      final String ruleId = (String) jsonRequest.get("M_Costing_Rule_ID");
      String processId = (String) parameters.get("processId");

      // Intitilize the Process Bundle
      ProcessBundle bundle = new ProcessBundle(processId, vars).init(conn);
      HashMap<String, Object> params = new HashMap<String, Object>();
      params.put("M_Costing_Rule_ID", ruleId);
      params.put("adOrgId", vars.getStringParameter("inpadOrgId"));
      params.put("adClientId", vars.getStringParameter("inpadClientId"));
      params.put("tabId", costingRuleTabId);
      bundle.setParams(params);

      // Initialize CostingRuleProcess
      CostingRuleProcess process = new CostingRuleProcess();
      process.execute(bundle);
      OBError result = (OBError) bundle.getResult();
      if (result.getType().equalsIgnoreCase("error")) {
        msg.put("severity", "error");
      } else if (result.getType().equalsIgnoreCase("success")) {
        msg.put("severity", "success");
      }
      msg.put("title", result.getTitle());
      msg.put("text", Utility.parseTranslation(new DalConnectionProvider(false), vars,
          vars.getLanguage(), result.getMessage()));
      jsonResponse.put("message", msg);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      Throwable ex = DbUtility.getUnderlyingSQLException(e);
      String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
      log4j.error(message, e);
      try {
        msg = new JSONObject();
        msg.put("severity", "error");
        msg.put("text", message);
        msg.put("title", OBMessageUtils.messageBD("Error"));
        jsonResponse.put("message", msg);
      } catch (Exception ignore) {
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonResponse;
  }
}
