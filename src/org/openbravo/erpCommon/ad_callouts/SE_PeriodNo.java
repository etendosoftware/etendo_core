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

public class SE_PeriodNo extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strOrgId = info.getStringParameter("inpadOrgId", IsIDFilter.instance);
    String strCalendarId = info.getStringParameter("inpcCalendarId", IsIDFilter.instance);
    String strYearId = info.getStringParameter("inpcYearId", IsIDFilter.instance);

    try {

      // Update the Periods
      if (StringUtils.equals(strChanged, "inpcYearId") && StringUtils.isNotEmpty(strYearId)) {
        SEPeriodNoData[] tdv = null;
        tdv = SEPeriodNoData.getPeriodNo(this, strYearId);
        if (tdv != null && tdv.length > 0) {
          info.addSelect("inpperiodno");
          for (int i = 0; i < tdv.length; i++) {
            info.addSelectResult(tdv[i].getField("id"), tdv[i].getField("Name"));
          }
          info.endSelect();
        }
      }

      else if (StringUtils.isNotEmpty(strOrgId)) {

        // Update the Calendar
        SEPeriodNoData[] tdv = null;
        tdv = SEPeriodNoData.getCalendar(this, strOrgId);
        if (tdv != null && tdv.length > 0) {
          info.addSelect("inpcCalendarId");
          for (int i = 0; i < tdv.length; i++) {
            info.addSelectResult(tdv[i].getField("id"), tdv[i].getField("Name"));
            strCalendarId = tdv[i].getField("id");
          }
          info.endSelect();
        }

        // Update the years
        tdv = SEPeriodNoData.getYears(this, strCalendarId);
        String strLastYear = "";
        if (tdv != null && tdv.length > 0) {
          info.addSelect("inpcYearId");
          for (int i = 0; i < tdv.length; i++) {
            info.addSelectResult(tdv[i].getField("id"), tdv[i].getField("Name"), (i == 0));
            if (i == 0) {
              strLastYear = tdv[i].getField("id");
            }
          }
          info.endSelect();
        }

        // Update the Periods
        tdv = SEPeriodNoData.getPeriodNo(this, strLastYear);
        if (tdv != null && tdv.length > 0) {
          info.addSelect("inpperiodno");
          for (int i = 0; i < tdv.length; i++) {
            info.addSelectResult(tdv[i].getField("id"), tdv[i].getField("Name"));
          }
          info.endSelect();
        }

      }
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
  }

}
