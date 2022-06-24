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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test to check that a property resolved through navigation can be used to be shown in the selector
 * pick-list. This kind of properties are sent to the datasource inside the "_extraProperties"
 * parameter in order to handle them properly.
 */
@RunWith(Parameterized.class)
public class SelectorPickListFieldsDataSourceTest extends BaseDataSourceTestNoDal {
  private static boolean defaultRoleSet = false;
  private String extraProperty;

  public SelectorPickListFieldsDataSourceTest(String extraProperty) {
    this.extraProperty = extraProperty;
  }

  @Parameters(name = "{index}: extraProperty = {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] { { "standardPrice" }, { "product$uOM$id" },
        { "product$uOM" }, { "productPrice$priceListVersion$priceList$currency$id" } });
  }

  @Test
  public void extraPropertyIsCalculated() throws Exception {
    JSONObject resp = performRequest();
    JSONObject product = resp.getJSONArray("data").getJSONObject(0);
    assertTrue("Extra property is returned", product.has(extraProperty));
    assertFalse("Extra property has a value", product.isNull(extraProperty));
  }

  private JSONObject performRequest() throws Exception {
    if (!defaultRoleSet) {
      changeProfile("42D0EEB1C66F497A90DD526DC597E6F0", "192", "E443A31992CB4635AFCAEABE7183CE85",
          "B2D40D8A5D644DD89E329DC297309055");
      defaultRoleSet = true;
    }

    Map<String, String> params = new HashMap<String, String>();
    params.put("_selectorDefinitionId", "2E64F551C7C4470C80C29DBA24B34A5F");
    params.put("filterClass", "org.openbravo.userinterface.selector.SelectorDataSourceFilter");

    JSONObject criteria = new JSONObject();
    criteria.put("fieldName", "active");
    criteria.put("operator", "equals");
    criteria.put("value", "true");
    criteria.put("_constructor", "AdvancedCriteria");
    params.put("criteria", criteria.toString());

    params.put("_sortBy", "_identifier");
    params.put("_operationType", "fetch");
    params.put("_startRow", "0");
    params.put("_endRow", "75");
    params.put("_textMatchStyle", "substring");
    params.put("targetProperty", "product");
    params.put("inpTableId", "260");

    params.put("_selectedProperties", "id");
    params.put("_extraProperties", extraProperty);

    params.put("inpmPricelistId", "AEE66281A08F42B6BC509B8A80A33C29");

    String response = doRequest("/org.openbravo.service.datasource/ProductByPriceAndWarehouse",
        params, 200, "POST");
    JSONObject resp = new JSONObject(response).getJSONObject("response");

    assertTrue("Response should have data", resp.has("data"));
    return resp;
  }
}
