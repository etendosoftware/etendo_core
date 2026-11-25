package org.openbravo.test.stress;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.service.db.CallStoredProcedure;

/**
 * Concurrent stress tests that simulate multiple users operating simultaneously.
 * Tests DAL thread safety, connection pool behavior, and lock contention under load.
 */
public class ConcurrentStressTest extends StressTestBase {

  private static final String STRESS_DOC_PREFIX = "STRESS-CONC-";
  private static final BigDecimal UNIT_PRICE = new BigDecimal("25.00");

  private static final String LOG_THREADS = "[STRESS-CONC] Threads: ";
  private static final String LOG_WALL_TIME = "[STRESS-CONC] Wall time: ";
  private static final String LOG_THROUGHPUT = "[STRESS-CONC] Throughput: ";
  private static final String LOG_ERRORS = "[STRESS-CONC] ERRORS (";
  private static final String LOG_ERROR_ITEM = "[STRESS-CONC]   - ";
  private static final String ERRORS_OCCURRED = "Errors occurred: ";
  private static final String LOG_THREAD = "Thread ";
  private static final String ORDERS_PER_SEC = " orders/sec";
  private static final String LOG_FUTURE = "Future ";
  private static final String LOG_FAILED = " failed: ";

  @Override
  protected String[] getAuditTriggers() {
    return ORDER_TRIGGERS;
  }

  @Override
  protected String[] getAuditTables() {
    return ORDER_TABLES;
  }

  @Override
  protected String getStressDocPrefix() {
    return STRESS_DOC_PREFIX;
  }

