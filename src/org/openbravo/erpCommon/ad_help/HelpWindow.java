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
 * All portions are Copyright (C) 2001-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_help;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.GCField;
import org.openbravo.client.application.GCSystem;
import org.openbravo.client.application.GCTab;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.ModulesInOB3Distribution;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.xmlEngine.XmlDocument;
import org.openbravo.xmlEngine.XmlEngine;

public class HelpWindow {

  /**
   * Currently wiki help is generated just for modules in Openbravo 3 distribution.
   * <code>ob3DistroModules</code> maintains the list of modules within it, so link to wiki help is
   * only shown for them.
   */
  private static final List<String> ob3DistroModules = ModulesInOB3Distribution.getModules();

  private static final String COMPATIBILITY_MODULE = "677192D0C60F411384832241227360E3";

  private static Logger log4j = LogManager.getLogger();

  public static String generateWindow(ConnectionProvider conn, XmlEngine xmlEngine,
      VariablesSecureApp vars, boolean discardEdit, String strKeyId)
      throws IOException, ServletException {
    String localStrKeyId = strKeyId;
    OBContext.setAdminMode();
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: Help Window");
    }
    Boolean window = true;
    String[] discard = { "", "", "", "", "", "", "" };
    String strType = "";
    String strWindowName = "";
    String strWindowHelp = "";
    String strBaseName = "";
    String strCommand = "";
    boolean showWikiLink = false;
    int nDiscards = 0;

    Language language = OBContext.getOBContext().getLanguage();
    final String YES = Utility.messageBD(conn, "OBUIAPP_Yes", language.getLanguage());
    final String NO = Utility.messageBD(conn, "OBUIAPP_No", language.getLanguage());

    final String SORTABLE_FIELD_ID = "44109E9F610A48AEBBB3AEF767C46FDF";
    final String FILTERABLE_FIELD_ID = "8869F84F7E24490496AF3081B9E5BB38";
    final String TEXT_FILTER_BEHAVIOR_FIELD_ID = "FBB6C7E587984DBC9AB858EC876EC1E5";
    final String LAZY_FILTERING_FIELD_ID = "B222E0B3174A43D0935CCDD7520C37EF";
    final String FILTER_ON_CHANGE_FIELD_ID = "C62B99B9A0BE4A59B695BB731B61899C";
    final String FILTER_THRESHOLD_FIELD_ID = "3C47334AF3154A668BEBF2AB21EA046B";

    final String SORTABLE = (String) OBDal.getInstance()
        .get(Field.class, SORTABLE_FIELD_ID)
        .get(Field.PROPERTY_NAME, language);
    final String FILTERABLE = (String) OBDal.getInstance()
        .get(Field.class, FILTERABLE_FIELD_ID)
        .get(Field.PROPERTY_NAME, language);
    final String TEXT_FILTER_BEHAVIOR = (String) OBDal.getInstance()
        .get(Field.class, TEXT_FILTER_BEHAVIOR_FIELD_ID)
        .get(Field.PROPERTY_NAME, language);
    final String LAZY_FILTERING = (String) OBDal.getInstance()
        .get(Field.class, LAZY_FILTERING_FIELD_ID)
        .get(Field.PROPERTY_NAME, language);
    final String FILTER_ON_CHANGE = (String) OBDal.getInstance()
        .get(Field.class, FILTER_ON_CHANGE_FIELD_ID)
        .get(Field.PROPERTY_NAME, language);
    final String FILTER_THRESHOLD = (String) OBDal.getInstance()
        .get(Field.class, FILTER_THRESHOLD_FIELD_ID)
        .get(Field.PROPERTY_NAME, language);

    final String YES_NO_DEFAULT_REF = "Yes/No/Default";
    final String TEXT_FILTER_BEHAVIOR_REF = "Text Filter Behavior (Window/Tab/Field)";

    boolean forcedDiscardEdit = false;
    if (!discardEdit) {
      // help edition is now only available in compatibility module
      Module compatModule = OBDal.getInstance().get(Module.class, COMPATIBILITY_MODULE);
      forcedDiscardEdit = compatModule == null;
    }

    if (forcedDiscardEdit || discardEdit) {
      discard[nDiscards++] = new String("discardEdit");
    }

