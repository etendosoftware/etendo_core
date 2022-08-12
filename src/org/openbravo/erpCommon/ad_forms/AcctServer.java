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
 * Contributions are Copyright (C) 2001-2019 Openbravo S.L.U.
 ******************************************************************************
 */
package org.openbravo.erpCommon.ad_forms;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.APRM_FinaccTransactionV;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.FieldProviderFactory;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.CategoryAccounts;
import org.openbravo.model.common.businesspartner.CustomerAccounts;
import org.openbravo.model.common.businesspartner.VendorAccounts;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.AcctSchemaTableDocType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.ReversedInvoice;
import org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaTable;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.gl.GLItemAccounts;
import org.openbravo.model.financialmgmt.gl.GLJournal;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;

public abstract class AcctServer {
  static Logger log4j = LogManager.getLogger();

  protected ConnectionProvider connectionProvider;

  public String batchSize = "100";

  public BigDecimal ZERO = BigDecimal.ZERO;

  public String groupLines = "";
  public String Qty = null;
  public String tableName = "";
  public String strDateColumn = "";
  public String AD_Table_ID = "";
  public String AD_Client_ID = "";
  public String AD_Org_ID = "";
  public String Status = "";
  public String C_BPartner_ID = "";
  public String C_BPartner_Location_ID = "";
  public String M_Product_ID = "";
  public String AD_OrgTrx_ID = "";
  public String C_SalesRegion_ID = "";
  public String C_Project_ID = "";
  public String C_Campaign_ID = "";
  public String C_Activity_ID = "";
  public String C_LocFrom_ID = "";
  public String C_LocTo_ID = "";
  public String User1_ID = "";
  public String User2_ID = "";
  public String C_Costcenter_ID = "";
  public String Name = "";
  public String DocumentNo = "";
  public String DateAcct = "";
  public Date dateAcct = null;
  public String DateDoc = "";
  public String C_Period_ID = "";
  public String C_Currency_ID = "";
  public String C_DocType_ID = "";
  public String C_Charge_ID = "";
  public String ChargeAmt = "";
  public String C_BankAccount_ID = "";
  public String FIN_Financial_Account_ID = "";
  public String C_CashBook_ID = "";
  public String M_Warehouse_ID = "";
  public String Posted = "";
  public String DocumentType = "";
  public String TaxIncluded = "";
  public String GL_Category_ID = "";
  public String Record_ID = "";
  public String IsReversal = "";
  public String IsReturn = "";
  /** No Currency in Document Indicator */
  protected static final String NO_CURRENCY = "-1";
  // This is just for the initialization of the accounting
  public String m_IsOpening = "N";
  // To match balances
  public String m_Record_Id2 = "";

  public Fact[] m_fact = null;
  public AcctSchema[] m_as = null;

  private FieldProvider objectFieldProvider[];

  public String[] Amounts = new String[4];

  // Conversion Rate precision. defaulted to 6 as it is stated in Format.xml
  int conversionRatePrecision = 6;

  public DocLine[] p_lines = new DocLine[0];
  public DocLine_Payment[] m_debt_payments = new DocLine_Payment[0];

  /**
   * Is (Source) Multi-Currency Document - i.e. the document has different currencies (if true, the
   * document will not be source balanced)
   */
  public boolean MultiCurrency = false;

  /** Amount Type - Invoice */
  public static final int AMTTYPE_Gross = 0;
  public static final int AMTTYPE_Net = 1;
  public static final int AMTTYPE_Charge = 2;
  /** Amount Type - Allocation */
  public static final int AMTTYPE_Invoice = 0;
  public static final int AMTTYPE_Allocation = 1;
  public static final int AMTTYPE_Discount = 2;
  public static final int AMTTYPE_WriteOff = 3;

  /** Document Status */
  public static final String STATUS_NotPosted = "N";
  /** Document Status */
  public static final String STATUS_NotBalanced = "b";
  /** Document Status */
  public static final String STATUS_NotConvertible = "c";
  /** Document Status */
  public static final String STATUS_PeriodClosed = "p";
  /** Document Status */
  public static final String STATUS_InvalidAccount = "i";
  /** Document Status */
  public static final String STATUS_PostPrepared = "y";
  /** Document Status */
  public static final String STATUS_Posted = "Y";
  /** Document Status */
  public static final String STATUS_Error = "E";
  /** Document Status */
  public static final String STATUS_InvalidCost = "C";
  /** Document Status */
  public static final String STATUS_NotCalculatedCost = "NC";
  /** Document Status */
  public static final String STATUS_NoRelatedPO = "NO";
  /** Document Status */
  public static final String STATUS_DocumentLocked = "L";
  /** Document Status */
  public static final String STATUS_DocumentDisabled = "D";
  /** Document Status */
  public static final String STATUS_TableDisabled = "T";
  /** Document Status */
  public static final String STATUS_BackgroundDisabled = "d";
  /** Document Status */
  public static final String STATUS_NoAccountingDate = "AD";

  /** Table IDs for document level conversion rates */
  public static final String TABLEID_Invoice = "318";
  public static final String TABLEID_Payment = "D1A97202E832470285C9B1EB026D54E2";
  public static final String TABLEID_Transaction = "4D8C3B3C31D1410DA046140C9F024D17";
  public static final String TABLEID_GLJournal = "224";
  public static final String TABLEID_Reconciliation = "B1B7075C46934F0A9FD4C4D0F1457B42";

  @Deprecated
  // Use TABLEID_Invoice instead
  public static final String EXCHANGE_DOCTYPE_Invoice = "318";
  @Deprecated
  // Use TABLEID_Payment instead
  public static final String EXCHANGE_DOCTYPE_Payment = "D1A97202E832470285C9B1EB026D54E2";
  @Deprecated
  // Use TABLEID_Transaction instead
  public static final String EXCHANGE_DOCTYPE_Transaction = "4D8C3B3C31D1410DA046140C9F024D17";

  OBError messageResult = null;
  String strMessage = null;

  /** AR Invoices */
  public static final String DOCTYPE_ARInvoice = "ARI";
  /** Return Material Sales Invoice */
  public static final String DOCTYPE_RMSalesInvoice = "ARI_RM";
  /** AR Credit Memo */
  public static final String DOCTYPE_ARCredit = "ARC";
  /** AR Receipt */
  public static final String DOCTYPE_ARReceipt = "ARR";
  /** AR ProForma */
  public static final String DOCTYPE_ARProForma = "ARF";

  /** AP Invoices */
  public static final String DOCTYPE_APInvoice = "API";
  /** AP Credit Memo */
  public static final String DOCTYPE_APCredit = "APC";
  /** AP Payment */
  public static final String DOCTYPE_APPayment = "APP";

  /** CashManagement Bank Statement */
  public static final String DOCTYPE_BankStatement = "CMB";
  /** CashManagement Cash Journals */
  public static final String DOCTYPE_CashJournal = "CMC";
  /** CashManagement Allocations */
  public static final String DOCTYPE_Allocation = "CMA";

  /** Amortization */
  public static final String DOCTYPE_Amortization = "AMZ";

  /** Material Shipment */
  public static final String DOCTYPE_MatShipment = "MMS";
  /** Material Receipt */
  public static final String DOCTYPE_MatReceipt = "MMR";
  /** Material Inventory */
  public static final String DOCTYPE_MatInventory = "MMI";
  /** Material Movement */
  public static final String DOCTYPE_MatMovement = "MMM";
  /** Material Production */
  public static final String DOCTYPE_MatProduction = "MMP";
  /** Material Internal Consumption */
  public static final String DOCTYPE_MatInternalConsumption = "MIC";

  /** Match Invoice */
  public static final String DOCTYPE_MatMatchInv = "MXI";
  /** Match PO */
  public static final String DOCTYPE_MatMatchPO = "MXP";

  /** GL Journal */
  public static final String DOCTYPE_GLJournal = "GLJ";

  /** Purchase Order */
  public static final String DOCTYPE_POrder = "POO";
  /** Sales Order */
  public static final String DOCTYPE_SOrder = "SOO";

  // DPManagement
  public static final String DOCTYPE_DPManagement = "DPM";

  // FinAccTransaction
  public static final String DOCTYPE_FinAccTransaction = "FAT";
  // FinReconciliation
  public static final String DOCTYPE_Reconciliation = "REC";
  // FinBankStatement
  public static final String DOCTYPE_FinBankStatement = "BST";
  // CostAdjustment
  public static final String DOCTYPE_CostAdjustment = "CAD";
  // LandedCost
  public static final String DOCTYPE_LandedCost = "LDC";
  // LandedCostCost
  public static final String DOCTYPE_LandedCostCost = "LCC";

  /*************************************************************************/

  /** Account Type - Invoice */
  public static final String ACCTTYPE_Charge = "0";
  public static final String ACCTTYPE_C_Receivable = "1";
  public static final String ACCTTYPE_V_Liability = "2";
  public static final String ACCTTYPE_V_Liability_Services = "3";

  /** Account Type - Payment */
  public static final String ACCTTYPE_UnallocatedCash = "10";
  public static final String ACCTTYPE_BankInTransit = "11";
  public static final String ACCTTYPE_PaymentSelect = "12";
  public static final String ACCTTYPE_WriteOffDefault = "13";
  public static final String ACCTTYPE_WriteOffDefault_Revenue = "63";
  public static final String ACCTTYPE_BankInTransitDefault = "14";
  public static final String ACCTTYPE_ConvertChargeDefaultAmt = "15";
  public static final String ACCTTYPE_ConvertGainDefaultAmt = "16";

  /** Account Type - Cash */
  public static final String ACCTTYPE_CashAsset = "20";
  public static final String ACCTTYPE_CashTransfer = "21";
  public static final String ACCTTYPE_CashExpense = "22";
  public static final String ACCTTYPE_CashReceipt = "23";
  public static final String ACCTTYPE_CashDifference = "24";

  /** Account Type - Allocation */
  public static final String ACCTTYPE_DiscountExp = "30";
  public static final String ACCTTYPE_DiscountRev = "31";
  public static final String ACCTTYPE_WriteOff = "32";
  public static final String ACCTTYPE_WriteOff_Revenue = "64";

  /** Account Type - Bank Statement */
  public static final String ACCTTYPE_BankAsset = "40";
  public static final String ACCTTYPE_InterestRev = "41";
  public static final String ACCTTYPE_InterestExp = "42";
  public static final String ACCTTYPE_ConvertChargeLossAmt = "43";
  public static final String ACCTTYPE_ConvertChargeGainAmt = "44";

  /** Inventory Accounts */
  public static final String ACCTTYPE_InvDifferences = "50";
  public static final String ACCTTYPE_NotInvoicedReceipts = "51";

  /** Project Accounts */
  public static final String ACCTTYPE_ProjectAsset = "61";
  public static final String ACCTTYPE_ProjectWIP = "62";

  /** GL Accounts */
  public static final String ACCTTYPE_PPVOffset = "60";

  // Reference (to find SalesRegion from BPartner)
  public String BP_C_SalesRegion_ID = ""; // set in FactLine

  public int errors = 0;
  public static boolean throwErrors = false;
  int success = 0;
  // Distinguish background process
  boolean isBackground = false;

  /**
   * Constructor
   * 
   * @param m_AD_Client_ID
   *          Client ID of these Documents
   * @param connectionProvider
   *          Provider for db connections.
   */
  public AcctServer(String m_AD_Client_ID, String m_AD_Org_ID,
      ConnectionProvider connectionProvider) {
    AD_Client_ID = m_AD_Client_ID;
    AD_Org_ID = m_AD_Org_ID;
    this.connectionProvider = connectionProvider;
    if (log4j.isDebugEnabled()) {
      log4j.debug("AcctServer - LOADING ARRAY: " + m_AD_Client_ID);
    }
    m_as = AcctSchema.getAcctSchemaArray(connectionProvider, m_AD_Client_ID, m_AD_Org_ID);
  } //

  /*
   * Empty constructor to initialize the class using reflexion, set() method should be called
   * afterwards.
   */

  public AcctServer() {

  }

  public void setBatchSize(String newbatchSize) {
    batchSize = newbatchSize;
  }

  public void run(VariablesSecureApp vars) throws IOException, ServletException {
    run(vars, null, null);
  }

  public void run(VariablesSecureApp vars, String strDateFrom, String strDateTo)
      throws IOException, ServletException {
    if (AD_Client_ID.equals("")) {
      AD_Client_ID = vars.getClient();
    }
    Connection con = null;
    try {
      String strIDs = "";

      if (log4j.isDebugEnabled()) {
        log4j.debug("AcctServer - Run - TableName = " + tableName + strDateFrom + strDateTo);
      }

      log4j.debug("AcctServer.run - AD_Client_ID: " + AD_Client_ID);

      AcctServerData[] data = null;
      final Set<String> orgSet = OBContext.getOBContext()
          .getOrganizationStructureProvider(AD_Client_ID)
          .getChildTree(AD_Org_ID, true);
      String strOrgs = Utility.getInStrSet(orgSet);
      // Send limit manually to SQL because auto-generated query doesn't limit properly
      String limit = StringUtils.equals(connectionProvider.getRDBMS(), "ORACLE")
          ? " AND ROWNUM < " + batchSize
          : " LIMIT " + batchSize;
      data = AcctServerData.select(connectionProvider, tableName, strDateColumn, AD_Client_ID,
          strOrgs, strDateFrom, strDateTo, limit);
      if (data != null && data.length > 0) {
        if (log4j.isDebugEnabled()) {
          log4j.debug("AcctServer - Run -Select inicial realizada N = " + data.length + " - Key: "
              + data[0].id);
        }
      }

      for (int i = 0; data != null && i < data.length; i++) {
        con = connectionProvider.getTransactionConnection();
        strIDs += data[i].getField("ID") + ", ";
        this.setMessageResult(null);
        AcctServer tempServer = get(AD_Table_ID, AD_Client_ID, AD_Org_ID, connectionProvider);
        boolean postSuccess = false;
        postSuccess = tempServer.post(data[i].getField("ID"), false, vars, connectionProvider, con);
        errors = errors + tempServer.errors;
        success = success + tempServer.success;
        if (!postSuccess) {
          connectionProvider.releaseRollbackConnection(con);
          return;
        } else {
          connectionProvider.releaseCommitConnection(con);
          OBDal.getInstance().commitAndClose();
        }
      }
      if (log4j.isDebugEnabled() && data != null) {
        log4j.debug("AcctServer - Run -" + data.length + " IDs [" + strIDs + "]");
      }
    } catch (NoConnectionAvailableException ex) {
      throw new ServletException("@CODE=NoConnectionAvailable", ex);
    } catch (SQLException ex2) {
      try {
        connectionProvider.releaseRollbackConnection(con);
      } catch (SQLException se) {
        log4j.error("Failed to close connection after an error", se);
      }
      throw new ServletException(
          "@CODE=" + Integer.toString(ex2.getErrorCode()) + "@" + ex2.getMessage(), ex2);
    } catch (Exception ex3) {
      log4j.error("Exception in AcctServer.run", ex3);
      try {
        connectionProvider.releaseRollbackConnection(con);
      } catch (SQLException se) {
        log4j.error("Failed to close connection after an error", se);
      }
    }
  }

  /**
   * @return the isBackground
   */
  public boolean isBackground() {
    return isBackground;
  }

  /**
   * @param isBackground
   *          the isBackground to set
   */
  public void setBackground(boolean isBackground) {
    this.isBackground = isBackground;
  }

