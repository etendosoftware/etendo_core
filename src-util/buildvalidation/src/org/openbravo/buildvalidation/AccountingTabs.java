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
 * All portions are Copyright (C) 2012-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.buildvalidation;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.base.ExecutionLimits;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.OpenbravoVersion;

/**
 * This validation is related to this issue: https://issues.openbravo.com/view.php?id=13691
 * C_BP_CUSTOMER_ACCT.C_BP_CUSTOMER_ACCT_BPARTNER_UN constraint, removed status column
 * C_BP_VENDOR_ACCT.C_BP_VENDOR_ACCT_ACCTSCHEMA_UN constraint, removed status column
 * C_BP_GROUP_ACCT.C_BP_GROUP_ACCT_SCHEM_GROUP_UN constraint added
 */
public class AccountingTabs extends BuildValidation {
  private static final String alertGroupName = "Business Partner Category Duplicated Accounts";
  private static final String alertCustomerName = "Customer Business Partner Duplicated Accounts";
  private static final String alertVendorName = "Vendor Business Partner Duplicated Accounts";

  @Override
  public List<String> execute() {
    ConnectionProvider cp = getConnectionProvider();
    ArrayList<String> errors = new ArrayList<String>();
    try {
      final int wrongGroupAcct = Integer.parseInt(AccountingTabsData.countWrongBPGroupAcct(cp));
      final int wrongCustomerAcct = Integer.parseInt(AccountingTabsData.countWrongCustomerAcct(cp));
      final int wrongVendorAcct = Integer.parseInt(AccountingTabsData.countWrongVendorAcct(cp));

      if (wrongGroupAcct > 0 || wrongCustomerAcct > 0 || wrongVendorAcct > 0) {
        errors
            .add("You can not apply this MP because your instance fails in the pre-validation phase: ");

        if (wrongGroupAcct > 0) {
          errors
              .add("It is not allowed to have more than one entry in Business Partner Category ->  Accounting tab for the same accounting schema. Until 3.0MP11 it was allowed although it was wrong since the behaviour was unpredictable: any of the duplicated accounts could be used for the accounting of that business partner category. To fix this problem in your instance, you can know the duplicated entries by reviewing Alerts in your system (Alert Rule: "
                  + alertGroupName
                  + "). Once you find the duplicated entries you should remove the wrong ones. After fixing all these entries you should be able to apply this MP.");

          if (AccountingTabsData.existsAlertRule(cp, alertGroupName).equals("0")) {
            final String alertRuleId = AccountingTabsData.getUUID(cp);
            AccountingTabsData
                .insertAlertRule(
                    cp,
                    alertRuleId,
                    alertGroupName,
                    "323",
                    "select max(C_BP_Group_Acct_ID) as referencekey_id,  ad_column_identifier('C_BP_Group_Acct',max(C_BP_Group_Acct_ID),'en_US') as record_id, 0 as ad_role_id, null as ad_user_id, 'Duplicated accounting Configuration entry. Please ensure just one entry exists per accounting schema for this business partner category' as description, 'Y' as isActive, max(ad_org_id) as ad_org_id, max(ad_client_id) as ad_client_id, now() as created, 0 as createdBy,  now() as updated, 0 as updatedBy  from C_BP_Group_Acct p group by c_acctschema_id, c_bp_group_id having count(*)>1");
            processAlert(alertRuleId, cp);
          }
        }

        if (wrongCustomerAcct > 0) {
          errors
              .add("It is not allowed to have more than one entry in Business Partner -> Customer -> Customer Accounting tab for the same accounting schema. Until 3.0MP11 it was allowed although it was wrong since the behaviour was unpredictable: any of the duplicated accounts could be used for the accounting of that business partner. To fix this problem in your instance, you can know the duplicated entries by reviewing Alerts in your system (Alert Rule: "
                  + alertCustomerName
                  + "). Once you find the duplicated entries you should remove the wrong ones. After fixing all these entries you should be able to apply this MP.");

          if (AccountingTabsData.existsAlertRule(cp, alertCustomerName).equals("0")) {
            final String alertRuleId = AccountingTabsData.getUUID(cp);
            AccountingTabsData
                .insertAlertRule(
                    cp,
                    alertRuleId,
                    alertCustomerName,
                    "212",
                    "select max(C_BP_CUSTOMER_ACCT_ID) as referencekey_id, ad_column_identifier('C_BP_CUSTOMER_ACCT',max(C_BP_CUSTOMER_ACCT_ID),'en_US') as record_id, 0 as ad_role_id, null as ad_user_id, 'Duplicated accounting Configuration entry. Please ensure just one entry exists per accounting schema for this business partner (customer)' as description, 'Y' as isActive, max(ad_org_id) as ad_org_id, max(ad_client_id) as ad_client_id, now() as created, 0 as createdBy,  now() as updated, 0 as updatedBy  from C_BP_CUSTOMER_ACCT p group by c_acctschema_id, c_bpartner_id having count(*)>1");
            processAlert(alertRuleId, cp);
          }
        }

        if (wrongVendorAcct > 0) {
          errors
              .add("It is not allowed to have more than one entry in Business Partner -> Vendor -> Vendor Accounting tab for the same accounting schema. Until 3.0MP11 it was allowed although it was wrong since the behaviour was unpredictable: any of the duplicated accounts could be used for the accounting of that business partner. To fix this problem in your instance, you can know the duplicated entries by reviewing Alerts in your system (Alert Rule: "
                  + alertVendorName
                  + "). Once you find the duplicated entries you should remove the wrong ones. After fixing all these entries you should be able to apply this MP.");

          if (AccountingTabsData.existsAlertRule(cp, alertVendorName).equals("0")) {
            final String alertRuleId = AccountingTabsData.getUUID(cp);
            AccountingTabsData
                .insertAlertRule(
                    cp,
                    alertRuleId,
                    alertVendorName,
                    "213",
                    "select max(C_BP_VENDOR_ACCT_ID) as referencekey_id, ad_column_identifier('C_BP_VENDOR_ACCT',max(C_BP_VENDOR_ACCT_ID),'en_US') as record_id, 0 as ad_role_id, null as ad_user_id, 'Duplicated accounting Configuration entry. Please ensure just one entry exists per accounting schema for this business partner (vendor)' as description, 'Y' as isActive, max(ad_org_id) as ad_org_id, max(ad_client_id) as ad_client_id, now() as created, 0 as createdBy,  now() as updated, 0 as updatedBy  from C_BP_VENDOR_ACCT p group by c_acctschema_id, c_bpartner_id having count(*)>1");
            processAlert(alertRuleId, cp);
          }
        }
      }
    } catch (Exception e) {
      return handleError(e);
    }
    return errors;
  }

