/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2009-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.HttpBaseUtils;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.portal.PortalAccessible;
import org.openbravo.service.web.WebServiceUtil;

/**
 * The main servlet responsible for handling all the requests for components from the system.
 * 
 * @author mtaal
 */
public class KernelServlet extends BaseKernelServlet {
  // private static final Logger log = LogManager.getLogger();
  private static final Logger log = LogManager.getLogger();

  // this is needed to support logout deep in the code...
  // TODO: make it easier to get to the authentication manager from
  // the
  public static final String KERNEL_SERVLET = "kernelServletInstance";

  private static final String REQUEST_HEADER_IFMODIFIEDSINCE = "If-Modified-Since";
  private static final String REQUEST_HEADER_IFNONEMATCH = "If-None-Match";
  private static final String RESPONSE_HEADER_WINDOW_CREATE_ERROR = "window_create_error";

  private static final long serialVersionUID = 1L;

  private static String servletPathPart = "org.openbravo.client.kernel";

  // are used to compute the relative path
  private static ConfigParameters kernelServletglobalParameters;

  private static ServletContext servletContext;

  /**
   * @return the parameters as they are defined in the servlet context.
   */
  public static ConfigParameters getGlobalParameters() {
    return kernelServletglobalParameters;
  }

  public static String getServletPathPart() {
    return servletPathPart;
  }

  // the inc and dec by passauthentication count must be synchronized
  // and static, there might be multiple kernelservlets and multiple threads
  // may use the same kernelservlet
  private static synchronized void incBypassAuthenticationCount(HttpServletRequest request) {
    HttpSession session = request.getSession(true);
    OBContext context = OBContext.getOBContext();
    boolean sessionForThisRequest = (context == null
        || session.getAttribute("#Authenticated_user") == null)
        && !"Y".equals(session.getAttribute("#LOGGINGIN"));

    if (sessionForThisRequest) {
      session.setAttribute("forceLogin", "Y");
    }

    if (session != null && "Y".equals(session.getAttribute("forceLogin"))) {
      // session has been created to retrieve a non authenticated component, it might be several
      // non authenticated components sharing the same session, count them to invalidate the
      // session after all of them are done
      Integer count = (Integer) session.getAttribute("forcedSessionsRequestCount");
      if (count == null || count == 0) {
        count = 1;
      } else {
        count += 1;
      }
      session.setAttribute("forcedSessionsRequestCount", count);
      log.warn("The KernelServlet should not be used for unauthenticated access (this request url: "
          + request.getRequestURL() + " - " + request.getQueryString()
          + "). This functionality is deprecated, "
          + "use 'org.openbravo.mobile.core' instead of 'org.openbravo.client.kernel'; "
          + "see this issue https://issues.openbravo.com/view.php?id=27248 for more information");
    }
  }

  private static synchronized void decBypassAuthenticationCount(HttpSession session,
      HttpServletRequest request) {
    if (session != null && "Y".equals(session.getAttribute("forceLogin"))) {
      Integer count = (Integer) session.getAttribute("forcedSessionsRequestCount");
      count = (count != null ? count : 0) - 1;

      if (count <= 0) {
        session.invalidate();
        log.debug("Invalidating session created for bypass authentication elements");
      } else {
        session.setAttribute("forcedSessionsRequestCount", count);
      }
      log.warn("The KernelServlet should not be used for unauthenticated access (this request url: "
          + request.getRequestURL() + " - " + request.getQueryString()
          + "). This functionality is deprecated, "
          + "use 'org.openbravo.mobile.core' instead of 'org.openbravo.client.kernel'; "
          + "see this issue https://issues.openbravo.com/view.php?id=27248 for more information");
    }
  }

  @Inject
  @Any
  private Instance<ComponentProvider> componentProviders;

  @Inject
  private WeldUtils weldUtils;

  @Override
  public void init(ServletConfig config) {
    super.init(config);
    kernelServletglobalParameters = ConfigParameters.retrieveFrom(config.getServletContext());
    servletContext = config.getServletContext();
  }

