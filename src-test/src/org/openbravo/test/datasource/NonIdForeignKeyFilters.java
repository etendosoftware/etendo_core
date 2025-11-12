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
 * All portions are Copyright (C) 2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.datasource;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.openbravo.dal.core.OBContext;

/**
 * Tests the criteria that the grid builds when a foreign key that does not reference the id of the
 * target table is filtered
 */
public class NonIdForeignKeyFilters extends BaseDataSourceTestDal {
  private static final int STATUS_OK = 0;
  private static final Logger log = LogManager.getLogger();

  @Test
  public void testFilterWithNonIdReferencedProperty() throws Exception {
    OBContext.setOBContext(TEST_USER_ID);

    // the language FK references a non-id column
    JSONObject response = requestCountry(getLanguageCriteria());

    assertThat("Response is OK", isResponseOk(response), equalTo(true));
    assertThat("Response contains one record", getNumberOfDataItems(response), equalTo(1));
  }

  @Test
  public void testFilterWithIdReferencedProperty() throws Exception {
    OBContext.setOBContext(TEST_USER_ID);

    // the currency FK references an id column
    JSONObject response = requestCountry(getCurrencyCriteria());

    assertThat("Response is OK", isResponseOk(response), equalTo(true));
    assertThat("Response contains one record", getNumberOfDataItems(response), equalTo(1));
  }

  private String getLanguageCriteria() {
    return "{\"_constructor\":\"AdvancedCriteria\",\"fieldName\":\"language\",\"value\":\"sq_AL\",\"operator\":\"equals\"}";
  }

  private String getCurrencyCriteria() {
    return "{\"_constructor\":\"AdvancedCriteria\",\"fieldName\":\"currency\",\"value\":\"238\",\"operator\":\"equals\"}";
  }

  private JSONObject requestCountry(String criteria) throws Exception {
    Map<String, String> params = this.getParameters(criteria);

    String response = this.doRequest("/org.openbravo.service.datasource/Country", params, 200,
        "POST");
    log.debug("NonIdForeignKeyFilters criteria {} -> {}", criteria, response);
    return new JSONObject(response).getJSONObject("response");
  }

  private Map<String, String> getParameters(String criteria) {
    Map<String, String> params = new HashMap<>();
    params.put("_operationType", "fetch");
    params.put("_startRow", "0");
    params.put("_endRow", "100");
    params.put("criteria", criteria);
    return params;
  }

  private boolean isResponseOk(JSONObject response) throws JSONException {
    return response.getInt("status") == STATUS_OK;
  }

  private int getNumberOfDataItems(JSONObject response) throws JSONException {
    return response.getJSONArray("data").length();
  }

}
