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
package org.openbravo.erpCommon.ad_callouts;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.filter.RegexFilter;
import org.openbravo.base.filter.RequestFilter;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.window.servlet.CalloutServletConfig;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonConstants;

/**
 * This class is used to implement Openbravo ERP servlet callouts in a simple manner.
 * <p>
 * To develop a new servlet callout based on this class you only have to create a new java class
 * that extends the method: <blockquote>
 * 
 * <pre>
 * protected void execute(CalloutInfo info) throws ServletException;
 * </pre>
 * 
 * </blockquote>
 * <p>
 * In this method you can develop the logic of the callout and use the info object of class
 * {@link CalloutInfo} to access window fields, database and other methods
 * 
 * @author aro
 */
public abstract class SimpleCallout extends DelegateConnectionProvider {

  private static Logger log = LogManager.getLogger();

  /**
   * Overwrite this method to implement a new servlet callout based in <code>SimlpleCallout</code>
   * 
   * @param info
   *          The {@code CalloutInfo} that contains all helper data to access callout information
   *          and servlet information
   * @throws ServletException
   */
  protected abstract void execute(CalloutInfo info) throws ServletException;

  @Override
  public void init(CalloutServletConfig config) {
    super.init(config);
  }

  /**
   * This method execute the SimpleCallout operations.
   * 
   * @return JSONObject with the values updated by the SimpleCallout.
   */
  public JSONObject executeSimpleCallout(RequestContext request)
      throws ServletException, JSONException {
    // prepare values for callout
    VariablesSecureApp vars = new VariablesSecureApp(request.getRequest());
    CalloutInfo info = new CalloutInfo(vars);

    try {
      // execute the callout
      execute(info);
    } catch (ServletException ex) {
      // Error in current SimpleCallout, continue with following callout.
    }

    return info.getJSONObjectResult();
  }

  /**
   * Helper class that contains all data to access callout information and servlet information
   */
  public static class CalloutInfo {

    private JSONObject result;
    private String currentElement;
    private Map<String, String> currentComboResult;

    /**
     * Provides the coder friendly methods to retrieve certain environment, session and servlet call
     * variables.
     */
    public VariablesSecureApp vars;

    private CalloutInfo(VariablesSecureApp vars) {
      this.vars = vars;
      result = new JSONObject();
      currentElement = null;
    }

    /**
     * 
     * Invokes another SimpleCallout. This method allows to divide callouts functionality into
     * several callout classes
     * 
     * @param callout
     *          SimpleCallout instance to invoke
     */
    public void executeCallout(SimpleCallout callout) throws ServletException {
      callout.execute(this);
    }

    /**
     * 
     * @return The name of field that triggered the callout.
     */
    public String getLastFieldChanged() {
      return vars.getStringParameter("inpLastFieldChanged");
    }

    /**
     * 
     * @return The Tab Id that triggered the callout.
     */
    public String getTabId() {
      return vars.getStringParameter("inpTabId", IsIDFilter.instance);
    }

    /**
     * 
     * @return The Window Id that triggered the callout.
     */
    public String getWindowId() {
      return vars.getStringParameter("inpwindowId", IsIDFilter.instance);
    }

    /**
     * 
     * @param param
     *          The name of the field to get the value.
     * @param filter
     *          Filter used to validate the input against list of allowed inputs.
     * @return The value of a field named param as an {@code String}. If value is modified
     *         previously by a parent callout, updated value is returned.
     */
    public String getStringParameter(String param, RequestFilter filter) {
      String value = "";
      try {
        // if a parent callout modified any value, updated value is returned.
        if (result.has(param)) {
          value = result.getJSONObject(param).get(CalloutConstants.CLASSIC_VALUE).toString();
        } else {
          value = vars.getStringParameter(param, filter);
        }
      } catch (JSONException e) {
        log.error("Error parsing JSON Object.", e);
      }
      return value;
    }

