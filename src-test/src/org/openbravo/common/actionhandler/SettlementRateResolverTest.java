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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.common.actionhandler.BPCreditUsedRecalculator.PendingSchedule;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Unit tests for {@link SettlementRateResolver}. The access to the database is replaced: the
 * conversion rate table lookup of the settlement is mocked through {@link FIN_Utility} and the
 * lookups of the resolver itself are stubbed through its package-private seams.
 */
@SuppressWarnings({"java:S101", "java:S1192"})
@DisplayName("SettlementRateResolver")
public class SettlementRateResolverTest {

  private static final String OLD_ID = "100";
  private static final String NEW_ID = "118";
  private static final String OTHER_ID = "102";
  private static final String INVOICE_ID = "inv-1";
  private static final int RATE_PRECISION = 6;

  private MockedStatic<FIN_Utility> finUtility;
  private Currency oldCurrency;
  private Currency newCurrency;
  private Currency otherCurrency;

  @BeforeEach
  void setUp() {
    finUtility = mockStatic(FIN_Utility.class);
    oldCurrency = currency(OLD_ID, "USD", 2L);
    newCurrency = currency(NEW_ID, "ARS", 2L);
    otherCurrency = currency(OTHER_ID, "EUR", 2L);
  }

  @AfterEach
  void tearDown() {
    finUtility.close();
  }

  private static Currency currency(String id, String iso, long precision) {
    final Currency currency = mock(Currency.class);
    when(currency.getId()).thenReturn(id);
    when(currency.getISOCode()).thenReturn(iso);
    when(currency.getStandardPrecision()).thenReturn(precision);
    return currency;
  }

  private static Date date(int year, int month, int day) {
    final Calendar cal = Calendar.getInstance();
    cal.clear();
    cal.set(year, month - 1, day);
    return cal.getTime();
  }

  private static PendingSchedule schedule(String currencyId, Date invoiceDate, String outstanding) {
    return new PendingSchedule(INVOICE_ID, "1000121", currencyId, "XXX", invoiceDate, "org",
        "client", true, new BigDecimal(outstanding));
  }

  private static String docKey(String fromId, String toId) {
    return SettlementRateResolver.docRateKey(INVOICE_ID, fromId, toId);
  }

  /** Creates a resolver whose entity loading does not need a database. */
  private SettlementRateResolver resolver(Map<String, BigDecimal> docRates) {
    final SettlementRateResolver resolver = spy(
        new SettlementRateResolver(oldCurrency, newCurrency, docRates, RATE_PRECISION));
    final Map<String, Currency> currencies = new HashMap<>();
    currencies.put(OLD_ID, oldCurrency);
    currencies.put(NEW_ID, newCurrency);
    currencies.put(OTHER_ID, otherCurrency);
    doAnswer(invocation -> currencies.get(invocation.<String> getArgument(0))).when(resolver)
        .loadCurrency(anyString());
    doReturn(mock(Organization.class)).when(resolver).loadOrganization(anyString());
    return resolver;
  }

  private void tableRate(String multipleRateBy) {
    final ConversionRate rate = mock(ConversionRate.class);
    when(rate.getMultipleRateBy()).thenReturn(new BigDecimal(multipleRateBy));
    finUtility.when(() -> FIN_Utility.getConversionRate(any(Currency.class),
        any(Currency.class), any(Date.class), any(Organization.class))).thenReturn(rate);
  }

  private void noTableRate() {
    finUtility.when(() -> FIN_Utility.getConversionRate(any(Currency.class),
        any(Currency.class), any(Date.class), any(Organization.class))).thenReturn(null);
  }

  // ── to the new currency ──────────────────────────────────────────────

  @Test
  @DisplayName("the invoice rate to the new currency is preferred and not rounded")
  void docRateToNewPreferredAndUnrounded() {
    Map<String, BigDecimal> docRates = new HashMap<>();
    docRates.put(docKey(OLD_ID, NEW_ID), new BigDecimal("1523.45678951"));
    tableRate("1");

    BigDecimal converted = resolver(docRates)
        .toNew(schedule(OLD_ID, date(2026, 11, 15), "121"));

    assertEquals(new BigDecimal("121").multiply(new BigDecimal("1523.45678951")), converted);
    finUtility.verify(() -> FIN_Utility.getConversionRate(any(Currency.class),
        any(Currency.class), any(Date.class), any(Organization.class)), never());
  }

  @Test
  @DisplayName("the invoice rate to the old currency is ignored for the new currency")
  void docRateToOldCurrencyIgnoredForNewPart() {
    Map<String, BigDecimal> docRates = new HashMap<>();
    docRates.put(docKey(OTHER_ID, OLD_ID), new BigDecimal("1.1"));
    tableRate("1000.5");

    BigDecimal converted = resolver(docRates)
        .toNew(schedule(OTHER_ID, date(2026, 11, 15), "10"));

    assertEquals(new BigDecimal("10").multiply(new BigDecimal("1000.500000")), converted);
  }

  @Test
  @DisplayName("the table rate is rounded half up to the conversion rate precision")
  void tableRateRoundedHalfUpToPrecision() {
    tableRate("1523.45678951");

    BigDecimal converted = resolver(new HashMap<>())
        .toNew(schedule(OLD_ID, date(2026, 11, 15), "100"));

    assertEquals(new BigDecimal("152345.679000"), converted);
  }

  @Test
  @DisplayName("a table rate that is not found is missing")
  void nullTableRateIsMissing() {
    noTableRate();

    assertNull(resolver(new HashMap<>()).toNew(schedule(OLD_ID, date(2026, 11, 15), "100")));
  }

