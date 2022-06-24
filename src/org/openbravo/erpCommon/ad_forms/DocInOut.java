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
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.costing.CostingAlgorithm.CostDimension;
import org.openbravo.costing.CostingUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

public class DocInOut extends AcctServer {
  private static final long serialVersionUID = 1L;
  private static final Logger log4jDocInOut = LogManager.getLogger();

  /** AD_Table_ID */
  private String SeqNo = "0";

  /**
   * Constructor
   * 
   * @param AD_Client_ID
   *          AD_Client_ID
   */
  public DocInOut(String AD_Client_ID, String AD_Org_ID, ConnectionProvider connectionProvider) {
    super(AD_Client_ID, AD_Org_ID, connectionProvider);
  }

  @Override
  public void loadObjectFieldProvider(ConnectionProvider conn, String stradClientId, String Id)
      throws ServletException {
    setObjectFieldProvider(DocInOutData.selectRegistro(conn, stradClientId, Id));
  }

  /**
   * Load Document Details
   * 
   * @return true if loadDocumentType was set
   */
  @Override
  public boolean loadDocumentDetails(FieldProvider[] data, ConnectionProvider conn) {
    C_Currency_ID = NO_CURRENCY;
    log4jDocInOut.debug("loadDocumentDetails - C_Currency_ID : " + C_Currency_ID);
    DateDoc = data[0].getField("MovementDate");
    C_BPartner_Location_ID = data[0].getField("C_BPartner_Location_ID");

    loadDocumentType(); // lines require doc type
    // Contained Objects
    p_lines = loadLines(conn);
    log4jDocInOut.debug("Lines=" + p_lines.length);
    return true;
  } // loadDocumentDetails

