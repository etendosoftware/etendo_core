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
 * All portions are Copyright (C) 2013 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_process;

import java.sql.Connection;
import java.util.Map;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.EndYearCloseUtility;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.financialmgmt.calendar.YearClose;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

public class EndYearClose implements Process {

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    final String ACTION_CLOSE = "C";
    final String ACTION_UNDOCLOSE = "O";
    VariablesSecureApp vars = bundle.getContext().toVars();
    Map<String, Object> params = bundle.getParams();

    String strKey = (String) params.get("C_Year_Close_V_ID");
    YearClose yearClose = OBDal.getInstance().get(YearClose.class, strKey);
    ConnectionProvider conn = bundle.getConnection();
    Connection con = conn.getTransactionConnection();
    try {
      OBError msg = null;
      if (ACTION_CLOSE.equals(yearClose.getProcessed())) {
        EndYearCloseUtility eycu = new EndYearCloseUtility(yearClose.getOrganization(),
            yearClose.getYear(), conn, con, vars);
        msg = eycu.processYearClose();
      } else if (ACTION_UNDOCLOSE.equals(yearClose.getProcessed())) {
        EndYearCloseUtility eycu = new EndYearCloseUtility(yearClose.getOrganization(),
            yearClose.getYear(), conn, con, vars);
        msg = eycu.processUndoYearClose();
      }
      bundle.setResult(msg);
    } catch (Exception e) {
      throw new OBException("Process End Year Close failed", e);
    }
  }
}
