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
package org.openbravo.advpaymentmngt.modulescript;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

// This modulescript initializes the colum TRXTYPE (Transaction Type) in FIN_FINACC_TRANSACTION TABLE
public class UpdateTransactionTypeTransactionTab extends ModuleScript {
 
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      boolean isUpdated= UpdateTransactionTypeTransactionTabData.isExecuted(cp);
      if (!isUpdated){
        UpdateTransactionTypeTransactionTabData.updateToBankFee(cp);
        UpdateTransactionTypeTransactionTabData.updateToBPDeposit(cp);
        UpdateTransactionTypeTransactionTabData.updateToBPWithdrawal(cp);
        UpdateTransactionTypeTransactionTabData.createPreference(cp);
      }
    } catch (Exception e) {
      handleError(e);
    }   
  }
  
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("A918E3331C404B889D69AA9BFAFB23AC", null, 
        new OpenbravoVersion(3,0,26174));
  }
}