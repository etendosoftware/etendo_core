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
 * All portions are Copyright (C) 2010-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.client.application.ApplicationUtils;
import org.openbravo.client.application.GCSystem;
import org.openbravo.client.application.GCTab;
import org.openbravo.client.application.window.OBViewFieldHandler.OBViewField;
import org.openbravo.client.application.window.OBViewFieldHandler.OBViewFieldDefinition;
import org.openbravo.client.application.window.OBViewTab.ButtonField;
import org.openbravo.client.kernel.BaseTemplateComponent;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.Template;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.AuxiliaryInput;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.service.datasource.DataSource;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * The backing bean for generating the OBViewGrid client-side representation.
 * 
 * @author mtaal
 */
public class OBViewGridComponent extends BaseTemplateComponent {

  private static final String DEFAULT_TEMPLATE_ID = "91DD63545B674BE8801E1FA4F48FF4C6";
  private static final String PROCESS_NOW_PROPERTY = "processNow";
  private static final String PROCESSED_PROPERTY = "processed";
  protected static final Map<String, String> TEMPLATE_MAP = new HashMap<String, String>();

  static {
    // Map: WindowType - Template
    TEMPLATE_MAP.put("OBUIAPP_PickAndExecute", "EE3A4F4E485D47CB8057B90C40D134A0");
  }

  private boolean applyTransactionalFilter = false;
  private Tab tab;
  private Entity entity;

  private OBViewTab viewTab;
  private Optional<GCSystem> systemGridConfig;
  private Map<String, Optional<GCTab>> tabsGridConfig;

  @Override
  protected Template getComponentTemplate() {
    final String windowType = tab.getWindow().getWindowType();
    if (TEMPLATE_MAP.containsKey(windowType)) {
      return OBDal.getInstance().get(Template.class, TEMPLATE_MAP.get(windowType));
    }
    return OBDal.getInstance().get(Template.class, DEFAULT_TEMPLATE_ID);
  }

  public Tab getTab() {
    return tab;
  }

  public void setTab(Tab tab) {
    this.tab = tab;
    entity = ModelProvider.getInstance().getEntityByTableId(tab.getTable().getId());
  }

  public String getWhereClauseSQL() {
    if (tab.getSQLWhereClause() != null) {
      return tab.getSQLWhereClause();
    }
    return "";
  }

  public String getOrderByClause() {
    if (tab.getHqlorderbyclause() != null) {
      return tab.getHqlorderbyclause();
    }
    return "";
  }

  public String getOrderByClauseSQL() {
    if (tab.getSQLOrderByClause() != null) {
      return tab.getSQLOrderByClause();
    }
    return "";
  }

  public String getSortField() {
    if (getOrderByClause().length() > 0) {
      return "";
    }

    long lowestSortno = Long.MAX_VALUE;
    OBViewField sortByField = null;
    for (OBViewFieldDefinition localField : getViewTab().getFieldHandler().getFields()) {
      if (!(localField instanceof OBViewField)) {
        continue;
      }
      final OBViewField viewField = (OBViewField) localField;
      final Long recordSortno = viewField.getField().getRecordSortNo();
      if (viewField.getLength() < 2000 && viewField.isShowInitiallyInGrid() && recordSortno != null
          && recordSortno < lowestSortno) {
        sortByField = viewField;
      }
    }
    if (sortByField != null && sortByField.getProperty() != null) {
      return sortByField.getProperty().getName();
    }

    // use 2 examples of sequence number of line no
    if (entity.hasProperty(Tab.PROPERTY_SEQUENCENUMBER)) {
      return Tab.PROPERTY_SEQUENCENUMBER;
    }
    if (entity.hasProperty(OrderLine.PROPERTY_LINENO)) {
      return OrderLine.PROPERTY_LINENO;
    }

    for (OBViewFieldDefinition localField : getViewTab().getFieldHandler().getFields()) {
      if (!(localField instanceof OBViewField)) {
        continue;
      }
      final OBViewField viewField = (OBViewField) localField;
      if (viewField.getProperty() != null && viewField.getProperty().isIdentifier()) {
        return viewField.getProperty().getName();
      }
    }
    return "";
  }

