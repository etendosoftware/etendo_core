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

import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;

public class ReceivedFromPaymentMethodActionHandler extends BaseActionHandler {

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    try {
      final JSONObject jsonData = new JSONObject(data);
      JSONObject result = new JSONObject();
      FIN_FinancialAccount financialAccount = OBDal.getInstance()
          .get(FIN_FinancialAccount.class, jsonData.getString("financialAccount"));
      FIN_PaymentMethod paymentMethod = null;
      String paymentMethodId = "";
      String paymentMethodName = "";

      if (financialAccount != null) {
        if (jsonData.has("receivedFrom") && jsonData.get("receivedFrom") != JSONObject.NULL) {
          final String receivedFrom = jsonData.getString("receivedFrom");
          BusinessPartner businessPartner = OBDal.getInstance()
              .get(BusinessPartner.class, receivedFrom);
          if (jsonData.getString("isSOTrx").toString().equals("true")) {
            paymentMethod = businessPartner.getPaymentMethod();
          } else {
            paymentMethod = businessPartner.getPOPaymentMethod();
          }

          if (paymentMethod != null) {
            OBCriteria<FinAccPaymentMethod> criteria = OBDal.getInstance()
                .createCriteria(FinAccPaymentMethod.class);
            criteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, financialAccount));
            criteria
                .add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, paymentMethod));
            criteria.setFilterOnReadableOrganization(false);
            criteria.setFilterOnActive(true);
            criteria.setMaxResults(1);
            FinAccPaymentMethod finAccPaymentMethod = (FinAccPaymentMethod) criteria.uniqueResult();

            if (finAccPaymentMethod != null) {
              paymentMethodId = finAccPaymentMethod.getPaymentMethod().getId();
              paymentMethodName = finAccPaymentMethod.getPaymentMethod().getName();
            }
          }
        }
      }

      result.put("paymentMethodId", paymentMethodId);
      result.put("paymentMethodName", paymentMethodName);
      return result;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }
}
