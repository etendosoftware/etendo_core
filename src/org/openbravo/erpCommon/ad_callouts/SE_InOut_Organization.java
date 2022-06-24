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
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.Utility;

public class SE_InOut_Organization extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    String strIsSOTrx = Utility.getContext(this, info.vars, "isSOTrx", info.getWindowId());
    String strMWarehouseId = info.vars.getStringParameter("inpmWarehouseId");
    boolean updateWarehouse = true;
    FieldProvider[] td = null;
    try {
      ComboTableData comboTableData = new ComboTableData(info.vars, this, "18", "M_Warehouse_ID",
          "197", strIsSOTrx.equals("Y") ? "C4053C0CD3DC420A9924F24FC1F860A0" : "",
          Utility.getReferenceableOrg(info.vars, info.vars.getStringParameter("inpadOrgId")),
          Utility.getContext(this, info.vars, "#User_Client", info.getWindowId()), 0);
      Utility.fillSQLParameters(this, info.vars, null, comboTableData, info.getWindowId(), "");
      td = comboTableData.select(false);
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    if (td != null && td.length > 0) {
      for (int i = 0; i < td.length; i++) {
        if (td[i].getField("id").equals(strMWarehouseId)) {
          updateWarehouse = false;
          break;
        }
      }
      if (updateWarehouse) {
        info.addResult("inpmWarehouseId", td[0].getField("id"));
      }
    } else {
      info.addResult("inpmWarehouseId", "");
    }
  }
}
