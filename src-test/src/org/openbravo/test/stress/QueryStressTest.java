package org.openbravo.test.stress;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;

/**
 * Query performance stress tests on heavily loaded tables.
 * All tests are read-only - no cleanup needed.
 */
public class QueryStressTest extends StressTestBase {

  private static final String EXPECTED_RESULTS = "Expected results";
  private static final String MS_MAX_SUFFIX = " ms";
  private static final String COUNT_SO_QUERY =
      "SELECT count(*) FROM c_order WHERE issotrx = 'Y'";

  @Override
  protected String[] getAuditTriggers() {
    return new String[0];
  }

  @Override
  protected String[] getAuditTables() {
    return new String[0];
  }

  @Override
  protected String getStressDocPrefix() {
    return "STRESS-QUERY-";
  }

  // ==========================================================================
  // Basic count queries on large tables
  // ==========================================================================

  @Test
  public void testOrderCountQuery() {
    OBContext.setAdminMode(true);
    try {
      long start = System.nanoTime();
      Number count = (Number) OBDal.getInstance().getSession()
          .createNativeQuery("SELECT count(*) FROM c_order")
          .uniqueResult();
      long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

      log.info("[STRESS-QUERY] Order count: " + count + " in " + elapsed + " ms");
      Assert.assertTrue("Expected orders > 0", count.longValue() > 0);
      Assert.assertTrue("Count query too slow: " + elapsed + " ms (max: 500 ms)",
          elapsed <= 500);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // ==========================================================================
  // Filtered queries
  // ==========================================================================

  @Test
  public void testOrderFilterByDateRange() {
    OBContext.setAdminMode(true);
    try {
      long start = System.nanoTime();
      OBQuery<Order> query = OBDal.getInstance()
          .createQuery(Order.class,
              "orderDate >= '2024-01-01' and orderDate <= '2024-12-31'");
      query.setMaxResult(1000);
      List<Order> orders = query.list();
      long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

      log.info("[STRESS-QUERY] Date range query: " + orders.size()
          + " orders in " + elapsed + " ms");
      Assert.assertTrue("Date range query too slow: " + elapsed + " ms (max: 600 ms)",
          elapsed <= 600);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // ==========================================================================
  // JOIN queries
  // ==========================================================================

  @Test
  public void testOrderWithLinesJoin() {
    OBContext.setAdminMode(true);
    try {
      long start = System.nanoTime();
      @SuppressWarnings("unchecked")
      List<Object[]> results = OBDal.getInstance().getSession()
          .createNativeQuery(
              "SELECT o.documentno, count(ol.c_orderline_id) "
                  + "FROM c_order o "
                  + "JOIN c_orderline ol ON o.c_order_id = ol.c_order_id "
                  + "WHERE o.issotrx = 'Y' "
                  + "GROUP BY o.documentno "
                  + "ORDER BY count(ol.c_orderline_id) DESC "
                  + "LIMIT 100")
          .list();
      long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

      log.info("[STRESS-QUERY] Order+Lines JOIN: " + results.size()
          + " results in " + elapsed + " ms");
      Assert.assertFalse(EXPECTED_RESULTS, results.isEmpty());
      Assert.assertTrue("JOIN query too slow: " + elapsed + " ms (max: 7000 ms)",
          elapsed <= 7000);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // ==========================================================================
  // Aggregation queries
  // ==========================================================================

  @Test
  public void testInvoiceLineAggregation() {
    OBContext.setAdminMode(true);
    try {
      long start = System.nanoTime();
      @SuppressWarnings("unchecked")
      List<Object[]> results = OBDal.getInstance().getSession()
          .createNativeQuery(
              "SELECT bp.name, count(il.c_invoiceline_id), sum(il.linenetamt) "
                  + "FROM c_invoiceline il "
                  + "JOIN c_invoice i ON il.c_invoice_id = i.c_invoice_id "
                  + "JOIN c_bpartner bp ON i.c_bpartner_id = bp.c_bpartner_id "
                  + "WHERE i.issotrx = 'Y' "
                  + "GROUP BY bp.name "
                  + "ORDER BY sum(il.linenetamt) DESC "
                  + "LIMIT 50")
          .list();
      long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

      log.info("[STRESS-QUERY] Invoice aggregation: " + results.size()
          + " BPs in " + elapsed + " ms");
      Assert.assertFalse(EXPECTED_RESULTS, results.isEmpty());
      Assert.assertTrue("Aggregation query too slow: " + elapsed + MS_MAX_SUFFIX,
          elapsed <= 9000);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // ==========================================================================
  // Heavy table queries (millions of rows)
  // ==========================================================================

  @Test
  public void testFactAcctHeavyQuery() {
    OBContext.setAdminMode(true);
    try {
      long start = System.nanoTime();
      @SuppressWarnings("unchecked")
      List<Object[]> results = OBDal.getInstance().getSession()
          .createNativeQuery(
              "SELECT f.account_id, sum(f.amtacctdr) AS total_debit, sum(f.amtacctcr) AS total_credit "
                  + "FROM fact_acct f "
                  + "WHERE f.ad_org_id = :orgId "
                  + "GROUP BY f.account_id "
                  + "ORDER BY total_debit DESC "
                  + "LIMIT 100")
          .setParameter("orgId", CLIENT_ORG_ID)
          .list();
      long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

      log.info("[STRESS-QUERY] fact_acct aggregation: " + results.size()
          + " accounts in " + elapsed + " ms");
      Assert.assertFalse(EXPECTED_RESULTS, results.isEmpty());
      Assert.assertTrue("fact_acct query too slow: " + elapsed + " ms (max: 13000 ms)",
          elapsed <= 13000);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testTransactionVolumeQuery() {
    OBContext.setAdminMode(true);
    try {
      long start = System.nanoTime();
      @SuppressWarnings("unchecked")
      List<Object[]> results = OBDal.getInstance().getSession()
          .createNativeQuery(
              "SELECT date_trunc('month', movementdate) as month, count(*), sum(movementqty) "
                  + "FROM m_transaction "
                  + "WHERE ad_org_id = :orgId "
                  + "GROUP BY date_trunc('month', movementdate) "
                  + "ORDER BY month")
          .setParameter("orgId", CLIENT_ORG_ID)
          .list();
      long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

      log.info("[STRESS-QUERY] m_transaction monthly: " + results.size()
          + " months in " + elapsed + " ms");
      Assert.assertFalse(EXPECTED_RESULTS, results.isEmpty());
      Assert.assertTrue("Transaction query too slow: " + elapsed + " ms (max: 10000 ms)",
          elapsed <= 10000);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // ==========================================================================
  // Pagination simulation
  // ==========================================================================

  @Test
  public void testPaginatedOrderQuery() {
    OBContext.setAdminMode(true);
    try {
      int pageSize = 100;
      int totalPages = 10;
      long totalTime = 0;
      long maxPageTime = 0;

      for (int page = 0; page < totalPages; page++) {
        long start = System.nanoTime();

        OBQuery<Order> query = OBDal.getInstance()
            .createQuery(Order.class, "salesTransaction = true order by orderDate desc");
        query.setFirstResult(page * pageSize);
        query.setMaxResult(pageSize);
        List<Order> orders = query.list();

        long pageTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        totalTime += pageTime;
        if (pageTime > maxPageTime) {
          maxPageTime = pageTime;
        }

        Assert.assertEquals("Page " + page + " should have " + pageSize + " results",
            pageSize, orders.size());

        OBDal.getInstance().getSession().clear();
      }

      log.info("[STRESS-QUERY] Pagination (" + totalPages + " pages x "
          + pageSize + "): total " + totalTime + " ms, avg "
          + (totalTime / totalPages) + " ms/page, max " + maxPageTime + " ms");
      Assert.assertTrue(
          "Avg page time too slow: " + (totalTime / totalPages) + " ms (max: 200 ms)",
          (totalTime / totalPages) <= 200);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // ==========================================================================
  // Search simulation
  // ==========================================================================

  @Test
  public void testProductSearchPerformance() {
    OBContext.setAdminMode(true);
    try {
      String[] searches = {
          "%TELA%", "%ALGOD%", "%POLY%", "%HILO%", "%TEJIDO%",
          "%PUNTO%", "%RIZO%", "%FELPA%", "%COLOR%", "%BLANCO%",
          "%NEGRO%", "%ROJO%", "%100%", "%200%", "%METRO%",
          "%ROLLO%", "%PIEZA%", "%MUESTRA%", "%EXPORT%", "%IMPORT%"
      };

      long totalTime = 0;
      int totalResults = 0;

      for (String search : searches) {
        long start = System.nanoTime();

        OBQuery<Product> query = OBDal.getInstance()
            .createQuery(Product.class, "name like :search");
        query.setNamedParameter("search", search);
        query.setMaxResult(50);
        List<Product> products = query.list();

        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        totalTime += elapsed;
        totalResults += products.size();

        OBDal.getInstance().getSession().clear();
      }

      log.info("[STRESS-QUERY] Product search (" + searches.length
          + " queries): total " + totalTime + " ms, avg "
          + (totalTime / searches.length) + " ms/query, " + totalResults + " total results");
      Assert.assertTrue(
          "Avg search too slow: " + (totalTime / searches.length) + " ms (max: 500 ms)",
          (totalTime / searches.length) <= 500);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // ==========================================================================
  // SQL Logic function-per-row tests (grid simulation)
  // ==========================================================================

  /**
   * Simulates the Goods Receipt grid WITHOUT the SQL Logic column.
   * Baseline query to compare against the function-per-row variant.
   */
  @Test
  public void testGoodsReceiptGridBaseline() {
    OBContext.setAdminMode(true);
    try {
      long start = System.nanoTime();
      @SuppressWarnings("unchecked")
      List<Object[]> results = OBDal.getInstance().getSession()
          .createNativeQuery(
              "SELECT m.m_inout_id, m.documentno, m.movementdate, m.docstatus, "
                  + "bp.name AS bpartner "
                  + "FROM m_inout m "
                  + "JOIN c_bpartner bp ON m.c_bpartner_id = bp.c_bpartner_id "
                  + "WHERE m.issotrx = 'N' "
                  + "ORDER BY m.movementdate DESC "
                  + "LIMIT 100")
          .list();
      long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

      log.info("[STRESS-QUERY] Goods Receipt grid (baseline, no function): "
          + results.size() + " rows in " + elapsed + " ms");
      Assert.assertFalse(EXPECTED_RESULTS, results.isEmpty());
      Assert.assertTrue(
          "Goods Receipt baseline too slow: " + elapsed + " ms (max: 100 ms)",
          elapsed <= 100);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Simulates the Goods Receipt grid WITH the Invoice Status SQL Logic column.
   * This calls C_GETINVOICESTATUSFROMSHIPMENT(m_inout_id) per row, reproducing
   * the N+1 function-call pattern that caused ETP-2696 grid loading issues.
   */
  @Test
  public void testGoodsReceiptGridWithInvoiceStatusFunction() {
    OBContext.setAdminMode(true);
    try {
      long start = System.nanoTime();
      @SuppressWarnings("unchecked")
      List<Object[]> results = OBDal.getInstance().getSession()
          .createNativeQuery(
              "SELECT m.m_inout_id, m.documentno, m.movementdate, m.docstatus, "
                  + "bp.name AS bpartner, "
                  + "C_GETINVOICESTATUSFROMSHIPMENT(m.m_inout_id) AS invoice_status "
                  + "FROM m_inout m "
                  + "JOIN c_bpartner bp ON m.c_bpartner_id = bp.c_bpartner_id "
                  + "WHERE m.issotrx = 'N' "
                  + "ORDER BY m.movementdate DESC "
                  + "LIMIT 100")
          .list();
      long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

      log.info("[STRESS-QUERY] Goods Receipt grid (with InvoiceStatus function): "
          + results.size() + " rows in " + elapsed + " ms");
      Assert.assertFalse(EXPECTED_RESULTS, results.isEmpty());
      Assert.assertTrue(
          "Goods Receipt with InvoiceStatus function too slow: " + elapsed
              + " ms (max: 300 ms)",
          elapsed <= 300);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Compares grid query performance with and without the SQL Logic function column.
   * A high degradation factor indicates N+1 function-call overhead that will
   * cause grid loading issues in the UI.
   */
  @Test
  public void testGoodsReceiptInvoiceStatusDegradation() {
    OBContext.setAdminMode(true);
    try {
      int pageSize = 100;

      // Baseline: without function
      long baseStart = System.nanoTime();
      OBDal.getInstance().getSession()
          .createNativeQuery(
              "SELECT m.m_inout_id, m.documentno, m.movementdate, m.docstatus "
                  + "FROM m_inout m "
                  + "WHERE m.issotrx = 'N' "
                  + "ORDER BY m.movementdate DESC "
                  + "LIMIT " + pageSize)
          .list();
      long baseTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - baseStart);

      OBDal.getInstance().getSession().clear();

      // With function: per-row call
      long funcStart = System.nanoTime();
      OBDal.getInstance().getSession()
          .createNativeQuery(
              "SELECT m.m_inout_id, m.documentno, m.movementdate, m.docstatus, "
                  + "C_GETINVOICESTATUSFROMSHIPMENT(m.m_inout_id) AS invoice_status "
                  + "FROM m_inout m "
                  + "WHERE m.issotrx = 'N' "
                  + "ORDER BY m.movementdate DESC "
                  + "LIMIT " + pageSize)
          .list();
      long funcTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - funcStart);

      double degradation = baseTime > 0 ? (double) funcTime / baseTime : funcTime;

      log.info("[STRESS-QUERY] === GOODS RECEIPT INVOICE STATUS DEGRADATION ===");
      log.info("[STRESS-QUERY] Baseline (no function): " + baseTime + " ms");
      log.info("[STRESS-QUERY] With InvoiceStatus function: " + funcTime + " ms");
      log.info(String.format("[STRESS-QUERY] Degradation factor: %.1fx", degradation));

      Assert.assertTrue(
          String.format("InvoiceStatus function degrades grid query by %.1fx "
              + "(max: 10x, base: %d ms, with function: %d ms)",
              degradation, baseTime, funcTime),
          degradation <= 10);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Simulates paginated grid browsing with the SQL Logic function column.
   * Tests multiple pages to detect if the function overhead accumulates
   * or remains stable across pagination.
   */
  @Test
  public void testGoodsReceiptPaginatedWithFunction() {
    OBContext.setAdminMode(true);
    try {
      int pageSize = 100;
      int totalPages = 5;
      long totalTime = 0;
      long maxPageTime = 0;

      for (int page = 0; page < totalPages; page++) {
        long start = System.nanoTime();

        @SuppressWarnings("unchecked")
        List<Object[]> results = OBDal.getInstance().getSession()
            .createNativeQuery(
                "SELECT m.m_inout_id, m.documentno, m.movementdate, m.docstatus, "
                    + "C_GETINVOICESTATUSFROMSHIPMENT(m.m_inout_id) AS invoice_status "
                    + "FROM m_inout m "
                    + "WHERE m.issotrx = 'N' "
                    + "ORDER BY m.movementdate DESC "
                    + "LIMIT " + pageSize + " OFFSET " + (page * pageSize))
            .list();

        long pageTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        totalTime += pageTime;
        if (pageTime > maxPageTime) {
          maxPageTime = pageTime;
        }

        log.info("[STRESS-QUERY] Goods Receipt page " + page + ": "
            + results.size() + " rows in " + pageTime + " ms");

        OBDal.getInstance().getSession().clear();
      }

      long avgPageTime = totalTime / totalPages;
      log.info("[STRESS-QUERY] Goods Receipt pagination (" + totalPages
          + " pages x " + pageSize + " with function): total " + totalTime
          + " ms, avg " + avgPageTime + " ms/page, max " + maxPageTime + " ms");

      Assert.assertTrue(
          "Avg page time with function too slow: " + avgPageTime + " ms (max: 1000 ms)",
          avgPageTime <= 1000);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Tests the function with a larger result set (500 rows) to detect
   * N+1 overhead that only surfaces at higher volumes.
   * The UI grid may fetch more than 100 rows depending on configuration.
   */
  @Test
  public void testGoodsReceiptFunctionHighVolume() {
    OBContext.setAdminMode(true);
    try {
      int[] rowCounts = {100, 250, 500};

      long prevTime = 0;
      for (int rows : rowCounts) {
        long start = System.nanoTime();
        @SuppressWarnings("unchecked")
        List<Object[]> results = OBDal.getInstance().getSession()
            .createNativeQuery(
                "SELECT m.m_inout_id, m.documentno, "
                    + "C_GETINVOICESTATUSFROMSHIPMENT(m.m_inout_id) AS invoice_status "
                    + "FROM m_inout m "
                    + "WHERE m.issotrx = 'N' "
                    + "ORDER BY m.movementdate DESC "
                    + "LIMIT " + rows)
            .list();
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        double msPerRow = results.isEmpty() ? 0 : (double) elapsed / results.size();

        log.info("[STRESS-QUERY] InvoiceStatus function " + rows + " rows: "
            + elapsed + " ms (" + String.format("%.2f", msPerRow) + " ms/row)");

        OBDal.getInstance().getSession().clear();
        prevTime = elapsed;
      }

      // The 500-row query should complete within a reasonable time
      Assert.assertTrue(
          "InvoiceStatus function with 500 rows too slow: " + prevTime + " ms (max: 1500 ms)",
          prevTime <= 1500);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Scans all SQL Logic function columns used in grid views on high-volume tables
   * to detect N+1 performance patterns before they reach production.
   * Each function is called per-row on 100 records; if any exceeds the threshold,
   * it signals a potential grid loading issue.
   */
  @Test
  public void testSqlLogicFunctionsOnHighVolumeTables() {
    OBContext.setAdminMode(true);
    try {
      // Pairs of: [table, filter, function_call, label]
      String[][] functionTests = {
          {"m_inout", "issotrx = 'N'",
              "C_GETINVOICESTATUSFROMSHIPMENT(m_inout_id)", "GoodsReceipt.InvoiceStatus"},
      };

      long maxAllowed = 800;

      for (String[] test : functionTests) {
        String table = test[0];
        String filter = test[1];
        String functionCall = test[2];
        String label = test[3];

        // Baseline without function
        long baseStart = System.nanoTime();
        OBDal.getInstance().getSession()
            .createNativeQuery(
                "SELECT * FROM " + table
                    + " WHERE " + filter
                    + " ORDER BY created DESC LIMIT 100")
            .list();
        long baseTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - baseStart);

        OBDal.getInstance().getSession().clear();

        // With function
        long funcStart = System.nanoTime();
        OBDal.getInstance().getSession()
            .createNativeQuery(
                "SELECT *, " + functionCall + " FROM " + table
                    + " WHERE " + filter
                    + " ORDER BY created DESC LIMIT 100")
            .list();
        long funcTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - funcStart);

        double degradation = baseTime > 0 ? (double) funcTime / baseTime : funcTime;

        log.info("[STRESS-QUERY] SQL Logic [" + label + "]: base=" + baseTime
            + " ms, with function=" + funcTime + " ms, degradation="
            + String.format("%.1fx", degradation));

        Assert.assertTrue(
            "SQL Logic function [" + label + "] too slow: " + funcTime
                + " ms (max: " + maxAllowed + " ms)",
            funcTime <= maxAllowed);
        Assert.assertTrue(
            String.format("SQL Logic function [%s] degrades query by %.1fx (max: 10x)",
                label, degradation),
            degradation <= 10);

        OBDal.getInstance().getSession().clear();
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // ==========================================================================
  // Concurrent queries
  // ==========================================================================

  @Test
  public void testConcurrentQueries_10Threads() {
    int threadCount = 10;
    int queriesPerThread = 50;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    AtomicInteger totalQueries = new AtomicInteger(0);
    CopyOnWriteArrayList<String> errors = new CopyOnWriteArrayList<>();

    long startTime = System.nanoTime();

    List<Future<?>> futures = new ArrayList<>();
    for (int t = 0; t < threadCount; t++) {
      final int threadId = t;
      futures.add(executor.submit(() -> {
        int completed = 0;
        try {
          OBContext.setOBContext(CLIENT_USER_ID, CLIENT_ROLE_ID, CLIENT_CLIENT_ID, CLIENT_ORG_ID);
          OBContext.setAdminMode(true);

          for (int i = 0; i < queriesPerThread; i++) {
            Number count = (Number) OBDal.getInstance().getSession()
                .createNativeQuery(COUNT_SO_QUERY)
                .uniqueResult();
            Assert.assertTrue(count.longValue() > 0);
            completed++;

            if (completed % 10 == 0) {
              OBDal.getInstance().getSession().clear();
            }
          }

          SessionHandler.getInstance().commitAndClose();
        } catch (Exception e) {
          errors.add("Thread " + threadId + ": " + e.getMessage());
          try {
            OBDal.getInstance().rollbackAndClose();
          } catch (Exception ex) {
            // ignore
          }
        } finally {
          OBContext.restorePreviousMode();
        }
        totalQueries.addAndGet(completed);
      }));
    }

    for (Future<?> f : futures) {
      try {
        f.get(5, TimeUnit.MINUTES);
      } catch (Exception e) {
        Thread.currentThread().interrupt();
        errors.add("Future failed: " + e.getMessage());
      }
    }
    executor.shutdown();

    long wallTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

    log.info("[STRESS-QUERY] === CONCURRENT QUERIES RESULTS ===");
    log.info("[STRESS-QUERY] Threads: " + threadCount
        + ", Queries/thread: " + queriesPerThread);
    log.info("[STRESS-QUERY] Total queries: " + totalQueries.get());
    log.info("[STRESS-QUERY] Wall time: " + wallTime + " ms");
    log.info("[STRESS-QUERY] Throughput: "
        + (totalQueries.get() * 1000L / Math.max(wallTime, 1)) + " queries/sec");

    if (!errors.isEmpty()) {
      errors.forEach(e -> log.info("[STRESS-QUERY]   ERROR: " + e));
    }

    Assert.assertTrue("Errors occurred: " + errors, errors.isEmpty());
    Assert.assertEquals(threadCount * queriesPerThread, totalQueries.get());
  }

  // ==========================================================================
  // Sequential vs Concurrent comparison
  // ==========================================================================

  @Test
  public void testSequentialVsConcurrentQueryComparison() {
    int totalQueries = 100;
    int threadCount = 10;

    // Sequential
    OBContext.setAdminMode(true);
    long seqStart = System.nanoTime();
    try {
      for (int i = 0; i < totalQueries; i++) {
        OBDal.getInstance().getSession()
            .createNativeQuery(COUNT_SO_QUERY)
            .uniqueResult();
        if (i % 20 == 0) {
          OBDal.getInstance().getSession().clear();
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    long seqTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - seqStart);

    OBDal.getInstance().getSession().clear();

    // Concurrent
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(1);
    AtomicInteger completed = new AtomicInteger(0);
    int queriesPerThread = totalQueries / threadCount;

    long concStart = System.nanoTime();
    List<Future<?>> futures = new ArrayList<>();
    for (int t = 0; t < threadCount; t++) {
      futures.add(executor.submit(() -> {
        try {
          latch.await();
          OBContext.setOBContext(CLIENT_USER_ID, CLIENT_ROLE_ID, CLIENT_CLIENT_ID, CLIENT_ORG_ID);
          OBContext.setAdminMode(true);

          for (int i = 0; i < queriesPerThread; i++) {
            OBDal.getInstance().getSession()
                .createNativeQuery(COUNT_SO_QUERY)
                .uniqueResult();
            completed.incrementAndGet();
          }

          SessionHandler.getInstance().commitAndClose();
        } catch (Exception e) {
          Thread.currentThread().interrupt();
          try {
            OBDal.getInstance().rollbackAndClose();
          } catch (Exception ex) {
            // ignore
          }
        } finally {
          OBContext.restorePreviousMode();
        }
      }));
    }

    latch.countDown();
    for (Future<?> f : futures) {
      try {
        f.get(5, TimeUnit.MINUTES);
      } catch (Exception e) {
        Thread.currentThread().interrupt();
      }
    }
    executor.shutdown();
    long concTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - concStart);

    double speedup = (double) seqTime / Math.max(concTime, 1);

    log.info("[STRESS-QUERY] === SEQUENTIAL vs CONCURRENT COMPARISON ===");
    log.info("[STRESS-QUERY] " + totalQueries + " queries");
    log.info("[STRESS-QUERY] Sequential: " + seqTime + " ms ("
        + (totalQueries * 1000L / Math.max(seqTime, 1)) + " queries/sec)");
    log.info("[STRESS-QUERY] Concurrent (" + threadCount + " threads): "
        + concTime + " ms ("
        + (completed.get() * 1000L / Math.max(concTime, 1)) + " queries/sec)");
    log.info(String.format("[STRESS-QUERY] Speedup: %.2fx", speedup));

    Assert.assertEquals(totalQueries, completed.get());
  }
}
