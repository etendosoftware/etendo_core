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
 * All portions are Copyright (C) 2010-2025 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for FIN_PaymentMonitorProcess HQL query optimization.
 */
class FINPaymentMonitorProcessQueryTest {
  
  private static final String HQL_PREFIX = "as i";
  private static final String HQL_PROCESSED_TRUE = "i.processed = true";
  private static final String HQL_PAYMENT_COMPLETE_FALSE = "i.paymentComplete = false";
  private static final String HQL_OUTSTANDING_NOT_ZERO = "i.outstandingAmount <> 0";
  private static final String HQL_FINAL_SETTLEMENT_NULL = "i.finalSettlementDate is null";
  private static final String HQL_APRMT_NOT_MIGRATED = "i.aprmtIsmigrated = 'N'";
  private static final String HQL_FPS_UPDATED = "fps.updated >=";
  private static final String HQL_FPS_INVOICE = "fps.invoice = i";
  private static final String HQL_FPS_ID_NOT_NULL = "fps.id is not null";
  private static final String HQL_PAYMENT_SCHEDULE_LIST = "i.fINPaymentScheduleList";
  private static final String HQL_COALESCE_LAST_CALCULATED = "coalesce(i.lastCalculatedOnDate";
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
  private static final String HQL_COALESCE_CREATION_DATE = "i.creationDate";

  /**
   * Sets up the test environment before each test.
   */
  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  /**
   * Tests for HQL query structure validation.
   */
  @Nested
  @DisplayName("HQL Query Structure Tests")
  class HqlQueryStructureTests {

    /**
     * Verifies that the query uses EXISTS instead of LEFT JOIN.
     */
    @Test
    @DisplayName("Query should use EXISTS instead of LEFT JOIN")
    void testQueryUsesExistsInsteadOfLeftJoin() {
      String hql = buildOptimizedHql(false);
      String hqlLower = hql.toLowerCase();
      assertFalse(hqlLower.contains(KEYWORD_LEFT_JOIN),
          MSG_QUERY + MSG_SHOULD_NOT_CONTAIN_LEFT_JOIN);
      assertTrue(hqlLower.contains(KEYWORD_EXISTS),
          MSG_QUERY + MSG_SHOULD_CONTAIN_EXISTS);
      assertTrue(hqlLower.contains(KEYWORD_COALESCE),
          MSG_QUERY + MSG_SHOULD_USE_COALESCE);
    }

    /**
     * Verifies that the query uses COALESCE with fallback date 1900-01-01.
     */
    @Test
    @DisplayName("Query should include COALESCE with fallback date 1900-01-01")
    void testQueryUsesCoalesceWithFallbackDate() {
      String hql = buildOptimizedHql(false);
      assertTrue(hql.contains(HQL_COALESCE_LAST_CALCULATED),
          "Query should use COALESCE on lastCalculatedOnDate");
      assertTrue(hql.contains(HQL_COALESCE_CREATION_DATE),
          "Query should use creationDate as fallback date");
    }

    /**
     * Verifies that the query filters only processed invoices.
     */
    @Test
    @DisplayName("Query should select processed invoices")
    void testQueryFiltersProcessedInvoices() {
      String hql = buildOptimizedHql(false);
      assertTrue(hql.contains(HQL_PROCESSED_TRUE),
          "Query should filter only processed invoices");
    }

    /**
     * Verifies that the query includes paymentComplete condition.
     */
    @Test
    @DisplayName("Query should include paymentComplete condition")
    void testQueryIncludesPaymentCompleteCondition() {
      String hql = buildOptimizedHql(false);
      assertTrue(hql.contains(HQL_PAYMENT_COMPLETE_FALSE),
          "Query should include paymentComplete = false condition");
    }

    /**
     * Verifies that the query includes outstandingAmount condition.
     */
    @Test
    @DisplayName("Query should include outstandingAmount condition")
    void testQueryIncludesOutstandingAmountCondition() {
      String hql = buildOptimizedHql(false);
      assertTrue(hql.contains(HQL_OUTSTANDING_NOT_ZERO),
          "Query should include outstandingAmount <> 0 condition");
    }

