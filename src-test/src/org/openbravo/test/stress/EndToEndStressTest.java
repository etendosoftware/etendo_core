package org.openbravo.test.stress;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

/**
 * End-to-end stress test: Sales Order -> Shipment -> Invoice (full sales cycle).
 */
public class EndToEndStressTest extends StressTestBase {

  private static final String STRESS_DOC_PREFIX = "STRESS-E2E-";

  private static final long MAX_MS_PER_FULL_SALES_CYCLE = 600;
  private static final BigDecimal UNIT_PRICE = new BigDecimal("25.00");

  @Override
  protected String[] getAuditTriggers() {
    return FULL_CYCLE_TRIGGERS;
  }

  @Override
  protected String[] getAuditTables() {
    return FULL_CYCLE_TABLES;
  }

  @Override
  protected String getStressDocPrefix() {
    return STRESS_DOC_PREFIX;
  }

  @After
  public void cleanUp() {
    runCleanup(() -> {
      String prefix = STRESS_DOC_PREFIX + "%";
      cleanupInvoiceData(prefix);
      cleanupShipmentData(STRESS_DOC_PREFIX + "SHIP-%");
      String orderPrefix = STRESS_DOC_PREFIX + "SO-%";
      cleanupReservationData(orderPrefix);
      cleanupOrderData(orderPrefix);
    });
  }

  // ==========================================================================
  // Test methods
  // ==========================================================================

  @Test
  public void testFullSalesCycle_10() {
    runFullSalesCycle(10, 1);
  }

  @Test
  public void testFullSalesCycle_50() {
    runFullSalesCycle(50, 1);
  }

  @Test
  public void testFullSalesCycle_100() {
    runFullSalesCycle(100, 1);
  }

  @Test
  public void testFullSalesCycleWith5Lines_10() {
    runFullSalesCycle(10, 5);
  }

  @Test
  public void testFullSalesCycleWith5Lines_25() {
    runFullSalesCycle(25, 5);
  }

  // ==========================================================================
  // Implementation
  // ==========================================================================

  private void runFullSalesCycle(int totalFlows, int linesPerDocument) {
    OBContext.setAdminMode(true);
    try {
      OrderContext soCtx = OrderContext.load(SO_DOCTYPE_ID, BPARTNER_ID, SO_WAREHOUSE_ID,
          SO_PRICELIST_ID, SO_PRODUCT_ID);
      ShipmentContext shipCtx = ShipmentContext.load(SHIPMENT_DOCTYPE_ID, BPARTNER_ID,
          SO_WAREHOUSE_ID, SO_LOCATOR_ID, SO_PRODUCT_ID);
      OrderContext invCtx = OrderContext.load(SI_DOCTYPE_ID, BPARTNER_ID, SO_WAREHOUSE_ID,
          SO_PRICELIST_ID, SO_PRODUCT_ID);

      long startTime = System.nanoTime();
      int completed = 0;
      long soCreateMs = 0;
      long soCompleteMs = 0;
      long shipCreateMs = 0;
      long shipCompleteMs = 0;
      long invCreateMs = 0;
      long invCompleteMs = 0;

      for (int i = 0; i < totalFlows; i++) {

        // ---- STEP 1: Create and complete Sales Order ----
        long t0 = System.nanoTime();

        Order order = createOrder(soCtx.docType, soCtx.bp, soCtx.warehouse, soCtx.priceList,
            soCtx.paymentTerm, soCtx.paymentMethod, soCtx.currency,
            STRESS_DOC_PREFIX + "SO-" + i, true);
        OBDal.getInstance().save(order);

        List<String> orderLineIds = new ArrayList<>();
        for (int j = 0; j < linesPerDocument; j++) {
          OrderLine ol = createOrderLine(order, soCtx.product, soCtx.tax,
              (long) (j + 1) * 10, new BigDecimal(j + 1), UNIT_PRICE, UNIT_PRICE);
          OBDal.getInstance().save(ol);
          OBDal.getInstance().flush();
          orderLineIds.add(ol.getId());
        }
        OBDal.getInstance().flush();
        soCreateMs += TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);

        long t1 = System.nanoTime();
        callStoredProc("c_order_post1", order.getId());
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(order);
        Assert.assertEquals("SO not completed", "CO", order.getDocumentStatus());
        soCompleteMs += TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t1);

        // ---- STEP 2: Create and complete Shipment ----
        long t2 = System.nanoTime();

        ShipmentInOut shipment = createShipmentOrReceipt(shipCtx.docType, shipCtx.bp,
            shipCtx.warehouse, STRESS_DOC_PREFIX + "SHIP-" + i, "C-", true);
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
        shipCreateMs += TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t2);

