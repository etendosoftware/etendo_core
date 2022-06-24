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

public class SL_WRPhase_Sequence extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strMASequenceID = info.getStringParameter("inpmaSequenceId", IsIDFilter.instance);
    String strMAWReqID = info.getStringParameter("inpmaWorkrequirementId", IsIDFilter.instance);

    if (StringUtils.isNotEmpty(strMASequenceID)) {
      SLWRPhaseSequenceData[] data = SLWRPhaseSequenceData.select(this, strMASequenceID);
      // Update the activity from Sequence
      info.addResult("inpmaProcessId", data[0].process);
      // Update the quantity
      String strQuantity = SLWRPhaseSequenceData.selectQuantity(this, strMASequenceID, strMAWReqID);
      info.addResult("inpquantity", strQuantity);
      // Update cost center from Sequence
      info.addResult("inpcostcenteruse", FormatUtilities.replaceJS(data[0].ccuse));
      // Update Preparation Time from Sequence
      info.addResult("inppreptime", FormatUtilities.replaceJS(data[0].preptime));
      // Update if it is Outsourced from Sequence
      info.addResult("inpoutsourced", FormatUtilities.replaceJS(data[0].outsourced));
    }
  }
}
