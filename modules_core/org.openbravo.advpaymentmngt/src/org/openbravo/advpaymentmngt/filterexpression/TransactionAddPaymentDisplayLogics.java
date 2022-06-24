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
 * All portions are Copyright (C) 2014-2016 Openbravo SLU
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

@ComponentProvider.Qualifier(APRMConstants.TRANSACTION_WINDOW_ID)
public class TransactionAddPaymentDisplayLogics extends AddPaymentDisplayLogicsHandler {

  private static final long SEQUENCE = 100l;

  @Override
  protected long getSeq() {
    return SEQUENCE;
  }

  @Override
  public boolean getDocumentDisplayLogic(Map<String, String> requestMap) throws JSONException {
    // Document Type
    return true;
  }

  @Override
  public boolean getOrganizationDisplayLogic(Map<String, String> requestMap) throws JSONException {
    // Organization
    return false;
  }

  @Override
  public boolean getCreditToUseDisplayLogic(Map<String, String> requestMap) throws JSONException {
    JSONObject context = new JSONObject(requestMap.get("context"));
    if ((context.has("received_from") && !context.isNull("received_from")
        && !"".equals(context.getString("received_from")))
        || (context.has("inpreceivedFrom") && !context.isNull("inpreceivedFrom")
            && !"".equals(context.getString("inpreceivedFrom")))) {
      String document = !context.isNull("received_from") ? context.getString("trxtype")
          : context.getString("inptrxtype");
      String strBusinessPartner = !context.isNull("received_from")
          ? context.getString("received_from")
          : context.getString("inpreceivedFrom");
      if (getDefaultGeneratedCredit(requestMap).signum() == 0 || "RCIN".equals(document)) {
        BusinessPartner bpartner = OBDal.getInstance()
            .get(BusinessPartner.class, strBusinessPartner);
        Organization org = OBDal.getInstance().get(Organization.class, context.get("ad_org_id"));
        Currency currency = OBDal.getInstance().get(Currency.class, context.get("c_currency_id"));
        BigDecimal customerCredit = new AdvPaymentMngtDao().getCustomerCredit(bpartner,
            "RCIN".equals(document), org, currency);
        return customerCredit.signum() > 0;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  BigDecimal getDefaultGeneratedCredit(Map<String, String> requestMap) throws JSONException {
    return BigDecimal.ZERO;
  }

  @Override
  public boolean getBankStatementLineDisplayLogic(Map<String, String> requestMap)
      throws JSONException {
    JSONObject context = new JSONObject(requestMap.get("context"));

    // BankStatementLineDisplayLogic
    if (context.has("trxtype")) {
      return true;
    } else {
      return false;

    }

  }
}
