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

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;

import jakarta.servlet.ServletException;

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
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;

public class DocInventory extends AcctServer {
  private static final long serialVersionUID = 1L;
  private static final Logger log4jDocInventory = LogManager.getLogger();

  private String SeqNo = "0";

  /**
   * Constructor
   * 
   * @param AD_Client_ID
   *          client
   */
  public DocInventory(String AD_Client_ID, String AD_Org_ID,
      ConnectionProvider connectionProvider) {
    super(AD_Client_ID, AD_Org_ID, connectionProvider);
  }

  @Override
  public void loadObjectFieldProvider(ConnectionProvider conn, String stradClientId, String Id)
      throws ServletException {
    setObjectFieldProvider(DocInventoryData.select(conn, stradClientId, Id));
  }

  /**
   * Load Document Details
   * 
   * @return true if loadDocumentType was set
   */
  @Override
  public boolean loadDocumentDetails(FieldProvider[] data, ConnectionProvider conn) {
    DocumentType = AcctServer.DOCTYPE_MatInventory;
    C_Currency_ID = NO_CURRENCY;
    DateDoc = data[0].getField("MovementDate");
    loadDocumentType(); // lines require doc type
    // Contained Objects
    p_lines = loadLines(conn);
    log4jDocInventory.debug("Lines=" + p_lines.length);
    return true;
  } // loadDocumentDetails

