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
 * All portions are Copyright (C) 2010-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.dao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.hibernate.sql.JoinType;
import org.openbravo.advpaymentmngt.hook.BankStatementLineFilterContext;
import org.openbravo.advpaymentmngt.hook.BankStatementLineFilterContext.Usage;
import org.openbravo.advpaymentmngt.utility.BankStatementLineFilterHelper;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatement;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;

public class MatchTransactionDao {

  public MatchTransactionDao() {
  }

  public static <T extends BaseOBObject> T getObject(Class<T> t, String strId) {
    return OBDal.getInstance().get(t, strId);
  }

  public static BigDecimal getClearedLinesAmount(String strReconciliationId) {
    OBCriteria<FIN_FinaccTransaction> obCriteria = OBDal.getInstance()
        .createCriteria(FIN_FinaccTransaction.class);
    obCriteria.add(Restrictions.eq(FIN_FinaccTransaction.PROPERTY_RECONCILIATION,
        MatchTransactionDao.getObject(FIN_Reconciliation.class, strReconciliationId)));
    obCriteria.add(Restrictions.eq(FIN_FinaccTransaction.PROPERTY_STATUS, "RPPC"));
    List<FIN_FinaccTransaction> lines = obCriteria.list();

    BigDecimal total = new BigDecimal("0");
    if (lines.isEmpty()) {
      return total;
    }

    for (FIN_FinaccTransaction item : lines) {
      total = total.add(item.getDepositAmount().subtract(item.getPaymentAmount()));
    }

    return total;
  }

  public static boolean checkAllLinesCleared(String strFinancialAccountId) {
    // Check if all lines has been cleared: Bank Statement Lines
    OBCriteria<FIN_BankStatementLine> obCriteria = OBDal.getInstance()
        .createCriteria(FIN_BankStatementLine.class);
    FIN_FinancialAccount financialAccount = MatchTransactionDao
        .getObject(FIN_FinancialAccount.class, strFinancialAccountId);
    // FIXME : ****There should be some other filter, like the reconciliation id?
    obCriteria.add(Restrictions.in(FIN_BankStatementLine.PROPERTY_BANKSTATEMENT,
        financialAccount.getFINBankStatementList()));
    obCriteria.add(Restrictions.isNull(FIN_BankStatementLine.PROPERTY_FINANCIALACCOUNTTRANSACTION));
    obCriteria.setMaxResults(1);
    List<FIN_BankStatementLine> lines = obCriteria.list();

    return (lines.isEmpty());
  }

