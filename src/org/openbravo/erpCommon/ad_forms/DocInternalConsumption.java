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
 * Contributions are Copyright (C) 2014-2019 Openbravo S.L.U.
 ******************************************************************************
 */
package org.openbravo.erpCommon.ad_forms;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;

import jakarta.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.InternalConsumptionLine;

public class DocInternalConsumption extends AcctServer {
  private static final long serialVersionUID = 1L;
  private static final Logger log4jDocInternalConsumption = LogManager.getLogger();

  private String SeqNo = "0";

  /**
   * Constructor
   * 
   * @param AD_Client_ID
   *          client
   */
  public DocInternalConsumption(String AD_Client_ID, String AD_Org_ID,
      ConnectionProvider connectionProvider) {
    super(AD_Client_ID, AD_Org_ID, connectionProvider);
  }

  @Override
  public void loadObjectFieldProvider(ConnectionProvider conn, String aD_Client_ID, String Id)
      throws ServletException {
    setObjectFieldProvider(DocInternalConsumptionData.select(conn, aD_Client_ID, Id));
  }

  /**
   * Load Document Details
   * 
   * @return true if loadDocumentType was set
   */
  @Override
  public boolean loadDocumentDetails(FieldProvider[] data, ConnectionProvider conn) {
    DocumentType = AcctServer.DOCTYPE_MatInternalConsumption;
    C_Currency_ID = NO_CURRENCY;
    DateDoc = data[0].getField("MovementDate");
    loadDocumentType(); // lines require doc type
    // Contained Objects
    p_lines = loadLines(conn);
    log4jDocInternalConsumption.debug("Lines=" + p_lines.length);
    return true;
  } // loadDocumentDetails

  /**
   * Load Internal Consumption Line
   * 
   * @return DocLine Array
   */
  private DocLine[] loadLines(ConnectionProvider conn) {
    ArrayList<Object> list = new ArrayList<>();
    DocLineInternalConsumptionData[] data = null;
    OBContext.setAdminMode(false);
    try {
      data = DocLineInternalConsumptionData.select(conn, Record_ID);
      for (int i = 0; data != null && i < data.length; i++) {
        String Line_ID = data[i].getField("mInternalConsumptionlineId");
        DocLine_Material docLine = new DocLine_Material(DocumentType, Record_ID, Line_ID);
        docLine.loadAttributes(data[i], this);
        log4jDocInternalConsumption.debug("MovementQty = " + data[i].getField("movementqty"));
        BigDecimal MovementQty = new BigDecimal(data[i].getField("movementqty"));
        docLine.setQty(MovementQty.toPlainString(), conn);
        docLine.m_M_Locator_ID = data[i].getField("mLocatorId");

        // Get related M_Transaction_ID
        InternalConsumptionLine intConsLine = OBDal.getInstance()
            .get(InternalConsumptionLine.class, Line_ID);
        updateTransactionForDocLine(intConsLine, docLine);

        DocInternalConsumptionData[] data1 = getInternalConsumptionWarehouse(conn, docLine);
        if (data1 != null && data1.length > 0) {
          this.M_Warehouse_ID = data1[0].mWarehouseId;
        }
        list.add(docLine);
      }
    } catch (ServletException e) {
      log4jDocInternalConsumption.warn(e);
    } finally {
      OBContext.restorePreviousMode();
    }
    // Return Array
    DocLine[] dl = new DocLine[list.size()];
    list.toArray(dl);
    return dl;
  } // loadLines

  private DocInternalConsumptionData[] getInternalConsumptionWarehouse(ConnectionProvider conn,
      DocLine_Material docLine) {
    DocInternalConsumptionData[] data = null;
    try {
      data = DocInternalConsumptionData.selectWarehouse(conn, docLine.m_M_Locator_ID);
    } catch (ServletException e) {
      log4jDocInternalConsumption.warn(e);
    }
    return data;
  }

  private void updateTransactionForDocLine(InternalConsumptionLine intConsLine, DocLine_Material docLine) {
    if (!intConsLine.getMaterialMgmtMaterialTransactionList().isEmpty()) {
      docLine.setTransaction(intConsLine.getMaterialMgmtMaterialTransactionList().get(0));
    }
  }

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
   * Create Facts (the accounting logic) for MIC.
   *
   * <pre>
   *  Internal Consumption
   *      CoGS            DR
   *      Inventory               CR
   * </pre>
   *
   * @param as
   *     account schema
   * @return Fact
   */
  @Override
  public Fact createFact(AcctSchema as, ConnectionProvider conn, Connection con,
      VariablesSecureApp vars) throws ServletException {
    // Select specific definition
    String strClassname = getTemplateClassname(as, conn);
    if (!StringUtils.isEmpty(strClassname)) {
      return instantiateTemplateAndCreateFact(strClassname, as, conn, con, vars);
    }

    C_Currency_ID = as.getC_Currency_ID();
    // create Fact Header
    Fact fact = new Fact(this, as, Fact.POST_Actual);
    String factAcctGroupId = SequenceIdData.getUUID();
    // Line pointers
    FactLine dr = null;
    FactLine cr = null;
    log4jDocInternalConsumption.debug("CreateFact - before loop");
    for (DocLine p_line : p_lines) {
      DocLine_Material line = (DocLine_Material) p_line;

      Currency costCurrency = determineCostCurrency(line);
      validateCostCalculation(line, conn);

      String strCosts = getCostsString(line, as, conn, con);
      createFactLines(as, fact, line, costCurrency, strCosts, factAcctGroupId, conn);
    }
    log4jDocInternalConsumption.debug("CreateFact - after loop");
    SeqNo = "0";
    return fact;
  } // createFact

