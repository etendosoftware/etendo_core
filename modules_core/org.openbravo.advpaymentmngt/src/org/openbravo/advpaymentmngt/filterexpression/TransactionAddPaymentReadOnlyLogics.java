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

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.utility.APRMConstants;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;

@ComponentProvider.Qualifier(APRMConstants.TRANSACTION_WINDOW_ID)
public class TransactionAddPaymentReadOnlyLogics extends AddPaymentReadOnlyLogicsHandler {

  private static final long SEQUENCE = 100l;

  @Override
  protected long getSeq() {
    return SEQUENCE;
  }

  @Override
  public boolean getPaymentDocumentNoReadOnlyLogic(Map<String, String> requestMap)
      throws JSONException {
    return false;
  }

  @Override
  public boolean getReceivedFromReadOnlyLogic(Map<String, String> requestMap) throws JSONException {
    return false;
  }

  @Override
  public boolean getPaymentMethodReadOnlyLogic(Map<String, String> requestMap)
      throws JSONException {
    return false;
  }

  @Override
  public boolean getActualPaymentReadOnlyLogic(Map<String, String> requestMap)
      throws JSONException {
    JSONObject context = new JSONObject(requestMap.get("context"));
    String document = null;
    if (context.has("inptrxtype") && !context.isNull("inptrxtype")) {
      document = context.getString("inptrxtype");
    }
    if (context.has("trxtype") && !context.isNull("trxtype")) {
      document = context.getString("trxtype");
    }
    if (document != null) {
      if ("BPD".equals(document) || "RCIN".equals(document)) {
        return false;
      } else {
        return true;
      }
    } else if ((context.has("inpwindowId")
        && context.get("inpwindowId").equals(APRMConstants.TRANSACTION_WINDOW_ID))) {
      return false;
    } else {
      return true;
    }
  }

  @Override
  public boolean getPaymentDateReadOnlyLogic(Map<String, String> requestMap) throws JSONException {
    return false;
  }

  @Override
  public boolean getFinancialAccountReadOnlyLogic(Map<String, String> requestMap)
      throws JSONException {
    return true;
  }

  @Override
  public boolean getCurrencyReadOnlyLogic(Map<String, String> requestMap) throws JSONException {
    JSONObject context = new JSONObject(requestMap.get("context"));
    FIN_PaymentMethod paymentMethod = null;
    String trxtype = null;
    boolean readOnly = true;
    if (context.has("fin_paymentmethod_id") && !context.isNull("fin_paymentmethod_id")) {
      paymentMethod = OBDal.getInstance()
          .get(FIN_PaymentMethod.class, context.getString("fin_paymentmethod_id"));
    } else {
      paymentMethod = OBDal.getInstance()
          .get(FIN_PaymentMethod.class, getPaymentMethod(requestMap));
    }
    if (context.has("inptrxtype") && !context.isNull("inptrxtype")) {
      trxtype = context.getString("inptrxtype");
    }
    if (context.has("trxtype") && !context.isNull("trxtype")) {
      trxtype = context.getString("trxtype");
    }
    if (trxtype != null) {
      for (FinAccPaymentMethod finAccPaymentMethod : getFinancialAccount(requestMap)
          .getFinancialMgmtFinAccPaymentMethodList()) {
        if (trxtype.equals("RCIN") || trxtype.equals("BPD")) {
          if (finAccPaymentMethod.getPaymentMethod().equals(paymentMethod)
              && finAccPaymentMethod.isPayinIsMulticurrency()) {
            readOnly = false;
          }
        } else if (trxtype.equals("PDOUT") || trxtype.equals("BPW")) {
          if (finAccPaymentMethod.getPaymentMethod().equals(paymentMethod)
              && finAccPaymentMethod.isPayoutIsMulticurrency()) {
            readOnly = false;
          }
        }
      }
    }
    return readOnly;
  }

  private String getPaymentMethod(Map<String, String> requestMap) throws JSONException {
    boolean isReceipt = "Y".equals(getDefaultIsSOTrx(requestMap));

    FinAccPaymentMethod anyFinAccPaymentMethod = null;
    for (FinAccPaymentMethod finAccPaymentMethod : getFinancialAccount(requestMap)
        .getFinancialMgmtFinAccPaymentMethodList()) {
      if (finAccPaymentMethod.isDefault()) {
        if ((isReceipt && finAccPaymentMethod.isPayinAllow())
            || (!isReceipt && finAccPaymentMethod.isPayoutAllow())) {
          return finAccPaymentMethod.getPaymentMethod().getId();
        }
      }
      if ((isReceipt && finAccPaymentMethod.isPayinAllow())
          || (!isReceipt && finAccPaymentMethod.isPayoutAllow())) {
        anyFinAccPaymentMethod = finAccPaymentMethod;
      }
    }
    return anyFinAccPaymentMethod != null ? anyFinAccPaymentMethod.getPaymentMethod().getId() : "";
  }

  private FIN_FinancialAccount getFinancialAccount(Map<String, String> requestMap)
      throws JSONException {
    JSONObject context = new JSONObject(requestMap.get("context"));
    if (context.has("inpfinFinancialAccountId") && !context.isNull("inpfinFinancialAccountId")
        && !"".equals(context.getString("inpfinFinancialAccountId"))) {
      return OBDal.getInstance()
          .get(FIN_FinancialAccount.class, context.get("inpfinFinancialAccountId"));
    }
    if (context.has("fin_financial_account_id") && !context.isNull("fin_financial_account_id")
        && !"".equals(context.getString("fin_financial_account_id"))) {
      return OBDal.getInstance()
          .get(FIN_FinancialAccount.class, context.get("fin_financial_account_id"));
    }
    if (context.has("Fin_Financial_Account_ID") && !context.isNull("Fin_Financial_Account_ID")
        && !"".equals(context.getString("Fin_Financial_Account_ID"))) {
      return OBDal.getInstance()
          .get(FIN_FinancialAccount.class, context.get("Fin_Financial_Account_ID"));
    }
    return null;
  }

  private String getDefaultIsSOTrx(Map<String, String> requestMap) throws JSONException {
    JSONObject context = new JSONObject(requestMap.get("context"));
    String document = null;
    if (context.has("trxtype") && context.get("trxtype") != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString("trxtype"))) {
      document = context.getString("trxtype");
    } else if (context.has("inptrxtype") && context.get("inptrxtype") != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString("inptrxtype"))) {
      document = context.getString("inptrxtype");
    }
    if ("BPD".equals(document)) {
      return "Y";
    } else if ("BPW".equals(document)) {
      return "N";
    } else {
      return "";
    }
  }
}
