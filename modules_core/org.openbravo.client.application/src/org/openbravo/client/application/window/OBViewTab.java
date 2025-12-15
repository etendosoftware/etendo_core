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
 * All portions are Copyright (C) 2010-2020 Openbravo SLU 
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.window;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.client.application.ApplicationUtils;
import org.openbravo.client.application.DynamicExpressionParser;
import org.openbravo.client.application.GCSystem;
import org.openbravo.client.application.GCTab;
import org.openbravo.client.kernel.BaseTemplateComponent;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.Template;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.data.Sqlc;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.ModelImplementation;
import org.openbravo.model.ad.domain.ModelImplementationMapping;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.service.datasource.DataSourceComponent;
import org.openbravo.service.datasource.DataSourceConstants;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.userinterface.selector.Selector;
import org.openbravo.utils.FormatUtilities;

/**
 * Represents the Openbravo Tab (form and grid combination).
 * 
 * @author mtaal
 */
@Dependent
public class OBViewTab extends BaseTemplateComponent {

  private static final Logger log = LogManager.getLogger();
  private static final String DEFAULT_TEMPLATE_ID = "B5124C0A450D4D3A867AEAC7DF64D6F0";
  private static final String DEFAULT_EMAIL_PROCESS_ID = "5638D6D4B33F44C889C3AFCA0DEB8130";
  private static final String PICK_AND_EXECUTE = "OBUIAPP_PickAndExecute";
  private static final String ACTION = "A";
  protected static final Map<String, String> TEMPLATE_MAP = new HashMap<>();

  static {
    // Map: WindowType - Template
    TEMPLATE_MAP.put(PICK_AND_EXECUTE, "FF808181330BD14F01330BD34EA00008");
  }

  private Entity entity;
  private Tab tab;
  private String tabTitle;
  private List<OBViewTab> childTabs = new ArrayList<>();
  private OBViewTab parentTabComponent;
  private String parentProperty = null;
  private List<ButtonField> buttonFields = null;
  // Includes also the non displayed buttons
  private List<ButtonField> allButtonFields = null;
  private List<IconButton> iconButtons = null;
  private boolean buttonSessionLogic;
  private boolean isRootTab;
  private String uniqueString = "" + System.currentTimeMillis();

  private Map<String, String> preferenceAttributesMap = new HashMap<>();

  @Inject
  private OBViewFieldHandler fieldHandler;

  @Inject
  @ComponentProvider.Qualifier(DataSourceConstants.DS_COMPONENT_TYPE)
  private ComponentProvider dsComponentProvider;
  private Map<String, Optional<GCTab>> tabsGridConfig;
  private Optional<GCSystem> systemGridConfig;

  public String getDataSourceJavaScript() {
    final String dsId = getDataSourceId();
    final Map<String, Object> dsParameters = new HashMap<>(getParameters());
    dsParameters.put(DataSourceConstants.DS_ONLY_GENERATE_CREATESTATEMENT, true);
    if (PICK_AND_EXECUTE.equals(tab.getWindow().getWindowType())) {
      dsParameters.put(DataSourceConstants.DS_CLASS_NAME, "OBPickAndExecuteDataSource");
    } else {
      dsParameters.put(DataSourceConstants.DS_CLASS_NAME, "OBViewDataSource");
    }
    dsParameters.put(DataSourceConstants.MINIMAL_PROPERTY_OUTPUT, true);

    final StringBuilder sb = new StringBuilder();
    for (Field fld : tab.getADFieldList()) {
      if (fld.getProperty() != null && fld.getProperty().contains(".")) {
        if (sb.length() > 0) {
          sb.append(",");
        }
        sb.append(fld.getProperty());
      }
    }

    if (sb.length() > 0) {
      dsParameters.put(JsonConstants.ADDITIONAL_PROPERTIES_PARAMETER, sb.toString());
    }
    // If the tab is based on a hql table, then the tableId must be passed to the datasource so that
    // it can build the datasource properties based on the columns of the table
    if (ApplicationConstants.HQLBASEDTABLE.equals(tab.getTable().getDataOriginType())) {
      dsParameters.put("tableId", tab.getTable().getId());
    }
    DataSourceComponent component = (DataSourceComponent) dsComponentProvider.getComponent(dsId,
        dsParameters);
    if (PICK_AND_EXECUTE.equals(tab.getWindow().getWindowType())) {
      component.setIncludeCreationCode(false);
    }
    return component.generate();
  }

