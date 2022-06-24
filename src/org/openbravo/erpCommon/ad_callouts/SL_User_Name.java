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
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.utils.FormatUtilities;

public class SL_User_Name extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strFirstname = info.getStringParameter("inpfirstname");
    String strLastname = info.getStringParameter("inplastname");
    String strName = info.getStringParameter("inpname");
    String strUserName = info.getStringParameter("inpusername");

    // limits name and username to a maximum number of characters
    int maxChar = 60;

    if (StringUtils.isNotEmpty(strLastname)) {
      strLastname = " " + strLastname;
    }

    // do not change the name field, if the user just left it
    if (!StringUtils.equals(strChanged, "inpname")) {
      strName = FormatUtilities.replaceJS(strFirstname + strLastname);
      if (strName.length() > maxChar) {
        strName = strName.substring(0, maxChar);
      }
      info.addResult("inpname", strName);
    }

    if (StringUtils.isEmpty(strUserName)) {
      // if we have a name filled in use that for the username
      if (StringUtils.isNotEmpty(strName)) {
        strUserName = strName;
      }
      // else concatenate first- and lastname
      else {
        strUserName = strFirstname + strLastname;
      }
    }
    strUserName = FormatUtilities.replaceJS(strUserName);
    if (strUserName.length() > maxChar) {
      strUserName = strUserName.substring(0, maxChar);
    }
    info.addResult("inpusername", strUserName);

    // informs about characters cut
    if (FormatUtilities.replaceJS(strFirstname + strLastname).length() > maxChar) {
      info.showMessage(FormatUtilities
          .replaceJS(Utility.messageBD(this, "NameUsernameLengthCut", info.vars.getLanguage())));
    }
  }
}
