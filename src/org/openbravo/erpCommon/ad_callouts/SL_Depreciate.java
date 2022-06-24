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
 * All portions are Copyright (C) 2012-2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.assetmgmt.AssetGroup;

public class SL_Depreciate extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    final String assetCategoryID = info.getStringParameter("inpaAssetGroupId", IsIDFilter.instance);
    if (!assetCategoryID.isEmpty()) {
      OBContext.setAdminMode(true);
      try {
        final AssetGroup assetGroup = OBDal.getInstance().get(AssetGroup.class, assetCategoryID);
        info.addResult("inpisdepreciated", assetGroup.isDepreciate());
        info.addResult("inpamortizationtype", assetGroup.getDepreciationType());
        info.addResult("inpamortizationcalctype", assetGroup.getCalculateType());
        info.addResult("inpannualamortizationpercentage", assetGroup.getAnnualDepreciation());
        info.addResult("inpassetschedule", assetGroup.getAmortize());
        info.addResult("inpuselifemonths", assetGroup.getUsableLifeMonths());
        info.addResult("inpuselifeyears", assetGroup.getUsableLifeYears());
        info.addResult("inpis30daymonth", assetGroup.isEveryMonthIs30Days() ? "Y" : "N");
      } finally {
        OBContext.restorePreviousMode();
      }
    }
  }

  protected Boolean getDepreciatedFlag(final String assetCategoryID) {
    OBContext.setAdminMode();
    try {
      final AssetGroup assetCategory = OBDal.getInstance().get(AssetGroup.class, assetCategoryID);
      return (assetCategory == null ? Boolean.FALSE : assetCategory.isDepreciate());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
