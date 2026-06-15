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
 * All portions are Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.modulescript;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;

/**
 * Install-time enforcement of read-only on fields backed by stored computed columns (EPL-1807).
 *
 * <p>Marks {@code AD_Field.ISREADONLY = 'Y'} for every field whose {@code AD_Column} has
 * {@code Computation_Mode = 'S'}. Runs on every {@code update.database} (no execution limits) and is
 * idempotent — it only touches rows still flagged {@code 'N'}. The {@link ADFieldStoredComputedHandler}
 * event handler covers subsequent saves; this script fixes pre-existing field metadata.</p>
 */
public class EnforceStoredComputedReadOnly extends ModuleScript {

  private static final Logger log = LogManager.getLogger();

  private static final String SQL =
      "UPDATE ad_field f "
    + "SET    isreadonly = 'Y', updated = now(), updatedby = '0' "
    + "FROM   ad_column c "
    + "WHERE  f.ad_column_id = c.ad_column_id "
    + "AND    c.computation_mode = 'S' "
    + "AND    f.isreadonly = 'N'";

  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      int updated = cp.getPreparedStatement(SQL).executeUpdate();
      if (updated > 0) {
        log.info("Marked {} AD_Field row(s) read-only for stored computed columns", updated);
      }
    } catch (Exception e) {
      handleError(e);
    }
  }
}
