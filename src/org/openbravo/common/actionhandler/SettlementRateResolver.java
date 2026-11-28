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
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.common.actionhandler.BPCreditUsedRecalculator.PendingSchedule;
import org.openbravo.common.actionhandler.BPCreditUsedRecalculator.RateResolver;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Converts the outstanding amount of a pending invoice payment schedule to the new and to the old
 * currency of a business partner, mimicking the criteria used by the rest of the application:
 * <ul>
 * <li>To the new currency, the criteria of the payment settlement (FIN_PaymentProcess): the rate
 * defined at invoice document level, unrounded, or else the rate of the conversion rate table
 * rounded to the conversion rate precision. The converted amount is not rounded.</li>
 * <li>To the old currency, the criteria used when the invoice was completed
 * (APRM_GEN_PAYMENTSCHEDULE_INV and C_CURRENCY_RATE): the rate defined at invoice document level,
 * unrounded, or else the spot rate of the conversion rate table valid on the invoice date, with the
 * converted amount rounded to the standard precision of the old currency. If that rate is not
 * found, the settlement criteria is used towards the old currency.</li>
 * </ul>
 * A conversion is missing (null) when no rate is found or when the rate is zero. The results of the
 * rate lookups are cached, as they are deterministic for the same currencies, date and
 * organization.
 */
final class SettlementRateResolver implements RateResolver {

  private static final String KEY_SEPARATOR = "|";
  private static final String SPOT_RATE_TYPE = "S";
  private static final String DAY_PATTERN = "yyyyMMdd";
  private static final String SETTLEMENT_PREFIX = "S";
  private static final String COMPLETION_PREFIX = "C";
  private static final String PARAM_FROM = "fromId";
  private static final String PARAM_TO = "toId";
  private static final String PARAM_TYPE = "rateType";
  private static final String PARAM_DATE = "date";
  private static final String PARAM_CLIENT = "clientId";
  private static final String PARAM_ORG = "orgId";

  /**
   * Same criteria as C_CURRENCY_RATE: spot rate valid for the day, defined for the client (or the
   * system) and the organization (or the root), the most specific and recent one first.
   */
  private static final String COMPLETION_RATE_HQL = "select cr.multipleRateBy "
      + "from CurrencyConversionRate cr "
      + "where cr.currency.id = :" + PARAM_FROM + " and cr.toCurrency.id = :" + PARAM_TO
      + " and cr.conversionRateType = :" + PARAM_TYPE
      + " and cr.validFromDate <= :" + PARAM_DATE + " and cr.validToDate >= :" + PARAM_DATE
      + " and cr.client.id in ('0', :" + PARAM_CLIENT + ")"
      + " and cr.organization.id in ('0', :" + PARAM_ORG + ")"
      + " and cr.active = true "
      + "order by cr.client.id desc, cr.organization.id desc, cr.validFromDate desc";

  private final Currency oldCurrency;
  private final Currency newCurrency;
  private final Map<String, BigDecimal> docRates;
  private final int ratePrecision;
  private final Map<String, BigDecimal> rateCache = new HashMap<>();

  /**
   * @param oldCurrency
   *          the current currency of the business partner
   * @param newCurrency
   *          the currency the business partner is changing to
   * @param docRates
   *          the rates defined at invoice document level, by key (see
   *          {@link #docRateKey(String, String, String)})
   * @param ratePrecision
   *          the number of decimals the table rates are rounded to, as settlement does
   */
  SettlementRateResolver(Currency oldCurrency, Currency newCurrency,
      Map<String, BigDecimal> docRates, int ratePrecision) {
    this.oldCurrency = oldCurrency;
    this.newCurrency = newCurrency;
    this.docRates = docRates;
    this.ratePrecision = ratePrecision;
  }

  /**
   * Builds the key used to look up the rate defined at invoice document level.
   *
   * @param invoiceId
   *          the id of the invoice
   * @param fromId
   *          the id of the currency being converted from
   * @param toId
   *          the id of the currency being converted to
   * @return the key of the rate
   */
  static String docRateKey(String invoiceId, String fromId, String toId) {
    return invoiceId + KEY_SEPARATOR + fromId + KEY_SEPARATOR + toId;
  }

  @Override
  public BigDecimal toNew(PendingSchedule s) {
    if (s.currencyId.equals(newCurrency.getId())) {
      return s.outstanding;
    }
    final BigDecimal docRate = docRates
        .get(docRateKey(s.invoiceId, s.currencyId, newCurrency.getId()));
    if (docRate != null) {
      return convert(s.outstanding, docRate);
    }
    return convert(s.outstanding, settlementRate(s, newCurrency));
  }

