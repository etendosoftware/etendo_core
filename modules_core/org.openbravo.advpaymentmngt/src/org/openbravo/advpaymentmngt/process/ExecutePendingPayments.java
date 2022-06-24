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
 * All portions are Copyright (C) 2010-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.advpaymentmngt.process;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.advpaymentmngt.APRMPendingPaymentFromInvoice;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.PaymentExecutionProcess;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.quartz.JobExecutionException;

public class ExecutePendingPayments extends DalBaseProcess {

  private ProcessLogger logger;
  private AdvPaymentMngtDao dao;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    dao = new AdvPaymentMngtDao();
    PaymentExecutionProcess executionProcess = null;
    Organization organization = null;
    VariablesSecureApp vars = bundle.getContext().toVars();
    final String language = bundle.getContext().getLanguage();

    OBContext.setAdminMode();
    try {
      List<APRMPendingPaymentFromInvoice> pendingPayments = dao.getPendingPayments();
      List<FIN_Payment> payments = new ArrayList<FIN_Payment>();
      // If there are no pending payments to process, return and skip this process.
      if (pendingPayments.isEmpty()) {
        return;
      }
      // FIXME: this code is not properly written and it's impossible to understand.
      // The comparations of objects are wrong (!=), probably they are always false in runtime.
      // The first time we iterate the for, the executionProcess is null, so it won't enter the if.
      // Is it right?
      try {
        for (APRMPendingPaymentFromInvoice pendingPayment : pendingPayments) {
          if (executionProcess != null && organization != null
              && (executionProcess != pendingPayment.getPaymentExecutionProcess()
                  || organization != pendingPayment.getOrganization())) {
            logger.logln(executionProcess.getIdentifier());
            if (dao.isAutomaticExecutionProcess(executionProcess)) {
              FIN_ExecutePayment executePayment = new FIN_ExecutePayment();
              executePayment.init("OTHER", executionProcess, payments, null,
                  pendingPayment.getOrganization());
              OBError result = executePayment.execute();
              logger.logln(Utility.parseTranslation(bundle.getConnection(), vars, language,
                  result.getMessage()));
            }
            payments.clear();
          }
          executionProcess = pendingPayment.getPaymentExecutionProcess();
          organization = pendingPayment.getOrganization();
          FIN_Payment payment = pendingPayment.getPayment();
          if (payment.getStatus().equals("RPAE")) {
            payments.add(pendingPayment.getPayment());
          } else {
            OBDal.getInstance().remove(pendingPayment);
          }
        }
        logger.logln(executionProcess.getIdentifier());
        if (dao.isAutomaticExecutionProcess(executionProcess)) {
          FIN_ExecutePayment executePayment = new FIN_ExecutePayment();
          executePayment.init("APP", executionProcess, payments, null, organization);
          OBError result = executePayment.execute();
          logger.logln(Utility.parseTranslation(bundle.getConnection(), vars, language,
              result.getMessage()));
        }

      } catch (Exception e) {
        throw new JobExecutionException(e.getMessage(), e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
