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
import org.openbravo.erpCommon.utility.Utility;

public class SE_Order_Project extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strProjectId = info.getStringParameter("inpcProjectId", IsIDFilter.instance);
    String strIsSOTrx = Utility.getContext(this, info.vars, "isSOTrx", info.getWindowId());

    if (StringUtils.equals(strIsSOTrx, "Y")) {
      SEOrderProjectData[] data = SEOrderProjectData.select(this, strProjectId);
      if (data != null && data.length > 0) {
        // Payment Rule
        String strPaymentRule = data[0].paymentrule;
        if (StringUtils.isNotEmpty(strPaymentRule)) {
          info.addResult("inppaymentrule", strPaymentRule);
        }
        // Payment Term
        String strPaymentTerm = data[0].paymentterm;
        if (StringUtils.isNotEmpty(strPaymentTerm)) {
          info.addResult("inpcPaymenttermId", strPaymentTerm);
        }
      }
    }
  }
}
