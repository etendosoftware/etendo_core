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
 * All portions are Copyright (C) 2012-2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.advpaymentmngt.actionHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.DoubtfulDebt;
import org.openbravo.model.financialmgmt.payment.DoubtfulDebtRun;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DbUtility;

public class DoubtFulDebtPickEditLines extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    JSONObject errorMessage = new JSONObject();
    try {
      OBContext.setAdminMode(true);
      log.debug(jsonRequest);
      errorMessage.put("severity", "success");
      errorMessage.put("text", OBMessageUtils.messageBD("Success"));
      jsonRequest = new JSONObject(content);

      // When the focus is NOT in the tab of the button (i.e. any child tab) and the tab does not
      // contain any record, the inpfinDoubtfulDebtRunId parameter contains "null" string. Use
      // FIN_Doubtful_Debt_Run_ID instead because it always contains the id of the selected doubtful
      // debt run. Issue 20585: https://issues.openbravo.com/view.php?id=20585
      final String strDoubtFulDebtRunId = jsonRequest.getString("FIN_Doubtful_Debt_Run_ID");
      final DoubtfulDebtRun doubtfulDebtRun = OBDal.getInstance()
          .get(DoubtfulDebtRun.class, strDoubtFulDebtRunId);

      if (doubtfulDebtRun != null) {
        List<String> idList = OBDao.getIDListFromOBObject(doubtfulDebtRun.getFINDoubtfulDebtList());
        errorMessage = createDoubtfulDebt(doubtfulDebtRun, jsonRequest, idList);
      }

      jsonRequest.put("message", errorMessage);

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("DoubtFulDebtPickeditLines error: " + e.getMessage(), e);

      Throwable ex = DbUtility.getUnderlyingSQLException(e);
      String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
      try {
        errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonRequest.put("message", errorMessage);
      } catch (JSONException ignore) {
        log.error("DoubtFulDebtPickeditLines error: " + ignore.getMessage(), ignore);
      }

    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonRequest;
  }

  private JSONObject createDoubtfulDebt(DoubtfulDebtRun doubtfulDebtRun, JSONObject jsonRequest,
      List<String> idList) throws JSONException {
    final JSONArray selectedLines = jsonRequest.getJSONObject("_params")
        .getJSONObject("grid")
        .getJSONArray("_selection");
    DocumentType documentType = null;
    Currency currency = null;
    JSONObject message = new JSONObject();
    message.put("severity", "success");

    DoubtfulDebt newDoubtfulDebt = null;
    int cont = 0;
    for (int i = 0; i < selectedLines.length(); i++) {
      JSONObject selectedLine = selectedLines.getJSONObject(i);

      BigDecimal amount = new BigDecimal(selectedLine.getString("doubtfulDebtAmount"));

      if (amount.compareTo(BigDecimal.ZERO) != 0) {
        String strDebtdId = selectedLine.getString("fINDoubtfulDebt");
        String strPaymentSchedule = selectedLine.getString("finPaymentSchedule");
        String strCurrency = selectedLine.getString("currency");
        FIN_PaymentSchedule paymentSchedule = (FIN_PaymentSchedule) OBDal.getInstance()
            .getProxy(FIN_PaymentSchedule.ENTITY_NAME, strPaymentSchedule);
        boolean notExistsDebtLine = idList.contains(strDebtdId);
        if (notExistsDebtLine) {
          newDoubtfulDebt = OBDal.getInstance().get(DoubtfulDebt.class, strDebtdId);
          idList.remove(strDebtdId);
        } else {
          newDoubtfulDebt = OBProvider.getInstance().get(DoubtfulDebt.class);
          if (documentType == null) {
            documentType = getDoubtfulDebtDocumentType(doubtfulDebtRun.getClient(),
                doubtfulDebtRun.getOrganization());
          }
          if (currency == null) {
            currency = (Currency) OBDal.getInstance().getProxy(Currency.ENTITY_NAME, strCurrency);
          }
          int stdPrecision = 2;
          stdPrecision = currency.getStandardPrecision().intValue();
          amount = amount.setScale(stdPrecision, RoundingMode.HALF_UP);
          newDoubtfulDebt.setClient(doubtfulDebtRun.getClient());
          newDoubtfulDebt.setOrganization(doubtfulDebtRun.getOrganization());
          newDoubtfulDebt.setAccountingDate(doubtfulDebtRun.getAccountingDate());
          newDoubtfulDebt.setDescription(doubtfulDebtRun.getDescription());
          newDoubtfulDebt.setDocumentNo(FIN_Utility.getDocumentNo(documentType,
              documentType.getTable() != null ? documentType.getTable().getDBTableName() : ""));
          newDoubtfulDebt.setCurrency(currency);
          newDoubtfulDebt.setFINDoubtfulDebtRun(doubtfulDebtRun);
          newDoubtfulDebt.setDocumentType(documentType);
          newDoubtfulDebt.setFINPaymentSchedule(paymentSchedule);
          // Dimensions
          newDoubtfulDebt.setBusinessPartner(paymentSchedule.getInvoice().getBusinessPartner());
          newDoubtfulDebt.setProject(paymentSchedule.getInvoice().getProject());
          newDoubtfulDebt.setCostCenter(paymentSchedule.getInvoice().getCostcenter());
          newDoubtfulDebt.setStDimension(paymentSchedule.getInvoice().getStDimension());
          newDoubtfulDebt.setNdDimension(paymentSchedule.getInvoice().getNdDimension());
          newDoubtfulDebt.setSalesCampaign(paymentSchedule.getInvoice().getSalesCampaign());
          newDoubtfulDebt.setActivity(paymentSchedule.getInvoice().getActivity());

          OBDal.getInstance().save(newDoubtfulDebt);
          OBDal.getInstance().save(doubtfulDebtRun);
        }
        newDoubtfulDebt.setAmount(amount);
      }

      cont++;
    }

    OBDal.getInstance().flush();

    removeNonSelectedLines(idList, doubtfulDebtRun);
    message.put("text", cont + " " + OBMessageUtils.messageBD("RowsInserted"));
    return message;
  }

  private void removeNonSelectedLines(List<String> idList, DoubtfulDebtRun doubtfulDebtRun) {
    if (idList.size() > 0) {
      for (String id : idList) {
        DoubtfulDebt dd = OBDal.getInstance().get(DoubtfulDebt.class, id);
        doubtfulDebtRun.getFINDoubtfulDebtList().remove(dd);
        OBDal.getInstance().remove(dd);
      }
      OBDal.getInstance().save(doubtfulDebtRun);
      OBDal.getInstance().flush();
    }
  }

  private DocumentType getDoubtfulDebtDocumentType(Client client, Organization organization) {
    final List<Object> parameters = new ArrayList<Object>();
    parameters.add(client.getId());
    parameters.add(organization.getId());
    parameters.add("DDB");
    String strDocTypeId = (String) CallStoredProcedure.getInstance()
        .call("AD_GET_DOCTYPE", parameters, null);
    if (strDocTypeId == null || "".equals(strDocTypeId)) {
      throw new OBException("@APRM_DoubtfulDebtNoDocument@");
    }
    return OBDal.getInstance().get(DocumentType.class, strDocTypeId);
  }
}
