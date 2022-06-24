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

package org.openbravo.test.datasource;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

/**
 * Test cases for datasources for HQL based tables
 * 
 * @author alostale
 *
 */
public class HQLDataSourceTest extends BaseDataSourceTestNoDal {

  /**
   * Filtering columns with an alias different than the column name should work
   */
  @Test
  public void filterByColumnWithDifferentAlias() throws Exception {
    Map<String, String> params = new HashMap<String, String>();
    params.put("tableId", "59ED9B23854A4B048CBBAE38436B99C2");
    params.put("operator", "and");
    params.put("_constructor", "AdvancedCriteria");
    params.put("_operationType", "fetch");
    params.put("_startRow", "0");
    params.put("_endRow", "1");

    // filtering by outstandingAmount which alias and column name is different
    params.put("criteria",
        "{\"fieldName\":\"outstandingAmount\",\"operator\":\"equals\",\"value\":100,\"_constructor\":\"AdvancedCriteria\"}\"");

    String response = doRequest(
        "/org.openbravo.service.datasource/3C1148C0AB604DE1B51B7EA4112C325F", params, 200, "POST");

    assertThat("status should be success (0)",
        new JSONObject(response).getJSONObject("response").getLong("status"), is(0L));
  }

}
