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
 * All portions are Copyright (C) 2012-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.buildvalidation;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.base.ExecutionLimits;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.OpenbravoVersion;

/**
 * This validation is related to this issue: https://issues.openbravo.com/view.php?id=20142
 * 
 * NULL values are not considered equal for PostgreSQL unique constraints
 * http://www.postgresql.org/docs/9.0/static/ddl-constraints.html
 * 
 * If you are using PostgreSQL database you can have duplicated records in
 * C_CONVERSION_RATE_DOCUMENT table
 */
public class DuplicateDocExchangeRate extends BuildValidation {
  private static final String PAY = "PAY";
  private static final String INV = "INV";
  private static final String TRX = "TRX";

  private static final String SALES_INVOICE_HEADER_TAB = "263";
  private static final String PURCHASE_INVOICE_HEADER_TAB = "290";
  private static final String PAYMENT_IN_HEADER_TAB = "C4B6506838E14A349D6717D6856F1B56";
  private static final String PAYMENT_OUT_HEADER_TAB = "F7A52FDAAA0346EFA07D53C125B40404";
  private static final String TRANSACTION_HEADER_TAB = "23691259D1BD4496BCC5F32645BCA4B9";

  private static final String SALES_INV_WINDOW = "167";
  private static final String PURCHASE_INV_WINDOW = "183";
  private static final String PAYMENT_IN_WINDOW = "E547CE89D4C04429B6340FFA44E70716";
  private static final String PAYMENT_OUT_WINDOW = "6F8F913FA60F4CBD93DC1D3AA696E76E";
  private static final String FINANCIAL_ACCOUNT_WINDOW = "94EAA455D2644E04AB25D93BE5157B6D";

  @Override
  public List<String> execute() {
    ConnectionProvider cp = getConnectionProvider();
    ArrayList<String> errors = new ArrayList<String>();
    try {
      // Prevent error when upgrading from a pure 2.50
      // Check if APRM tables and C_Conversion_Rate_Document table exist
      if (DuplicateDocExchangeRateData.existConvRateDoctable(cp)) {

        String errorMessage = "It is not allowed to have the multiple exchange rates defined for the same pair of currencies (Currency From -> Currency To) in the same %s. "
            + "To fix this problem in your instance, you can know the duplicated entries by reviewing Alerts in your system. "
            + "Once you find the duplicated entries you should remove the wrong ones. After fixing all these entries you should be able to apply this MP.";

        // INVOICES
        DuplicateDocExchangeRateData[] listOfDupInvoices = DuplicateDocExchangeRateData
            .selectDupInvoiceExcRate(cp);
        if (listOfDupInvoices != null && listOfDupInvoices.length > 0) {
          errors.add(String.format(errorMessage, "INVOICE"));
        }
        for (DuplicateDocExchangeRateData dupInv : listOfDupInvoices) {
          processAlert(cp, dupInv, INV);
        }

        // PAYMENTS
        DuplicateDocExchangeRateData[] listOfDupPayments = DuplicateDocExchangeRateData
            .selectDupPaymentExcRate(cp);
        if (listOfDupPayments != null && listOfDupPayments.length > 0) {
          errors.add(String.format(errorMessage, "PAYMENT"));
        }
        for (DuplicateDocExchangeRateData dupPay : listOfDupPayments) {
          processAlert(cp, dupPay, PAY);
        }

        // TRANSACTIONS
        DuplicateDocExchangeRateData[] listOfDupTransactions = DuplicateDocExchangeRateData
            .selectDupTrxExcRate(cp);
        if (listOfDupTransactions != null && listOfDupTransactions.length > 0) {
          errors.add(String.format(errorMessage, "TRANSACTION"));
        }
        for (DuplicateDocExchangeRateData dupTrx : listOfDupTransactions) {
          processAlert(cp, dupTrx, TRX);
        }

      }
    } catch (Exception e) {
      return handleError(e);
    }
    return errors;
  }

