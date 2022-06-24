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

package org.openbravo.erpCommon.info;

import java.util.Map;

import org.openbravo.base.model.AccessLevel;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

/**
 * Filter expression for the "Context Role Direct Accessible Organizations" reference.
 * 
 * It filters the organizations to be displayed taking into account if their access has been
 * explicitly defined for the role in the current context. It also takes into account if the access
 * level of the process definition allows to include the organization '*'.
 */
public class ContextRoleDirectAccessibleOrganizations implements FilterExpression {

  @Override
  public String getExpression(Map<String, String> requestMap) {
    StringBuilder filterExpression = new StringBuilder("");
    filterExpression.append("exists (select 1 from ADRoleOrganization ro where ro.role.id = '"
        + OBContext.getOBContext().getRole().getId() + "' and ro.organization.id = e.id)");

    int accessLevel;
    if (requestMap.containsKey("inpadProcessId")) {
      accessLevel = getProcessAccessLevel(requestMap.get("inpadProcessId"));
    } else if (requestMap.containsKey("_processDefinitionId")) {
      accessLevel = getProcessDefinitionAccessLevel(requestMap.get("_processDefinitionId"));
    } else {
      return filterExpression.toString();
    }

    if (accessLevel == AccessLevel.ORGANIZATION.getDbValue()) {
      filterExpression.append(" AND e.id <> '0'");
    } else if (accessLevel == AccessLevel.ALL.getDbValue()
        || accessLevel == AccessLevel.CLIENT_ORGANIZATION.getDbValue()) {
      filterExpression.append(" OR e.id = '0'");
    } else if (accessLevel == AccessLevel.SYSTEM_CLIENT.getDbValue()) {
      filterExpression = new StringBuilder("e.id = '0'");
    }
    return filterExpression.toString();
  }

  private int getProcessAccessLevel(String processId) {
    org.openbravo.model.ad.ui.Process process = OBDal.getInstance()
        .get(org.openbravo.model.ad.ui.Process.class, processId);
    if (process == null) {
      return -1;
    }
    return Integer.parseInt(process.getDataAccessLevel());
  }

  private int getProcessDefinitionAccessLevel(String processDefinitionId) {
    org.openbravo.client.application.Process processDefinition = OBDal.getInstance()
        .get(org.openbravo.client.application.Process.class, processDefinitionId);
    if (processDefinition == null) {
      return -1;
    }
    return Integer.parseInt(processDefinition.getDataAccessLevel());
  }
}
