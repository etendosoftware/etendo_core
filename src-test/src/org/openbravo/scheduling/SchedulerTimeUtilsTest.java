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

  public static final String DATE_TIME_FORMAT = "dd-MM-yyyy HH:mm:ss";
  public static final String DATE_FORMAT = "dd-MM-yyyy";

  /**
   * Tests timestamp creation with a valid date and time string.
   * Expected format: "dd-MM-yyyy HH:mm:ss".
   *
   * @throws ParseException if the input date string cannot be parsed according to the expected format,
   *                       which can occur in cases such as:
   *                       - Invalid date components (e.g., month > 12)
   *                       - Invalid time components (e.g., hours > 23)
   *                       - Incorrect date/time format
   *                       - Malformed input string
   */
  @Test
  public void testTimestampValidDateTime() throws ParseException {
    // GIVEN
    String dateTime = "25-12-2023 15:30:00";

    // WHEN
    Date result = SchedulerTimeUtils.timestamp(dateTime);

    // THEN
    assertNotNull(result);
    assertEquals("25-12-2023 15:30:00", SchedulerTimeUtils.format(result, DATE_TIME_FORMAT));
  }

  /**
   * Tests timestamp creation with an empty date time string.
   * Should return current date and time.
   *
   * @throws ParseException if there's an error converting the empty string to timestamp,
   *                       though this should not occur with empty input as it defaults to current time
   */
  @Test
  public void testTimestampEmptyDateTime() throws ParseException {
    // GIVEN
    String dateTime = "";

    // WHEN
    Date result = SchedulerTimeUtils.timestamp(dateTime);

    // THEN
    assertNotNull(result);
    String expectedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
    assertEquals(expectedDate.substring(0, 10), SchedulerTimeUtils.format(result, DATE_FORMAT).substring(0, 10));
  }

  /**
   * Tests timestamp creation with an invalid date format.
   * Should throw ParseException.
   *
   * @throws ParseException when attempting to parse an invalid date format,
   *                       which is the expected behavior for this test case
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
   *
   * @throws ParseException if the input string cannot be parsed into a LocalDateTime,
   *                       which should not occur with valid input format
   */
  @Test
  public void testParseValidDateTime() throws ParseException {
    // GIVEN
    String dateTime = "01-01-2024 00:00:00";

    // WHEN
    LocalDateTime result = SchedulerTimeUtils.parse(dateTime);

    // THEN
    assertNotNull(result);
    assertEquals("01-01-2024 00:00:00", result.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
  }

  /**
   * Tests parsing an invalid date time string.
   * Should throw ParseException.
   *
   * @throws ParseException when attempting to parse an invalid date string,
   *                       which is the expected behavior for this test case
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
    String expectedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));
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
    String format = DATE_FORMAT;

    // WHEN
    String result = SchedulerTimeUtils.format(date, format);

    // THEN
    String expectedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern(format));
    assertEquals(expectedDate, result);
  }
}
