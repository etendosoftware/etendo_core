/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2010-2011 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.advpaymentmngt.process;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.payment.DoubtfulDebt;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.scheduling.ProcessBundle;

public class FIN_DoubtfulDebtProcess implements org.openbravo.scheduling.Process {
  private static AdvPaymentMngtDao dao;
  private static final Logger log4j = LogManager.getLogger();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    dao = new AdvPaymentMngtDao();
    OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(
        Utility.messageBD(bundle.getConnection(), "Success", bundle.getContext().getLanguage()));

    OBContext.setAdminMode(false);
    try {
      // retrieve custom params
      final String strAction = (String) bundle.getParams().get("action");

      // retrieve standard params
      String recordID = (String) bundle.getParams().get("FIN_Doubtful_Debt_ID");

      final DoubtfulDebt doubtfulDebt = dao.getObject(DoubtfulDebt.class, recordID);
      final VariablesSecureApp vars = bundle.getContext().toVars();
      final ConnectionProvider conProvider = bundle.getConnection();
      final String language = bundle.getContext().getLanguage();

      // ***********************
      // Process Doubtful Debt
      // ***********************
      if (strAction.equals("P")) {
        // Check payment exists
        if (doubtfulDebt.getFINPaymentSchedule()
            .getOutstandingAmount()
            .compareTo(doubtfulDebt.getAmount()) < 0) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(conProvider, "Error", language));
          Map<String, String> parameters = new HashMap<String, String>();
          parameters.put("documentNo", doubtfulDebt.getDocumentNo());
          msg.setMessage(Utility.parseTranslation(conProvider, vars, parameters, language,
              OBMessageUtils.messageBD("APRM_DoubtfulDebtPaymentExists")));
          bundle.setResult(msg);
          return;
        }
        updateDoubtfulDebtScheduleDetails(doubtfulDebt.getFINPaymentSchedule(),
            doubtfulDebt.getAmount());
        doubtfulDebt.setProcessed(true);
        doubtfulDebt.setAPRMProcess("R");
        OBDal.getInstance().save(doubtfulDebt);
        OBDal.getInstance().flush();

        // ***********************
        // Reactivate Doubtful Debt
        // ***********************
      } else if (strAction.equals("R")) {
        // Already Posted Document
        if ("Y".equals(doubtfulDebt.getPosted())) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(conProvider, "Error", language));
          msg.setMessage(Utility.parseTranslation(conProvider, vars, language, "@PostedDocument@"));
          bundle.setResult(msg);
          return;
        }
        if (doubtfulDebt.getFINPaymentSchedule()
            .getOutstandingAmount()
            .compareTo(doubtfulDebt.getAmount()) < 0) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(conProvider, "Error", language));
          Map<String, String> parameters = new HashMap<String, String>();
          parameters.put("documentNo", doubtfulDebt.getDocumentNo());
          msg.setMessage(Utility.parseTranslation(conProvider, vars, parameters, language,
              OBMessageUtils.messageBD("APRM_DoubtfulDebtPaymentExists")));
          bundle.setResult(msg);
          return;
        }
        updateDoubtfulDebtScheduleDetails(doubtfulDebt.getFINPaymentSchedule(),
            doubtfulDebt.getAmount().negate());
        doubtfulDebt.setProcessed(false);
        OBDal.getInstance().save(doubtfulDebt);
        OBDal.getInstance().flush();
        doubtfulDebt.setAPRMProcess("P");
        OBDal.getInstance().save(doubtfulDebt);
        OBDal.getInstance().flush();
      }
      bundle.setResult(msg);
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log4j.error("FIN_DoubtfulDebtProcess error: " + e.getMessage(), e);
      msg.setType("Error");
      msg.setTitle(
          Utility.messageBD(bundle.getConnection(), "Error", bundle.getContext().getLanguage()));
      msg.setMessage(FIN_Utility.getExceptionMessage(e));
      bundle.setResult(msg);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void updateDoubtfulDebtScheduleDetails(FIN_PaymentSchedule salesInvoicePaymentSchedule,
      BigDecimal debtAmount) {
    BigDecimal pendingDebtAmount = debtAmount;
    for (FIN_PaymentScheduleDetail psd : salesInvoicePaymentSchedule
        .getFINPaymentScheduleDetailInvoicePaymentScheduleList()) {
      if (psd.getPaymentDetails() == null) {
        // Pending amount should be greater or equals than the doubtful debt amount
        if (psd.getAmount().compareTo(pendingDebtAmount) >= 0) {
          psd.setDoubtfulDebtAmount(psd.getDoubtfulDebtAmount().add(pendingDebtAmount));
          OBDal.getInstance().save(psd);
        } else {
          psd.setDoubtfulDebtAmount(psd.getDoubtfulDebtAmount().add(psd.getAmount()));
          pendingDebtAmount = getDifferenceOfAmountsOrZero(pendingDebtAmount, psd);
          OBDal.getInstance().save(psd);
        }
      }
    }
  }

  private BigDecimal getDifferenceOfAmountsOrZero(BigDecimal debtAmountAux,
      FIN_PaymentScheduleDetail psd) {
    return debtAmountAux.subtract(psd.getAmount()).compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO
        : debtAmountAux.subtract(psd.getAmount());
  }
}
