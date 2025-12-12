package com.etendoerp.base.filter.filters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.etendoerp.base.filter.core.FilterContext;
import com.etendoerp.base.filter.core.FilterException;
import com.etendoerp.base.filter.core.FilterExecutor;
import org.openbravo.database.SessionInfo;

import javax.servlet.http.HttpSession;

/**
 * Core filter for session information management.
 *
 * <p>Manages SessionInfo thread-local state for database audit tracking.</p>
 *
 * <p><strong>Lifecycle:</strong></p>
 * <ol>
 *   <li><strong>execute():</strong> Initialize SessionInfo for database auditing</li>
 *   <li><em>(Database operations use SessionInfo for audit)</em></li>
 *   <li><strong>cleanup():</strong> Clear SessionInfo thread-local state</li>
 * </ol>
 *
 * <p><strong>Priority:</strong> 30 (executes after OBContext setup)</p>
 *
 * @since Etendo 24.Q4
 */
public class SessionInfoFilter implements FilterExecutor {

  private static final Logger log = LogManager.getLogger(SessionInfoFilter.class);

  private static final String ATTR_SESSION_INFO_INITIALIZED = "sessionInfo.initialized";
  private static final String SESSION_ATTR_USER = "#Authenticated_user";

  private static final int PRIORITY = 30;
  private static final int TIMEOUT_SECONDS = 30;

  @Override
  public void execute(FilterContext context) throws FilterException {
    log.debug("[{}] Initializing SessionInfo", context.getRequestId());

    try {
      // Get HTTP session
      HttpSession httpSession = context.getRequest().getSession(false);
      if (httpSession == null) {
        log.debug("[{}] No HTTP session - skipping SessionInfo initialization",
            context.getRequestId());
        return;
      }

      // Get user ID from session
      String userId = (String) httpSession.getAttribute(SESSION_ATTR_USER);

      // Set SessionInfo for database audit tracking
      if (userId != null) {
        SessionInfo.setUserId(userId);
        SessionInfo.setProcessType("HTTP"); // HTTP request
        SessionInfo.setProcessId(context.getMethod()); // GET, POST, etc.
        SessionInfo.setSessionId(httpSession.getId());

        log.debug("[{}] SessionInfo initialized (user={}, session={})",
            context.getRequestId(), userId, httpSession.getId());
      } else {
        log.debug("[{}] No user in session - SessionInfo not set", context.getRequestId());
      }

      context.setAttribute(ATTR_SESSION_INFO_INITIALIZED, true);

    } catch (Exception e) {
      log.error("[{}] Failed to initialize SessionInfo", context.getRequestId(), e);
      throw new FilterException("Failed to initialize SessionInfo: " + e.getMessage(), 500);
    }
  }

  @Override
  public void cleanup(FilterContext context, boolean errorOccurred) {
    Boolean initialized = context.getAttribute(ATTR_SESSION_INFO_INITIALIZED, Boolean.class);

    if (!Boolean.TRUE.equals(initialized)) {
      log.trace("[{}] SessionInfo was not initialized - skipping cleanup",
          context.getRequestId());
      return;
    }

    try {
      log.debug("[{}] Cleaning up SessionInfo (error={})",
          context.getRequestId(), errorOccurred);

      // SessionInfo uses thread-local storage that gets cleared automatically
      // by the connection pool, but we can explicitly mark it as changed
      SessionInfo.infoChanged();

      log.debug("[{}] SessionInfo cleanup completed", context.getRequestId());

    } catch (Exception e) {
      log.error("[{}] Error during SessionInfo cleanup", context.getRequestId(), e);
      // Don't throw - cleanup must complete
    } finally {
      context.setAttribute(ATTR_SESSION_INFO_INITIALIZED, false);
    }
  }

  @Override
  public boolean shouldExecute(FilterContext context) {
    // Execute for all requests except static resources
    String path = context.getPath();

    if (path.endsWith(".js") || path.endsWith(".css") ||
        path.endsWith(".png") || path.endsWith(".jpg") ||
        path.endsWith(".gif") || path.endsWith(".ico") ||
        path.endsWith(".svg") || path.endsWith(".woff") ||
        path.endsWith(".woff2") || path.endsWith(".ttf")) {
      return false;
    }

    return true;
  }

  @Override
  public int getPriority() {
    return PRIORITY;
  }

  @Override
  public String getName() {
    return "SessionInfoFilter";
  }

  @Override
  public int getTimeout() {
    return TIMEOUT_SECONDS;
  }
}