  public String getNotesDataSourceJavaScript() {
    final Map<String, Object> dsParameters = new HashMap<>(getParameters());
    dsParameters.put(DataSourceConstants.DS_ONLY_GENERATE_CREATESTATEMENT, true);
    final Component component = dsComponentProvider.getComponent("090A37D22E61FE94012E621729090048",
        dsParameters);
    return component.generate();
  }

  @Override
  protected Template getComponentTemplate() {
    final String windowType = tab.getWindow().getWindowType();
    if (TEMPLATE_MAP.containsKey(windowType)) {
      return OBDal.getInstance().get(Template.class, TEMPLATE_MAP.get(windowType));
    }
    return OBDal.getInstance().get(Template.class, DEFAULT_TEMPLATE_ID);
  }

  public OBViewFieldHandler getFieldHandler() {
    return fieldHandler;
  }

  public List<OtherField> getOtherFields() {
    final List<OtherField> otherFields = new ArrayList<>();
    for (Field fld : fieldHandler.getIgnoredFields()) {
      if (fld.getColumn() == null) {
        continue;
      }
      otherFields.add(new OtherField(fld.getColumn()));
    }

    // Adding PK as additional field
    if (entity.getIdProperties().size() == 1) {
      Property pkProperty = entity.getIdProperties().get(0);
      OtherField pkField = new OtherField(pkProperty);
      pkField.inpColumnName = pkProperty.getColumnName();
      pkField.session = true;
      otherFields.add(pkField);
    }
    return otherFields;
  }

  public void addChildTabComponent(OBViewTab childTabComponent) {
    childTabComponent.setParentTabComponent(this);
    childTabs.add(childTabComponent);
  }

  public boolean getDefaultEditMode() {
    return tab.isDefaultEditMode() != null && tab.isDefaultEditMode();
  }

  public String getMapping250() {
    return Utility.getTabURL(tab, "none", false);
  }

  public List<ButtonField> getButtonFields() {
    if (buttonFields != null) {
      return buttonFields;
    }
    buttonFields = new ArrayList<>();
    final List<Field> adFields = new ArrayList<>(tab.getADFieldList());
    Collections.sort(adFields, new FormFieldComparator());
    for (Field fld : adFields) {
      if (fld.isActive() && fld.isDisplayed()) {
        if (!(ApplicationUtils.isUIButton(fld))) {
          continue;
        }
        ButtonField btn = new ButtonField(fld);
        buttonFields.add(btn);
        if (btn.sessionLogic) {
          buttonSessionLogic = true;
        }
      }
    }
    return buttonFields;
  }

  public void setGCSettings(Optional<GCSystem> systemGridConfig,
      Map<String, Optional<GCTab>> tabsGridConfig) {
    fieldHandler.setGCSettings(systemGridConfig, tabsGridConfig);
    this.systemGridConfig = systemGridConfig;
    this.tabsGridConfig = tabsGridConfig;
  }

  public List<ButtonField> getAllButtonFields() {
    if (allButtonFields != null) {
      return allButtonFields;
    }
    allButtonFields = new ArrayList<>();
    final List<Field> adFields = new ArrayList<>(tab.getADFieldList());
    Collections.sort(adFields, new FormFieldComparator());
    for (Field fld : adFields) {
      if (fld.isActive()) {
        if (!(ApplicationUtils.isUIButton(fld))) {
          continue;
        }
        ButtonField btn = new ButtonField(fld);
        allButtonFields.add(btn);
      }
    }
    return allButtonFields;
  }

  public List<IconButton> getIconButtons() {
    if (iconButtons != null) {
      return iconButtons;
    }

    iconButtons = new ArrayList<>();

    // Print/email button
    if (tab.getProcess() != null) {
      iconButtons.addAll(getPrintEmailButtons());
    }

    // Audit trail button
    if (tab.getTable().isFullyAudited()) {
      IconButton auditBtn = new IconButton();
      auditBtn.type = "audit";
      auditBtn.label = Utility.messageBD(new DalConnectionProvider(false), "AuditTrail",
          OBContext.getOBContext().getLanguage().getLanguage());
      auditBtn.action = "OB.ToolbarUtils.showAuditTrail(this.view);";
      iconButtons.add(auditBtn);
    }

    if (tab.getTableTree() != null) {
      IconButton treeBtn = new IconButton();
      treeBtn.type = "treeGrid";
      treeBtn.label = Utility.messageBD(new DalConnectionProvider(false),
          "OBUIAPP_TOGGLE_TREE_BUTTON", OBContext.getOBContext().getLanguage().getLanguage());
      treeBtn.action = "OB.ToolbarUtils.toggleTreeGridVisibility(this.view);";
      iconButtons.add(treeBtn);
    }

    return iconButtons;
  }

