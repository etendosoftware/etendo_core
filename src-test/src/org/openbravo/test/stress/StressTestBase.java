package org.openbravo.test.stress;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.test.base.OBBaseTest;

/**
 * Base class for stress tests. Extracts common constants, setup, audit trigger
 * management, and entity creation methods shared across all stress test files.
 */
public class StressTestBase extends OBBaseTest {

  protected static final Logger log = LogManager.getLogger();

  // ---- Context constants (shared by ALL stress tests) ----
  protected static final String CLIENT_USER_ID = "100";
  protected static final String CLIENT_ROLE_ID = "B50792EDEDEE41E789C84F8B82CE9684";
  protected static final String CLIENT_CLIENT_ID = "94C21765333E4281B1135F224263E101";
  protected static final String CLIENT_ORG_ID = "585F008579C947FFB1BAFFC540C8FEFE";

  // ---- Shared entity IDs ----
  protected static final String BPARTNER_ID = "AA047183BB9442C1AD6B309AD3752CFD";
  protected static final String PAYMENT_METHOD_ID = "C901CE06189A41A7B93DA69D7F186686";
  protected static final String PAYMENT_TERM_ID = "F804D910229943FBA8E38CC1950AD6EA";
  protected static final String TAX_ID = "EAF6BC1594C747FBB5D386B37531D289";
  protected static final String CURRENCY_ID = "102";

  // ---- Purchase flow IDs ----
  protected static final String PO_WAREHOUSE_ID = "64154F86F3CA4A3B9F147F4AF13DED39";
  protected static final String PO_LOCATOR_ID = "343D1C118527495C8B522DE982B45BD4";
  protected static final String PO_PRODUCT_ID = "DC66BC296C824B428DA276510A3DA92D";

  // ---- Sales flow IDs ----
  protected static final String SO_WAREHOUSE_ID = "9D0A87A11ADB43C583E5EAB5974D4B1E";
  protected static final String SO_LOCATOR_ID = "E338C679163B4EF0AA711F2CFA6D49CD";
  protected static final String SO_PRODUCT_ID = "09C44930F3444262A614060220003B95";

  // ---- Price list IDs (used across multiple tests) ----
  protected static final String SO_PRICELIST_ID = "B38041493C7B466A9D5B5B9235B9F229";
  protected static final String PO_PRICELIST_ID = "91361AE819E0424CB8D601BBA4CB68F6";

  // ---- Document type IDs (used across multiple tests) ----
  protected static final String SO_DOCTYPE_ID = "CB6EEA256BBC41109911215C5A14D39B";
  protected static final String PO_DOCTYPE_ID = "808F8818F724497D94282AC83493F394";
  protected static final String SHIPMENT_DOCTYPE_ID = "0CD50184705A42CCBECA4EC967646440";
  protected static final String RECEIPT_DOCTYPE_ID = "B7BFC8D519D24545AFDC1CC9661A6EC3";
  protected static final String SI_DOCTYPE_ID = "F31073DE1E0A482F8C9D1D30D7362EE9";
  protected static final String PI_DOCTYPE_ID = "5C6E02993E9B4FCA81C07955EF676C62";

  // ---- Query helper constants ----
  protected static final String PARAM_PREFIX = "prefix";
  protected static final String SUBQUERY_ORDER_BY_PREFIX =
      "(SELECT c_order_id FROM c_order WHERE documentno LIKE :prefix)";
  protected static final String SUBQUERY_INVOICE_BY_PREFIX =
      "(SELECT c_invoice_id FROM c_invoice WHERE documentno LIKE :prefix)";

  // ---- Individual trigger/table name constants ----
  private static final String TRG_ORDER = "au_c_order_trg";
  private static final String TRG_ORDERLINE = "au_c_orderline_trg";
  private static final String TRG_PAYMENT_SCHEDULE = "au_fin_payment_schedule_trg";
  private static final String TRG_INOUT = "au_m_inout_trg";
  private static final String TRG_INOUTLINE = "au_m_inoutline_trg";
  private static final String TRG_INVOICE = "au_c_invoice_trg";
  private static final String TRG_INVOICELINE = "au_c_invoiceline_trg";

