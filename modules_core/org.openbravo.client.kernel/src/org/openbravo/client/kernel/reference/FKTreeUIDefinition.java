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
 * All portions are Copyright (C) 2013-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel.reference;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.util.Check;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.window.OBTreeReferenceComponent;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.domain.ReferencedTree;
import org.openbravo.model.ad.domain.ReferencedTreeField;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.userinterface.selector.Selector;
import org.openbravo.userinterface.selector.SelectorComponent;
import org.openbravo.userinterface.selector.SelectorConstants;

/**
 * Implementation of the foreign key tree ui definition. It uses a tree grid for its input and
 * filter types
 */
public class FKTreeUIDefinition extends ForeignKeyUIDefinition {

  private static final String PARAM_TAB_ID = "adTabId";
  private static final String PARAM_COLUMN_NAME = "columnName";
  private static final String SELECTOR_ITEM_PARAMETER = "IsSelectorItem";
  private static final String PARAM_TARGET_PROPERTY_NAME = "targetProperty";

  @Override
  public String getFormEditorType() {
    return "OBTreeItem";
  }

  @Override
  public String getFilterEditorType() {
    return "OBTreeFilterItem";
  }

  @Override
  // get the current value for a selector item from the database
  public String getFieldProperties(Field field, boolean getValueFromSession) {
    try {
      final JSONObject json = new JSONObject(super.getFieldProperties(field, getValueFromSession));
      if (json.has("value")) {
        final Property prop = KernelUtils.getInstance().getPropertyFromColumn(field.getColumn());
        if (prop.isPrimitive()) {
          json.put("identifier", json.getString("value"));
        } else {
          final BaseOBObject target = OBDal.getInstance()
              .get(prop.getTargetEntity().getName(), json.getString("value"));
          if (target != null) {
            final ReferencedTree referencedTree = getReferencedTree(field);
            final ReferencedTreeField displayField = referencedTree.getDisplayfield();
            if (displayField == null) {
              json.put("identifier", target.getIdentifier());
            } else if (displayField.getProperty() != null) {
              json.put("identifier", DalUtil.getValueFromPath(target, displayField.getProperty()));
            } else {
              json.put("identifier", target.getIdentifier());
            }
          }
        }
      }
      return json.toString();
    } catch (Exception e) {
      throw new OBException("Exception when processing field " + field, e);
    }
  }

  @Override
  public Map<String, Object> getDataSourceParameters() {
    final Map<String, Object> params = new HashMap<String, Object>();
    final Reference reference = OBDal.getInstance().get(Reference.class, getReference().getId());
    for (ReferencedTree referencedTree : reference.getADReferencedTreeList()) {
      if (referencedTree.isActive() && referencedTree.getTable() != null) {
        final String extraProperties = OBTreeReferenceComponent
            .getAdditionalProperties(referencedTree, true);
        if (extraProperties.length() > 0) {
          params.put(JsonConstants.ADDITIONAL_PROPERTIES_PARAMETER, extraProperties);
        }
        return params;
      }
    }
    return params;
  }

  @Override
  protected String getDisplayFieldName(Field field, Property prop) {
    final ReferencedTree referencedTree = getReferencedTree(field);
    final ReferencedTreeField displayField = referencedTree.getDisplayfield();
    String displayFieldName = JsonConstants.IDENTIFIER;
    if (displayField != null && displayField.getProperty() != null) {
      displayFieldName = displayField.getProperty();
    } else {
      // fallback to the default
      return null;
    }

    if (!prop.getReferencedProperty().getEntity().hasProperty(getFirstProperty(displayFieldName))) {
      // If the first property of the display field name does not belong to the referenced entity,
      // return the displayFieldName
      // Otherwise trying to append the displayFieldName to the referenced property would later
      // result in an error
      return displayFieldName.replace(".", DalUtil.FIELDSEPARATOR);
    } else {
      final String result = (prop.getName() + DalUtil.FIELDSEPARATOR + displayFieldName)
          .replace(".", DalUtil.FIELDSEPARATOR);
      return result;
    }

  }

