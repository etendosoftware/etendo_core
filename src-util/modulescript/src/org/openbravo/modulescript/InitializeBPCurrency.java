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

package org.openbravo.modulescript;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.utils.FormatUtilities;

public class InitializeBPCurrency extends ModuleScript {

  @Override
  // Initialize the currency for the customer.Related to the issue
  // https://issues.openbravo.com/view.php?id=25392
  // https://issues.openbravo.com/view.php?id=22091
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      boolean isExecuted= InitializeBPCurrencyData.isExecuted(cp);
      if (!isExecuted){
        InitializeBPCurrencyData.initializeCurrency(cp);
        InitializeBPCurrencyData.createPreference(cp);
        InitializeBPCurrencyData.createUpdatePreference(cp);
        // update bp currency if it is not updated by initializeCurrency
        // and there exists completed invoice in same currency 
        // for any business partner
        InitializeBPCurrencyData.UpdateBpCurrency(cp);
        InitializeBPCurrencyData.createUpdateFromInvoicePref(cp);
      } else {
        boolean isUpdated = InitializeBPCurrencyData.isUpdated(cp);
        if(!isUpdated) {
          InitializeBPCurrencyData.initializeCurrency(cp);
          InitializeBPCurrencyData.createUpdatePreference(cp);
        }
        boolean isUpdatedFromInv = InitializeBPCurrencyData.isUpdatedFromInvoice(cp);
        if(!isUpdatedFromInv) {
          InitializeBPCurrencyData.UpdateBpCurrency(cp);
          InitializeBPCurrencyData.createUpdateFromInvoicePref(cp);
        }
      }
    } catch (Exception e) {
      handleError(e);
    }
  }
  
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, 
        new OpenbravoVersion(3,0,25367));
  }
}