  public boolean isHasFilterClause() {
    return (this.isApplyTransactionalFilter() || StringUtils.isNotBlank(tab.getHqlfilterclause()));
  }

  public String getFilterClauseSQL() {
    if (tab.getFilterClause() != null) {
      return tab.getFilterClause();
    }
    return "";
  }

  public String getFilterName() {
    String filterName = "";

    if (tab.getHqlfilterclause() != null) {
      filterName = Utility.messageBD(new DalConnectionProvider(false), "OBUIAPP_ImplicitFilter",
          OBContext.getOBContext().getLanguage().getLanguage());
      if (tab.getFilterName() != null) {
        filterName += "<i>("
            + OBViewUtil.getLabel(tab, tab.getADTabTrlList(), Tab.PROPERTY_FILTERNAME) + ")</i>";
      }
    }

    if (isApplyTransactionalFilter()) {
      if (!filterName.isEmpty()) {
        filterName += " " + Utility.messageBD(new DalConnectionProvider(false), "And",
            OBContext.getOBContext().getLanguage().getLanguage()) + " ";
      }
      filterName += Utility
          .messageBD(new DalConnectionProvider(false), "OBUIAPP_TransactionalFilter",
              OBContext.getOBContext().getLanguage().getLanguage())
          .replace("%n", Utility.getTransactionalDate(new DalConnectionProvider(false),
              RequestContext.get().getVariablesSecureApp(), tab.getWindow().getId()));
    }

    if (!filterName.isEmpty()) {
      filterName = Utility.messageBD(new DalConnectionProvider(false), "OBUIAPP_FilteredGrid",
          OBContext.getOBContext().getLanguage().getLanguage()) + " " + filterName + ".";
    }

    return filterName;
  }

  public String getUiPattern() {
    return tab.getUIPattern();
  }

  public boolean isApplyTransactionalFilter() {
    return applyTransactionalFilter;
  }

  public void setApplyTransactionalFilter(boolean applyTransactionalFilter) {
    this.applyTransactionalFilter = applyTransactionalFilter;
  }

  public OBViewTab getViewTab() {
    return viewTab;
  }

  public void setViewTab(OBViewTab viewTab) {
    this.viewTab = viewTab;
  }

