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
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.erpCommon.utility.Utility;

public class SL_Project_Planned extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strcProjectLineId = info.getStringParameter("inpcProjectlineId", IsIDFilter.instance);
    BigDecimal plannedQty = info.getBigDecimalParameter("inpplannedqty");
    BigDecimal plannedPrice = info.getBigDecimalParameter("inpplannedprice");
    BigDecimal plannedPurchasePrice = info.getBigDecimalParameter("inpplannedpoprice");
    BigDecimal plannedMargin = info.getBigDecimalParameter("inpplannedmarginamt");

    // Standard Precision
    SLProjectPlannedAmtData[] data = SLProjectPlannedAmtData.select(this, strcProjectLineId);
    int stdPrecision = 0;
    if (data != null && data.length > 0) {
      stdPrecision = Integer.valueOf(data[0].stdprecision);
    } else {
      String strcCurrencyId = Utility.getContext(this, info.vars, "$C_Currency_ID", "");
      stdPrecision = Integer.valueOf(SLProjectPlannedAmtData.selectPrecision(this, strcCurrencyId));
    }

    // Planned Amount - PA = PQ*PP
    BigDecimal plannedAmt = BigDecimal.ZERO;
    if (StringUtils.equals(strChanged, "inpplannedqty")
        || StringUtils.equals(strChanged, "inpplannedprice")) {
      plannedAmt = plannedQty.multiply(plannedPrice);
      if (plannedAmt.scale() > stdPrecision) {
        plannedAmt = plannedAmt.setScale(stdPrecision, RoundingMode.HALF_UP);
      }
      info.addResult("inpplannedamt", plannedAmt);
    }

    // Planned Margin Amount - PM = (PP - PPP)*100/PP
    if (StringUtils.equals(strChanged, "inpplannedprice")
        || StringUtils.equals(strChanged, "inpplannedpoprice")) {
      if (plannedPrice.compareTo(BigDecimal.ZERO) != 0) {
        plannedMargin = (((plannedPrice.subtract(plannedPurchasePrice))
            .multiply(new BigDecimal("100"))).divide(plannedPrice, 12, RoundingMode.HALF_EVEN))
                .setScale(2, RoundingMode.HALF_UP);
      } else {
        plannedMargin = BigDecimal.ZERO;
      }
      info.addResult("inpplannedmarginamt", plannedMargin);
    }

    // Planned PO Price - PPP = PP*(1-PM/100)
    if (strChanged.equals("inpplannedmarginamt")) {
      plannedPurchasePrice = plannedPrice.multiply((BigDecimal.ONE)
          .subtract(plannedMargin.divide(new BigDecimal("100"), 12, RoundingMode.HALF_EVEN)));
      if (plannedPurchasePrice.scale() > stdPrecision) {
        plannedPurchasePrice = plannedPurchasePrice.setScale(stdPrecision, RoundingMode.HALF_UP);
      }
      info.addResult("inpplannedpoprice", plannedPurchasePrice);
    }
  }
}
