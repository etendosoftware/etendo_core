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
 * All portions are Copyright (C) 2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.advpaymentmngt.actionHandler;

import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.common.enterprise.Organization;

public class MatchStatementOnLoadPreferenceActionHandler extends BaseActionHandler {

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    try {
      OBContext.setAdminMode(true);
      Preference userPreference = OBProvider.getInstance().get(Preference.class);
      userPreference.setNewOBObject(true);
      userPreference.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
      userPreference.setClient(OBContext.getOBContext().getCurrentClient());
      userPreference.setUserContact(OBContext.getOBContext().getUser());
      userPreference.setSearchKey("Y");
      userPreference.setPropertyList(false);
      userPreference.setAttribute("APRM_NoPersistInfoMessageInMatching");
      OBDal.getInstance().save(userPreference);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonResponse;
  }
}
