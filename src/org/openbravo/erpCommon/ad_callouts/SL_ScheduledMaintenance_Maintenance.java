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
import org.openbravo.utils.FormatUtilities;

public class SL_ScheduledMaintenance_Maintenance extends SimpleCallout {
  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameter
    String strMaintenance = info.getStringParameter("inpmaMaintenanceId", IsIDFilter.instance);

    // Schedule Maintenance Data
    if (StringUtils.isNotEmpty(strMaintenance)) {
      SLScheduledMaintenanceMaintenanceData[] data = SLScheduledMaintenanceMaintenanceData
          .select(this, strMaintenance);

      // Set Session value for IsSOTrx
      if (StringUtils.isEmpty(data[0].maMachineId)) {
        info.vars.setSessionValue(info.getWindowId() + "|IsSOTrx", "N");
      } else {
        info.vars.setSessionValue(info.getWindowId() + "|IsSOTrx", "Y");
      }

      // Maintenance Operation, Type, Machine Type, Machine
      info.addResult("inpmaMaintOperationId", data[0].maMaintOperationId);
      info.addResult("inpmaintenanceType", FormatUtilities.replaceJS(data[0].maintenanceType));
      info.addResult("inpmaMachineTypeId", data[0].maMachineTypeId);
      info.addResult("inpmaMachineId", data[0].maMachineId);
    }
  }
}
