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
 * All portions are Copyright (C)2016-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.webservice;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

/**
 * This test evaluates the correct behavior of a JSON webservice request using the "_where"
 * parameter.
 * 
 * @author Naroa Iriarte
 * 
 */
public class JSONWebServicesWhereParameter extends BaseWSTest {
  private static final String SPAIN_ID = "106";

  @Test
  public void WebserviceWithWhereParameter() throws Exception {
    JSONWebServices jws = new JSONWebServices();
    JSONObject resp = new JSONObject(
        jws.request("Country", null, "_where=iSOCountryCode='ES'", "GET"))
            .getJSONObject("response");
    assertThat("Total Rows", resp.getInt("totalRows"), is(1));
    JSONObject firstRecord = resp.getJSONArray("data").getJSONObject(0);
    assertThat("Spanish Country", firstRecord.getString("id"), is(equalTo(SPAIN_ID)));
    assertThat("Sucess status", resp.getInt("status"), is(0));
  }
}
