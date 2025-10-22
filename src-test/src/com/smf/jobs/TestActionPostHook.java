package com.smf.jobs;

import jakarta.enterprise.context.ApplicationScoped;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.ComponentProvider;

import com.smf.jobs.interfaces.PostActionHook;


/**
 * {@link PostActionHook} that sets a property in a singleton instance, after the action: {@link TestAction} is executed.
 *
 * <p>
 * This class is a post-action hook that implements the {@link PostActionHook} interface.
 * It is responsible for setting a metadata property in the {@link SingletonToTestHooks} singleton instance
 * after the {@link TestAction} has been executed. This allows for testing and verification of post-action logic.
 * </p>
 *
 * <p>
 * The hook is triggered after the {@link TestAction} and sets the "propAddedByPostHook" property to true in the
 * {@link SingletonToTestHooks} instance.
 * </p>
 */
@ApplicationScoped
@ComponentProvider.Qualifier("com.smf.jobs.TestAction")
public class TestActionPostHook implements PostActionHook {

  /**
   * Returns the priority of this post-action hook.
   * <p>
   * This method returns the priority value which determines the order in which the hook is executed.
   *
   * @return The priority value of this post-action hook.
   */
  @Override
  public int getPriority() {
    return 10;
  }

  /**
   * Checks if this post-action hook applies to the given parameters.
   * <p>
   * This method determines if the post-action hook should be applied based on the provided action and result.
   *
   * @param action
   *     The JSON object containing the action data.
   * @param result
   *     The ActionResult object containing the result data.
   * @return true if the post-action hook applies, false otherwise.
   */
  @Override
  public boolean applies(JSONObject action, ActionResult result) {
    return true;
  }

  /**
   * Executes the post-action hook with the given action and result.
   * <p>
   * This method runs the post-action hook logic using the provided action and result JSON objects.
   *
   * @param actionParam
   *     The JSON object containing the action data.
   * @param result
   *     The ActionResult object containing the result data.
   */
  @Override
  public void run(JSONObject actionParam, ActionResult result) {
    SingletonToTestHooks.getInstance().setMetadata("propAddedByPostHook", true);
  }
}
