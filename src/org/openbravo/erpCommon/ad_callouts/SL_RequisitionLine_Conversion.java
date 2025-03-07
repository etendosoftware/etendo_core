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
import java.math.RoundingMode;

import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.utils.FormatUtilities;

public class SL_RequisitionLine_Conversion extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strUOM = info.getStringParameter("inpcUomId", IsIDFilter.instance);
    String strMProductUOMID = info.getStringParameter("inpmProductUomId", IsIDFilter.instance);
    String strQuantityOrder = info.vars.getNumericParameter("inpquantityorder");

    // Multiple Rate for UOM conversion if any
    String strInitUOM = SLRequisitionLineConversionData.initUOMId(this, strMProductUOMID);
    String strMultiplyRate = StringUtils.equals(strInitUOM, strUOM) ? "1" : "";
    if (StringUtils.isEmpty(strMultiplyRate)) {
      strMultiplyRate = SLRequisitionLineConversionData.multiplyRate(this, strInitUOM, strUOM);
    }
    if (StringUtils.isEmpty(strMultiplyRate)) {
      strMultiplyRate = SLRequisitionLineConversionData.divideRate(this, strUOM, strInitUOM);
    }
    if (StringUtils.isEmpty(strMultiplyRate)) {
      strMultiplyRate = "1";

      // Set message when no UOM Conversion exists
      if (StringUtils.isNotEmpty(strMProductUOMID)) {
        info.showMessage(FormatUtilities
            .replaceJS(Utility.messageBD(this, "NoUOMConversion", info.vars.getLanguage())));
      }
    }

    // Calculate Quantity based on UOM Conversion
    if (StringUtils.isNotEmpty(strQuantityOrder)) {
      BigDecimal multiplyRate = new BigDecimal(strMultiplyRate);
      BigDecimal quantityOrder = new BigDecimal(strQuantityOrder);
      BigDecimal movementQty = quantityOrder.multiply(multiplyRate);
      int stdPrecision = Integer
          .valueOf(SLRequisitionLineConversionData.stdPrecision(this, strUOM));
      if (movementQty.scale() > stdPrecision) {
        movementQty = movementQty.setScale(stdPrecision, RoundingMode.HALF_UP);
      }
      info.addResult("inpqty", movementQty);
    }
  }
}
