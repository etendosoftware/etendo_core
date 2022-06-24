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
 * All portions are Copyright (C) 2008-2017 Openbravo SLU 
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

public class SL_Project_Margin extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strcProjectId = info.getStringParameter("inpcProjectId", IsIDFilter.instance);
    // Services
    BigDecimal serviceRevenue = info.getBigDecimalParameter("inpservrevenue");
    BigDecimal serviceCost = info.getBigDecimalParameter("inpservcost");
    BigDecimal serviceMargin = info.getBigDecimalParameter("inpservmargin");
    // Expenses
    BigDecimal plannedExpenses = info.getBigDecimalParameter("inpexpexpenses");
    BigDecimal reinvoicedExpenses = info.getBigDecimalParameter("inpexpreinvoicing");
    BigDecimal expensesMargin = info.getBigDecimalParameter("inpexpmargin");

    // Standard Precision
    SLProjectMarginData[] data = SLProjectMarginData.select(this, strcProjectId);
    int stdPrecision = 0;
    if (data != null && data.length > 0) {
      stdPrecision = Integer.valueOf(data[0].stdprecision);
    }

    // Service Margin - SM = (SR-SC)*100/SR
    if (StringUtils.equals(strChanged, "inpservrevenue")
        || StringUtils.equals(strChanged, "inpservcost")) {
      if (serviceRevenue.compareTo(BigDecimal.ZERO) != 0) {
        serviceMargin = (((serviceRevenue.subtract(serviceCost)).divide(serviceRevenue, 12,
            RoundingMode.HALF_EVEN)).multiply(new BigDecimal("100"))).setScale(2,
                RoundingMode.HALF_UP);
      } else {
        serviceMargin = BigDecimal.ZERO;
      }
      info.addResult("inpservmargin", serviceMargin);
    }

    // Service Cost - SC = SR*(1-SM/100)
    if (StringUtils.equals(strChanged, "inpservmargin")) {
      serviceCost = serviceRevenue
          .multiply((BigDecimal.ONE).subtract(serviceMargin.divide(new BigDecimal("100"))));
      if (serviceCost.scale() > stdPrecision) {
        serviceCost = serviceCost.setScale(stdPrecision, RoundingMode.HALF_UP);
      }
      info.addResult("inpservcost", serviceCost);
    }

    // Expense Margin - EM = (RE-PE)*100/RE
    if (StringUtils.equals(strChanged, "inpexpexpenses")
        || StringUtils.equals(strChanged, "inpexpreinvoicing")) {
      if (reinvoicedExpenses.compareTo(BigDecimal.ZERO) != 0) {
        expensesMargin = (((reinvoicedExpenses.subtract(plannedExpenses))
            .multiply(new BigDecimal("100"))).divide(reinvoicedExpenses, 12,
                RoundingMode.HALF_EVEN)).setScale(2, RoundingMode.HALF_UP);
      } else {
        expensesMargin = BigDecimal.ZERO;
      }
      info.addResult("inpexpmargin", expensesMargin);
    }

    if (StringUtils.equals(strChanged, "inpexpmargin")) {
      // Planned Expenses - PE = 0 (because EM = 100 %)
      if (expensesMargin.compareTo(new BigDecimal("100")) == 0) {
        plannedExpenses = BigDecimal.ZERO;
        if (plannedExpenses.scale() > stdPrecision) {
          plannedExpenses = plannedExpenses.setScale(stdPrecision, RoundingMode.HALF_UP);
        }
        info.addResult("inpexpexpenses", plannedExpenses);
      }
      // Re-Invoiced Expenses - RE = PE/(1-EM/100)
      else {
        reinvoicedExpenses = plannedExpenses.divide(
            (BigDecimal.ONE)
                .subtract(expensesMargin.divide(new BigDecimal("100"), 12, RoundingMode.HALF_EVEN)),
            12, RoundingMode.HALF_EVEN);
        if (reinvoicedExpenses.scale() > stdPrecision) {
          reinvoicedExpenses = reinvoicedExpenses.setScale(stdPrecision, RoundingMode.HALF_UP);
        }
        info.addResult("inpexpreinvoicing", reinvoicedExpenses);
      }
    }

  }
}
