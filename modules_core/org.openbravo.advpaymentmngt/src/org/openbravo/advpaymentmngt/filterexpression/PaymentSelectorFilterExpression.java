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
 * All portions are Copyright (C) 2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.advpaymentmngt.filterexpression;

import java.util.Map;

import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;

import jakarta.enterprise.context.Dependent;

@Dependent
public class PaymentSelectorFilterExpression implements FilterExpression {
  private Map<String, String> requestMap;

  @Override
  public String getExpression(Map<String, String> _requestMap) {
    requestMap = _requestMap;

    if (requestMap.containsKey("inpfinFinancialAccountId")) {
      FIN_FinancialAccount financialaccount = OBDal.getInstance()
          .get(FIN_FinancialAccount.class, requestMap.get("inpfinFinancialAccountId"));
      return financialaccount.getId();
    }

    return "";
  }
}