  private static final String TBL_ORDER = "c_order";
  private static final String TBL_ORDERLINE = "c_orderline";
  private static final String TBL_PAYMENT_SCHEDULE = "fin_payment_schedule";
  private static final String TBL_INOUT = "m_inout";
  private static final String TBL_INOUTLINE = "m_inoutline";
  private static final String TBL_INVOICE = "c_invoice";
  private static final String TBL_INVOICELINE = "c_invoiceline";

  // ---- Common audit trigger/table sets ----
  protected static final String[] ORDER_TRIGGERS = {
      TRG_ORDER, TRG_ORDERLINE, TRG_PAYMENT_SCHEDULE
  };
  protected static final String[] ORDER_TABLES = {
      TBL_ORDER, TBL_ORDERLINE, TBL_PAYMENT_SCHEDULE
  };
  protected static final String[] ORDER_AND_SHIPMENT_TRIGGERS = {
      TRG_ORDER, TRG_ORDERLINE, TRG_PAYMENT_SCHEDULE, TRG_INOUT, TRG_INOUTLINE
  };
  protected static final String[] ORDER_AND_SHIPMENT_TABLES = {
      TBL_ORDER, TBL_ORDERLINE, TBL_PAYMENT_SCHEDULE, TBL_INOUT, TBL_INOUTLINE
  };
  protected static final String[] INVOICE_TRIGGERS = {
      TRG_INVOICE, TRG_INVOICELINE, TRG_PAYMENT_SCHEDULE
  };
  protected static final String[] INVOICE_TABLES = {
      TBL_INVOICE, TBL_INVOICELINE, TBL_PAYMENT_SCHEDULE
  };
  protected static final String[] FULL_CYCLE_TRIGGERS = {
      TRG_ORDER, TRG_ORDERLINE, TRG_INOUT, TRG_INOUTLINE,
      TRG_INVOICE, TRG_INVOICELINE, TRG_PAYMENT_SCHEDULE
  };
  protected static final String[] FULL_CYCLE_TABLES = {
      TBL_ORDER, TBL_ORDERLINE, TBL_INOUT, TBL_INOUTLINE,
      TBL_INVOICE, TBL_INVOICELINE, TBL_PAYMENT_SCHEDULE
  };

  // ==========================================================================
  // Subclass customization (with default empty impls)
  // ==========================================================================

  /**
   * Returns the audit trigger names to disable/enable during tests.
   * Override in subclasses that need audit trigger management.
   */
  protected String[] getAuditTriggers() {
    return ORDER_AND_SHIPMENT_TRIGGERS;
  }

  /**
   * Returns the table names corresponding to the audit triggers.
   * Must have the same length as {@link #getAuditTriggers()}.
   */
  protected String[] getAuditTables() {
    return ORDER_AND_SHIPMENT_TABLES;
  }

  /**
   * Returns the document number prefix used by this test class for cleanup.
   * Override in subclasses.
   */
  protected String getStressDocPrefix() {
    return "STRESS-";
  }

  // ==========================================================================
  // Common setUp
  // ==========================================================================

