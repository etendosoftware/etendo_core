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
import org.openbravo.client.application.GCTab;
import org.openbravo.client.application.window.OBViewUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Test cases with different combinations of grid configurations at tab level with different
 * combinations of sequence numbers.
 * 
 * @author NaroaIriarte
 *
 */
public class GCSequenceNumberTests extends GridConfigurationTest {
  private static final String CLIENT_FOR_GC_SYSTEM_FIELD_TAB = "0";
  private static final String ZERO_ORGANIZATION = "0";
  private static final String BUSINESS_PARTNER_TAB_ID = "220";
  private static final String PRODUCT_TAB_ID = "180";
  private static final String CAN_FILTER_FALSE = "\"canFilter\":false";
  private static final String CAN_FILTER_TRUE = "\"canFilter\":true";
  private static final String YES = "Y";
  private static final String NO = "N";
  private static final long LOW_SEQUENCE_NUMBER = 10;
  private static final long HIGH_SEQUENCE_NUMBER = 20;

  /**
   * Execute these test cases only if there is no custom grid config as it could make unstable
   * results
   */
  @Before
  public void shouldExecuteOnlyIfThereIsNoGridConfig() {
    assumeThat("Number of custom grid configs", getNumberOfGridConfigurations(), is(0));
  }

  /**
   * If different sequence number is set for the same tab, the expected behavior is that the
   * configuration set in the record with the highest sequence number be applied.
   */
  @Test
  public void sameTabDifferentSequenceNumber() throws Exception {
    GCTab gctabFirstRecord = null;
    GCTab gctabSecondRecord = null;
    OBContext.setAdminMode(false);
    try {
      gctabFirstRecord = OBProvider.getInstance().get(GCTab.class);
      gctabSecondRecord = OBProvider.getInstance().get(GCTab.class);
      gctabFirstRecord
          .setClient(OBDal.getInstance().get(Client.class, CLIENT_FOR_GC_SYSTEM_FIELD_TAB));
      gctabFirstRecord
          .setOrganization(OBDal.getInstance().get(Organization.class, ZERO_ORGANIZATION));
      gctabFirstRecord.setFilterable(YES);
      gctabFirstRecord.setSeqno(HIGH_SEQUENCE_NUMBER);
      Tab firstTab = OBDal.getInstance().get(Tab.class, BUSINESS_PARTNER_TAB_ID);
      firstTab.getOBUIAPPGCTabList().add(gctabFirstRecord);
      OBDal.getInstance().save(gctabFirstRecord);

      gctabSecondRecord
          .setClient(OBDal.getInstance().get(Client.class, CLIENT_FOR_GC_SYSTEM_FIELD_TAB));
      gctabSecondRecord
          .setOrganization(OBDal.getInstance().get(Organization.class, ZERO_ORGANIZATION));
      gctabSecondRecord.setFilterable(NO);
      gctabSecondRecord.setSeqno(LOW_SEQUENCE_NUMBER);
      firstTab.getOBUIAPPGCTabList().add(gctabSecondRecord);
      OBDal.getInstance().save(gctabSecondRecord);

      Tab tab = OBDal.getInstance().get(Tab.class, BUSINESS_PARTNER_TAB_ID);
      JSONObject tabConfig = OBViewUtil.getGridConfigurationSettings(getSystemGridConfig(),
          getTabGridConfig(tab));

      assertThat("Grid configuration in business partner tab with filtering enabled:",
          tabConfig.toString(), containsString(CAN_FILTER_TRUE));

    } finally {
      OBDal.getInstance().rollbackAndClose();
      OBContext.restorePreviousMode();
    }
  }

