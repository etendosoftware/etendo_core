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
package org.openbravo.modulescript;

import org.openbravo.database.ConnectionProvider;

/**
 * Build-time gate for stored computed column definitions (EPL-1807, Phase 5b). Runs on every
 * {@code update.database} and validates the whole-DB set of stored computed columns via
 * {@link StoredComputedValidator#assertDefinitionsValid(ConnectionProvider)}.
 *
 * <p>The validator throws its <b>own</b> {@link org.apache.tools.ant.BuildException} carrying the
 * aggregated, human-readable report, so this script deliberately does <b>not</b> wrap the call in
 * {@code handleError(...)} — that would replace the detailed report with the generic ModuleScript
 * failure message. In {@code ETGO_SCD_VALIDATION=warn} mode the validator logs and returns without
 * throwing, so the build proceeds.</p>
 *
 * <p>Read-only and idempotent: it inspects catalog + AD metadata but never writes.</p>
 */
public class ValidateStoredComputedColumns extends ModuleScript {

  @Override
  public void execute() {
    StoredComputedValidator.assertDefinitionsValid(getConnectionProvider());
  }
}
