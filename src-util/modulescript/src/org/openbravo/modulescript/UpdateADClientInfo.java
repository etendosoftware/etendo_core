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
 * All portions are Copyright (C) 2012-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.modulescript;

import org.openbravo.database.ConnectionProvider;
import javax.servlet.ServletException;

public class UpdateADClientInfo extends ModuleScript {

  //This module script has ben created due to issue 18407 and related to issue 19697
  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      UpdateADClientInfoData[] clientsID = UpdateADClientInfoData.selectClientsID(cp);
      for (UpdateADClientInfoData clientID : clientsID) {
	// MC tree
        UpdateADClientInfoData.update(cp,clientID.adClientId);
      }
      // Asset tree
      createTreeAndUpdateClientInfo(cp, "Asset", "AS", "AD_TREE_ASSET_ID");
      // Product Category tree
      updateClientInfo(cp, "AD_TREE_PRODUCT_CATEGORY_ID", "PC");
      // Cost Center Tree
      createTreeAndUpdateClientInfo(cp, "Cost Center", "CC", "AD_TREE_COSTCENTER_ID");
      // User Defined Dimension 1 Tree
      createTreeAndUpdateClientInfo(cp, "User Dimension 1", "U1", "AD_TREE_USER1_ID");
      // User Defined Dimension 2 Tree
      createTreeAndUpdateClientInfo(cp, "User Dimension 2", "U2", "AD_TREE_USER2_ID");
      // Resource Category tree
      createTreeAndUpdateClientInfo(cp, "Resource Category", "OBRE_RC", "AD_TREE_OBRE_RESOURCE_CATEGORY");

      // Insert Missing Treenodes for Assets
      UpdateADClientInfoData.insertMissingTreeNodes(cp, "AS", "A_ASSET");
      // Insert Missing Treenodes for Product Categories
      UpdateADClientInfoData.insertMissingTreeNodes(cp, "PC", "M_PRODUCT_CATEGORY");
    } catch (Exception e) {
      handleError(e);
    }
  }
  
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, 
        new OpenbravoVersion(3,0,18977));
  }

    private void createTreeAndUpdateClientInfo(final ConnectionProvider cp, final String treeTypeName, final String treeTypeValue, final String columnName)
	throws ServletException {
      UpdateADClientInfoData[] clientsID = UpdateADClientInfoData.selectClientsMissingTree(cp, columnName);
      for (UpdateADClientInfoData clientID: clientsID) {
        final String treeId = UpdateADClientInfoData.getUUID(cp);
        final String nameAndDesc = clientID.clientname + " " + treeTypeName;
        UpdateADClientInfoData.createTree(cp, treeId, clientID.adClientId, nameAndDesc, treeTypeValue);
        UpdateADClientInfoData.updateClientTree(cp, columnName, treeId, clientID.adClientId);
      }
    }

    private void updateClientInfo(final ConnectionProvider cp, final String columnName, final String treeTypeValue)
	throws ServletException {
      UpdateADClientInfoData[] clientsID = UpdateADClientInfoData.selectClientsWithoutTree(cp, columnName);
      for (UpdateADClientInfoData clientID : clientsID) {
        UpdateADClientInfoData.updateClientTreeAuto(cp, columnName, treeTypeValue, clientID.adClientId);
      }
    }
}
