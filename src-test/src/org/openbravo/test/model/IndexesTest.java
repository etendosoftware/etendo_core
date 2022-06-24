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
 * All portions are Copyright (C) 2013-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.model;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

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
 * Tests cases to warranty standard database indexes from child to parent columns are present.
 * 
 * @author alostale
 * 
 */
public class IndexesTest extends OBBaseTest {
  final static private Logger log = LogManager.getLogger();

  /**
   * Verifies in subtabs that their tables have a index to for the FK column linking to their parent
   * table.
   */
  @Test
  public void testSubTabs() {
    int windowName = 1;
    int parentTab = 2;
    int parentTable = 3;
    int childTab = 4;
    int childTable = 5;
    int otherIndexes = 6;

    String sql = "select *" //
        + " from (" //
        + "    select w.name as window_name," //
        + "           t1.name as parentTab," //
        + "           ptb.tablename as parentTable," //
        + "           t2.name as childTab," //
        + "           ctb.tablename as childTable," //
        + "          (select count(*)" //
        + "              from user_ind_columns" //
        + "             where table_name = upper(ctb.tablename)" //
        + "               and column_name = upper(ptb.tablename)||'_ID') as indexes_to_parent_any_col," //
        + "           (select count(*)" //
        + "              from user_ind_columns" //
        + "             where table_name = upper(ctb.tablename)" //
        + "               and column_name = upper(ptb.tablename)||'_ID'" //
        + "               and column_position = 1) as indexes_to_parent_first_col" //
        + "      from ad_tab t1," //
        + "           ad_tab t2," //
        + "           ad_table ptb," //
        + "           ad_table ctb," //
        + "           ad_window w," //
        + "           (select ct.ad_tab_id as child, " //
        + "                   (select pt.ad_tab_id" //
        + "                      from ad_tab pt" //
        + "                     where pt.ad_window_id = ct.ad_window_id" //
        + "                       and pt.tablevel = (select max(tablevel)" //
        + "                                            from ad_tab wt" //
        + "                                           where wt.ad_window_id = pt.ad_window_id" //
        + "                                             and wt.seqno < ct.seqno" //
        + "                                             and wt.tablevel < ct.tablevel)" //
        + "                       and pt.seqno    = (select max(seqno)" //
        + "                                            from ad_tab wt" //
        + "                                           where wt.ad_window_id = pt.ad_window_id" //
        + "                                             and wt.seqno < ct.seqno" //
        + "                                             and wt.tablevel = (select max(tablevel)" //
        + "                                                      from ad_tab wt" //
        + "                                                     where wt.ad_window_id = pt.ad_window_id" //
        + "                                                       and wt.seqno < ct.seqno" //
        + "                                                       and wt.tablevel < ct.tablevel))) as parent" //
        + "              from ad_tab ct, ad_table t" //
        + "             where tablevel > 0" //
        + "               and ct.ad_table_id = t.ad_table_id" //
        + "               and t.dataorigintype = 'Table' " //
        + "               and t.isview ='N') rel" //
        + "     where rel.child = t2.ad_tab_id" //
        + "       and t2.ad_table_id = ctb.ad_table_id" //
        + "       and rel.parent = t1.ad_tab_id" //
        + "       and t1.ad_table_id = ptb.ad_table_id" //
        + "       and t1.ad_window_id = w.ad_window_id" //
        + "       and exists (select 1" //
        + "                     from ad_column c" //
        + "                    where c.ad_table_id = ctb.ad_table_id" //
        + "                      and upper(columnname) = upper(ptb.tablename)||'_ID')" //
        + ") i" //
        + " where indexes_to_parent_first_col = 0" //
        + " order by parenttable";

    @SuppressWarnings("serial")
    List<String> errors = new ArrayList<String>() {
      @Override
      public String toString() {
        StringBuilder s = new StringBuilder();
        for (String e : this) {
          s.append("\n   ").append(e);
        }
        return s.toString();
      }
    };
    PreparedStatement sqlQuery = null;
    ResultSet rs = null;
    try {
      sqlQuery = new DalConnectionProvider(false).getPreparedStatement(sql);
      sqlQuery.execute();

      rs = sqlQuery.getResultSet();
      while (rs.next()) {
        String msg = "Missing index in " + rs.getString(childTable) + "."
            + rs.getString(parentTable) + "_ID. Because of child tab relationship in window "
            + rs.getString(windowName) + " from tab " + rs.getString(childTab) + " to "
            + rs.getString(parentTab) + ". ";

        int otherIndexesCount = rs.getInt(otherIndexes);
        if (otherIndexesCount > 0) {
          msg += "There are other " + otherIndexesCount
              + " indexes or unique constraints including that column. You might recheck columns position for them.";
        }
        errors.add(msg);
      }

      assertThat("No subtabs without index to parent tab:" + errors, errors, hasSize(0));
    } catch (Exception e) {
      log.error("Error when executing query", e);
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
