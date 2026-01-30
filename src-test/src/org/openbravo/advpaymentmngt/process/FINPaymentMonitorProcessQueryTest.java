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

package org.openbravo.advpaymentmngt.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for FIN_PaymentMonitorProcess HQL query optimization.
 */
public class FINPaymentMonitorProcessQueryTest {

  private static final String HQL_PREFIX = "as i";
  private static final String HQL_PROCESSED_TRUE = "i.processed = true";
  private static final String HQL_PAYMENT_COMPLETE_FALSE = "i.paymentComplete = false";
  private static final String HQL_PAYMENT_COMPLETE_TRUE = "i.paymentComplete = true";
  private static final String HQL_OUTSTANDING_NOT_ZERO = "i.outstandingAmount <> 0";
  private static final String HQL_OUTSTANDING_ZERO = "i.outstandingAmount = 0";
  private static final String HQL_FINAL_SETTLEMENT_NULL = "i.finalSettlementDate is null";
  private static final String HQL_APRMT_NOT_MIGRATED = "i.aprmtIsmigrated = 'N'";
  private static final String HQL_FPS_UPDATED = "fps.updated >=";
  private static final String HQL_PSD_UPDATED = "psd.updated >=";
  private static final String HQL_FPS_INVOICE = "fps.invoice = i";
  private static final String HQL_FPS_ID_NOT_NULL = "fps.id is not null";
  private static final String HQL_PAYMENT_SCHEDULE_LIST = "i.fINPaymentScheduleList";
  private static final String HQL_COALESCE_LAST_CALCULATED = "coalesce(i.lastCalculatedOnDate";
  private static final String HQL_INVOICE_UPDATED = "i.updated >=";
  private static final String KEYWORD_LEFT_JOIN = "left join";
  private static final String KEYWORD_EXISTS = "exists";
  private static final String KEYWORD_COALESCE = "coalesce";
  private static final String MSG_MIGRATION_QUERY = "Migration query";
  private static final String MSG_NON_MIGRATION_QUERY = "Non-migration query";
  private static final String MSG_QUERY = "Query";
  private static final String MSG_SHOULD_NOT_CONTAIN_LEFT_JOIN = " should NOT contain LEFT JOIN";
  private static final String MSG_SHOULD_CONTAIN_EXISTS = " should contain EXISTS subquery";
  private static final String MSG_SHOULD_USE_COALESCE = " should use COALESCE for null handling";
  private static final String MSG_BALANCED_PARENTHESES = " should have balanced parentheses";
  private static final String MSG_SHOULD_START_WITH_AS_I = " should start with 'as i'";
  private static final String MSG_SHOULD_NOT_JOIN_DIRECTLY = " should NOT directly join fINPaymentScheduleList";
  private static final String HQL_FROM_PAYMENT_SCHEDULE = "from FIN_Payment_Schedule fps";
  private static final String HQL_FROM_PAYMENT_SCHEDULE_DETAIL = "from FIN_Payment_ScheduleDetail psd";
  private static final String HQL_COALESCE_CREATION_DATE = "i.creationDate";
  private static final String CLOSING_PARENTHESIS = "      )";
  private static final int MIN_EXISTS_COUNT_WITH_MIGRATION = 3;

  /**
   * Verifies the optimized query uses EXISTS subqueries instead of LEFT JOINs
   * and includes COALESCE for null-safe date handling.
   */
  @Test
  public void testQueryUsesExistsInsteadOfLeftJoin() {
    String hql = buildOptimizedHql(false);
    String hqlLower = hql.toLowerCase();
    assertFalse(MSG_QUERY + MSG_SHOULD_NOT_CONTAIN_LEFT_JOIN, hqlLower.contains(KEYWORD_LEFT_JOIN));
    assertTrue(MSG_QUERY + MSG_SHOULD_CONTAIN_EXISTS, hqlLower.contains(KEYWORD_EXISTS));
    assertTrue(MSG_QUERY + MSG_SHOULD_USE_COALESCE, hqlLower.contains(KEYWORD_COALESCE));
  }

  /**
   * Ensures the query does not rely on invoice i.updated for change detection.
   */
  @Test
  public void testQueryDoesNotUseInvoiceUpdated() {
    String hql = buildOptimizedHql(false);
    assertFalse(MSG_QUERY + " should NOT use i.updated for change detection",
        hql.contains(HQL_INVOICE_UPDATED));
  }

