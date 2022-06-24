/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.modulescript;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

public class GrantAccessToProcessDefinition extends ModuleScript {
  private static final String AD_PROCESS_ACCESS_TABLE_ID = "197";
  private static final String NEW_PURCHASE_ORDER_REPORT_ID = "4BDE0AF5E8C44B6C9575E388AAECDF69";
  private static final String OLD_PURCHASE_ORDER_REPORT_ID = "800171";
  private static final String MANUAL_ACTION_TITLE = "Grant access to %s";
  private static final String MANUAL_ACTION_MESSAGE = "Role '%s (%s)' has access to legacy report '%s'. You should either apply the dataset update on module '%s' or manually grant access to new Process Definition, otherwise users belonging to this role won't be able to launch the report anymore.";

  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      String[] newIdArray = { NEW_PURCHASE_ORDER_REPORT_ID };
      String[] oldIdArray = { OLD_PURCHASE_ORDER_REPORT_ID };
      int autoUpdated = 0;

      for (int i = 0; i < newIdArray.length; i++) {
        String newId = newIdArray[i];
        String oldId = oldIdArray[i];

        autoUpdated = autoUpdated
            + GrantAccessToProcessDefinitionData.grantAccess(cp, newId, oldId,
                AD_PROCESS_ACCESS_TABLE_ID);

        createAlert(cp, oldId);
      }

      if (autoUpdated > 0) {
        log4j.info("Manual roles automatically updated: " + autoUpdated);
      }

    } catch (Exception e) {
      handleError(e);
    }
  }

  private void createAlert(ConnectionProvider cp, String oldId) throws ServletException {
    GrantAccessToProcessDefinitionData[] rolesToBeUpdated = GrantAccessToProcessDefinitionData
        .getRolesToBeUpdated(cp, AD_PROCESS_ACCESS_TABLE_ID, oldId);

    for (GrantAccessToProcessDefinitionData roleToBeUpdated : rolesToBeUpdated) {
      String clientId = roleToBeUpdated.getField("client_id");
      String clientName = roleToBeUpdated.getField("client_name");
      String roleId = roleToBeUpdated.getField("role_id");
      String roleName = roleToBeUpdated.getField("role_name");
      String processName = roleToBeUpdated.getField("process_name");
      String moduleName = roleToBeUpdated.getField("module_name");
      String title = String.format(MANUAL_ACTION_TITLE, processName);
      String msg = String.format(MANUAL_ACTION_MESSAGE, roleName, clientName, processName,
          moduleName);

      // If exists an alert rule for client, use it, else create a new one
      String alertRuleId = GrantAccessToProcessDefinitionData.getAlertRule(cp, clientId, title);
      if (StringUtils.isEmpty(alertRuleId)) {
        GrantAccessToProcessDefinitionData.createAlertRule(cp, clientId, title);
        alertRuleId = GrantAccessToProcessDefinitionData.getAlertRule(cp, clientId, title);
      }

      // Create the alert
      GrantAccessToProcessDefinitionData.createAlert(cp, clientId, msg, alertRuleId, roleId,
          roleName);

      // Register the affected role as recipient
      GrantAccessToProcessDefinitionData.createRecipients(cp, clientId, alertRuleId, roleId);

      // Register the client's admin role as recipient too if it doesn't exist
      String adminRoleId = GrantAccessToProcessDefinitionData.getAdminRole(cp, clientId);
      if (StringUtils.isNotEmpty(adminRoleId)
          && !GrantAccessToProcessDefinitionData.existRecipient(cp, clientId, alertRuleId,
              adminRoleId)) {
        GrantAccessToProcessDefinitionData.createRecipients(cp, clientId, alertRuleId, adminRoleId);
      }
    }
  }

  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, new OpenbravoVersion(3, 0, 32271));
  }
}
