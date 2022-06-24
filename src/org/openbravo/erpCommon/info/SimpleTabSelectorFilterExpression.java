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
 * All portions are Copyright (C) 2015 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.info;

import java.util.Map;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.Property;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Field;

/**
 * Filter expression for Simple Tab Selector reference.
 * 
 * It tries to restrict tabs to the ones matching a given table, this table can be obtained from
 * field or table parameters. If none of them is available, no filter is applied.
 * 
 * @author alostale
 *
 */
public class SimpleTabSelectorFilterExpression implements FilterExpression {

  @Override
  public String getExpression(Map<String, String> requestMap) {
    String fieldId = requestMap.get("inpadFieldId");
    String tableId = null;
    if (fieldId != null && !"null".equals(fieldId)) {
      Property fieldProperty = KernelUtils
          .getProperty(OBDal.getInstance().get(Field.class, fieldId));
      Entity targetEntity = fieldProperty.getTargetEntity();
      if (targetEntity != null) {
        tableId = targetEntity.getTableId();
      }
    } else {
      tableId = requestMap.get("inpadTableId");
    }

    if (tableId != null && !"null".equals(tableId)) {
      return "e.table.id ='" + tableId + "'";
    }
    return "";
  }
}
