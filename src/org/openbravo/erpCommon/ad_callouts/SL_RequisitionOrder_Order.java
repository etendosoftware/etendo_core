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
 * All portions are Copyright (C) 2008-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import java.math.BigDecimal;

import jakarta.servlet.ServletException;

import org.openbravo.base.filter.IsIDFilter;

public class SL_RequisitionOrder_Order extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strOrderLineId = info.getStringParameter("inpcOrderlineId", IsIDFilter.instance);
    String strRequisitionLineId = info.getStringParameter("inpmRequisitionlineId",
        IsIDFilter.instance);

    // Set Quantity
    BigDecimal qtyRequired = new BigDecimal(
        SLRequisitionOrderOrderData.getRequired(this, strRequisitionLineId));
    BigDecimal qtyOrderLine = new BigDecimal(
        SLRequisitionOrderOrderData.getOrderLine(this, strOrderLineId));
    BigDecimal reqOrder = qtyRequired.compareTo(qtyOrderLine) > 0 ? qtyOrderLine : qtyRequired;
    info.addResult("inpqty", reqOrder);
  }
}
