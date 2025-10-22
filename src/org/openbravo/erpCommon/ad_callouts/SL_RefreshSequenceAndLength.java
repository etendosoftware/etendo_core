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
 * All portions are Copyright (C) 2012-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import jakarta.servlet.ServletException;

import org.openbravo.base.filter.RequestFilter;
import org.openbravo.base.filter.ValueListFilter;

public class SL_RefreshSequenceAndLength extends SimpleCallout {

  private static final RequestFilter filterYesNo = new ValueListFilter("Y", "N", "");

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    String strIsBpNewAccount = info.getStringParameter("inpisbpnewaccount", filterYesNo);
    String strIsPrNewAccount = info.getStringParameter("inpisprnewaccount", filterYesNo);

    try {
      if (strIsBpNewAccount.equals("N")) {
        info.addResult("inpadSequenceBpId", "");
      }
      if (strIsPrNewAccount.equals("N")) {
        info.addResult("inpadSequencePrId", "");
      }
      if ((strIsBpNewAccount.equals("N")) && (strIsPrNewAccount.equals("N"))) {

        info.addResult("inpacctLength", null);
        info.addResult("inpsubacctLength", null);
      }
    } catch (Exception e) {
      log4j.error("Error in the callout org schema", e);
    }

  }
}
