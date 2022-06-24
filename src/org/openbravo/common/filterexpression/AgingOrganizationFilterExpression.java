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
 * All portions are Copyright (C) 2017-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.common.filterexpression;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Organization;

public class AgingOrganizationFilterExpression implements FilterExpression {

  @Override
  public String getExpression(Map<String, String> requestMap) {
    try {
      String contextOrgId = OBContext.getOBContext().getCurrentOrganization().getId();
      String orgId = getValidOrganization(contextOrgId);
      if (StringUtils.isEmpty(orgId)) {
        orgId = getValidOrganization(null);
      }
      return orgId;
    } catch (Exception e) {
      return null;
    }
  }

  private String getValidOrganization(String contextOrgId) {
    //@formatter:off
    String hql =
            "as o" +
            "  join o.organizationType as ot" +
            " where ot.transactionsAllowed = true" +
            "   and o.ready = true";
    //@formatter:on
    if (StringUtils.isNotEmpty(contextOrgId)) {
      //@formatter:off
      hql +=
            "   and o.id = :contextOrgId";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            " order by o.name";
    //@formatter:on

    final OBQuery<Organization> query = OBDal.getInstance().createQuery(Organization.class, hql);
    if (StringUtils.isNotEmpty(contextOrgId)) {
      query.setNamedParameter("contextOrgId", contextOrgId);
    }
    query.setMaxResult(1);
    Organization org = query.uniqueResult();
    return org != null ? org.getId() : null;
  }

}