    if (!discardEdit && localStrKeyId.equals("")) {
      strType = vars.getRequiredStringParameter("inpwindowType");
      window = false;
      if (strType.equals("X")) {
        strCommand = "FORM";
        localStrKeyId = vars.getRequiredStringParameter("inpwindowName");
        DisplayHelpData[] dataForm = DisplayHelpData.selectFormTrl(conn, vars.getLanguage(),
            localStrKeyId);
        if (dataForm != null && dataForm.length > 0) {
          strWindowName = dataForm[0].name;
          strWindowHelp = dataForm[0].help;
          strBaseName = dataForm[0].basename;
          showWikiLink = ob3DistroModules.contains(dataForm[0].moduleid);
        } else {
          discard[3] = new String("discardEdit");
        }
      } else if (strType.equals("P") || strType.equals("R")) {
        strCommand = "PROCESS";
        localStrKeyId = vars.getRequiredStringParameter("inpwindowName");
        DisplayHelpData[] dataProcess = DisplayHelpData.selectProcessTrl(conn, vars.getLanguage(),
            localStrKeyId);
        if (dataProcess != null && dataProcess.length > 0) {
          strWindowName = dataProcess[0].name;
          strWindowHelp = dataProcess[0].help;
          strBaseName = dataProcess[0].basename;
          showWikiLink = ob3DistroModules.contains(dataProcess[0].moduleid);
        } else {
          discard[3] = new String("discardEdit");
        }
      }
      discard[nDiscards++] = new String("sectionTabsRelation");
      discard[nDiscards++] = new String("sectionTabsDescription");
      discard[nDiscards++] = new String("sectionCabeceraFields");
    }

    Map<String, String> systemGridProperties = new HashMap<String, String>();
    List<TabGridConfigParameter> tabGridConfigParams = new ArrayList<TabGridConfigParameter>();
    DisplayHelpData[] data = DisplayHelpData.set();
    if (window) {
      data = DisplayHelpData.selectTrl(conn, vars.getLanguage(), localStrKeyId);
      if (data != null && data.length > 0) {
        strWindowName = data[0].windowname;
        strBaseName = data[0].basename;
        showWikiLink = ob3DistroModules.contains(data[0].moduleid);
      } else {
        discard[nDiscards++] = new String("discardEdit");
      }
      strWindowHelp = DisplayHelpData.windowHelpTrl(conn, vars.getLanguage(), localStrKeyId);
      strCommand = "WINDOW";

      // Grid Configuration at System Level
      OBCriteria<GCSystem> systemGridConfigCriteria = OBDal.getInstance()
          .createCriteria(GCSystem.class);

      List<GCSystem> systemGridConfigList = systemGridConfigCriteria.list();
      if (!systemGridConfigList.isEmpty()) {
        GCSystem systemGridConfig = systemGridConfigList.get(0);
        systemGridProperties.put(FILTERABLE, systemGridConfig.isFilterable() ? YES : NO);
        systemGridProperties.put(SORTABLE, systemGridConfig.isSortable() ? YES : NO);
        systemGridProperties.put(TEXT_FILTER_BEHAVIOR,
            Utility.getListValueName(TEXT_FILTER_BEHAVIOR_REF,
                systemGridConfig.getTextFilterBehavior(), language.getLanguage()));
        systemGridProperties.put(FILTER_THRESHOLD, systemGridConfig.isFilterable() ? YES : NO);
        systemGridProperties.put(LAZY_FILTERING, systemGridConfig.isLazyFiltering() ? YES : NO);
        systemGridProperties.put(FILTER_ON_CHANGE, systemGridConfig.isFilterOnChange() ? YES : NO);
      } else {
        discard[nDiscards++] = "systemGridConfig";
      }

      // Grid Configuration at Tab Level
      OBQuery<GCTab> tabGridConfigQuery = OBDal.getInstance()
          .createQuery(GCTab.class, "tab.window.id = :windowId");
      tabGridConfigQuery.setNamedParameter("windowId", localStrKeyId);
      List<GCTab> tabGridConfigList = tabGridConfigQuery.list();
      for (GCTab gcTab : tabGridConfigList) {
        String tabId = gcTab.getTab().getId();
        String tabName = gcTab.getTab().getName();
        Map<String, String> tabGridProperties = new HashMap<String, String>();
        if (!"D".equals(gcTab.getFilterable())) {
          tabGridProperties.put(FILTERABLE, Utility.getListValueName(YES_NO_DEFAULT_REF,
              gcTab.getFilterable(), language.getLanguage()));
        }
        if (!"D".equals(gcTab.getSortable())) {
          tabGridProperties.put(SORTABLE, Utility.getListValueName(YES_NO_DEFAULT_REF,
              gcTab.getSortable(), language.getLanguage()));
        }
        if (!"D".equals(gcTab.getTextFilterBehavior())) {
          tabGridProperties.put(TEXT_FILTER_BEHAVIOR, Utility.getListValueName(
              TEXT_FILTER_BEHAVIOR_REF, gcTab.getTextFilterBehavior(), language.getLanguage()));
        }
        if (gcTab.getThresholdToFilter() != null) {
          tabGridProperties.put(FILTER_THRESHOLD, gcTab.getThresholdToFilter().toString());
        }
        if (!"D".equals(gcTab.getIsLazyFiltering())) {
          tabGridProperties.put(LAZY_FILTERING, Utility.getListValueName(YES_NO_DEFAULT_REF,
              gcTab.getIsLazyFiltering(), language.getLanguage()));
        }
        if (!"D".equals(gcTab.getFilterOnChange())) {
          tabGridProperties.put(FILTER_ON_CHANGE, Utility.getListValueName(YES_NO_DEFAULT_REF,
              gcTab.getFilterOnChange(), language.getLanguage()));
        }
        if (!tabGridProperties.isEmpty()) {
          tabGridConfigParams.add(new TabGridConfigParameter(tabId, tabName,
              formatGridConfiguration(tabGridProperties)));
        }

        // Grid Configuration at Field Level
        List<GCField> fieldGridConfigList = gcTab.getOBUIAPPGCFieldList();
        for (GCField gcField : fieldGridConfigList) {
          Map<String, String> fieldGridProperties = new HashMap<String, String>();
          if (!"D".equals(gcField.getFilterable())) {
            fieldGridProperties.put(FILTERABLE, Utility.getListValueName(YES_NO_DEFAULT_REF,
                gcField.getFilterable(), language.getLanguage()));
          }
          if (!"D".equals(gcField.getSortable())) {
            fieldGridProperties.put(SORTABLE, Utility.getListValueName(YES_NO_DEFAULT_REF,
                gcField.getSortable(), language.getLanguage()));
          }
          if (!"D".equals(gcField.getTextFilterBehavior())) {
            fieldGridProperties.put(TEXT_FILTER_BEHAVIOR, Utility.getListValueName(
                TEXT_FILTER_BEHAVIOR_REF, gcField.getFilterOnChange(), language.getLanguage()));
          }
          if (gcField.getThresholdToFilter() != null) {
            fieldGridProperties.put(FILTER_THRESHOLD, gcField.getThresholdToFilter().toString());
          }
          if (!"D".equals(gcTab.getFilterOnChange())) {
            fieldGridProperties.put(FILTER_ON_CHANGE, Utility.getListValueName(YES_NO_DEFAULT_REF,
                gcField.getFilterOnChange(), language.getLanguage()));
          }
          if (!fieldGridProperties.isEmpty()) {
            tabGridConfigParams.add(new TabGridConfigParameter(tabId, tabName,
                formatGridConfiguration(tabGridProperties), gcField.getField().getName(),
                formatGridConfiguration(fieldGridProperties)));
          }
        }
      }
      if (systemGridConfigList.isEmpty() && tabGridConfigParams.isEmpty()) {
        discard[nDiscards++] = "sectionCabeceraGridConfig";
      }
    }

