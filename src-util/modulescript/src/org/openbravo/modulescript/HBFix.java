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

/**
 * @deprecated Heartbeat is scheduled by {@code org.openbravo.listeners.HeartbeatListener}. This
 *             legacy module script is intentionally kept as a no-op to avoid recreating the old
 *             process request.
 */
@SuppressWarnings("java:S1133") // intentional no-op — see class javadoc
@Deprecated
public class HBFix extends ModuleScript {

  @Override
  public void execute() {
    // Intentionally empty.
  }
}
