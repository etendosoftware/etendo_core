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
 * All portions are Copyright (C) 2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.datasource;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

/**
 * Test case to cover the functionality of automatically open a record in particular after login,
 * this can be done by appending to the Login page a URL query string containing the document link.
 */
public class OpenRecordAfterLogin extends BaseDataSourceTestDal {

  private static final String URL_QUERY_STRING = "tabId=186&recordId=05C426107193402BBD8379EF92A63E96";

  @Test
  public void targetQueryStringIsKeptAfterLogin() throws Exception {
    // Request for the Openbravo base URL with query string
    String redirectedLoginURL = doRequestForLoginPage();
    assertThat(redirectedLoginURL, equalTo(getLoginURLWithQueryString()));
    // Now we do the login
    String urlAfterLogin = doLoginHandlerRequest();
    assertThat(urlAfterLogin, equalTo(getOpenbravoURLWithQueryString()));
  }

  private String doRequestForLoginPage() throws Exception {
    final HttpURLConnection hc = DatasourceTestUtil.createConnection(getOpenbravoURL(),
        "/?" + URL_QUERY_STRING, "GET", null);
    try (OutputStream os = hc.getOutputStream()) {
      os.flush();
      hc.connect();
    }
    assertThat(hc.getResponseCode(), equalTo(HttpURLConnection.HTTP_OK));
    // A redirect to the login page should be done, HttpURLConnection handles redirection
    // automatically. Therefore with getURL() method we should retrieve the redirected URL.
    return hc.getURL().toString();
  }

  private String doLoginHandlerRequest() throws UnsupportedEncodingException, Exception {
    final HttpURLConnection hc = DatasourceTestUtil.createConnection(getOpenbravoURL(),
        getLoginHandlerRelativeURLWithQueryString(), "POST", null);
    try (OutputStream os = hc.getOutputStream()) {
      String content = "user=" + LOGIN + "&password=" + PWD;
      os.write(content.getBytes("UTF-8"));
      os.flush();
      hc.connect();
    }
    assertThat(hc.getResponseCode(), equalTo(HttpURLConnection.HTTP_OK));
    StringBuilder sb = new StringBuilder();
    JSONObject response;
    try (BufferedReader br = new BufferedReader(new InputStreamReader(hc.getInputStream()))) {
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
      response = new JSONObject(sb.toString());
    }
    // After the login we are redirected to the URL inside the "target" property of the response
    return response != null && response.has("target") ? response.getString("target") : null;
  }

  private String getOpenbravoURLWithQueryString() {
    return getOpenbravoURL() + "/?" + URL_QUERY_STRING;
  }

  private String getLoginURLWithQueryString() {
    return getOpenbravoURL() + "/security/Login?" + URL_QUERY_STRING;
  }

  private String getLoginHandlerRelativeURLWithQueryString() throws UnsupportedEncodingException {
    String encodedTargetQueryString = URLEncoder.encode(URL_QUERY_STRING, "UTF-8");
    return "/secureApp/LoginHandler.html?targetQueryString=" + encodedTargetQueryString;
  }
}
