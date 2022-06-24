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

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;

public class SL_MachineCost extends SimpleCallout {
  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    BigDecimal fPurchaseAmt = info.getBigDecimalParameter("inppurchaseamt");
    BigDecimal fToolsetAmt = info.getBigDecimalParameter("inptoolsetamt");
    BigDecimal fYearValue = info.getBigDecimalParameter("inpyearvalue");
    BigDecimal fAmortization = info.getBigDecimalParameter("inpamortization");
    BigDecimal fDaysYear = info.getBigDecimalParameter("inpdaysyear");
    BigDecimal fDayHours = info.getBigDecimalParameter("inpdayhours");
    BigDecimal fImproductiveHoursYear = info.getBigDecimalParameter("inpimproductivehoursyear");
    BigDecimal fCostUomYear = info.getBigDecimalParameter("inpcostuomyear");
    BigDecimal fCost = info.getBigDecimalParameter("inpcost");
    String strCostUom = info.getStringParameter("inpcostuom");

    if (StringUtils.equals(strChanged, "inppurchaseamt")
        || StringUtils.equals(strChanged, "inptoolsetamt")
        || StringUtils.equals(strChanged, "inpyearvalue")) {
      // Calculate Amortization, Cost
      fAmortization = fYearValue.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
          : (fPurchaseAmt.add(fToolsetAmt)).divide(fYearValue, 12, RoundingMode.HALF_EVEN);
      fCost = fCostUomYear.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
          : fYearValue.divide(fCostUomYear, 12, RoundingMode.HALF_EVEN);

    } else if (StringUtils.equals(strChanged, "inpamortization")) {
      // Calculate Value per Year, Cost
      fYearValue = fAmortization.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
          : (fPurchaseAmt.add(fToolsetAmt)).divide(fAmortization, 12, RoundingMode.HALF_EVEN);
      fCost = fCostUomYear.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
          : fYearValue.divide(fCostUomYear, 12, RoundingMode.HALF_EVEN);

    } else if (StringUtils.equals(strChanged, "inpdaysyear")
        || StringUtils.equals(strChanged, "inpdayhours")
        || strChanged.equals("inpimproductivehoursyear")) {
      // Calculate UOM Annual Cost, Cost
      fCostUomYear = (fDaysYear.multiply(fDayHours)).subtract(fImproductiveHoursYear);
      fCost = fCostUomYear.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
          : fYearValue.divide(fCostUomYear, 12, RoundingMode.HALF_EVEN);

    } else if (StringUtils.equals(strChanged, "inpcostuomyear")) {
      // Calculate Productive Hours per Year, Cost
      if (strCostUom.equals("H")) {
        fImproductiveHoursYear = (fDaysYear.multiply(fDayHours)).subtract(fCostUomYear);
      }
      fCost = fCostUomYear.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
          : fYearValue.divide(fCostUomYear, 12, RoundingMode.HALF_EVEN);

    } else if (StringUtils.equals(strChanged, "inpcost")) {
      // Calculate Value per Year, Amortization
      fYearValue = fCost.multiply(fCostUomYear);
      fAmortization = fYearValue.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
          : (fPurchaseAmt.add(fToolsetAmt)).divide(fYearValue, 12, RoundingMode.HALF_EVEN);
    }

    // Set Purchase Amount, ToolSetAmount, Value per Year, Amortization, Hours per Day,
    // ProductiveHours per Year, UOM Annual Cost, Cost
    info.addResult("inppurchaseamt", fPurchaseAmt);
    info.addResult("inptoolsetamt", fToolsetAmt);
    info.addResult("inpyearvalue", fYearValue);
    info.addResult("inpamortization", fAmortization);
    info.addResult("inpdaysyear", fDaysYear);
    info.addResult("inpdayhours", fDayHours);
    info.addResult("inpimproductivehoursyear", fImproductiveHoursYear);
    info.addResult("inpcostuomyear", fCostUomYear);
    info.addResult("inpcost", fCost);
  }
}
