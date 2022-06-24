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
 * All portions are Copyright (C) 2013-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.model;

import static org.junit.Assert.assertEquals;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.base.OBBaseTest;

/**
 * Verifies Oracle types and length of TRL tables are equal to their counterpart in the base table.
 * 
 * This is mainly intended to catch NVARCHAR vs VARCHAR differences
 * 
 * @author alostale
 * 
 */
public class TrlColumnsOraTypeTest extends OBBaseTest {
  final static private Logger log = LogManager.getLogger();

  final static int TABLE_NAME = 1;
  final static int COLUMN_NAME = 2;
  final static int TRL_TYPE = 3;
  final static int BASE_TYPE = 4;
  final static int TRL_LENGTH = 5;
  final static int BASE_LENGTH = 6;

  @Test
  public void testTrlColumnsOraType() {
    DalConnectionProvider cp = new DalConnectionProvider(false);
    if (!cp.getRDBMS().equals("ORACLE")) {
      // do nothing, Oracle specific test
      return;
    }

    String sql = "select c1.table_name, c1.column_name, c1.data_type, c2.data_type, c1.data_length, c2.data_length" //
        + "         from user_tab_cols c1, user_tab_cols c2" //
        + "        where c1.table_name like '%TRL'" //
        + "          and c2.table_name = substr(c1.table_name,1, length(c1.table_name)-4)" //
        + "          and c2.column_name = c1.column_name" //
        + "          and (c2.data_type != c1.data_type or c1.data_length != c2.data_length)";

    List<String> errors = new ArrayList<String>();
    PreparedStatement sqlQuery = null;
    ResultSet rs = null;
    try {
      sqlQuery = cp.getPreparedStatement(sql);
      sqlQuery.execute();
      rs = sqlQuery.getResultSet();
      while (rs.next()) {
        String msg = "TRL table " + rs.getString(TABLE_NAME)
            + " has different type/length for column " + rs.getString(COLUMN_NAME) + "\n";
        msg += "   Type in base table: " + rs.getString(BASE_TYPE) + " ("
            + rs.getString(BASE_LENGTH) + ")\n";
        msg += "   Type in trl table:  " + rs.getString(TRL_TYPE) + " (" + rs.getString(TRL_LENGTH)
            + ")";

        errors.add(msg);
      }
      for (String error : errors) {
        log.error(error);
      }
      assertEquals("There are columns in TRL tables not matching Oracle types", 0, errors.size());
    } catch (Exception e) {
      log.error("Error executing query", e);
    } finally {
      try {
        if (sqlQuery != null) {
          sqlQuery.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {
        log.error("Error when closing statement", e);
      }
    }
  }
}
