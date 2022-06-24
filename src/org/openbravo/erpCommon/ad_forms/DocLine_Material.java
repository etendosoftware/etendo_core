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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;

public class DocLine_Material extends DocLine {
  static Logger log4jDocLine_Material = LogManager.getLogger();

  /**
   * Constructor
   * 
   * @param DocumentType
   *          document type
   * @param TrxHeader_ID
   *          trx header id
   * @param TrxLine_ID
   *          trx line id
   */
  public DocLine_Material(String DocumentType, String TrxHeader_ID, String TrxLine_ID) {
    super(DocumentType, TrxHeader_ID, TrxLine_ID);
  }

  /** Locator */
  public String m_M_Locator_ID = "";
  public String m_M_LocatorTo_ID = "";
  public String m_M_Warehouse_ID = "";
  /** Production */
  public String m_Productiontype = "";
  public MaterialTransaction transaction = null;
  public String m_breakdownqty = "";

  /**
   * Set Trasaction Quantity and Storage Qty
   * 
   * @param qty
   *          qty
   */
  public void setQty(String qty, ConnectionProvider conn) {
    log4jDocLine_Material.debug(" setQty - qty= " + qty);
    super.setQty(qty); // save TrxQty
    p_productInfo.setQty(qty, p_productInfo.m_C_UOM_ID, conn);
    log4jDocLine_Material.debug(" setQty - productInfo.qty = " + p_productInfo.m_qty);
  } // setQty

  private String getQty() {
    return m_qty;
  }

  public void setTransaction(MaterialTransaction transaction) {
    this.transaction = transaction;
  }

  public Warehouse getWarehouse() {
    return OBDal.getInstance().get(Warehouse.class, m_M_Warehouse_ID);
  }

  /**
   * Get Total Product Costs. If exists a transaction retrieves the cost from it, otherwise calls
   * the
   * {@link ProductInfo#getProductDefaultCosts(String, BigDecimal, Organization, Warehouse, Currency)}
   * 
   * @param date
   *          String with the accounting date used in case there is no material transaction.
   * @param as
   */

  public String getProductCosts(String date, AcctSchema as, ConnectionProvider conn,
      Connection con) {
    if (transaction != null && transaction.getTransactionCost() != null) {
      BigDecimal sign = new BigDecimal(new BigDecimal(getQty()).signum());
      return transaction.getTransactionCost().multiply(sign).toString();
    } else if (transaction != null) {
      return "";
    } else {
      // If there isn't any material transaction get the default cost of the product.
      try {
        Organization legalEntity = OBContext.getOBContext()
            .getOrganizationStructureProvider(p_productInfo.m_AD_Client_ID)
            .getLegalEntity(OBDal.getInstance().get(Organization.class, m_AD_Org_ID));
        return p_productInfo.getProductDefaultCosts(date, null, legalEntity, getWarehouse(),
            legalEntity.getCurrency() != null ? legalEntity.getCurrency()
                : legalEntity.getClient().getCurrency());
      } catch (OBException e) {
        log4jDocLine_Material.error("No standard cost found for product: "
            + OBDal.getInstance().get(Product.class, m_M_Product_ID).getIdentifier()
            + " DocumentType: " + p_DocumentType + " record id: " + m_TrxHeader_ID);
        return "";
      }
    }
  } // getProductCosts

  /**
   * Line Account from Product
   * 
   * @param AcctType
   *          see ProoductInfo.ACCTTYPE_* (0..3)
   * @param as
   *          accounting schema
   * @return Requested Product Account
   */
  public Account getAccount(String AcctType, AcctSchema as, ConnectionProvider conn) {
    return p_productInfo.getAccount(AcctType, as, conn);
  } // getAccount

  public String getBreakdownQty() {
    return m_breakdownqty;
  }

  public void setBreakdownQty(String breakdownqty) {
    this.m_breakdownqty = breakdownqty;
  }

  @Override
  public String getServletInfo() {
    return "Servlet for the accounting";
  } // end of getServletInfo() method
}
