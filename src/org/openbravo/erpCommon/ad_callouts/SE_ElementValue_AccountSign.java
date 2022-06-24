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
 * All portions are Copyright (C) 2012-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.accounting.coa.Element;

public class SE_ElementValue_AccountSign extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    final String ACCOUNTSIGN_CREDIT = "C";
    final String ACCOUNTSIGN_DEBIT = "D";
    final String ACCOUNTTYPE_ASSET = "A";
    final String ACCOUNTTYPE_LIABILITY = "L";
    final String ACCOUNTTYPE_EQUITY = "O";
    final String ACCOUNTTYPE_REVENUE = "R";
    final String ACCOUNTTYPE_EXPENSE = "E";

    final String strAccountType = info.getStringParameter("inpaccounttype", null);
    boolean centrallyMaintained = false;
    final String strElementId = info.getStringParameter("inpcElementId", null);
    Element element = OBDal.getInstance().get(Element.class, strElementId);
    if (element.getFinancialMgmtAcctSchemaElementList().size() == 1) {
      centrallyMaintained = element.getFinancialMgmtAcctSchemaElementList()
          .get(0)
          .getAccountingSchema()
          .isCentralMaintenance();
    }
    if (!centrallyMaintained) {
      if (strAccountType.equals(ACCOUNTTYPE_ASSET) || strAccountType.equals(ACCOUNTTYPE_EXPENSE)) {
        info.addResult("inpaccountsign", ACCOUNTSIGN_DEBIT);
      } else {
        info.addResult("inpaccountsign", ACCOUNTSIGN_CREDIT);
      }
    } else {
      AcctSchema as = element.getFinancialMgmtAcctSchemaElementList().get(0).getAccountingSchema();
      if ((strAccountType.equals(ACCOUNTTYPE_ASSET) && as.isAssetPositive())
          || (strAccountType.equals(ACCOUNTTYPE_EXPENSE) && as.isExpensePositive())
          || (strAccountType.equals(ACCOUNTTYPE_LIABILITY) && !as.isLiabilityPositive())
          || (strAccountType.equals(ACCOUNTTYPE_EQUITY) && !as.isEquityPositive())
          || (strAccountType.equals(ACCOUNTTYPE_REVENUE) && !as.isRevenuePositive())) {
        info.addResult("inpaccountsign", ACCOUNTSIGN_DEBIT);
      } else {
        info.addResult("inpaccountsign", ACCOUNTSIGN_CREDIT);
      }

    }
  }
}
