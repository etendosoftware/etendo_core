/*
 ******************************************************************************
 * The contents of this file are subject to the   Compiere License  Version 1.1
 * ("License"); You may not use this file except in compliance with the License
 * You may obtain a copy of the License at http://www.compiere.org/license.html
 * Software distributed under the License is distributed on an  "AS IS"  basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * The Original Code is                  Compiere  ERP & CRM  Business Solution
 * The Initial Developer of the Original Code is Jorg Janke  and ComPiere, Inc.
 * Portions created by Jorg Janke are Copyright (C) 1999-2001 Jorg Janke, parts
 * created by ComPiere are Copyright (C) ComPiere, Inc.;   All Rights Reserved.
 * Contributor(s): Openbravo SLU
 * Contributions are Copyright (C) 2001-2018 Openbravo S.L.U.
 ******************************************************************************
 */
package org.openbravo.erpCommon.ad_forms;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.financialmgmt.gl.GLJournal;

public class DocGLJournal extends AcctServer {
  private static final long serialVersionUID = 1L;
  private static final Logger log4jDocGLJournal = LogManager.getLogger();

  private String SeqNo = "0";
  private String C_AcctSchema_ID;

  /**
   * Constructor
   * 
   * @param AD_Client_ID
   *          client
   */
  public DocGLJournal(String AD_Client_ID, String AD_Org_ID,
      ConnectionProvider connectionProvider) {
    super(AD_Client_ID, AD_Org_ID, connectionProvider);
    if (log4jDocGLJournal.isDebugEnabled()) {
      log4jDocGLJournal.debug("- DocGLJournal - OBJECT CREATED.");
    }
  }

  public String m_PostingType = Fact.POST_Actual;

  @Override
  public void loadObjectFieldProvider(ConnectionProvider conn, String aD_Client_ID, String Id)
      throws ServletException {
    AcctSchema docAcctSchema = new AcctSchema(conn,
        DocGLJournalData.selectAcctSchema(conn, aD_Client_ID, Id));
    ArrayList<Object> list = new ArrayList<Object>();
    list.add(docAcctSchema);
    AcctSchema[] retValue = new AcctSchema[list.size()];
    list.toArray(retValue);
    this.m_as = retValue;
    setObjectFieldProvider(DocGLJournalData.select(conn, aD_Client_ID, Id));
  }

  /**
   * Load Specific Document Details
   * 
   * @return true if loadDocumentType was set
   */
  @Override
  public boolean loadDocumentDetails(FieldProvider[] data, ConnectionProvider conn) {
    loadDocumentType(); // lines require doc type
    m_PostingType = data[0].getField("PostingType");
    m_IsOpening = data[0].getField("isopening");
    C_Period_ID = isperiodOpen(conn, data[0].getField("period"));
    C_Currency_ID = data[0].getField("c_currency_id");
    try {
      C_AcctSchema_ID = DocGLJournalData.selectAcctSchema(conn, AD_Client_ID, Record_ID);
    } catch (ServletException e) {
      log4j.error("Error retrieving the accounting schema (general ledger)", e);
    }
    // Contained Objects
    p_lines = loadLines(conn);
    log4jDocGLJournal.debug("Lines=" + p_lines.length);
    return true;
  } // loadDocumentDetails

