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

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.costing.CostingServer;
import org.openbravo.costing.CostingServer.TrxType;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;

public class DocLine_CostAdjustment extends DocLine {
  private static final Logger log4jDocLine_CostAdjustment = LogManager.getLogger();

  public DocLine_CostAdjustment(String DocumentType, String TrxHeader_ID, String TrxLine_ID) {
    super(DocumentType, TrxHeader_ID, TrxLine_ID);
  }

  /** Transaction Type */
  private String transactionType;
  private TrxType trxType;
  private String trxAmt;
  private boolean transactionNegative;
  private String warehouseId;
  private String sourceProcess;
  private boolean isSource;
  private boolean isUnitCost;

  public static final String TRXTYPE_SHIPMENT = "SMT";
  public static final String TRXTYPE_RECEIPT = "RCT";
  public static final String TRXTYPE_INVENTORY = "INV";
  public static final String TRXTYPE_INTERNALMOVEMENTFROM = "INTMOVF";
  public static final String TRXTYPE_INTERNALMOVEMENTTO = "INTMOVT";
  public static final String TRXTYPE_INTERNALCONSUMPTION = "INTCONS";
  public static final String TRXTYPE_BOM = "BOM";
  public static final String TRXTYPE_MANUFACTURING = "MAN";

  public void setSourceProcess(String p_sourceProcess) {
    sourceProcess = p_sourceProcess;
  }

  public String getSourceProcess() {
    return sourceProcess;
  }

  public void setIsSource(boolean p_isSource) {
    isSource = p_isSource;
  }

  public boolean getIsSource() {
    return isSource;
  }

  public void setIsUnitCost(boolean isUnitCost) {
    this.isUnitCost = isUnitCost;
  }

  public boolean isUnitCost() {
    return isUnitCost;
  }

  public void setTrxType(String transactionId) {
    MaterialTransaction transaction = OBDal.getInstance()
        .get(MaterialTransaction.class, transactionId);
    TrxType trxTypeLocal = TrxType.getTrxType(transaction);
    setTransactionType(trxTypeLocal);
    this.trxType = trxTypeLocal;
  }

  public CostingServer.TrxType getTrxType() {
    return trxType;
  }

  public void setTransactionType(TrxType trxType) {
    switch (trxType) {
      case Shipment:
        transactionType = TRXTYPE_SHIPMENT;
        transactionNegative = false;
        break;
      case ShipmentReturn:
        transactionType = TRXTYPE_SHIPMENT;
        transactionNegative = true;
        break;
      case ShipmentVoid:
        transactionType = TRXTYPE_SHIPMENT;
        transactionNegative = true;
        break;
      case ShipmentNegative:
        transactionType = TRXTYPE_SHIPMENT;
        transactionNegative = true;
        break;
      case Receipt:
        transactionType = TRXTYPE_RECEIPT;
        transactionNegative = false;
        break;
      case ReceiptReturn:
        transactionType = TRXTYPE_RECEIPT;
        transactionNegative = true;
        break;
      case ReceiptVoid:
        transactionType = TRXTYPE_RECEIPT;
        transactionNegative = true;
        break;
      case ReceiptNegative:
        transactionType = TRXTYPE_RECEIPT;
        transactionNegative = true;
        break;
      case InventoryDecrease:
        transactionType = TRXTYPE_INVENTORY;
        transactionNegative = true;
        break;
      case InventoryIncrease:
        transactionType = TRXTYPE_INVENTORY;
        transactionNegative = false;
        break;
      case InventoryOpening:
        transactionType = TRXTYPE_INVENTORY;
        transactionNegative = false;
        break;
      case InventoryClosing:
        transactionType = TRXTYPE_INVENTORY;
        transactionNegative = false;
        break;
      case IntMovementFrom:
        transactionType = TRXTYPE_INTERNALMOVEMENTFROM;
        transactionNegative = true;
        break;
      case IntMovementTo:
        transactionType = TRXTYPE_INTERNALMOVEMENTTO;
        transactionNegative = false;
        break;
      case InternalCons:
        transactionType = TRXTYPE_INTERNALCONSUMPTION;
        transactionNegative = false;
        break;
      case InternalConsNegative:
        transactionType = TRXTYPE_INTERNALCONSUMPTION;
        transactionNegative = true;
        break;
      case InternalConsVoid:
        transactionType = TRXTYPE_INTERNALCONSUMPTION;
        transactionNegative = true;
        break;
      case BOMPart:
        transactionType = TRXTYPE_BOM;
        transactionNegative = true;
        break;
      case BOMProduct:
        transactionType = TRXTYPE_BOM;
        transactionNegative = false;
        break;
      case ManufacturingConsumed:
        transactionType = TRXTYPE_MANUFACTURING;
        transactionNegative = true;
        break;
      case ManufacturingProduced:
        transactionType = TRXTYPE_MANUFACTURING;
        transactionNegative = false;
        break;
      case Unknown:
        throw new OBException("@UnknownTrxType@: " + trxType);
      default:
        throw new OBException("@UnknownTrxType@: " + trxType);
    }
  }