  /**
   * Factory - Create Posting document
   * 
   * @param AD_Table_ID
   *          Table ID of Documents
   * @param AD_Client_ID
   *          Client ID of Documents
   * @param connectionProvider
   *          Database connection provider
   * @return Document
   */
  public static AcctServer get(String AD_Table_ID, String AD_Client_ID, String AD_Org_ID,
      ConnectionProvider connectionProvider) throws ServletException {
    AcctServer acct = null;
    if (log4j.isDebugEnabled()) {
      log4j.debug("get - table: " + AD_Table_ID);
    }
    if (AD_Table_ID.equals("318") || AD_Table_ID.equals("800060") || AD_Table_ID.equals("800176")
        || AD_Table_ID.equals("407") || AD_Table_ID.equals("392") || AD_Table_ID.equals("259")
        || AD_Table_ID.equals("800019") || AD_Table_ID.equals("319") || AD_Table_ID.equals("321")
        || AD_Table_ID.equals("323") || AD_Table_ID.equals("325") || AD_Table_ID.equals("224")
        || AD_Table_ID.equals("472") || AD_Table_ID.equals("800168")) {
      switch (Integer.parseInt(AD_Table_ID)) {
        case 318:
          acct = new DocInvoice(AD_Client_ID, AD_Org_ID, connectionProvider);
          acct.tableName = "C_Invoice";
          acct.AD_Table_ID = "318";
          acct.strDateColumn = "DateAcct";
          acct.reloadAcctSchemaArray();
          acct.groupLines = AcctServerData.selectGroupLines(acct.connectionProvider, AD_Client_ID);
          break;
        /*
         * case 390: acct = new DocAllocation (AD_Client_ID); acct.strDateColumn = "";
         * acct.AD_Table_ID = "390"; acct.reloadAcctSchemaArray(); acct.break;
         */
        case 800060:
          acct = new DocAmortization(AD_Client_ID, AD_Org_ID, connectionProvider);
          acct.tableName = "A_Amortization";
          acct.AD_Table_ID = "800060";
          acct.strDateColumn = "DateAcct";
          acct.reloadAcctSchemaArray();
          break;

        case 800176:
          if (log4j.isDebugEnabled()) {
            log4j.debug("AcctServer - Get DPM");
          }
          acct = new DocDPManagement(AD_Client_ID, AD_Org_ID, connectionProvider);
          acct.tableName = "C_DP_Management";
          acct.AD_Table_ID = "800176";
          acct.strDateColumn = "DateAcct";
          acct.reloadAcctSchemaArray();
          break;
        case 407:
          acct = new DocCash(AD_Client_ID, AD_Org_ID, connectionProvider);
          acct.tableName = "C_Cash";
          acct.strDateColumn = "DateAcct";
          acct.AD_Table_ID = "407";
          acct.reloadAcctSchemaArray();
          break;
        case 392:
          acct = new DocBank(AD_Client_ID, AD_Org_ID, connectionProvider);
          acct.tableName = "C_Bankstatement";
          acct.strDateColumn = "StatementDate";
          acct.AD_Table_ID = "392";
          acct.reloadAcctSchemaArray();
          break;
        case 259:
          acct = new DocOrder(AD_Client_ID, AD_Org_ID, connectionProvider);
          acct.tableName = "C_Order";
          acct.strDateColumn = "DateAcct";
          acct.AD_Table_ID = "259";
          acct.reloadAcctSchemaArray();
          break;
        case 800019:
          acct = new DocPayment(AD_Client_ID, AD_Org_ID, connectionProvider);
          acct.tableName = "C_Settlement";
          acct.strDateColumn = "Dateacct";
          acct.AD_Table_ID = "800019";
          acct.reloadAcctSchemaArray();
          break;
        case 319:
          acct = new DocInOut(AD_Client_ID, AD_Org_ID, connectionProvider);
          acct.tableName = "M_InOut";
          acct.strDateColumn = "DateAcct";
          acct.AD_Table_ID = "319";
          acct.reloadAcctSchemaArray();
          break;
        case 321:
          acct = new DocInventory(AD_Client_ID, AD_Org_ID, connectionProvider);
          acct.tableName = "M_Inventory";
          acct.strDateColumn = "MovementDate";
          acct.AD_Table_ID = "321";
          acct.reloadAcctSchemaArray();
          break;
        case 323:
          acct = new DocMovement(AD_Client_ID, AD_Org_ID, connectionProvider);
          acct.tableName = "M_Movement";
          acct.strDateColumn = "MovementDate";
          acct.AD_Table_ID = "323";
          acct.reloadAcctSchemaArray();
          break;
        case 325:
          acct = new DocProduction(AD_Client_ID, AD_Org_ID, connectionProvider);
          acct.tableName = "M_Production";
          acct.strDateColumn = "MovementDate";
          acct.AD_Table_ID = "325";
          acct.reloadAcctSchemaArray();
          break;
        case 224:
          if (log4j.isDebugEnabled()) {
            log4j.debug("AcctServer - Before OBJECT CREATION");
          }
          acct = new DocGLJournal(AD_Client_ID, AD_Org_ID, connectionProvider);
          acct.tableName = "GL_Journal";
          acct.strDateColumn = "DateAcct";
          acct.AD_Table_ID = "224";
          acct.reloadAcctSchemaArray();
          break;
        case 472:
          acct = new DocMatchInv(AD_Client_ID, AD_Org_ID, connectionProvider);
          acct.tableName = "M_MatchInv";
          acct.strDateColumn = "DateTrx";
          acct.AD_Table_ID = "472";
          acct.reloadAcctSchemaArray();
          break;
        case 800168:
          acct = new DocInternalConsumption(AD_Client_ID, AD_Org_ID, connectionProvider);
          acct.tableName = "M_Internal_Consumption";
          acct.strDateColumn = "MovementDate";
          acct.AD_Table_ID = "800168";
          acct.reloadAcctSchemaArray();
          break;
      }
    } else {
      AcctServerData[] acctinfo = AcctServerData.getTableInfo(connectionProvider, AD_Table_ID);
      if (acctinfo != null && acctinfo.length != 0) {
        if (!acctinfo[0].acctclassname.equals("") && !acctinfo[0].acctdatecolumn.equals("")) {
          try {
            acct = (AcctServer) Class.forName(acctinfo[0].acctclassname)
                .getDeclaredConstructor()
                .newInstance();
            acct.set(AD_Table_ID, AD_Client_ID, AD_Org_ID, connectionProvider,
                acctinfo[0].tablename, acctinfo[0].acctdatecolumn);
            acct.reloadAcctSchemaArray();
          } catch (Exception e) {
            log4j.error("Error while creating new instance for AcctServer - " + e, e);
          }
        }
      }
    }

    if (acct == null) {
      log4j.warn("AcctServer - get - Unknown AD_Table_ID=" + AD_Table_ID);
    } else if (log4j.isDebugEnabled()) {
      log4j.debug("AcctServer - get - AcctSchemaArray length=" + (acct.m_as).length);
    }
    if (log4j.isDebugEnabled()) {
      log4j.debug("AcctServer - get - AD_Table_ID=" + AD_Table_ID);
    }
    return acct;
  } // get

  public void set(String m_AD_Table_ID, String m_AD_Client_ID, String m_AD_Org_ID,
      ConnectionProvider connectionProvider, String tablename, String acctdatecolumn) {
    AD_Client_ID = m_AD_Client_ID;
    AD_Org_ID = m_AD_Org_ID;
    this.connectionProvider = connectionProvider;
    tableName = tablename;
    strDateColumn = acctdatecolumn;
    AD_Table_ID = m_AD_Table_ID;
    if (log4j.isDebugEnabled()) {
      log4j.debug("AcctServer - LOADING ARRAY: " + m_AD_Client_ID);
    }
    m_as = AcctSchema.getAcctSchemaArray(connectionProvider, m_AD_Client_ID, m_AD_Org_ID);
  }