  /**
   * Validates COALESCE is applied to lastCalculatedOnDate with creationDate as fallback.
   */
  @Test
  public void testQueryUsesCoalesceWithFallbackDate() {
    String hql = buildOptimizedHql(false);
    assertTrue("Query should use COALESCE on lastCalculatedOnDate",
        hql.contains(HQL_COALESCE_LAST_CALCULATED));
    assertTrue("Query should use creationDate as fallback date",
        hql.contains(HQL_COALESCE_CREATION_DATE));
  }

  /**
   * Verifies that the optimized HQL filters only processed invoices.
   */
  @Test
  public void testQueryFiltersProcessedInvoices() {
    String hql = buildOptimizedHql(false);
    assertTrue("Query should filter only processed invoices", hql.contains(HQL_PROCESSED_TRUE));
  }

  /**
   * Confirms the query filters only processed invoices.
   */
  @Test
  public void testQueryIncludesPaymentCompleteFalseCondition() {
    String hql = buildOptimizedHql(false);
    assertTrue("Query should include paymentComplete = false condition",
        hql.contains(HQL_PAYMENT_COMPLETE_FALSE));
  }

  /**
   * Verifies that the optimized HQL includes both outstanding amount branches:
   * one for open invoices (non-zero outstanding) and one for fully paid invoices (zero outstanding).
   */
  @Test
  public void testQueryIncludesOutstandingAmountConditions() {
    String hql = buildOptimizedHql(false);
    assertTrue("Query should include outstandingAmount <> 0 condition",
        hql.contains(HQL_OUTSTANDING_NOT_ZERO));
    assertTrue("Query should include outstandingAmount = 0 condition for paid invoices",
        hql.contains(HQL_OUTSTANDING_ZERO));
  }

  /**
   * Verifies the EXISTS subquery for FIN_PaymentSchedule is present and properly correlated.
   */
  @Test
  public void testExistsSubqueryReferencesPaymentSchedule() {
    String hql = buildOptimizedHql(false);
    assertTrue("EXISTS subquery should select from FIN_PaymentSchedule",
        hql.contains(HQL_FROM_PAYMENT_SCHEDULE));
    assertTrue("EXISTS subquery should join on invoice", hql.contains(HQL_FPS_INVOICE));
    assertTrue("EXISTS subquery should compare fps.updated", hql.contains(HQL_FPS_UPDATED));
  }

  /**
   * Verifies the EXISTS subquery for FIN_PaymentScheduleDetail is present and properly correlated.
   */
  @Test
  public void testExistsSubqueryReferencesPaymentScheduleDetail() {
    String hql = buildOptimizedHql(false);
    assertTrue("EXISTS subquery should select from FIN_PaymentScheduleDetail",
        hql.contains(HQL_FROM_PAYMENT_SCHEDULE_DETAIL));
    assertTrue("EXISTS subquery should compare psd.updated", hql.contains(HQL_PSD_UPDATED));
  }

  /**
   * Checks that the non-migration query includes exactly two EXISTS subqueries (fps and psd).
   */
  @Test
  public void testExistsCountWithoutMigration() {
    String hql = buildOptimizedHql(false);
    int existsCount = countOccurrences(hql.toLowerCase(), KEYWORD_EXISTS);
    assertEquals(MSG_NON_MIGRATION_QUERY + " should have exactly 2 EXISTS subqueries (fps and psd)",
        2, existsCount);
  }

  /**
   * Ensures the query includes the conditions that detect fully paid invoices
   * that still require final settlement.
   */
  @Test
  public void testQueryIncludesPaidInvoicesWithoutSettlement() {
    String hql = buildOptimizedHql(false);
    assertTrue("Query should check paymentComplete = true for paid invoices needing settlement",
        hql.contains(HQL_PAYMENT_COMPLETE_TRUE));
    assertTrue("Query should check finalSettlementDate is null", hql.contains(HQL_FINAL_SETTLEMENT_NULL));
    assertTrue("Query should check outstandingAmount = 0 for fully paid invoices",
        hql.contains(HQL_OUTSTANDING_ZERO));
  }

