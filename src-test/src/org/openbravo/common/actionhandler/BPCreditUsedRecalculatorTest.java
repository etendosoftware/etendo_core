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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.exception.OBException;
import org.openbravo.common.actionhandler.BPCreditUsedRecalculator.MissingRate;
import org.openbravo.common.actionhandler.BPCreditUsedRecalculator.PendingSchedule;
import org.openbravo.common.actionhandler.BPCreditUsedRecalculator.RateResolver;
import org.openbravo.common.actionhandler.BPCreditUsedRecalculator.Result;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;

/**
 * Unit tests for {@link BPCreditUsedRecalculator}.
 */
@SuppressWarnings({"java:S101", "java:S1192"})
@DisplayName("BPCreditUsedRecalculator")
public class BPCreditUsedRecalculatorTest {

  private static final String USD = "USD";
  private static final String ARS = "ARS";
  private static final String EUR = "EUR";
  private static final String USD_ID = "100";
  private static final String ARS_ID = "118";
  private static final String DATE_PATTERN = "dd-MM-yyyy";
  private static final String NO_CONVERSION_PREFIX = "No rate for the pending invoices: ";
  private static final Map<String, String> TEMPLATES = new HashMap<>();
  static {
    TEMPLATES.put(BPCreditUsedRecalculator.MSG_NO_RATE, NO_CONVERSION_PREFIX + "%s");
    TEMPLATES.put(BPCreditUsedRecalculator.MSG_NO_RATE_ITEM,
        "Invoice %1$s (%2$s to %3$s, date %4$s)");
    TEMPLATES.put(BPCreditUsedRecalculator.MSG_NO_RATE_MORE, "and %s more");
  }
  private static final Function<String, String> MESSAGES = TEMPLATES::get;
  private static final BigDecimal RATE_1500 = new BigDecimal("1500");
  private static final BigDecimal RATE_1523 = new BigDecimal("1523.4567");

  private static Date date(int year, int month, int day) {
    final Calendar cal = Calendar.getInstance();
    cal.clear();
    cal.set(year, month - 1, day);
    return cal.getTime();
  }

  private static PendingSchedule schedule(String documentNo, boolean sales, String outstanding) {
    return new PendingSchedule("inv-" + documentNo, documentNo, USD_ID, USD,
        date(2026, 11, 15), "org", "client", sales, new BigDecimal(outstanding));
  }

  /**
   * Resolver that returns the amounts configured by document number. A document without a
   * configured amount has no usable rate.
   */
  private static final class StubResolver implements RateResolver {
    private final Map<String, BigDecimal> toNew = new HashMap<>();
    private final Map<String, BigDecimal> toOld = new HashMap<>();

    StubResolver put(String documentNo, String newAmount, String oldAmount) {
      if (newAmount != null) {
        toNew.put(documentNo, new BigDecimal(newAmount));
      }
      if (oldAmount != null) {
        toOld.put(documentNo, new BigDecimal(oldAmount));
      }
      return this;
    }

    @Override
    public BigDecimal toNew(PendingSchedule s) {
      return toNew.get(s.documentNo);
    }

    @Override
    public BigDecimal toOld(PendingSchedule s) {
      return toOld.get(s.documentNo);
    }

    @Override
    public String oldCurrencyIso() {
      return USD;
    }

    @Override
    public String newCurrencyIso() {
      return ARS;
    }
  }

  // ── compute ──────────────────────────────────────────────────────────

  @Nested
  @DisplayName("compute")
  class Compute {

    @Test
    @DisplayName("ticket case yields the unrounded product of the invoice")
    void ticketCaseYieldsUnroundedProduct() {
      StubResolver resolver = new StubResolver().put("1000121", "184338.2607", "121");

      Result result = BPCreditUsedRecalculator.compute(new BigDecimal("121"), RATE_1500,
          Collections.singletonList(schedule("1000121", true, "121")), resolver);

      assertTrue(result.missing.isEmpty());
      assertEquals(new BigDecimal("184338.2607"), result.value);
    }

    @Test
    @DisplayName("the remainder not explained by invoices is converted with the chosen rate")
    void remainderConvertedWithChosenRate() {
      StubResolver resolver = new StubResolver().put("1000121", "184338.2607", "121");

      Result result = BPCreditUsedRecalculator.compute(new BigDecimal("131"), RATE_1500,
          Collections.singletonList(schedule("1000121", true, "121")), resolver);

      assertEquals(0, new BigDecimal("199338.2607").compareTo(result.value));
    }

