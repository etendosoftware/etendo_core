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
 * All portions are Copyright (C) 2011-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.advpaymentmngt.executionprocess;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.utility.FIN_PaymentExecutionProcess;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.PaymentRun;
import org.openbravo.model.financialmgmt.payment.PaymentRunPayment;

public class LeaveAsCredit implements FIN_PaymentExecutionProcess {

  @Override
  public OBError execute(PaymentRun paymentRun) throws ServletException {
    AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
    paymentRun.setStatus("PE");
    OBDal.getInstance().save(paymentRun);
    for (PaymentRunPayment paymentRunPayment : paymentRun.getFinancialMgmtPaymentRunPaymentList()) {
      FIN_Payment payment = paymentRunPayment.getPayment();
      if (payment.getAmount().compareTo(BigDecimal.ZERO) < 0) {
        payment.setGeneratedCredit(payment.getAmount().negate());
        payment.setAmount(BigDecimal.ZERO);
        // Set payment as processed = No to add payment detail for credit amount
        if (payment.isProcessed()) {
          payment.setProcessed(false);
          OBDal.getInstance().save(payment);
          OBDal.getInstance().flush();
        }
        // Add schedule detail for credit amount
        FIN_PaymentScheduleDetail creditScheduleDetail = dao
            .getNewPaymentScheduleDetail(payment.getOrganization(), payment.getGeneratedCredit());
        // Add payment detail for credit amount schedule detail
        dao.getNewPaymentDetail(payment, creditScheduleDetail, payment.getGeneratedCredit(),
            BigDecimal.ZERO, false, null);
        // Reset payment as processed = Yes
        payment.setProcessed(true);
      }
      payment.setStatus(payment.isReceipt() ? "RPR" : "PPM");
      paymentRunPayment.setResult("S");
      OBDal.getInstance().save(payment);
      OBDal.getInstance().save(paymentRunPayment);
    }
    paymentRun.setStatus("E");
    OBDal.getInstance().save(paymentRun);
    OBDal.getInstance().flush();
    OBError result = new OBError();
    result.setType("Success");
    result.setMessage("@Success@");
    return result;
  }
}
