package com.etendoerp.base.filter.core;

/**
 * Exception thrown by filter implementations during execution.
 *
 * <p>Supports HTTP status codes to indicate the type of error that occurred,
 * allowing the FilterChainCoordinator to return appropriate HTTP responses.</p>
 *
 * @since Etendo 24.Q4
 */
public class FilterException extends Exception {

  private static final long serialVersionUID = 1L;

  private final int httpStatusCode;

  /**
   * Creates a new FilterException with a message and HTTP 500 (Internal Server Error) status.
   *
   * @param message Error message describing what went wrong
   */
  public FilterException(String message) {
    this(message, 500);
  }

  /**
   * Creates a new FilterException with a message and specific HTTP status code.
   *
   * @param message Error message describing what went wrong
   * @param httpStatusCode HTTP status code (e.g., 400, 401, 500, 503)
   */
  public FilterException(String message, int httpStatusCode) {
    super(message);
    this.httpStatusCode = httpStatusCode;
  }

  /**
   * Creates a new FilterException with a message, cause, and HTTP 500 status.
   *
   * @param message Error message describing what went wrong
   * @param cause The underlying exception that caused this error
   */
  public FilterException(String message, Throwable cause) {
    this(message, 500, cause);
  }

  /**
   * Creates a new FilterException with a message, HTTP status code, and cause.
   *
   * @param message Error message describing what went wrong
   * @param httpStatusCode HTTP status code (e.g., 400, 401, 500, 503)
   * @param cause The underlying exception that caused this error
   */
  public FilterException(String message, int httpStatusCode, Throwable cause) {
    super(message, cause);
    this.httpStatusCode = httpStatusCode;
  }

  /**
   * Returns the HTTP status code associated with this exception.
   *
   * <p>Common status codes:</p>
   * <ul>
   *   <li>400 - Bad Request (invalid input)</li>
   *   <li>401 - Unauthorized (authentication failed)</li>
   *   <li>403 - Forbidden (access denied)</li>
   *   <li>429 - Too Many Requests (rate limit exceeded)</li>
   *   <li>500 - Internal Server Error (unexpected error)</li>
   *   <li>503 - Service Unavailable (timeout, temporary failure)</li>
   * </ul>
   *
   * @return HTTP status code
   */
  public int getHttpStatusCode() {
    return httpStatusCode;
  }

  @Override
  public String toString() {
    return String.format("FilterException[status=%d, message=%s]",
        httpStatusCode, getMessage());
  }
}
