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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.modulescript;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;

public class UpdatePersistOrgInfo extends ModuleScript {

  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      boolean isPersistOrgInfoUpdated = UpdatePersistOrgInfoData.isPersistOrgInfoUpdated(cp);
      if (!isPersistOrgInfoUpdated) {
        UpdatePersistOrgInfoData[] organizations = UpdatePersistOrgInfoData.selectOrgs(cp);
        for (UpdatePersistOrgInfoData org : organizations) {
          String periodControlAllowedOrg = UpdatePersistOrgInfoData.getPeriodControlAllowedOrg(cp,
              org.adOrgId);
          String calendarOwnerOrg = UpdatePersistOrgInfoData.getCalendarOwnerOrg(cp, org.adOrgId);
          String calendar = UpdatePersistOrgInfoData.getCalendar(cp, calendarOwnerOrg);
          String legalEntity = UpdatePersistOrgInfoData.getLegalEntityBusinessUnitOrg(cp,
              org.adOrgId, "LE");
          String businessUnit = UpdatePersistOrgInfoData.getLegalEntityBusinessUnitOrg(cp,
              org.adOrgId, "BU");
          UpdatePersistOrgInfoData.updatePersistOrgInfo(cp, periodControlAllowedOrg,
              calendarOwnerOrg, calendar, legalEntity, businessUnit, org.adOrgId);
        }
        UpdatePersistOrgInfoData.createPreferencePersistOrgInfoUpdated(cp);
      }
    } catch (Exception e) {
      handleError(e);
    }
  }

  @Override
  protected boolean executeOnInstall() {
    return true;
  }

  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, new OpenbravoVersion(3, 0, 33898));
  }
}
