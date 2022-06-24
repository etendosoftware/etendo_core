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
 * All portions are Copyright (C) 2012-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.costing;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.scheduling.KillableProcess;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * @author gorkaion
 * 
 */
public class CostingBackground extends DalBaseProcess implements KillableProcess {
  private static final Logger log4j = LogManager.getLogger();
  public static final String AD_PROCESS_ID = "3F2B4AAC707B4CE7B98D2005CF7310B5";
  private ProcessLogger logger;
  public static final String TRANSACTION_COST_DATEACCT_INITIALIZED = "TransactionCostDateacctInitialized";
  private boolean killProcess = false;
  private final int BATCH_SIZE = 10000;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    logger = bundle.getLogger();
    OBError result = new OBError();
    List<String> orgsWithRule = new ArrayList<String>();
    List<String> trxs = null;
    try {
      OBContext.setAdminMode(false);

      CostingUtils.checkValidOrganization(AD_PROCESS_ID,
          OBContext.getOBContext().getCurrentOrganization());

      result.setType("Success");
      result.setTitle(OBMessageUtils.messageBD("Success"));

      // Initialize Transaction Cost Date Acct
      initializeMtransCostDateAcct();

      //@formatter:off
      // Get organizations with costing rules.
      final String where = " as o"
          + " where exists ("
          + "    select 1 from CostingRule as cr"
          + "    where ad_isorgincluded(o.id, cr.organization.id, client.id) <> -1 "
          + "      and cr.validated is true"
          + "    )"
          + " and ad_isorgincluded(o.id, :orgId, :clientId) <> -1 ";
      //@formatter:on
      OBQuery<Organization> orgQry = OBDal.getInstance().createQuery(Organization.class, where);
      orgQry.setNamedParameter("orgId", bundle.getContext().getOrganization());
      orgQry.setNamedParameter("clientId", bundle.getContext().getClient());
      List<Organization> orgs = orgQry.list();
      if (orgs.size() == 0) {
        log4j.debug("No organizations with Costing Rule defined");
        logger.logln(OBMessageUtils.messageBD("Success"));
        bundle.setResult(result);
        return;
      }
      for (Organization org : orgs) {
        orgsWithRule.add(org.getId());
      }

      // Fix the Not Processed flag for those Transactions with Cost Not Calculated
      setNotProcessedWhenNotCalculatedTransactions(orgsWithRule);

      int batch = 0;
      int counter = 0;
      int counterBatch;
      int pendingTrxs = 0;
      long elapsedTime = 0;
      if (log4j.isDebugEnabled()) {
        pendingTrxs = getTransactionsBatchCount(orgsWithRule);
        log4j.debug("Pending transactions: " + pendingTrxs);
      }

      trxs = getTransactionsBatch(orgsWithRule);
      while (!trxs.isEmpty()) {
        long t1 = System.currentTimeMillis();
        batch++;
        counterBatch = 0;
        for (String trxId : trxs) {
          if (killProcess) {
            throw new OBException("Process killed");
          }
          long t3 = System.currentTimeMillis();
          log4j.debug("Starting transaction process: " + trxId);
          counter++;
          counterBatch++;
          MaterialTransaction transaction = OBDal.getInstance()
              .get(MaterialTransaction.class, trxId);
          CostingServer transactionCost = new CostingServer(transaction);
          transactionCost.process();
          OBDal.getInstance().getSession().clear();
          OBDal.getInstance().getConnection(true).commit();
          long t4 = System.currentTimeMillis();
          log4j.debug("Ending transaction process: transaction: " + counter + ", batch: " + batch
              + ". Took: " + (t4 - t3) + " ms.");
        }
        trxs = getTransactionsBatch(orgsWithRule);

        if (log4j.isDebugEnabled()) {
          long t2 = System.currentTimeMillis();
          log4j.debug("Processing batch: " + batch + " (" + counterBatch + " transactions) took: "
              + (t2 - t1) + " ms.");
          pendingTrxs -= counterBatch;
          int pendingBatches = (pendingTrxs % BATCH_SIZE == 0) ? (pendingTrxs / BATCH_SIZE)
              : (pendingTrxs / BATCH_SIZE) + 1;
          elapsedTime += t2 - t1;
          long avgTimePerBatch = elapsedTime / batch / 1000;
          log4j.debug("Pending transactions: " + pendingTrxs);
          log4j.debug(
              "Average time per batch: " + avgTimePerBatch + " seconds. Estimated time to finish: "
                  + (avgTimePerBatch * pendingBatches) + " seconds.");
        }
      }

      logger.logln(OBMessageUtils.messageBD("Success"));
      bundle.setResult(result);
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      String message = OBMessageUtils.parseTranslation(bundle.getConnection(),
          bundle.getContext().toVars(), OBContext.getOBContext().getLanguage().getLanguage(),
          e.getMessage());
      result.setMessage(message);
      result.setType("Error");
      log4j.error(message, e);
      logger.logln(message);
      bundle.setResult(result);
      return;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      result = OBMessageUtils.translateError(bundle.getConnection(), bundle.getContext().toVars(),
          OBContext.getOBContext().getLanguage().getLanguage(), e.getMessage());
      result.setType("Error");
      result.setTitle(OBMessageUtils.messageBD("Error"));
      log4j.error(result.getMessage(), e);
      logger.logln(result.getMessage());
      bundle.setResult(result);
      return;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Get Transactions with Processed flag = 'Y' and it's cost is Not Calculated and set Processed
   * flag = 'N'
   */
  private void setNotProcessedWhenNotCalculatedTransactions(List<String> orgsWithRule) {
    //@formatter:off
    final String hqlTransactions = " update MaterialMgmtMaterialTransaction as trx "
        + " set trx.isProcessed = false "
        + " where trx.isProcessed = true"
        + "   and trx.isCostCalculated = false"
        + "   and trx.costingStatus <> 'S'"
        + "   and trx.organization.id in (:orgs)";

    //@formatter:on
    @SuppressWarnings("rawtypes")
    Query updateTransactions = OBDal.getInstance().getSession().createQuery(hqlTransactions);
    updateTransactions.setParameterList("orgs", orgsWithRule);
    updateTransactions.executeUpdate();

    OBDal.getInstance().flush();
  }

  private List<String> getTransactionsBatch(List<String> orgsWithRule) {
    //@formatter:off
    final String where = " select trx.id as id "
        + " from MaterialMgmtMaterialTransaction as trx"
        + "   join trx.product as p,"
        + "   ADList as trxtype"
        + " where trx.isProcessed = false"
        + "   and trx.costingStatus <> 'S'"
        + "   and p.productType = 'I'"
        + "   and p.stocked = true"
        + "   and trxtype.reference.id = :refid"
        + "   and trxtype.searchKey = trx.movementType"
        + "   and trx.transactionProcessDate <= :now"
        + "   and trx.organization.id in (:orgs)"
        + " order by trx.transactionProcessDate, trxtype.sequenceNumber, "
        + " trx.movementQuantity desc, trx.id";
    
    //@formatter:on
    Query<String> trxQry = OBDal.getInstance().getSession().createQuery(where, String.class);

    trxQry.setParameter("refid", CostAdjustmentUtils.MovementTypeRefID);
    trxQry.setParameter("now", new Date());
    trxQry.setParameterList("orgs", orgsWithRule);
    trxQry.setMaxResults(BATCH_SIZE);
    return trxQry.list();
  }

  private int getTransactionsBatchCount(List<String> orgsWithRule) {
    //@formatter:off
    final String where = " select count(trx.id) "
        + " from MaterialMgmtMaterialTransaction as trx"
        + "   join trx.product as p, ADList as trxtype"
        + " where trx.isProcessed = false"
        + "   and trx.costingStatus <> 'S'"
        + "   and p.productType = 'I'"
        + "   and p.stocked = true"
        + "   and trxtype.reference.id = :refid"
        + "   and trxtype.searchKey = trx.movementType"
        + "   and trx.transactionProcessDate <= :now"
        + "   and trx.organization.id in (:orgs)";

    //@formatter:on
    Query<Long> trxQry = OBDal.getInstance().getSession().createQuery(where, Long.class);

    trxQry.setParameter("refid", CostAdjustmentUtils.MovementTypeRefID);
    trxQry.setParameter("now", new Date());
    trxQry.setParameterList("orgs", orgsWithRule);
    trxQry.setMaxResults(1);
    return (trxQry.uniqueResult()).intValue();
  }

  private void initializeMtransCostDateAcct() throws Exception {
    boolean transactionCostDateacctInitialized = false;
    Client client = OBDal.getInstance().get(Client.class, "0");
    Organization organization = OBDal.getInstance().get(Organization.class, "0");
    try {
      transactionCostDateacctInitialized = Preferences
          .getPreferenceValue(CostingBackground.TRANSACTION_COST_DATEACCT_INITIALIZED, false,
              client, organization, null, null, null)
          .equals(Preferences.YES);
    } catch (PropertyException e1) {
      transactionCostDateacctInitialized = false;
    }

    if (!transactionCostDateacctInitialized) {

      try {
        ConnectionProvider cp = new DalConnectionProvider();
        InitializeCostingMTransCostDateacctData
            .initializeCostingMTransCostDateacct(cp.getConnection(), cp);
        InitializeCostingMTransCostDateacctData
            .initializeCostingMTransCostDateacct2(cp.getConnection(), cp);

      } catch (ServletException e) {
        log4j.error(
            "SQL error in Costing Backgroung Initializing Transaction Cost Date Acct: Exception:"
                + e);
        throw new OBException("@CODE=" + e.getCause() + "@" + e.getMessage());
      } catch (NoConnectionAvailableException e) {
        log4j.error("Connection error in query: Exception:" + e);
        throw new OBException("@CODE=NoConnectionAvailable");
      } finally {
        try {
        } catch (Exception ignore) {
        }
      }

      // Create the preference
      Preference transactionCostDateacctInitializedPreference = OBProvider.getInstance()
          .get(Preference.class);
      transactionCostDateacctInitializedPreference.setClient(client);
      transactionCostDateacctInitializedPreference.setOrganization(organization);
      transactionCostDateacctInitializedPreference
          .setAttribute(CostingBackground.TRANSACTION_COST_DATEACCT_INITIALIZED);
      transactionCostDateacctInitializedPreference.setSearchKey("Y");
      transactionCostDateacctInitializedPreference.setPropertyList(false);
      OBDal.getInstance().save(transactionCostDateacctInitializedPreference);
      OBDal.getInstance().flush();
      OBDal.getInstance().getConnection(true).commit();
    }
  }

  @Override
  public void kill(ProcessBundle processBundle) throws Exception {
    this.killProcess = true;
  }
}
