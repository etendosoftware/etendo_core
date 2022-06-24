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
 * All portions are Copyright (C) 2014-2016 Openbravo SLU 
 * All Rights Reserved. 
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;

/**
 * This callout is executed when a table is picked for a tab If the selected table is a HQL table,
 * then the uipattern of the tab will be set to read only
 * 
 */
public class SL_TableTab extends SimpleCallout {
  private static final String READ_ONLY_UIPATTERN = "RO";

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    final String tableId = info.getStringParameter("inpadTableId", null);
    Table table = OBDal.getInstance().get(Table.class, tableId);
    if (table != null) {
      // if the table is based on a HQL query, the tab must be read only
      if (ApplicationConstants.HQLBASEDTABLE.equals(table.getDataOriginType())) {
        info.addResult("inpuipattern", READ_ONLY_UIPATTERN);
      }
    }
  }
}
