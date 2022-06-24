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
package org.openbravo.client.kernel.reference;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.domain.ReferencedTable;
import org.openbravo.model.ad.ui.Field;

/**
 * Implementation of the foreign key ui definition which uses a combo box for its input/filter
 * types.
 * 
 * @author mtaal
 */
public class FKComboUIDefinition extends ForeignKeyUIDefinition {

  private static final String TABLE_AD_REFERENCE_ID = "18";

  @Override
  public String getFormEditorType() {
    return "OBFKComboItem";
  }

  @Override
  public String getGridEditorFieldProperties(Field field) {
    return "displayField: '_identifier', valueField: 'id'";
  }

  @Override
  // Overriden to include the criteriaField property for those Fields whose reference is table, and
  // whose referenced table has more than one column acting as identifier
  public String getGridFieldProperties(Field field) {
    Column column = field.getColumn();
    Reference reference = column.getReference();
    String criteriaField = "";
    if (reference.getId().equals(TABLE_AD_REFERENCE_ID)) {
      Reference referenceSearchKey = column.getReferenceSearchKey();
      if (referenceSearchKey != null && referenceSearchKey.getADReferencedTableList().size() > 0) {
        ReferencedTable referencedTable = referenceSearchKey.getADReferencedTableList().get(0);
        if (referencedTable != null) {
          Property prop = KernelUtils.getInstance().getPropertyFromColumn(column);
          Property referencedProp = KernelUtils.getInstance()
              .getPropertyFromColumn(referencedTable.getDisplayedColumn());
          if (prop != null && referencedProp != null) {
            if (isTableWithMultipleIdentifierColumns(referencedTable.getTable())
                && !isPropertyField(field)) {
              // For property fields we do not set the criteriaField, this way we can retrieve the
              // complete path from the client side.
              // See issue https://issues.openbravo.com/view.php?id=30800
              criteriaField = ", criteriaField: " + "'" + prop.getName() + DalUtil.FIELDSEPARATOR
                  + referencedProp.getName() + "', criteriaDisplayField: '"
                  + referencedProp.getName() + "'";
            }
            // always pass the display property in case of table references. This is used to fetch
            // the record properly in grid filter.
            // refer issue https://issues.openbravo.com/view.php?id=26696
            criteriaField = criteriaField
                .concat(", displayProperty: '" + referencedProp.getName() + "'");
          }
        }
      }
    }
    return super.getGridFieldProperties(field) + criteriaField;
  }

  /* Returns true if the identifier of the table is composed of more than one column */
  private Boolean isTableWithMultipleIdentifierColumns(Table relatedTable) {
    return ModelProvider.getInstance() //
        .getTable(relatedTable.getDBTableName()) //
        .getColumns() //
        .stream() //
        .filter(org.openbravo.base.model.Column::isIdentifier) //
        .count() > 1;
  }

  private boolean isPropertyField(Field field) {
    if (field.getProperty() != null) {
      return true;
    }
    return false;
  }

  @Override
  public String getFieldProperties(Field field, boolean getValueFromSession) {
    JSONObject value;
    try {
      value = new JSONObject(super.getFieldProperties(field, getValueFromSession));
      return getValueInComboReference(field, getValueFromSession, value.getString("classicValue"));
    } catch (JSONException e) {
      throw new OBException("Error while computing combo data", e);
    }
  }

  @Override
  public String getFieldPropertiesFirstRecord(Field field, boolean getValueFromSession) {
    JSONObject value;
    try {
      value = new JSONObject(super.getFieldProperties(field, getValueFromSession));
      return getValueInComboReference(field, getValueFromSession, value.getString("classicValue"),
          true);
    } catch (JSONException e) {
      throw new OBException("Error while computing combo data", e);
    }
  }

  @Override
  public String getFieldPropertiesWithoutCombo(Field field, boolean getValueFromSession) {
    return super.getFieldProperties(field, getValueFromSession);
  }

}