  private Collection<? extends IconButton> getPrintEmailButtons() {
    List<IconButton> btns = new ArrayList<>();

    PrintButton printBtn = new PrintButton();
    btns.add(printBtn);

    if (printBtn.hasEmail) {
      IconButton emailBtn = new IconButton();
      emailBtn.type = "email";
      emailBtn.label = Utility.messageBD(new DalConnectionProvider(false), "Email",
          OBContext.getOBContext().getLanguage().getLanguage());
      emailBtn.action = printBtn.action.replace("print.html", "send.html");
      emailBtn.action = emailBtn.action.replace("printButton", "emailButton");
      btns.add(emailBtn);
    }

    return btns;
  }

  public String getParentProperty() {
    Boolean disableParentKeyProperty = getTab().isDisableParentKeyProperty();
    if (parentTabComponent == null || disableParentKeyProperty) {
      return "";
    }
    if (parentProperty != null) {
      return parentProperty;
    }
    if (tab.getTable().getId().equals(parentTabComponent.getTab().getTable().getId())
        && ("RO".equals(tab.getUIPattern()) || "SR".equals(tab.getUIPattern()))) {
      parentProperty = getEntity().getIdProperties().get(0).getName();
    } else {
      parentProperty = ApplicationUtils.getParentProperty(tab, parentTabComponent.getTab());
    }
    return parentProperty;
  }

  public boolean getDeleteableTable() {
    return tab.getTable().isDeletableRecords();
  }

  public String getViewForm() {
    final OBViewFormComponent viewFormComponent = createComponent(OBViewFormComponent.class);
    viewFormComponent.setParameters(getParameters());
    viewFormComponent.setParentProperty(getParentProperty());
    viewFormComponent.setFieldHandler(fieldHandler);
    return viewFormComponent.generate();
  }

  public String getViewGrid() {
    // check at least one field is visible in grid view, does not stop the execution
    OBCriteria<Field> fieldCriteria = OBDal.getInstance().createCriteria(Field.class);
    fieldCriteria.add(Restrictions.eq(Field.PROPERTY_TAB, getTab()));
    fieldCriteria.add(Restrictions.eq(Field.PROPERTY_SHOWINGRIDVIEW, true));
    if (fieldCriteria.count() == 0) {
      log.error("No Fields are visible in grid view for Tab " + tab.getWindow().getName() + " - "
          + tab.getName());
    }

    final OBViewGridComponent viewGridComponent = createComponent(OBViewGridComponent.class);
    viewGridComponent.setParameters(getParameters());
    viewGridComponent.setTab(tab);
    viewGridComponent.setViewTab(this);
    viewGridComponent.setApplyTransactionalFilter(shouldApplyTransactionalFilter());
    viewGridComponent.setGCSettings(systemGridConfig, tabsGridConfig);
    return viewGridComponent.generate();
  }

  private boolean shouldApplyTransactionalFilter() {
    return isRootTab() && tab.getWindow().getWindowType().equals("T")
        && areTransactionalFiltersEnabled();
  }

  private boolean areTransactionalFiltersEnabled() {
    if (!systemGridConfig.isPresent()) {
      return true;
    }
    return systemGridConfig.get().isAllowTransactionalFilters();
  }

  public OBViewTab getParentTabComponent() {
    return parentTabComponent;
  }

  public void setParentTabComponent(OBViewTab parentTabComponent) {
    this.parentTabComponent = parentTabComponent;
  }

  public List<OBViewTab> getChildTabs() {
    return childTabs;
  }

  private boolean hasAlwaysVisibleChildTab() {
    boolean hasVisibleChildTab = false;
    for (OBViewTab childTab : this.getChildTabs()) {
      if (!childTab.getAcctTab() && !childTab.getTrlTab()) {
        hasVisibleChildTab = true;
        break;
      }
    }
    return hasVisibleChildTab;
  }

  private boolean hasAccountingChildTab() {
    boolean hasAccountingChildTab = false;
    for (OBViewTab childTab : this.getChildTabs()) {
      if (childTab.getAcctTab()) {
        hasAccountingChildTab = true;
        break;
      }
    }
    return hasAccountingChildTab;
  }

