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
 * All portions are Copyright (C) 2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.window;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.domaintype.DomainType;
import org.openbravo.base.model.domaintype.ForeignKeyDomainType;
import org.openbravo.base.model.domaintype.PrimitiveDomainType;
import org.openbravo.base.model.domaintype.StringEnumerateDomainType;
import org.openbravo.base.util.Check;
import org.openbravo.client.kernel.BaseTemplateComponent;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.client.kernel.Template;
import org.openbravo.client.kernel.reference.FKComboUIDefinition;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.ReferencedTree;
import org.openbravo.model.ad.domain.ReferencedTreeField;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.utility.TableTree;
import org.openbravo.service.datasource.DataSourceConstants;
import org.openbravo.service.datasource.DataSourceProperty;
import org.openbravo.service.datasource.DataSourceProperty.RefListEntry;
import org.openbravo.service.json.JsonConstants;

/**
 * The backing bean for generating the OBTreeReference client-side representation.
 * 
 * @author AugustoMauch
 */
public class OBTreeReferenceComponent extends BaseTemplateComponent {

  private static final String TREE_REFERENCE_TEMPLATE = "9690A685A3D245899EA2A9C15D50D9FB";

  private static final String TREENODE_DATASOURCE = "90034CAE96E847D78FBEF6D38CB1930D";
  private static final String LINKTOPARENT_DATASOURCE = "610BEAE5E223447DBE6FF672B703F72F";

  private static final String TREENODE_STRUCTURE = "ADTree";
  private static final String LINKTOPARENT_STRUCTURE = "LinkToParent";

  private ReferencedTree referencedTree;
  private List<ReferencedTreeField> treeFields;

  private static Logger log = LogManager.getLogger();

  @Override
  protected Template getComponentTemplate() {
    Template template = OBDal.getInstance().get(Template.class, TREE_REFERENCE_TEMPLATE);
    return template;
  }

  public void setReferencedTree(ReferencedTree referencedTree) {
    this.referencedTree = referencedTree;
  }

  public ReferencedTree getreferencedTree() {
    return referencedTree;
  }

  public static String getAdditionalProperties(ReferencedTree referencedTree,
      boolean onlyDisplayField) {
    if (onlyDisplayField && (referencedTree.getDisplayfield() == null
        || !referencedTree.getDisplayfield().isActive())) {
      return "";
    }
    final StringBuilder extraProperties = new StringBuilder();
    for (ReferencedTreeField treeField : referencedTree.getADReferencedTreeFieldList()) {
      if (onlyDisplayField && treeField != referencedTree.getDisplayfield()) {
        continue;
      }
      if (!treeField.isActive()) {
        continue;
      }
      String fieldName = getPropertyOrDataSourceField(treeField);
      final DomainType domainType = getDomainType(treeField);
      if (domainType instanceof ForeignKeyDomainType) {
        fieldName = fieldName + DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER;
      }
      if (extraProperties.length() > 0) {
        extraProperties.append(",");
      }
      extraProperties.append(fieldName);
    }
    return extraProperties.toString();
  }

  public boolean isParentSelectionAllowed() {
    return getReferencedTree().getTableTreeCategory().isParentSelectionAllowed();
  }

