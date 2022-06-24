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
import org.openbravo.utils.FormatUtilities;

public class SE_Pricelist_Active extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strLastChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("Changed: " + strLastChanged);
    }

    // Parameters
    String strPricelistId = info.getStringParameter("inpmPricelistId", IsIDFilter.instance);
    String strIsActive = info.getStringParameter("inpisactive", new ValueListFilter("Y", "N"));

    // Check whether there exists active versions for price list being set Active = No
    if (StringUtils.equals(strIsActive, "N")) {
      SEPricelistActiveData[] pricelistVersion = SEPricelistActiveData
          .getActivePricelistVersion(this, strPricelistId);
      if (pricelistVersion != null && pricelistVersion.length > 0) {
        info.showMessage(FormatUtilities
            .replaceJS(Utility.messageBD(this, "PricelistVersionActive", info.vars.getLanguage())));
      }
    }
  }
}
