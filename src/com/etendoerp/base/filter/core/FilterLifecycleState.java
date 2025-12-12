package com.etendoerp.base.filter.core;

/**
 * Lifecycle states for filter chain execution.
 *
 * <p>Represents the current stage of request processing through the filter chain.
 * State transitions follow a strict order: INITIALIZED → EXECUTING → CLEANING_UP → COMPLETED</p>
 *
 * @since Etendo 24.Q4
 */
public enum FilterLifecycleState {

  /**
   * Context created, filters not yet executed.
   * <p>Initial state when FilterContext is instantiated.</p>
   */
  INITIALIZED,

  /**
   * Filters currently executing.
   * <p>Active state during filter chain traversal and servlet processing.</p>
   */
  EXECUTING,

  /**
   * Cleanup phase in progress.
   * <p>State during reverse-order cleanup of executed filters.</p>
   */
  CLEANING_UP,

  /**
   * Request processing complete.
   * <p>Terminal state after all filters have executed and cleaned up.</p>
   */
  COMPLETED
}
