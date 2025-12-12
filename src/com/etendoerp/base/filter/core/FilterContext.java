package com.etendoerp.base.filter.core;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Execution context for a single request through the filter chain.
 *
 * <p>Provides access to request/response objects and a shared attribute map
 * for filters to communicate state. Thread-safe for concurrent attribute access.</p>
 *
 * @since Etendo 24.Q4
 */
public class FilterContext {

  private final String requestId;
  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private final Map<String, Object> attributes;
  private final long startTime;

  private FilterLifecycleState lifecycleState;
  private volatile boolean errorOccurred;

  /**
   * Creates a new FilterContext for a request.
   *
   * @param request HTTP servlet request (must not be null)
   * @param response HTTP servlet response (must not be null)
   */
  public FilterContext(HttpServletRequest request, HttpServletResponse response) {
    this.requestId = UUID.randomUUID().toString();
    this.request = Objects.requireNonNull(request, "request must not be null");
    this.response = Objects.requireNonNull(response, "response must not be null");
    this.attributes = new ConcurrentHashMap<>();
    this.startTime = System.currentTimeMillis();
    this.lifecycleState = FilterLifecycleState.INITIALIZED;
    this.errorOccurred = false;
  }

  /**
   * Returns the unique identifier for this request.
   *
   * <p>Used for log correlation across filters and asynchronous operations.</p>
   *
   * @return UUID string (never null)
   */
  public String getRequestId() {
    return requestId;
  }

  /**
   * Returns the HTTP servlet request.
   *
   * @return request object (never null)
   */
  public HttpServletRequest getRequest() {
    return request;
  }

  /**
   * Returns the HTTP servlet response.
   *
   * @return response object (never null)
   */
  public HttpServletResponse getResponse() {
    return response;
  }

  /**
   * Returns the request URI path.
   *
   * <p>Convenience method equivalent to {@code getRequest().getRequestURI()}.</p>
   *
   * @return URI path (e.g., "/api/invoices/123")
   */
  public String getPath() {
    return request.getRequestURI();
  }

  /**
   * Returns the HTTP method.
   *
   * <p>Convenience method equivalent to {@code getRequest().getMethod()}.</p>
   *
   * @return HTTP method (e.g., "GET", "POST")
   */
  public String getMethod() {
    return request.getMethod();
  }

  /**
   * Stores an attribute in the request-scoped context.
   *
   * <p>Attributes are shared across all filters in the chain. Use for
   * passing state between filters (e.g., transaction flags, timing data).</p>
   *
   * <p>Thread-safe: Multiple filters can access attributes concurrently.</p>
   *
   * @param key attribute name (must not be null)
   * @param value attribute value (null removes the attribute)
   */
  public void setAttribute(String key, Object value) {
    Objects.requireNonNull(key, "key must not be null");
    if (value == null) {
      attributes.remove(key);
    } else {
      attributes.put(key, value);
    }
  }

  /**
   * Retrieves an attribute from the context.
   *
   * @param key attribute name (must not be null)
   * @return attribute value or null if not present
   */
  public Object getAttribute(String key) {
    Objects.requireNonNull(key, "key must not be null");
    return attributes.get(key);
  }

  /**
   * Retrieves an attribute with type casting.
   *
   * @param key attribute name
   * @param type expected type class
   * @param <T> attribute type
   * @return typed attribute value or null if not present or wrong type
   */
  @SuppressWarnings("unchecked")
  public <T> T getAttribute(String key, Class<T> type) {
    Object value = getAttribute(key);
    return type.isInstance(value) ? (T) value : null;
  }

  /**
   * Removes an attribute from the context.
   *
   * @param key attribute name
   * @return previous value or null if not present
   */
  public Object removeAttribute(String key) {
    Objects.requireNonNull(key, "key must not be null");
    return attributes.remove(key);
  }

  /**
   * Checks if an attribute exists.
   *
   * @param key attribute name
   * @return true if attribute is present (even if value is null)
   */
  public boolean hasAttribute(String key) {
    Objects.requireNonNull(key, "key must not be null");
    return attributes.containsKey(key);
  }

  /**
   * Returns the current lifecycle state.
   *
   * @return current state (never null)
   */
  public FilterLifecycleState getLifecycleState() {
    return lifecycleState;
  }

  /**
   * Updates the lifecycle state.
   *
   * <p>Internal use by FilterChainCoordinator only.</p>
   *
   * @param lifecycleState new state (must not be null)
   */
  public void setLifecycleState(FilterLifecycleState lifecycleState) {
    this.lifecycleState = Objects.requireNonNull(lifecycleState);
  }

  /**
   * Returns the request start timestamp.
   *
   * @return milliseconds since epoch
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * Returns the elapsed time since request start.
   *
   * @return duration in milliseconds
   */
  public long getElapsedTime() {
    return System.currentTimeMillis() - startTime;
  }

  /**
   * Checks if an error occurred during filter execution.
   *
   * @return true if any filter threw an exception
   */
  public boolean isErrorOccurred() {
    return errorOccurred;
  }

  /**
   * Marks that an error occurred.
   *
   * <p>Internal use by FilterChainCoordinator only.</p>
   */
  public void setErrorOccurred() {
    this.errorOccurred = true;
  }

  /**
   * Returns a string representation for logging.
   *
   * @return formatted string with request ID, method, path, and state
   */
  @Override
  public String toString() {
    return String.format("FilterContext[id=%s, %s %s, state=%s, error=%s]",
        requestId, getMethod(), getPath(), lifecycleState, errorOccurred);
  }
}
