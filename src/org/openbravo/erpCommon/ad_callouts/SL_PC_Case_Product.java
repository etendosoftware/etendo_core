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
 * All portions are Copyright (C) 2010-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import jakarta.servlet.ServletException;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.utils.FormatUtilities;

public class SL_PC_Case_Product extends SimpleCallout {
  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameter
    String strMProductID = info.getStringParameter("inpmProductId", IsIDFilter.instance);
    String strPAttr = info.getStringParameter("inpmProductId_ATR", IsIDFilter.instance);

    // AttributeSetInstance, AttributeSet, AttributeSetValueType
    info.addResult("inpmAttributesetinstanceId", strPAttr);
    OBContext.setAdminMode();
    try {
      final Product product = OBDal.getInstance().get(Product.class, strMProductID);
      if (product != null) {
        info.addResult("inpattributeset",
            product.getAttributeSet() != null ? product.getAttributeSet().getId() : "");
        info.addResult("inpattrsetvaluetype",
            FormatUtilities.replaceJS(product.getUseAttributeSetValueAs()));
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
