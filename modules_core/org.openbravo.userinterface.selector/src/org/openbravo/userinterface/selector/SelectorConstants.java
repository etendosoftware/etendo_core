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
 * All portions are Copyright (C) 2009-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.userinterface.selector;

import java.util.Map;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;

/**
 * Defines constants for this module.
 * 
 * @author mtaal
 */
public class SelectorConstants {
  public static final String SELECTOR_COMPONENT_TYPE = "OBUISEL_Selector";
  public static final String JS = "js";

  // parameters used by the SelectorComponent
  public static final String PARAM_ID = "id";
  public static final String PARAM_IDENTIFIER = "_identifier";
  public static final String PARAM_FIELD_NAME = "fieldName";
  public static final String PARAM_COLUMN_NAME = "columnName";
  public static final String PARAM_DISABLED = "disabled";
  public static final String PARAM_REQUIRED = "required";
  public static final String PARAM_CALLOUT = "callOut";
  public static final String PARAM_TAB_ID = "adTabId";
  public static final String PARAM_COMBO_RELOAD = "comboReload";
  public static final String PARAM_TARGET_PROPERTY_NAME = "targetProperty";
  public static final String PARAM_ID_FILTERS = "idFilters";
  public static final String PARAM_FILTER_EXPRESSION = "filterExpression";
  private static final String PARAM_TABLE_ID = "inpTableId";
  private static final String PARAM_PICK_AND_EXECUTE_TABLE_ID = "inpPickAndExecuteTableId";

  // Reference definition IDs
  public static final String SELECTOR_REFERENCE_ID = "95E2A8B50A254B2AAE6774B8C2F28120";
  public static final String SELECTOR_AS_LINK_REFERENCE_ID = "80B1630792EA46F298A3FBF81E77EF9C";

  // SelectorDataSourceFilter constants
  public static final String DS_REQUEST_SELECTOR_ID_PARAMETER = "_selectorDefinitionId";
  public static final String DS_REQUEST_TYPE_PARAMETER = "_requestType";
  public static final String DS_REQUEST_PROCESS_DEFINITION_ID = "_processDefinitionId";
  public static final String DS_REQUEST_SELECTOR_FIELD_ID = "_selectorFieldId";
  public static final String DS_REQUEST_IS_FILTER_BY_ID_SUPPORTED = "_isFilterByIdSupported";

  /**
   * Returns whether Organization filter should be included for selectors. It should always be
   * included except when the data source is for a selector and the property the selector is for
   * allows cross organization references.
   */
  public static boolean includeOrgFilter(Map<String, String> parameters) {
    boolean isSelector = parameters.containsKey(DS_REQUEST_SELECTOR_ID_PARAMETER)
        && parameters.containsKey(PARAM_TABLE_ID)
        && parameters.containsKey(PARAM_TARGET_PROPERTY_NAME);
    if (isSelector) {
      String tableId = parameters.containsKey(PARAM_PICK_AND_EXECUTE_TABLE_ID)
          ? parameters.get(PARAM_PICK_AND_EXECUTE_TABLE_ID)
          : parameters.get(PARAM_TABLE_ID);
      Entity entity = ModelProvider.getInstance().getEntityByTableId(tableId);
      if (entity != null) {
        Property property = entity.getProperty(parameters.get(PARAM_TARGET_PROPERTY_NAME), false);
        return property != null && !property.isAllowedCrossOrgReference();
      }
    }
    return true;
  }
}
