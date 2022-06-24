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

package org.openbravo.test.selector;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.openbravo.userinterface.selector.SelectorConstants.PARAM_FILTER_EXPRESSION;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.openbravo.test.datasource.BaseDataSourceTestDal;
import org.openbravo.test.datasource.DatasourceTestUtil;

/** Test cases covering selector default filters computation */
public class TestSelectorDefaultFilterActionHandler extends BaseDataSourceTestDal {

  private static final String PRODUCT_COMPLETE_SELECTOR_ID = "4C8BC3E8E56441F4B8C98C684A0C9212";
  private static final String GOODS_MOVEMENTS_WINDOW_ID = "170";

  @Test
  public void filterExpressionShouldBeReturned() throws Exception {
    String response = doSelectorDefaultFilterActionHandlerRequest();
    JSONObject defaultsFilterSelector = new JSONObject(response);
    // The Product Complete selector has a Filter Expression defined in the Application Dictionary
    // that we should be retrieving here
    String filterExpression = defaultsFilterSelector.has(PARAM_FILTER_EXPRESSION)
        ? defaultsFilterSelector.getString(PARAM_FILTER_EXPRESSION)
        : null;
    assertThat("Selector default filters are returned", filterExpression, notNullValue());
  }

  private String doSelectorDefaultFilterActionHandlerRequest() throws Exception {
    String cookie = authenticate();
    String urlPart = "/org.openbravo.client.kernel?_action=org.openbravo.userinterface.selector.SelectorDefaultFilterActionHandler";
    String resp = DatasourceTestUtil.request(getOpenbravoURL(), urlPart, "POST",
        getRequestContent(), cookie, 200, "application/json");
    return resp;
  }

  private String getRequestContent() {
    JSONObject content = new JSONObject();
    try {
      content.put("_selectorDefinitionId", PRODUCT_COMPLETE_SELECTOR_ID);
      content.put("inpwindowId", GOODS_MOVEMENTS_WINDOW_ID);
    } catch (JSONException ignore) {
    }
    return content.toString();
  }
}