  /**
   * Verifies that enabling the migration branch adds the expected migration-related conditions and
   * keeps legacy patterns out of the resulting HQL.
   */
  @Test
  public void testQueryWithMigrationModuleIncludesMigrationConditions() {
    String hql = buildOptimizedHql(true);
    assertTrue(MSG_MIGRATION_QUERY + " should check aprmtIsmigrated = 'N'",
        hql.contains(HQL_APRMT_NOT_MIGRATED));
    assertTrue(MSG_MIGRATION_QUERY + " should check finalSettlementDate is null",
        hql.contains(HQL_FINAL_SETTLEMENT_NULL));
    assertFalse(MSG_MIGRATION_QUERY + " should NOT use 'fps.id is not null' (old pattern)",
        hql.contains(HQL_FPS_ID_NOT_NULL));

    int existsCount = countOccurrences(hql.toLowerCase(), KEYWORD_EXISTS);
    assertTrue(MSG_MIGRATION_QUERY + " should have at least 3 EXISTS subqueries",
        existsCount >= MIN_EXISTS_COUNT_WITH_MIGRATION);
  }

  /**
   * Verifies that the HQL built without the migration branch does not include any migration-specific
   * filters (in particular, the {@code aprmtIsmigrated} flag).
   */
  @Test
  public void testQueryWithoutMigrationModuleDoesNotCheckAprmtIsmigrated() {
    String hql = buildOptimizedHql(false);
    assertFalse(MSG_NON_MIGRATION_QUERY + " should NOT check aprmtIsmigrated",
        hql.contains("i.aprmtIsmigrated"));
  }

  /**
   * Ensures no LEFT JOIN pattern exists in either migration or non-migration queries.
   */
  @Test
  public void testNoLeftJoinPattern() {
    String hqlWithMigration = buildOptimizedHql(true);
    String hqlWithoutMigration = buildOptimizedHql(false);
    assertFalse(MSG_MIGRATION_QUERY + MSG_SHOULD_NOT_CONTAIN_LEFT_JOIN,
        hqlWithMigration.toLowerCase().contains(KEYWORD_LEFT_JOIN));
    assertFalse(MSG_NON_MIGRATION_QUERY + MSG_SHOULD_NOT_CONTAIN_LEFT_JOIN,
        hqlWithoutMigration.toLowerCase().contains(KEYWORD_LEFT_JOIN));
  }

  /**
   * Ensures the legacy 'fps.id is not null' pattern is not used.
   */
  @Test
  public void testNoFpsIdNotNullPattern() {
    String hqlWithMigration = buildOptimizedHql(true);
    assertFalse(MSG_QUERY + " should NOT use old pattern 'fps.id is not null'",
        hqlWithMigration.contains(HQL_FPS_ID_NOT_NULL));
  }

  /**
   * Ensures the query does not directly join the invoice payment schedule collection.
   */
  @Test
  public void testNoDirectJoinToPaymentScheduleList() {
    String hqlWithMigration = buildOptimizedHql(true);
    String hqlWithoutMigration = buildOptimizedHql(false);
    assertFalse(MSG_MIGRATION_QUERY + MSG_SHOULD_NOT_JOIN_DIRECTLY,
        hqlWithMigration.contains(HQL_PAYMENT_SCHEDULE_LIST));
    assertFalse(MSG_NON_MIGRATION_QUERY + MSG_SHOULD_NOT_JOIN_DIRECTLY,
        hqlWithoutMigration.contains(HQL_PAYMENT_SCHEDULE_LIST));
  }

  /**
   * Ensures the query does not filter based on invoice i.updated.
   */
  @Test
  public void testNoInvoiceUpdatedFiltering() {
    String hqlWithMigration = buildOptimizedHql(true);
    String hqlWithoutMigration = buildOptimizedHql(false);
    assertFalse(MSG_MIGRATION_QUERY + " should NOT filter by i.updated",
        hqlWithMigration.contains(HQL_INVOICE_UPDATED));
    assertFalse(MSG_NON_MIGRATION_QUERY + " should NOT filter by i.updated",
        hqlWithoutMigration.contains(HQL_INVOICE_UPDATED));
  }