    public String getStringParameter(String param) {
      return getStringParameter(param, null);
    }

    /**
     * 
     * @param param
     *          The name of the field to get the value.
     * @return The value of a field named param as a {@code BigDecimal}.
     * @throws ServletException
     */
    public BigDecimal getBigDecimalParameter(String param) throws ServletException {
      return new BigDecimal(vars.getNumericParameter(param, "0"));
    }

    /**
     * Starts the inclusion of values to the combo field with the name passed as parameter.
     * 
     * @param param
     *          The name of the combo field to set the values.
     */
    public void addSelect(String param) {
      if (result.has(param)) {
        try {
          currentComboResult = getComboMap(result.getJSONObject(param));
        } catch (JSONException e) {
          log.error("Error retrieving combo entries for field " + param, e);
        }
      } else {
        currentComboResult = new LinkedHashMap<String, String>();
      }

      if (currentComboResult == null) {
        throw new OBException("Could not retrieve entries for combo field with name " + param);
      }

      currentElement = param;
    }

    private Map<String, String> getComboMap(JSONObject comboField) throws JSONException {
      if (!comboField.has(CalloutConstants.ENTRIES)) {
        return null;
      }
      JSONArray entries = comboField.getJSONArray(CalloutConstants.ENTRIES);
      Map<String, String> comboMap = new LinkedHashMap<String, String>();
      for (int i = 0; i < entries.length(); i++) {
        JSONObject item = entries.getJSONObject(i);
        if (item.has(JsonConstants.ID) && item.has(JsonConstants.IDENTIFIER)) {
          comboMap.put(item.getString(JsonConstants.ID), item.getString(JsonConstants.IDENTIFIER));
        }
      }
      return comboMap;
    }

    /**
     * Adds an entry to the select field and marks it as unselected.
     * 
     * @param name
     *          The entry name to add.
     * @param value
     *          The entry value to add.
     */
    public void addSelectResult(String name, String value) {
      addSelectResult(name, value, false);
    }

    /**
     * Removes an entry of the combo field.
     * 
     * @param id
     *          The id of the combo entry to be removed.
     */
    public void removeSelectResult(String id) {
      currentComboResult.remove(id);
    }

    /**
     * Adds an entry value to the combo field.
     * 
     * @param id
     *          The id of the combo entry to add.
     * @param identifier
     *          The identifier of the combo entry to add
     * @param selected
     *          Whether this entry field is selected or not.
     */
    public void addSelectResult(String id, String identifier, boolean selected) {
      try {
        currentComboResult.put(id, identifier);
        if (selected) {
          // If value of combo is selected
          JSONObject valueSelected = new JSONObject();
          valueSelected.put(CalloutConstants.VALUE, id);
          valueSelected.put(CalloutConstants.CLASSIC_VALUE, id);
          result.put(currentElement, valueSelected);
        }
      } catch (JSONException e) {
        log.error("Error adding combo entry with id " + id + " and identifier " + identifier
            + " for combo field " + currentElement, e);
      }
    }

    /**
     * Finish the inclusion of values to the combo field.
     */
    public void endSelect() {
      try {
        if (isComboWithoutSelectedEntry()) {
          JSONObject notSelectedItem = new JSONObject();
          notSelectedItem.put(CalloutConstants.ENTRIES, getComboEntries());
          result.put(currentElement, notSelectedItem);
        } else {
          result.getJSONObject(currentElement).put(CalloutConstants.ENTRIES, getComboEntries());
        }
      } catch (JSONException e) {
        log.error("Error parsing JSON Object.", e);
      }
    }

    private boolean isComboWithoutSelectedEntry() throws JSONException {
      return (result.isNull(currentElement) ? true
          : !result.getJSONObject(currentElement).has(CalloutConstants.VALUE));
    }

