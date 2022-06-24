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
 * All portions are Copyright (C) 2015-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.buildvalidation;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.ExecutionLimits;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.OpenbravoVersion;

/**
 * This build validation prevents issue #29127 by deleting rows in ad_process_run for executions of
 * CostingRuleProcess which is removed but due to issue #29142 update database process is not able
 * to properly handle.
 * 
 * It is a temporary workaround and it should be removed one #29142 is resolved.
 * 
 * It should have been implemented as ModuleScript because it is repairing data, but because
 * ad_process_run and ad_process_request tables are recreated when updating from PR14Q3 to PR14Q4,
 * module script would be executed without indexes nor DB statistics which might result, depending
 * on the data volumes in these table, in slow executions due to poor query execution plans.
 * Exceptionally, implementing it as build validation to be executed before actual database update
 * allowing better plans.
 * 
 * @author alostale
 * 
 */
public class RemoveCostingRuleProcessExecutions extends BuildValidation {
  private static Logger log = LogManager.getLogger();

  @Override
  public List<String> execute() {
    ConnectionProvider cp = getConnectionProvider();

    try {
      // CostingRuleProcess is removed in PR14Q4, check if it is present to remove data if so
      if (RemoveCostingRuleProcessExecutionsData.isCostingRuleProcessPresent(cp)) {
        int deletedRows = RemoveCostingRuleProcessExecutionsData.removeCostingRuleExecutions(cp);
        if (deletedRows > 0) {
          log.info("Deleted " + deletedRows
              + " rows in ad_process_run for execution of CostingRuleProcess");
        }
      }
    } catch (Exception e) {
      handleError(e);
    }

    return new ArrayList<String>();
  }

  @Override
  protected ExecutionLimits getBuildValidationLimits() {
    return new ExecutionLimits("0", null, new OpenbravoVersion(3, 0, 25161));
  }
}
