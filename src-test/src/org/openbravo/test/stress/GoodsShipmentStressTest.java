package org.openbravo.test.stress;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

/** Stress tests for Goods Shipment creation and completion. */
public class GoodsShipmentStressTest extends StressTestBase {

  private static final String STRESS_DOC_PREFIX = "STRESS-SHIP-";
  private static final String LOG_TOTAL_TIME = "[STRESS-SHIP] Total time: ";

  private static final long MAX_MS_PER_SHIPMENT_CREATION = 25;
  private static final long MAX_MS_PER_SHIPMENT_COMPLETE = 185;
  private static final long MAX_MS_PER_LINE_CREATION = 12;
  private static final long MAX_MEMORY_MB_PER_100_SHIPMENTS = 30;

  @Override
  protected String getStressDocPrefix() {
    return STRESS_DOC_PREFIX;
  }

  @After
  @AfterEach
  public void cleanUp() {
    runCleanup(() -> {
      String prefix = STRESS_DOC_PREFIX + "%";
      executeCleanupQuery(
          "DELETE FROM m_inoutline WHERE m_inout_id IN "
              + "(SELECT m_inout_id FROM m_inout WHERE documentno LIKE :prefix)", prefix);
      executeCleanupQuery(
          "DELETE FROM m_inout WHERE documentno LIKE :prefix", prefix);

      String orderPrefix = STRESS_DOC_PREFIX + "ORD-%";
      Number orderCount = (Number) OBDal.getInstance().getSession()
          .createNativeQuery("SELECT count(*) FROM c_order WHERE documentno LIKE :prefix")
          .setParameter(PARAM_PREFIX, orderPrefix)
          .uniqueResult();
      if (orderCount.intValue() > 0) {
        cleanupOrderLinkedShipments(orderPrefix);
        cleanupReservationData(orderPrefix);
        cleanupOrderData(orderPrefix);
      }
    });
  }

  @Test
  public void testBulkShipmentCreation_100() {
    runBulkShipmentCreation(100, 1);
  }

  @Test
  public void testBulkShipmentCreation_500() {
    runBulkShipmentCreation(500, 1);
  }

  @Test
  public void testBulkShipmentCreation_1000() {
    runBulkShipmentCreation(1000, 1);
  }

  @Test
  public void testShipmentWithManyLines_50() {
    runBulkShipmentCreation(1, 50);
  }

  @Test
  public void testShipmentWithManyLines_200() {
    runBulkShipmentCreation(1, 200);
  }

  @Test
  public void testCombined_100ShipmentsWith5Lines() {
    runBulkShipmentCreation(100, 5);
  }

  @Test
  public void testBulkShipmentComplete_10() {
    runBulkShipmentCreateAndComplete(10, 1);
  }

  @Test
  public void testBulkShipmentComplete_50() {
    runBulkShipmentCreateAndComplete(50, 1);
  }

  @Test
  public void testBulkShipmentComplete_100() {
    runBulkShipmentCreateAndComplete(100, 1);
  }

  @Test
  public void testBulkShipmentCompleteWith5Lines_50() {
    runBulkShipmentCreateAndComplete(50, 5);
  }

