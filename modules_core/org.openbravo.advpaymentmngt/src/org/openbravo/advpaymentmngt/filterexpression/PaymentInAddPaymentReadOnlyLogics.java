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
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.advpaymentmngt.filterexpression;

import java.util.Map;

import jakarta.enterprise.context.Dependent;
import org.codehaus.jettison.json.JSONException;
import org.openbravo.advpaymentmngt.utility.APRMConstants;
import org.openbravo.client.kernel.ComponentProvider;

@Dependent
@ComponentProvider.Qualifier(APRMConstants.PAYMENT_IN_WINDOW_ID)
public class PaymentInAddPaymentReadOnlyLogics extends AddPaymentReadOnlyLogicsHandler {

  private static final long SEQUENCE = 100l;

  @Override
  protected long getSeq() {
    return SEQUENCE;
  }

  @Override
  public boolean getPaymentDocumentNoReadOnlyLogic(Map<String, String> requestMap)
      throws JSONException {
    return true;
  }

  @Override
  public boolean getReceivedFromReadOnlyLogic(Map<String, String> requestMap) throws JSONException {
    return true;
  }

  @Override
  public boolean getPaymentMethodReadOnlyLogic(Map<String, String> requestMap)
      throws JSONException {
    return true;
  }

  @Override
  public boolean getActualPaymentReadOnlyLogic(Map<String, String> requestMap)
      throws JSONException {
    return false;
  }

  @Override
  public boolean getPaymentDateReadOnlyLogic(Map<String, String> requestMap) throws JSONException {
    return true;
  }

  @Override
  public boolean getFinancialAccountReadOnlyLogic(Map<String, String> requestMap)
      throws JSONException {
    return true;
  }

  @Override
  public boolean getCurrencyReadOnlyLogic(Map<String, String> requestMap) throws JSONException {
    return true;
  }
}
