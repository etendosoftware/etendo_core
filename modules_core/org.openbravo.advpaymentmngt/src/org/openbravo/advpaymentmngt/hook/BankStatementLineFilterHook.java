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

import java.util.Map;

/**
 * Extension point that allows any module to exclude bank statement lines from the matching and
 * reconciliation logic of the Advanced Payables and Receivables Management module.
 * <p>
 * Each implementation returns an HQL boolean condition that a {@code FIN_BankStatementLine} must
 * <b>satisfy</b> to take part in the process. The conditions of all the implementations are
 * combined with {@code AND}, so a line is excluded as soon as one hook rejects it. The condition is
 * applied to:
 * <ul>
 * <li>the candidate lines of the automatic matching algorithm;</li>
 * <li>the "nothing left to match" check of Match Statement;</li>
 * <li>the Match Statement grid;</li>
 * <li>the ending balance and the ending date of the reconciliation;</li>
 * <li>the un-reconciled bank statement lines of the reconciliation report.</li>
 * </ul>
 * The {@link BankStatementLineFilterContext.Usage} received in the context identifies each of these
 * cases.
 * <p>
 * <b>Rules for the implementations:</b>
 * <ul>
 * <li>They must be CDI beans annotated with {@code @ApplicationScoped}. They are discovered through
 * {@code WeldUtils.getInstances}, so the module must provide a {@code beans.xml}.</li>
 * <li>The condition must reference the line only through the alias received as parameter, never
 * through a hardcoded alias.</li>
 * <li>Nullable properties must be handled explicitly, for example with
 * {@code coalesce(alias.myFlag, false) = false}: in HQL a comparison against {@code null} is never
 * true, so a line with a {@code null} value would be excluded unintentionally.</li>
 * <li>Any parameter used by the condition must be added to the {@code namedParameters} map with a
 * module-prefixed key (for example {@code psd2_status}). A key that is already in use makes the
 * query building fail.</li>
 * <li>An excluded line must never be matched to a financial account transaction. A matched line
 * that is excluded would still be counted as a manually reconciled transaction when the ending
 * balance is calculated, leading to inconsistent balances.</li>
 * </ul>
 * <p>
 * <b>Known limitation:</b> the Match Statement grid applies the condition from
 * {@code MatchStatementTransformer}. The HQL datasource only uses one transformer per table, so if
 * a module replaces that transformer with a higher priority one, the condition will not be applied
 * to the grid unless the replacement also calls {@code BankStatementLineFilterHelper}.
 *
 * @see org.openbravo.advpaymentmngt.utility.BankStatementLineFilterHelper
 */
public interface BankStatementLineFilterHook {

  /**
   * Returns the priority of the hook. Hooks are evaluated in ascending order of priority, which
   * makes the generated HQL deterministic.
   *
   * @return the priority of the hook, 100 by default
   */
  default int getPriority() {
    return 100;
  }

  /**
   * Returns the HQL boolean condition that a bank statement line must satisfy to be taken into
   * account.
   *
   * @param bslAlias
   *          the alias of the {@code FIN_BankStatementLine} entity in the query where the condition
   *          is applied
   * @param context
   *          information about the financial account, the reconciliation and the process that
   *          builds the query
   * @param namedParameters
   *          an empty map where the hook must put the named parameters used by its condition, using
   *          module-prefixed keys
   * @return the HQL condition, without a leading {@code and}. A {@code null} or blank value means
   *         that the hook does not restrict the lines
   */
  String getHqlCondition(String bslAlias, BankStatementLineFilterContext context,
      Map<String, Object> namedParameters);
}
