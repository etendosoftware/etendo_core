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
 * All portions are Copyright (C) 2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.erpCommon.utility.ISOCurrencyPrecision;
import org.openbravo.erpCommon.utility.Utility;

/**
 * Callout used to validate that the currency standard precision is correct. This is, validate with
 * the specified currency precision in ISO 4217 Currency codes. If the new precision is higher than
 * expected in the specification, then a warning is shown to user.
 * 
 * @author Mark
 *
 */
public class SL_Currency_StdPrecision extends SimpleCallout {

  private static final int DEFAULT_CURRENCY_STANDARD_PRECISION = 2;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    // Parameters
    final String paramStandardPrecision = info.getStringParameter("inpstdprecision");
    final String paramISOCode = info.getStringParameter("inpisoCode");

    if (StringUtils.isNotEmpty(paramStandardPrecision) && StringUtils.isNotEmpty(paramISOCode)) {
      final int stdPrecision = Integer.parseInt(paramStandardPrecision);
      if (stdPrecision < 0) {
        info.addResult("inpstdprecision", DEFAULT_CURRENCY_STANDARD_PRECISION);
        info.showError(
            Utility.messageBD(this, "CurrencyStdPrecisionNegative", info.vars.getLanguage()));
      } else {
        int isoCurrencyPrecision = ISOCurrencyPrecision
            .getCurrencyPrecisionInISO4217Spec(paramISOCode);
        if (stdPrecision > isoCurrencyPrecision) {
          info.showWarning(
              String.format(Utility.messageBD(this, "CurrencyStdPrecisionHigherThanISOSpec",
                  info.vars.getLanguage()), stdPrecision, isoCurrencyPrecision, paramISOCode));
        }
      }
    }
  }

}
