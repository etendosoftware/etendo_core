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

package org.openbravo.test.datasource;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openbravo.dal.core.OBContext;
import org.openbravo.test.base.Issue;

/**
 * Test to check that an Organization selector (both normal and custom query based) applies the
 * filter based on the role's organization access, even when an organization has been selected in
 * the field before calling to the datasource.
 */
@RunWith(Parameterized.class)
@Issue("36151")
@Issue("36863")
public class OrganizationSelectorDataSourceTest extends BaseDataSourceTestDal {
  private static final String USER_ID = "100";
  private static final String ROLE_ID = "9D320A774FCD4E47801DF5E03AA11F2D";
  private static final String LANGUAGE_ID = "192";
  private OBContext initialContext;
  private boolean organizationSelected;

  public OrganizationSelectorDataSourceTest(boolean organizationSelected) {
    this.organizationSelected = organizationSelected;
  }

  @Parameters(name = "{index}: organizationSelected = {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] { { false }, { true } });
  }

  @Before
  public void initOBContext() throws Exception {
    initialContext = OBContext.getOBContext();
    // Use F&B España, S.A - Procurement role
    OBContext.setOBContext(USER_ID, ROLE_ID, TEST_CLIENT_ID, TEST_ORG_ID);
    changeProfile(ROLE_ID, LANGUAGE_ID, TEST_ORG_ID, TEST_WAREHOUSE_ID);
  }

  @After
  public void resetOBContext() throws Exception {
    String roleId = initialContext.getRole() != null ? initialContext.getRole().getId() : null;
    String languageId = initialContext.getLanguage() != null ? initialContext.getLanguage().getId()
        : null;
    String orgId = initialContext.getCurrentOrganization() != null
        ? initialContext.getCurrentOrganization().getId()
        : null;
    String warehouseId = initialContext.getWarehouse() != null
        ? initialContext.getWarehouse().getId()
        : null;
    changeProfile(roleId, languageId, orgId, warehouseId);
  }

  @Test
  public void retrieveExpectedOrganizationListFromSelector() throws Exception {
    JSONObject resp = performSelectorRequest();
    assertOrganizations(getReturnedOrgs(resp.getJSONArray("data")));
  }

  @Test
  public void retrieveExpectedOrganizationListFromCustomQuerySelector() throws Exception {
    JSONObject resp = performCustomQuerySelectorRequest();
    assertOrganizations(getReturnedOrgs(resp.getJSONArray("data")));
  }

  private List<String> getReturnedOrgs(JSONArray returnedOrgs) {
    List<String> orgs = new ArrayList<>();
    try {
      for (int i = 0; i < returnedOrgs.length(); i++) {
        JSONObject org = returnedOrgs.getJSONObject(i);
        if (org.has("orgid")) {
          orgs.add(org.getString("orgid"));
        } else {
          orgs.add(org.getString("id"));
        }
      }
    } catch (Exception ignore) {
    }
    return orgs;
  }

  private void assertOrganizations(List<String> returnedOrgs) {
    Object[] readableOrgs = OBContext.getOBContext().getReadableOrganizations();
    assertThat("Retrieved the expected organizations", returnedOrgs,
        allOf(hasSize(readableOrgs.length), containsInAnyOrder(readableOrgs)));
  }

  private JSONObject performSelectorRequest() throws Exception {
    Map<String, String> params = new HashMap<String, String>();
    params.put("_selectorDefinitionId", "4E0AC6FEC5EA4A2BB474747DB03A3A21");
    params.put("filterClass", "org.openbravo.userinterface.selector.SelectorDataSourceFilter");
    params.put("_selectedProperties", "id");
    params.put("_extraProperties", "id,");
    params.put("_noCount", "true");
    params.put("_sortBy", "_identifier");
    params.put("_operationType", "fetch");
    params.put("_startRow", "0");
    params.put("_endRow", "75");
    params.put("_textMatchStyle", "startsWith");
    params.put("columnName", "AD_Org_ID");
    params.put("isSelectorItem", "true");
    params.put("operator", "or");
    params.put("_constructor", "AdvancedCriteria");
    JSONObject criteria = new JSONObject();
    criteria.put("fieldName", "_dummy");
    criteria.put("operator", "equals");
    criteria.put("value", "1506076927291");
    params.put("criteria", criteria.toString());

    if (organizationSelected) {
      params.put("_org", "0");
    } else {
      params.put("_calculateOrgs", "true");
    }

    String response = doRequest("/org.openbravo.service.datasource/Organization", params, 200,
        "POST");
    JSONObject resp = new JSONObject(response).getJSONObject("response");

    assertTrue("Response should have data", resp.has("data"));
    return resp;
  }

  private JSONObject performCustomQuerySelectorRequest() throws Exception {
    Map<String, String> params = new HashMap<String, String>();
    params.put("_selectorDefinitionId", "B748F356A65641D4974E5C349A16FB27");
    params.put("filterClass", "org.openbravo.userinterface.selector.SelectorDataSourceFilter");
    params.put("_selectedProperties", "id,name");
    params.put("_extraProperties", "orgid,name,");
    params.put("_noCount", "true");
    params.put("_sortBy", "_identifier");
    params.put("_operationType", "fetch");
    params.put("_startRow", "0");
    params.put("_endRow", "75");
    params.put("_textMatchStyle", "substring");
    params.put("columnName", "AD_Org_ID");
    params.put("targetProperty", "organization");
    params.put("isSelectorItem", "true");
    params.put("operator", "or");
    params.put("_constructor", "AdvancedCriteria");
    JSONObject criteria = new JSONObject();
    criteria.put("fieldName", "_dummy");
    criteria.put("operator", "equals");
    criteria.put("value", "1506076927291");
    params.put("criteria", criteria.toString());
    params.put("adTabId", "351");

    if (organizationSelected) {
      params.put("_org", "0");
    } else {
      params.put("_calculateOrgs", "true");
    }

    String response = doRequest(
        "/org.openbravo.service.datasource/F8DD408F2F3A414188668836F84C21AF", params, 200, "POST");
    JSONObject resp = new JSONObject(response).getJSONObject("response");

    assertTrue("Response should have data", resp.has("data"));
    return resp;
  }
}
