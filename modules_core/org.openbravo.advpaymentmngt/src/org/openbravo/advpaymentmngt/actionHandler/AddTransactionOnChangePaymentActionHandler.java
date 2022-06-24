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

import java.math.BigDecimal;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

/**
 * Handler in Match Statement window | Add new transaction, that controls the Payment field on
 * change event
 * 
 */
public class AddTransactionOnChangePaymentActionHandler extends BaseActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject result = new JSONObject();

    try {
      OBContext.setAdminMode(true);
      final JSONObject jsonData = new JSONObject(data);
      String description = jsonData.getString("strDescription");
      if (jsonData.isNull("strPaymentId")) {
        description = FIN_Utility.getFinAccTransactionDescription(description, "", "");
        result.put("description", description);
        result.put("depositamt", BigDecimal.ZERO);
        result.put("paymentamt", BigDecimal.ZERO);
      } else {
        final String strPaymentId = jsonData.getString("strPaymentId");
        final FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, strPaymentId);
        if ((payment.isReceipt() && payment.getAmount().compareTo(BigDecimal.ZERO) > 0)
            || (!payment.isReceipt() && payment.getAmount().compareTo(BigDecimal.ZERO) < 0)) {
          result.put("depositamt", payment.getFinancialTransactionAmount().abs());
          result.put("paymentamt", BigDecimal.ZERO);
        } else {
          result.put("depositamt", BigDecimal.ZERO);
          result.put("paymentamt", payment.getFinancialTransactionAmount().abs());
        }
        if (payment.getBusinessPartner() != null) {
          result.put("cBpartnerId", payment.getBusinessPartner().getId());
        }
        if (payment.getDescription() != null) {
          description = FIN_Utility.getFinAccTransactionDescription(description, "",
              payment.getDescription());
          result.put("description", description);
        }
      }
    } catch (Exception e) {
      log.error("Error when executing AddTransactionOnChangePaymentActionHandler", e);
    } finally {
      OBContext.restorePreviousMode();
    }

    return result;
  }

}
