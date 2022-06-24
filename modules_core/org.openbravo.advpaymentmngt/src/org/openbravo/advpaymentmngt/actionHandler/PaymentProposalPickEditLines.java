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
 * All portions are Copyright (C) 2012 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.advpaymentmngt.actionHandler;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentPropDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentProposal;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;

public class PaymentProposalPickEditLines extends BaseProcessActionHandler {
  private static Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    OBContext.setAdminMode();
    try {
      jsonRequest = new JSONObject(content);
      log.debug(jsonRequest);
      // When the focus is NOT in the tab of the button (i.e. any child tab) and the tab does not
      // contain any record, the inppaymentproposal parameter contains "null" string. Use
      // Fin_Payment_Proposal_ID
      // instead because it always contains the id of the selected order.
      // Issue 20585: https://issues.openbravo.com/view.php?id=20585
      final String strPaymentProposalId = jsonRequest.getString("Fin_Payment_Proposal_ID");
      FIN_PaymentProposal paymentProposal = OBDal.getInstance()
          .get(FIN_PaymentProposal.class, strPaymentProposalId);
      final String strPaymentMethodId = jsonRequest.getString("inpfinPaymentmethodId");
      FIN_PaymentMethod paymentMethod = OBDal.getInstance()
          .get(FIN_PaymentMethod.class, strPaymentMethodId);

      List<String> idList = OBDao
          .getIDListFromOBObject(paymentProposal.getFINPaymentPropDetailList());
      HashMap<String, String> map = createPaymentProposalDetails(jsonRequest, paymentMethod,
          idList);
      jsonRequest = new JSONObject();

      JSONObject errorMessage = new JSONObject();
      errorMessage.put("severity", "success");
      errorMessage.put("text", OBMessageUtils.messageBD("Success"));
      if (map.get("DifferentPaymentMethod").equals("true")) {
        errorMessage.put("severity", "warning");
        errorMessage.put("text", OBMessageUtils.messageBD("APRM_Different_PaymentMethod_Selected"));
      }
      jsonRequest.put("message", errorMessage);

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(e.getMessage(), e);

      try {
        jsonRequest = new JSONObject();

        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD(e.getMessage()));

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

  private HashMap<String, String> createPaymentProposalDetails(JSONObject jsonRequest,
      FIN_PaymentMethod paymentMethod, List<String> idList) throws JSONException, OBException {

    HashMap<String, String> map = new HashMap<String, String>();
    map.put("DifferentPaymentMethod", "false");
    map.put("Count", "0");
    JSONObject grid = jsonRequest.getJSONObject("_params").getJSONObject("grid");
    JSONArray selectedLines = grid.getJSONArray("_selection");
    final String strPaymentProposalId = jsonRequest.getString("Fin_Payment_Proposal_ID");
    FIN_PaymentProposal paymentProposal = OBDal.getInstance()
        .get(FIN_PaymentProposal.class, strPaymentProposalId);
    // if no lines selected don't do anything.
    if (selectedLines.length() == 0) {
      removeNonSelectedLines(idList, paymentProposal);
      return map;
    }
    BigDecimal totalAmount = BigDecimal.ZERO, totalWriteOff = BigDecimal.ZERO;
    int cont = 0;
    String differentPaymentMethod = "false";
    for (int i = 0; i < selectedLines.length(); i++) {
      JSONObject selectedLine = selectedLines.getJSONObject(i);
      log.debug(selectedLine);
      BigDecimal paidAmount = new BigDecimal(selectedLine.getString("payment"));

      if (paidAmount.compareTo(BigDecimal.ZERO) != 0) {
        FIN_PaymentMethod linePaymentMethod = OBDal.getInstance()
            .get(FIN_PaymentMethod.class, selectedLine.getString("paymentMethod"));
        if (!paymentMethod.equals(linePaymentMethod)) {
          differentPaymentMethod = "true";
        }

        FIN_PaymentPropDetail newPPD = null;
        String strPpdId = selectedLine.getString("id");
        boolean notExistsPayPropLine = idList.contains(strPpdId);
        if (notExistsPayPropLine) {
          newPPD = OBDal.getInstance().get(FIN_PaymentPropDetail.class, strPpdId);
          idList.remove(strPpdId);
        } else {
          newPPD = OBProvider.getInstance().get(FIN_PaymentPropDetail.class);
        }

        newPPD.setOrganization(paymentProposal.getOrganization());
        newPPD.setClient(paymentProposal.getClient());
        newPPD.setCreatedBy(paymentProposal.getCreatedBy());
        newPPD.setUpdatedBy(paymentProposal.getUpdatedBy());
        newPPD.setFinPaymentProposal(paymentProposal);
        newPPD.setFINPaymentScheduledetail(OBDal.getInstance()
            .get(FIN_PaymentScheduleDetail.class, selectedLine.getString("paymentScheduleDetail")));
        BigDecimal difference = new BigDecimal(selectedLine.getString("difference"));
        boolean writeOff = selectedLine.getString("writeoff").equals("true");
        newPPD.setAmount(paidAmount);
        totalAmount = totalAmount.add(paidAmount);
        if (difference.compareTo(BigDecimal.ZERO) != 0 && writeOff) {
          newPPD.setWriteoffAmount(difference);
          totalWriteOff = totalWriteOff.add(difference);
        }

        OBDal.getInstance().save(newPPD);
        OBDal.getInstance().save(paymentProposal);
        OBDal.getInstance().flush();
        cont++;
      }
    }

    removeNonSelectedLines(idList, paymentProposal);

    paymentProposal.setAmount(totalAmount);
    paymentProposal.setWriteoffAmount(totalWriteOff);
    OBDal.getInstance().save(paymentProposal);
    map.put("DifferentPaymentMethod", differentPaymentMethod);
    map.put("Count", Integer.toString(cont));
    return map;
  }

  private void removeNonSelectedLines(List<String> idList, FIN_PaymentProposal paymentProposal) {
    if (idList.size() > 0) {
      for (String id : idList) {
        FIN_PaymentPropDetail ppd = OBDal.getInstance().get(FIN_PaymentPropDetail.class, id);
        paymentProposal.getFINPaymentPropDetailList().remove(ppd);
        OBDal.getInstance().remove(ppd);
      }
      OBDal.getInstance().save(paymentProposal);
      OBDal.getInstance().flush();
    }
  }
}