  /**
   * @param alertRule
   * @param conn
   * @throws Exception
   */
  private void processAlert(String adAlertruleId, ConnectionProvider cp) throws Exception {
    AccountingTabsData[] alertRule = AccountingTabsData.select(cp, adAlertruleId);
    AccountingTabsData[] alert = null;
    if (!alertRule[0].sql.equals("")) {
      try {
        alert = AccountingTabsData.selectAlert(cp, alertRule[0].sql);
      } catch (Exception ex) {
        return;
      }
    }
    // Insert
    if (alert != null && alert.length != 0) {
      StringBuilder msg = new StringBuilder();

      for (int i = 0; i < alert.length; i++) {
        boolean existsReference = false;
        if (AccountingTabsData.existsStatusColumn(cp)) {
          existsReference = AccountingTabsData.existsReference(cp, adAlertruleId,
              alert[i].referencekeyId);
        } else {
          existsReference = AccountingTabsData.existsReferenceOld(cp, adAlertruleId,
              alert[i].referencekeyId);
        }
        if (!existsReference) {
          AccountingTabsData.insertAlert(cp, alert[i].description, alertRule[0].adAlertruleId,
              alert[i].recordId, alert[i].referencekeyId);
        }
      }
    }
  }
  
  @Override
  protected ExecutionLimits getBuildValidationLimits() {
    return new ExecutionLimits("0", null, new OpenbravoVersion(3, 0, 17301));
  }

}