  /**
   * If the same sequence number is set for different tabs, the expected behavior is that set
   * configurations for these tabs be applied.
   */
  @Test
  public void differentTabSameSequenceNumber() throws Exception {
    GCTab gctabFirstRecord = null;
    GCTab gctabSecondRecord = null;
    OBContext.setAdminMode(false);
    try {
      Tab firstTab = OBDal.getInstance().get(Tab.class, BUSINESS_PARTNER_TAB_ID);
      gctabFirstRecord = OBProvider.getInstance().get(GCTab.class);
      gctabSecondRecord = OBProvider.getInstance().get(GCTab.class);
      gctabFirstRecord
          .setClient(OBDal.getInstance().get(Client.class, CLIENT_FOR_GC_SYSTEM_FIELD_TAB));
      gctabFirstRecord
          .setOrganization(OBDal.getInstance().get(Organization.class, ZERO_ORGANIZATION));
      gctabFirstRecord.setFilterable(YES);
      gctabFirstRecord.setSeqno(LOW_SEQUENCE_NUMBER);
      gctabFirstRecord.setTab(firstTab);
      firstTab.getOBUIAPPGCTabList().add(gctabFirstRecord);
      OBDal.getInstance().save(gctabFirstRecord);

      Tab secondTab = OBDal.getInstance().get(Tab.class, PRODUCT_TAB_ID);
      gctabSecondRecord
          .setClient(OBDal.getInstance().get(Client.class, CLIENT_FOR_GC_SYSTEM_FIELD_TAB));
      gctabSecondRecord
          .setOrganization(OBDal.getInstance().get(Organization.class, ZERO_ORGANIZATION));
      gctabSecondRecord.setFilterable(NO);
      gctabSecondRecord.setSeqno(LOW_SEQUENCE_NUMBER);
      gctabSecondRecord.setTab(secondTab);
      secondTab.getOBUIAPPGCTabList().add(gctabSecondRecord);
      OBDal.getInstance().save(gctabSecondRecord);

      OBDal.getInstance().flush();

      Tab tab = OBDal.getInstance().get(Tab.class, BUSINESS_PARTNER_TAB_ID);
      JSONObject bpTabConfig = OBViewUtil.getGridConfigurationSettings(getSystemGridConfig(),
          getTabGridConfig(tab));

      tab = OBDal.getInstance().get(Tab.class, PRODUCT_TAB_ID);
      JSONObject productTabConfig = OBViewUtil.getGridConfigurationSettings(getSystemGridConfig(),
          getTabGridConfig(tab));

      assertThat("Grid configuration in business partner tab with filtering enabled:",
          bpTabConfig.toString(), containsString(CAN_FILTER_TRUE));
      assertThat("Grid configuration in product tab with filtering disabled:",
          productTabConfig.toString(), containsString(CAN_FILTER_FALSE));

    } finally {
      OBDal.getInstance().rollbackAndClose();
      OBContext.restorePreviousMode();
    }
  }

  /**
   * If different sequence number is set for different tabs the expected result is that the set
   * configuration be applied for these tabs.
   */
  @Test
  public void differentTabDifferentSequenceNumber() throws Exception {
    GCTab gctabFirstRecord = null;
    GCTab gctabSecondRecord = null;
    OBContext.setAdminMode(false);
    try {
      Tab firstTab = OBDal.getInstance().get(Tab.class, BUSINESS_PARTNER_TAB_ID);
      gctabFirstRecord = OBProvider.getInstance().get(GCTab.class);
      gctabSecondRecord = OBProvider.getInstance().get(GCTab.class);
      gctabFirstRecord
          .setClient(OBDal.getInstance().get(Client.class, CLIENT_FOR_GC_SYSTEM_FIELD_TAB));
      gctabFirstRecord
          .setOrganization(OBDal.getInstance().get(Organization.class, ZERO_ORGANIZATION));
      gctabFirstRecord.setFilterable(YES);
      gctabFirstRecord.setSeqno(HIGH_SEQUENCE_NUMBER);
      gctabFirstRecord.setTab(firstTab);
      firstTab.getOBUIAPPGCTabList().add(gctabFirstRecord);
      OBDal.getInstance().save(gctabFirstRecord);

      Tab secondTab = OBDal.getInstance().get(Tab.class, PRODUCT_TAB_ID);
      gctabSecondRecord
          .setClient(OBDal.getInstance().get(Client.class, CLIENT_FOR_GC_SYSTEM_FIELD_TAB));
      gctabSecondRecord
          .setOrganization(OBDal.getInstance().get(Organization.class, ZERO_ORGANIZATION));
      gctabSecondRecord.setFilterable(NO);
      gctabSecondRecord.setSeqno(LOW_SEQUENCE_NUMBER);
      gctabSecondRecord.setTab(secondTab);
      secondTab.getOBUIAPPGCTabList().add(gctabSecondRecord);
      OBDal.getInstance().save(gctabSecondRecord);

      OBDal.getInstance().flush();

      Tab tab = OBDal.getInstance().get(Tab.class, BUSINESS_PARTNER_TAB_ID);
      JSONObject bpTabConfig = OBViewUtil.getGridConfigurationSettings(getSystemGridConfig(),
          getTabGridConfig(tab));

      tab = OBDal.getInstance().get(Tab.class, PRODUCT_TAB_ID);
      JSONObject productTabConfig = OBViewUtil.getGridConfigurationSettings(getSystemGridConfig(),
          getTabGridConfig(tab));

      assertThat("Grid configuration in business partner tab with filtering enabled:",
          bpTabConfig.toString(), containsString(CAN_FILTER_TRUE));
      assertThat("Grid configuration in product tab level with filtering disabled:",
          productTabConfig.toString(), containsString(CAN_FILTER_FALSE));

    } finally {
      OBDal.getInstance().rollbackAndClose();
      OBContext.restorePreviousMode();
    }
  }
}
