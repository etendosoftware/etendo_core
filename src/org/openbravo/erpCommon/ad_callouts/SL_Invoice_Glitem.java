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

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.erpCommon.businessUtility.Tax;
import org.openbravo.erpCommon.utility.Utility;

public class SL_Invoice_Glitem extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strAccountID = info.getStringParameter("inpaccountId", IsIDFilter.instance);
    String strADOrgID = info.getStringParameter("inpadOrgId", IsIDFilter.instance);
    String strCInvoiceID = info.getStringParameter("inpcInvoiceId", IsIDFilter.instance);
    String strIsSOTrx = Utility.getContext(this, info.vars, "isSOTrx", info.getWindowId());
    String strWarehouse = Utility.getContext(this, info.vars, "#M_Warehouse_ID",
        info.getWindowId());

    if (StringUtils.equalsIgnoreCase(strChanged, "inpaccountId")) {
      // Tax
      SLInvoiceTaxData[] data = SLInvoiceTaxData.select(this, strCInvoiceID);
      if (data != null && data.length > 0) {
        try {
          String strCTaxID = Tax.get(this, null, data[0].dateinvoiced, strADOrgID, strWarehouse,
              data[0].cBpartnerLocationId, data[0].cBpartnerLocationId, data[0].cProjectId,
              strIsSOTrx, strAccountID);
          info.addResult("inpcTaxId", strCTaxID);
        } catch (IOException e) {
          log4j.error(e.getMessage());
        }
      }
    }
  }
}
