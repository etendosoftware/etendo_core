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
 * All portions are Copyright (C) 2012-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.modulescript;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;
import java.sql.Connection;
import org.openbravo.utils.FormatUtilities;

public class InitializeAcctDimensionsInClient extends ModuleScript {


  @Override
  public void execute() {

    try {
      ConnectionProvider cp = getConnectionProvider();
      Connection conn = cp.getTransactionConnection();
      try {
    
          for (InitializeAcctDimensionsInClientData client : InitializeAcctDimensionsInClientData
              .getClients(cp)) {
            InitializeAcctDimensionsInClientData.updateDimClient(conn, cp);  
            InitializeAcctDimensionsInClientData.updatebpari(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updatebparirm(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updatebpesh(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updatebpmmr(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updatebpsoo(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updatebpmms(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updatebparr(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updatebpapc(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updatebpfat(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updatebpapp(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updatebparf(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updatebparc(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updatebpbgt(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updatebpamz(conn, cp, client.adClientId);  
            InitializeAcctDimensionsInClientData.updatebpapi(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updatebpglj(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updatepresh(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateprarirm(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateprapi(conn, cp, client.adClientId);  
            InitializeAcctDimensionsInClientData.updateprglj(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updatepramz(conn, cp, client.adClientId);      
            InitializeAcctDimensionsInClientData.updateprarc(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateprarf(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateprbgt(conn, cp, client.adClientId); 
            InitializeAcctDimensionsInClientData.updateprapp(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateprfat(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateprapc(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateprpoo(conn, cp, client.adClientId);   
            InitializeAcctDimensionsInClientData.updateprmms(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateprarr(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateprsoo(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateprmmr(conn, cp, client.adClientId);   
            InitializeAcctDimensionsInClientData.updateprmmi(conn, cp, client.adClientId);    
            InitializeAcctDimensionsInClientData.updateooamz(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateooapc(conn, cp, client.adClientId);  
            InitializeAcctDimensionsInClientData.updateooapi(conn, cp, client.adClientId);     
            InitializeAcctDimensionsInClientData.updateooapp(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateooarc(conn, cp, client.adClientId); 
            InitializeAcctDimensionsInClientData.updateooarf(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateooari(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateooarirm(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateooarr(conn, cp, client.adClientId);        
            InitializeAcctDimensionsInClientData.updateoobgt(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateooesh(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateooglj(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateoommi(conn, cp, client.adClientId);   
            InitializeAcctDimensionsInClientData.updateoommm(conn, cp, client.adClientId);      
            InitializeAcctDimensionsInClientData.updateoommr(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateoomms(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateoopoo(conn, cp, client.adClientId);
            InitializeAcctDimensionsInClientData.updateoorec(conn, cp, client.adClientId);   
            InitializeAcctDimensionsInClientData.updateoosoo(conn, cp, client.adClientId); 
            
         }
          cp.releaseCommitConnection(conn);
          
      } catch (Exception e) {
        cp.releaseRollbackConnection(conn);
        handleError(e);
      }
    } catch (Exception e) {
      handleError(e);
    }
  }
  
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, 
        new OpenbravoVersion(3,0,19954));
  }
}
