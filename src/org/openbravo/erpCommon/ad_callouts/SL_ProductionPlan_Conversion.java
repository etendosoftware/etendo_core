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
import java.math.RoundingMode;

import jakarta.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.filter.IsIDFilter;

public class SL_ProductionPlan_Conversion extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strSecQty = info.vars.getNumericParameter("inpsecondaryqty");
    String strMaWrphaseId = info.getStringParameter("inpmaWrphaseId", IsIDFilter.instance);

    // Production Quantity
    if (StringUtils.isNotEmpty(strSecQty)) {
      BigDecimal convRate = new BigDecimal(
          SLProductionPlanWRPhaseData.getMultiplier(this, strMaWrphaseId));
      BigDecimal secondaryQty = new BigDecimal(strSecQty);
      BigDecimal quantity = secondaryQty.divide(convRate, 0, RoundingMode.HALF_UP);
      info.addResult("inpproductionqty", quantity);
    }
  }
}
