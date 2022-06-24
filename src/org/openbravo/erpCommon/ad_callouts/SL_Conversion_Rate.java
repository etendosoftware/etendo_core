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

public class SL_Conversion_Rate extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    BigDecimal multiplyRate = info.getBigDecimalParameter("inpmultiplyrate");
    BigDecimal divideRate = info.getBigDecimalParameter("inpdividerate");

    // Calculate Divide Rate By based on Multiple Rate By
    if (StringUtils.equals(strChanged, "inpmultiplyrate")) {
      BigDecimal rate = BigDecimal.ZERO;
      if (multiplyRate.compareTo(BigDecimal.ZERO) != 0) {
        rate = BigDecimal.ONE.divide(multiplyRate, 12, RoundingMode.HALF_UP);
      }
      info.addResult("inpdividerate", rate);
    }

    // Calculate Multiple Rate By based on Divide Rate By
    else if (StringUtils.equals(strChanged, "inpdividerate")) {
      BigDecimal rate = BigDecimal.ZERO;
      if (divideRate.compareTo(BigDecimal.ZERO) != 0) {
        rate = BigDecimal.ONE.divide(divideRate, 12, RoundingMode.HALF_UP);
      }
      info.addResult("inpmultiplyrate", rate);
    }
  }
}
