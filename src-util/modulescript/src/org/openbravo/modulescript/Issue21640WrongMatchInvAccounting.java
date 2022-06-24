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

import javax.servlet.ServletException;

import org.openbravo.database.ConnectionProvider;

public class Issue21640WrongMatchInvAccounting extends ModuleScript {

  final static private String ALERT_RULE_SQL1 = "SELECT DISTINCT ad_column_identifier('m_matchinv', fa.record_id, 'en_US') as record_id, fa.record_id as referencekey_id, 0 as ad_role_id, null as ad_user_id, 'This Matched Invoice need to be posted again due to wrong accounting.' as description, 'Y' as isactive, mi.ad_org_id, mi.ad_client_id,  now() as created, 0 as createdBy, now() as updated, 0 as updatedBy FROM fact_acct fa JOIN m_matchinv mi ON fa.record_id = mi.m_matchinv_id WHERE fa.ad_table_id = '472' GROUP BY fa.fact_acct_group_id, mi.m_matchinv_id, mi.ad_org_id, fa.record_id, mi.ad_client_id HAVING count(*) > 3";
  final static private String ALERT_RULE_SQL2 = "SELECT DISTINCT ad_column_identifier('m_matchinv', fa.record_id, 'en_US') as record_id, fa.record_id as referencekey_id, 0 as ad_role_id, null as ad_user_id, 'This Matched Invoice need to be posted again due to wrong accounting.' as description, 'Y' as isactive, mi.ad_org_id, mi.ad_client_id, now() as created, 0 as createdBy, now() as updated, 0 as updatedBy FROM fact_acct fa JOIN m_matchinv mi ON fa.record_id = mi.m_matchinv_id LEFT JOIN fact_acct far ON far.line_id = mi.m_inoutline_id AND far.account_id = fa.account_id LEFT JOIN fact_acct fi ON fi.line_id = mi.c_invoiceline_id AND fi.account_id = fa.account_id AND fi.ad_table_id = '318' WHERE (round((select movementqty from m_inoutline where m_inoutline_id=mi.m_inoutline_id),2) = round((select qtyinvoiced from c_invoiceline where c_invoiceline_id=mi.c_invoiceline_id),2)) AND fa.ad_table_id = '472' AND (COALESCE(far.amtacctcr + far.amtacctdr, fa.amtacctdr + fa.amtacctcr) <> (fa.amtacctdr + fa.amtacctcr) OR COALESCE(fi.amtacctcr + fi.amtacctdr, fa.amtacctdr + fa.amtacctcr) <> (fa.amtacctdr + fa.amtacctcr))";
  final static private String ALERT_RULE1 = "Wrong Matched Invoice posting. Wrong account.";
  final static private String ALERT_RULE2 = "Wrong Matched Invoice posting. Wrong currency conversion.";
  final static private String ALERT_NAME = "This Matched Invoice need to be posted again due to wrong accounting.";
  final static private String MATCHINV_WINDOW = "107";
  final static private String MATCHINV_TAB = "408";

  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      for (Issue21640WrongMatchInvAccountingData client : Issue21640WrongMatchInvAccountingData
          .getClients(cp)) {
        if (!Issue21640WrongMatchInvAccountingData.existsAlertRule(cp, ALERT_RULE1,
            client.adClientId)) {
          createAlert1(cp, client.adClientId);
        } else {
          Issue21640WrongMatchInvAccountingData.updateAlertRule(cp, client.adClientId, ALERT_RULE1);
        }
        if (!Issue21640WrongMatchInvAccountingData.existsAlertRule(cp, ALERT_RULE2,
            client.adClientId)) {
          createAlert2(cp, client.adClientId);
        } else {
          Issue21640WrongMatchInvAccountingData.updateAlertRule(cp, client.adClientId, ALERT_RULE2);
        }
      }
    } catch (Exception e) {
      handleError(e);
    }
  }
  
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, 
        new OpenbravoVersion(3,0,20482));
  }

  private void createAlert1(ConnectionProvider cp, String clientId) throws ServletException {
    Issue21640WrongMatchInvAccountingData.insertAlertRule(cp, clientId, ALERT_RULE1, MATCHINV_TAB,
        "");
    final String alertRuleId = Issue21640WrongMatchInvAccountingData.getAlertRuleId(cp,
        ALERT_RULE1, clientId);
    Issue21640WrongMatchInvAccountingData[] roles = Issue21640WrongMatchInvAccountingData
        .getRoleIds(cp, MATCHINV_WINDOW, clientId);
    for (Issue21640WrongMatchInvAccountingData role : roles) {
      Issue21640WrongMatchInvAccountingData.insertAlertRecipient(cp, clientId, "0", alertRuleId,
          role.adRoleId);
    }
    for (Issue21640WrongMatchInvAccountingData matchInv : Issue21640WrongMatchInvAccountingData
        .select1(cp, clientId)) {
      if (!Issue21640WrongMatchInvAccountingData.existsAlert(cp, alertRuleId, matchInv.mMatchinvId)) {
        Issue21640WrongMatchInvAccountingData.insertAlert(cp, clientId, matchInv.adOrgId,
            ALERT_NAME, alertRuleId, matchInv.matchinv, matchInv.mMatchinvId);
      }
    }
  }

  private void createAlert2(ConnectionProvider cp, String clientId) throws ServletException {
    Issue21640WrongMatchInvAccountingData.insertAlertRule(cp, clientId, ALERT_RULE2, MATCHINV_TAB,
        "");
    final String alertRuleId = Issue21640WrongMatchInvAccountingData.getAlertRuleId(cp,
        ALERT_RULE2, clientId);
    Issue21640WrongMatchInvAccountingData[] roles = Issue21640WrongMatchInvAccountingData
        .getRoleIds(cp, MATCHINV_WINDOW, clientId);
    for (Issue21640WrongMatchInvAccountingData role : roles) {
      Issue21640WrongMatchInvAccountingData.insertAlertRecipient(cp, clientId, "0", alertRuleId,
          role.adRoleId);
    }
    for (Issue21640WrongMatchInvAccountingData matchInv : Issue21640WrongMatchInvAccountingData
        .select2(cp, clientId)) {
      if (!Issue21640WrongMatchInvAccountingData.existsAlert(cp, alertRuleId, matchInv.mMatchinvId)) {
        Issue21640WrongMatchInvAccountingData.insertAlert(cp, clientId, matchInv.adOrgId,
            ALERT_NAME, alertRuleId, matchInv.matchinv, matchInv.mMatchinvId);
      }
    }
  }
}