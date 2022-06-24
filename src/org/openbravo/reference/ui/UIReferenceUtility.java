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
 * All portions are Copyright (C) 2010-2011 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.reference.ui;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.ComboTableQueryData;

/**
 * Utility methods used by UIRefernce classes
 * 
 */
public class UIReferenceUtility {
  public static final String INACTIVE_DATA = "**";

  /**
   * Checks if the table has a translated table, making the joins to the translated one.
   * 
   * @param comboTableData
   * 
   * @param tableName
   *          Name of the table.
   * @param field
   *          Name of the field.
   * @param reference
   *          Id of the reference.
   * @return Boolean to indicate if the translated table has been found.
   * @throws Exception
   */
  static public boolean checkTableTranslation(ComboTableData comboTableData, String tableName,
      FieldProvider field, String reference) throws Exception {
    if (tableName == null || tableName.equals("") || field == null) {
      return false;
    }
    ComboTableQueryData[] data = ComboTableQueryData.selectTranslatedColumn(
        comboTableData.getPool(), field.getField("tablename"), field.getField("name"));
    if (data == null || data.length == 0) {
      return false;
    }
    int myIndex = comboTableData.index++;
    comboTableData
        .addSelectField("(CASE WHEN td_trl" + myIndex + "." + data[0].columnname + " IS NULL THEN "
            + formatField(comboTableData.getVars(), reference,
                (tableName + "." + field.getField("name")))
            + " ELSE " + formatField(comboTableData.getVars(), reference,
                ("td_trl" + myIndex + "." + data[0].columnname))
            + " END)", "NAME");
    comboTableData.addFromField(data[0].tablename + " td_trl" + myIndex + " on " + tableName + "."
        + data[0].reference + " = td_trl" + myIndex + "." + data[0].reference + " AND td_trl"
        + myIndex + ".AD_Language = ?", "td_trl" + myIndex);
    comboTableData.addFromParameter("#AD_LANGUAGE", "LANGUAGE");
    return true;
  }

  /**
   * Formats the fields to get a correct output.
   */
  static String formatField(VariablesSecureApp vars, String reference, String field) {
    String result = "";
    if (field == null) {
      return "";
    } else if (reference == null || reference.length() == 0) {
      return field;
    }

    if (reference.equals("11") /* INTEGER */
        || reference.equals("12")/* AMOUNT */
        || reference.equals("22") /* NUMBER */
        || reference.equals("23") /* ROWID */
        || reference.equals("29") /* QUANTITY */
        || reference.equals("800008") /* PRICE */
        || reference.equals("800019")/* GENERAL QUANTITY */) {
      result = "TO_NUMBER(" + field + ")";
    } else if (reference.equals("15")) {
      // DATE
      result = "TO_CHAR(" + field
          + (vars == null ? "" : (", '" + vars.getSessionValue("#AD_SqlDateFormat") + "'")) + ")";
    } else if (reference.equals("16")) {
      // DATE-TIME
      result = "TO_CHAR(" + field
          + (vars == null ? "" : (", '" + vars.getSessionValue("#AD_SqlDateTimeFormat") + "'"))
          + ")";
    } else if (reference.equals("24")) {
      // TIME
      result = "TO_CHAR(" + field + ", 'HH24:MI:SS')";
    } else if (reference.equals("20")) {
      // YESNO
      result = "COALESCE(" + field + ", 'N')";
    } else if (reference.equals("23") /* Binary */ || reference.equals("14")/* Text */) {
      result = field;
    } else {
      result = "COALESCE(TO_CHAR(" + field + "),'')";
    }

    return result;
  }

}
