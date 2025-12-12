package com.etendoerp.base.filter.filters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.etendoerp.base.filter.core.FilterContext;
import com.etendoerp.base.filter.core.FilterException;
import com.etendoerp.base.filter.core.FilterExecutor;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Core filter for request context initialization.
 *
 * <p>Manages request-specific context information including request parameters,
 * headers, and application-specific metadata that needs to be available
 * throughout request processing.</p>
 *
 * <p><strong>Lifecycle:</strong></p>
 * <ol>
 *   <li><strong>execute():</strong> Initialize request context with parameters and metadata</li>
 *   <li><em>(Other filters and business logic use request context)</em></li>
 *   <li><strong>cleanup():</strong> Clear thread-local request context state</li>
 * </ol>
 *
 * <p><strong>Request Context Information:</strong></p>
 * <ul>
 *   <li>Request parameters (query string + form data)</li>
 *   <li>Request headers (content type, accept, etc.)</li>
 *   <li>Request metadata (servlet path, context path)</li>
 *   <li>Application context (base URL, web root)</li>
 * </ul>
 *
 * <p><strong>Use Cases:</strong></p>
 * <ul>
 *   <li>Access request parameters from deep in call stack</li>
 *   <li>URL generation (base URL for redirects, links)</li>
 *   <li>Content negotiation (Accept header processing)</li>
 *   <li>Debugging (full request context in logs)</li>
 * </ul>
 *
 * <p><strong>Context Attributes:</strong></p>
 * <ul>
 *   <li>{@code "requestContext"} - Map of request context data</li>
 *   <li>{@code "requestContext.initialized"} - Boolean flag</li>
 *   <li>{@code "requestContext.baseUrl"} - String base URL</li>
 *   <li>{@code "requestContext.parameters"} - Map of request parameters</li>
 * </ul>
 *
 * <p><strong>Priority:</strong> 40 (executes after SessionInfo setup)</p>
 *
 * @since Etendo 24.Q4
 */
public class RequestContextFilter implements FilterExecutor {

  private static final Logger log = LogManager.getLogger(RequestContextFilter.class);

  private static final String ATTR_REQUEST_CONTEXT = "requestContext";
  private static final String ATTR_REQUEST_CONTEXT_INITIALIZED = "requestContext.initialized";
  private static final String ATTR_REQUEST_CONTEXT_BASE_URL = "requestContext.baseUrl";
  private static final String ATTR_REQUEST_CONTEXT_PARAMETERS = "requestContext.parameters";

  private static final int PRIORITY = 40;
  private static final int TIMEOUT_SECONDS = 30;

  @Override
  public void execute(FilterContext context) throws FilterException {
    log.debug("[{}] Initializing request context", context.getRequestId());

    try {
      HttpServletRequest request = context.getRequest();

      // Build request context data
      Map<String, Object> requestContext = new HashMap<>();

      // Basic request info
      requestContext.put("method", context.getMethod());
      requestContext.put("path", context.getPath());
      requestContext.put("queryString", request.getQueryString());
      requestContext.put("servletPath", request.getServletPath());
      requestContext.put("contextPath", request.getContextPath());

      // Request parameters
      Map<String, String[]> parameters = extractRequestParameters(request);
      requestContext.put("parameters", parameters);

      // Important headers
      String contentType = request.getContentType();
      String accept = request.getHeader("Accept");
      String referer = request.getHeader("Referer");

      requestContext.put("contentType", contentType);
      requestContext.put("accept", accept);
      requestContext.put("referer", referer);

      // Build base URL
      String baseUrl = buildBaseUrl(request);
      requestContext.put("baseUrl", baseUrl);

      // Request characteristics
      requestContext.put("isAjax", isAjaxRequest(request));
      requestContext.put("isSecure", request.isSecure());
      requestContext.put("scheme", request.getScheme());
      requestContext.put("serverName", request.getServerName());
      requestContext.put("serverPort", request.getServerPort());

      // Store in context
      context.setAttribute(ATTR_REQUEST_CONTEXT, requestContext);
      context.setAttribute(ATTR_REQUEST_CONTEXT_INITIALIZED, true);
      context.setAttribute(ATTR_REQUEST_CONTEXT_BASE_URL, baseUrl);
      context.setAttribute(ATTR_REQUEST_CONTEXT_PARAMETERS, parameters);

      log.debug("[{}] Request context initialized (baseUrl={}, params={})",
          context.getRequestId(), baseUrl, parameters.size());

    } catch (Exception e) {
      log.error("[{}] Failed to initialize request context", context.getRequestId(), e);
      throw new FilterException("Failed to initialize request context: " + e.getMessage(), 500);
    }
  }

