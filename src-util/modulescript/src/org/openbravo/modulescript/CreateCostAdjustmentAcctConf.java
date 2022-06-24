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
 * All portions are Copyright (C) 2014-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.modulescript;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;

public class CreateCostAdjustmentAcctConf extends ModuleScript {
  private static final Logger log4j = LogManager.getLogger();

  @Override
  // Inserting:
  // 1) accounting schema tables for existing tables that are missing
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      createAcctSchemaTables(cp);
    } catch (Exception e) {
      handleError(e);
    }
  }

  void createAcctSchemaTables(ConnectionProvider cp) throws Exception {
    CreateCostAdjustmentAcctConfData[] data = CreateCostAdjustmentAcctConfData.selectAcctSchema(cp);
    final String TABLE_COST_ADJ = "D022B92163074E5E82449C8E0B5AFDF6";
    String tables[] = {TABLE_COST_ADJ};
    for (int i = 0; i < data.length; i++) {
      for (int j = 0; j < tables.length; j++) {
        boolean existInAcctSchema = CreateCostAdjustmentAcctConfData.selectTables(cp, data[i].cAcctschemaId, tables[j]);
        if(!existInAcctSchema){
          CreateCostAdjustmentAcctConfData.insertAcctSchemaTable(cp.getConnection(), cp, data[i].cAcctschemaId, tables[j], data[i].adClientId);
        }
      }
    }
  }
  
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, 
        new OpenbravoVersion(3,0,24790));
  }
}
