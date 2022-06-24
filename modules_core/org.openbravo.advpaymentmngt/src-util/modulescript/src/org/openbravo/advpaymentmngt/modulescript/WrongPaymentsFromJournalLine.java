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

import javax.servlet.ServletException;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

public class WrongPaymentsFromJournalLine extends ModuleScript {
  
  final static private String ALERT_RULE_SQL = "select distinct ad_column_identifier('FIN_Payment', p.fin_payment_id, 'en_US') as record_id, p.fin_payment_id  as referencekey_id, 0 as ad_role_id, null as ad_user_id, 'This payment needs to be checked due to wrong amount.' as description, 'Y' as isActive, p.ad_org_id, p.ad_client_id, now() as created, 0 as createdBy, now() as updated, 0 as updatedBy from fin_payment p left join gl_journalline jl on jl.fin_payment_id = p.fin_payment_id where abs(jl.amtsourcedr-jl.amtsourcecr) != p.amount order by 1";
  
  @Override
  // Inserting Alerts for payments created from G/L Journal Line with wrong amounts
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      WrongPaymentsFromJournalLineData[] data = WrongPaymentsFromJournalLineData.select(cp);
      for (WrongPaymentsFromJournalLineData wrongPayment : data) {
        createAlert(cp, wrongPayment);
      }
     } catch (Exception e) {
      handleError(e);
    }
  }
  
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("A918E3331C404B889D69AA9BFAFB23AC", null, 
        new OpenbravoVersion(3,0,24269));
  }

  private void createAlert(ConnectionProvider cp, WrongPaymentsFromJournalLineData wrongPayment)
	      throws ServletException {
	  	final String PAYMENT_IN_WINDOW = "E547CE89D4C04429B6340FFA44E70716";
	    final String PAYMENT_OUT_WINDOW = "6F8F913FA60F4CBD93DC1D3AA696E76E";
	    final String PAYMENT_IN_TAB = "C4B6506838E14A349D6717D6856F1B56";
	    final String PAYMENT_OUT_TAB = "F7A52FDAAA0346EFA07D53C125B40404";
	    String WindowOrderId = PAYMENT_OUT_WINDOW;
	    String strTabId = PAYMENT_OUT_TAB;
	    String ALERT_RULE = "Wrong Payment Out Amount";
	    if ("Y".equals(wrongPayment.isreceipt)) {
	      strTabId = PAYMENT_IN_TAB;
	      WindowOrderId = PAYMENT_IN_WINDOW;	    
	      ALERT_RULE = "Wrong Payment In Amount";
	    }
	    String strName = "Payment: '" + wrongPayment.paymentinfo
	            + "' needs to be checked due to wrong amount.";
	    String oldAlertRuleId = WrongPaymentsFromJournalLineData.getAlertRuleId(cp, ALERT_RULE,
	            wrongPayment.adClientId);
	    if (!WrongPaymentsFromJournalLineData.existsAlert(cp, oldAlertRuleId, wrongPayment.finPaymentId)) {
	    if (!WrongPaymentsFromJournalLineData.existsAlertRule(cp, ALERT_RULE, wrongPayment.adClientId)) {
	      WrongPaymentsFromJournalLineData.insertAlertRule(cp, wrongPayment.adClientId, ALERT_RULE, strTabId, "");
	      WrongPaymentsFromJournalLineData[] roles = WrongPaymentsFromJournalLineData.getRoleId(cp,
	            WindowOrderId, wrongPayment.adClientId);
	        for (WrongPaymentsFromJournalLineData role : roles) {
	          WrongPaymentsFromJournalLineData.insertAlertRecipient(cp, wrongPayment.adClientId,
	              wrongPayment.adOrgId,
	              WrongPaymentsFromJournalLineData.getAlertRuleId(cp, ALERT_RULE, wrongPayment.adClientId),
	              role.adRoleId);
	        }
	      }
	    	String alertRuleId = WrongPaymentsFromJournalLineData.getAlertRuleId(cp, ALERT_RULE,
	            wrongPayment.adClientId);
	    	WrongPaymentsFromJournalLineData.insertAlert(cp, wrongPayment.adClientId, wrongPayment.adOrgId,
	          strName, alertRuleId, wrongPayment.paymentinfo, wrongPayment.finPaymentId);
	    }
  	}
}