    @Test
    @DisplayName("purchase invoices subtract from the credit used")
    void purchaseInvoiceSubtracts() {
      StubResolver resolver = new StubResolver().put("2000001", "75000", "50");

      Result result = BPCreditUsedRecalculator.compute(new BigDecimal("-50"), RATE_1500,
          Collections.singletonList(schedule("2000001", false, "50")), resolver);

      assertEquals(0, new BigDecimal("-75000").compareTo(result.value));
    }

    @Test
    @DisplayName("sales and purchase invoices are combined with their sign")
    void salesAndPurchaseCombined() {
      StubResolver resolver = new StubResolver().put("1000001", "150000", "100")
          .put("2000001", "75000", "50");

      Result result = BPCreditUsedRecalculator.compute(new BigDecimal("50"), RATE_1500,
          Arrays.asList(schedule("1000001", true, "100"), schedule("2000001", false, "50")),
          resolver);

      assertEquals(0, new BigDecimal("75000").compareTo(result.value));
    }

    @Test
    @DisplayName("an invoice already in the new currency counts with factor one")
    void invoiceInNewCurrencyFactorOne() {
      PendingSchedule inNewCurrency = new PendingSchedule("inv-1", "1000200", ARS_ID, ARS,
          date(2026, 11, 15), "org", "client", true, new BigDecimal("1500"));
      StubResolver resolver = new StubResolver().put("1000200", "1500", "1");

      Result result = BPCreditUsedRecalculator.compute(new BigDecimal("1"), RATE_1500,
          Collections.singletonList(inNewCurrency), resolver);

      assertEquals(0, new BigDecimal("1500").compareTo(result.value));
    }

    @Test
    @DisplayName("a null credit used is treated as zero")
    void nullOldTreatedAsZero() {
      StubResolver resolver = new StubResolver().put("1000121", "10", "5");

      Result result = BPCreditUsedRecalculator.compute(null, new BigDecimal("2"),
          Collections.singletonList(schedule("1000121", true, "5")), resolver);

      assertEquals(0, BigDecimal.ZERO.compareTo(result.value));
    }

    @Test
    @DisplayName("without pending invoices the whole balance is converted with the chosen rate")
    void noPendingInvoicesConvertsWholeBalance() {
      Result result = BPCreditUsedRecalculator.compute(new BigDecimal("100"), RATE_1500,
          Collections.<PendingSchedule> emptyList(), new StubResolver());

      assertEquals(0, new BigDecimal("150000").compareTo(result.value));
    }

    @Test
    @DisplayName("a missing rate to the new currency is collected")
    void missingNewRateCollected() {
      StubResolver resolver = new StubResolver().put("1000121", null, "121");

      Result result = BPCreditUsedRecalculator.compute(new BigDecimal("121"), RATE_1500,
          Collections.singletonList(schedule("1000121", true, "121")), resolver);

      assertNull(result.value);
      assertEquals(1, result.missing.size());
      assertEquals("1000121", result.missing.get(0).documentNo);
      assertEquals(USD, result.missing.get(0).fromIso);
      assertEquals(ARS, result.missing.get(0).toIso);
    }

    @Test
    @DisplayName("a missing rate to the old currency is collected")
    void missingOldRateCollected() {
      StubResolver resolver = new StubResolver().put("1000121", "184338.2607", null);

      Result result = BPCreditUsedRecalculator.compute(new BigDecimal("121"), RATE_1500,
          Collections.singletonList(schedule("1000121", true, "121")), resolver);

      assertNull(result.value);
      assertEquals(1, result.missing.size());
      assertEquals(USD, result.missing.get(0).fromIso);
      assertEquals(USD, result.missing.get(0).toIso);
    }

    @Test
    @DisplayName("an invoice with both rates missing is never partially counted")
    void bothMissingNeverPartiallyCounted() {
      StubResolver resolver = new StubResolver();

      Result result = BPCreditUsedRecalculator.compute(new BigDecimal("121"), RATE_1500,
          Collections.singletonList(schedule("1000121", true, "121")), resolver);

      assertNull(result.value);
      assertEquals(2, result.missing.size());
    }

    @Test
    @DisplayName("the total is not rounded")
    void noRoundingOfTotal() {
      StubResolver resolver = new StubResolver().put("1000121", "0.333333333333", "1")
          .put("1000122", "0.666666666667", "2");

      Result result = BPCreditUsedRecalculator.compute(new BigDecimal("3.1"), RATE_1523,
          Arrays.asList(schedule("1000121", true, "1"), schedule("1000122", true, "2")),
          resolver);

      assertEquals(new BigDecimal("153.345670000000"), result.value);
    }
  }