  /**
   * Load Invoice Line
   * 
   * @return DocLine Array
   */
  private DocLine[] loadLines(ConnectionProvider conn) {
    ArrayList<Object> list = new ArrayList<Object>();
    DocLineGLJournalData[] data = null;
    try {
      data = DocLineGLJournalData.select(conn, Record_ID);
      for (int i = 0; i < data.length; i++) {
        String Line_ID = data[i].glJournallineId;
        DocLine_GLJournal docLine = new DocLine_GLJournal(DocumentType, Record_ID, Line_ID);
        docLine.loadAttributes(data[i], this);
        docLine.m_Record_Id2 = data[i].cDebtPaymentId;
        // -- Source Amounts
        docLine.setAmount(data[i].amtsourcedr, data[i].amtsourcecr);
        // -- Converted Amounts (only in non multi-general ledger)
        if (!"".equals(C_AcctSchema_ID)) {
          docLine.setConvertedAmt(null, data[i].amtacctdr, data[i].amtacctcr);
        }
        docLine.m_DateAcct = this.DateAcct;
        // -- Account
        String C_ValidCombination_ID = data[i].cValidcombinationId;
        Account acct = null;
        try {
          acct = new Account(conn, C_ValidCombination_ID);
        } catch (ServletException e) {
          log4jDocGLJournal.warn(e);
        }
        docLine.setAccount(acct);
        // -- Set Org from account (x-org)
        docLine.setAD_Org_ID(data[i].adOrgId);
        list.add(docLine);
      }
    } catch (ServletException e) {
      log4jDocGLJournal.warn(e);
    }
    // Return Array
    DocLine[] dl = new DocLine[list.size()];
    list.toArray(dl);
    return dl;
  } // loadLines

  /**
   * Get Source Currency Balance - subtracts line and tax amounts from total - no rounding
   * 
   * @return positive amount, if total invoice is bigger than lines
   */
  @Override
  public BigDecimal getBalance() {
    BigDecimal retValue = ZERO;
    StringBuffer sb = new StringBuffer(" [");
    // Lines
    for (int i = 0; i < p_lines.length; i++) {
      retValue = retValue.add(new BigDecimal(p_lines[i].getAmount()));
      sb.append("+").append(p_lines[i].getAmount());
    }
    sb.append("]");
    log4jDocGLJournal.debug(toString() + " Balance=" + retValue + sb.toString());
    return retValue;
  } // getBalance

  /**
   * Create Facts (the accounting logic) for GLJ. (only for the accounting scheme, it was created)
   * 
   * <pre>
   *      account     DR          CR
   * </pre>
   * 
   * @param as
   *          acct schema
   * @return Fact
   */
  @Override
  public Fact createFact(AcctSchema as, ConnectionProvider conn, Connection con,
      VariablesSecureApp vars) throws ServletException {
    // Select specific definition
    String strClassname = AcctServerData.selectTemplateDoc(conn, as.m_C_AcctSchema_ID,
        DocumentType);
    if (strClassname.equals("")) {
      strClassname = AcctServerData.selectTemplate(conn, as.m_C_AcctSchema_ID, AD_Table_ID);
    }
    if (!strClassname.equals("")) {
      try {
        DocGLJournalTemplate newTemplate = (DocGLJournalTemplate) Class.forName(strClassname)
            .getDeclaredConstructor()
            .newInstance();
        return newTemplate.createFact(this, as, conn, con, vars);
      } catch (Exception e) {
        log4j.error("Error while creating new instance for DocGLJournalTemplate - " + e);
      }
    }
    // create Fact Header
    Fact fact = new Fact(this, as, m_PostingType);
    String Fact_Acct_Group_ID = SequenceIdData.getUUID();
    // GLJ
    if (DocumentType.equals(AcctServer.DOCTYPE_GLJournal)) {
      // account DR CR
      OBContext.setAdminMode(true);
      try {
        Account account;
        for (int i = 0; i < p_lines.length; i++) {
          if (Float.parseFloat(p_lines[i].getAmtSourceDr()) > Float
              .parseFloat(p_lines[i].getAmtSourceCr())) {
            account = ((DocLine_GLJournal) p_lines[i]).getAccount("1", as, conn);
          } else {
            account = ((DocLine_GLJournal) p_lines[i]).getAccount("2", as, conn);
          }
          fact.createLine(p_lines[i], account, C_Currency_ID, p_lines[i].getAmtSourceDr(),
              p_lines[i].getAmtSourceCr(), Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType,
              conn);
        }
      } finally {
        OBContext.restorePreviousMode();
      }
    } else {
      log4jDocGLJournal.warn("createFact - " + "DocumentType unknown: " + DocumentType);
      fact = null;
    }
    SeqNo = "0";
    return fact;
  } // createFact

  /**
   * @return the seqNo
   */
  public String getSeqNo() {
    return SeqNo;
  }

