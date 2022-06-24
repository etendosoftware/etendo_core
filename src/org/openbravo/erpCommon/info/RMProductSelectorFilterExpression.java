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
 * All portions are Copyright (C) 2012 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.info;

import java.util.Map;

import org.openbravo.client.application.FilterExpression;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.Utility;

public class RMProductSelectorFilterExpression implements FilterExpression {

  final static String RETURN_FROM_CUSTOMER_WINDOW = "FF808081330213E60133021822E40007";

  @Override
  public String getExpression(Map<String, String> requestMap) {
    if (!RETURN_FROM_CUSTOMER_WINDOW.equals(requestMap.get("inpwindowId"))) {
      return "";
    }
    StringBuilder whereClause = new StringBuilder();
    String orgId = (String) RequestContext.get()
        .getSession()
        .getAttribute(RETURN_FROM_CUSTOMER_WINDOW + "|AD_ORG_ID");
    String orgList = Utility.getInStrSet(
        OBContext.getOBContext().getOrganizationStructureProvider().getNaturalTree(orgId));
    if (!orgList.isEmpty()) {
      whereClause.append("e.organization.id in (" + orgList + ")");
    }
    return whereClause.toString();
  }

}
