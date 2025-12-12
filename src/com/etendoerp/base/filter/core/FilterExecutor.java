package com.etendoerp.base.filter.core;

/**
 * Contract for all filter implementations in the refactored filter chain.
 * Both core filters and custom module plugins must implement this interface.
 *
 * <p>Filters are executed in priority order (lower values execute first).
 * Core filters use priority 0-100, custom filters use 101+.</p>
 *
 * @since Etendo 24.Q4
 */
public interface FilterExecutor {

  /**
   * Executes the main filter logic.
   *
   * <p>This method must complete within the timeout specified by {@link #getTimeout()}.
   * If execution exceeds the timeout, the FilterChainCoordinator will interrupt
   * the thread and return HTTP 503.</p>
   *
   * <p>Implementations should NOT call cleanup() directly - the coordinator
   * handles cleanup automatically.</p>
   *
   * @param context Execution context containing request, response, and shared state
   * @throws FilterException if filter execution fails (will trigger cleanup)
   */
  void execute(FilterContext context) throws FilterException;

  /**
   * Performs cleanup and resource release after filter execution.
   *
   * <p>This method is ALWAYS called by the coordinator, even if execute() throws
   * an exception. Implementations must be idempotent (safe to call multiple times).</p>
   *
   * <p>Cleanup is called in REVERSE order of execution. If this filter is filter #3
   * in the chain, it will be the 3rd-to-last to clean up.</p>
   *
   * @param context Execution context (same instance passed to execute())
   * @param errorOccurred true if any filter in the chain threw an exception
   */
  void cleanup(FilterContext context, boolean errorOccurred);

  /**
   * Determines if this filter should execute for the given request.
   *
   * <p>Allows conditional execution based on request path, method, headers, etc.
   * The coordinator checks this before calling execute().</p>
   *
   * <p>Default implementation: always execute</p>
   *
   * @param context Execution context containing request information
   * @return true if this filter should execute, false to skip
   */
  default boolean shouldExecute(FilterContext context) {
    return true;
  }

  /**
   * Returns the execution priority of this filter.
   *
   * <p>Filters execute in ascending priority order (lower values first).</p>
   *
   * <ul>
   *   <li>Core filters: 0-100</li>
   *   <li>Custom filters: 101+</li>
   * </ul>
   *
   * <p>Within the same priority, core filters execute before custom filters.
   * Custom filters with the same priority execute in discovery order.</p>
   *
   * @return priority value (must be consistent across calls)
   */
  int getPriority();

  /**
   * Returns the unique name of this filter for logging and debugging.
   *
   * <p>Filter names should be descriptive and unique across all filters
   * in the application. Used in log messages, stack traces, and metrics.</p>
   *
   * <p>Recommended format: "DescriptiveNameFilter" (e.g., "TransactionManagementFilter")</p>
   *
   * @return unique filter name (must not be null or empty)
   */
  String getName();

  /**
   * Returns the maximum execution time allowed for this filter in seconds.
   *
   * <p>If execute() does not complete within this time, the coordinator
   * will interrupt the thread and return HTTP 503 Service Unavailable.</p>
   *
   * <p>Default: 30 seconds (suitable for most filters)</p>
   *
   * @return timeout in seconds (must be > 0 and <= 300)
   */
  default int getTimeout() {
    return 30; // 30 seconds default
  }
}
