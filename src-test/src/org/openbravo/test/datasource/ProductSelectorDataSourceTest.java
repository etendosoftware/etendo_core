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
 * All portions are Copyright (C) 2014-2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

/**
 * Test Product Complete Selector DataSource
 * 
 * @author alostale
 * 
 */
public class ProductSelectorDataSourceTest extends BaseDataSourceTestNoDal {
  private boolean defaultRoleSet = false;

  /**
   * Obtains a list of all warehouses from the selector. Test for issue #26317
   */
  @Test
  public void testWarehouseFKDropDown() throws Exception {
    JSONObject resp = performRequest(false);

    assertEquals("Data should have 4 warehouses", 4, resp.getJSONArray("data").length());
  }

  /**
   * Obtains a list of filtered warehouses. Test for issue #26317
   */
  @Test
  public void testWarehouseFKDropDownFilter() throws Exception {
    JSONObject resp = performRequest(true);
    assertEquals("Data should have 2 warehouses", 2, resp.getJSONArray("data").length());
  }

  private JSONObject performRequest(boolean addFilter) throws Exception {
    if (!defaultRoleSet) {
      changeProfile("42D0EEB1C66F497A90DD526DC597E6F0", "192", "E443A31992CB4635AFCAEABE7183CE85",
          "B2D40D8A5D644DD89E329DC297309055");
      defaultRoleSet = true;
    }

    Map<String, String> params = new HashMap<String, String>();
    params.put("_selectorDefinitionId", "4C8BC3E8E56441F4B8C98C684A0C9212");
    params.put("filterClass", "org.openbravo.userinterface.selector.SelectorDataSourceFilter");

    JSONObject criteria = new JSONObject();
    criteria.put("fieldName", "active");
    criteria.put("operator", "equals");
    criteria.put("value", "true");
    criteria.put("_constructor", "AdvancedCriteria");
    params.put("criteria", criteria.toString());

    params.put("_sortBy", "_identifier");
    params.put("_requestType", "Window");
    params.put("_distinct", "storageBin$warehouse");

    params.put("_operationType", "fetch");
    params.put("_startRow", "0");
    params.put("_endRow", "75");
    params.put("_textMatchStyle", "substring");

    params.put("targetProperty", "product");
    params.put("inpTableId", "FF8080812E381D1E012E3898C5DD0010");

    if (addFilter) {
      JSONObject filterCriteria = new JSONObject();
      filterCriteria.put("fieldName", "storageBin$warehouse$_identifier");
      filterCriteria.put("operator", "iContains");
      filterCriteria.put("value", "US");
      params.put("criteria", filterCriteria.toString());
    }

    String response = doRequest("/org.openbravo.service.datasource/ProductStockView", params, 200,
        "POST");
    JSONObject resp = new JSONObject(response).getJSONObject("response");

    assertTrue("Response should have data", resp.has("data"));
    return resp;
  }
}
