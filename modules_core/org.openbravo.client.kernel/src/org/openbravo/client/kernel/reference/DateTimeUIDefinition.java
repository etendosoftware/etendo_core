/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2010-2020 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel.reference;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.domaintype.PrimitiveDomainType;
import org.openbravo.base.session.OBPropertiesProvider;

/**
 * Implementation of the date time ui definition.
 * 
 * @author mtaal
 */
public class DateTimeUIDefinition extends DateUIDefinition {
  private SimpleDateFormat dateFormat = null;

  @Override
  public String getParentType() {
    return "datetime";
  }

  @Override
  public String getFormEditorType() {
    return "OBDateTimeItem";
  }

  @Override
  protected String getClientFormatObject() {
    return "OB.Format.dateTime";
  }

  @Override
  public String convertToClassicString(Object value) {
    if (value == null || value == "") {
      return "";
    }

    if (value instanceof String) {
      return (String) value;
    }
    Date date = (Date) value;
    date = convertLocalDateTimeToUTC(date);

    return formatDateTime(date);
  }

  /**
   * Creates a classic string which is used by callouts from an object value.
   * Date is formatted as is, using local timezone
   *
   * @param value Object to be converted
   * @return converted of formatted date string
   */
  public String convertToClassicStringInLocalTime(Object value) {
    if (value == null || value == "") {
      return "";
    }

    if (value instanceof String) {
      return (String) value;
    }
    Date date = (Date) value;

    return formatDateTime(date);
  }

  private Date convertLocalDateTimeToUTC(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);

    int gmtMillisecondOffset = (calendar.get(Calendar.ZONE_OFFSET)
        + calendar.get(Calendar.DST_OFFSET));
    calendar.add(Calendar.MILLISECOND, -gmtMillisecondOffset);
    return calendar.getTime();
  }

  private String formatDateTime(Date date) {
    SimpleDateFormat dateTimeFormat = getClassicFormat();
    synchronized (dateTimeFormat) {
      return getClassicFormat().format(date, new StringBuffer(), new FieldPosition(0)).toString();
    }
  }

  @Override
  protected SimpleDateFormat getClassicFormat() {
    if (dateFormat == null) {
      String pattern = OBPropertiesProvider.getInstance()
          .getOpenbravoProperties()
          .getProperty("dateTimeFormat.java");
      dateFormat = new SimpleDateFormat(pattern);
      dateFormat.setLenient(true);
    }
    return dateFormat;
  }

  @Override
  public synchronized Object createFromClassicString(String value) {
    try {
      if (value == null || value.length() == 0 || value.equals("null")) {
        return null;
      }
      if (value.contains("T")) {
        return value;
      }
      Calendar now = Calendar.getInstance();
      final Date date = getClassicFormat().parse(value);
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(date);
      // Applies the zone offset and the dst offset to convert the time from local to UTC
      int gmtMillisecondOffset = (now.get(Calendar.ZONE_OFFSET) + now.get(Calendar.DST_OFFSET));
      calendar.add(Calendar.MILLISECOND, -gmtMillisecondOffset);
      return ((PrimitiveDomainType) getDomainType()).convertToString(date);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

}