        long t3 = System.nanoTime();
        callStoredProc("m_inout_post", shipment.getId());
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(shipment);
        Assert.assertEquals("Shipment not completed", "CO", shipment.getDocumentStatus());
        shipCompleteMs += TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t3);

        // ---- STEP 3: Create and complete Invoice ----
        long t4 = System.nanoTime();

        Invoice invoice = createInvoice(invCtx.docType, invCtx.bp, invCtx.priceList,
            invCtx.paymentTerm, invCtx.paymentMethod, invCtx.currency,
            STRESS_DOC_PREFIX + "INV-" + i, true);
        OBDal.getInstance().save(invoice);

        for (String olId : orderLineIds) {
          OrderLine ol = OBDal.getInstance().get(OrderLine.class, olId);
          InvoiceLine invLine = createInvoiceLine(invoice, ol.getProduct(), invCtx.tax,
              ol.getLineNo(), ol.getOrderedQuantity(),
              UNIT_PRICE, UNIT_PRICE);
          OBDal.getInstance().save(invLine);
        }
        OBDal.getInstance().flush();
        invCreateMs += TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t4);

        long t5 = System.nanoTime();
        callStoredProc("c_invoice_post", invoice.getId());
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(invoice);
        Assert.assertEquals("Invoice not completed", "CO", invoice.getDocumentStatus());
        invCompleteMs += TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t5);

        completed++;

        if (completed % 10 == 0) {
          OBDal.getInstance().getSession().clear();

          soCtx.reload(SO_DOCTYPE_ID, BPARTNER_ID, SO_WAREHOUSE_ID, SO_PRICELIST_ID,
              SO_PRODUCT_ID);
          shipCtx.reload(SHIPMENT_DOCTYPE_ID, BPARTNER_ID, SO_WAREHOUSE_ID, SO_LOCATOR_ID,
              SO_PRODUCT_ID);
          invCtx.reload(SI_DOCTYPE_ID, BPARTNER_ID, SO_WAREHOUSE_ID, SO_PRICELIST_ID,
              SO_PRODUCT_ID);

          long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
          log.info("[STRESS-E2E] Cycle {}/{} in {} ms", completed, totalFlows, elapsed);
        }
      }

      long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

      log.info("[STRESS-E2E] === FULL SALES CYCLE RESULTS ===");
      log.info("[STRESS-E2E] Flows: {}, Lines/doc: {}", completed, linesPerDocument);
      log.info("[STRESS-E2E] SO create: {} ms, SO complete: {} ms", soCreateMs, soCompleteMs);
      log.info("[STRESS-E2E] Shipment create: {} ms, Shipment complete: {} ms",
          shipCreateMs, shipCompleteMs);
      log.info("[STRESS-E2E] Invoice create: {} ms, Invoice complete: {} ms",
          invCreateMs, invCompleteMs);
      log.info("[STRESS-E2E] Total time: {} ms", totalTime);
      log.info("[STRESS-E2E] Avg per cycle: {} ms",
          totalTime / Math.max(completed, 1));
      log.info("[STRESS-E2E] Throughput: {} cycles/sec",
          completed * 1000L / Math.max(totalTime, 1));

      Assert.assertEquals(totalFlows, completed);

      long avgPerCycle = totalTime / Math.max(completed, 1);
      Assert.assertTrue(
          "Full sales cycle too slow: " + avgPerCycle + " ms/cycle (max: "
              + MAX_MS_PER_FULL_SALES_CYCLE + " ms)",
          avgPerCycle <= MAX_MS_PER_FULL_SALES_CYCLE);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
