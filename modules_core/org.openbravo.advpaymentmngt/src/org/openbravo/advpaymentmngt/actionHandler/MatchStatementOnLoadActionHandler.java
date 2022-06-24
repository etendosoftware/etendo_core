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
 * All portions are Copyright (C) 2014-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.advpaymentmngt.actionHandler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollableResults;
import org.openbravo.advpaymentmngt.dao.MatchTransactionDao;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.advpaymentmngt.utility.APRM_MatchingUtility;
import org.openbravo.advpaymentmngt.utility.FIN_MatchedTransaction;
import org.openbravo.advpaymentmngt.utility.FIN_MatchingTransaction;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.model.financialmgmt.payment.MatchingAlgorithm;
import org.openbravo.service.db.DbUtility;

public class MatchStatementOnLoadActionHandler extends BaseActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    JSONArray actions = new JSONArray();

    String strReconciliationId = null;
    try {
      OBContext.setAdminMode(true);

      final JSONObject context = new JSONObject((String) parameters.get("context"));
      final String strFinancialAccountId = context.getString("Fin_Financial_Account_ID");
      boolean executeAutoMatchingAlgm = "true".equals(parameters.get("executeMatching")) ? true
          : false;

      final FIN_FinancialAccount financialAccount = OBDal.getInstance()
          .get(FIN_FinancialAccount.class, strFinancialAccountId);

      /* Get the right reconciliation */
      FIN_Reconciliation reconciliation = TransactionsDao.getLastReconciliation(
          OBDal.getInstance().get(FIN_FinancialAccount.class, strFinancialAccountId), "N");
      if (reconciliation == null) {
        // Create a new reconciliation
        reconciliation = APRM_MatchingUtility.addNewDraftReconciliation(financialAccount);
      } else {
        APRM_MatchingUtility.fixMixedLines(reconciliation);
        APRM_MatchingUtility.updateReconciliation(reconciliation, financialAccount, false);
      }
      strReconciliationId = reconciliation.getId();

      if (executeAutoMatchingAlgm) {
        /* Verify we have something left to match */
        if (MatchTransactionDao.getUnMatchedBankStatementLines(financialAccount).isEmpty()) {
          actions = APRM_MatchingUtility.createMessageInProcessView("@APRM_NoStatementsToMatch@",
              "warning");
          jsonResponse.put("responseActions", actions);
          return jsonResponse;
        }

        /* Run the automatic matching algorithm */
        int matchedLines = runAutoMatchingAlgorithm(strReconciliationId, strFinancialAccountId,
            financialAccount, reconciliation);
        if (matchedLines > 0) {
          actions = APRM_MatchingUtility.createMessageInProcessView("@APRM_AutomaticMatchedLines@",
              "success", matchedLines);
        } else {
          actions = APRM_MatchingUtility
              .createMessageInProcessView("@APRM_NoAutomaticMatchedLines@", "warning");
        }
        jsonResponse.put("responseActions", actions);
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception loading match statement", e);

      try {
        jsonResponse = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        actions = APRM_MatchingUtility.createMessageInProcessView(ex.getMessage(), "error");
        jsonResponse.put("responseActions", actions);
      } catch (Exception ignore) {
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    return jsonResponse;
  }

  private int runAutoMatchingAlgorithm(String strReconciliationId,
      final String strFinancialAccountId, final FIN_FinancialAccount financialAccount,
      FIN_Reconciliation reconciliation) throws InterruptedException, SQLException {
    final MatchingAlgorithm ma = financialAccount.getMatchingAlgorithm();
    final FIN_MatchingTransaction matchingTransaction = new FIN_MatchingTransaction(
        ma.getJavaClassName());
    final ScrollableResults bankLinesSR = APRM_MatchingUtility
        .getPendingToBeMatchedBankStatementLines(strFinancialAccountId, strReconciliationId);
    final List<FIN_FinaccTransaction> excluded = new ArrayList<FIN_FinaccTransaction>();
    int matchedLines = 0, i = 0;
    try {
      List<String> bankLines = new ArrayList<String>();
      while (bankLinesSR.next()) {
        final FIN_BankStatementLine bankStatementLine = (FIN_BankStatementLine) bankLinesSR.get(0);
        bankLines.add(bankStatementLine.getId());
        if ((i % 100) == 0) {
          OBDal.getInstance().getSession().clear();
        }
        i++;
      }
      bankLinesSR.close();
      i = 0;
      for (i = 0; i < bankLines.size(); i++) {
        final FIN_BankStatementLine bankStatementLine = OBDal.getInstance()
            .get(FIN_BankStatementLine.class, bankLines.get(i));
        FIN_MatchedTransaction matched;
        // try to match if exception is thrown continue
        try {
          matched = matchingTransaction.match(bankStatementLine, excluded);
        } catch (Exception e) {
          matched = new FIN_MatchedTransaction(null, FIN_MatchedTransaction.NOMATCH);
        }

        FIN_FinaccTransaction transaction = matched.getTransaction();
        if (transaction != null && APRM_MatchingUtility.matchBankStatementLine(bankStatementLine,
            transaction, reconciliation, matched.getMatchLevel(), false)) {
          excluded.add(transaction);
          matchedLines++;
          // Required to persist current matching so that it is not rollbacked afterwards because
          // of
          // a
          // future error
          OBDal.getInstance().getConnection().commit();
        }

        if ((i % 100) == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
    }

    return matchedLines;
  }
}
