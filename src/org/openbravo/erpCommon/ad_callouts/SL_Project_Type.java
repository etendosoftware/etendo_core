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
 * All portions are Copyright (C) 2009-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.Utility;

public class SL_Project_Type extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String fieldChanged = info.getLastFieldChanged();
    log4j.debug("CHANGED: " + fieldChanged);

    // Parameters
    String projectTypeID = info.getStringParameter("inpcProjecttypeId", IsIDFilter.instance);
    String orgId = info.getStringParameter("inpadOrgId", IsIDFilter.instance);
    String windowId = info.getWindowId();

    if (StringUtils.isNotEmpty(projectTypeID)) {
      ComboTableData comboTableData = null;
      boolean isRelatedProjectType = false;
      FieldProvider[] data = null;
      try {
        comboTableData = new ComboTableData(info.vars, this, "19", "C_ProjectType_ID", "", "",
            Utility.getReferenceableOrg(info.vars, orgId),
            Utility.getContext(this, info.vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        data = comboTableData.select(false);
      } catch (Exception ex) {
        throw new ServletException(ex);
      }
      if (data != null && data.length > 0) {
        for (FieldProvider fp : data) {
          if (StringUtils.equalsIgnoreCase(fp.getField("ID").trim(), projectTypeID)) {
            isRelatedProjectType = true;
            break;
          }
        }
      }
      if (!isRelatedProjectType && StringUtils.equals(windowId, "130")) {
        info.showMessage(Utility.messageBD(this, "ProjectTypeNull", info.vars.getLanguage()));
      }
    }
  }
}
