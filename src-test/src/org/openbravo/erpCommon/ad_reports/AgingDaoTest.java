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
package org.openbravo.erpCommon.ad_reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AgingDao}.
 * <p>
 * The public methods of AgingDao depend heavily on the database layer, so this
 * test class focuses on the private helper methods accessed via reflection.
 */
@DisplayName("AgingDao Tests")
public class AgingDaoTest {

  private AgingDao agingDao;

  @BeforeEach
  void setUp() {
    agingDao = new AgingDao();
  }

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("Should create a non-null AgingDao instance")
    void shouldCreateInstance() {
      assertNotNull(agingDao);
    }
  }

  @Nested
  @DisplayName("convertToDate")
  class ConvertToDateTests {

    private Method convertToDateMethod;

    @BeforeEach
    void setUp() throws Exception {
      convertToDateMethod = AgingDao.class.getDeclaredMethod("convertToDate", Date.class,
          String.class);
      convertToDateMethod.setAccessible(true);
    }

    @Test
    @DisplayName("Should subtract 30 days from the given date")
    void shouldSubtract30Days() throws Exception {
      Date baseDate = createDate(2025, Calendar.JUNE, 15);

      Date result = (Date) convertToDateMethod.invoke(agingDao, baseDate, "30");

      Date expected = createDate(2025, Calendar.MAY, 16);
      assertSameDay(expected, result);
    }

    @Test
    @DisplayName("Should return same day when column is 0")
    void shouldReturnSameDayWhenZero() throws Exception {
      Date baseDate = createDate(2025, Calendar.MARCH, 10);

      Date result = (Date) convertToDateMethod.invoke(agingDao, baseDate, "0");

      assertEquals(baseDate, result);
    }

    @Test
    @DisplayName("Should subtract 1 day from the given date")
    void shouldSubtract1Day() throws Exception {
      Date baseDate = createDate(2025, Calendar.JANUARY, 1);

      Date result = (Date) convertToDateMethod.invoke(agingDao, baseDate, "1");

      Date expected = createDate(2024, Calendar.DECEMBER, 31);
      assertSameDay(expected, result);
    }

    @Test
    @DisplayName("Should handle leap year date correctly")
    void shouldHandleLeapYear() throws Exception {
      Date baseDate = createDate(2024, Calendar.MARCH, 1);

      Date result = (Date) convertToDateMethod.invoke(agingDao, baseDate, "1");

      Date expected = createDate(2024, Calendar.FEBRUARY, 29);
      assertSameDay(expected, result);
    }

    @Test
    @DisplayName("Should subtract 365 days from the given date")
    void shouldSubtract365Days() throws Exception {
      Date baseDate = createDate(2026, Calendar.JUNE, 15);

      Date result = (Date) convertToDateMethod.invoke(agingDao, baseDate, "365");

      Date expected = createDate(2025, Calendar.JUNE, 15);
      assertSameDay(expected, result);
    }

    @Test
    @DisplayName("Should cross month boundary correctly")
    void shouldCrossMonthBoundary() throws Exception {
      Date baseDate = createDate(2026, Calendar.MARCH, 5);

      Date result = (Date) convertToDateMethod.invoke(agingDao, baseDate, "10");

      Date expected = createDate(2026, Calendar.FEBRUARY, 23);
      assertSameDay(expected, result);
    }

    private void assertSameDay(Date expected, Date actual) {
      Calendar calExpected = new GregorianCalendar();
      calExpected.setTime(expected);
      Calendar calActual = new GregorianCalendar();
      calActual.setTime(actual);

      assertEquals(calExpected.get(Calendar.YEAR), calActual.get(Calendar.YEAR),
          "Year should match");
      assertEquals(calExpected.get(Calendar.MONTH), calActual.get(Calendar.MONTH),
          "Month should match");
      assertEquals(calExpected.get(Calendar.DAY_OF_MONTH), calActual.get(Calendar.DAY_OF_MONTH),
          "Day should match");
    }
  }

  @Nested
  @DisplayName("calculatePercentage")
  class CalculatePercentageTests {

    private Method calculatePercentageMethod;

    @BeforeEach
    void setUp() throws Exception {
      calculatePercentageMethod = AgingDao.class.getDeclaredMethod("calculatePercentage",
          BigDecimal.class, BigDecimal.class);
      calculatePercentageMethod.setAccessible(true);
    }

    @Test
    @DisplayName("Should return ZERO when doubtfulDebtAmount is zero")
    void shouldReturnZeroWhenDoubtfulDebtIsZero() throws Exception {
      BigDecimal totalAmount = new BigDecimal("100");
      BigDecimal doubtfulDebt = BigDecimal.ZERO;

      BigDecimal result = (BigDecimal) calculatePercentageMethod.invoke(agingDao, totalAmount,
          doubtfulDebt);

      assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Should return 50.00000 when totalAmount=100 and doubtfulDebt=50")
    void shouldReturn50PercentFor100And50() throws Exception {
      BigDecimal totalAmount = new BigDecimal("100");
      BigDecimal doubtfulDebt = new BigDecimal("50");

      BigDecimal result = (BigDecimal) calculatePercentageMethod.invoke(agingDao, totalAmount,
          doubtfulDebt);

      assertEquals(0, new BigDecimal("50.00000").compareTo(result));
    }

    @Test
    @DisplayName("Should return 50.00000 when totalAmount=200 and doubtfulDebt=100")
    void shouldReturn50PercentFor200And100() throws Exception {
      BigDecimal totalAmount = new BigDecimal("200");
      BigDecimal doubtfulDebt = new BigDecimal("100");

      BigDecimal result = (BigDecimal) calculatePercentageMethod.invoke(agingDao, totalAmount,
          doubtfulDebt);

      assertEquals(0, new BigDecimal("50.00000").compareTo(result));
    }

    @Test
    @DisplayName("Should return ~33.33333 when totalAmount=300 and doubtfulDebt=100")
    void shouldReturnApprox33PercentFor300And100() throws Exception {
      BigDecimal totalAmount = new BigDecimal("300");
      BigDecimal doubtfulDebt = new BigDecimal("100");

      BigDecimal result = (BigDecimal) calculatePercentageMethod.invoke(agingDao, totalAmount,
          doubtfulDebt);

      assertEquals(0, new BigDecimal("33.33333").compareTo(result));
    }

    @Test
    @DisplayName("Should return ZERO when both totalAmount and doubtfulDebt are zero")
    void shouldReturnZeroWhenBothAreZero() throws Exception {
      BigDecimal totalAmount = BigDecimal.ZERO;
      BigDecimal doubtfulDebt = BigDecimal.ZERO;

      BigDecimal result = (BigDecimal) calculatePercentageMethod.invoke(agingDao, totalAmount,
          doubtfulDebt);

      assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Should return 100.00000 when totalAmount equals doubtfulDebt")
    void shouldReturn100WhenTotalEqualsDoubtful() throws Exception {
      BigDecimal totalAmount = new BigDecimal("500");
      BigDecimal doubtfulDebt = new BigDecimal("500");

      BigDecimal result = (BigDecimal) calculatePercentageMethod.invoke(agingDao, totalAmount,
          doubtfulDebt);

      assertEquals(0, new BigDecimal("100.00000").compareTo(result));
    }

    @Test
    @DisplayName("Should return a small fraction for a very small doubtfulDebt relative to total")
    void shouldReturnSmallFraction() throws Exception {
      BigDecimal totalAmount = new BigDecimal("10000");
      BigDecimal doubtfulDebt = new BigDecimal("1");

      BigDecimal result = (BigDecimal) calculatePercentageMethod.invoke(agingDao, totalAmount,
          doubtfulDebt);

      assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
      assertTrue(result.compareTo(new BigDecimal("1")) < 0);
    }
  }

  @Nested
  @DisplayName("insertData")
  class InsertDataTests {

    private static final String DD_MM_YYYY = "dd-MM-yyyy";
    private static final String NETDUE = "NETDUE";

    private Method insertDataMethod;
    private SimpleDateFormat dateFormat;

    @BeforeEach
    void setUp() throws Exception {
      insertDataMethod = AgingDao.class.getDeclaredMethod("insertData", String.class, String.class,
          Date.class, BigDecimal.class, String.class, String.class, int.class, String.class,
          SimpleDateFormat.class, boolean.class, BigDecimal.class);
      insertDataMethod.setAccessible(true);
      dateFormat = new SimpleDateFormat(DD_MM_YYYY);
    }

    @SuppressWarnings("unchecked")
    private HashMap<String, String> invokeInsertData(String documentNo, String id, Date date,
        BigDecimal amount, String bpartnerId, String bpartnerName, int group, String tabId,
        boolean credits, BigDecimal doubtfulDebt) throws Exception {
      return (HashMap<String, String>) insertDataMethod.invoke(agingDao, documentNo, id, date,
          amount, bpartnerId, bpartnerName, group, tabId, dateFormat, credits, doubtfulDebt);
    }

    @Test
    @DisplayName("Should contain all required keys for non-credit invoice")
    void shouldContainAllKeysForNonCredit() throws Exception {
      Date invoiceDate = createDate(2026, Calendar.JANUARY, 15);
      BigDecimal amount = new BigDecimal("100.50");

      HashMap<String, String> result = invokeInsertData("INV001", "id123", invoiceDate, amount,
          "bp001", "Test Partner", 1, "263", false, BigDecimal.ZERO);

      assertNotNull(result);
      assertEquals("INV001", result.get("INVOICE_NUMBER"));
      assertEquals("id123", result.get("INVOICE_ID"));
      assertEquals("15-01-2026", result.get("INVOICE_DATE"));
      assertEquals("bp001", result.get("BPARTNER"));
      assertEquals("Test Partner", result.get("BPARTNERNAME"));
      assertEquals("263", result.get("TABID"));
      assertNotNull(result.get(NETDUE));
      assertNotNull(result.get("SHOW_NETDUE"));
    }

    @Test
    @DisplayName("Should set AMOUNTn to null when amount is ZERO")
    void shouldSetAmountToNullWhenZero() throws Exception {
      Date invoiceDate = createDate(2026, Calendar.JANUARY, 15);

      HashMap<String, String> result = invokeInsertData("INV002", "id456", invoiceDate,
          BigDecimal.ZERO, "bp002", "Partner 2", 1, "263", false, BigDecimal.ZERO);

      assertNull(result.get("AMOUNT1"), "AMOUNT1 should be null when amount is ZERO");
    }

    @Test
    @DisplayName("Should set AMOUNTn to amount string when amount is non-zero")
    void shouldSetAmountWhenNonZero() throws Exception {
      Date invoiceDate = createDate(2026, Calendar.JANUARY, 15);
      BigDecimal amount = new BigDecimal("500");

      HashMap<String, String> result = invokeInsertData("INV003", "id789", invoiceDate, amount,
          "bp003", "Partner 3", 3, "263", false, BigDecimal.ZERO);

      assertEquals("500", result.get("AMOUNT3"));
      assertNull(result.get("AMOUNT1"));
    }

    @Test
    @DisplayName("Should not contain NETDUE key when credits is true")
    void shouldNotContainNetdueWhenCreditsTrue() throws Exception {
      Date paymentDate = createDate(2026, Calendar.JANUARY, 15);
      BigDecimal amount = new BigDecimal("200");

      HashMap<String, String> result = invokeInsertData("PAY001", "pay123", paymentDate, amount,
          "bp004", "Partner 4", 6, "C4B6506838E14A349D6717D6856F1B56", true, BigDecimal.ZERO);

      assertNull(result.get(NETDUE), "NETDUE should not be set for credits");
      assertEquals("200", result.get("SHOW_NETDUE"));
    }

    @Test
    @DisplayName("Should contain NETDUE key when credits is false")
    void shouldContainNetdueWhenCreditsFalse() throws Exception {
      Date invoiceDate = createDate(2026, Calendar.JANUARY, 15);
      BigDecimal amount = new BigDecimal("300");

      HashMap<String, String> result = invokeInsertData("INV004", "inv456", invoiceDate, amount,
          "bp005", "Partner 5", 2, "263", false, BigDecimal.ZERO);

      assertEquals("300", result.get(NETDUE));
      assertEquals("300", result.get("SHOW_NETDUE"));
    }

    @Test
    @DisplayName("Should set DOUBTFUL_DEBT when doubtfulDebt is non-zero")
    void shouldSetDoubtfulDebtWhenNonZero() throws Exception {
      Date invoiceDate = createDate(2026, Calendar.JANUARY, 15);
      BigDecimal amount = new BigDecimal("400");
      BigDecimal doubtfulDebt = new BigDecimal("100");

      HashMap<String, String> result = invokeInsertData("INV005", "inv789", invoiceDate, amount,
          "bp006", "Partner 6", 1, "263", false, doubtfulDebt);

      assertEquals("100", result.get("DOUBTFUL_DEBT"));
      assertEquals("500", result.get(NETDUE));
      assertNotNull(result.get("PERCENTAGE"));
    }

    @Test
    @DisplayName("Should set DOUBTFUL_DEBT to null when doubtfulDebt is ZERO")
    void shouldSetDoubtfulDebtToNullWhenZero() throws Exception {
      Date invoiceDate = createDate(2026, Calendar.JANUARY, 15);
      BigDecimal amount = new BigDecimal("400");

      HashMap<String, String> result = invokeInsertData("INV006", "inv000", invoiceDate, amount,
          "bp007", "Partner 7", 1, "263", false, BigDecimal.ZERO);

      assertNull(result.get("DOUBTFUL_DEBT"));
      assertNull(result.get("PERCENTAGE"));
    }

    @Test
    @DisplayName("Should compute SHOW_NETDUE as amount + doubtfulDebt for credits")
    void shouldComputeShowNetdueForCredits() throws Exception {
      Date paymentDate = createDate(2026, Calendar.FEBRUARY, 20);
      BigDecimal creditAmount = new BigDecimal("300.00");
      BigDecimal doubtfulDebt = new BigDecimal("50.00");

      HashMap<String, String> result = invokeInsertData("PAY002", "pay456", paymentDate,
          creditAmount, "bp008", "Credit Partner Two", 6, "F7A52FDAAA0346EFA07D53C125B40404",
          true, doubtfulDebt);

      assertEquals("350.00", result.get("SHOW_NETDUE"));
    }

    @Test
    @DisplayName("Should set PERCENTAGE to null when doubtfulDebt is ZERO")
    void shouldSetPercentageToNullWhenDoubtfulDebtZero() throws Exception {
      Date invoiceDate = createDate(2026, Calendar.MARCH, 25);
      BigDecimal amount = new BigDecimal("500.00");

      HashMap<String, String> result = invokeInsertData("INV007", "inv999", invoiceDate, amount,
          "bp009", "Partner Nine", 5, "263", false, BigDecimal.ZERO);

      assertNull(result.get("PERCENTAGE"));
    }
  }

  /**
   * Helper to create a Date for a specific year, month, and day.
   */
  private static Date createDate(int year, int month, int day) {
    return new GregorianCalendar(year, month, day).getTime();
  }
}
