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
 * All portions are Copyright (C) 2009-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.reference.ui;

import static org.openbravo.erpCommon.utility.ComboTableData.CLIENT_LIST_PARAM_HOLDER;
import static org.openbravo.erpCommon.utility.ComboTableData.ORG_LIST_PARAM_HOLDER;

import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.ComboTableQueryData;

public class UITableDir extends UIReference {

  public UITableDir(String reference, String subreference) {
    super(reference, subreference);
  }

  @Override
  public void setComboTableDataIdentifier(ComboTableData comboTableData, String tableName,
      FieldProvider field) throws Exception {
    String fieldName = field == null ? "" : field.getField("name");
    String parentFieldName = fieldName;

    int myIndex = comboTableData.index++;
    String name = ((fieldName != null && !fieldName.equals("")) ? fieldName
        : comboTableData.getObjectName());

    String tableDirName = null;
    if (name.equalsIgnoreCase("createdby") || name.equalsIgnoreCase("updatedby")) {
      tableDirName = "AD_User";
      name = "AD_User_ID";
    } else {
      // Try to obtain the referenced table from reference. Note it is possible not to be a TableDir
      // reference, but another one inheriting from this (search).
      if (subReference != null && !subReference.equals("")) {
        TableSQLQueryData[] search = TableSQLQueryData.searchInfo(comboTableData.getPool(),
            subReference);
        if (search != null && search.length != 0) {
          name = search[0].columnname;
          tableDirName = search[0].tablename;
        }
      }
      // If not possible, use the columnname
      if (tableDirName == null) {
        tableDirName = name.substring(0, name.length() - 3);
      }
    }
    ComboTableQueryData trd[] = ComboTableQueryData.identifierColumns(comboTableData.getPool(),
        tableDirName);
    comboTableData.addSelectField("td" + myIndex + "." + name, "ID");

    String tables = tableDirName + " td" + myIndex;
    if (tableName != null && !tableName.equals("") && parentFieldName != null
        && !parentFieldName.equals("")) {
      tables += " on " + tableName + "." + parentFieldName + " = td" + myIndex + "." + name + "\n";
      tables += "AND td" + myIndex + ".AD_Client_ID IN (" + CLIENT_LIST_PARAM_HOLDER + ") \n";
      if (!comboTableData.isAllowedCrossOrgReference()) {
        tables += "AND td" + myIndex + ".AD_Org_ID IN (" + ORG_LIST_PARAM_HOLDER + ")";
      }
    } else {
      comboTableData.addWhereField(
          "td" + myIndex + ".AD_Client_ID IN (" + CLIENT_LIST_PARAM_HOLDER + ")", "CLIENT_LIST");
      if (!comboTableData.isAllowedCrossOrgReference()) {
        comboTableData.addWhereField(
            "td" + myIndex + ".AD_Org_ID IN (" + ORG_LIST_PARAM_HOLDER + ")", "ORG_LIST");
      }
    }
    comboTableData.addFromField(tables, "td" + myIndex);
    if (tableName == null || tableName.equals("")) {
      comboTableData.parseValidation();
      comboTableData.addWhereField(
          "(td" + myIndex + ".isActive = 'Y' OR td" + myIndex + "." + name + " = (?) )",
          "ISACTIVE");
      comboTableData.addWhereParameter("@ACTUAL_VALUE@", "ACTUAL_VALUE", "ISACTIVE");
    }
    for (int i = 0; i < trd.length; i++) {
      comboTableData.identifier("td" + myIndex, trd[i]);
    }
    comboTableData.addOrderByField("2");
  }

  @Override
  public boolean canBeCached() {
    return true;
  }
}
