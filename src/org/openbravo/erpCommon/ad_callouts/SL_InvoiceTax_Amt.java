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

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;

public class SL_InvoiceTax_Amt extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String fieldChanged = info.getLastFieldChanged();
    log4j.debug("CHANGED: " + fieldChanged);

    // Parameters
    String taxId = info.getStringParameter("inpcTaxId", IsIDFilter.instance);
    String invoiceId = info.getStringParameter("inpcInvoiceId", IsIDFilter.instance);
    BigDecimal taxAmt = info.getBigDecimalParameter("inptaxamt");
    BigDecimal taxBaseAmt = info.getBigDecimalParameter("inptaxbaseamt");

    // Update Tax Amount and Tax Base Amount
    SLInvoiceTaxAmtData[] data = SLInvoiceTaxAmtData.select(this, taxId, invoiceId);
    BigDecimal taxRate = StringUtils.isEmpty(data[0].rate) ? BigDecimal.ONE
        : new BigDecimal(data[0].rate);
    Integer taxScale = Integer.valueOf(data[0].priceprecision);

    if (StringUtils.equals(fieldChanged, "inptaxamt")) {
      if (taxRate.compareTo(BigDecimal.ZERO) != 0) {
        taxBaseAmt = ((taxAmt.divide(taxRate, 12, RoundingMode.HALF_EVEN))
            .multiply(new BigDecimal("100"))).setScale(taxScale, RoundingMode.HALF_UP);
      }
    } else {
      taxAmt = ((taxBaseAmt.multiply(taxRate)).divide(new BigDecimal("100"), 12,
          RoundingMode.HALF_EVEN)).setScale(taxScale, RoundingMode.HALF_UP);
    }

    info.addResult("inptaxamt", taxAmt);
    info.addResult("inptaxbaseamt", taxBaseAmt);
  }
}
