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
package org.openbravo.buildvalidation;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.base.ExecutionLimits;
import org.openbravo.buildvalidation.BuildValidation;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.OpenbravoVersion;

/**
 * This validation is related to Issue 0038386: Same search key can be used twice in warehouse
 * definition
 */

public class DuplicatedClientWarehouseValue extends BuildValidation {
  @Override
  public List<String> execute() {
    ConnectionProvider cp = getConnectionProvider();
    ArrayList<String> errors = new ArrayList<String>();
    try {
      if (DuplicatedClientWarehouseValueData.existsDuplicatedClientWarehouseValue(cp)) {
        DuplicatedClientWarehouseValueData[] clientWarehouse = DuplicatedClientWarehouseValueData
            .duplicatedClientWarehouseValue(cp);
        errors
            .add("Due to a database constraint modification, is no longer allowed to "
                + "have the same Warehouse search key more than once within client. "
                + "There exists data in your database that do not fit this new constraint. Please review following:- ");
        for (int i = 0; i < clientWarehouse.length; i++) {
          errors.add(" Client: " + clientWarehouse[i].client + ", Search Key: "
              + clientWarehouse[i].searchkey + ", Warehouse: " + clientWarehouse[i].warehouse);
        }
      }
    } catch (Exception e) {
      return handleError(e);
    }
    return errors;
  }

  @Override
  protected ExecutionLimits getBuildValidationLimits() {
    return new ExecutionLimits("0", null, new OpenbravoVersion(3, 0, 33955));
  }
}