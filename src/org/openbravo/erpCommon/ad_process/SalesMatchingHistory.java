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
import org.openbravo.scheduling.KillableProcess;
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
public class SalesMatchingHistory extends DalBaseProcess implements KillableProcess {
  private static final String PREFERENCE_MATCH_DAYS = "MatchedSalesDaysBack";
  private static final String SUCCESS = "Success";
  private ProcessLogger logger;
  private boolean killProcess;

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
    logger = bundle.getLogger();
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
    lineCriteria.add(Restrictions.eq("inv." + Invoice.PROPERTY_SALESTRANSACTION, true));
    lineCriteria.add(Restrictions.ge("inv." + Invoice.PROPERTY_INVOICEDATE, fromDate));
    lineCriteria.add(Restrictions.in("inv." + Invoice.PROPERTY_DOCUMENTSTATUS, Arrays.asList("CO", "CL", "VO", "RE")));

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
        match = OBProvider.getInstance().get(SIMatch.class);
        match.setClient(invLine.getClient());
        match.setOrganization(invLine.getOrganization());
        match.setActive(true);
        match.setCreationDate(now);
        match.setCreatedBy(OBContext.getOBContext().getUser());
        match.setUpdated(now);
        match.setUpdatedBy(OBContext.getOBContext().getUser());
        match.setGoodsShipmentLine(shipLine);
        match.setInvoiceLine(invLine);
        match.setTransactionDate(invLine.getInvoice().getAccountingDate());
        match.setQuantity(qty);
        match.setProcessNow(false);
        match.setProcessed(true);
        match.setPosted("N");
        Product product = invLine.getProduct();
        if (product != null) {
          match.setProduct(product);
        }

        OBDal.getInstance().save(match);
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
   * Backfills {@link SOMatch} (M_MATCHSO) records for historical sales orders.
   * <p>
   * It handles two scenarios:
   * <ol>
   *   <li><strong>SO → GS</strong>: links sales order lines to goods shipment lines.</li>
   *   <li><strong>SO → SI</strong>: links sales order lines to sales invoice lines.</li>
   * </ol>
   *
   * <p><strong>SO → GS</strong>:</p>
   * <ul>
   *   <li>Processes {@link ShipmentInOutLine} records whose header is a sales
   *       shipment ({@code IsSOTrx = 'Y'}).</li>
   *   <li>Movement date {@code >= fromDate} and document status in {@code (CO, CL, VO, RE)}.</li>
   *   <li>Shipment line must be linked to a {@link OrderLine}.</li>
   *   <li>If no {@link SOMatch} exists for (order line, shipment line, no invoice line),
   *       a new row is created; otherwise the existing row is updated.</li>
   * </ul>
   *
   * <p><strong>SO → SI</strong>:</p>
   * <ul>
   *   <li>Processes {@link InvoiceLine} records from sales invoices
   *       ({@code IsSOTrx = 'Y'}).</li>
   *   <li>Invoice date {@code >= fromDate} and document status in {@code (CO, CL, VO, RE)}.</li>
   *   <li>Invoice line must be linked to a {@link OrderLine}.</li>
   *   <li>If no {@link SOMatch} exists for (order line, invoice line, no shipment line),
   *       a new row is created; otherwise the existing row is updated.</li>
   * </ul>
   * <p>
   * Changes are flushed in batches (every 100 records) to improve performance on large datasets.
   *
   * @param fromDate
   *     lower bound of the document date (movement date / invoice date) to consider
   */
  private void backfillSOMatch(Date fromDate) {
    int counter = 0;
    Date now = new Date();

    // 1) SO -> GS (OrderLine + ShipmentInOutLine, invoiceLine = null)
    OBCriteria<ShipmentInOutLine> shipLineCrit = OBDal.getInstance().createCriteria(ShipmentInOutLine.class);
    shipLineCrit.add(Restrictions.isNotNull(ShipmentInOutLine.PROPERTY_SALESORDERLINE));
    shipLineCrit.createAlias(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT, "ship");
    shipLineCrit.add(Restrictions.eq("ship." + ShipmentInOut.PROPERTY_SALESTRANSACTION, true));
    shipLineCrit.add(Restrictions.ge("ship." + ShipmentInOut.PROPERTY_MOVEMENTDATE, fromDate));
    shipLineCrit.add(
        Restrictions.in("ship." + ShipmentInOut.PROPERTY_DOCUMENTSTATUS, Arrays.asList("CO", "CL", "VO", "RE")));

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
        match = OBProvider.getInstance().get(SOMatch.class);
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

      counter++;
      if (counter % 100 == 0) {
        OBDal.getInstance().flush();
        OBDal.getInstance().getSession().clear();
      }
    }

    // 2) SO -> SI (OrderLine + InvoiceLine, goodsShipmentLine = null)
    OBCriteria<InvoiceLine> invLineCrit = OBDal.getInstance().createCriteria(InvoiceLine.class);
    invLineCrit.createAlias(InvoiceLine.PROPERTY_INVOICE, "inv");
    invLineCrit.add(Restrictions.eq("inv." + Invoice.PROPERTY_SALESTRANSACTION, true));
    invLineCrit.add(Restrictions.ge("inv." + Invoice.PROPERTY_INVOICEDATE, fromDate));
    invLineCrit.add(Restrictions.in("inv." + Invoice.PROPERTY_DOCUMENTSTATUS, Arrays.asList("CO", "CL", "VO", "RE")));
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
        match = OBProvider.getInstance().get(SOMatch.class);
        match.setClient(invLine.getClient());
        match.setOrganization(invLine.getOrganization());
        match.setActive(true);
        match.setCreationDate(now);
        match.setCreatedBy(OBContext.getOBContext().getUser());
        match.setUpdated(now);
        match.setUpdatedBy(OBContext.getOBContext().getUser());
        match.setSalesOrderLine(orderLine);
        match.setInvoiceLine(invLine);
        match.setTransactionDate(invLine.getInvoice().getAccountingDate());
        match.setQuantity(qty);
        match.setProcessNow(false);
        match.setProcessed(true);
        match.setPosted("N");
        Product product = invLine.getProduct();
        if (product != null) {
          match.setProduct(product);
        }

        OBDal.getInstance().save(match);
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

  /**
   * Requests a graceful stop of the process.
   * <p>
   * This method sets the {@code killProcess} flag to {@code true}. Loops inside
   * the process can periodically check this flag and abort early if needed.
   *
   * @param processBundle
   *     the bundle representing the running process instance
   * @throws Exception
   *     unused, declared to comply with {@link KillableProcess}
   */
  @Override
  public void kill(ProcessBundle processBundle) throws Exception {
    this.killProcess = true;
  }
}
