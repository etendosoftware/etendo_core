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
 * All portions are Copyright (C) 2015-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.materialmgmt;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.onhandquantity.StockProposed;
import org.openbravo.service.db.DalConnectionProvider;

public class StockUtils {
  /*
   * Calls getStock with Available, Nettable and OverIssue = null
   */
  public static CSResponseGetStockParam getStock(String uuid, String recordId, BigDecimal quantity,
      String mProductId, String mLocatorId, String mWarehouseId, String priorityWarehouseId,
      String adOrgId, String mAttributeSetInstanceId, String adUserId, String adClientId,
      String warehouseRuleId, String cUomId, String productUomId, String adTableId, String auxId,
      Long lineNo, String processId, String mReservationId, String calledFromApp)
      throws ServletException, NoConnectionAvailableException {
    return getStock(uuid, recordId, quantity, mProductId, mLocatorId, mWarehouseId,
        priorityWarehouseId, adOrgId, mAttributeSetInstanceId, adUserId, adClientId,
        warehouseRuleId, cUomId, productUomId, adTableId, auxId, lineNo, processId, mReservationId,
        calledFromApp, "Y", null, null);
  }

  /*
   * Calls M_GET_STOCK_PARAM1 and retrieves result in a CSResponseGetStockParam object. Records will
   * be created in M_STOCK_PROPOSAL with AD_PINSTASNCE_ID = uuid (parameter).
   */
  public static CSResponseGetStockParam getStock(String uuid, String recordId, BigDecimal quantity,
      String mProductId, String mLocatorId, String mWarehouseId, String priorityWarehouseId,
      String adOrgId, String mAttributeSetInstanceId, String adUserId, String adClientId,
      String warehouseRuleId, String cUomId, String productUomId, String adTableId, String auxId,
      Long lineNo, String processId, String mReservationId, String calledFromApp, String available,
      String nettable, String overIssue) throws ServletException, NoConnectionAvailableException {
    return StockUtilsData.getStock(OBDal.getInstance().getConnection(true),
        new DalConnectionProvider(true), uuid, recordId,
        quantity != null ? quantity.toString() : null, mProductId, mLocatorId, mWarehouseId,
        priorityWarehouseId, adOrgId, mAttributeSetInstanceId, adUserId, adClientId,
        warehouseRuleId, cUomId, productUomId, adTableId, auxId,
        lineNo != null ? lineNo.toString() : null, processId, mReservationId, calledFromApp,
        available, nettable, overIssue);
  }

  /*
   * Gets the StockProposed as an ScrollableResults for the order line product, the given warehouse
   * and quantity
   */
  public static ScrollableResults getStockProposed(OrderLine orderLine, BigDecimal quantity,
      final Warehouse warehouse) {
    try {
      getStock(orderLine.getId(), orderLine.getId(), quantity, orderLine.getProduct().getId(), null,
          null, warehouse.getId(), orderLine.getOrganization().getId(),
          orderLine.getAttributeSetValue() != null ? orderLine.getAttributeSetValue().getId()
              : null,
          OBContext.getOBContext().getUser().getId(), orderLine.getClient().getId(),
          orderLine.getWarehouseRule() != null ? orderLine.getWarehouseRule().getId() : null,
          orderLine.getUOM().getId(), null, null, null, null, null, null, "N");
    } catch (ServletException | NoConnectionAvailableException e) {
      throw new OBException("Error getting stock: OrderlineID: " + orderLine.getId(), e);
    }
    OBCriteria<StockProposed> stockProposed = OBDal.getInstance()
        .createCriteria(StockProposed.class);
    stockProposed.add(Restrictions.eq(StockProposed.PROPERTY_PROCESSINSTANCE, orderLine.getId()));
    stockProposed.addOrderBy(StockProposed.PROPERTY_PRIORITY, true);
    return stockProposed.scroll(ScrollMode.FORWARD_ONLY);
  }
}
