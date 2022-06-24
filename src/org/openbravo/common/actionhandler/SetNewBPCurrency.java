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
package org.openbravo.common.actionhandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollableResults;
import org.openbravo.advpaymentmngt.process.FIN_PaymentProcess;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FIN_Payment_Credit;
import org.openbravo.service.db.DbUtility;

public class SetNewBPCurrency extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {

    JSONObject jsonRequest = null;
    OBContext.setAdminMode(true);
    try {
      jsonRequest = new JSONObject(content);
      JSONObject params = jsonRequest.getJSONObject("_params");
      final String strOrgId = jsonRequest.getString("inpadOrgId");
      final String strFromCurrencyId = jsonRequest.getString("inpbpCurrencyId");
      final String strToCurrencyId = params.getString("C_Currency_ID");
      final String strRate = params.getString("Rate");
      final String strAmount = params.getString("Foreign_Amount");
      final boolean strSetAmount = params.getBoolean("Amount");
      final boolean strUseDefaultConversion = params.getBoolean("Default_Conversion_Rate");
      final String strBpartnerId = jsonRequest.getString("C_BPartner_ID");
      final String glItemId = params.getString("c_glitem_id");
      BigDecimal creditUsed = BigDecimal.ZERO;
      BigDecimal rate = BigDecimal.ZERO;
      Double amount = 0d;
      if (strSetAmount && !"null".equals(strAmount)) {
        amount = Double.parseDouble(strAmount);
      }

      if (strUseDefaultConversion && !strSetAmount) {
        rate = getConversionRate(strOrgId, strFromCurrencyId, strToCurrencyId);
        if (rate == BigDecimal.ZERO && !strFromCurrencyId.equals(strToCurrencyId)) {
          try {
            jsonRequest = new JSONObject();
            String message = OBMessageUtils.messageBD("NoCurrencyConversion");
            JSONObject errorMessage = new JSONObject();
            errorMessage.put("severity", "error");
            errorMessage.put("text", message);
            jsonRequest.put("message", errorMessage);
          } catch (Exception e) {
            OBDal.getInstance().rollbackAndClose();
            log.error(e.getMessage(), e);
          }
          return jsonRequest;
        }
      } else {
        rate = "null".equals(strRate) ? BigDecimal.ZERO
            : BigDecimal.valueOf(Double.parseDouble(strRate));
      }
      BusinessPartner businessPartner = OBDal.getInstance()
          .get(BusinessPartner.class, strBpartnerId);
      creditUsed = businessPartner.getCreditUsed();

      ScrollableResults scroll = null;
      GLItem glItem = OBDal.getInstance().get(GLItem.class, glItemId);
      Currency currency = OBDal.getInstance().get(Currency.class, strToCurrencyId);
      BigDecimal creditAmount = BigDecimal.ZERO;
      BigDecimal creditRate = BigDecimal.ONE;
      BigDecimal reverseCreditRate = BigDecimal.ONE;

      // Convert available credit automatically
      if (!StringUtils.equals(strFromCurrencyId, strToCurrencyId) && !StringUtils.isEmpty(glItemId)
          && !StringUtils.equals(glItemId, "null")) {

        // Get the rate
        if (!strSetAmount) {
          creditRate = rate;
        } else if (creditUsed.compareTo(BigDecimal.ZERO) != 0) {
          creditRate = BigDecimal.valueOf(amount)
              .divide(creditUsed, FIN_Utility.getConversionRatePrecision(
                  RequestContext.get().getVariablesSecureApp()), RoundingMode.HALF_UP);
        }
        if (creditRate.compareTo(BigDecimal.ZERO) != 0) {
          reverseCreditRate = BigDecimal.ONE.divide(creditRate,
              FIN_Utility.getConversionRatePrecision(RequestContext.get().getVariablesSecureApp()),
              RoundingMode.HALF_UP);
        }

        // Loop through all payment documents which generate credit
        scroll = FinancialUtils.getPaymentsWithCredit(businessPartner.getId(), strFromCurrencyId);
        int i = 0;
        try {
          while (scroll.next()) {
            final String paymentCreditId = (String) scroll.get()[0];
            final FIN_Payment paymentCredit = OBDal.getInstance()
                .get(FIN_Payment.class, paymentCreditId);
            creditAmount = paymentCredit.getGeneratedCredit()
                .subtract(paymentCredit.getUsedCredit());

            // Create a payment to consume the credit with a glitem
            FIN_Payment payment1 = (FIN_Payment) DalUtil.copy(paymentCredit, false);
            payment1.setPaymentDate(new Date());
            payment1.setAmount(creditAmount);
            payment1.setDocumentNo(FIN_Utility.getDocumentNo(payment1.getOrganization(),
                payment1.getDocumentType().getDocumentCategory(), "DocumentNo_FIN_Payment"));
            payment1.setProcessed(false);
            payment1.setPosted("N");
            payment1.setDescription(null);
            payment1.setGeneratedCredit(BigDecimal.ZERO);
            payment1.setUsedCredit(BigDecimal.ZERO);

            // Create a payment detail to consume the credit with a glitem
            FIN_PaymentDetail paymentDetail1 = OBProvider.getInstance()
                .get(FIN_PaymentDetail.class);
            paymentDetail1.setClient(paymentCredit.getClient());
            paymentDetail1.setOrganization(paymentCredit.getOrganization());
            paymentDetail1.setFinPayment(payment1);
            paymentDetail1.setAmount(creditAmount);
            paymentDetail1.setRefund(false);
            paymentDetail1.setGLItem(glItem);
            paymentDetail1.setPrepayment(false);

            // Create a payment schedule detail to consume the credit with a glitem
            FIN_PaymentScheduleDetail paymentScheduleDetail1 = OBProvider.getInstance()
                .get(FIN_PaymentScheduleDetail.class);
            paymentScheduleDetail1.setClient(paymentCredit.getClient());
            paymentScheduleDetail1.setOrganization(paymentCredit.getOrganization());
            paymentScheduleDetail1.setPaymentDetails(paymentDetail1);
            paymentScheduleDetail1.setAmount(creditAmount);

            // Process the payment
            paymentDetail1.getFINPaymentScheduleDetailList().add(paymentScheduleDetail1);
            payment1.getFINPaymentDetailList().add(paymentDetail1);
            OBDal.getInstance().save(payment1);
            OBDal.getInstance().save(paymentDetail1);
            OBDal.getInstance().save(paymentScheduleDetail1);
            FIN_PaymentProcess.doProcessPayment(payment1, "D", null, null);

            // Modify description of original credit payment
            String paymentCreditDesc = paymentCredit.getDescription() + "\n" + String.format(
                OBMessageUtils.messageBD("APRM_CreditUsedPayment"), payment1.getDocumentNo());
            paymentCredit.setDescription((paymentCreditDesc.length() > 255)
                ? paymentCreditDesc.substring(0, 251).concat("...").toString()
                : paymentCreditDesc.toString());

            // Create a payment to refund the credit
            FIN_Payment payment2 = (FIN_Payment) DalUtil.copy(paymentCredit, false);
            payment2.setPaymentDate(new Date());
            payment2.setAmount(creditAmount.negate());
            payment2.setDocumentNo(FIN_Utility.getDocumentNo(payment2.getOrganization(),
                payment2.getDocumentType().getDocumentCategory(), "DocumentNo_FIN_Payment"));
            payment2.setProcessed(false);
            payment2.setPosted("N");
            payment2.setDescription(
                OBMessageUtils.messageBD("APRM_RefundPayment") + ": " + payment1.getDocumentNo());
            payment2.setGeneratedCredit(BigDecimal.ZERO);
            payment2.setUsedCredit(creditAmount);
            payment2.setFinancialTransactionAmount(
                paymentCredit.getFinancialTransactionAmount().negate());

            // Create a payment credit to refund the credit
            FIN_Payment_Credit paymentCredit2 = OBProvider.getInstance()
                .get(FIN_Payment_Credit.class);
            paymentCredit2.setClient(paymentCredit.getClient());
            paymentCredit2.setOrganization(paymentCredit.getOrganization());
            paymentCredit2.setPayment(payment2);
            paymentCredit2.setCreditPaymentUsed(paymentCredit);
            paymentCredit2.setAmount(creditAmount);
            paymentCredit2.setCurrency(paymentCredit.getCurrency());

            // Create a payment detail to refund the credit
            FIN_PaymentDetail paymentDetail2 = OBProvider.getInstance()
                .get(FIN_PaymentDetail.class);
            paymentDetail2.setClient(paymentCredit.getClient());
            paymentDetail2.setOrganization(paymentCredit.getOrganization());
            paymentDetail2.setFinPayment(payment2);
            paymentDetail2.setAmount(creditAmount.negate());
            paymentDetail2.setRefund(true);
            paymentDetail2.setPrepayment(true);

            // Create a payment schedule detail to refund the credit
            FIN_PaymentScheduleDetail paymentScheduleDetail2 = OBProvider.getInstance()
                .get(FIN_PaymentScheduleDetail.class);
            paymentScheduleDetail2.setClient(paymentCredit.getClient());
            paymentScheduleDetail2.setOrganization(paymentCredit.getOrganization());
            paymentScheduleDetail2.setPaymentDetails(paymentDetail2);
            paymentScheduleDetail2.setAmount(creditAmount.negate());

            // Process the payment
            paymentDetail2.getFINPaymentScheduleDetailList().add(paymentScheduleDetail2);
            payment2.getFINPaymentDetailList().add(paymentDetail2);
            payment2.getFINPaymentCreditList().add(paymentCredit2);
            paymentCredit.setUsedCredit(creditAmount);
            OBDal.getInstance().save(paymentCredit);
            OBDal.getInstance().save(payment2);
            OBDal.getInstance().save(paymentCredit2);
            OBDal.getInstance().save(paymentDetail2);
            OBDal.getInstance().save(paymentScheduleDetail2);
            FIN_PaymentProcess.doProcessPayment(payment2, "D", null, null);

            i++;
            if (i % 100 == 0) {
              OBDal.getInstance().flush();
              OBDal.getInstance().getSession().clear();
            }
          }

          // Set the new currency
          businessPartner.setCurrency(currency);

          // Loop through all payment documents which generate credit
          scroll.beforeFirst();
          i = 0;
          while (scroll.next()) {
            final String paymentCreditId = (String) scroll.get()[0];
            final FIN_Payment paymentCredit = OBDal.getInstance()
                .get(FIN_Payment.class, paymentCreditId);
            creditAmount = paymentCredit.getGeneratedCredit();

            // Create a payment to create the credit with a glitem
            FIN_Payment payment3 = (FIN_Payment) DalUtil.copy(paymentCredit, false);
            payment3.setPaymentDate(new Date());
            payment3.setCurrency(currency);
            payment3.setAmount(BigDecimal.ZERO);
            payment3.setDocumentNo(FIN_Utility.getDocumentNo(payment3.getOrganization(),
                payment3.getDocumentType().getDocumentCategory(), "DocumentNo_FIN_Payment"));
            payment3.setProcessed(false);
            payment3.setPosted("N");
            payment3.setDescription(null);
            final BigDecimal generatedCredit = creditAmount.multiply(creditRate)
                .setScale(currency.getStandardPrecision().intValue(), RoundingMode.HALF_UP);
            payment3.setGeneratedCredit(generatedCredit);
            payment3.setUsedCredit(BigDecimal.ZERO);
            payment3.setFinancialTransactionConvertRate(reverseCreditRate);
            payment3.setFinancialTransactionAmount(generatedCredit);

            // Create a payment detail to create the credit with a glitem
            FIN_PaymentDetail paymentDetail3 = OBProvider.getInstance()
                .get(FIN_PaymentDetail.class);
            paymentDetail3.setClient(paymentCredit.getClient());
            paymentDetail3.setOrganization(paymentCredit.getOrganization());
            paymentDetail3.setFinPayment(payment3);
            paymentDetail3.setAmount(generatedCredit);
            paymentDetail3.setRefund(false);
            paymentDetail3.setPrepayment(true);

            // Create a payment detail to create the credit with a glitem
            FIN_PaymentDetail paymentDetail4 = OBProvider.getInstance()
                .get(FIN_PaymentDetail.class);
            paymentDetail4.setClient(paymentCredit.getClient());
            paymentDetail4.setOrganization(paymentCredit.getOrganization());
            paymentDetail4.setFinPayment(payment3);
            paymentDetail4.setAmount(generatedCredit.negate());
            paymentDetail4.setGLItem(glItem);
            paymentDetail4.setRefund(false);
            paymentDetail4.setPrepayment(false);

            // Create a payment schedule detail to create the credit with a glitem
            FIN_PaymentScheduleDetail paymentScheduleDetail3 = OBProvider.getInstance()
                .get(FIN_PaymentScheduleDetail.class);
            paymentScheduleDetail3.setClient(paymentCredit.getClient());
            paymentScheduleDetail3.setOrganization(paymentCredit.getOrganization());
            paymentScheduleDetail3.setPaymentDetails(paymentDetail3);
            paymentScheduleDetail3.setAmount(generatedCredit);

            // Create a payment schedule detail to create the credit with a glitem
            FIN_PaymentScheduleDetail paymentScheduleDetail4 = OBProvider.getInstance()
                .get(FIN_PaymentScheduleDetail.class);
            paymentScheduleDetail4.setClient(paymentCredit.getClient());
            paymentScheduleDetail4.setOrganization(paymentCredit.getOrganization());
            paymentScheduleDetail4.setPaymentDetails(paymentDetail4);
            paymentScheduleDetail4.setAmount(generatedCredit.negate());

            // Process the payment
            paymentDetail3.getFINPaymentScheduleDetailList().add(paymentScheduleDetail3);
            paymentDetail4.getFINPaymentScheduleDetailList().add(paymentScheduleDetail4);
            payment3.getFINPaymentDetailList().add(paymentDetail3);
            payment3.getFINPaymentDetailList().add(paymentDetail4);
            OBDal.getInstance().save(payment3);
            OBDal.getInstance().save(paymentDetail3);
            OBDal.getInstance().save(paymentDetail4);
            OBDal.getInstance().save(paymentScheduleDetail3);
            OBDal.getInstance().save(paymentScheduleDetail4);
            OBDal.getInstance().save(paymentCredit);
            FIN_PaymentProcess.doProcessPayment(payment3, "D", null, null);

            i++;
            if (i % 100 == 0) {
              OBDal.getInstance().flush();
              OBDal.getInstance().getSession().clear();
            }
          }
        } finally {
          scroll.close();
        }
      }

      if (strSetAmount && creditUsed.compareTo(BigDecimal.valueOf(amount)) != 0) {
        businessPartner.setCreditUsed(BigDecimal.valueOf(amount));
      }
      if (!strToCurrencyId.equals(strFromCurrencyId) && strToCurrencyId != null
          && !"null".equals(strToCurrencyId)) {
        businessPartner.setCurrency(OBDal.getInstance().get(Currency.class, strToCurrencyId));
        if (rate.compareTo(BigDecimal.ZERO) != 0 && creditUsed.compareTo(BigDecimal.ZERO) != 0
            && !strSetAmount) {
          businessPartner.setCreditUsed(creditUsed.multiply(rate));
        }
      }

      String messageText = OBMessageUtils.messageBD("CurrencyUpdated");
      JSONObject msg = new JSONObject();
      msg.put("severity", "success");
      msg.put("text", OBMessageUtils.parseTranslation(messageText));
      jsonRequest.put("message", msg);

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Error in set new currency Action Handler", e);

      try {
        jsonRequest = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonRequest.put("message", errorMessage);
      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
        // do nothing, give up
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonRequest;
  }

  private BigDecimal getConversionRate(String strOrgId, String strFromCurrencyId,
      String strToCurrencyId) {
    final Date today = new Date();
    BigDecimal exchangeRate = BigDecimal.ZERO;
    // Apply default conversion rate
    int conversionRatePrecision = FIN_Utility
        .getConversionRatePrecision(RequestContext.get().getVariablesSecureApp());
    Organization organization = OBDal.getInstance().get(Organization.class, strOrgId);
    Currency fromCurrency = OBDal.getInstance().get(Currency.class, strFromCurrencyId);
    Currency toCurrency = OBDal.getInstance().get(Currency.class, strToCurrencyId);
    final ConversionRate conversionRate = FIN_Utility.getConversionRate(fromCurrency, toCurrency,
        today, organization);
    if (conversionRate != null) {
      exchangeRate = conversionRate.getMultipleRateBy()
          .setScale(conversionRatePrecision, RoundingMode.HALF_UP);
    } else {
      exchangeRate = BigDecimal.ZERO;
    }
    return exchangeRate;
  }
}