  /**
   * Load Invoice Line
   * 
   * @return DocLine Array
   */
  public DocLine[] loadLines(ConnectionProvider conn) {
    ArrayList<Object> list = new ArrayList<Object>();
    DocLineInOutData[] data = null;
    try {
      data = DocLineInOutData.select(conn, Record_ID);
    } catch (ServletException e) {
      log4jDocInOut.warn(e);
    }
    //
    for (int i = 0; data != null && i < data.length; i++) {
      String Line_ID = data[i].getField("M_INOUTLINE_ID");
      DocLine_Material docLine = new DocLine_Material(DocumentType, Record_ID, Line_ID);
      docLine.loadAttributes(data[i], this);
      docLine.setQty(data[i].getField("MOVEMENTQTY"), conn); // sets Trx
      docLine.setBreakdownQty(data[i].getField("BREAKDOWNQTY"));
      // and
      // Storage
      // Qty
      docLine.m_M_Locator_ID = data[i].getField("M_LOCATOR_ID");
      OBContext.setAdminMode(false);
      try {
        // Get related M_Transaction_ID
        ShipmentInOutLine inOut = OBDal.getInstance().get(ShipmentInOutLine.class, Line_ID);
        if (inOut.getMaterialMgmtMaterialTransactionList().size() > 0) {
          docLine.setTransaction(inOut.getMaterialMgmtMaterialTransactionList().get(0));
        }
      } finally {
        OBContext.restorePreviousMode();
      }
      if (docLine.m_M_Product_ID.equals("")) {
        log4jDocInOut.debug(" - No Product - ignored");
      } else {
        list.add(docLine);
      }
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
    if (strClassname.equals("")) {
      strClassname = AcctServerData.selectTemplate(conn, as.m_C_AcctSchema_ID, AD_Table_ID);
    }
    if (!strClassname.equals("")) {
      try {
        DocInOutTemplate newTemplate = (DocInOutTemplate) Class.forName(strClassname)
            .getDeclaredConstructor()
            .newInstance();
        return newTemplate.createFact(this, as, conn, con, vars);
      } catch (Exception e) {
        log4j.error("Error while creating new instance for DocInOutTemplate - " + e);
      }
    }
    // C_Currency_ID = as.getC_Currency_ID();
    // create Fact Header
    Fact fact = new Fact(this, as, Fact.POST_Actual);
    String Fact_Acct_Group_ID = SequenceIdData.getUUID();
    // Line pointers
    FactLine dr = null;
    FactLine cr = null;

    OBContext.setAdminMode(false);
    try {
      // Sales or Return from Customer
      if (DocumentType.equals(AcctServer.DOCTYPE_MatShipment)) {
        Boolean matReturn = IsReturn.equals("Y");

        for (int i = 0; p_lines != null && i < p_lines.length; i++) {
          DocLine_Material line = (DocLine_Material) p_lines[i];
          Organization legalEntity = OBContext.getOBContext()
              .getOrganizationStructureProvider(AD_Client_ID)
              .getLegalEntity(OBDal.getInstance().get(Organization.class, line.m_AD_Org_ID));
          Currency costCurrency = FinancialUtils.getLegalEntityCurrency(legalEntity);
          if (line.transaction != null && line.transaction.getCurrency() != null) {
            costCurrency = line.transaction.getCurrency();
          }
          int standardPrecision = 2;
          standardPrecision = costCurrency.getStandardPrecision().intValue();
          C_Currency_ID = costCurrency.getId();
          Account cogsAccount = null;
          if (matReturn) {
            cogsAccount = line.getAccount(ProductInfo.ACCTTYPE_P_CogsReturn, as, conn);
          }
          if (cogsAccount == null) {
            cogsAccount = line.getAccount(ProductInfo.ACCTTYPE_P_Cogs, as, conn);
          }
          Product product = OBDal.getInstance().get(Product.class, line.m_M_Product_ID);
          if (cogsAccount == null) {
            org.openbravo.model.financialmgmt.accounting.coa.AcctSchema schema = OBDal.getInstance()
                .get(org.openbravo.model.financialmgmt.accounting.coa.AcctSchema.class,
                    as.m_C_AcctSchema_ID);
            if (matReturn) {
              log4j.error("No Account COGS Return for product: " + product.getName()
                  + " in accounting schema: " + schema.getName());
            } else {
              log4j.error("No Account COGS for product: " + product.getName()
                  + " in accounting schema: " + schema.getName());
            }
          }
          Account assetAccount = line.getAccount(ProductInfo.ACCTTYPE_P_Asset, as, conn);
          if (assetAccount == null) {
            org.openbravo.model.financialmgmt.accounting.coa.AcctSchema schema = OBDal.getInstance()
                .get(org.openbravo.model.financialmgmt.accounting.coa.AcctSchema.class,
                    as.m_C_AcctSchema_ID);
            log4j.error("No Account Asset for product: " + product.getName()
                + " in accounting schema: " + schema.getName());
          }
          if (!isConvertible(as, conn)) {
            setMessageResult(conn, STATUS_NotConvertible, "error", null);
            throw new IllegalStateException();
          }
          if (line.transaction != null && !line.transaction.isCostCalculated()) {
            Map<String, String> parameters = getNotCalculatedCostParameters(line.transaction);
            setMessageResult(conn, STATUS_NotCalculatedCost, "error", parameters);
            throw new IllegalStateException();
          } else if (line.transaction == null) {
            // Check default cost existence
            HashMap<CostDimension, BaseOBObject> costDimensions = CostingUtils.getEmptyDimensions();
            costDimensions.put(CostDimension.Warehouse, line.getWarehouse());
            if (!CostingUtils.hasStandardCostDefinition(product, legalEntity, dateAcct,
                costDimensions)) {
              Map<String, String> parameters = getInvalidCostParameters(product.getIdentifier(),
                  DateAcct);
              setMessageResult(conn, STATUS_InvalidCost, "error", parameters);
              throw new IllegalStateException();
            }
          }
          String costs = line.getProductCosts(DateAcct, as, conn, con);
          if (matReturn) {
            log4jDocInOut.debug("(MatShipmentReturn) - DR account: "
                + line.getAccount(ProductInfo.ACCTTYPE_P_Cogs, as, conn));
            log4jDocInOut.debug("(MatShipmentReturn) - DR costs: " + costs);
          } else {
            log4jDocInOut.debug("(MatShipment) - DR account: "
                + line.getAccount(ProductInfo.ACCTTYPE_P_Cogs, as, conn));
            log4jDocInOut.debug("(MatShipment) - DR costs: " + costs);
          }
          BigDecimal b_Costs = new BigDecimal(costs)
              .multiply(new BigDecimal(line.getBreakdownQty()))
              .divide(new BigDecimal(line.m_qty), standardPrecision, RoundingMode.HALF_UP);
          String strCosts = b_Costs.toString();
          // CoGS DR
          dr = fact.createLine(line, cogsAccount, costCurrency.getId(), strCosts, "",
              Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
          if (dr != null) {
            dr.setM_Locator_ID(line.m_M_Locator_ID);
            dr.setLocationFromLocator(line.m_M_Locator_ID, true, conn); // from
            // Loc
            dr.setLocationFromBPartner(C_BPartner_Location_ID, false, conn); // to
          }
          // Loc
          if (matReturn) {
            log4jDocInOut.debug("(MatShipmentReturn) - CR account: "
                + line.getAccount(ProductInfo.ACCTTYPE_P_Asset, as, conn));
            log4jDocInOut.debug("(MatShipmentReturn) - CR costs: " + strCosts);
          } else {
            log4jDocInOut.debug("(MatShipment) - CR account: "
                + line.getAccount(ProductInfo.ACCTTYPE_P_Asset, as, conn));
            log4jDocInOut.debug("(MatShipment) - CR costs: " + strCosts);
          }
          // Inventory CR
          cr = fact.createLine(line, assetAccount, costCurrency.getId(), "", strCosts,
              Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
          if (cr != null) {
            cr.setM_Locator_ID(line.m_M_Locator_ID);
            cr.setLocationFromLocator(line.m_M_Locator_ID, true, conn); // from
            // Loc
            cr.setLocationFromBPartner(C_BPartner_Location_ID, false, conn); // to
            // Loc
          }
        }
      }
      // Purchasing
      else if (DocumentType.equals(AcctServer.DOCTYPE_MatReceipt)) {
        for (int i = 0; p_lines != null && i < p_lines.length; i++) {
          DocLine_Material line = (DocLine_Material) p_lines[i];
          Product product = OBDal.getInstance().get(Product.class, line.m_M_Product_ID);
          Organization legalEntity = OBContext.getOBContext()
              .getOrganizationStructureProvider(AD_Client_ID)
              .getLegalEntity(OBDal.getInstance().get(Organization.class, line.m_AD_Org_ID));
          Currency costCurrency = FinancialUtils.getLegalEntityCurrency(legalEntity);
          if (line.transaction != null && line.transaction.getCurrency() != null) {
            costCurrency = line.transaction.getCurrency();
          }
          C_Currency_ID = costCurrency.getId();

          String costs = "0";
          String strCosts = "0";
          if (product.isBookUsingPurchaseOrderPrice()) {
            // If the Product is checked as book using PO Price, the Price of the Purchase Order
            // will
            // be used to create the FactAcct Line
            ShipmentInOutLine inOutLine = OBDal.getInstance()
                .get(ShipmentInOutLine.class, line.m_TrxLine_ID);
            OrderLine ol = inOutLine.getSalesOrderLine();
            if (ol == null) {
              Map<String, String> parameters = new HashMap<String, String>();
              parameters.put("product", inOutLine.getProduct().getIdentifier());
              parameters.put("line", inOutLine.getLineNo().toString());
              setMessageResult(conn, STATUS_NoRelatedPO, "error", parameters);
              throw new IllegalStateException();
            }
            costCurrency = ol.getCurrency();
            C_Currency_ID = costCurrency.getId();
            costs = ol.getUnitPrice().multiply(new BigDecimal(line.getBreakdownQty())).toString();
            BigDecimal b_Costs = new BigDecimal(costs)
                .setScale(costCurrency.getStandardPrecision().intValue(), RoundingMode.HALF_UP);
            strCosts = b_Costs.toString();
          } else {
            // If the Product is not checked as book using PO Price, the Cost of the
            // Transaction will be used to create the FactAcct Line
            if (line.transaction != null && !line.transaction.isCostCalculated()) {
              Map<String, String> parameters = getNotCalculatedCostParameters(line.transaction);
              setMessageResult(conn, STATUS_NotCalculatedCost, "error", parameters);
              throw new IllegalStateException();
            } else if (line.transaction == null) {
              // Check default cost existence
              HashMap<CostDimension, BaseOBObject> costDimensions = CostingUtils
                  .getEmptyDimensions();
              costDimensions.put(CostDimension.Warehouse, line.getWarehouse());
              if (!CostingUtils.hasStandardCostDefinition(product, legalEntity, dateAcct,
                  costDimensions)) {
                Map<String, String> parameters = getInvalidCostParameters(product.getIdentifier(),
                    DateAcct);
                setMessageResult(conn, STATUS_InvalidCost, "error", parameters);
                throw new IllegalStateException();
              }
            }
            costs = line.getProductCosts(DateAcct, as, conn, con);
            BigDecimal b_Costs = new BigDecimal(costs)
                .multiply(new BigDecimal(line.getBreakdownQty()))
                .divide(new BigDecimal(line.m_qty), costCurrency.getStandardPrecision().intValue(),
                    RoundingMode.HALF_UP);
            strCosts = b_Costs.toString();
          }
          if (!isConvertible(as, conn)) {
            setMessageResult(conn, STATUS_NotConvertible, "error", null);
            throw new IllegalStateException();
          }

          Account notInvoicedReceiptsAccount = getAccount(AcctServer.ACCTTYPE_NotInvoicedReceipts,
              as, conn);
          if (notInvoicedReceiptsAccount == null) {
            org.openbravo.model.financialmgmt.accounting.coa.AcctSchema schema = OBDal.getInstance()
                .get(org.openbravo.model.financialmgmt.accounting.coa.AcctSchema.class,
                    as.m_C_AcctSchema_ID);
            log4j.error("No Account Not Invoiced Receipts for product: " + product.getName()
                + " in accounting schema: " + schema.getName());
          }
          Account assetAccount = line.getAccount(ProductInfo.ACCTTYPE_P_Asset, as, conn);
          if (assetAccount == null) {
            org.openbravo.model.financialmgmt.accounting.coa.AcctSchema schema = OBDal.getInstance()
                .get(org.openbravo.model.financialmgmt.accounting.coa.AcctSchema.class,
                    as.m_C_AcctSchema_ID);
            log4j.error("No Account Asset for product: " + product.getName()
                + " in accounting schema: " + schema.getName());
          }
          // If there exists cost for the product, but it is equals to zero, then no line is added,
          // but no error is thrown. If this is the only line in the document, yes an error will be
          // thrown
          if (!costs.equals("0")
              || DocInOutData.existsCost(conn, DateAcct, line.m_M_Product_ID).equals("0")) {

            log4jDocInOut.debug("(matReceipt) - DR account: " + assetAccount);
            log4jDocInOut.debug("(matReceipt) - DR costs: " + strCosts);
            // Inventory DR
            dr = fact.createLine(line, assetAccount, costCurrency.getId(), strCosts, "",
                Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
            if (!getStatus().equals("i")) {
              if (dr != null) {
                dr.setM_Locator_ID(line.m_M_Locator_ID);
                dr.setLocationFromBPartner(C_BPartner_Location_ID, true, conn); // from
                // Loc
                dr.setLocationFromLocator(line.m_M_Locator_ID, false, conn); // to
                // Loc
              }
              log4jDocInOut.debug("(matReceipt) - CR account: "
                  + line.getAccount(AcctServer.ACCTTYPE_NotInvoicedReceipts, as, conn));
              log4jDocInOut.debug("(matReceipt) - CR costs: " + strCosts);
              // NotInvoicedReceipt CR
              cr = fact.createLine(line, notInvoicedReceiptsAccount, costCurrency.getId(), "",
                  strCosts, Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
              if (cr != null) {
                cr.setM_Locator_ID(line.m_M_Locator_ID);
                cr.setLocationFromBPartner(C_BPartner_Location_ID, true, conn); // from
                // Loc
                cr.setLocationFromLocator(line.m_M_Locator_ID, false, conn); // to
                // Loc
              }
            }
          }
        }
      } else {
        log4jDocInOut.warn("createFact - " + "DocumentType unknown: " + DocumentType);
        return null;
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    //
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
    log4jDocInOut.debug("DocInOut - oldSeqNo = " + oldSeqNo);
    BigDecimal seqNo = new BigDecimal(oldSeqNo);
    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
    log4jDocInOut.debug("DocInOut - nextSeqNo = " + SeqNo);
    return SeqNo;
  }

  /**
   * Get Document Confirmation
   * 
   * not used
   */
  @Override
  public boolean getDocumentConfirmation(ConnectionProvider conn, String strRecordId) {
    try {
      DocLineInOutData[] data = DocLineInOutData.select(conn, Record_ID);
      ShipmentInOut inOut = OBDal.getInstance().get(ShipmentInOut.class, strRecordId);
      int validLines = 0;
      for (int i = 0; i < data.length; i++) {
        BigDecimal trxCost = null;
        OBContext.setAdminMode(false);
        try {
          // Get related M_Transaction_ID
          ShipmentInOutLine inOutLine = OBDal.getInstance()
              .get(ShipmentInOutLine.class, data[i].mInoutlineId);
          if (inOutLine.getProduct() == null) {
            continue;
          }
          MaterialTransaction trx = null;
          if (inOutLine.getMaterialMgmtMaterialTransactionList().size() > 0) {
            trx = inOutLine.getMaterialMgmtMaterialTransactionList().get(0);
            trxCost = trx.getTransactionCost();
          } else {
            if (inOutLine.getProduct().isBookUsingPurchaseOrderPrice()) {
              // Not stocked item type product.
              // If the Product is checked as book using Purchase Order Price, the Price of the PO
              // will be used to create the FactAcct Line, therefore a related PO must exist
              OrderLine ol = inOutLine.getSalesOrderLine();
              if (ol == null) {
                Map<String, String> parameters = new HashMap<String, String>();
                parameters.put("product", inOutLine.getProduct().getIdentifier());
                parameters.put("line", inOutLine.getLineNo().toString());
                setMessageResult(conn, STATUS_NoRelatedPO, "error", parameters);
                throw new IllegalStateException();
              }
              trxCost = inOutLine.getMovementQuantity().multiply(ol.getUnitPrice());
            } else {
              // Not stocked item type product. Check standard cost existence.
              // If the Product is not checked as book using PO Price, the Cost of the
              // Transaction will be used to create the FactAcct Line, therefore the Cost of the
              // Transaction must have been calculated before.
              Organization legalEntity = OBContext.getOBContext()
                  .getOrganizationStructureProvider(AD_Client_ID)
                  .getLegalEntity(inOut.getOrganization());
              HashMap<CostDimension, BaseOBObject> costDimensions = CostingUtils
                  .getEmptyDimensions();
              if (inOutLine.getStorageBin() == null) {
                costDimensions.put(CostDimension.Warehouse,
                    inOutLine.getShipmentReceipt().getWarehouse());
              } else {
                costDimensions.put(CostDimension.Warehouse,
                    inOutLine.getStorageBin().getWarehouse());
              }
              if (!CostingUtils.hasStandardCostDefinition(inOutLine.getProduct(), legalEntity,
                  inOut.getAccountingDate(), costDimensions)) {
                Map<String, String> parameters = getInvalidCostParameters(
                    inOutLine.getProduct().getIdentifier(), DateAcct);
                setMessageResult(conn, STATUS_InvalidCost, "error", parameters);
                throw new IllegalStateException();
              } else {
                Currency currency = legalEntity.getCurrency() != null ? legalEntity.getCurrency()
                    : legalEntity.getClient().getCurrency();
                trxCost = CostingUtils
                    .getStandardCost(inOutLine.getProduct(), legalEntity, inOut.getAccountingDate(),
                        costDimensions, currency)
                    .multiply(new BigDecimal(data[i].breakdownqty));
              }
            }
          }
          if (trxCost == null) {
            Map<String, String> parameters = getNotCalculatedCostParameters(trx);
            setMessageResult(conn, STATUS_NotCalculatedCost, "error", parameters);
            setStatus(STATUS_NotCalculatedCost);
            return false;
          }
        } finally {
          OBContext.restorePreviousMode();
        }

        if (trxCost != null && trxCost.signum() != 0) {
          validLines++;
        }
      }
      if (validLines == 0) {
        setStatus(STATUS_DocumentDisabled);
        return false;
      }
    } catch (ServletException e) {
      log4j.error("Servlet Exception in document confirmation", e);
      return false;
    }
    return true;
  }

  @Override
  public String getServletInfo() {
    return "Servlet for the accounting";
  } // end of getServletInfo() method

}
