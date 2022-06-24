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

package org.openbravo.test.views;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.net.HttpURLConnection;

import javax.servlet.http.HttpServletResponse;

import org.hibernate.query.Query;
import org.junit.Test;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.test.base.TestConstants.Tables;
import org.openbravo.test.base.TestConstants.Windows;
import org.openbravo.test.datasource.BaseDataSourceTestDal;
import org.openbravo.test.datasource.DatasourceTestUtil;

/** Test cases covering ETag management for generated views */
public class ETagGeneration extends BaseDataSourceTestDal {

  @Test
  public void eTagShouldBeStable() throws Exception {
    assumeThat("Has modules in development", hasModulesInDevelopment(), is(false));

    String eTag = getEtag(Windows.SALES_ORDER);

    assertResponseCode("Response without changes", Windows.SALES_ORDER, eTag,
        HttpServletResponse.SC_NOT_MODIFIED);
  }

  @Test
  public void serverDisplayLogicConfigShouldChangeETag() throws Exception {
    assumeThat("Has modules in development", hasModulesInDevelopment(), is(false));

    Preference newPref = null;
    setSystemAdministratorContext();
    try {
      String oldEtag = getEtag(Windows.SALES_ORDER);

      if (!Preferences.existsPreference("UomManagement", true, "0", "0", null, null, null)) {
        newPref = Preferences.setPreferenceValue("UomManagement", "Y", true,
            OBDal.getInstance().getProxy(Client.class, "0"),
            OBDal.getInstance().getProxy(Organization.class, "0"), null, null, null, null);
      }
      OBDal.getInstance().commitAndClose();

      String newEtag = getEtag(Windows.SALES_ORDER);
      assertThat("ETag should change", newEtag, is(not(oldEtag)));

      assertResponseCode("Response after adding server dl config", Windows.SALES_ORDER, oldEtag,
          HttpServletResponse.SC_OK);
      assertResponseCode("Response on 2nd request after change in dl server config",
          Windows.SALES_ORDER, newEtag, HttpServletResponse.SC_NOT_MODIFIED);
    } finally {
      if (newPref != null) {
        OBDal.getInstance().remove(newPref);
      }
    }
  }

  @Test
  public void auditTableShouldChangeETag() throws Exception {
    assumeThat("Has modules in development", hasModulesInDevelopment(), is(false));
    setSystemAdministratorContext();
    boolean wasAudited = false;

    try {
      String oldEtag = getEtag(Windows.SALES_ORDER);

      Table orderTable = OBDal.getInstance().get(Table.class, Tables.C_ORDER);
      wasAudited = orderTable.isFullyAudited();
      orderTable.setFullyAudited(!wasAudited);
      OBDal.getInstance().commitAndClose();

      String newEtag = getEtag(Windows.SALES_ORDER);
      assertThat("ETag should change", newEtag, is(not(oldEtag)));

      assertResponseCode("Response after adding server dl config", Windows.SALES_ORDER, oldEtag,
          HttpServletResponse.SC_OK);
      assertResponseCode("Response on 2nd request after change audit config", Windows.SALES_ORDER,
          newEtag, HttpServletResponse.SC_NOT_MODIFIED);
    } finally {
      Table orderTable = OBDal.getInstance().get(Table.class, Tables.C_ORDER);
      orderTable.setFullyAudited(wasAudited);
    }
  }

  private Boolean hasModulesInDevelopment() {
    final Query<Object> indevelMods = OBDal.getInstance()
        .getSession()
        .createQuery("select 1 from ADModule m where m.inDevelopment=true", Object.class);
    indevelMods.setMaxResults(1);
    return indevelMods.list().size() > 0;
  }

  private void assertResponseCode(String msg, String windowId, String eTag, int expectedCode)
      throws Exception {
    HttpURLConnection conn = getConnection(windowId);
    conn.setRequestProperty("If-None-Match", eTag);
    conn.connect();

    assertThat(msg, conn.getResponseCode(), is(expectedCode));
  }

  private String getEtag(String windowId) throws Exception {
    HttpURLConnection conn = getConnection(windowId);
    conn.connect();
    return conn.getHeaderField("Etag");
  }

  private HttpURLConnection getConnection(String windowId) throws Exception {
    String cookie = authenticate();
    HttpURLConnection conn = DatasourceTestUtil.createConnection(getOpenbravoURL(),
        "/org.openbravo.client.kernel/OBUIAPP_MainLayout/View?viewId=_" + windowId, "GET", cookie);
    return conn;
  }
}
