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
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.common.actionhandler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;

/**
 * Recomputes the credit used (C_BPartner.SO_CreditUsed) of a business partner when its currency is
 * changed by the Set New Currency process.
 * <p>
 * The credit used is an aggregate of the outstanding amounts of the pending invoices, each of them
 * converted to the business partner currency with the rate that applied when the invoice was
 * completed. Converting the aggregate with one single rate leaves a residual balance once the
 * invoices are settled, because settlement converts each paid invoice with the rate of the invoice.
 * This class instead converts every pending invoice with the same criteria used by the settlement
 * and the remainder of the old balance (credit not tied to pending invoices) with the rate chosen
 * by the user.
 * <p>
 * If any pending invoice cannot be converted, the whole currency change must be blocked: an
 * {@link OBException} is thrown before anything has been modified.
 * <p>
 * Design notes:
 * <ul>
 * <li>The recomputation does not depend on the old credit used being different from zero. With a
 * zero balance there can still be pending invoices offset by credit not tied to invoices, and the
 * recomputed value is the one that stays consistent when those invoices are settled.</li>
 * <li>The rate of each invoice is searched with the organization and the currency of the invoice.
 * Settlement of a manual payment searches it with the organization of the payment, so both can
 * differ when the payment belongs to another organization or is made in another currency. This is
 * a known approximation.</li>
 * </ul>
 */
final class BPCreditUsedRecalculator {

  /** AD_Message keys. Each one is a whole sentence so it can be translated and reordered. */
  static final String MSG_NO_RATE = "BPCurrencyNoInvoiceRate";
  static final String MSG_NO_RATE_ITEM = "BPCurrencyNoInvoiceRateItem";
  static final String MSG_NO_RATE_MORE = "BPCurrencyNoInvoiceRateMore";

  private static final int MAX_INVOICES_IN_MESSAGE = 10;
  private static final String ITEM_SEPARATOR = "; ";
  private static final String NULL_TEXT = "null";
  private static final String KEY_SEPARATOR = "|";
  private static final String PARAM_BP = "bpId";
  private static final String PARAM_OLD_CURRENCY = "oldCurrencyId";
  private static final String PARAM_NEW_CURRENCY = "newCurrencyId";

  /**
   * Pending invoice payment schedules of the business partner. Only the scalar values needed to
   * convert them are read, no entity is loaded.
   */
  private static final String PENDING_SCHEDULES_HQL = "select i.id, i.documentNo, i.currency.id, "
      + "i.currency.iSOCode, i.invoiceDate, i.organization.id, i.client.id, i.salesTransaction, "
      + "fps.outstandingAmount "
      + "from FIN_Payment_Schedule fps join fps.invoice i "
      + "where i.businessPartner.id = :" + PARAM_BP + " and i.processed = true "
      + "and fps.outstandingAmount <> 0";

  /** Rates defined at invoice document level for the invoices of the business partner. */
  private static final String DOC_RATES_HQL = "select d.invoice.id, d.currency.id, "
      + "d.toCurrency.id, d.rate "
      + "from CurrencyConversionRateDoc d "
      + "where d.invoice.businessPartner.id = :" + PARAM_BP + " and d.active = true "
      + "and d.toCurrency.id in (:" + PARAM_OLD_CURRENCY + ", :" + PARAM_NEW_CURRENCY + ")";

  private BPCreditUsedRecalculator() {
    // Utility class
  }

  /**
   * Computes the new credit used of the business partner, expressed in the new currency.
   * <p>
   * It does not modify anything. It returns null when the recomputation does not apply: the user
   * set the amount manually, there is no new currency, both currencies are the same or the chosen
   * rate is missing or zero. In those cases the caller keeps its previous behavior.
   *
   * @param bp
   *          the business partner whose currency is being changed
   * @param old
   *          the credit used of the business partner, in the old currency, captured before any
   *          modification. It can be null (treated as zero).
   * @param fromId
   *          the id of the old currency
   * @param toId
   *          the id of the new currency, it can be null or the text "null"
   * @param rate
   *          the rate chosen by the user to convert from the old to the new currency
   * @param setAmount
   *          whether the user has set the new amount manually
   * @return the new credit used, or null if the recomputation does not apply
   * @throws OBException
   *           if any pending invoice cannot be converted. The message names the invoices.
   */
  static BigDecimal recalculate(BusinessPartner bp, BigDecimal old, String fromId, String toId,
      BigDecimal rate, boolean setAmount) {
    if (!applies(fromId, toId, rate, setAmount)) {
      return null;
    }
    final List<PendingSchedule> rows = loadPendingSchedules(bp.getId());
    final int ratePrecision = FIN_Utility
        .getConversionRatePrecision(RequestContext.get().getVariablesSecureApp());
    final Map<String, BigDecimal> docRates = rows.isEmpty()
        ? Collections.<String, BigDecimal> emptyMap()
        : loadDocRates(bp.getId(), fromId, toId);
    final RateResolver resolver = new SettlementRateResolver(
        OBDal.getInstance().get(Currency.class, fromId),
        OBDal.getInstance().get(Currency.class, toId), docRates, ratePrecision);
    return recalculateWith(old, rate, rows, resolver, OBMessageUtils::messageBD);
  }

