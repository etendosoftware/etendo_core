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
 * All portions are Copyright (C) 2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.datasource;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.openbravo.test.base.Issue;

/**
 * Test cases to check behavior of FK filter drop downs
 * 
 * @author alostale
 *
 */
public class FKDropDownDatasource extends BaseDataSourceTestNoDal {

  /**
   * Drop down FK filter in Organization dataset is a special case because org filtering must be
   * done in the base entity.
   */
  @Test
  @Issue("28085")
  public void filterFKInOrganization() throws Exception {
    Map<String, String> params = new HashMap<String, String>();

    params.put("tabId", "143");
    params.put("_distinct", "organizationType");
    params.put("_operationType", "fetch");
    params.put("_startRow", "0");
    params.put("_endRow", "75");
    JSONObject resp = new JSONObject(
        doRequest("/org.openbravo.service.datasource/Organization", params, 200, "POST"))
            .getJSONObject("response");

    assertThat("response status", resp.getInt("status"), is(0));
    assertThat("number of rows", resp.getInt("totalRows"), greaterThan(0));
  }
}
