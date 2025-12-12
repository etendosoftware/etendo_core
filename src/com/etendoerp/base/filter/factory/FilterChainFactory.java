package com.etendoerp.base.filter.factory;

import com.etendoerp.base.filter.audit.UsageAuditFilter;
import com.etendoerp.base.filter.core.FilterExecutor;
import com.etendoerp.base.filter.filters.OBContextFilter;
import com.etendoerp.base.filter.filters.RequestContextFilter;
import com.etendoerp.base.filter.filters.SessionInfoFilter;
import com.etendoerp.base.filter.filters.TransactionManagementFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.etendoerp.base.filter.coordinator.FilterChainCoordinator;
import org.openbravo.base.filter.filters.*;
import com.etendoerp.base.filter.threadhandler.UnifiedThreadHandler;

/**
 * Factory for creating and configuring the filter chain coordinator.
 *
 * <p>Responsible for:</p>
 * <ul>
 *   <li>Creating UnifiedThreadHandler with all thread handlers</li>
 *   <li>Creating FilterChainCoordinator</li>
 *   <li>Registering all core filters in priority order</li>
 *   <li>Discovering and registering custom CDI filter plugins</li>
 *   <li>Initializing the coordinator</li>
 * </ul>
 *
 * <p><strong>Core Filters (Priority Order):</strong></p>
 * <ol>
 *   <li>TransactionManagementFilter (priority=10)</li>
 *   <li>OBContextFilter (priority=20)</li>
 *   <li>SessionInfoFilter (priority=30)</li>
 *   <li>RequestContextFilter (priority=40)</li>
 * </ol>
 *
 * <p><strong>Thread Handlers (Registration Order):</strong></p>
 * <ol>
 *   <li>TransactionThreadHandler</li>
 *   <li>OBContextThreadHandler</li>
 *   <li>SessionInfoThreadHandler</li>
 *   <li>RequestContextThreadHandler</li>
 * </ol>
 *
 * <p>Note: Thread handlers are registered in the same logical order as filters,
 * but execute BEFORE filters (setup) and cleanup in REVERSE order.</p>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * // Create coordinator with all filters configured
 * FilterChainCoordinator coordinator = FilterChainFactory.createCoordinator();
 *
 * // Use coordinator to process requests
 * FilterContext context = coordinator.createContext(request, response);
 * coordinator.execute(context);
 * }</pre>
 *
 * @since Etendo 24.Q4
 */
public class FilterChainFactory {

  private static final Logger log = LogManager.getLogger(FilterChainFactory.class);

  /**
   * Private constructor - use static factory methods.
   */
  private FilterChainFactory() {
    // Prevent instantiation
  }

  /**
   * Creates a fully configured FilterChainCoordinator.
   *
   * <p>This is the main entry point for creating the filter chain.
   * The coordinator is returned ready to use (already initialized).</p>
   *
   * <p><strong>Configuration Steps:</strong></p>
   * <ol>
   *   <li>Create UnifiedThreadHandler</li>
   *   <li>Register all thread handlers</li>
   *   <li>Create FilterChainCoordinator</li>
   *   <li>Register all core filters</li>
   *   <li>Discover and register custom CDI filters</li>
   *   <li>Initialize coordinator</li>
   * </ol>
   *
   * @return initialized filter chain coordinator
   * @throws IllegalStateException if initialization fails
   */
  public static FilterChainCoordinator createCoordinator() {
    log.info("Creating FilterChainCoordinator via factory");

    try {
      // Step 1: Create thread handler
      UnifiedThreadHandler threadHandler = createThreadHandler();
      log.debug("UnifiedThreadHandler created with {} handler(s)", threadHandler.getHandlerCount());

      // Step 2: Create coordinator
      FilterChainCoordinator coordinator = new FilterChainCoordinator(threadHandler);
      log.debug("FilterChainCoordinator created");

      // Step 3: Register core filters
      registerCoreFilters(coordinator);
      log.debug("Core filters registered");

      // Step 4: Discover custom filters
      coordinator.discoverCustomFilters();
      log.debug("Custom filters discovered");

      // Step 5: Initialize coordinator
      coordinator.initialize();
      log.info("FilterChainCoordinator initialized successfully");

      return coordinator;

    } catch (Exception e) {
      log.error("Failed to create FilterChainCoordinator", e);
      throw new IllegalStateException("Failed to create FilterChainCoordinator: " + e.getMessage(), e);
    }
  }

