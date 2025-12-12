package com.etendoerp.base.filter.coordinator;

import com.etendoerp.base.filter.core.FilterContext;
import com.etendoerp.base.filter.core.FilterException;
import com.etendoerp.base.filter.core.FilterExecutor;
import com.etendoerp.base.filter.core.FilterLifecycleState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.filter.core.*;
import com.etendoerp.base.filter.extension.CustomFilterPlugin;
import com.etendoerp.base.filter.threadhandler.UnifiedThreadHandler;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.*;

/**
 * Central coordinator for the refactored filter chain.
 *
 * <p>Manages filter discovery, execution order, timeout enforcement, and lifecycle.
 * Replaces nested legacy filters with a clean, coordinated execution model.</p>
 *
 * <p><strong>Key Responsibilities:</strong></p>
 * <ul>
 *   <li>Discover and register filters (core + CDI plugins)</li>
 *   <li>Execute filters in priority order with timeout enforcement</li>
 *   <li>Manage FilterContext lifecycle (INITIALIZED → EXECUTING → CLEANING_UP → COMPLETED)</li>
 *   <li>Guarantee cleanup execution in reverse order, even on errors</li>
 *   <li>Integrate with UnifiedThreadHandler for thread-local state management</li>
 * </ul>
 *
 * <p><strong>Filter Discovery:</strong></p>
 * <ul>
 *   <li>Core filters: Registered explicitly via registerFilter()</li>
 *   <li>Custom filters: Auto-discovered via CDI (must implement CustomFilterPlugin)</li>
 * </ul>
 *
 * <p><strong>Execution Order:</strong></p>
 * <ol>
 *   <li>Sort filters by priority (lower values execute first)</li>
 *   <li>Within same priority: core filters before custom filters</li>
 *   <li>Within same priority+type: order by class name (deterministic)</li>
 * </ol>
 *
 * <p><strong>Error Handling:</strong></p>
 * <ul>
 *   <li>Filter timeout → HTTP 503 (Service Unavailable)</li>
 *   <li>Filter exception → HTTP status from FilterException, cleanup executed</li>
 *   <li>Cleanup always runs in reverse order for executed filters</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Initialization (once at startup)
 * FilterChainCoordinator coordinator = new FilterChainCoordinator(threadHandler);
 * coordinator.registerFilter(new TransactionManagementFilter());
 * coordinator.registerFilter(new OBContextFilter());
 * coordinator.discoverCustomFilters();
 * coordinator.initialize();
 *
 * // Request processing
 * FilterContext context = coordinator.createContext(request, response);
 * try {
 *   coordinator.execute(context);
 * } catch (FilterException e) {
 *   response.sendError(e.getHttpStatusCode(), e.getMessage());
 * }
 * }</pre>
 *
 * @since Etendo 24.Q4
 */
public class FilterChainCoordinator {

  private static final Logger log = LogManager.getLogger(FilterChainCoordinator.class);

  private final UnifiedThreadHandler threadHandler;
  private final List<FilterExecutor> registeredFilters;
  private final List<FilterExecutor> sortedFilters;
  private final ExecutorService timeoutExecutor;
  private volatile boolean initialized;

