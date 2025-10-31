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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.Dependent;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.utility.APRMConstants;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.service.json.JsonUtils;

@Dependent
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
      JSONObject context = new JSONObject(requestMap.get(APRMConstants.CONTEXT));
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

  /**
   * Converts a given string representation of a BigDecimal into a safe format for processing.
   *
   * The method performs the following transformations:
   * - If the input string is in scientific notation, it is returned unchanged. The detection is
   *   performed using a precompiled regular expression pattern.
   * - Any characters that are not digits, commas, or periods are removed from the string.
   * - The method detects and handles European and Anglo-Saxon number formats using precompiled
   *   patterns:
   *     - European format (e.g., "1.234,56") is converted to "1234.56" by removing the thousands
   *       separator (period) and replacing the decimal separator (comma) with a period.
   *     - Anglo-Saxon format (e.g., "1,234.56") is converted to "1234.56" by removing the commas
   *       used as thousands separators.
   * - If no specific format is detected, the method defaults to replacing any remaining commas
   *   with periods.
   *
   * @param bigdecimalString The string representing a BigDecimal, which may include commas, periods,
   *                         or scientific notation (e.g., "1.214859023E7" or "12,148,590.23").
   * @return A properly formatted string that can be parsed as a BigDecimal, where commas are either
   *         removed (if used as thousands separators) or converted to periods (if used as decimal separators).
   */
  private String convertToSafeDecimalString(String bigdecimalString) {
    String localBigdecimalString = bigdecimalString;
    Pattern scientificNotationPattern = Pattern.compile("^[+-]?\\d+(\\.\\d+)?[eE][+-]?\\d+$");
    Matcher matcher = scientificNotationPattern.matcher(bigdecimalString);

    // If the string is in scientific notation, we return it directly
    if (matcher.matches()) {
      return localBigdecimalString;
    }

    // Any character that is not a digit, comma, or period is removed.
    localBigdecimalString = localBigdecimalString.replaceAll("[^\\d,\\.]++", "");

    // European format check
    Pattern europeanFormatPattern = Pattern.compile(".+\\.\\d+,\\d+$");
    matcher = europeanFormatPattern.matcher(localBigdecimalString);
    if (matcher.matches()) {
      return localBigdecimalString.replaceAll("\\.", "").replaceAll(",", ".");
    }

    // Anglo-Saxon format check
    Pattern angloSaxonFormatPattern = Pattern.compile(".+,\\d+\\.\\d+$");
    matcher = angloSaxonFormatPattern.matcher(localBigdecimalString);
    if (matcher.matches()) {
      return localBigdecimalString.replaceAll(",", "");
    }

    // Basic conversion, all commas are replaced by periods
    return localBigdecimalString.replaceAll(",", ".");
  }

  @Override
  public String getDefaultIsSOTrx(Map<String, String> requestMap) throws JSONException {
    JSONObject context = new JSONObject(requestMap.get(APRMConstants.CONTEXT));
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
    JSONObject context = new JSONObject(requestMap.get(APRMConstants.CONTEXT));
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
    JSONObject context = new JSONObject(requestMap.get(APRMConstants.CONTEXT));
    if (context.has("ad_org_id") && context.get("ad_org_id") != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString("ad_org_id"))) {
      return context.getString("ad_org_id");
    }
    if (context.has(APRMConstants.INPAD_ORG_ID) && context.get(APRMConstants.INPAD_ORG_ID) != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString(APRMConstants.INPAD_ORG_ID))) {
      return context.getString(APRMConstants.INPAD_ORG_ID);
    }
    return getFinancialAccount(requestMap).getOrganization().getId();
  }

  @Override
  public String getDefaultPaymentMethod(Map<String, String> requestMap) throws JSONException {
    JSONObject context = new JSONObject(requestMap.get(APRMConstants.CONTEXT));
    boolean isReceipt = StringUtils.equals("Y", getDefaultIsSOTrx(requestMap));
    String bpartnerId = getDefaultReceivedFrom(requestMap);
    if (StringUtils.isNotEmpty(bpartnerId)) {
      BusinessPartner businessPartner = OBDal.getInstance().get(BusinessPartner.class, bpartnerId);
      if (isReceipt && businessPartner.getPaymentMethod() != null && businessPartner.getAccount() != null &&
          FIN_Utility.getFinancialAccountPaymentMethod(
              businessPartner.getPaymentMethod().getId(), businessPartner.getAccount().getId(), isReceipt, null,
              context.getString(APRMConstants.INPAD_ORG_ID)) != null) {
        return businessPartner.getPaymentMethod().getId();
      } else if (!isReceipt && businessPartner.getPOPaymentMethod() != null && businessPartner.getPOFinancialAccount() != null &&
          FIN_Utility.getFinancialAccountPaymentMethod(
              businessPartner.getPOPaymentMethod().getId(), businessPartner.getPOFinancialAccount().getId(), !isReceipt,
              null, context.getString(APRMConstants.INPAD_ORG_ID)) != null) {
        return businessPartner.getPOPaymentMethod().getId();
      }
    }
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
    JSONObject context = new JSONObject(requestMap.get(APRMConstants.CONTEXT));
    return FIN_Utility.getDefaultAddPaymentDocument(context);
  }

  @Override
  public String getDefaultPaymentDate(Map<String, String> requestMap) throws JSONException {
    JSONObject context = new JSONObject(requestMap.get(APRMConstants.CONTEXT));
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
    JSONObject context = new JSONObject(requestMap.get(APRMConstants.CONTEXT));
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
    JSONObject context = new JSONObject(requestMap.get(APRMConstants.CONTEXT));
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
