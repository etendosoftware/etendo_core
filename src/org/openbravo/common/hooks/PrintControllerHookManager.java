package org.openbravo.common.hooks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.google.common.collect.HashBasedTable;

public class PrintControllerHookManager {
  public static final String RESULTS = "results";
  public static final String FAILURES = "failures";
  public static final String PREPROCESS = "preProcess";
  public static final String POSTPROCESS = "postProcess";
  public static final String MESSAGE = "message";

  @Inject
  @Any
  private Instance<PrintControllerHook> hooks;

  /**
   * Sorts a list of hooks by their priority.
   *
   * @param hooks
   *     the hooks to be sorted
   * @return a list of hooks sorted by priority
   */
  public static List<PrintControllerHook> sortHooksByPriority(Instance<PrintControllerHook> hooks) {
    List<PrintControllerHook> hookList = new ArrayList<>();
    hooks.forEach(hookList::add);

    hookList.sort((Comparator<Object>) (o1, o2) -> {
      int o1Priority = (o1 instanceof PrintControllerHookPrioritizer) ? ((PrintControllerHookPrioritizer) o1).getPriority() : 100;
      int o2Priority = (o2 instanceof PrintControllerHookPrioritizer) ? ((PrintControllerHookPrioritizer) o2).getPriority() : 100;

      return (int) Math.signum((float) o1Priority - (float) o2Priority);
    });

    return hookList;
  }

  /**
   * Handles errors encountered during hook execution, recording the error message in the JSON parameters.
   *
   * @param jsonParams
   *     the JSON parameters being processed
   * @param isPreProcess
   *     whether the error occurred during preProcess
   * @param e
   *     the exception that was thrown
   * @param hook
   *     the hook that encountered the error
   * @throws JSONException
   *     if there is an error processing the JSON parameters
   */
  private static void handleHookError(JSONObject jsonParams, boolean isPreProcess, Exception e,
      PrintControllerHook hook) throws JSONException {
    jsonParams.getJSONObject(RESULTS).put(FAILURES, true);
    HashBasedTable<String, Boolean, String> messageInfo = (HashBasedTable<String, Boolean, String>) jsonParams.getJSONObject(
        RESULTS).get(MESSAGE);
    messageInfo.put(hook.getClass().getSimpleName(), isPreProcess, e.getMessage());
    jsonParams.getJSONObject(RESULTS).put(MESSAGE, messageInfo);
  }

  /**
   * Executes the hooks based on the specified method name (preProcess or postProcess).
   * If any hook returns an error message, the execution stops and the error message is returned.
   *
   * @param jsonParams
   *     the JSON parameters to be processed by the hooks
   * @param methodName
   *     the name of the method to be executed (preProcess or postProcess)
   * @throws JSONException
   *     if there is an error processing the JSON parameters
   */
  public void executeHooks(JSONObject jsonParams, String methodName) throws JSONException {
    List<PrintControllerHook> hookList = sortHooksByPriority(hooks);
    for (PrintControllerHook hook : hookList) {
      if (StringUtils.equals(methodName, PREPROCESS)) {
        try {
          hook.preProcess(jsonParams);
        } catch (Exception e) {
          handleHookError(jsonParams, true, e, hook);
        }
      } else if (StringUtils.equals(methodName, POSTPROCESS)) {
        try {
          hook.postProcess(jsonParams);
        } catch (Exception e) {
          handleHookError(jsonParams, false, e, hook);
        }
      }
    }
  }

  /**
   * Gets the name of the preProcess method.
   *
   * @return the name of the preProcess method
   */
  public String getPreProcess() {
    return PREPROCESS;
  }

  /**
   * Gets the name of the postProcess method.
   *
   * @return the name of the postProcess method
   */
  public String getPostProcess() {
    return POSTPROCESS;
  }
}
