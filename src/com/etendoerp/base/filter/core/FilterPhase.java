package com.etendoerp.base.filter.core;

/**
 * Represents stages in the unified thread handler lifecycle.
 *
 * <p>Maps to filter operations: BEFORE→setup, ACTION→execute, CLEANUP→cleanup.
 * Phases execute in order: BEFORE → ACTION → CLEANUP (cleanup runs in reverse).</p>
 *
 * @since Etendo 24.Q4
 */
public enum FilterPhase {

  /**
   * Pre-execution setup phase.
   * <p>Initialize context, begin transaction, setup thread-local state.</p>
   */
  BEFORE,

  /**
   * Main filter execution phase.
   * <p>Execute filter business logic, process request.</p>
   */
  ACTION,

  /**
   * Post-execution cleanup phase.
   * <p>Commit/rollback transaction, release resources, clear thread-local state.
   * Executes in reverse order of BEFORE phase.</p>
   */
  CLEANUP
}
