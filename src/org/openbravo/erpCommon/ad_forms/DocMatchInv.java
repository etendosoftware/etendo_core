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
 * Contributions are Copyright (C) 2001-2020 Openbravo S.L.U.
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
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.procurement.ReceiptInvoiceMatch;

public class DocMatchInv extends AcctServer {

  private static final long serialVersionUID = 1L;
  private static final Logger log4jDocMatchInv = LogManager.getLogger();

  /** AD_Table_ID */
  private String seqNum = "0";
  private DocLine[] pinOutlines = new DocLine[0];

  /**
   * Constructor
   * 
   * @param adClientId
   *          AD_Client_ID
   */
  public DocMatchInv(String adClientId, String adOrgId, ConnectionProvider connectionProvider) {
    super(adClientId, adOrgId, connectionProvider);
  }

  @Override
  public void loadObjectFieldProvider(ConnectionProvider conn, String adClientId, String id)
      throws ServletException {
    setObjectFieldProvider(DocMatchInvData.selectRegistro(conn, adClientId, id));
  }

  /**
   * Load Document Details
   * 
   * @return true if loadDocumentType was set
   */
  @Override
  public boolean loadDocumentDetails(FieldProvider[] data, ConnectionProvider conn) {
    C_Currency_ID = NO_CURRENCY;
    DocumentType = AcctServer.DOCTYPE_MatMatchInv;
    log4jDocMatchInv.debug("loadDocumentDetails - C_Currency_ID : {}", C_Currency_ID);
    DateDoc = data[0].getField("DateTrx");
    C_BPartner_ID = data[0].getField("C_Bpartner_Id");

    loadDocumentType(); // lines require doc type
    // Contained Objects
    p_lines = loadLines(conn, data[0].getField("C_InvoiceLine_Id"));
    pinOutlines = loadInOutLines(conn, data[0].getField("M_InOutLine_Id"));
    return true;
  } // loadDocumentDetails

  /**
   * Load Invoice Line
   * 
   * @return DocLine Array
   */
  public DocLine[] loadLines(ConnectionProvider conn, String strCInvoiceLineId) {
    ArrayList<Object> list = new ArrayList<Object>();
    DocMatchInvData[] data = null;
    try {
      log4jDocMatchInv.debug("############### groupLines = {}", groupLines);
      if (groupLines.equals("Y")) {
        data = DocMatchInvData.selectInvoiceLineTotal(connectionProvider, strCInvoiceLineId);
      } else {
        data = DocMatchInvData.selectInvoiceLine(connectionProvider, strCInvoiceLineId);
      }
    } catch (ServletException e) {
      log4jDocMatchInv.warn(e);
    }
    if (data == null || data.length == 0) {
      return null;
    }
    for (int i = 0; i < data.length; i++) {
      DocLine_Invoice docLine = new DocLine_Invoice(DocumentType, Record_ID, strCInvoiceLineId);
      docLine.loadAttributes(data[i], this);
      OBContext.setAdminMode(false);
      try {
        Invoice invoice = OBDal.getInstance().get(Invoice.class, data[i].cInvoiceId);
        docLine.m_C_Currency_ID = invoice.getCurrency().getId();
      } finally {
        OBContext.restorePreviousMode();
      }
      String strQty = data[i].qtyinvoiced;
      docLine.setQty(strQty);
      String lineNetAmt = data[i].linenetamt;
      String priceList = data[i].pricelist;
      docLine.setAmount(lineNetAmt, priceList, strQty);

      list.add(docLine);
    }
    // Return Array
    DocLine[] dl = new DocLine[list.size()];
    list.toArray(dl);
    return dl;
  } // loadLines

