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

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.service.db.DbUtility;

public class AddPaymentOnProcessActionHandler extends BaseActionHandler {
  private static Logger log = LogManager.getLogger();

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject result = new JSONObject();
    JSONObject errorMessage = new JSONObject();
    OBContext.setAdminMode(true);
    try {
      final JSONObject jsonData = new JSONObject(data);
      boolean isReceipt = "true".equals(jsonData.getString("issotrx"));
      String strFinFinancialAccountId = jsonData.getString("finFinancialAccount");
      FIN_FinancialAccount finFinancialAccount = OBDal.getInstance()
          .get(FIN_FinancialAccount.class, strFinFinancialAccountId);
      String strBusinessPartnerId = null;

      if (jsonData.get("receivedFrom") != JSONObject.NULL) {

        strBusinessPartnerId = jsonData.getString("receivedFrom");
        BusinessPartner businessPartner = OBDal.getInstance()
            .get(BusinessPartner.class, strBusinessPartnerId);
        if (FIN_Utility.isBlockedBusinessPartner(businessPartner.getId(), isReceipt, 4)) {
          String strThebusinessPartner = OBMessageUtils.parseTranslation("@ThebusinessPartner@");
          String strBusinessPartnerBlocked = OBMessageUtils
              .parseTranslation("@BusinessPartnerBlocked@");
          errorMessage.put("severity", "error");
          errorMessage.put("title", "Error");
          errorMessage.put("text", strThebusinessPartner + " " + businessPartner.getName() + " "
              + strBusinessPartnerBlocked);
          result.put("message", errorMessage);
          return result;
        }

        // If the payment uses or generates credit, payment currency should map bp currency
        String currencyId = jsonData.getString("currencyId");
        boolean usesCredit = jsonData.getBoolean("usesCredit");
        boolean generatesCredit = jsonData.getBoolean("generatesCredit");

        if (businessPartner.getCurrency() == null) {
          String message = String.format(OBMessageUtils.messageBD("InitBPCurrencyLnk", false),
              businessPartner.getId(), businessPartner.getName());
          errorMessage.put("severity", "error");
          errorMessage.put("title", "Error");
          errorMessage.put("text", message);
          result.put("message", errorMessage);
          return result;
        } else {
          if ((usesCredit || generatesCredit)
              && !StringUtils.equals(currencyId, businessPartner.getCurrency().getId())) {
            String message = String.format(OBMessageUtils.messageBD("APRM_CreditCurrency"),
                businessPartner.getCurrency().getISOCode());
            errorMessage.put("severity", "error");
            errorMessage.put("title", "Error");
            errorMessage.put("text", message);
            result.put("message", errorMessage);
            return result;
          }
        }

      } else {
        JSONArray selectedPSDs = jsonData.getJSONArray("selectedRecords");
        for (int i = 0; i < selectedPSDs.length(); i++) {
          JSONObject psdRow = selectedPSDs.getJSONObject(i);
          strBusinessPartnerId = psdRow.getString("businessPartner");
          BusinessPartner businessPartner = OBDal.getInstance()
              .get(BusinessPartner.class, strBusinessPartnerId);
          if (FIN_Utility.isBlockedBusinessPartner(businessPartner.getId(), isReceipt, 4)) {
            String strThebusinessPartner = OBMessageUtils.parseTranslation("@ThebusinessPartner@");
            String strBusinessPartnerBlocked = OBMessageUtils
                .parseTranslation("@BusinessPartnerBlocked@");
            errorMessage.put("severity", "error");
            errorMessage.put("title", "Error");
            errorMessage.put("text", strThebusinessPartner + " " + businessPartner.getName() + " "
                + strBusinessPartnerBlocked);
            result.put("message", errorMessage);
            return result;
          }
        }
      }
      String message = "Ok";
      errorMessage.put("severity", "success");
      errorMessage.put("text", message);
      result.put("message", errorMessage);
      result.put("writeofflimit", finFinancialAccount.getWriteofflimit());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(e.getMessage(), e);
      try {
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("title", "Error");
        errorMessage.put("text", message);
        result.put("message", errorMessage);
      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
        // do nothing, give up
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }
}
