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
 * All portions are Copyright (C) 2010-2011 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel.reference;

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.data.Sqlc;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.service.json.JsonUtils;

/**
 * Implementation of the date ui definition.
 * 
 * @author mtaal
 */
public class TimeUIDefinition extends UIDefinition {
  private SimpleDateFormat classicFormat = null;
  private SimpleDateFormat xmlTimeFormat = JsonUtils.createJSTimeFormat();

  @Override
  public String getParentType() {
    return "time";
  }

  @Override
  public String getFormEditorType() {
    return "OBTimeItem";
  }

  @Override
  public synchronized String convertToClassicString(Object value) {
    if (value == null || value == "") {
      return "";
    }

    if (value instanceof String) {
      return (String) value;
    }
    String timestamp = value.toString();
    timestamp = timestamp.substring(timestamp.indexOf(" ") + 1);
    StringBuffer convertedValue = convertLocalTimeToUTC(timestamp);
    return convertedValue.toString();
  }

  @Override
  public String getGridFieldProperties(Field field) {
    final String superResult = super.getGridFieldProperties(field);
    return superResult + ", timeFormatter: isc.Time.shortDisplayFormat";
  }

  private SimpleDateFormat getClassicFormat() {
    if (classicFormat == null) {
      String dateTimeFormat = (String) OBPropertiesProvider.getInstance()
          .getOpenbravoProperties()
          .get(KernelConstants.DATETIME_FORMAT_PROPERTY);
      if (dateTimeFormat.endsWith(" a")) {
        // The value of this reference always go to/from the client in the 24h notation, so in case
        // the dateTimeFormat.java be defined to use the 'AM/PM' notation, it should be modified to
        // work with this reference
        dateTimeFormat = dateTimeFormat.replace(" a", "");
        dateTimeFormat = dateTimeFormat.replace("hh", "HH");
      }
      if (dateTimeFormat.contains(" ")) {
        dateTimeFormat = dateTimeFormat.substring(dateTimeFormat.indexOf(" ") + 1);
      } else {
        dateTimeFormat = "HH:mm:ss";
      }
      classicFormat = new SimpleDateFormat(dateTimeFormat);
      classicFormat.setLenient(true);
    }
    return classicFormat;
  }

  /*
   * createFromClassicString has been called with an UTC date, it expects a date in local time,so
   * the time is going to be converted to local time, and going to be passed to
   * createFromClassicString
   */
  private StringBuffer convertUtcToLocalTime(String value) {
    StringBuffer localTimeColumnValue = null;
    try {
      Date UTCDate = getClassicFormat().parse(value);
      Calendar now = Calendar.getInstance();

      Calendar calendar = Calendar.getInstance();
      calendar.setTime(UTCDate);
      calendar.set(Calendar.DATE, now.get(Calendar.DATE));
      calendar.set(Calendar.MONTH, now.get(Calendar.MONTH));
      calendar.set(Calendar.YEAR, now.get(Calendar.YEAR));

      int gmtMillisecondOffset = (now.get(Calendar.ZONE_OFFSET) + now.get(Calendar.DST_OFFSET));
      calendar.add(Calendar.MILLISECOND, gmtMillisecondOffset);
      localTimeColumnValue = getClassicFormat().format(calendar.getTime(), new StringBuffer(),
          new FieldPosition(0));
    } catch (ParseException e) {
      throw new OBException("Exception when parsing date ", e);
    }
    return localTimeColumnValue;
  }

  private StringBuffer convertLocalTimeToUTC(String value) {
    StringBuffer localTimeColumnValue = null;
    try {
      Date UTCDate = getClassicFormat().parse(value);
      Calendar now = Calendar.getInstance();

      Calendar calendar = Calendar.getInstance();
      calendar.setTime(UTCDate);
      calendar.set(Calendar.DATE, now.get(Calendar.DATE));
      calendar.set(Calendar.MONTH, now.get(Calendar.MONTH));
      calendar.set(Calendar.YEAR, now.get(Calendar.YEAR));

      int gmtMillisecondOffset = (now.get(Calendar.ZONE_OFFSET) + now.get(Calendar.DST_OFFSET));
      calendar.add(Calendar.MILLISECOND, -gmtMillisecondOffset);
      localTimeColumnValue = getClassicFormat().format(calendar.getTime(), new StringBuffer(),
          new FieldPosition(0));
    } catch (ParseException e) {
      throw new OBException("Exception when parsing date ", e);
    }
    return localTimeColumnValue;
  }

  // getFieldProperties has to be overridden because depending on the value of getValueFromSession,
  // time fields have to be converted from localTime to UTC before sending the to the client
  @Override
  public String getFieldProperties(Field field, boolean getValueFromSession) {
    String result = super.getFieldProperties(field, getValueFromSession);
    try {
      JSONObject jsnobject = new JSONObject(result);
      if (getValueFromSession) {
        RequestContext rq = RequestContext.get();
        String columnValue = rq.getRequestParameter(
            "inp" + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName()));
        if (StringUtils.isEmpty(columnValue)) {
          // If the date is empty, it does not have to be converted
          return result;
        }
        StringBuffer localTimeColumnValue = convertUtcToLocalTime(columnValue);
        jsnobject.put("value", createFromClassicString(localTimeColumnValue.toString()));
        jsnobject.put("classicValue", localTimeColumnValue.toString());
        return jsnobject.toString();
      }
    } catch (JSONException e) {
      throw new OBException("Exception when parsing date ", e);
    }
    return result;
  }

  @Override
  // Value is a date in local time format
  public synchronized Object createFromClassicString(String value) {
    try {
      if (value == null || value.length() == 0 || value.equals("null")) {
        return null;
      }

      final Date localDate = getClassicFormat().parse(value);
      // If a date is not specified, 01-01-1970 will be set by default
      // Today's date should be returned
      Calendar now = Calendar.getInstance();
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(localDate);
      calendar.set(Calendar.DATE, now.get(Calendar.DATE));
      calendar.set(Calendar.MONTH, now.get(Calendar.MONTH));
      calendar.set(Calendar.YEAR, now.get(Calendar.YEAR));

      // Applies the zone offset and the dst offset to convert the time from local to UTC
      int gmtMillisecondOffset = (now.get(Calendar.ZONE_OFFSET) + now.get(Calendar.DST_OFFSET));
      calendar.add(Calendar.MILLISECOND, -gmtMillisecondOffset);

      return xmlTimeFormat.format(calendar.getTime());
    } catch (Exception e) {
      throw new OBException("Exception when handling value " + value, e);
    }
  }
}