    if (!showWikiLink) {
      discard[nDiscards++] = "showWikiLink";
    }

    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_help/DisplayHelp", discard)
        .createXmlDocument();

    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory",
        "var baseDirectory = \"" + xmlEngine.strReplaceWith + "/\";\n");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("windowId", localStrKeyId);
    xmlDocument.setParameter("windowName", strWindowName);
    xmlDocument.setParameter("windowHelp", strWindowHelp);
    xmlDocument.setParameter("command", strCommand);
    xmlDocument.setParameter("wikiLink", strBaseName);
    xmlDocument.setData("structure1", data);
    xmlDocument.setData("structure2", data);
    xmlDocument.setData("structure3", data);
    xmlDocument.setParameter("systemGridConfigProperties",
        formatGridConfiguration(systemGridProperties));
    xmlDocument.setData("structure4",
        tabGridConfigParams.toArray(new TabGridConfigParameter[tabGridConfigParams.size()]));
    OBContext.restorePreviousMode();
    return (xmlDocument.print());
  }

  private static String formatGridConfiguration(Map<String, String> gridConfiguration) {
    StringBuilder formattedGridConfig = new StringBuilder();
    boolean first = true;
    for (String key : gridConfiguration.keySet()) {
      if (!first) {
        formattedGridConfig.append(", ");
      } else {
        first = false;
      }
      formattedGridConfig.append(key + ": ");
      formattedGridConfig.append(gridConfiguration.get(key));
    }
    return formattedGridConfig.toString();
  }

  @SuppressWarnings("serial")
  private static class TabGridConfigParameter implements FieldProvider {
    public String tabId;
    public String tabName;
    public String tabGridProperties;
    public String tabFieldName;
    public String fieldGridProperties;

    public TabGridConfigParameter(String tabId, String tabName, String tabGridProperties) {
      this.tabId = tabId;
      this.tabName = tabName;
      this.tabGridProperties = tabGridProperties;
    }

    public TabGridConfigParameter(String tabId, String tabName, String tabGridProperties,
        String tabFieldName, String fieldGridProperties) {
      this.tabId = tabId;
      this.tabName = tabName;
      this.tabGridProperties = tabGridProperties;
      this.tabFieldName = tabFieldName;
      this.fieldGridProperties = fieldGridProperties;
    }

    @Override
    public String getField(String fieldName) {
      if (fieldName.equalsIgnoreCase("tabId")) {
        return tabId;
      } else if (fieldName.equalsIgnoreCase("tabName")) {
        return tabName;
      } else if (fieldName.equalsIgnoreCase("tabGridProperties")) {
        return tabGridProperties;
      } else if (fieldName.equalsIgnoreCase("tabFieldName")) {
        return tabFieldName;
      } else if (fieldName.equalsIgnoreCase("fieldGridProperties")) {
        return fieldGridProperties;
      } else {
        return null;
      }
    }
  }

}
