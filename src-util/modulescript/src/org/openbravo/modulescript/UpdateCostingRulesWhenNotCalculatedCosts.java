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
 * All portions are Copyright (C) 2012-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.modulescript;

import java.sql.PreparedStatement;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;

public class UpdateCostingRulesWhenNotCalculatedCosts extends ModuleScript {

  public void execute() {
    try {
      String query = " UPDATE m_costing_rule ";
      query = query + "SET isvalidated = 'N' ";
      query = query + "WHERE m_costing_rule_id IN ";
      query = query + "     (SELECT  m_costing_rule_id ";
      query = query + "      FROM m_costing_rule cr ";
      query = query + "        JOIN (SELECT ad_org_id, MIN(datefrom) AS startingdate ";
      query = query + "              FROM m_costing_rule ";
      query = query + "              GROUP BY ad_org_id) mcr ON cr.ad_org_id = mcr.ad_org_id ";
      query = query + "      WHERE EXISTS ";
      query = query + "             (SELECT 1 ";
      query = query + "              FROM m_transaction trx ";
      query = query + "              WHERE ad_get_org_le_bu(trx.ad_org_id, 'LE') = cr.ad_org_id ";
      query = query + "                AND trx.iscostcalculated = 'N' ";
      query = query + "                AND trxprocessdate < mcr.startingdate)) ";
      ConnectionProvider cp = getConnectionProvider();
      PreparedStatement ps = cp.getPreparedStatement(query);
      ps.executeUpdate();
    } catch (Exception e) {
      handleError(e);
    }
  }
  
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, 
        new OpenbravoVersion(3,0,17837));
  }
}
