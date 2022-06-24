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
 * All portions are Copyright (C) 2010-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.advpaymentmngt.process;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.dao.MatchTransactionDao;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.model.financialmgmt.payment.FIN_ReconciliationLine_v;
import org.openbravo.scheduling.ProcessBundle;

public class FIN_ReconciliationProcess implements org.openbravo.scheduling.Process {
  private static AdvPaymentMngtDao dao;
  private static final Logger log = LogManager.getLogger();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    dao = new AdvPaymentMngtDao();
    OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(
        Utility.messageBD(bundle.getConnection(), "Success", bundle.getContext().getLanguage()));

    OBContext.setAdminMode();
    try {
      // retrieve custom params
      final String strAction = (String) bundle.getParams().get("action");

      // retrieve standard params
      String recordID = (String) bundle.getParams().get("FIN_Reconciliation_ID");
      // This code is kept to maintain compatibility with previous tab which was built
      // on to of a view
      if (recordID == null || "".equals(recordID)) {
        recordID = (String) bundle.getParams().get("Aprm_Reconciliation_V_ID");
      }
      final FIN_Reconciliation reconciliation = dao.getObject(FIN_Reconciliation.class, recordID);
      final VariablesSecureApp vars = bundle.getContext().toVars();
      final ConnectionProvider conProvider = bundle.getConnection();
      final String language = bundle.getContext().getLanguage();

      reconciliation.setProcessNow(true);
      OBDal.getInstance().save(reconciliation);
      OBDal.getInstance().flush();
      if (strAction.equals("P")) {
        for (FIN_ReconciliationLine_v recLine : reconciliation.getFINReconciliationLineVList()) {
          recLine.getFinancialAccountTransaction().getReconciliation();
          recLine.getFinancialAccountTransaction().getReconciliation();
          boolean orgLegalWithAccounting = FIN_Utility.periodControlOpened(
              FIN_Reconciliation.TABLE_NAME,
              recLine.getFinancialAccountTransaction().getReconciliation().getId(),
              FIN_Reconciliation.TABLE_NAME + "_ID", "LE");
          if (!FIN_Utility.isPeriodOpen(
              recLine.getFinancialAccountTransaction().getClient().getId(),
              AcctServer.DOCTYPE_Reconciliation,
              recLine.getFinancialAccountTransaction().getOrganization().getId(),
              OBDateUtils.formatDate(recLine.getFinancialAccountTransaction().getDateAcct()))
              && orgLegalWithAccounting) {
            msg.setType("Error");
            msg.setTitle(Utility.messageBD(conProvider, "Error", language));
            msg.setMessage(String.format(Utility.parseTranslation(conProvider, vars, language,
                "@APRM_PeriodNotAvailableClearedItem@"), recLine.getIdentifier()));
            bundle.setResult(msg);
            OBDal.getInstance().rollbackAndClose();
            return;
          }
        }
        updateReconciliations(reconciliation);
        reconciliation.setProcessed(true);
        reconciliation.setAPRMProcessReconciliation("R");
        reconciliation.setAprmProcessRec("R");
        reconciliation.setAPRMProcessReconciliationForce("R");
        reconciliation.setDocumentStatus("CO");
        OBDal.getInstance().save(reconciliation);
        OBDal.getInstance().flush();

        // ***********************
        // Reactivate Reconciliation
        // ***********************
      } else if (strAction.equals("R")) {
        // Already Posted Document
        if ("Y".equals(reconciliation.getPosted())) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(conProvider, "Error", language));
          msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
              "@PostedDocument@" + ": " + reconciliation.getDocumentNo()));
          bundle.setResult(msg);
          return;
        }
        final boolean isForceProcess = "6BF16EFC772843AC9A17552AE0B26AB7"
            .equals(bundle.getProcessId());
        // Transaction exists
        if (!isForceProcess && !MatchTransactionDao.islastreconciliation(reconciliation)) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(conProvider, "Error", language));
          msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
              "@APRM_FutureReconciliationExists@"));
          bundle.setResult(msg);
          return;
        }
        if (existsDraftReconciliation(reconciliation.getAccount())) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(conProvider, "Error", language));
          msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
              "@APRM_DraftReconciliationExists@"));
          bundle.setResult(msg);
          return;
        }
        for (FIN_ReconciliationLine_v recLine : reconciliation.getFINReconciliationLineVList()) {
          recLine.getFinancialAccountTransaction().getReconciliation();
          recLine.getFinancialAccountTransaction().getReconciliation();
          boolean orgLegalWithAccounting = FIN_Utility.periodControlOpened(
              FIN_Reconciliation.TABLE_NAME,
              recLine.getFinancialAccountTransaction().getReconciliation().getId(),
              FIN_Reconciliation.TABLE_NAME + "_ID", "LE");
          if (!FIN_Utility.isPeriodOpen(
              recLine.getFinancialAccountTransaction().getClient().getId(),
              AcctServer.DOCTYPE_Reconciliation,
              recLine.getFinancialAccountTransaction().getOrganization().getId(),
              OBDateUtils.formatDate(recLine.getFinancialAccountTransaction().getDateAcct()))
              && orgLegalWithAccounting) {
            msg.setType("Error");
            msg.setTitle(Utility.messageBD(conProvider, "Error", language));
            msg.setMessage(String.format(Utility.parseTranslation(conProvider, vars, language,
                "@APRM_PeriodNotAvailableClearedItem@"), recLine.getIdentifier()));
            bundle.setResult(msg);
            OBDal.getInstance().rollbackAndClose();
            return;
          }
        }
        reconciliation.setProcessed(false);
        OBDal.getInstance().save(reconciliation);
        OBDal.getInstance().flush();
        reconciliation.setDocumentStatus("DR");
        reconciliation.setAPRMProcessReconciliation("P");
        reconciliation.setAprmProcessRec("P");
        reconciliation.setAPRMProcessReconciliationForce("P");
        OBDal.getInstance().save(reconciliation);
        OBDal.getInstance().flush();
      }
      reconciliation.setProcessNow(false);
      OBDal.getInstance().save(reconciliation);
      OBDal.getInstance().flush();
      bundle.setResult(msg);
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Error while executing FIN_ReconciliationProcess", e);
      msg.setType("Error");
      msg.setTitle(
          Utility.messageBD(bundle.getConnection(), "Error", bundle.getContext().getLanguage()));
      msg.setMessage(FIN_Utility.getExceptionMessage(e));
      bundle.setResult(msg);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private boolean existsDraftReconciliation(FIN_FinancialAccount account) {
    OBContext.setAdminMode(false);
    try {
      OBCriteria<FIN_Reconciliation> obc = OBDal.getInstance()
          .createCriteria(FIN_Reconciliation.class);
      obc.add(Restrictions.eq(FIN_Reconciliation.PROPERTY_DOCUMENTSTATUS, "DR"));
      obc.add(Restrictions.eq(FIN_Reconciliation.PROPERTY_PROCESSED, false));
      obc.add(Restrictions.eq(FIN_Reconciliation.PROPERTY_ACCOUNT, account));
      obc.setMaxResults(1);
      return obc.uniqueResult() != null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void updateReconciliations(FIN_Reconciliation reconciliation) {
    final OBCriteria<FIN_Reconciliation> obc = OBDal.getInstance()
        .createCriteria(FIN_Reconciliation.class);
    obc.add(
        Restrictions.ge(FIN_Reconciliation.PROPERTY_ENDINGDATE, reconciliation.getEndingDate()));
    obc.add(Restrictions.ge(FIN_Reconciliation.PROPERTY_CREATIONDATE,
        reconciliation.getCreationDate()));
    obc.add(Restrictions.eq(FIN_Reconciliation.PROPERTY_ACCOUNT, reconciliation.getAccount()));
    obc.addOrder(Order.asc(FIN_Reconciliation.PROPERTY_ENDINGDATE));
    obc.addOrder(Order.asc(FIN_Reconciliation.PROPERTY_CREATIONDATE));
    final List<FIN_Reconciliation> reconciliations = obc.list();
    for (FIN_Reconciliation rec : reconciliations) {
      updateReconciliation(rec);
    }
    return;
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
    return;
  }

}
