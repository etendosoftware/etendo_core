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
 * All portions are Copyright (C) 2011-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.filter.NumberFilter;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.manufacturing.transaction.WorkRequirementOperation;

public class SL_ProductionPlan_WRPhase_Quantity extends SimpleCallout {

  private static final IsIDFilter idFilter = new IsIDFilter();
  private static final NumberFilter numFilter = new NumberFilter();

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    try {
      OBContext.setAdminMode(true);
      String strmWRPhase = info.getStringParameter("inpmaWrphaseId", idFilter);
      String strQty = info.getStringParameter("inpproductionqty", numFilter);

      if (StringUtils.isNotEmpty(strmWRPhase)) {
        WorkRequirementOperation wrPhase = OBDal.getInstance()
            .get(WorkRequirementOperation.class, strmWRPhase);
        BigDecimal wrPhaseEstTime = wrPhase.getEstimatedTime();
        BigDecimal wrPhaseQty = wrPhase.getQuantity();
        if (wrPhaseEstTime != null && wrPhaseQty != null
            && wrPhaseQty.compareTo(BigDecimal.ZERO) != 0) {
          BigDecimal qty = new BigDecimal(strQty);
          info.addResult("inpestimatedtime",
              wrPhaseEstTime.divide(wrPhaseQty).multiply(qty).toPlainString());
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
