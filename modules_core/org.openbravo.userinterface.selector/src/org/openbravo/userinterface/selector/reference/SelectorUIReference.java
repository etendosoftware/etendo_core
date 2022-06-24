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

package org.openbravo.userinterface.selector.reference;

import static org.openbravo.erpCommon.utility.ComboTableData.CLIENT_LIST_PARAM_HOLDER;
import static org.openbravo.erpCommon.utility.ComboTableData.ORG_LIST_PARAM_HOLDER;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.ComboTableQueryData;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.reference.ui.UIReference;
import org.openbravo.userinterface.selector.Selector;

/**
 * Implements the User Interface part of the new customizable Reference. This part takes care of the
 * user interface in the grid and in the filter popup.
 * 
 * @author mtaal
 */
public class SelectorUIReference extends UIReference {

  public SelectorUIReference(String reference, String subreference) {
    super(reference, subreference);
  }

  private String getTableName(Selector selector) {
    // TODO: add support for datasource field
    if (selector.getTable() != null) {
      if (selector.getValuefield() != null && !selector.isCustomQuery()) {
        final Entity startEntity = ModelProvider.getInstance()
            .getEntity(selector.getTable().getName());
        final Property referedProperty = DalUtil.getPropertyFromPath(startEntity,
            selector.getValuefield().getProperty());
        return referedProperty.getEntity().getTableName();
      } else {
        return selector.getTable().getDBTableName();
      }
    } else if (selector.getObserdsDatasource() != null
        && selector.getObserdsDatasource().getTable() != null) {
      return selector.getObserdsDatasource().getTable().getDBTableName();
    }
    return null;
  }

  @Override
  public void setComboTableDataIdentifier(ComboTableData comboTableData, String tableName,
      FieldProvider field) throws Exception {
    OBContext.setAdminMode();
    try {
      Reference ref = OBDal.getInstance().get(Reference.class, subReference);
      if (!ref.getOBUISELSelectorList().isEmpty()) {
        final Selector selector = ref.getOBUISELSelectorList().get(0);
        final String selectorTableName = getTableName(selector);
        if (selectorTableName != null) {
          String fieldName = field == null ? "" : field.getField("name");
          String parentFieldName = fieldName;
          String name = ((fieldName != null && !fieldName.equals("")) ? fieldName
              : comboTableData.getObjectName());
          /*
           * if the column name is available in selector use that for comparison as it is the column
           * that is going to be saved. Solves issues
           * https://issues.openbravo.com/view.php?id=23267,
           * https://issues.openbravo.com/view.php?id=23124
           */
          if (selector.getColumn() != null) {
            name = selector.getColumn().getDBColumnName();
          }
          String tableDirName;
          if (name.equalsIgnoreCase("createdby") || name.equalsIgnoreCase("updatedby")) {
            tableDirName = "AD_User";
            name = "AD_User_ID";
          } else {
            tableDirName = selectorTableName;
          }

          int myIndex = comboTableData.index++;

          ComboTableQueryData trd[] = ComboTableQueryData
              .identifierColumns(comboTableData.getPool(), tableDirName);
          comboTableData.addSelectField("td" + myIndex + "." + name, "ID");

          String tables = tableDirName + " td" + myIndex;
          if (tableName != null && !tableName.equals("") && parentFieldName != null
              && !parentFieldName.equals("")) {
            tables += " on " + tableName + "." + parentFieldName + " = td" + myIndex + "." + name
                + "\n";
            tables += "AND td" + myIndex + ".AD_Client_ID IN (" + CLIENT_LIST_PARAM_HOLDER + ") \n";
            if (!comboTableData.isAllowedCrossOrgReference()) {
              tables += "AND td" + myIndex + ".AD_Org_ID IN (" + ORG_LIST_PARAM_HOLDER + ")";
            }
          } else {
            comboTableData.addWhereField(
                "td" + myIndex + ".AD_Client_ID IN (" + CLIENT_LIST_PARAM_HOLDER + ")",
                "CLIENT_LIST");
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
      } else {
        super.setComboTableDataIdentifier(comboTableData, tableName, field);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
