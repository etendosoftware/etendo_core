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
 * All portions are Copyright (C) 2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.filter.RegexFilter;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.Utility;

public class SL_AdvPayment_Document extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    VariablesSecureApp vars = info.vars;
    String strWindowNo = info.getWindowId();
    String strTableNameId = info.getStringParameter("inpkeyColumnId",
        new RegexFilter("[a-zA-Z0-9_]*_ID"));
    String strDocType_Id = info.getStringParameter("inpcDoctypeId", IsIDFilter.instance);
    String strTableName = strTableNameId.substring(0, strTableNameId.length() - 3);
    String strDocumentNo = Utility.getDocumentNo(this, vars, strWindowNo, strTableName,
        strDocType_Id, strDocType_Id, false, false);
    info.addResult("inpdocumentno", "<" + strDocumentNo + ">");
  }

}
