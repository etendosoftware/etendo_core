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

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.utility.APRMConstants;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.model.common.order.Order;

@ComponentProvider.Qualifier(APRMConstants.SALES_ORDER_WINDOW_ID)
public class SalesOrderAddPaymentDefaultValues extends AddPaymentDefaultValuesHandler {

  private static final long SEQUENCE = 100l;

  @Override
  protected long getSeq() {
    return SEQUENCE;
  }

  @Override
  public String getDefaultExpectedAmount(Map<String, String> requestMap) throws JSONException {
    // Expected amount is the amount pending to pay on the Sales Order
    JSONObject context = new JSONObject(requestMap.get("context"));
    String strOrderId = context.getString("inpcOrderId");
    BigDecimal pendingAmt = getPendingAmt(strOrderId);
    return pendingAmt.toPlainString();
  }

  @Override
  public String getDefaultActualAmount(Map<String, String> requestMap) throws JSONException {
    // Actual amount is the amount pending to pay on the Sales Order
    JSONObject context = new JSONObject(requestMap.get("context"));
    String strOrderId = context.getString("inpcOrderId");
    BigDecimal pendingAmt = getPendingAmt(strOrderId);
    return pendingAmt.toPlainString();
  }

  @Override
  public String getDefaultIsSOTrx(Map<String, String> requestMap) {
    return "Y";
  }

  @Override
  public String getDefaultTransactionType(Map<String, String> requestMap) {
    return "O";
  }

  private BigDecimal getPendingAmt(String strOrderId) {
    Order order = OBDal.getInstance().get(Order.class, strOrderId);
    BigDecimal pendingAmt = getPendingAmt(order.getFINPaymentScheduleList());
    return pendingAmt;
  }

  @Override
  public String getDefaultPaymentType(Map<String, String> requestMap) throws JSONException {
    return "";
  }

  @Override
  public String getDefaultOrderType(Map<String, String> requestMap) throws JSONException {
    JSONObject context = new JSONObject(requestMap.get("context"));
    return context.getString("inpcOrderId");
  }

  @Override
  public String getDefaultInvoiceType(Map<String, String> requestMap) throws JSONException {
    return "";
  }

  @Override
  public String getDefaultConversionRate(Map<String, String> requestMap) throws JSONException {
    return "";
  }

  @Override
  public String getDefaultConvertedAmount(Map<String, String> requestMap) throws JSONException {
    return "";
  }

  @Override
  public String getDefaultReceivedFrom(Map<String, String> requestMap) throws JSONException {
    // Business Partner of the current Sales Order
    JSONObject context = new JSONObject(requestMap.get("context"));
    Order order = getOrder(context);
    return order.getBusinessPartner().getId();
  }

  @Override
  public String getDefaultStandardPrecision(Map<String, String> requestMap) throws JSONException {
    // Standard Precision of the currency
    JSONObject context = new JSONObject(requestMap.get("context"));
    Order order = getOrder(context);
    return order.getCurrency().getStandardPrecision().toString();
  }

  @Override
  public String getDefaultCurrency(Map<String, String> requestMap) throws JSONException {
    // Currency of the current Sales Order
    JSONObject context = new JSONObject(requestMap.get("context"));
    Order order = getOrder(context);
    return order.getCurrency().getId();
  }

  @Override
  public String getOrganization(Map<String, String> requestMap) throws JSONException {
    // Organization of the current Sales Order
    return getOrder(new JSONObject(requestMap.get("context"))).getOrganization().getId();
  }

  Order getOrder(JSONObject context) throws JSONException {
    return OBDal.getInstance().get(Order.class, context.getString("inpcOrderId"));
  }

  @Override
  public String getDefaultPaymentDate(Map<String, String> requestMap) throws JSONException {
    return OBDateUtils.formatDate(new Date());
  }

  @Override
  public String getDefaultDocument(Map<String, String> requestMap) throws JSONException {
    // Document Type
    return "";
  }

  @Override
  public String getBankStatementLineAmount(Map<String, String> requestMap) throws JSONException {
    // BankStatementLineAmount
    return "";
  }

}