  /**
   * @param seqNo
   *          the seqNo to set
   */
  public void setSeqNo(String seqNo) {
    SeqNo = seqNo;
  }

  /**
   * @return the m_PostingType
   */
  public String getM_PostingType() {
    return m_PostingType;
  }

  /**
   * @param postingType
   *          the m_PostingType to set
   */
  public void setM_PostingType(String postingType) {
    m_PostingType = postingType;
  }

  /**
   * @return the serialVersionUID
   */
  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public String nextSeqNo(String oldSeqNo) {
    log4jDocGLJournal.debug("DocGLJournal - oldSeqNo = " + oldSeqNo);
    BigDecimal seqNo = new BigDecimal(oldSeqNo);
    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
    log4jDocGLJournal.debug("DocGLJournal - nextSeqNo = " + SeqNo);
    return SeqNo;
  }

  /**
   * Get Document Confirmation
   * 
   * not used
   */
  @Override
  public boolean getDocumentConfirmation(ConnectionProvider conn, String strRecordId) {
    final String STATUS_VOIDED = "VO";
    if (STATUS_VOIDED
        .equals(OBDal.getInstance().get(GLJournal.class, strRecordId).getDocumentStatus())) {
      setStatus(STATUS_DocumentDisabled);
      return false;
    }
    DocGLJournalData[] data = null;
    try {
      data = DocGLJournalData.select(conn, AD_Client_ID, strRecordId);
      AcctSchema[] m_acctSchemas = reloadLocalAcctSchemaArray(data[0].adOrgId);

      AcctSchema acct = null;

      for (int i = 0; i < m_acctSchemas.length; i++) {
        acct = m_acctSchemas[i];
        data = DocGLJournalData.selectFinInvCount(conn, strRecordId, acct.m_C_AcctSchema_ID);
        int countFinInv = Integer.parseInt(data[0].fininvcount);
        int countGLItemAcct = Integer.parseInt(data[0].finacctcount);
        // For any GL Item used in financial invoice lines debit/credit accounts must be defined
        if (countFinInv != 0 && (countFinInv != countGLItemAcct)) {
          log4jDocGLJournal
              .debug("DocGLJournal - getDocumentConfirmation - GL Item used in financial "
                  + "GLJournal lines debit/credit accounts must be defined.");
          setStatus(STATUS_InvalidAccount);
          return false;
        }
      }

    } catch (Exception e) {
      log4jDocGLJournal.error("Exception in getDocumentConfirmation method.", e);
    }
    return true;
  }

  private String isperiodOpen(ConnectionProvider conn, String periodId) {
    if (StringUtils.isEmpty(periodId)) {
      return "";
    }
    try {
      DocGLJournalData[] data = DocGLJournalData.periodOpen(conn, periodId);
      if (data != null && data.length > 0 && StringUtils.isNotEmpty(data[0].period)) {
        return periodId;
      } else {
        return "";
      }
    } catch (ServletException e) {
      e.printStackTrace();
      return "";
    }
  }

  @Override
  public String getServletInfo() {
    return "Servlet for the accounting";
  } // end of getServletInfo() method

  private AcctSchema[] reloadLocalAcctSchemaArray(String adOrgId) throws ServletException {
    AcctSchema acct = null;
    ArrayList<Object> new_as = new ArrayList<Object>();
    // We reload again all the acct schemas of the client
    AcctSchema[] m_aslocal = AcctSchema.getAcctSchemaArray(connectionProvider, AD_Client_ID,
        adOrgId);
    // Filter the right acct schemas for the organization
    for (int i = 0; i < m_aslocal.length; i++) {
      acct = m_aslocal[i];
      if (AcctSchemaData.selectAcctSchemaTable(connectionProvider, acct.m_C_AcctSchema_ID,
          AD_Table_ID)) {
        new_as.add(new AcctSchema(connectionProvider, acct.m_C_AcctSchema_ID));
      }
    }
    AcctSchema[] retValue = new AcctSchema[new_as.size()];
    new_as.toArray(retValue);
    m_aslocal = retValue;
    return m_aslocal;
  }

}
