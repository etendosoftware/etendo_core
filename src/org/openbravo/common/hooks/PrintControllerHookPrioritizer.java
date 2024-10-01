package org.openbravo.common.hooks;

/**
 * Abstract class that defines a prioritization mechanism for print controller hooks.
 * Classes extending {@link PrintControllerHookPrioritizer} must implement the
 * {@link #getPriority()} method to specify the priority level of the hook.
 *
 * <p>
 * The priority is used by the {@link PrintControllerHookManager} to determine the order
 * in which hooks are executed. Hooks with lower priority values are executed before
 * those with higher values, allowing for fine-grained control over the execution flow
 * of the print process.
 * </p>
 *
 * <p>
 * This class serves as a base for any hook that requires prioritization in the
 * print workflow.
 * </p>
 *
 * @see PrintControllerHook
 * @see PrintControllerHookManager
 */
public abstract class PrintControllerHookPrioritizer {

  /**
   * Returns the priority of the hook.
   *
   * @return the priority of the hook
   */
  public abstract int getPriority();
}