  private void processAlert(ConnectionProvider cp, DuplicateDocExchangeRateData exchangeRate,
      String type) throws Exception {
    String ALERT_RULE_NAME = "Duplicated %s Exchange Rates";
    String alertDescription = "Duplicated %s Exchange Rate. Please ensure just one entry exists per %s";
    String strTabId = "";
    String strWindowId = "";
    String ALERT_RULE_SQL = "";
    String strRecordId = "";

    if (INV.equals(type)) {
      String invoice = "Y".equals(exchangeRate.issotrx) ? "Sales Invoice" : "Purchase Invoice";
      ALERT_RULE_NAME = String.format(ALERT_RULE_NAME, invoice);
      alertDescription = String.format(alertDescription, invoice, invoice);
      strTabId = "Y".equals(exchangeRate.issotrx) ? SALES_INVOICE_HEADER_TAB
          : PURCHASE_INVOICE_HEADER_TAB;
      strWindowId = "Y".equals(exchangeRate.issotrx) ? SALES_INV_WINDOW : PURCHASE_INV_WINDOW;
      strRecordId = exchangeRate.referencekeyId;

      ALERT_RULE_SQL = " select crd.c_invoice_id as referencekey_id, ad_column_identifier('C_INVOICE', crd.c_invoice_id,'en_US') as record_id, "
          + " 0 as ad_role_id, null as ad_user_id, '"
          + alertDescription
          + "' as description, "
          + " 'Y' as isActive, crd.ad_org_id, crd.ad_client_id, now() as created, 0 as createdBy, now() as updated, 0 as updatedBy "
          + " from c_conversion_rate_document crd join c_invoice i on (crd.c_invoice_id = i.c_invoice_id) "
          + " where fin_payment_id is null and aprm_finacc_transaction_v_id is null and i.issotrx = '"
          + exchangeRate.issotrx
          + "'"
          + " group by crd.c_currency_id, crd.c_currency_id_to, crd.c_invoice_id, crd.fin_payment_id, crd.aprm_finacc_transaction_v_id, "
          + "          crd.ad_org_id, crd.ad_client_id " + " having count(*) > 1";

    } else if (PAY.equals(type)) {
      String payment = "Y".equals(exchangeRate.isreceipt) ? "Payment IN" : "Payment OUT";
      ALERT_RULE_NAME = String.format(ALERT_RULE_NAME, payment);
      alertDescription = String.format(alertDescription, payment, payment);
      strTabId = "Y".equals(exchangeRate.isreceipt) ? PAYMENT_IN_HEADER_TAB
          : PAYMENT_OUT_HEADER_TAB;
      strWindowId = "Y".equals(exchangeRate.isreceipt) ? PAYMENT_IN_WINDOW : PAYMENT_OUT_WINDOW;
      strRecordId = exchangeRate.referencekeyId;

      ALERT_RULE_SQL = " select crd.fin_payment_id as referencekey_id, ad_column_identifier('FIN_PAYMENT', crd.fin_payment_id,'en_US') as record_id, "
          + " 0 as ad_role_id, null as ad_user_id, '"
          + alertDescription
          + "' as description, "
          + " 'Y' as isActive, crd.ad_org_id, crd.ad_client_id, now() as created, 0 as createdBy, now() as updated, 0 as updatedBy "
          + " from c_conversion_rate_document crd join fin_payment p on (crd.c_invoice_id = p.fin_payment_id) "
          + " where c_invoice_id is null and aprm_finacc_transaction_v_id is null and p.isreceipt = '"
          + exchangeRate.isreceipt
          + "' "
          + " group by crd.c_currency_id, crd.c_currency_id_to, crd.c_invoice_id, crd.fin_payment_id, crd.aprm_finacc_transaction_v_id, "
          + "          crd.ad_org_id, crd.ad_client_id" + " having count(*) > 1";

    } else if (TRX.equals(type)) {
      ALERT_RULE_NAME = String.format(ALERT_RULE_NAME, "Transaction");
      alertDescription = String.format(alertDescription, "Transaction", "Transaction");
      strTabId = TRANSACTION_HEADER_TAB;
      strWindowId = FINANCIAL_ACCOUNT_WINDOW;
      strRecordId = exchangeRate.referencekeyId;

      ALERT_RULE_SQL = " select crd.aprm_finacc_transaction_v_id as referencekey_id, ad_column_identifier('FIN_FINACC_TRANSACTION', crd.aprm_finacc_transaction_v_id,'en_US') as record_id, "
          + " 0 as ad_role_id, null as ad_user_id, '"
          + alertDescription
          + "' as description, "
          + " 'Y' as isActive, crd.ad_org_id, crd.ad_client_id, now() as created, 0 as createdBy,  now() as updated, 0 as updatedBy "
          + " from c_conversion_rate_document crd join fin_finacc_transaction ft on (crd.c_invoice_id = ft.fin_finacc_transaction_id) "
          + " where crd.c_invoice_id is null and crd.fin_payment_id is null "
          + " group by crd.c_currency_id, crd.c_currency_id_to, crd.c_invoice_id, crd.fin_payment_id, crd.aprm_finacc_transaction_v_id, "
          + "          crd.ad_org_id, crd.ad_client_id " + " having count(*) > 1";

    } else {
      // invalid type
    }

    String alertRuleId = "";

    // Check if exists the alert rule
    if (!DuplicateDocExchangeRateData.existsAlertRule(cp, ALERT_RULE_NAME, exchangeRate.adClientId)) {
      DuplicateDocExchangeRateData.insertAlertRule(cp, exchangeRate.adClientId,
          exchangeRate.adOrgId, ALERT_RULE_NAME, strTabId, ALERT_RULE_SQL);

      alertRuleId = DuplicateDocExchangeRateData.getAlertRuleId(cp, ALERT_RULE_NAME,
          exchangeRate.adClientId);
      DuplicateDocExchangeRateData[] roles = DuplicateDocExchangeRateData.getRoleId(cp,
          strWindowId, exchangeRate.adClientId);
      for (DuplicateDocExchangeRateData role : roles) {
        DuplicateDocExchangeRateData.insertAlertRecipient(cp, exchangeRate.adClientId,
            exchangeRate.adOrgId, alertRuleId, role.adRoleId);
      }
    } else {
      alertRuleId = DuplicateDocExchangeRateData.getAlertRuleId(cp, ALERT_RULE_NAME,
          exchangeRate.adClientId);
    }

    // Check if exist the concrete alert
    if (!DuplicateDocExchangeRateData.existsAlert(cp, alertRuleId, strRecordId)) {
      DuplicateDocExchangeRateData.insertAlert(cp, exchangeRate.adClientId, alertDescription,
          alertRuleId, exchangeRate.recordinfo, strRecordId);
    }

  }
  
  @Override
  protected ExecutionLimits getBuildValidationLimits() {
    return new ExecutionLimits("0", null, new OpenbravoVersion(3, 0, 17393));
  }
}
