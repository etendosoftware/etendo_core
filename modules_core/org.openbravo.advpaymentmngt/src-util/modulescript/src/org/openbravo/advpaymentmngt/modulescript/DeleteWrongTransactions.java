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
 * All portions are Copyright (C) 2014-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.advpaymentmngt.modulescript;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.sql.Connection;
import org.openbravo.utils.FormatUtilities;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

public class DeleteWrongTransactions extends ModuleScript {

  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      Connection conn = cp.getTransactionConnection();
      try {
        boolean isDeleteWrongTransactionsExecuted = DeleteWrongTransactionsData.isDeleteWrongTransactionsExecuted(cp);
        if (!isDeleteWrongTransactionsExecuted) {
          for (DeleteWrongTransactionsData paymentTransaction : DeleteWrongTransactionsData.selectPaymentTransactions(cp)) {
            DeleteWrongTransactionsData.updatePaymentStatus(conn, cp, paymentTransaction.finPaymentId);          
          }
          DeleteWrongTransactionsData.updateWrongTransactions(conn, cp);
          DeleteWrongTransactionsData.deleteWrongTransactions(conn, cp);
          DeleteWrongTransactionsData.createPreference(conn, cp); 
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
    return new ModuleScriptExecutionLimits("A918E3331C404B889D69AA9BFAFB23AC", null, 
        new OpenbravoVersion(3,0,24735));
  }
}