  @Override
  public void service(final HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean bypassAuthentication = false;
    doOptions(request, response);
    final String action = request.getParameter(KernelConstants.ACTION_PARAMETER);
    if (action == null) {
      Component component = getComponent(request);

      if (component instanceof BaseComponent
          && ((BaseComponent) component).bypassAuthentication()) {
        bypassAuthentication = true;
        incBypassAuthenticationCount(request);
      }
    }

    super.service(request, response);

    if (bypassAuthentication) {
      HttpSession session = request.getSession(false);
      decBypassAuthenticationCount(session, request);
    }
  }

  @Override
  public void doOptions(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    setCORSHeaders(request, response);
  }

  protected void setCORSHeaders(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    String origin = request.getHeader("Origin");

    if (origin != null && !origin.equals("")) {
      response.setHeader("Access-Control-Allow-Origin", origin);
      response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
      response.setHeader("Access-Control-Allow-Headers",
          "Content-Type, origin, accept, X-Requested-With");
      response.setHeader("Access-Control-Max-Age", "1000");
    }
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    if (!request.getRequestURI().contains("/" + servletPathPart)) {
      throw new UnsupportedOperationException("Invalid url " + request.getRequestURI());
    }

    final String action = request.getParameter(KernelConstants.ACTION_PARAMETER);
    if (action != null) {
      processActionRequest(request, response);
    } else {
      processComponentRequest(request, response);
    }
  }

  // NOTE: this exact same method is present in the MobileCoreComponentServlet in the mobile core
  // module, if changed here, please also check the method in the MobileCoreComponentServlet class
  private Component getComponent(HttpServletRequest request) {

    final int nameIndex = request.getRequestURI().indexOf(servletPathPart);
    final String servicePart = request.getRequestURI().substring(nameIndex);
    final String[] pathParts = WebServiceUtil.getInstance().getSegments(servicePart);
    if (pathParts.length < 2) {
      throw new UnsupportedOperationException(
          "No service name present in url " + request.getRequestURI());
    }
    final String componentProviderName = pathParts[1];

    final ComponentProvider componentProvider = componentProviders
        .select(new ComponentProvider.Selector(componentProviderName))
        .get();

    final String componentId;
    if (pathParts.length > 2) {
      componentId = pathParts[2];
    } else {
      componentId = null;
    }

    final Map<String, Object> parameters = getParameterMap(request);
    final Component component = componentProvider.getComponent(componentId, parameters);
    return component;
  }

  // NOTE: this exact same method is present in the MobileCoreComponentServlet in the mobile core
  // module, if changed here, please also check the method in the MobileCoreComponentServlet class
  protected void processComponentRequest(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    Component component = getComponent(request);
    OBContext.setAdminMode();
    String eTag;
    try {
      eTag = component.getETag();
    } finally {
      OBContext.restorePreviousMode();
    }
    final String requestETag = request.getHeader(REQUEST_HEADER_IFNONEMATCH);

    if (requestETag != null && eTag.equals(requestETag)) {
      response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
      response.setDateHeader(RESPONSE_HEADER_LASTMODIFIED,
          request.getDateHeader(REQUEST_HEADER_IFMODIFIEDSINCE));
      return;
    }

    try {
      final String result = ComponentGenerator.getInstance().generate(component);

      response.setHeader(RESPONSE_HEADER_ETAG, eTag);
      response.setDateHeader(RESPONSE_HEADER_LASTMODIFIED, component.getLastModified().getTime());
      response.setContentType(component.getContentType());
      response.setHeader(RESPONSE_HEADER_CONTENTTYPE, component.getContentType());
      response.setHeader(RESPONSE_HEADER_CACHE_CONTROL, RESPONSE_NO_CACHE);

      final PrintWriter pw = response.getWriter();
      pw.write(result);
      pw.close();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      if (!response.isCommitted()) {
        response.setContentType(KernelConstants.JAVASCRIPT_CONTENTTYPE);
        response.setHeader(RESPONSE_HEADER_CONTENTTYPE, KernelConstants.JAVASCRIPT_CONTENTTYPE);
        response.setHeader(RESPONSE_HEADER_CACHE_CONTROL, RESPONSE_NO_CACHE);
        response.setHeader(RESPONSE_HEADER_WINDOW_CREATE_ERROR, "true");
        response.getWriter().write(KernelUtils.getInstance().createErrorJavaScript(e));
      }
    }
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    doGet(request, response);
  }

