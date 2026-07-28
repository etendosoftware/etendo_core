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
 * All portions are Copyright © 2021–2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */

package org.openbravo.modulescript;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;

/**
 * Populates C_POC_CONFIGURATION.EMAIL_CONF_AD_CLIENT_ID with the value of AD_CLIENT_ID for
 * records that pre-date ETP-2816 and therefore have the three cascade discriminator columns
 * (EMAIL_CONF_AD_CLIENT_ID, EMAIL_CONF_AD_ORG_ID, EMAIL_CONF_AD_USER_ID) set to NULL.
 * Without this migration, those records become invisible in the Email Configuration tab
 * after upgrading to 26.1.x because the tab WHERE clause filters by EMAIL_CONF_AD_CLIENT_ID.
 */
public class UpdateEmailConfigClientId extends ModuleScript {

  private static final Logger log4j = LogManager.getLogger();

  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      long init = System.currentTimeMillis();
      int updated = UpdateEmailConfigClientIdData.updateEmailConfigClientId(cp);
      log4j.info("Updated " + updated + " email configuration record(s) with EMAIL_CONF_AD_CLIENT_ID in "
          + (System.currentTimeMillis() - init) + " ms.");
    } catch (Exception e) {
      handleError(e);
    }
  }

  @Override
  protected boolean executeOnInstall() {
    return false;
  }
}
