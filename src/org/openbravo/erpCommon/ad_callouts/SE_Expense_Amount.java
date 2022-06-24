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
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.timeandexpense.Sheet;

public class SE_Expense_Amount extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    BigDecimal amount = info.getBigDecimalParameter("inpexpenseamt");
    String strDateexpense = info.getStringParameter("inpdateexpense");
    String strcCurrencyId = info.getStringParameter("inpcCurrencyId", IsIDFilter.instance);
    String strTimeExpenseId = info.getStringParameter("inpsTimeexpenseId", IsIDFilter.instance);

    // Get the currency to from organization's currency, or from the client's currency if it doesn't
    // exists
    final Organization org = OBDal.getInstance()
        .get(Sheet.class, strTimeExpenseId)
        .getOrganization();
    String c_Currency_To_ID = getCurrency(org.getId());
    if (c_Currency_To_ID == null) {
      c_Currency_To_ID = OBDal.getInstance()
          .get(Sheet.class, strTimeExpenseId)
          .getClient()
          .getCurrency()
          .getId();
    }

    if (StringUtils.isEmpty(strDateexpense)) {
      strDateexpense = StringUtils.isEmpty(
          SEExpenseAmountData.selectReportDate(this, strTimeExpenseId)) ? DateTimeData.today(this)
              : SEExpenseAmountData.selectReportDate(this, strTimeExpenseId);
    }

    // Amount expense
    int stdPrecision = 0;
    if (StringUtils.isNotEmpty(strcCurrencyId)) {
      stdPrecision = Integer.valueOf(SEExpenseAmountData.selectPrecision(this, strcCurrencyId));
    }
    if (amount.scale() > stdPrecision) {
      amount = amount.setScale(stdPrecision, RoundingMode.HALF_UP);
    }

    // Checks if there is a conversion rate for each of the transactions of
    // the report
    BigDecimal convAmount = amount;
    if (!StringUtils.equals(strcCurrencyId, c_Currency_To_ID)) {
      String convertedAmount = amount.toPlainString();
      try {
        convertedAmount = SEExpenseProductData.selectConvertedAmt(this, amount.toPlainString(),
            strcCurrencyId, c_Currency_To_ID, strDateexpense, info.vars.getClient(), org.getId());
      } catch (Exception e) {
        convertedAmount = "";
        OBDal.getInstance().rollbackAndClose();
        info.showMessage(
            Utility.translateError(this, info.vars, info.vars.getLanguage(), e.getMessage())
                .getMessage());
        log4j.warn("Currency does not exist. Exception:" + e);
      }
      convAmount = StringUtils.isNotEmpty(convertedAmount) ? new BigDecimal(convertedAmount)
          : BigDecimal.ZERO;
      int stdPrecisionConv = 0;
      if (StringUtils.isNotEmpty(c_Currency_To_ID)) {
        stdPrecisionConv = Integer
            .valueOf(SEExpenseAmountData.selectPrecision(this, c_Currency_To_ID));
      }
      if (convAmount.scale() > stdPrecisionConv) {
        convAmount = convAmount.setScale(stdPrecisionConv, RoundingMode.HALF_UP);
      }
    }

    // Update Expense Amount and Converted Amount
    info.addResult("inpexpenseamt", amount);
    info.addResult("inpconvertedamt",
        convAmount.compareTo(BigDecimal.ZERO) != 0 ? convAmount : null);
  }

  private static String getCurrency(String org) {
    if (StringUtils.equals(org, "0")) {
      return null;
    } else {
      Organization organization = OBDal.getInstance().get(Organization.class, org);
      if (organization.getCurrency() != null) {
        return organization.getCurrency().getId();
      } else {
        return getCurrency(new OrganizationStructureProvider().getParentOrg(org));
      }
    }
  }
}
