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
 * All portions are Copyright (C) 2014-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.advpaymentmngt.actionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.advpaymentmngt.utility.APRM_MatchingUtility;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.service.db.DbUtility;

public class FindTransactionsToMatchActionHandler extends BaseActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject result = new JSONObject();

    try {
      OBContext.setAdminMode(true);
      final JSONObject jsonData = new JSONObject(data);
      final JSONObject params = jsonData.getJSONObject("_params");
      final JSONArray selection = params.getJSONObject("findtransactiontomatch")
          .getJSONArray("_selection");

      if (selection.length() > 0) {
        final String strBankLineId = params.getString("bankStatementLineId");
        final List<String> selectedTransactionIds = new ArrayList<String>();
        for (int i = 0; i < selection.length(); i++) {
          selectedTransactionIds.add(selection.getJSONObject(i).getString("id"));
        }

        final FIN_FinancialAccount account = OBDal.getInstance()
            .get(FIN_FinancialAccount.class, jsonData.getString("inpfinFinancialAccountId"));
        final FIN_Reconciliation reconciliation = TransactionsDao.getLastReconciliation(account,
            "N");
        final FIN_BankStatementLine bankStatementLine = OBDal.getInstance()
            .get(FIN_BankStatementLine.class, strBankLineId);
        APRM_MatchingUtility.matchBankStatementLine(bankStatementLine, selectedTransactionIds,
            reconciliation, null, true);

      } else {
        final JSONArray actions = APRM_MatchingUtility
            .createMessageInProcessView("@APRM_SELECT_RECORD_ERROR@", "error");
        result.put("responseActions", actions);
        result.put("retryExecution", true);
      }

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Error creating a match using find button", e);
      try {
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        final JSONArray actions = APRM_MatchingUtility.createMessageInProcessView(ex.getMessage(),
            "error");
        result.put("responseActions", actions);
        result.put("retryExecution", true);
      } catch (Exception e2) {
        log.error("Error message could not be built", e2);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }
}
