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
 * All portions are Copyright (C) 2009-2011 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.reference.ui;

import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.ComboTableQueryData;

public class UIList extends UIReference {

  public UIList(String reference, String subreference) {
    super(reference, subreference);
  }

  @Override
  public void setComboTableDataIdentifier(ComboTableData comboTableData, String tableName,
      FieldProvider field) throws Exception {
    String fieldName = field == null ? "" : field.getField("name");
    String referenceValue = field == null ? "" : field.getField("referencevalue");

    boolean isValueDisplayed = ComboTableQueryData.isValueDisplayed(comboTableData.getPool(),
        ((referenceValue != null && !referenceValue.equals("")) ? referenceValue
            : comboTableData.getObjectReference()));

    int myIndex = comboTableData.index++;
    comboTableData.addSelectField("td" + myIndex + ".value", "id");

    StringBuffer identifier = new StringBuffer();
    // Add inactive data info
    identifier.append("((CASE td")
        .append(myIndex)
        .append(".isActive WHEN 'N' THEN '")
        .append(UIReferenceUtility.INACTIVE_DATA)
        .append("' ELSE '' END) ");
    // Add value
    if (isValueDisplayed) {
      identifier.append(" || td").append(myIndex).append(".value ||' - '");
    }

    // Add name
    identifier.append("|| (CASE WHEN td_trl")
        .append(myIndex)
        .append(".name IS NULL THEN td")
        .append(myIndex)
        .append(".name ELSE td_trl")
        .append(myIndex)
        .append(".name END))");

    comboTableData.addSelectField(identifier.toString(), "NAME");
    comboTableData.addSelectField("(CASE WHEN td_trl" + myIndex + ".description IS NULL THEN td"
        + myIndex + ".description ELSE td_trl" + myIndex + ".description END)", "DESCRIPTION");
    String tables = "ad_ref_list td" + myIndex;
    if (tableName != null && tableName.length() != 0 && fieldName != null
        && fieldName.length() != 0) {
      tables += " on " + tableName + "." + fieldName + " = td" + myIndex + ".value ";
    }
    comboTableData.addFromField(tables, "td" + myIndex);
    comboTableData.addFromField(
        "ad_ref_list_trl td_trl" + myIndex + " on td" + myIndex + ".ad_ref_list_id = td_trl"
            + myIndex + ".ad_ref_list_id AND td_trl" + myIndex + ".ad_language = ?",
        "td_trl" + myIndex);
    comboTableData.addFromParameter("#AD_LANGUAGE", "LANGUAGE");
    comboTableData.addWhereField("td" + myIndex + ".ad_reference_id = (?)", "KEY");
    if (referenceValue == null || referenceValue.equals("")) {
      comboTableData.addWhereParameter("AD_REFERENCE_ID", "KEY", "KEY");
      comboTableData.setParameter("AD_REFERENCE_ID", comboTableData.getObjectReference());
    } else {
      comboTableData.addWhereParameter("TD" + myIndex + ".AD_REFERENCE_ID", "KEY", "KEY");
      comboTableData.setParameter("TD" + myIndex + ".AD_REFERENCE_ID", referenceValue);
    }
    if (tableName == null || tableName.length() == 0) {
      comboTableData.parseValidation();
      comboTableData.addWhereField(
          "(td" + myIndex + ".isActive = 'Y' OR td" + myIndex + ".Value = ? )", "ISACTIVE");
      comboTableData.addWhereParameter("@ACTUAL_VALUE@", "ACTUAL_VALUE", "ISACTIVE");
    }
    comboTableData.addOrderByField("td" + myIndex + ".SeqNo");
    comboTableData.addOrderByField("(CASE WHEN td_trl" + myIndex + ".name IS NULL THEN td" + myIndex
        + ".name ELSE td_trl" + myIndex + ".name END)");
  }

  @Override
  public boolean canBeCached() {
    return true;
  }
}
