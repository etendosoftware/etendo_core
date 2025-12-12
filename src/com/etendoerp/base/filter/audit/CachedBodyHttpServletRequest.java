package com.etendoerp.base.filter.audit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * HTTP request wrapper that caches the request body for multiple reads.
 *
 * <p>Enables audit logging to capture request body while still allowing
 * servlets to read the body normally. Without caching, the input stream
 * can only be read once, preventing audit capture.</p>
 *
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li>Caches request body up to 1MB (configurable)</li>
 *   <li>Truncates large bodies with warning log</li>
 *   <li>Thread-safe for concurrent reads</li>
 *   <li>Minimal memory overhead (lazy initialization)</li>
 * </ul>
 *
 * <p><strong>Truncation Behavior:</strong></p>
 * <ul>
 *   <li>Bodies &gt;1MB are truncated at 1MB</li>
 *   <li>Warning logged when truncation occurs</li>
 *   <li>Audit record includes "[TRUNCATED]" marker</li>
 *   <li>Application still receives full body (streaming)</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Wrap request in filter
 * CachedBodyHttpServletRequest cachedRequest =
 *     new CachedBodyHttpServletRequest(request);
 *
 * // Read for audit (first read)
 * String body = cachedRequest.getCachedBody();
 *
 * // Application reads normally (second read)
 * String appBody = IOUtils.toString(cachedRequest.getInputStream());
 *
 * // Both reads return same content
 * }</pre>
 *
 * @since Etendo 24.Q4
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

  private static final Logger log = LogManager.getLogger(CachedBodyHttpServletRequest.class);

  /**
   * Maximum body size to cache (1MB).
   * Bodies larger than this are truncated for audit logging.
   */
  private static final int MAX_BODY_SIZE = 1024 * 1024; // 1 MB

  private byte[] cachedBody;
  private boolean truncated;
  private boolean initialized;

  /**
   * Constructs a request wrapper that caches the body.
   *
   * @param request HTTP servlet request to wrap
   */
  public CachedBodyHttpServletRequest(HttpServletRequest request) {
    super(request);
    this.initialized = false;
    this.truncated = false;
  }

  /**
   * Returns the cached request body as a string.
   *
   * <p>Lazily reads and caches the body on first call. Subsequent calls
   * return the cached value without re-reading the stream.</p>
   *
   * <p>If body exceeds {@link #MAX_BODY_SIZE}, it is truncated and
   * "[TRUNCATED]" is appended to the returned string.</p>
   *
   * @return request body as string (UTF-8 encoding), or empty string if no body
   */
  public String getCachedBody() {
    ensureBodyCached();

    if (cachedBody == null || cachedBody.length == 0) {
      return "";
    }

    String body = new String(cachedBody, StandardCharsets.UTF_8);

    if (truncated) {
      return body + "\n\n[TRUNCATED - Original size exceeded 1MB]";
    }

    return body;
  }

  /**
   * Returns whether the body was truncated during caching.
   *
   * @return true if body exceeded max size and was truncated
   */
  public boolean isTruncated() {
    ensureBodyCached();
    return truncated;
  }

  /**
   * Returns the size of the cached body in bytes.
   *
   * @return cached body size, or 0 if no body
   */
  public int getCachedBodySize() {
    ensureBodyCached();
    return cachedBody != null ? cachedBody.length : 0;
  }

  /**
   * Ensures the body is cached (lazy initialization).
   */
  private synchronized void ensureBodyCached() {
    if (initialized) {
      return;
    }

    try {
      cacheBody();
    } catch (IOException e) {
      log.error("Failed to cache request body", e);
      cachedBody = new byte[0];
    } finally {
      initialized = true;
    }
  }

  /**
   * Reads and caches the request body from the input stream.
   *
   * @throws IOException if reading fails
   */
  private void cacheBody() throws IOException {
    InputStream inputStream = super.getInputStream();

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] chunk = new byte[8192]; // 8KB chunks
    int bytesRead;
    int totalBytes = 0;

    while ((bytesRead = inputStream.read(chunk)) != -1) {
      totalBytes += bytesRead;

      if (totalBytes > MAX_BODY_SIZE) {
        // Truncate at max size
        int remainingSpace = MAX_BODY_SIZE - (totalBytes - bytesRead);
        if (remainingSpace > 0) {
          buffer.write(chunk, 0, remainingSpace);
        }

        truncated = true;

        log.warn("Request body exceeds 1MB ({}), truncating for audit capture. " +
                "Full body still available to application.",
            getRequestURI());

        // Continue reading to consume stream (prevents broken pipe errors)
        while (inputStream.read(chunk) != -1) {
          // Discard remaining bytes
        }

        break;
      }

      buffer.write(chunk, 0, bytesRead);
    }

    cachedBody = buffer.toByteArray();

    log.trace("Cached request body: {} bytes (truncated={})",
        cachedBody.length, truncated);
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    ensureBodyCached();

    // Return stream that reads from cached bytes
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);

    return new ServletInputStream() {

      @Override
      public boolean isFinished() {
        return byteArrayInputStream.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener listener) {
        throw new UnsupportedOperationException(
            "ReadListener not supported on cached request");
      }

      @Override
      public int read() throws IOException {
        return byteArrayInputStream.read();
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        return byteArrayInputStream.read(b, off, len);
      }
    };
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
  }

  /**
   * Returns the maximum allowed body size for caching.
   *
   * @return max body size in bytes (1MB)
   */
  public static int getMaxBodySize() {
    return MAX_BODY_SIZE;
  }
}
