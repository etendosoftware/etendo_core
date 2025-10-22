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

package org.openbravo.erpCommon.ad_callouts;

import jakarta.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.cost.LCDistributionAlgorithm;
import org.openbravo.model.materialmgmt.cost.LandedCostType;

public class SL_LandedCost_Cost_Type extends SimpleCallout {
  private static final String LANDED_COST_TYPE = "inpmLcTypeId";
  private static final String DISTRIBUTION_ALGORITHM = "inpmLcDistributionAlgId";

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String landedCostTypeId = info.getStringParameter(LANDED_COST_TYPE, null);

    if (StringUtils.isNotEmpty(landedCostTypeId)) {
      LandedCostType landedCostType = OBDal.getInstance().get(LandedCostType.class, landedCostTypeId);
      if (landedCostType != null) {
        LCDistributionAlgorithm distributionAlgorithm = landedCostType.getLandedCostDistributionAlgorithm();
        if (distributionAlgorithm != null) {
          info.addResult(DISTRIBUTION_ALGORITHM, distributionAlgorithm.getId());
        }
      }
    }
    // When Landed Cost Type changes, empty the Invoice Line so the selector can recalculate again
    // the Invoice Lines that must be displayed
    info.addResult("inpcInvoicelineId", "");
    // Remove session values for Auxiliary inputs so they can be calculated again instead of
    // retrieving them from session
    info.vars.setSessionValue(info.getWindowId() + "|LCC_PRODUCT_ID", null);
    info.vars.setSessionValue(info.getWindowId() + "|LCC_ACCOUNT_ID", null);
  }
}
