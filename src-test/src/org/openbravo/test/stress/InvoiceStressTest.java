package org.openbravo.test.stress;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;

/** Stress tests for Invoice creation and completion. */
public class InvoiceStressTest extends StressTestBase {

  private static final String STRESS_DOC_PREFIX = "STRESS-INV-";
  private static final String LOG_TOTAL_TIME = "[STRESS-INV] Total time: ";
  private static final String SO_UNIT_PRICE_STR = "25.00";
  private static final String PO_UNIT_PRICE_STR = "9.08";
  private static final BigDecimal UNIT_PRICE = new BigDecimal(SO_UNIT_PRICE_STR);

  private static final long MAX_MS_PER_INVOICE_CREATION = 15;
  private static final long MAX_MS_PER_INVOICE_COMPLETE = 150;
  private static final long MAX_MS_PER_LINE_CREATION = 15;
  private static final long MAX_MEMORY_MB_PER_100_INVOICES = 30;

  @Override
  protected String[] getAuditTriggers() {
    return INVOICE_TRIGGERS;
  }

  @Override
  protected String[] getAuditTables() {
    return INVOICE_TABLES;
  }

  @Override
  protected String getStressDocPrefix() {
    return STRESS_DOC_PREFIX;
  }

  @After
  public void cleanUp() {
    runCleanup(() -> cleanupInvoiceData(STRESS_DOC_PREFIX + "%"));
  }

  // ==========================================================================
  // Bulk creation tests
  // ==========================================================================

  @Test
  public void testBulkSalesInvoiceCreation_100() {
    runBulkInvoiceCreation(100, 1, true);
  }

  @Test
  public void testBulkSalesInvoiceCreation_500() {
    runBulkInvoiceCreation(500, 1, true);
  }

  @Test
  public void testBulkSalesInvoiceCreation_1000() {
    runBulkInvoiceCreation(1000, 1, true);
  }

  @Test
  public void testInvoiceWithManyLines_50() {
    runBulkInvoiceCreation(1, 50, true);
  }

  @Test
  public void testInvoiceWithManyLines_200() {
    runBulkInvoiceCreation(1, 200, true);
  }

  @Test
  public void testCombined_100InvoicesWith5Lines() {
    runBulkInvoiceCreation(100, 5, true);
  }

  // ==========================================================================
  // Create & Complete tests
  // ==========================================================================

  @Test
  public void testBulkSalesInvoiceComplete_10() {
    runBulkInvoiceCreateAndComplete(10, 1, true);
  }

  @Test
  public void testBulkSalesInvoiceComplete_50() {
    runBulkInvoiceCreateAndComplete(50, 1, true);
  }

  @Test
  public void testBulkSalesInvoiceComplete_100() {
    runBulkInvoiceCreateAndComplete(100, 1, true);
  }

  @Test
  public void testBulkPurchaseInvoiceComplete_10() {
    runBulkInvoiceCreateAndComplete(10, 1, false);
  }

  @Test
  public void testBulkPurchaseInvoiceComplete_50() {
    runBulkInvoiceCreateAndComplete(50, 1, false);
  }

  // ==========================================================================
  // Query performance test
  // ==========================================================================

  @Test
  public void testBulkInvoiceQuery_Performance() {
    int totalInvoices = 200;
    int linesPerInvoice = 3;
    runBulkInvoiceCreation(totalInvoices, linesPerInvoice, true);

    OBDal.getInstance().getSession().clear();

    long startQuery = System.nanoTime();
    OBQuery<Invoice> query = OBDal.getInstance()
        .createQuery(Invoice.class, "documentNo like :prefix");
    query.setNamedParameter(PARAM_PREFIX, STRESS_DOC_PREFIX + "%");
    List<Invoice> invoices = query.list();
    long queryTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startQuery);

    log.info(
        "[STRESS-INV] Query " + invoices.size() + " invoices took: " + queryTime + " ms");
    Assert.assertEquals(totalInvoices, invoices.size());

