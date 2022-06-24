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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

/**
 * Tests Property selector datasource. Checking issue #26238 is not reproduced anymore.
 * 
 * @author alostale
 * 
 */
public class SelectorFieldPropertySelectorDSTest extends BaseDataSourceTestNoDal {
  private static final Logger log = LogManager.getLogger();
  private boolean sysAdminProfileSet = false;

  private static final String PRODUCT_SELECTOR_ID = "1F051395F1CC4A40ADFE5C440EBCAA7F";
  private static final String SHIPMENT_TABLE_ID = "319";

  /**
   * Performs a request for properties without filtering
   */
  @Test
  public void testFullList() throws Exception {
    Map<String, String> params = new HashMap<String, String>();
    params.put("inpobuiselSelectorId", PRODUCT_SELECTOR_ID);

    JSONObject resp = executeDSRequest(params);

    JSONArray data = resp.getJSONArray("data");

    assertTrue("data should contain several values, it has " + data.length(), data.length() > 1);
    assertTrue("totalRows should be bigger than 1, it is " + resp.getInt("totalRows"),
        resp.getInt("totalRows") > 1);
  }

  /**
   * Performs a request for properties filtering by property "id"
   */
  @Test
  public void testFilter() throws Exception {
    Map<String, String> params = new HashMap<String, String>();
    params.put("inpobuiselSelectorId", PRODUCT_SELECTOR_ID);
    params.putAll(getFilter("id"));

    JSONObject resp = executeDSRequest(params);

    JSONArray data = resp.getJSONArray("data");

    assertEquals("data length", data.length(), 1);
    assertEquals("totalRows", resp.getInt("totalRows"), 1);
  }

  /**
   * Testing issue #26432: in M_Inout table, filtering properties by salesOrder._Com should return
   * _Computed column proxy property
   * 
   */
  @Test
  public void testPropertyFieldComputedColumn1() throws Exception {
    Map<String, String> params = new HashMap<String, String>();
    params.put("inpadTableId", SHIPMENT_TABLE_ID);
    params.putAll(getFilter("salesOrder._Com"));

    JSONObject resp = executeDSRequest(params);

    JSONArray data = resp.getJSONArray("data");

    assertEquals("data length", 1, data.length());
    assertEquals("totalRows", 1, resp.getInt("totalRows"));
    assertEquals("salesOrder._computedColumns", data.getJSONObject(0).getString("property"));
  }

  /**
   * Testing issue #26432: in M_Inout table, filtering properties by salesOrder._ComputedColumns.
   * should return a list of all computed columns defined in C_Order table
   * 
   */
  @Test
  public void testPropertyFieldComputedColumn2() throws Exception {
    Map<String, String> params = new HashMap<String, String>();
    params.put("inpadTableId", SHIPMENT_TABLE_ID);
    params.putAll(getFilter("salesOrder._ComputedColumns."));

    JSONObject resp = executeDSRequest(params);

    JSONArray data = resp.getJSONArray("data");

    assertTrue("data should contain all computed columns", data.length() > 1);
    checkJSONcontains(data, "salesOrder._ComputedColumns.invoiceStatus");
  }

  private void checkJSONcontains(JSONArray data, String value) throws JSONException {
    for (int i = 0; i < data.length(); i++) {
      if (value.equals(data.getJSONObject(i).getString("property"))) {
        return;
      }
    }
    fail("Expecting value <" + value + "> in array but found:" + data.toString(1));

  }

  private Map<? extends String, ? extends String> getFilter(String value) throws JSONException {
    Map<String, String> params = new HashMap<String, String>();
    params.put("operator", "or");
    params.put("_constructor", "AdvancedCriteria");
    JSONObject criteria = new JSONObject();
    criteria.put("fieldName", "property");
    criteria.put("operator", "iContains");
    criteria.put("value", value);
    params.put("criteria", criteria.toString());
    return params;
  }

  private JSONObject executeDSRequest(Map<String, String> extraParams) throws Exception {
    if (!sysAdminProfileSet) {
      changeProfile("0", "192", "0", null);
      sysAdminProfileSet = true;
    }

    Map<String, String> params = new HashMap<String, String>();
    params.putAll(extraParams);

    // this is how value is sent when in new, regardless typed filter
    params.put("inpproperty", "null");

    params.put("_operationType", "fetch");
    params.put("filterClass", "org.openbravo.userinterface.selector.SelectorDataSourceFilter");
    params.put("_sortBy", "property");

    JSONObject resp = new JSONObject(doRequest(
        "/org.openbravo.service.datasource/83B60C4C19AE4A9EBA947B948C5BA04D", params, 200, "POST"));

    assertTrue("expecting response, got: " + resp, resp.has("response"));

    JSONObject r = resp.getJSONObject("response");
    log.debug("Response: {}", r);
    assertTrue("expecting data in response, got: " + r, r.has("data"));

    return r;
  }
}
