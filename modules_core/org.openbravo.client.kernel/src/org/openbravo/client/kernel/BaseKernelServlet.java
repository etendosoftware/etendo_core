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
 * All portions are Copyright (C) 2009-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

import org.openbravo.base.secureApp.HttpSecureAppServlet;

/**
 * A servlet base class which handles redirects and other base tasks.
 * 
 * @author mtaal
 */
public abstract class BaseKernelServlet extends HttpSecureAppServlet {
  protected static final String RESPONSE_HEADER_ETAG = "ETag";
  protected static final String RESPONSE_HEADER_LASTMODIFIED = "Last-Modified";
  protected static final String RESPONSE_HEADER_CACHE_CONTROL = "Cache-Control";
  protected static final String RESPONSE_NO_CACHE = "no-cache";
  protected static final String RESPONSE_HEADER_CONTENTTYPE = "Content-Type";

  private static final long serialVersionUID = 1L;

  @Override
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @Override
  public void service(final HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // encapsulate the response to catch any redirects
    // redirects are done by the authentication manager
    final KernelHttpServletResponse localResponse = new KernelHttpServletResponse(response);
    RequestContext.get().setResponse(localResponse);

    callServiceInSuper(request, localResponse);

    if (localResponse.isDoLogout()) {
      logout(request, localResponse);
    }

    if (localResponse.getRedirectTarget() != null) {
      if (!response.isCommitted()) {
        // get where the request originated
        String referer = RequestContext.get().getRequest().getHeader("referer");
        // if we can't get where the request originated, redirect to the ERP
        if (referer == null) {
          referer = localResponse.getRedirectTarget();
        }
        // will this always work.... not if the writer is already closed
        response.getWriter().write("window.location.href = '" + referer + "';");
        response.setHeader("Content-Type", KernelConstants.JAVASCRIPT_CONTENTTYPE);
        // prevent caching
        response.setHeader(RESPONSE_HEADER_ETAG, "" + System.currentTimeMillis());
        response.setDateHeader(RESPONSE_HEADER_LASTMODIFIED, System.currentTimeMillis());
        response.setHeader(RESPONSE_HEADER_CACHE_CONTROL, RESPONSE_NO_CACHE);
      }
    } else {
      response.setStatus(200);
    }
  }

  protected void callServiceInSuper(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    super.service(request, response);
  }

  @Override
  protected void invalidateSession(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
  }

  public static class KernelHttpServletResponse extends HttpServletResponseWrapper {
    private String redirectTarget = null;
    private boolean doLogout = false;

    public KernelHttpServletResponse(HttpServletResponse response) {
      super(response);
    }

    @Override
    public void sendRedirect(String arg0) throws IOException {
      setRedirectTarget(arg0);
    }

    public boolean isDoLogout() {
      return doLogout;
    }

    public void setDoLogout(boolean doLogout) {
      this.doLogout = doLogout;
    }

    public String getRedirectTarget() {
      return redirectTarget;
    }

    public void setRedirectTarget(String redirectTarget) {
      this.redirectTarget = redirectTarget;
    }
  }
}