  /**
   * Creates and configures the UnifiedThreadHandler.
   *
   * <p>Registers all thread handlers in order. Thread handlers execute
   * BEFORE filters for setup and in REVERSE order for cleanup.</p>
   *
   * <p>Note: Currently no thread handlers are registered as they've been
   * consolidated into the filters themselves. This method is kept for
   * future extensibility if thread handlers are needed.</p>
   *
   * @return configured thread handler
   */
  private static UnifiedThreadHandler createThreadHandler() {
    UnifiedThreadHandler threadHandler = new UnifiedThreadHandler();

    // Note: In the refactored design, thread-local state management
    // has been moved into the filters themselves for better cohesion.
    //
    // If future requirements demand separate thread handlers,
    // register them here in order:
    //
    // threadHandler.registerHandler(new TransactionThreadHandler());
    // threadHandler.registerHandler(new OBContextThreadHandler());
    // threadHandler.registerHandler(new SessionInfoThreadHandler());
    // threadHandler.registerHandler(new RequestContextThreadHandler());

    log.debug("UnifiedThreadHandler created (no separate handlers - using filter-based management)");

    return threadHandler;
  }

  /**
   * Registers all core filters with the coordinator.
   *
   * <p>Filters are registered in priority order (although the coordinator
   * will re-sort them). This makes the registration order explicit and
   * easier to understand.</p>
   *
   * @param coordinator filter chain coordinator
   */
  private static void registerCoreFilters(FilterChainCoordinator coordinator) {
    // Register in priority order for clarity
    // (coordinator will sort by priority anyway)

    // Priority 10: Transaction Management
    coordinator.registerFilter(new TransactionManagementFilter());
    log.debug("Registered: TransactionManagementFilter (priority=10)");

    // Priority 20: OBContext
    coordinator.registerFilter(new OBContextFilter());
    log.debug("Registered: OBContextFilter (priority=20)");

    // Priority 30: Session Info
    coordinator.registerFilter(new SessionInfoFilter());
    log.debug("Registered: SessionInfoFilter (priority=30)");

    // Priority 40: Request Context
    coordinator.registerFilter(new RequestContextFilter());
    log.debug("Registered: RequestContextFilter (priority=40)");

    // Priority 50: Usage Audit (automatic audit logging)
    coordinator.registerFilter(new UsageAuditFilter());
    log.debug("Registered: UsageAuditFilter (priority=50)");

    log.info("Registered {} core filter(s)", 5);
  }

  /**
   * Creates a coordinator for testing with custom filters.
   *
   * <p>Allows tests to inject custom filters without CDI discovery.
   * The coordinator is returned uninitialized - caller must call initialize().</p>
   *
   * @param filters custom filters to register
   * @return uninitialized coordinator
   */
  public static FilterChainCoordinator createCoordinatorForTesting(
      FilterExecutor... filters) {

    UnifiedThreadHandler threadHandler = new UnifiedThreadHandler();
    FilterChainCoordinator coordinator = new FilterChainCoordinator(threadHandler);

    for (FilterExecutor filter : filters) {
      coordinator.registerFilter(filter);
    }

    log.debug("Created coordinator for testing with {} filter(s)", filters.length);

    return coordinator;
  }

  /**
   * Creates a minimal coordinator for testing (no filters).
   *
   * <p>The coordinator is returned uninitialized. Tests can register filters
   * manually and then call initialize().</p>
   *
   * @return empty, uninitialized coordinator
   */
  public static FilterChainCoordinator createMinimalCoordinatorForTesting() {
    UnifiedThreadHandler threadHandler = new UnifiedThreadHandler();
    FilterChainCoordinator coordinator = new FilterChainCoordinator(threadHandler);

    log.debug("Created minimal coordinator for testing (no filters)");

    return coordinator;
  }
}
