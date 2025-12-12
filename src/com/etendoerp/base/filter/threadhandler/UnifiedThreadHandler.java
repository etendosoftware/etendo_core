package com.etendoerp.base.filter.threadhandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.etendoerp.base.filter.core.FilterContext;
import com.etendoerp.base.filter.core.FilterException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified handler for managing thread-local state across filter execution phases.
 *
 * <p>Consolidates multiple nested ThreadHandlers from legacy filters into a single
 * coordinated lifecycle. Handlers execute in three phases:</p>
 *
 * <ul>
 *   <li><strong>BEFORE:</strong> Setup phase (initialize state, begin transaction)</li>
 *   <li><strong>ACTION:</strong> Execution phase (main filter logic)</li>
 *   <li><strong>CLEANUP:</strong> Teardown phase (commit/rollback, release resources)</li>
 * </ul>
 *
 * <p><strong>Execution Order:</strong></p>
 * <ul>
 *   <li>BEFORE/ACTION: Execute handlers in registration order</li>
 *   <li>CLEANUP: Execute handlers in REVERSE order (stack unwinding)</li>
 * </ul>
 *
 * <p><strong>Error Handling:</strong></p>
 * <ul>
 *   <li>If BEFORE handler fails: skip remaining BEFORE handlers, run CLEANUP in reverse</li>
 *   <li>If ACTION handler fails: skip remaining ACTION handlers, run CLEANUP in reverse</li>
 *   <li>CLEANUP always executes for all handlers that completed BEFORE phase</li>
 *   <li>CLEANUP exceptions are logged but don't stop remaining cleanup handlers</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong></p>
 * <p>Thread-safe for handler registration. Each request gets its own execution context
 * stored in ThreadLocal, preventing cross-thread contamination.</p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * UnifiedThreadHandler handler = new UnifiedThreadHandler();
 *
 * // Register handlers
 * handler.registerHandler(new TransactionThreadHandler());
 * handler.registerHandler(new OBContextThreadHandler());
 * handler.registerHandler(new SessionInfoThreadHandler());
 *
 * // Execute lifecycle
 * try {
 *   handler.executeBefore(context);
 *   handler.executeAction(context, () -> {
 *     // Main filter logic
 *     actualFilter.execute(context);
 *   });
 * } finally {
 *   handler.executeCleanup(context, errorOccurred);
 * }
 * }</pre>
 *
 * @since Etendo 24.Q4
 */
public class UnifiedThreadHandler {

  private static final Logger log = LogManager.getLogger(UnifiedThreadHandler.class);

  /**
   * Registered handlers in registration order.
   */
  private final List<ThreadHandler> handlers;

  /**
   * Thread-local tracking of which handlers completed BEFORE phase.
   * Used to determine which handlers need cleanup.
   */
  private final ThreadLocal<Set<ThreadHandler>> completedBeforeHandlers;

  /**
   * Thread-local flag indicating if an error occurred during execution.
   */
  private final ThreadLocal<Boolean> errorFlag;

  /**
   * Creates a new unified thread handler with no registered handlers.
   */
  public UnifiedThreadHandler() {
    this.handlers = Collections.synchronizedList(new ArrayList<>());
    this.completedBeforeHandlers = ThreadLocal.withInitial(ConcurrentHashMap::newKeySet);
    this.errorFlag = ThreadLocal.withInitial(() -> false);
  }

  /**
   * Registers a thread handler.
   *
   * <p>Handlers execute in registration order for BEFORE/ACTION phases,
   * and reverse order for CLEANUP phase.</p>
   *
   * <p>Thread-safe: Can be called during application startup.</p>
   *
   * @param handler handler to register (must not be null)
   * @throws IllegalArgumentException if handler is null
   */
  public void registerHandler(ThreadHandler handler) {
    Objects.requireNonNull(handler, "handler must not be null");
    handlers.add(handler);
    log.debug("Registered thread handler: {}", handler.getName());
  }

  /**
   * Registers multiple thread handlers in order.
   *
   * @param handlersToRegister handlers to register (must not be null or contain nulls)
   */
  public void registerHandlers(ThreadHandler... handlersToRegister) {
    Objects.requireNonNull(handlersToRegister, "handlers must not be null");
    for (ThreadHandler handler : handlersToRegister) {
      registerHandler(handler);
    }
  }

  /**
   * Executes BEFORE phase for all registered handlers.
   *
   * <p>Handlers execute in registration order. If any handler fails,
   * remaining handlers are skipped and cleanup is triggered for handlers
   * that already completed.</p>
   *
   * @param context filter execution context
   * @throws FilterException if any handler fails during BEFORE phase
   */
  public void executeBefore(FilterContext context) throws FilterException {
    log.debug("[{}] Executing BEFORE phase for {} handlers",
        context.getRequestId(), handlers.size());

    Set<ThreadHandler> completed = completedBeforeHandlers.get();
    completed.clear();
    errorFlag.set(false);

    for (ThreadHandler handler : handlers) {
      try {
        log.trace("[{}] BEFORE: {}", context.getRequestId(), handler.getName());
        handler.before(context);
        completed.add(handler);
      } catch (Exception e) {
        errorFlag.set(true);
        log.error("[{}] BEFORE phase failed for handler: {}",
            context.getRequestId(), handler.getName(), e);

        // Cleanup handlers that completed before this failure
        executeCleanupInternal(context, true);

        if (e instanceof FilterException) {
          throw (FilterException) e;
        }
        throw new FilterException(
            "BEFORE phase failed for handler: " + handler.getName() + " - " + e.getMessage(),
            500);
      }
    }

    log.debug("[{}] BEFORE phase completed for all handlers", context.getRequestId());
  }

