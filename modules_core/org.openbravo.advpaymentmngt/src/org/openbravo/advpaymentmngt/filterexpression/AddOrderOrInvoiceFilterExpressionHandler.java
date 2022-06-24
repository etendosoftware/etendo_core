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
 * All portions are Copyright (C) 2014-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.advpaymentmngt.filterexpression;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.OBBindingsConstants;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

@RequestScoped
abstract class AddOrderOrInvoiceFilterExpressionHandler {
  private static final Logger log = LogManager.getLogger();

  @Inject
  @Any
  private Instance<AddPaymentDefaultValuesHandler> addPaymentDefaultValuesHandlers;

  protected abstract long getSeq();

  /**
   * This method gets called to obtain the default filtering values for the grid. It can be
   * overwritten using Injections.
   * 
   * @return String containing the value for the filter expression
   */
  String getFilterExpression(Map<String, String> requestMap) throws JSONException {
    return getDefaultPaymentMethod(requestMap);
  }

  protected AddPaymentDefaultValuesHandler getDefaultsHandler(String strWindowId) {
    AddPaymentDefaultValuesHandler handler = null;

    for (AddPaymentDefaultValuesHandler nextHandler : addPaymentDefaultValuesHandlers
        .select(new ComponentProvider.Selector(strWindowId))) {
      if (handler == null) {
        handler = nextHandler;
      } else if (nextHandler.getSeq() < handler.getSeq()) {
        handler = nextHandler;
      } else if (nextHandler.getSeq() == handler.getSeq()) {
        log.warn(
            "Trying to get handler for window with id {}, there are more than one instance with the same sequence",
            strWindowId);
      }
    }
    return handler;
  }

  protected String getDefaultPaymentMethod(Map<String, String> requestMap) throws JSONException {
    final String strContext = requestMap.get("context");
    final JSONObject context = new JSONObject(strContext);
    final String strWindowId = context.getString(OBBindingsConstants.WINDOW_ID_PARAM);
    final AddPaymentDefaultValuesHandler handler = getDefaultsHandler(strWindowId);
    final String paymentMethodId = handler.getDefaultPaymentMethod(requestMap);
    if (context.has("inpfinPaymentId") && context.get("inpfinPaymentId") != JSONObject.NULL
        && StringUtils.isNotBlank(context.getString("inpfinPaymentId"))) {
      if (hasDetailsWithDifferentPaymentMethods((String) context.get("inpfinPaymentId"))) {
        return "";
      } else {
        return paymentMethodId;
      }
    }
    return paymentMethodId;
  }

  private boolean hasDetailsWithDifferentPaymentMethods(final String paymentId) {
    //@formatter:off
    final String hql = 
            "select coalesce(ipspm.id, opspm.id) as pm" +
            "  from FIN_Payment_ScheduleDetail as psd" +
            "    join psd.paymentDetails as pd" +
            "    left join psd.orderPaymentSchedule as ops" +
            "    left join ops.finPaymentmethod as opspm" +
            "    left join psd.invoicePaymentSchedule as ips" +
            "    left join ips.finPaymentmethod as ipspm" +
            " where pd.finPayment.id = :paymentId" +
            "   and pd.gLItem is null" +
            " group by coalesce(ipspm, opspm)";
  //@formatter:on

    final FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, paymentId);
    final List<String> paymentMethodIdList = OBDal.getInstance()
        .getSession()
        .createQuery(hql, String.class)
        .setParameter("paymentId", paymentId)
        .list();

    for (final String paymentMethodId : paymentMethodIdList) {
      if (!payment.getPaymentMethod().getId().equals(paymentMethodId)) {
        return true;
      }
    }
    return false;
  }
}
