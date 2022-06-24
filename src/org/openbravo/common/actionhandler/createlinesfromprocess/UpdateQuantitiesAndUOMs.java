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
 * All portions are Copyright (C) 2018-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.actionhandler.createlinesfromprocess;

import java.math.BigDecimal;

import javax.enterprise.context.Dependent;

import org.apache.commons.lang.StringUtils;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductUOM;
import org.openbravo.model.common.uom.UOM;

@Dependent
@Qualifier(CreateLinesFromProcessHook.CREATE_LINES_FROM_PROCESS_HOOK_QUALIFIER)
class UpdateQuantitiesAndUOMs extends CreateLinesFromProcessHook {

  @Override
  public int getOrder() {
    return -40;
  }

  /**
   * Calculation of quantities and UOM-AUM Support
   */
  @Override
  public void exec() {
    BigDecimal orderedQuantity = CreateLinesFromUtil.getOrderedQuantity(getPickExecJSONObject());
    BigDecimal operativeQuantity = CreateLinesFromUtil
        .getOperativeQuantity(getPickExecJSONObject());
    UOM operativeUOM = CreateLinesFromUtil.getAUM(getPickExecJSONObject());
    ProductUOM orderUOM = (ProductUOM) getCopiedFromLine().get("orderUOM");
    Product product = (Product) getCopiedFromLine().get("product");

    if (uomManagementIsEnabledAndAUMAndOrderUOMAreEmpty()) {
      String defaultAum = UOMUtil.getDefaultAUMForDocument(product.getId(),
          getInvoice().getTransactionDocument().getId());
      operativeQuantity = orderedQuantity;
      operativeUOM = OBDal.getInstance().getProxy(UOM.class, defaultAum);
      orderUOM = null;
      if (aUMIsDifferentThanUOM(defaultAum)) {
        operativeQuantity = UOMUtil.getConvertedAumQty(product.getId(), orderedQuantity,
            operativeUOM.getId());
      }
    }

    getInvoiceLine().setInvoicedQuantity(orderedQuantity);
    getInvoiceLine().setUOM((UOM) getCopiedFromLine().get("uOM"));
    if (UOMUtil.isUomManagementEnabled()) {
      getInvoiceLine().setOperativeQuantity(operativeQuantity);
      getInvoiceLine().setOperativeUOM(operativeUOM);
    }
    getInvoiceLine()
        .setOrderQuantity(CreateLinesFromUtil.getOrderQuantity(getPickExecJSONObject()));
    getInvoiceLine().setOrderUOM(orderUOM);
  }

  private boolean uomManagementIsEnabledAndAUMAndOrderUOMAreEmpty() {
    boolean isUomManagementEnabled = UOMUtil.isUomManagementEnabled();
    UOM operativeUOM = (UOM) getCopiedFromLine().get("operativeUOM");
    ProductUOM orderUOM = (ProductUOM) getCopiedFromLine().get("orderUOM");

    return isUomManagementEnabled && orderUOM == null && operativeUOM == null;
  }

  private boolean aUMIsDifferentThanUOM(final String defaultAum) {
    UOM uOM = (UOM) getCopiedFromLine().get("uOM");
    return !StringUtils.equals(defaultAum, uOM.getId());
  }

}
