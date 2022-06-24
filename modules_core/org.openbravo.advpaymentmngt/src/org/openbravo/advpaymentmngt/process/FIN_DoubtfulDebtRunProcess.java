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

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.payment.DoubtfulDebt;
import org.openbravo.model.financialmgmt.payment.DoubtfulDebtRun;
import org.openbravo.scheduling.ProcessBundle;

public class FIN_DoubtfulDebtRunProcess implements org.openbravo.scheduling.Process {
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
      String recordID = (String) bundle.getParams().get("FIN_Doubtful_Debt_Run_ID");

      final DoubtfulDebtRun doubtfulDebtRun = dao.getObject(DoubtfulDebtRun.class, recordID);
      final VariablesSecureApp vars = bundle.getContext().toVars();
      final ConnectionProvider conProvider = bundle.getConnection();
      final String language = bundle.getContext().getLanguage();

      // *************************
      // Process Doubtful Debt Run
      // *************************
      if (strAction.equals("P")) {
        // Check lines exists
        if (doubtfulDebtRun.getFINDoubtfulDebtList().size() == 0) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(conProvider, "Error", language));
          msg.setMessage(Utility.parseTranslation(conProvider, vars, language, "@APRM_NoLines@"));
          bundle.setResult(msg);
          return;
        }
        for (DoubtfulDebt ddb : doubtfulDebtRun.getFINDoubtfulDebtList()) {
          if (!ddb.isProcessed()) {
            OBError result = processDoubtfulDebt(vars, conProvider, ddb, strAction);
            if ("Error".equals(result.getType())) {
              bundle.setResult(result);
              return;
            }
          }
        }
        doubtfulDebtRun.setAPRMProcess("R");
        doubtfulDebtRun.setProcessed(true);
        OBDal.getInstance().save(doubtfulDebtRun);
        // ****************************
        // Reactivate Doubtful Debt Run
        // ****************************
      } else if (strAction.equals("R")) {
        // Already Posted Document
        for (DoubtfulDebt ddb : doubtfulDebtRun.getFINDoubtfulDebtList()) {
          if (ddb.isProcessed()) {
            OBError result = processDoubtfulDebt(vars, conProvider, ddb, strAction);
            if ("error".equals(result.getType())) {
              bundle.setResult(result);
              return;
            }
          }
        }
        doubtfulDebtRun.setAPRMProcess("P");
        doubtfulDebtRun.setProcessed(false);
        OBDal.getInstance().save(doubtfulDebtRun);
      }
      bundle.setResult(msg);
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log4j.error("FIN_DoubtfulDebtRunProcess error: " + e.getMessage(), e);
      msg.setType("Error");
      msg.setTitle(
          Utility.messageBD(bundle.getConnection(), "Error", bundle.getContext().getLanguage()));
      msg.setMessage(FIN_Utility.getExceptionMessage(e));
      bundle.setResult(msg);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private OBError processDoubtfulDebt(VariablesSecureApp vars, ConnectionProvider conn,
      DoubtfulDebt ddb, String strAction) throws Exception {
    ProcessBundle pb = new ProcessBundle("017312F51139438A9665775E3B5392A1", vars).init(conn);
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("action", strAction);
    parameters.put("FIN_Doubtful_Debt_ID", ddb.getId());
    pb.setParams(parameters);
    OBError myMessage = null;
    new FIN_DoubtfulDebtProcess().execute(pb);
    myMessage = (OBError) pb.getResult();
    return myMessage;
  }
}
