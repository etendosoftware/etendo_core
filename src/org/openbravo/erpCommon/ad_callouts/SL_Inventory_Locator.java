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
 * All portions are Copyright (C) 2009-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import java.math.BigDecimal;

import jakarta.servlet.ServletException;

public class SL_Inventory_Locator extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strProduct = info.vars.getStringParameter("inpmProductId");
    String strLocator = info.vars.getStringParameter("inpmLocatorId");
    String strAttribute = info.vars.getStringParameter("inpmAttributesetinstanceId");
    String strUOM = info.vars.getStringParameter("inpcUomId");
    String strSecUOM = info.vars.getStringParameter("inpmProductUomId");

    if (strProduct.startsWith("\"")) {
      strProduct = strProduct.substring(1, strProduct.length() - 1);
    }

    if (!strProduct.equals("")) {

      SLInventoryLocatorData[] data = SLInventoryLocatorData.select(this, strProduct, strLocator,
          strUOM, strSecUOM, ((strSecUOM == null || strSecUOM.equals("")) ? "productuom" : ""),
          ((strAttribute == null || strAttribute.equals("")) ? null : strAttribute));
      if (data == null || data.length == 0) {
        data = SLInventoryLocatorData.set();
        data[0].qty = "0";
        data[0].qtyorder = "0";
      }

      BigDecimal qtyOrder = new BigDecimal(data[0].qtyorder);
      BigDecimal qty = new BigDecimal(data[0].qty);
      info.addResult("inpquantityorderbook", qtyOrder);
      info.addResult("inpqtycount", qty);
      info.addResult("inpqtybook", qty);
    }
  }
}
