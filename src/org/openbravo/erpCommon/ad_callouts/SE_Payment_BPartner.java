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
 * All portions are Copyright (C) 2010-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.service.db.DalConnectionProvider;

public class SE_Payment_BPartner extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    VariablesSecureApp vars = info.vars;
    String strcBpartnerId = vars.getStringParameter("inpcBpartnerId");
    String strisreceipt = vars.getStringParameter("inpisreceipt");
    boolean isReceipt = "Y".equals(strisreceipt);

    // Get the Payment Method and the Financial Acoount
    FIN_PaymentMethod paymentMethod = null;
    FIN_FinancialAccount financialAccount = null;

    if (StringUtils.isNotEmpty(strcBpartnerId)) {
      BusinessPartner bpartner = OBDal.getInstance().get(BusinessPartner.class, strcBpartnerId);
      if (FIN_Utility.isBlockedBusinessPartner(strcBpartnerId, "Y".equals(strisreceipt), 4)) {
        // If the Business Partner is blocked for this document, show an information message.
        info.addResult("MESSAGE", OBMessageUtils.messageBD("ThebusinessPartner") + " "
            + bpartner.getIdentifier() + " " + OBMessageUtils.messageBD("BusinessPartnerBlocked"));
      }
      if (isReceipt) {
        paymentMethod = bpartner.getPaymentMethod();
        financialAccount = bpartner.getAccount();
      } else {
        paymentMethod = bpartner.getPOPaymentMethod();
        financialAccount = bpartner.getPOFinancialAccount();
      }

      if (paymentMethod != null && financialAccount != null) {
        final OBCriteria<FinAccPaymentMethod> apmCriteria = OBDal.getInstance()
            .createCriteria(FinAccPaymentMethod.class);
        apmCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, paymentMethod));
        apmCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, financialAccount));
        apmCriteria.setFilterOnActive(false);
        FinAccPaymentMethod accPaymentMethod = (FinAccPaymentMethod) apmCriteria.uniqueResult();
        if (accPaymentMethod != null) {
          if (financialAccount.isActive() && accPaymentMethod.isActive()) {
            info.addResult("inpfinPaymentmethodId", paymentMethod.getId());
            info.addResult("inpfinFinancialAccountId", financialAccount.getId());
          } else if (!financialAccount.isActive() && !accPaymentMethod.isActive()) {
            info.addResult("WARNING",
                String.format(
                    Utility.messageBD(new DalConnectionProvider(), "finnac_paymet_inact",
                        vars.getLanguage()),
                    financialAccount.getIdentifier(), paymentMethod.getIdentifier()));
          } else if (!financialAccount.isActive()) {
            info.addResult("WARNING", String.format(
                Utility.messageBD(new DalConnectionProvider(), "finnac_inact", vars.getLanguage()),
                financialAccount.getIdentifier()));
          } else if (!accPaymentMethod.isActive()) {
            info.addResult("WARNING",
                String.format(
                    Utility.messageBD(new DalConnectionProvider(), "paymet_inact",
                        vars.getLanguage()),
                    paymentMethod.getIdentifier(), financialAccount.getIdentifier()));
          }
        } else {
          log4j.info("No default info for the selected business partner {}", bpartner);
        }
      } else {
        log4j.info("No default info for the selected business partner {}", bpartner);
      }
    }
  }
}
