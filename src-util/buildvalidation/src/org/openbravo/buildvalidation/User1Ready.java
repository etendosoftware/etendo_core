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
 * All portions are Copyright (C) 2013-2016 Openbravo SLU
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
 * This validation is related to an API change done to implement properly User1 and User2
 * Dimensions. This change was introduced by MP18
 */
public class User1Ready extends BuildValidation {
  String[] tables = { "a_amortization", "c_budgetline", "c_cash", "c_dp_management", "c_invoice",
      "c_invoiceline_acctdimension", "c_order", "c_settlement", "c_validcombination",
      "fin_finacc_transaction", "fin_payment", "fact_acct", "gl_journal", "gl_journalline",
      "m_inout", "m_inventory", "m_movement", "m_production" };
  String[] columns = { "user2_id", "user1_id" };

  @Override
  public List<String> execute() {
    ConnectionProvider cp = getConnectionProvider();
    ArrayList<String> errors = new ArrayList<String>();
    try {
      if (!User1ReadyData.tableExists(cp, "user1")) {
        for (String table : tables) {
          if (User1ReadyData.tableExists(cp, table)) {
            for (String column : columns) {
              if (User1ReadyData.columnExists(cp, column, table)) {
                if (!User1ReadyData.countUser1Usage(cp, table, column).equals("0")) {
                  errors
                      .add("You can not apply this MP because your instance fails in the pre-validation phase: User1 and User2 dimensions information needs to be properly migrated to newly created tables (User1 and User2). Please contact support.openbravo.com for further details");
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      return handleError(e);
    }
    return errors;
  }

  @Override
  protected ExecutionLimits getBuildValidationLimits() {
    return new ExecutionLimits("0", null, new OpenbravoVersion(3, 0, 19604));
  }
}