  @Override
  @Before
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    // Commit and release the connection so child threads can use the pool
    OBDal.getInstance().commitAndClose();
  }

  @After
  @AfterEach
  public void cleanUp() {
    OBContext.setOBContext(CLIENT_USER_ID, CLIENT_ROLE_ID, CLIENT_CLIENT_ID, CLIENT_ORG_ID);
    runCleanup(() -> {
      String prefix = STRESS_DOC_PREFIX + "%";
      cleanupReservationData(prefix);
      cleanupOrderData(prefix);
    });
  }

  // ==========================================================================
  // CONCURRENT ORDER CREATION - Multiple threads creating orders simultaneously
  // ==========================================================================

  @Test
  public void testConcurrentOrderCreation_5Threads_50Each() {
    runConcurrentOrderCreation(5, 50, 3);
  }

  @Test
  public void testConcurrentOrderCreation_10Threads_20Each() {
    runConcurrentOrderCreation(10, 20, 3);
  }

  @Test
  public void testConcurrentOrderCreation_20Threads_10Each() {
    runConcurrentOrderCreation(20, 10, 3);
  }

  // ==========================================================================
  // CONCURRENT MIXED OPERATIONS - SO and PO created simultaneously
  // ==========================================================================

  @Test
  public void testConcurrentMixedSalesAndPurchase_10Threads() {
    runConcurrentMixedOperations(10, 20, 3);
  }

  // ==========================================================================
  // CONCURRENT COMPLETE - Multiple threads completing orders via stored proc
  // ==========================================================================

  @Test
  public void testConcurrentOrderComplete_5Threads_10Each() {
    runConcurrentOrderCreateAndComplete(5, 10, 1);
  }

  @Test
  public void testConcurrentOrderComplete_10Threads_5Each() {
    runConcurrentOrderCreateAndComplete(10, 5, 1);
  }

  // ==========================================================================
  // CONCURRENT READ + WRITE - Readers and writers competing
  // ==========================================================================

  @Test
  public void testConcurrentReadWrite_5Writers3Readers() {
    runConcurrentReadWrite(5, 3, 30, 50);
  }

  // ==========================================================================
  // BURST TEST - All threads start at the exact same moment
  // ==========================================================================

  @Test
  public void testBurstOrderCreation_10Threads() {
    runBurstOrderCreation(10, 20, 3);
  }

  // ==========================================================================
  // Implementation methods
  // ==========================================================================

  private void runConcurrentOrderCreation(int threadCount, int ordersPerThread,
      int linesPerOrder) {

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    AtomicInteger totalCreated = new AtomicInteger(0);
    AtomicLong totalTimeMs = new AtomicLong(0);
    CopyOnWriteArrayList<String> errors = new CopyOnWriteArrayList<>();

    long startTime = System.nanoTime();

    List<Future<ThreadResult>> futures = new ArrayList<>();
    for (int t = 0; t < threadCount; t++) {
      final int threadId = t;
      futures.add(executor.submit(
          new OrderCreationTask(threadId, ordersPerThread, linesPerOrder, true, errors)));
    }

    collectResults(futures, totalCreated, totalTimeMs, errors);
    executor.shutdown();

    long wallTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
    int expectedTotal = threadCount * ordersPerThread;

    printConcurrentResults("CONCURRENT ORDER CREATION", threadCount, ordersPerThread,
        linesPerOrder, totalCreated.get(), wallTime, totalTimeMs.get(), errors);

    Assert.assertTrue(ERRORS_OCCURRED + errors, errors.isEmpty());
    Assert.assertEquals("Not all orders were created", expectedTotal, totalCreated.get());
  }

  private void runConcurrentMixedOperations(int threadCount, int ordersPerThread,
      int linesPerOrder) {

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    AtomicInteger totalCreated = new AtomicInteger(0);
    AtomicLong totalTimeMs = new AtomicLong(0);
    CopyOnWriteArrayList<String> errors = new CopyOnWriteArrayList<>();

    long startTime = System.nanoTime();

    List<Future<ThreadResult>> futures = new ArrayList<>();
    for (int t = 0; t < threadCount; t++) {
      final int threadId = t;
      boolean isSalesOrder = (t % 2 == 0);
      futures.add(executor.submit(
          new OrderCreationTask(threadId, ordersPerThread, linesPerOrder, isSalesOrder, errors)));
    }

    collectResults(futures, totalCreated, totalTimeMs, errors);
    executor.shutdown();

    long wallTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

    printConcurrentResults("CONCURRENT MIXED SO/PO", threadCount, ordersPerThread,
        linesPerOrder, totalCreated.get(), wallTime, totalTimeMs.get(), errors);

    Assert.assertTrue(ERRORS_OCCURRED + errors, errors.isEmpty());
    Assert.assertEquals(threadCount * ordersPerThread, totalCreated.get());
  }

  private void runConcurrentOrderCreateAndComplete(int threadCount, int ordersPerThread,
      int linesPerOrder) {

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    AtomicInteger totalCompleted = new AtomicInteger(0);
    AtomicLong totalTimeMs = new AtomicLong(0);
    AtomicLong totalCompleteTime = new AtomicLong(0);
    CopyOnWriteArrayList<String> errors = new CopyOnWriteArrayList<>();

    long startTime = System.nanoTime();

    List<Future<ThreadResult>> futures = new ArrayList<>();
    for (int t = 0; t < threadCount; t++) {
      final int threadId = t;
      futures.add(executor.submit(() -> {
        long threadStart = System.nanoTime();
        int created = 0;
        long completeMs = 0;
        try {
          OBContext.setOBContext(CLIENT_USER_ID, CLIENT_ROLE_ID, CLIENT_CLIENT_ID, CLIENT_ORG_ID);
          OBContext.setAdminMode(true);

          for (int i = 0; i < ordersPerThread; i++) {
            String docNo = STRESS_DOC_PREFIX + "T" + threadId + "-CO-" + i;

            OrderContext ctx = OrderContext.load(PO_DOCTYPE_ID, BPARTNER_ID, PO_WAREHOUSE_ID,
                PO_PRICELIST_ID, PO_PRODUCT_ID);

            Order order = createOrder(ctx.docType, ctx.bp, ctx.warehouse, ctx.priceList,
                ctx.paymentTerm, ctx.paymentMethod, ctx.currency, docNo, false);
            OBDal.getInstance().save(order);

            for (int j = 0; j < linesPerOrder; j++) {
              OrderLine line = createOrderLine(order, ctx.product, ctx.tax,
                  (long) (j + 1) * 10, new BigDecimal(j + 1), new BigDecimal("9.08"),
                  new BigDecimal("9.08"));
              OBDal.getInstance().save(line);
            }
            OBDal.getInstance().flush();

            long completeStart = System.nanoTime();
            List<Object> params = new ArrayList<>();
            params.add(null);
            params.add(order.getId());
            CallStoredProcedure.getInstance().call("c_order_post1", params, null, true, false);
            OBDal.getInstance().flush();
            completeMs += TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - completeStart);

            OBDal.getInstance().refresh(order);
            if (!"CO".equals(order.getDocumentStatus())) {
              errors.add(LOG_THREAD + threadId + ": Order " + docNo
                  + " not completed, status=" + order.getDocumentStatus());
            }

            created++;

            OBDal.getInstance().getSession().clear();
          }

          SessionHandler.getInstance().commitAndClose();
        } catch (Exception e) {
          errors.add(LOG_THREAD + threadId + ": " + e.getClass().getSimpleName()
              + ": " + e.getMessage());
          try {
            OBDal.getInstance().rollbackAndClose();
          } catch (Exception ex) {
            // ignore
          }
        } finally {
          OBContext.restorePreviousMode();
        }

        long threadTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - threadStart);
        totalCompleted.addAndGet(created);
        totalTimeMs.addAndGet(threadTime);
        totalCompleteTime.addAndGet(completeMs);
        return new ThreadResult(created, threadTime);
      }));
    }

    for (Future<ThreadResult> f : futures) {
      try {
        f.get(5, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        errors.add(LOG_FUTURE + "failed: " + e.getMessage());
      } catch (Exception e) {
        errors.add(LOG_FUTURE + "failed: " + e.getMessage());
      }
    }
    executor.shutdown();

    long wallTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
    int expectedTotal = threadCount * ordersPerThread;

    log.info("[STRESS-CONC] === CONCURRENT CREATE & COMPLETE RESULTS ===");
    log.info(LOG_THREADS + threadCount
        + ", Orders/thread: " + ordersPerThread);
    log.info("[STRESS-CONC] Total completed: " + totalCompleted.get());
    log.info(LOG_WALL_TIME + wallTime + " ms");
    log.info("[STRESS-CONC] Total c_order_post1 time: "
        + totalCompleteTime.get() + " ms");
    log.info("[STRESS-CONC] Avg complete per order: "
        + (totalCompleteTime.get() / Math.max(totalCompleted.get(), 1)) + " ms");
    log.info(LOG_THROUGHPUT
        + (totalCompleted.get() * 1000L / Math.max(wallTime, 1)) + ORDERS_PER_SEC);
    log.info("[STRESS-CONC] Parallelism efficiency: "
        + (totalTimeMs.get() * 100 / Math.max(wallTime * threadCount, 1)) + "%");

    if (!errors.isEmpty()) {
      log.info(LOG_ERRORS + errors.size() + "):");
      errors.forEach(e -> log.info(LOG_ERROR_ITEM + e));
    }

    Assert.assertTrue(ERRORS_OCCURRED + errors, errors.isEmpty());
    Assert.assertEquals(expectedTotal, totalCompleted.get());
  }

  private void runConcurrentReadWrite(int writerCount, int readerCount,
      int ordersPerWriter, int queriesPerReader) {

    ExecutorService executor = Executors.newFixedThreadPool(writerCount + readerCount);
    AtomicInteger totalWritten = new AtomicInteger(0);
    AtomicInteger totalRead = new AtomicInteger(0);
    AtomicLong writeTimeMs = new AtomicLong(0);
    AtomicLong readTimeMs = new AtomicLong(0);
    CopyOnWriteArrayList<String> errors = new CopyOnWriteArrayList<>();

    long startTime = System.nanoTime();
    List<Future<?>> futures = new ArrayList<>();

    for (int w = 0; w < writerCount; w++) {
      final int writerId = w;
      futures.add(executor.submit(() -> {
        long threadStart = System.nanoTime();
        int created = createOrdersWithCommit(ordersPerWriter, 3,
            STRESS_DOC_PREFIX + "W" + writerId + "-", errors, "Writer " + writerId);
        totalWritten.addAndGet(created);
        writeTimeMs.addAndGet(
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - threadStart));
      }));
    }

    for (int r = 0; r < readerCount; r++) {
      final int readerId = r;
      futures.add(executor.submit(() -> {
        long threadStart = System.nanoTime();
        int queryCount = 0;
        try {
          OBContext.setOBContext(CLIENT_USER_ID, CLIENT_ROLE_ID, CLIENT_CLIENT_ID, CLIENT_ORG_ID);
          OBContext.setAdminMode(true);

          for (int i = 0; i < queriesPerReader; i++) {
            Number count = (Number) OBDal.getInstance().getSession()
                .createNativeQuery("SELECT count(*) FROM c_order WHERE issotrx = 'Y'")
                .uniqueResult();

            Assert.assertNotNull("Query returned null", count);
            Assert.assertTrue("Expected orders > 0", count.longValue() > 0);

            queryCount++;

            if (queryCount % 10 == 0) {
              OBDal.getInstance().getSession().clear();
            }
          }

          SessionHandler.getInstance().commitAndClose();
        } catch (Exception e) {
          errors.add("Reader " + readerId + ": " + e.getClass().getSimpleName()
              + ": " + e.getMessage());
          try {
            OBDal.getInstance().rollbackAndClose();
          } catch (Exception ex) {
            // ignore
          }
        } finally {
          OBContext.restorePreviousMode();
        }

        totalRead.addAndGet(queryCount);
        readTimeMs.addAndGet(
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - threadStart));
      }));
    }

    waitForFutures(futures);
    executor.shutdown();

    long wallTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

    log.info("[STRESS-CONC] === CONCURRENT READ/WRITE RESULTS ===");
    log.info("[STRESS-CONC] Writers: " + writerCount
        + " (" + ordersPerWriter + " orders each)"
        + ", Readers: " + readerCount
        + " (" + queriesPerReader + " queries each)");
    log.info("[STRESS-CONC] Total written: " + totalWritten.get()
        + ", Total queries: " + totalRead.get());
    log.info(LOG_WALL_TIME + wallTime + " ms");
    log.info("[STRESS-CONC] Write throughput: "
        + (totalWritten.get() * 1000L / Math.max(wallTime, 1)) + ORDERS_PER_SEC);
    log.info("[STRESS-CONC] Read throughput: "
        + (totalRead.get() * 1000L / Math.max(wallTime, 1)) + " queries/sec");

    if (!errors.isEmpty()) {
      log.info(LOG_ERRORS + errors.size() + "):");
      errors.forEach(e -> log.info(LOG_ERROR_ITEM + e));
    }

    Assert.assertTrue(ERRORS_OCCURRED + errors, errors.isEmpty());
    Assert.assertEquals(writerCount * ordersPerWriter, totalWritten.get());
    Assert.assertEquals(readerCount * queriesPerReader, totalRead.get());
  }

  private void runBurstOrderCreation(int threadCount, int ordersPerThread, int linesPerOrder) {
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    AtomicInteger totalCreated = new AtomicInteger(0);
    AtomicLong totalTimeMs = new AtomicLong(0);
    CopyOnWriteArrayList<String> errors = new CopyOnWriteArrayList<>();

    List<Future<ThreadResult>> futures = new ArrayList<>();
    for (int t = 0; t < threadCount; t++) {
      final int threadId = t;
      futures.add(executor.submit(() -> {
        try {
          startLatch.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return new ThreadResult(0, 0);
        }

        long threadStart = System.nanoTime();
        int created = createOrdersWithCommit(ordersPerThread, linesPerOrder,
            STRESS_DOC_PREFIX + "BURST-T" + threadId + "-", errors, LOG_THREAD + threadId);
        long threadTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - threadStart);
        return new ThreadResult(created, threadTime);
      }));
    }

    long startTime = System.nanoTime();
    startLatch.countDown();

    collectResults(futures, totalCreated, totalTimeMs, errors);
    executor.shutdown();

    long wallTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

    log.info("[STRESS-CONC] === BURST ORDER CREATION RESULTS ===");
    log.info(LOG_THREADS + threadCount
        + " (all started simultaneously)");
    log.info("[STRESS-CONC] Orders/thread: " + ordersPerThread
        + ", Lines/order: " + linesPerOrder);
    log.info("[STRESS-CONC] Total created: " + totalCreated.get());
    log.info(LOG_WALL_TIME + wallTime + " ms");
    log.info(LOG_THROUGHPUT
        + (totalCreated.get() * 1000L / Math.max(wallTime, 1)) + ORDERS_PER_SEC);

    if (!errors.isEmpty()) {
      log.info(LOG_ERRORS + errors.size() + "):");
      errors.forEach(e -> log.info(LOG_ERROR_ITEM + e));
    }

    Assert.assertTrue(ERRORS_OCCURRED + errors, errors.isEmpty());
    Assert.assertEquals(threadCount * ordersPerThread, totalCreated.get());
  }

  // ==========================================================================
  // Helper classes and methods
  // ==========================================================================

  private int createOrdersWithCommit(int orderCount, int linesPerOrder,
      String docNoPrefix, CopyOnWriteArrayList<String> errors, String errorLabel) {
    int created = 0;
    try {
      OBContext.setOBContext(CLIENT_USER_ID, CLIENT_ROLE_ID, CLIENT_CLIENT_ID, CLIENT_ORG_ID);
      OBContext.setAdminMode(true);

      OrderContext ctx = OrderContext.load(SO_DOCTYPE_ID, BPARTNER_ID, PO_WAREHOUSE_ID,
          SO_PRICELIST_ID, PO_PRODUCT_ID);

      for (int i = 0; i < orderCount; i++) {
        String docNo = docNoPrefix + i;
        Order order = createOrder(ctx.docType, ctx.bp, ctx.warehouse, ctx.priceList,
            ctx.paymentTerm, ctx.paymentMethod, ctx.currency, docNo, true);
        OBDal.getInstance().save(order);

        for (int j = 0; j < linesPerOrder; j++) {
          OrderLine line = createOrderLine(order, ctx.product, ctx.tax,
              (long) (j + 1) * 10, new BigDecimal(j + 1), UNIT_PRICE, UNIT_PRICE);
          OBDal.getInstance().save(line);
        }

        created++;

        if (created % 10 == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();

          ctx.reload(SO_DOCTYPE_ID, BPARTNER_ID, PO_WAREHOUSE_ID, SO_PRICELIST_ID,
              PO_PRODUCT_ID);
        }
      }

      OBDal.getInstance().flush();
      SessionHandler.getInstance().commitAndClose();
    } catch (Exception e) {
      errors.add(errorLabel + ": " + e.getClass().getSimpleName()
          + ": " + e.getMessage());
      try {
        OBDal.getInstance().rollbackAndClose();
      } catch (Exception ex) {
        // ignore
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return created;
  }

  private static class ThreadResult {
    final int count;
    final long timeMs;

    ThreadResult(int count, long timeMs) {
      this.count = count;
      this.timeMs = timeMs;
    }
  }

  private class OrderCreationTask implements Callable<ThreadResult> {
    private final int threadId;
    private final int orderCount;
    private final int linesPerOrder;
    private final boolean isSalesOrder;
    private final CopyOnWriteArrayList<String> errors;

    OrderCreationTask(int threadId, int orderCount, int linesPerOrder,
        boolean isSalesOrder, CopyOnWriteArrayList<String> errors) {
      this.threadId = threadId;
      this.orderCount = orderCount;
      this.linesPerOrder = linesPerOrder;
      this.isSalesOrder = isSalesOrder;
      this.errors = errors;
    }

    @Override
    public ThreadResult call() {
      long threadStart = System.nanoTime();
      int created = 0;
      try {
        OBContext.setOBContext(CLIENT_USER_ID, CLIENT_ROLE_ID, CLIENT_CLIENT_ID, CLIENT_ORG_ID);
        OBContext.setAdminMode(true);

        String docTypeId = isSalesOrder ? SO_DOCTYPE_ID : PO_DOCTYPE_ID;
        String priceListId = isSalesOrder ? SO_PRICELIST_ID : PO_PRICELIST_ID;
        String unitPrice = isSalesOrder ? "25.00" : "9.08";

        OrderContext ctx = OrderContext.load(docTypeId, BPARTNER_ID, PO_WAREHOUSE_ID,
            priceListId, PO_PRODUCT_ID);

        for (int i = 0; i < orderCount; i++) {
          String prefix = isSalesOrder ? "SO" : "PO";
          String docNo = STRESS_DOC_PREFIX + prefix + "-T" + threadId + "-" + i;

          Order order = createOrder(ctx.docType, ctx.bp, ctx.warehouse, ctx.priceList,
              ctx.paymentTerm, ctx.paymentMethod, ctx.currency, docNo, isSalesOrder);
          OBDal.getInstance().save(order);

          for (int j = 0; j < linesPerOrder; j++) {
            OrderLine line = createOrderLine(order, ctx.product, ctx.tax,
                (long) (j + 1) * 10, new BigDecimal(j + 1), new BigDecimal(unitPrice),
                new BigDecimal(unitPrice));
            OBDal.getInstance().save(line);
          }

          created++;

          if (created % 20 == 0) {
            OBDal.getInstance().flush();
            OBDal.getInstance().getSession().clear();

            ctx.reload(docTypeId, BPARTNER_ID, PO_WAREHOUSE_ID, priceListId, PO_PRODUCT_ID);
          }
        }

        OBDal.getInstance().flush();
        SessionHandler.getInstance().commitAndClose();
      } catch (Exception e) {
        errors.add(LOG_THREAD + threadId + " (" + (isSalesOrder ? "SO" : "PO") + "): "
            + e.getClass().getSimpleName() + ": " + e.getMessage());
        try {
          OBDal.getInstance().rollbackAndClose();
        } catch (Exception ex) {
          // ignore
        }
      } finally {
        OBContext.restorePreviousMode();
      }

      long threadTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - threadStart);
      return new ThreadResult(created, threadTime);
    }
  }

  private void collectResults(List<Future<ThreadResult>> futures,
      AtomicInteger totalCount, AtomicLong totalTime, CopyOnWriteArrayList<String> errors) {
    for (int i = 0; i < futures.size(); i++) {
      try {
        ThreadResult result = futures.get(i).get(5, TimeUnit.MINUTES);
        totalCount.addAndGet(result.count);
        totalTime.addAndGet(result.timeMs);
      } catch (java.util.concurrent.TimeoutException e) {
        errors.add(LOG_FUTURE + i + " timed out after 5 minutes");
      } catch (java.util.concurrent.ExecutionException e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        errors.add(LOG_FUTURE + i + LOG_FAILED + cause.getClass().getSimpleName()
            + ": " + cause.getMessage());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        errors.add(LOG_FUTURE + i + LOG_FAILED + e.getClass().getSimpleName()
            + ": " + e.getMessage());
      } catch (Exception e) {
        errors.add(LOG_FUTURE + i + LOG_FAILED + e.getClass().getSimpleName()
            + ": " + e.getMessage());
      }
    }
  }

  private void waitForFutures(List<Future<?>> futures) {
    for (Future<?> f : futures) {
      try {
        f.get(5, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        // errors already captured in CopyOnWriteArrayList
      } catch (Exception e) {
        // errors already captured in CopyOnWriteArrayList
      }
    }
  }

  private void printConcurrentResults(String testName, int threadCount, int ordersPerThread,
      int linesPerOrder, int totalCreated, long wallTime, long totalThreadTime,
      List<String> errors) {

    log.info("[STRESS-CONC] === " + testName + " RESULTS ===");
    log.info(LOG_THREADS + threadCount
        + ", Orders/thread: " + ordersPerThread
        + ", Lines/order: " + linesPerOrder);
    log.info("[STRESS-CONC] Total created: " + totalCreated);
    log.info(LOG_WALL_TIME + wallTime + " ms");
    log.info("[STRESS-CONC] Avg per order (wall): "
        + (wallTime / Math.max(totalCreated, 1)) + " ms");
    log.info(LOG_THROUGHPUT
        + (totalCreated * 1000L / Math.max(wallTime, 1)) + ORDERS_PER_SEC);
    log.info("[STRESS-CONC] Sum of thread times: " + totalThreadTime + " ms");
    log.info("[STRESS-CONC] Parallelism efficiency: "
        + (totalThreadTime * 100 / Math.max(wallTime * threadCount, 1)) + "%");

    if (!errors.isEmpty()) {
      log.info(LOG_ERRORS + errors.size() + "):");
      errors.forEach(e -> log.info(LOG_ERROR_ITEM + e));
    }
  }
}
