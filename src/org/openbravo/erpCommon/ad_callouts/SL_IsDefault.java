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
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.Sqlc;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.ui.Tab;

/**
 * This callout checks if this is the only active isDefault checked for the table (with
 * organization) or, in case it is a tab with parent, it is the only checked taking into account its
 * parent.
 * 
 * If another one already exists an error message is raised and the checkbox is unchecked.
 * 
 */
public class SL_IsDefault extends SimpleCallout {
  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    String strValue = info.getStringParameter(strChanged);

    if (StringUtils.equals(strValue, "Y")) {

      // Parameters
      String strTableId = info.getStringParameter("inpTableId", IsIDFilter.instance);
      String strOrg = info.getStringParameter("inpadOrgId", IsIDFilter.instance);
      String parentColumn = info.getStringParameter("inpParentKeyColumn");
      String currentColumnKey = info.getStringParameter("inpkeyColumnId");
      String currentKeyValue = info.getStringParameter(info.getStringParameter("inpKeyName"));

      // if parentColumn is null, compute parentColumn using tab information
      if ((StringUtils.isEmpty(parentColumn) && StringUtils.isNotEmpty(info.getTabId()))) {
        Tab currentTab = OBDal.getInstance().get(Tab.class, info.getTabId());
        parentColumn = KernelUtils.getInstance().getParentColumnName(currentTab);
      }

      SLIsDefaultData[] data = SLIsDefaultData.select(this, strTableId);
      if (data != null && data.length > 0) {
        String parentClause = "";
        String currentClause = "";
        // Include parent column if it exists
        if (StringUtils.isNotEmpty(parentColumn)) {
          String parentValue = info.getStringParameter(
              "inp" + Sqlc.TransformaNombreColumna(parentColumn), IsIDFilter.instance);
          if (StringUtils.isNotEmpty(parentValue)) {
            parentClause = "AND " + parentColumn + "='" + parentValue + "'";
          }
        }

        // In case the current record already exists in DB not sum it to
        // the total
        if (StringUtils.isNotEmpty(currentKeyValue)) {
          currentClause = "AND " + currentColumnKey + " != '" + currentKeyValue + "'";
        }

        String strTotalDefaults = SLIsDefaultData.selectHasDefaults(this, data[0].tablename,
            parentClause, currentClause, strOrg);
        if (!StringUtils.equals(strTotalDefaults, "0")) {
          info.showError(Utility.messageBD(this, "DuplicatedDefaults", info.vars.getLanguage()));
          info.addResult("inpisdefault", "N");
        }
      }
    }
  }
}
