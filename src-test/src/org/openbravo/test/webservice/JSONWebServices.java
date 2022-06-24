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
 * All portions are Copyright (C) 2014-2015 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.webservice;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.test.base.Issue;

/**
 * Test cases for JSON dal web services
 * 
 * @author alostale
 *
 */
public class JSONWebServices extends BaseWSTest {
  @SuppressWarnings("serial")
  private final Set<String> expectedProperties = new HashSet<String>() {
    {
      // infrastructure properties, always present in all responses
      add("_identifier");
      add("_entityName");
      add("$ref");
      add("recordTime");

      // selected properties
      add("id");
      add("name");
    }
  };

  /** When getting a single record by id, selected properties should be taken into account */
  @Test
  public void selectedPropertiesUsingId() throws JSONException {
    JSONObject resp = new JSONObject(
        request("Country", "100", "_selectedProperties=id,name", "GET"));
    assertSingleRecord(resp);
  }

  /** When getting a set of records without id, selected properties should be taken into account. */
  @Test
  @Issue("28214")
  public void selectedPropertiesNotUsingId() throws JSONException {
    JSONObject resp = new JSONObject(request("Country", "", "_selectedProperties=id,name", "GET"));
    JSONObject firstRecord = resp.getJSONObject("response").getJSONArray("data").getJSONObject(0);
    assertSingleRecord(firstRecord);
  }

  private void assertSingleRecord(JSONObject resp) {
    @SuppressWarnings("unchecked")
    Iterator<String> it = resp.keys();
    Set<String> receivedProperties = new HashSet<String>();

    while (it.hasNext()) {
      receivedProperties.add(it.next());
    }

    assertThat("Properties received in JSON", receivedProperties, is(equalTo(expectedProperties)));
  }

  /** Asserts JSON REST web services support distinct parameter. */
  @Test
  @Issue("29385")
  public void distinctParameterShouldWork() throws JSONException {
    JSONObject resp = new JSONObject(request("ADPreference", null, "_distinct=module", "GET"))
        .getJSONObject("response");

    assertThat("Sucess status", resp.getInt("status"), is(0));
    assertThat("Total Rows", resp.getInt("totalRows"), is(greaterThan(0)));

    JSONObject aRecord = resp.getJSONArray("data").getJSONObject(0);
    assertThat("Row's entity", aRecord.getString("_entityName"), is(equalTo("ADModule")));
  }

  /**
   * Asserts JSON REST web services support distinct parameter for Organization, this case is
   * internally managed differently.
   */
  @Test
  @Issue("29385")
  public void distinctOrgParameterShouldWork() throws JSONException {
    JSONObject resp = new JSONObject(request("ADPreference", null, "_distinct=organization", "GET"))
        .getJSONObject("response");

    assertThat("Sucess status", resp.getInt("status"), is(0));
    assertThat("Total Rows", resp.getInt("totalRows"), is(greaterThan(0)));

    JSONObject aRecord = resp.getJSONArray("data").getJSONObject(0);
    assertThat("Row's entity", aRecord.getString("_entityName"), is(equalTo("Organization")));
  }

  protected String request(String entityName, String id, String queryPart, String method) {
    String wsPart = entityName + (id == null ? "" : "/" + id)
        + (queryPart == null ? "" : "?" + queryPart);

    final StringBuilder sb = new StringBuilder();
    try {

      final HttpURLConnection hc = createConnection(
          "/org.openbravo.service.json.jsonrest/" + wsPart, method);
      hc.connect();
      final InputStream is = hc.getInputStream();
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(is, StandardCharsets.UTF_8))) {

        String line;
        while ((line = reader.readLine()) != null) {
          sb.append(line).append("\n");
        }
        return sb.toString();
      }
    } catch (Exception e) {
      throw new OBException("Exception when executing ws: " + wsPart, e);
    }
  }
}