  private boolean hasTranslationChildTab() {
    boolean hasTranslationChildTab = false;
    for (OBViewTab childTab : this.getChildTabs()) {
      if (childTab.getTrlTab()) {
        hasTranslationChildTab = true;
        break;
      }
    }
    return hasTranslationChildTab;
  }

  public String getHasChildTabsProperty() {
    String hasChildTabs = null;
    if (this.hasAlwaysVisibleChildTab()) {
      hasChildTabs = "true";
    } else {
      boolean hasAcctChildTab = this.hasAccountingChildTab();
      boolean hasTrlChildTab = this.hasTranslationChildTab();
      if (hasAcctChildTab && hasTrlChildTab) {
        hasChildTabs = "(OB.PropertyStore.get('ShowTrl', this.windowId) === 'Y') || (OB.PropertyStore.get('ShowAcct', this.windowId) === 'Y')";
      } else if (hasAcctChildTab) {
        hasChildTabs = "(OB.PropertyStore.get('ShowAcct', this.windowId) === 'Y')";
      } else { // hasTrlChildTab == true
        hasChildTabs = "(OB.PropertyStore.get('ShowTrl', this.windowId) === 'Y')";
      }
    }
    return hasChildTabs;
  }

  public Tab getTab() {
    return tab;
  }

  public void setTab(Tab tab) {
    this.tab = tab;
    fieldHandler.setTab(tab);
  }

  public boolean isTabSet() {
    return tab != null;
  }

  public String getTabId() {
    return tab.getId();
  }

  public String getModuleId() {
    return tab.getModule().getId();
  }

  private Entity getEntity() {
    if (entity == null) {
      entity = ModelProvider.getInstance().getEntity(tab.getTable().getName());
    }
    return entity;
  }

  public String getEntityName() {
    return getEntity().getName();
  }

  public String getTabTitle() {
    if (tabTitle == null) {
      tabTitle = OBViewUtil.getLabel(tab, tab.getADTabTrlList(), Tab.PROPERTY_NAME);
    }
    return tabTitle;
  }

  public String getDataSourceId() {
    String dataSourceId = null;
    String dataOriginType = tab.getTable().getDataOriginType();
    if (ApplicationConstants.TABLEBASEDTABLE.equals(dataOriginType)) {
      dataSourceId = tab.getTable().getName();
    } else if (ApplicationConstants.DATASOURCEBASEDTABLE.equals(dataOriginType)) {
      dataSourceId = tab.getTable().getObserdsDatasource().getId();
    } else if (ApplicationConstants.HQLBASEDTABLE.equals(dataOriginType)) {
      dataSourceId = ApplicationConstants.HQL_TABLE_DATASOURCE_ID;
    }
    return dataSourceId;
  }

  public String getSelectionFunction() {
    if (tab.getOBUIAPPSelection() != null) {
      return tab.getOBUIAPPSelection();
    }
    return "";
  }

  public void setTabTitle(String tabTitle) {
    this.tabTitle = tabTitle;
  }

  public boolean getAcctTab() {
    return tab.isAccountingTab();
  }

  public boolean getTrlTab() {
    return tab.isTranslationTab();
  }

  public String getTableId() {
    return tab.getTable().getId();
  }

  public Property getKeyProperty() {
    for (Property prop : getEntity().getProperties()) {
      if (prop.isId()) {
        return prop;
      }
    }
    throw new IllegalStateException("Entity " + getEntityName() + " does not have an id property");
  }

  public boolean isDataSourceTable() {
    return ApplicationConstants.DATASOURCEBASEDTABLE
        .equals(this.tab.getTable().getDataOriginType());
  }

  public String getKeyPropertyType() {
    return UIDefinitionController.getInstance()
        .getUIDefinition(getKeyProperty().getColumnId())
        .getName();
  }

  public String getKeyColumnName() {
    return getKeyProperty().getColumnName();
  }

  public String getKeyInpName() {
    return "inp" + Sqlc.TransformaNombreColumna(getKeyProperty().getColumnName());
  }

  public String getWindowId() {
    return tab.getWindow().getId();
  }

  public boolean isButtonSessionLogic() {
    if (buttonFields == null) {
      // Generate buttons fields if they haven't been already generated, to calculate
      // buttonSessionLogic
      getButtonFields();
    }
    return buttonSessionLogic;
  }

  public void setUniqueString(String uniqueString) {
    this.uniqueString = uniqueString;
  }

