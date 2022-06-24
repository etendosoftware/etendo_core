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
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.advpaymentmngt.actionHandler;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.advpaymentmngt.utility.APRM_MatchingUtility;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;

public class MatchStatementActionHandler extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();
  private static final String OK_ACTION = "OK";

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    OBContext.setAdminMode(true);
    try {
      JSONObject jsonRequest = new JSONObject(content);
      final String strFinancialAccount = jsonRequest.getString("Fin_Financial_Account_ID");
      final String action = jsonRequest.getString("_buttonValue");
      if (OK_ACTION.equals(action)) {
        return jsonResponse;
      }
      final FIN_FinancialAccount finAccount = OBDal.getInstance()
          .get(FIN_FinancialAccount.class, strFinancialAccount);
      final FIN_Reconciliation lastReconciliation = TransactionsDao
          .getLastReconciliation(finAccount, "N");
      if (APRM_MatchingUtility.updateReconciliation(lastReconciliation, finAccount, true)) {
        final VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
        final JSONObject msg = new JSONObject();
        msg.put("severity", "success");
        msg.put("title", "");
        msg.put("text", Utility.parseTranslation(new DalConnectionProvider(false), vars,
            vars.getLanguage(), "@Success@"));
        jsonResponse.put("message", msg);
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception handling the match statement", e);

      try {
        jsonResponse = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonResponse.put("message", errorMessage);

      } catch (Exception ignore) {
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonResponse;
  }
}
