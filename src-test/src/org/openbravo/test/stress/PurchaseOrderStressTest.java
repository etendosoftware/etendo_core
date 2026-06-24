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

/** Stress tests for Purchase Order creation and completion. */
public class PurchaseOrderStressTest extends StressTestBase {

  private static final String STRESS_DOC_PREFIX = "STRESS-PO-";
  private static final String LOG_TOTAL_TIME = "[STRESS-PO] Total time: ";
  private static final BigDecimal UNIT_PRICE_PO = new BigDecimal("9.08");

  private static final long MAX_MS_PER_ORDER_CREATION = 15;
  private static final long MAX_MS_PER_ORDER_COMPLETE = 100;
  private static final long MAX_MS_PER_LINE_CREATION = 15;
  private static final long MAX_MEMORY_MB_PER_100_ORDERS = 50;

  @Override
  protected String getStressDocPrefix() {
    return STRESS_DOC_PREFIX;
  }

  @After
  public void cleanUp() {
    runCleanup(() -> {
      String prefix = STRESS_DOC_PREFIX + "%";
      cleanupOrderLinkedShipments(prefix);
      cleanupReservationData(prefix);
      cleanupOrderData(prefix);
    });
  }

  @Test
  public void testBulkPurchaseOrderCreation_100() {
    runBulkOrderCreation(100, 1);
  }

  @Test
  public void testBulkPurchaseOrderCreation_500() {
    runBulkOrderCreation(500, 1);
  }

  @Test
  public void testBulkPurchaseOrderCreation_1000() {
    runBulkOrderCreation(1000, 1);
  }

  @Test
  public void testPurchaseOrderWithManyLines_50() {
    runBulkOrderCreation(1, 50);
  }

  @Test
  public void testPurchaseOrderWithManyLines_200() {
    runBulkOrderCreation(1, 200);
  }

  @Test
  public void testCombined_100PurchaseOrdersWith5Lines() {
    runBulkOrderCreation(100, 5);
  }

  @Test
  public void testBulkPurchaseOrderComplete_10() {
    runBulkOrderCreateAndComplete(10, 1);
  }

  @Test
  public void testBulkPurchaseOrderComplete_50() {
    runBulkOrderCreateAndComplete(50, 1);
  }

  @Test
  public void testBulkPurchaseOrderComplete_100() {
    runBulkOrderCreateAndComplete(100, 1);
  }

  @Test
  public void testBulkPurchaseOrderCompleteWith5Lines_50() {
    runBulkOrderCreateAndComplete(50, 5);
  }

  @Test
  public void testBulkPurchaseOrderQuery_Performance() {
    int totalOrders = 200;
    int linesPerOrder = 3;
    runBulkOrderCreation(totalOrders, linesPerOrder);

    verifyOrderQueryResults(STRESS_DOC_PREFIX + "%", totalOrders, linesPerOrder, "[STRESS-PO] ");
  }

