package org.openbravo.common.hooks;

import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class PrintControllerHookManager {
  private static final String MESSAGE = "message";
  private static final String SEVERITY = "severity";
  private static final String ERROR = "error";
  private static final String PREPROCESS = "preProcess";
  private static final String POSTPROCESS = "postProcess";

  @Inject
  @Any
  private Instance<PrintControllerHook> hooks;

  /**
   * Executes the hooks based on the specified method name (preProcess or postProcess).
   * If any hook returns an error message, the execution stops and the error message is returned.
   *
   * @param jsonParams the JSON parameters to be processed by the hooks
   * @param methodName the name of the method to be executed (preProcess or postProcess)
   * @return the result of the hook execution, which contains an error message if any hook fails, or null if all hooks succeed
   * @throws JSONException if there is an error processing the JSON parameters
   */
  public JSONObject executeHooks(JSONObject jsonParams, String methodName) throws JSONException {
    List<PrintControllerHook> hookList = PrintControllerOrderHook.sortHooksByPriority(hooks);
    for (PrintControllerHook hook : hookList) {
      JSONObject resultHook = null;
      if (StringUtils.equals(methodName, PREPROCESS)) {
        resultHook = hook.preProcess(jsonParams);
      } else if (StringUtils.equals(methodName, POSTPROCESS)) {
        resultHook = hook.postProcess(jsonParams);
      }
      JSONObject message = (resultHook != null && resultHook.has(MESSAGE)) ? resultHook.getJSONObject(MESSAGE) : null;
      if (message != null && message.has(SEVERITY)
          && StringUtils.equalsIgnoreCase(ERROR, message.getString(SEVERITY))) {
        return resultHook;
      }
    }
    return null;
  }

  /**
   * Gets the name of the preProcess method.
   *
   * @return the name of the preProcess method
   */
  public String getPreProcess(){
    return PREPROCESS;
  }

  /**
   * Gets the name of the postProcess method.
   *
   * @return the name of the postProcess method
   */
  public String getPostProcess(){
    return POSTPROCESS;
  }
}