  public void reloadAcctSchemaArray() throws ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("AcctServer - reloadAcctSchemaArray - " + AD_Table_ID);
    }
    AcctSchema acct = null;
    ArrayList<Object> new_as = new ArrayList<Object>();
    for (int i = 0; i < (this.m_as).length; i++) {
      acct = m_as[i];
      if (AcctSchemaData.selectAcctSchemaTable(connectionProvider, acct.m_C_AcctSchema_ID,
          AD_Table_ID)) {
        new_as.add(new AcctSchema(connectionProvider, acct.m_C_AcctSchema_ID));
      }
    }
    AcctSchema[] retValue = new AcctSchema[new_as.size()];
    new_as.toArray(retValue);
    if (log4j.isDebugEnabled()) {
      log4j.debug("AcctServer - RELOADING ARRAY: " + retValue.length);
    }
    this.m_as = retValue;
  }

  private void reloadAcctSchemaArray(String adOrgId) throws ServletException {
    if (log4j.isDebugEnabled()) {
      log4j
          .debug("AcctServer - reloadAcctSchemaArray - " + AD_Table_ID + ", AD_ORG_ID: " + adOrgId);
    }
    AcctSchema acct = null;
    ArrayList<Object> new_as = new ArrayList<Object>();
    // We reload again all the acct schemas of the client
    m_as = AcctSchema.getAcctSchemaArray(connectionProvider, AD_Client_ID, AD_Org_ID);
    // Filter the right acct schemas for the organization
    for (int i = 0; i < (this.m_as).length; i++) {
      acct = m_as[i];
      if (AcctSchemaData.selectAcctSchemaTable(connectionProvider, acct.m_C_AcctSchema_ID,
          AD_Table_ID)) {
        new_as.add(new AcctSchema(connectionProvider, acct.m_C_AcctSchema_ID));
      }
    }
    AcctSchema[] retValue = new AcctSchema[new_as.size()];
    new_as.toArray(retValue);
    if (log4j.isDebugEnabled()) {
      log4j.debug("AcctServer - RELOADING ARRAY: " + retValue.length);
    }
    this.m_as = retValue;
  }

  /**
   * This method handles the accounting of the record identified by strClave. Due to the possibility
   * of developing processes that may run after the standard accounting the commit of the
   * transactions made in this method cannot be done here. The commit must be handled in the caller
   * of this method appropriately. Realize that you must handle the commit of the transactions made
   * through a normal or a DAL connection.
   */
  public boolean post(String strClave, boolean force, VariablesSecureApp vars,
      ConnectionProvider conn, Connection con) throws ServletException {
    Record_ID = strClave;
    if (log4j.isDebugEnabled()) {
      log4j.debug("post " + strClave + " tablename: " + tableName);
    }
    try {
      if (AcctServerData.update(conn, tableName, strClave) != 1) {
        log4j.warn(
            "AcctServer - Post -Cannot lock Document - ignored: " + tableName + "_ID=" + strClave);
        setStatus(STATUS_DocumentLocked); // Status locked document
        this.setMessageResult(conn, vars, STATUS_DocumentLocked, "Error");
        return false;
      } else {
        AcctServerData.delete(connectionProvider, AD_Table_ID, Record_ID);
      }
      if (log4j.isDebugEnabled()) {
        log4j.debug("AcctServer - Post -TableName -" + tableName + "- ad_client_id -" + AD_Client_ID
            + "- " + tableName + "_id -" + strClave);
      }
      try {
        loadObjectFieldProvider(connectionProvider, AD_Client_ID, strClave);
      } catch (ServletException e) {
        log4j.warn(e);
        e.printStackTrace();
      }
      FieldProvider data[] = getObjectFieldProvider();
      // If there is any template active for current document in any accounting schema, skip this
      // step as getDocumentConfirmation can lock template
      try {
        if ((disableDocumentConfirmation() || getDocumentConfirmation(conn, Record_ID))
            && post(data, force, vars, conn, con)) {
          success++;
        } else {
          errors++;
          if (messageResult == null) {
            setMessageResult(conn, vars, getStatus(), "");
          }
          if (throwErrors) {
            throw new OBException(messageResult.getMessage());
          }
          save(conn, vars.getUser());
        }
      } catch (Exception e) {
        errors++;
        save(conn, vars.getUser());
        log4j.error("An error ocurred posting RecordId: " + strClave + " - tableId: " + AD_Table_ID,
            e);
        if(throwErrors) {
          if (messageResult == null){
            OBError error = new OBError();
            error.setMessage(e.getMessage());
            error.setType(STATUS_Error);
            setMessageResult(error);
          }
          throw new OBException(messageResult.getMessage());
        }
      }
    } catch (ServletException e) {
      log4j.error(e);
      if (throwErrors) {
        throw new OBException(e.getMessage());
      }
      return false;
    }
    return true;
  }

  private boolean post(FieldProvider[] data, boolean force, VariablesSecureApp vars,
      ConnectionProvider conn, Connection con) throws ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("post data" + C_Currency_ID);
    }
    if (!loadDocument(data, force, conn, con)) {
      log4j.warn("AcctServer - post - Error loading document");
      return false;
    }
    // Set Currency precision
    conversionRatePrecision = getConversionRatePrecision(vars);
    if (data == null || data.length == 0) {
      return false;
    }
    if (!String.valueOf(data[0].getField("multiGl")).equals("N")) {
      reloadAcctSchemaArray(AD_Org_ID);
    }
    m_fact = new Fact[m_as.length];
    // AcctSchema Table check
    boolean isTableActive = false;
    try {
      OBContext.setAdminMode(true);
      for (AcctSchema as : m_as) {
        AcctSchemaTable table = null;
        OBCriteria<AcctSchemaTable> criteria = OBDao.getFilteredCriteria(AcctSchemaTable.class,
            Restrictions.eq("accountingSchema.id", as.getC_AcctSchema_ID()),
            Restrictions.eq("table.id", AD_Table_ID));
        criteria.setFilterOnReadableClients(false);
        criteria.setFilterOnReadableOrganization(false);
        table = (AcctSchemaTable) criteria.uniqueResult();
        if (table != null) {
          isTableActive = true;
          break;
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    if (!isTableActive) {
      setMessageResult(conn, vars, STATUS_TableDisabled, "Warning");
      return false;
    }
    // for all Accounting Schema
    boolean OK = true;
    if (log4j.isDebugEnabled()) {
      log4j.debug("AcctServer - Post -Beforde the loop - C_CURRENCY_ID = " + C_Currency_ID);
    }
    for (int i = 0; OK && i < m_as.length; i++) {
      setStatus(STATUS_NotPosted);
      if (isBackground && !isBackGroundEnabled(conn, m_as[i], AD_Table_ID)) {
        setStatus(STATUS_BackgroundDisabled);
        break;
      }
      if (log4j.isDebugEnabled()) {
        log4j.debug("AcctServer - Post - Before the postLogic - C_CURRENCY_ID = " + C_Currency_ID);
      }
      Status = postLogic(i, conn, con, vars, m_as[i]);
      if (log4j.isDebugEnabled()) {
        log4j.debug("AcctServer - Post - After postLogic");
      }
      if (!Status.equals(STATUS_Posted)) {
        return false;
      }
    }
    if (log4j.isDebugEnabled()) {
      log4j.debug("AcctServer - Post - Before the postCommit - C_CURRENCY_ID = " + C_Currency_ID);
    }
    for (int i = 0; i < m_fact.length; i++) {
      if (m_fact[i] != null && (m_fact[i].getLines() == null || m_fact[i].getLines().length == 0) && !STATUS_Posted.equals(getStatus())) {
        return false;
      }
    }
    // commitFact
    Status = postCommit(Status, conn, vars, con);

    // dispose facts
    for (int i = 0; i < m_fact.length; i++) {
      if (m_fact[i] != null) {
        m_fact[i].dispose();
      }
    }
    p_lines = null;
    return Status.equals(STATUS_Posted);
  } // post

  boolean isBackGroundEnabled(ConnectionProvider conn, AcctSchema acctSchema, String adTableId)
      throws ServletException {
    return AcctServerData.selectBackgroundEnabled(conn, acctSchema.m_C_AcctSchema_ID, adTableId);
  }

  /**
   * Post Commit. Save Facts & Document
   * 
   * @param status
   *          status
   * @return Posting Status
   */
  private final String postCommit(String status, ConnectionProvider conn, VariablesSecureApp vars,
      Connection con) throws ServletException {
    log4j.debug(
        "AcctServer - postCommit Sta=" + status + " DT=" + DocumentType + " ID=" + Record_ID);
    Status = status;
    try {
      // *** Transaction Start ***
      // Commit Facts
      if (Status.equals(AcctServer.STATUS_Posted)) {
        if (m_fact != null && m_fact.length != 0) {
          log4j.debug("AcctServer - postCommit - m_fact.length = " + m_fact.length);
          for (int i = 0; i < m_fact.length; i++) {
            if (!m_fact[i].save(con, conn, vars)) {
              unlock(conn);
              Status = AcctServer.STATUS_Error;
            }
          }
        }
      }
      // Commit Doc
      if (!save(conn, vars.getUser())) { // contains unlock
        unlock(conn);
      }
      // *** Transaction End ***
    } catch (Exception e) {
      log4j.warn("AcctServer - postCommit" + e);
      Status = AcctServer.STATUS_Error;
      unlock(conn);
    }
    return Status;
  } // postCommit

  /**
   * Save to Disk - set posted flag
   * 
   * @param conn
   *          connection
   * @param strUser
   *          AD_User_ID
   * @return true if saved
   */
  private final boolean save(ConnectionProvider conn, String strUser) {
    int no = 0;
    try {
      no = AcctServerData.updateSave(conn, tableName, Status, strUser, Record_ID);
    } catch (ServletException e) {
      log4j.warn(e);
      e.printStackTrace();
    }
    return no == 1;
  } // save

  /**
   * Unlock Document
   */
  private void unlock(ConnectionProvider conn) {
    try {
      AcctServerData.updateUnlock(conn, tableName, Record_ID);
    } catch (ServletException e) {
      log4j.warn("AcctServer - Document locked: -" + e);
    }
  } // unlock

  @Deprecated
  // Deprecated in 2.50 because of a missing connection needed
  public boolean loadDocument(FieldProvider[] data, boolean force, ConnectionProvider conn) {
    try {
      Connection con = conn.getConnection();
      return loadDocument(data, force, conn, con);
    } catch (NoConnectionAvailableException e) {
      log4j.warn(e);
      e.printStackTrace();
      return false;
    }
  }

  public boolean loadDocument(FieldProvider[] data, boolean force, ConnectionProvider conn,
      Connection con) {
    if (log4j.isDebugEnabled()) {
      log4j.debug("loadDocument " + data.length);
    }

    setStatus(STATUS_NotPosted);
    Name = "";
    AD_Client_ID = data[0].getField("AD_Client_ID");
    AD_Org_ID = data[0].getField("AD_Org_ID");
    C_BPartner_ID = data[0].getField("C_BPartner_ID");
    M_Product_ID = data[0].getField("M_Product_ID");
    AD_OrgTrx_ID = data[0].getField("AD_OrgTrx_ID");
    C_SalesRegion_ID = data[0].getField("C_SalesRegion_ID");
    C_Project_ID = data[0].getField("C_Project_ID");
    C_Campaign_ID = data[0].getField("C_Campaign_ID");
    C_Activity_ID = data[0].getField("C_Activity_ID");
    C_LocFrom_ID = data[0].getField("C_LocFrom_ID");
    C_LocTo_ID = data[0].getField("C_LocTo_ID");
    User1_ID = data[0].getField("User1_ID");
    User2_ID = data[0].getField("User2_ID");
    C_Costcenter_ID = data[0].getField("C_Costcenter_ID");

    Name = data[0].getField("Name");
    DocumentNo = data[0].getField("DocumentNo");
    DateAcct = data[0].getField("DateAcct");
    DateDoc = data[0].getField("DateDoc");
    C_Period_ID = data[0].getField("C_Period_ID");
    C_Currency_ID = data[0].getField("C_Currency_ID");
    C_DocType_ID = data[0].getField("C_DocType_ID");
    C_Charge_ID = data[0].getField("C_Charge_ID");
    ChargeAmt = data[0].getField("ChargeAmt");
    C_BankAccount_ID = data[0].getField("C_BankAccount_ID");
    FIN_Financial_Account_ID = data[0].getField("FIN_Financial_Account_ID");
    if (log4j.isDebugEnabled()) {
      log4j.debug("AcctServer - loadDocument - C_BankAccount_ID : " + C_BankAccount_ID);
    }
    Posted = data[0].getField("Posted");
    if (!loadDocumentDetails(data, conn)) {
      loadDocumentType();
    }
    if ((DateAcct == null || DateAcct.equals("")) && (DateDoc != null && !DateDoc.equals(""))) {
      DateAcct = DateDoc;
    } else if ((DateDoc == null || DateDoc.equals(""))
        && (DateAcct != null && !DateAcct.equals(""))) {
      DateDoc = DateAcct;
    }
    // DocumentNo (or Name)
    if (DocumentNo == null || DocumentNo.length() == 0) {
      DocumentNo = Name;
    }
    // Check Mandatory Info
    String error = "";
    if (AD_Table_ID == null || AD_Table_ID.equals("")) {
      error += " AD_Table_ID";
    }
    if (Record_ID == null || Record_ID.equals("")) {
      error += " Record_ID";
    }
    if (AD_Client_ID == null || AD_Client_ID.equals("")) {
      error += " AD_Client_ID";
    }
    if (AD_Org_ID == null || AD_Org_ID.equals("")) {
      error += " AD_Org_ID";
    }
    if (C_Currency_ID == null || C_Currency_ID.equals("")) {
      error += " C_Currency_ID";
    }
    if (DateAcct == null || DateAcct.equals("")) {
      error += " DateAcct";
    }
    if (DateDoc == null || DateDoc.equals("")) {
      error += " DateDoc";
    }
    if (error.length() > 0) {
      log4j.warn(
          "AcctServer - loadDocument - " + DocumentNo + " - Mandatory info missing: " + error);
      return false;
    }
    try {
      dateAcct = OBDateUtils.getDate(DateAcct);
    } catch (ParseException e1) {
      // Do nothing
    }

    // Delete existing Accounting
    if (force) {
      if (Posted.equals("Y") && !isPeriodOpen()) { // already posted -
        // don't delete if
        // period closed
        log4j.warn("AcctServer - loadDocument - " + DocumentNo
            + " - Period Closed for already posted document");
        return false;
      }
      // delete it
      try {
        AcctServerData.delete(connectionProvider, AD_Table_ID, Record_ID);
      } catch (ServletException e) {
        log4j.warn(e);
        e.printStackTrace();
      }
    } else if (Posted.equals("Y")) {
      log4j.warn("AcctServer - loadDocument - " + DocumentNo + " - Document already posted");
      return false;
    }
    return true;
  } // loadDocument

  public void loadDocumentType() {
    loadDocumentType(false);
  }

  public void loadDocumentType(boolean supressWarnings) {
    try {
      if (/* DocumentType.equals("") && */C_DocType_ID != null && C_DocType_ID != "") {
        AcctServerData[] data = AcctServerData.selectDocType(connectionProvider, C_DocType_ID);
        DocumentType = data[0].docbasetype;
        GL_Category_ID = data[0].glCategoryId;
        IsReversal = data[0].isreversal;
        IsReturn = data[0].isreturn;
      }
      // We have a document Type, but no GL info - search for DocType
      if (GL_Category_ID != null && GL_Category_ID.equals("")) {
        AcctServerData[] data = AcctServerData.selectGLCategory(connectionProvider, AD_Client_ID,
            DocumentType);
        if (data != null && data.length != 0) {
          GL_Category_ID = data[0].glCategoryId;
          IsReversal = data[0].isreversal;
          IsReturn = data[0].isreturn;
        }
      }
      if (!supressWarnings && DocumentType != null && DocumentType.equals("")) {
        log4j.warn("AcctServer - loadDocumentType - No DocType for GL Info");
      }
      if (GL_Category_ID != null && GL_Category_ID.equals("")) {
        AcctServerData[] data = AcctServerData.selectDefaultGLCategory(connectionProvider,
            AD_Client_ID);
        GL_Category_ID = data[0].glCategoryId;
      }
    } catch (ServletException e) {
      log4j.warn(e);
      e.printStackTrace();
      if (throwErrors) {
        throw new OBException(e);
      }
    }
    if (GL_Category_ID != null && GL_Category_ID.equals("")) {
      log4j.warn("AcctServer - loadDocumentType - No GL Info");
    }
  }

  /**
   * @deprecated During cleanup for 3.0 the entire table ad_node was removed from core, so this
   *             insertNote method doesn't serve have any purpose anymore. Keep as deprecated noop
   *             in case any module may call it.
   */
  @Deprecated
  public boolean insertNote(String aD_Client_ID, String aD_Org_ID, String AD_User_ID,
      String aD_Table_ID, String record_ID, String AD_MessageValue, String Text, String Reference,
      VariablesSecureApp vars, ConnectionProvider conn, Connection con) {
    return false;
  }

  /**
   * Posting logic for Accounting Schema index
   * 
   * @param index
   *          Accounting Schema index
   * @return posting status/error code
   */
  private final String postLogic(int index, ConnectionProvider conn, Connection con,
      VariablesSecureApp vars, AcctSchema as) throws ServletException {
    // rejectUnbalanced
    if (!m_as[index].isSuspenseBalancing() && !isBalanced()) {
      return STATUS_NotBalanced;
    }

    // rejectUnconvertible
    if (!isConvertible(m_as[index], conn)) {
      return STATUS_NotConvertible;
    }
    // rejectPeriodClosed
    if (!isPeriodOpen()) {
      return STATUS_PeriodClosed;
    }

    // createFacts
    try {
      m_fact[index] = createFact(m_as[index], conn, con, vars);
    } catch (OBException e) {
      log4j.warn("Accounting process failed. RecordID: " + Record_ID + " - TableId: " + AD_Table_ID,
          e);
      String strMessageError = e.getMessage();
      if (strMessageError.indexOf("") != -1) {
        if (messageResult == null
            || (messageResult != null && StringUtils.isBlank(messageResult.getMessage()))) {
          setMessageResult(OBMessageUtils.translateError(strMessageError));
        }
        if ("@NotConvertible@".equals(strMessageError)) {
          return STATUS_NotConvertible;
        } else if (StringUtils.equals(strMessageError, "@PeriodNotAvailable@")) {
          return STATUS_PeriodClosed;
        } else if (StringUtils.equals(strMessageError, "@NotCalculatedCost@")) {
          return STATUS_NotCalculatedCost;
        }
      }
      return STATUS_Error;
    } catch (Exception e) {
      log4j.warn("Accounting process failed. RecordID: " + Record_ID + " - TableId: " + AD_Table_ID,
          e);
      return STATUS_Error;
    }
    if (!Status.equals(STATUS_NotPosted)) {
      return Status;
    }
    if (m_fact[index] == null) {
      return STATUS_Error;
    }
    Status = STATUS_PostPrepared;

    // Distinguish multi-currency Documents
    MultiCurrency = m_fact[index].isMulticurrencyDocument();
    // balanceSource
    if (!MultiCurrency && !m_fact[index].isSourceBalanced()) {
      m_fact[index].balanceSource(conn);
    }
    // balanceSegments
    if (!MultiCurrency && !m_fact[index].isSegmentBalanced(conn)) {
      m_fact[index].balanceSegments(conn);
    }
    // balanceAccounting
    if (!m_fact[index].isAcctBalanced()) {
      m_fact[index].balanceAccounting(conn);
    }
    // Here processes defined to be executed at posting time, when existing, will be executed
    AcctServerData[] data = AcctServerData.selectAcctProcess(conn, as.m_C_AcctSchema_ID);
    for (int i = 0; data != null && i < data.length; i++) {
      String strClassname = data[i].classname;
      if (!strClassname.equals("")) {
        try {
          AcctProcessTemplate newTemplate = (AcctProcessTemplate) Class.forName(strClassname)
              .getDeclaredConstructor()
              .newInstance();
          if (!newTemplate.execute(this, as, conn, con, vars)) {
            OBDal.getInstance().rollbackAndClose();
            return getStatus();
          }
        } catch (Exception e) {
          log4j.error("Error while creating new instance for AcctProcessTemplate - " + e);
          return AcctServer.STATUS_Error;
        }
      }
    }
    if (messageResult != null) {
      return getStatus();
    }
    return STATUS_Posted;
  } // postLogic

  /**
   * Is the Source Document Balanced
   * 
   * @return true if (source) balanced
   */
  public boolean isBalanced() {
    // Multi-Currency documents are source balanced by definition
    if (MultiCurrency) {
      return true;
    }
    //
    boolean retValue = (getBalance().compareTo(ZERO) == 0);
    if (retValue) {
      if (log4j.isDebugEnabled()) {
        log4j.debug("AcctServer - isBalanced - " + DocumentNo);
      }
    } else {
      log4j.warn("AcctServer - is not Balanced - " + DocumentNo);
    }
    return retValue;
  } // isBalanced

  /**
   * Is Document convertible to currency and Conversion Type
   * 
   * @param acctSchema
   *          accounting schema
   * @return true, if convertible to accounting currency
   */
  public boolean isConvertible(AcctSchema acctSchema, ConnectionProvider conn)
      throws ServletException {
    // No Currency in document
    if (NO_CURRENCY.equals(C_Currency_ID)) {
      return true;
    }
    // Get All Currencies
    Vector<Object> set = new Vector<Object>();
    set.addElement(C_Currency_ID);
    for (int i = 0; p_lines != null && i < p_lines.length; i++) {
      String currency = p_lines[i].m_C_Currency_ID;
      if (currency != null && !currency.equals("")) {
        set.addElement(currency);
      }
    }

    // just one and the same
    if (set.size() == 1 && acctSchema.m_C_Currency_ID.equals(C_Currency_ID)) {
      return true;
    }
    boolean convertible = true;
    for (int i = 0; i < set.size() && convertible == true; i++) {
      String currency = (String) set.elementAt(i);
      if (currency == null) {
        currency = "";
      }
      if (!currency.equals(acctSchema.m_C_Currency_ID)) {
        String amt = "";
        OBQuery<ConversionRateDoc> conversionQuery = null;
        int conversionCount = 0;
        //@formatter:off
        if (AD_Table_ID.equals(TABLEID_Invoice)) {
          String whereClause = "invoice.id = :recordId "
              + "  and currency.id = :currency "
              + "  and toCurrency.id = :toCurrency";
          conversionQuery = OBDal.getInstance().createQuery(ConversionRateDoc.class, whereClause);
        } else if (AD_Table_ID.equals(TABLEID_Payment)) {
          String whereClause = "payment.id = :recordId "
              + "  and currency.id = :currency "
              + "  and toCurrency.id = :toCurrency";
          conversionQuery = OBDal.getInstance().createQuery(ConversionRateDoc.class, whereClause);
        } else if (AD_Table_ID.equals(TABLEID_Transaction)) {
          String whereClause = "financialAccountTransaction.id = :recordId "
              + "  and currency.id = :currency "
              + "  and toCurrency.id = :toCurrency";
          conversionQuery = OBDal.getInstance().createQuery(ConversionRateDoc.class, whereClause);
        } else if (AD_Table_ID.equals(TABLEID_GLJournal)) {
          String whereClause = "journalEntry.id = :recordId "
              + "  and currency.id = :currency "
              + "  and toCurrency.id = :toCurrency";
          conversionQuery = OBDal.getInstance().createQuery(ConversionRateDoc.class, whereClause);
        }
        //@formatter:on
        if (conversionQuery != null) {
          conversionQuery.setNamedParameter("recordId", Record_ID);
          conversionQuery.setNamedParameter("currency", currency);
          conversionQuery.setNamedParameter("toCurrency", acctSchema.m_C_Currency_ID);
          conversionCount = conversionQuery.count();
        }
        try {
          OBContext.setAdminMode(true);
          if (conversionCount > 0) {
            List<ConversionRateDoc> conversionRate = conversionQuery.list();
            OBCriteria<Currency> currencyCrit = OBDal.getInstance().createCriteria(Currency.class);
            currencyCrit.add(Restrictions.eq(Currency.PROPERTY_ID, acctSchema.m_C_Currency_ID));
            currencyCrit.setProjection(Projections.max(Currency.PROPERTY_STANDARDPRECISION));
            Long precision = 0L;
            if (currencyCrit.count() > 0) {
              List<Currency> toCurrency = currencyCrit.list();
              precision = toCurrency.get(0).getStandardPrecision();
            }
            BigDecimal convertedAmount = new BigDecimal("1")
                .multiply(conversionRate.get(0).getRate());
            amt = convertedAmount.setScale(precision.intValue(), RoundingMode.HALF_UP).toString();
          }
        } finally {
          OBContext.restorePreviousMode();
        }
        if (("").equals(amt) || amt == null) {
          amt = getConvertedAmt("1", currency, acctSchema.m_C_Currency_ID, getConversionDate(),
              acctSchema.m_CurrencyRateType, AD_Client_ID, AD_Org_ID, conn);
        }
        if (amt == null || ("").equals(amt)) {
          convertible = false;
          log4j.warn("AcctServer - isConvertible NOT from " + currency + " - " + DocumentNo);
        } else if (log4j.isDebugEnabled()) {
          log4j.debug("AcctServer - isConvertible from " + currency);
        }
      }
    }
    return convertible;
  } // isConvertible

  /**
   * Get the Amount (loaded in loadDocumentDetails)
   * 
   * @param AmtType
   *          see AMTTYPE_*
   * @return Amount
   */
  public String getAmount(int AmtType) {
    if (AmtType < 0 || Amounts == null || AmtType >= Amounts.length) {
      return null;
    }
    return (Amounts[AmtType].equals("")) ? "0" : Amounts[AmtType];
  } // getAmount

  /**
   * Get Amount with index 0
   * 
   * @return Amount (primary document amount)
   */
  public String getAmount() {
    return Amounts[0];
  } // getAmount

  /**
   * Get the Convertion Date
   * 
   * @return DateAcct
   */
  protected String getConversionDate() {
    return DateAcct;
  }

  /**
   * Convert an amount
   * 
   * @param CurFrom_ID
   *          The C_Currency_ID FROM
   * @param CurTo_ID
   *          The C_Currency_ID TO
   * @param ConvDate
   *          The Conversion date - if null - use current date
   * @param RateType
   *          The Conversion rate type - if null/empty - use Spot
   * @param Amt
   *          The amount to be converted
   * @return converted amount
   */
  public static String getConvertedAmt(String Amt, String CurFrom_ID, String CurTo_ID,
      String ConvDate, String RateType, ConnectionProvider conn) {
    if (log4j.isDebugEnabled()) {
      log4j.debug("AcctServer - getConvertedAmount no client nor org");
    }
    return getConvertedAmt(Amt, CurFrom_ID, CurTo_ID, ConvDate, RateType, "", "", conn);
  }

  public static String getConvertedAmt(String Amt, String CurFrom_ID, String CurTo_ID,
      String ConvDate, String RateType, String client, String org, ConnectionProvider conn) {
    String localRateType = RateType;
    String localConvDate = ConvDate;
    if (log4j.isDebugEnabled()) {
      log4j.debug(
          "AcctServer - getConvertedAmount - starting method - Amt : " + Amt + " - CurFrom_ID : "
              + CurFrom_ID + " - CurTo_ID : " + CurTo_ID + "- ConvDate: " + localConvDate
              + " - RateType:" + localRateType + " - client:" + client + "- org:" + org);
    }
    if (Amt.equals("")) {
      throw new IllegalArgumentException(
          "AcctServer - getConvertedAmt - required parameter missing - Amt");
    }
    if (CurFrom_ID.equals(CurTo_ID) || Amt.equals("0")) {
      return Amt;
    }
    AcctServerData[] data = null;
    try {
      if (localConvDate != null && localConvDate.equals("")) {
        localConvDate = DateTimeData.today(conn);
      }
      // ConvDate IN DATE
      if (localRateType == null || localRateType.equals("")) {
        localRateType = "S";
      }
      data = AcctServerData.currencyConvert(conn, Amt, CurFrom_ID, CurTo_ID, localConvDate,
          localRateType, client, org);
    } catch (ServletException e) {
      log4j.warn(e);
      if (AcctServer.throwErrors) {
        throw new OBException(e.getMessage());
      }
    }
    if (data == null || data.length == 0) {
      return "";
    } else {
      if (log4j.isDebugEnabled()) {
        log4j.debug("getConvertedAmount - converted:" + data[0].converted);
      }
      return data[0].converted;
    }
  } // getConvertedAmt

  public static BigDecimal getConvertionRate(String CurFrom_ID, String CurTo_ID, String ConvDate,
      String RateType, String client, String org, ConnectionProvider conn) {
    String localRateType = RateType;
    String localConvDate = ConvDate;
    if (CurFrom_ID.equals(CurTo_ID)) {
      return BigDecimal.ONE;
    }
    AcctServerData[] data = null;
    try {
      if (localConvDate != null && localConvDate.equals("")) {
        localConvDate = DateTimeData.today(conn);
      }
      // ConvDate IN DATE
      if (localRateType == null || localRateType.equals("")) {
        localRateType = "S";
      }
      data = AcctServerData.currencyConvertionRate(conn, CurFrom_ID, CurTo_ID, localConvDate,
          localRateType, client, org);
    } catch (ServletException e) {
      log4j.warn(e);
      e.printStackTrace();
    }
    if (data == null || data.length == 0) {
      log4j.error("No conversion ratio");
      return BigDecimal.ZERO;
    } else {
      if (log4j.isDebugEnabled()) {
        log4j.debug("getConvertionRate - rate:" + data[0].converted);
      }
      return new BigDecimal(data[0].converted);
    }
  } // getConvertedAmt

  /**
   * Is Period Open
   * 
   * @return true if period is open
   */
  public boolean isPeriodOpen() {
    setC_Period_ID();
    boolean open = (!C_Period_ID.equals(""));
    if (open) {
      if (log4j.isDebugEnabled()) {
        log4j.debug("AcctServer - isPeriodOpen - " + DocumentNo);
      }
    } else {
      log4j.warn("AcctServer - isPeriodOpen NO - " + DocumentNo);
    }
    return open;
  } // isPeriodOpen

  /**
   * Calculate Period ID. Set to -1 if no period open, 0 if no period control
   */
  public void setC_Period_ID() {
    if (C_Period_ID != null) {
      return;
    }
    if (log4j.isDebugEnabled()) {
      log4j.debug("AcctServer - setC_Period_ID - AD_Client_ID - " + AD_Client_ID + "--DateAcct - "
          + DateAcct + "--DocumentType -" + DocumentType);
    }
    AcctServerData[] data = null;
    try {
      if (log4j.isDebugEnabled()) {
        log4j.debug("setC_Period_ID - inside try - AD_Client_ID - " + AD_Client_ID
            + " -- DateAcct - " + DateAcct + " -- DocumentType - " + DocumentType);
      }

      String strOrgCalendarOwner = OBContext.getOBContext()
          .getOrganizationStructureProvider(AD_Client_ID)
          .getPeriodControlAllowedOrganization(
              OBDal.getInstance().get(Organization.class, AD_Org_ID))
          .getId();
      data = AcctServerData.selectPeriodOpen(connectionProvider, AD_Client_ID, DocumentType,
          strOrgCalendarOwner, DateAcct);
      C_Period_ID = data[0].period;

      if (log4j.isDebugEnabled()) {
        log4j.debug("AcctServer - setC_Period_ID - " + AD_Client_ID + "/" + DateAcct + "/"
            + DocumentType + " => " + C_Period_ID);
      }
    } catch (ServletException e) {
      log4j.warn(e);
      e.printStackTrace();
    }
  } // setC_Period_ID

  /**
   * Matching
   * 
   * <pre>
   *  Derive Invoice-Receipt Match from PO-Invoice and PO-Receipt
   *  Purchase Order (20)
   *  - Invoice1 (10)
   *  - Invoice2 (10)
   *  - Receipt1 (5)
   *  - Receipt2 (15)
   *  (a) Creates Directs
   *      - Invoice1 - Receipt1 (5)
   *      - Invoice2 - Receipt2 (10)
   *  (b) Creates Indirects
   *      - Invoice1 - Receipt2 (5)
   *  (Not imlemented)
   * 
   * 
   * </pre>
   * 
   * @return number of records created
   */
  public int match(VariablesSecureApp vars, ConnectionProvider conn, Connection con) {
    int counter = 0;
    // (a) Direct Matches
    AcctServerData[] data = null;
    try {
      data = AcctServerData.selectMatch(conn, AD_Client_ID);
      for (int i = 0; i < data.length; i++) {
        BigDecimal qty1 = new BigDecimal(data[i].qty1);
        BigDecimal qty2 = new BigDecimal(data[i].qty2);
        BigDecimal qty = qty1.min(qty2);
        if (qty.toString().equals("0")) {
          continue;
        }
        String dateTrx1 = data[i].datetrx1;
        String dateTrx2 = data[i].datetrx2;
        String compare = "";
        try {
          compare = DateTimeData.compare(conn, dateTrx1, dateTrx2);
        } catch (ServletException e) {
          log4j.warn(e);
          e.printStackTrace();
        }
        String DateTrx = dateTrx1;
        if (compare.equals("-1")) {
          DateTrx = dateTrx2;
        }
        //
        String strQty = qty.toString();
        String strDateTrx = DateTrx;
        String adClientId = data[i].adClientId;
        String adOrgId = data[i].adOrgId;
        String C_InvoiceLine_ID = data[i].cInvoicelineId;
        String M_InOutLine_ID = data[i].mInoutlineId;
        String mProductId = data[i].mProductId;
        //
        if (createMatchInv(adClientId, adOrgId, M_InOutLine_ID, C_InvoiceLine_ID, mProductId,
            strDateTrx, strQty, vars, conn, con) == 1) {
          counter++;
        }
      }
    } catch (ServletException e) {
      log4j.warn(e);
      e.printStackTrace();
    }
    return counter;
  } // match

  /**
   * Create MatchInv record
   * 
   * @param aD_Client_ID
   *          Client
   * @param aD_Org_ID
   *          Org
   * @param M_InOutLine_ID
   *          Receipt
   * @param C_InvoiceLine_ID
   *          Invoice
   * @param m_Product_ID
   *          Product
   * @param DateTrx
   *          Date
   * @param qty
   *          Qty
   * @return true if record created
   */
  private int createMatchInv(String aD_Client_ID, String aD_Org_ID, String M_InOutLine_ID,
      String C_InvoiceLine_ID, String m_Product_ID, String DateTrx, String qty,
      VariablesSecureApp vars, ConnectionProvider conn, Connection con) {
    int no = 0;
    try {
      String M_MatchInv_ID = SequenceIdData.getUUID();
      //
      no = AcctServerData.insertMatchInv(con, conn, M_MatchInv_ID, aD_Client_ID, aD_Org_ID,
          M_InOutLine_ID, C_InvoiceLine_ID, m_Product_ID, DateTrx, qty);
    } catch (ServletException e) {
      log4j.warn(e);
      e.printStackTrace();
    }
    return no;
  } // createMatchInv

  /**
   * Get the account for Accounting Schema
   * 
   * @param AcctType
   *          see ACCTTYPE_*
   * @param as
   *          accounting schema
   * @return Account
   */
  public final Account getAccount(String AcctType, AcctSchema as, ConnectionProvider conn) {
    BigDecimal AMT = null;
    AcctServerData[] data = null;
    try {
      /** Account Type - Invoice */
      if (AcctType.equals(ACCTTYPE_Charge)) { // see getChargeAccount in
        // DocLine
        AMT = new BigDecimal(getAmount(AMTTYPE_Charge));
        int cmp = AMT.compareTo(BigDecimal.ZERO);
        if (cmp == 0) {
          return null;
        } else if (cmp < 0) {
          data = AcctServerData.selectExpenseAcct(conn, C_Charge_ID, as.getC_AcctSchema_ID());
        } else {
          data = AcctServerData.selectRevenueAcct(conn, C_Charge_ID, as.getC_AcctSchema_ID());
        }
      } else if (AcctType.equals(ACCTTYPE_V_Liability)) {
        data = AcctServerData.selectLiabilityAcct(conn, C_BPartner_ID, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_V_Liability_Services)) {
        data = AcctServerData.selectLiabilityServicesAcct(conn, C_BPartner_ID,
            as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_C_Receivable)) {
        data = AcctServerData.selectReceivableAcct(conn, C_BPartner_ID, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_UnallocatedCash)) {
        /** Account Type - Payment */
        data = AcctServerData.selectUnallocatedCashAcct(conn, C_BankAccount_ID,
            as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_BankInTransit)) {
        data = AcctServerData.selectInTransitAcct(conn, C_BankAccount_ID, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_BankInTransitDefault)) {
        data = AcctServerData.selectInTransitDefaultAcct(conn, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_ConvertChargeDefaultAmt)) {
        data = AcctServerData.selectConvertChargeDefaultAmtAcct(conn, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_ConvertGainDefaultAmt)) {
        data = AcctServerData.selectConvertGainDefaultAmtAcct(conn, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_PaymentSelect)) {
        data = AcctServerData.selectPaymentSelectAcct(conn, C_BankAccount_ID,
            as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_WriteOffDefault)) {
        data = AcctServerData.selectWriteOffDefault(conn, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_WriteOffDefault_Revenue)) {
        data = AcctServerData.selectWriteOffDefaultRevenue(conn, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_DiscountExp)) {
        /** Account Type - Allocation */
        data = AcctServerData.selectDiscountExpAcct(conn, C_BPartner_ID, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_DiscountRev)) {
        data = AcctServerData.selectDiscountRevAcct(conn, C_BPartner_ID, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_WriteOff)) {
        data = AcctServerData.selectWriteOffAcct(conn, C_BPartner_ID, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_WriteOff_Revenue)) {
        data = AcctServerData.selectWriteOffAcctRevenue(conn, C_BPartner_ID,
            as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_ConvertChargeLossAmt)) {
        /** Account Type - Bank Statement */
        data = AcctServerData.selectConvertChargeLossAmt(conn, FIN_Financial_Account_ID,
            as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_ConvertChargeGainAmt)) {
        data = AcctServerData.selectConvertChargeGainAmt(conn, FIN_Financial_Account_ID,
            as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_BankAsset)) {
        data = AcctServerData.selectAssetAcct(conn, C_BankAccount_ID, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_InterestRev)) {
        data = AcctServerData.selectInterestRevAcct(conn, C_BankAccount_ID,
            as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_InterestExp)) {
        data = AcctServerData.selectInterestExpAcct(conn, C_BankAccount_ID,
            as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_CashAsset)) {
        /** Account Type - Cash */
        data = AcctServerData.selectCBAssetAcct(conn, C_CashBook_ID, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_CashTransfer)) {
        data = AcctServerData.selectCashTransferAcct(conn, C_CashBook_ID, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_CashExpense)) {
        data = AcctServerData.selectCBExpenseAcct(conn, C_CashBook_ID, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_CashReceipt)) {
        data = AcctServerData.selectCBReceiptAcct(conn, C_CashBook_ID, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_CashDifference)) {
        data = AcctServerData.selectCBDifferencesAcct(conn, C_CashBook_ID, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_InvDifferences)) {
        /** Inventory Accounts */
        data = AcctServerData.selectWDifferencesAcct(conn, M_Warehouse_ID, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_NotInvoicedReceipts)) {
        if (log4j.isDebugEnabled()) {
          log4j.debug("AcctServer - getAccount - ACCTYPE_NotInvoicedReceipts - C_BPartner_ID - "
              + C_BPartner_ID);
        }
        data = AcctServerData.selectNotInvoicedReceiptsAcct(conn, C_BPartner_ID,
            as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_ProjectAsset)) {
        /** Project Accounts */
        data = AcctServerData.selectPJAssetAcct(conn, C_Project_ID, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_ProjectWIP)) {
        data = AcctServerData.selectPJWIPAcct(conn, C_Project_ID, as.getC_AcctSchema_ID());
      } else if (AcctType.equals(ACCTTYPE_PPVOffset)) {
        /** GL Accounts */
        data = AcctServerData.selectPPVOffsetAcct(conn, as.getC_AcctSchema_ID());
      } else {
        log4j.warn("AcctServer - getAccount - Not found AcctType=" + AcctType);
        return null;
      }
    } catch (ServletException e) {
      log4j.warn(e);
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
      log4j.warn("AcctServer - getAccount - NO account Type=" + AcctType + ", Record=" + Record_ID);
      return null;
    }
    // Return Account
    Account acct = null;
    try {
      acct = Account.getAccount(conn, Account_ID);
    } catch (ServletException e) {
      log4j.warn(e);
      e.printStackTrace();
    }
    return acct;
  } // getAccount

  /**
   * Get the account for Accounting Schema
   * 
   * @param cBPartnerId
   *          business partner id
   * @param as
   *          accounting schema
   * @return Account
   */
  public final Account getAccountBPartner(String cBPartnerId, AcctSchema as, boolean isReceipt,
      boolean isPrepayment, ConnectionProvider conn) throws ServletException {
    return getAccountBPartner(cBPartnerId, as, isReceipt, isPrepayment, false, conn);
  }

  /**
   * Get the account for Accounting Schema
   * 
   * @param cBPartnerId
   *          business partner id
   * @param as
   *          accounting schema
   * @return Account
   */
  public final Account getAccountBPartner(String cBPartnerId, AcctSchema as, boolean isReceipt,
      boolean isPrepayment, boolean isDoubtfuldebt, ConnectionProvider conn)
      throws ServletException {

    String strValidCombination = "";
    OBContext.setAdminMode();
    try {
      if (isReceipt) {
        String whereClause = "";
        if (isDoubtfuldebt) {
          BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class, cBPartnerId);
          //@formatter:off
          whereClause += " as cuscata "
              + " where cuscata.businessPartnerCategory.id = :bpCategoryID"
              + "  and cuscata.accountingSchema.id = :acctSchemaID";
          //@formatter:on
          final OBQuery<CategoryAccounts> obqParameters = OBDal.getInstance()
              .createQuery(CategoryAccounts.class, whereClause);
          obqParameters.setFilterOnReadableClients(false);
          obqParameters.setFilterOnReadableOrganization(false);
          obqParameters.setNamedParameter("bpCategoryID", bp.getBusinessPartnerCategory().getId());
          obqParameters.setNamedParameter("acctSchemaID", as.m_C_AcctSchema_ID);
          final List<CategoryAccounts> customerAccounts = obqParameters.list();
          if (customerAccounts != null && customerAccounts.size() > 0
              && customerAccounts.get(0).getDoubtfulDebtAccount() != null) {
            strValidCombination = customerAccounts.get(0).getDoubtfulDebtAccount().getId();
          }
          if (strValidCombination.equals("")) {
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("Account", "@DoubtfulDebt@");
            parameters.put("Entity", bp.getBusinessPartnerCategory().getIdentifier());
            parameters.put("AccountingSchema",
                OBDal.getInstance()
                    .get(org.openbravo.model.financialmgmt.accounting.coa.AcctSchema.class,
                        as.getC_AcctSchema_ID())
                    .getIdentifier());
            setMessageResult(conn, STATUS_InvalidAccount, "error", parameters);
            throw new IllegalStateException();
          }
          return new Account(conn, strValidCombination);
        }

        //@formatter:off
        whereClause +=" as cusa "
            + " where cusa.businessPartner.id = :bpartnerId"
            + " and cusa.accountingSchema.id = :accountId"
            + " and (cusa.status is null or cusa.status = 'DE')";
        //@formatter:on

        final OBQuery<CustomerAccounts> obqParameters = OBDal.getInstance()
            .createQuery(CustomerAccounts.class, whereClause);
        obqParameters.setNamedParameter("bpartnerId", cBPartnerId);
        obqParameters.setNamedParameter("accountId", as.m_C_AcctSchema_ID);
        obqParameters.setFilterOnReadableClients(false);
        obqParameters.setFilterOnReadableOrganization(false);
        final List<CustomerAccounts> customerAccounts = obqParameters.list();
        if (customerAccounts != null && customerAccounts.size() > 0
            && customerAccounts.get(0).getCustomerReceivablesNo() != null && !isPrepayment) {
          strValidCombination = customerAccounts.get(0).getCustomerReceivablesNo().getId();
        }
        if (customerAccounts != null && customerAccounts.size() > 0
            && customerAccounts.get(0).getCustomerPrepayment() != null && isPrepayment) {
          strValidCombination = customerAccounts.get(0).getCustomerPrepayment().getId();
        }
      } else {
        //@formatter:off
        final String whereClause = " as vena "
            + " where vena.businessPartner.id = :bpartnerId"
            + " and vena.accountingSchema.id = :accountId"
            + " and (vena.status is null or vena.status = 'DE')";
        //@formatter:on

        final OBQuery<VendorAccounts> obqParameters = OBDal.getInstance()
            .createQuery(VendorAccounts.class, whereClause);
        obqParameters.setNamedParameter("bpartnerId", cBPartnerId);
        obqParameters.setNamedParameter("accountId", as.m_C_AcctSchema_ID);
        obqParameters.setFilterOnReadableClients(false);
        obqParameters.setFilterOnReadableOrganization(false);
        final List<VendorAccounts> vendorAccounts = obqParameters.list();
        if (vendorAccounts != null && vendorAccounts.size() > 0
            && vendorAccounts.get(0).getVendorLiability() != null && !isPrepayment) {
          strValidCombination = vendorAccounts.get(0).getVendorLiability().getId();
        }
        if (vendorAccounts != null && vendorAccounts.size() > 0
            && vendorAccounts.get(0).getVendorPrepayment() != null && isPrepayment) {
          strValidCombination = vendorAccounts.get(0).getVendorPrepayment().getId();
        }
      }
      if (strValidCombination.equals("")) {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("Account",
            isReceipt ? (isPrepayment ? "@CustomerPrepayment@" : "@CustomerReceivables@")
                : (isPrepayment ? "@VendorPrepayment@" : "@VendorLiability@"));
        BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class, cBPartnerId);
        if (bp != null) {
          parameters.put("Entity", bp.getIdentifier());
        }
        parameters.put("AccountingSchema",
            OBDal.getInstance()
                .get(org.openbravo.model.financialmgmt.accounting.coa.AcctSchema.class,
                    as.getC_AcctSchema_ID())
                .getIdentifier());
        setMessageResult(conn, STATUS_InvalidAccount, "error", parameters);
        throw new IllegalStateException();
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return new Account(conn, strValidCombination);
  } // getAccount

  /**
   * It gets Account to be used to provision for the selected Business Partner
   * 
   * @param BPartnerId
   *          : ID of the Business Partner
   * @param isExpense
   *          : Provision Expense Account. If not it applies to Provision Applied account
   * @param as
   *          : Accounting Schema
   * @param conn
   *          : Connection Provider
   * @return Account
   * @throws ServletException
   */
  public final Account getAccountBPartnerBadDebt(String BPartnerId, boolean isExpense,
      AcctSchema as, ConnectionProvider conn) throws ServletException {

    String strValidCombination = "";
    BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class, BPartnerId);
    //@formatter:off
    final String whereClause = " as cuscata "
        + " where cuscata.businessPartnerCategory.id = :bpCategoryID"
        + "  and cuscata.accountingSchema.id = :acctSchemaID";
    //@formatter:on

    final OBQuery<CategoryAccounts> obqParameters = OBDal.getInstance()
        .createQuery(CategoryAccounts.class, whereClause);
    obqParameters.setFilterOnReadableClients(false);
    obqParameters.setFilterOnReadableOrganization(false);
    obqParameters.setNamedParameter("bpCategoryID", bp.getBusinessPartnerCategory().getId());
    obqParameters.setNamedParameter("acctSchemaID", as.m_C_AcctSchema_ID);
    final List<CategoryAccounts> customerAccounts = obqParameters.list();
    if (customerAccounts != null && customerAccounts.size() > 0
        && customerAccounts.get(0).getBadDebtExpenseAccount() != null && isExpense) {
      strValidCombination = customerAccounts.get(0).getBadDebtExpenseAccount().getId();
    } else if (customerAccounts != null && customerAccounts.size() > 0
        && customerAccounts.get(0).getBadDebtRevenueAccount() != null && !isExpense) {
      strValidCombination = customerAccounts.get(0).getBadDebtRevenueAccount().getId();
    }
    if (strValidCombination.equals("")) {
      Map<String, String> parameters = new HashMap<String, String>();
      if (isExpense) {
        parameters.put("Account", "@BadDebtExpenseAccount@");
      } else {
        parameters.put("Account", "@BadDebtRevenueAccount@");
      }
      parameters.put("Entity", bp.getBusinessPartnerCategory().getIdentifier());
      parameters.put("AccountingSchema",
          OBDal.getInstance()
              .get(org.openbravo.model.financialmgmt.accounting.coa.AcctSchema.class,
                  as.getC_AcctSchema_ID())
              .getIdentifier());
      setMessageResult(conn, STATUS_InvalidAccount, "error", parameters);
      throw new IllegalStateException();
    }
    return new Account(conn, strValidCombination);
  } // getAccountBPartnerBadDebt

  /**
   * It gets Account to be used to provision for the selected Business Partner
   * 
   * @param BPartnerId
   *          : ID of the Business Partner
   * @param as
   *          : Accounting Schema
   * @param conn
   *          : Connection Provider
   * @return Account
   * @throws ServletException
   */
  public final Account getAccountBPartnerAllowanceForDoubtfulDebt(String BPartnerId, AcctSchema as,
      ConnectionProvider conn) throws ServletException {

    String strValidCombination = "";
    BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class, BPartnerId);
    //@formatter:off
    String whereClause = " as cuscata "
        + " where cuscata.businessPartnerCategory.id = :bpCategoryID"
        + "   and cuscata.accountingSchema.id = :acctSchemaID";
    //@formatter:on

    final OBQuery<CategoryAccounts> obqParameters = OBDal.getInstance()
        .createQuery(CategoryAccounts.class, whereClause);
    obqParameters.setFilterOnReadableClients(false);
    obqParameters.setFilterOnReadableOrganization(false);
    obqParameters.setNamedParameter("bpCategoryID", bp.getBusinessPartnerCategory().getId());
    obqParameters.setNamedParameter("acctSchemaID", as.m_C_AcctSchema_ID);
    final List<CategoryAccounts> customerAccounts = obqParameters.list();
    if (customerAccounts != null && customerAccounts.size() > 0
        && customerAccounts.get(0).getAllowanceForDoubtfulDebtAccount() != null) {
      strValidCombination = customerAccounts.get(0).getAllowanceForDoubtfulDebtAccount().getId();
    }
    if (strValidCombination.equals("")) {
      Map<String, String> parameters = new HashMap<String, String>();
      parameters.put("Account", "@AllowanceForDoubtfulDebtAccount@");
      parameters.put("Entity", bp.getBusinessPartnerCategory().getIdentifier());
      parameters.put("AccountingSchema",
          OBDal.getInstance()
              .get(org.openbravo.model.financialmgmt.accounting.coa.AcctSchema.class,
                  as.getC_AcctSchema_ID())
              .getIdentifier());
      setMessageResult(conn, STATUS_InvalidAccount, "error", parameters);
      throw new IllegalStateException();
    }
    return new Account(conn, strValidCombination);
  }

  /**
   * Get the account for GL Item
   */
  public Account getAccountGLItem(GLItem glItem, AcctSchema as, boolean bIsReceipt,
      ConnectionProvider conn) throws ServletException {
    OBContext.setAdminMode();
    Account account = null;
    try {
      OBCriteria<GLItemAccounts> accounts = OBDal.getInstance()
          .createCriteria(GLItemAccounts.class);
      accounts.add(Restrictions.eq(GLItemAccounts.PROPERTY_GLITEM, glItem));
      accounts.add(Restrictions.eq(GLItemAccounts.PROPERTY_ACCOUNTINGSCHEMA,
          OBDal.getInstance()
              .get(org.openbravo.model.financialmgmt.accounting.coa.AcctSchema.class,
                  as.m_C_AcctSchema_ID)));
      accounts.add(Restrictions.eq(GLItemAccounts.PROPERTY_ACTIVE, true));
      accounts.setFilterOnReadableClients(false);
      accounts.setFilterOnReadableOrganization(false);
      List<GLItemAccounts> accountList = accounts.list();
      if (accountList == null || accountList.size() == 0) {
        return null;
      }
      if (bIsReceipt) {
        account = new Account(conn, accountList.get(0).getGlitemCreditAcct().getId());
      } else {
        account = new Account(conn, accountList.get(0).getGlitemDebitAcct().getId());
      }
    } finally {
      OBContext.restorePreviousMode();
      if (account == null) {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("Account", bIsReceipt ? "@GlitemCreditAccount@" : "@GlitemDebitAccount@");
        if (glItem != null) {
          parameters.put("Entity", glItem.getIdentifier());
        }
        parameters.put("AccountingSchema",
            OBDal.getInstance()
                .get(org.openbravo.model.financialmgmt.accounting.coa.AcctSchema.class,
                    as.getC_AcctSchema_ID())
                .getIdentifier());
        setMessageResult(conn, STATUS_InvalidAccount, "error", parameters);
        throw new IllegalStateException();
      }
    }
    return account;
  }

  public Account getAccountFee(AcctSchema as, FIN_FinancialAccount finAccount,
      ConnectionProvider conn) throws ServletException {
    Account account = null;
    OBContext.setAdminMode();
    try {
      OBCriteria<FIN_FinancialAccountAccounting> accounts = OBDal.getInstance()
          .createCriteria(FIN_FinancialAccountAccounting.class);
      accounts.add(Restrictions.eq(FIN_FinancialAccountAccounting.PROPERTY_ACCOUNT, finAccount));
      accounts.add(Restrictions.eq(FIN_FinancialAccountAccounting.PROPERTY_ACCOUNTINGSCHEMA,
          OBDal.getInstance()
              .get(org.openbravo.model.financialmgmt.accounting.coa.AcctSchema.class,
                  as.m_C_AcctSchema_ID)));
      accounts.add(Restrictions.eq(FIN_FinancialAccountAccounting.PROPERTY_ACTIVE, true));
      accounts.setFilterOnReadableClients(false);
      accounts.setFilterOnReadableOrganization(false);
      List<FIN_FinancialAccountAccounting> accountList = accounts.list();
      if (accountList == null || accountList.size() == 0) {
        return account;
      }
      account = new Account(conn, accountList.get(0).getFINBankfeeAcct().getId());
    } finally {
      OBContext.restorePreviousMode();
      if (account == null) {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("Account", "@BankfeeAccount@");
        if (finAccount != null) {
          parameters.put("Entity", finAccount.getIdentifier());
        }
        parameters.put("AccountingSchema",
            OBDal.getInstance()
                .get(org.openbravo.model.financialmgmt.accounting.coa.AcctSchema.class,
                    as.getC_AcctSchema_ID())
                .getIdentifier());
        setMessageResult(conn, STATUS_InvalidAccount, "error", parameters);
        throw new IllegalStateException();
      }
    }
    return account;
  }

  /**
   * Get the account for Financial Account (Uses: INT - In Transit DEP - Deposit CLE - Clearing WIT
   * - Withdraw)
   */
  public Account getAccount(ConnectionProvider conn, String use,
      FIN_FinancialAccountAccounting financialAccountAccounting, boolean bIsReceipt)
      throws ServletException {
    OBContext.setAdminMode();
    Account account = null;
    String strvalidCombination = "";
    try {
      if ("INT".equals(use)) {
        strvalidCombination = bIsReceipt
            ? financialAccountAccounting.getInTransitPaymentAccountIN().getId()
            : financialAccountAccounting.getFINOutIntransitAcct().getId();
      } else if ("DEP".equals(use)) {
        strvalidCombination = financialAccountAccounting.getDepositAccount().getId();
      } else if ("CLE".equals(use)) {
        strvalidCombination = bIsReceipt
            ? financialAccountAccounting.getClearedPaymentAccount().getId()
            : financialAccountAccounting.getClearedPaymentAccountOUT().getId();
      } else if ("WIT".equals(use)) {
        strvalidCombination = financialAccountAccounting.getWithdrawalAccount().getId();
      } else {
        return account;
      }
      account = new Account(conn, strvalidCombination);
    } finally {
      OBContext.restorePreviousMode();
      if (account == null) {
        Map<String, String> parameters = new HashMap<String, String>();
        String strAccount = bIsReceipt
            ? ("INT".equals(use) ? "@InTransitPaymentAccountIN@"
                : ("DEP".equals(use) ? "@DepositAccount@" : "@ClearedPaymentAccount@"))
            : ("INT".equals(use) ? "@InTransitPaymentAccountOUT@"
                : ("CLE".equals(use) ? "@ClearedPaymentAccountOUT@" : "@WithdrawalAccount@"));
        parameters.put("Account", strAccount);
        if (financialAccountAccounting.getAccount() != null) {
          parameters.put("Entity", financialAccountAccounting.getAccount().getIdentifier());
        }
        parameters.put("AccountingSchema",
            financialAccountAccounting.getAccountingSchema().getIdentifier());
        setMessageResult(conn, STATUS_InvalidAccount, "error", parameters);
        throw new IllegalStateException();
      }
    }
    return account;
  }

  public FieldProvider[] getObjectFieldProvider() {
    return objectFieldProvider;
  }

  public void setObjectFieldProvider(FieldProvider[] fieldProvider) {
    objectFieldProvider = fieldProvider;
  }

  public abstract void loadObjectFieldProvider(ConnectionProvider conn, String aD_Client_ID,
      String Id) throws ServletException;

  public abstract boolean loadDocumentDetails(FieldProvider[] data, ConnectionProvider conn);

  /**
   * Get Source Currency Balance - subtracts line (and tax) amounts from total - no rounding
   * 
   * @return positive amount, if total header is bigger than lines
   */
  public abstract BigDecimal getBalance();

  /**
   * Create Facts (the accounting logic)
   * 
   * @param as
   *          accounting schema
   * @return Fact
   */
  public abstract Fact createFact(AcctSchema as, ConnectionProvider conn, Connection con,
      VariablesSecureApp vars) throws ServletException;

  public abstract boolean getDocumentConfirmation(ConnectionProvider conn, String strRecordId);

  public String getInfo(VariablesSecureApp vars) {
    return (Utility.messageBD(connectionProvider, "Created", vars.getLanguage()) + "=" + success
    );
  } // end of getInfo() method

  /**
   * @param language
   * @return a String representing the result of created
   */
  public String getInfo(String language) {
    return (Utility.messageBD(connectionProvider, "Created", language) + "=" + success);
  }

  public boolean checkDocuments() throws ServletException {
    return checkDocuments(null, null);
  }

  public boolean checkDocuments(String dateFrom, String dateTo) throws ServletException {
    if (m_as.length == 0) {
      return false;
    }
    AcctServerData[] docTypes = AcctServerData.selectDocTypes(connectionProvider, AD_Table_ID,
        AD_Client_ID);
    final Set<String> orgSet = OBContext.getOBContext()
        .getOrganizationStructureProvider(AD_Client_ID)
        .getChildTree(AD_Org_ID, true);
    String strorgs = Utility.getInStrSet(orgSet);

    for (int i = 0; i < docTypes.length; i++) {
      long init = System.currentTimeMillis();
      AcctServerData data = AcctServerData.selectDocumentsDates(connectionProvider, strDateColumn,
          tableName, AD_Client_ID, strorgs, docTypes[i].name, dateFrom, dateTo);
      log4j.debug("AcctServerData.selectDocumentsDates for: " + docTypes[i].name + " took: "
          + (System.currentTimeMillis() - init));
      if (data != null) {
        if (data.id != null && !data.id.equals("")) {
          return true;
        }
      }
    }
    return false;
  } // end of checkDocuments() method

  @Deprecated
  /*
   * Use checkDocuments method instead
   */
  public boolean filterDatesCheckDocuments(String dateFrom, String dateTo) throws ServletException {
    return checkDocuments(dateFrom, dateTo);
  } // end of filterDatesCheckDocuments() method

  public void setMessageResult(OBError error) {
    messageResult = error;
  }

  /*
   * Sets OBError message for the given status
   */
  public void setMessageResult(ConnectionProvider conn, VariablesSecureApp vars, String strStatus,
      String strMessageType) {
    setMessageResult(conn, strStatus, strMessageType, null);
  }

  /*
   * Sets OBError message for the given status
   */
  public void setMessageResult(ConnectionProvider conn, String _strStatus, String strMessageType,
      Map<String, String> _parameters) {
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars;

    if (request != null) {
      // getting context info from session
      vars = new VariablesSecureApp(RequestContext.get().getRequest());
    } else {
      // there is no session, getting context info from OBContext
      OBContext ctx = OBContext.getOBContext();
      vars = new VariablesSecureApp(ctx.getUser().getId(), ctx.getCurrentClient().getId(),
          ctx.getCurrentOrganization().getId(), ctx.getRole().getId(),
          ctx.getLanguage().getLanguage());
    }
    setMessageResult(conn, vars, _strStatus, strMessageType, _parameters);
  }

  /*
   * Sets OBError message for the given status
   */
  public void setMessageResult(ConnectionProvider conn, VariablesSecureApp vars, String _strStatus,
      String strMessageType, Map<String, String> _parameters) {
    String strStatus = StringUtils.isEmpty(_strStatus) ? getStatus() : _strStatus;
    setStatus(strStatus);
    String strTitle = "";
    Map<String, String> parameters = _parameters != null ? _parameters
        : new HashMap<String, String>();
    if (messageResult == null) {
      messageResult = new OBError();
    }
    if (strMessageType == null || strMessageType.equals("")) {
      messageResult.setType("Error");
    } else {
      messageResult.setType(strMessageType);
    }
    if (strStatus.equals(STATUS_Error)) {
      strTitle = "@ProcessRunError@";
    } else if (strStatus.equals(STATUS_DocumentLocked)) {
      strTitle = "@OtherPostingProcessActive@";
      messageResult.setType("Warning");
    } else if (strStatus.equals(STATUS_NotCalculatedCost)) {
      if (parameters.isEmpty()) {
        strTitle = "@NotCalculatedCost@";
      } else {
        strTitle = "@NotCalculatedCostWithTransaction@";
      }
    } else if (strStatus.equals(STATUS_InvalidCost)) {
      if (parameters.isEmpty()) {
        strTitle = "@InvalidCost@";
      } else {
        strTitle = "@InvalidCostWhichProduct@";
        // Transalate account name from messages
        parameters.put("Account",
            Utility.parseTranslation(conn, vars, vars.getLanguage(), parameters.get("Account")));
      }
    } else if (strStatus.equals(STATUS_NoRelatedPO)) {
      if (parameters.isEmpty()) {
        strTitle = "@GoodsReceiptTransactionWithNoPO@";
      } else {
        strTitle = "@GoodsReceiptTransactionWithNoPOWichProduct@";
      }
    } else if (strStatus.equals(STATUS_DocumentDisabled)) {
      strTitle = "@DocumentDisabled@";
      messageResult.setType("Warning");
    } else if (strStatus.equals(STATUS_BackgroundDisabled)) {
      strTitle = "@BackgroundDisabled@";
      messageResult.setType("Warning");
    } else if (strStatus.equals(STATUS_InvalidAccount)) {
      if (parameters.isEmpty()) {
        strTitle = "@InvalidAccount@";
      } else {
        strTitle = "@InvalidWhichAccount@";
        // Transalate account name from messages
        parameters.put("Account",
            Utility.parseTranslation(conn, vars, vars.getLanguage(), parameters.get("Account")));
      }
    } else if (strStatus.equals(STATUS_PeriodClosed)) {
      strTitle = "@PeriodNotAvailable@";
    } else if (strStatus.equals(STATUS_NotConvertible)) {
      strTitle = "@NotConvertible@";
    } else if (strStatus.equals(STATUS_NotBalanced)) {
      strTitle = "@NotBalanced@";
    } else if (strStatus.equals(STATUS_NotPosted)) {
      strTitle = "@NotPosted@";
    } else if (strStatus.equals(STATUS_PostPrepared)) {
      strTitle = "@PostPrepared@";
    } else if (strStatus.equals(STATUS_Posted)) {
      strTitle = "@Posted@";
    } else if (strStatus.equals(STATUS_TableDisabled)) {
      strTitle = "@TableDisabled@";
      parameters.put("Table", tableName);
      messageResult.setType("Warning");
    } else if (strStatus.equals(STATUS_NoAccountingDate)) {
      strTitle = "@NoAccountingDate@";
    }
    messageResult.setMessage(Utility.parseTranslation(conn, vars, parameters, vars.getLanguage(),
        Utility.parseTranslation(conn, vars, vars.getLanguage(), strTitle)));
    if (strMessage != null) {
      messageResult.setMessage(Utility.parseTranslation(conn, vars, parameters, vars.getLanguage(),
          Utility.parseTranslation(conn, vars, vars.getLanguage(), strMessage)));
    }
  }

  public Map<String, String> getInvalidAccountParameters(String strAccount, String strEntity,
      String strAccountingSchema) {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("Account", strAccount);
    parameters.put("Entity", strEntity);
    parameters.put("AccountingSchema", strAccountingSchema);
    return parameters;
  }

  public Map<String, String> getInvalidCostParameters(String strProduct, String strDate) {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("Product", strProduct);
    parameters.put("Date", strDate);
    return parameters;
  }

  public Map<String, String> getNotCalculatedCostParameters(MaterialTransaction trx) {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("trx", trx.getIdentifier());
    parameters.put("product", trx.getProduct().getIdentifier());
    return parameters;
  }

  public OBError getMessageResult() {
    return messageResult;
  }

  public String getServletInfo() {
    return "Servlet for the accounting";
  } // end of getServletInfo() method

  public String getStatus() {
    return Status;
  }

  public void setStatus(String strStatus) {
    Status = strStatus;
  }

  public ConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  public ConversionRateDoc getConversionRateDoc(String table_ID, String record_ID,
      String curFrom_ID, String curTo_ID) {
    OBCriteria<ConversionRateDoc> docRateCriteria = OBDal.getInstance()
        .createCriteria(ConversionRateDoc.class);
    docRateCriteria.setFilterOnReadableClients(false);
    docRateCriteria.setFilterOnReadableOrganization(false);
    docRateCriteria.add(Restrictions.eq(ConversionRateDoc.PROPERTY_TOCURRENCY,
        OBDal.getInstance().get(Currency.class, curTo_ID)));
    docRateCriteria.add(Restrictions.eq(ConversionRateDoc.PROPERTY_CURRENCY,
        OBDal.getInstance().get(Currency.class, curFrom_ID)));
    if (record_ID != null) {
      if (table_ID.equals(TABLEID_Invoice)) {
        docRateCriteria.add(Restrictions.eq(ConversionRateDoc.PROPERTY_INVOICE,
            OBDal.getInstance().get(Invoice.class, record_ID)));
      } else if (table_ID.equals(TABLEID_Payment)) {
        docRateCriteria.add(Restrictions.eq(ConversionRateDoc.PROPERTY_PAYMENT,
            OBDal.getInstance().get(FIN_Payment.class, record_ID)));
      } else if (table_ID.equals(TABLEID_Transaction)) {
        docRateCriteria.add(Restrictions.eq(ConversionRateDoc.PROPERTY_FINANCIALACCOUNTTRANSACTION,
            OBDal.getInstance().get(FIN_FinaccTransaction.class, record_ID)));
      } else if (table_ID.equals(TABLEID_GLJournal)) {
        docRateCriteria.add(Restrictions.eq(ConversionRateDoc.PROPERTY_JOURNALENTRY,
            OBDal.getInstance().get(GLJournal.class, record_ID)));
      } else {
        return null;
      }
    } else {
      return null;
    }
    List<ConversionRateDoc> conversionRates = docRateCriteria.list();
    if (!conversionRates.isEmpty()) {
      return conversionRates.get(0);
    }
    return null;
  }

  public BigDecimal convertAmount(BigDecimal _amount, boolean isReceipt, String acctDate,
      String table_ID, String record_ID, String currencyIDFrom, String currencyIDTo, DocLine line,
      AcctSchema as, Fact fact, String Fact_Acct_Group_ID, String seqNo, ConnectionProvider conn)
      throws ServletException {
    return convertAmount(_amount, isReceipt, acctDate, table_ID, record_ID, currencyIDFrom,
        currencyIDTo, line, as, fact, Fact_Acct_Group_ID, seqNo, conn, true);
  }

  /*
   * Returns an amount without applying currency precision for rounding purposes
   */
  public BigDecimal convertAmount(BigDecimal _amount, boolean isReceipt, String acctDate,
      String table_ID, String record_ID, String currencyIDFrom, String currencyIDTo, DocLine line,
      AcctSchema as, Fact fact, String Fact_Acct_Group_ID, String seqNo, ConnectionProvider conn,
      boolean bookDifferences) throws ServletException {
    BigDecimal amtDiff = BigDecimal.ZERO;
    if (_amount == null || _amount.compareTo(BigDecimal.ZERO) == 0) {
      return _amount;
    }
    String conversionDate = acctDate;
    String strDateFormat = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("dateFormat.java");
    final SimpleDateFormat dateFormat = new SimpleDateFormat(strDateFormat);
    ConversionRateDoc conversionRateDoc = getConversionRateDoc(table_ID, record_ID, currencyIDFrom,
        currencyIDTo);
    BigDecimal amtFrom = BigDecimal.ZERO;
    BigDecimal amtFromSourcecurrency = BigDecimal.ZERO;
    BigDecimal amtTo = BigDecimal.ZERO;
    if (table_ID.equals(TABLEID_Invoice)) {
      Invoice invoice = OBDal.getInstance().get(Invoice.class, record_ID);
      conversionDate = dateFormat.format(invoice.getAccountingDate());
    } else if (table_ID.equals(TABLEID_Payment)) {
      FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, record_ID);
      conversionDate = dateFormat.format(payment.getPaymentDate());
    } else if (table_ID.equals(TABLEID_Transaction)) {
      FIN_FinaccTransaction transaction = OBDal.getInstance()
          .get(FIN_FinaccTransaction.class, record_ID);
      conversionDate = dateFormat.format(transaction.getDateAcct());
    }
    if (conversionRateDoc != null && record_ID != null) {
      amtFrom = applyRate(_amount, conversionRateDoc, true);
    } else {
      // I try to find a reversal rate for the doc, if exists i apply it reversal as well
      conversionRateDoc = getConversionRateDoc(table_ID, record_ID, currencyIDTo, currencyIDFrom);
      if (conversionRateDoc != null) {
        amtFrom = applyRate(_amount, conversionRateDoc, false);
      } else {
        String convertedAmt = getConvertedAmt(_amount.toString(), currencyIDFrom, currencyIDTo,
            conversionDate, "", AD_Client_ID, AD_Org_ID, conn);
        if (convertedAmt != null && !"".equals(convertedAmt)) {
          amtFrom = new BigDecimal(convertedAmt);
        } else {
          throw new OBException("@NotConvertible@");
        }
      }
    }
    ConversionRateDoc conversionRateCurrentDoc = getConversionRateDoc(AD_Table_ID, Record_ID,
        currencyIDFrom, currencyIDTo);
    if (AD_Table_ID.equals(TABLEID_Invoice)) {
      Invoice invoice = OBDal.getInstance().get(Invoice.class, Record_ID);
      conversionDate = dateFormat.format(invoice.getAccountingDate());
    } else if (AD_Table_ID.equals(TABLEID_Payment)) {
      FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, Record_ID);
      conversionDate = dateFormat.format(payment.getPaymentDate());
    } else if (AD_Table_ID.equals(TABLEID_Transaction)
        || AD_Table_ID.equals(TABLEID_Reconciliation)) {
      String transactionID = Record_ID;
      // When TableID= Reconciliation info is loaded from transaction
      if (AD_Table_ID.equals(AcctServer.TABLEID_Reconciliation)
          && line instanceof DocLine_FINReconciliation) {
        transactionID = ((DocLine_FINReconciliation) line).getFinFinAccTransactionId();
      }
      FIN_FinaccTransaction transaction = OBDal.getInstance()
          .get(FIN_FinaccTransaction.class, transactionID);
      conversionDate = dateFormat.format(transaction.getDateAcct());
      conversionRateCurrentDoc = getConversionRateDoc(TABLEID_Transaction, transaction.getId(),
          currencyIDFrom, currencyIDTo);
    } else {
      conversionDate = acctDate;
    }
    if (conversionRateCurrentDoc != null) {
      amtTo = applyRate(_amount, conversionRateCurrentDoc, true);
      amtFromSourcecurrency = applyRate(amtFrom, conversionRateCurrentDoc, false);
    } else {
      // I try to find a reversal rate for the doc, if exists i apply it reversal as well
      if (AD_Table_ID.equals(AcctServer.TABLEID_Reconciliation)
          && line instanceof DocLine_FINReconciliation) {
        String transactionID = ((DocLine_FINReconciliation) line).getFinFinAccTransactionId();
        conversionRateCurrentDoc = getConversionRateDoc(TABLEID_Transaction, transactionID,
            currencyIDTo, currencyIDFrom);
      } else {
        conversionRateCurrentDoc = getConversionRateDoc(AD_Table_ID, Record_ID, currencyIDTo,
            currencyIDFrom);
      }
      if (conversionRateCurrentDoc != null) {
        amtTo = applyRate(_amount, conversionRateCurrentDoc, false);
        amtFromSourcecurrency = applyRate(amtFrom, conversionRateCurrentDoc, true);
      } else {
        String convertedAmt = getConvertedAmt(_amount.toString(), currencyIDFrom, currencyIDTo,
            conversionDate, "", AD_Client_ID, AD_Org_ID, conn);
        if (convertedAmt != null && !"".equals(convertedAmt)) {
          amtTo = new BigDecimal(convertedAmt);
        } else {
          throw new OBException("@NotConvertible@");
        }
        if (amtTo.compareTo(BigDecimal.ZERO) != 0) {
          amtFromSourcecurrency = amtFrom.multiply(_amount)
              .divide(amtTo, conversionRatePrecision, RoundingMode.HALF_EVEN);
        } else {
          amtFromSourcecurrency = amtFrom;
        }
      }
    }
    amtDiff = (amtTo).subtract(amtFrom);
    // Add differences related to Different rates for accounting among currencies
    // _amount * ((TrxRate *
    // AccountingRateCurrencyFromCurrencyTo)-AccountingRateCurrencyDocCurrencyTo)
    amtDiff = amtDiff
        .add(calculateMultipleRatesDifferences(_amount, currencyIDFrom, currencyIDTo, line, conn));
    Currency currencyTo = OBDal.getInstance().get(Currency.class, currencyIDTo);
    amtDiff = amtDiff.setScale(currencyTo.getStandardPrecision().intValue(),
        RoundingMode.HALF_EVEN);
    if (bookDifferences) {
      if ((!isReceipt && amtDiff.compareTo(BigDecimal.ZERO) == 1)
          || (isReceipt && amtDiff.compareTo(BigDecimal.ZERO) == -1)) {
        String convertAccount = StringUtils.isNotEmpty(FIN_Financial_Account_ID)
            && StringUtils.equals(OBDal.getInstance()
                .get(FIN_FinancialAccount.class, FIN_Financial_Account_ID)
                .getType(), "B") ? AcctServer.ACCTTYPE_ConvertChargeGainAmt
                    : AcctServer.ACCTTYPE_ConvertGainDefaultAmt;
        fact.createLine(line, getAccount(convertAccount, as, conn), currencyIDTo, "",
            amtDiff.abs().toString(), Fact_Acct_Group_ID, seqNo, DocumentType, conn);
      } else if (amtDiff.compareTo(BigDecimal.ZERO) != 0) {
        String convertAccount = StringUtils.isNotEmpty(FIN_Financial_Account_ID)
            && StringUtils.equals(OBDal.getInstance()
                .get(FIN_FinancialAccount.class, FIN_Financial_Account_ID)
                .getType(), "B") ? AcctServer.ACCTTYPE_ConvertChargeLossAmt
                    : AcctServer.ACCTTYPE_ConvertChargeDefaultAmt;
        fact.createLine(line, getAccount(convertAccount, as, conn), currencyIDTo,
            amtDiff.abs().toString(), "", Fact_Acct_Group_ID, seqNo, DocumentType, conn);
      } else {
        return amtFromSourcecurrency;
      }
    }
    if (log4j.isDebugEnabled()) {
      log4j.debug("Amt from: " + amtFrom + "[" + currencyIDFrom + "]" + " Amt to: " + amtTo + "["
          + currencyIDTo + "] - amtFromSourcecurrency: " + amtFromSourcecurrency);
    }
    // return value in original currency
    return amtFromSourcecurrency;
  }

  @Deprecated
  public static String getConvertedAmt(String Amt, String CurFrom_ID, String CurTo_ID,
      String ConvDate, String RateType, String client, String org, String recordId, String docType,
      ConnectionProvider conn) {
    String localRateType = RateType;
    String localConvDate = ConvDate;
    String amt = Amt;
    boolean useSystemConversionRate = true;
    if (log4j.isDebugEnabled()) {
      log4j.debug(
          "AcctServer - getConvertedAmount - starting method - Amt : " + amt + " - CurFrom_ID : "
              + CurFrom_ID + " - CurTo_ID : " + CurTo_ID + "- ConvDate: " + localConvDate
              + " - RateType:" + localRateType + " - client:" + client + "- org:" + org);
    }

    if (amt.equals("")) {
      throw new IllegalArgumentException(
          "AcctServer - getConvertedAmt - required parameter missing - Amt");
    }
    if ((CurFrom_ID.equals(CurTo_ID) && !docType.equals(EXCHANGE_DOCTYPE_Transaction))
        || amt.equals("0")) {
      return amt;
    }
    AcctServerData[] data = null;
    OBContext.setAdminMode();
    try {
      if (localConvDate != null && localConvDate.equals("")) {
        localConvDate = DateTimeData.today(conn);
      }
      // ConvDate IN DATE
      if (localRateType == null || localRateType.equals("")) {
        localRateType = "S";
      }
      data = AcctServerData.currencyConvert(conn, amt, CurFrom_ID, CurTo_ID, localConvDate,
          localRateType, client, org);
      // Search if exists any conversion rate at document level

      OBCriteria<ConversionRateDoc> docRateCriteria = OBDal.getInstance()
          .createCriteria(ConversionRateDoc.class);
      if (docType.equals(EXCHANGE_DOCTYPE_Invoice) && recordId != null) {
        docRateCriteria.add(Restrictions.eq(ConversionRateDoc.PROPERTY_TOCURRENCY,
            OBDal.getInstance().get(Currency.class, CurTo_ID)));
        docRateCriteria.add(Restrictions.eq(ConversionRateDoc.PROPERTY_CURRENCY,
            OBDal.getInstance().get(Currency.class, CurFrom_ID)));
        // get reversed invoice id if exist.
        OBCriteria<ReversedInvoice> reversedCriteria = OBDal.getInstance()
            .createCriteria(ReversedInvoice.class);
        reversedCriteria.add(Restrictions.eq(ReversedInvoice.PROPERTY_INVOICE,
            OBDal.getInstance().get(Invoice.class, recordId)));
        if (!reversedCriteria.list().isEmpty()) {
          String strDateFormat;
          strDateFormat = OBPropertiesProvider.getInstance()
              .getOpenbravoProperties()
              .getProperty("dateFormat.java");
          final SimpleDateFormat dateFormat = new SimpleDateFormat(strDateFormat);
          localConvDate = dateFormat
              .format(reversedCriteria.list().get(0).getReversedInvoice().getAccountingDate());
          data = AcctServerData.currencyConvert(conn, amt, CurFrom_ID, CurTo_ID, localConvDate,
              localRateType, client, org);
          docRateCriteria.add(Restrictions.eq(ConversionRateDoc.PROPERTY_INVOICE,
              OBDal.getInstance()
                  .get(Invoice.class,
                      reversedCriteria.list().get(0).getReversedInvoice().getId())));
        } else {
          docRateCriteria.add(Restrictions.eq(ConversionRateDoc.PROPERTY_INVOICE,
              OBDal.getInstance().get(Invoice.class, recordId)));
        }
        useSystemConversionRate = false;
      } else if (docType.equals(EXCHANGE_DOCTYPE_Payment)) {
        docRateCriteria.add(Restrictions.eq(ConversionRateDoc.PROPERTY_TOCURRENCY,
            OBDal.getInstance().get(Currency.class, CurTo_ID)));
        docRateCriteria.add(Restrictions.eq(ConversionRateDoc.PROPERTY_CURRENCY,
            OBDal.getInstance().get(Currency.class, CurFrom_ID)));
        docRateCriteria.add(Restrictions.eq(ConversionRateDoc.PROPERTY_PAYMENT,
            OBDal.getInstance().get(FIN_Payment.class, recordId)));
        useSystemConversionRate = false;
      } else if (docType.equals(EXCHANGE_DOCTYPE_Transaction)) {
        APRM_FinaccTransactionV a = OBDal.getInstance()
            .get(APRM_FinaccTransactionV.class, recordId);
        if (a.getForeignCurrency() != null) {
          amt = a.getForeignAmount().toString();
          data = AcctServerData.currencyConvert(conn, amt, a.getForeignCurrency().getId(), CurTo_ID,
              localConvDate, localRateType, client, org);
          docRateCriteria.add(Restrictions.eq(ConversionRateDoc.PROPERTY_TOCURRENCY,
              OBDal.getInstance().get(Currency.class, CurTo_ID)));
          docRateCriteria.add(Restrictions.eq(ConversionRateDoc.PROPERTY_CURRENCY,
              OBDal.getInstance().get(Currency.class, a.getForeignCurrency().getId())));
        } else {
          docRateCriteria.add(Restrictions.eq(ConversionRateDoc.PROPERTY_TOCURRENCY,
              OBDal.getInstance().get(Currency.class, CurTo_ID)));
          docRateCriteria.add(Restrictions.eq(ConversionRateDoc.PROPERTY_CURRENCY,
              OBDal.getInstance().get(Currency.class, CurFrom_ID)));
        }
        docRateCriteria.add(Restrictions.eq(ConversionRateDoc.PROPERTY_FINANCIALACCOUNTTRANSACTION,
            OBDal.getInstance().get(APRM_FinaccTransactionV.class, recordId)));
        useSystemConversionRate = false;
      }
      if (docType.equals(EXCHANGE_DOCTYPE_Invoice) || docType.equals(EXCHANGE_DOCTYPE_Payment)
          || docType.equals(EXCHANGE_DOCTYPE_Transaction)) {
        List<ConversionRateDoc> conversionRates = docRateCriteria.list();
        if (!conversionRates.isEmpty() && !useSystemConversionRate) {
          BigDecimal Amount = new BigDecimal(amt);
          BigDecimal AmountConverted = Amount.multiply(conversionRates.get(0).getRate())
              .setScale(2, RoundingMode.HALF_UP);
          return AmountConverted.toString();
        }
      }
    } catch (ServletException e) {
      log4j.warn(e);
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
    if (data == null || data.length == 0) {
      return "";
    } else {
      if (log4j.isDebugEnabled()) {
        log4j.debug("getConvertedAmount - converted:" + data[0].converted);
      }
      return data[0].converted;
    }
  } // getConvertedAmt

  private BigDecimal calculateMultipleRatesDifferences(BigDecimal _amount, String currencyIDFrom,
      String currencyIDTo, DocLine line, ConnectionProvider conn) {
    // _amount * ((TrxRate *
    // AccountingRateCurrencyFromCurrencyTo)-AccountingRateCurrencyDocCurrencyTo)
    String conversionDate = DateAcct;
    String strDateFormat = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("dateFormat.java");
    final SimpleDateFormat dateFormat = new SimpleDateFormat(strDateFormat);
    // Calculate accountingRateCurrencyFromCurrencyTo
    BigDecimal accountingRateCurrencyFromCurrencyTo = BigDecimal.ONE;
    if (!currencyIDFrom.equals(currencyIDTo)) {
      ConversionRateDoc conversionRateCurrentDoc = getConversionRateDoc(AD_Table_ID, Record_ID,
          currencyIDFrom, currencyIDTo);
      if (AD_Table_ID.equals(AcctServer.TABLEID_Reconciliation)
          && line instanceof DocLine_FINReconciliation) {
        String transactionID = ((DocLine_FINReconciliation) line).getFinFinAccTransactionId();
        FIN_FinaccTransaction transaction = OBDal.getInstance()
            .get(FIN_FinaccTransaction.class, transactionID);
        conversionDate = dateFormat.format(transaction.getDateAcct());
        conversionRateCurrentDoc = getConversionRateDoc(TABLEID_Transaction, transaction.getId(),
            currencyIDFrom, currencyIDTo);
      }
      if (conversionRateCurrentDoc != null) {
        accountingRateCurrencyFromCurrencyTo = conversionRateCurrentDoc.getRate();
      } else {
        // I try to find a reversal rate for the doc, if exists i apply it reversal as well
        if (AD_Table_ID.equals(AcctServer.TABLEID_Reconciliation)
            && line instanceof DocLine_FINReconciliation) {
          String transactionID = ((DocLine_FINReconciliation) line).getFinFinAccTransactionId();
          FIN_FinaccTransaction transaction = OBDal.getInstance()
              .get(FIN_FinaccTransaction.class, transactionID);
          conversionRateCurrentDoc = getConversionRateDoc(TABLEID_Transaction, transaction.getId(),
              currencyIDTo, currencyIDFrom);
        } else {
          conversionRateCurrentDoc = getConversionRateDoc(AD_Table_ID, Record_ID, currencyIDTo,
              currencyIDFrom);
        }
        if (conversionRateCurrentDoc != null) {
          accountingRateCurrencyFromCurrencyTo = BigDecimal.ONE
              .divide(conversionRateCurrentDoc.getRate(), MathContext.DECIMAL64);
        } else {
          accountingRateCurrencyFromCurrencyTo = getConvertionRate(currencyIDFrom, currencyIDTo,
              conversionDate, "", AD_Client_ID, AD_Org_ID, conn);
        }
      }
    }

    // Calculate accountingRateCurrencyFromCurrencyTo
    BigDecimal accountingRateCurrencyDocCurrencyTo = BigDecimal.ONE;
    if (!C_Currency_ID.equals(currencyIDTo)) {
      ConversionRateDoc conversionRateCurrentDoc = getConversionRateDoc(AD_Table_ID, Record_ID,
          C_Currency_ID, currencyIDTo);
      if (AD_Table_ID.equals(AcctServer.TABLEID_Reconciliation)
          && line instanceof DocLine_FINReconciliation) {
        String transactionID = ((DocLine_FINReconciliation) line).getFinFinAccTransactionId();
        FIN_FinaccTransaction transaction = OBDal.getInstance()
            .get(FIN_FinaccTransaction.class, transactionID);
        conversionDate = dateFormat.format(transaction.getTransactionDate());
        conversionRateCurrentDoc = getConversionRateDoc(TABLEID_Transaction, transaction.getId(),
            C_Currency_ID, currencyIDTo);
      }
      if (conversionRateCurrentDoc != null) {
        accountingRateCurrencyDocCurrencyTo = conversionRateCurrentDoc.getRate();
      } else {
        // I try to find a reversal rate for the doc, if exists i apply it reversal as well
        if (AD_Table_ID.equals(AcctServer.TABLEID_Reconciliation)
            && line instanceof DocLine_FINReconciliation) {
          String transactionID = ((DocLine_FINReconciliation) line).getFinFinAccTransactionId();
          FIN_FinaccTransaction transaction = OBDal.getInstance()
              .get(FIN_FinaccTransaction.class, transactionID);
          conversionRateCurrentDoc = getConversionRateDoc(TABLEID_Transaction, transaction.getId(),
              currencyIDTo, C_Currency_ID);
        } else {
          conversionRateCurrentDoc = getConversionRateDoc(AD_Table_ID, Record_ID, currencyIDTo,
              C_Currency_ID);
        }
        if (conversionRateCurrentDoc != null) {
          accountingRateCurrencyDocCurrencyTo = BigDecimal.ONE
              .divide(conversionRateCurrentDoc.getRate(), MathContext.DECIMAL64);
        } else {
          accountingRateCurrencyDocCurrencyTo = getConvertionRate(C_Currency_ID, currencyIDTo,
              conversionDate, "", AD_Client_ID, AD_Org_ID, conn);
        }
      }
    }
    // Calculate transaction rate
    BigDecimal trxRate = BigDecimal.ONE;
    if (!C_Currency_ID.equals(currencyIDFrom)) {
      if (AD_Table_ID.equals(TABLEID_Payment)) {
        FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, Record_ID);
        trxRate = payment.getFinancialTransactionConvertRate();
      } else if (AD_Table_ID.equals(TABLEID_Transaction)
          || AD_Table_ID.equals(TABLEID_Reconciliation)) {
        String transactionID = Record_ID;
        // When TableID = Reconciliation info is loaded from transaction
        if (AD_Table_ID.equals(AcctServer.TABLEID_Reconciliation)
            && line instanceof DocLine_FINReconciliation) {
          transactionID = ((DocLine_FINReconciliation) line).getFinFinAccTransactionId();
        }
        FIN_FinaccTransaction transaction = OBDal.getInstance()
            .get(FIN_FinaccTransaction.class, transactionID);
        trxRate = transaction.getForeignConversionRate();
      }
    }
    Currency currencyFrom = OBDal.getInstance().get(Currency.class, currencyIDTo);
    return _amount
        .multiply(trxRate.multiply(accountingRateCurrencyDocCurrencyTo)
            .subtract(accountingRateCurrencyFromCurrencyTo))
        .setScale(currencyFrom.getStandardPrecision().intValue(), RoundingMode.HALF_EVEN);
  }

  /*
   * Returns an amount without applying currency precision for rounding purposes
   */
  public static BigDecimal applyRate(BigDecimal _amount, ConversionRateDoc conversionRateDoc,
      boolean multiply) {
    BigDecimal amount = _amount;
    if (multiply) {
      return amount.multiply(conversionRateDoc.getRate());
    } else {
      return amount.divide(conversionRateDoc.getRate(), 12, RoundingMode.HALF_EVEN);
    }
  }

  public static int getConversionRatePrecision(VariablesSecureApp vars) {
    try {
      String formatOutput = vars.getSessionValue("#FormatOutput|generalQtyEdition", "#0.######");
      String decimalSeparator = ".";
      if (formatOutput.contains(decimalSeparator)) {
        formatOutput = formatOutput.substring(formatOutput.indexOf(decimalSeparator),
            formatOutput.length());
        return formatOutput.length() - decimalSeparator.length();
      } else {
        return 0;
      }
    } catch (Exception e) {
      log4j.error(e);
      return 6; // by default precision of 6 decimals as is defaulted in Format.xml
    }
  }

  /**
   * If there is any template active for current document in any accounting schema, it returns true
   * to skip this step as getDocumentConfirmation can lock template
   */
  boolean disableDocumentConfirmation() {
    C_DocType_ID = objectFieldProvider[0].getField("cDoctypeId");
    if ("".equals(DocumentType)) {
      loadDocumentType(true);
    }
    OBContext.setAdminMode();
    try {
      for (int i = 0; i < m_as.length; i++) {
        //@formatter:off
        String whereClause = " as astdt "
            + " where astdt.acctschemaTable.accountingSchema.id = :accountSchemaId"
            + "   and astdt.acctschemaTable.table.id = :tableId";
        if (!"".equals(DocumentType)) {
          whereClause += " and astdt.documentCategory = :documentType";
        }
        //@formatter:on
        final OBQuery<AcctSchemaTableDocType> obqParameters = OBDal.getInstance()
            .createQuery(AcctSchemaTableDocType.class, whereClause);
        obqParameters.setNamedParameter("accountSchemaId", m_as[i].m_C_AcctSchema_ID);
        obqParameters.setNamedParameter("tableId", AD_Table_ID);
        if (!"".equals(DocumentType)) {
          obqParameters.setNamedParameter("documentType", DocumentType);
        }
        final List<AcctSchemaTableDocType> acctSchemaTableDocTypes = obqParameters.list();
        if (acctSchemaTableDocTypes != null && acctSchemaTableDocTypes.size() > 0
            && acctSchemaTableDocTypes.get(0).getCreatefactTemplate() != null) {
          return true;
        }
        //@formatter:off
        final String whereClause2 = " as ast "
            + " where ast.accountingSchema.id = :accountSchemaId"
            + "   and ast.table.id = :tableId";
        //@formatter:on
        final OBQuery<AcctSchemaTable> obqParameters2 = OBDal.getInstance()
            .createQuery(AcctSchemaTable.class, whereClause2);
        obqParameters2.setNamedParameter("accountSchemaId", m_as[i].m_C_AcctSchema_ID);
        obqParameters2.setNamedParameter("tableId", AD_Table_ID);
        final List<AcctSchemaTable> acctSchemaTables = obqParameters2.list();
        if (acctSchemaTables != null && acctSchemaTables.size() > 0
            && acctSchemaTables.get(0).getCreatefactTemplate() != null) {
          return true;
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return false;
  }

  /**
   * Returns the amount of a Payment Detail. In case the related Payment Schedule Detail was
   * generated for compensate the difference between an Order and a related Invoice, it merges it's
   * amount with the next Payment Schedule Detail. Issue 19567:
   * https://issues.openbravo.com/view.php?id=19567
   * 
   * @param paymentDetails
   *          List of payment Details
   * @param ps
   *          Previous Payment Schedule
   * @param psi
   *          Invoice Payment Schedule of actual Payment Detail
   * @param pso
   *          Order Payment Schedule of actual Payment Detail
   * @param currentPaymentDetailIndex
   *          Index
   */
  @Deprecated
  public BigDecimal getPaymentDetailAmount(List<FIN_PaymentDetail> paymentDetails,
      FIN_PaymentSchedule ps, FIN_PaymentSchedule psi, FIN_PaymentSchedule pso,
      int currentPaymentDetailIndex) {
    if (psi == null && pso == null) {
      return paymentDetails.get(currentPaymentDetailIndex).getAmount();
    }
    // If the actual Payment Detail belongs to the same Invoice Payment Schedule as the previous
    // record, or it has no Order related.
    if ((psi != null && psi.equals(ps)) || pso == null) {
      FIN_PaymentScheduleDetail psdNext = (currentPaymentDetailIndex == paymentDetails.size() - 1)
          ? null
          : paymentDetails.get(currentPaymentDetailIndex + 1)
              .getFINPaymentScheduleDetailList()
              .get(0);
      FIN_PaymentScheduleDetail psdPrevious = (currentPaymentDetailIndex == 0) ? null
          : paymentDetails.get(currentPaymentDetailIndex - 1)
              .getFINPaymentScheduleDetailList()
              .get(0);
      // If it has no Order related, and the next record belongs to the same Invoice Payment
      // Schedule and the next record has an Order related.
      if (pso == null && psdNext != null && psdNext.getInvoicePaymentSchedule() == psi
          && psdNext.getOrderPaymentSchedule() != null) {
        return null;
        // If the previous record belongs to the same Invoice Payment Schedule and the previous
        // record has no Order related.
      } else if (psdPrevious != null && psdPrevious.getInvoicePaymentSchedule() == psi
          && psdPrevious.getOrderPaymentSchedule() == null) {
        return paymentDetails.get(currentPaymentDetailIndex)
            .getAmount()
            .add(paymentDetails.get(currentPaymentDetailIndex - 1).getAmount());
      } else {
        return paymentDetails.get(currentPaymentDetailIndex).getAmount();
      }
    } else {
      return paymentDetails.get(currentPaymentDetailIndex).getAmount();
    }
  }

  /**
   * Returns the writeoff and the amount of a Payment Detail. In case the related Payment Schedule
   * Detail was generated for compensate the difference between an Order and a related Invoice, it
   * merges it's amount with the next Payment Schedule Detail. Issue 19567:
   * https://issues.openbravo.com/view.php?id=19567. Use
   * {@link #getPaymentDetailIdWriteOffAndAmount(List, FIN_PaymentSchedule, FIN_PaymentSchedule, FIN_PaymentSchedule, int)}
   * instead
   * 
   * @param paymentDetails
   *          List of payment Details
   * @param ps
   *          Previous Payment Schedule
   * @param psi
   *          Invoice Payment Schedule of actual Payment Detail
   * @param pso
   *          Order Payment Schedule of actual Payment Detail
   * @param currentPaymentDetailIndex
   *          Index
   */
  @Deprecated
  public HashMap<String, BigDecimal> getPaymentDetailWriteOffAndAmount(
      List<FIN_PaymentDetail> paymentDetails, FIN_PaymentSchedule ps, FIN_PaymentSchedule psi,
      FIN_PaymentSchedule pso, int currentPaymentDetailIndex) {
    return getPaymentDetailWriteOffAndAmount(paymentDetails, ps, psi, pso,
        currentPaymentDetailIndex, null);
  }

  /**
   * Returns the writeoff and the amount of a Payment Detail. In case the related Payment Schedule
   * Detail was generated for compensate the difference between an Order and a related Invoice, it
   * merges it's amount with the next Payment Schedule Detail. Issue 19567:
   * https://issues.openbravo.com/view.php?id=19567
   * 
   * @param paymentDetailsIds
   *          List of payment Details Ids
   * @param ps
   *          Previous Payment Schedule
   * @param psi
   *          Invoice Payment Schedule of actual Payment Detail
   * @param pso
   *          Order Payment Schedule of actual Payment Detail
   * @param currentPaymentDetailIndex
   *          Index
   */
  public HashMap<String, BigDecimal> getPaymentDetailIdWriteOffAndAmount(
      List<String> paymentDetailsIds, FIN_PaymentSchedule ps, FIN_PaymentSchedule psi,
      FIN_PaymentSchedule pso, int currentPaymentDetailIndex) {
    FIN_PaymentDetail paymentDetail = OBDal.getInstance()
        .get(FIN_PaymentDetail.class, paymentDetailsIds.get(currentPaymentDetailIndex));
    String paymentDetailNextId = null;
    String paymentDetailPreviousId = null;
    if (currentPaymentDetailIndex < paymentDetailsIds.size() - 1) {
      paymentDetailNextId = paymentDetailsIds.get(currentPaymentDetailIndex + 1);
    }
    if (currentPaymentDetailIndex > 0) {
      paymentDetailPreviousId = paymentDetailsIds.get(currentPaymentDetailIndex - 1);
    }
    return getPaymentDetailWriteOffAndAmount(paymentDetail, paymentDetailNextId,
        paymentDetailPreviousId, ps, psi, pso, null);
  }

  /**
   * Returns the writeoff and the amount of a Payment Detail. In case the related Payment Schedule
   * Detail was generated for compensate the difference between an Order and a related Invoice, it
   * merges it's amount with the next Payment Schedule Detail. Issue 19567:
   * https://issues.openbravo.com/view.php?id=19567 <br>
   * It does exactly the same as the
   * {@link #getPaymentDetailWriteOffAndAmount(List, FIN_PaymentSchedule, FIN_PaymentSchedule, FIN_PaymentSchedule, int)}
   * method, but it also stores a new field "MergedPaymentDetailId" inside the fieldProvider with
   * the merged payment detail id (if any).
   * 
   * @param paymentDetails
   *          List of payment Details
   * @param ps
   *          Previous Payment Schedule
   * @param psi
   *          Invoice Payment Schedule of actual Payment Detail
   * @param pso
   *          Order Payment Schedule of actual Payment Detail
   * @param currentPaymentDetailIndex
   *          Index
   * @param fieldProvider
   *          contains the FieldProvider with the Payment Detail currently being processed. Used to
   *          store the "MergedPaymentDetailId" (if any) as a new field of the fieldProvider
   */
  public HashMap<String, BigDecimal> getPaymentDetailWriteOffAndAmount(
      List<FIN_PaymentDetail> paymentDetails, FIN_PaymentSchedule ps, FIN_PaymentSchedule psi,
      FIN_PaymentSchedule pso, int currentPaymentDetailIndex, final FieldProvider fieldProvider) {
    FIN_PaymentDetail paymentDetail = paymentDetails.get(currentPaymentDetailIndex);
    String paymentDetailNextId = null;
    String paymentDetailPreviousId = null;
    if (currentPaymentDetailIndex < paymentDetails.size() - 1) {
      paymentDetailNextId = paymentDetails.get(currentPaymentDetailIndex + 1).getId();
    }
    if (currentPaymentDetailIndex > 0) {
      paymentDetailPreviousId = paymentDetails.get(currentPaymentDetailIndex - 1).getId();
    }
    return getPaymentDetailWriteOffAndAmount(paymentDetail, paymentDetailNextId,
        paymentDetailPreviousId, ps, psi, pso, fieldProvider);
  }

  /**
   * Returns the writeoff and the amount of a Payment Detail. In case the related Payment Schedule
   * Detail was generated for compensate the difference between an Order and a related Invoice, it
   * merges it's amount with the next Payment Schedule Detail. Issue 19567:
   * https://issues.openbravo.com/view.php?id=19567 <br>
   * It does exactly the same as the
   * {@link #getPaymentDetailWriteOffAndAmount(List, FIN_PaymentSchedule, FIN_PaymentSchedule, FIN_PaymentSchedule, int)}
   * method, but it also stores a new field "MergedPaymentDetailId" inside the fieldProvider with
   * the merged payment detail id (if any).
   * 
   * @param paymentDetail
   *          Payment Detail
   * @param paymentDetailNextId
   *          Next Payment Detail id
   * @param paymentDetailPreviousId
   *          Previous Payment Detail id
   * @param ps
   *          Previous Payment Schedule
   * @param psi
   *          Invoice Payment Schedule of actual Payment Detail
   * @param pso
   *          Order Payment Schedule of actual Payment Detail
   * @param fieldProvider
   *          contains the FieldProvider with the Payment Detail currently being processed. Used to
   *          store the "MergedPaymentDetailId" (if any) as a new field of the fieldProvider
   */
  public HashMap<String, BigDecimal> getPaymentDetailWriteOffAndAmount(
      FIN_PaymentDetail paymentDetail, String paymentDetailNextId, String paymentDetailPreviousId,
      FIN_PaymentSchedule ps, FIN_PaymentSchedule psi, FIN_PaymentSchedule pso,
      final FieldProvider fieldProvider) {
    HashMap<String, BigDecimal> amountAndWriteOff = new HashMap<String, BigDecimal>();

    // Default return values
    amountAndWriteOff.put("amount", paymentDetail.getAmount());
    amountAndWriteOff.put("writeoff", paymentDetail.getWriteoffAmount());

    // This value indicates that the current payment detail amount must be added to the previous one
    amountAndWriteOff.put("merged", BigDecimal.ZERO);

    // If the Payment Detail has either an Invoice or an Order associated to it
    if (psi != null || pso != null) {
      // If the Payment Detail has no Order associated to it, or it has an Invoice associated and is
      // the same one as the previous Payment Detail
      if ((psi != null && psi.equals(ps)) || pso == null) {
        FIN_PaymentDetail paymentDetailNext = null;
        FIN_PaymentDetail paymentDetailPrevious = null;
        FIN_PaymentScheduleDetail psdNext = null;
        FIN_PaymentScheduleDetail psdPrevious = null;
        if (paymentDetailNextId != null) {
          paymentDetailNext = OBDal.getInstance().get(FIN_PaymentDetail.class, paymentDetailNextId);
          psdNext = paymentDetailNext.getFINPaymentScheduleDetailList().get(0);
        }
        if (paymentDetailPreviousId != null) {
          paymentDetailPrevious = OBDal.getInstance()
              .get(FIN_PaymentDetail.class, paymentDetailPreviousId);
          psdPrevious = paymentDetailPrevious.getFINPaymentScheduleDetailList().get(0);
        }
        // If the Payment Detail has no Order associated, and the next Payment Detail belongs to the
        // same Invoice and it has an Order related, then return null
        if (pso == null && psdNext != null && psdNext.getInvoicePaymentSchedule() == psi
            && psdNext.getOrderPaymentSchedule() != null) {
          amountAndWriteOff.put("amount", null);
          amountAndWriteOff.put("writeoff", null);
          // If there is a previous Payment Detail that belongs to the same Invoice and has no Order
          // related to it, return the sum of amounts.
        } else if (psdPrevious != null && psdPrevious.getInvoicePaymentSchedule() == psi
            && psdPrevious.getOrderPaymentSchedule() == null) {
          amountAndWriteOff.put("amount",
              paymentDetail.getAmount().add(paymentDetailPrevious.getAmount()));
          amountAndWriteOff.put("writeoff",
              paymentDetail.getWriteoffAmount().add(paymentDetailPrevious.getWriteoffAmount()));

          if (fieldProvider != null) {
            FieldProviderFactory.setField(fieldProvider, "MergedPaymentDetailId",
                paymentDetailPreviousId);
          }
        }
        // If there is a previous Payment Detail that belongs to the same Invoice, the amounts
        // should be added.
        else if (psdPrevious != null && psdPrevious.getInvoicePaymentSchedule() == psi) {
          amountAndWriteOff.put("merged", BigDecimal.ONE);
        }
      }
    }

    return amountAndWriteOff;

  }

  public String catchPostError(String strKey, boolean force, VariablesSecureApp vars, ConnectionProvider connectionProvider, Connection con ){
    try{
      AcctServer.throwErrors = true;
      this.post(strKey, false,  vars, connectionProvider, con);
    }
    catch (OBException |ServletException e){
      log4j.error(e);
      return e.toString();

    }finally {
      AcctServer.throwErrors = false;
    }
    return null;
  }
}
