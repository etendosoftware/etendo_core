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
package org.openbravo.advpaymentmngt.filterexpression;

import java.math.BigDecimal;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.utility.APRMConstants;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

@ComponentProvider.Qualifier(APRMConstants.PAYMENT_OUT_WINDOW_ID)
public class PaymentOutAddPaymentDisplayLogics extends AddPaymentDisplayLogicsHandler {

  private static final long SEQUENCE = 100l;

  @Override
  protected long getSeq() {
    return SEQUENCE;
  }

  @Override
  public boolean getOrganizationDisplayLogic(Map<String, String> requestMap) throws JSONException {
    // Organization
    return false;
  }

  @Override
  public boolean getDocumentDisplayLogic(Map<String, String> requestMap) throws JSONException {
    // Document Type
    return false;
  }

  @Override
  public boolean getCreditToUseDisplayLogic(Map<String, String> requestMap) throws JSONException {
    FIN_Payment paymentOut = getPayment(requestMap);
    if (getGeneratedCredit(requestMap).signum() == 0) {
      BusinessPartner bpartner = paymentOut.getBusinessPartner();
      if (bpartner != null) {
        Organization org = paymentOut.getOrganization();
        Currency currency = paymentOut.getCurrency();
        BigDecimal customerCredit = new AdvPaymentMngtDao().getCustomerCredit(bpartner, false, org,
            currency);
        return customerCredit.signum() > 0;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  private FIN_Payment getPayment(Map<String, String> requestMap) throws JSONException {
    // Current Payment
    JSONObject context = new JSONObject(requestMap.get("context"));
    String strFinPaymentId = "";
    if (context.has("inpfinPaymentId") && !context.isNull("inpfinPaymentId")) {
      strFinPaymentId = context.getString("inpfinPaymentId");
    }
    if (context.has("Fin_Payment_ID") && !context.isNull("Fin_Payment_ID")) {
      strFinPaymentId = context.getString("Fin_Payment_ID");
    }
    FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, strFinPaymentId);
    return payment;
  }

  BigDecimal getGeneratedCredit(Map<String, String> requestMap) throws JSONException {
    // Generated Credit of the current Payment
    return getPayment(requestMap).getGeneratedCredit();
  }

  @Override
  public boolean getBankStatementLineDisplayLogic(Map<String, String> requestMap)
      throws JSONException {
    // BankStatementLineDisplayLogic
    return false;
  }
}
