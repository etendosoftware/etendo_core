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
 * All portions are Copyright (C) 2017-2019 Openbravo SLU 
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.base.mock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.HttpSessionWrapper;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.test.base.HiddenObjectHelper;

/**
 * HttpServletRequest mock to be used to test components requiring requests
 */
public class HttpServletRequestMock implements HttpServletRequest {
  private static final Logger log = LogManager.getLogger();

  private HttpSession session;

  /**
   * Creates a new HttpServletRequestMock instance and sets it in RequestContext together with the
   * VariablesSecureApp instance received as parameter, so it can be used as a mock.
   */
  public static void setRequestMockInRequestContext(VariablesSecureApp vars) {
    try {
      RequestContext reqCtx = RequestContext.get();
      HiddenObjectHelper.set(reqCtx, "variablesSecureApp", vars);
      HiddenObjectHelper.set(reqCtx, "request", new HttpServletRequestMock());
    } catch (Exception e) {
      log.error("Error setting mock vars in context", e);
    }
  }

  /**
   * Creates a new HttpServletRequestMock instance and sets it in RequestContext so it can be used
   * as a mock.
   */
  public static void setRequestMockInRequestContext() {
    setRequestMockInRequestContext(new VariablesSecureAppMock());
  }

  /**
   * Create a mock for HttpServletRequest setting some basic configuration parameters in session.
   */
  public HttpServletRequestMock() {
    session = new HttpSessionWrapper();
    Properties props = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    session.setAttribute("#AD_JAVADATEFORMAT", props.getProperty("dateFormat.java"));
    session.setAttribute("#AD_JAVADATETIMEFORMAT", props.getProperty("dateTimeFormat.java"));
  }

  @Override
  public AsyncContext getAsyncContext() {
    return null;
  }

  @Override
  public Object getAttribute(String arg0) {
    return null;
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    return null;
  }

  @Override
  public String getCharacterEncoding() {
    return null;
  }

  @Override
  public int getContentLength() {
    return 0;
  }

  @Override
  public String getContentType() {
    return null;
  }

  @Override
  public DispatcherType getDispatcherType() {
    return null;
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    return null;
  }

  @Override
  public String getLocalAddr() {
    return null;
  }

  @Override
  public String getLocalName() {
    return null;
  }

  @Override
  public int getLocalPort() {
    return 0;
  }

  @Override
  public Locale getLocale() {
    return null;
  }

  @Override
  public Enumeration<Locale> getLocales() {
    return null;
  }

  @Override
  public String getParameter(String arg0) {
    return null;
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    return null;
  }

  @Override
  public Enumeration<String> getParameterNames() {
    return null;
  }

  @Override
  public String[] getParameterValues(String arg0) {
    return null;
  }

  @Override
  public String getProtocol() {
    return null;
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return null;
  }

  @SuppressWarnings("deprecation")
  @Override
  public String getRealPath(String arg0) {
    return null;
  }

  @Override
  public String getRemoteAddr() {
    return null;
  }

  @Override
  public String getRemoteHost() {
    return null;
  }

  @Override
  public int getRemotePort() {
    return 0;
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String arg0) {
    return null;
  }

  @Override
  public String getScheme() {
    return null;
  }

  @Override
  public String getServerName() {
    return null;
  }

  @Override
  public int getServerPort() {
    return 0;
  }

  @Override
  public ServletContext getServletContext() {
    return null;
  }

  @Override
  public boolean isAsyncStarted() {
    return false;
  }

  @Override
  public boolean isAsyncSupported() {
    return false;
  }

  @Override
  public boolean isSecure() {
    return false;
  }

  @Override
  public void removeAttribute(String arg0) {

  }

  @Override
  public void setAttribute(String arg0, Object arg1) {

  }

  @Override
  public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {

  }

  @Override
  public AsyncContext startAsync() {
    return null;
  }

  @Override
  public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) {
    return null;
  }

  @Override
  public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException {
    return false;
  }

  @Override
  public String getAuthType() {
    return null;
  }

  @Override
  public String getContextPath() {
    return null;
  }

  @Override
  public Cookie[] getCookies() {
    return null;
  }

  @Override
  public long getDateHeader(String arg0) {
    return 0;
  }

  @Override
  public String getHeader(String arg0) {
    return null;
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    return null;
  }

  @Override
  public Enumeration<String> getHeaders(String arg0) {
    return null;
  }

  @Override
  public int getIntHeader(String arg0) {
    return 0;
  }

  @Override
  public String getMethod() {
    return null;
  }

  @Override
  public Part getPart(String arg0) throws IOException, IllegalStateException, ServletException {
    return null;
  }

  @Override
  public Collection<Part> getParts() throws IOException, IllegalStateException, ServletException {
    return null;
  }

  @Override
  public String getPathInfo() {
    return null;
  }

  @Override
  public String getPathTranslated() {
    return null;
  }

  @Override
  public String getQueryString() {
    return null;
  }

  @Override
  public String getRemoteUser() {
    return null;
  }

  @Override
  public String getRequestURI() {
    return null;
  }

  @Override
  public StringBuffer getRequestURL() {
    return null;
  }

  @Override
  public String getRequestedSessionId() {
    return null;
  }

  @Override
  public String getServletPath() {
    return null;
  }

  @Override
  public HttpSession getSession() {
    return session;
  }

  @Override
  public HttpSession getSession(boolean arg0) {
    return null;
  }

  @Override
  public Principal getUserPrincipal() {
    return null;
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    return false;
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean isRequestedSessionIdFromUrl() {
    return false;
  }

  @Override
  public boolean isRequestedSessionIdValid() {
    return false;
  }

  @Override
  public boolean isUserInRole(String arg0) {
    return false;
  }

  @Override
  public void login(String arg0, String arg1) throws ServletException {
  }

  @Override
  public void logout() throws ServletException {
  }

  // methods bellow this line were introduced in servlet-api 3.1
  @Override
  public long getContentLengthLong() {
    return 0;
  }

  @Override
  public String changeSessionId() {
    return null;
  }

  @Override
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0)
      throws IOException, ServletException {
    return null;
  }
}