  // ── buildMissingRateMessage ──────────────────────────────────────────

  @Nested
  @DisplayName("buildMissingRateMessage")
  class BuildMissingRateMessage {

    private MockedStatic<OBDateUtils> dateUtils;

    @BeforeEach
    void setUp() {
      dateUtils = mockStatic(OBDateUtils.class);
      dateUtils.when(() -> OBDateUtils.formatDate(any(Date.class)))
          .thenAnswer(invocation -> new SimpleDateFormat(DATE_PATTERN)
              .format((Date) invocation.getArgument(0)));
    }

    @AfterEach
    void tearDown() {
      dateUtils.close();
    }

    @Test
    @DisplayName("contains the invoices, the currencies and the dates")
    void messageContainsExpectedData() {
      List<MissingRate> missing = Arrays.asList(
          new MissingRate("1000121", USD, ARS, date(2026, 11, 15)),
          new MissingRate("1000130", EUR, USD, date(2026, 11, 12)));

      String message = BPCreditUsedRecalculator.buildMissingRateMessage(missing, 10, MESSAGES);

      assertEquals(NO_CONVERSION_PREFIX + "Invoice 1000121 (USD to ARS, date 15-11-2026); "
          + "Invoice 1000130 (EUR to USD, date 12-11-2026)", message);
    }

    @Test
    @DisplayName("a translation can reorder the data of every sentence")
    void translationCanReorderData() {
      Map<String, String> spanish = new HashMap<>();
      spanish.put(BPCreditUsedRecalculator.MSG_NO_RATE, "Sin tasa en: %s.");
      spanish.put(BPCreditUsedRecalculator.MSG_NO_RATE_ITEM,
          "el %4$s, de %2$s a %3$s, factura %1$s");
      spanish.put(BPCreditUsedRecalculator.MSG_NO_RATE_MORE, "%s más");
      List<MissingRate> missing = Collections
          .singletonList(new MissingRate("1000121", USD, ARS, date(2026, 11, 15)));

      String message = BPCreditUsedRecalculator.buildMissingRateMessage(missing, 10,
          spanish::get);

      assertEquals("Sin tasa en: el 15-11-2026, de USD a ARS, factura 1000121.", message);
    }

    @Test
    @DisplayName("lists at most the cap and counts the rest")
    void messageCapsAtTenAndCountsRest() {
      List<MissingRate> missing = new ArrayList<>();
      for (int i = 1; i <= 13; i++) {
        missing.add(new MissingRate("DOC" + i, USD, ARS, date(2026, 11, 15)));
      }

      String message = BPCreditUsedRecalculator.buildMissingRateMessage(missing, 10, MESSAGES);

      assertTrue(message.contains("Invoice DOC10 ("));
      assertFalse(message.contains("DOC11"));
      assertTrue(message.endsWith("; and 3 more"));
    }

    @Test
    @DisplayName("has no suffix when the cap is not exceeded")
    void messageHasNoSuffixWithinCap() {
      List<MissingRate> missing = Collections
          .singletonList(new MissingRate("DOC1", USD, ARS, date(2026, 11, 15)));

      String message = BPCreditUsedRecalculator.buildMissingRateMessage(missing, 10, MESSAGES);

      assertFalse(message.contains("more"));
    }

    @Test
    @DisplayName("removes the at sign of the document numbers")
    void messageStripsAtSign() {
      List<MissingRate> missing = Collections
          .singletonList(new MissingRate("10@0@", USD, ARS, date(2026, 11, 15)));

      String message = BPCreditUsedRecalculator.buildMissingRateMessage(missing, 10, MESSAGES);

      assertTrue(message.contains("Invoice 100 ("));
    }

    @Test
    @DisplayName("an invoice and direction found twice is listed once")
    void bothMissingSameInvoiceListedOnce() {
      List<MissingRate> missing = Arrays.asList(
          new MissingRate("1000121", USD, ARS, date(2026, 11, 15)),
          new MissingRate("1000121", USD, ARS, date(2026, 11, 15)));

      String message = BPCreditUsedRecalculator.buildMissingRateMessage(missing, 10, MESSAGES);

      assertEquals(NO_CONVERSION_PREFIX + "Invoice 1000121 (USD to ARS, date 15-11-2026)",
          message);
    }

