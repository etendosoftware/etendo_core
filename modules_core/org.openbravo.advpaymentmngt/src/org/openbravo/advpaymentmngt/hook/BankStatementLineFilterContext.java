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
package org.openbravo.advpaymentmngt.hook;

import java.util.Objects;

import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;

/**
 * Information passed to every {@link BankStatementLineFilterHook} about the query being built.
 * Instances are immutable.
 */
public class BankStatementLineFilterContext {

  /**
   * Identifies the process that builds the query where the hook condition is applied. New values
   * may be added in the future, so implementations should not fail on unknown values.
   */
  public enum Usage {
    /** Bank statement lines processed by the automatic matching algorithm. */
    AUTO_MATCH_CANDIDATES,
    /** Check performed before the automatic matching to verify there are lines left to match. */
    UNMATCHED_CHECK,
    /** Lines shown in the Match Statement grid. */
    MATCH_STATEMENT_GRID,
    /** Sum of the bank statement lines used to calculate the reconciliation ending balance. */
    ENDING_BALANCE,
    /** Latest bank statement line date, used as the reconciliation ending date. */
    MAX_DATE,
    /** Un-reconciled bank statement lines shown in the reconciliation report. */
    RECONCILIATION_REPORT
  }

  private final FIN_FinancialAccount account;
  private final FIN_Reconciliation reconciliation;
  private final Usage usage;

  /**
   * Creates a new context.
   *
   * @param account
   *          the financial account whose bank statement lines are queried, never {@code null}
   * @param reconciliation
   *          the reconciliation being processed, or {@code null} if the query is not related to a
   *          specific reconciliation
   * @param usage
   *          the process that builds the query, never {@code null}
   */
  public BankStatementLineFilterContext(FIN_FinancialAccount account,
      FIN_Reconciliation reconciliation, Usage usage) {
    this.account = Objects.requireNonNull(account, "account is required");
    this.reconciliation = reconciliation;
    this.usage = Objects.requireNonNull(usage, "usage is required");
  }

  /**
   * Returns the financial account whose bank statement lines are queried.
   *
   * @return the financial account, never {@code null}
   */
  public FIN_FinancialAccount getAccount() {
    return account;
  }

  /**
   * Returns the reconciliation being processed.
   *
   * @return the reconciliation, or {@code null} if the query is not related to a specific
   *         reconciliation
   */
  public FIN_Reconciliation getReconciliation() {
    return reconciliation;
  }

  /**
   * Returns the process that builds the query.
   *
   * @return the usage, never {@code null}
   */
  public Usage getUsage() {
    return usage;
  }
}
