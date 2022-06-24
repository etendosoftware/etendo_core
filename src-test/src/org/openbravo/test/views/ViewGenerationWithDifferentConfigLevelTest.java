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
 * All portions are Copyright (C) 2015-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.views;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.GCField;
import org.openbravo.client.application.GCSystem;
import org.openbravo.client.application.GCTab;
import org.openbravo.client.application.window.OBViewUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Test cases to check if the correct configurations are set, with the different grid configurations
 * in tab, field and in system level.
 * 
 * @author NaroaIriarte
 *
 */
public class ViewGenerationWithDifferentConfigLevelTest extends GridConfigurationTest {
  private static final String CLIENT_FOR_GC_SYSTEM_FIELD_TAB = "0";
  private static final String ZERO_ORGANIZATION = "0";
  private static final String BUSINESS_PARTNER_TAB_ID = "220";
  private static final String BUSINESS_PARTNER_CATEGORY_FIELD_ID = "3955";
  private static final String BUSINESS_PARTNER_TAB_CURRENCY_FIELD_ID = "0C6C5DF6CB874BC5A77C946231AA4E07";
  private static final String CAN_SORT_FALSE = "\"canSort\":false";
  private static final String CAN_SORT_TRUE = "\"canSort\":true";
  private static final String CAN_FILTER_FALSE = "\"canFilter\":false";
  private static final String CAN_FILTER_TRUE = "\"canFilter\":true";
  private static final long SEQUENCE = 10;

  /**
   * Execute these test cases only if there is no custom grid config as it could make unstable
   * results
   */
  @Before
  public void shouldExecuteOnlyIfThereIsNoGridConfig() {
    assumeThat("Number of custom grid configs", getNumberOfGridConfigurations(), is(0));
  }