  @Override
  @Before
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    OBContext.setOBContext(CLIENT_USER_ID, CLIENT_ROLE_ID, CLIENT_CLIENT_ID, CLIENT_ORG_ID);
    disableAuditTriggers();
  }

  // ==========================================================================
  // Audit trigger management
  // ==========================================================================

  protected void disableAuditTriggers() {
    String[] triggers = getAuditTriggers();
    String[] tables = getAuditTables();
    for (int i = 0; i < triggers.length; i++) {
      OBDal.getInstance().getSession()
          .createNativeQuery(
              "ALTER TABLE " + tables[i] + " DISABLE TRIGGER " + triggers[i])
          .executeUpdate();
    }
    OBDal.getInstance().flush();
  }

  protected void enableAuditTriggers() {
    String[] triggers = getAuditTriggers();
    String[] tables = getAuditTables();
    try {
      for (int i = 0; i < triggers.length; i++) {
        OBDal.getInstance().getSession()
            .createNativeQuery(
                "ALTER TABLE " + tables[i] + " ENABLE TRIGGER " + triggers[i])
            .executeUpdate();
      }
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.error("[STRESS] Warning: Could not re-enable audit triggers: " + e.getMessage());
    }
  }

  // ==========================================================================
  // Utility methods
  // ==========================================================================

  /**
   * Forces a GC to establish a memory baseline for memory usage tests.
   * Intentional call — suppressing S1215.
   */
  @SuppressWarnings("java:S1215")
  protected void forceGarbageCollection() {
    Runtime.getRuntime().gc();
  }

  protected Date getOpenPeriodDate() {
    return new GregorianCalendar(2025, Calendar.DECEMBER, 15).getTime();
  }

  /**
   * Completes an order via stored procedure and verifies it reached "CO" status.
   *
   * @return the time in milliseconds spent completing the order
   */
  protected long completeOrderAndVerify(Order order) {
    long completeStart = System.nanoTime();
    callStoredProc("c_order_post1", order.getId());
    OBDal.getInstance().flush();
    long completeTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - completeStart);

    OBDal.getInstance().refresh(order);
    org.junit.Assert.assertEquals("CO", order.getDocumentStatus());
    return completeTime;
  }

  protected void callStoredProc(String procName, String documentId) {
    final List<Object> parameters = new ArrayList<>();
    parameters.add(null);
    parameters.add(documentId);
    CallStoredProcedure.getInstance().call(procName, parameters, null, true, false);
  }

  /**
   * Executes a native SQL cleanup query with a prefix parameter.
   *
   * @param sql    the SQL statement containing a {@code :prefix} parameter
   * @param prefix the value to bind to the {@code :prefix} parameter
   */
  protected void executeCleanupQuery(String sql, String prefix) {
    OBDal.getInstance().getSession()
        .createNativeQuery(sql)
        .setParameter(PARAM_PREFIX, prefix)
        .executeUpdate();
  }

  // ==========================================================================
  // Common cleanup helpers
  // ==========================================================================

  /**
   * Wraps cleanup logic with proper error handling: admin mode, rollback on error,
   * re-enable audit triggers in finally block.
   */
  protected void runCleanup(Runnable cleanupLogic) {
    OBContext.setAdminMode(true);
    try {
      OBDal.getInstance().getSession().clear();
      cleanupLogic.run();
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      try {
        OBDal.getInstance().rollbackAndClose();
      } catch (Exception ex) {
        log.error("[STRESS] Cleanup rollback error: " + ex.getMessage());
      }
    } finally {
      enableAuditTriggers();
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Deletes order-related data (payment schedule details, payment schedules,
   * ordertax, orderlines, orders) matching a documentno prefix.
   */
  protected void cleanupOrderData(String prefix) {
    executeCleanupQuery(
        "DELETE FROM fin_payment_scheduledetail WHERE fin_payment_schedule_order IN "
            + "(SELECT fps.fin_payment_schedule_id FROM fin_payment_schedule fps "
            + "JOIN c_order o ON fps.c_order_id = o.c_order_id "
            + "WHERE o.documentno LIKE :prefix)", prefix);
    executeCleanupQuery(
        "DELETE FROM fin_payment_schedule WHERE c_order_id IN "
            + SUBQUERY_ORDER_BY_PREFIX, prefix);
    executeCleanupQuery(
        "DELETE FROM c_ordertax WHERE c_order_id IN "
            + SUBQUERY_ORDER_BY_PREFIX, prefix);
    executeCleanupQuery(
        "DELETE FROM c_orderline WHERE c_order_id IN "
            + SUBQUERY_ORDER_BY_PREFIX, prefix);
    executeCleanupQuery(
        "DELETE FROM c_order WHERE documentno LIKE :prefix", prefix);
  }

  /**
   * Deletes shipment/receipt data (matchinv, transactions, lines, headers)
   * matching a documentno prefix.
   */
  protected void cleanupShipmentData(String prefix) {
    executeCleanupQuery(
        "DELETE FROM m_matchinv WHERE m_inoutline_id IN "
            + "(SELECT m_inoutline_id FROM m_inoutline WHERE m_inout_id IN "
            + "(SELECT m_inout_id FROM m_inout WHERE documentno LIKE :prefix))", prefix);
    executeCleanupQuery(
        "DELETE FROM m_transaction WHERE m_inoutline_id IN "
            + "(SELECT m_inoutline_id FROM m_inoutline WHERE m_inout_id IN "
            + "(SELECT m_inout_id FROM m_inout WHERE documentno LIKE :prefix))", prefix);
    executeCleanupQuery(
        "DELETE FROM m_inoutline WHERE m_inout_id IN "
            + "(SELECT m_inout_id FROM m_inout WHERE documentno LIKE :prefix)", prefix);
    executeCleanupQuery(
        "DELETE FROM m_inout WHERE documentno LIKE :prefix", prefix);
  }

  /**
   * Deletes order-related shipment/receipt data where shipments are linked
   * to orders matching a documentno prefix.
   */
  protected void cleanupOrderLinkedShipments(String orderPrefix) {
    executeCleanupQuery(
        "DELETE FROM m_matchinv WHERE m_inoutline_id IN "
            + "(SELECT mil.m_inoutline_id FROM m_inoutline mil "
            + "JOIN c_orderline ol ON mil.c_orderline_id = ol.c_orderline_id "
            + "JOIN c_order o ON ol.c_order_id = o.c_order_id "
            + "WHERE o.documentno LIKE :prefix)", orderPrefix);
    executeCleanupQuery(
        "DELETE FROM m_transaction WHERE m_inoutline_id IN "
            + "(SELECT mil.m_inoutline_id FROM m_inoutline mil "
            + "JOIN c_orderline ol ON mil.c_orderline_id = ol.c_orderline_id "
            + "JOIN c_order o ON ol.c_order_id = o.c_order_id "
            + "WHERE o.documentno LIKE :prefix)", orderPrefix);
    executeCleanupQuery(
        "DELETE FROM m_inoutline WHERE c_orderline_id IN "
            + "(SELECT ol.c_orderline_id FROM c_orderline ol "
            + "JOIN c_order o ON ol.c_order_id = o.c_order_id "
            + "WHERE o.documentno LIKE :prefix)", orderPrefix);
    executeCleanupQuery(
        "DELETE FROM m_inout WHERE c_order_id IN "
            + SUBQUERY_ORDER_BY_PREFIX, orderPrefix);
  }

  /**
   * Deletes order reservation data matching a documentno prefix.
   */
  protected void cleanupReservationData(String orderPrefix) {
    executeCleanupQuery(
        "DELETE FROM m_reservation_stock WHERE m_reservation_id IN "
            + "(SELECT r.m_reservation_id FROM m_reservation r "
            + "JOIN c_orderline ol ON r.c_orderline_id = ol.c_orderline_id "
            + "JOIN c_order o ON ol.c_order_id = o.c_order_id "
            + "WHERE o.documentno LIKE :prefix)", orderPrefix);
    executeCleanupQuery(
        "DELETE FROM m_reservation WHERE c_orderline_id IN "
            + "(SELECT ol.c_orderline_id FROM c_orderline ol "
            + "JOIN c_order o ON ol.c_order_id = o.c_order_id "
            + "WHERE o.documentno LIKE :prefix)", orderPrefix);
  }

  /**
   * Deletes invoice-related data (payment schedule details, payment schedules,
   * invoicelinetax, invoicetax, invoicelines, invoices) matching a documentno prefix.
   */
  protected void cleanupInvoiceData(String prefix) {
    executeCleanupQuery(
        "DELETE FROM fin_payment_scheduledetail WHERE fin_payment_schedule_invoice IN "
            + "(SELECT fps.fin_payment_schedule_id FROM fin_payment_schedule fps "
            + "JOIN c_invoice i ON fps.c_invoice_id = i.c_invoice_id "
            + "WHERE i.documentno LIKE :prefix)", prefix);
    executeCleanupQuery(
        "DELETE FROM fin_payment_schedule WHERE c_invoice_id IN "
            + SUBQUERY_INVOICE_BY_PREFIX, prefix);
    executeCleanupQuery(
        "DELETE FROM c_invoicelinetax WHERE c_invoiceline_id IN "
            + "(SELECT c_invoiceline_id FROM c_invoiceline WHERE c_invoice_id IN "
            + SUBQUERY_INVOICE_BY_PREFIX + ")", prefix);
    executeCleanupQuery(
        "DELETE FROM c_invoicetax WHERE c_invoice_id IN "
            + SUBQUERY_INVOICE_BY_PREFIX, prefix);
    executeCleanupQuery(
        "DELETE FROM c_invoiceline WHERE c_invoice_id IN "
            + SUBQUERY_INVOICE_BY_PREFIX, prefix);
    executeCleanupQuery(
        "DELETE FROM c_invoice WHERE documentno LIKE :prefix", prefix);
  }

  // ==========================================================================
  // Entity context — encapsulates DAL entity loading to avoid duplication
  // ==========================================================================

  /**
   * Holds DAL entities needed for order creation. Provides load/reload
   * to avoid duplicating entity-fetch blocks across tests.
   */
  protected static class OrderContext {
    DocumentType docType;
    BusinessPartner bp;
    Warehouse warehouse;
    PriceList priceList;
    PaymentTerm paymentTerm;
    FIN_PaymentMethod paymentMethod;
    Currency currency;
    Product product;
    TaxRate tax;

    /** Loads all order-related entities from the DAL. */
    static OrderContext load(String docTypeId, String bpId, String warehouseId,
        String priceListId, String productId) {
      OrderContext ctx = new OrderContext();
      ctx.docType = OBDal.getInstance().get(DocumentType.class, docTypeId);
      ctx.bp = OBDal.getInstance().get(BusinessPartner.class, bpId);
      ctx.warehouse = OBDal.getInstance().get(Warehouse.class, warehouseId);
      ctx.priceList = OBDal.getInstance().get(PriceList.class, priceListId);
      ctx.paymentTerm = OBDal.getInstance().get(PaymentTerm.class, PAYMENT_TERM_ID);
      ctx.paymentMethod = OBDal.getInstance().get(FIN_PaymentMethod.class, PAYMENT_METHOD_ID);
      ctx.currency = OBDal.getInstance().get(Currency.class, CURRENCY_ID);
      ctx.product = OBDal.getInstance().get(Product.class, productId);
      ctx.tax = OBDal.getInstance().get(TaxRate.class, TAX_ID);
      return ctx;
    }

    /** Reloads all entities after a session clear. */
    void reload(String docTypeId, String bpId, String warehouseId,
        String priceListId, String productId) {
      docType = OBDal.getInstance().get(DocumentType.class, docTypeId);
      bp = OBDal.getInstance().get(BusinessPartner.class, bpId);
      warehouse = OBDal.getInstance().get(Warehouse.class, warehouseId);
      priceList = OBDal.getInstance().get(PriceList.class, priceListId);
      paymentTerm = OBDal.getInstance().get(PaymentTerm.class, PAYMENT_TERM_ID);
      paymentMethod = OBDal.getInstance().get(FIN_PaymentMethod.class, PAYMENT_METHOD_ID);
      currency = OBDal.getInstance().get(Currency.class, CURRENCY_ID);
      product = OBDal.getInstance().get(Product.class, productId);
      tax = OBDal.getInstance().get(TaxRate.class, TAX_ID);
    }
  }

  /**
   * Holds DAL entities needed for shipment/receipt creation.
   */
  protected static class ShipmentContext {
    DocumentType docType;
    BusinessPartner bp;
    Warehouse warehouse;
    Locator locator;
    Product product;

    /** Loads all shipment/receipt-related entities from the DAL. */
    static ShipmentContext load(String docTypeId, String bpId, String warehouseId,
        String locatorId, String productId) {
      ShipmentContext ctx = new ShipmentContext();
      ctx.docType = OBDal.getInstance().get(DocumentType.class, docTypeId);
      ctx.bp = OBDal.getInstance().get(BusinessPartner.class, bpId);
      ctx.warehouse = OBDal.getInstance().get(Warehouse.class, warehouseId);
      ctx.locator = OBDal.getInstance().get(Locator.class, locatorId);
      ctx.product = OBDal.getInstance().get(Product.class, productId);
      return ctx;
    }

    /** Reloads all entities after a session clear. */
    void reload(String docTypeId, String bpId, String warehouseId,
        String locatorId, String productId) {
      docType = OBDal.getInstance().get(DocumentType.class, docTypeId);
      bp = OBDal.getInstance().get(BusinessPartner.class, bpId);
      warehouse = OBDal.getInstance().get(Warehouse.class, warehouseId);
      locator = OBDal.getInstance().get(Locator.class, locatorId);
      product = OBDal.getInstance().get(Product.class, productId);
    }
  }

  /**
   * Queries orders by prefix, verifies count and loads lines lazily.
   * Returns the query time in milliseconds.
   */
  protected long verifyOrderQueryResults(String prefix, int expectedOrders,
      int expectedLinesPerOrder, String logPrefix) {
    long startQuery = System.nanoTime();
    OBDal.getInstance().getSession().clear();

    org.openbravo.dal.service.OBQuery<Order> query = OBDal.getInstance()
        .createQuery(Order.class, "documentNo like :prefix");
    query.setNamedParameter(PARAM_PREFIX, prefix);
    List<Order> orders = query.list();
    long queryTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startQuery);

    log.info(logPrefix + "Query " + orders.size() + " orders took: " + queryTime + " ms");
    org.junit.Assert.assertEquals(expectedOrders, orders.size());

    long startLines = System.nanoTime();
    int totalLines = 0;
    for (Order order : orders) {
      totalLines += order.getOrderLineList().size();
    }
    long linesTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startLines);

    log.info(logPrefix + "Loading " + totalLines + " order lines (lazy) took: "
        + linesTime + " ms");
    org.junit.Assert.assertEquals(expectedOrders * expectedLinesPerOrder, totalLines);
    return queryTime;
  }

  /**
   * Executes thread-local DAL work with proper error handling.
   * Commits on success, rolls back on failure, restores OBContext in finally.
   *
   * @param work   the work to execute
   * @param errors list to collect error messages
   * @param label  identifier for error messages (e.g. "Thread 1")
   */
  protected void executeThreadWork(Runnable work, List<String> errors, String label) {
    try {
      OBContext.setOBContext(CLIENT_USER_ID, CLIENT_ROLE_ID, CLIENT_CLIENT_ID, CLIENT_ORG_ID);
      OBContext.setAdminMode(true);
      work.run();
      OBDal.getInstance().flush();
      org.openbravo.dal.core.SessionHandler.getInstance().commitAndClose();
    } catch (Exception e) {
      errors.add(label + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
      try {
        OBDal.getInstance().rollbackAndClose();
      } catch (Exception ex) {
        // ignore rollback errors in thread cleanup
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Flushes the session, clears it, and returns. Callers should reload
   * entities after calling this.
   */
  protected void flushAndClear() {
    OBDal.getInstance().flush();
    OBDal.getInstance().getSession().clear();
  }

  // ==========================================================================
  // Order creation
  // ==========================================================================

  /**
   * Creates an Order (sales or purchase) in draft status.
   */
  protected Order createOrder(DocumentType docType, BusinessPartner bp, Warehouse warehouse,
      PriceList priceList, PaymentTerm paymentTerm, FIN_PaymentMethod paymentMethod,
      Currency currency, String docNo, boolean isSalesTransaction) {

    Order order = OBProvider.getInstance().get(Order.class);
    Organization org = OBContext.getOBContext().getCurrentOrganization();
    Date now = getOpenPeriodDate();

    order.setClient(OBContext.getOBContext().getCurrentClient());
    order.setOrganization(org);
    order.setDocumentNo(docNo);
    order.setDocumentStatus("DR");
    order.setDocumentAction("CO");
    order.setDocumentType(docType);
    order.setTransactionDocument(docType);
    order.setOrderDate(now);
    order.setAccountingDate(now);
    order.setBusinessPartner(bp);
    order.setPartnerAddress(bp.getBusinessPartnerLocationList().get(0));
    order.setPaymentMethod(paymentMethod);
    order.setCurrency(currency);
    order.setFormOfPayment("5");
    order.setPaymentTerms(paymentTerm);
    order.setInvoiceTerms("I");
    order.setDeliveryTerms("A");
    order.setFreightCostRule("I");
    order.setDeliveryMethod("P");
    order.setPriority("5");
    order.setWarehouse(warehouse);
    order.setPriceList(priceList);
    order.setSalesTransaction(isSalesTransaction);
    order.setProcessed(false);
    order.setPosted("N");
    order.setSummedLineAmount(BigDecimal.ZERO);
    order.setGrandTotalAmount(BigDecimal.ZERO);

    return order;
  }

  /**
   * Creates an OrderLine for the given order.
   */
  protected OrderLine createOrderLine(Order order, Product product, TaxRate tax, Long lineNo,
      BigDecimal quantity, BigDecimal unitPrice, BigDecimal listPrice) {

    OrderLine line = OBProvider.getInstance().get(OrderLine.class);
    Date now = getOpenPeriodDate();

    line.setClient(order.getClient());
    line.setOrganization(order.getOrganization());
    line.setSalesOrder(order);
    line.setLineNo(lineNo);
    line.setOrderDate(now);
    line.setScheduledDeliveryDate(now);
    line.setWarehouse(order.getWarehouse());
    line.setCurrency(order.getCurrency());
    line.setProduct(product);
    line.setUOM(product.getUOM());
    line.setOrderedQuantity(quantity);
    line.setTax(tax);
    line.setUnitPrice(unitPrice);
    line.setListPrice(listPrice);
    line.setLineNetAmount(quantity.multiply(unitPrice));

    return line;
  }

  // ==========================================================================
  // Shipment / Receipt creation
  // ==========================================================================

  /**
   * Creates a ShipmentInOut (goods shipment or goods receipt) in draft status.
   *
   * @param movementType       "C-" for shipment (customer), "V+" for receipt (vendor)
   * @param isSalesTransaction true for shipment, false for receipt
   */
  protected ShipmentInOut createShipmentOrReceipt(DocumentType docType, BusinessPartner bp,
      Warehouse warehouse, String docNo, String movementType, boolean isSalesTransaction) {

    ShipmentInOut shipmentReceipt = OBProvider.getInstance().get(ShipmentInOut.class);
    Organization org = OBContext.getOBContext().getCurrentOrganization();
    Date now = getOpenPeriodDate();

    shipmentReceipt.setClient(OBContext.getOBContext().getCurrentClient());
    shipmentReceipt.setOrganization(org);
    shipmentReceipt.setDocumentNo(docNo);
    shipmentReceipt.setDocumentStatus("DR");
    shipmentReceipt.setDocumentAction("CO");
    shipmentReceipt.setDocumentType(docType);
    shipmentReceipt.setMovementType(movementType);
    shipmentReceipt.setMovementDate(now);
    shipmentReceipt.setAccountingDate(now);
    shipmentReceipt.setBusinessPartner(bp);
    shipmentReceipt.setPartnerAddress(bp.getBusinessPartnerLocationList().get(0));
    shipmentReceipt.setWarehouse(warehouse);
    shipmentReceipt.setSalesTransaction(isSalesTransaction);
    shipmentReceipt.setProcessed(false);
    shipmentReceipt.setPosted("N");

    return shipmentReceipt;
  }

  /**
   * Creates a ShipmentInOutLine. Handles AttributeSetInstance logic when the product
   * requires at least one attribute value.
   */
  protected ShipmentInOutLine createShipmentOrReceiptLine(ShipmentInOut shipmentReceipt,
      Product product, Locator locator, Long lineNo, BigDecimal quantity) {

    ShipmentInOutLine line = OBProvider.getInstance().get(ShipmentInOutLine.class);

    line.setClient(shipmentReceipt.getClient());
    line.setOrganization(shipmentReceipt.getOrganization());
    line.setShipmentReceipt(shipmentReceipt);
    line.setLineNo(lineNo);
    line.setProduct(product);
    line.setUOM(product.getUOM());
    line.setMovementQuantity(quantity);
    line.setStorageBin(locator);

    if (product.getAttributeSet() != null
        && (product.getUseAttributeSetValueAs() == null
            || !"F".equals(product.getUseAttributeSetValueAs()))
        && product.getAttributeSet().isRequireAtLeastOneValue().booleanValue()) {
      AttributeSetInstance attr = OBProvider.getInstance().get(AttributeSetInstance.class);
      attr.setAttributeSet(product.getAttributeSet());
      attr.setDescription("1");
      OBDal.getInstance().save(attr);
      line.setAttributeSetValue(attr);
    }

    return line;
  }

  // ==========================================================================
  // Invoice creation
  // ==========================================================================

  /**
   * Creates an Invoice (sales or purchase) in draft status.
   */
  protected Invoice createInvoice(DocumentType docType, BusinessPartner bp, PriceList priceList,
      PaymentTerm paymentTerm, FIN_PaymentMethod paymentMethod, Currency currency,
      String docNo, boolean isSalesTransaction) {

    Invoice invoice = OBProvider.getInstance().get(Invoice.class);
    Organization org = OBContext.getOBContext().getCurrentOrganization();
    Date now = getOpenPeriodDate();

    invoice.setClient(OBContext.getOBContext().getCurrentClient());
    invoice.setOrganization(org);
    invoice.setDocumentNo(docNo);
    invoice.setDocumentStatus("DR");
    invoice.setDocumentAction("CO");
    invoice.setDocumentType(docType);
    invoice.setTransactionDocument(docType);
    invoice.setInvoiceDate(now);
    invoice.setAccountingDate(now);
    invoice.setBusinessPartner(bp);
    invoice.setPartnerAddress(bp.getBusinessPartnerLocationList().get(0));
    invoice.setPaymentMethod(paymentMethod);
    invoice.setCurrency(currency);
    invoice.setPaymentTerms(paymentTerm);
    invoice.setPriceList(priceList);
    invoice.setSalesTransaction(isSalesTransaction);
    invoice.setProcessed(false);
    invoice.setPosted("N");
    invoice.setSummedLineAmount(BigDecimal.ZERO);
    invoice.setGrandTotalAmount(BigDecimal.ZERO);

    return invoice;
  }

  /**
   * Creates an InvoiceLine for the given invoice.
   */
  protected InvoiceLine createInvoiceLine(Invoice invoice, Product product, TaxRate tax,
      Long lineNo, BigDecimal quantity, BigDecimal unitPrice, BigDecimal listPrice) {

    InvoiceLine line = OBProvider.getInstance().get(InvoiceLine.class);

    line.setClient(invoice.getClient());
    line.setOrganization(invoice.getOrganization());
    line.setInvoice(invoice);
    line.setLineNo(lineNo);
    line.setProduct(product);
    line.setUOM(product.getUOM());
    line.setInvoicedQuantity(quantity);
    line.setTax(tax);
    line.setUnitPrice(unitPrice);
    line.setListPrice(listPrice);
    line.setLineNetAmount(quantity.multiply(unitPrice));

    return line;
  }
}
