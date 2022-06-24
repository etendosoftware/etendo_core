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
 * All portions are Copyright (C) 2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.modulescript;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;

public class InitializeReservationColumnsForStorageDetail extends ModuleScript {
  private static final Logger log4j = LogManager.getLogger();

  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      boolean isMigrated = InitializeReservationColumnsForStorageDetailData.isMigrated(cp);
      if (!isMigrated) {
        InitializeReservationColumnsForStorageDetailData [] data = InitializeReservationColumnsForStorageDetailData.selectReservationAmounts(cp);
        for(int i =0; i< data.length;i++){
          InitializeReservationColumnsForStorageDetailData.updateStorageDetail(cp, data[i].reservedqty, data[i].allocatedqty, data[i].mAttributesetinstanceId, data[i].mLocatorId, data[i].mProductId, data[i].cUomId);
        }
        InitializeReservationColumnsForStorageDetailData.createPreference(cp);
      }
    } catch (Exception e) {
      log4j.error("Error executing modulescript Initializa Reservation Columns for Storage Detail");
      handleError(e);
    }
  }
  
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, 
        new OpenbravoVersion(3,0,25835));
  }
}
