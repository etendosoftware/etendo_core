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
 * All portions are Copyright (C) 2010-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel.reference;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.model.ad.ui.Field;

/**
 * Implementation of the String ui definition.
 * 
 * @author mtaal
 */
public class StringUIDefinition extends UIDefinition {

  @Override
  public String getParentType() {
    return "text";
  }

  @Override
  public String getFilterEditorType() {
    return "OBTextFilterItem";
  }

  @Override
  public String getFormEditorType() {
    return "OBTextItem";
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

    return super.getFilterEditorPropertiesProperty(field) + append;
  }

  @Override
  public String getGridFieldProperties(Field field) {
    Long length = field.getColumn().getLength();

    Long displaylength = field.getDisplayedLength();
    if (displaylength == null || displaylength == 0) {
      displaylength = length;
    }

    // custom override in case the length of a document number column is 0
    if (field.getColumn().getDBColumnName().compareToIgnoreCase("documentno") == 0 && length == 0) {
      length = 20L;
    }
    return (length != null ? ", length:" + length : "") + ", displaylength:" + displaylength
        + super.getGridFieldProperties(field);
  }

  @Override
  public String getFieldProperties(Field field) {
    String fieldProperties = super.getFieldProperties(field);
    if (field != null && field.getColumn() != null) {
      final Long length = field.getColumn().getLength();
      try {
        if (length != null) {
          JSONObject o = new JSONObject(
              fieldProperties != null && fieldProperties.length() > 0 ? fieldProperties : "{}");
          o.put("length", length);
          return o.toString();
        }
      } catch (Exception e) { // ignore
        throw new OBException(e);
      }
    }
    return fieldProperties;
  }

}
