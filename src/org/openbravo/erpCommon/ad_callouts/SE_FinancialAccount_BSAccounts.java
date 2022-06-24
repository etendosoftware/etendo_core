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
 * All portions are Copyright (C) 2010-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;

public class SE_FinancialAccount_BSAccounts extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    final String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    final String strfinFinancialAccountId = info.getStringParameter("inpfinFinancialAccountId",
        IsIDFilter.instance);
    final String strfinTransitoryAcct = info.getStringParameter("inpfinTransitoryAcct",
        IsIDFilter.instance);
    final String strOB3UIMode = info.vars.getStringParameter("inpOB3UIMode", "N");

    if (info.vars.commandIn("EXECUTE")) {
      updatePaymentMethodConfiguration(strfinFinancialAccountId);
      info.addResult("inpfinInClearAcct", strfinTransitoryAcct);
      info.addResult("inpfinOutClearAcct", strfinTransitoryAcct);
    }

    else {
      if (StringUtils.equals(strChanged, "inpfinTransitoryAcct")
          && StringUtils.isNotEmpty(strfinTransitoryAcct)) {
        String strConfirmMessage = Utility.messageBD(this, "BankStatementAccountWarning",
            info.vars.getLanguage());
        if (StringUtils.equals(strOB3UIMode, "Y")) {
          String strScript = "OB.APRM.bankTransitoryAccountCalloutResponse(this, '"
              + strConfirmMessage.replaceAll("\\\\n", "<br>") + "', '" + strfinFinancialAccountId
              + "')";
          info.addResult("JSEXECUTE", strScript);
        } else {
          String strScript = "(function(){var confirmation = confirm(\'"
              + strConfirmMessage.replaceAll("\\\\n", "\\\\\\\\n")
              + "\'); if(confirmation){submitCommandFormParameter(\'EXECUTE\', frmMain.inpLastFieldChanged, \'"
              + strChanged
              + "\', false, null, \'../ad_callouts/SE_FinancialAccount_BSAccounts.html\', \'hiddenFrame\', null, null, true);}})();";
          info.addResult("EXECUTE", strScript);
        }
      }
    }
  }

  void updatePaymentMethodConfiguration(String strfinFinancialAccountId) {
    FIN_FinancialAccount account = OBDal.getInstance()
        .get(FIN_FinancialAccount.class, strfinFinancialAccountId);

    // Configure clearing account for all payment methods upon clearing event
    for (FinAccPaymentMethod paymentMethod : account.getFinancialMgmtFinAccPaymentMethodList()) {
      paymentMethod.setOUTUponClearingUse("CLE");
      paymentMethod.setINUponClearingUse("CLE");
      OBDal.getInstance().save(paymentMethod);
      OBDal.getInstance().flush();
    }
  }
}