  protected void processActionRequest(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    final String action = request.getParameter(KernelConstants.ACTION_PARAMETER);

    response.setContentType(KernelConstants.JAVASCRIPT_CONTENTTYPE);
    response.setHeader(RESPONSE_HEADER_CONTENTTYPE, KernelConstants.JAVASCRIPT_CONTENTTYPE);
    response.setHeader(RESPONSE_HEADER_CACHE_CONTROL, RESPONSE_NO_CACHE);

    try {
      @SuppressWarnings("unchecked")
      final Class<ActionHandler> actionHandlerClass = (Class<ActionHandler>) OBClassLoader
          .getInstance()
          .loadClass(action);
      final ActionHandler actionHandler = weldUtils.getInstance(actionHandlerClass);

      if (OBContext.getOBContext() != null && OBContext.getOBContext().isPortalRole()) {
        if (!(actionHandler instanceof PortalAccessible)) {
          log.error("Portal user " + OBContext.getOBContext().getUser() + " with role "
              + OBContext.getOBContext().getRole()
              + " is trying to access to non granted action handler " + request.getRequestURL()
              + "?" + request.getQueryString());
          throw new OBSecurityException("Portal role has no access to this handler");
        }
      }

      actionHandler.execute();
    } catch (Exception e) {
      log.error("Error executing action " + action + " error: " + e.getMessage(), e);
      if (!response.isCommitted()) {
        response.setContentType(KernelConstants.JAVASCRIPT_CONTENTTYPE);
        response.setHeader(RESPONSE_HEADER_CONTENTTYPE, KernelConstants.JAVASCRIPT_CONTENTTYPE);
        response.setHeader(RESPONSE_HEADER_CACHE_CONTROL, RESPONSE_NO_CACHE);
        response.getWriter().write(KernelUtils.getInstance().createErrorJavaScript(e));
      }
    }

  }

  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    throw new UnsupportedOperationException("Only GET/POST is supported");
  }

  @Override
  public void doPut(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    throw new UnsupportedOperationException("Only GET/POST is supported");
  }

  @SuppressWarnings("rawtypes")
  private Map<String, Object> getParameterMap(HttpServletRequest request) {
    final Map<String, Object> parameterMap = new HashMap<String, Object>();
    for (Enumeration keys = request.getParameterNames(); keys.hasMoreElements();) {
      final String key = (String) keys.nextElement();
      parameterMap.put(key, request.getParameter(key));
    }

    if (!parameterMap.containsKey(KernelConstants.HTTP_SESSION)) {
      parameterMap.put(KernelConstants.HTTP_SESSION, request.getSession());
    }

    if (!parameterMap.containsKey(KernelConstants.CONTEXT_URL)) {
      parameterMap.put(KernelConstants.CONTEXT_URL, computeContextURL(request));
    }

    if (!parameterMap.containsKey(KernelConstants.SERVLET_CONTEXT)) {
      parameterMap.put(KernelConstants.SERVLET_CONTEXT, servletContext);
    }

    if (!parameterMap.containsKey(KernelConstants.SKIN_PARAMETER)) {
      parameterMap.put(KernelConstants.SKIN_PARAMETER, KernelConstants.SKIN_DEFAULT);
    }

    return parameterMap;
  }

  private String computeContextURL(HttpServletRequest request) {
    return HttpBaseUtils.getLocalAddress(request);
  }
}