  /**
   * Load Invoice Line
   * 
   * @return DocLine Array
   */
  public DocLine[] loadInOutLines(ConnectionProvider conn, String strInOutLineId) {
    ArrayList<Object> list = new ArrayList<Object>();
    DocMatchInvData[] data = null;
    try {
      log4jDocMatchInv.debug("############### groupLines = {}", groupLines);
      data = DocMatchInvData.selectInOutLineTotal(connectionProvider, strInOutLineId);
    } catch (ServletException e) {
      log4jDocMatchInv.warn(e);
    }
    if (data == null || data.length == 0) {
      return new DocLine[0];
    }

    try {
      OBContext.setAdminMode(false);
      for (int i = 0; i < data.length; i++) {
        DocLine_Material docLine = new DocLine_Material(DocumentType, Record_ID, strInOutLineId);
        docLine.loadAttributes(data[i], this);
        String strQty = data[i].movementqty;
        docLine.setQty(strQty);
        list.add(docLine);
      }
    } finally {
      OBContext.restorePreviousMode();
    } // Return Array
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
    return ZERO;
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
        DocMatchInvTemplate newTemplate = (DocMatchInvTemplate) Class.forName(strClassname)
            .getDeclaredConstructor()
            .newInstance();
        return newTemplate.createFact(this, as, conn, con, vars);
      } catch (Exception e) {
        log4j.error("Error while creating new instance for DocMatchInvTemplate - ", e);
      }
    }
    C_Currency_ID = as.getC_Currency_ID();
    // create Fact Header
    Fact fact = new Fact(this, as, Fact.POST_Actual);
    String factAcctGroupId = SequenceIdData.getUUID();
    // Line pointers
    FactLine dr = null;
    FactLine cr = null;
    FactLine diff = null;

    // Entry to build has the form:
    // Account......................................Debit.......................... Credit
    // Not Invoiced Receipts........... Cost in the goods receipt
    // Expenses......................................................... Expenses in the Invoice
    // Invoice Price Variance........ Difference of cost and expenses

    FieldProvider[] data = getObjectFieldProvider();
    ShipmentInOutLine inOutLine = getShipmentLine(Record_ID);
    Currency costCurrency = FinancialUtils
        .getLegalEntityCurrency(OBDal.getInstance().get(Organization.class, AD_Org_ID));
    BigDecimal bdCost;
    BigDecimal trxCost = null;
    if (inOutLine.getProduct().isBookUsingPurchaseOrderPrice()) {
      // If the Product is checked as book using PO Price, the Price of the Purchase Order will
      // be used to create the FactAcct Line
      OrderLine ol = inOutLine.getSalesOrderLine();
      if (ol == null) {
        setMessageResult(conn, STATUS_NoRelatedPO, "error", null);
        throw new IllegalStateException();
      }
      costCurrency = ol.getCurrency();
      Long scale = costCurrency.getStandardPrecision();
      BigDecimal bdQty = new BigDecimal(data[0].getField("Qty"));
      bdCost = ol.getUnitPrice().multiply(bdQty).setScale(scale.intValue(), RoundingMode.HALF_UP);
    } else {
      // If the Product is not checked as book using PO Price, the Cost of the
      // Transaction will be used to create the FactAcct Line
      MaterialTransaction transaction = getTransaction(Record_ID);
      Organization legalEntity = OBContext.getOBContext()
          .getOrganizationStructureProvider(AD_Client_ID)
          .getLegalEntity(inOutLine.getShipmentReceipt().getOrganization());
      HashMap<CostDimension, BaseOBObject> costDimensions = CostingUtils.getEmptyDimensions();
      if (inOutLine.getStorageBin() == null) {
        costDimensions.put(CostDimension.Warehouse, inOutLine.getShipmentReceipt().getWarehouse());
      } else {
        costDimensions.put(CostDimension.Warehouse, inOutLine.getStorageBin().getWarehouse());
      }
      if (transaction == null) {
        if (!CostingUtils.hasStandardCostDefinition(inOutLine.getProduct(), legalEntity,
            inOutLine.getShipmentReceipt().getAccountingDate(), costDimensions)) {
          Map<String, String> parameters = new HashMap<String, String>();
          parameters.put("Product", inOutLine.getProduct().getIdentifier());
          parameters.put("Date", (OBDateUtils.formatDate(
              OBDal.getInstance().get(ReceiptInvoiceMatch.class, Record_ID).getTransactionDate())));
          setMessageResult(conn, STATUS_InvalidCost, "error", parameters);
          throw new IllegalStateException();

        } else {
          Currency currency = legalEntity.getCurrency() != null ? legalEntity.getCurrency()
              : legalEntity.getClient().getCurrency();
          trxCost = CostingUtils
              .getStandardCost(inOutLine.getProduct(), legalEntity,
                  inOutLine.getShipmentReceipt().getAccountingDate(), costDimensions, currency)
              .multiply(new BigDecimal(data[0].getField("Qty")));
        }
      } else {
        trxCost = transaction.getTransactionCost();
      }
      if (transaction != null && transaction.getCurrency() != null) {
        costCurrency = transaction.getCurrency();
      }
      if (transaction != null && !transaction.isCostCalculated()) {
        Map<String, String> parameters = getNotCalculatedCostParameters(transaction);
        setMessageResult(conn, STATUS_NotCalculatedCost, "error", parameters);
        throw new IllegalStateException();
      }

      // Cost is retrieved from the transaction and if it does not exist It calls the old way
      // The precision of the divide is set to 10 because the rounding is needed to avoid
      // exceptions.
      // The rounding itself is not needed because it is done some lines later.
      bdCost = trxCost.divide(transaction == null ? new BigDecimal(data[0].getField("Qty")).abs()
          : transaction.getMovementQuantity().abs(), 10, RoundingMode.HALF_UP);
      Long scale = costCurrency.getStandardPrecision();
      BigDecimal bdQty = new BigDecimal(data[0].getField("Qty"));
      bdCost = bdCost.multiply(bdQty).setScale(scale.intValue(), RoundingMode.HALF_UP);
    }

    DocMatchInvData[] invoiceData = DocMatchInvData.selectInvoiceData(conn, vars.getClient(),
        data[0].getField("C_InvoiceLine_Id"));

    String strExpenses = invoiceData[0].linenetamt;
    String strInvoiceCurrency = invoiceData[0].cCurrencyId;
    String strIsSOTrx = invoiceData[0].issotrx;
    String strRecordId = invoiceData[0].cInvoiceId;
    String strReceiptDate = data[0].getField("orderAcctDate");
    int invoiceCurrencyPrecission = 2;
    try {
      OBContext.setAdminMode();
      invoiceCurrencyPrecission = OBDal.getInstance()
          .get(Currency.class, strInvoiceCurrency)
          .getStandardPrecision()
          .intValue();
    } catch (Exception ignore) {
      log4j.error("Error when getting std precision from invoice currency..", ignore);
    } finally {
      OBContext.restorePreviousMode();
    }

    BigDecimal bdExpenses = new BigDecimal(strExpenses)
        .multiply(new BigDecimal(data[0].getField("QTY")))
        .divide(new BigDecimal(data[0].getField("QTYINVOICED")), RoundingMode.HALF_UP);

    DocLine docLine = new DocLine(DocumentType, Record_ID, "");
    docLine.m_C_Project_ID = data[0].getField("INOUTPROJECT");
    docLine.m_User1_ID = data[0].getField("INOUTUSER1");
    docLine.m_User2_ID = data[0].getField("INOUTUSER2");
    docLine.m_C_Campaign_ID = data[0].getField("INOUTCAMPAING");
    docLine.m_C_Activity_ID = data[0].getField("INOUTACTIVITY");
    docLine.m_C_Costcenter_ID = data[0].getField("INOUTCOSCENTER");

    if (ZERO.compareTo(bdCost) == 0) {
      strMessage = "@MatchedInvIsZero@";
      setStatus(STATUS_DocumentDisabled);
    }
    BigDecimal totalInOutLines = BigDecimal.ZERO;
    for (int i = 0; i < pinOutlines.length; i++) {
      DocLine_Material line = (DocLine_Material) pinOutlines[i];
      BigDecimal lineAmount = bdCost.multiply(new BigDecimal(line.m_qty))
          .divide(new BigDecimal(data[0].getField("MOVEMENTQTY")),
              costCurrency.getStandardPrecision().intValue(), RoundingMode.HALF_UP);
      if (i == pinOutlines.length - 1) {
        lineAmount = bdCost.subtract(totalInOutLines);
      }
      dr = fact.createLine(line, getAccount(AcctServer.ACCTTYPE_NotInvoicedReceipts, as, conn),
          costCurrency.getId(), lineAmount.toString(), "0", factAcctGroupId, nextSeqNo(seqNum),
          DocumentType, strReceiptDate, conn);
      totalInOutLines = totalInOutLines.add(lineAmount);
    }

    if (dr == null) {
      log4j.warn("createFact - unable to calculate line with "
          + " cost of the product to not invoiced receipt account.");
      return null;
    }

    ProductInfo p = new ProductInfo(data[0].getField("M_Product_Id"), conn);
    BigDecimal totalLines = BigDecimal.ZERO;
    for (int i = 0; i < p_lines.length; i++) {
      DocLine_Invoice line = (DocLine_Invoice) p_lines[i];
      BigDecimal lineAmount;
      BigDecimal linenetamt = new BigDecimal(invoiceData[0].linenetamt);
      if (linenetamt.compareTo(BigDecimal.ZERO) != 0) {
        lineAmount = bdExpenses.multiply(new BigDecimal(line.getAmount()))
            .divide(linenetamt, invoiceCurrencyPrecission, RoundingMode.HALF_UP);
        if (i == p_lines.length - 1) {
          lineAmount = bdExpenses.subtract(totalLines);
        }
        cr = fact.createLine(line, p.getAccount(ProductInfo.ACCTTYPE_P_Expense, as, conn),
            strInvoiceCurrency, "0", lineAmount.toString(), factAcctGroupId, nextSeqNo(seqNum),
            DocumentType, DateAcct, conn);
        totalLines = totalLines.add(lineAmount);
      }
    }
    if (cr == null && ZERO.compareTo(bdExpenses) != 0) {
      log4j.warn(
          "createFact - unable to calculate line with " + " expenses to product expenses account.");
      return null;
    }
    // Set Locations
    FactLine[] fLines = fact.getLines();
    for (int i = 0; fLines != null && i < fLines.length; i++) {
      if (fLines[i] != null) {
        fLines[i].setLocationFromBPartner(C_BPartner_Location_ID, true, conn); // from Loc
        fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(conn), false, conn); // to Loc
      }
    }
    updateProductInfo(as.getC_AcctSchema_ID(), conn, con); // only API

    bdExpenses = convertAmount(bdExpenses, strIsSOTrx.equalsIgnoreCase("Y"), DateAcct,
        TABLEID_Invoice, strRecordId, strInvoiceCurrency, as.m_C_Currency_ID, docLine, as, fact,
        factAcctGroupId, seqNum, conn, false);

    BigDecimal bdExpensesAcctCurrency = new BigDecimal(
        getConvertedAmt(bdExpenses.toString(), strInvoiceCurrency, as.m_C_Currency_ID, DateAcct, "",
            vars.getClient(), vars.getOrg(), conn));
    // Calculate Difference amount in schema currency
    bdCost = new BigDecimal(getConvertedAmt(bdCost.toString(), costCurrency.getId(),
        as.m_C_Currency_ID, strReceiptDate, "", vars.getClient(), vars.getOrg(), conn));
    BigDecimal bdDifference = bdExpensesAcctCurrency.subtract(bdCost);
    if (bdDifference.signum() != 0) {
      BigDecimal totalDiffLines = BigDecimal.ZERO;
      for (int i = 0; i < pinOutlines.length; i++) {
        DocLine_Material line = (DocLine_Material) pinOutlines[i];
        BigDecimal lineAmount = bdDifference.multiply(new BigDecimal(line.m_qty))
            .divide(new BigDecimal(data[0].getField("MOVEMENTQTY")),
                OBDal.getInstance()
                    .get(Currency.class, as.m_C_Currency_ID)
                    .getStandardPrecision()
                    .intValue(),
                RoundingMode.HALF_UP);
        if (i == pinOutlines.length - 1) {
          lineAmount = bdDifference.subtract(totalDiffLines);
        }
        diff = fact.createLine(line, p.getAccount(ProductInfo.ACCTTYPE_P_IPV, as, conn),
            as.m_C_Currency_ID,
            (lineAmount.compareTo(BigDecimal.ZERO) > 0) ? lineAmount.abs().toString() : "0",
            (lineAmount.compareTo(BigDecimal.ZERO) < 1) ? lineAmount.abs().toString() : "0",
            factAcctGroupId, nextSeqNo(seqNum), DocumentType, conn);
        totalDiffLines = totalDiffLines.add(lineAmount);
        if (diff == null) {
          log4j.warn("createFact - unable to calculate line with "
              + " difference to InvoicePriceVariant account.");
          return null;
        }
      }
    }
    seqNum = "0";
    return fact;
  } // createFact

  /**
   * @return the seqNo
   */
  public String getSeqNo() {
    return seqNum;
  }

  /**
   * @param seqNo
   *          the seqNo to set
   */
  public void setSeqNo(String seqNo) {
    seqNum = seqNo;
  }

  /**
   * @return the serialVersionUID
   */
  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public String nextSeqNo(String oldSeqNo) {
    log4jDocMatchInv.debug("DocMatchInv - oldSeqNo = {}", oldSeqNo);
    BigDecimal seqNo = new BigDecimal(oldSeqNo);
    seqNum = (seqNo.add(new BigDecimal("10"))).toString();
    log4jDocMatchInv.debug("DocMatchInv - nextSeqNo = {}", seqNum);
    return seqNum;
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
   * Update Product Info. - Costing (PriceLastInv) - PO (PriceLastInv)
   * 
   * @param C_AcctSchema_ID
   *          accounting schema
   */
  public void updateProductInfo(String C_AcctSchema_ID, ConnectionProvider conn, Connection con) {
    log4jDocMatchInv.debug("updateProductInfo - C_Invoice_ID={}", this.Record_ID);

    /**
     * @todo Last.. would need to compare document/last updated date would need to maintain
     *       LastPriceUpdateDate on _PO and _Costing
     */

    // update Product PO info
    // should only be once, but here for every AcctSchema
    // ignores multiple lines with same product - just uses first
    int no = 0;
    try {
      no = DocInvoiceData.updateProductPO(con, conn, Record_ID);
      log4jDocMatchInv.debug("M_Product_PO - Updated={}", no);

    } catch (ServletException e) {
      log4jDocMatchInv.warn(e);
    }
  } // updateProductInfo

  private MaterialTransaction getTransaction(String matchInvId) {
    MaterialTransaction transaction;
    try {
      OBContext.setAdminMode(false);
      if (OBDal.getInstance()
          .get(ReceiptInvoiceMatch.class, matchInvId)
          .getGoodsShipmentLine()
          .getMaterialMgmtMaterialTransactionList()
          .isEmpty()) {
        return null;
      }
      transaction = OBDal.getInstance()
          .get(ReceiptInvoiceMatch.class, matchInvId)
          .getGoodsShipmentLine()
          .getMaterialMgmtMaterialTransactionList()
          .get(0);
    } finally {
      OBContext.restorePreviousMode();
    }
    return transaction;
  }

  private ShipmentInOutLine getShipmentLine(String matchInvId) {
    OBContext.setAdminMode(false);
    ShipmentInOutLine shipmentLine;
    try {
      shipmentLine = OBDal.getInstance()
          .get(ReceiptInvoiceMatch.class, matchInvId)
          .getGoodsShipmentLine();
    } finally {
      OBContext.restorePreviousMode();
    }
    return shipmentLine;
  }

  @Override
  public String getServletInfo() {
    return "Servlet for the accounting";
  } // end of getServletInfo() method
}