  /**
   * Returns the string representation of an array that contains all the properties that must always
   * be returned from the datasource when the grid asks for data: - id - client and organization -
   * all the properties that compose the identifier of the entity - all button fields with label
   * values - the link to parent properties - all the properties that are part of the display logic
   * of the tab buttons
   */
  public List<String> getRequiredGridProperties() {
    List<String> requiredGridProperties = new ArrayList<String>();
    requiredGridProperties.add("id");
    // Needed to check if the record is readonly (check addWritableAttribute method of
    // DefaultJsonDataService)
    requiredGridProperties.add("client");
    requiredGridProperties.add("organization");
    // Audit fields are mandatory because the FIC does not returned them when called in EDIT mode
    requiredGridProperties.add("updatedBy");
    requiredGridProperties.add("updated");
    requiredGridProperties.add("creationDate");
    requiredGridProperties.add("createdBy");

    // Always include all the properties that are part of the identifier of the entity
    for (Property identifierProperty : this.entity.getIdentifierProperties()) {
      requiredGridProperties.add(identifierProperty.getName());
    }

    // Properties related to buttons that have label values
    List<ButtonField> buttonFields = getViewTab().getAllButtonFields();
    for (ButtonField buttonField : buttonFields) {
      requiredGridProperties.add(buttonField.getPropertyName());
    }

    // List of properties that are part of the display logic of the subtabs
    List<String> tabDisplayLogicFields = getViewTab().getDisplayLogicFields();
    for (String tabDisplayLogicField : tabDisplayLogicFields) {
      requiredGridProperties.add(tabDisplayLogicField);
    }

    // List of properties that are part of the display logic of buttons
    List<String> propertiesInButtonFieldDisplayLogic = getViewTab().getFieldHandler()
        .getPropertiesInButtonFieldDisplayLogic();
    for (String propertyName : propertiesInButtonFieldDisplayLogic) {
      requiredGridProperties.add(propertyName);
    }

    // List of hidden properties that are part of display logic (see
    // https://issues.openbravo.com/view.php?id=25586)
    List<String> hiddenPropertiesInDisplayLogic = getViewTab().getFieldHandler()
        .getHiddenPropertiesInDisplayLogic();
    for (String propertyName : hiddenPropertiesInDisplayLogic) {
      requiredGridProperties.add(propertyName);
    }

    // Always include the property that links to the parent tab
    String linkToParentPropertyName = this.getLinkToParentPropertyName();
    if (linkToParentPropertyName != null && !linkToParentPropertyName.isEmpty()) {
      requiredGridProperties.add(linkToParentPropertyName);
    } else {
      // See issue https://issues.openbravo.com/view.php?id=30132
      // If the child tab does not have a property marked as link to parent, look for the property
      // in the entity of the tab pointing to the parent tab
      String parentPropertyName = this.getParentPropertyName();
      if (parentPropertyName != null && !parentPropertyName.isEmpty()
          && !requiredGridProperties.contains(parentPropertyName)) {
        requiredGridProperties.add(parentPropertyName);
      }
    }

    // Include the Stored in Session properties
    List<String> storedInSessionProperties = getViewTab().getFieldHandler()
        .getStoredInSessionProperties();
    for (String storedInSessionProperty : storedInSessionProperties) {
      requiredGridProperties.add(storedInSessionProperty);
    }

    // Include the properties used in the auxiliary inputs of this tab
    List<String> propertiesUsedInAuxiliaryInputs = getPropertiesUsedInAuxiliaryInputs();
    for (String propertyUsedInAuxiliaryInputs : propertiesUsedInAuxiliaryInputs) {
      requiredGridProperties.add(propertyUsedInAuxiliaryInputs);
    }

    // Include the Processing and Processed propertes, required by doc action buttons (see
    // https://issues.openbravo.com/view.php?id=25460)
    if (getViewTab().getFieldHandler().hasProcessNowProperty()) {
      requiredGridProperties.add(PROCESS_NOW_PROPERTY);
    }
    if (getViewTab().getFieldHandler().hasProcessedProperty()) {
      requiredGridProperties.add(PROCESSED_PROPERTY);
    }

    return requiredGridProperties;
  }

  /**
   * @return the list of properties that belong to this entity and that are used in auxiliary inputs
   *         declared for this tab
   */
  private List<String> getPropertiesUsedInAuxiliaryInputs() {
    OBCriteria<AuxiliaryInput> criteria = OBDal.getInstance().createCriteria(AuxiliaryInput.class);
    criteria.add(Restrictions.eq(AuxiliaryInput.PROPERTY_TAB, tab));
    List<AuxiliaryInput> auxInputs = criteria.list();
    boolean throwExceptionIfNotExists = false;
    List<String> propertiesUsedInAuxiliaryInputs = new ArrayList<String>();
    for (AuxiliaryInput auxInput : auxInputs) {
      List<String> possibleColumns = parseAuxInputCode(auxInput.getValidationCode());
      for (String columnName : possibleColumns) {
        Property property = entity.getPropertyByColumnName(columnName, throwExceptionIfNotExists);
        if (property != null) {
          propertiesUsedInAuxiliaryInputs.add(property.getName());
        }
      }
    }
    return propertiesUsedInAuxiliaryInputs;
  }

  /**
   * Returns the list of tokens that appear between '@' in a validation code
   * 
   * @param validationCode
   *          the validation code where the '@' token '@' substrings will be looked for in
   * @return the list of tokens that appear between '@' in a validation code
   */
  private List<String> parseAuxInputCode(String validationCode) {
    List<String> possibleProperties = new ArrayList<String>();
    String token = validationCode;
    int i = token.indexOf("@");
    while (i != -1) {
      token = token.substring(i + 1);
      if (!token.startsWith("SQL")) {
        i = token.indexOf("@");
        if (i != -1) {
          String strAux = token.substring(0, i);
          token = token.substring(i + 1);
          if (!possibleProperties.contains(strAux)) {
            possibleProperties.add(strAux);
          }
        }
      }
      i = token.indexOf("@");
    }
    return possibleProperties;
  }

