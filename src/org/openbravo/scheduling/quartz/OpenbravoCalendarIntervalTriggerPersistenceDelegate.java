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
 * All portions are Copyright (C) 2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.scheduling.quartz;

import static org.openbravo.scheduling.quartz.OpenbravoJDBCPersistenceSupport.getBooleanValue;
import static org.openbravo.scheduling.quartz.OpenbravoJDBCPersistenceSupport.setBooleanValue;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.quartz.JobDetail;
import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.CalendarIntervalTriggerPersistenceDelegate;
import org.quartz.impl.jdbcjobstore.SimplePropertiesTriggerProperties;
import org.quartz.impl.jdbcjobstore.Util;
import org.quartz.spi.OperableTrigger;

/**
 * Extends CalendarIntervalTriggerPersistenceDelegate because handling of Bool fields is not
 * extensible in TriggerPersistenceDelegates that use extended properties
 */
public class OpenbravoCalendarIntervalTriggerPersistenceDelegate
    extends CalendarIntervalTriggerPersistenceDelegate {

  @Override
  public int insertExtendedTriggerProperties(Connection conn, OperableTrigger trigger, String state,
      JobDetail jobDetail) throws SQLException, IOException {

    SimplePropertiesTriggerProperties properties = getTriggerProperties(trigger);

    PreparedStatement ps = null;

    try {
      ps = conn
          .prepareStatement(Util.rtp(INSERT_SIMPLE_PROPS_TRIGGER, tablePrefix, schedNameLiteral));
      ps.setString(1, trigger.getKey().getName());
      ps.setString(2, trigger.getKey().getGroup());
      ps.setString(3, properties.getString1());
      ps.setString(4, properties.getString2());
      ps.setString(5, properties.getString3());
      ps.setInt(6, properties.getInt1());
      ps.setInt(7, properties.getInt2());
      ps.setLong(8, properties.getLong1());
      ps.setLong(9, properties.getLong2());
      ps.setBigDecimal(10, properties.getDecimal1());
      ps.setBigDecimal(11, properties.getDecimal2());
      setBooleanValue(ps, 12, properties.isBoolean1());
      setBooleanValue(ps, 13, properties.isBoolean2());

      return ps.executeUpdate();
    } finally {
      Util.closeStatement(ps);
    }
  }

  @Override
  public TriggerPropertyBundle loadExtendedTriggerProperties(Connection conn, TriggerKey triggerKey)
      throws SQLException {

    PreparedStatement ps = null;
    ResultSet rs = null;

    try {
      ps = conn
          .prepareStatement(Util.rtp(SELECT_SIMPLE_PROPS_TRIGGER, tablePrefix, schedNameLiteral));
      ps.setString(1, triggerKey.getName());
      ps.setString(2, triggerKey.getGroup());
      rs = ps.executeQuery();

      if (rs.next()) {
        SimplePropertiesTriggerProperties properties = new SimplePropertiesTriggerProperties();

        properties.setString1(rs.getString(COL_STR_PROP_1));
        properties.setString2(rs.getString(COL_STR_PROP_2));
        properties.setString3(rs.getString(COL_STR_PROP_3));
        properties.setInt1(rs.getInt(COL_INT_PROP_1));
        properties.setInt2(rs.getInt(COL_INT_PROP_2));
        properties.setLong1(rs.getInt(COL_LONG_PROP_1));
        properties.setLong2(rs.getInt(COL_LONG_PROP_2));
        properties.setDecimal1(rs.getBigDecimal(COL_DEC_PROP_1));
        properties.setDecimal2(rs.getBigDecimal(COL_DEC_PROP_2));
        properties.setBoolean1(getBooleanValue(rs, COL_BOOL_PROP_1));
        properties.setBoolean2(getBooleanValue(rs, COL_BOOL_PROP_2));

        return getTriggerPropertyBundle(properties);
      }

      throw new IllegalStateException(
          "No record found for selection of Trigger with key: '" + triggerKey + "' and statement: "
              + Util.rtp(SELECT_SIMPLE_TRIGGER, tablePrefix, schedNameLiteral));
    } finally {
      Util.closeResultSet(rs);
      Util.closeStatement(ps);
    }
  }

  @Override
  public int updateExtendedTriggerProperties(Connection conn, OperableTrigger trigger, String state,
      JobDetail jobDetail) throws SQLException, IOException {

    SimplePropertiesTriggerProperties properties = getTriggerProperties(trigger);

    PreparedStatement ps = null;

    try {
      ps = conn
          .prepareStatement(Util.rtp(UPDATE_SIMPLE_PROPS_TRIGGER, tablePrefix, schedNameLiteral));
      ps.setString(1, properties.getString1());
      ps.setString(2, properties.getString2());
      ps.setString(3, properties.getString3());
      ps.setInt(4, properties.getInt1());
      ps.setInt(5, properties.getInt2());
      ps.setLong(6, properties.getLong1());
      ps.setLong(7, properties.getLong2());
      ps.setBigDecimal(8, properties.getDecimal1());
      ps.setBigDecimal(9, properties.getDecimal2());
      setBooleanValue(ps, 10, properties.isBoolean1());
      setBooleanValue(ps, 11, properties.isBoolean2());
      ps.setString(12, trigger.getKey().getName());
      ps.setString(13, trigger.getKey().getGroup());

      return ps.executeUpdate();
    } finally {
      Util.closeStatement(ps);
    }
  }

}
