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
 * All portions are Copyright (C) 2010-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;

public class SE_PaymentMethod_FinAccount extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String tabId = info.getTabId();
    boolean isVendorTab = "224".equals(tabId);
    String finIsReceipt = info.getStringParameter("inpisreceipt", null);
    boolean isPaymentOut = isVendorTab || "N".equals(finIsReceipt);
    String strOrgId = info.getStringParameter("inpadOrgId", IsIDFilter.instance);

    String strSelectedPaymentMethod = info.getStringParameter(
        isVendorTab ? "inppoPaymentmethodId" : "inpfinPaymentmethodId", IsIDFilter.instance);

    FIN_PaymentMethod paymentMethod = OBDal.getInstance()
        .get(FIN_PaymentMethod.class, strSelectedPaymentMethod);

    String strSelectedFinancialAccount = info.getStringParameter(
        isVendorTab ? "inppoFinancialAccountId" : "inpfinFinancialAccountId", IsIDFilter.instance);

    FIN_FinancialAccount financialAccount = OBDal.getInstance()
        .get(FIN_FinancialAccount.class, strSelectedFinancialAccount);

    boolean isMultiCurrencyEnabled = false;

    if (paymentMethod != null && financialAccount != null) {
      OBCriteria<FinAccPaymentMethod> obc = OBDal.getInstance()
          .createCriteria(FinAccPaymentMethod.class);
      // (paymentmethod, financial_account) is unique
      obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, paymentMethod));
      obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, financialAccount));
      obc.add(Restrictions.in("organization.id",
          OBContext.getOBContext().getOrganizationStructureProvider().getNaturalTree(strOrgId)));

      FinAccPaymentMethod selectedAccPaymentMethod = (FinAccPaymentMethod) obc.uniqueResult();
      if (selectedAccPaymentMethod != null) {
        if (isPaymentOut) {
          isMultiCurrencyEnabled = selectedAccPaymentMethod.isPayoutAllow()
              && selectedAccPaymentMethod.isPayoutIsMulticurrency();
        } else {
          isMultiCurrencyEnabled = selectedAccPaymentMethod.isPayinAllow()
              && selectedAccPaymentMethod.isPayinIsMulticurrency();
        }
      }
    }
    info.addResult("inpismulticurrencyenabled", isMultiCurrencyEnabled ? "Y" : "N");
  }
}