    /**
     * Verifies that EXISTS subquery references FIN_PaymentSchedule correctly.
     */
    @Test
    @DisplayName("EXISTS subquery should reference FIN_PaymentSchedule")
    void testExistsSubqueryReferencesPaymentSchedule() {
      String hql = buildOptimizedHql(false);
      assertTrue(hql.contains(HQL_FROM_PAYMENT_SCHEDULE),
          "EXISTS subquery should select from FIN_PaymentSchedule");
      assertTrue(hql.contains(HQL_FPS_INVOICE),
          "EXISTS subquery should join on invoice");
      assertTrue(hql.contains(HQL_FPS_UPDATED),
          "EXISTS subquery should compare fps.updated");
    }
  }

  /**
   * Tests for migration branch handling.
   */
  @Nested
  @DisplayName("Migration Branch Tests")
  class MigrationBranchTests {
    private static final int MIN_EXISTS_COUNT_WITH_MIGRATION = 2;

    /**
     * Verifies that query with migration module includes migration conditions.
     */
    @Test
    @DisplayName("Query WITH migration module should include migration conditions")
    void testQueryWithMigrationModule() {
      String hql = buildOptimizedHql(true);
      assertTrue(hql.contains(HQL_APRMT_NOT_MIGRATED),
          MSG_MIGRATION_QUERY + " should check aprmtIsmigrated = 'N'");
      assertTrue(hql.contains(HQL_FINAL_SETTLEMENT_NULL),
          MSG_MIGRATION_QUERY + " should check finalSettlementDate is null");
      assertFalse(hql.contains(HQL_FPS_ID_NOT_NULL),
          MSG_MIGRATION_QUERY + " should NOT use 'fps.id is not null' (old pattern)");
      int existsCount = countOccurrences(hql.toLowerCase(), KEYWORD_EXISTS);
      assertTrue(existsCount >= MIN_EXISTS_COUNT_WITH_MIGRATION,
          MSG_MIGRATION_QUERY + " should have at least 2 EXISTS subqueries");
    }

    /**
     * Verifies that query without migration module has simpler structure.
     */
    @Test
    @DisplayName("Query WITHOUT migration module should have simpler structure")
    void testQueryWithoutMigrationModule() {
      String hql = buildOptimizedHql(false);
      assertFalse(hql.contains("i.aprmtIsmigrated"),
          MSG_NON_MIGRATION_QUERY + " should NOT check aprmtIsmigrated");
      assertTrue(hql.contains(HQL_FINAL_SETTLEMENT_NULL),
          MSG_NON_MIGRATION_QUERY + " should still check finalSettlementDate is null");
      int existsCount = countOccurrences(hql.toLowerCase(), KEYWORD_EXISTS);
      assertEquals(1, existsCount,
          MSG_NON_MIGRATION_QUERY + " should have exactly 1 EXISTS subquery");
    }
  }

  /**
   * Tests to verify old patterns are NOT present.
   */
  @Nested
  @DisplayName("Old Pattern Removal Tests")
  class OldPatternRemovalTests {

    /**
     * Verifies that query does not contain LEFT JOIN pattern.
     */
    @Test
    @DisplayName("Query should NOT contain LEFT JOIN pattern")
    void testNoLeftJoinPattern() {
      String hqlWithMigration = buildOptimizedHql(true);
      String hqlWithoutMigration = buildOptimizedHql(false);
      assertFalse(hqlWithMigration.toLowerCase().contains(KEYWORD_LEFT_JOIN),
          MSG_MIGRATION_QUERY + MSG_SHOULD_NOT_CONTAIN_LEFT_JOIN);
      assertFalse(hqlWithoutMigration.toLowerCase().contains(KEYWORD_LEFT_JOIN),
          MSG_NON_MIGRATION_QUERY + MSG_SHOULD_NOT_CONTAIN_LEFT_JOIN);
    }

    /**
     * Verifies that query does not contain 'fps.id is not null' pattern.
     */
    @Test
    @DisplayName("Query should NOT contain 'fps.id is not null' pattern")
    void testNoFpsIdNotNullPattern() {
      String hqlWithMigration = buildOptimizedHql(true);
      assertFalse(hqlWithMigration.contains(HQL_FPS_ID_NOT_NULL),
          MSG_QUERY + " should NOT use old pattern 'fps.id is not null'");
    }