  /**
   * Creates a new coordinator with the given thread handler.
   *
   * @param threadHandler unified thread handler for managing thread-local state
   */
  public FilterChainCoordinator(UnifiedThreadHandler threadHandler) {
    this.threadHandler = Objects.requireNonNull(threadHandler, "threadHandler must not be null");
    this.registeredFilters = new ArrayList<>();
    this.sortedFilters = new ArrayList<>();
    this.timeoutExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
      private int counter = 0;

      @Override
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "FilterTimeout-" + (counter++));
        t.setDaemon(true);
        return t;
      }
    });
    this.initialized = false;
  }

  /**
   * Registers a core filter.
   *
   * <p>Must be called before initialize(). Filters are sorted by priority
   * during initialization.</p>
   *
   * @param filter filter to register (must not be null)
   * @throws IllegalStateException if already initialized
   */
  public void registerFilter(FilterExecutor filter) {
    Objects.requireNonNull(filter, "filter must not be null");

    if (initialized) {
      throw new IllegalStateException("Cannot register filters after initialization");
    }

    registeredFilters.add(filter);
    log.debug("Registered core filter: {} (priority={})", filter.getName(), filter.getPriority());
  }

  /**
   * Discovers and registers custom filters via CDI.
   *
   * <p>Scans for all beans implementing CustomFilterPlugin and registers them
   * based on @Priority annotation. Must be called before initialize().</p>
   *
   * @throws IllegalStateException if already initialized
   */
  public void discoverCustomFilters() {
    if (initialized) {
      throw new IllegalStateException("Cannot discover filters after initialization");
    }

    try {
      CDI<Object> cdi = CDI.current();
      Instance<CustomFilterPlugin> plugins = cdi.select(CustomFilterPlugin.class);

      int discoveredCount = 0;
      for (CustomFilterPlugin plugin : plugins) {
        registeredFilters.add(plugin);
        log.info("Discovered custom filter plugin: {} (priority={}, module={})",
            plugin.getName(), plugin.getPriority(), plugin.getModuleId());
        discoveredCount++;
      }

      log.info("Discovered {} custom filter plugin(s)", discoveredCount);

    } catch (IllegalStateException e) {
      // CDI not available - this is expected in some environments (e.g., tests)
      log.warn("CDI not available - skipping custom filter discovery");
    }
  }

  /**
   * Initializes the coordinator by sorting filters and validating configuration.
   *
   * <p>Must be called after all filters are registered and before execute().
   * This method is idempotent - subsequent calls have no effect.</p>
   *
   * @throws IllegalStateException if no filters registered
   */
  public void initialize() {
    if (initialized) {
      log.debug("Coordinator already initialized - skipping");
      return;
    }

    if (registeredFilters.isEmpty()) {
      throw new IllegalStateException("No filters registered - cannot initialize");
    }

    sortFilters();
    validateFilters();
    logFilterChain();

    initialized = true;
    log.info("FilterChainCoordinator initialized with {} filter(s)", sortedFilters.size());
  }

  /**
   * Sorts filters by priority, type, and class name.
   *
   * <p>Sorting criteria (in order):</p>
   * <ol>
   *   <li>Priority (ascending - lower values first)</li>
   *   <li>Type (core filters before custom filters)</li>
   *   <li>Class name (alphabetical - for determinism)</li>
   * </ol>
   */
  private void sortFilters() {
    sortedFilters.clear();
    sortedFilters.addAll(registeredFilters);

    sortedFilters.sort((f1, f2) -> {
      // 1. Sort by priority (ascending)
      int priorityCompare = Integer.compare(f1.getPriority(), f2.getPriority());
      if (priorityCompare != 0) {
        return priorityCompare;
      }

      // 2. Core filters before custom filters (at same priority)
      boolean f1IsCustom = f1 instanceof CustomFilterPlugin;
      boolean f2IsCustom = f2 instanceof CustomFilterPlugin;
      if (f1IsCustom != f2IsCustom) {
        return f1IsCustom ? 1 : -1;
      }

      // 3. Alphabetical by class name (deterministic order)
      return f1.getClass().getName().compareTo(f2.getClass().getName());
    });

    log.debug("Sorted {} filters by priority", sortedFilters.size());
  }

  /**
   * Validates filter configuration.
   *
   * @throws IllegalStateException if validation fails
   */
  private void validateFilters() {
    for (FilterExecutor filter : sortedFilters) {
      // Validate name
      if (filter.getName() == null || filter.getName().trim().isEmpty()) {
        throw new IllegalStateException(
            "Filter " + filter.getClass().getName() + " has null or empty name");
      }

      // Validate timeout
      if (filter.getTimeout() <= 0 || filter.getTimeout() > 300) {
        throw new IllegalStateException(
            "Filter " + filter.getName() + " has invalid timeout: " + filter.getTimeout() +
                " (must be 1-300 seconds)");
      }

      // Validate priority range for custom filters
      if (filter instanceof CustomFilterPlugin && filter.getPriority() < 101) {
        log.warn("Custom filter {} has priority {} < 101 (reserved for core filters)",
            filter.getName(), filter.getPriority());
      }
    }
  }

  /**
   * Logs the filter execution order.
   */
  private void logFilterChain() {
    log.info("=== Filter Chain Execution Order ===");
    for (int i = 0; i < sortedFilters.size(); i++) {
      FilterExecutor filter = sortedFilters.get(i);
      String type = filter instanceof CustomFilterPlugin ?
          "CUSTOM (" + ((CustomFilterPlugin) filter).getModuleId() + ")" : "CORE";
      log.info("  {}. [{}] {} (priority={}, timeout={}s)",
          i + 1, type, filter.getName(), filter.getPriority(), filter.getTimeout());
    }
    log.info("====================================");
  }

  /**
   * Creates a new FilterContext for a request.
   *
   * @param request HTTP request
   * @param response HTTP response
   * @return new filter context
   */
  public FilterContext createContext(HttpServletRequest request, HttpServletResponse response) {
    return new FilterContext(request, response);
  }

  /**
   * Executes the filter chain for the given context.
   *
   * <p>Lifecycle: INITIALIZED → EXECUTING → CLEANING_UP → COMPLETED</p>
   *
   * <p>Guarantees:</p>
   * <ul>
   *   <li>Filters execute in priority order</li>
   *   <li>Each filter respects timeout constraint</li>
   *   <li>Cleanup runs in reverse order, even on errors</li>
   *   <li>Thread-local state is properly managed</li>
   * </ul>
   *
   * @param context filter execution context
   * @throws FilterException if any filter fails or times out
   */
  public void execute(FilterContext context) throws FilterException {
    if (!initialized) {
      throw new IllegalStateException("Coordinator not initialized - call initialize() first");
    }

    Objects.requireNonNull(context, "context must not be null");

    log.debug("[{}] Starting filter chain execution ({} filter(s))",
        context.getRequestId(), sortedFilters.size());

    List<FilterExecutor> executedFilters = new ArrayList<>();
    boolean errorOccurred = false;

    try {
      // Transition to EXECUTING state
      context.setLifecycleState(FilterLifecycleState.EXECUTING);

      // Execute thread handler BEFORE phase
      threadHandler.executeBefore(context);

      // Execute filters in order
      for (FilterExecutor filter : sortedFilters) {
        // Check if filter should execute
        if (!filter.shouldExecute(context)) {
          log.debug("[{}] Skipping filter (shouldExecute=false): {}",
              context.getRequestId(), filter.getName());
          continue;
        }

        // Execute filter with timeout
        executeFilterWithTimeout(context, filter);
        executedFilters.add(filter);

        log.debug("[{}] Filter completed: {}", context.getRequestId(), filter.getName());
      }

      log.info("[{}] Filter chain execution completed successfully ({} filter(s), {}ms)",
          context.getRequestId(), executedFilters.size(), context.getElapsedTime());

    } catch (Exception e) {
      errorOccurred = true;
      context.setErrorOccurred();
      log.error("[{}] Filter chain execution failed after {}ms",
          context.getRequestId(), context.getElapsedTime(), e);

      if (e instanceof FilterException) {
        throw (FilterException) e;
      }
      throw new FilterException("Filter chain execution failed: " + e.getMessage(), 500);

    } finally {
      // Transition to CLEANING_UP state
      context.setLifecycleState(FilterLifecycleState.CLEANING_UP);

      // Cleanup executed filters in reverse order
      cleanupFilters(context, executedFilters, errorOccurred);

      // Execute thread handler CLEANUP phase
      threadHandler.executeCleanup(context, errorOccurred);

      // Transition to COMPLETED state
      context.setLifecycleState(FilterLifecycleState.COMPLETED);

      log.debug("[{}] Filter chain lifecycle completed", context.getRequestId());
    }
  }

  /**
   * Executes a single filter with timeout enforcement.
   *
   * @param context filter execution context
   * @param filter filter to execute
   * @throws FilterException if filter fails or times out
   */
  private void executeFilterWithTimeout(FilterContext context, FilterExecutor filter)
      throws FilterException {

    log.debug("[{}] Executing filter: {} (timeout={}s)",
        context.getRequestId(), filter.getName(), filter.getTimeout());

    Future<Void> future = timeoutExecutor.submit(() -> {
      filter.execute(context);
      return null;
    });

    try {
      future.get(filter.getTimeout(), TimeUnit.SECONDS);

    } catch (TimeoutException e) {
      future.cancel(true);
      log.error("[{}] Filter timed out after {}s: {}",
          context.getRequestId(), filter.getTimeout(), filter.getName());
      throw new FilterException(
          "Filter " + filter.getName() + " timed out after " + filter.getTimeout() + "s",
          503);

    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof FilterException) {
        throw (FilterException) cause;
      }
      throw new FilterException(
          "Filter " + filter.getName() + " failed: " + cause.getMessage(),
          500);

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new FilterException(
          "Filter " + filter.getName() + " was interrupted",
          503);
    }
  }

  /**
   * Cleans up executed filters in reverse order.
   *
   * @param context filter execution context
   * @param executedFilters filters that executed (in execution order)
   * @param errorOccurred true if an error occurred during execution
   */
  private void cleanupFilters(FilterContext context, List<FilterExecutor> executedFilters,
      boolean errorOccurred) {

    log.debug("[{}] Cleaning up {} filter(s) (error={})",
        context.getRequestId(), executedFilters.size(), errorOccurred);

    // Reverse order cleanup
    List<FilterExecutor> reversedFilters = new ArrayList<>(executedFilters);
    Collections.reverse(reversedFilters);

    for (FilterExecutor filter : reversedFilters) {
      try {
        log.trace("[{}] Cleanup: {}", context.getRequestId(), filter.getName());
        filter.cleanup(context, errorOccurred);
      } catch (Exception e) {
        // Log but continue - don't let one cleanup failure prevent others
        log.error("[{}] Cleanup failed for filter: {} (continuing with remaining filters)",
            context.getRequestId(), filter.getName(), e);
      }
    }
  }

  /**
   * Returns read-only view of registered filters in execution order.
   *
   * @return unmodifiable list of filters
   */
  public List<FilterExecutor> getFilters() {
    return Collections.unmodifiableList(new ArrayList<>(sortedFilters));
  }

  /**
   * Returns the number of registered filters.
   *
   * @return filter count
   */
  public int getFilterCount() {
    return sortedFilters.size();
  }

  /**
   * Shuts down the coordinator and releases resources.
   *
   * <p>Should be called during application shutdown.</p>
   */
  public void shutdown() {
    log.info("Shutting down FilterChainCoordinator");
    timeoutExecutor.shutdown();
    try {
      if (!timeoutExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        timeoutExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      timeoutExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