  /**
   * Executes ACTION phase with provided action.
   *
   * <p>The action (typically filter execution) runs between BEFORE and CLEANUP phases.
   * If action fails, CLEANUP is still triggered for all handlers that completed BEFORE.</p>
   *
   * @param context filter execution context
   * @param action action to execute (must not be null)
   * @throws FilterException if action fails
   */
  public void executeAction(FilterContext context, FilterAction action) throws FilterException {
    Objects.requireNonNull(action, "action must not be null");

    log.debug("[{}] Executing ACTION phase", context.getRequestId());

    try {
      action.execute();
      log.debug("[{}] ACTION phase completed", context.getRequestId());
    } catch (Exception e) {
      errorFlag.set(true);
      log.error("[{}] ACTION phase failed", context.getRequestId(), e);

      if (e instanceof FilterException) {
        throw (FilterException) e;
      }
      throw new FilterException("ACTION phase failed: " + e.getMessage(), 500);
    }
  }

  /**
   * Executes CLEANUP phase for all handlers that completed BEFORE phase.
   *
   * <p>Handlers execute in REVERSE order (stack unwinding). Cleanup always
   * executes, even if errors occurred. Individual handler failures are logged
   * but don't prevent remaining cleanup handlers from executing.</p>
   *
   * @param context filter execution context
   * @param errorOccurred true if error occurred during BEFORE or ACTION phase
   */
  public void executeCleanup(FilterContext context, boolean errorOccurred) {
    if (errorOccurred) {
      errorFlag.set(true);
    }
    executeCleanupInternal(context, errorFlag.get());
  }

  /**
   * Internal cleanup implementation.
   *
   * @param context filter execution context
   * @param errorOccurred true if error occurred
   */
  private void executeCleanupInternal(FilterContext context, boolean errorOccurred) {
    Set<ThreadHandler> completed = completedBeforeHandlers.get();

    log.debug("[{}] Executing CLEANUP phase for {} handlers (error={})",
        context.getRequestId(), completed.size(), errorOccurred);

    // Reverse order cleanup (stack unwinding)
    List<ThreadHandler> reversedHandlers = new ArrayList<>(handlers);
    Collections.reverse(reversedHandlers);

    for (ThreadHandler handler : reversedHandlers) {
      // Only cleanup handlers that completed BEFORE phase
      if (!completed.contains(handler)) {
        continue;
      }

      try {
        log.trace("[{}] CLEANUP: {} (error={})",
            context.getRequestId(), handler.getName(), errorOccurred);
        handler.cleanup(context, errorOccurred);
      } catch (Exception e) {
        // Log but continue cleanup - don't let one handler's failure prevent others
        log.error("[{}] CLEANUP failed for handler: {} (continuing with remaining handlers)",
            context.getRequestId(), handler.getName(), e);
      }
    }

    log.debug("[{}] CLEANUP phase completed", context.getRequestId());

    // Clear thread-local state
    completed.clear();
    errorFlag.set(false);
  }

  /**
   * Executes complete lifecycle: BEFORE → ACTION → CLEANUP.
   *
   * <p>Convenience method that handles the full lifecycle in proper order
   * with guaranteed cleanup execution.</p>
   *
   * @param context filter execution context
   * @param action action to execute between BEFORE and CLEANUP
   * @throws FilterException if BEFORE or ACTION phase fails
   */
  public void executeLifecycle(FilterContext context, FilterAction action) throws FilterException {
    boolean errorOccurred = false;

    try {
      executeBefore(context);
      executeAction(context, action);
    } catch (FilterException e) {
      errorOccurred = true;
      throw e;
    } finally {
      executeCleanup(context, errorOccurred);
    }
  }

  /**
   * Returns the number of registered handlers.
   *
   * @return handler count
   */
  public int getHandlerCount() {
    return handlers.size();
  }

  /**
   * Returns read-only view of registered handlers.
   *
   * @return unmodifiable list of handlers
   */
  public List<ThreadHandler> getHandlers() {
    return Collections.unmodifiableList(new ArrayList<>(handlers));
  }

  /**
   * Clears all registered handlers (for testing only).
   */
  void clearHandlersForTesting() {
    handlers.clear();
    completedBeforeHandlers.remove();
    errorFlag.remove();
  }

  /**
   * Functional interface for filter action execution.
   */
  @FunctionalInterface
  public interface FilterAction {
    /**
     * Executes the filter action.
     *
     * @throws FilterException if action fails
     */
    void execute() throws FilterException;
  }
}
