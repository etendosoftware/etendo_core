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
 * All portions are Copyright © 2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.ad_process;

import java.sql.Connection;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;

/**
 * Manual, user-startable process that fully rebuilds one stored computed column by recomputing every
 * target row through the shared Java engine {@link StoredColumnRecomputer#rebuild}.
 *
 * <p>It is the operator entry point for post-migration repair and on-demand initial population —
 * complementary to the automatic first-activation population done by
 * {@code GenerateStoredComputedTriggers} and the async {@link StoredColumnQueueProcessor}. Because the
 * rebuild re-derives every target row from its current dependencies, running it again is always
 * safe.</p>
 *
 * <p>The rebuild runs in Java rather than calling the PL/pgSQL {@code ad_scd_rebuild}, so it works
 * identically on PostgreSQL and Oracle — Oracle has no engine PL/SQL functions deployed. It is the
 * dialect-neutral equivalent of that function's per-row recompute loop.</p>
 *
 * <p>The single parameter {@code AD_Column_ID} identifies the stored computed column (the
 * {@code Computation_Mode='S'} column whose value is recomputed). The whole rebuild runs in one
 * transaction. On PostgreSQL it engages the transaction-local {@code my.scd_refreshing} GUC, which
 * suppresses the re-entrant enqueue that its <em>synchronous</em> recompute would otherwise cause.
 * Oracle has <b>no</b> runtime recursion guard — it deliberately does not honour Etendo's global
 * trigger-disable — because it does not need one: re-enqueue from the engine's own write is
 * prevented structurally (the recompute writes a target column that is never in its own watched
 * set, and the watched-column value guard rejects no-op writes), and dependency cycles that could
 * otherwise loop the async {@code 'Q'} drain are rejected at validation time by rule V14
 * ({@code ETGO_ScdDependencyCycle}, a hard error). Either way the engine's own writes to the target
 * table do not re-enqueue dirty rows.</p>
 */
public class StoredColumnRebuild implements Process {

  private static final Logger log4j = LogManager.getLogger();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    ProcessLogger logger = bundle.getLogger();
    String columnId = strParam(bundle, "AD_Column_ID");
    if (columnId == null || columnId.isEmpty()) {
      throw new OBException("Stored column rebuild requires the AD_Column_ID parameter");
    }

    ConnectionProvider conn = bundle.getConnection();
    boolean oracle = "ORACLE".equals(conn.getRDBMS());
    StoredColumnRecomputer recomputer = new StoredColumnRecomputer(oracle);
    Connection con = conn.getTransactionConnection();
    try {
      // Suppress re-entrant enqueues from the engine's own synchronous writes during the rebuild
      // (PostgreSQL only; the transaction-local GUC is released when the transaction ends).
      recomputer.setRefreshingGuard(con);

      int rebuilt = recomputer.rebuild(con, columnId);

      conn.releaseCommitConnection(con);

      String summary = "Stored column rebuild: " + rebuilt + " row(s) recomputed for column "
          + columnId;
      if (logger != null) {
        logger.logln(summary);
      }
      log4j.info(summary);
    } catch (Exception e) {
      try {
        conn.releaseRollbackConnection(con);
      } catch (Exception rollbackEx) {
        log4j.error("Could not roll back stored-column rebuild transaction", rollbackEx);
      }
      throw new OBException("Stored computed column rebuild failed for column " + columnId, e);
    }
  }

  /** Reads a string process parameter, returning null when the bundle or value is absent. */
  private String strParam(ProcessBundle bundle, String name) {
    if (bundle == null) {
      return null;
    }
    Map<String, Object> params = bundle.getParams();
    if (params == null) {
      return null;
    }
    Object raw = params.get(name);
    if (raw == null) {
      return null;
    }
    String text = raw.toString().trim();
    return text.isEmpty() ? null : text;
  }
}
