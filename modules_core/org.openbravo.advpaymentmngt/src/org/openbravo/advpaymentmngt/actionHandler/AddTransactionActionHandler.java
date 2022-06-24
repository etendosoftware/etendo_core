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
 * All portions are Copyright (C) 2014-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.advpaymentmngt.actionHandler;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.utility.APRM_MatchingUtility;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.model.sales.SalesRegion;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.json.JsonUtils;

public class AddTransactionActionHandler extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    final JSONObject result = new JSONObject();

    try {
      final JSONObject request = new JSONObject(content);
      final JSONObject params = request.getJSONObject("_params");

      final String strFinBankStatementLineId = params.getString("bankStatementLineId");
      final String strTabId = request.getString("inpTabId");
      final String strFinancialAccountId = request.getString("Fin_Financial_Account_ID");
      final String strTransactionType = params.getString("trxtype");
      final String strTransactionDate = params.getString("trxdate");
      final Date transactionDate = JsonUtils.createDateFormat().parse(strTransactionDate);
      final String selectedPaymentId = params.has("fin_payment_id")
          ? params.getString("fin_payment_id")
          : "";
      final String strGLItemId = params.has("c_glitem_id") ? params.getString("c_glitem_id") : "";
      final String strDepositAmount = params.getString("depositamt");
      final String strWithdrawalamt = params.getString("withdrawalamt");
      final String strDescription = params.has("description") ? params.getString("description")
          : "";

      createAndMatchTransaction(strTabId, strFinancialAccountId, selectedPaymentId,
          strTransactionType, strGLItemId, transactionDate, strFinBankStatementLineId,
          strDepositAmount, strWithdrawalamt, strDescription, params);
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Error when executing AddTransactionActionHandler", e);
      try {
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        final JSONArray actions = APRM_MatchingUtility.createMessageInProcessView(ex.getMessage(),
            "error");
        result.put("responseActions", actions);
        result.put("retryExecution", true);
      } catch (Exception e2) {
        log.error("Error message could not be built", e2);
      }
    }
    return result;
  }

  private void createAndMatchTransaction(String strTabId, String strFinancialAccountId,
      String selectedPaymentId, String strTransactionType, String strGLItemId, Date transactionDate,
      String strFinBankStatementLineId, String strDepositAmount, String strWithdrawalamt,
      String strDescription, JSONObject params) throws Exception {
    final VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    final ConnectionProvider conn = new DalConnectionProvider(false);

    try {
      OBContext.setAdminMode(true);

      Organization organization = null;
      final FIN_FinancialAccount account = OBDal.getInstance()
          .get(FIN_FinancialAccount.class, strFinancialAccountId);
      String description = "";
      GLItem glItem = null;
      FIN_Payment payment = null;
      boolean isReceipt = true;
      BigDecimal depositAmt = BigDecimal.ZERO;
      BigDecimal paymentAmt = BigDecimal.ZERO;

      Currency paymentCurrency = null;
      BigDecimal convertRate = null;
      BigDecimal sourceAmount = null;

      Campaign campaign = null;
      Project project = null;
      ABCActivity activity = null;
      SalesRegion salesRegion = null;
      Product product = null;
      BusinessPartner businessPartner = null;
      UserDimension1 user1 = null;
      UserDimension2 user2 = null;
      Costcenter costcenter = null;

      final FIN_BankStatementLine bankStatementLine = OBDal.getInstance()
          .get(FIN_BankStatementLine.class, strFinBankStatementLineId);
      // Accounting Dimensions
      final String strElement_OT = params.getString("ad_org_id");
      organization = OBDal.getInstance().get(Organization.class, strElement_OT);
      final String strElement_BP = params.getString("c_bpartner_id");
      businessPartner = OBDal.getInstance().get(BusinessPartner.class, strElement_BP);
      final String strElement_PR = params.getString("m_product_id");
      product = OBDal.getInstance().get(Product.class, strElement_PR);
      final String strElement_PJ = params.getString("c_project_id");
      project = OBDal.getInstance().get(Project.class, strElement_PJ);
      final String strElement_AY = params.getString("c_activity_id");
      activity = OBDal.getInstance().get(ABCActivity.class, strElement_AY);
      final String strElement_SR = params.getString("c_salesregion_id");
      salesRegion = OBDal.getInstance().get(SalesRegion.class, strElement_SR);
      final String strElement_MC = params.getString("c_campaign_id");
      campaign = OBDal.getInstance().get(Campaign.class, strElement_MC);
      final String strElement_U1 = params.getString("user1_id");
      user1 = OBDal.getInstance().get(UserDimension1.class, strElement_U1);
      final String strElement_U2 = params.getString("user2_id");
      user2 = OBDal.getInstance().get(UserDimension2.class, strElement_U2);
      final String strElement_CC = params.getString("c_costcenter_id");
      costcenter = OBDal.getInstance().get(Costcenter.class, strElement_CC);

      String paymentId = selectedPaymentId;
      String glitemId = strGLItemId;
      if (strTransactionType.equals("BF")) {
        paymentId = "null";
        glitemId = "null";
      }

      if (!paymentId.equals("null")) { // Payment
        payment = OBDal.getInstance().get(FIN_Payment.class, paymentId);
        depositAmt = FIN_Utility.getDepositAmount(payment.isReceipt(),
            payment.getFinancialTransactionAmount());
        paymentAmt = FIN_Utility.getPaymentAmount(payment.isReceipt(),
            payment.getFinancialTransactionAmount());
        isReceipt = payment.isReceipt();
        String paymentDescription = StringUtils.isNotBlank(payment.getDescription())
            ? payment.getDescription().replace("\n", ". ")
            : "";
        description = StringUtils.isNotBlank(strDescription) ? strDescription : paymentDescription;
        paymentCurrency = payment.getCurrency();
        convertRate = payment.getFinancialTransactionConvertRate();
        sourceAmount = payment.getAmount();
      } else if (!glitemId.equals("null")) {// GL item
        glItem = OBDal.getInstance().get(GLItem.class, glitemId);

        depositAmt = new BigDecimal(strDepositAmount);
        paymentAmt = new BigDecimal(strWithdrawalamt);
        isReceipt = (depositAmt.compareTo(paymentAmt) >= 0);
        description = (StringUtils.isBlank(strDescription) || strDescription.equals("null"))
            ? OBMessageUtils.messageBD("APRM_GLItem") + ": " + glItem.getName()
            : strDescription;
      } else { // Bank Fee or transaction without payment and gl item
        depositAmt = new BigDecimal(strDepositAmount);
        paymentAmt = new BigDecimal(strWithdrawalamt);
        isReceipt = (depositAmt.compareTo(paymentAmt) >= 0);
        description = StringUtils.isBlank(strDescription) ? OBMessageUtils.messageBD("APRM_BankFee")
            : strDescription;
      }

      APRM_MatchingUtility.createAndMatchFinancialTransaction(strFinancialAccountId,
          strTransactionType, transactionDate, strFinBankStatementLineId, organization, account,
          payment, description, glItem, isReceipt, depositAmt, paymentAmt, paymentCurrency,
          convertRate, sourceAmount, campaign, project, activity, salesRegion, product,
          businessPartner, user1, user2, costcenter, bankStatementLine, vars, conn, true);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
