package org.openbravo.scheduling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.junit.Test;

/**
 * Test class for SchedulerTimeUtils.
 * Tests date and time manipulation utilities used in the scheduling system.
 */
public class SchedulerTimeUtilsTest {

  /**
   * Tests timestamp creation with a valid date and time string.
   * Expected format: "dd-MM-yyyy HH:mm:ss".
   */
  @Test
  public void testTimestampValidDateTime() throws ParseException {
    // GIVEN
    String dateTime = "25-12-2023 15:30:00";

    // WHEN
    Date result = SchedulerTimeUtils.timestamp(dateTime);

    // THEN
    assertNotNull(result);
    assertEquals("25-12-2023 15:30:00", SchedulerTimeUtils.format(result, "dd-MM-yyyy HH:mm:ss"));
  }

  /**
   * Tests timestamp creation with an empty date time string.
   * Should return current date and time.
   */
  @Test
  public void testTimestampEmptyDateTime() throws ParseException {
    // GIVEN
    String dateTime = "";

    // WHEN
    Date result = SchedulerTimeUtils.timestamp(dateTime);

    // THEN
    assertNotNull(result);
    String expectedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
    assertEquals(expectedDate.substring(0, 10), SchedulerTimeUtils.format(result, "dd-MM-yyyy").substring(0, 10));
  }

  /**
   * Tests timestamp creation with an invalid date format.
   * Should throw ParseException.
   */
  @Test(expected = ParseException.class)
  public void testTimestampInvalidDateTime() throws ParseException {
    // GIVEN
    String dateTime = "invalid-date";

    // WHEN
    SchedulerTimeUtils.timestamp(dateTime);

    // THEN
    // Expect ParseException
  }

  /**
   * Tests parsing a valid date time string into LocalDateTime.
   * Expected format: "dd-MM-yyyy HH:mm:ss".
   */
  @Test
  public void testParseValidDateTime() throws ParseException {
    // GIVEN
    String dateTime = "01-01-2024 00:00:00";

    // WHEN
    LocalDateTime result = SchedulerTimeUtils.parse(dateTime);

    // THEN
    assertNotNull(result);
    assertEquals("01-01-2024 00:00:00", result.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
  }

  /**
   * Tests parsing an invalid date time string.
   * Should throw ParseException.
   */
  @Test(expected = ParseException.class)
  public void testParseInvalidDateTime() throws ParseException {
    // GIVEN
    String dateTime = "invalid-date";

    // WHEN
    SchedulerTimeUtils.parse(dateTime);

    // THEN
    // Expect ParseException
  }

  /**
   * Tests getting current date time with both date and time provided.
   * Should return combined date and time string.
   */
  @Test
  public void testGetCurrentDateTimeWithDateAndTime() {
    // GIVEN
    String date = "10-10-2023";
    String time = "12:00:00";

    // WHEN
    String result = SchedulerTimeUtils.getCurrentDateTime(date, time);

    // THEN
    assertEquals("10-10-2023 12:00:00", result);
  }

  /**
   * Tests getting current date time with empty date.
   * Should use current date with provided time.
   */
  @Test
  public void testGetCurrentDateTimeWithEmptyDate() {
    // GIVEN
    String date = "";
    String time = "12:00:00";

    // WHEN
    String result = SchedulerTimeUtils.getCurrentDateTime(date, time);

    // THEN
    String expectedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    assertEquals(expectedDate + " 12:00:00", result);
  }

  /**
   * Tests getting current date time with empty time.
   * Should use provided date with current time.
   */
  @Test
  public void testGetCurrentDateTimeWithEmptyTime() {
    // GIVEN
    String date = "10-10-2023";
    String time = "";

    // WHEN
    String result = SchedulerTimeUtils.getCurrentDateTime(date, time);

    // THEN
    String expectedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    assertEquals("10-10-2023 " + expectedTime, result);
  }

  /**
   * Tests formatting current date with specific format pattern.
   * Verifies correct date formatting.
   */
  @Test
  public void testCurrentDate() {
    // GIVEN
    String format = "yyyy/MM/dd HH:mm";

    // WHEN
    String result = SchedulerTimeUtils.currentDate(format);

    // THEN
    String expectedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern(format));
    assertEquals(expectedDate, result);
  }

  /**
   * Tests formatting a Date object with specific format pattern.
   * Verifies correct date formatting.
   */
  @Test
  public void testFormatValidDate() {
    // GIVEN
    Date date = new Date();
    String format = "dd-MM-yyyy";

    // WHEN
    String result = SchedulerTimeUtils.format(date, format);

    // THEN
    String expectedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern(format));
    assertEquals(expectedDate, result);
  }
}
