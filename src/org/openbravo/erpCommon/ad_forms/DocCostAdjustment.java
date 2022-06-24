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
 * All portions are Copyright (C) 2014-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.erpCommon.ad_forms;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.materialmgmt.cost.CostAdjustmentLine;

public class DocCostAdjustment extends AcctServer {

  private static final long serialVersionUID = 1L;
  private static final Logger log4jDocCostAdjustment = LogManager.getLogger();

  /** AD_Table_ID */
  private String SeqNo = "0";

  public DocCostAdjustment() {
  }

  /**
   * Constructor
   * 
   * @param AD_Client_ID
   *          AD_Client_ID
   */
  public DocCostAdjustment(String AD_Client_ID, String AD_Org_ID,
      ConnectionProvider connectionProvider) {
    super(AD_Client_ID, AD_Org_ID, connectionProvider);
  }

  @Override
  public void loadObjectFieldProvider(ConnectionProvider conn,
      @SuppressWarnings("hiding") String AD_Client_ID, String Id) throws ServletException {
    setObjectFieldProvider(DocCostAdjustmentData.selectRegistro(conn, AD_Client_ID, Id));
  }

  /**
   * Load Document Details
   * 
   * @return true if loadDocumentType was set
   */
  @Override
  public boolean loadDocumentDetails(FieldProvider[] data, ConnectionProvider conn) {
    C_Currency_ID = NO_CURRENCY;

    DocumentType = AcctServer.DOCTYPE_CostAdjustment;
    log4jDocCostAdjustment.debug("loadDocumentDetails - C_Currency_ID : " + C_Currency_ID);
    DateDoc = data[0].getField("DateTrx");
    loadDocumentType(); // lines require doc type
    // Contained Objects
    p_lines = loadLines(conn);
    return true;
  } // loadDocumentDetails

  /**
   * Load Invoice Line
   * 
   * @return DocLine Array
   */
  private DocLine[] loadLines(ConnectionProvider conn) {
    ArrayList<Object> list = new ArrayList<Object>();

    DocLineCostAdjustmentData[] data = null;
    try {
      data = DocLineCostAdjustmentData.select(conn, Record_ID);
      for (int i = 0; i < data.length; i++) {
        String Line_ID = data[i].mCostadjustmentlineId;
        DocLine_CostAdjustment docLine = new DocLine_CostAdjustment(DocumentType, Record_ID,
            Line_ID);
        docLine.loadAttributes(data[i], this);
        docLine.setTrxType(data[i].mTransactionId);
        docLine.m_C_Currency_ID = data[i].cCurrencyId;
        docLine.setWarehouseId(data[i].mWarehouseId);
        docLine.m_DateAcct = data[i].dateacct;
        docLine.m_C_BPartner_ID = data[i].cBpartnerId;
        docLine.setSourceProcess(data[i].sourceProcess);
        docLine.setIsSource("Y".equals(data[i].issource));
        docLine.setIsUnitCost("Y".equals(data[i].isunitcost));
        // -- Source Amounts
        String amt = data[i].adjustmentAmount;
        docLine.setAmount(amt);
        list.add(docLine);
      }
    } catch (ServletException e) {
      log4jDocCostAdjustment.warn(e);
    }
    // Return Array
    DocLine[] dl = new DocLine[list.size()];
    list.toArray(dl);
    return dl;
  } // loadLines

  /**
   * Get Balance
   * 
   * @return Zero (always balanced)
   */
  @Override
  public BigDecimal getBalance() {
    BigDecimal retValue = ZERO;
    return retValue;
  } // getBalance

