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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.modulescript;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;

/**
 * Updates M_InOut.IsCompletelyInvoiced flag.
 * The IsCompletelyInvoiced checkbox indicates if this document is completely invoiced or not.
 * This flag is used only in sales flow and shown in the Goods Shipment Header.
 */
public class UpdateIsCompletelyInvoiced extends ModuleScript {

  private static final Logger log4j = LogManager.getLogger();

  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      boolean isCompletelyInvoicedUpdated= UpdateIsCompletelyInvoicedData.isCompletelyInvoicedUpdated(cp);
      if(!isCompletelyInvoicedUpdated) {
        log4j.info("This moduleScript can take long to finish. Please be patient...");
        long init = System.currentTimeMillis();
        int shipmentsUpdated = UpdateIsCompletelyInvoicedData.updateIsCompletelyInvoiced(cp);
        if (shipmentsUpdated > 0) {
          log4j.info("Updated " + shipmentsUpdated + " shipments in "+(System.currentTimeMillis() - init)+" ms.");
        }
        UpdateIsCompletelyInvoicedData.createPreference(cp);
      }
    } catch (Exception e) {
      handleError(e);
    }
  }

  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, new OpenbravoVersion(3, 0, 33403));
  }
}