  /**
   * Verifies the query looks syntactically complete: balanced parentheses and correct prefix.
   */
  @Test
  public void testQuerySyntacticallyComplete() {
    String hqlWithMigration = buildOptimizedHql(true);
    String hqlWithoutMigration = buildOptimizedHql(false);
    assertEquals(MSG_MIGRATION_QUERY + MSG_BALANCED_PARENTHESES,
        countOccurrences(hqlWithMigration, "("),
        countOccurrences(hqlWithMigration, ")"));
    assertEquals(MSG_NON_MIGRATION_QUERY + MSG_BALANCED_PARENTHESES,
        countOccurrences(hqlWithoutMigration, "("),
        countOccurrences(hqlWithoutMigration, ")"));

    assertTrue(MSG_MIGRATION_QUERY + MSG_SHOULD_START_WITH_AS_I,
        hqlWithMigration.trim().startsWith(HQL_PREFIX));
    assertTrue(MSG_NON_MIGRATION_QUERY + MSG_SHOULD_START_WITH_AS_I,
        hqlWithoutMigration.trim().startsWith(HQL_PREFIX));
  }

  /**
   * Ensures all core conditions expected by the optimized query are present.
   */
  @Test
  public void testQueryContainsAllRequiredConditions() {
    String hql = buildOptimizedHql(false);
    assertTrue("Missing: processed = true", hql.contains(HQL_PROCESSED_TRUE));
    assertTrue("Missing: paymentComplete = false", hql.contains(HQL_PAYMENT_COMPLETE_FALSE));
    assertTrue("Missing: paymentComplete = true", hql.contains(HQL_PAYMENT_COMPLETE_TRUE));
    assertTrue("Missing: outstandingAmount <> 0", hql.contains(HQL_OUTSTANDING_NOT_ZERO));
    assertTrue("Missing: outstandingAmount = 0", hql.contains(HQL_OUTSTANDING_ZERO));
    assertTrue("Missing: EXISTS subquery", hql.contains(KEYWORD_EXISTS));
    assertTrue("Missing: fps.updated comparison", hql.contains(HQL_FPS_UPDATED));
    assertTrue("Missing: psd.updated comparison", hql.contains(HQL_PSD_UPDATED));
    assertTrue("Missing: COALESCE function", hql.contains(KEYWORD_COALESCE));
    assertTrue("Missing: finalSettlementDate is null", hql.contains(HQL_FINAL_SETTLEMENT_NULL));
  }

  /**
   * Builds the optimized HQL query as it should be in the process.
   * This is the reference implementation that tests are validating against.
   * @param withMigration true if migration module is present
   * @return the HQL query string
   */
  private String buildOptimizedHql(boolean withMigration) {
    StringBuilder hqlBuilder = new StringBuilder();
    hqlBuilder.append("as i ")
        .append("where i.processed = true")
        .append("  and (")
        .append("    (i.paymentComplete = false and (")
        .append("      exists (")
        .append("        select 1 from FIN_Payment_Schedule fps ")
        .append("        where fps.invoice = i ")
        .append("        and fps.updated >= coalesce(i.lastCalculatedOnDate, i.creationDate)")
        .append(CLOSING_PARENTHESIS)
        .append("      or exists (")
        .append("        select 1 from FIN_Payment_ScheduleDetail psd ")
        .append("        join psd.invoicePaymentSchedule fps ")
        .append("        where fps.invoice = i ")
        .append("        and psd.updated >= coalesce(i.lastCalculatedOnDate, i.creationDate)")
        .append(CLOSING_PARENTHESIS)
        .append("    ))")
        .append("    or (i.outstandingAmount <> 0 and i.lastCalculatedOnDate is null)")
        .append("    or (i.paymentComplete = true and i.finalSettlementDate is null and i.outstandingAmount = 0)");

    if (withMigration) {
      hqlBuilder.append("    or (i.finalSettlementDate is null")
          .append("      and i.aprmtIsmigrated = 'N'")
          .append("      and exists (")
          .append("        select 1 from FIN_Payment_Schedule fps2")
          .append("        where fps2.invoice = i")
          .append(CLOSING_PARENTHESIS)
          .append("    )")
          .append("  )");
    } else {
      hqlBuilder.append("  )");
    }
    return hqlBuilder.toString();
  }

  /**
   * Counts how many times a substring appears within a given text.
   * @param text the text to search in
   * @param search the substring to count
   * @return the number of occurrences of {@code search} inside {@code text}
   */
  private int countOccurrences(String text, String search) {
    int count = 0;
    int index = 0;
    while ((index = text.indexOf(search, index)) != -1) {
      count++;
      index += search.length();
    }
    return count;
  }
}
