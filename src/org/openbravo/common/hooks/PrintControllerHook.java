package org.openbravo.common.hooks;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Interface representing a hook for the print controller process. Implementations of this
 * interface can define custom behavior to be executed before and after the main printing
 * process by providing implementations for the {@link #preProcess(JSONObject)} and
 * {@link #postProcess(JSONObject)} methods.
 *
 * <p>
 * The {@link PrintControllerHook} interface is utilized by the {@link PrintControllerHookManager},
 * which manages the execution of hooks in a prioritized manner. Hooks can implement the
 * {@link PrintControllerHookPrioritizer} interface to specify their execution priority,
 * allowing for flexible control over the order in which hooks are invoked.
 * </p>
 *
 * <p>
 * The parameters for the pre-processing and post-processing methods are provided as a
 * {@link JSONObject}, which contains the necessary data to be processed. Implementations
 * are expected to handle any exceptions that may arise during the processing of these
 * parameters, specifically {@link JSONException}.
 * </p>
 *
 * <p>
 * This interface is essential for extending the functionality of the print workflow,
 * enabling developers to insert custom logic at critical points in the process.
 * </p>
 *
 * @see JSONObject
 * @see JSONException
 * @see PrintControllerHookManager
 * @see PrintControllerHookPrioritizer
 */
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
