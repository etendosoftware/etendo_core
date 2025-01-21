package com.smf.jobs;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.kernel.ComponentProvider;

import com.smf.jobs.interfaces.PostActionHook;
import com.smf.jobs.interfaces.PreActionHook;

/**
 * Executes an Action inside a Job or from the smartclient UI as a Process Definition.
 *
 * @author angelo
 */
public abstract class Action extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();
  public static final String PARAMS = "_params";

  Data input;
  JSONObject parameters;
  Map<String, Object> requestParameters;

  @Inject
  @Any
  private Instance<PreActionHook> preHooks;

  @Inject
  @Any
  private Instance<PostActionHook> postHooks;


  /**
   * Allows to execute a process from the UI
   *
   * @param parameters
   *     Process parameters defined in AD
   * @param content
   *     JSON with window and parameter data from the UI
   * @return JSONObject with response messages and actions
   */
  @SuppressWarnings("unchecked")
  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    try {
      var jsonContent = new JSONObject(content);

      requestParameters = parameters;

      input = new Data(jsonContent, (Class<? extends BaseOBObject>) getInputClass());

      input = preRun(jsonContent);

      var allParams = jsonContent.has(PARAMS) ? jsonContent.getJSONObject(PARAMS) : new JSONObject();
      if (jsonContent.has("_buttonValue")) {
        allParams.put("_buttonValue", jsonContent.get("_buttonValue"));
      }
      JSONObject params = jsonContent.optJSONObject(PARAMS);
      runPreActionHooks(jsonContent);
      var result = action(params, new MutableBoolean(false));

      result = postRun(result);
      runPostActionHooks(jsonContent, result);
      ActionResult finalResult = result;

      return result.getResponseActionsBuilder().map(b -> finalResult.toJSON(b, false)).orElse(
          finalResult.toJSON(getResponseBuilder(), true));
    } catch (JSONException e) {
      throw new OBException("Cannot load json from Process Definition call", e);
    }
  }

  /**
   * Override to add code before Action execution
   *
   * @param jsonContent
   *     json content will not be null when coming from the UI (Process Definition)
   * @return input as a {@link Data } (modified or not)
   */
  protected Data preRun(JSONObject jsonContent) {
    log.debug(jsonContent);

    return input;
  }

  /**
   * Override to add code after Action execution
   *
   * @param result
   *     the ActionResult of an execution
   * @return the result argument (modified or not)
   */
  protected ActionResult postRun(ActionResult result) {
    return result;
  }

  /**
   * Runs the entire action flow. Used by Runner class.
   * Do not override this, instead override {@link Action#action(JSONObject, MutableBoolean)} and {@link Action#preRun(JSONObject)} or {@link Action#postRun(ActionResult)} if necessary
   *
   * @param input
   *     The input data that will come from a Filter
   * @param stopped
   *     If the action was stopped due to a kill signal on the Job that runs it.
   * @return an ActionResult which will contain the message to the user, and optionally Data to pass to another Action.
   */
  public ActionResult run(Data input, MutableBoolean stopped) {
    this.input = input;

    preRun(null);

    runPreActionHooks(parameters);
    var result = action(parameters, stopped);
    runPostActionHooks(parameters, result);

    return postRun(result);
  }

  /**
   * Runs all pre-action hooks.
   * <p>
   * This method selects all pre-action hooks for the current class, sorts them by priority,
   * filters them based on whether they apply to the given parameters, and runs each applicable hook.
   *
   * @param parameters
   *     The parameters to be passed to the pre-action hooks.
   */
  private void runPreActionHooks(JSONObject parameters) {
    if (preHooks == null) {
      return;
    }
    preHooks.select(new ComponentProvider.Selector(this.getClass().getName())).stream().sorted(
        Comparator.comparingInt(PreActionHook::getPriority)).filter(
        hook -> hook.applies(parameters)).forEach(hook -> hook.run(parameters));
  }

  /**
   * Runs all post-action hooks.
   * <p>
   * This method selects all post-action hooks for the current class, sorts them by priority,
   * filters them based on whether they apply to the given parameters and result, and runs each applicable hook.
   *
   * @param parameters
   *     The parameters to be passed to the post-action hooks.
   * @param result
   *     The result to be passed to the post-action hooks.
   */
  private void runPostActionHooks(JSONObject parameters, ActionResult result) {
    if (postHooks == null) {
      return;
    }
    postHooks.select(new ComponentProvider.Selector(this.getClass().getName())).stream().sorted(
        Comparator.comparingInt(PostActionHook::getPriority)).filter(hook -> hook.applies(parameters, result)).forEach(
        hook -> hook.run(parameters, result));
  }

  /**
   * Sets the action parameters.
   *
   * @param parameters
   *     JSONObject with parameter-value map.
   */
  protected void setParameters(JSONObject parameters) {
    this.parameters = parameters;
  }

  /**
   * Obtain the input contents casted to a specific class that extends BaseOBObject
   *
   * @param entityClass
   *     the class representing an entity (example: Invoice.class)
   * @param <T>
   *     the type of the entityClass parameter. Must extend BaseOBObject
   * @return a List with objects of the type sent as a parameter
   */
  protected <T extends BaseOBObject> List<T> getInputContents(Class<T> entityClass) {
    return input.getContents(entityClass);
  }

  /**
   * Get the input data contents. See also {@link #getInputContents(Class)}
   *
   * @return a list of BaseOBObjects (which can be casted to their respective subclass).
   */
  protected List<BaseOBObject> getInputContents() {
    return input.getContents();
  }

  /**
   * Returns the input as a {@link Data} object.
   * Useful when sending the input (unmodified) to another action via the {@link ActionResult#setOutput(Data)} method
   * To obtain its contents, its better to use {@link Action#getInputContents(Class)} or {@link Action#getInputContents()}
   *
   * @return the input as a {@link Data} object
   */
  protected Data getInput() {
    return input;
  }

  /**
   * Sets the action input.
   *
   * @param input
   *     {@link Data} with new contents.
   */
  protected void setInput(Data input) {
    this.input = input;
  }

  /**
   * Returns the requestParameters as a {@link Map<String, Object>} object.
   *
   * @return the action requestParameters attribute as a {@link Map<String, Object>} object
   */
  protected Map<String, Object> getRequestParameters() {
    return requestParameters;
  }

  /**
   * Sets the action requestParameters.
   *
   * @param requestParameters
   *     Process parameters defined in AD
   */
  protected void setRequestParameters(Map<String, Object> requestParameters) {
    this.requestParameters = requestParameters;
  }


  /**
   * The main method of the action.
   * Override in a subclass to make your process support execution from the UI, Background and as Job
   *
   * @param parameters
   *     Parameters (if any) defined in dictionary
   * @param isStopped
   *     true when the Job that runs the action was signaled to be stopped.
   *     Use when performing long or intensive task inside the action, and stop when the value switches to true.
   * @return an ActionResult which will contain the message to the user, and optionally Data to pass to another Action.
   */
  protected abstract ActionResult action(JSONObject parameters, MutableBoolean isStopped);

  /**
   * Override this and return a Class<T> in case your action supports only one entity (T will be your entity type).
   * In case your action supports multiple entities, return a BaseOBObject.class.
   * Use with {@link #getInputContents(Class)} to obtain a typed list with the class of your choice.
   *
   * @return a Class of the type your action supports (example: Class<\Invoice> Invoice.class)
   */
  protected abstract Class<?> getInputClass();

  /**
   * Override this and return a Class<T> in case your action has output in its Result.  (T will be your entity type).
   * This is purely informative, but is required to be able to chain your action o others in the UI.
   *
   * @return a Class of the type of your output list (example: Invoice.class)
   */
  protected Class<?> getOutputClass() {
    return null;
  }
}
