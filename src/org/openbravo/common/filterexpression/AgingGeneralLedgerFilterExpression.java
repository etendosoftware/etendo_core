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
 * All portions are Copyright (C) 2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.common.filterexpression;

import java.util.Map;

import org.openbravo.client.application.FilterExpression;
import org.openbravo.client.application.ParameterUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.erpCommon.utility.OBLedgerUtils;

public class AgingGeneralLedgerFilterExpression implements FilterExpression {

  @Override
  public String getExpression(Map<String, String> requestMap) {
    try {
      String defaultOrg = (String) ParameterUtils.getJSExpressionResult(requestMap,
          RequestContext.get().getSession(),
          "OB.getFilterExpression('org.openbravo.common.filterexpression.AgingOrganizationFilterExpression')");
      return OBLedgerUtils.getOrgLedger(defaultOrg);
    } catch (Exception e) {
      return null;
    }
  }

}
