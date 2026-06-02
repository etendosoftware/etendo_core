package org.openbravo.test.stress;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;

/** Stress tests for Sales Order creation and completion. */
public class OrderStressTest extends StressTestBase {

  private static final String STRESS_DOC_PREFIX = "STRESS-";
  private static final String LOG_TOTAL_TIME = "[STRESS] Total time: ";
  private static final String LOG_AVG_PER_ORDER = "[STRESS] Avg per order: ";
  private static final BigDecimal UNIT_PRICE = new BigDecimal("25.00");

  private static final long MAX_MS_PER_ORDER_CREATION = 15;
  private static final long MAX_MS_PER_ORDER_COMPLETE = 100;

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

  @After
  public void cleanUp() {
    OBContext.setOBContext(CLIENT_USER_ID, CLIENT_ROLE_ID, CLIENT_CLIENT_ID, CLIENT_ORG_ID);
    runCleanup(() -> cleanupOrderData(STRESS_DOC_PREFIX + "%"));
  }

  @Test
  public void testBulkOrderCreation_100Orders() {
    runBulkOrderCreation(100, 1);
  }

  @Test
  public void testBulkOrderCreation_500Orders() {
    runBulkOrderCreation(500, 1);
  }

  @Test
  public void testBulkOrderCreation_1000Orders() {
    runBulkOrderCreation(1000, 1);
  }

  @Test
  public void testOrderWithManyLines_50Lines() {
    runBulkOrderCreation(1, 50);
  }

  @Test
  public void testOrderWithManyLines_200Lines() {
    runBulkOrderCreation(1, 200);
  }

  @Test
  public void testCombined_100OrdersWith5Lines() {
    runBulkOrderCreation(100, 5);
  }

  @Test
  public void testCombined_500OrdersWith10Lines() {
    runBulkOrderCreation(500, 10);
  }

  @Test
  public void testBulkOrderQuery_Performance() {
    int totalOrders = 200;
    int linesPerOrder = 3;
    String queryPrefix = STRESS_DOC_PREFIX + "QUERY-";
    runBulkOrderCreation(totalOrders, linesPerOrder, queryPrefix);

    verifyOrderQueryResults(queryPrefix + "%", totalOrders, linesPerOrder, "[STRESS] ");
  }