  /**
   * Having only grid configuration in System level. In the configuration, the "by default allow
   * filtering" checkbox is checked, so, the expression "canFilter: true" must be found.
   */
  @Test
  public void gridConfigurationSystemLevel() throws Exception {
    GCSystem gcsystem = null;
    OBContext.setAdminMode(false);
    try {
      gcsystem = OBProvider.getInstance().get(GCSystem.class);
      gcsystem.setClient(OBDal.getInstance().get(Client.class, CLIENT_FOR_GC_SYSTEM_FIELD_TAB));
      gcsystem.setOrganization(OBDal.getInstance().get(Organization.class, ZERO_ORGANIZATION));
      gcsystem.setFilterable(true);
      gcsystem.setSeqno(SEQUENCE);
      OBDal.getInstance().save(gcsystem);
      OBDal.getInstance().flush();

      Field field = OBDal.getInstance().get(Field.class, BUSINESS_PARTNER_CATEGORY_FIELD_ID);
      JSONObject systemConfig = OBViewUtil.getGridConfigurationSettings(field,
          getSystemGridConfig(), getTabGridConfig(field.getTab()));

      assertThat("Grid configuration at system level with filtering enabled:",
          systemConfig.toString(), containsString(CAN_FILTER_TRUE));
    } finally {
      OBDal.getInstance().rollbackAndClose();
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Having only grid configuration at tab level. The "allow filtering" property in the grid
   * configuration at tab level, in the Business Partner tab has been set to "No". So this test
   * checks that the "canFilter: false" expression is present.
   */
  @Test
  public void gridConfigurationTabLevel() throws Exception {
    GCTab gctab = null;
    OBContext.setAdminMode(false);
    try {
      Tab tab = OBDal.getInstance().get(Tab.class, BUSINESS_PARTNER_TAB_ID);
      gctab = OBProvider.getInstance().get(GCTab.class);
      gctab.setClient(OBDal.getInstance().get(Client.class, CLIENT_FOR_GC_SYSTEM_FIELD_TAB));
      gctab.setOrganization(OBDal.getInstance().get(Organization.class, ZERO_ORGANIZATION));
      gctab.setFilterable("N");
      gctab.setSeqno(SEQUENCE);
      gctab.setTab(tab);
      tab.getOBUIAPPGCTabList().add(gctab);
      OBDal.getInstance().save(gctab);
      OBDal.getInstance().flush();

      tab = OBDal.getInstance().get(Tab.class, BUSINESS_PARTNER_TAB_ID);
      JSONObject tabConfig = OBViewUtil.getGridConfigurationSettings(getSystemGridConfig(),
          getTabGridConfig(tab));

      assertThat("Grid configuration at tab level with filtering disabled:", tabConfig.toString(),
          containsString(CAN_FILTER_FALSE));
    } finally {
      OBDal.getInstance().rollbackAndClose();
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Having grid configuration at System and tab level. The "allow filtering" property in the grid
   * configuration at tab level, in the Business Partner tab has been set to "No". So this test
   * checks that the "canFilter: false" expression is present. Also, the "allow sorting" property
   * has been set to default, so, the taken value is going to be the one set in the grid
   * configuration at system level, which is true. The test checks that the "canSort: true"
   * expression is present.
   */
  @Test
  public void gridConfigurationTabAndSystemLevel() throws Exception {
    GCSystem gcsystem = null;
    GCTab gctab = null;
    OBContext.setAdminMode(false);
    try {
      gcsystem = OBProvider.getInstance().get(GCSystem.class);
      gctab = OBProvider.getInstance().get(GCTab.class);
      gcsystem.setClient(OBDal.getInstance().get(Client.class, CLIENT_FOR_GC_SYSTEM_FIELD_TAB));
      gcsystem.setOrganization(OBDal.getInstance().get(Organization.class, ZERO_ORGANIZATION));
      gcsystem.setSortable(true);
      gcsystem.setSeqno(SEQUENCE);
      OBDal.getInstance().save(gcsystem);
      OBDal.getInstance().flush();

      Tab tab = OBDal.getInstance().get(Tab.class, BUSINESS_PARTNER_TAB_ID);
      gctab.setClient(OBDal.getInstance().get(Client.class, CLIENT_FOR_GC_SYSTEM_FIELD_TAB));
      gctab.setOrganization(OBDal.getInstance().get(Organization.class, ZERO_ORGANIZATION));
      gctab.setFilterable("N");
      gctab.setSeqno(SEQUENCE);
      gctab.setTab(tab);

      tab.getOBUIAPPGCTabList().add(gctab);
      OBDal.getInstance().save(gctab);
      OBDal.getInstance().flush();

      tab = OBDal.getInstance().get(Tab.class, BUSINESS_PARTNER_TAB_ID);

      JSONObject tabConfig = OBViewUtil.getGridConfigurationSettings(getSystemGridConfig(),
          getTabGridConfig(tab));

      Field field = OBDal.getInstance().get(Field.class, BUSINESS_PARTNER_TAB_CURRENCY_FIELD_ID);
      JSONObject systemConfig = OBViewUtil.getGridConfigurationSettings(field,
          getSystemGridConfig(), getTabGridConfig(field.getTab()));

      assertThat(
          "Grid configuration at system level with sorting enabled and grid configuration at tab level with filtering disabled:",
          tabConfig.toString(), containsString(CAN_FILTER_FALSE));
      assertThat(
          "Grid configuration at field level with sorting diabled for the business partner category field, but enabled at system level for any other field:",
          systemConfig.toString(), containsString(CAN_SORT_TRUE));
    } finally {
      OBDal.getInstance().rollbackAndClose();
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Having only grid configuration at field level. The Business Partner category field of business
   * Partner has the property allow sorting set to "Yes", so the view must have "canSort: true"
   * expression.
   */
  @Test
  public void gridConfigurationFieldLevel() throws Exception {
    GCTab gctab = null;
    GCField gcfield = null;
    OBContext.setAdminMode(false);
    try {
      gctab = OBProvider.getInstance().get(GCTab.class);
      gcfield = OBProvider.getInstance().get(GCField.class);
      gctab.setClient(OBDal.getInstance().get(Client.class, CLIENT_FOR_GC_SYSTEM_FIELD_TAB));
      gctab.setOrganization(OBDal.getInstance().get(Organization.class, ZERO_ORGANIZATION));
      gctab.setSeqno(SEQUENCE);
      Tab tab = OBDal.getInstance().get(Tab.class, BUSINESS_PARTNER_TAB_ID);
      tab.getOBUIAPPGCTabList().add(gctab);
      OBDal.getInstance().save(gctab);

      gcfield.setClient(OBDal.getInstance().get(Client.class, CLIENT_FOR_GC_SYSTEM_FIELD_TAB));
      gcfield.setOrganization(OBDal.getInstance().get(Organization.class, ZERO_ORGANIZATION));
      gcfield.setField(OBDal.getInstance().get(Field.class, BUSINESS_PARTNER_CATEGORY_FIELD_ID));
      gcfield.setSortable("Y");
      gctab.getOBUIAPPGCFieldList().add(gcfield);
      OBDal.getInstance().save(gcfield);

      Field field = OBDal.getInstance().get(Field.class, BUSINESS_PARTNER_CATEGORY_FIELD_ID);
      JSONObject fieldConfig = OBViewUtil.getGridConfigurationSettings(field, getSystemGridConfig(),
          getTabGridConfig(field.getTab()));

      assertThat(
          "Grid configuration at field level with sorting enabled for the business partner category field:",
          fieldConfig.toString(), containsString(CAN_SORT_TRUE));

    } finally {
      OBDal.getInstance().rollbackAndClose();
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Having grid configuration at field and System level. In the grid configuration at system level,
   * the "by default allow sorting" checkbox is checked. The tests ensures that the expression
   * "canSort: true" exists. The business partner category of the business partner tab has the allow
   * filtering property set to "No". The test ensures that the "canFilter: false" expression is
   * present.
   */
  @Test
  public void gridConfigurationFieldAndSystemLevel() throws Exception {
    GCSystem gcsystem = null;
    GCTab gctab = null;
    GCField gcfield = null;
    OBContext.setAdminMode(false);
    try {
      gcsystem = OBProvider.getInstance().get(GCSystem.class);
      gcsystem.setClient(OBDal.getInstance().get(Client.class, CLIENT_FOR_GC_SYSTEM_FIELD_TAB));
      gcsystem.setOrganization(OBDal.getInstance().get(Organization.class, ZERO_ORGANIZATION));
      gcsystem.setSortable(true);
      gcsystem.setSeqno(SEQUENCE);
      OBDal.getInstance().save(gcsystem);
      OBDal.getInstance().flush();

      Tab tab = OBDal.getInstance().get(Tab.class, BUSINESS_PARTNER_TAB_ID);
      gctab = OBProvider.getInstance().get(GCTab.class);
      gctab.setClient(OBDal.getInstance().get(Client.class, CLIENT_FOR_GC_SYSTEM_FIELD_TAB));
      gctab.setOrganization(OBDal.getInstance().get(Organization.class, ZERO_ORGANIZATION));
      gctab.setSeqno(SEQUENCE);
      gctab.setTab(tab);
      tab.getOBUIAPPGCTabList().add(gctab);
      OBDal.getInstance().save(gctab);

      gcfield = OBProvider.getInstance().get(GCField.class);
      gcfield.setClient(OBDal.getInstance().get(Client.class, CLIENT_FOR_GC_SYSTEM_FIELD_TAB));
      gcfield.setOrganization(OBDal.getInstance().get(Organization.class, ZERO_ORGANIZATION));
      gcfield.setField(OBDal.getInstance().get(Field.class, BUSINESS_PARTNER_CATEGORY_FIELD_ID));
      gcfield.setSortable("N");
      gcfield.setObuiappGcTab(gctab);
      gctab.getOBUIAPPGCFieldList().add(gcfield);
      OBDal.getInstance().save(gcfield);
      OBDal.getInstance().flush();

      Field field = OBDal.getInstance().get(Field.class, BUSINESS_PARTNER_CATEGORY_FIELD_ID);
      JSONObject fieldConfig = OBViewUtil.getGridConfigurationSettings(field, getSystemGridConfig(),
          getTabGridConfig(field.getTab()));

      field = OBDal.getInstance().get(Field.class, BUSINESS_PARTNER_TAB_CURRENCY_FIELD_ID);
      JSONObject systemConfig = OBViewUtil.getGridConfigurationSettings(field,
          getSystemGridConfig(), getTabGridConfig(field.getTab()));

      assertThat(
          "Grid configuration at field level with sorting diabled for the business partner category field, but enabled at system level for any other field:",
          fieldConfig.toString(), containsString(CAN_SORT_FALSE));
      assertThat(
          "Grid configuration at field level with sorting diabled for the business partner category field, but enabled at system level for any other field:",
          systemConfig.toString(), containsString(CAN_SORT_TRUE));

    } finally {
      OBDal.getInstance().rollbackAndClose();
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Having grid configuration at field and tab level. The field Business Partner category of
   * Business Parter has the property "allow sorting" set to "Yes". The test checks if the "canSort:
   * true" exists. In the tab configuration the allow filtering property is set to no, so the
   * "canFilter: false" must exist.
   */
  @Test
  public void gridConfigurationFieldAndTabLevel() throws Exception {
    GCTab gctab = null;
    GCField gcfield = null;
    OBContext.setAdminMode(false);
    try {
      Tab tab = OBDal.getInstance().get(Tab.class, BUSINESS_PARTNER_TAB_ID);
      gctab = OBProvider.getInstance().get(GCTab.class);
      gctab.setClient(OBDal.getInstance().get(Client.class, CLIENT_FOR_GC_SYSTEM_FIELD_TAB));
      gctab.setOrganization(OBDal.getInstance().get(Organization.class, ZERO_ORGANIZATION));
      gctab.setFilterable("N");
      gctab.setSeqno(SEQUENCE);
      gctab.setTab(tab);
      tab.getOBUIAPPGCTabList().add(gctab);
      OBDal.getInstance().save(gctab);

      gcfield = OBProvider.getInstance().get(GCField.class);
      gcfield.setClient(OBDal.getInstance().get(Client.class, CLIENT_FOR_GC_SYSTEM_FIELD_TAB));
      gcfield.setOrganization(OBDal.getInstance().get(Organization.class, ZERO_ORGANIZATION));
      gcfield.setField(OBDal.getInstance().get(Field.class, BUSINESS_PARTNER_CATEGORY_FIELD_ID));
      gcfield.setSortable("Y");
      gcfield.setObuiappGcTab(gctab);
      gctab.getOBUIAPPGCFieldList().add(gcfield);
      OBDal.getInstance().save(gcfield);
      OBDal.getInstance().flush();

      tab = OBDal.getInstance().get(Tab.class, BUSINESS_PARTNER_TAB_ID);
      JSONObject tabConfig = OBViewUtil.getGridConfigurationSettings(getSystemGridConfig(),
          getTabGridConfig(tab));

      Field field = OBDal.getInstance().get(Field.class, BUSINESS_PARTNER_CATEGORY_FIELD_ID);
      JSONObject fieldConfig = OBViewUtil.getGridConfigurationSettings(field, getSystemGridConfig(),
          getTabGridConfig(field.getTab()));

      assertThat(
          "Grid configuration at tab level with filtering disabled for the business partner tab:",
          tabConfig.toString(), containsString(CAN_FILTER_FALSE));
      assertThat(
          "Grid configuration at field level with sorting enabled for the business partner category field:",
          fieldConfig.toString(), containsString(CAN_SORT_TRUE));
    } finally {
      OBDal.getInstance().rollbackAndClose();
      OBContext.restorePreviousMode();
    }
  }
}
