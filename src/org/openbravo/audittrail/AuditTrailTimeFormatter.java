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
package org.openbravo.audittrail;

import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

import org.openbravo.base.secureApp.VariablesSecureApp;

/**
 * Formats the times shown in the audit trail popup in the timezone of the browser that opened it,
 * so that they match the times shown by the rest of the user interface.
 * <p>
 * Timestamps are stored in UTC and the grid converts them on the client side. As the audit trail
 * popup builds its contents on the server, the browser sends its timezone offset when the popup is
 * opened and that offset is applied here. When it is not available the timezone of the server is
 * used instead.
 */
public final class AuditTrailTimeFormatter {

  private static final String CLIENT_TZ_OFFSET_PARAMETER = "inpClientTZOffset";
  private static final String CLIENT_TZ_OFFSET_SESSION_KEY = "AuditTrail.clientTZOffset";
  private static final int SECONDS_PER_MINUTE = 60;
  /** The largest offset a zone can have, as accepted by {@link ZoneOffset} */
  private static final int MAX_OFFSET_MINUTES = 18 * 60;

  private AuditTrailTimeFormatter() {
  }

  /**
   * Stores in session the timezone offset reported by the browser when opening the popup. Values
   * that are not a valid offset are ignored, keeping whatever was stored before.
   * <p>
   * It is kept in session because the requests that render the data afterwards do not send it.
   *
   * @param vars
   *          the session variables of the request that opened the popup
   */
  public static void saveClientTimeZoneOffset(VariablesSecureApp vars) {
    Integer offsetMinutes = parseOffsetMinutes(
        vars.getStringParameter(CLIENT_TZ_OFFSET_PARAMETER));
    if (offsetMinutes != null) {
      vars.setSessionValue(CLIENT_TZ_OFFSET_SESSION_KEY, offsetMinutes.toString());
    }
  }

  /**
   * Removes the timezone offset stored in session, so that a popup opened without sending it does
   * not inherit the offset of a previous one.
   *
   * @param vars
   *          the session variables of the current request
   */
  public static void removeClientTimeZoneOffset(VariablesSecureApp vars) {
    vars.removeSessionValue(CLIENT_TZ_OFFSET_SESSION_KEY);
  }

  /**
   * Formats an event time in the timezone of the browser, using the date and time format of the
   * user.
   *
   * @param vars
   *          the session variables of the current request
   * @param time
   *          the moment of the event
   * @return the formatted time
   */
  public static String format(VariablesSecureApp vars, Date time) {
    SimpleDateFormat format = new SimpleDateFormat(vars.getJavaDataTimeFormat());
    format.setTimeZone(getClientTimeZone(vars));
    return format.format(time);
  }

  /**
   * Returns the timezone of the browser that opened the popup, falling back to the timezone of the
   * server when the offset is not available, i.e. when the popup is requested without going through
   * the toolbar button.
   */
  private static TimeZone getClientTimeZone(VariablesSecureApp vars) {
    Integer offsetMinutes = parseOffsetMinutes(
        vars.getSessionValue(CLIENT_TZ_OFFSET_SESSION_KEY));
    if (offsetMinutes == null) {
      return TimeZone.getDefault();
    }
    // Date.getTimezoneOffset() returns the minutes to be added to the local time to obtain UTC,
    // so its sign is the opposite of the one of the zone offset
    return TimeZone.getTimeZone(ZoneOffset.ofTotalSeconds(-offsetMinutes * SECONDS_PER_MINUTE));
  }

  /**
   * Interprets the offset reported by the browser, in minutes, returning null when it is missing or
   * it is not a number within the range of a timezone offset.
   */
  private static Integer parseOffsetMinutes(String clientTZOffset) {
    if (clientTZOffset == null || clientTZOffset.isEmpty()) {
      return null;
    }
    try {
      int offsetMinutes = Integer.parseInt(clientTZOffset.trim());
      if (Math.abs(offsetMinutes) > MAX_OFFSET_MINUTES) {
        return null;
      }
      return offsetMinutes;
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
