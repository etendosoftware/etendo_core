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

public class SL_BankStmt_Amount extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    BigDecimal stmAmount = info.getBigDecimalParameter("inpstmtamt");
    BigDecimal trxAmount = info.getBigDecimalParameter("inptrxamt");
    BigDecimal chgAmount = info.getBigDecimalParameter("inpchargeamt");
    BigDecimal convChgAmount = info.getBigDecimalParameter("inpconvertchargeamt");
    String strCurrencyId = info.getStringParameter("inpcCurrencyId", IsIDFilter.instance);
    String strDP = info.getStringParameter("inpcDebtPaymentId", IsIDFilter.instance);

    boolean isConversion = false;
    if (StringUtils.isNotEmpty(strDP)) {
      isConversion = StringUtils
          .equals(SLBankStmtAmountData.isConversion(this, strCurrencyId, strDP), "Y");
    }

    if (StringUtils.equals(strChanged, "inpstmtamt")) {
      if (isConversion) {
        if (log4j.isDebugEnabled()) {
          log4j.debug("trx: " + trxAmount.toString() + "chg" + chgAmount.toString());
        }
        convChgAmount = trxAmount.subtract(chgAmount).subtract(stmAmount);
        info.addResult("inpconvertchargeamt", convChgAmount);
      } else {
        trxAmount = stmAmount.subtract(chgAmount);
        info.addResult("inptrxamt", trxAmount);
      }
    }

    if (StringUtils.equals(strChanged, "inpchargeamt")
        || (StringUtils.equals(strChanged, "inpconvertchargeamt"))) {
      stmAmount = trxAmount.subtract(chgAmount).subtract(convChgAmount);
      info.addResult("inpstmtamt", stmAmount);
    }
  }
}
