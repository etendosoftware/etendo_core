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

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.client.application.OBBindingsConstants;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;

public class AddPaymentDefaultValuesExpression implements FilterExpression {
  private static final Logger log = LogManager.getLogger();
  @Inject
  @Any
  private Instance<AddPaymentDefaultValuesHandler> addPaymentFilterExpressionHandlers;

  @Override
  public String getExpression(Map<String, String> requestMap) {
    String strCurrentParam = "";
    try {
      OBContext.setAdminMode(true);
      final String strWindowId = getWindowId(requestMap);

      AddPaymentDefaultValuesHandler handler = getHandler(strWindowId);
      if (handler == null) {
        throw new OBException(String.format(OBMessageUtils.messageBD("APRM_NOHANDLER")));
      }
      strCurrentParam = requestMap.get("currentParam");
      Parameters param = Parameters.getParameter(strCurrentParam);
      try {
        switch (param) {
          case ExpectedPayment:
            return handler.getDefaultExpectedAmount(requestMap);
          case ActualPayment:
            return handler.getDefaultActualAmount(requestMap);
          case CurrencyTo:
            return handler.getDefaultCurrencyTo(requestMap);
          case CustomerCredit:
            return handler.getDefaultCustomerCredit(requestMap);
          case DocumentNo:
            return handler.getDefaultDocumentNo(requestMap);
          case FinancialAccount:
            return handler.getDefaultFinancialAccount(requestMap);
          case IsSOTrx:
            return handler.getDefaultIsSOTrx(requestMap);
          case PaymentDate:
            return handler.getDefaultPaymentDate(requestMap);
          case PaymentMethod:
            return handler.getDefaultPaymentMethod(requestMap);
          case ReceivedFrom:
            return handler.getDefaultReceivedFrom(requestMap);
          case TransactionType:
            return handler.getDefaultTransactionType(requestMap);
          case Invoice:
            return handler.getDefaultInvoiceType(requestMap);
          case Order:
            return handler.getDefaultOrderType(requestMap);
          case Payment:
            return handler.getDefaultPaymentType(requestMap);
          case ConversionRate:
            return handler.getDefaultConversionRate(requestMap);
          case ConvertedAmount:
            return handler.getDefaultConvertedAmount(requestMap);
          case StandardPrecision:
            return handler.getDefaultStandardPrecision(requestMap);
          case GenerateCredit:
            return handler.getDefaultGeneratedCredit(requestMap);
          case DocumentCategory:
            return handler.getDefaultDocumentCategory(requestMap);
          case ReferenceNo:
            return handler.getDefaultReferenceNo(requestMap);
          case Currency:
            return handler.getDefaultCurrency(requestMap);
          case Organization:
            return handler.getOrganization(requestMap);
          case Document:
            return handler.getDefaultDocument(requestMap);
          case BankStatementLineAmount:
            return handler.getBankStatementLineAmount(requestMap);
        }
      } catch (Exception e) {
        log.error("Error trying to get default value of " + strCurrentParam + " " + e.getMessage(),
            e);
        return null;
      }
    } catch (JSONException ignore) {
    } finally {
      OBContext.restorePreviousMode();
    }
    log.error("No default value found for param: " + strCurrentParam);
    return null;
  }

  private String getWindowId(Map<String, String> requestMap) throws JSONException {
    final String strContext = requestMap.get("context");
    if (strContext != null) {
      JSONObject context = new JSONObject(strContext);
      if (context != null && context.has(OBBindingsConstants.WINDOW_ID_PARAM)) {
        return context.getString(OBBindingsConstants.WINDOW_ID_PARAM);
      }
    }
    return "NULLWINDOWID";
  }

  private AddPaymentDefaultValuesHandler getHandler(String strWindowId) {
    AddPaymentDefaultValuesHandler handler = null;
    for (AddPaymentDefaultValuesHandler nextHandler : addPaymentFilterExpressionHandlers
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

  private enum Parameters {
    ActualPayment("actual_payment"),
    ExpectedPayment("expected_payment"),
    DocumentNo("payment_documentno"),
    CurrencyTo("c_currency_to_id"),
    ReceivedFrom("received_from"),
    FinancialAccount("fin_financial_account_id"),
    PaymentDate("payment_date"),
    PaymentMethod("fin_paymentmethod_id"),
    TransactionType("transaction_type"),
    CustomerCredit("customer_credit"),
    IsSOTrx("issotrx"),
    Payment("fin_payment_id"),
    Invoice("c_invoice_id"),
    Order("c_order_id"),
    ConversionRate("conversion_rate"),
    ConvertedAmount("converted_amount"),
    StandardPrecision("StdPrecision"),
    GenerateCredit("generateCredit"),
    DocumentCategory("DOCBASETYPE"),
    ReferenceNo("reference_no"),
    Currency("c_currency_id"),
    Organization("ad_org_id"),
    Document("trxtype"),
    BankStatementLineAmount("bslamount");

    private String columnname;

    Parameters(String columnname) {
      this.columnname = columnname;
    }

    public String getColumnName() {
      return this.columnname;
    }

    static Parameters getParameter(String strColumnName) {
      for (Parameters parameter : Parameters.values()) {
        if (strColumnName.equals(parameter.getColumnName())) {
          return parameter;
        }
      }
      return null;
    }
  }

}
