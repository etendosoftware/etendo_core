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

public class SL_Assets extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strLastChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strLastChanged);
    }

    // Parameters
    BigDecimal fAssetvalue = info.getBigDecimalParameter("inpassetvalueamt");
    BigDecimal fResidualvalue = info.getBigDecimalParameter("inpresidualassetvalueamt");
    BigDecimal fAmortizationvalue = info.getBigDecimalParameter("inpamortizationvalueamt");

    // If Asset Value changes recalculate Residual Asset Value using current Depreciation amount
    if (StringUtils.equals(strLastChanged, "inpassetvalueamt")) {
      if (fAmortizationvalue.compareTo(BigDecimal.ZERO) != 0) {
        fResidualvalue = fAssetvalue.subtract(fAmortizationvalue);
      }
      fAmortizationvalue = fAssetvalue.subtract(fResidualvalue);
    }

    // If Residual Asset Value changes recalculate Asset Value using new residual value
    if (StringUtils.equals(strLastChanged, "inpresidualassetvalueamt")) {
      fAmortizationvalue = fAssetvalue.subtract(fResidualvalue);
    }

    // If Depreciation amount changes recalculate Residual Asset Value using current Asset and
    // Depreciation values
    if (StringUtils.equals(strLastChanged, "inpamortizationvalueamt")) {
      fResidualvalue = fAssetvalue.subtract(fAmortizationvalue);
    }

    info.addResult("inpassetvalueamt", fAssetvalue);
    info.addResult("inpresidualassetvalueamt", fResidualvalue);
    info.addResult("inpamortizationvalueamt", fAmortizationvalue);
  }
}
