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
 * All portions are Copyright (C) 2014-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;

public class SL_InvAmtUpd_AmtUnitCost extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    BigDecimal onHandQty = info.getBigDecimalParameter("inponhandqty");
    String orgId = info.getStringParameter("inpadOrgId", null);
    Organization organization = OBDal.getInstance().get(Organization.class, orgId);
    Currency currency = organization.getCurrency() != null ? organization.getCurrency()
        : organization.getClient().getCurrency();

    if (info.getLastFieldChanged().equalsIgnoreCase("inpinventoryAmount")) {
      BigDecimal invAmount = info.getBigDecimalParameter("inpinventoryAmount");
      info.addResult("inpunitcost",
          onHandQty.intValue() == 0 ? BigDecimal.ZERO
              : invAmount.divide(onHandQty, currency.getPricePrecision().intValue(),
                  RoundingMode.HALF_UP));
    } else if (info.getLastFieldChanged().equalsIgnoreCase("inpunitcost")) {
      BigDecimal unitCost = info.getBigDecimalParameter("inpunitcost");
      info.addResult("inpinventoryAmount", unitCost.multiply(onHandQty));
    }
  }
}
