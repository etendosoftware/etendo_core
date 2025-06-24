package com.smf.jobs;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.structure.BaseOBObject;

/**
 * TestAction class that extends the Action class.
 * <p>
 * This class represents a test action that sets a metadata property and returns a success result.
 */
public class TestAction extends Action {

  /**
   * Executes the action with the given parameters.
   * <p>
   * This method sets a metadata property to indicate that the action was executed and returns a success result.
   *
   * @param parameters
   *     The JSON object containing the action parameters.
   * @param isStopped
   *     A mutable boolean indicating if the action is stopped.
   * @return The result of the action execution.
   */
  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
    SingletonToTestHooks.getInstance().setMetadata("actionExecuted", true);
    var res = new ActionResult();
    res.setType(Result.Type.SUCCESS);
    res.setMessage("Test action executed");

    return res;
  }

  /**
   * Returns the input class for the action.
   * <p>
   * This method returns the class type of the input for the action.
   *
   * @return The input class type.
   */
  @Override
  protected Class<?> getInputClass() {
    return BaseOBObject.class;
  }
}
