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
 * All portions are Copyright (C) 2013-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.process;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.gl.GLJournalLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;

public class FIN_AddPaymentFromJournalLine extends DalBaseProcess {
  private static AdvPaymentMngtDao dao;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    dao = new AdvPaymentMngtDao();
    OBError message = null;

    String strMessageType = "";
    StringBuilder strMessageResult = new StringBuilder();
    String strTitle = "";
    OBContext.setAdminMode(true);
    try {

      // retrieve the parameters from the bundle
      final String journalLineId = (String) bundle.getParams().get("GL_JournalLine_ID");

      // Initialize objects
      GLJournalLine journalLine = OBDal.getInstance().get(GLJournalLine.class, journalLineId);
      FIN_FinancialAccount financialAccount = journalLine.getFinancialAccount();
      FIN_PaymentMethod paymentMethod = journalLine.getPaymentMethod();
      BusinessPartner bPartner = journalLine.getBusinessPartner();
      GLItem glItem = journalLine.getGLItem();
      Date date = journalLine.getPaymentDate();
      boolean isReceipt = journalLine.getDebit()
          .subtract(journalLine.getCredit())
          .compareTo(BigDecimal.ZERO) > 0;

      // Check restrictions
      OBContext.setAdminMode(false);
      try {
        if (!journalLine.getJournalEntry().isMultigeneralLedger()) {
          //@formatter:off
          final String hsqlScript = "select distinct(o.generalLedger) "
              + "from Organization o "
              + "where ad_isorgincluded(:orgId, o.id, o.client) <> -1 "
              + "and o.generalLedger is not null ";
          //@formatter:on
          final Session session = OBDal.getInstance().getSession();
          final Query<AcctSchema> query = session.createQuery(hsqlScript, AcctSchema.class);
          query.setParameter("orgId", journalLine.getOrganization().getId());
          if (query.list().size() != 1) {
            throw new OBException("@FIN_NoMultiAccountingAllowed@");
          }
        }
      } finally {
        OBContext.restorePreviousMode();
      }

      if (!journalLine.getCurrency().equals(financialAccount.getCurrency())) {
        throw new OBException("@FIN_NoMultiCurrencyAllowed@");
      }
      if (journalLine.getDebit().subtract(journalLine.getCredit()).compareTo(BigDecimal.ZERO) > 0
          && bPartner != null && !bPartner.isCustomer()) {
        throw new OBException("@FIN_NoCustomer@");
      }
      if (journalLine.getDebit().subtract(journalLine.getCredit()).compareTo(BigDecimal.ZERO) < 0
          && bPartner != null && !bPartner.isVendor()) {
        throw new OBException("@FIN_NoVendor@");
      }

      // Retrieve additional variables
      final List<Object> parameters = new ArrayList<Object>();
      parameters.add(journalLine.getClient().getId());
      parameters.add(journalLine.getOrganization().getId());
      parameters.add(isReceipt ? "ARR" : "APP");
      String strDocTypeId = (String) CallStoredProcedure.getInstance()
          .call("AD_GET_DOCTYPE", parameters, null);
      String strPaymentDocumentNo = FIN_Utility.getDocumentNo(journalLine.getOrganization(),
          (isReceipt) ? "ARR" : "APP", (isReceipt) ? "AR Receipt" : "AP Payment");

      // Generate Payment
      FIN_Payment payment = dao.getNewPayment(isReceipt, journalLine.getOrganization(),
          dao.getObject(DocumentType.class, strDocTypeId), strPaymentDocumentNo, bPartner,
          paymentMethod, financialAccount,
          journalLine.getForeignCurrencyDebit()
              .subtract(journalLine.getForeignCurrencyCredit())
              .abs()
              .toString(),
          date, null, journalLine.getCurrency(), null, null);

      // Add Payment Details
      FIN_AddPayment.saveGLItem(payment,
          journalLine.getForeignCurrencyDebit()
              .subtract(journalLine.getForeignCurrencyCredit())
              .abs(),
          glItem, bPartner, journalLine.getProduct(), journalLine.getProject(),
          journalLine.getSalesCampaign(), journalLine.getActivity(), journalLine.getSalesRegion(),
          journalLine.getCostCenter(), journalLine.getStDimension(), journalLine.getNdDimension());

      OBDal.getInstance().flush();

      // process payment
      message = FIN_AddPayment.processPayment(bundle.getContext().toVars(), bundle.getConnection(),
          "P", payment);

      // Print result
      if (message.getType().equals("Error")) {
        String exceptionMessage = payment.getBusinessPartner() != null
            ? payment.getBusinessPartner().getName()
            : "";
        exceptionMessage += ": " + message.getMessage();
        throw new OBException(exceptionMessage);
      } else if (message.getType().equals("Warning")) {
        strTitle = "@Warning@";
        strMessageType = "Warning";
      } else {
        strTitle = "@Success@";
        strMessageType = "Success";
      }
      strMessageResult.append("@Payment@ ").append(payment.getDocumentNo());
      strMessageResult.append(" (")
          .append(
              payment.getBusinessPartner() != null ? payment.getBusinessPartner().getName() : "")
          .append(")");
      if (!"".equals(message.getMessage())) {
        strMessageResult.append(": ").append(message.getMessage());
      }
      strMessageResult.append("<br>");

      // OBError is also used for successful results
      final OBError msg = new OBError();
      msg.setType(strMessageType);
      msg.setTitle(strTitle);
      msg.setMessage(strMessageResult.toString());

      journalLine.setRelatedPayment(payment);
      OBDal.getInstance().flush();

      bundle.setResult(msg);

    } catch (final OBException e) {
      final OBError msg = new OBError();
      msg.setType("Error");
      msg.setMessage(e.getMessage());
      msg.setTitle("@Error@");
      bundle.setResult(msg);
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}