  private String getTemplateClassname(AcctSchema as, ConnectionProvider conn) throws ServletException {
    String strClassname = AcctServerData.selectTemplateDoc(conn, as.m_C_AcctSchema_ID, DocumentType);
    if (StringUtils.isEmpty(strClassname)) {
      strClassname = AcctServerData.selectTemplate(conn, as.m_C_AcctSchema_ID, AD_Table_ID);
    }
    return strClassname;
  }

  private Fact instantiateTemplateAndCreateFact(String strClassname, AcctSchema as, ConnectionProvider conn,
      Connection con, VariablesSecureApp vars) throws ServletException {
    try {
      DocInternalConsumptionTemplate newTemplate = (DocInternalConsumptionTemplate) Class.forName(strClassname)
          .getDeclaredConstructor().newInstance();
      return newTemplate.createFact(this, as, conn, con, vars);
    } catch (Exception e) {
      log4j.error("Error while creating new instance for DocInternalConsumptionTemplate - " + e);
      throw new ServletException(e);
    }
  }

  private Currency determineCostCurrency(DocLine_Material line) {
    if (line.transaction != null && line.transaction.getCurrency() != null) {
      return line.transaction.getCurrency();
    }
    return FinancialUtils.getLegalEntityCurrency(
        OBDal.getInstance().get(Organization.class, line.m_AD_Org_ID));
  }

  private void validateCostCalculation(DocLine_Material line, ConnectionProvider conn) {
    if (line.transaction != null && !line.transaction.isCostCalculated()) {
      Map<String, String> parameters = getNotCalculatedCostParameters(line.transaction);
      setMessageResult(conn, STATUS_NotCalculatedCost, "error", parameters);
      throw new IllegalStateException();
    }
  }

  private String getCostsString(DocLine_Material line, AcctSchema as, ConnectionProvider conn,
      Connection con) {
    String costs = line.getProductCosts(DateAcct, as, conn, con);
    log4jDocInternalConsumption.debug("CreateFact - before DR - Costs: " + costs);
    BigDecimal bCosts = new BigDecimal(costs);
    return bCosts.toPlainString();
  }

  private void createFactLines(AcctSchema as, Fact fact, DocLine_Material line, Currency costCurrency, String strCosts,
      String factAcctGroupId, ConnectionProvider conn) throws ServletException {
    Account cogsAccount = getAccount(line, ProductInfo.ACCTTYPE_P_Cogs, as, conn, "COGS");
    Account assetAccount = getAccount(line, ProductInfo.ACCTTYPE_P_Asset, as, conn, "Asset");

    FactLine dr = fact.createLine(line, cogsAccount, costCurrency.getId(), strCosts, "", factAcctGroupId,
        nextSeqNo(SeqNo), DocumentType, conn);
    if (dr != null) {
      setFactLineLocations(dr, line, conn);
    }

    FactLine cr = fact.createLine(line, assetAccount, costCurrency.getId(), "", strCosts, factAcctGroupId,
        nextSeqNo(SeqNo), DocumentType, conn);
    if (cr != null) {
      setFactLineLocations(cr, line, conn);
    }
  }

  private Account getAccount(DocLine_Material line, String acctType, AcctSchema as, ConnectionProvider conn,
      String accountTypeName) {
    Account account = line.getAccount(acctType, as, conn);
    if (account == null) {
      org.openbravo.model.financialmgmt.accounting.coa.AcctSchema schema = OBDal.getInstance()
          .get(org.openbravo.model.financialmgmt.accounting.coa.AcctSchema.class, as.m_C_AcctSchema_ID);
      Product product = OBDal.getInstance().get(Product.class, line.m_M_Product_ID);
      log4j.error(
          "No Account " + accountTypeName + " for product: " + product.getName() + " in accounting schema: " + schema.getName());
    }
    return account;
  }

  private void setFactLineLocations(FactLine factLine, DocLine_Material line,
      ConnectionProvider conn) {
    factLine.setM_Locator_ID(line.m_M_Locator_ID);
    factLine.setLocationFromLocator(line.m_M_Locator_ID, true, conn); // from
    factLine.setLocationFromBPartner(C_BPartner_Location_ID, false, conn); // to
  }

  /**
   * Get Document Confirmation
   * 
   * not used
   */
  @Override
  public boolean getDocumentConfirmation(ConnectionProvider conn, String strRecordId) {
    return true;
  }

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
   * @param oldSeqNo
   *          old sequence number
   * @return the next sequence number
   */
  public String nextSeqNo(String oldSeqNo) {
    log4jDocInternalConsumption.debug("DocInternalConsumption - oldSeqNo = " + oldSeqNo);
    BigDecimal seqNo = new BigDecimal(oldSeqNo);
    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
    log4jDocInternalConsumption.debug("DocInternalConsumption - nextSeqNo = " + SeqNo);
    return SeqNo;
  }

  /**
   * @return the serialVersionUID
   */
  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  /**
   * @return the servlet information
   */
  @Override
  public String getServletInfo() {
    return "Servlet for the internal consumption accounting";
  }
}
