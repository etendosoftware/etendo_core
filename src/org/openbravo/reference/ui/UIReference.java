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
 * All portions are Copyright (C) 2009-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.reference.ui;

import org.apache.commons.lang.StringEscapeUtils;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.ComboTableData;

/**
 * Base implementation for UI objects
 * 
 */
public class UIReference {

  protected String reference;
  protected String subReference;
  protected boolean numeric;

  public UIReference(String reference, String subreference) {
    this.reference = reference;
    this.subReference = subreference;
    this.numeric = false;
  }

  /**
   * This method is called to show the value in the grid, it is intended to format the value
   * properly
   * 
   * @param vars
   */
  public String formatGridValue(VariablesSecureApp vars, String value) {
    return StringEscapeUtils.escapeHtml(value);
  }

  public boolean isNumeric() {
    return numeric;
  }

  public void setComboTableDataIdentifier(ComboTableData comboTableData, String tableName,
      FieldProvider field) throws Exception {
    if (!UIReferenceUtility.checkTableTranslation(comboTableData, tableName, field, reference)) {
      comboTableData.addSelectField(UIReferenceUtility.formatField(comboTableData.getVars(),
          reference, (((tableName != null && tableName.length() != 0) ? (tableName + ".") : "")
              + field.getField("name"))),
          "NAME");
    }
  }

  /**
   * Indicates whether this reference is a cacheable combo Basically, this indicates whether the
   * ComboTableData instances related to this class will be // cached and reused by the
   * FormInitializationComponent or not. // For them to be cached, it's very important that the
   * Combo values themselves only depend on the // parameter values the combo uses; that is, that
   * they do not depend on things like session // variables, ...
   */
  public boolean canBeCached() {
    return false;
  }

}
