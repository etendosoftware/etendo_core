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
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.advpaymentmngt.buildvalidation;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.base.ExecutionLimits;
import org.openbravo.buildvalidation.BuildValidation;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.OpenbravoVersion;

public class TransactionsWithMissingData extends BuildValidation {

  @Override
  public List<String> execute() {
    ConnectionProvider cp = getConnectionProvider();
    ArrayList<String> errors = new ArrayList<String>();
    try {
      // Prevent error when upgrading from a pure 2.50
      if (TransactionsWithMissingDataData.existAPRMbasetables(cp)) {

        TransactionsWithMissingDataData[] listofTransactions = TransactionsWithMissingDataData
            .selectTransactionsWithMissingData(cp);
        if (listofTransactions != null && listofTransactions.length > 0) {
          String message = "You cannot update your instance, because a pre-validation fails. "
              + "It is not allowed to upgrade to this version having transactions with null Transaction Date and/or null Accounting Date. "
              + "To fix this problem in your instance, have a look to generated alerts (Transactions with Missing Data) and identify the affected transactions. "
              + "If you have a transaction like that, fix the problem filling Transaction Date and/or Accounting Date. "
              + "It can be very helpful to visit <a href=http://wiki.openbravo.com/wiki/Update_Transaction_Refactor >Learn more</a>";

          errors.add(message);
        }
      }
    } catch (Exception e) {
      return handleError(e);
    }
    return errors;
  }

  @Override
  protected ExecutionLimits getBuildValidationLimits() {
    return new ExecutionLimits("A918E3331C404B889D69AA9BFAFB23AC", null, new OpenbravoVersion(3, 0,
        24742));
  }
}
