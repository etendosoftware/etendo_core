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
 * All portions are Copyright (C) 2001-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_callouts;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.utils.FormatUtilities;

public class SL_ProductionPlan_WRPhase extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strProductionId = info.getStringParameter("inpmProductionId", IsIDFilter.instance);
    String strWRPhaseId = info.getStringParameter("inpmaWrphaseId", IsIDFilter.instance);
    BigDecimal quantity = info.getBigDecimalParameter("inpproductionqty");

    SLProductionPlanWRPhaseData[] data = SLProductionPlanWRPhaseData.select(this, strProductionId,
        strWRPhaseId);
    if (data == null || data.length == 0) {
      data = SLProductionPlanWRPhaseData.set();
    }

    // Estimated Time
    BigDecimal estimatedTime = BigDecimal.ZERO;
    BigDecimal qtyWRPhase = new BigDecimal(data[0].quantity);
    if (StringUtils.isNotEmpty(data[0].estimatedtime)
        && qtyWRPhase.compareTo(BigDecimal.ZERO) != 0) {
      estimatedTime = new BigDecimal(data[0].estimatedtime).divide(qtyWRPhase).multiply(quantity);
    }
    info.addResult("inpestimatedtime", estimatedTime);

    // Outsourced flag
    String strOutsourced = SLProductionPlanWRPhaseData.selectOutsourced(this, strWRPhaseId);
    info.addResult("inpoutsourced", strOutsourced);

    // Needed Quantity
    String strNeededQuantity = data[0].neededqty;
    if (StringUtils.isNotEmpty(strNeededQuantity)) {
      info.addResult("inpneededquantity", strNeededQuantity);
    }

    // Secondary Unit
    info.addResult("inpsecondaryunit", FormatUtilities.replaceJS(data[0].secondaryunit));

    // Conversion Rate
    if (StringUtils.isNotEmpty(data[0].conversionrate)) {
      info.addResult("inpconversionrate", data[0].conversionrate);
    }

    // Cost Center Version
    info.addResult("inpmaCostcenterVersionId", data[0].maCostcenterVersionId);
  }
}
