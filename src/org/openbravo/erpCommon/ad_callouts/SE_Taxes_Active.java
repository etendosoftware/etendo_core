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
import org.openbravo.erpCommon.utility.Utility;

public class SE_Taxes_Active extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strLastChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("Last Field Changed: " + strLastChanged);
    }

    // Parameter
    String strIsActive = info.getStringParameter("inpisactive", new ValueListFilter("Y", "N"));
    String strParentTaxId = info.getStringParameter("inpcTaxId", IsIDFilter.instance);

    // Check whether Active child taxes exists for the Tax Rate being set as Active = No.
    if (StringUtils.equals(strIsActive, "N")) {
      SETaxesActiveData[] children = SETaxesActiveData.getActiveChildrenTaxes(this, strParentTaxId);
      if (children != null && children.length > 0) {
        info.showMessage(Utility.messageBD(this, "ChildrenTaxRateActive", info.vars.getLanguage()));
      }
    }
  }
}