  /**
   * Load Invoice Line
   * 
   * @return DocLine Array
   */
  private DocLine[] loadLines(ConnectionProvider conn) {
    ArrayList<Object> list = new ArrayList<Object>();
    DocLineInventoryData[] data = null;
    OBContext.setAdminMode(false);
    try {
      data = DocLineInventoryData.select(conn, Record_ID);
      for (int i = 0; i < data.length; i++) {
        String Line_ID = data[i].getField("mInventorylineId");
        DocLine_Material docLine = new DocLine_Material(DocumentType, Record_ID, Line_ID);
        docLine.loadAttributes(data[i], this);
        log4jDocInventory.debug("QtyBook = " + data[i].getField("qtybook") + " - QtyCount = "
            + data[i].getField("qtycount"));
        BigDecimal QtyBook = new BigDecimal(data[i].getField("qtybook"));
        BigDecimal QtyCount = new BigDecimal(data[i].getField("qtycount"));
        docLine.setQty((QtyCount.subtract(QtyBook)).toString(), conn);
        docLine.m_M_Locator_ID = data[i].getField("mLocatorId");
        // Get related M_Transaction_ID
        InventoryCountLine invLine = OBDal.getInstance().get(InventoryCountLine.class, Line_ID);
        if (invLine.getMaterialMgmtMaterialTransactionList().size() > 0) {
          docLine.setTransaction(invLine.getMaterialMgmtMaterialTransactionList().get(0));
        }
        DocInventoryData[] data1 = null;
        try {
          data1 = DocInventoryData.selectWarehouse(conn, docLine.m_M_Locator_ID);
        } catch (ServletException e) {
          log4jDocInventory.warn(e);
        }
        if (data1 != null && data1.length > 0) {
          this.M_Warehouse_ID = data1[0].mWarehouseId;
        }
        // Set Charge ID only when Inventory Type = Charge
        if (!"C".equals(data[i].getField("inventorytype"))) {
          docLine.m_C_Charge_ID = "";
        }
        //
        list.add(docLine);
      }
    } catch (ServletException e) {
      log4jDocInventory.warn(e);
    } finally {
      OBContext.restorePreviousMode();
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
   * Create Facts (the accounting logic) for MMI.
   * 
   * <pre>
   *  Inventory
   *      Inventory       DR      CR
   *      InventoryDiff   DR      CR   (or Charge)
   * </pre>
   * 
   * @param as
   *          account schema
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
        DocInventoryTemplate newTemplate = (DocInventoryTemplate) Class.forName(strClassname)
            .getDeclaredConstructor()
            .newInstance();
        return newTemplate.createFact(this, as, conn, con, vars);
      } catch (Exception e) {
        log4j.error("Error while creating new instance for DocInventoryTemplate - " + e);
      }
    }
    // Log.trace(Log.l4_Data, "Doc.Inventory.createFact");
    C_Currency_ID = as.getC_Currency_ID();
    // create Fact Header
    Fact fact = new Fact(this, as, Fact.POST_Actual);
    String Fact_Acct_Group_ID = SequenceIdData.getUUID();
    // Line pointers
    FactLine dr = null;
    FactLine cr = null;
    log4jDocInventory.debug("CreateFact - before loop");
    if (p_lines.length == 0) {
      setStatus(STATUS_DocumentDisabled);
    }
    int countInvLinesWithTrnCostZero = 0;
    for (int i = 0; i < p_lines.length; i++) {
      DocLine_Material line = (DocLine_Material) p_lines[i];
      if (line.transaction != null && "NC".equals(line.transaction.getCostingStatus())) {
        setStatus(STATUS_NotCalculatedCost);
      }

      if (line.transaction == null || (line.transaction.getTransactionCost() != null
          && line.transaction.getTransactionCost().compareTo(ZERO) == 0)) {
        countInvLinesWithTrnCostZero++;
      }
    }
    if (p_lines.length == countInvLinesWithTrnCostZero) {
      setStatus(STATUS_DocumentDisabled);
    }
    for (int i = 0; i < p_lines.length; i++) {
      DocLine_Material line = (DocLine_Material) p_lines[i];

      Currency costCurrency = FinancialUtils
          .getLegalEntityCurrency(OBDal.getInstance().get(Organization.class, line.m_AD_Org_ID));
      if (line.transaction != null && line.transaction.getCurrency() != null) {
        costCurrency = line.transaction.getCurrency();
      }
      if (line.transaction != null && !line.transaction.isCostCalculated()) {
        Map<String, String> parameters = getNotCalculatedCostParameters(line.transaction);
        setMessageResult(conn, STATUS_NotCalculatedCost, "error", parameters);
        throw new IllegalStateException();
      }
      String costs = "";
      BigDecimal b_Costs = BigDecimal.ZERO;
      if (line.transaction != null) {
        costs = line.getProductCosts(DateAcct, as, conn, con);
        b_Costs = new BigDecimal(costs);
      }
      log4jDocInventory.debug("CreateFact - before DR - Costs: " + costs);
      Account assetAccount = line.getAccount(ProductInfo.ACCTTYPE_P_Asset, as, conn);
      if (assetAccount == null) {
        Product product = OBDal.getInstance().get(Product.class, line.m_M_Product_ID);
        org.openbravo.model.financialmgmt.accounting.coa.AcctSchema schema = OBDal.getInstance()
            .get(org.openbravo.model.financialmgmt.accounting.coa.AcctSchema.class,
                as.m_C_AcctSchema_ID);
        log4j.error("No Account Asset for product: " + product.getName() + " in accounting schema: "
            + schema.getName());
      }
      // Inventory DR CR
      dr = fact.createLine(line, assetAccount, costCurrency.getId(), costs, Fact_Acct_Group_ID,
          nextSeqNo(SeqNo), DocumentType, conn);
      // may be zero difference - no line created.
      if (dr == null) {
        continue;
      }
      dr.setM_Locator_ID(line.m_M_Locator_ID);
      log4jDocInventory.debug("CreateFact - before CR");
      // InventoryDiff DR CR
      // or Charge
      Account invDiff = line.getChargeAccount(as, b_Costs.negate(), conn);
      log4jDocInventory.debug("CreateFact - after getChargeAccount");
      if (invDiff == null) {
        invDiff = getAccount(AcctServer.ACCTTYPE_InvDifferences, as, conn);
      }
      log4jDocInventory.debug("CreateFact - after getAccount - invDiff; " + invDiff);
      cr = fact.createLine(line, invDiff, costCurrency.getId(), (b_Costs.negate()).toString(),
          Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
      if (cr == null) {
        continue;
      }
      cr.setM_Locator_ID(line.m_M_Locator_ID);
    }
    log4jDocInventory.debug("CreateFact - after loop");
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
    log4jDocInventory.debug("DocInventory - oldSeqNo = " + oldSeqNo);
    BigDecimal seqNo = new BigDecimal(oldSeqNo);
    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
    log4jDocInventory.debug("DocInventory - nextSeqNo = " + SeqNo);
    return SeqNo;
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

  @Override
  public String getServletInfo() {
    return "Servlet for the accounting";
  } // end of getServletInfo() method
}
