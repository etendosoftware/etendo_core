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

public class SL_BankDebt_Amount extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strDebtPayment = info.getStringParameter("inpcDebtPaymentId", IsIDFilter.instance);
    String strBankStatement = info.getStringParameter("inpcBankstatementId", IsIDFilter.instance);
    String strCurrency = info.getStringParameter("inpcCurrencyId", IsIDFilter.instance);
    String strDesc = info.getStringParameter("inpdescription");

    String conv = "N";
    BigDecimal amount = BigDecimal.ZERO;
    if (StringUtils.isNotEmpty(strDebtPayment)) {
      String strAmt = SLCashJournalAmountsData.amountDebtPaymentBank(this, strBankStatement,
          strDebtPayment);
      amount = StringUtils.isEmpty(strAmt) ? BigDecimal.ZERO : new BigDecimal(strAmt);

      if (StringUtils.isNotEmpty(strDesc)) {
        strDesc = strDesc + " - ";
      }
      strDesc = strDesc + SLCashJournalAmountsData.debtPaymentDescription(this, strDebtPayment);
      conv = SLBankStmtAmountData.isConversion(this, strCurrency, strDebtPayment);
    }

    info.addResult("inpdescription", FormatUtilities.replaceJS(strDesc));
    info.addResult("inptrxamt", amount);
    info.addResult("inpcurrconv", conv);
    info.addResult("inpconvertchargeamt", BigDecimal.ZERO);
    info.addResult("inpstmtamt", amount);
  }
}
