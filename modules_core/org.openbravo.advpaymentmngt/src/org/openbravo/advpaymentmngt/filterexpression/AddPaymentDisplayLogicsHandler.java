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

import java.math.BigDecimal;
import java.util.Map;

import javax.enterprise.context.RequestScoped;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@RequestScoped
// Public class to allow extend the functionality, for example Add Payment popup opening from menu
public abstract class AddPaymentDisplayLogicsHandler {

  /**
   * boolean value to set organization display logic
   * 
   * @param requestMap
   *          map with parameters
   * 
   */
  public abstract boolean getOrganizationDisplayLogic(Map<String, String> requestMap)
      throws JSONException;

  /**
   * boolean value to set document display logic
   * 
   * @param requestMap
   *          map with parameters
   * 
   */
  public abstract boolean getDocumentDisplayLogic(Map<String, String> requestMap)
      throws JSONException;

  /**
   * boolean value to set Credit to use display logic
   * 
   * @param requestMap
   *          map with parameters
   * 
   */
  public abstract boolean getCreditToUseDisplayLogic(Map<String, String> requestMap)
      throws JSONException;

  /**
   * boolean value to set bank statement line display logic
   * 
   * @param requestMap
   *          map with parameters
   * 
   */
  public abstract boolean getBankStatementLineDisplayLogic(Map<String, String> requestMap)
      throws JSONException;

  protected abstract long getSeq();

  /**
   * boolean value to set overpayment action display logic
   * 
   * @param requestMap
   *          map with parameters
   * 
   */
  public boolean getOverpaymentActionDisplayLogic(Map<String, String> requestMap)
      throws JSONException {
    String strContext = requestMap.get("context");
    if (strContext == null) {
      return false;
    }
    JSONObject context = new JSONObject(strContext);
    if (context.has("difference")) {
      double diff = context.getDouble("difference");
      BigDecimal difference = new BigDecimal(diff);
      return (difference.signum() > 0);
    } else {
      return false;
    }

  }

}
