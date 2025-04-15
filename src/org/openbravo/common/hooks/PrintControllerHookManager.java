package org.openbravo.common.hooks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.google.common.collect.HashBasedTable;

/**
 * Manages the execution of print controller hooks. The {@link PrintControllerHookManager}
 * class is responsible for sorting and executing hooks that implement the {@link PrintControllerHook}
 * interface, allowing for pre-processing and post-processing logic to be applied during
 * the print workflow.
 *
 * <p>
 * This class utilizes dependency injection to obtain instances of hooks and provides methods
 * to execute them in a prioritized order. It also handles errors encountered during hook
 * execution, allowing for graceful error reporting and management.
 * </p>
 *
 * <p>
 * The following constants are defined for use within the class:
 * </p>
 * <ul>
 *   <li>{@link #RESULTS} - Key for storing results in JSON parameters.</li>
 *   <li>{@link #FAILURES} - Key for indicating failures in JSON parameters.</li>
 *   <li>{@link #PREPROCESS} - String representing the preProcess method name.</li>
 *   <li>{@link #POSTPROCESS} - String representing the postProcess method name.</li>
 *   <li>{@link #MESSAGE} - Key for storing error messages in JSON parameters.</li>
 *   <li>{@link #CANCELLATION} - Key for indicating cancellation in JSON parameters.</li>
 * </ul>
 *
 * <p>
 * The class provides the following methods:
 * </p>
 * <ul>
 *   <li>{@link #sortHooksByPriority(Instance)} - Sorts a list of hooks by their priority.</li>
 *   <li>{@link #executeHooks(JSONObject, String)} - Executes the hooks based on the specified method name.</li>
 *   <li>{@link #getPreProcess()} - Returns the name of the preProcess method.</li>
 *   <li>{@link #getPostProcess()} - Returns the name of the postProcess method.</li>
 * </ul>
 *
 * @see PrintControllerHook
 * @see JSONObject
 * @see JSONException
 */
public class PrintControllerHookManager {
  public static final String RESULTS = "results";
  public static final String FAILURES = "failures";
  public static final String PREPROCESS = "preProcess";
  public static final String POSTPROCESS = "postProcess";
  public static final String MESSAGE = "message";
  public static final String CANCELLATION = "cancellation";

  private static final Logger log4j = LogManager.getLogger(PrintControllerHookManager.class);

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
    log4j.info("sorting " + hookList.size() + " hooks...");
    hookList.sort((Comparator<Object>) (o1, o2) -> {
      int o1Priority = (o1 instanceof PrintControllerHookPrioritizer) ? ((PrintControllerHookPrioritizer) o1).getPriority() : 100;
      int o2Priority = (o2 instanceof PrintControllerHookPrioritizer) ? ((PrintControllerHookPrioritizer) o2).getPriority() : 100;

      return Integer.compare(o1Priority, o2Priority);
    });
    log4j.info("Hooks sorted");
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
      PrintControllerHook hook) throws PrintControllerHookException {
    try {
      JSONObject resultsObj = jsonParams.optJSONObject(RESULTS);
      if (resultsObj == null) {
        resultsObj = new JSONObject();
        jsonParams.put(RESULTS, resultsObj);
      }

      if (jsonParams.optBoolean(CANCELLATION, false)) {
        log4j.error("Error is too severe. Cancelling printing...");
        throw new PrintControllerHookException(e.getMessage());
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
      log4j.info("Hook error handling completed");
    } catch (JSONException jsonE) {
      log4j.error("Unexpected error while handling hook errors...");
      throw new PrintControllerHookException(e.getMessage());
    }
  }

  /**
   * Executes the hooks based on the specified method name (preProcess or postProcess).
   * If any hook returns an error message, the execution stops and the error message is returned.
   *
   * @param jsonParams
   *     the JSON parameters to be processed by the hooks
   * @param methodName
   *     the name of the method to be executed (preProcess or postProcess)
   * @throws PrintControllerHookException
   *     if there is an error at any moment executing the hooks
   */
  public void executeHooks(JSONObject jsonParams, String methodName) throws PrintControllerHookException {
    log4j.info("Starting PrintController hooks " + methodName + " execution...");
    List<PrintControllerHook> hookList = sortHooksByPriority(hooks);
    for (PrintControllerHook hook : hookList) {
      if (StringUtils.equals(methodName, PREPROCESS)) {
        try {
          log4j.info("Executing preProcess for " + hook.getClass().getSimpleName() + " hook...");
          hook.preProcess(jsonParams);
        } catch (Exception e) {
          log4j.error(
              hook.getClass().getSimpleName() + " hook preProcess finished with errors. Handling hook errors...");
          handleHookError(jsonParams, true, e, hook);
        }
      } else {
        try {
          log4j.info("Executing postProcess for " + hook.getClass().getSimpleName() + " hook...");
          hook.postProcess(jsonParams);
          try {
            log4j.info("Refreshing report input stream for next hook postProcess...");
            ByteArrayOutputStream reportOutputStream = (ByteArrayOutputStream) jsonParams.get("reportOutputStream");
            ByteArrayInputStream reportInputStream = new ByteArrayInputStream(reportOutputStream.toByteArray());
            jsonParams.put("reportInputStream", reportInputStream);
          } catch (JSONException e) {
            log4j.error("There was a problem refreshing the report input stream. Handling error...");
            handleHookError(jsonParams, false, e, hook);
          }
        } catch (Exception e) {
          log4j.error(
              hook.getClass().getSimpleName() + " hook postProcess finished with errors. Handling hook errors...");
          handleHookError(jsonParams, false, e, hook);
        }
      }
    }
    log4j.info("Finished hooks executions");
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

  /**
   * Exception class to handle errors specific to the execution of print controller hooks.
   * This exception is thrown when an error occurs during the execution of hooks in the
   * {@link PrintControllerHookManager} class, particularly during the pre-processing or
   * post-processing phases.
   *
   * <p>
   * The {@link PrintControllerHookException} class extends the standard {@link Exception}
   * class, allowing it to be thrown and caught like any other exception in Java. It provides
   * a constructor that accepts a custom error message, which can be used to convey specific
   * information about the error that occurred.
   * </p>
   *
   * <p>
   * This exception is typically used to signal issues such as:
   * </p>
   * <ul>
   *   <li>Errors encountered during the execution of a hook's methods.</li>
   *   <li>Invalid or unexpected results produced by hooks.</li>
   *   <li>General failures in the hook execution workflow.</li>
   * </ul>
   *
   * @see PrintControllerHookManager
   * @see PrintControllerHook
   */
  public static class PrintControllerHookException extends Exception {
    public PrintControllerHookException(String errorMessage) {
      super(errorMessage);
    }
  }
}
