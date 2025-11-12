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
 * All portions are Copyright (C) 2015-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.datasource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.test.base.Issue;

/**
 * Test case for 'Allow Unpaged Datasource In Manual Request' preference
 */
@Issue("30204")
public class TestAllowUnpagedDatasourcePreference extends BaseDataSourceTestDal {

  protected Logger logger = LogManager.getLogger();

  private static final String ROLE_SYSTEM = "0";
  private static final String ORGANIZATION = "0";
  private static final String LANGUAGE_ID = "192";
  private static final String WAREHOUSE_ID = "B2D40D8A5D644DD89E329DC297309055";

  private static Stream<Arguments> preferenceValues() {
    return Stream.of(Arguments.of(Preferences.NO, "Manual request should not be performed"),
        Arguments.of(Preferences.YES, "Manual request should be performed"));
  }

  @ParameterizedTest(name = "{1} (value={0})")
  @MethodSource("preferenceValues")
  public void testDatasourceRequest(String preferenceValue, String description) throws Exception {
    OBContext.setAdminMode(false);
    String preferenceId = "";
    try {
      logger.info("Testing allow-unpaged preference scenario '{}' with value {}", description,
          preferenceValue);
      preferenceId = createPreference(preferenceValue);

      // Preference is cached at session scope, ensure a new session is created
      logout();

      // ensures a role with an access to a UOM entity
      changeProfile(ROLE_SYSTEM, LANGUAGE_ID, ORGANIZATION, WAREHOUSE_ID);

      Map<String, String> params = new HashMap<>();
      params.put("_operationType", "fetch");
      String response = doRequest("/org.openbravo.service.datasource/UOM", params,
          HttpServletResponse.SC_OK, POST_METHOD);

      switch (preferenceValue) {
        case Preferences.NO:
          String errorMsg = OBMessageUtils.messageBD("OBJSON_NoPagedFetchManual");
          assertThat("Datasource returned error message " + response,
              getResponseErrorMessage(response), equalTo(errorMsg));
          break;
        case Preferences.YES:
          int responseStatus = new JSONObject(response).getJSONObject("response").getInt("status");
          assertThat("Response status " + response, responseStatus,
              equalTo(JsonConstants.RPCREQUEST_STATUS_SUCCESS));
      }

    } finally {
      OBDal.getInstance().remove(OBDal.getInstance().get(Preference.class, preferenceId));
      OBDal.getInstance().commitAndClose();
      OBContext.restorePreviousMode();
    }
  }

  private String createPreference(String value) {
    Preference pref = OBProvider.getInstance().get(Preference.class);
    pref.setProperty("OBJSON_AllowUnpagedDatasourceManualRequest");
    pref.setPropertyList(true);
    pref.setSearchKey(value);
    pref.setVisibleAtClient(OBDal.getInstance().getProxy(Client.class, "0"));
    pref.setVisibleAtOrganization(OBDal.getInstance().getProxy(Organization.class, "0"));
    pref.setClient(OBDal.getInstance().getProxy(Client.class, "0"));
    pref.setOrganization(OBDal.getInstance().getProxy(Organization.class, "0"));
    pref.setSelected(true);
    OBDal.getInstance().save(pref);

    // commit to make the new preference available from datasource request in a different trx
    OBDal.getInstance().commitAndClose();
    return pref.getId();
  }

  private String getResponseErrorMessage(String response) throws JSONException {
    JSONObject jsonResponse = new JSONObject(response).getJSONObject("response");
    if (jsonResponse.has("error")) {
      JSONObject error = jsonResponse.getJSONObject("error");
      return error.getString("message");
    }
    return "";
  }
}
