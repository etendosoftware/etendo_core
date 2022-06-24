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

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.openbravo.base.exception.OBException;

/**
 * Test case to ensure the cookie is regenerated each time the user log in the application
 *
 * @author jorge.garcia
 *
 */
public class ResetCookieOnLogin extends BaseDataSourceTestDal {
  private String cookie;
  private String JSESSIONID1;
  private String JSESSIONID2;

  @Test
  public void roleShouldBeResetOnLogin() throws Exception {
    Matcher matcher;
    String pattern = "JSESSIONID=([a-zA-Z0-9]+).*";
    final HttpURLConnection hc = DatasourceTestUtil.createConnection(getOpenbravoURL(),
        "/security/Login_FS.html", "GET", null);
    final OutputStream os = hc.getOutputStream();
    os.flush();
    os.close();
    hc.connect();
    cookie = hc.getHeaderField("Set-Cookie");
    matcher = Pattern.compile(pattern).matcher(cookie);
    if (!matcher.find()) {
      throw new OBException("No JSESSIONID found in cookie");
    }
    JSESSIONID1 = matcher.group(1);
    cookie = DatasourceTestUtil.authenticate(getOpenbravoURL(), getLogin(), getPassword());
    matcher = Pattern.compile(pattern).matcher(cookie);
    if (!matcher.find()) {
      throw new OBException("No JSESSIONID found in cookie");
    }
    JSESSIONID2 = matcher.group(1);

    if (JSESSIONID1.equals(JSESSIONID2)) {
      throw new OBException("JSESSIONID after loginshould be different from: " + JSESSIONID1);
    }

  }

}