  private String getLinkToParentPropertyName() {
    Tab parentTab = KernelUtils.getInstance().getParentTab(tab);
    if (parentTab == null) {
      return null;
    }
    List<Property> linkToparentPropertyList = entity.getParentProperties();
    if (linkToparentPropertyList.isEmpty()) {
      return null;
    }
    String parentTableId = parentTab.getTable().getId();
    for (Property linkToParentProperty : linkToparentPropertyList) {
      Property referencedProperty = linkToParentProperty.getReferencedProperty();
      String referencedTableId = referencedProperty.getEntity().getTableId();
      if (parentTableId.equals(referencedTableId)) {
        return linkToParentProperty.getName();
      }
    }
    return null;
  }

  private String getParentPropertyName() {
    Tab parentTab = KernelUtils.getInstance().getParentTab(tab);
    if (parentTab == null) {
      return null;
    }
    String parentProperty = null;
    if (tab.getTable().getId().equals(parentTab.getTable().getId())
        && ("RO".equals(tab.getUIPattern()) || "SR".equals(tab.getUIPattern()))) {
      if (entity.getIdProperties().size() > 0) {
        parentProperty = entity.getIdProperties().get(0).getName();
      }
    } else {
      parentProperty = ApplicationUtils.getParentProperty(tab, parentTab);
    }
    return parentProperty;
  }

  /**
   * Returns true if the grid should filter and sort lazily. In that case, the changes done by the
   * user to filter editor and to the grid sorting will not by applied until the user clicks on an
   * 'Apply changes' button
   */
  public boolean getLazyFiltering() {
    return isConfigurationPropertyEnabled(GCTab.PROPERTY_ISLAZYFILTERING,
        GCSystem.PROPERTY_ISLAZYFILTERING, false);
  }

  /**
   * Returns true if the grid allows adding summary functions. If the tab is based on an HQL table
   * or Datasource table, this method is returning false because grid summaries are not allowed for
   * these kind of tables.
   */
  public boolean getAllowSummaryFunctions() {
    if (isHqlBasedTable(tab.getTable()) || isDatasourceBasedTable(tab.getTable())) {
      return false;
    }
    return isConfigurationPropertyEnabled(GCTab.PROPERTY_ALLOWSUMMARYFUNCTIONS,
        GCSystem.PROPERTY_ALLOWSUMMARYFUNCTIONS, true);
  }

  private boolean isConfigurationPropertyEnabled(String propertyNameAtTabLevel,
      String propertyNameAtSystemLevel, boolean defaultReturnValue) {
    Optional<GCTab> tabConf = tabsGridConfig.get(tab.getId());
    if (tabConf.isPresent()) {
      if ("Y".equals(tabConf.get().get(propertyNameAtTabLevel))) {
        return true;
      } else if ("N".equals(tabConf.get().get(propertyNameAtTabLevel))) {
        return false;
      }
    }

    if (systemGridConfig.isPresent()) {
      return (boolean) systemGridConfig.get().get(propertyNameAtSystemLevel);
    }

    return defaultReturnValue;
  }

  public boolean getAlwaysFilterFksByIdentifier() {
    DataSource dataSource = tab.getTable().getObserdsDatasource();
    // always filter using the identifier if the grid fetches its data from a manual datasource and
    // if that datasource does not support filtering foreign keys using their ids
    return (dataSource != null && !dataSource.isSupportIdFkFiltering());
  }

  public String getTableAlias() {
    Table table = tab.getTable();
    return isHqlBasedTable(table) && !StringUtils.isBlank(table.getEntityAlias())
        ? table.getEntityAlias()
        : "e";
  }

  private boolean isHqlBasedTable(Table table) {
    return ApplicationConstants.HQLBASEDTABLE.equals(table.getDataOriginType());
  }

  private boolean isDatasourceBasedTable(Table table) {
    return ApplicationConstants.DATASOURCEBASEDTABLE.equals(table.getDataOriginType());
  }

  void setGCSettings(Optional<GCSystem> systemGridConfig,
      Map<String, Optional<GCTab>> tabsGridConfig) {
    this.systemGridConfig = systemGridConfig;
    this.tabsGridConfig = tabsGridConfig;
  }
}
