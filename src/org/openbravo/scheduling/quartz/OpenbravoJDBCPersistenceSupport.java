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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Provides utility methods related with the JDBC persistence
 */
public class OpenbravoJDBCPersistenceSupport {

  public static final String TRUE_STRING = "Y";
  public static final String FALSE_STRING = "N";

  private OpenbravoJDBCPersistenceSupport() {
  }

  /**
   * Sets the designated parameter to the given Java <code>String</code> value, after replacing a
   * boolean with its String representation
   */
  public static void setBooleanValue(PreparedStatement ps, int index, boolean val)
      throws SQLException {
    ps.setString(index, val ? TRUE_STRING : FALSE_STRING);
  }

  /**
   * Given a ResultSet and a columnName, returns a boolean after replacing the string contained in
   * the ResultSet by its boolean representation
   */
  public static boolean getBooleanValue(ResultSet rs, String columnName) throws SQLException {
    return TRUE_STRING.equals(rs.getString(columnName));
  }

  /**
   * Given a ResultSet and a column index, returns a boolean after replacing the string contained in
   * the ResultSet by its boolean representation
   */
  public static boolean getBooleanValue(ResultSet rs, int columnIndex) throws SQLException {
    return TRUE_STRING.equals(rs.getString(columnIndex));
  }

}