  @Override
  public void cleanup(FilterContext context, boolean errorOccurred) {
    Boolean initialized = context.getAttribute(ATTR_REQUEST_CONTEXT_INITIALIZED, Boolean.class);

    if (!Boolean.TRUE.equals(initialized)) {
      log.trace("[{}] Request context was not initialized - skipping cleanup",
          context.getRequestId());
      return;
    }

    try {
      log.debug("[{}] Cleaning up request context (error={})",
          context.getRequestId(), errorOccurred);

      // Clear thread-local request context (if using HttpBaseServlet pattern)
      // Note: Actual implementation depends on how HttpBaseServlet stores context
      // This is a placeholder for compatibility with existing code

      log.debug("[{}] Request context cleanup completed", context.getRequestId());

    } catch (Exception e) {
      log.error("[{}] Error during request context cleanup", context.getRequestId(), e);
      // Don't throw - cleanup must complete
    } finally {
      // Clear context attributes
      context.removeAttribute(ATTR_REQUEST_CONTEXT);
      context.setAttribute(ATTR_REQUEST_CONTEXT_INITIALIZED, false);
      context.removeAttribute(ATTR_REQUEST_CONTEXT_BASE_URL);
      context.removeAttribute(ATTR_REQUEST_CONTEXT_PARAMETERS);
    }
  }

  @Override
  public boolean shouldExecute(FilterContext context) {
    // Execute for all requests except static resources
    String path = context.getPath();

    // Skip for static resources
    if (path.endsWith(".js") || path.endsWith(".css") ||
        path.endsWith(".png") || path.endsWith(".jpg") ||
        path.endsWith(".gif") || path.endsWith(".ico") ||
        path.endsWith(".svg") || path.endsWith(".woff") ||
        path.endsWith(".woff2") || path.endsWith(".ttf")) {
      return false;
    }

    return true;
  }

  /**
   * Extracts all request parameters into a map.
   *
   * @param request HTTP servlet request
   * @return map of parameter names to value arrays
   */
  private Map<String, String[]> extractRequestParameters(HttpServletRequest request) {
    Map<String, String[]> parameters = new HashMap<>();

    Enumeration<String> parameterNames = request.getParameterNames();
    while (parameterNames.hasMoreElements()) {
      String paramName = parameterNames.nextElement();
      String[] paramValues = request.getParameterValues(paramName);
      parameters.put(paramName, paramValues);
    }

    return parameters;
  }

  /**
   * Builds the base URL for the application.
   *
   * <p>Format: {scheme}://{serverName}:{port}{contextPath}</p>
   * <p>Example: https://example.com:8080/etendo</p>
   *
   * @param request HTTP servlet request
   * @return base URL string
   */
  private String buildBaseUrl(HttpServletRequest request) {
    StringBuilder baseUrl = new StringBuilder();

    // Scheme (http or https)
    String scheme = request.getScheme();
    baseUrl.append(scheme).append("://");

    // Server name
    baseUrl.append(request.getServerName());

    // Port (only if non-standard)
    int port = request.getServerPort();
    boolean isDefaultPort = ("http".equals(scheme) && port == 80) ||
        ("https".equals(scheme) && port == 443);

    if (!isDefaultPort) {
      baseUrl.append(":").append(port);
    }

    // Context path
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty()) {
      baseUrl.append(contextPath);
    }

    return baseUrl.toString();
  }

  /**
   * Detects if the request is an AJAX request.
   *
   * <p>Checks for:</p>
   * <ul>
   *   <li>X-Requested-With: XMLHttpRequest header (jQuery, Prototype)</li>
   *   <li>Accept: application/json header</li>
   * </ul>
   *
   * @param request HTTP servlet request
   * @return true if AJAX request
   */
  private boolean isAjaxRequest(HttpServletRequest request) {
    // Check X-Requested-With header (standard AJAX indicator)
    String requestedWith = request.getHeader("X-Requested-With");
    if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
      return true;
    }

    // Check Accept header for JSON
    String accept = request.getHeader("Accept");
    if (accept != null && accept.contains("application/json")) {
      return true;
    }

    return false;
  }

  @Override
  public int getPriority() {
    return PRIORITY;
  }

  @Override
  public String getName() {
    return "RequestContextFilter";
  }

  @Override
  public int getTimeout() {
    return TIMEOUT_SECONDS;
  }
}
