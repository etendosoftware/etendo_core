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
 * All portions are Copyright (C) 2014-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.advpaymentmngt.filterexpression;

import java.util.Map;

import javax.enterprise.context.RequestScoped;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.model.ad.ui.Window;

@RequestScoped
// Public class to allow extend the functionality, for example Add Payment popup opening from menu
public abstract class AddPaymentReadOnlyLogicsHandler {

  private static final Logger logger = LogManager.getLogger();

  /**
   * boolean value to set document number read only logic
   * 
   * @param requestMap
   *          map with parameters
   * 
   */
  public abstract boolean getPaymentDocumentNoReadOnlyLogic(Map<String, String> requestMap)
      throws JSONException;

  /**
   * boolean value to set business partner read only logic
   * 
   * @param requestMap
   *          map with parameters
   * 
   */
  public abstract boolean getReceivedFromReadOnlyLogic(Map<String, String> requestMap)
      throws JSONException;

  /**
   * boolean value to set payment method read only logic
   * 
   * @param requestMap
   *          map with parameters
   * 
   */
  public abstract boolean getPaymentMethodReadOnlyLogic(Map<String, String> requestMap)
      throws JSONException;

  /**
   * boolean value to set actual payment read only logic
   * 
   * @param requestMap
   *          map with parameters
   * 
   */
  public abstract boolean getActualPaymentReadOnlyLogic(Map<String, String> requestMap)
      throws JSONException;

  /**
   * boolean value to set payment date read only logic
   * 
   * @param requestMap
   *          map with parameters
   * 
   */
  public abstract boolean getPaymentDateReadOnlyLogic(Map<String, String> requestMap)
      throws JSONException;

  /**
   * boolean value to set financial account read only logic
   * 
   * @param requestMap
   *          map with parameters
   * 
   */
  public abstract boolean getFinancialAccountReadOnlyLogic(Map<String, String> requestMap)
      throws JSONException;

  /**
   * boolean value to set currency read only logic
   * 
   * @param requestMap
   *          map with parameters
   * 
   */
  public abstract boolean getCurrencyReadOnlyLogic(Map<String, String> requestMap)
      throws JSONException;

  protected abstract long getSeq();

  /**
   * boolean value to set converted amount read only logic
   * 
   * @param requestMap
   *          map with parameters
   * 
   */
  public boolean getConvertedAmountReadOnlyLogic(Map<String, String> requestMap)
      throws JSONException {
    String strContext = requestMap.get("context");
    if (strContext == null) {
      return false;
    }
    OBContext.setAdminMode(false);
    JSONObject context = new JSONObject(strContext);
    String strWindow = null;
    if (context.has("inpwindowId") && context.get("inpwindowId") != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString("inpwindowId"))) {
      strWindow = context.getString("inpwindowId");
    }
    Window window = OBDal.getInstance().get(Window.class, strWindow == null ? "" : strWindow);

    try {
      String value = org.openbravo.erpCommon.businessUtility.Preferences.getPreferenceValue(
          "NotAllowChangeExchange", true, OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          OBContext.getOBContext().getRole(), window);
      return Preferences.YES.equals(value);
    } catch (PropertyNotFoundException e) {
      logger.debug("Property NotAllowChangeExchange not found");
      return false;
    } catch (PropertyException e) {
      logger.error("PropertyException, there is a conflict for NotAllowChangeExchange property");
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * boolean value to set conversion rate read only logic
   * 
   * @param requestMap
   *          map with parameters
   * 
   */
  public boolean getConversionRateReadOnlyLogic(Map<String, String> requestMap)
      throws JSONException {
    String strContext = requestMap.get("context");
    if (strContext == null) {
      return false;
    }
    OBContext.setAdminMode(false);
    JSONObject context = new JSONObject(strContext);
    String strWindow = null;
    if (context.has("inpwindowId") && context.get("inpwindowId") != JSONObject.NULL
        && StringUtils.isNotEmpty(context.getString("inpwindowId"))) {
      strWindow = context.getString("inpwindowId");
    }
    Window window = OBDal.getInstance().get(Window.class, strWindow == null ? "" : strWindow);

    try {
      String value = org.openbravo.erpCommon.businessUtility.Preferences.getPreferenceValue(
          "NotAllowChangeExchange", true, OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          OBContext.getOBContext().getRole(), window);
      return Preferences.YES.equals(value);
    } catch (PropertyNotFoundException e) {
      logger.debug("Property NotAllowChangeExchange not found");
      return false;
    } catch (PropertyException e) {
      logger.error("PropertyException, there is a conflict for NotAllowChangeExchange property\n");
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
