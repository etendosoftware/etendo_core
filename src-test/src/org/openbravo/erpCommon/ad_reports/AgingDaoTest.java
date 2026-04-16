/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.erpCommon.ad_reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link AgingDao}.
 */
@SuppressWarnings({"java:S107", "java:S120", "java:S112"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class AgingDaoTest {

  private static final String DD_MM_YYYY = "dd-MM-yyyy";
  private static final String NETDUE = "NETDUE";

  private AgingDao dao;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    dao = new AgingDao();
  }

  // --- convertToDate tests ---
  /**
   * Convert to date subtracts days.
   * @throws Exception if an error occurs
   */

  @Test
  public void testConvertToDateSubtractsDays() throws Exception {
    Calendar cal = new GregorianCalendar(2026, Calendar.JANUARY, 31);
    Date currentDate = cal.getTime();

    Date result = invokeConvertToDate(currentDate, "10");

    Calendar expected = new GregorianCalendar(2026, Calendar.JANUARY, 21);
    assertEquals(expected.getTime(), result);
  }
  /**
   * Convert to date subtracts zero days.
   * @throws Exception if an error occurs
   */

  @Test
  public void testConvertToDateSubtractsZeroDays() throws Exception {
    Calendar cal = new GregorianCalendar(2026, Calendar.MARCH, 15);
    Date currentDate = cal.getTime();

    Date result = invokeConvertToDate(currentDate, "0");

    assertEquals(currentDate, result);
  }
  /**
   * Convert to date crosses month boundary.
   * @throws Exception if an error occurs
   */

  @Test
  public void testConvertToDateCrossesMonthBoundary() throws Exception {
    Calendar cal = new GregorianCalendar(2026, Calendar.MARCH, 5);
    Date currentDate = cal.getTime();

    Date result = invokeConvertToDate(currentDate, "10");

    Calendar expected = new GregorianCalendar(2026, Calendar.FEBRUARY, 23);
    assertEquals(expected.getTime(), result);
  }
  /**
   * Convert to date crosses year boundary.
   * @throws Exception if an error occurs
   */

  @Test
  public void testConvertToDateCrossesYearBoundary() throws Exception {
    Calendar cal = new GregorianCalendar(2026, Calendar.JANUARY, 5);
    Date currentDate = cal.getTime();

    Date result = invokeConvertToDate(currentDate, "10");

    Calendar expected = new GregorianCalendar(2025, Calendar.DECEMBER, 26);
    assertEquals(expected.getTime(), result);
  }
  /**
   * Convert to date large number of days.
   * @throws Exception if an error occurs
   */

  @Test
  public void testConvertToDateLargeNumberOfDays() throws Exception {
    Calendar cal = new GregorianCalendar(2026, Calendar.JUNE, 15);
    Date currentDate = cal.getTime();

    Date result = invokeConvertToDate(currentDate, "365");

    Calendar expected = new GregorianCalendar(2025, Calendar.JUNE, 15);
    assertEquals(expected.getTime(), result);
  }

  // --- calculatePercentage tests ---
  /**
   * Calculate percentage returns zero when doubtful is zero.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCalculatePercentageReturnsZeroWhenDoubtfulIsZero() throws Exception {
    BigDecimal totalAmount = new BigDecimal("1000");
    BigDecimal doubtfulDebt = BigDecimal.ZERO;

    BigDecimal result = invokeCalculatePercentage(totalAmount, doubtfulDebt);

    assertEquals(0, BigDecimal.ZERO.compareTo(result));
  }
  /**
   * Calculate percentage fifty percent.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCalculatePercentageFiftyPercent() throws Exception {
    BigDecimal totalAmount = new BigDecimal("200");
    BigDecimal doubtfulDebt = new BigDecimal("100");

    BigDecimal result = invokeCalculatePercentage(totalAmount, doubtfulDebt);

    assertEquals(0, new BigDecimal("50.00000").compareTo(result));
  }
  /**
   * Calculate percentage hundred percent.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCalculatePercentageHundredPercent() throws Exception {
    BigDecimal totalAmount = new BigDecimal("500");
    BigDecimal doubtfulDebt = new BigDecimal("500");

    BigDecimal result = invokeCalculatePercentage(totalAmount, doubtfulDebt);

    assertEquals(0, new BigDecimal("100.00000").compareTo(result));
  }
  /**
   * Calculate percentage small fraction.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCalculatePercentageSmallFraction() throws Exception {
    BigDecimal totalAmount = new BigDecimal("10000");
    BigDecimal doubtfulDebt = new BigDecimal("1");

    BigDecimal result = invokeCalculatePercentage(totalAmount, doubtfulDebt);

    assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
    assertTrue(result.compareTo(new BigDecimal("1")) < 0);
  }

  // --- insertData tests ---
  /**
   * Insert data basic fields.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInsertDataBasicFields() throws Exception {
    SimpleDateFormat dateFormat = new SimpleDateFormat(DD_MM_YYYY);
    Date date = new GregorianCalendar(2026, Calendar.JANUARY, 15).getTime();
    BigDecimal amount = new BigDecimal("100.50");

    HashMap<String, String> result = invokeInsertData("INV001", "id123", date, amount,
        "bp001", "Test Partner", 1, "263", dateFormat, false, BigDecimal.ZERO);

    assertNotNull(result);
    assertEquals("INV001", result.get("INVOICE_NUMBER"));
    assertEquals("id123", result.get("INVOICE_ID"));
    assertEquals("15-01-2026", result.get("INVOICE_DATE"));
    assertEquals("bp001", result.get("BPARTNER"));
    assertEquals("Test Partner", result.get("BPARTNERNAME"));
    assertEquals("263", result.get("TABID"));
  }
  /**
   * Insert data amount is null when zero.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInsertDataAmountIsNullWhenZero() throws Exception {
    SimpleDateFormat dateFormat = new SimpleDateFormat(DD_MM_YYYY);
    Date date = new GregorianCalendar(2026, Calendar.JANUARY, 15).getTime();

    HashMap<String, String> result = invokeInsertData("INV002", "id456", date,
        BigDecimal.ZERO, "bp002", "Partner 2", 1, "263", dateFormat, false, BigDecimal.ZERO);

    assertNull(result.get("AMOUNT1"));
  }
  /**
   * Insert data amount stored in correct scope.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInsertDataAmountStoredInCorrectScope() throws Exception {
    SimpleDateFormat dateFormat = new SimpleDateFormat(DD_MM_YYYY);
    Date date = new GregorianCalendar(2026, Calendar.JANUARY, 15).getTime();
    BigDecimal amount = new BigDecimal("500");

    HashMap<String, String> result = invokeInsertData("INV003", "id789", date, amount,
        "bp003", "Partner 3", 3, "263", dateFormat, false, BigDecimal.ZERO);

    assertEquals("500", result.get("AMOUNT3"));
    assertNull(result.get("AMOUNT1"));
  }
  /**
   * Insert data credit mode does not set netdue.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInsertDataCreditModeDoesNotSetNetdue() throws Exception {
    SimpleDateFormat dateFormat = new SimpleDateFormat(DD_MM_YYYY);
    Date date = new GregorianCalendar(2026, Calendar.JANUARY, 15).getTime();
    BigDecimal amount = new BigDecimal("200");

    HashMap<String, String> result = invokeInsertData("PAY001", "pay123", date, amount,
        "bp004", "Partner 4", 6, "C4B6506838E14A349D6717D6856F1B56", dateFormat, true,
        BigDecimal.ZERO);

    assertNull(result.get(NETDUE));
    assertEquals("200", result.get("SHOW_NETDUE"));
  }
  /**
   * Insert data non credit mode sets netdue.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInsertDataNonCreditModeSetsNetdue() throws Exception {
    SimpleDateFormat dateFormat = new SimpleDateFormat(DD_MM_YYYY);
    Date date = new GregorianCalendar(2026, Calendar.JANUARY, 15).getTime();
    BigDecimal amount = new BigDecimal("300");

    HashMap<String, String> result = invokeInsertData("INV004", "inv456", date, amount,
        "bp005", "Partner 5", 2, "263", dateFormat, false, BigDecimal.ZERO);

    assertEquals("300", result.get(NETDUE));
    assertEquals("300", result.get("SHOW_NETDUE"));
  }
  /**
   * Insert data with doubtful debt.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInsertDataWithDoubtfulDebt() throws Exception {
    SimpleDateFormat dateFormat = new SimpleDateFormat(DD_MM_YYYY);
    Date date = new GregorianCalendar(2026, Calendar.JANUARY, 15).getTime();
    BigDecimal amount = new BigDecimal("400");
    BigDecimal doubtfulDebt = new BigDecimal("100");

    HashMap<String, String> result = invokeInsertData("INV005", "inv789", date, amount,
        "bp006", "Partner 6", 1, "263", dateFormat, false, doubtfulDebt);

    assertEquals("100", result.get("DOUBTFUL_DEBT"));
    assertEquals("500", result.get(NETDUE));
    assertNotNull(result.get("PERCENTAGE"));
  }
  /**
   * Insert data doubtful debt null when zero.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInsertDataDoubtfulDebtNullWhenZero() throws Exception {
    SimpleDateFormat dateFormat = new SimpleDateFormat(DD_MM_YYYY);
    Date date = new GregorianCalendar(2026, Calendar.JANUARY, 15).getTime();
    BigDecimal amount = new BigDecimal("400");

    HashMap<String, String> result = invokeInsertData("INV006", "inv000", date, amount,
        "bp007", "Partner 7", 1, "263", dateFormat, false, BigDecimal.ZERO);

    assertNull(result.get("DOUBTFUL_DEBT"));
    assertNull(result.get("PERCENTAGE"));
  }

  // --- Helper methods ---

  private Date invokeConvertToDate(Date currentDate, String strcolumn) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = AgingDao.class.getDeclaredMethod("convertToDate", Date.class, String.class);
    method.setAccessible(true);
    return (Date) method.invoke(dao, currentDate, strcolumn);
  }

  private BigDecimal invokeCalculatePercentage(BigDecimal totalAmount, BigDecimal doubtfulDebt)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = AgingDao.class.getDeclaredMethod("calculatePercentage", BigDecimal.class,
        BigDecimal.class);
    method.setAccessible(true);
    return (BigDecimal) method.invoke(dao, totalAmount, doubtfulDebt);
  }

  @SuppressWarnings("unchecked")
  private HashMap<String, String> invokeInsertData(String documentNo, String id, Date date,
      BigDecimal amount, String bpartnerId, String bpartnerName, int group, String tabId,
      SimpleDateFormat dateFormat, boolean credits, BigDecimal doubtfulDebt) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = AgingDao.class.getDeclaredMethod("insertData", String.class, String.class,
        Date.class, BigDecimal.class, String.class, String.class, int.class, String.class,
        SimpleDateFormat.class, boolean.class, BigDecimal.class);
    method.setAccessible(true);
    return (HashMap<String, String>) method.invoke(dao, documentNo, id, date, amount, bpartnerId,
        bpartnerName, group, tabId, dateFormat, credits, doubtfulDebt);
  }
}
