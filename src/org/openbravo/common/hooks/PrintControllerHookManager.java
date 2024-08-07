package org.openbravo.common.hooks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;

import com.google.common.collect.HashBasedTable;

public class PrintControllerHookManager {
  public static final String RESULTS = "results";
  public static final String FAILURES = "failures";
  public static final String PREPROCESS = "preProcess";
  public static final String POSTPROCESS = "postProcess";
  public static final String MESSAGE = "message";
  public static final String CANCELLATION = "cancellation";

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

      return Integer.compare(o1Priority, o2Priority);
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
    JSONObject resultsObj = jsonParams.optJSONObject(RESULTS);
    if (resultsObj == null) {
      resultsObj = new JSONObject();
      jsonParams.put(RESULTS, resultsObj);
    }

    if (jsonParams.optBoolean(CANCELLATION, false)) {
      throw new OBException(e.getMessage());
    }

    resultsObj.put(FAILURES, true);
    HashBasedTable<String, Boolean, String> messageInfo;
    if (resultsObj.has(MESSAGE)) {
      messageInfo = (HashBasedTable<String, Boolean, String>) resultsObj.get(MESSAGE);
    } else {
      messageInfo = HashBasedTable.create();
    }
    messageInfo.put(hook.getClass().getSimpleName(), isPreProcess, e.getMessage());
    resultsObj.put(MESSAGE, messageInfo);
    jsonParams.put(RESULTS, resultsObj);
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
      } else {
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
