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
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;

@RequestScoped
// Public class to allow extend the functionality, for example Add Payment popup opening from menu
public abstract class AddPaymentDefaultValuesHandler {
  /**
   * Expected amount default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public abstract String getDefaultExpectedAmount(Map<String, String> requestMap)
      throws JSONException;

  /**
   * Actual Payment default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public abstract String getDefaultActualAmount(Map<String, String> requestMap)
      throws JSONException;

  /**
   * Expected Payment default value
   * 
   * @param requestMap
   *          requestMap with parameters
   */
  public abstract String getDefaultIsSOTrx(Map<String, String> requestMap) throws JSONException;

  /**
   * Transaction Type default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public abstract String getDefaultTransactionType(Map<String, String> requestMap)
      throws JSONException;

  /**
   * fin_payment_id default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public abstract String getDefaultPaymentType(Map<String, String> requestMap) throws JSONException;

  /**
   * c_order_id default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public abstract String getDefaultOrderType(Map<String, String> requestMap) throws JSONException;

  /**
   * c_invoice_id default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public abstract String getDefaultInvoiceType(Map<String, String> requestMap) throws JSONException;

  /**
   * Conversion rate default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public abstract String getDefaultConversionRate(Map<String, String> requestMap)
      throws JSONException;

  /**
   * Converted amount default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public abstract String getDefaultConvertedAmount(Map<String, String> requestMap)
      throws JSONException;

  /**
   * Business partner default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public abstract String getDefaultReceivedFrom(Map<String, String> requestMap)
      throws JSONException;

  /**
   * Standard Precision default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public abstract String getDefaultStandardPrecision(Map<String, String> requestMap)
      throws JSONException;

  /**
   * Currency default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public abstract String getDefaultCurrency(Map<String, String> requestMap) throws JSONException;

  /**
   * Organization default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public abstract String getOrganization(Map<String, String> requestMap) throws JSONException;

  /**
   * Document default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public abstract String getDefaultDocument(Map<String, String> requestMap) throws JSONException;

  /**
   * Payment date default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public abstract String getDefaultPaymentDate(Map<String, String> requestMap) throws JSONException;

  /**
   * Bank Statement Line Amount default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public abstract String getBankStatementLineAmount(Map<String, String> requestMap)
      throws JSONException;

  protected abstract long getSeq();

  /**
   * Currency to Amount default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public String getDefaultCurrencyTo(Map<String, String> requestMap) throws JSONException {
    String strContext = requestMap.get("context");
    if (strContext == null) {
      return "";
    }
    JSONObject context = new JSONObject(strContext);
    if (context.has("inpfinFinancialAccountId")
        && context.get("inpfinFinancialAccountId") != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString("inpfinFinancialAccountId"))) {
      FIN_FinancialAccount finFinancialAccount = OBDal.getInstance()
          .get(FIN_FinancialAccount.class, context.getString("inpfinFinancialAccountId"));
      return finFinancialAccount.getCurrency().getId();
    }
    String strBPartnerId = getDefaultReceivedFrom(requestMap);

    if (StringUtils.isNotEmpty(strBPartnerId)) {
      BusinessPartner businessPartner = OBDal.getInstance()
          .get(BusinessPartner.class, strBPartnerId);
      boolean isSOTrx = "Y".equals(getDefaultIsSOTrx(requestMap));
      if (isSOTrx && businessPartner.getAccount() != null) {
        return businessPartner.getAccount().getCurrency().getId();
      } else if (!isSOTrx && businessPartner.getPOFinancialAccount() != null) {
        return businessPartner.getPOFinancialAccount().getCurrency().getId();
      }
    }
    return null;
  }

  /**
   * Customer credit default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public String getDefaultCustomerCredit(Map<String, String> requestMap) throws JSONException {
    String strBusinessPartnerId = getDefaultReceivedFrom(requestMap);
    String strOrgId = getOrganization(requestMap);
    String strReceipt = getDefaultIsSOTrx(requestMap);
    if (StringUtils.isEmpty(strBusinessPartnerId) || strReceipt == null) {
      return null;
    }
    BusinessPartner bpartner = OBDal.getInstance().get(BusinessPartner.class, strBusinessPartnerId);
    Organization org = OBDal.getInstance().get(Organization.class, strOrgId);
    BigDecimal customerCredit = new AdvPaymentMngtDao().getCustomerCredit(bpartner,
        "Y".equals(strReceipt), org);
    return customerCredit.toPlainString();

  }

  /**
   * Document number default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public String getDefaultDocumentNo(Map<String, String> requestMap) throws JSONException {
    String strContext = requestMap.get("context");
    String strOrgId = getOrganization(requestMap);
    if (strContext == null) {
      return "";
    }

    Organization org = OBDal.getInstance().get(Organization.class, strOrgId);
    boolean isReceipt = "Y".equals(getDefaultIsSOTrx(requestMap));

    String strDocNo = FIN_Utility.getDocumentNo(org, isReceipt ? "ARR" : "APP", "FIN_Payment",
        false);

    return "<" + strDocNo + ">";
  }

  /**
   * Financial Account default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public String getDefaultFinancialAccount(Map<String, String> requestMap) throws JSONException {
    String strContext = requestMap.get("context");
    if (strContext == null) {
      return "";
    }
    JSONObject context = new JSONObject(strContext);
    if (context.has("inpfinFinancialAccountId")
        && context.get("inpfinFinancialAccountId") != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString("inpfinFinancialAccountId"))) {
      return context.getString("inpfinFinancialAccountId");
    } else if (context.has("Fin_Financial_Account_ID")
        && context.get("Fin_Financial_Account_ID") != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString("Fin_Financial_Account_ID"))) {
      return context.getString("Fin_Financial_Account_ID");
    }

    String currencyId = null;
    if (context.has("inpcCurrencyId") && context.get("inpcCurrencyId") != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString("inpcCurrencyId"))) {
      currencyId = (String) context.get("inpcCurrencyId");
    }

    String paymentMethodId = getDefaultPaymentMethod(requestMap);
    if (StringUtils.isNotEmpty(paymentMethodId) && context.has("inpadClientId")
        && context.has("inpadOrgId")) {

      String strBPartnerId = getDefaultReceivedFrom(requestMap);
      boolean isSOTrx = "Y".equals(getDefaultIsSOTrx(requestMap));
      if (StringUtils.isNotEmpty(strBPartnerId)) {

        BusinessPartner businessPartner = OBDal.getInstance()
            .get(BusinessPartner.class, strBPartnerId);
        if (isSOTrx && businessPartner.getAccount() != null
            && FIN_Utility.getFinancialAccountPaymentMethod(paymentMethodId,
                businessPartner.getAccount().getId(), isSOTrx, currencyId,
                context.getString("inpadOrgId")) != null) {
          return businessPartner.getAccount().getId();
        } else if (!isSOTrx && businessPartner.getPOFinancialAccount() != null
            && FIN_Utility.getFinancialAccountPaymentMethod(paymentMethodId,
                businessPartner.getPOFinancialAccount().getId(), isSOTrx, currencyId,
                context.getString("inpadOrgId")) != null) {
          return businessPartner.getPOFinancialAccount().getId();
        }
      }

      FinAccPaymentMethod fpm = FIN_Utility.getFinancialAccountPaymentMethod(paymentMethodId, null,
          isSOTrx, currencyId, context.getString("inpadOrgId"));
      if (fpm != null) {
        return fpm.getAccount().getId();
      }
    }

    return null;
  }

  private String getContextFinancialAccount(Map<String, String> requestMap) throws JSONException {
    String strContext = requestMap.get("context");
    if (strContext == null) {
      return null;
    }
    JSONObject context = new JSONObject(strContext);
    if (context.has("inpfinFinancialAccountId")
        && context.get("inpfinFinancialAccountId") != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString("inpfinFinancialAccountId"))) {
      return context.getString("inpfinFinancialAccountId");
    } else if (context.has("Fin_Financial_Account_ID")
        && context.get("Fin_Financial_Account_ID") != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString("Fin_Financial_Account_ID"))) {
      return context.getString("Fin_Financial_Account_ID");
    }
    return null;
  }

  /**
   * Payment method default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public String getDefaultPaymentMethod(Map<String, String> requestMap) throws JSONException {
    String strContext = requestMap.get("context");
    String strFinPaymentMethodId = null;
    String strFinancialAccountId = null;
    if (strContext == null) {
      return "";
    }
    JSONObject context = new JSONObject(strContext);
    if (context.has("inpfinPaymentmethodId")
        && context.get("inpfinPaymentmethodId") != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString("inpfinPaymentmethodId"))) {
      strFinPaymentMethodId = context.getString("inpfinPaymentmethodId");
    }

    boolean isSOTrx = "Y".equals(getDefaultIsSOTrx(requestMap));
    strFinancialAccountId = getContextFinancialAccount(requestMap);
    if (strFinPaymentMethodId != null
        && FIN_Utility.getFinancialAccountPaymentMethod(strFinPaymentMethodId,
            strFinancialAccountId, isSOTrx, null, context.getString("inpadOrgId")) != null) {
      return strFinPaymentMethodId;
    }

    String strBPartnerId = getDefaultReceivedFrom(requestMap);
    if (StringUtils.isNotEmpty(strBPartnerId) && context.has("inpadClientId")
        && context.has("inpadOrgId")) {

      BusinessPartner businessPartner = OBDal.getInstance()
          .get(BusinessPartner.class, strBPartnerId);

      if (isSOTrx && businessPartner.getPaymentMethod() != null
          && FIN_Utility.getFinancialAccountPaymentMethod(
              businessPartner.getPaymentMethod().getId(), strFinancialAccountId, isSOTrx, null,
              context.getString("inpadOrgId")) != null) {
        return businessPartner.getPaymentMethod().getId();
      } else if (!isSOTrx && businessPartner.getPOPaymentMethod() != null
          && FIN_Utility.getFinancialAccountPaymentMethod(
              businessPartner.getPOPaymentMethod().getId(), strFinancialAccountId, isSOTrx, null,
              context.getString("inpadOrgId")) != null) {
        return businessPartner.getPOPaymentMethod().getId();
      }
    }
    return null;
  }

  BigDecimal getPendingAmt(List<FIN_PaymentSchedule> pslist) {
    BigDecimal pendingAmt = BigDecimal.ZERO;
    for (FIN_PaymentSchedule ps : pslist) {
      List<FIN_PaymentScheduleDetail> psds = null;
      if (ps.getInvoice() != null) {
        psds = ps.getFINPaymentScheduleDetailInvoicePaymentScheduleList();
      } else {
        psds = ps.getFINPaymentScheduleDetailOrderPaymentScheduleList();
      }
      for (FIN_PaymentScheduleDetail psd : psds) {
        if (psd.getPaymentDetails() == null) {
          pendingAmt = pendingAmt.add(psd.getAmount());
        }
      }
    }
    return pendingAmt;
  }

  /**
   * Generated Credit default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public String getDefaultGeneratedCredit(Map<String, String> requestMap) throws JSONException {
    BigDecimal generateCredit = BigDecimal.ZERO;
    return generateCredit.toPlainString();
  }

  /**
   * Document category default value
   * 
   * @param requestMap
   *          map with parameters
   */
  public String getDefaultDocumentCategory(Map<String, String> requestMap) throws JSONException {
    boolean isSOTrx = "Y".equals(getDefaultIsSOTrx(requestMap));
    if (isSOTrx) {
      return "ARR";
    } else if (!isSOTrx) {
      return "APP";
    }
    return null;
  }

  public String getDefaultReferenceNo(Map<String, String> requestMap) throws JSONException {
    String strContext = requestMap.get("context");
    if (strContext == null) {
      return "";
    }
    JSONObject context = new JSONObject(strContext);
    if (!context.has("inpfinPaymentId")) {
      return null;
    }
    if (context.get("inpfinPaymentId") == JSONObject.NULL) {
      return null;
    }
    String finPaymentId = context.getString("inpfinPaymentId");
    if (!"".equals(finPaymentId)) {
      return OBDal.getInstance().get(FIN_Payment.class, finPaymentId).getReferenceNo();
    } else {
      return null;
    }
  }
}