  @Test
  public void testMemoryUsageDuringBulkShipmentCreation() {
    int totalShipments = 500;
    int flushInterval = 100;

    OBContext.setAdminMode(true);
    try {
      ShipmentContext ctx = ShipmentContext.load(SHIPMENT_DOCTYPE_ID, BPARTNER_ID,
          SO_WAREHOUSE_ID, SO_LOCATOR_ID, SO_PRODUCT_ID);

      Runtime runtime = Runtime.getRuntime();
      forceGarbageCollection();
      long memBefore = runtime.totalMemory() - runtime.freeMemory();
      long startTime = System.nanoTime();

      for (int i = 0; i < totalShipments; i++) {
        ShipmentInOut shipment = createShipmentOrReceipt(ctx.docType, ctx.bp, ctx.warehouse,
            STRESS_DOC_PREFIX + "MEM-" + i, "C-", true);
        OBDal.getInstance().save(shipment);

        for (int j = 0; j < 3; j++) {
          ShipmentInOutLine line = createShipmentOrReceiptLine(shipment, ctx.product, ctx.locator,
              (long) (j + 1) * 10, new BigDecimal(j + 1));
          OBDal.getInstance().save(line);
        }

        if ((i + 1) % flushInterval == 0) {
          flushAndClear();

          long memCurrent = runtime.totalMemory() - runtime.freeMemory();
          long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
          log.info("[STRESS-SHIP] After " + (i + 1) + " shipments - Memory: "
              + ((memCurrent - memBefore) / (1024 * 1024)) + " MB, Time: " + elapsed + " ms");

          ctx.reload(SHIPMENT_DOCTYPE_ID, BPARTNER_ID,
              SO_WAREHOUSE_ID, SO_LOCATOR_ID, SO_PRODUCT_ID);
        }
      }

      OBDal.getInstance().flush();
      long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
      long memAfter = runtime.totalMemory() - runtime.freeMemory();

      log.info("[STRESS-SHIP] === MEMORY USAGE TEST RESULTS ===");
      log.info("[STRESS-SHIP] Total shipments: " + totalShipments + " (3 lines each)");
      log.info(LOG_TOTAL_TIME + totalTime + " ms");
      log.info(
          "[STRESS-SHIP] Memory delta: " + ((memAfter - memBefore) / (1024 * 1024)) + " MB");

      long memDeltaMB = (memAfter - memBefore) / (1024 * 1024);
      long maxExpectedMB = (totalShipments / 100) * MAX_MEMORY_MB_PER_100_SHIPMENTS;
      Assert.assertTrue(
          "Memory usage too high: " + memDeltaMB + " MB (max: " + maxExpectedMB + " MB)",
          memDeltaMB <= maxExpectedMB);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void runBulkShipmentCreation(int totalShipments, int linesPerShipment) {
    OBContext.setAdminMode(true);
    try {
      ShipmentContext ctx = ShipmentContext.load(SHIPMENT_DOCTYPE_ID, BPARTNER_ID,
          SO_WAREHOUSE_ID, SO_LOCATOR_ID, SO_PRODUCT_ID);

      long startTime = System.nanoTime();
      int created = 0;

      for (int i = 0; i < totalShipments; i++) {
        ShipmentInOut shipment = createShipmentOrReceipt(ctx.docType, ctx.bp, ctx.warehouse,
            STRESS_DOC_PREFIX + i, "C-", true);
        OBDal.getInstance().save(shipment);

        for (int j = 0; j < linesPerShipment; j++) {
          ShipmentInOutLine line = createShipmentOrReceiptLine(shipment, ctx.product, ctx.locator,
              (long) (j + 1) * 10, new BigDecimal(j + 1));
          OBDal.getInstance().save(line);
        }

        created++;

        if (created % 100 == 0) {
          flushAndClear();

          ctx.reload(SHIPMENT_DOCTYPE_ID, BPARTNER_ID,
              SO_WAREHOUSE_ID, SO_LOCATOR_ID, SO_PRODUCT_ID);

          long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
          log.info("[STRESS-SHIP] Created " + created + "/" + totalShipments
              + " shipments in " + elapsed + " ms");
        }
      }

      OBDal.getInstance().flush();
      long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

      log.info("[STRESS-SHIP] === BULK CREATION TEST RESULTS ===");
      log.info("[STRESS-SHIP] Total shipments: " + created);
      log.info("[STRESS-SHIP] Lines per shipment: " + linesPerShipment);
      log.info("[STRESS-SHIP] Total lines: " + (created * linesPerShipment));
      log.info(LOG_TOTAL_TIME + totalTime + " ms");
      log.info(
          "[STRESS-SHIP] Avg per shipment: " + (totalTime / Math.max(created, 1)) + " ms");
      log.info("[STRESS-SHIP] Throughput: "
          + (created * 1000L / Math.max(totalTime, 1)) + " shipments/sec");

      Assert.assertEquals(totalShipments, created);

      long avgPerLine = totalTime / Math.max(created * linesPerShipment, 1);
      Assert.assertTrue(
          "Line creation too slow: " + avgPerLine + " ms/line (max: "
              + MAX_MS_PER_LINE_CREATION + " ms)",
          avgPerLine <= MAX_MS_PER_LINE_CREATION);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void runBulkShipmentCreateAndComplete(int totalShipments, int linesPerShipment) {
    OBContext.setAdminMode(true);
    try {
      OrderContext orderCtx = OrderContext.load(SO_DOCTYPE_ID, BPARTNER_ID,
          SO_WAREHOUSE_ID, SO_PRICELIST_ID, SO_PRODUCT_ID);
      ShipmentContext shipCtx = ShipmentContext.load(SHIPMENT_DOCTYPE_ID, BPARTNER_ID,
          SO_WAREHOUSE_ID, SO_LOCATOR_ID, SO_PRODUCT_ID);

      long startTime = System.nanoTime();
      int completed = 0;
      long createTime = 0;
      long completeTime = 0;

      for (int i = 0; i < totalShipments; i++) {
        long createStart = System.nanoTime();

        Order order = createOrder(orderCtx.docType, orderCtx.bp, orderCtx.warehouse,
            orderCtx.priceList, orderCtx.paymentTerm, orderCtx.paymentMethod,
            orderCtx.currency, STRESS_DOC_PREFIX + "ORD-" + i, true);
        OBDal.getInstance().save(order);

        List<String> orderLineIds = new ArrayList<>();
        for (int j = 0; j < linesPerShipment; j++) {
          OrderLine orderLine = createOrderLine(order, orderCtx.product, orderCtx.tax,
              (long) (j + 1) * 10,
              new BigDecimal(j + 1), new BigDecimal("25.00"), new BigDecimal("25.00"));
          OBDal.getInstance().save(orderLine);
          OBDal.getInstance().flush();
          orderLineIds.add(orderLine.getId());
        }

        OBDal.getInstance().flush();

        callStoredProc("c_order_post1", order.getId());
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(order);

        ShipmentInOut shipment = createShipmentOrReceipt(shipCtx.docType, shipCtx.bp,
            shipCtx.warehouse, STRESS_DOC_PREFIX + i, "C-", true);
        shipment.setSalesOrder(order);
        OBDal.getInstance().save(shipment);

        for (String olId : orderLineIds) {
          OrderLine ol = OBDal.getInstance().get(OrderLine.class, olId);
          ShipmentInOutLine shipLine = createShipmentOrReceiptLine(shipment, ol.getProduct(),
              shipCtx.locator, ol.getLineNo(), ol.getOrderedQuantity());
          shipLine.setSalesOrderLine(ol);
          OBDal.getInstance().save(shipLine);
        }

        OBDal.getInstance().flush();
        createTime += TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - createStart);

        long completeStart = System.nanoTime();
        callStoredProc("m_inout_post", shipment.getId());
        OBDal.getInstance().flush();
        completeTime += TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - completeStart);

        OBDal.getInstance().refresh(shipment);
        Assert.assertEquals("CO", shipment.getDocumentStatus());

        completed++;

        if (completed % 10 == 0) {
          OBDal.getInstance().getSession().clear();

          orderCtx.reload(SO_DOCTYPE_ID, BPARTNER_ID,
              SO_WAREHOUSE_ID, SO_PRICELIST_ID, SO_PRODUCT_ID);
          shipCtx.reload(SHIPMENT_DOCTYPE_ID, BPARTNER_ID,
              SO_WAREHOUSE_ID, SO_LOCATOR_ID, SO_PRODUCT_ID);

          long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
          log.info("[STRESS-SHIP] Completed " + completed + "/" + totalShipments
              + " shipments in " + elapsed + " ms (create: " + createTime
              + " ms, complete: " + completeTime + " ms)");
        }
      }

      long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

      log.info("[STRESS-SHIP] === CREATE & COMPLETE TEST RESULTS ===");
      log.info("[STRESS-SHIP] Total shipments completed: " + completed);
      log.info("[STRESS-SHIP] Lines per shipment: " + linesPerShipment);
      log.info(LOG_TOTAL_TIME + totalTime + " ms");
      log.info("[STRESS-SHIP] Create time: " + createTime + " ms");
      log.info("[STRESS-SHIP] Complete time (m_inout_post): " + completeTime + " ms");
      log.info(
          "[STRESS-SHIP] Avg per shipment: " + (totalTime / Math.max(completed, 1)) + " ms");
      log.info("[STRESS-SHIP] Avg complete time: "
          + (completeTime / Math.max(completed, 1)) + " ms");
      log.info("[STRESS-SHIP] Throughput: "
          + (completed * 1000L / Math.max(totalTime, 1)) + " shipments/sec");

      Assert.assertEquals(totalShipments, completed);

      long avgPerShipment = totalTime / Math.max(completed, 1);
      long avgComplete = completeTime / Math.max(completed, 1);
      Assert.assertTrue(
          "Shipment create+complete too slow: " + avgPerShipment + " ms/shipment (max: "
              + (MAX_MS_PER_SHIPMENT_CREATION + MAX_MS_PER_SHIPMENT_COMPLETE) + " ms)",
          avgPerShipment <= MAX_MS_PER_SHIPMENT_CREATION + MAX_MS_PER_SHIPMENT_COMPLETE);
      Assert.assertTrue(
          "Shipment completion (m_inout_post) too slow: " + avgComplete
              + " ms/shipment (max: " + MAX_MS_PER_SHIPMENT_COMPLETE + " ms)",
          avgComplete <= MAX_MS_PER_SHIPMENT_COMPLETE);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
