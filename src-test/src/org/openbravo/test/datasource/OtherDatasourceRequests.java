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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

/** Covers different datasource requests */
public class OtherDatasourceRequests extends BaseDataSourceTestDal {

  private static final String GOODS_MVNT = "170";
  private static final String ON_HAND_LOCATOR = "E8F1B0721E104D07AAC532290C951C37";

  /** Covers regression #37544 */
  @Test
  public void bindingsShouldGetIsSOTrxProperty() throws JSONException, Exception {
    JSONObject selectorResponse = performRequest();

    JSONArray data = selectorResponse.getJSONArray("data");
    boolean found = false;
    int i = 0;
    String lookFor = "RN-1-0-0";
    while (!found && i < data.length()) {
      found = lookFor.equals(data.getJSONObject(i).getString("_identifier"));
      i += 1;
    }

    assertThat(lookFor + " identifier in " + selectorResponse.toString(1), found, is(true));
  }

  private JSONObject performRequest() throws Exception {
    changeProfile(TEST_ROLE_ID, "192", TEST_ORG_ID, TEST_WAREHOUSE_ID);

    Map<String, String> params = new HashMap<String, String>();
    params.put("inpwindowId", GOODS_MVNT);
    params.put("_selectorDefinitionId", ON_HAND_LOCATOR);

    params.put("filterClass", "org.openbravo.userinterface.selector.SelectorDataSourceFilter");

    params.put("_operationType", "fetch");
    params.put("_startRow", "0");
    params.put("_endRow", "75");

    String response = doRequest("/org.openbravo.service.datasource/Locator", params, 200, "POST");
    JSONObject resp = new JSONObject(response).getJSONObject("response");

    assertTrue("Response should have data", resp.has("data"));
    return resp;
  }
}
