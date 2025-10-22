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
 * All portions are Copyright (C) 2014-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.modulescript;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.ServletException;

import org.openbravo.database.ConnectionProvider;

/*
 * Related to issue #26826: Fix allow period control flag
 */
public class Issue26826_Org_AllowPeriodControl extends ModuleScript {
  private static final Logger log4j = LogManager.getLogger();
  
  @Override
  // Sets AD_Org.AllowPeriodControl = N where organization type is 
  // neither a business unit nor a legal entity with accounting
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      boolean isModuleScriptExecuted= Issue26826OrgAllowPeriodControlData.isModuleScriptExecuted(cp);
      if (!isModuleScriptExecuted) {
        int updatedOrgs = Issue26826OrgAllowPeriodControlData.updateOrganizations(cp);
        if (updatedOrgs > 0 ) {
          log4j.info("Fixed " + updatedOrgs+ " organizations. ");
        }
        Issue26826OrgAllowPeriodControlData.createPreference(cp);
      }
    } catch (Exception e) {
      handleError(e);
    }
  }
  
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, 
        new OpenbravoVersion(3,0,24733));
  }
}