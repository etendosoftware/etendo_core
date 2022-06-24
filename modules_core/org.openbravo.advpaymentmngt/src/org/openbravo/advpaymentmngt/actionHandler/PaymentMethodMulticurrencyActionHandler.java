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
 * All portions are Copyright (C) 2014-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.advpaymentmngt.actionHandler;

import java.util.Date;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.service.json.JsonUtils;

public class PaymentMethodMulticurrencyActionHandler extends BaseActionHandler {

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    try {
      final JSONObject jsonData = new JSONObject(data);
      final String paymentMethodId = jsonData.getString("paymentMethodId");
      final String financialAccountId = jsonData.getString("financialAccountId");
      final boolean isSOTrx = jsonData.getBoolean("isSOTrx");
      String currencyId = jsonData.getString("currencyId");
      final String strPaymentDate = jsonData.getString("paymentDate");
      Date paymentDate = JsonUtils.createDateFormat().parse(strPaymentDate);
      final String strOrgId = jsonData.getString("orgId");

      JSONObject result = new JSONObject();

      if ("null".equals(currencyId) && !"null".equals(financialAccountId)
          && !"".equals(financialAccountId)) {
        FIN_FinancialAccount financialAccount = OBDal.getInstance()
            .get(FIN_FinancialAccount.class, financialAccountId);
        currencyId = financialAccount.getCurrency().getId();
        result.put("currencyIdIdentifier", financialAccount.getCurrency().getIdentifier());
        result.put("currencyId", currencyId);
      }

      final FinAccPaymentMethod finAccPaymentMethod = getFinancialAccountPaymentMethod(
          paymentMethodId, financialAccountId);
      if (finAccPaymentMethod != null) {
        result.put("isPayIsMulticurrency", isSOTrx ? finAccPaymentMethod.isPayinIsMulticurrency()
            : finAccPaymentMethod.isPayoutIsMulticurrency());
      } else {
        result.put("isPayIsMulticurrency", false);
      }
      if (!isValidFinancialAccount(finAccPaymentMethod, currencyId, isSOTrx)) {
        result.put("isWrongFinancialAccount", true);
      } else {
        result.put("isWrongFinancialAccount", false);
      }
      if (finAccPaymentMethod != null) {
        if (finAccPaymentMethod.getAccount().getCurrency().getId().equals(currencyId)) {
          result.put("conversionrate", 1);
        } else {
          ConversionRate convRate = FinancialUtils.getConversionRate(paymentDate,
              OBDal.getInstance().get(Currency.class, currencyId),
              finAccPaymentMethod.getAccount().getCurrency(),
              OBDal.getInstance().get(Organization.class, strOrgId),
              OBDal.getInstance().get(Organization.class, strOrgId).getClient());
          if (convRate != null) {
            result.put("conversionrate", convRate.getMultipleRateBy());
          } else {
            result.put("conversionrate", "");
            result.put("convertedamount", "");
          }
        }
        result.put("currencyToId", finAccPaymentMethod.getAccount().getCurrency().getId());
        result.put("currencyToIdentifier",
            finAccPaymentMethod.getAccount().getCurrency().getIdentifier());
      } else {
        result.put("conversionrate", 1);
        result.put("currencyToId", currencyId);
      }
      return result;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  private boolean isValidFinancialAccount(FinAccPaymentMethod finAccPaymentMethod,
      String currencyId, boolean isSOTrx) {
    if (finAccPaymentMethod != null) {
      if (finAccPaymentMethod.getAccount().getCurrency().getId().equals(currencyId)) {
        return isSOTrx ? finAccPaymentMethod.isPayinAllow() : finAccPaymentMethod.isPayoutAllow();
      } else {
        return isSOTrx ? finAccPaymentMethod.isPayinIsMulticurrency()
            : finAccPaymentMethod.isPayoutIsMulticurrency();
      }
    } else {
      return false;
    }
  }

  private FinAccPaymentMethod getFinancialAccountPaymentMethod(String paymentMethodId,
      String financialAccountId) {
    OBContext.setAdminMode(true);
    try {
      OBCriteria<FinAccPaymentMethod> obc = OBDal.getInstance()
          .createCriteria(FinAccPaymentMethod.class);
      obc.setFilterOnReadableOrganization(false);
      obc.setMaxResults(1);
      obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT,
          OBDal.getInstance().get(FIN_FinancialAccount.class, financialAccountId)));
      obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD,
          OBDal.getInstance().get(FIN_PaymentMethod.class, paymentMethodId)));
      return (FinAccPaymentMethod) obc.uniqueResult();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
