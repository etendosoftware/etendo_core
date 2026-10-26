/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright © 2021–2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.advpaymentmngt.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeNotNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.openbravo.advpaymentmngt.hook.BankStatementLineFilterHook;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatement;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.test.base.TestConstants.Roles;

/**
 * Database tests that verify that, without bank statement line filter hooks, the queries of
 * {@link MatchTransactionDao} used by the reconciliation report and the reconciliation ending
 * balance return exactly the same results as the implementation they replaced.
 * <p>
 * The legacy implementations are reproduced in this class: the SQL query of the
 * UnreconciledBankStatement subreport and the criteria used to calculate the report total and the
 * bank statement lines amount. No data is committed: the edge case data is rolled back.
 */
public class MatchTransactionDaoParityTest extends OBBaseTest {

  /** SQL query of the UnreconciledBankStatement subreport before the hook was introduced. */
  private static final String LEGACY_DETAIL_SQL = "SELECT bsl.datetrx as BSLINE_DATE,"
      + " coalesce(TO_CHAR(bp.name), TO_CHAR(bsl.bpartnername), '') as DESCRIPTION,"
      + " bsl.referenceno as REF, (bsl.cramount - bsl.dramount) as AMOUNT"
      + " FROM fin_bankstatementline bsl"
      + " join fin_bankstatement bs on (bsl.fin_bankstatement_id=bs.fin_bankstatement_id)"
      + " left join c_bpartner bp on (bp.c_bpartner_id = bsl.c_bpartner_id)"
      + " WHERE bs.processed = 'Y'"
      + " and bs.fin_financial_account_id = (SELECT rec.fin_financial_account_id"
      + "   FROM fin_reconciliation rec WHERE fin_reconciliation_id = :reconciliationId)"
      + " and bsl.datetrx <= (SELECT rec.dateto"
      + "   FROM fin_reconciliation rec WHERE fin_reconciliation_id = :reconciliationId)"
      + " and (bsl.fin_finacc_transaction_id is null"
      + "   or bsl.fin_finacc_transaction_id in (select fin_finacc_transaction_id"
      + "     from fin_finacc_transaction, fin_reconciliation"
      + "     where fin_finacc_transaction.fin_reconciliation_id = fin_reconciliation.fin_reconciliation_id"
      + "     and fin_reconciliation.created > (SELECT rec.created"
      + "       FROM fin_reconciliation rec WHERE fin_reconciliation_id = :reconciliationId)))"
      + " ORDER BY BSLINE_DATE, DESCRIPTION, AMOUNT";

  private static final String INACTIVE_LINE_REFERENCE = "ETP5581-INACTIVE";
  private static final String OTHER_ACCOUNT_LINE_REFERENCE = "ETP5581-OTHER-ACCOUNT";

  private MockedStatic<WeldUtils> mockedWeldUtils;

  /**
   * Makes sure that no bank statement line filter hook is applied.
   */
  @Before
  public void disableHooks() {
    mockedWeldUtils = mockStatic(WeldUtils.class, CALLS_REAL_METHODS);
    mockedWeldUtils.when(() -> WeldUtils.getInstances(BankStatementLineFilterHook.class))
        .thenReturn(Collections.emptyList());
  }

  /**
   * Restores the CDI lookup and discards any pending change.
   */
  @After
  public void restore() {
    mockedWeldUtils.close();
    OBDal.getInstance().rollbackAndClose();
  }

  /**
   * With a role that can read all the organizations, the detail, the total and the bank statement
   * lines amount are the same as before for every reconciliation.
   */
  @Test
  public void testParityWithGroupAdminRole() {
    setTestAdminContext();
    assertParityForAllReconciliations();
  }

  /**
   * With a role limited to some organizations, the detail, the total and the bank statement lines
   * amount are the same as before for every reconciliation.
   */
  @Test
  public void testParityWithOrganizationLimitedRole() {
    OBContext.setOBContext(TEST_USER_ID, Roles.ESP_ADMIN, TEST_CLIENT_ID, TEST_ORG_ID);
    assertParityForAllReconciliations();
  }

