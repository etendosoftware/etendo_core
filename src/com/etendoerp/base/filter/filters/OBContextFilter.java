package com.etendoerp.base.filter.filters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.etendoerp.base.filter.core.FilterContext;
import com.etendoerp.base.filter.core.FilterException;
import com.etendoerp.base.filter.core.FilterExecutor;
import org.openbravo.dal.core.OBContext;

import javax.servlet.http.HttpSession;

/**
 * Core filter for OBContext initialization and cleanup.
 *
 * <p>Manages the OBContext thread-local state that provides access to user,
 * role, organization, and other contextual information throughout request processing.</p>
 *
 * <p><strong>Lifecycle:</strong></p>
 * <ol>
 *   <li><strong>execute():</strong> Initialize OBContext from session</li>
 *   <li><em>(Other filters and business logic use OBContext)</em></li>
 *   <li><strong>cleanup():</strong> Clear thread-local OBContext state</li>
 * </ol>
 *
 * <p><strong>Priority:</strong> 20 (executes after transaction management)</p>
 *
 * @since Etendo 24.Q4
 */
public class OBContextFilter implements FilterExecutor {

  private static final Logger log = LogManager.getLogger(OBContextFilter.class);

  private static final String ATTR_OBCONTEXT_INITIALIZED = "obcontext.initialized";
  private static final String ATTR_OBCONTEXT_ADMIN_MODE = "obcontext.adminMode";

  private static final String SESSION_ATTR_USER = "#Authenticated_user";
  private static final String SESSION_ATTR_ROLE = "#AD_Role_ID";
  private static final String SESSION_ATTR_ORG = "#AD_Org_ID";
  private static final String SESSION_ATTR_CLIENT = "#AD_Client_ID";

  private static final int PRIORITY = 20;
  private static final int TIMEOUT_SECONDS = 30;

  @Override
  public void execute(FilterContext context) throws FilterException {
    log.debug("[{}] Initializing OBContext", context.getRequestId());

    try {
      // Get HTTP session
      HttpSession session = context.getRequest().getSession(false);
      if (session == null) {
        log.debug("[{}] No HTTP session - skipping OBContext initialization",
            context.getRequestId());
        return;
      }

      // Check if admin mode should be enabled
      boolean adminMode = shouldEnableAdminMode(context);

      if (adminMode) {
        log.debug("[{}] Enabling admin mode for system path: {}",
            context.getRequestId(), context.getPath());
        OBContext.setAdminMode();
      } else {
        // Initialize from session
        String userId = (String) session.getAttribute(SESSION_ATTR_USER);
        String roleId = (String) session.getAttribute(SESSION_ATTR_ROLE);
        String orgId = (String) session.getAttribute(SESSION_ATTR_ORG);
        String clientId = (String) session.getAttribute(SESSION_ATTR_CLIENT);

        if (userId != null && roleId != null && clientId != null && orgId != null) {
          // Set user context using the static method with IDs
          OBContext.setOBContext(userId, roleId, clientId, orgId);

          log.debug("[{}] OBContext initialized (user={}, role={}, org={}, client={})",
              context.getRequestId(), userId, roleId, orgId, clientId);

        } else {
          log.debug("[{}] Session incomplete (user={}, role={}, org={}, client={}) - partial context",
              context.getRequestId(), userId != null, roleId != null, orgId != null, clientId != null);
        }
      }

      context.setAttribute(ATTR_OBCONTEXT_INITIALIZED, true);
      context.setAttribute(ATTR_OBCONTEXT_ADMIN_MODE, adminMode);

      log.debug("[{}] OBContext setup completed (adminMode={})",
          context.getRequestId(), adminMode);

    } catch (Exception e) {
      log.error("[{}] Failed to initialize OBContext", context.getRequestId(), e);
      throw new FilterException("Failed to initialize OBContext: " + e.getMessage(), 500);
    }
  }

  @Override
  public void cleanup(FilterContext context, boolean errorOccurred) {
    Boolean initialized = context.getAttribute(ATTR_OBCONTEXT_INITIALIZED, Boolean.class);

    if (!Boolean.TRUE.equals(initialized)) {
      log.trace("[{}] OBContext was not initialized - skipping cleanup",
          context.getRequestId());
      return;
    }

    try {
      Boolean adminMode = context.getAttribute(ATTR_OBCONTEXT_ADMIN_MODE, Boolean.class);

      log.debug("[{}] Cleaning up OBContext (adminMode={}, error={})",
          context.getRequestId(), adminMode, errorOccurred);

      // Reset admin mode if it was enabled
      if (Boolean.TRUE.equals(adminMode)) {
        OBContext.restorePreviousMode();
      }

      // Remove OBContext from thread-local (use explicit cast to avoid ambiguity)
      OBContext.setOBContext((OBContext) null);

      log.debug("[{}] OBContext cleanup completed", context.getRequestId());

    } catch (Exception e) {
      log.error("[{}] Error during OBContext cleanup", context.getRequestId(), e);
      // Don't throw - cleanup must complete
    } finally {
      // Clear context attributes
      context.setAttribute(ATTR_OBCONTEXT_INITIALIZED, false);
      context.removeAttribute(ATTR_OBCONTEXT_ADMIN_MODE);
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

  /**
   * Determines if admin mode should be enabled for the request.
   *
   * @param context filter context
   * @return true if admin mode should be enabled
   */
  private boolean shouldEnableAdminMode(FilterContext context) {
    String path = context.getPath();

    return path.startsWith("/system/") ||
        path.startsWith("/admin/") ||
        path.startsWith("/background/") ||
        path.startsWith("/health/") ||
        path.equals("/health");
  }

  @Override
  public int getPriority() {
    return PRIORITY;
  }

  @Override
  public String getName() {
    return "OBContextFilter";
  }

  @Override
  public int getTimeout() {
    return TIMEOUT_SECONDS;
  }
}
