package org.openbravo.modulescript;

import java.sql.PreparedStatement;
import java.sql.Connection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;

/**
 * Module script that backfills {@code C_INVOICETAX.ORIGTAXAMT} with the current
 * {@code TAXAMT} for existing rows where the original tax amount is not set (null or zero).
 */
public class BackfillInvoiceTaxOrigAmt extends ModuleScript {
  private static final Logger log = LogManager.getLogger();

  /**
   * Executes the backfill
   * If an exception occurs, it is handed to {@link #handleError(Exception)}.
   */
  @Override
  public void execute() {
    log.info("Actualizando impuesto original");
    ConnectionProvider cp = getConnectionProvider();
    PreparedStatement ps = null;
    try {
      final String sql =
        "UPDATE c_invoicetax " + 
        "SET origtaxamt = taxamt " +
        "WHERE COALESCE(origtaxamt, 0) = 0";
      ps = cp.getPreparedStatement(sql);
      int updated = ps.executeUpdate();
      releasePreparedStatement(ps);
      log.info("Cantidad de impuestos actualizados: {}", updated);
    } catch (Exception e) {
      handleError(e);
    }
  }

  /**
   * Returns the execution limits for this module script.
   * @return a {@link ModuleScriptExecutionLimits} with no restrictions
   */
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits(null, null, null);
  }
}
