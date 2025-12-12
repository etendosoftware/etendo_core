package com.etendoerp.base.filter.extension;

import com.etendoerp.base.filter.core.FilterExecutor;

/**
 * Extension point for module developers to add custom filters.
 *
 * <p>Custom filters are discovered via CDI using {@code @ApplicationScoped} annotation
 * and executed based on {@code @Priority} value. Lower priority values execute first.</p>
 *
 * <p><strong>Implementation Requirements:</strong></p>
 * <ul>
 *   <li>Annotate with {@code @ApplicationScoped} for CDI discovery</li>
 *   <li>Annotate with {@code @Priority(X)} where X >= 101 (custom filter range)</li>
 *   <li>Implement all FilterExecutor methods</li>
 *   <li>Ensure execute() completes within getTimeout() seconds</li>
 *   <li>Make cleanup() idempotent (safe to call multiple times)</li>
 * </ul>
 *
 * <p><strong>Example Implementation:</strong></p>
 * <pre>{@code
 * @ApplicationScoped
 * @Priority(200)
 * public class MyCustomFilter implements CustomFilterPlugin {
 *
 *   @Override
 *   public void execute(FilterContext context) throws FilterException {
 *     // Your custom filter logic
 *     if (someCondition) {
 *       throw new FilterException("Custom validation failed", 400);
 *     }
 *   }
 *
 *   @Override
 *   public void cleanup(FilterContext context, boolean errorOccurred) {
 *     // Release resources
 *   }
 *
 *   @Override
 *   public boolean shouldExecute(FilterContext context) {
 *     // Only execute for specific paths
 *     return context.getPath().startsWith("/api/custom");
 *   }
 *
 *   @Override
 *   public int getPriority() {
 *     return 200; // Must match @Priority annotation
 *   }
 *
 *   @Override
 *   public String getName() {
 *     return "MyCustomFilter";
 *   }
 * }
 * }</pre>
 *
 * <p><strong>Priority Ranges:</strong></p>
 * <ul>
 *   <li>0-100: Reserved for core Etendo filters (DO NOT USE)</li>
 *   <li>101-500: Recommended for most custom filters</li>
 *   <li>501-999: Low priority filters (executed last)</li>
 * </ul>
 *
 * <p><strong>Best Practices:</strong></p>
 * <ul>
 *   <li>Keep execute() fast - avoid expensive operations</li>
 *   <li>Use shouldExecute() to skip unnecessary execution</li>
 *   <li>Set appropriate timeout via getTimeout() for long operations</li>
 *   <li>Store state in FilterContext attributes, not instance fields</li>
 *   <li>Always clean up resources in cleanup(), even on errors</li>
 *   <li>Use unique, descriptive names for logging</li>
 * </ul>
 *
 * @since Etendo 24.Q4
 * @see FilterExecutor
 */
public interface CustomFilterPlugin extends FilterExecutor {

  /**
   * Returns the module identifier that provides this custom filter.
   *
   * <p>Used for logging, debugging, and module dependency tracking.
   * Should return a stable identifier for the module.</p>
   *
   * <p>Recommended format: "com.example.modulename"</p>
   *
   * @return module identifier (must not be null or empty)
   */
  default String getModuleId() {
    return "unknown.module";
  }

  /**
   * Returns a human-readable description of what this filter does.
   *
   * <p>Used in logs, admin UI, and documentation generation.
   * Should be concise but informative.</p>
   *
   * <p>Example: "Validates custom business rules for invoice approval"</p>
   *
   * @return filter description (must not be null)
   */
  default String getDescription() {
    return "Custom filter plugin";
  }

  /**
   * Returns the version of this filter implementation.
   *
   * <p>Used for troubleshooting and compatibility checks.
   * Should follow semantic versioning (e.g., "1.0.0").</p>
   *
   * @return version string (must not be null)
   */
  default String getVersion() {
    return "1.0.0";
  }

  /**
   * Indicates whether this filter modifies the response.
   *
   * <p>If true, the coordinator may apply additional caching
   * or validation logic. Set to true if your filter writes to
   * the response body or modifies response headers.</p>
   *
   * @return true if filter modifies response, false otherwise
   */
  default boolean modifiesResponse() {
    return false;
  }

  /**
   * Indicates whether this filter requires database access.
   *
   * <p>If true, ensures proper transaction context is available.
   * Set to true if your filter reads from or writes to the database.</p>
   *
   * @return true if filter needs database access, false otherwise
   */
  default boolean requiresDatabase() {
    return false;
  }
}
