/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright © 2021–2025 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.ad_process;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.sales.SIMatch;
import org.openbravo.model.sales.SOMatch;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

/**
 * Background process that backfills historical sales matching data.
 * <p>
 * It populates {@link SIMatch} (M_MATCHSI) and {@link SOMatch} (M_MATCHSO) entries
 * for existing sales documents (Sales Invoices, Sales Orders and Goods Shipments)
 * within a configurable number of days in the past.
 * <p>
 * The number of days to look back is controlled by the {@code MatchedSalesDaysBack}
 * preference. For each eligible record, the process:
 * <ul>
 *   <li>Creates a new match row if none exists yet, or</li>
 *   <li>Updates quantity and transaction date in the existing match row.</li>
 * </ul>
 */
public class SalesMatchingHistory extends DalBaseProcess {
  private static final String PREFERENCE_MATCH_DAYS = "MatchedSalesDaysBack";
  private static final String SUCCESS = "Success";
  private static final String SHIPMENT_ALIAS_PREFIX = "ship.";
  private static final String INVOICE_ALIAS_PREFIX = "inv.";

  /**
   * Main entry point for the process.
   * <p>
   * It reads the {@code MatchedSalesDaysBack} preference, calculates the
   * start date, and then backfills both:
   * <ul>
   *   <li>{@link SIMatch} (invoice ↔ shipment) and</li>
   *   <li>{@link SOMatch} (order ↔ shipment, order ↔ invoice)</li>
   * for sales documents within that period.
   * <p>
   * On success, it sets an {@link OBError} with type {@code Success}.
   * On failure, it logs the error and rethrows an {@link OBException}.
   *
   * @param bundle
   *     the process bundle providing context (logger, parameters, etc.)
   * @throws Exception
   *     if any unexpected error occurs during execution
   */
  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    ProcessLogger logger = bundle.getLogger();
    OBError result = new OBError();
    try {
      OBContext.setAdminMode(false);
      result.setType(SUCCESS);
      result.setTitle(OBMessageUtils.messageBD(SUCCESS));

      int amountOfDays = getAmountOfDays();
      Date fromDate = calculateFromDate(amountOfDays);

      backfillSIMatch(fromDate);
      backfillSOMatch(fromDate);

      OBDal.getInstance().flush();
      logger.logln(OBMessageUtils.messageBD(SUCCESS));
      bundle.setResult(result);
    } catch (Exception e) {
      String errorMessage = OBMessageUtils.messageBD(e.getMessage());
      logger.logln(OBMessageUtils.messageBD("error: " + errorMessage));
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Backfills {@link SIMatch} (M_MATCHSI) records for historical sales invoices.
   * <p>
   * This method processes:
   * <ul>
   *   <li>Sales invoices ({@code IsSOTrx = 'Y'})</li>
   *   <li>Invoice lines that are linked to a {@link ShipmentInOutLine}</li>
   *   <li>Invoices with invoice date {@code >= fromDate}</li>
   *   <li>Invoice document status in {@code (CO, CL, VO, RE)}</li>
   * </ul>
   * For each eligible invoice line:
   * <ul>
   *   <li>If no {@link SIMatch} exists for the (shipment line, invoice line) pair,
   *       a new row is created with the invoiced quantity.</li>
   *   <li>If a match already exists, its quantity and transaction date are updated.</li>
   * </ul>
   * Changes are flushed in batches (every 100 records) to avoid excessive memory usage.
   *
   * @param fromDate
   *     lower bound of the invoice date (inclusive) to consider
   */
  private void backfillSIMatch(Date fromDate) {
    int counter = 0;

    OBCriteria<InvoiceLine> lineCriteria = OBDal.getInstance().createCriteria(InvoiceLine.class);
    lineCriteria.createAlias(InvoiceLine.PROPERTY_INVOICE, "inv");
    lineCriteria.createAlias(InvoiceLine.PROPERTY_GOODSSHIPMENTLINE, "shipLine");
    lineCriteria.add(Restrictions.isNotNull(InvoiceLine.PROPERTY_GOODSSHIPMENTLINE));
    lineCriteria.add(Restrictions.eq(INVOICE_ALIAS_PREFIX + Invoice.PROPERTY_SALESTRANSACTION, true));
    lineCriteria.add(Restrictions.ge(INVOICE_ALIAS_PREFIX + Invoice.PROPERTY_INVOICEDATE, fromDate));
    lineCriteria.add(
        Restrictions.in(INVOICE_ALIAS_PREFIX + Invoice.PROPERTY_DOCUMENTSTATUS, Arrays.asList("CO", "CL", "VO", "RE")));

    List<InvoiceLine> lines = lineCriteria.list();
    Date now = new Date();

    for (InvoiceLine invLine : lines) {
      ShipmentInOutLine shipLine = invLine.getGoodsShipmentLine();
      if (shipLine == null) {
        continue;
      }

      BigDecimal qty = invLine.getInvoicedQuantity();
      if (qty == null) {
        qty = BigDecimal.ZERO;
      }

      OBCriteria<SIMatch> matchCrit = OBDal.getInstance().createCriteria(SIMatch.class);
      matchCrit.add(Restrictions.eq(SIMatch.PROPERTY_GOODSSHIPMENTLINE, shipLine));
      matchCrit.add(Restrictions.eq(SIMatch.PROPERTY_INVOICELINE, invLine));

      SIMatch match = (SIMatch) matchCrit.uniqueResult();

      if (match == null) {
        createShipmentSIMatch(invLine, shipLine, qty, now);
      }

      counter++;
      if (counter % 100 == 0) {
        OBDal.getInstance().flush();
        OBDal.getInstance().getSession().clear();
      }
    }

    OBDal.getInstance().flush();
  }

  /**
   * Creates a {@link SIMatch} (M_MATCHSI) record for the
   * Sales Invoice → Goods Shipment scenario.
   * <p>
   * The new match links the given invoice line and shipment line,
   * sets the transaction date from the invoice accounting date,
   * stores the provided quantity, and initializes all audit and
   * processing flags ({@code active}, {@code processed}, {@code posted}, etc.).
   *
   * @param invLine
   *     sales invoice line to be matched
   * @param shipLine
   *     goods shipment line related to the invoice
   * @param qty
   *     quantity to store in the match record
   * @param now
   *     timestamp used for creation and update audit fields
   */
  private void createShipmentSIMatch(InvoiceLine invLine, ShipmentInOutLine shipLine, java.math.BigDecimal qty,
      java.util.Date now) {
    SIMatch match = OBProvider.getInstance().get(SIMatch.class);
    match.setClient(invLine.getClient());
    match.setOrganization(invLine.getOrganization());
    match.setActive(true);
    match.setCreationDate(now);
    match.setCreatedBy(OBContext.getOBContext().getUser());
    match.setUpdated(now);
    match.setUpdatedBy(OBContext.getOBContext().getUser());
    match.setGoodsShipmentLine(shipLine);

    fillInvoiceMatchCommonData(match, invLine, qty);

    OBDal.getInstance().save(match);
  }

  /**
   * Backfills {@link SOMatch} (M_MATCHSO) records for historical sales orders.
   * <p>
   * It delegates to the shipment-based and invoice-based backfill methods
   * and performs a final {@link OBDal#flush()} to persist all pending changes.
   *
   * @param fromDate
   *     lower bound of the document date (movement date / invoice date) to consider
   */
  private void backfillSOMatch(Date fromDate) {
    Date now = new Date();

    backfillSOMatchFromShipments(fromDate, now);
    backfillSOMatchFromInvoices(fromDate, now);

    OBDal.getInstance().flush();
  }

  /**
   * Backfills {@link SOMatch} (M_MATCHSO) records for the
   * <strong>SO → GS</strong> scenario (Sales Order → Goods Shipment).
   * <p>
   * It processes {@link ShipmentInOutLine} records that:
   * <ul>
   *   <li>belong to a sales shipment header ({@code IsSOTrx = 'Y'}),</li>
   *   <li>have a movement date {@code >= fromDate},</li>
   *   <li>have a document status in {@code (CO, CL, VO, RE)}, and</li>
   *   <li>are linked to a {@link OrderLine}.</li>
   * </ul>
   * For each eligible shipment line, if no {@link SOMatch} exists for the
   * (order line, shipment line, no invoice line) combination, a new record is created.
   * Changes are flushed in batches through {@link #incrementAndMaybeFlush(int)}.
   *
   * @param fromDate
   *     lower bound of the shipment movement date (inclusive) to consider
   * @param now
   *     reference timestamp used to populate audit fields in new match records
   */
  private void backfillSOMatchFromShipments(Date fromDate, Date now) {
    int counter = 0;

    // SO -> GS (OrderLine + ShipmentInOutLine, invoiceLine = null)
    OBCriteria<ShipmentInOutLine> shipLineCrit = OBDal.getInstance().createCriteria(ShipmentInOutLine.class);
    shipLineCrit.add(Restrictions.isNotNull(ShipmentInOutLine.PROPERTY_SALESORDERLINE));
    shipLineCrit.createAlias(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT, "ship");
    shipLineCrit.add(Restrictions.eq(SHIPMENT_ALIAS_PREFIX + ShipmentInOut.PROPERTY_SALESTRANSACTION, true));
    shipLineCrit.add(Restrictions.ge(SHIPMENT_ALIAS_PREFIX + ShipmentInOut.PROPERTY_MOVEMENTDATE, fromDate));
    shipLineCrit.add(Restrictions.in(SHIPMENT_ALIAS_PREFIX + ShipmentInOut.PROPERTY_DOCUMENTSTATUS,
        Arrays.asList("CO", "CL", "VO", "RE")));

    List<ShipmentInOutLine> shipLines = shipLineCrit.list();

    for (ShipmentInOutLine shipLine : shipLines) {
      OrderLine orderLine = shipLine.getSalesOrderLine();
      if (orderLine == null) {
        continue;
      }

      BigDecimal qty = shipLine.getMovementQuantity();
      if (qty == null) {
        qty = BigDecimal.ZERO;
      }

      OBCriteria<SOMatch> matchCrit = OBDal.getInstance().createCriteria(SOMatch.class);
      matchCrit.add(Restrictions.eq(SOMatch.PROPERTY_SALESORDERLINE, orderLine));
      matchCrit.add(Restrictions.eq(SOMatch.PROPERTY_GOODSSHIPMENTLINE, shipLine));
      matchCrit.add(Restrictions.isNull(SOMatch.PROPERTY_INVOICELINE));

      SOMatch match = (SOMatch) matchCrit.uniqueResult();

      if (match == null) {
        createShipmentSOMatch(shipLine, orderLine, qty, now);
      }

      counter = incrementAndMaybeFlush(counter);
    }
  }

  /**
   * Backfills {@link SOMatch} (M_MATCHSO) records for the
   * <strong>SO → SI</strong> scenario (Sales Order → Sales Invoice).
   * <p>
   * It processes {@link InvoiceLine} records that:
   * <ul>
   *   <li>belong to a sales invoice header ({@code IsSOTrx = 'Y'}),</li>
   *   <li>have an invoice date {@code >= fromDate},</li>
   *   <li>have a document status in {@code (CO, CL, VO, RE)}, and</li>
   *   <li>are linked to a {@link OrderLine}.</li>
   * </ul>
   * For each eligible invoice line, if no {@link SOMatch} exists for the
   * (order line, invoice line, no shipment line) combination, a new record is created.
   * Changes are flushed in batches through {@link #incrementAndMaybeFlush(int)}.
   *
   * @param fromDate
   *     lower bound of the invoice date (inclusive) to consider
   * @param now
   *     reference timestamp used to populate audit fields in new match records
   */
  private void backfillSOMatchFromInvoices(Date fromDate, Date now) {
    int counter = 0;

    // SO -> SI (OrderLine + InvoiceLine, goodsShipmentLine = null)
    OBCriteria<InvoiceLine> invLineCrit = OBDal.getInstance().createCriteria(InvoiceLine.class);
    invLineCrit.createAlias(InvoiceLine.PROPERTY_INVOICE, "inv");
    invLineCrit.add(Restrictions.eq(INVOICE_ALIAS_PREFIX + Invoice.PROPERTY_SALESTRANSACTION, true));
    invLineCrit.add(Restrictions.ge(INVOICE_ALIAS_PREFIX + Invoice.PROPERTY_INVOICEDATE, fromDate));
    invLineCrit.add(
        Restrictions.in(INVOICE_ALIAS_PREFIX + Invoice.PROPERTY_DOCUMENTSTATUS, Arrays.asList("CO", "CL", "VO", "RE")));
    invLineCrit.add(Restrictions.isNotNull(InvoiceLine.PROPERTY_SALESORDERLINE));

    List<InvoiceLine> invLines = invLineCrit.list();

    for (InvoiceLine invLine : invLines) {
      OrderLine orderLine = invLine.getSalesOrderLine();
      if (orderLine == null) {
        continue;
      }

      BigDecimal qty = invLine.getInvoicedQuantity();
      if (qty == null) {
        qty = BigDecimal.ZERO;
      }

      OBCriteria<SOMatch> matchCrit = OBDal.getInstance().createCriteria(SOMatch.class);
      matchCrit.add(Restrictions.eq(SOMatch.PROPERTY_SALESORDERLINE, orderLine));
      matchCrit.add(Restrictions.eq(SOMatch.PROPERTY_INVOICELINE, invLine));
      matchCrit.add(Restrictions.isNull(SOMatch.PROPERTY_GOODSSHIPMENTLINE));

      SOMatch match = (SOMatch) matchCrit.uniqueResult();

      if (match == null) {
        createInvoiceSOMatch(invLine, orderLine, qty, now);
      }

      counter = incrementAndMaybeFlush(counter);
    }
  }

  /**
   * Creates a {@link SOMatch} (M_MATCHSO) record for the
   * <strong>SO → GS</strong> scenario (Sales Order → Goods Shipment).
   * <p>
   * The new match links the given sales order line and shipment line,
   * sets the transaction date from the shipment header movement date,
   * stores the provided quantity, and initializes all audit and
   * processing flags ({@code active}, {@code processed}, {@code posted}, etc.).
   *
   * @param shipLine
   *     goods shipment line to be matched
   * @param orderLine
   *     sales order line related to the shipment
   * @param qty
   *     quantity to store in the match record
   * @param now
   *     timestamp used for creation and update audit fields
   */
  private void createShipmentSOMatch(ShipmentInOutLine shipLine, OrderLine orderLine, BigDecimal qty, Date now) {
    SOMatch match = OBProvider.getInstance().get(SOMatch.class);
    match.setClient(shipLine.getClient());
    match.setOrganization(shipLine.getOrganization());
    match.setActive(true);
    match.setCreationDate(now);
    match.setCreatedBy(OBContext.getOBContext().getUser());
    match.setUpdated(now);
    match.setUpdatedBy(OBContext.getOBContext().getUser());

    match.setSalesOrderLine(orderLine);
    match.setGoodsShipmentLine(shipLine);
    match.setTransactionDate(shipLine.getShipmentReceipt().getMovementDate());
    match.setQuantity(qty);
    match.setProcessNow(false);
    match.setProcessed(true);
    match.setPosted("N");

    Product product = shipLine.getProduct();
    if (product != null) {
      match.setProduct(product);
    }

    OBDal.getInstance().save(match);
  }

  /**
   * Creates a {@link SOMatch} (M_MATCHSO) record for the
   * <strong>SO → SI</strong> scenario (Sales Order → Sales Invoice).
   * <p>
   * The new match links the given sales order line and invoice line,
   * sets the transaction date from the invoice accounting date,
   * stores the provided quantity, and initializes all audit and
   * processing flags ({@code active}, {@code processed}, {@code posted}, etc.).
   *
   * @param invLine
   *     sales invoice line to be matched
   * @param orderLine
   *     sales order line related to the invoice
   * @param qty
   *     quantity to store in the match record
   * @param now
   *     timestamp used for creation and update audit fields
   */
  private void createInvoiceSOMatch(InvoiceLine invLine, OrderLine orderLine, BigDecimal qty, Date now) {
    SOMatch match = OBProvider.getInstance().get(SOMatch.class);
    match.setClient(invLine.getClient());
    match.setOrganization(invLine.getOrganization());
    match.setActive(true);
    match.setCreationDate(now);
    match.setCreatedBy(OBContext.getOBContext().getUser());
    match.setUpdated(now);
    match.setUpdatedBy(OBContext.getOBContext().getUser());
    match.setSalesOrderLine(orderLine);
    fillInvoiceMatchCommonData(match, invLine, qty);

    OBDal.getInstance().save(match);
  }

  /**
   * Fills common invoice-related data for SIMatch / SOMatch instances.
   *
   * @param match
   *     DAL entity instance (SIMatch or SOMatch)
   * @param invLine
   *     invoice line used as a source
   * @param qty
   *     quantity to store in the match
   */
  private void fillInvoiceMatchCommonData(BaseOBObject match, InvoiceLine invLine, BigDecimal qty) {
    match.set(SIMatch.PROPERTY_INVOICELINE, invLine);
    match.set(SIMatch.PROPERTY_TRANSACTIONDATE, invLine.getInvoice().getAccountingDate());
    match.set(SIMatch.PROPERTY_QUANTITY, qty);
    match.set(SIMatch.PROPERTY_PROCESSNOW, false);
    match.set(SIMatch.PROPERTY_PROCESSED, true);
    match.set(SIMatch.PROPERTY_POSTED, "N");

    Product product = invLine.getProduct();
    if (product != null) {
      match.set(SIMatch.PROPERTY_PRODUCT, product);
    }
  }

  /**
   * Increments a batch counter and performs a periodic DAL flush/clear.
   * <p>
   * When the counter reaches a multiple of 100, it:
   * <ul>
   *   <li>flushes pending changes to the database, and</li>
   *   <li>clears the current Hibernate session to free memory.</li>
   * </ul>
   *
   * @param counter
   *     current processed records counter
   * @return the incremented counter-value
   */
  private int incrementAndMaybeFlush(int counter) {
    counter++;
    if (counter % 100 == 0) {
      OBDal.getInstance().flush();
      OBDal.getInstance().getSession().clear();
    }
    return counter;
  }

  /**
   * Returns the number of days to look back when backfilling matches.
   * <p>
   * The value is read from the {@code MatchedSalesDaysBack} preference for the
   * current client, organization, user and role. If the preference is missing
   * or cannot be parsed as an integer, an {@link OBException} is thrown with
   * the message {@code MatchedSalesDaysBackError}.
   *
   * @return number of days to go back from today when searching for historical data
   * @throws OBException
   *     if the preference is not defined or is not a valid integer
   */
  private static int getAmountOfDays() {
    int amountOfDays;
    try {
      String preferenceValue = Preferences.getPreferenceValue(PREFERENCE_MATCH_DAYS, true,
          OBContext.getOBContext().getCurrentClient(), OBContext.getOBContext().getCurrentOrganization(),
          OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null);
      amountOfDays = Integer.parseInt(preferenceValue);
    } catch (Exception e) {
      throw new OBException(OBMessageUtils.messageBD("MatchedSalesDaysBackError"));
    }
    return amountOfDays;
  }

  /**
   * Calculates the starting date used to filter historical documents.
   * <p>
   * The returned date is computed as:
   * <ul>
   *   <li>today at midnight (00:00:00.000) in the server timezone,</li>
   *   <li>minus {@code amountOfDays} days.</li>
   * </ul>
   *
   * @param amountOfDays
   *     number of days to go back from today
   * @return a {@link Date} representing the lower bound of the search window
   */
  private static Date calculateFromDate(int amountOfDays) {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    cal.add(Calendar.DAY_OF_YEAR, -amountOfDays);
    return cal.getTime();
  }
}