    @Test
    @DisplayName("both directions of the same invoice are listed separately")
    void bothDirectionsListedSeparately() {
      List<MissingRate> missing = Arrays.asList(
          new MissingRate("1000121", EUR, ARS, date(2026, 11, 15)),
          new MissingRate("1000121", EUR, USD, date(2026, 11, 15)));

      String message = BPCreditUsedRecalculator.buildMissingRateMessage(missing, 10, MESSAGES);

      assertTrue(message.contains("(EUR to ARS,"));
      assertTrue(message.contains("(EUR to USD,"));
    }
  }

  // ── recalculate ──────────────────────────────────────────────────────

  @Nested
  @DisplayName("recalculate")
  class Recalculate {

    private final BusinessPartner bp = mock(BusinessPartner.class);

    @Test
    @DisplayName("returns null when the amount is set manually")
    void recalculateReturnsNullWhenSetAmount() {
      assertNull(BPCreditUsedRecalculator.recalculate(bp, BigDecimal.TEN, USD_ID, ARS_ID,
          RATE_1500, true));
    }

    @Test
    @DisplayName("returns null when both currencies are the same")
    void recalculateReturnsNullWhenSameCurrency() {
      assertNull(BPCreditUsedRecalculator.recalculate(bp, BigDecimal.TEN, USD_ID, USD_ID,
          RATE_1500, false));
    }

    @Test
    @DisplayName("returns null when there is no new currency")
    void recalculateReturnsNullWhenNullOrStringNullToCurrency() {
      assertNull(BPCreditUsedRecalculator.recalculate(bp, BigDecimal.TEN, USD_ID, null,
          RATE_1500, false));
      assertNull(BPCreditUsedRecalculator.recalculate(bp, BigDecimal.TEN, USD_ID, "null",
          RATE_1500, false));
      assertNull(BPCreditUsedRecalculator.recalculate(bp, BigDecimal.TEN, USD_ID, "",
          RATE_1500, false));
    }

    @Test
    @DisplayName("returns null when there is no old currency")
    void recalculateReturnsNullWhenNullOrStringNullFromCurrency() {
      assertNull(BPCreditUsedRecalculator.recalculate(bp, BigDecimal.TEN, null, ARS_ID,
          RATE_1500, false));
      assertNull(BPCreditUsedRecalculator.recalculate(bp, BigDecimal.TEN, "null", ARS_ID,
          RATE_1500, false));
      assertNull(BPCreditUsedRecalculator.recalculate(bp, BigDecimal.TEN, "", ARS_ID,
          RATE_1500, false));
    }

    @Test
    @DisplayName("returns null when the rate is null or zero")
    void recalculateReturnsNullWhenZeroRate() {
      assertNull(BPCreditUsedRecalculator.recalculate(bp, BigDecimal.TEN, USD_ID, ARS_ID,
          BigDecimal.ZERO, false));
      assertNull(BPCreditUsedRecalculator.recalculate(bp, BigDecimal.TEN, USD_ID, ARS_ID,
          new BigDecimal("0.000"), false));
      assertNull(BPCreditUsedRecalculator.recalculate(bp, BigDecimal.TEN, USD_ID, ARS_ID, null,
          false));
    }

    @Test
    @DisplayName("returns the computed value when every invoice can be converted")
    void recalculateWithReturnsValue() {
      StubResolver resolver = new StubResolver().put("1000121", "184338.2607", "121");

      BigDecimal value = BPCreditUsedRecalculator.recalculateWith(new BigDecimal("121"),
          RATE_1500, Collections.singletonList(schedule("1000121", true, "121")), resolver,
          MESSAGES);

      assertEquals(new BigDecimal("184338.2607"), value);
    }

    @Test
    @DisplayName("throws an OBException naming the invoice when it cannot be converted")
    void recalculateThrowsOBExceptionNamingTheInvoice() {
      StubResolver resolver = new StubResolver().put("1000121", null, "121");
      List<PendingSchedule> rows = Collections
          .singletonList(schedule("1000121", true, "121"));

      try (MockedStatic<OBDateUtils> dateUtils = mockStatic(OBDateUtils.class)) {
        dateUtils.when(() -> OBDateUtils.formatDate(any(Date.class)))
            .thenAnswer(invocation -> new SimpleDateFormat(DATE_PATTERN)
                .format((Date) invocation.getArgument(0)));

        OBException exception = assertThrows(OBException.class,
            () -> BPCreditUsedRecalculator.recalculateWith(new BigDecimal("121"), RATE_1500,
                rows, resolver, MESSAGES));

        assertEquals(NO_CONVERSION_PREFIX + "Invoice 1000121 (USD to ARS, date 15-11-2026)",
            exception.getMessage());
      }
    }
  }
}