  @Test
  public void testMemoryUsageDuringBulkPurchaseOrderCreation() {
    int totalOrders = 500;
    int flushInterval = 100;

    OBContext.setAdminMode(true);
    try {
      OrderContext ctx = OrderContext.load(PO_DOCTYPE_ID, BPARTNER_ID, PO_WAREHOUSE_ID,
          PO_PRICELIST_ID, PO_PRODUCT_ID);

      Runtime runtime = Runtime.getRuntime();
      forceGarbageCollection();
      long memBefore = runtime.totalMemory() - runtime.freeMemory();
      long startTime = System.nanoTime();

      for (int i = 0; i < totalOrders; i++) {
        Order order = createOrder(ctx.docType, ctx.bp, ctx.warehouse, ctx.priceList,
            ctx.paymentTerm, ctx.paymentMethod, ctx.currency,
            STRESS_DOC_PREFIX + "MEM-" + i, false);
        OBDal.getInstance().save(order);

        for (int j = 0; j < 3; j++) {
          OrderLine line = createOrderLine(order, ctx.product, ctx.tax, (long) (j + 1) * 10,
              new BigDecimal(j + 1), UNIT_PRICE_PO, UNIT_PRICE_PO);
          OBDal.getInstance().save(line);
        }

        if ((i + 1) % flushInterval == 0) {
          flushAndClear();

          long memCurrent = runtime.totalMemory() - runtime.freeMemory();
          long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
          log.info("[STRESS-PO] After " + (i + 1) + " orders - Memory: "
              + ((memCurrent - memBefore) / (1024 * 1024)) + " MB, Time: " + elapsed + " ms");

          ctx.reload(PO_DOCTYPE_ID, BPARTNER_ID, PO_WAREHOUSE_ID, PO_PRICELIST_ID,
              PO_PRODUCT_ID);
        }
      }

      OBDal.getInstance().flush();
      long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
      long memAfter = runtime.totalMemory() - runtime.freeMemory();

      log.info("[STRESS-PO] === MEMORY USAGE TEST RESULTS ===");
      log.info("[STRESS-PO] Total orders: " + totalOrders + " (3 lines each)");
      log.info(LOG_TOTAL_TIME + totalTime + " ms");
      log.info(
          "[STRESS-PO] Memory delta: " + ((memAfter - memBefore) / (1024 * 1024)) + " MB");

      long memDeltaMB = (memAfter - memBefore) / (1024 * 1024);
      long maxExpectedMB = (totalOrders / 100) * MAX_MEMORY_MB_PER_100_ORDERS;
      Assert.assertTrue(
          "Memory usage too high: " + memDeltaMB + " MB (max: " + maxExpectedMB + " MB)",
          memDeltaMB <= maxExpectedMB);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void runBulkOrderCreation(int totalOrders, int linesPerOrder) {
    OBContext.setAdminMode(true);
    try {
      OrderContext ctx = OrderContext.load(PO_DOCTYPE_ID, BPARTNER_ID, PO_WAREHOUSE_ID,
          PO_PRICELIST_ID, PO_PRODUCT_ID);

      long startTime = System.nanoTime();
      int created = 0;

      for (int i = 0; i < totalOrders; i++) {
        Order order = createOrder(ctx.docType, ctx.bp, ctx.warehouse, ctx.priceList,
            ctx.paymentTerm, ctx.paymentMethod, ctx.currency,
            STRESS_DOC_PREFIX + i, false);
        OBDal.getInstance().save(order);

        for (int j = 0; j < linesPerOrder; j++) {
          OrderLine line = createOrderLine(order, ctx.product, ctx.tax, (long) (j + 1) * 10,
              new BigDecimal(j + 1), UNIT_PRICE_PO, UNIT_PRICE_PO);
          OBDal.getInstance().save(line);
        }

        created++;

        if (created % 100 == 0) {
          flushAndClear();
          ctx.reload(PO_DOCTYPE_ID, BPARTNER_ID, PO_WAREHOUSE_ID, PO_PRICELIST_ID,
              PO_PRODUCT_ID);

          long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
          log.info("[STRESS-PO] Created " + created + "/" + totalOrders
              + " purchase orders in " + elapsed + " ms");
        }
      }

      OBDal.getInstance().flush();
      long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

      log.info("[STRESS-PO] === BULK CREATION TEST RESULTS ===");
      log.info("[STRESS-PO] Total purchase orders: " + created);
      log.info("[STRESS-PO] Lines per order: " + linesPerOrder);
      log.info("[STRESS-PO] Total lines: " + (created * linesPerOrder));
      log.info(LOG_TOTAL_TIME + totalTime + " ms");
      log.info(
          "[STRESS-PO] Avg per order: " + (totalTime / Math.max(created, 1)) + " ms");
      log.info("[STRESS-PO] Throughput: "
          + (created * 1000L / Math.max(totalTime, 1)) + " orders/sec");

      Assert.assertEquals(totalOrders, created);

      long avgPerLine = totalTime / Math.max(created * linesPerOrder, 1);
      Assert.assertTrue(
          "Line creation too slow: " + avgPerLine + " ms/line (max: "
              + MAX_MS_PER_LINE_CREATION + " ms)",
          avgPerLine <= MAX_MS_PER_LINE_CREATION);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void runBulkOrderCreateAndComplete(int totalOrders, int linesPerOrder) {
    OBContext.setAdminMode(true);
    try {
      OrderContext ctx = OrderContext.load(PO_DOCTYPE_ID, BPARTNER_ID, PO_WAREHOUSE_ID,
          PO_PRICELIST_ID, PO_PRODUCT_ID);

      long startTime = System.nanoTime();
      int completed = 0;
      long createTime = 0;
      long completeTime = 0;

      for (int i = 0; i < totalOrders; i++) {
        long createStart = System.nanoTime();

        Order order = createOrder(ctx.docType, ctx.bp, ctx.warehouse, ctx.priceList,
            ctx.paymentTerm, ctx.paymentMethod, ctx.currency,
            STRESS_DOC_PREFIX + "CO-" + i, false);
        OBDal.getInstance().save(order);

        for (int j = 0; j < linesPerOrder; j++) {
          OrderLine line = createOrderLine(order, ctx.product, ctx.tax, (long) (j + 1) * 10,
              new BigDecimal(j + 1), UNIT_PRICE_PO, UNIT_PRICE_PO);
          OBDal.getInstance().save(line);
        }

        OBDal.getInstance().flush();
        createTime += TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - createStart);

        completeTime += completeOrderAndVerify(order);

        completed++;

        if (completed % 10 == 0) {
          OBDal.getInstance().getSession().clear();
          ctx.reload(PO_DOCTYPE_ID, BPARTNER_ID, PO_WAREHOUSE_ID, PO_PRICELIST_ID,
              PO_PRODUCT_ID);

          long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
          log.info("[STRESS-PO] Completed " + completed + "/" + totalOrders
              + " purchase orders in " + elapsed + " ms (create: " + createTime
              + " ms, complete: " + completeTime + " ms)");
        }
      }

      long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

      log.info("[STRESS-PO] === CREATE & COMPLETE TEST RESULTS ===");
      log.info("[STRESS-PO] Total purchase orders completed: " + completed);
      log.info("[STRESS-PO] Lines per order: " + linesPerOrder);
      log.info(LOG_TOTAL_TIME + totalTime + " ms");
      log.info("[STRESS-PO] Create time: " + createTime + " ms");
      log.info("[STRESS-PO] Complete time (c_order_post1): " + completeTime + " ms");
      log.info(
          "[STRESS-PO] Avg per order: " + (totalTime / Math.max(completed, 1)) + " ms");
      log.info("[STRESS-PO] Avg complete time: "
          + (completeTime / Math.max(completed, 1)) + " ms");
      log.info("[STRESS-PO] Throughput: "
          + (completed * 1000L / Math.max(totalTime, 1)) + " orders/sec");

      Assert.assertEquals(totalOrders, completed);

      long avgPerOrder = totalTime / Math.max(completed, 1);
      long avgComplete = completeTime / Math.max(completed, 1);
      Assert.assertTrue(
          "Purchase order create+complete too slow: " + avgPerOrder + " ms/order (max: "
              + (MAX_MS_PER_ORDER_CREATION + MAX_MS_PER_ORDER_COMPLETE) + " ms)",
          avgPerOrder <= MAX_MS_PER_ORDER_CREATION + MAX_MS_PER_ORDER_COMPLETE);
      Assert.assertTrue(
          "Purchase order completion (c_order_post1) too slow: " + avgComplete
              + " ms/order (max: " + MAX_MS_PER_ORDER_COMPLETE + " ms)",
          avgComplete <= MAX_MS_PER_ORDER_COMPLETE);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
