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
package org.openbravo.advpaymentmngt.modulescript;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

import java.math.BigDecimal;

public class Issue28591UpdatePSD extends ModuleScript {
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      boolean issue28591UpdatePSD =  Issue28591UpdatePSDData.updateWrongPSD(cp);
      if(!issue28591UpdatePSD) {
        Issue28591UpdatePSDData[] dataps = Issue28591UpdatePSDData.selectPS(cp);
        for (Issue28591UpdatePSDData ups : dataps) {
          Issue28591UpdatePSDData.updateFinPaymentschedule(cp, ups.outstandingamt, ups.paidamt, ups.cInvoiceId);         
        }
        Issue28591UpdatePSDData[] data = Issue28591UpdatePSDData.selectPSD(cp);
        for (Issue28591UpdatePSDData upsd : data) {
          Issue28591UpdatePSDData.updatePSDAmount(cp, upsd.outstandingamt, upsd.finPaymentScheduledetailId);
        }
        Issue28591UpdatePSDData[] datapp = Issue28591UpdatePSDData.selectPaymentProposal(cp);
        for (Issue28591UpdatePSDData uppp : datapp) {
          Issue28591UpdatePSDData.updatePaymentProposal(cp, uppp.amount, uppp.finPaymentProposalId);
        }
        Issue28591UpdatePSDData.updatePaymentProp(cp);
        Issue28591UpdatePSDData.createPreference(cp);
     }
    } catch (Exception e) {
      handleError(e);
    }
 }
  
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("A918E3331C404B889D69AA9BFAFB23AC", new OpenbravoVersion(3,0,22941), 
        new OpenbravoVersion(3,0,25949));
  }
}