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
 * All portions are Copyright (C) 2013-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.modulescript;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;

/**
 * 
 * @author dmiguelez
 */
public class UpdateOpenClosePeriodsButton extends ModuleScript {

  private static final Logger log4j = LogManager.getLogger();

  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      // If the preference does not exist in the database yet the modulescript must be executed.
      boolean isUpdated = UpdateOpenClosePeriodsButtonData.isUpdated(cp);
      if (!isUpdated) {
        UpdateOpenClosePeriodsButtonData.updatePeriodControl(cp);
        UpdateOpenClosePeriodsButtonData.updatePeriod(cp);
        // Update entries in Fact Acct Type that have a Accounting Group related to a Divide Up
        UpdateOpenClosePeriodsButtonData.updateFactAcctTypeForDivideUp(cp);
        UpdateOpenClosePeriodsButtonData.createPreference(cp);
      }
      boolean isUpdated2 = UpdateOpenClosePeriodsButtonData.isUpdated2(cp);
      if (!isUpdated2){
        UpdateOpenClosePeriodsButtonData.updateFactAcctTypeForDivideUp(cp); 
        UpdateOpenClosePeriodsButtonData.createPreference2(cp);
      }
    } catch (Exception e) {
      handleError(e);
    }
  }
  
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, 
        new OpenbravoVersion(3,0,20433));
  }

}
