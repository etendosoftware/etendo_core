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
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.utility.APRMConstants;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.service.json.JsonUtils;

@ComponentProvider.Qualifier(APRMConstants.TRANSACTION_WINDOW_ID)
public class TransactionAddPaymentDefaultValues extends AddPaymentDefaultValuesHandler {

  private static final long SEQUENCE = 100l;

  @Override
  protected long getSeq() {
    return SEQUENCE;
  }

  @Override
  public String getDefaultExpectedAmount(Map<String, String> requestMap) throws JSONException {
    return BigDecimal.ZERO.toPlainString();
  }

  @Override
  public String getDefaultActualAmount(Map<String, String> requestMap) throws JSONException {
    if ("Y".equals(getDefaultIsSOTrx(requestMap))) {
      JSONObject context = new JSONObject(requestMap.get("context"));
      if (context.has("depositamt") && context.has("withdrawalamt")) {
        return new BigDecimal(convertToSafeDecimalString(context.getString("depositamt")))
            .subtract(
                new BigDecimal(convertToSafeDecimalString(context.getString("withdrawalamt"))))
            .toString();
      }
      if (context.has("inpdepositamt") && context.has("inppaymentamt")) {
        return new BigDecimal(convertToSafeDecimalString(context.getString("inpdepositamt")))
            .subtract(
                new BigDecimal(convertToSafeDecimalString(context.getString("inppaymentamt"))))
            .toString();
      }
    }
    return BigDecimal.ZERO.toPlainString();
  }

  private String convertToSafeDecimalString(String bigdecimalString) {
    String localBigdecimalString = bigdecimalString;
    localBigdecimalString = localBigdecimalString.replaceAll("[^\\d,\\.]++", "");
    if (localBigdecimalString.matches(".+\\.\\d+,\\d+$")) {
      return localBigdecimalString.replaceAll("\\.", "").replaceAll(",", ".");
    }
    if (localBigdecimalString.matches(".+,\\d+\\.\\d+$")) {
      return localBigdecimalString.replaceAll(",", "");
    }
    return localBigdecimalString.replaceAll(",", ".");
  }