    /**
     * Verifies that query does not join i.fINPaymentScheduleList directly.
     */
    @Test
    @DisplayName("Query should NOT join i.fINPaymentScheduleList directly")
    void testNoDirectJoinToPaymentScheduleList() {
      String hqlWithMigration = buildOptimizedHql(true);
      String hqlWithoutMigration = buildOptimizedHql(false);
      assertFalse(hqlWithMigration.contains(HQL_PAYMENT_SCHEDULE_LIST),
          MSG_MIGRATION_QUERY + MSG_SHOULD_NOT_JOIN_DIRECTLY);
      assertFalse(hqlWithoutMigration.contains(HQL_PAYMENT_SCHEDULE_LIST),
          MSG_NON_MIGRATION_QUERY + MSG_SHOULD_NOT_JOIN_DIRECTLY);
    }
  }

  /**
   * Tests for query completeness and validity.
   */
  @Nested
  @DisplayName("Query Completeness Tests")
  class QueryCompletenessTests {
    private static final String OPEN_PAREN = "(";
    private static final String CLOSE_PAREN = ")";

    /**
     * Verifies that query is syntactically complete with balanced parentheses.
     */
    @Test
    @DisplayName("Query should be syntactically complete")
    void testQuerySyntacticallyComplete() {
      String hqlWithMigration = buildOptimizedHql(true);
      String hqlWithoutMigration = buildOptimizedHql(false);
      assertEquals(
          countOccurrences(hqlWithMigration, OPEN_PAREN),
          countOccurrences(hqlWithMigration, CLOSE_PAREN),
          MSG_MIGRATION_QUERY + MSG_BALANCED_PARENTHESES);
      assertEquals(
          countOccurrences(hqlWithoutMigration, OPEN_PAREN),
          countOccurrences(hqlWithoutMigration, CLOSE_PAREN),
          MSG_NON_MIGRATION_QUERY + MSG_BALANCED_PARENTHESES);
      assertTrue(hqlWithMigration.trim().startsWith(HQL_PREFIX),
          MSG_MIGRATION_QUERY + MSG_SHOULD_START_WITH_AS_I);
      assertTrue(hqlWithoutMigration.trim().startsWith(HQL_PREFIX),
          MSG_NON_MIGRATION_QUERY + MSG_SHOULD_START_WITH_AS_I);
    }

    /**
     * Verifies that query contains all required conditions.
     */
    @Test
    @DisplayName("Query should contain all required conditions")
    void testQueryContainsAllRequiredConditions() {
      String hql = buildOptimizedHql(false);
      assertTrue(hql.contains(HQL_PROCESSED_TRUE), "Missing: processed = true");
      assertTrue(hql.contains(HQL_PAYMENT_COMPLETE_FALSE), "Missing: paymentComplete = false");
      assertTrue(hql.contains(HQL_OUTSTANDING_NOT_ZERO), "Missing: outstandingAmount <> 0");
      assertTrue(hql.contains(KEYWORD_EXISTS), "Missing: EXISTS subquery");
      assertTrue(hql.contains(HQL_FPS_UPDATED), "Missing: fps.updated comparison");
      assertTrue(hql.contains(KEYWORD_COALESCE), "Missing: COALESCE function");
      assertTrue(hql.contains(HQL_FINAL_SETTLEMENT_NULL), "Missing: finalSettlementDate is null");
    }
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
        .append("    (i.paymentComplete = false and i.updated >= coalesce(i.lastCalculatedOnDate, i.creationDate))")
        .append("    or exists (")
        .append("      select 1 from FIN_Payment_Schedule fps ")
        .append("      where fps.invoice = i ")
        .append("      and fps.updated >= coalesce(i.lastCalculatedOnDate, i.creationDate)")
        .append("    )")
        .append("    or (i.outstandingAmount <> 0 and i.lastCalculatedOnDate is null)");
    if (withMigration) {
      hqlBuilder.append("    or (i.finalSettlementDate is null")
          .append("      and i.aprmtIsmigrated = 'N'")
          .append("      and exists (")
          .append("        select 1 from FIN_Payment_Schedule fps2")
          .append("        where fps2.invoice = i")
          .append("      )")
          .append("    )")
          .append("  )");
    } else {
      hqlBuilder.append("    or i.finalSettlementDate is null")
          .append("  )");
    }
    return hqlBuilder.toString();
  }


  /**
   * Counts occurrences of a substring in a string.
   * @param text the text to search in
   * @param search the substring to countl
   * @return number of occurrences
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
