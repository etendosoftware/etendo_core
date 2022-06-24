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
 * All portions are Copyright (C) 2010-2016 Openbravo SLU
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
 * This validation is related to this issue: 23164: It is possible to create a two price list
 * versions with same name and same valid from date
 */
public class DuplicatedPriceListVersion extends BuildValidation {

  @Override
  public List<String> execute() {
    ConnectionProvider cp = getConnectionProvider();
    ArrayList<String> errors = new ArrayList<String>();
    try {
      if (DuplicatedPriceListVersionData.existsDuplicatedPriceListVersion(cp)) {
        errors.add("Due to a database constraint modification, is no longer allowed "
            + "to create two price list versions with same name and same valid from date"
            + " to one price list. "
            + "There exists data in your database that do not fit this new constraint. "
            + "Please fix it in M_PRICELIST_VERSION table as M_PRICELIST_ID, NAME AND "
            + "VALIDFROM columns have not got duplicated values");
      }
    } catch (Exception e) {
      return handleError(e);
    }
    return errors;
  }
  
  @Override
  protected ExecutionLimits getBuildValidationLimits() {
    return new ExecutionLimits("0", null, new OpenbravoVersion(3, 0, 20294));
  }
}
