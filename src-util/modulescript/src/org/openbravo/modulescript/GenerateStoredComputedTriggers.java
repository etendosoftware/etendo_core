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
 * All portions are Copyright (C) 2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.modulescript;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;

/**
 * Generates and deploys PostgreSQL AFTER triggers for every active AD_COLUMN_COMP_DEPENDENCY row.
 *
 * Each trigger writes a dirty-row record into AD_STOREDCOLUMN_DIRTY so the deferred computation
 * pass (Phase 4) knows which target rows need their stored computed column recalculated.
 *
 * Runs on every update.database call (no execution limits). Idempotent via CREATE OR REPLACE /
 * DROP IF EXISTS. Orphaned functions (whose dependency row was removed) are cleaned up
 * automatically.
 */
public class GenerateStoredComputedTriggers extends ModuleScript {

  private static final Logger log = LogManager.getLogger();

  private static final String QUERY_DEPS =
      "SELECT d.ad_column_comp_dependency_id, d.ad_column_id, d.insert_event, "
    + "       d.update_event, d.delete_event, d.watched_columns, d.target_id_resolver_sql, "
    + "       t.tablename AS source_table, c.refresh_mode, c.computation_sequence_number "
    + "FROM   ad_column_comp_dependency d "
    + "JOIN   ad_table  t ON t.ad_table_id  = d.source_table_id "
    + "JOIN   ad_column c ON c.ad_column_id = d.ad_column_id "
    + "WHERE  d.isactive = 'Y' AND c.isactive = 'Y' AND c.computation_mode = 'S' "
    + "ORDER  BY c.computation_sequence_number, d.ad_column_comp_dependency_id";

  private static final String QUERY_COL_NAME =
      "SELECT columnname FROM ad_column WHERE ad_column_id = ?";

  private static final String QUERY_DEPLOYED =
      "SELECT proname FROM pg_proc WHERE proname LIKE 'ad_scd_%_trf'";

  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      Set<String> activeDepIds = new HashSet<>();

      PreparedStatement psDeps = cp.getPreparedStatement(QUERY_DEPS);
      ResultSet rs = psDeps.executeQuery();
      while (rs.next()) {
        String depId       = rs.getString("ad_column_comp_dependency_id").toLowerCase();
        String columnId    = rs.getString("ad_column_id");
        String insertEvent = rs.getString("insert_event");
        String updateEvent = rs.getString("update_event");
        String deleteEvent = rs.getString("delete_event");
        String watchedCols = rs.getString("watched_columns");
        String resolverSql = rs.getString("target_id_resolver_sql");
        String sourceTable = rs.getString("source_table").toLowerCase();
        String refreshMode = rs.getString("refresh_mode");
        int    seqNo       = rs.getInt("computation_sequence_number");

        if (refreshMode == null) {
          log.warn("Skipping SCD dependency {} — target column {} has no REFRESH_MODE set",
              depId, columnId);
          continue;
        }

        activeDepIds.add(depId);
        String funcName    = "ad_scd_" + depId + "_trf";
        String triggerName = "ad_scd_" + depId + "_trg";

        cp.getPreparedStatement(
            buildFunctionDdl(funcName, resolverSql, columnId, refreshMode, seqNo)).execute();

        String eventClause = buildEventClause(cp, insertEvent, updateEvent, deleteEvent,
            watchedCols);

        cp.getPreparedStatement(
            "DROP TRIGGER IF EXISTS " + triggerName + " ON " + sourceTable).execute();
        cp.getPreparedStatement(
            "CREATE TRIGGER " + triggerName
            + " AFTER " + eventClause + " ON " + sourceTable
            + " FOR EACH ROW EXECUTE FUNCTION " + funcName + "()").execute();

        log.info("Deployed SCD trigger {} on table {}", triggerName, sourceTable);
      }
      cp.releasePreparedStatement(psDeps);

