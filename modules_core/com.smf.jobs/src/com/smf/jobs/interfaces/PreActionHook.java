package com.smf.jobs.interfaces;

import org.codehaus.jettison.json.JSONObject;

/**
 * Interface for pre-action hooks.
 * <p>
 * This interface defines the methods that must be implemented by any class that
 * wants to provide a pre-action hook. Pre-action hooks are executed before a
 * specific action is performed.
 * Its important to note that the implementation of this interface must be annotated with @ApplicationScoped
 * and @Qualifier("the.action.java.packge") to be recognized by the system. i.e. @Qualifier("com.smf.jobs.defaults.CloneRecords")
 */
public interface PreActionHook {

  /**
   * Returns the priority of the pre-action hook.
   * <p>
   * The priority determines the order in which the hooks are executed. Hooks with
   * lower priority values are executed before those with higher values.
   *
   * @return The priority of the pre-action hook. Default is 100.
   */
  default int getPriority() {
    return 100;
  }

  /**
   * Determines if the pre-action hook applies to the given parameters.
   * <p>
   * This method checks whether the hook should be executed based on the provided
   * parameters.
   *
   * @param parameters
   *     The parameters to be checked.
   * @return True if the hook applies, false otherwise.
   */
  boolean applies(JSONObject parameters);

  /**
   * Executes the pre-action hook.
   * <p>
   * This method contains the logic that should be executed before the action is
   * performed.
   *
   * @param action
   *     The action to be performed.
   */
  void run(JSONObject action);
}