  public String getProcessViews() {
    StringBuilder views = new StringBuilder();
    // Use HashSet to avoid processId duplicities
    HashSet<String> processIds = new HashSet<>();
    for (ButtonField f : getButtonFields()) {
      // Get processes coming from action buttons
      if (f.column.getOBUIAPPProcess() == null
          || (!PICK_AND_EXECUTE.equals(f.column.getOBUIAPPProcess().getUIPattern()) && !ACTION.equals(f.column.getOBUIAPPProcess().getUIPattern()))) {
        continue;
      }
      processIds.add(f.column.getOBUIAPPProcess().getId());
    }
    final List<Field> adFields = new ArrayList<>(tab.getADFieldList());
    Collections.sort(adFields, new FormFieldComparator());
    for (Field fld : adFields) {
      // Get processes coming from selectors
      if (fld.isActive() && fld.isDisplayed()) {
        if (fld.getColumn() == null || fld.getColumn().getReferenceSearchKey() == null) {
          continue;
        }
        final OBCriteria<Selector> criteria = OBDal.getInstance().createCriteria(Selector.class);
        criteria.add(Restrictions.eq(Selector.PROPERTY_REFERENCE, fld.getColumn().getReferenceSearchKey()));
        criteria.setMaxResults(1);
        Selector selector = (Selector) criteria.uniqueResult();

        if (selector==null) {
          continue;
        }

        if (selector.getProcessDefintion() == null) {
          continue;
        }
        processIds.add(selector.getProcessDefintion().getId());
      }
    }

    for (String processId : processIds) {
      org.openbravo.client.application.Process process = OBDal.getInstance()
          .get(org.openbravo.client.application.Process.class, processId);
      final ParameterWindowComponent processWindow = createComponent(
          ParameterWindowComponent.class);
      processWindow.setParameters(getParameters());
      processWindow.setUniqueString(uniqueString);
      processWindow.setProcess(process);
      processWindow.setParentWindow(getTab().getWindow());
      processWindow.setPoup(true);
      views.append(processWindow.generate()).append("\n");
    }
    return views.toString();
  }

  public boolean isAllowAdd() {
    if (tab.isObuiappCanAdd() != null) {
      return tab.isObuiappCanAdd();
    }
    return false;
  }

  public boolean isAllowDelete() {
    if (tab.isObuiappCanDelete() != null) {
      return tab.isObuiappCanDelete();
    }
    return false;
  }

  public boolean isShowSelect() {
    if (tab.isObuiappShowSelect() != null) {
      return tab.isObuiappShowSelect();
    }
    return true;
  }

  public String getSelectionType() {
    if (tab.getObuiappSelectionType() != null) {
      return tab.getObuiappSelectionType();
    }
    // "M" or "Multiple" is the default value
    return "M";
  }

  public String getNewFunction() {
    if (tab.getOBUIAPPNewFn() != null) {
      return tab.getOBUIAPPNewFn();
    }
    return "";
  }

  public String getRemoveFunction() {
    if (tab.getObuiappRemovefn() != null) {
      return tab.getObuiappRemovefn();
    }
    return "";
  }

  public String getShowIf() {

    String jsExpression = null;
    if (tab.getDisplayLogic() != null && !tab.getDisplayLogic().isEmpty()) {
      boolean inpColumnNames = true;
      final DynamicExpressionParser parser = new DynamicExpressionParser(tab.getDisplayLogic(), tab,
          inpColumnNames);
      jsExpression = parser.getJSExpression();
      // Retrieves the preference attributes used in the display logic of the tab
      setPreferenceAttributesFromParserResult(parser, this.getWindowId());
    }
    if (jsExpression != null) {
      return jsExpression;
    } else {
      return "";
    }
  }

  public String getDefaultTreeViewLogicIf() {

    String jsExpression = null;
    if (tab.getDefaultTreeViewLogic() != null && !tab.getDefaultTreeViewLogic().isEmpty()) {
      boolean inpColumnNames = true;
      final DynamicExpressionParser parser = new DynamicExpressionParser(
          tab.getDefaultTreeViewLogic(), tab, inpColumnNames);
      jsExpression = parser.getJSExpression();
      // Retrieves the preference attributes used in the display logic of the tab
      setPreferenceAttributesFromParserResult(parser, this.getWindowId());
    }
    if (jsExpression != null) {
      return jsExpression;
    } else {
      return "";
    }
  }

