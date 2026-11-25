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

/** Stress tests for Goods Receipt creation and completion. */
public class GoodsReceiptStressTest extends StressTestBase {

  private static final String STRESS_DOC_PREFIX = "STRESS-GR-";
  private static final String LOG_TOTAL_TIME = "[STRESS-GR] Total time: ";

  private static final long MAX_MS_PER_RECEIPT_COMPLETE = 200;
  private static final long MAX_MS_PER_LINE_CREATION = 15;
  private static final long MAX_MEMORY_MB_PER_100_RECEIPTS = 50;
  private static final long MAX_MS_PER_FULL_FLOW = 400;

  @Override
  protected String getStressDocPrefix() {
    return STRESS_DOC_PREFIX;
  }

  @After
  @AfterEach
  public void cleanUp() {
    runCleanup(() -> {
      String prefix = STRESS_DOC_PREFIX + "%";
      cleanupShipmentData(prefix);

      String orderPrefix = STRESS_DOC_PREFIX + "PO-%";
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
  public void testBulkReceiptCreation_100() {
    runBulkReceiptCreation(100, 1);
  }

  @Test
  public void testBulkReceiptCreation_500() {
    runBulkReceiptCreation(500, 1);
  }

  @Test
  public void testBulkReceiptCreation_1000() {
    runBulkReceiptCreation(1000, 1);
  }

  @Test
  public void testReceiptWithManyLines_50() {
    runBulkReceiptCreation(1, 50);
  }

  @Test
  public void testReceiptWithManyLines_200() {
    runBulkReceiptCreation(1, 200);
  }

  @Test
  public void testCombined_100ReceiptsWith5Lines() {
    runBulkReceiptCreation(100, 5);
  }

  @Test
  public void testFullPurchaseFlow_10() {
    runFullPurchaseFlow(10, 1);
  }

  @Test
  public void testFullPurchaseFlow_50() {
    runFullPurchaseFlow(50, 1);
  }

  @Test
  public void testFullPurchaseFlow_100() {
    runFullPurchaseFlow(100, 1);
  }

  @Test
  public void testFullPurchaseFlowWith5Lines_10() {
    runFullPurchaseFlow(10, 5);
  }

  @Test
  public void testFullPurchaseFlowWith5Lines_50() {
    runFullPurchaseFlow(50, 5);
  }

  @Test
  public void testMemoryUsageDuringBulkReceiptCreation() {
    int totalReceipts = 500;
    int flushInterval = 100;

    OBContext.setAdminMode(true);
    try {
      ShipmentContext ctx = ShipmentContext.load(RECEIPT_DOCTYPE_ID, BPARTNER_ID,
          PO_WAREHOUSE_ID, PO_LOCATOR_ID, PO_PRODUCT_ID);

      Runtime runtime = Runtime.getRuntime();
      forceGarbageCollection();
      long memBefore = runtime.totalMemory() - runtime.freeMemory();
      long startTime = System.nanoTime();

      for (int i = 0; i < totalReceipts; i++) {
        ShipmentInOut receipt = createShipmentOrReceipt(ctx.docType, ctx.bp, ctx.warehouse,
            STRESS_DOC_PREFIX + "MEM-" + i, "V+", false);
        OBDal.getInstance().save(receipt);

        for (int j = 0; j < 3; j++) {
          ShipmentInOutLine line = createShipmentOrReceiptLine(receipt, ctx.product, ctx.locator,
              (long) (j + 1) * 10, new BigDecimal(j + 1));
          OBDal.getInstance().save(line);
        }

        if ((i + 1) % flushInterval == 0) {
          flushAndClear();

          long memCurrent = runtime.totalMemory() - runtime.freeMemory();
          long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
          log.info("[STRESS-GR] After " + (i + 1) + " receipts - Memory: "
              + ((memCurrent - memBefore) / (1024 * 1024)) + " MB, Time: " + elapsed + " ms");

          ctx.reload(RECEIPT_DOCTYPE_ID, BPARTNER_ID,
              PO_WAREHOUSE_ID, PO_LOCATOR_ID, PO_PRODUCT_ID);
        }
      }

      OBDal.getInstance().flush();
      long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
      long memAfter = runtime.totalMemory() - runtime.freeMemory();

      log.info("[STRESS-GR] === MEMORY USAGE TEST RESULTS ===");
      log.info("[STRESS-GR] Total receipts: " + totalReceipts + " (3 lines each)");
      log.info(LOG_TOTAL_TIME + totalTime + " ms");
      log.info(
          "[STRESS-GR] Memory delta: " + ((memAfter - memBefore) / (1024 * 1024)) + " MB");

      long memDeltaMB = (memAfter - memBefore) / (1024 * 1024);
      long maxExpectedMB = (totalReceipts / 100) * MAX_MEMORY_MB_PER_100_RECEIPTS;
      Assert.assertTrue(
          "Memory usage too high: " + memDeltaMB + " MB (max: " + maxExpectedMB + " MB)",
          memDeltaMB <= maxExpectedMB);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void runBulkReceiptCreation(int totalReceipts, int linesPerReceipt) {
    OBContext.setAdminMode(true);
    try {
      ShipmentContext ctx = ShipmentContext.load(RECEIPT_DOCTYPE_ID, BPARTNER_ID,
          PO_WAREHOUSE_ID, PO_LOCATOR_ID, PO_PRODUCT_ID);

      long startTime = System.nanoTime();
      int created = 0;

      for (int i = 0; i < totalReceipts; i++) {
        ShipmentInOut receipt = createShipmentOrReceipt(ctx.docType, ctx.bp, ctx.warehouse,
            STRESS_DOC_PREFIX + i, "V+", false);
        OBDal.getInstance().save(receipt);

        for (int j = 0; j < linesPerReceipt; j++) {
          ShipmentInOutLine line = createShipmentOrReceiptLine(receipt, ctx.product, ctx.locator,
              (long) (j + 1) * 10, new BigDecimal(j + 1));
          OBDal.getInstance().save(line);
        }

        created++;

        if (created % 100 == 0) {
          flushAndClear();

          ctx.reload(RECEIPT_DOCTYPE_ID, BPARTNER_ID,
              PO_WAREHOUSE_ID, PO_LOCATOR_ID, PO_PRODUCT_ID);

          long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
          log.info("[STRESS-GR] Created " + created + "/" + totalReceipts
              + " goods receipts in " + elapsed + " ms");
        }
      }

      OBDal.getInstance().flush();
      long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

      log.info("[STRESS-GR] === BULK CREATION TEST RESULTS ===");
      log.info("[STRESS-GR] Total goods receipts: " + created);
      log.info("[STRESS-GR] Lines per receipt: " + linesPerReceipt);
      log.info("[STRESS-GR] Total lines: " + (created * linesPerReceipt));
      log.info(LOG_TOTAL_TIME + totalTime + " ms");
      log.info(
          "[STRESS-GR] Avg per receipt: " + (totalTime / Math.max(created, 1)) + " ms");
      log.info("[STRESS-GR] Throughput: "
          + (created * 1000L / Math.max(totalTime, 1)) + " receipts/sec");

      Assert.assertEquals(totalReceipts, created);

      long avgPerLine = totalTime / Math.max(created * linesPerReceipt, 1);
      Assert.assertTrue(
          "Line creation too slow: " + avgPerLine + " ms/line (max: "
              + MAX_MS_PER_LINE_CREATION + " ms)",
          avgPerLine <= MAX_MS_PER_LINE_CREATION);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void runFullPurchaseFlow(int totalFlows, int linesPerDocument) {
    OBContext.setAdminMode(true);
    try {
      OrderContext orderCtx = OrderContext.load(PO_DOCTYPE_ID, BPARTNER_ID,
          PO_WAREHOUSE_ID, PO_PRICELIST_ID, PO_PRODUCT_ID);
      ShipmentContext receiptCtx = ShipmentContext.load(RECEIPT_DOCTYPE_ID, BPARTNER_ID,
          PO_WAREHOUSE_ID, PO_LOCATOR_ID, PO_PRODUCT_ID);

      long startTime = System.nanoTime();
      int completed = 0;
      long poCreateTime = 0;
      long poCompleteTime = 0;
      long receiptCreateTime = 0;
      long receiptCompleteTime = 0;

      for (int i = 0; i < totalFlows; i++) {
        long poCreateStart = System.nanoTime();

        Order po = createOrder(orderCtx.docType, orderCtx.bp, orderCtx.warehouse,
            orderCtx.priceList, orderCtx.paymentTerm, orderCtx.paymentMethod,
            orderCtx.currency, STRESS_DOC_PREFIX + "PO-" + i, false);
        OBDal.getInstance().save(po);

        List<String> orderLineIds = new ArrayList<>();
        for (int j = 0; j < linesPerDocument; j++) {
          OrderLine orderLine = createOrderLine(po, orderCtx.product, orderCtx.tax,
              (long) (j + 1) * 10,
              new BigDecimal(j + 1), new BigDecimal("9.08"), new BigDecimal("9.08"));
          OBDal.getInstance().save(orderLine);
          OBDal.getInstance().flush();
          orderLineIds.add(orderLine.getId());
        }

        OBDal.getInstance().flush();
        poCreateTime += TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - poCreateStart);

        long poCompleteStart = System.nanoTime();
        callStoredProc("c_order_post1", po.getId());
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(po);
        poCompleteTime += TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - poCompleteStart);

        Assert.assertEquals("CO", po.getDocumentStatus());

        long receiptCreateStart = System.nanoTime();

        ShipmentInOut receipt = createShipmentOrReceipt(receiptCtx.docType, receiptCtx.bp,
            receiptCtx.warehouse, STRESS_DOC_PREFIX + i, "V+", false);
        receipt.setSalesOrder(po);
        OBDal.getInstance().save(receipt);

        for (String olId : orderLineIds) {
          OrderLine ol = OBDal.getInstance().get(OrderLine.class, olId);
          ShipmentInOutLine receiptLine = createShipmentOrReceiptLine(receipt, ol.getProduct(),
              receiptCtx.locator, ol.getLineNo(), ol.getOrderedQuantity());
          receiptLine.setSalesOrderLine(ol);
          OBDal.getInstance().save(receiptLine);
        }

        OBDal.getInstance().flush();
        receiptCreateTime += TimeUnit.NANOSECONDS.toMillis(
            System.nanoTime() - receiptCreateStart);

        long receiptCompleteStart = System.nanoTime();
        callStoredProc("m_inout_post", receipt.getId());
        OBDal.getInstance().flush();
        receiptCompleteTime += TimeUnit.NANOSECONDS.toMillis(
            System.nanoTime() - receiptCompleteStart);

        OBDal.getInstance().refresh(receipt);
        Assert.assertEquals("CO", receipt.getDocumentStatus());

        completed++;

        if (completed % 10 == 0) {
          OBDal.getInstance().getSession().clear();

          orderCtx.reload(PO_DOCTYPE_ID, BPARTNER_ID,
              PO_WAREHOUSE_ID, PO_PRICELIST_ID, PO_PRODUCT_ID);
          receiptCtx.reload(RECEIPT_DOCTYPE_ID, BPARTNER_ID,
              PO_WAREHOUSE_ID, PO_LOCATOR_ID, PO_PRODUCT_ID);

          long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
          log.info("[STRESS-GR] Full flow " + completed + "/" + totalFlows
              + " in " + elapsed + " ms (PO create: " + poCreateTime
              + " ms, PO complete: " + poCompleteTime
              + " ms, Receipt create: " + receiptCreateTime
              + " ms, Receipt complete: " + receiptCompleteTime + " ms)");
        }
      }

      long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

      log.info("[STRESS-GR] === FULL PURCHASE FLOW TEST RESULTS ===");
      log.info("[STRESS-GR] Total flows completed: " + completed);
      log.info("[STRESS-GR] Lines per document: " + linesPerDocument);
      log.info(LOG_TOTAL_TIME + totalTime + " ms");
      log.info("[STRESS-GR] PO create time: " + poCreateTime + " ms");
      log.info("[STRESS-GR] PO complete time (c_order_post1): "
          + poCompleteTime + " ms");
      log.info("[STRESS-GR] Receipt create time: " + receiptCreateTime + " ms");
      log.info("[STRESS-GR] Receipt complete time (m_inout_post): "
          + receiptCompleteTime + " ms");
      log.info(
          "[STRESS-GR] Avg per flow: " + (totalTime / Math.max(completed, 1)) + " ms");
      log.info("[STRESS-GR] Avg PO complete: "
          + (poCompleteTime / Math.max(completed, 1)) + " ms");
      log.info("[STRESS-GR] Avg Receipt complete: "
          + (receiptCompleteTime / Math.max(completed, 1)) + " ms");
      log.info("[STRESS-GR] Throughput: "
          + (completed * 1000L / Math.max(totalTime, 1)) + " flows/sec");

      Assert.assertEquals(totalFlows, completed);

      long avgPerFlow = totalTime / Math.max(completed, 1);
      long avgReceiptComplete = receiptCompleteTime / Math.max(completed, 1);
      Assert.assertTrue(
          "Full purchase flow too slow: " + avgPerFlow + " ms/flow (max: "
              + MAX_MS_PER_FULL_FLOW + " ms)",
          avgPerFlow <= MAX_MS_PER_FULL_FLOW);
      Assert.assertTrue(
          "Receipt completion (m_inout_post) too slow: " + avgReceiptComplete
              + " ms/receipt (max: " + MAX_MS_PER_RECEIPT_COMPLETE + " ms)",
          avgReceiptComplete <= MAX_MS_PER_RECEIPT_COMPLETE);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