  private static String getPropertyOrDataSourceField(ReferencedTreeField treeField) {
    String result = null;
    if (treeField.getProperty() != null) {
      result = treeField.getProperty();
    }
    return result.replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR);
  }

  private static DomainType getDomainType(ReferencedTreeField treeField) {
    if (treeField.getRefTree().getTable() != null && treeField.getProperty() != null) {
      final String entityName = treeField.getRefTree().getTable().getName();
      final Entity entity = ModelProvider.getInstance().getEntity(entityName);
      final Property property = DalUtil.getPropertyFromPath(entity, treeField.getProperty());
      Check.isNotNull(property,
          "Property " + treeField.getProperty() + " not found in Entity " + entity);
      return property.getDomainType();
    }
    return null;
  }

  @Inject
  @ComponentProvider.Qualifier(DataSourceConstants.DS_COMPONENT_TYPE)
  private ComponentProvider componentProvider;

  @Override
  public Module getModule() {
    return getReferencedTree().getModule();
  }

  /**
   * Computes the field in the popup which can receive the value entered by the user in the
   * suggestion box, to set the first default filter.
   * 
   * @return the field in the popup to set.
   */
  public String getDefaultPopupFilterField() {
    if (getReferencedTree().getDisplayfield() != null
        && getReferencedTree().getDisplayfield().isShowingrid()) {
      if (getReferencedTree().getDisplayfield().getProperty() != null) {
        return getReferencedTree().getDisplayfield()
            .getProperty()
            .replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR);
      }
    }
    // a very common case, return the first selector field which is part of the
    // identifier
    if (getReferencedTree().getDisplayfield() == null
        || (getReferencedTree().getDisplayfield().getProperty() != null
            && getReferencedTree().getDisplayfield()
                .getProperty()
                .equals(JsonConstants.IDENTIFIER))) {
      final Entity entity = getEntity();
      if (entity != null) {
        for (Property prop : entity.getIdentifierProperties()) {
          for (ReferencedTreeField treeField : getActiveTreeFields()) {
            if (treeField.getProperty() != null && treeField.getProperty().equals(prop.getName())) {
              return treeField.getProperty();
            }
          }
        }
      }
    }
    return JsonConstants.IDENTIFIER;
  }

  public ReferencedTree getReferencedTree() {
    if (referencedTree == null) {
      referencedTree = OBDal.getInstance().get(ReferencedTree.class, getId());
      Check.isNotNull(referencedTree, "No tree reference found using id " + getId());
      Check.isTrue(referencedTree.isActive(),
          "Tree reference " + referencedTree + " is not active anymore");
    }
    return referencedTree;
  }

  private List<ReferencedTreeField> getActiveTreeFields() {
    if (treeFields == null) {
      treeFields = OBDao.getActiveOBObjectList(getReferencedTree(),
          ReferencedTree.PROPERTY_ADREFERENCEDTREEFIELDLIST);
    }
    return treeFields;
  }

  public String getValueField() {
    if (getReferencedTree().getValuefield() != null) {
      final String valueField = getPropertyOrDataSourceField(getReferencedTree().getValuefield());
      final DomainType domainType = getDomainType(getReferencedTree().getValuefield());
      if (domainType instanceof ForeignKeyDomainType) {
        return valueField + DalUtil.FIELDSEPARATOR + JsonConstants.ID;
      }
      return valueField;
    }
    return JsonConstants.ID;
  }

  public String getDisplayField() {
    if (getReferencedTree().getDisplayfield() != null) {
      return getPropertyOrDataSourceField(getReferencedTree().getDisplayfield());
    }
    // in all other cases use an identifier
    return JsonConstants.IDENTIFIER;
  }

  private boolean isBoolean(ReferencedTreeField treeField) {
    final DomainType domainType = getDomainType(treeField);
    if (domainType instanceof PrimitiveDomainType) {
      final PrimitiveDomainType primitiveDomainType = (PrimitiveDomainType) domainType;
      return boolean.class == primitiveDomainType.getPrimitiveType()
          || Boolean.class == primitiveDomainType.getPrimitiveType();
    }
    return false;
  }

  /**
   * @return true if there is at least one active field shown in grid
   */
  public String getShowSelectorGrid() {
    if (OBDao
        .getFilteredCriteria(ReferencedTreeField.class,
            Restrictions.eq(ReferencedTreeField.PROPERTY_REFTREE, getReferencedTree()),
            Restrictions.eq(ReferencedTreeField.PROPERTY_SHOWINGRID, true))
        .count() > 0) {
      return Boolean.TRUE.toString();
    }
    return Boolean.FALSE.toString();
  }

  public String getTitle() {
    String description = getReferencedTree().getReference().getName();
    if (description == null) {
      return "";
    }
    return description;
  }

  public String getDataSourceJavascript() {
    final String dataSourceId;

    Check.isNotNull(getReferencedTree().getTable(),
        "The table is null for this tree reference: " + referencedTree);
    dataSourceId = getReferencedTree().getTable().getName();

    final Map<String, Object> dsParameters = new HashMap<String, Object>(getParameters());
    dsParameters.put(DataSourceConstants.DS_ONLY_GENERATE_CREATESTATEMENT, true);
    dsParameters.put(DataSourceConstants.MINIMAL_PROPERTY_OUTPUT, true);
    final String extraProperties = getAdditionalProperties(getReferencedTree(), false);
    if (extraProperties.length() > 0) {
      dsParameters.put(JsonConstants.ADDITIONAL_PROPERTIES_PARAMETER, extraProperties.toString());
    }

    final Component component = componentProvider.getComponent(dataSourceId, dsParameters);

    return component.generate();
  }

  public String getExtraSearchFields() {
    final String displayField = getDisplayField();
    final StringBuilder sb = new StringBuilder();
    for (ReferencedTreeField treeField : getActiveTreeFields()) {
      String fieldName = getPropertyOrDataSourceField(treeField);
      if (fieldName.equals(displayField)) {
        continue;
      }
      // prevent booleans as search fields, they don't work
      if (treeField.isSearchinsuggestionbox() && !isBoolean(treeField)) {

        // handle the case that the field is a foreign key
        // in that case always show the identifier
        final DomainType domainType = getDomainType(treeField);
        if (domainType instanceof ForeignKeyDomainType) {
          fieldName = fieldName + DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER;
        }

        if (sb.length() > 0) {
          sb.append(",");
        }
        sb.append("'" + fieldName + "'");
      }
    }
    return sb.toString();
  }

  public List<LocalTreeField> getPickListFields() {
    // return the displayfield as the first column of the picklist and add all the extra fields with
    // the showInPicklist flag.
    List<LocalTreeField> pickListFields = new ArrayList<LocalTreeField>();
    final String displayField = getDisplayField();
    final LocalTreeField LocalTreeField = new LocalTreeField();
    LocalTreeField.setName(displayField);
    LocalTreeField.setPickListField(true);
    pickListFields.add(LocalTreeField);
    pickListFields.addAll(getTreeFields(true, false));
    return pickListFields;
  }

  public List<LocalTreeField> getTreeGridFields() {
    return getTreeFields(false, true);
  }

  private List<LocalTreeField> getTreeFields(boolean pickList, boolean popupGrid) {
    final List<LocalTreeField> result = new ArrayList<LocalTreeField>();

    final List<ReferencedTreeField> sortedFields = new ArrayList<ReferencedTreeField>(
        getActiveTreeFields());
    Collections.sort(sortedFields, new TreeFieldComparator());

    for (ReferencedTreeField treeField : sortedFields) {
      if (popupGrid && !treeField.isShowingrid()) {
        continue;
      }
      if (pickList && (!treeField.isShowinpicklist()
          || treeField.equals(getReferencedTree().getDisplayfield()))) {
        continue;
      }
      final LocalTreeField LocalTreeField = new LocalTreeField();
      LocalTreeField.setPickListField(pickList);
      String fieldName = getPropertyOrDataSourceField(treeField);

      // handle the case that the field is a foreign key
      // in that case always show the identifier
      final DomainType domainType = getDomainType(treeField);
      if (domainType instanceof ForeignKeyDomainType) {
        String displayField = fieldName.replace(".", DalUtil.FIELDSEPARATOR)
            + DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER;
        LocalTreeField.setDisplayField(displayField);
      }

      fieldName = fieldName.replace(".", DalUtil.FIELDSEPARATOR);

      LocalTreeField.setName(fieldName);
      LocalTreeField.setTitle(treeField.getName());
      LocalTreeField.setSort(treeField.isSort());
      LocalTreeField.setFilter(treeField.isFilter());
      LocalTreeField.setDomainType(domainType);
      LocalTreeField.setUIDefinition(getUIDefinition(treeField));
      LocalTreeField.setReferencedTreeField(treeField);
      result.add(LocalTreeField);
    }
    return result;
  }

  // used to create picklist and grid fields
  public static class LocalTreeField {
    private String title = " ";
    private String name;
    private String displayField;
    private boolean filter;
    private boolean sort;
    private DomainType domainType;
    private UIDefinition uiDefinition;
    private ReferencedTreeField treeField;
    private boolean pickListField;

    public DomainType getDomainType() {
      return domainType;
    }

    public void setDomainType(DomainType domainType) {
      this.domainType = domainType;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDisplayField() {
      return displayField;
    }

    public void setDisplayField(String displayField) {
      this.displayField = displayField;
    }

    public String getFilterEditorProperties() {
      if (getUIDefinition() != null) {
        if (getUIDefinition() instanceof FKComboUIDefinition) {
          return ", canFilter:true, required: false, filterEditorType: 'OBTreeFilterSelectItem', filterEditorProperties: {entity: '"
              + getEntityName() + "'}";
        }
        return getUIDefinition().getFilterEditorProperties(null);
      }
      return ", filterEditorType: 'OBTextItem'";
    }

    public List<LocalTreeFieldProperty> getProperties() {
      final List<LocalTreeFieldProperty> result = new ArrayList<LocalTreeFieldProperty>();
      result.add(createLocalTreeFieldProperty("title", title));
      result.add(createLocalTreeFieldProperty("name", name));
      // is used at runtime to set canFilter on false for a field
      if (!isPickListField()) {
        if (!filter) {
          result.add(createLocalTreeFieldProperty("disableFilter", !filter));
        }
        if (!sort) {
          result.add(createLocalTreeFieldProperty("canSort", sort));
        }
      }
      result.add(createLocalTreeFieldProperty("type", getType()));
      if ((domainType instanceof PrimitiveDomainType)) {
        final PrimitiveDomainType primitiveDomainType = (PrimitiveDomainType) domainType;
        if (Date.class.isAssignableFrom(primitiveDomainType.getPrimitiveType())) {
          result.add(createLocalTreeFieldProperty("width", 100));
        }
      }
      if (domainType instanceof ForeignKeyDomainType) {
        result.add(createLocalTreeFieldProperty("displayField", displayField));
      }

      if (domainType instanceof StringEnumerateDomainType) {
        Column column = null;
        if (treeField.getRefTree().getTable() != null && treeField.getProperty() != null) {
          final String entityName = treeField.getRefTree().getTable().getName();
          final Entity entity = ModelProvider.getInstance().getEntity(entityName);
          final Property property = DalUtil.getPropertyFromPath(entity, treeField.getProperty());
          if (property != null) {
            column = OBDal.getInstance().get(Column.class, property.getColumnId());
          }
        }
        if (column != null && column.getReferenceSearchKey() != null) {
          Set<String> allowedValues = DataSourceProperty
              .getAllowedValues(column.getReferenceSearchKey());
          List<RefListEntry> entries = DataSourceProperty.createValueMap(allowedValues,
              column.getReferenceSearchKey().getId());
          JSONObject jsonValueMap = new JSONObject();
          for (RefListEntry entry : entries) {
            try {
              jsonValueMap.put(entry.getValue(), entry.getLabel());
            } catch (JSONException e) {
              log.error("Error generating value map for " + name, e);
            }
          }
          LocalTreeFieldProperty valueMap = new LocalTreeFieldProperty();
          valueMap.setName("valueMap");
          valueMap.setValue(jsonValueMap.toString());
          result.add(valueMap);
        } else {
          log.warn("Cannot set value map for selector enum " + name);
        }

      }
      return result;
    }

    private LocalTreeFieldProperty createLocalTreeFieldProperty(String propName, Object propValue) {
      LocalTreeFieldProperty LocalTreeFieldProperty = new LocalTreeFieldProperty();
      LocalTreeFieldProperty.setName(propName);
      if (propValue instanceof String) {
        LocalTreeFieldProperty.setStringValue((String) propValue);
      } else {
        LocalTreeFieldProperty.setValue("" + propValue);
      }
      return LocalTreeFieldProperty;
    }

    public boolean isFilter() {
      return filter;
    }

    public void setFilter(boolean filter) {
      this.filter = filter;
    }

    public boolean isSort() {
      return sort;
    }

    public void setSort(boolean sort) {
      this.sort = sort;
    }

    public static class LocalTreeFieldProperty {
      private String name;
      private String value;

      public String getName() {
        return name;
      }

      public void setName(String name) {
        this.name = name;
      }

      public String getValue() {
        return value;
      }

      public void setStringValue(String value) {
        this.value = "'" + StringEscapeUtils.escapeJavaScript(value) + "'";
      }

      public void setValue(String value) {
        this.value = value;
      }
    }

    public String getType() {
      if (getUIDefinition() == null) {
        return "text";
      }
      return getUIDefinition().getName();
    }

    public UIDefinition getUIDefinition() {
      return uiDefinition;
    }

    public void setUIDefinition(UIDefinition uiDefinition) {
      this.uiDefinition = uiDefinition;
    }

    public void setReferencedTreeField(ReferencedTreeField treeField) {
      this.treeField = treeField;
    }

    public ReferencedTreeField getReferencedTreeField() {
      return treeField;
    }

    public String getEntityName() {
      if (treeField == null) {
        return null;
      }
      if (treeField.getRefTree().getTable() != null && treeField.getProperty() != null) {
        final String entityName = treeField.getRefTree().getTable().getName();
        final Entity entity = ModelProvider.getInstance().getEntity(entityName);
        final Property property = DalUtil.getPropertyFromPath(entity, treeField.getProperty());
        return property.getTargetEntity().getName();
      }
      return null;
    }

    public boolean isPickListField() {
      return pickListField;
    }

    public void setPickListField(boolean pickListField) {
      this.pickListField = pickListField;
    }

  }

  private class TreeFieldComparator implements Comparator<ReferencedTreeField> {

    @Override
    public int compare(ReferencedTreeField field0, ReferencedTreeField field1) {
      return (int) (field0.getRecordSortNo() - field1.getRecordSortNo());
    }

  }

  private UIDefinition getUIDefinition(ReferencedTreeField treeField) {
    if (treeField.getRefTree().getTable() != null && treeField.getProperty() != null) {
      final String entityName = treeField.getRefTree().getTable().getName();
      final Entity entity = ModelProvider.getInstance().getEntity(entityName);
      final Property property = DalUtil.getPropertyFromPath(entity, treeField.getProperty());
      return UIDefinitionController.getInstance().getUIDefinition(property.getColumnId());
    }
    return null;
  }

  private Entity getEntity() {
    if (getReferencedTree().getTable() != null) {
      final String entityName = getReferencedTree().getTable().getName();
      return ModelProvider.getInstance().getEntity(entityName);
    }
    return null;
  }

  public String getDataSourceId() {
    String dataSourceId = null;
    TableTree tableTree = referencedTree.getTableTreeCategory();
    if (tableTree != null) {
      if (TREENODE_STRUCTURE.equals(tableTree.getTreeStructure())) {
        dataSourceId = TREENODE_DATASOURCE;
      } else if (LINKTOPARENT_STRUCTURE.equals(tableTree.getTreeStructure())) {
        dataSourceId = LINKTOPARENT_DATASOURCE;
      } else {
        return tableTree.getDatasource().getId();
      }
      return dataSourceId;
    } else {
      return null;
    }
  }

  public String getReferencedTableId() {
    return referencedTree.getTable().getId();
  }
}
