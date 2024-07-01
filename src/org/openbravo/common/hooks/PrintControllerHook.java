package org.openbravo.common.hooks;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public interface PrintControllerHook {

  /**
   * Method to be executed before the main process.
   *
   * @param params
   *     the JSON parameters to be processed
   * @throws JSONException
   *     if there is an error processing the JSON parameters
   */
  void preProcess(JSONObject params) throws JSONException;

  /**
   * Method to be executed after the main process.
   *
   * @param params
   *     the JSON parameters to be processed
   * @throws JSONException
   *     if there is an error processing the JSON parameters
   */
  void postProcess(JSONObject params) throws JSONException;
}
