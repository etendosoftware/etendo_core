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
import org.openbravo.model.common.order.Order;

@ComponentProvider.Qualifier(APRMConstants.SALES_ORDER_WINDOW_ID)
public class SalesOrderAddPaymentDisplayLogics extends AddPaymentDisplayLogicsHandler {

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
    // "@customer_credit@ > 0 & @received_from@!'' & (@issotrx@ == true | @generateCredit@ == 0)";
    JSONObject context = new JSONObject(requestMap.get("context"));
    Order order = getOrder(context);
    BusinessPartner bpartner = order.getBusinessPartner();
    if (bpartner != null) {
      Organization org = order.getOrganization();
      Currency currency = order.getCurrency();
      BigDecimal customerCredit = new AdvPaymentMngtDao().getCustomerCredit(bpartner, true, org,
          currency);
      return customerCredit.signum() > 0;
    } else {
      return false;
    }
  }

  private Order getOrder(JSONObject context) throws JSONException {
    return OBDal.getInstance().get(Order.class, context.getString("inpcOrderId"));
  }

  @Override
  public boolean getBankStatementLineDisplayLogic(Map<String, String> requestMap)
      throws JSONException {
    // BankStatementLineDisplayLogic
    return false;
  }
}
