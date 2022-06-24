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
 * All portions are Copyright (C) 2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.info;

import java.util.Map;

import javax.enterprise.context.Dependent;

import org.apache.commons.lang.StringUtils;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Adds a filter by natural organizations if selector is being used by BusinessPartner column in
 * tables C_INVOICE, C_INVOICELINE, M_INOUT, M_INOUTLINE, C_ORDER and C_ORDERLINE
 * 
 */
@Dependent
public class BusinessPartnerSelectorFilterExpression implements FilterExpression {

  private static final String C_ORDER = "259";
  private static final String C_ORDERLINE = "260";
  private static final String M_INOUT = "319";
  private static final String M_INOUTLINE = "320";
  private static final String C_INVOICE = "318";
  private static final String C_INVOICELINE = "333";

  @Override
  public String getExpression(Map<String, String> requestMap) {
    String tableId = StringUtils.defaultIfBlank(requestMap.get("inpTableId"), "0");
    switch (tableId) {
      case C_ORDER:
      case C_ORDERLINE:
      case M_INOUT:
      case M_INOUTLINE:
      case C_INVOICE:
      case C_INVOICELINE:
        String organizationId = StringUtils.defaultIfBlank(requestMap.get("inpadOrgId"), "0");
        String clientId = StringUtils.defaultIfBlank(requestMap.get("inpadClientId"), "0");
        StringBuilder query = new StringBuilder(" ad_isorgincluded('")
            .append(OBDal.getInstance().getProxy(Organization.class, organizationId).getId())
            .append("', bp.organization.id, '")
            .append(clientId)
            .append("') <> -1 ");
        return query.toString();
      default:
        return StringUtils.EMPTY;
    }
  }

}
