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

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

public class SL_Costing_Currency extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    final String strAdOrgId = info.getStringParameter("inpadOrgId", IsIDFilter.instance);
    final String strClientId = info.getStringParameter("inpadClientId", IsIDFilter.instance);
    String strCurrencyId = null;
    try {
      OBContext.setAdminMode(true);
      Organization org = OBDal.getInstance().get(Organization.class, strAdOrgId);
      if (org.getCurrency() != null) {
        strCurrencyId = org.getCurrency().getId();
      } else {
        Organization legalOrg = OBContext.getOBContext()
            .getOrganizationStructureProvider()
            .getLegalEntity(org);
        if (legalOrg != null) {
          strCurrencyId = legalOrg.getCurrency() != null ? legalOrg.getCurrency().getId() : null;
        }
        if (strCurrencyId == null) {
          Client client = OBDal.getInstance().get(Client.class, strClientId);
          strCurrencyId = client.getCurrency() != null ? client.getCurrency().getId() : null;
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    info.addResult("inpcCurrencyId", strCurrencyId);
  }
}