    private JSONArray getComboEntries() throws JSONException {
      JSONArray entries = new JSONArray();

      if (currentComboResult.isEmpty()) {
        entries.put(new JSONObject());
      } else {
        for (Entry<String, String> item : currentComboResult.entrySet()) {
          JSONObject entry = new JSONObject();
          entry.put(JsonConstants.ID, item.getKey());
          entry.put(JsonConstants.IDENTIFIER, item.getValue());
          entries.put(entry);
        }
      }
      return entries;
    }

    /**
     * Sets the value of a field named param with the value indicated.
     * 
     * @param param
     *          The name of the field to get the value.
     * @param value
     *          The value to assign to the field.
     */
    public void addResult(String param, Object value) {
      JSONObject columnValue = new JSONObject();

      Object resultValue = value;
      if (resultValue != null) {
        // handle case when SimpleCallouts are sending us "\"\"" string.
        if ("\"\"".equals(resultValue)) {
          resultValue = "";
        }
        // handle case when SimpleCallouts are sending us "null" string. Force to be null object in
        // order to ensure backwards compatibility.
        resultValue = JsonConstants.NULL.equals(resultValue) ? null : resultValue;
      }

      try {
        columnValue.put(CalloutConstants.VALUE, resultValue);
        columnValue.put(CalloutConstants.CLASSIC_VALUE, resultValue);
        result.put(param, columnValue);
      } catch (JSONException e) {
        log.error("Error parsing JSON Object.", e);
      }
    }

    /**
     * Sets the value of a field named param with the value indicated. This method is useful to set
     * numbers like {@code BigDecimal} objects.
     * 
     * @param param
     *          The name of the field to get the value.
     * @param value
     *          The value to assign to the field.
     */
    public void addResult(String param, String value) {
      addResult(param, (Object) (value == null ? null : value));
    }

    /**
     * Adds a default document number to the result.
     */
    void addDocumentNo() {
      String strTableNameId = getStringParameter("inpkeyColumnId",
          new RegexFilter("[a-zA-Z0-9_]*_ID"));
      String strDocType_Id = getStringParameter("inpcDoctypeId", IsIDFilter.instance);
      String strTableName = strTableNameId.substring(0, strTableNameId.length() - 3);
      String strDocumentNo = Utility.getDocumentNo(new DalConnectionProvider(false), vars,
          getWindowId(), strTableName, strDocType_Id, strDocType_Id, false, false);
      addResult("inpdocumentno", "<" + strDocumentNo + ">");
    }

    /**
     * Shows a message in the browser with the value indicated.
     * 
     * @param value
     *          The message to display in the browser.
     */
    protected void showMessage(String value) {
      addResult("MESSAGE", value);
    }

    /**
     * Shows an error message in the browser with the value indicated.
     * 
     * @param value
     *          The error message to display in the browser.
     */
    protected void showError(String value) {
      addResult("ERROR", value);
    }

    /**
     * Shows a warning message in the browser with the value indicated.
     * 
     * @param value
     *          The warning message to display in the browser.
     */
    protected void showWarning(String value) {
      addResult("WARNING", value);
    }

    /**
     * Shows an information message in the browser with the value indicated.
     * 
     * @param value
     *          The information message to display in the browser.
     */
    protected void showInformation(String value) {
      addResult("INFO", value);
    }

    /**
     * Shows a success message in the browser with the value indicated.
     * 
     * @param value
     *          The success message to display in the browser.
     */
    protected void showSuccess(String value) {
      addResult("SUCCESS", value);
    }

    /**
     * Executes the javascript code indicated in the value in the browser.
     * 
     * @param value
     *          The javascript code to execute in the browser.
     */
    protected void executeCodeInBrowser(String value) {
      addResult("JSEXECUTE", value);
    }

    /**
     * Returns the value of the result variable as a String
     */
    public String getResult() {
      return result.toString();
    }

    /**
     * Returns the value of the result variable
     */
    public JSONObject getJSONObjectResult() {
      return result;
    }

  }
}