  @Test
  @DisplayName("a zero rate is missing, in the table and in the invoice")
  void zeroRateIsMissing() {
    tableRate("0.0000001");
    assertNull(resolver(new HashMap<>()).toNew(schedule(OLD_ID, date(2026, 11, 15), "100")));

    Map<String, BigDecimal> docRates = new HashMap<>();
    docRates.put(docKey(OLD_ID, NEW_ID), BigDecimal.ZERO);
    assertNull(resolver(docRates).toNew(schedule(OLD_ID, date(2026, 11, 15), "100")));
  }

  @Test
  @DisplayName("an invoice already in the converted currency is returned unchanged")
  void sameCurrencyReturnsOutstandingUnchanged() {
    final SettlementRateResolver resolver = resolver(new HashMap<>());
    PendingSchedule inNew = schedule(NEW_ID, date(2026, 11, 15), "1500");
    PendingSchedule inOld = schedule(OLD_ID, date(2026, 11, 15), "121");

    assertSame(inNew.outstanding, resolver.toNew(inNew));
    assertSame(inOld.outstanding, resolver.toOld(inOld));
  }

  // ── to the old currency ──────────────────────────────────────────────

  @Test
  @DisplayName("the invoice rate to the old currency is preferred and not rounded")
  void docRateToOldPreferredAndUnrounded() {
    Map<String, BigDecimal> docRates = new HashMap<>();
    docRates.put(docKey(OTHER_ID, OLD_ID), new BigDecimal("1.123456789"));
    final SettlementRateResolver resolver = resolver(docRates);

    BigDecimal converted = resolver.toOld(schedule(OTHER_ID, date(2026, 11, 15), "10"));

    assertEquals(new BigDecimal("11.234567890"), converted);
    verify(resolver, never()).findCompletionRate(any(PendingSchedule.class));
  }

  @Test
  @DisplayName("the old part is rounded to the standard precision of the old currency")
  void oldPartRoundsToStdPrecision() {
    final SettlementRateResolver resolver = resolver(new HashMap<>());
    doReturn(new BigDecimal("1.23456")).when(resolver)
        .findCompletionRate(any(PendingSchedule.class));

    BigDecimal converted = resolver.toOld(schedule(OTHER_ID, date(2026, 11, 15), "10"));

    assertEquals(new BigDecimal("12.35"), converted);
  }

  @Test
  @DisplayName("without a completion rate the old part falls back to the settlement criterion")
  void oldPartFallsBackToSettlementCriterion() {
    final SettlementRateResolver resolver = resolver(new HashMap<>());
    doReturn(null).when(resolver).findCompletionRate(any(PendingSchedule.class));
    tableRate("0.9123456789");

    BigDecimal converted = resolver.toOld(schedule(OTHER_ID, date(2026, 11, 15), "10"));

    assertEquals(new BigDecimal("10").multiply(new BigDecimal("0.912346")), converted);
  }

  @Test
  @DisplayName("the old part is missing when neither criterion finds a rate")
  void oldPartMissingWhenBothFail() {
    final SettlementRateResolver resolver = resolver(new HashMap<>());
    doReturn(null).when(resolver).findCompletionRate(any(PendingSchedule.class));
    noTableRate();

    assertNull(resolver.toOld(schedule(OTHER_ID, date(2026, 11, 15), "10")));
  }

  // ── cache ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("the same currencies, day and organization are looked up only once")
  void cacheHitsSameKeyOnce() {
    tableRate("1500");
    final SettlementRateResolver resolver = resolver(new HashMap<>());

    BigDecimal first = resolver.toNew(schedule(OLD_ID, date(2026, 11, 15), "10"));
    BigDecimal second = resolver.toNew(schedule(OLD_ID, date(2026, 11, 15), "20"));

    assertEquals(new BigDecimal("15000.000000"), first);
    assertEquals(new BigDecimal("30000.000000"), second);
    finUtility.verify(() -> FIN_Utility.getConversionRate(any(Currency.class),
        any(Currency.class), any(Date.class), any(Organization.class)), times(1));
  }

  @Test
  @DisplayName("a different day is looked up again")
  void cacheMissesOtherDay() {
    tableRate("1500");
    final SettlementRateResolver resolver = resolver(new HashMap<>());

    resolver.toNew(schedule(OLD_ID, date(2026, 11, 15), "10"));
    resolver.toNew(schedule(OLD_ID, date(2026, 11, 16), "10"));

    finUtility.verify(() -> FIN_Utility.getConversionRate(any(Currency.class),
        any(Currency.class), any(Date.class), any(Organization.class)), times(2));
  }

  @Test
  @DisplayName("a missing rate is also cached")
  void missingRateIsCached() {
    noTableRate();
    final SettlementRateResolver resolver = resolver(new HashMap<>());

    assertNull(resolver.toNew(schedule(OLD_ID, date(2026, 11, 15), "10")));
    assertNull(resolver.toNew(schedule(OLD_ID, date(2026, 11, 15), "20")));

    finUtility.verify(() -> FIN_Utility.getConversionRate(any(Currency.class),
        any(Currency.class), any(Date.class), any(Organization.class)), times(1));
  }

  @Test
  @DisplayName("the currencies ISO codes are reported")
  void reportsIsoCodes() {
    final SettlementRateResolver resolver = resolver(new HashMap<>());

    assertEquals("USD", resolver.oldCurrencyIso());
    assertEquals("ARS", resolver.newCurrencyIso());
  }
}
