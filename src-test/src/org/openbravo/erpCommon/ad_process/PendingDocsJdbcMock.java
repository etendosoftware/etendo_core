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
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.ad_process;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mocks the JDBC connection used by the pending documents pre-filter of
 * {@link AcctServerProcess#clientsWithPendingDocs}.
 */
final class PendingDocsJdbcMock {

  private PendingDocsJdbcMock() {
  }

  /**
   * @param tableNames
   *          AD_Table_ID to table name, as read from AD_Table
   * @param pendingByTable
   *          table name to the client ids with pending documents, or to the SQLException its query
   *          throws
   * @param sqlSink
   *          receives every pending documents query
   */
  static Connection mockJdbc(Map<String, String> tableNames, Map<String, Object> pendingByTable,
      List<String> sqlSink) throws SQLException {
    Connection conn = mock(Connection.class);
    AtomicReference<String> tableIdParam = new AtomicReference<>();
    PreparedStatement namePs = mock(PreparedStatement.class);
    doAnswer(inv -> {
      tableIdParam.set(inv.getArgument(1));
      return null;
    }).when(namePs).setString(eq(1), anyString());
    when(namePs.executeQuery()).thenAnswer(inv -> mockResultSet(
        Collections.singletonList(tableNames.get(tableIdParam.get()))));
    when(conn.prepareStatement(anyString())).thenAnswer(inv -> {
      String sql = inv.getArgument(0);
      if (sql.startsWith("SELECT tablename")) {
        return namePs;
      }
      sqlSink.add(sql);
      for (Map.Entry<String, Object> entry : pendingByTable.entrySet()) {
        if (sql.contains(" FROM " + entry.getKey() + " ")) {
          PreparedStatement ps = mock(PreparedStatement.class);
          if (entry.getValue() instanceof SQLException) {
            when(ps.executeQuery()).thenThrow((SQLException) entry.getValue());
          } else {
            @SuppressWarnings("unchecked")
            List<String> clients = (List<String>) entry.getValue();
            ResultSet rs = mockResultSet(clients);
            when(ps.executeQuery()).thenReturn(rs);
          }
          return ps;
        }
      }
      throw new AssertionError("Unexpected SQL: " + sql);
    });
    return conn;
  }

  private static ResultSet mockResultSet(List<String> values) throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    Iterator<String> it = values.iterator();
    AtomicReference<String> current = new AtomicReference<>();
    when(rs.next()).thenAnswer(inv -> {
      if (!it.hasNext()) {
        return false;
      }
      current.set(it.next());
      return true;
    });
    when(rs.getString(1)).thenAnswer(inv -> current.get());
    return rs;
  }
}