  /**
   * Computes the new credit used from already loaded data and throws if any pending invoice cannot
   * be converted. This is the seam used to test the blocking path without a database.
   *
   * @param old
   *          the credit used in the old currency. It can be null (treated as zero).
   * @param chosenRate
   *          the rate chosen by the user
   * @param rows
   *          the pending invoice payment schedules
   * @param resolver
   *          converts the outstanding amount of each schedule
   * @param messages
   *          returns the (translated) text of an AD_Message given its key
   * @return the new credit used
   * @throws OBException
   *           if any pending invoice cannot be converted
   */
  static BigDecimal recalculateWith(BigDecimal old, BigDecimal chosenRate,
      List<PendingSchedule> rows, RateResolver resolver, Function<String, String> messages) {
    final Result result = compute(old, chosenRate, rows, resolver);
    if (!result.missing.isEmpty()) {
      throw new OBException(
          buildMissingRateMessage(result.missing, MAX_INVOICES_IN_MESSAGE, messages));
    }
    return result.value;
  }

  private static boolean applies(String fromId, String toId, BigDecimal rate,
      boolean setAmount) {
    return !setAmount && !StringUtils.isEmpty(fromId) && !NULL_TEXT.equals(fromId)
        && !StringUtils.isEmpty(toId) && !NULL_TEXT.equals(toId)
        && !StringUtils.equals(fromId, toId) && rate != null && rate.signum() != 0;
  }

  /**
   * Pure computation of the new credit used.
   * <p>
   * The result is the sum of the pending invoices converted with the settlement criteria, plus the
   * part of the old credit that is not explained by the pending invoices (converted with the
   * chosen rate). The part explained by the pending invoices is the sum of the same invoices
   * converted to the old currency with the criteria used when the invoices were completed. Nothing
   * is rounded here: settlement subtracts unrounded amounts.
   *
   * @param old
   *          the credit used in the old currency. It can be null (treated as zero).
   * @param chosenRate
   *          the rate chosen by the user, applied to the remainder
   * @param rows
   *          the pending invoice payment schedules
   * @param r
   *          converts each schedule to the new and to the old currency
   * @return the new credit used, or the list of invoices that could not be converted
   */
  static Result compute(BigDecimal old, BigDecimal chosenRate, List<PendingSchedule> rows,
      RateResolver r) {
    BigDecimal newPart = BigDecimal.ZERO;
    BigDecimal oldPart = BigDecimal.ZERO;
    final List<MissingRate> missing = new ArrayList<>();
    for (PendingSchedule s : rows) {
      final BigDecimal converted = r.toNew(s);
      final BigDecimal original = r.toOld(s);
      if (converted == null) {
        missing.add(new MissingRate(s.documentNo, s.currencyIso, r.newCurrencyIso(),
            s.invoiceDate));
      }
      if (original == null) {
        missing.add(new MissingRate(s.documentNo, s.currencyIso, r.oldCurrencyIso(),
            s.invoiceDate));
      }
      if (converted != null && original != null) {
        newPart = newPart.add(signed(s, converted));
        oldPart = oldPart.add(signed(s, original));
      }
    }
    if (!missing.isEmpty()) {
      return new Result(null, missing);
    }
    final BigDecimal base = old == null ? BigDecimal.ZERO : old;
    return new Result(newPart.add(base.subtract(oldPart).multiply(chosenRate)),
        Collections.<MissingRate> emptyList());
  }

  private static BigDecimal signed(PendingSchedule s, BigDecimal amount) {
    return s.sales ? amount : amount.negate();
  }

  /**
   * Builds the message that names the invoices that could not be converted. Every sentence comes
   * from an AD_Message with positional placeholders (%s), so a translation can reorder the words.
   *
   * @param missing
   *          the conversions that could not be found
   * @param cap
   *          the maximum number of conversions named in the message. The rest are only counted.
   * @param messages
   *          returns the (translated) text of an AD_Message given its key
   * @return the message, in a single line
   */
  static String buildMissingRateMessage(List<MissingRate> missing, int cap,
      Function<String, String> messages) {
    final String itemTemplate = messages.apply(MSG_NO_RATE_ITEM);
    final Set<String> seen = new HashSet<>();
    final List<String> entries = new ArrayList<>();
    int unique = 0;
    for (MissingRate m : missing) {
      final String key = m.documentNo + KEY_SEPARATOR + m.fromIso + KEY_SEPARATOR + m.toIso;
      if (seen.add(key)) {
        unique++;
        if (entries.size() < cap) {
          entries.add(describe(m, itemTemplate));
        }
      }
    }
    final int remaining = unique - entries.size();
    if (remaining > 0) {
      entries.add(String.format(messages.apply(MSG_NO_RATE_MORE), remaining));
    }
    return String.format(messages.apply(MSG_NO_RATE), String.join(ITEM_SEPARATOR, entries));
  }