  private void setPreferenceAttributesFromParserResult(DynamicExpressionParser parser,
      String windowId) {
    for (String attrName : parser.getSessionAttributes()) {
      // The value of the Session properties (#sessionPropertyName) and Preferences (name does not
      // start with inp) can be evaluated before the tab is loaded
      if (!preferenceAttributesMap.containsKey(attrName)
          && (attrName.startsWith("#") || !attrName.startsWith("inp"))) {
        final String attrValue = Utility.getContext(new DalConnectionProvider(false),
            RequestContext.get().getVariablesSecureApp(), attrName, windowId);
        preferenceAttributesMap.put(attrName.replace("#", "_"), attrValue);
      }
    }
  }

  // Returns the preference attributes map in JSON format
  public String getPreferenceAttributes() {
    String preferenceAttributes = "";
    if (!preferenceAttributesMap.isEmpty()) {
      try {
        JSONObject preferenceAttributesJSON = new JSONObject();
        for (Entry<String, String> entry : preferenceAttributesMap.entrySet()) {
          preferenceAttributesJSON.put(entry.getKey(), entry.getValue());
        }
        preferenceAttributes = preferenceAttributesJSON.toString();
      } catch (JSONException e) {
      }
    }
    return preferenceAttributes;
  }

  // Returns the preference's names as a string
  public String getPreferenceAttributesNames() {
    boolean first = true;
    String preferenceAttributes = "";
    if (!preferenceAttributesMap.isEmpty()) {
      for (String attr : preferenceAttributesMap.keySet()) {
        if (!first) {
          preferenceAttributes = preferenceAttributes.concat(",");
        } else {
          first = false;
        }
        preferenceAttributes = preferenceAttributes.concat("'" + attr + "'");
      }
    }
    return preferenceAttributes;
  }

  // Return the list of fields of these tab that are part of the display logic of its subtabs
  public List<String> getDisplayLogicFields() {
    boolean getOnlyFirstLevelSubTabs = false;
    List<Tab> subTabs = KernelUtils.getInstance().getTabSubtabs(tab, getOnlyFirstLevelSubTabs);
    List<String> displayLogicFields = new ArrayList<>();
    for (Tab subTab : subTabs) {
      if (subTab.getDisplayLogic() != null && !subTab.getDisplayLogic().isEmpty()) {
        boolean inpColumnNames = true;
        final DynamicExpressionParser parser = new DynamicExpressionParser(subTab.getDisplayLogic(),
            tab, inpColumnNames);
        List<String> tokens = parser.getOtherTokensInExpression();
        for (String token : tokens) {
          if (!displayLogicFields.contains(token) && fieldHandler.isField(token)) {
            displayLogicFields.add(token);
          }
        }
      }
    }
    return displayLogicFields;
  }

  @Dependent
  public class ButtonField {
    private String id;
    private String label;
    private String url;
    private String propertyName;
    private List<Value> labelValues = null;
    private boolean autosave;
    private String showIf = "";
    private String readOnlyIf = "";
    private boolean sessionLogic = false;
    private boolean modal = true;
    private String processId = "";
    private String windowId = "";
    private String windowTitle = "";
    private boolean newDefinition = false;
    private String uiPattern = "";
    private boolean multiRecord = false;
    private Column column;

