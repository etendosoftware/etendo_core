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
import org.openbravo.erpCommon.calloutsSequence.CalloutSequence;

import java.util.HashMap;

public class SE_InOut_DocType extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameter
    String strDocType = info.getStringParameter("inpcDoctypeId", IsIDFilter.instance);

    // Movement Type and Document No.
    SEInOutDocTypeData[] data = SEInOutDocTypeData.select(this, strDocType);
    if (data != null && data.length > 0) {
      if (StringUtils.equals(data[0].docbasetype, "MMS")) {
        info.addResult("inpmovementtype", "C-");
      } else if (StringUtils.equals(data[0].docbasetype, "MMR")) {
        info.addResult("inpmovementtype", "V+");
      } else {
        info.addResult("inpmovementtype", null);
      }
      if (StringUtils.equals(data[0].isdocnocontrolled, "Y")) {
        HashMap<String, Object> values = new HashMap<>();
        values.put("currentNext", data[0].currentnext);
        var inOutSequenceAction = CalloutSequence.getInstance().getSE_InOut_SequenceAction().get();
        if (inOutSequenceAction != null) {
          inOutSequenceAction.get_SE_InOut_inpdocumentnoValue(info, values);
        }
      }
    }

  }
}
