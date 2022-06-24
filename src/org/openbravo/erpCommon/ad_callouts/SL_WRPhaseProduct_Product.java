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
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.Utility;

public class SL_WRPhaseProduct_Product extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strMProductID = info.getStringParameter("inpmProductId", IsIDFilter.instance);

    // Update product UOM
    String strProductUom = SLWRPhaseProductProductData.selectProductUom(this, strMProductID);
    info.addResult("inpcUomId", strProductUom);

    // Update if it has secondary UOM
    String strHasSecondaryUOM = SLWRPhaseProductProductData.hasSecondaryUOM(this, strMProductID);
    info.addResult("inphasseconduom", Integer.parseInt(strHasSecondaryUOM));

    // Update Product UOMs
    String strmProductUOMId = SLWRPhaseProductProductData.strMProductUOMID(this, strMProductID,
        strProductUom);
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
            StringUtils.equalsIgnoreCase(tld[i].getField("id"), strmProductUOMId));
      }
      info.endSelect();
    }
  }
}