  @Override
  public String getDefaultIsSOTrx(Map<String, String> requestMap) throws JSONException {
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

  @Override
  public String getDefaultTransactionType(Map<String, String> requestMap) {
    return "I";
  }

  @Override
  public String getDefaultPaymentType(Map<String, String> requestMap) throws JSONException {
    return "";
  }

  @Override
  public String getDefaultOrderType(Map<String, String> requestMap) throws JSONException {
    return "";
  }

  @Override
  public String getDefaultInvoiceType(Map<String, String> requestMap) throws JSONException {
    return "";
  }

  @Override
  public String getDefaultConversionRate(Map<String, String> requestMap) throws JSONException {
    return BigDecimal.ONE.toPlainString();
  }

  @Override
  public String getDefaultConvertedAmount(Map<String, String> requestMap) throws JSONException {
    return BigDecimal.ZERO.toPlainString();
  }

  @Override
  public String getDefaultReceivedFrom(Map<String, String> requestMap) throws JSONException {
    JSONObject context = new JSONObject(requestMap.get("context"));
    if (context.has("c_bpartner_id") && context.get("c_bpartner_id") != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString("c_bpartner_id"))) {
      return context.getString("c_bpartner_id");
    }
    if (context.has("inpcBpartnerId") && context.get("inpcBpartnerId") != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString("inpcBpartnerId"))) {
      return context.getString("inpcBpartnerId");
    }
    return "";
  }

  @Override
  public String getDefaultStandardPrecision(Map<String, String> requestMap) throws JSONException {
    return getFinancialAccount(requestMap).getCurrency().getStandardPrecision().toString();
  }

  @Override
  public String getDefaultCurrency(Map<String, String> requestMap) throws JSONException {
    return getFinancialAccount(requestMap).getCurrency().getId().toString();
  }

  @Override
  public String getOrganization(Map<String, String> requestMap) throws JSONException {
    // Organization of the current Payment
    JSONObject context = new JSONObject(requestMap.get("context"));
    if (context.has("ad_org_id") && context.get("ad_org_id") != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString("ad_org_id"))) {
      return context.getString("ad_org_id");
    }
    if (context.has("inpadOrgId") && context.get("inpadOrgId") != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString("inpadOrgId"))) {
      return context.getString("inpadOrgId");
    }
    return getFinancialAccount(requestMap).getOrganization().getId();
  }

  @Override
  public String getDefaultPaymentMethod(Map<String, String> requestMap) throws JSONException {
    boolean isReceipt = "Y".equals(getDefaultIsSOTrx(requestMap));

    FinAccPaymentMethod anyFinAccPaymentMethod = null;
    for (FinAccPaymentMethod finAccPaymentMethod : getFinancialAccount(requestMap)
        .getFinancialMgmtFinAccPaymentMethodList()) {
      if (finAccPaymentMethod.isActive()) {
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
    }
    return anyFinAccPaymentMethod != null ? anyFinAccPaymentMethod.getPaymentMethod().getId() : "";
  }

  @Override
  public String getDefaultDocument(Map<String, String> requestMap) throws JSONException {
    // Document Type
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
      return "RCIN";
    } else if ("BPW".equals(document)) {
      return "PDOUT";
    } else {
      return "";
    }
  }

  @Override
  public String getDefaultPaymentDate(Map<String, String> requestMap) throws JSONException {
    JSONObject context = new JSONObject(requestMap.get("context"));
    String strTransactionDate = null;
    try {
      if (context.has("trxdate") && context.get("trxdate") != JSONObject.NULL
          && StringUtils.isNotEmpty(context.getString("trxdate"))) {
        strTransactionDate = context.getString("trxdate");
        Date transactionDate = JsonUtils.createDateFormat().parse(strTransactionDate);
        return OBDateUtils.formatDate(transactionDate);
      } else if (context.has("inpstatementdate") && !context.isNull("inpstatementdate")
          && !"".equals(context.getString("inpstatementdate"))) {
        strTransactionDate = context.getString("inpstatementdate");
      }
      if (strTransactionDate != null) {
        Date date = OBDateUtils.getDate(strTransactionDate);
        return OBDateUtils.formatDate(date);
      } else {
        return OBDateUtils.formatDate(new Date());
      }
    } catch (ParseException e) {
      return OBDateUtils.formatDate(new Date());
    }

  }

  private FIN_FinancialAccount getFinancialAccount(Map<String, String> requestMap)
      throws JSONException {
    JSONObject context = new JSONObject(requestMap.get("context"));
    String strFinancialAccount = null;
    if (context.has("inpfinFinancialAccountId")
        && context.get("inpfinFinancialAccountId") != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString("inpfinFinancialAccountId"))) {
      strFinancialAccount = context.getString("inpfinFinancialAccountId");
    } else if (context.has("Fin_Financial_Account_ID")
        && context.get("Fin_Financial_Account_ID") != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString("Fin_Financial_Account_ID"))) {
      strFinancialAccount = context.getString("Fin_Financial_Account_ID");
    }
    if (strFinancialAccount != null) {
      return OBDal.getInstance().get(FIN_FinancialAccount.class, strFinancialAccount);
    }
    return null;
  }

  @Override
  public String getBankStatementLineAmount(Map<String, String> requestMap) throws JSONException {
    JSONObject context = new JSONObject(requestMap.get("context"));
    if (context.has("depositamt") && context.has("withdrawalamt")
        && context.get("depositamt") != JSONObject.NULL
        && context.get("withdrawalamt") != JSONObject.NULL) {
      return new BigDecimal(convertToSafeDecimalString(context.getString("depositamt")))
          .subtract(new BigDecimal(convertToSafeDecimalString(context.getString("withdrawalamt"))))
          .toString();
    }
    if (context.has("inpdepositamt") && context.has("inppaymentamt")
        && context.get("inpdepositamt") != JSONObject.NULL
        && context.get("inppaymentamt") != JSONObject.NULL) {
      return new BigDecimal(convertToSafeDecimalString(context.getString("inpdepositamt")))
          .subtract(new BigDecimal(convertToSafeDecimalString(context.getString("inppaymentamt"))))
          .toString();
    }
    return BigDecimal.ZERO.toPlainString();
  }
}