    public ButtonField(Field fld) {
      id = fld.getId();
      label = OBViewUtil.getLabel(fld);

      // TODO: column might be null when model doesn't require it!!
      column = fld.getColumn();

      propertyName = KernelUtils.getInstance().getPropertyFromColumn(column).getName();
      autosave = column.isAutosave();

      // Define command
      Process process = null;

      if (column.getOBUIAPPProcess() != null) {
        // new process definition has more precedence
        org.openbravo.client.application.Process newProcess = column.getOBUIAPPProcess();
        processId = newProcess.getId();
        url = "/";
        command = newProcess.getJavaClassName();
        newDefinition = true;
        uiPattern = newProcess.getUIPattern();
        multiRecord = newProcess.isMultiRecord();

        setWindowId(tab.getWindow().getId());
        if (PICK_AND_EXECUTE.equals(uiPattern) || ACTION.equals(uiPattern)) {
          // TODO: modal should be a parameter in the process definition?
          modal = false;
        }
      } else if (column.getProcess() != null) {
        process = column.getProcess();
        String manualProcessMapping = null;
        for (ModelImplementation impl : process.getADModelImplementationList()) {
          if (impl.isDefault()) {
            for (ModelImplementationMapping mapping : impl.getADModelImplementationMappingList()) {
              if (mapping.isDefault()) {
                manualProcessMapping = mapping.getMappingName();
                break;
              }
            }
            break;
          }
        }

        if (manualProcessMapping == null) {
          // Standard UI process
          url = Utility.getTabURL(fld.getTab(), "E", false);
          command = "BUTTON" + FormatUtilities.replace(column.getDBColumnName())
              + column.getProcess().getId();
        } else {
          url = manualProcessMapping;
          command = "DEFAULT";
        }

        modal = Utility.isModalProcess(process);
        processId = process.getId();

      } else {
        String colName = column.getDBColumnName();
        if ("Posted".equalsIgnoreCase(colName) || "CreateFrom".equalsIgnoreCase(colName)) {
          command = "BUTTON" + colName;
          url = Utility.getTabURL(fld.getTab(), "E", false);
        }
      }

      if (labelValues == null) {
        labelValues = new ArrayList<>();

        if (column.getReferenceSearchKey() != null) {
          for (org.openbravo.model.ad.domain.List valueList : column.getReferenceSearchKey()
              .getADListList()) {
            labelValues.add(new Value(valueList));
          }
        }
      }

      // Display Logic
      if (fld.getDisplayLogic() != null) {
        final DynamicExpressionParser parser = new DynamicExpressionParser(fld.getDisplayLogic(),
            tab, fld);
        showIf = parser.getJSExpression();
        if (!parser.getSessionAttributes().isEmpty()) {
          sessionLogic = true;
        }
      }

      // Read only logic
      if (fld.getColumn().getReadOnlyLogic() != null) {
        final DynamicExpressionParser parser = new DynamicExpressionParser(
            fld.getColumn().getReadOnlyLogic(), tab);
        readOnlyIf = parser.getJSExpression();
        if (!parser.getSessionAttributes().isEmpty()) {
          sessionLogic = true;
        }
      }
    }

    public boolean isAutosave() {
      return autosave;
    }

    public String getPropertyName() {
      return propertyName;
    }

    public boolean getHasLabelValues() {
      return !labelValues.isEmpty();
    }

    public List<Value> getLabelValues() {
      return labelValues;
    }

