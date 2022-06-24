/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2014-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.modulescript;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;
import java.io.FileInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import javax.servlet.ServletException;
/**
 * This module script has ben created due to issue 28939. 
 * Due to some performance issues this module script sometimes 
 * has to create a primary key when it is missing. 
 * This should never be done but in really strange situations.
 *
 */

public class UpdateBpPaymentLine extends ModuleScript {
  private static final Logger log4j = LogManager.getLogger();

  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      boolean executed = UpdateBpPaymentLineData.isModuleScriptExecuted(cp);
      if (!executed) {
        String strRDBMS = cp.getRDBMS();
        boolean missingIndex =  false;
        int count = 0;
        try{
          if (strRDBMS.equalsIgnoreCase("POSTGRE")) {
            missingIndex = !invoiceIndexExists(cp);
            if(missingIndex){
              UpdateBpPaymentLineData.createInvoiceIndex(cp);
              UpdateBpPaymentLineData.analyzeInvoice(cp);
            }
          }
          count = UpdateBpPaymentLineData.updateBpPaymentLineInvoice(cp);
          count += UpdateBpPaymentLineData.updateBpPaymentLineOrder(cp);
        }finally{
          if (strRDBMS.equalsIgnoreCase("POSTGRE")) {
            if(missingIndex){
              UpdateBpPaymentLineData.dropInvoiceIndex(cp);
            }
          }
        }
        if (count > 0)
          log4j.info("Updated " + count + " Payment Scheduled Details.");
        UpdateBpPaymentLineData.createPreference(cp);
      }
    } catch (Exception e) {
      handleError(e);
    }
  }
  
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, 
        new OpenbravoVersion(3,0,25828));
  }
  
  private static boolean invoiceIndexExists(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        select" +
      "            i.relname as index_name" +
      "        from" +
      "            pg_class i," +
      "            pg_index ix" +
      "        where i.oid = ix.indexrelid" +
      "            and i.relname = 'c_invoice_key'    ";

    ResultSet result;
    boolean boolReturn = false;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if(result.next()) {
        boolReturn = !UtilSql.getValue(result, "index_name").equals("0");
      }
      result.close();
    } catch(SQLException e){
      log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
      throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch(Exception ex){
      log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch(Exception ignore){
        ignore.printStackTrace();
      }
    }
    return(boolReturn);
  }
}