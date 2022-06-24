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
 * All portions are Copyright (C) 2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.buildvalidation;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.base.ExecutionLimits;
import org.openbravo.buildvalidation.BuildValidation;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.OpenbravoVersion;

/**
 * This build validation is related to Old Costing Cleanup
 * https://issues.openbravo.com/view.php?id=41421
 */

public class CostingMigrationCheck extends BuildValidation {
  @Override
  public List<String> execute() {
    ConnectionProvider cp = getConnectionProvider();
    ArrayList<String> errors = new ArrayList<String>();
    try {
      if (!CostingMigrationCheckData.existsMigrationPreference(cp)
          && CostingMigrationCheckData.existsCostingData(cp)
          && (!CostingMigrationCheckData.existsCostingRuleTable(cp)
          || !CostingMigrationCheckData.existsCostingRuleData(cp))) {
        errors.add(
            "Legacy cost engine is no longer supported. It is mandatory to run Costing Migration Process before updating the instance.");
      }
    } catch (Exception e) {
      return handleError(e);
    }
    return errors;
  }

  @Override
  protected ExecutionLimits getBuildValidationLimits() {
    return new ExecutionLimits("0", null, new OpenbravoVersion(3, 0, 36622));
  }
}