  public static boolean islastreconciliation(FIN_Reconciliation reconciliation) {
    if (MatchTransactionDao.getReconciliationListAfterDate(reconciliation).size() > 0) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * Returns the processed bank statement lines of the given financial account that are not matched
   * to any transaction. Lines excluded by a
   * {@link org.openbravo.advpaymentmngt.hook.BankStatementLineFilterHook} are not returned.
   *
   * @param account
   *          the financial account
   * @return the unmatched bank statement lines
   */
  public static List<FIN_BankStatementLine> getUnMatchedBankStatementLines(
      FIN_FinancialAccount account) {
    final Map<String, Object> parameters = new HashMap<>();
    parameters.put("accountId", account.getId());
    //@formatter:off
    final String whereClause = " as bsl "
        + " where bsl.bankStatement.account.id = :accountId"
        + "   and bsl.financialAccountTransaction is null"
        + "   and bsl.bankStatement.processed = 'Y'"
        + BankStatementLineFilterHelper.buildHqlCondition("bsl",
            new BankStatementLineFilterContext(account, null, Usage.UNMATCHED_CHECK), parameters);
    //@formatter:on
    final OBQuery<FIN_BankStatementLine> obData = OBDal.getInstance()
        .createQuery(FIN_BankStatementLine.class, whereClause);
    obData.setNamedParameters(parameters);
    return obData.list();
  }

  public static List<FIN_FinaccTransaction> getMatchingFinancialTransaction(
      String strFinancialAccountId, Date transactionDate, String strReference, BigDecimal amount,
      String strBpartner, List<FIN_FinaccTransaction> excluded) {
    final Map<String, Object> parameters = new HashMap<>();
    List<FIN_FinaccTransaction> result = null;
    OBContext.setAdminMode();
    try {
      //@formatter:off
      String whereClause = " as ft " 
          + " where ft.account.id = :financialAccountId"
          + "   and ft.reconciliation is null" 
          + "   and ft.processed = true"
          + "   and ft.status <> 'RPPC' " 
          + "   and (ft.depositAmount - paymentAmount) = :amount";
      parameters.put("financialAccountId", strFinancialAccountId);
      parameters.put("amount", amount);
      if (transactionDate != null) {
        whereClause += " and ft.transactionDate = :transactionDate";
        parameters.put("transactionDate", transactionDate);
      }
      whereClause += "   and ft.finPayment.businessPartner.name";

      if (strBpartner != null) {
        whereClause += " = :bpName";
        parameters.put("bpName", strBpartner);
      } else {
        whereClause += " is null";
      }
      if (!"".equals(strReference) && !"**".equals(strReference)) {
        whereClause += " and (ft.finPayment.referenceNo = :referenceNo"
                    +  "   or ft.finPayment.documentNo = :documentNo)";
        parameters.put("referenceNo", strReference);
        parameters.put("documentNo", strReference);
      }

      //@formatter:on
      final OBQuery<FIN_FinaccTransaction> obData = OBDal.getInstance()
          .createQuery(FIN_FinaccTransaction.class, whereClause);
      obData.setNamedParameters(parameters);

      result = obData.list();
      result.removeAll(excluded);
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  public static List<FIN_FinaccTransaction> getMatchingFinancialTransaction(
      String strFinancialAccountId, Date transactionDate, String strReference, BigDecimal amount,
      List<FIN_FinaccTransaction> excluded) {
    final Map<String, Object> parameters = new HashMap<>();
    List<FIN_FinaccTransaction> result = null;
    OBContext.setAdminMode();
    try {
      //@formatter:off
      String whereClause = " as ft "
       + " where ft.account.id = :financialAccountId"
       + "   and ft.reconciliation is null"
       + "   and ft.processed = true"
       + "   and ft.status <> 'RPPC' "
       + "   and (ft.depositAmount - paymentAmount) = :amount";
      parameters.put("financialAccountId", strFinancialAccountId);
      parameters.put("amount", amount);
      if (transactionDate != null) {
        whereClause += "   and ft.transactionDate = :transactionDate";
        parameters.put("transactionDate", transactionDate);
      }

      if (!"".equals(strReference) && !"**".equals(strReference)) {
        whereClause += "   and (ft.finPayment.referenceNo = :referenceNo"
                    +  "     or ft.finPayment.documentNo = :documentNo)";
        parameters.put("referenceNo", strReference);
        parameters.put("documentNo", strReference);
      }

      //@formatter:on
      final OBQuery<FIN_FinaccTransaction> obData = OBDal.getInstance()
          .createQuery(FIN_FinaccTransaction.class, whereClause);
      obData.setNamedParameters(parameters);

      result = obData.list();
      result.removeAll(excluded);
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  public static List<FIN_FinaccTransaction> getMatchingFinancialTransaction(
      String strFinancialAccountId, String strReference, BigDecimal amount, String strBpartner,
      List<FIN_FinaccTransaction> excluded) {
    return getMatchingFinancialTransaction(strFinancialAccountId, null, strReference, amount,
        strBpartner, excluded);
  }

  public static List<FIN_FinaccTransaction> getMatchingFinancialTransaction(
      String strFinancialAccountId, Date transactionDate, BigDecimal amount,
      List<FIN_FinaccTransaction> excluded) {
    final Map<String, Object> parameters = new HashMap<>();

    //@formatter:off
    String whereClause = " as ft "
       + " where ft.account.id = :financialAccountId"
       + "   and ft.reconciliation is null"
       + "   and ft.processed = true"
       + "   and ft.status <> 'RPPC' "
       + "   and (ft.depositAmount - paymentAmount) = :amount";
    parameters.put("financialAccountId", strFinancialAccountId);
    parameters.put("amount", amount);
    if (transactionDate != null) {
      whereClause += "   and ft.transactionDate = :transactionDate";
      parameters.put("transactionDate", transactionDate);
    }
    //@formatter:on
    final OBQuery<FIN_FinaccTransaction> obData = OBDal.getInstance()
        .createQuery(FIN_FinaccTransaction.class, whereClause);
    obData.setNamedParameters(parameters);
    List<FIN_FinaccTransaction> result = obData.list();
    result.removeAll(excluded);
    return result;
  }

  public static List<FIN_FinaccTransaction> getMatchingGLItemTransaction(
      String strFinancialAccountId, GLItem glItem, Date transactionDate, BigDecimal amount,
      List<FIN_FinaccTransaction> excluded) {
    final Map<String, Object> parameters = new HashMap<>();

    //@formatter:off
    String whereClause = " as ft "
        + " where ft.account.id = :financialAccountId"
        + "   and ft.reconciliation is null"
        + "   and ft.processed = true"
        + "   and ft.gLItem = :glItem"
        + "   and ft.status <> 'RPPC' "
        + "   and (ft.depositAmount - paymentAmount) = :amount";
    parameters.put("financialAccountId", strFinancialAccountId);
    parameters.put("glItem", glItem);
    parameters.put("amount", amount);
    if (transactionDate != null) {
      whereClause += "   and ft.transactionDate <= :transactionDate";
      parameters.put("transactionDate", transactionDate);
    }
    //@formatter:on
    final OBQuery<FIN_FinaccTransaction> obData = OBDal.getInstance()
        .createQuery(FIN_FinaccTransaction.class, whereClause);
    obData.setNamedParameters(parameters);
    List<FIN_FinaccTransaction> result = obData.list();
    result.removeAll(excluded);
    return result;
  }

  public static List<FIN_FinaccTransaction> getMatchingFinancialTransaction(
      String strFinancialAccountId, BigDecimal amount, List<FIN_FinaccTransaction> excluded) {
    return getMatchingFinancialTransaction(strFinancialAccountId, null, amount, excluded);
  }

  public static List<FIN_FinaccTransaction> getMatchingFinancialTransaction(
      String strFinancialAccountId, BigDecimal amount) {
    return getMatchingFinancialTransaction(strFinancialAccountId, null, amount, null);
  }

  /**
   * Returns the latest transaction date of the processed bank statement lines of the given
   * financial account. Lines excluded by a
   * {@link org.openbravo.advpaymentmngt.hook.BankStatementLineFilterHook} are not taken into
   * account.
   *
   * @param financialAccount
   *          the financial account
   * @return the latest transaction date, or the current date if there are no lines
   */
  public static Date getBankStatementLineMaxDate(FIN_FinancialAccount financialAccount) {
    OBContext.setAdminMode();
    Date maxDate = new Date();
    try {
      final Map<String, Object> parameters = new HashMap<>();
      parameters.put("account", financialAccount.getId());
      //@formatter:off
      final String hql = "as bsl"
          + " where bsl.bankStatement.account.id = :account"
          + "   and bsl.bankStatement.processed = true"
          + BankStatementLineFilterHelper.buildHqlCondition("bsl",
              new BankStatementLineFilterContext(financialAccount, null, Usage.MAX_DATE),
              parameters)
          + " order by bsl.transactionDate desc";
      //@formatter:on
      final OBQuery<FIN_BankStatementLine> obq = OBDal.getInstance()
          .createQuery(FIN_BankStatementLine.class, hql);
      obq.setNamedParameters(parameters);
      obq.setMaxResult(1);
      final List<FIN_BankStatementLine> bst = obq.list();
      if (bst.size() == 0) {
        return maxDate;
      }
      maxDate = bst.get(0).getTransactionDate();
    } finally {
      OBContext.restorePreviousMode();
    }
    return maxDate;
  }

  public static BigDecimal getReconciliationLastAmount(FIN_FinancialAccount financialAccount) {
    OBContext.setAdminMode();
    BigDecimal lastAmount = financialAccount.getInitialBalance();
    try {
      final OBCriteria<FIN_Reconciliation> obc = OBDal.getInstance()
          .createCriteria(FIN_Reconciliation.class);
      obc.add(Restrictions.eq(FIN_Reconciliation.PROPERTY_PROCESSED, true));
      obc.add(Restrictions.eq(FIN_Reconciliation.PROPERTY_ACCOUNT, financialAccount));
      obc.addOrderBy(FIN_Reconciliation.PROPERTY_ENDINGDATE, false);
      obc.setMaxResults(1);
      final List<FIN_Reconciliation> rec = obc.list();
      if (rec.size() == 0) {
        return lastAmount;
      }
      lastAmount = rec.get(0).getEndingBalance();
    } finally {
      OBContext.restorePreviousMode();
    }
    return lastAmount;
  }

  /**
   * Calculates the balance of unmatched bank statements for the given reconciliation
   * 
   * @param lastReconciliation
   *          Reconciliation.
   * @return Last reconciliation UnMatched balance
   */
  public static BigDecimal getLastReconciliationUnmatchedBalance(
      FIN_Reconciliation lastReconciliation) {
    BigDecimal total = BigDecimal.ZERO;
    OBContext.setAdminMode(true);
    try {
      OBCriteria<FIN_BankStatementLine> obcBsl = OBDal.getInstance()
          .createCriteria(FIN_BankStatementLine.class);
      obcBsl.createAlias(FIN_BankStatementLine.PROPERTY_BANKSTATEMENT, "bs");
      obcBsl.createAlias(FIN_BankStatementLine.PROPERTY_FINANCIALACCOUNTTRANSACTION, "tr",
          JoinType.LEFT_OUTER_JOIN);

      List<FIN_Reconciliation> afterReconciliations = getReconciliationListAfterDate(
          lastReconciliation);
      if (afterReconciliations.size() > 0) {
        obcBsl.add(Restrictions.or(
            Restrictions.isNull(FIN_BankStatementLine.PROPERTY_FINANCIALACCOUNTTRANSACTION),
            Restrictions.in("tr." + FIN_FinaccTransaction.PROPERTY_RECONCILIATION,
                afterReconciliations)));
      } else {
        obcBsl.add(Restrictions.isNull(FIN_BankStatementLine.PROPERTY_FINANCIALACCOUNTTRANSACTION));
      }
      obcBsl.add(Restrictions.eq("bs." + FIN_BankStatement.PROPERTY_ACCOUNT,
          lastReconciliation.getAccount()));
      obcBsl.add(Restrictions.eq("bs." + FIN_BankStatement.PROPERTY_PROCESSED, true));
      obcBsl.add(Restrictions.le(FIN_BankStatementLine.PROPERTY_TRANSACTIONDATE,
          lastReconciliation.getTransactionDate()));
      ProjectionList projections = Projections.projectionList();
      projections.add(Projections.sum(FIN_BankStatementLine.PROPERTY_CRAMOUNT));
      projections.add(Projections.sum(FIN_BankStatementLine.PROPERTY_DRAMOUNT));
      obcBsl.setProjection(projections);

      @SuppressWarnings("rawtypes")
      List o = obcBsl.list();
      if (o != null && o.size() > 0) {
        Object[] resultSet = (Object[]) o.get(0);
        BigDecimal credit = (resultSet[0] != null) ? (BigDecimal) resultSet[0] : BigDecimal.ZERO;
        BigDecimal debit = (resultSet[1] != null) ? (BigDecimal) resultSet[1] : BigDecimal.ZERO;
        total = credit.subtract(debit);
      }
      o.clear();

    } finally {
      OBContext.restorePreviousMode();
    }

    return total;
  }

  /**
   * 
   * @param reconciliation
   * @return List of later reconciliations that given one for the same financial account
   */
  public static List<FIN_Reconciliation> getReconciliationListAfterDate(
      FIN_Reconciliation reconciliation) {
    OBContext.setAdminMode(true);
    List<FIN_Reconciliation> reconciliations = new ArrayList<FIN_Reconciliation>();
    try {
      OBCriteria<FIN_Reconciliation> obc = OBDal.getInstance()
          .createCriteria(FIN_Reconciliation.class);
      obc.add(Restrictions.eq(FIN_Reconciliation.PROPERTY_ACCOUNT, reconciliation.getAccount()));
      obc.add(Restrictions.gt(FIN_Reconciliation.PROPERTY_CREATIONDATE,
          reconciliation.getCreationDate()));
      reconciliations = obc.list();
    } finally {
      OBContext.restorePreviousMode();
    }
    return reconciliations;
  }

  public static BigDecimal getEndingBalance(FIN_Reconciliation reconciliation) {
    OBContext.setAdminMode(false);
    try {
      BigDecimal endingBalance = reconciliation.getAccount().getInitialBalance();
      endingBalance = endingBalance.add(getBSLAmount(reconciliation))
          .add(getManualReconciliationAmount(reconciliation));
      return endingBalance;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private static BigDecimal getManualReconciliationAmount(FIN_Reconciliation reconciliation) {
    BigDecimal total = BigDecimal.ZERO;
    OBContext.setAdminMode(false);
    try {
      //@formatter:off
      final String hqlString = "select coalesce(sum(e.depositAmount-e.paymentAmount),0)"
          + " from FIN_Finacc_Transaction as e"
          + " where e.account.id = :account"
          + "   and e.processed = true"
          + "   and e.reconciliation is not null"
          + "   and not exists ("
          + "       select 1 "
          + "       from FIN_BankStatementLine as bsl "
          + "       where bsl.financialAccountTransaction = e)"
          + "   and e.transactionDate <= :date";
      
      //@formatter:on
      final Session session = OBDal.getInstance().getSession();
      final Query<BigDecimal> query = session.createQuery(hqlString, BigDecimal.class);
      query.setParameter("account", reconciliation.getAccount().getId());
      query.setParameter("date", reconciliation.getEndingDate());
      total = query.uniqueResult();
    } finally {
      OBContext.restorePreviousMode();
    }
    return total;
  }

  /**
   * Returns the net amount (credit minus debit) of the processed bank statement lines of the
   * reconciliation's financial account up to the reconciliation's ending date. Lines excluded by a
   * {@link org.openbravo.advpaymentmngt.hook.BankStatementLineFilterHook} are not taken into
   * account.
   *
   * @param reconciliation
   *          the reconciliation
   * @return the net amount of the bank statement lines, zero if there are no lines
   */
  private static BigDecimal getBSLAmount(FIN_Reconciliation reconciliation) {
    BigDecimal total = BigDecimal.ZERO;
    OBContext.setAdminMode(false);
    try {
      final Map<String, Object> parameters = new HashMap<>();
      parameters.put("account", reconciliation.getAccount().getId());
      parameters.put("endingDate", reconciliation.getEndingDate());
      //@formatter:off
      final String hql = "as bsl"
          + " where bsl.bankStatement.account.id = :account"
          + "   and bsl.bankStatement.processed = true"
          + "   and bsl.transactionDate <= :endingDate"
          + BankStatementLineFilterHelper.buildHqlCondition("bsl",
              new BankStatementLineFilterContext(reconciliation.getAccount(), reconciliation,
                  Usage.ENDING_BALANCE),
              parameters);
      //@formatter:on
      final OBQuery<FIN_BankStatementLine> obq = OBDal.getInstance()
          .createQuery(FIN_BankStatementLine.class, hql);
      obq.setNamedParameters(parameters);
      obq.setSelectClause("coalesce(sum(bsl.cramount), 0), coalesce(sum(bsl.dramount), 0)");

      final Object[] resultSet = obq.createQuery(Object[].class).uniqueResult();
      if (resultSet != null) {
        BigDecimal credit = (resultSet[0] != null) ? (BigDecimal) resultSet[0] : BigDecimal.ZERO;
        BigDecimal debit = (resultSet[1] != null) ? (BigDecimal) resultSet[1] : BigDecimal.ZERO;
        total = credit.subtract(debit);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return total;
  }

  /**
   * Returns the bank statement lines that are pending to be reconciled at the ending date of the
   * given reconciliation, as shown in the "Un-reconciled Bank Statement Lines" section of the
   * reconciliation report. A line is pending when:
   * <ul>
   * <li>it belongs to a processed bank statement of the reconciliation's financial account;</li>
   * <li>its transaction date is lower or equal than the reconciliation's ending date;</li>
   * <li>it is not matched to any transaction, or it is matched to a transaction that belongs to a
   * reconciliation created after the given one;</li>
   * <li>it is not excluded by a
   * {@link org.openbravo.advpaymentmngt.hook.BankStatementLineFilterHook}.</li>
   * </ul>
   *
   * @param reconciliation
   *          the reconciliation
   * @return one row per line, sorted by date, description and amount. Each row contains, in this
   *         order: the transaction date ({@link Date}), the description, which is the business
   *         partner name or the business partner name informed in the line ({@link String}, never
   *         {@code null}), the reference number ({@link String}) and the amount, credit minus debit
   *         ({@link BigDecimal})
   */
  public static List<Object[]> getUnreconciledBankStatementLines(
      FIN_Reconciliation reconciliation) {
    OBContext.setAdminMode(true);
    try {
      final Map<String, Object> parameters = new HashMap<>();
      parameters.put("account", reconciliation.getAccount().getId());
      parameters.put("endingDate", reconciliation.getEndingDate());

      final List<String> afterReconciliationIds = new ArrayList<>();
      for (FIN_Reconciliation afterReconciliation : getReconciliationListAfterDate(
          reconciliation)) {
        afterReconciliationIds.add(afterReconciliation.getId());
      }
      final String transactionCondition;
      if (afterReconciliationIds.isEmpty()) {
        transactionCondition = "tr is null";
      } else {
        transactionCondition = "(tr is null or tr.reconciliation.id in (:afterReconciliations))";
        parameters.put("afterReconciliations", afterReconciliationIds);
      }

      final String description = "coalesce(bp.name, bsl.bpartnername, '')";
      final String amount = "(bsl.cramount - bsl.dramount)";
      //@formatter:off
      final String hql = "as bsl"
          + " left join bsl.businessPartner as bp"
          + " left join bsl.financialAccountTransaction as tr"
          + " where bsl.bankStatement.account.id = :account"
          + "   and bsl.bankStatement.processed = true"
          + "   and bsl.transactionDate <= :endingDate"
          + "   and " + transactionCondition
          + BankStatementLineFilterHelper.buildHqlCondition("bsl",
              new BankStatementLineFilterContext(reconciliation.getAccount(), reconciliation,
                  Usage.RECONCILIATION_REPORT),
              parameters)
          + " order by bsl.transactionDate, " + description + ", " + amount;
      //@formatter:on
      final OBQuery<FIN_BankStatementLine> obq = OBDal.getInstance()
          .createQuery(FIN_BankStatementLine.class, hql);
      obq.setNamedParameters(parameters);
      obq.setSelectClause(
          "bsl.transactionDate, " + description + ", bsl.referenceNo, " + amount);
      return obq.createQuery(Object[].class).list();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static BigDecimal getStartingBalance(FIN_Reconciliation reconciliation) {
    OBContext.setAdminMode(false);
    try {
      BigDecimal statingBalance = reconciliation.getAccount().getInitialBalance();
      FIN_Reconciliation previousReconciliation = getPreviousReconciliation(reconciliation);
      if (previousReconciliation != null) {
        statingBalance = previousReconciliation.getEndingBalance();
      }
      return statingBalance;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private static FIN_Reconciliation getPreviousReconciliation(FIN_Reconciliation reconciliation) {
    OBContext.setAdminMode(false);
    try {
      final OBCriteria<FIN_Reconciliation> obc = OBDal.getInstance()
          .createCriteria(FIN_Reconciliation.class);
      obc.add(
          Restrictions.le(FIN_Reconciliation.PROPERTY_ENDINGDATE, reconciliation.getEndingDate()));
      obc.add(Restrictions.lt(FIN_Reconciliation.PROPERTY_CREATIONDATE,
          reconciliation.getCreationDate()));
      obc.add(Restrictions.eq(FIN_Reconciliation.PROPERTY_ACCOUNT, reconciliation.getAccount()));
      obc.addOrder(Order.desc(FIN_Reconciliation.PROPERTY_ENDINGDATE));
      obc.addOrder(Order.desc(FIN_Reconciliation.PROPERTY_CREATIONDATE));
      obc.setMaxResults(1);
      return (FIN_Reconciliation) obc.uniqueResult();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
