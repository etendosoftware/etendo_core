package com.smf.jobs.interfaces;

import org.codehaus.jettison.json.JSONObject;

import com.smf.jobs.ActionResult;

/**
 * Interface for post-action hooks.
 * <p>
 * This interface defines the methods that must be implemented by any class that
 * wants to provide a post-action hook. Post-action hooks are executed after a
 * specific action has been performed.
 * Its important to note that the implementation of this interface must be annotated with @ApplicationScoped
 * and @Qualifier("the.action.java.package") to be recognized by the system. i.e. @Qualifier("com.smf.jobs.defaults.CloneRecords")
 */
public interface PostActionHook {

  /**
   * Returns the priority of the post-action hook.
   * <p>
   * The priority determines the order in which the hooks are executed. Hooks with
   * lower priority values are executed before those with higher values.
   *
   * @return The priority of the post-action hook. Default is 100.
   */
  default int getPriority() {
    return 100;
  }

  /**
   * Determines if the post-action hook applies to the given action and result.
   * <p>
   * This method checks whether the hook should be executed based on the provided
   * action and result.
   *
   * @param params
   *     The action that was performed.
   * @param result
   *     The result of the action.
   * @return True if the hook applies, false otherwise.
   */
  boolean applies(JSONObject params, ActionResult result);

  /**
   * Executes the post-action hook.
   * <p>
   * This method contains the logic that should be executed after the action has
   * been performed.
   *
   * @param params
   *     The action that was performed.
   * @param result
   *     The result of the action.
   */
  void run(JSONObject params, ActionResult result);
}