    public String getUrl() {
      if (url == null) {
        url = "/";
        log.error(
            "The button " + column.getName() + " of the table " + column.getTable().getDBTableName()
                + " has not process or a process definition assigned to it");
      }
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getCommand() {
      if (command == null) {
        command = "/";
        log.error(
            "The button " + column.getName() + " of the table " + column.getTable().getDBTableName()
                + " has not process or a process definition assigned to it");
      }
      return command;
    }

    public void setCommand(String command) {
      this.command = command;
    }

    private String command;

    public String getLabel() {
      return label;
    }

    public void setLabel(String label) {
      this.label = label;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getShowIf() {
      return showIf;
    }

    public String getReadOnlyIf() {
      return readOnlyIf;
    }

    public boolean isModal() {
      return modal;
    }

    public String getProcessId() {
      return processId;
    }

    public boolean isNewDefinition() {
      return newDefinition;
    }

    public String getUiPattern() {
      return uiPattern;
    }

    public boolean isMultiRecord() {
      return multiRecord;
    }

    public void setNewDefinition(boolean newDefinition) {
      this.newDefinition = newDefinition;
    }

    public String getWindowId() {
      return windowId;
    }

    public void setWindowId(String windowId) {
      this.windowId = windowId;
    }

    public String getWindowTitle() {
      return windowTitle;
    }

    public void setWindowTitle(String windowTitle) {
      this.windowTitle = windowTitle;
    }

    @Dependent
    public class Value {
      private String value;
      private String labelValue;

      public Value(org.openbravo.model.ad.domain.List valueList) {
        labelValue = OBViewUtil.getLabel(valueList, valueList.getADListTrlList());
        value = valueList.getSearchKey();
      }

      public String getValue() {
        return value;
      }

      public String getLabel() {
        return labelValue;
      }
    }
  }

  @Dependent
  public class IconButton {
    protected String action;
    protected String type;
    protected String label;
    protected boolean processDefinition = false;
    protected String processDefinitionId = null;
    protected String url;
    protected String actionHandler;
    protected String uiPattern;
    protected String windowId;
    protected boolean multiRecord;

    public String getAction() {
      return action;
    }

    public String getType() {
      return type;
    }

    public String getLabel() {
      return label;
    }

    public boolean isProcessDefinition() {
      return processDefinition;
    }

    public String getProcessDefinitionId() {
      return processDefinitionId;
    }

    public void setProcessDefinition(String processDefinitionId) {
      // new process definition has more precedence
      org.openbravo.client.application.Process newProcess = OBDal.getInstance().get(org.openbravo.client.application.Process.class, processDefinitionId);
      url = "/";
      actionHandler = newProcess.getJavaClassName();
      uiPattern = newProcess.getUIPattern();
      multiRecord = newProcess.isMultiRecord();
      windowId = tab.getWindow().getId();
    }

    public String getUrl() {
      return url;
    }

    public String getActionHandler() {
      return actionHandler;
    }

    public String getUiPattern() {
      return uiPattern;
    }

    public String getWindowId() {
      return windowId;
    }

    public boolean isMultiRecord() {
      return multiRecord;
    }
  }

  public static String getPrintUrl(Tab myTab) {
    String processUrl = "";
    if (myTab.getProcess() != null) {
      Process process = myTab.getProcess();
      for (ModelImplementation mo : process.getADModelImplementationList()) {
        if (mo.isDefault() && ("P".equals(mo.getAction()) || "R".equals(mo.getAction()))) {
          for (ModelImplementationMapping mom : mo.getADModelImplementationMappingList()) {
            if (mom.isDefault()) {
              processUrl = ".." + mom.getMappingName();
              break;
            }
          }
          break;
        }
      }
      if (processUrl.isEmpty()) {
        processUrl = process.getSearchKey() + ".pdf";
      }
      if (processUrl.indexOf('/') == -1) {
        processUrl = "/" + FormatUtilities.replace(processUrl);
      }
    }
    return processUrl;
  }

  @Dependent
  public class PrintButton extends IconButton {
    public boolean hasEmail;

    public PrintButton() {
      Process process = tab.getProcess();
      String processUrl = getPrintUrl(tab);

      hasEmail = processUrl.contains("orders") || processUrl.contains("invoices")
          || processUrl.contains("payments");

      type = "print";
      action = "OB.ToolbarUtils.print(this.view, '" + processUrl + "', " + process.isDirectPrint()
          + ", 'printButton');";
      label = Utility.messageBD(new DalConnectionProvider(false), "Print",
          OBContext.getOBContext().getLanguage().getLanguage());
    }
  }

  public boolean isRootTab() {
    return isRootTab;
  }

  public void setRootTab(boolean isRootTab) {
    this.isRootTab = isRootTab;
  }

  public boolean isShowParentButtons() {
    return tab.isShowParentsButtons();
  }

  public boolean isTree() {
    return (tab.getTableTree() != null);
  }

  public String getTreeGrid() {
    final OBTreeGridComponent treeGridComponent = createComponent(OBTreeGridComponent.class);
    treeGridComponent.setParameters(getParameters());
    treeGridComponent.setTab(tab);
    treeGridComponent.setViewTab(this);
    return treeGridComponent.generate();
  }

  public String getReferencedTableId() {
    return tab.getTable().getId();
  }

  public boolean getShowCloneButton() {
    return tab.isObuiappShowCloneButton();
  }

  public boolean getAskToCloneChildren() {
    return tab.isObuiappCloneChildren();
  }

  private class FormFieldComparator implements Comparator<Field> {

    /**
     * Fields with null sequence number are in the bottom of the form. In case multiple null
     * sequences, it is sorted by field UUID.
     */
    @Override
    public int compare(Field arg0, Field arg1) {
      Long arg0Position = arg0.getSequenceNumber();
      Long arg1Position = arg1.getSequenceNumber();

      if (arg0Position == null && arg1Position == null) {
        return arg0.getId().compareTo(arg1.getId());
      } else if (arg0Position == null) {
        return 1;
      } else if (arg1Position == null) {
        return -1;
      }

      return (int) (arg0Position - arg1Position);
    }

  }

  @Dependent
  public class OtherField {
    private Property property;
    private boolean session;
    private String inpColumnName;

    private OtherField(Column col) {
      this(KernelUtils.getInstance().getPropertyFromColumn(col, false));
    }

    private OtherField(Property property) {
      this.property = property;
      session = property.isStoredInSession();
      inpColumnName = "inp" + Sqlc.TransformaNombreColumna(property.getColumnName());
    }

    public String getPropertyName() {
      return property.getName();
    }

    public String getInpColumnName() {
      return inpColumnName;
    }

    public String getDbColumnName() {
      return property.getColumnName();
    }

    public String getType() {
      return UIDefinitionController.getInstance().getUIDefinition(property.getColumnId()).getName();
    }

    public boolean getSession() {
      return session;
    }

  }

}
