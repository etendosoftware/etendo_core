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
 * All portions are Copyright (C) 2015-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.buildvalidation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.openbravo.base.ExecutionLimits;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.OpenbravoVersion;

/**
 * This validation is related to this issue: https://issues.openbravo.com/view.php?id=30069 It
 * checks if the decimal numbers are retrieved properly from the database. In case they are not
 * retrieved properly, this build validation will fail, showing a message that points users to the
 * documentation where they can check how to configure the database decimal separator properly.
 */
public class DatabaseDecimalNumberCheck extends BuildValidation {

  private final String ORIGINAL_NUMBER = "1.2";

  @Override
  public List<String> execute() {
    ConnectionProvider cp = getConnectionProvider();
    ArrayList<String> errors = new ArrayList<String>();
    try {
      String numberString = new BigDecimal(ORIGINAL_NUMBER).toString();
      String returnedNumber;
      if (cp.getRDBMS().equalsIgnoreCase("POSTGRE")) {
        returnedNumber = DatabaseDecimalNumberCheckData.checkToNumberPG(cp, numberString);
      } else {
        returnedNumber = DatabaseDecimalNumberCheckData.checkToNumberORA(cp, numberString);
      }
      if (!ORIGINAL_NUMBER.equals(returnedNumber)) {
        errors
            .add("The decimal numbers are not being retrieved properly from the database. "
                + "This could be caused because the current database locale uses a decimal separator different from a period(.). "
                + "For more information, please visit the following url: "
                + "http://wiki.openbravo.com/wiki/Installation/Custom/PostgreSQL_Database");
      }
    } catch (Exception e) {
      return handleError(e);
    }
    return errors;
  }
}
