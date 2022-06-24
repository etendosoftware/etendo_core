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
package org.openbravo.test.datasource;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.WindowAccess;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Test class to verify that Organization Window shows only the readable organizations for a role,
 * when this role has access to only a subset of all available organizations
 *
 * @author jarmendariz
 */
public class OrganizationWindowRoleFilterTest extends BaseDataSourceTestDal {
  private static final String F_B_SPAIN_EMPLOYEE_ROLE = "D615084948E046E3A439915008F464A6";
  private static final String NORTH_ZONE_ORGANIZATION = "E443A31992CB4635AFCAEABE7183CE85";
  private static final String F_B_INTL_GROUP_CLIENT = "23C59575B9CF467C9620760EB255B389";
  private static final String ASTERISK_ORGANIZATION = "0";
  private static final String ORGANIZATION_WINDOW = "110";
  private static final String EN_US_LANG = "192";
  private String windowAccessId;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    windowAccessId = enableOrgWindowsAccessToRole(F_B_SPAIN_EMPLOYEE_ROLE);
  }

  @Test
  public void testOrganizationShowsOnlyReadableOrgsByRole() throws Exception {
    changeProfile(F_B_SPAIN_EMPLOYEE_ROLE, EN_US_LANG, NORTH_ZONE_ORGANIZATION, null);

    Assert.assertEquals("Number of readable Organizations returned by datasource not matching", 5,
        getTotalOrganizationDSRequestResult());
  }

  @After
  public void tearDown() {
    removeOrgWindowAccess(windowAccessId);
    windowAccessId = "";
  }

  private int getTotalOrganizationDSRequestResult() throws Exception {
    Map<String, String> params = new HashMap<>();
    params.put("_operationType", "fetch");
    params.put("windowId", "110");
    params.put("tabId", "143");
    params.put("_startRow", "0");
    params.put("_endRow", "100");

    String response = doRequest("/org.openbravo.service.datasource/Organization", params, 200,
        "POST");
    JSONObject resp = new JSONObject(response).getJSONObject("response");
    return resp.getInt("totalRows");
  }

  private String enableOrgWindowsAccessToRole(String roleId) {
    WindowAccess orgWindowAccess = OBProvider.getInstance().get(WindowAccess.class);
    orgWindowAccess.setWindow(OBDal.getInstance().getProxy(Window.class, ORGANIZATION_WINDOW));
    orgWindowAccess.setRole(OBDal.getInstance().getProxy(Role.class, roleId));
    orgWindowAccess.setClient(OBDal.getInstance().getProxy(Client.class, F_B_INTL_GROUP_CLIENT));
    orgWindowAccess
        .setOrganization(OBDal.getInstance().getProxy(Organization.class, ASTERISK_ORGANIZATION));

    OBDal.getInstance().save(orgWindowAccess);
    OBDal.getInstance().commitAndClose();

    return orgWindowAccess.getId();
  }

  private void removeOrgWindowAccess(String winAccessId) {
    OBDal.getInstance().remove(OBDal.getInstance().get(WindowAccess.class, winAccessId));
    OBDal.getInstance().commitAndClose();
  }
}