  private static String describe(MissingRate m, String itemTemplate) {
    // The error translator parses the tokens between @, so they are removed from the document no.
    return String.format(itemTemplate, m.documentNo.replace("@", ""), m.fromIso, m.toIso,
        OBDateUtils.formatDate(m.date));
  }

  private static List<PendingSchedule> loadPendingSchedules(String bpId) {
    final List<Object[]> rows = OBDal.getInstance()
        .getSession()
        .createQuery(PENDING_SCHEDULES_HQL, Object[].class)
        .setParameter(PARAM_BP, bpId)
        .list();
    final List<PendingSchedule> schedules = new ArrayList<>(rows.size());
    for (Object[] row : rows) {
      schedules.add(new PendingSchedule((String) row[0], (String) row[1], (String) row[2],
          (String) row[3], (Date) row[4], (String) row[5], (String) row[6],
          Boolean.TRUE.equals(row[7]), (BigDecimal) row[8]));
    }
    return schedules;
  }

  private static Map<String, BigDecimal> loadDocRates(String bpId, String fromId, String toId) {
    final List<Object[]> rows = OBDal.getInstance()
        .getSession()
        .createQuery(DOC_RATES_HQL, Object[].class)
        .setParameter(PARAM_BP, bpId)
        .setParameter(PARAM_OLD_CURRENCY, fromId)
        .setParameter(PARAM_NEW_CURRENCY, toId)
        .list();
    final Map<String, BigDecimal> docRates = new HashMap<>();
    for (Object[] row : rows) {
      // Like settlement, the first rate found for each invoice and currencies is used
      docRates.putIfAbsent(SettlementRateResolver.docRateKey((String) row[0], (String) row[1],
          (String) row[2]), (BigDecimal) row[3]);
    }
    return docRates;
  }

  /** Converts the outstanding amount of a pending invoice payment schedule. */
  interface RateResolver {

    /**
     * Converts the outstanding amount to the new currency, as settlement does.
     *
     * @param s
     *          the pending payment schedule
     * @return the converted amount, or null if there is no usable rate
     */
    BigDecimal toNew(PendingSchedule s);

    /**
     * Converts the outstanding amount to the old currency, as it was done when the invoice was
     * completed.
     *
     * @param s
     *          the pending payment schedule
     * @return the converted amount, or null if there is no usable rate
     */
    BigDecimal toOld(PendingSchedule s);

    /** Returns the ISO code of the old currency, used to report missing rates. */
    String oldCurrencyIso();

    /** Returns the ISO code of the new currency, used to report missing rates. */
    String newCurrencyIso();
  }

  /** A pending payment schedule of a processed invoice. */
  static final class PendingSchedule {
    final String invoiceId;
    final String documentNo;
    final String currencyId;
    final String currencyIso;
    final Date invoiceDate;
    final String orgId;
    final String clientId;
    final boolean sales;
    final BigDecimal outstanding;

    PendingSchedule(String invoiceId, String documentNo, String currencyId, String currencyIso,
        Date invoiceDate, String orgId, String clientId, boolean sales, BigDecimal outstanding) {
      this.invoiceId = invoiceId;
      this.documentNo = documentNo;
      this.currencyId = currencyId;
      this.currencyIso = currencyIso;
      this.invoiceDate = invoiceDate;
      this.orgId = orgId;
      this.clientId = clientId;
      this.sales = sales;
      this.outstanding = outstanding;
    }
  }

  /** A conversion for which no usable rate was found. */
  static final class MissingRate {
    final String documentNo;
    final String fromIso;
    final String toIso;
    final Date date;

    MissingRate(String documentNo, String fromIso, String toIso, Date date) {
      this.documentNo = documentNo;
      this.fromIso = fromIso;
      this.toIso = toIso;
      this.date = date;
    }
  }

  /** The outcome of the computation: either the value or the conversions that are missing. */
  static final class Result {
    final BigDecimal value;
    final List<MissingRate> missing;

    Result(BigDecimal value, List<MissingRate> missing) {
      this.value = value;
      this.missing = missing;
    }
  }
}