  private String getFirstProperty(String displayFieldName) {
    int dotPosition = displayFieldName.indexOf(DalUtil.DOT);
    if (dotPosition == -1) {
      return displayFieldName;
    } else {
      return displayFieldName.substring(0, dotPosition);
    }
  }

  @Override
  public String getFieldProperties(Field field) {
    if (field == null) {
      return super.getFieldProperties(field);
    }
    final ReferencedTree referencedTree = getReferencedTree(field);

    final String tableName = field.getColumn().getTable().getDBTableName();
    final String columnName = field.getColumn().getDBColumnName();
    final String tableId = field.getColumn().getTable().getId();

    Property property = null;
    if (!ApplicationConstants.TABLEBASEDTABLE
        .equals(field.getColumn().getTable().getDataOriginType())) {
      property = DalUtil.getPropertyByTableId(tableId, columnName);
    } else {
      property = DalUtil.getProperty(tableName, columnName);
    }

    final OBTreeReferenceComponent treeReferenceComponent = WeldUtils
        .getInstanceFromStaticBeanManager(OBTreeReferenceComponent.class);
    final Map<String, Object> parameters = new HashMap<String, Object>();
    // TODO: Do not use Selector constants
    parameters.put(PARAM_TAB_ID, field.getTab().getId());
    parameters.put(PARAM_COLUMN_NAME, field.getColumn().getDBColumnName());
    parameters.put(SELECTOR_ITEM_PARAMETER, "true");
    parameters.put(PARAM_TARGET_PROPERTY_NAME, property.getName());
    treeReferenceComponent.setId(referencedTree.getId());
    treeReferenceComponent.setParameters(parameters);
    treeReferenceComponent.setReferencedTree(referencedTree);

    // append the super fields
    final String treeFields = treeReferenceComponent.generate();
    final String superJsonStr = super.getFieldProperties(field);
    if (superJsonStr.trim().startsWith("{")) {
      return treeFields + "," + superJsonStr.trim().substring(1, superJsonStr.trim().length() - 1);
    }
    return treeFields;
  }

  @Override
  public String getParameterProperties(Parameter parameter) {
    if (parameter == null) {
      return super.getParameterProperties(parameter);
    }

    final Selector selector = parameter.getReferenceSearchKey().getOBUISELSelectorList().get(0);

    final SelectorComponent selectorComponent = WeldUtils
        .getInstanceFromStaticBeanManager(SelectorComponent.class);
    final Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put(SelectorConstants.PARAM_COLUMN_NAME, parameter.getDBColumnName());
    parameters.put(SelectorComponent.SELECTOR_ITEM_PARAMETER, "true");
    selectorComponent.setId(selector.getId());
    selectorComponent.setParameters(parameters);

    // append the super fields
    final String selectorFields = selectorComponent.generate();
    final String superJsonStr = super.getParameterProperties(parameter);
    if (superJsonStr.trim().startsWith("{")) {
      return selectorFields + ","
          + superJsonStr.trim().substring(1, superJsonStr.trim().length() - 1);
    } else {
      return selectorFields + "," + superJsonStr;
    }

  }

  private ReferencedTree getReferencedTree(Field field) {
    final Reference reference = field.getColumn().getReferenceSearchKey();
    Check.isNotNull(reference, "Field " + field + " does not have a reference value set");
    for (ReferencedTree treeReference : reference.getADReferencedTreeList()) {
      if (treeReference.isActive()) {
        return treeReference;
      }
    }
    Check.fail("No valid tree reference for field " + field);
    return null;
  }

  public static ReferencedTree getReferencedTreeFromReference(Reference reference) {
    for (ReferencedTree treeReference : reference.getADReferencedTreeList()) {
      if (treeReference.isActive()) {
        return treeReference;
      }
    }
    return null;
  }
}