      dropOrphanedFunctions(cp, activeDepIds);

    } catch (Exception e) {
      handleError(e);
    }
  }

  private void dropOrphanedFunctions(ConnectionProvider cp, Set<String> activeDepIds)
      throws Exception {
    PreparedStatement psDeployed = cp.getPreparedStatement(QUERY_DEPLOYED);
    ResultSet rsDep = psDeployed.executeQuery();
    while (rsDep.next()) {
      String proname = rsDep.getString(1);
      // strip "ad_scd_" prefix (7 chars) and "_trf" suffix (4 chars)
      String depId = proname.substring(7, proname.length() - 4);
      if (!activeDepIds.contains(depId)) {
        cp.getPreparedStatement("DROP FUNCTION IF EXISTS " + proname + "() CASCADE").execute();
        log.info("Dropped orphaned SCD function {}", proname);
      }
    }
    cp.releasePreparedStatement(psDeployed);
  }

  private String buildFunctionDdl(String funcName, String resolverSql,
      String columnId, String refreshMode, int seqNo) {
    return "CREATE OR REPLACE FUNCTION " + funcName + "()\n"
        + "RETURNS TRIGGER AS $$\n"
        + "DECLARE\n"
        + "  v_target_id VARCHAR(32);\n"
        + "BEGIN\n"
        + "  IF current_setting('my.triggers_disabled', true) = 'Y' THEN\n"
        + "    RETURN COALESCE(NEW, OLD);\n"
        + "  END IF;\n"
        + "  FOR v_target_id IN (\n"
        + "    " + resolverSql + "\n"
        + "  ) LOOP\n"
        + "    INSERT INTO AD_STOREDCOLUMN_DIRTY (\n"
        + "      AD_STOREDCOLUMN_DIRTY_ID, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE,\n"
        + "      CREATED, CREATEDBY, UPDATED, UPDATEDBY,\n"
        + "      AD_COLUMN_ID, TARGET_RECORD_ID, TRANSACTION_ID,\n"
        + "      REFRESH_MODE, COMPUTATION_SEQUENCE_NUMBER\n"
        + "    ) VALUES (\n"
        + "      get_uuid(), '0', '0', 'Y', NOW(), '0', NOW(), '0',\n"
        + "      '" + columnId + "', v_target_id, pg_current_xact_id()::bigint,\n"
        + "      '" + refreshMode + "', " + seqNo + "\n"
        + "    ) ON CONFLICT DO NOTHING;\n"
        + "  END LOOP;\n"
        + "  RETURN COALESCE(NEW, OLD);\n"
        + "END;\n"
        + "$$ LANGUAGE plpgsql";
  }

  private String buildEventClause(ConnectionProvider cp,
      String insertEvent, String updateEvent, String deleteEvent,
      String watchedCols) throws Exception {
    List<String> parts = new ArrayList<>();
    if ("Y".equals(insertEvent)) {
      parts.add("INSERT");
    }
    if ("Y".equals(updateEvent)) {
      if (watchedCols != null && !watchedCols.isBlank()) {
        List<String> colNames = new ArrayList<>();
        for (String colId : watchedCols.split(",")) {
          String trimmed = colId.trim();
          if (trimmed.isEmpty()) {
            continue;
          }
          PreparedStatement ps = cp.getPreparedStatement(QUERY_COL_NAME);
          ps.setString(1, trimmed);
          ResultSet rs = ps.executeQuery();
          if (rs.next()) {
            colNames.add(rs.getString(1).toLowerCase());
          }
          cp.releasePreparedStatement(ps);
        }
        if (!colNames.isEmpty()) {
          parts.add("UPDATE OF " + String.join(", ", colNames));
        } else {
          parts.add("UPDATE");
        }
      } else {
        parts.add("UPDATE");
      }
    }
    if ("Y".equals(deleteEvent)) {
      parts.add("DELETE");
    }
    return String.join(" OR ", parts);
  }
}
