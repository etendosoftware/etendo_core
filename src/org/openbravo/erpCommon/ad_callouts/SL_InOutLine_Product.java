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
 * All portions are Copyright (C) 2001-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.common.plm.AttributeSet;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

public class SL_InOutLine_Product extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strIsSOTrx = Utility.getContext(this, info.vars, "isSOTrx", info.getWindowId());

    // Locator

    String strLocator = info.vars.getStringParameter("inpmProductId_LOC");

    if (strLocator.startsWith("\"")) {
      strLocator = strLocator.substring(1, strLocator.length() - 1);
    }

    if ("".equals(strLocator)) {
      if (strIsSOTrx.equals("Y")) {
        info.addResult("inpmLocatorId", "");
        info.addResult("inpmLocatorId_R", "");
      }
    } else {
      info.addResult("inpmLocatorId", strLocator);
      info.addResult("inpmLocatorId_R",
          SLInOutLineProductData.locator(this, strLocator, info.vars.getLanguage()));
    }

    // Attribute instance
    String strAttribute = info.vars.getStringParameter("inpmProductId_ATR");

    if (strAttribute.startsWith("\"")) {
      strAttribute = strAttribute.substring(1, strAttribute.length() - 1);
    }
    info.addResult("inpmAttributesetinstanceId", strAttribute);
    info.addResult("inpmAttributesetinstanceId_R",
        SLInOutLineProductData.attribute(this, strAttribute));

    // Attribute set

    String strMProductID = info.vars.getStringParameter("inpmProductId");
    String strAttrSet = "";
    String strAttrSetValueType = "";

    OBContext.setAdminMode();
    try {
      final Product product = OBDal.getInstance().get(Product.class, strMProductID);
      if (product != null) {
        AttributeSet attributeset = product.getAttributeSet();
        if (attributeset != null) {
          strAttrSet = product.getAttributeSet().toString();
        }
        strAttrSetValueType = product.getUseAttributeSetValueAs();
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    info.addResult("inpattributeset", strAttrSet);
    info.addResult("inpattrsetvaluetype", strAttrSetValueType);

    // Quantity order

    String strQtyOrder = info.vars.getNumericParameter("inpmProductId_PQTY");
    String strmInoutlineId = info.vars.getStringParameter("inpmInoutlineId");
    String strQty = info.vars.getNumericParameter("inpmProductId_QTY");
    String strHasSecondaryUOM = SLOrderProductData.hasSecondaryUOM(this, strMProductID);
    String strUOMProduct = info.vars.getStringParameter("inpmProductUomId");

    // This 'if' is used when the delivery note is created based in a
    // sale-order, to make it not ask for the quantity of the delivery-note
    // and to modify it with the quantity of product in the warehouse.
    // However, if the delivery-note doesn't come from an order, it modifies
    // the quantity field with the quantity in the warehouse.

    String fromOrder = SLInOutLineProductData.fromOrder(this, strmInoutlineId);
    boolean isUomManagementEnabled = UOMUtil.isUomManagementEnabled();
    if (fromOrder.equals("0")) {
      BigDecimal qtyOrder = StringUtils.isNotEmpty(strQtyOrder) ? new BigDecimal(strQtyOrder)
          : null;
      BigDecimal qty = StringUtils.isNotEmpty(strQty) ? new BigDecimal(strQty) : null;
      info.addResult("inpquantityorder", qtyOrder);
      info.addResult("inpmovementqty", qty);

      if (isUomManagementEnabled && productIsNotUsingSecondaryUom(strUOMProduct)) {
        // Set AUM based on default
        try {
          OBContext.setAdminMode();
          ShipmentInOut mInOut = OBDal.getInstance()
              .get(ShipmentInOut.class, info.vars.getStringParameter("inpmInoutId"));
          String finalAUM = UOMUtil.getDefaultAUMForDocument(strMProductID,
              mInOut.getDocumentType().getId());
          if (isValidUom(finalAUM)) {
            info.addResult("inpcAum", finalAUM);
            info.addResult("inpaumqty", UOMUtil.getConvertedAumQty(strMProductID, qty, finalAUM));
          }
        } finally {
          OBContext.restorePreviousMode();
        }
      }
    }

    // Secondary UOM
    info.addResult("inphasseconduom", Integer.parseInt(strHasSecondaryUOM));
    if (strHasSecondaryUOM.equals("1")
        && (!isUomManagementEnabled || (isUomManagementEnabled && !"".equals(strUOMProduct)))) {
      String strPUOM = info.vars.getStringParameter("inpmProductId_PUOM");

      if (strPUOM.startsWith("\"")) {
        strPUOM = strPUOM.substring(1, strPUOM.length() - 1);
      }
      FieldProvider[] tld = null;
      try {
        ComboTableData comboTableData = new ComboTableData(info.vars, this, "TABLE", "",
            "M_Product_UOM", "",
            Utility.getContext(this, info.vars, "#AccessibleOrgTree", "SLOrderProduct"),
            Utility.getContext(this, info.vars, "#User_Client", "SLOrderProduct"), 0);
        Utility.fillSQLParameters(this, info.vars, null, comboTableData, "SLOrderProduct", "");
        tld = comboTableData.select(false);
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }

      if (tld != null && tld.length > 0) {
        info.addSelect("inpmProductUomId");
        for (int i = 0; i < tld.length; i++) {
          info.addSelectResult(tld[i].getField("id"), tld[i].getField("name"),
              tld[i].getField("id").equalsIgnoreCase(strPUOM));
        }
        info.endSelect();
      } else {
        info.addResult("inpmProductUomId", "");
      }
    }

    // UOM

    info.addResult("inpcUomId", info.vars.getStringParameter("inpmProductId_UOM"));
  }

  private boolean isValidUom(final String finalAUM) {
    return finalAUM != null;
  }

  private boolean productIsNotUsingSecondaryUom(final String strUOMProduct) {
    return "".equals(strUOMProduct);
  }
}
