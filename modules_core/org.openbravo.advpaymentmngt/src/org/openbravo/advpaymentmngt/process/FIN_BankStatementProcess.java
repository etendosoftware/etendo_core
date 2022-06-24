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
 * All portions are Copyright (C) 2018-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.process;

import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.dao.MatchTransactionDao;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatement;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.scheduling.ProcessBundle;

public class FIN_BankStatementProcess implements org.openbravo.scheduling.Process {

  private static final Logger log = LogManager.getLogger();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(
        Utility.messageBD(bundle.getConnection(), "Success", bundle.getContext().getLanguage()));

    try {
      // retrieve custom params
      final String strAction = (String) bundle.getParams().get("action");

      // retrieve standard params
      final String recordID = (String) bundle.getParams().get("FIN_Bankstatement_ID");
      final FIN_BankStatement bankStatement = OBDal.getInstance()
          .get(FIN_BankStatement.class, recordID);
      final VariablesSecureApp vars = bundle.getContext().toVars();
      final ConnectionProvider conProvider = bundle.getConnection();
      final String language = bundle.getContext().getLanguage();
      final boolean isForceProcess = "2DDE7D3618034C38A4462B7F3456C28D"
          .equals(bundle.getProcessId());

      bankStatement.setProcessNow(true);
      OBDal.getInstance().save(bankStatement);
      OBDal.getInstance().flush();

      if ("P".equals(strAction)) {
        // ***********************
        // Process Bank Statement
        // ***********************

        // Check all dates are after last reconciliation date or last transaction date of previous
        // bank statements. Skip when force
        Date maxBSLDate = getMaxBSLDate(bankStatement);
        if (maxBSLDate != null && !isForceProcess) {
          for (FIN_BankStatementLine bsl : bankStatement.getFINBankStatementLineList()) {
            if (bsl.getTransactionDate().compareTo(maxBSLDate) <= 0) {
              if (!msg.getMessage().equals("")) {
                msg.setMessage(msg.getMessage() + ", " + bsl.getLineNo());
              } else {
                msg.setType("Warning");
                msg.setTitle(FIN_Utility.messageBD("Warning"));
                String pattern = OBPropertiesProvider.getInstance()
                    .getOpenbravoProperties()
                    .getProperty("dateFormat.java");
                msg.setMessage(msg.getMessage()
                    + FIN_Utility.messageBD("APRM_BankStatementLineWrongDateWarning")
                    + Utility.formatDate(maxBSLDate, pattern) + ". "
                    + FIN_Utility.messageBD("APRM_BankStatementLineWrongDateWarning2") + " "
                    + bsl.getLineNo());
              }
            }
          }
        }
        if (msg.getType() != null && !msg.getType().equalsIgnoreCase("warning")) {
          // Success
          bankStatement.setProcessed(true);
          bankStatement.setAPRMProcessBankStatement("R");
          bankStatement.setAPRMProcessBankStatementForce("R");
          OBDal.getInstance().save(bankStatement);
          OBDal.getInstance().flush();
        }
        if (isForceProcess) {
          // Update affected Reconciliations
          updateAffectedReconciliations(bankStatement);
        }
      } else if (strAction.equals("R")) {
        // *************************
        // Reactivate Bank Statement
        // *************************
        // Already Posted Document
        if ("Y".equals(bankStatement.getPosted())) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(conProvider, "Error", language));
          msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
              "@PostedDocument@" + ": " + bankStatement.getIdentifier()));
          bundle.setResult(msg);
          return;
        }
        if (!isForceProcess) {
          // Already Reconciled
          for (FIN_BankStatementLine bsl : bankStatement.getFINBankStatementLineList()) {
            if (bsl.getFinancialAccountTransaction() != null) {
              msg.setType("Error");
              msg.setTitle(Utility.messageBD(conProvider, "Error", language));
              msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
                  "@APRM_BSLineReconciled@" + ": " + bsl.getLineNo().toString()));
              bundle.setResult(msg);
              return;
            }
          }
        }

        bankStatement.setProcessed(false);
        bankStatement.setAPRMProcessBankStatement("P");
        bankStatement.setAPRMProcessBankStatementForce("P");
        OBDal.getInstance().save(bankStatement);
        OBDal.getInstance().flush();
      }

      bankStatement.setProcessNow(false);
      OBDal.getInstance().save(bankStatement);
      OBDal.getInstance().flush();
      bundle.setResult(msg);

    } catch (Exception e) {
      log.error("Error while executing FIN_BankStatementProcess", e);
      msg.setType("Error");
      msg.setTitle(
          Utility.messageBD(bundle.getConnection(), "Error", bundle.getContext().getLanguage()));
      msg.setMessage(FIN_Utility.getExceptionMessage(e));
      bundle.setResult(msg);
      OBDal.getInstance().rollbackAndClose();
    }

  }

  private Date getMaxBSLDate(FIN_BankStatement bankstatement) {
    // Get last transaction date from previous bank statements
    //@formatter:off
    final String hql =
                  "as bsl " +
                  " where bsl.bankStatement.account.id = :accountId" +
                  "   and bsl.bankStatement.id <> :bankStatementId" +
                  "   and bsl.bankStatement.processed = 'Y'" +
                  " order by bsl.transactionDate desc";
    //@formatter:on

    FIN_BankStatementLine line = OBDal.getInstance()
        .createQuery(FIN_BankStatementLine.class, hql)
        .setNamedParameter("accountId", bankstatement.getAccount().getId())
        .setNamedParameter("bankStatementId", bankstatement.getId())
        .setMaxResult(1)
        .uniqueResult();

    if (line != null) {
      return line.getTransactionDate();
    }

    // If no previous bank statement is found get the ending date of the last reconciliation
    FIN_Reconciliation rec = org.openbravo.advpaymentmngt.dao.TransactionsDao
        .getLastReconciliation(bankstatement.getAccount(), "Y");
    org.openbravo.dal.core.OBContext.setAdminMode(true);
    try {
      return (rec == null) ? null : rec.getEndingDate();
    } finally {
      org.openbravo.dal.core.OBContext.restorePreviousMode();
    }
  }

  private Date getBankStatementLineMinDate(FIN_BankStatement bankStatement) {
    OBContext.setAdminMode();
    Date minDate = new Date();
    try {
      final OBCriteria<FIN_BankStatementLine> obc = OBDal.getInstance()
          .createCriteria(FIN_BankStatementLine.class);
      obc.createAlias(FIN_BankStatementLine.PROPERTY_BANKSTATEMENT, "bs");
      obc.add(Restrictions.eq("bs." + FIN_BankStatement.PROPERTY_ID, bankStatement.getId()));
      obc.addOrderBy(FIN_BankStatementLine.PROPERTY_TRANSACTIONDATE, true);
      obc.setMaxResults(1);
      final List<FIN_BankStatementLine> bst = obc.list();
      if (bst.isEmpty()) {
        return minDate;
      }
      minDate = bst.get(0).getTransactionDate();
    } finally {
      OBContext.restorePreviousMode();
    }
    return minDate;
  }

  private void updateAffectedReconciliations(FIN_BankStatement bankStatement) {
    List<FIN_Reconciliation> affectedReconciliations = getAffectedReconciliations(
        bankStatement.getAccount(), getBankStatementLineMinDate(bankStatement));
    for (FIN_Reconciliation rec : affectedReconciliations) {
      updateReconciliation(rec);
    }

  }

  private List<FIN_Reconciliation> getAffectedReconciliations(FIN_FinancialAccount account,
      Date bankStatementLineMinDate) {
    OBContext.setAdminMode();
    try {
      final OBCriteria<FIN_Reconciliation> obc = OBDal.getInstance()
          .createCriteria(FIN_Reconciliation.class);
      obc.add(Restrictions.eq(FIN_Reconciliation.PROPERTY_ACCOUNT, account));
      obc.add(Restrictions.ge(FIN_Reconciliation.PROPERTY_ENDINGDATE, bankStatementLineMinDate));
      obc.setMaxResults(1);
      return obc.list();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void updateReconciliation(FIN_Reconciliation reconciliation) {
    OBContext.setAdminMode(false);
    try {
      // This is needed to allow completing a reconciliation with unmatched bank statement lines
      reconciliation.setStartingbalance(MatchTransactionDao.getStartingBalance(reconciliation));
      reconciliation.setEndingBalance(MatchTransactionDao.getEndingBalance(reconciliation));
      OBDal.getInstance().save(reconciliation);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
