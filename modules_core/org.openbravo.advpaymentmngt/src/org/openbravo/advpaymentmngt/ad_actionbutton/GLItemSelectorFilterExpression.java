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
 * All portions are Copyright (C) 2012-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.advpaymentmngt.ad_actionbutton;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.Utility;

public class GLItemSelectorFilterExpression implements FilterExpression {
  private static final String INPAD_ORG_ID_PARAM = "inpadOrgId";
  private static final String AD_ORG_ID_PARAM = "ad_org_id";
  private static final Logger log = LogManager.getLogger();

  @Override
  public String getExpression(Map<String, String> requestMap) {
    try {
      final JSONObject context = new JSONObject(requestMap);
      final String orgId = getOrganizationIdFromContext(context);
      String orgList = Utility.getInStrSet(
          OBContext.getOBContext().getOrganizationStructureProvider().getNaturalTree(orgId));
      if (!orgList.isEmpty()) {
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("e.organization.id in (" + orgList + ")");
        return whereClause.toString();
      } else {
        return "";
      }
    } catch (Exception e) {
      log.error("Error trying to get organization where clause for GL Item: ", e);
      return null;
    }
  }

  private String getOrganizationIdFromContext(final JSONObject context) throws JSONException {
    if (contextHasADOrgIDParam(context)) {
      return context.getString(AD_ORG_ID_PARAM);
    } else if (contextHasInpADOrgIDParam(context)) {
      return context.getString(INPAD_ORG_ID_PARAM);
    } else {
      return null;
    }
  }

  private boolean contextHasInpADOrgIDParam(final JSONObject context) throws JSONException {
    return context.has(INPAD_ORG_ID_PARAM) && context.get(INPAD_ORG_ID_PARAM) != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString(INPAD_ORG_ID_PARAM));
  }

  private boolean contextHasADOrgIDParam(final JSONObject context) throws JSONException {
    return context.has(AD_ORG_ID_PARAM) && context.get(AD_ORG_ID_PARAM) != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString(AD_ORG_ID_PARAM));
  }
}
