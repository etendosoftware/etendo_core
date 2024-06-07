package org.openbravo.common.hooks;

import org.codehaus.jettison.json.JSONObject;

public interface PrintControllerHook {

  /**
   * Method to be executed before the main process.
   *
   * @param params
   *     the JSON parameters to be processed
   * @return a JSON object containing the result of the pre-processing, which may include error messages
   */
  JSONObject preProcess(JSONObject params);

  /**
   * Method to be executed after the main process.
   *
   * @param params
   *     the JSON parameters to be processed
   * @return a JSON object containing the result of the post-processing, which may include error messages
   */
  JSONObject postProcess(JSONObject params);
}
