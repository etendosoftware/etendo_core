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
 * All portions are Copyright (C) 2014-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import java.math.BigDecimal;

import javax.servlet.ServletException;

public class SE_Trxtype_Transaction extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    try {
      final String strTrxType = info.getStringParameter("inptrxtype", null);
      String strWindowId = info.getStringParameter("inpwindowId", null);
      if ("BF".equals(strTrxType)) {
        info.addResult("inpfinPaymentId", "");
        info.addResult("inpcGlitemId", "");
        info.addResult("inpissotrx", "Y");
        info.vars.setSessionValue(strWindowId + "|IsSOTrx", "Y");
      } else if ("BPW".equals(strTrxType)) {
        info.addResult("inpfinPaymentId", "");
        info.addResult("inpdepositamt", BigDecimal.ZERO);
        info.addResult("inpissotrx", "N");
        info.vars.setSessionValue(strWindowId + "|IsSOTrx", "N");
      } else if ("BPD".equals(strTrxType)) {
        info.addResult("inpfinPaymentId", "");
        info.addResult("inppaymentamt", BigDecimal.ZERO);
        info.addResult("inpissotrx", "Y");
        info.vars.setSessionValue(strWindowId + "|IsSOTrx", "Y");
      }
    } catch (Exception e) {
      return;
    }
  }
}
