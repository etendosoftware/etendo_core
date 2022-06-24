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
 * All portions are Copyright (C) 2012-2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.gl.GLJournal;
import org.openbravo.model.financialmgmt.gl.GLJournalLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;

public class FIN_AddPaymentFromJournal extends DalBaseProcess {

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    int cont = 0;

    // Recover context and variables
    ConnectionProvider conn = bundle.getConnection();
    VariablesSecureApp varsAux = bundle.getContext().toVars();
    HttpServletRequest request = RequestContext.get().getRequest();

    OBContext.setOBContext(varsAux.getUser(), varsAux.getRole(), varsAux.getClient(),
        varsAux.getOrg());
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {

      // retrieve the parameters from the bundle
      final String journalId = (String) bundle.getParams().get("GL_Journal_ID");
      String docAction = vars.getStringParameter("inpdocaction");
      if ("".equals(docAction)) {
        docAction = "CO";
      }

      // Set the docAction of the Journal (Complete, Reactivate, Close...)
      GLJournal journal = OBDal.getInstance().get(GLJournal.class, journalId);
      journal.setDocumentAction(docAction);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(journal);

      // Check if the Lines of the Journal have related Payments. In that case
      // the Payments must be deleted before Closing or Reactivating the line.
      String relatedPayments = "";
      if (!"CO".equals(docAction)) {
        for (GLJournalLine journalLine : journal.getFinancialMgmtGLJournalLineList()) {
          if (journalLine.getRelatedPayment() != null) {
            relatedPayments = relatedPayments + journalLine.getLineNo() + ", ";
          }
        }
      }
      try {
        // Call GL_Journal_Post method from the database.
        final List<Object> parameters = new ArrayList<Object>();
        parameters.add(null);
        parameters.add(journalId);
        final String procedureName = "gl_journal_post";
        CallStoredProcedure mm = CallStoredProcedure.getInstance();
        mm.call(procedureName, parameters, null, false, false);
      } catch (Exception e) {
        OBDal.getInstance().rollbackAndClose();
        OBError error = OBMessageUtils.translateError(conn, vars, vars.getLanguage(),
            e.getCause().getMessage());
        throw new OBException(error.getMessage());
      }

      OBDal.getInstance().refresh(journal);

      // Complete the Journal
      if ("CO".equals(docAction)) {
        for (GLJournalLine journalLine : journal.getFinancialMgmtGLJournalLineList()) {
          // Recover again the object to avoid problems with Dal
          journalLine = OBDal.getInstance().get(GLJournalLine.class, journalLine.getId());
          if (journalLine.isOpenItems() && journalLine.getRelatedPayment() == null) {
            // Create bundle
            vars = new VariablesSecureApp(varsAux.getUser(), varsAux.getClient(), varsAux.getOrg(),
                varsAux.getRole(), varsAux.getLanguage());
            ProcessBundle pb = new ProcessBundle("DE1B382FDD2540199D223586F6E216D0", vars)
                .init(conn);
            HashMap<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("GL_JournalLine_ID", journalLine.getId());
            pb.setParams(parameters);
            OBError myMessage = null;
            // Create a Payment for the Journal line
            FIN_AddPaymentFromJournalLine myProcess = new FIN_AddPaymentFromJournalLine();
            myProcess.setDoCommit(false);
            myProcess.execute(pb);
            myMessage = (OBError) pb.getResult();

            if (myMessage.getType().equals("Error")) {
              throw new OBException("@FIN_PaymentFromJournalError@ " + journalLine.getLineNo()
                  + " - " + myMessage.getMessage());
            }
            cont++;
          }
        }
      }

      // OBError is also used for successful results
      final OBError msg = new OBError();
      msg.setType("Success");
      msg.setTitle("@Success@");
      if (cont > 0) {
        msg.setMessage(" @FIN_NumberOfPayments@: " + cont);
      }
      if (!"".equals(relatedPayments) && "RE".equals(docAction)) {
        relatedPayments = relatedPayments.substring(0, relatedPayments.length() - 2);
        msg.setType("Warning");
        msg.setTitle("@Success@");
        msg.setMessage("@Warning@: @FIN_JournalLineRelatedPayments@: " + relatedPayments
            + ". @ModifyGLJournalLine@");
      }
      bundle.setResult(msg);
      OBDal.getInstance().commitAndClose();
    } catch (final OBException e) {
      final OBError msg = new OBError();
      msg.setType("Error");
      msg.setMessage(e.getMessage());
      msg.setTitle("@Error@");
      OBDal.getInstance().rollbackAndClose();
      bundle.setResult(msg);
    }

  }
}
