package com.smf.jobs;

import jakarta.enterprise.context.ApplicationScoped;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.ComponentProvider;

import com.smf.jobs.interfaces.PreActionHook;

/**
 * PreActionHook that sets a property in a singleton instance, before the action: TestAction is
 * executed.
 */
@ApplicationScoped
@ComponentProvider.Qualifier("com.smf.jobs.TestAction")
public class TestActionPreHook implements PreActionHook {

  /**
   * Returns the priority of this pre-action hook.
   * <p>
   * This method returns the priority value which determines the order in which the hook is executed.
   *
   * @return The priority value of this pre-action hook.
   */
  @Override
  public int getPriority() {
    return 121;
  }

  /**
   * Checks if this pre-action hook applies to the given parameters.
   * <p>
   * This method determines if the pre-action hook should be applied based on the provided parameters.
   *
   * @param parameters
   *     The JSON object containing the parameters.
   * @return true if the pre-action hook applies, false otherwise.
   */
  @Override
  public boolean applies(JSONObject parameters) {
    return true;
  }

  /**
   * Executes the pre-action hook with the given action.
   * <p>
   * This method runs the pre-action hook logic using the provided action JSON object.
   *
   * @param action
   *     The JSON object containing the action data.
   */
  @Override
  public void run(JSONObject action) {
    SingletonToTestHooks.getInstance().setMetadata("propAddedByPreHook", true);
  }
}