  @Override
  public BigDecimal toOld(PendingSchedule s) {
    if (s.currencyId.equals(oldCurrency.getId())) {
      return s.outstanding;
    }
    final BigDecimal docRate = docRates
        .get(docRateKey(s.invoiceId, s.currencyId, oldCurrency.getId()));
    if (docRate != null) {
      return convert(s.outstanding, docRate);
    }
    final BigDecimal completionRate = completionRate(s);
    if (completionRate != null) {
      return s.outstanding.multiply(completionRate)
          .setScale(oldCurrency.getStandardPrecision().intValue(), RoundingMode.HALF_UP);
    }
    return convert(s.outstanding, settlementRate(s, oldCurrency));
  }

  @Override
  public String oldCurrencyIso() {
    return oldCurrency.getISOCode();
  }

  @Override
  public String newCurrencyIso() {
    return newCurrency.getISOCode();
  }

  /** Multiplies the amount by the rate, without rounding. A missing or zero rate gives null. */
  private static BigDecimal convert(BigDecimal amount, BigDecimal rate) {
    if (rate == null || rate.signum() == 0) {
      return null;
    }
    return amount.multiply(rate);
  }

  /**
   * Looks up the rate in the conversion rate table the same way the settlement does: the rate of
   * the invoice date and organization, rounded to the conversion rate precision.
   */
  private BigDecimal settlementRate(PendingSchedule s, Currency to) {
    final String key = cacheKey(SETTLEMENT_PREFIX, s.currencyId, to.getId(), s);
    if (!rateCache.containsKey(key)) {
      rateCache.put(key, findSettlementRate(s, to));
    }
    return rateCache.get(key);
  }

  /** Looks up the spot rate the same way it was looked up when the invoice was completed. */
  private BigDecimal completionRate(PendingSchedule s) {
    final String key = cacheKey(COMPLETION_PREFIX, s.currencyId, oldCurrency.getId(), s);
    if (!rateCache.containsKey(key)) {
      rateCache.put(key, findCompletionRate(s));
    }
    return rateCache.get(key);
  }

  private static String cacheKey(String prefix, String fromId, String toId, PendingSchedule s) {
    return prefix + KEY_SEPARATOR + fromId + KEY_SEPARATOR + toId + KEY_SEPARATOR
        + new SimpleDateFormat(DAY_PATTERN).format(s.invoiceDate) + KEY_SEPARATOR + s.orgId;
  }

  /**
   * Finds the rate of the conversion rate table with the criteria of the payment settlement.
   * Package-private so tests can replace the data access.
   *
   * @return the rate rounded to the conversion rate precision, or null if there is none
   */
  BigDecimal findSettlementRate(PendingSchedule s, Currency to) {
    final ConversionRate conversionRate = FIN_Utility.getConversionRate(
        loadCurrency(s.currencyId), to, s.invoiceDate, loadOrganization(s.orgId));
    if (conversionRate == null || conversionRate.getMultipleRateBy() == null) {
      return null;
    }
    return conversionRate.getMultipleRateBy().setScale(ratePrecision, RoundingMode.HALF_UP);
  }

  /**
   * Finds the spot rate with the criteria used when the invoice was completed. Package-private so
   * tests can replace the data access.
   *
   * @return the multiply rate, or null if there is none
   */
  BigDecimal findCompletionRate(PendingSchedule s) {
    final Date day = DateUtils.truncate(s.invoiceDate, Calendar.DATE);
    final List<BigDecimal> rates = OBDal.getInstance()
        .getSession()
        .createQuery(COMPLETION_RATE_HQL, BigDecimal.class)
        .setParameter(PARAM_FROM, s.currencyId)
        .setParameter(PARAM_TO, oldCurrency.getId())
        .setParameter(PARAM_TYPE, SPOT_RATE_TYPE)
        .setParameter(PARAM_DATE, day)
        .setParameter(PARAM_CLIENT, s.clientId)
        .setParameter(PARAM_ORG, s.orgId)
        .setMaxResults(1)
        .list();
    return rates.isEmpty() ? null : rates.get(0);
  }

  /** Loads a currency. Package-private so tests can replace the data access. */
  Currency loadCurrency(String currencyId) {
    if (currencyId.equals(oldCurrency.getId())) {
      return oldCurrency;
    }
    if (currencyId.equals(newCurrency.getId())) {
      return newCurrency;
    }
    return OBDal.getInstance().get(Currency.class, currencyId);
  }

  /** Loads an organization. Package-private so tests can replace the data access. */
  Organization loadOrganization(String orgId) {
    return OBDal.getInstance().get(Organization.class, orgId);
  }
}
