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

package org.openbravo.common.actionhandler.copyfromorderprocess;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;

public class CopyFromOrdersProcessFilterExpression implements FilterExpression {

  @Override
  public String getExpression(Map<String, String> requestMap) {
    try {
      String context = requestMap.get("context");
      JSONObject contextJSON = new JSONObject(context);

      if (calculateLegalEntityField(requestMap)) {
        String organizationId = contextJSON.getString("inpadOrgId");
        return getLegalEntityId(organizationId);
      } else if (calculateSoTrxField(requestMap)) {
        return getIsSoTrx(contextJSON.getString("inpissotrx"));
      }
    } catch (Exception ignore) {
    }

    return null;
  }

  private boolean calculateLegalEntityField(Map<String, String> requestMap) {
    return requestMap.containsKey("currentParam")
        && StringUtils.equals(requestMap.get("currentParam"), "ad_org_id");
  }

  private String getLegalEntityId(String organizationId) {
    final Organization organization = OBDal.getInstance()
        .getProxy(Organization.class, organizationId);
    return OBContext.getOBContext()
        .getOrganizationStructureProvider(organization.getClient().getId())
        .getLegalEntity(organization)
        .getId();
  }

  private boolean calculateSoTrxField(Map<String, String> requestMap) {
    return StringUtils.equals(requestMap.get("filterExpressionColumnName"), "IsSOTrx");
  }

  private String getIsSoTrx(final String inpissotrx) {
    return StringUtils.equals(inpissotrx, "N") ? "false" : "true";
  }
}
