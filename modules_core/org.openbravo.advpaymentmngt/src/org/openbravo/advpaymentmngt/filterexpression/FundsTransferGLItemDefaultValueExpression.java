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
 * All portions are Copyright (C) 2018-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.advpaymentmngt.filterexpression;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.gl.GLItem;

/**
 * This class returns the default value for the GL Item Parameter of the Funds Transfer Process in
 * Financial Account Window
 */

public class FundsTransferGLItemDefaultValueExpression implements FilterExpression {
  private static final String INPAD_ORG_ID_PARAM = "inpadOrgId";
  private static final String AD_ORG_ID_PARAM = "ad_org_id";
  private static final Logger log = LogManager.getLogger();

  @Override
  public String getExpression(Map<String, String> requestMap) {
    try {
      final JSONObject context = new JSONObject(requestMap.get("context"));
      final String organizationID = getOrganizationIdFromContext(context);
      if (organizationID == null || StringUtils.isBlank(organizationID)) {
        return null;
      } else {
        return getDefaultGLItemForOrganization(organizationID);
      }
    } catch (Exception e) {
      log.error("Error trying to get default value of the Default GL Item: " + e.getMessage(), e);
      return null;
    }
  }

  private String getDefaultGLItemForOrganization(final String organizationId) {
    OBContext.setAdminMode(true);
    try {
      final Organization organization = OBDal.getInstance().get(Organization.class, organizationId);
      if (organization == null) {
        return null;
      }

      if (organization.getAPRMGlitem() != null) {
        return organization.getAPRMGlitem().getId();
      } else {
        return getDefaultGLItemFromOrganizationTree(organization);
      }
    } catch (Exception e) {
      log.error("Impossible to get default GL Item for Funds Transfer\n" + " of organization id "
          + organizationId, e);
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private String getDefaultGLItemFromOrganizationTree(final Organization organization) {
    //@formatter:off
    final String hql = 
                  "select p.aPRMGlitem " +
                  "  from OrganizationTree t " +
                  "    join t.parentOrganization p " +
                  "    join t.organization o " +
                  " where o.id = :organizationId " +
                  "   and p.aPRMGlitem is not null " +
                  " order by t.levelno asc ";
  //@formatter:on

    final GLItem glItem = OBDal.getInstance()
        .getSession()
        .createQuery(hql, GLItem.class)
        .setParameter("organizationId", organization.getId())
        .setMaxResults(1)
        .uniqueResult();
    return glItem != null ? glItem.getId() : null;
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
