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
 * All portions are Copyright (C)2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.datasource;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.test.base.Issue;

/**
 * With these datasource parameters, the WHERE_AND_FILTER_CLAUSE parameter is the empty string and
 * that was the case that was failing. This test checks that the request to the datasource is
 * correct and the response is 0, which is the successful request status.
 * 
 * @author Naroa Iriarte
 * 
 */
@Issue("32912")
public class EmptyStringWhereAndFilterClauseParameter extends BaseDataSourceTestDal {
  private static final String WAREHOUSE_AND_STORAGE_BIN_WINDOW_ID = "139";
  private static final String STORAGE_BIN_TAB_ID = "178";
  private static final String WAREHOUSE_LOCATOR_ID = "54EB861A446D464EAA433477A1D867A6";

  @Test
  public void datasourceRequestStatusShouldBeSuccessful() throws Exception {
    String datasourceResponse = getDataSourceResponse();
    JSONObject jsonResponse = new JSONObject(datasourceResponse);
    assertThat("The request status should be successful.", getStatus(jsonResponse),
        is(String.valueOf(JsonConstants.RPCREQUEST_STATUS_SUCCESS)));
  }

  private Map<String, String> fillInTheParameters() {
    Map<String, String> params = new HashMap<String, String>();
    params.put("_targetRecordId", WAREHOUSE_LOCATOR_ID);
    params.put("_filterByParentProperty", "warehouse");
    params.put("windowId", WAREHOUSE_AND_STORAGE_BIN_WINDOW_ID);
    params.put("tabId", STORAGE_BIN_TAB_ID);
    params.put("_operationType", "fetch");
    params.put("_startRow", "0");
    params.put("_endRow", "100");
    return params;
  }

  private String getDataSourceResponse() throws Exception {
    String response = doRequest("/org.openbravo.service.datasource/Locator", fillInTheParameters(),
        200, "POST");
    return response;
  }

  private String getStatus(JSONObject jsonResponse) throws JSONException {
    return jsonResponse.getJSONObject("response").get("status").toString();
  }
}