  public String getTransactionType() {
    return transactionType;
  }

  @Override
  public void setAmount(String amt) {
    trxAmt = amt;
  } // setAmounts

  @Override
  public String getAmount() {
    return trxAmt;
  } // setAmounts

  public void setWarehouseId(String warehouse) {
    warehouseId = warehouse;
  } // setAmounts

  public String getWarehouseId() {
    return warehouseId;
  } // setAmounts

  public boolean isTransactionNegative() {
    return transactionNegative;
  } // setAmounts

  /**
   * Line Account from Product (or Charge).
   * 
   * @param AcctType
   *          see ProoductInfo.ACCTTYPE_* (0..3)
   * @param as
   *          Accounting schema
   * @return Requested Product Account
   */
  public Account getAccount(String AcctType, AcctSchema as, ConnectionProvider conn) {
    // Charge Account
    if (m_M_Product_ID.equals("") && !m_C_Charge_ID.equals("")) {
      BigDecimal amt = new BigDecimal(-1); // Revenue (-)
      if (p_DocumentType.indexOf("AP") != -1) {
        amt = new BigDecimal(+1); // Expense (+)
      }
      Account acct = getChargeAccount(as, amt, conn);
      if (acct != null) {
        return acct;
      }
    }
    // GL Item directly from Invoice Line
    else if (m_M_Product_ID.equals("") && !m_C_Glitem_ID.equals("")) {
      try {
        DocLineInvoiceData[] data = null;
        data = DocLineInvoiceData.selectGlitem(conn, m_C_Glitem_ID, as.getC_AcctSchema_ID());
        String Account_ID = "";
        if (data == null || data.length == 0) {
          return null;
        }
        if (data.length > 0) {
          switch (Integer.parseInt(AcctType)) {
            case 1:
              // It is similar to ProductInfo.ACCTTYPE_P_Revenue
              Account_ID = data[0].glitemCreditAcct;
              break;
            case 2:
              // It is similar to ProductInfo.ACCTTYPE_P_Expense
              Account_ID = data[0].glitemDebitAcct;
              break;
          }
        }
        // No account
        if (Account_ID.equals("")) {
          log4jDocLine_CostAdjustment
              .warn("getAccount - NO account for m_C_Glitem_ID=" + m_C_Glitem_ID);
          return null;
        }
        // Return Account
        return Account.getAccount(conn, Account_ID);

      } catch (ServletException e) {
        log4jDocLine_CostAdjustment.warn(e);
      }
    }
    // Product Account
    return p_productInfo.getAccount(AcctType, as, conn);
  } // getAccount

  @Override
  public String getServletInfo() {
    return "Servlet for the accounting";
  } // end of getServletInfo() method
}