    long startLines = System.nanoTime();
    int totalLines = 0;
    for (Invoice inv : invoices) {
      totalLines += inv.getInvoiceLineList().size();
    }
    long linesTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startLines);

    log.info(
        "[STRESS-INV] Loading " + totalLines + " invoice lines (lazy) took: " + linesTime + " ms");
    Assert.assertEquals(totalInvoices * linesPerInvoice, totalLines);
  }

  // ==========================================================================
  // Memory test
  // ==========================================================================

  @Test
  public void testMemoryUsageDuringBulkInvoiceCreation() {
    int totalInvoices = 500;
    int flushInterval = 100;

    OBContext.setAdminMode(true);
    try {
      OrderContext ctx = OrderContext.load(SI_DOCTYPE_ID, BPARTNER_ID, SO_WAREHOUSE_ID,
          SO_PRICELIST_ID, PO_PRODUCT_ID);

      Runtime runtime = Runtime.getRuntime();
      forceGarbageCollection();
      long memBefore = runtime.totalMemory() - runtime.freeMemory();
      long startTime = System.nanoTime();

      for (int i = 0; i < totalInvoices; i++) {
        Invoice invoice = createInvoice(ctx.docType, ctx.bp, ctx.priceList, ctx.paymentTerm,
            ctx.paymentMethod, ctx.currency, STRESS_DOC_PREFIX + "MEM-" + i, true);
        OBDal.getInstance().save(invoice);

        for (int j = 0; j < 3; j++) {
          InvoiceLine line = createInvoiceLine(invoice, ctx.product, ctx.tax,
              (long) (j + 1) * 10, new BigDecimal(j + 1), UNIT_PRICE, UNIT_PRICE);
          OBDal.getInstance().save(line);
        }

        if ((i + 1) % flushInterval == 0) {
          flushAndClear();

          long memCurrent = runtime.totalMemory() - runtime.freeMemory();
          long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
          log.info("[STRESS-INV] After " + (i + 1) + " invoices - Memory: "
              + ((memCurrent - memBefore) / (1024 * 1024)) + " MB, Time: " + elapsed + " ms");

          ctx.reload(SI_DOCTYPE_ID, BPARTNER_ID, SO_WAREHOUSE_ID, SO_PRICELIST_ID,
              PO_PRODUCT_ID);
        }
      }

      OBDal.getInstance().flush();
      long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
      long memAfter = runtime.totalMemory() - runtime.freeMemory();

      log.info("[STRESS-INV] === MEMORY USAGE TEST RESULTS ===");
      log.info("[STRESS-INV] Total invoices: " + totalInvoices + " (3 lines each)");
      log.info(LOG_TOTAL_TIME + totalTime + " ms");
      log.info(
          "[STRESS-INV] Memory delta: " + ((memAfter - memBefore) / (1024 * 1024)) + " MB");

      long memDeltaMB = (memAfter - memBefore) / (1024 * 1024);
      long maxExpectedMB = (totalInvoices / 100) * MAX_MEMORY_MB_PER_100_INVOICES;
      Assert.assertTrue(
          "Memory usage too high: " + memDeltaMB + " MB (max: " + maxExpectedMB + " MB)",
          memDeltaMB <= maxExpectedMB);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // ==========================================================================
  // Implementation methods
  // ==========================================================================

  private void runBulkInvoiceCreation(int totalInvoices, int linesPerInvoice,
      boolean isSalesInvoice) {
    OBContext.setAdminMode(true);
    try {
      String docTypeId = isSalesInvoice ? SI_DOCTYPE_ID : PI_DOCTYPE_ID;
      String priceListId = isSalesInvoice ? SO_PRICELIST_ID : PO_PRICELIST_ID;
      String unitPrice = isSalesInvoice ? SO_UNIT_PRICE_STR : PO_UNIT_PRICE_STR;
      String warehouseId = isSalesInvoice ? SO_WAREHOUSE_ID : PO_WAREHOUSE_ID;

      OrderContext ctx = OrderContext.load(docTypeId, BPARTNER_ID, warehouseId, priceListId,
          PO_PRODUCT_ID);

      long startTime = System.nanoTime();
      int created = 0;

      for (int i = 0; i < totalInvoices; i++) {
        Invoice invoice = createInvoice(ctx.docType, ctx.bp, ctx.priceList, ctx.paymentTerm,
            ctx.paymentMethod, ctx.currency, STRESS_DOC_PREFIX + i, isSalesInvoice);
        OBDal.getInstance().save(invoice);

        for (int j = 0; j < linesPerInvoice; j++) {
          InvoiceLine line = createInvoiceLine(invoice, ctx.product, ctx.tax,
              (long) (j + 1) * 10, new BigDecimal(j + 1), new BigDecimal(unitPrice),
              new BigDecimal(unitPrice));
          OBDal.getInstance().save(line);
        }

        created++;

        if (created % 100 == 0) {
          flushAndClear();

          ctx.reload(docTypeId, BPARTNER_ID, warehouseId, priceListId, PO_PRODUCT_ID);

          long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
          log.info("[STRESS-INV] Created " + created + "/" + totalInvoices
              + " invoices in " + elapsed + " ms");
        }
      }

      OBDal.getInstance().flush();
      long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

      log.info("[STRESS-INV] === BULK CREATION TEST RESULTS ===");
      log.info("[STRESS-INV] Total invoices: " + created);
      log.info("[STRESS-INV] Lines per invoice: " + linesPerInvoice);
      log.info("[STRESS-INV] Total lines: " + (created * linesPerInvoice));
      log.info(LOG_TOTAL_TIME + totalTime + " ms");
      log.info(
          "[STRESS-INV] Avg per invoice: " + (totalTime / Math.max(created, 1)) + " ms");
      log.info("[STRESS-INV] Throughput: "
          + (created * 1000L / Math.max(totalTime, 1)) + " invoices/sec");

      Assert.assertEquals(totalInvoices, created);

      long avgPerLine = totalTime / Math.max(created * linesPerInvoice, 1);
      Assert.assertTrue(
          "Line creation too slow: " + avgPerLine + " ms/line (max: "
              + MAX_MS_PER_LINE_CREATION + " ms)",
          avgPerLine <= MAX_MS_PER_LINE_CREATION);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void runBulkInvoiceCreateAndComplete(int totalInvoices, int linesPerInvoice,
      boolean isSalesInvoice) {
    OBContext.setAdminMode(true);
    try {
      String docTypeId = isSalesInvoice ? SI_DOCTYPE_ID : PI_DOCTYPE_ID;
      String priceListId = isSalesInvoice ? SO_PRICELIST_ID : PO_PRICELIST_ID;
      String unitPrice = isSalesInvoice ? SO_UNIT_PRICE_STR : PO_UNIT_PRICE_STR;
      String warehouseId = isSalesInvoice ? SO_WAREHOUSE_ID : PO_WAREHOUSE_ID;

      OrderContext ctx = OrderContext.load(docTypeId, BPARTNER_ID, warehouseId, priceListId,
          PO_PRODUCT_ID);

      long startTime = System.nanoTime();
      int completed = 0;
      long createTime = 0;
      long completeTime = 0;

      for (int i = 0; i < totalInvoices; i++) {
        long createStart = System.nanoTime();

        Invoice invoice = createInvoice(ctx.docType, ctx.bp, ctx.priceList, ctx.paymentTerm,
            ctx.paymentMethod, ctx.currency, STRESS_DOC_PREFIX + "CO-" + i, isSalesInvoice);
        OBDal.getInstance().save(invoice);

        for (int j = 0; j < linesPerInvoice; j++) {
          InvoiceLine line = createInvoiceLine(invoice, ctx.product, ctx.tax,
              (long) (j + 1) * 10, new BigDecimal(j + 1), new BigDecimal(unitPrice),
              new BigDecimal(unitPrice));
          OBDal.getInstance().save(line);
        }

        OBDal.getInstance().flush();
        createTime += TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - createStart);

        long completeStart = System.nanoTime();
        callStoredProc("c_invoice_post", invoice.getId());
        OBDal.getInstance().flush();
        completeTime += TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - completeStart);

        OBDal.getInstance().refresh(invoice);
        Assert.assertEquals("CO", invoice.getDocumentStatus());

        completed++;

        if (completed % 10 == 0) {
          OBDal.getInstance().getSession().clear();

          ctx.reload(docTypeId, BPARTNER_ID, warehouseId, priceListId, PO_PRODUCT_ID);

          long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
          log.info("[STRESS-INV] Completed " + completed + "/" + totalInvoices
              + " invoices in " + elapsed + " ms (create: " + createTime
              + " ms, complete: " + completeTime + " ms)");
        }
      }

      long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

      String type = isSalesInvoice ? "sales" : "purchase";
      log.info("[STRESS-INV] === " + type.toUpperCase()
          + " INVOICE CREATE & COMPLETE RESULTS ===");
      log.info("[STRESS-INV] Total " + type + " invoices completed: " + completed);
      log.info("[STRESS-INV] Lines per invoice: " + linesPerInvoice);
      log.info(LOG_TOTAL_TIME + totalTime + " ms");
      log.info("[STRESS-INV] Create time: " + createTime + " ms");
      log.info("[STRESS-INV] Complete time (c_invoice_post): " + completeTime + " ms");
      log.info("[STRESS-INV] Avg per invoice: "
          + (totalTime / Math.max(completed, 1)) + " ms");
      log.info("[STRESS-INV] Avg complete time: "
          + (completeTime / Math.max(completed, 1)) + " ms");
      log.info("[STRESS-INV] Throughput: "
          + (completed * 1000L / Math.max(totalTime, 1)) + " invoices/sec");

      Assert.assertEquals(totalInvoices, completed);

      long avgPerInvoice = totalTime / Math.max(completed, 1);
      long avgComplete = completeTime / Math.max(completed, 1);
      Assert.assertTrue(
          "Invoice create+complete too slow: " + avgPerInvoice + " ms/invoice (max: "
              + (MAX_MS_PER_INVOICE_CREATION + MAX_MS_PER_INVOICE_COMPLETE) + " ms)",
          avgPerInvoice <= MAX_MS_PER_INVOICE_CREATION + MAX_MS_PER_INVOICE_COMPLETE);
      Assert.assertTrue(
          "Invoice completion (c_invoice_post) too slow: " + avgComplete
              + " ms/invoice (max: " + MAX_MS_PER_INVOICE_COMPLETE + " ms)",
          avgComplete <= MAX_MS_PER_INVOICE_COMPLETE);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
