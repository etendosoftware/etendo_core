package com.etendoerp.base.filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.etendoerp.base.filter.config.FilterConfiguration;
import com.etendoerp.base.filter.coordinator.FilterChainCoordinator;
import com.etendoerp.base.filter.core.FilterContext;
import com.etendoerp.base.filter.core.FilterException;
import com.etendoerp.base.filter.factory.FilterChainFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Main servlet filter that coordinates the refactored filter chain.
 *
 * <p>This filter serves as the entry point for request processing and implements
 * the feature flag pattern to allow safe rollback to legacy implementation.</p>
 *
 * <p><strong>Feature Flag Behavior:</strong></p>
 * <ul>
 *   <li><strong>Legacy Mode (default):</strong> Delegates to existing legacy filter chain</li>
 *   <li><strong>New Mode:</strong> Uses FilterChainCoordinator for refactored implementation</li>
 *   <li><strong>Toggle:</strong> Set FILTER_CHAIN_LEGACY=false to enable new chain</li>
 * </ul>
 *
 * <p><strong>Initialization:</strong></p>
 * <ol>
 *   <li>Check FilterConfiguration for mode (legacy vs. new)</li>
 *   <li>If new mode: Initialize FilterChainCoordinator via FilterChainFactory</li>
 *   <li>If legacy mode: No initialization needed (uses existing filters)</li>
 * </ol>
 *
 * <p><strong>Request Processing:</strong></p>
 * <ol>
 *   <li>Cast request/response to HTTP types</li>
 *   <li>Check FilterConfiguration mode</li>
 *   <li>Route to appropriate implementation:
 *     <ul>
 *       <li>Legacy: chain.doFilter(request, response)</li>
 *       <li>New: coordinator.execute(context)</li>
 *     </ul>
 *   </li>
 *   <li>Handle exceptions and set appropriate HTTP status</li>
 * </ol>
 *
 * <p><strong>Error Handling:</strong></p>
 * <ul>
 *   <li>FilterException → HTTP status from exception + error message</li>
 *   <li>Unexpected exception → HTTP 500 + generic error message</li>
 *   <li>All exceptions logged with request correlation ID</li>
 * </ul>
 *
 * <p><strong>Configuration (web.xml):</strong></p>
 * <pre>{@code
 * <filter>
 *   <filter-name>RefactoredFilterChain</filter-name>
 *   <filter-class>org.openbravo.base.filter.RefactoredFilterChain</filter-class>
 * </filter>
 * <filter-mapping>
 *   <filter-name>RefactoredFilterChain</filter-name>
 *   <url-pattern>/*</url-pattern>
 * </filter-mapping>
 * }</pre>
 *
 * @since Etendo 24.Q4
 */
public class RefactoredFilterChain implements Filter {

  private static final Logger log = LogManager.getLogger(RefactoredFilterChain.class);

  private FilterChainCoordinator coordinator;
  private boolean legacyMode;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    log.info("Initializing RefactoredFilterChain");

    try {
      // Check configuration mode
      legacyMode = FilterConfiguration.isLegacyMode();

      if (legacyMode) {
        log.info("Filter chain in LEGACY mode - using existing filter implementation");
        log.info("To enable new filter chain, set FILTER_CHAIN_LEGACY=false");
      } else {
        log.info("Filter chain in NEW mode - initializing refactored implementation");

        // Initialize coordinator via factory
        coordinator = FilterChainFactory.createCoordinator();

        log.info("RefactoredFilterChain initialized successfully with {} filter(s)",
            coordinator.getFilterCount());
      }

    } catch (Exception e) {
      log.error("Failed to initialize RefactoredFilterChain - falling back to legacy mode", e);
      legacyMode = true;
      coordinator = null;
    }
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    // Ensure HTTP request/response
    if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
      log.warn("Non-HTTP request received - delegating to chain");
      chain.doFilter(request, response);
      return;
    }

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    // Route to appropriate implementation
    if (legacyMode) {
      // Legacy mode: delegate to existing filter chain
      chain.doFilter(request, response);
    } else {
      // New mode: use refactored filter chain
      processWithNewFilterChain(httpRequest, httpResponse, chain);
    }
  }

  /**
   * Processes request using the new refactored filter chain.
   *
   * @param request HTTP request
   * @param response HTTP response
   * @param chain servlet filter chain to continue processing
   */
  private void processWithNewFilterChain(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain chain) throws IOException, ServletException {

    FilterContext context = null;

    try {
      // Create filter context
      context = coordinator.createContext(request, response);

      log.debug("[{}] Processing request: {} {}",
          context.getRequestId(), request.getMethod(), request.getRequestURI());

      // Execute our filter chain
      coordinator.execute(context);

      // Check if filters wrapped the request (e.g., for body caching)
      HttpServletRequest requestToUse = request;
      Object cachedRequest = context.getAttribute("audit.cachedRequest");
      if (cachedRequest instanceof HttpServletRequest) {
        requestToUse = (HttpServletRequest) cachedRequest;
        log.debug("[{}] Using wrapped request for servlet chain", context.getRequestId());
      }

      // Continue to servlet processing
      log.debug("[{}] Filters completed, continuing to servlet", context.getRequestId());
      chain.doFilter(requestToUse, response);

      log.debug("[{}] Request completed successfully ({}ms)",
          context.getRequestId(), context.getElapsedTime());

    } catch (FilterException e) {
      // Handle filter exception with specific HTTP status
      handleFilterException(context, response, e);

    } catch (Exception e) {
      // Handle unexpected exception
      handleUnexpectedException(context, response, e);

    } finally {
      // Log request completion
      if (context != null) {
        logRequestCompletion(context, response);
      }
    }
  }

  /**
   * Handles FilterException by setting appropriate HTTP status and error message.
   *
   * @param context filter context (may be null)
   * @param response HTTP response
   * @param e filter exception
   */
  private void handleFilterException(FilterContext context, HttpServletResponse response,
      FilterException e) throws IOException {

    String requestId = context != null ? context.getRequestId() : "unknown";
    int statusCode = e.getHttpStatusCode();
    String message = e.getMessage();

    log.error("[{}] Filter exception: {} - {}", requestId, statusCode, message, e);

    if (!response.isCommitted()) {
      response.sendError(statusCode, message);
    } else {
      log.warn("[{}] Response already committed - cannot send error {}", requestId, statusCode);
    }
  }

  /**
   * Handles unexpected exception by setting HTTP 500 status.
   *
   * @param context filter context (may be null)
   * @param response HTTP response
   * @param e exception
   */
  private void handleUnexpectedException(FilterContext context, HttpServletResponse response,
      Exception e) throws IOException {

    String requestId = context != null ? context.getRequestId() : "unknown";

    log.error("[{}] Unexpected exception during filter chain execution", requestId, e);

    if (!response.isCommitted()) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Internal server error during request processing");
    } else {
      log.warn("[{}] Response already committed - cannot send error 500", requestId);
    }
  }

  /**
   * Logs request completion with summary information.
   *
   * @param context filter context
   * @param response HTTP response
   */
  private void logRequestCompletion(FilterContext context, HttpServletResponse response) {
    int statusCode = response.getStatus();
    long duration = context.getElapsedTime();

    String logLevel = statusCode >= 500 ? "ERROR" :
        statusCode >= 400 ? "WARN" :
            "INFO";

    String message = String.format("[%s] Request completed: %s %s -> %d (%dms)",
        context.getRequestId(),
        context.getMethod(),
        context.getPath(),
        statusCode,
        duration);

    switch (logLevel) {
      case "ERROR":
        log.error(message);
        break;
      case "WARN":
        log.warn(message);
        break;
      default:
        log.info(message);
    }
  }

  @Override
  public void destroy() {
    log.info("Destroying RefactoredFilterChain");

    if (coordinator != null) {
      try {
        coordinator.shutdown();
        log.info("FilterChainCoordinator shutdown completed");
      } catch (Exception e) {
        log.error("Error during coordinator shutdown", e);
      }
    }
  }

  /**
   * Returns whether legacy mode is active.
   *
   * @return true if using legacy filter chain
   */
  public boolean isLegacyMode() {
    return legacyMode;
  }

  /**
   * Returns the filter chain coordinator (null if in legacy mode).
   *
   * @return coordinator or null
   */
  public FilterChainCoordinator getCoordinator() {
    return coordinator;
  }
}
