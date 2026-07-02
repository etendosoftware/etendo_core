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
package org.openbravo.erpCommon.businessUtility;

/**
 * Handler interface for module-provided accounting setup during Initial Organization Setup.
 * Implementations are discovered through CDI and executed by priority when applicable.
 */
public interface InitialOrgSetupAccountingHandler {
  /**
   * Determines execution order when more than one handler can process the context.
   *
   * @return priority value; lower values run first
   */
  default int getPriority() {
    return 100;
  }

  /**
   * Determines whether this handler should process the supplied setup context.
   *
   * @param context setup data for the organization being initialized
   * @return {@code true} when this handler can process the context
   */
  boolean applies(InitialOrgSetupAccountingContext context);

  /**
   * Executes accounting setup for the supplied context.
   *
   * @param context setup data for the organization being initialized
   * @return explicit result indicating whether the handler processed the context and succeeded
   */
  InitialOrgSetupAccountingResult wire(InitialOrgSetupAccountingContext context);
}
