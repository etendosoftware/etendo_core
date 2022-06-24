/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2016-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;

/**
 * 
 * Callout to convert from alternate quantity to base quantity
 *
 */
public class OperativeQuantity_To_BaseQuantity extends SimpleCallout {

  private static final Logger logger = LogManager.getLogger();

  private static final String ADWINDOW_SalesOrder = "143";
  private static final String ADWINDOW_PurchaseOrder = "181";
  private static final String ADWINDOW_GoodsShipment = "169";
  private static final String ADWINDOW_GoodsReceipt = "184";
  private static final String ADWINDOW_GoodsMovements = "170";
  private static final String ADWINDOW_SalesInvoice = "167";
  private static final String ADWINDOW_PurchaseInvoice = "183";
  private static final String ADWINDOW_Requisition = "800092";
  private static final String ADWINDOW_ManageRequisition = "1004400000";
  private static final String ADWINDOW_SalesQuotation = "6CB5B67ED33F47DFA334079D3EA2340E";
  private static final String ADTABLE_GoodsMovement = "323";

  /**
   * Converts a quantity from an alternate unit of measure to the base unit of the product
   */

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String windowId = info.getWindowId();
    BigDecimal qty = info.getBigDecimalParameter("inpaumqty");
    String strOperativeUOM = info.getStringParameter("inpcAum");
    String strBaseUOM = info.getStringParameter("inpcUomId");
    String productId = info.getStringParameter("inpmProductId");
    String tableId = "";
    try {
      OBContext.setAdminMode();
      if (UOMUtil.isUomManagementEnabled()) {
        if (strOperativeUOM == null || strOperativeUOM.isEmpty()) {
          qty = null;
        } else if (!strOperativeUOM.equals(strBaseUOM)) {
          qty = UOMUtil.getConvertedQty(productId, qty, strOperativeUOM);
        }
        Table table = getTableOfHeaderTabFromWindow(windowId);
        tableId = table.getId();
      }
    } catch (OBException e) {
      logger.error("Error while converting UOM. ", e);
      info.showError(e.getMessage());
      qty = null;
    } finally {
      OBContext.restorePreviousMode();
      if (windowId.equals(ADWINDOW_SalesOrder) || windowId.equals(ADWINDOW_PurchaseOrder)
          || windowId.equals(ADWINDOW_SalesQuotation)) {
        info.addResult("inpqtyordered", qty);
      } else if (windowId.equals(ADWINDOW_GoodsShipment) || windowId.equals(ADWINDOW_GoodsReceipt)
          || windowId.equals(ADWINDOW_GoodsMovements)
          || StringUtils.equals(ADTABLE_GoodsMovement, tableId)) {
        info.addResult("inpmovementqty", qty);
      } else if (windowId.equals(ADWINDOW_SalesInvoice)
          || windowId.equals(ADWINDOW_PurchaseInvoice)) {
        info.addResult("inpqtyinvoiced", qty);
      } else if (windowId.equals(ADWINDOW_Requisition)) {
        info.addResult("inpqty", qty);
      } else if (windowId.equals(ADWINDOW_ManageRequisition)) {
        info.addResult("inpqty", qty);
      }
    }
  }

  private Table getTableOfHeaderTabFromWindow(String windowId) {
    OBCriteria<Tab> obc = OBDal.getInstance().createCriteria(Tab.class);
    obc.add(Restrictions.eq(Tab.PROPERTY_WINDOW, OBDal.getInstance().get(Window.class, windowId)));
    obc.add(Restrictions.eq(Tab.PROPERTY_TABLEVEL, 0L));
    obc.addOrderBy(Tab.PROPERTY_SEQUENCENUMBER, true);
    Tab tab = (Tab) obc.list().get(0);
    return tab.getTable();
  }
}
