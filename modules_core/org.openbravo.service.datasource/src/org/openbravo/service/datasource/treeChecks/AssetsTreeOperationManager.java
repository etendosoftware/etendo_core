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
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.datasource.treeChecks;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.assetmgmt.Asset;
import org.openbravo.service.datasource.CheckTreeOperationManager;

@ApplicationScoped
@Qualifier("FinancialMgmtAsset")
public class AssetsTreeOperationManager extends CheckTreeOperationManager {

  /**
   * Only allows to move an asset if it is not static
   */
  @Override
  public ActionResponse checkNodeMovement(Map<String, String> parameters, String nodeId,
      String newParentId, String prevNodeId, String nextNodeId) {
    Asset asset = OBDal.getInstance().get(Asset.class, nodeId);
    if (asset.isStatic()) {
      return new ActionResponse(false, "error", OBMessageUtils.messageBD("StaticRecord"));
    } else {
      return new ActionResponse(true);
    }
  }
}
