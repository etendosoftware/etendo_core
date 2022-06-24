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

public class AlertMASequenceProduct extends ModuleScript {

  final static private String ALERT_RULE_SQL = "select ma_sequenceproduct_id as referencekey_id, ad_column_identifier('MA_SequenceProduct', ma_sequenceproduct_id, 'en_US') as record_id , 0 as ad_role_id, null as ad_user_id, 'Deprecated Unique Attribute in use. Set the Warehouse rule' as description, 'Y' as isactive, ad_org_id, ad_client_id, now() as created, 0 as createdBy, now() as updated, 0 as updatedBy  from ma_sequenceproduct where uniqueattconsum = 'Y' and m_warehouse_rule_id IS NULL";
  final static private String PSECUENCE_WINDOW = "800051";
  final static private String PSECUENCE_TAB = "800110";
  final static private String ALERT_RULE="Deprecated Unique Attribute in use. Set the Warehouse rule";
  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      if (!AlertMASequenceProductData.existsAlertRule(cp, ALERT_RULE,null)) {
        AlertMASequenceProductData[] data = AlertMASequenceProductData.select(cp);
        for (AlertMASequenceProductData seqproduct : data) {
          createAlert(cp, seqproduct);
        } 
      }
      
    } catch (Exception e) {
      handleError(e);
    }
  }

  private void createAlert(ConnectionProvider cp, AlertMASequenceProductData seqproduct)
      throws ServletException {

   
    String oldAlertRuleId = AlertMASequenceProductData.getAlertRuleId(cp, ALERT_RULE,
        seqproduct.adClientId);
    if (!AlertMASequenceProductData.existsAlert(cp, oldAlertRuleId, seqproduct.maSequenceproductId)) {
      if (!AlertMASequenceProductData.existsAlertRule(cp, ALERT_RULE, seqproduct.adClientId)) {
        AlertMASequenceProductData.insertAlertRule(cp, seqproduct.adClientId, ALERT_RULE,
            PSECUENCE_TAB, ALERT_RULE_SQL);
        AlertMASequenceProductData[] roles = AlertMASequenceProductData.getRoleId(cp,
            PSECUENCE_WINDOW, seqproduct.adClientId);
        for (AlertMASequenceProductData role : roles) {
          AlertMASequenceProductData.insertAlertRecipient(cp, seqproduct.adClientId,
              seqproduct.adOrgId,
              AlertMASequenceProductData.getAlertRuleId(cp, ALERT_RULE, seqproduct.adClientId),
              role.adRoleId);
        }
      }
      String alertRuleId = AlertMASequenceProductData.getAlertRuleId(cp, ALERT_RULE,
          seqproduct.adClientId);
      AlertMASequenceProductData.insertAlert(cp, seqproduct.adClientId, seqproduct.adOrgId,
          ALERT_RULE, alertRuleId, seqproduct.product, seqproduct.maSequenceproductId);
    }
  }

  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, 
        new OpenbravoVersion(3,0,18481));
  }
}