  /**
   * Create Facts (the accounting logic) for MMS, MMR.
   * 
   * <pre>
   *  Shipment
   *      CoGS            DR
   *      Inventory               CR
   *  Shipment of Project Issue
   *      CoGS            DR
   *      Project                 CR
   *  Receipt
   *      Inventory       DR
   *      NotInvoicedReceipt      CR
   * </pre>
   * 
   * @param as
   *          accounting schema
   * @return Fact
   */
  @Override
  public Fact createFact(AcctSchema as, ConnectionProvider conn, Connection con,
      VariablesSecureApp vars) throws ServletException {
    // Select specific definition
    String strClassname = AcctServerData.selectTemplateDoc(conn, as.m_C_AcctSchema_ID,
        DocumentType);
    if (StringUtils.isEmpty(strClassname)) {
      strClassname = AcctServerData.selectTemplate(conn, as.m_C_AcctSchema_ID, AD_Table_ID);
    } else {
      try {
        DocCostAdjustmentTemplate newTemplate = (DocCostAdjustmentTemplate) Class
            .forName(strClassname)
            .getDeclaredConstructor()
            .newInstance();
        return newTemplate.createFact(this, as, conn, con, vars);
      } catch (Exception e) {
        log4j.error("Error while creating new instance for DocCostAdjustmentTemplate - ", e);
      }
    }
    C_Currency_ID = as.getC_Currency_ID();
    // create Fact Header
    Fact fact = new Fact(this, as, Fact.POST_Actual);
    String Fact_Acct_Group_ID = SequenceIdData.getUUID();
    String amtDebit = "0";
    String amtCredit = "0";

    // Lines
    for (int i = 0; p_lines != null && i < p_lines.length; i++) {
      DocLine_CostAdjustment line = (DocLine_CostAdjustment) p_lines[i];
      String transactionType = line.getTransactionType();

      BigDecimal amount = new BigDecimal(line.getAmount());
      ProductInfo p = new ProductInfo(line.m_M_Product_ID, conn);

      log4jDocCostAdjustment.debug("antes del creteline, line.getAmount(): " + line.getAmount()
          + " - TransactionType: " + transactionType);
      if (transactionType.equals(DocLine_CostAdjustment.TRXTYPE_SHIPMENT)) {
        // Cogs DR
        // Inventory Asset CR
        log4jDocCostAdjustment.debug("********** DocCostAdjustment - factAcct - account - "
            + p.getAccount(ProductInfo.ACCTTYPE_P_Cogs, as, conn).C_ValidCombination_ID);

        if (line.isTransactionNegative()) {
          amtDebit = "";
          amtCredit = amount.toPlainString();
        } else {
          amtDebit = amount.toPlainString();
          amtCredit = "";
        }
        fact.createLine(line, p.getAccount(ProductInfo.ACCTTYPE_P_Cogs, as, conn),
            line.m_C_Currency_ID, amtDebit, amtCredit, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            DocumentType, line.m_DateAcct, null, conn);
        fact.createLine(line, p.getAccount(ProductInfo.ACCTTYPE_P_Asset, as, conn),
            line.m_C_Currency_ID, amtCredit, amtDebit, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            DocumentType, line.m_DateAcct, null, conn);
      } else if (transactionType.equals(DocLine_CostAdjustment.TRXTYPE_RECEIPT)) {
        Account acct = null;
        // Inventory Asset DR
        if (line.isUnitCost()) {
          // Price Diff Correction
          // Invoice Price Variance CR
          acct = p.getAccount(ProductInfo.ACCTTYPE_P_IPV, as, conn);
        } else if (line.getIsSource() && ("LC").equals(line.getSourceProcess())) {
          throw new IllegalStateException(OBMessageUtils.messageBD("LCNotAccounting"));
        } else {
          // Product Exp CR
          acct = getAccountByWarehouse(AcctServer.ACCTTYPE_InvDifferences, as,
              line.getWarehouseId(), conn);
        }
        log4jDocCostAdjustment.debug("********** DocCostAdjustment - factAcct - account - "
            + p.getAccount(ProductInfo.ACCTTYPE_P_Expense, as, conn).C_ValidCombination_ID);
        if (line.isTransactionNegative()) {
          amtDebit = amount.toPlainString();
          amtCredit = "";
        } else {
          amtDebit = "";
          amtCredit = amount.toPlainString();
        }
        fact.createLine(line, acct, line.m_C_Currency_ID, amtDebit, amtCredit, Fact_Acct_Group_ID,
            nextSeqNo(SeqNo), DocumentType, line.m_DateAcct, null, conn);
        fact.createLine(line, p.getAccount(ProductInfo.ACCTTYPE_P_Asset, as, conn),
            line.m_C_Currency_ID, amtCredit, amtDebit, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            DocumentType, line.m_DateAcct, null, conn);
      } else if (transactionType.equals(DocLine_CostAdjustment.TRXTYPE_INVENTORY)) {
        // Inventory Asset DR
        // Inventory Adjustment CR
        log4jDocCostAdjustment.debug("********** DocCostAdjustment - factAcct - account - "
            + getAccountByWarehouse(AcctServer.ACCTTYPE_InvDifferences, as, line.getWarehouseId(),
                conn).C_ValidCombination_ID);
        if (line.isTransactionNegative()) {
          amtDebit = amount.toPlainString();
          amtCredit = "";
        } else {
          amtDebit = "";
          amtCredit = amount.toPlainString();
        }
        fact.createLine(line,
            getAccountByWarehouse(AcctServer.ACCTTYPE_InvDifferences, as, line.getWarehouseId(),
                conn),
            line.m_C_Currency_ID, amtDebit, amtCredit, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            DocumentType, line.m_DateAcct, null, conn);
        fact.createLine(line, p.getAccount(ProductInfo.ACCTTYPE_P_Asset, as, conn),
            line.m_C_Currency_ID, amtCredit, amtDebit, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            DocumentType, line.m_DateAcct, null, conn);
      } else if (transactionType.equals(DocLine_CostAdjustment.TRXTYPE_INTERNALMOVEMENTFROM)) {
        // Inventory Asset DR
        // Inventory Adjustment CR
        M_Warehouse_ID = line.getWarehouseId();
        log4jDocCostAdjustment.debug("********** DocCostAdjustment - factAcct - account - "
            + getAccountByWarehouse(AcctServer.ACCTTYPE_InvDifferences, as, line.getWarehouseId(),
                conn).C_ValidCombination_ID);
        if (line.isTransactionNegative()) {
          amtDebit = amount.negate().toPlainString();
          amtCredit = "";
        } else {
          amtDebit = "";
          amtCredit = amount.negate().toPlainString();
        }
        fact.createLine(line, p.getAccount(ProductInfo.ACCTTYPE_P_Asset, as, conn),
            line.m_C_Currency_ID, amtDebit, amtCredit, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            DocumentType, line.m_DateAcct, null, conn);

        fact.createLine(line,
            getAccountByWarehouse(AcctServer.ACCTTYPE_InvDifferences, as, line.getWarehouseId(),
                conn),
            line.m_C_Currency_ID, amtCredit, amtDebit, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            DocumentType, line.m_DateAcct, null, conn);
      } else if (transactionType.equals(DocLine_CostAdjustment.TRXTYPE_INTERNALMOVEMENTTO)) {
        // Inventory Asset DR
        // Inventory Adjustment CR
        M_Warehouse_ID = line.getWarehouseId();
        log4jDocCostAdjustment.debug("********** DocCostAdjustment - factAcct - account - "
            + getAccountByWarehouse(AcctServer.ACCTTYPE_InvDifferences, as, line.getWarehouseId(),
                conn).C_ValidCombination_ID);
        if (line.isTransactionNegative()) {
          amtDebit = amount.toPlainString();
          amtCredit = "";
        } else {
          amtDebit = "";
          amtCredit = amount.toPlainString();
        }
        fact.createLine(line,
            getAccountByWarehouse(AcctServer.ACCTTYPE_InvDifferences, as, line.getWarehouseId(),
                conn),
            line.m_C_Currency_ID, amtDebit, amtCredit, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            DocumentType, line.m_DateAcct, null, conn);
        fact.createLine(line, p.getAccount(ProductInfo.ACCTTYPE_P_Asset, as, conn),
            line.m_C_Currency_ID, amtCredit, amtDebit, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            DocumentType, line.m_DateAcct, null, conn);
      } else if (transactionType.equals(DocLine_CostAdjustment.TRXTYPE_INTERNALCONSUMPTION)) {
        // Inventory Asset DR
        // Inventory Adjustment CR
        M_Warehouse_ID = line.getWarehouseId();
        log4jDocCostAdjustment.debug("********** DocCostAdjustment - factAcct - account - "
            + getAccountByWarehouse(AcctServer.ACCTTYPE_InvDifferences, as, line.getWarehouseId(),
                conn).C_ValidCombination_ID);
        if (line.isTransactionNegative()) {
          amtDebit = amount.toPlainString();
          amtCredit = "";
        } else {
          amtDebit = "";
          amtCredit = amount.toPlainString();
        }
        fact.createLine(line, p.getAccount(ProductInfo.ACCTTYPE_P_Asset, as, conn),
            line.m_C_Currency_ID, amtDebit, amtCredit, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            DocumentType, line.m_DateAcct, null, conn);

        fact.createLine(line,
            getAccountByWarehouse(AcctServer.ACCTTYPE_InvDifferences, as, line.getWarehouseId(),
                conn),
            line.m_C_Currency_ID, amtCredit, amtDebit, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            DocumentType, line.m_DateAcct, null, conn);
      } else if (transactionType.equals(DocLine_CostAdjustment.TRXTYPE_BOM)) {
        // Inventory Asset DR
        // Inventory Adjustment CR
        M_Warehouse_ID = line.getWarehouseId();
        log4jDocCostAdjustment.debug("********** DocCostAdjustment - factAcct - account - "
            + getAccountByWarehouse(AcctServer.ACCTTYPE_InvDifferences, as, line.getWarehouseId(),
                conn).C_ValidCombination_ID);
        if (line.isTransactionNegative()) {
          amtDebit = amount.toPlainString();
          amtCredit = "";
        } else {
          amtDebit = "";
          amtCredit = amount.toPlainString();
        }
        fact.createLine(line,
            getAccountByWarehouse(AcctServer.ACCTTYPE_InvDifferences, as, line.getWarehouseId(),
                conn),
            line.m_C_Currency_ID, amtDebit, amtCredit, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            DocumentType, line.m_DateAcct, null, conn);

        fact.createLine(line, p.getAccount(ProductInfo.ACCTTYPE_P_Asset, as, conn),
            line.m_C_Currency_ID, amtCredit, amtDebit, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            DocumentType, line.m_DateAcct, null, conn);
      } else if (transactionType.equals(DocLine_CostAdjustment.TRXTYPE_MANUFACTURING)) {
        // Inventory Asset DR
        // Inventory Adjustment CR
        M_Warehouse_ID = line.getWarehouseId();
        log4jDocCostAdjustment.debug("********** DocCostAdjustment - factAcct - account - "
            + getAccountByWarehouse(AcctServer.ACCTTYPE_InvDifferences, as, line.getWarehouseId(),
                conn).C_ValidCombination_ID);
        if (line.isTransactionNegative()) {
          amtDebit = amount.toPlainString();
          amtCredit = "";
        } else {
          amtDebit = "";
          amtCredit = amount.toPlainString();
        }
        fact.createLine(line,
            getAccountByWarehouse(AcctServer.ACCTTYPE_InvDifferences, as, line.getWarehouseId(),
                conn),
            line.m_C_Currency_ID, amtDebit, amtCredit, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            DocumentType, line.m_DateAcct, null, conn);

        fact.createLine(line, p.getAccount(ProductInfo.ACCTTYPE_P_Asset, as, conn),
            line.m_C_Currency_ID, amtCredit, amtDebit, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            DocumentType, line.m_DateAcct, null, conn);
      }
    } // lines

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
   * @return the serialVersionUID
   */
  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public String nextSeqNo(String oldSeqNo) {
    log4jDocCostAdjustment.debug("DocMatchInv - oldSeqNo = " + oldSeqNo);
    BigDecimal seqNo = new BigDecimal(oldSeqNo);
    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
    log4jDocCostAdjustment.debug("DocMatchInv - nextSeqNo = " + SeqNo);
    return SeqNo;
  }

  /**
   * Get the account for Accounting Schema
   * 
   * @param AcctType
   *          see ACCTTYPE_*
   * @param as
   *          accounting schema
   * @return Account
   */
  public final Account getAccountByWarehouse(String AcctType, AcctSchema as, String WarehouseId,
      ConnectionProvider conn) {
    AcctServerData[] data = null;

    try {
      /** Account Type - Invoice */
      if (AcctType.equals(ACCTTYPE_InvDifferences)) {
        /** Inventory Accounts */
        data = AcctServerData.selectWDifferencesAcct(conn, WarehouseId, as.getC_AcctSchema_ID());
      } else {
        log4jDocCostAdjustment.warn("AcctServer - getAccount - Not found AcctType=" + AcctType);
        return null;
      }
    } catch (ServletException e) {
      log4jDocCostAdjustment.warn(e);
      e.printStackTrace();
    }
    // Get Acct
    String Account_ID = "";
    if (data != null && data.length != 0) {
      Account_ID = data[0].accountId;
    } else {
      return null;
    }
    // No account
    if (Account_ID.equals("")) {
      log4jDocCostAdjustment
          .warn("AcctServer - getAccount - NO account Type=" + AcctType + ", Record=" + Record_ID);
      return null;
    }
    Account acct = null;
    try {
      acct = Account.getAccount(conn, Account_ID);
    } catch (ServletException e) {
      log4jDocCostAdjustment.warn(e);
      e.printStackTrace();
    }
    return acct;
  } // getAccount

  /**
   * Get Document Confirmation
   * 
   * not used
   */
  @Override
  public boolean getDocumentConfirmation(ConnectionProvider conn, String strRecordId) {
    boolean isGeneratedAccounting = false;

    DocLine[] local_p_lines = new DocLine[0];
    local_p_lines = loadLines(conn);
    // Lines
    for (int i = 0; local_p_lines != null && i < local_p_lines.length; i++) {
      DocLine_CostAdjustment line = (DocLine_CostAdjustment) local_p_lines[i];
      BigDecimal amount = new BigDecimal(line.getAmount());
      if (amount.signum() != 0) {
        isGeneratedAccounting = true;
      }
    }

    if (!isGeneratedAccounting) {
      setStatus(STATUS_DocumentDisabled);
    }
    return isGeneratedAccounting;
  }

  public Account getAcctByBusinessPartner(String AcctType, String bpId, AcctSchema as,
      ConnectionProvider conn) {
    AcctServerData[] data = null;
    try {
      if (AcctType.equals(ACCTTYPE_NotInvoicedReceipts)) {
        data = AcctServerData.selectNotInvoicedReceiptsAcct(conn, bpId, as.getC_AcctSchema_ID());
      }

    } catch (ServletException e) {
      log4j.warn("SelectNotInvoicedReceiptsAcct" + e);
    }

    // Get Acct
    String Account_ID = "";
    if (data != null && data.length != 0) {
      Account_ID = data[0].accountId;
    } else {
      return null;
    }
    // No account
    if (Account_ID.equals("")) {
      log4j.warn("AcctServer - getAccount - NO account Type=" + AcctType + ", Record=" + Record_ID);
      return null;
    }

    Account acct = null;
    try {
      acct = Account.getAccount(conn, Account_ID);
    } catch (ServletException e) {
      log4j.warn("Get Account" + e);
    }
    return acct;

  }

  /**
   * Set the period for the Cost Adjustment considering if all the periods matching accounting dates
   * in Cost Adjustment Lines are opened
   */
  @Override
  public void setC_Period_ID() {
    if (C_Period_ID != null) {
      return;
    }
    if (areAllPeriodsOpened(Record_ID, DocumentType)) {
      super.setC_Period_ID();
    } else {
      C_Period_ID = "";
    }
  }

  private boolean areAllPeriodsOpened(final String record_ID, final String documentType) {
    String whereClause = "costAdjustment.id = :record_ID";
    OBQuery<CostAdjustmentLine> costAdjLineQry = OBDal.getInstance()
        .createQuery(CostAdjustmentLine.class, whereClause);
    costAdjLineQry.setNamedParameter("record_ID", record_ID);
    for (CostAdjustmentLine costAdjustmentLine : costAdjLineQry.list()) {
      if (!FIN_Utility.isPeriodOpen(costAdjustmentLine.getClient().getId(), documentType,
          costAdjustmentLine.getOrganization().getId(),
          OBDateUtils.formatDate(costAdjustmentLine.getAccountingDate()))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String getServletInfo() {
    return "Servlet for the accounting";
  } // end of getServletInfo() method
}
