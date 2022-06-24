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
 * All portions are Copyright (C) 2013-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.gl.GLItem;

public class SE_GLItem_Transaction extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    final String strGLItemId = info.getStringParameter("inpcGlitemId", IsIDFilter.instance);
    String description = info.getStringParameter("inpdescription", null);

    try {
      OBContext.setAdminMode(true);

      final String glItemDescPrefix = OBMessageUtils.messageBD("APRM_GLItem");

      // Delete only previous GL Item description
      if (StringUtils.isBlank(strGLItemId)) {
        description = FIN_Utility.getFinAccTransactionDescription(description,
            "\n" + glItemDescPrefix, "");
        description = FIN_Utility.getFinAccTransactionDescription(description, glItemDescPrefix,
            "");
      }

      // Write description for selected GL Item
      final GLItem glItem = OBDal.getInstance().get(GLItem.class, strGLItemId);
      if (glItem != null) {
        final String newGlItemDesc = glItemDescPrefix + ": " + glItem.getName();
        description = FIN_Utility.getFinAccTransactionDescription(description, glItemDescPrefix,
            newGlItemDesc);
      }

      info.addResult("inpdescription", description);
    } catch (Exception e) {
      return;
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
