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
 * All portions are Copyright (C) 2008-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;

public class SE_Expense_BP_Project extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strProjectId = info.getStringParameter("inpcProjectId", IsIDFilter.instance);
    String strBPartnerId = info.getStringParameter("inpcBpartnerId", IsIDFilter.instance);

    // If project changed
    if (StringUtils.equals(strChanged, "inpcProjectId")) {
      // Reset Project Phase and Project Task fields
      info.addResult("inpcProjectphaseId", "");
      info.addResult("inpcProjecttaskId", "");

      // If project changed, select project's business partner (if any).
      if (StringUtils.isNotEmpty(strProjectId)) {
        String strBPartner = SEExpenseBPProjectData.selectBPId(this, strProjectId);
        if (StringUtils.isNotEmpty(strBPartner)) {
          strBPartnerId = strBPartner;
          String strBPartnerName = SEExpenseBPProjectData.selectBPName(this, strProjectId);
          info.addResult("inpcBpartnerId", strBPartnerId);
          info.addResult("inpcBpartnerId_R", strBPartnerName);
        }
      }
    }

    // If business partner changed
    else if (StringUtils.equals(strChanged, "inpcBpartnerId")) {
      if (StringUtils.isNotEmpty(strBPartnerId)) {
        // If project is not null, check if it corresponds with the business partner
        if (StringUtils.isNotEmpty(strProjectId)) {
          String strBPartnerProject = SEExpenseBPProjectData.selectBPProject(this, strBPartnerId,
              strProjectId);
          // If there is no relationship between project and business partner, take the last project
          // of that business partner
          if (StringUtils.isEmpty(strBPartnerProject)) {
            strProjectId = SEExpenseBPProjectData.selectProjectId(this, strBPartnerId);
            info.addResult("inpcProjectId", strProjectId);
          }
        }
        // If project is null, take the last project of that business partner (if any).
        else {
          strProjectId = SEExpenseBPProjectData.selectProjectId(this, strBPartnerId);
          info.addResult("inpcProjectId", strProjectId);
          info.addResult("inpcProjectphaseId", "");
          info.addResult("inpcProjecttaskId", "");
        }
      }
    }
  }
}
