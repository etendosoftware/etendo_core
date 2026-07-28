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
package org.openbravo.erpCommon.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openbravo.base.BaseCoreTest;

/**
 * Unit tests for {@link OBDateUtils}.
 */
@DisplayName("OBDateUtils")
public class OBDateUtilsTest extends BaseCoreTest {

  private static final String DATE_PATTERN = "dd-MM-yyyy";
  private static final String DATETIME_PATTERN = "dd-MM-yyyy HH:mm:ss";
  private static final String DATE_15_03_2026 = "15-03-2026";
  private static final String DATE_10_03_2026 = "10-03-2026";
  private static final String DATE_16_03_2026 = "16-03-2026";
  private final DateFormat DF = new SimpleDateFormat(DATE_PATTERN);

  @Nested
  @DisplayName("formatDate(Date, String pattern)")
  class FormatDateWithPattern {
    @Test
    void testFormatWithPattern() {
      Calendar cal = Calendar.getInstance();
      cal.set(2026, Calendar.MARCH, 15, 0, 0, 0);
      assertEquals("2026/03/15", OBDateUtils.formatDate(cal.getTime(), "yyyy/MM/dd"));
    }
  }

  @Nested
  @DisplayName("addDaysToDate")
  class AddDaysToDate {
    @Test
    void testAddPositiveDays() throws ParseException {
      assertEquals("11-01-2026", OBDateUtils.addDaysToDate("01-01-2026", 10, DF));
    }

    @Test
    void testAddNegativeDays() throws ParseException {
      assertEquals(DATE_10_03_2026, OBDateUtils.addDaysToDate(DATE_15_03_2026, -5, DF));
    }

    @Test
    void testNullInput() throws ParseException {
      assertEquals("", OBDateUtils.addDaysToDate(null, 1, DF));
    }

    @Test
    void testEmptyInput() throws ParseException {
      assertEquals("", OBDateUtils.addDaysToDate("", 1, DF));
    }

    @Test
    void testZeroDays() throws ParseException {
      assertEquals(DATE_15_03_2026, OBDateUtils.addDaysToDate(DATE_15_03_2026, 0, DF));
    }
  }

  @Nested
  @DisplayName("isWeekendDay")
  class IsWeekendDay {
    @Test
    void testSaturday() throws ParseException {
      assertTrue(OBDateUtils.isWeekendDay("14-03-2026", DF));
    }

    @Test
    void testSunday() throws ParseException {
      assertTrue(OBDateUtils.isWeekendDay(DATE_15_03_2026, DF));
    }

    @Test
    void testMonday() throws ParseException {
      assertFalse(OBDateUtils.isWeekendDay(DATE_16_03_2026, DF));
    }

    @Test
    void testFriday() throws ParseException {
      assertFalse(OBDateUtils.isWeekendDay("13-03-2026", DF));
    }
  }

  @Nested
  @DisplayName("isBiggerDate")
  class IsBiggerDate {
    @Test
    void testDate1After() throws ParseException {
      assertTrue(OBDateUtils.isBiggerDate(DATE_15_03_2026, DATE_10_03_2026, DF));
    }

    @Test
    void testDate1Before() throws ParseException {
      assertFalse(OBDateUtils.isBiggerDate(DATE_10_03_2026, DATE_15_03_2026, DF));
    }

    @Test
    void testEqualDates() throws ParseException {
      assertFalse(OBDateUtils.isBiggerDate(DATE_15_03_2026, DATE_15_03_2026, DF));
    }
  }

  @Nested
  @DisplayName("calculateLaborDays")
  class CalculateLaborDays {
    @Test
    void testWeekdayRange() throws ParseException {
      assertEquals("4", OBDateUtils.calculateLaborDays(DATE_16_03_2026, "20-03-2026", DF));
    }

    @Test
    void testRangeSpanningWeekend() throws ParseException {
      assertEquals("1", OBDateUtils.calculateLaborDays("13-03-2026", DATE_16_03_2026, DF));
    }

    @Test
    void testNullDate() throws ParseException {
      assertEquals("", OBDateUtils.calculateLaborDays(null, DATE_16_03_2026, DF));
    }

    @Test
    void testEmptyDate() throws ParseException {
      assertEquals("", OBDateUtils.calculateLaborDays("", DATE_16_03_2026, DF));
    }

    @Test
    void testReversedDates() throws ParseException {
      assertEquals("4", OBDateUtils.calculateLaborDays("20-03-2026", DATE_16_03_2026, DF));
    }
  }

  @Nested
  @DisplayName("getEndOfDay")
  class GetEndOfDay {
    @Test
    void testEndOfDay() {
      Calendar cal = Calendar.getInstance();
      cal.set(2026, Calendar.MARCH, 15, 10, 30, 0);
      Date result = OBDateUtils.getEndOfDay(cal.getTime());
      Calendar resCal = Calendar.getInstance();
      resCal.setTime(result);
      assertEquals(23, resCal.get(Calendar.HOUR_OF_DAY));
      assertEquals(59, resCal.get(Calendar.MINUTE));
      assertEquals(59, resCal.get(Calendar.SECOND));
    }
  }

  @Nested
  @DisplayName("convertDateToUTC")
  class ConvertDateToUTC {
    @Test
    void testReturnsNonNull() {
      assertNotNull(OBDateUtils.convertDateToUTC(new Date()));
    }
  }

  @Nested
  @DisplayName("formatDate(Date) with mocked properties")
  class FormatDateFromProperties {
    @Test
    void testFormatDate() {
      mockDateFormats(DATE_PATTERN, DATETIME_PATTERN);
      Calendar cal = Calendar.getInstance();
      cal.set(2026, Calendar.MARCH, 15, 0, 0, 0);
      assertEquals(DATE_15_03_2026, OBDateUtils.formatDate(cal.getTime()));
    }
  }

  @Nested
  @DisplayName("formatDateTime(Date) with mocked properties")
  class FormatDateTime {
    @Test
    void testFormatDateTime() {
      mockDateFormats(DATE_PATTERN, DATETIME_PATTERN);
      Calendar cal = Calendar.getInstance();
      cal.set(2026, Calendar.MARCH, 15, 14, 30, 45);
      cal.set(Calendar.MILLISECOND, 0);
      assertEquals("15-03-2026 14:30:45", OBDateUtils.formatDateTime(cal.getTime()));
    }
  }

  @Nested
  @DisplayName("getDate(String) with mocked properties")
  class GetDate {
    @Test
    void testGetDate() throws ParseException {
      mockDateFormats(DATE_PATTERN, DATETIME_PATTERN);
      Date result = OBDateUtils.getDate(DATE_15_03_2026);
      assertNotNull(result);
      Calendar cal = Calendar.getInstance();
      cal.setTime(result);
      assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH));
    }

    @Test
    void testEmptyString() throws ParseException {
      mockDateFormats(DATE_PATTERN, DATETIME_PATTERN);
      assertNull(OBDateUtils.getDate(""));
    }
  }

  @Nested
  @DisplayName("getDateTime(String) with mocked properties")
  class GetDateTime {
    @Test
    void testWithTime() throws ParseException {
      mockDateFormats(DATE_PATTERN, DATETIME_PATTERN);
      Date result = OBDateUtils.getDateTime("15-03-2026 14:30:45");
      assertNotNull(result);
    }

    @Test
    void testEmptyString() throws ParseException {
      assertNull(OBDateUtils.getDateTime(""));
    }

    @Test
    void testNullString() throws ParseException {
      assertNull(OBDateUtils.getDateTime(null));
    }
  }
}
