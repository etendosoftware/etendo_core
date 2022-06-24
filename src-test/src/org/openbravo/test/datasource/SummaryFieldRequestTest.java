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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.openbravo.dal.core.OBContext;

/**
 * Test cases for grid summaries
 */
public class SummaryFieldRequestTest extends BaseDataSourceTestDal {
  private static final Logger log = LogManager.getLogger();

  /**
   * Test that a single summary function can be requested properly
   */
  @Test
  public void summaryFunctionIsCalculated() {
    OBContext.setOBContext(TEST_USER_ID);
    try {
      JSONObject summaryFunction = new JSONObject();
      summaryFunction.put("grandTotalAmount", "sum");
      JSONObject gridSummaries = getGridSummaries(summaryFunction);
      assertThat("Summary field sum(grandTotalAmount) is calculated",
          gridSummaries.getDouble("grandTotalAmount"), notNullValue());
    } catch (Exception ex) {
      log.error("Error executing request to calculate the summary function", ex);
    }
  }

  /**
   * Test that multiple summary functions can be requested properly
   */
  @Test
  public void summaryFunctionsAreCalculated() {
    OBContext.setOBContext(TEST_USER_ID);
    try {
      JSONObject summaryFunctions = new JSONObject();
      summaryFunctions.put("documentNo", "max");
      summaryFunctions.put("grandTotalAmount", "sum");
      JSONObject gridSummaries = getGridSummaries(summaryFunctions);
      assertThat("Summary field max(documentNo) is calculated",
          gridSummaries.getString("documentNo"), notNullValue());
      assertThat("Summary field sum(grandTotalAmount) is calculated",
          gridSummaries.getDouble("grandTotalAmount"), notNullValue());
    } catch (Exception ex) {
      log.error("Error executing request to calculate summary functions", ex);
    }
  }

  private JSONObject getGridSummaries(JSONObject summaryFunctions) throws Exception {
    Map<String, String> params = new HashMap<String, String>();

    params.put("_summary", summaryFunctions.toString());
    params.put("_noCount", "true");
    params.put("_operationType", "fetch");
    params.put("_startRow", "0");
    params.put("_endRow", "1");

    JSONObject resp = new JSONObject(
        doRequest("/org.openbravo.service.datasource/Order", params, 200, "POST"));

    assertThat("expecting response, got: " + resp, resp.has("response"), equalTo(true));

    JSONObject r = resp.getJSONObject("response");
    log.debug("Response: {}", r);
    assertThat("expecting data in response, got: " + r, r.has("data"), equalTo(true));

    JSONObject data = r.getJSONArray("data").getJSONObject(0);
    assertThat("Is Grid Summary", data.getBoolean("isGridSummary"), equalTo(true));

    return data;
  }

}
