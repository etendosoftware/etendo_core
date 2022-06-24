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
 * All portions are Copyright (C) 2016-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.views;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.util.ArrayList;
import java.util.Collection;

import org.codehaus.jettison.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.GCField;
import org.openbravo.client.application.GCSystem;
import org.openbravo.client.application.GCTab;
import org.openbravo.client.application.window.OBViewUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Test cases to check if the correct behavior with different combinations of sorting and filtering
 * properties in grid configurations at tab, field, system and column level are correct.
 * 
 */
@RunWith(Parameterized.class)
public class SortingFilteringGridConfiguration extends GridConfigurationTest {

  private static Boolean coreWasInDevelopment;

  /**
   * Execute these test cases only if there is no custom grid config as it could make unstable
   * results
   */
  @BeforeClass
  public static void shouldExecuteOnlyIfThereIsNoGridConfig() {
    assumeThat("Number of custom grid configs", getNumberOfGridConfigurations(), is(0));

    OBContext.setAdminMode(true);
    try {
      Module core = OBDal.getInstance().get(Module.class, "0");
      coreWasInDevelopment = core.isInDevelopment();
      if (!coreWasInDevelopment) {
        core.setInDevelopment(true);
        OBDal.getInstance().commitAndClose();
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @AfterClass
  public static void cleanUp() {
    if (getNumberOfGridConfigurations() > 0 || Boolean.TRUE.equals(coreWasInDevelopment)) {
      return;
    }
    OBContext.setAdminMode(true);
    try {
      Module core = OBDal.getInstance().get(Module.class, "0");
      core.setInDevelopment(coreWasInDevelopment);
      OBDal.getInstance().commitAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private enum ColumnLevel {
    SORTt_FILTERt(true, true), //
    SORTt_FILTERf(true, false), //
    SORTf_FILTERt(false, true), //
    SORTf_FILTERf(false, false);

    private boolean sort;
    private boolean filter;

    ColumnLevel(boolean sort, boolean filter) {
      this.sort = sort;
      this.filter = filter;
    }
  };

  private enum FieldLevel {
    NULL(null, null), //

    SORTt_FILTERt("Y", "Y"), //
    SORTt_FILTERf("Y", "N"), //
    SORTt_FILTERd("Y", "D"), //

    SORTf_FILTERt("N", "Y"), //
    SORTf_FILTERf("N", "N"), //
    SORTf_FILTERd("N", "D"), //

    SORTd_FILTERt("D", "Y"), //
    SORTd_FILTERf("D", "N"), //
    SORTd_FILTERd("D", "D");

    private String sort;
    private String filter;

    FieldLevel(String sort, String filter) {
      this.sort = sort;
      this.filter = filter;
    }
  };

  private enum TabLevel {
    NULL(null, null), //

    SORTt_FILTERt("Y", "Y"), //
    SORTt_FILTERf("Y", "N"), //
    SORTt_FILTERd("Y", "D"), //

    SORTf_FILTERt("N", "Y"), //
    SORTf_FILTERf("N", "N"), //
    SORTf_FILTERd("N", "D"), //

    SORTd_FILTERt("D", "Y"), //
    SORTd_FILTERf("D", "N"), //
    SORTd_FILTERd("D", "D");

    private String sort;
    private String filter;

    TabLevel(String sort, String filter) {
      this.sort = sort;
      this.filter = filter;
    }
  };

  private enum SysLevel {
    NULL(null, null), //

    SORTt_FILTERt(true, true), //
    SORTt_FILTERf(true, false), //
    SORTf_FILTERt(false, true), //
    SORTf_FILTERf(false, false);

    private Boolean sort;
    private Boolean filter;

    SysLevel(Boolean sort, Boolean filter) {
      this.sort = sort;
      this.filter = filter;
    }
  }

  private static final String BUSINESS_PARTNER_TAB_ID = "220";
  private static final String BUSINESS_PARTNER_CATEGORY_FIELD_ID = "3955";

  private ConfigSetting setting;

  @Parameters(name = "{0}")
  public static Collection<Object[]> parameters() {
    Collection<Object[]> params = new ArrayList<Object[]>();
    for (SysLevel sysLevel : SysLevel.values()) {
      for (TabLevel tabLevel : TabLevel.values()) {
        for (FieldLevel fieldLevel : FieldLevel.values()) {
          for (ColumnLevel columnLevel : ColumnLevel.values()) {
            if (!(fieldLevel != FieldLevel.NULL && tabLevel == TabLevel.NULL)) {
              params.add(
                  new Object[] { new ConfigSetting(sysLevel, tabLevel, fieldLevel, columnLevel) });
            }
          }
        }
      }
    }
    return params;
  }

  public SortingFilteringGridConfiguration(ConfigSetting setting) {
    this.setting = setting;
  }

  @Test
  public void gridConfigurationShouldBeApplied() {
    assertThat("Grid configuration at field level:", setting.computeResultForField(), allOf(
        containsString(setting.getExpectedFilter()), containsString(setting.getExpectedSort())));
  }

  private static class ConfigSetting {
    private SysLevel sysLevel;
    private TabLevel tabLevel;
    private FieldLevel fieldLevel;
    private ColumnLevel columnLevel;

    public ConfigSetting(SysLevel sysLevel, TabLevel tabLevel, FieldLevel fieldLevel,
        ColumnLevel columnLevel) {
      this.sysLevel = sysLevel;
      this.tabLevel = tabLevel;
      this.fieldLevel = fieldLevel;
      this.columnLevel = columnLevel;
    }

    public String computeResultForField() {
      JSONObject fieldConfig;
      GCSystem gcsystem = null;
      GCTab gctab = null;
      GCField gcfield = null;
      OBContext.setAdminMode(false);
      try {
        Field field = OBDal.getInstance().get(Field.class, BUSINESS_PARTNER_CATEGORY_FIELD_ID);
        field.getColumn().setAllowFiltering(columnLevel.filter);
        field.getColumn().setAllowSorting(columnLevel.sort);
        if (sysLevel != SysLevel.NULL) {
          gcsystem = OBProvider.getInstance().get(GCSystem.class);
          gcsystem.setClient(OBDal.getInstance().get(Client.class, "0"));
          gcsystem.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
          gcsystem.setFilterable(sysLevel.filter);
          gcsystem.setSortable(sysLevel.sort);
          gcsystem.setSeqno(10L);
          OBDal.getInstance().save(gcsystem);
        }
        if (tabLevel != TabLevel.NULL) {
          Tab tab = OBDal.getInstance().get(Tab.class, BUSINESS_PARTNER_TAB_ID);
          gctab = OBProvider.getInstance().get(GCTab.class);
          gctab.setClient(OBDal.getInstance().get(Client.class, "0"));
          gctab.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
          gctab.setFilterable(tabLevel.filter);
          gctab.setSortable(tabLevel.sort);
          gctab.setSeqno(10L);
          gctab.setTab(tab);
          OBDal.getInstance().save(gctab);
        }
        if (fieldLevel != FieldLevel.NULL) {
          field = OBDal.getInstance().get(Field.class, BUSINESS_PARTNER_CATEGORY_FIELD_ID);
          gcfield = OBProvider.getInstance().get(GCField.class);
          gcfield.setClient(OBDal.getInstance().get(Client.class, "0"));
          gcfield.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
          gcfield.setField(field);
          gcfield.setFilterable(fieldLevel.filter);
          gcfield.setSortable(fieldLevel.sort);
          gcfield.setObuiappGcTab(gctab);
          gctab.getOBUIAPPGCFieldList().add(gcfield);
          OBDal.getInstance().save(gcfield);
        }
        OBDal.getInstance().flush();
        field = OBDal.getInstance().get(Field.class, BUSINESS_PARTNER_CATEGORY_FIELD_ID);
        fieldConfig = OBViewUtil.getGridConfigurationSettings(field, getSystemGridConfig(),
            getTabGridConfig(field.getTab()));
        return fieldConfig.toString();
      } finally {
        OBDal.getInstance().rollbackAndClose();
        OBContext.restorePreviousMode();
      }
    }

    public String getExpectedSort() {
      boolean canSort = columnLevel.sort;
      if (!canSort) {
        if ("Y".equals(fieldLevel.sort)) {
          canSort = true;
        }
      } else if (fieldLevel != FieldLevel.NULL && !"D".equals(fieldLevel.sort)) {
        canSort = "Y".equals(fieldLevel.sort);
      } else if (tabLevel != TabLevel.NULL && !"D".equals(tabLevel.sort)) {
        canSort = "Y".equals(tabLevel.sort);
      } else if (sysLevel != SysLevel.NULL) {
        canSort = sysLevel.sort;
      }

      return "\"canSort\":" + canSort;
    }

    public String getExpectedFilter() {
      boolean canFilter = columnLevel.filter;
      if (!canFilter) {
        if ("Y".equals(fieldLevel.filter)) {
          canFilter = true;
        }
      } else if (fieldLevel != FieldLevel.NULL && !"D".equals(fieldLevel.filter)) {
        canFilter = "Y".equals(fieldLevel.filter);
      } else if (tabLevel != TabLevel.NULL && !"D".equals(tabLevel.filter)) {
        canFilter = "Y".equals(tabLevel.filter);
      } else if (sysLevel != SysLevel.NULL) {
        canFilter = sysLevel.filter;
      }

      return "\"canFilter\":" + canFilter;
    }

    @Override
    public String toString() {
      return "sys: " + sysLevel + " - tab: " + tabLevel + " - field: " + fieldLevel + " - column: "
          + columnLevel;
    }
  }
}