  /**
   * An inactive line and a line matched to a transaction of another financial account that belongs
   * to a later reconciliation are shown in the detail, as the original SQL query did, while the
   * total keeps excluding them, as the original criteria did.
   */
  @Test
  public void testEdgeCasesKeepLegacyBehaviour() {
    setTestAdminContext();
    OBContext.setAdminMode(true);
    try {
      FIN_FinaccTransaction otherAccountTransaction = findReconciledTransaction();
      assumeNotNull(otherAccountTransaction);
      FIN_Reconciliation reconciliation = findEarlierReconciliationOfAnotherAccount(
          otherAccountTransaction);
      assumeNotNull(reconciliation);
      FIN_BankStatement template = findProcessedBankStatement(reconciliation);
      assumeNotNull(template);

      FIN_BankStatement statement = createBankStatement(template);
      FIN_BankStatementLine inactiveLine = createLine(statement, reconciliation, 10L,
          INACTIVE_LINE_REFERENCE, null);
      inactiveLine.setActive(false);
      createLine(statement, reconciliation, 20L, OTHER_ACCOUNT_LINE_REFERENCE,
          otherAccountTransaction);
      OBDal.getInstance().flush();
      statement.setProcessed(true);
      OBDal.getInstance().flush();

      List<Object[]> detail = MatchTransactionDao.getUnreconciledBankStatementLines(reconciliation);
      List<String> references = new ArrayList<>();
      for (Object[] row : detail) {
        references.add((String) row[2]);
      }
      assertTrue(references.contains(INACTIVE_LINE_REFERENCE));
      assertTrue(references.contains(OTHER_ACCOUNT_LINE_REFERENCE));
      assertEquals(toKeys(getLegacyDetail(reconciliation)), toKeys(detail));
      assertEquals(getLegacyUnreconciledAmount(reconciliation),
          MatchTransactionDao.getUnreconciledBankStatementLinesAmount(reconciliation));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Compares the new and the legacy implementations for every reconciliation readable by the
   * current role.
   */
  private void assertParityForAllReconciliations() {
    List<FIN_Reconciliation> reconciliations;
    OBContext.setAdminMode(true);
    try {
      reconciliations = OBDal.getInstance().createCriteria(FIN_Reconciliation.class).list();
    } finally {
      OBContext.restorePreviousMode();
    }
    assumeFalse(reconciliations.isEmpty());
    for (FIN_Reconciliation reconciliation : reconciliations) {
      String message = "Reconciliation " + reconciliation.getId();
      OBContext.setAdminMode(true);
      try {
        assertEquals(message, toKeys(getLegacyDetail(reconciliation)),
            toKeys(MatchTransactionDao.getUnreconciledBankStatementLines(reconciliation)));
        assertEquals(message, getLegacyUnreconciledAmount(reconciliation),
            MatchTransactionDao.getUnreconciledBankStatementLinesAmount(reconciliation));
      } finally {
        OBContext.restorePreviousMode();
      }
      OBContext.setAdminMode(false);
      try {
        assertEquals(message, getLegacyBankStatementLinesAmount(reconciliation),
            getBankStatementLinesAmount(reconciliation));
      } finally {
        OBContext.restorePreviousMode();
      }
    }
  }

  /**
   * Converts the rows of the detail to comparable keys, keeping their order. Rows with the same
   * date, description and amount have no guaranteed order, neither in the legacy query nor in the
   * new one, so the reference is not part of the sort key and those rows are sorted by reference.
   *
   * @param rows
   *          the rows of the detail: date, description, reference and amount
   * @return one key per row
   */
  private List<String> toKeys(List<Object[]> rows) {
    List<String> keys = new ArrayList<>();
    for (Object[] row : rows) {
      keys.add(((Date) row[0]).getTime() + "|" + row[1] + "|"
          + ((BigDecimal) row[3]).stripTrailingZeros().toPlainString() + "|" + row[2]);
    }
    for (int i = 0; i < keys.size(); i++) {
      int end = i;
      while (end + 1 < keys.size() && sortKey(keys.get(end + 1)).equals(sortKey(keys.get(i)))) {
        end++;
      }
      Collections.sort(keys.subList(i, end + 1));
      i = end;
    }
    return keys;
  }

  /**
   * Returns the part of a row key used to sort the detail: date, description and amount.
   *
   * @param key
   *          the row key
   * @return the sort key
   */
  private String sortKey(String key) {
    return key.substring(0, key.lastIndexOf('|'));
  }

  /**
   * Runs the legacy SQL query of the UnreconciledBankStatement subreport.
   *
   * @param reconciliation
   *          the reconciliation
   * @return the rows of the detail: date, description, reference and amount
   */
  @SuppressWarnings("unchecked")
  private List<Object[]> getLegacyDetail(FIN_Reconciliation reconciliation) {
    return OBDal.getInstance()
        .getSession()
        .createNativeQuery(LEGACY_DETAIL_SQL)
        .setParameter("reconciliationId", reconciliation.getId())
        .list();
  }

  /**
   * Calculates the report total with the legacy criteria of the report.
   *
   * @param reconciliation
   *          the reconciliation
   * @return the net amount of the un-reconciled bank statement lines
   */
  private BigDecimal getLegacyUnreconciledAmount(FIN_Reconciliation reconciliation) {
    OBCriteria<FIN_BankStatementLine> criteria = OBDal.getInstance()
        .createCriteria(FIN_BankStatementLine.class);
    criteria.createAlias(FIN_BankStatementLine.PROPERTY_BANKSTATEMENT, "bs");
    criteria.createAlias(FIN_BankStatementLine.PROPERTY_FINANCIALACCOUNTTRANSACTION, "tr",
        JoinType.LEFT_OUTER_JOIN);
    criteria.add(Restrictions.le(FIN_BankStatementLine.PROPERTY_TRANSACTIONDATE,
        reconciliation.getEndingDate()));
    List<FIN_Reconciliation> afterReconciliations = MatchTransactionDao
        .getReconciliationListAfterDate(reconciliation);
    if (afterReconciliations.isEmpty()) {
      criteria.add(Restrictions.isNull(FIN_BankStatementLine.PROPERTY_FINANCIALACCOUNTTRANSACTION));
    } else {
      criteria.add(Restrictions.or(
          Restrictions.isNull(FIN_BankStatementLine.PROPERTY_FINANCIALACCOUNTTRANSACTION),
          Restrictions.in("tr." + FIN_FinaccTransaction.PROPERTY_RECONCILIATION,
              afterReconciliations)));
    }
    criteria.add(
        Restrictions.eq("bs." + FIN_BankStatement.PROPERTY_ACCOUNT, reconciliation.getAccount()));
    criteria.add(Restrictions.eq("bs." + FIN_BankStatement.PROPERTY_PROCESSED, true));
    return sumCreditMinusDebit(criteria);
  }

  /**
   * Calculates the bank statement lines amount of the ending balance with the legacy criteria.
   *
   * @param reconciliation
   *          the reconciliation
   * @return the net amount of the bank statement lines
   */
  private BigDecimal getLegacyBankStatementLinesAmount(FIN_Reconciliation reconciliation) {
    OBCriteria<FIN_BankStatementLine> criteria = OBDal.getInstance()
        .createCriteria(FIN_BankStatementLine.class);
    criteria.createAlias(FIN_BankStatementLine.PROPERTY_BANKSTATEMENT, "bs");
    criteria.createAlias(FIN_BankStatementLine.PROPERTY_FINANCIALACCOUNTTRANSACTION, "tr",
        JoinType.LEFT_OUTER_JOIN);
    criteria.add(
        Restrictions.eq("bs." + FIN_BankStatement.PROPERTY_ACCOUNT, reconciliation.getAccount()));
    criteria.add(Restrictions.eq("bs." + FIN_BankStatement.PROPERTY_PROCESSED, true));
    criteria.add(Restrictions.le(FIN_BankStatementLine.PROPERTY_TRANSACTIONDATE,
        reconciliation.getEndingDate()));
    return sumCreditMinusDebit(criteria);
  }

  /**
   * Sums the credit and debit amounts of the lines of a criteria, as the legacy code did.
   *
   * @param criteria
   *          the criteria over the bank statement lines
   * @return the sum of the credit amounts minus the sum of the debit amounts
   */
  private BigDecimal sumCreditMinusDebit(OBCriteria<FIN_BankStatementLine> criteria) {
    ProjectionList projections = Projections.projectionList();
    projections.add(Projections.sum(FIN_BankStatementLine.PROPERTY_CRAMOUNT));
    projections.add(Projections.sum(FIN_BankStatementLine.PROPERTY_DRAMOUNT));
    criteria.setProjection(projections);
    BigDecimal total = BigDecimal.ZERO;
    List<?> result = criteria.list();
    if (result != null && !result.isEmpty()) {
      Object[] sums = (Object[]) result.get(0);
      BigDecimal credit = (sums[0] != null) ? (BigDecimal) sums[0] : BigDecimal.ZERO;
      BigDecimal debit = (sums[1] != null) ? (BigDecimal) sums[1] : BigDecimal.ZERO;
      total = credit.subtract(debit);
    }
    return total;
  }

  /**
   * Invokes the private method that calculates the bank statement lines amount of the ending
   * balance.
   *
   * @param reconciliation
   *          the reconciliation
   * @return the net amount of the bank statement lines
   */
  private BigDecimal getBankStatementLinesAmount(FIN_Reconciliation reconciliation) {
    try {
      Method method = MatchTransactionDao.class.getDeclaredMethod("getBSLAmount",
          FIN_Reconciliation.class);
      method.setAccessible(true);
      return (BigDecimal) method.invoke(null, reconciliation);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns a transaction that belongs to a reconciliation, the most recently created first.
   *
   * @return the transaction, or {@code null} if there is none
   */
  private FIN_FinaccTransaction findReconciledTransaction() {
    OBQuery<FIN_FinaccTransaction> query = OBDal.getInstance()
        .createQuery(FIN_FinaccTransaction.class,
            "as t where t.reconciliation is not null order by t.reconciliation.creationDate desc");
    query.setMaxResult(1);
    return query.uniqueResult();
  }

  /**
   * Returns a reconciliation of another financial account created before the reconciliation of
   * the given transaction.
   *
   * @param transaction
   *          the reconciled transaction
   * @return the reconciliation, or {@code null} if there is none
   */
  private FIN_Reconciliation findEarlierReconciliationOfAnotherAccount(
      FIN_FinaccTransaction transaction) {
    OBQuery<FIN_Reconciliation> query = OBDal.getInstance()
        .createQuery(FIN_Reconciliation.class,
            "as r where r.account.id <> :account and r.creationDate < :created"
                + " order by r.creationDate desc");
    query.setNamedParameter("account", transaction.getAccount().getId());
    query.setNamedParameter("created", transaction.getReconciliation().getCreationDate());
    query.setMaxResult(1);
    return query.uniqueResult();
  }

  /**
   * Returns a processed bank statement of the financial account of the reconciliation, used as a
   * template for the organization and the document type of the test bank statement.
   *
   * @param reconciliation
   *          the reconciliation
   * @return the bank statement, or {@code null} if there is none
   */
  private FIN_BankStatement findProcessedBankStatement(FIN_Reconciliation reconciliation) {
    OBQuery<FIN_BankStatement> query = OBDal.getInstance()
        .createQuery(FIN_BankStatement.class, "as bs where bs.account.id = :account"
            + " and bs.processed = true");
    query.setNamedParameter("account", reconciliation.getAccount().getId());
    query.setMaxResult(1);
    return query.uniqueResult();
  }

  /**
   * Creates a bank statement that is not processed yet, so lines can be added to it.
   *
   * @param template
   *          the processed bank statement whose organization, account and document type are used
   * @return the new bank statement
   */
  private FIN_BankStatement createBankStatement(FIN_BankStatement template) {
    FIN_BankStatement statement = OBProvider.getInstance().get(FIN_BankStatement.class);
    statement.setClient(template.getClient());
    statement.setOrganization(template.getOrganization());
    statement.setAccount(template.getAccount());
    statement.setDocumentType(template.getDocumentType());
    statement.setDocumentNo("ETP5581-TEST");
    statement.setName("ETP-5581 parity test");
    statement.setImportdate(new Date());
    statement.setTransactionDate(new Date());
    statement.setProcessed(false);
    OBDal.getInstance().save(statement);
    return statement;
  }

  /**
   * Creates a bank statement line dated on the ending date of the reconciliation.
   *
   * @param statement
   *          the bank statement
   * @param reconciliation
   *          the reconciliation whose ending date is used
   * @param lineNo
   *          the line number
   * @param reference
   *          the reference number, used to find the line in the results
   * @param transaction
   *          the transaction the line is matched to, or {@code null}
   * @return the new line
   */
  private FIN_BankStatementLine createLine(FIN_BankStatement statement,
      FIN_Reconciliation reconciliation, long lineNo, String reference,
      FIN_FinaccTransaction transaction) {
    FIN_BankStatementLine line = OBProvider.getInstance().get(FIN_BankStatementLine.class);
    line.setClient(statement.getClient());
    line.setOrganization(statement.getOrganization());
    line.setBankStatement(statement);
    line.setLineNo(lineNo);
    line.setTransactionDate(reconciliation.getEndingDate());
    line.setReferenceNo(reference);
    line.setCramount(new BigDecimal("1.23"));
    line.setDramount(BigDecimal.ZERO);
    line.setFinancialAccountTransaction(transaction);
    OBDal.getInstance().save(line);
    return line;
  }
}
