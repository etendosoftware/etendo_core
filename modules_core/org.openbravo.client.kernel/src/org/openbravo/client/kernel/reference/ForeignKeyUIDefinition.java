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

import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.model.ad.ui.Field;

/**
 * Base class of all foreign key/reference ui definitions.
 * 
 * @author mtaal
 */
public class ForeignKeyUIDefinition extends UIDefinition {

  @Override
  public String getParentType() {
    return "text";
  }

  @Override
  public String getFormEditorType() {
    return "OBFKItem";
  }

  @Override
  public String getFilterEditorType() {
    return "OBFKFilterTextItem";
  }

  @Override
  public String getFilterEditorPropertiesProperty(Field field) {
    String operator = (String) this.readGridConfigurationSetting("operator");
    String append = "";

    if (operator != null) {
      append = ", operator: '" + operator + "'";
    }

    Boolean filterOnChange = (Boolean) readGridConfigurationSetting("filterOnChange");
    if (Boolean.FALSE.equals(filterOnChange)) {
      append = append + ", filterOnChange: " + filterOnChange.toString();
    } else {
      Long thresholdToFilter = (Long) readGridConfigurationSetting("thresholdToFilter");
      if (thresholdToFilter != null) {
        append = append + ", thresholdToFilter: " + thresholdToFilter.toString();
      }
    }

    Boolean disableFkDropdown = (Boolean) readGridConfigurationSetting("disableFkDropdown");
    if (Boolean.TRUE.equals(disableFkDropdown)) {
      append = append + ", disableFkDropdown: " + disableFkDropdown.toString();
      // if the fk drop down is disabled then the filter should behave like a text filter
      // that means the filter could be trigger on editor change, if that configuration is enabled
      if (Boolean.TRUE.equals(filterOnChange)) {
        append = append + ", filterOnChange: " + filterOnChange.toString();
      }
    } else {
      // these configurations only apply if the fk filter combo is enabled
      Boolean allowFkFilterByIdentifier = (Boolean) readGridConfigurationSetting(
          "allowFkFilterByIdentifier");
      if (Boolean.FALSE.equals(allowFkFilterByIdentifier)) {
        append = append + ", allowFkFilterByIdentifier: " + allowFkFilterByIdentifier.toString();
      }

      Boolean showFkDropdownUnfiltered = (Boolean) readGridConfigurationSetting(
          "showFkDropdownUnfiltered");
      if (Boolean.TRUE.equals(showFkDropdownUnfiltered)) {
        append = append + ", showFkDropdownUnfiltered: " + showFkDropdownUnfiltered.toString();
      }
    }
    if (field != null) {
      Property keyProperty = getKeyProperty(field);
      if (keyProperty != null) {
        append = append + ", keyProperty: '" + keyProperty.getName() + "'";
      }
    }

    return super.getFilterEditorPropertiesProperty(field) + append;
  }

  private Property getKeyProperty(Field field) {
    return KernelUtils.getInstance()
        .getPropertyFromColumn(field.getColumn())
        .getReferencedProperty();
  }

  @Override
  public String getGridFieldProperties(Field field) {
    final Property prop = KernelUtils.getInstance().getPropertyFromColumn(field.getColumn());

    Long displaylength = field.getDisplayedLength();
    if (displaylength == null || displaylength == 0) {
      displaylength = field.getColumn().getLength();
    }

    // only output when really needed
    String displayField = "";
    if (getDisplayFieldName(field, prop) != null) {
      displayField = ", displayField: '" + getDisplayFieldName(field, prop) + "'";
    }
    return displayField + ", displaylength:" + displaylength + ",fkField: true"
        + super.getGridFieldProperties(field);
  }

  /**
   * Note: can return null, in that case the default display field name is used
   */
  protected String getDisplayFieldName(Field field, Property prop) {
    return null;
  }

  protected String getSuperGridFieldProperties(Field field) {
    return super.getGridFieldProperties(field);
  }

  protected String getSuperGridFieldName(Field field) {
    return super.getGridFieldName(field);
  }

  @Override
  public String getTypeProperties() {
    return "sortNormalizer: function (item, field, context){ return OB.Utilities.enumSortNormalizer(item, field, context);},";
  }
}
