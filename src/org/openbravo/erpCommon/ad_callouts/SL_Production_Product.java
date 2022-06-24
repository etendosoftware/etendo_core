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
 * All portions are Copyright (C) 2001-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.plm.Product;
import org.openbravo.utils.FormatUtilities;

public class SL_Production_Product extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strProduct = info.getStringParameter("inpmProductId", IsIDFilter.instance);
    String strPLocator = info.getStringParameter("inpmProductId_LOC", IsIDFilter.instance);
    String strPAttr = info.getStringParameter("inpmProductId_ATR", IsIDFilter.instance);
    String strPQty = info.vars.getNumericParameter("inpmProductId_PQTY");
    String strPUOM = info.getStringParameter("inpmProductId_PUOM", IsIDFilter.instance);
    String strQty = info.vars.getNumericParameter("inpmProductId_QTY");
    String strUOM = info.getStringParameter("inpmProductId_UOM", IsIDFilter.instance);

    // UOM, Locator
    info.addResult("inpcUomId", strUOM);
    info.addResult("inpmLocatorId", strPLocator);

    // Has Second UOM
    String strHasSecondaryUOM = SLOrderProductData.hasSecondaryUOM(this, strProduct);
    info.addResult("inphasseconduom", Integer.parseInt(strHasSecondaryUOM));

    // AttributeSetInstance, AttributeSet, AttributeSetValueType
    info.addResult("inpmAttributesetinstanceId", strPAttr);
    OBContext.setAdminMode();
    try {
      final Product product = OBDal.getInstance().get(Product.class, strProduct);
      if (product != null) {
        info.addResult("inpattributeset",
            product.getAttributeSet() != null ? product.getAttributeSet().getId() : "");
        info.addResult("inpattrsetvaluetype",
            FormatUtilities.replaceJS(product.getUseAttributeSetValueAs()));
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    // MovementQty, Quantity Order
    info.addResult("inpmovementqty", strQty);
    info.addResult("inpquantityorder", strPQty);

    // Product UOM List
    FieldProvider[] tld = null;
    try {
      ComboTableData comboTableData = new ComboTableData(info.vars, this, "TABLE", "",
          "M_Product_UOM", "",
          Utility.getContext(this, info.vars, "#AccessibleOrgTree", "SLProductionProduct"),
          Utility.getContext(this, info.vars, "#User_Client", "SLProductionProduct"), 0);
      Utility.fillSQLParameters(this, info.vars, null, comboTableData, "SLProductionProduct", "");
      tld = comboTableData.select(false);
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    if (tld != null && tld.length > 0) {
      info.addSelect("inpmProductUomId");
      for (int i = 0; i < tld.length; i++) {
        info.addSelectResult(tld[i].getField("id"),
            FormatUtilities.replaceJS(tld[i].getField("name")),
            StringUtils.equals(tld[i].getField("id"), strPUOM));
      }
      info.endSelect();
    } else {
      info.addResult("inpmProductUomId", "");
    }

  }
}
