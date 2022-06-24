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
 * All portions are Copyright (C) 2016-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.dal.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.query.Query;
import org.openbravo.base.provider.OBNotSingleton;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

public class AcctSchemaStructureProvider implements OBNotSingleton {

  private Map<String, List<String>> acctSchemaByOrg = new HashMap<String, List<String>>();
  private String clientId;

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public List<String> getAcctSchemas(String orgID, String clientID) {
    if (acctSchemaByOrg.containsKey(orgID)) {
      return acctSchemaByOrg.get(orgID);
    } else {
      initialize(orgID, clientID);
      return getAcctSchemas(orgID, clientID);
    }
  }

  private synchronized void initialize(String orgId, String clientID) {
    if (getClientId() == null) {
      setClientId(OBContext.getOBContext().getCurrentClient().getId());
    }
    acctSchemaByOrg.put(orgId, getAcctSchemasFromDB(orgId, clientID));
  }

  private List<String> getAcctSchemasFromDB(String orgId, String clientID) {
    try {
      OBContext.setAdminMode(true);
      String where = " select a.id" + " from OrganizationAcctSchema as oas"
          + " join oas.accountingSchema as a" + " where oas.client.id = :clientId"
          + " and (ad_isorgincluded(:orgId, oas.organization.id, oas.client.id) <> -1 or :orgId = '0')"
          + " and a.active = true" + " and oas.active = true" + " group by a.id";

      final Query<String> qry = OBDal.getInstance().getSession().createQuery(where, String.class);
      qry.setParameter("clientId", clientID);
      qry.setParameter("orgId", orgId);
      return qry.list();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
