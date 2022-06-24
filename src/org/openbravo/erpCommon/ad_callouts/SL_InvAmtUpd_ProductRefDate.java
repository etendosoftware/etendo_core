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
 * All portions are Copyright (C) 2014-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.costing.CostAdjustmentUtils;
import org.openbravo.costing.CostingAlgorithm.CostDimension;
import org.openbravo.costing.CostingUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.cost.CostingRule;

public class SL_InvAmtUpd_ProductRefDate extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    try {
      Date referenceDate;
      BigDecimal currentValuedStock = BigDecimal.ZERO;
      BigDecimal currentStock = BigDecimal.ZERO;

      String warehouseId = info.getStringParameter("inpmWarehouseId", null);
      String isWarehouseDimension = info.getStringParameter("isWarehouseDimension");
      String productId = info.getStringParameter("inpmProductId", null);
      Product product = OBDal.getInstance().get(Product.class, productId);
      String orgId = info.getStringParameter("inpadOrgId", null);
      Organization organization = OBDal.getInstance().get(Organization.class, orgId);
      Currency currency = organization.getCurrency() != null ? organization.getCurrency()
          : organization.getClient().getCurrency();
      String dateFormat = OBPropertiesProvider.getInstance()
          .getOpenbravoProperties()
          .getProperty("dateTimeFormat.java");
      SimpleDateFormat outputFormat = new SimpleDateFormat(dateFormat);

      referenceDate = outputFormat.parse(info.getStringParameter("inpreferencedate", null));
      boolean isProductionProduct = product != null && product.isProduction();
      if (StringUtils.equals(isWarehouseDimension, "N") || isProductionProduct) {
        warehouseId = null;
        info.addResult("inpmWarehouseId", "");
      }

      if (product == null) {
        return;
      }

      CostingRule costRule = CostingUtils.getCostDimensionRule(organization, referenceDate);
      HashMap<CostDimension, BaseOBObject> costDimensions = CostingUtils.getEmptyDimensions();
      if (costRule.isWarehouseDimension() && StringUtils.isNotEmpty(warehouseId)) {
        Warehouse warehouse = OBDal.getInstance().get(Warehouse.class, warehouseId);
        costDimensions.put(CostDimension.Warehouse, warehouse);
      }
      currentValuedStock = CostAdjustmentUtils.getValuedStockOnMovementDate(product, organization,
          referenceDate, costDimensions, currency, costRule.isBackdatedTransactionsFixed());
      currentStock = CostAdjustmentUtils.getStockOnMovementDate(product, organization,
          referenceDate, costDimensions, costRule.isBackdatedTransactionsFixed());
      info.addResult("inpcurInventoryAmount", currentValuedStock);
      info.addResult("inponhandqty", currentStock);
      info.addResult("inpcurUnitcost",
          currentStock.intValue() == 0 ? BigDecimal.ZERO
              : currentValuedStock.divide(currentStock, currency.getPricePrecision().intValue(),
                  RoundingMode.HALF_UP));
    } catch (ParseException ignore) {
    }
  }
}
