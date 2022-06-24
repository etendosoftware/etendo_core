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
 * All portions are Copyright (C) 2010-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.erpCommon.businessUtility.PAttributeSet;
import org.openbravo.erpCommon.businessUtility.PAttributeSetData;
import org.openbravo.utils.FormatUtilities;

public class SL_Asset_Product extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strMProductID = info.getStringParameter("inpmProductId", IsIDFilter.instance);

    // Get the product attribute data
    PAttributeSetData[] dataPAttr = PAttributeSetData.selectProductAttr(this, strMProductID);
    if (dataPAttr != null && dataPAttr.length > 0
        && StringUtils.equals(dataPAttr[0].attrsetvaluetype, "D")) {

      // If the attribute value type is "D"
      PAttributeSetData[] data2 = PAttributeSetData.select(this, dataPAttr[0].mAttributesetId);

      // If it is an Instance Attribute, clean value of Attribute set instance, else fill it with
      // the attribute set instance
      if (PAttributeSet.isInstanceAttributeSet(data2)) {
        info.addResult("inpmAttributesetinstanceId", "");
        info.addResult("inpmAttributesetinstanceId_R", "");
      } else {
        info.addResult("inpmAttributesetinstanceId", dataPAttr[0].mAttributesetinstanceId);
        info.addResult("inpmAttributesetinstanceId_R",
            FormatUtilities.replaceJS(dataPAttr[0].description));
      }
    } else {
      info.addResult("inpmAttributesetinstanceId", "");
      info.addResult("inpmAttributesetinstanceId_R", "");
    }

    // Update the attribute set and attribute set value type according the product attribute.
    info.addResult("inpattributeset", FormatUtilities.replaceJS(dataPAttr[0].mAttributesetId));
    info.addResult("inpattrsetvaluetype", FormatUtilities.replaceJS(dataPAttr[0].attrsetvaluetype));
  }
}