  @Test
  public void testBulkOrderCreationWithFlushIntervals() {
    int totalOrders = 1000;
    int flushInterval = 50;

    OBContext.setAdminMode(true);
    try {
      OrderContext ctx = OrderContext.load(SO_DOCTYPE_ID, BPARTNER_ID, PO_WAREHOUSE_ID,
          SO_PRICELIST_ID, PO_PRODUCT_ID);

      long startTime = System.nanoTime();
      int created = 0;

      for (int i = 0; i < totalOrders; i++) {
        Order order = createOrder(ctx.docType, ctx.bp, ctx.warehouse, ctx.priceList,
            ctx.paymentTerm, ctx.paymentMethod, ctx.currency,
            STRESS_DOC_PREFIX + "FLUSH-" + i, true);
        OBDal.getInstance().save(order);

        OrderLine line = createOrderLine(order, ctx.product, ctx.tax, 10L, BigDecimal.TEN,
            UNIT_PRICE, UNIT_PRICE);
        OBDal.getInstance().save(line);

        created++;

        if (created % flushInterval == 0) {
          flushAndClear();
          ctx.reload(SO_DOCTYPE_ID, BPARTNER_ID, PO_WAREHOUSE_ID, SO_PRICELIST_ID,
              PO_PRODUCT_ID);

          long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
          log.info(
              "[STRESS] Created " + created + "/" + totalOrders + " orders in " + elapsed + " ms");
        }
      }

      OBDal.getInstance().flush();
      long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

      log.info("[STRESS] === FLUSH INTERVAL TEST RESULTS ===");
      log.info(
          "[STRESS] Total orders created: " + created + " (flush every " + flushInterval + ")");
      log.info(LOG_TOTAL_TIME + totalTime + " ms");
      log.info(LOG_AVG_PER_ORDER + (totalTime / created) + " ms");

      Assert.assertEquals(totalOrders, created);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testMemoryUsageDuringBulkCreation() {
    int totalOrders = 500;
    int flushInterval = 100;

    OBContext.setAdminMode(true);
    try {
      OrderContext ctx = OrderContext.load(SO_DOCTYPE_ID, BPARTNER_ID, PO_WAREHOUSE_ID,
          SO_PRICELIST_ID, PO_PRODUCT_ID);

      Runtime runtime = Runtime.getRuntime();
      forceGarbageCollection();
      long memBefore = runtime.totalMemory() - runtime.freeMemory();

      long startTime = System.nanoTime();

      for (int i = 0; i < totalOrders; i++) {
        Order order = createOrder(ctx.docType, ctx.bp, ctx.warehouse, ctx.priceList,
            ctx.paymentTerm, ctx.paymentMethod, ctx.currency,
            STRESS_DOC_PREFIX + "MEM-" + i, true);
        OBDal.getInstance().save(order);

        for (int j = 0; j < 5; j++) {
          OrderLine line = createOrderLine(order, ctx.product, ctx.tax, (long) (j + 1) * 10,
              new BigDecimal(j + 1), UNIT_PRICE, UNIT_PRICE);
          OBDal.getInstance().save(line);
        }

        if ((i + 1) % flushInterval == 0) {
          flushAndClear();

          long memCurrent = runtime.totalMemory() - runtime.freeMemory();
          long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
          log.info("[STRESS] After " + (i + 1) + " orders - Memory: "
              + ((memCurrent - memBefore) / (1024 * 1024)) + " MB, Time: " + elapsed + " ms");

          ctx.reload(SO_DOCTYPE_ID, BPARTNER_ID, PO_WAREHOUSE_ID, SO_PRICELIST_ID,
              PO_PRODUCT_ID);
        }
      }

      OBDal.getInstance().flush();
      long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
      long memAfter = runtime.totalMemory() - runtime.freeMemory();

      log.info("[STRESS] === MEMORY USAGE TEST RESULTS ===");
      log.info("[STRESS] Total orders: " + totalOrders + " (5 lines each)");
      log.info(LOG_TOTAL_TIME + totalTime + " ms");
      log.info(
          "[STRESS] Memory delta: " + ((memAfter - memBefore) / (1024 * 1024)) + " MB");

      Assert.assertTrue("Memory usage should be reported", totalTime > 0);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void runBulkOrderCreation(int totalOrders, int linesPerOrder) {
    runBulkOrderCreation(totalOrders, linesPerOrder, STRESS_DOC_PREFIX);
  }

  private void runBulkOrderCreation(int totalOrders, int linesPerOrder, String docPrefix) {
    OBContext.setAdminMode(true);
    try {
      OrderContext ctx = OrderContext.load(SO_DOCTYPE_ID, BPARTNER_ID, PO_WAREHOUSE_ID,
          SO_PRICELIST_ID, PO_PRODUCT_ID);

      long startTime = System.nanoTime();
      int created = 0;

      for (int i = 0; i < totalOrders; i++) {
        Order order = createOrder(ctx.docType, ctx.bp, ctx.warehouse, ctx.priceList,
            ctx.paymentTerm, ctx.paymentMethod, ctx.currency, docPrefix + i, true);
        OBDal.getInstance().save(order);

        for (int j = 0; j < linesPerOrder; j++) {
          OrderLine line = createOrderLine(order, ctx.product, ctx.tax, (long) (j + 1) * 10,
              new BigDecimal(j + 1), UNIT_PRICE, UNIT_PRICE);
          OBDal.getInstance().save(line);
        }

        created++;

        if (created % 100 == 0) {
          flushAndClear();
          ctx.reload(SO_DOCTYPE_ID, BPARTNER_ID, PO_WAREHOUSE_ID, SO_PRICELIST_ID,
              PO_PRODUCT_ID);

          long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
          log.info(
              "[STRESS] Created " + created + "/" + totalOrders + " orders in " + elapsed + " ms");
        }
      }

      OBDal.getInstance().flush();
      long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

      log.info("[STRESS] === BULK CREATION TEST RESULTS ===");
      log.info("[STRESS] Total orders: " + created);
      log.info("[STRESS] Lines per order: " + linesPerOrder);
      log.info("[STRESS] Total lines: " + (created * linesPerOrder));
      log.info(LOG_TOTAL_TIME + totalTime + " ms");
      log.info(LOG_AVG_PER_ORDER + (totalTime / Math.max(created, 1)) + " ms");
      log.info("[STRESS] Throughput: "
          + (created * 1000L / Math.max(totalTime, 1)) + " orders/sec");

      Assert.assertEquals(totalOrders, created);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // ==========================================================================
  // Sales Order Create & Complete tests (c_order_post1)
  // ==========================================================================

  @Test
  public void testBulkSalesOrderComplete_10() {
    runBulkOrderCreateAndComplete(10, 1);
  }

  @Test
  public void testBulkSalesOrderComplete_50() {
    runBulkOrderCreateAndComplete(50, 1);
  }

  @Test
  public void testBulkSalesOrderComplete_100() {
    runBulkOrderCreateAndComplete(100, 1);
  }

  @Test
  public void testBulkSalesOrderCompleteWith5Lines_50() {
    runBulkOrderCreateAndComplete(50, 5);
  }

  private void runBulkOrderCreateAndComplete(int totalOrders, int linesPerOrder) {
    OBContext.setAdminMode(true);
    try {
      OrderContext ctx = OrderContext.load(SO_DOCTYPE_ID, BPARTNER_ID, PO_WAREHOUSE_ID,
          SO_PRICELIST_ID, PO_PRODUCT_ID);

      long startTime = System.nanoTime();
      int completed = 0;
      long createTime = 0;
      long completeTime = 0;

      for (int i = 0; i < totalOrders; i++) {
        long createStart = System.nanoTime();

        Order order = createOrder(ctx.docType, ctx.bp, ctx.warehouse, ctx.priceList,
            ctx.paymentTerm, ctx.paymentMethod, ctx.currency,
            STRESS_DOC_PREFIX + "CO-" + i, true);
        order.setOrderDate(getOpenPeriodDate());
        order.setAccountingDate(getOpenPeriodDate());
        OBDal.getInstance().save(order);

        for (int j = 0; j < linesPerOrder; j++) {
          OrderLine line = createOrderLine(order, ctx.product, ctx.tax, (long) (j + 1) * 10,
              new BigDecimal(j + 1), UNIT_PRICE, UNIT_PRICE);
          line.setOrderDate(getOpenPeriodDate());
          line.setScheduledDeliveryDate(getOpenPeriodDate());
          OBDal.getInstance().save(line);
        }

        OBDal.getInstance().flush();
        createTime += TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - createStart);

        completeTime += completeOrderAndVerify(order);

        completed++;

        if (completed % 10 == 0) {
          OBDal.getInstance().getSession().clear();
          ctx.reload(SO_DOCTYPE_ID, BPARTNER_ID, PO_WAREHOUSE_ID, SO_PRICELIST_ID,
              PO_PRODUCT_ID);

          long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
          log.info("[STRESS] Completed " + completed + "/" + totalOrders
              + " sales orders in " + elapsed + " ms (create: " + createTime
              + " ms, complete: " + completeTime + " ms)");
        }
      }

      long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

      log.info("[STRESS] === SO CREATE & COMPLETE TEST RESULTS ===");
      log.info("[STRESS] Total sales orders completed: " + completed);
      log.info("[STRESS] Lines per order: " + linesPerOrder);
      log.info(LOG_TOTAL_TIME + totalTime + " ms");
      log.info("[STRESS] Create time: " + createTime + " ms");
      log.info("[STRESS] Complete time (c_order_post1): " + completeTime + " ms");
      log.info(LOG_AVG_PER_ORDER
          + (totalTime / Math.max(completed, 1)) + " ms");
      log.info("[STRESS] Avg complete time: "
          + (completeTime / Math.max(completed, 1)) + " ms");
      log.info("[STRESS] Throughput: "
          + (completed * 1000L / Math.max(totalTime, 1)) + " orders/sec");

      Assert.assertEquals(totalOrders, completed);

      long avgPerOrder = totalTime / Math.max(completed, 1);
      long avgComplete = completeTime / Math.max(completed, 1);
      Assert.assertTrue(
          "Sales order create+complete too slow: " + avgPerOrder + " ms/order (max: "
              + (MAX_MS_PER_ORDER_CREATION + MAX_MS_PER_ORDER_COMPLETE) + " ms)",
          avgPerOrder <= MAX_MS_PER_ORDER_CREATION + MAX_MS_PER_ORDER_COMPLETE);
      Assert.assertTrue(
          "Sales order completion (c_order_post1) too slow: " + avgComplete
              + " ms/order (max: " + MAX_MS_PER_ORDER_COMPLETE + " ms)",
          avgComplete <= MAX_MS_PER_ORDER_COMPLETE);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
