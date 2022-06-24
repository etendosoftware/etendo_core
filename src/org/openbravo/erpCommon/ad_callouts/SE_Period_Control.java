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
import org.openbravo.base.filter.ValueListFilter;

public class SE_Period_Control extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strAdOrgTypeID = info.getStringParameter("inpadOrgtypeId", IsIDFilter.instance);
    String strIsReady = info.getStringParameter("inpisready", new ValueListFilter("Y", "N"));

    String strIsBULE = SEPeriodControlData.select(this, strAdOrgTypeID);
    info.addResult("inpisperiodcontrolallowedAux", strIsBULE);

    if (StringUtils.isEmpty(strIsBULE) && StringUtils.equals(strIsReady, "N")) {
      info.addResult("inpisperiodcontrolallowed", "N");
    }

    String strIsLegalEntity = SEPeriodControlData.selectIsLegalEntity(this, strAdOrgTypeID);
    String strIsBusinessUnit = SEPeriodControlData.selectIsBusinessUnit(this, strAdOrgTypeID);
    info.addResult("inpisorglegalentiy", strIsLegalEntity);
    info.addResult("inpisorgbusinessunit", strIsBusinessUnit);
  }
}
