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
 * All portions are Copyright © 2021–2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.test.businessutility;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.audittrail.AuditTrailTimeFormatter;
import org.openbravo.base.secureApp.VariablesSecureApp;

/**
 * Tests the timezone conversion applied when formatting the times shown in the audit trail popup,
 * which must match the local time shown by the rest of the user interface.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AuditTrailTimeFormatterTest {

  private static final String DATE_TIME_FORMAT = "dd-MM-yyyy HH:mm:ss";
  private static final String TZ_OFFSET_SESSION_KEY = "AuditTrail.clientTZOffset";
  private static final String TZ_OFFSET_PARAMETER = "inpClientTZOffset";
  /** UTC-3, as reported by Date.getTimezoneOffset() */
  private static final String OFFSET_UTC_MINUS_3 = "180";
  private static final String INVALID_OFFSET = "';DROP TABLE ad_user;--";
  private static final String UNPARSEABLE_TIME = "not-a-date";
  /** 2026-08-27 08:45:30 UTC */
  private static final Date EVENT_TIME = Date.from(Instant.parse("2026-08-27T08:45:30Z"));

  @Mock
  private VariablesSecureApp mockVars;

  /** Every test formats with the same date and time format of the user. */
  @Before
  public void setUp() {
    when(mockVars.getJavaDataTimeFormat()).thenReturn(DATE_TIME_FORMAT);
  }

  /** A browser behind UTC sees the time moved back by its offset. */
  @Test
  public void formatsTimeInTheBrowserTimeZoneWhenItIsBehindUTC() {
    givenClientTimeZoneOffset(OFFSET_UTC_MINUS_3);

    assertEquals("27-08-2026 05:45:30", AuditTrailTimeFormatter.format(mockVars, EVENT_TIME));
  }

  /** A browser ahead of UTC sees the time moved forward: the sign of the offset is inverted. */
  @Test
  public void formatsTimeInTheBrowserTimeZoneWhenItIsAheadOfUTC() {
    givenClientTimeZoneOffset("-120");

    assertEquals("27-08-2026 10:45:30", AuditTrailTimeFormatter.format(mockVars, EVENT_TIME));
  }

  /** A browser in UTC sees the time exactly as it is stored. */
  @Test
  public void formatsTimeInUTCWhenTheBrowserIsInUTC() {
    givenClientTimeZoneOffset("0");

    assertEquals("27-08-2026 08:45:30", AuditTrailTimeFormatter.format(mockVars, EVENT_TIME));
  }

  /** Without an offset in session the timezone of the server is used. */
  @Test
  public void fallsBackToServerTimeZoneWhenOffsetIsNotAvailable() {
    givenClientTimeZoneOffset("");

    assertEquals(formatInServerTimeZone(), AuditTrailTimeFormatter.format(mockVars, EVENT_TIME));
  }

  /** An offset that is not a number does not raise an error. */
  @Test
  public void fallsBackToServerTimeZoneWhenOffsetIsNotANumber() {
    givenClientTimeZoneOffset("not-a-number");

    assertEquals(formatInServerTimeZone(), AuditTrailTimeFormatter.format(mockVars, EVENT_TIME));
  }

  /** An offset beyond the range of a timezone does not raise an error. */
  @Test
  public void fallsBackToServerTimeZoneWhenOffsetIsOutOfRange() {
    givenClientTimeZoneOffset("5000");

    assertEquals(formatInServerTimeZone(), AuditTrailTimeFormatter.format(mockVars, EVENT_TIME));
  }

  /** The deleted records view receives the time as a string in the timezone of the server. */
  @Test
  public void formatsTheDeletedRecordsTimeInTheBrowserTimeZone() {
    givenClientTimeZoneOffset(OFFSET_UTC_MINUS_3);

    assertEquals(expectedLocalTime(-3), AuditTrailTimeFormatter.format(mockVars, serverTime()));
  }

  /** A deleted records time that cannot be interpreted is shown as it comes. */
  @Test
  public void keepsTheDeletedRecordsTimeWhenItCannotBeParsed() {
    givenClientTimeZoneOffset(OFFSET_UTC_MINUS_3);

    assertEquals(UNPARSEABLE_TIME, AuditTrailTimeFormatter.format(mockVars, UNPARSEABLE_TIME));
  }

  /** An empty deleted records time is shown as it comes. */
  @Test
  public void keepsAnEmptyDeletedRecordsTime() {
    givenClientTimeZoneOffset(OFFSET_UTC_MINUS_3);

    assertEquals("", AuditTrailTimeFormatter.format(mockVars, ""));
  }

  /** The offset sent by the browser is kept in session for the requests that render the data. */
  @Test
  public void storesTheOffsetSentByTheBrowserInSession() {
    when(mockVars.getStringParameter(TZ_OFFSET_PARAMETER)).thenReturn(OFFSET_UTC_MINUS_3);

    AuditTrailTimeFormatter.saveClientTimeZoneOffset(mockVars);

    verify(mockVars).setSessionValue(TZ_OFFSET_SESSION_KEY, OFFSET_UTC_MINUS_3);
  }

  /** A parameter that is not an offset never reaches the session. */
  @Test
  public void doesNotStoreAnInvalidOffset() {
    when(mockVars.getStringParameter(TZ_OFFSET_PARAMETER)).thenReturn(INVALID_OFFSET);

    AuditTrailTimeFormatter.saveClientTimeZoneOffset(mockVars);

    verify(mockVars, never()).setSessionValue(TZ_OFFSET_SESSION_KEY, INVALID_OFFSET);
  }

  /** Opening the popup clears the offset of a previous one. */
  @Test
  public void removesTheOffsetFromSession() {
    AuditTrailTimeFormatter.removeClientTimeZoneOffset(mockVars);

    verify(mockVars).removeSessionValue(TZ_OFFSET_SESSION_KEY);
  }

  private void givenClientTimeZoneOffset(String offset) {
    when(mockVars.getSessionValue(TZ_OFFSET_SESSION_KEY)).thenReturn(offset);
  }

  /** The event time as returned by the deleted records query, in the timezone of the server. */
  private String serverTime() {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    format.setTimeZone(TimeZone.getDefault());
    return format.format(EVENT_TIME);
  }

  private String expectedLocalTime(int hoursFromUTC) {
    SimpleDateFormat format = new SimpleDateFormat(DATE_TIME_FORMAT);
    format.setTimeZone(TimeZone.getTimeZone(ZoneOffset.ofHours(hoursFromUTC)));
    return format.format(EVENT_TIME);
  }

  private String formatInServerTimeZone() {
    SimpleDateFormat format = new SimpleDateFormat(DATE_TIME_FORMAT);
    format.setTimeZone(TimeZone.getDefault());
    return format.format(EVENT_TIME);
  }
}
