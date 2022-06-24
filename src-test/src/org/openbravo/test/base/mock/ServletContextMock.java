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
 * All portions are Copyright (C) 2019 Openbravo SLU 
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.base.mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * ServletContext mock to be used to test components requiring the web application context.
 */
public class ServletContextMock implements ServletContext {

  private static final Logger log = LogManager.getLogger();

  private static final int SERVLET_API_MAJOR_VERSION = 3;
  private static final int SERVLET_API_MINOR_VERSION = 1;

  private final Map<String, Object> attributes = new HashMap<String, Object>();
  private final Map<String, String> initParameters = new HashMap<String, String>();
  private final Map<String, String> mimeTypes = new HashMap<String, String>();
  private File webAppRoot;

  /**
   * MockServletContext base constructor.
   * 
   * @param contextPath
   *          the path of the context root directory
   * @param tmpPath
   *          the path of the temporary working directory
   */
  public ServletContextMock(String contextPath, String tmpPath) {
    Optional<File> optWebAppRoot = getDirectory(contextPath);
    if (optWebAppRoot.isPresent()) {
      webAppRoot = optWebAppRoot.get();
    } else {
      log.warn("The webapp root directory is invalid: " + contextPath);
    }

    Optional<File> optTmpDir = getDirectory(tmpPath);
    if (optTmpDir.isPresent()) {
      attributes.put("javax.servlet.context.tempdir", optTmpDir.get());
    }

    initMimeTypes();
  }

  private Optional<File> getDirectory(String path) {
    if (path == null) {
      return Optional.empty();
    }
    File file = new File(path);
    if (!file.exists() || !file.isDirectory()) {
      return Optional.empty();
    }
    return Optional.of(file);
  }

  private void initMimeTypes() {
    mimeTypes.put("html", "text/html");
    mimeTypes.put("css", "text/css");
    mimeTypes.put("xml", "text/xml");
    mimeTypes.put("js", "text/javascript");
    mimeTypes.put("jrxml", "application/jrxml");
    mimeTypes.put("pdf", "application/pdf");
    mimeTypes.put("gif", "image/gif");
    mimeTypes.put("jpg", "image/jpeg");
    mimeTypes.put("png", "image/png");
  }

  /**
   * Adds an initialization parameter.
   * 
   * @param name
   *          The name of the initialization parameter.
   * @param value
   *          The value for the initialization parameter.
   */
  protected void addInitParameter(String name, String value) {
    initParameters.put(name, value);
  }

  @Override
  public Object getAttribute(String name) {
    return attributes.get(name);
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    return Collections.enumeration(attributes.keySet());
  }

  @Override
  public ServletContext getContext(String name) {
    return this;
  }

  @Override
  public String getInitParameter(String name) {
    return initParameters.get(name);
  }

  @Override
  public Enumeration<String> getInitParameterNames() {
    return Collections.enumeration(initParameters.keySet());
  }

  @Override
  public int getMajorVersion() {
    return SERVLET_API_MAJOR_VERSION;
  }

  @Override
  public String getMimeType(String name) {
    int index = name.lastIndexOf('.');
    if (index == -1 || index == (name.length() - 1)) {
      return null;
    } else {
      return mimeTypes.get(name.substring(index + 1));
    }
  }

  @Override
  public int getMinorVersion() {
    return SERVLET_API_MINOR_VERSION;
  }

  @Override
  public RequestDispatcher getNamedDispatcher(String name) {
    return getRequestDispatcher(name);
  }

  @Override
  public String getRealPath(String name) {
    File file = getFile(name);
    return file != null && file.exists() ? file.getPath() : null;
  }

  protected File getFile(String name) {
    if (webAppRoot == null) {
      return null;
    }
    String fileName = name.startsWith("/") ? name.substring(1) : name;
    return new File(webAppRoot, fileName);
  }

  /**
   * @return a dummy implementation of a RequestDispatcher.
   */
  @Override
  public RequestDispatcher getRequestDispatcher(String name) {
    return new RequestDispatcher() {
      @Override
      public void forward(ServletRequest servletRequest, ServletResponse servletResponse)
          throws IOException {
        // NOOP
      }

      @Override
      public void include(ServletRequest servletRequest, ServletResponse servletResponse)
          throws IOException {
        // NOOP
      }
    };
  }

  @Override
  public URL getResource(String name) throws MalformedURLException {
    File file = getFile(name);
    return file.exists() ? file.toURI().toURL() : null;
  }

  @Override
  public InputStream getResourceAsStream(String name) {
    File file = getFile(name);
    try {
      return new FileInputStream(file);
    } catch (FileNotFoundException e) {
      return null;
    }
  }

  @Override
  public Set<String> getResourcePaths(String name) {
    if (webAppRoot == null) {
      return Collections.emptySet();
    }

    File file = new File(webAppRoot, name);

    if (!file.exists() || !file.isDirectory()) {
      return Collections.emptySet();
    }

    return Arrays.stream(file.listFiles())
        .map(resource -> getRelativePath(resource))
        .collect(Collectors.toSet());
  }

  private String getRelativePath(File file) {
    int stripLength = webAppRoot.getPath().length();
    String path = file.getPath().substring(stripLength).replace('\\', '/');
    return file.isDirectory() ? path + "/" : path;
  }

  @Override
  public String getServerInfo() {
    return "";
  }

  @SuppressWarnings("deprecation")
  @Override
  public Servlet getServlet(String name) throws ServletException {
    return null;
  }

  @Override
  public String getServletContextName() {
    return "ServletContextMock";
  }

  @SuppressWarnings("deprecation")
  @Override
  public Enumeration<String> getServletNames() {
    return null;
  }

  @SuppressWarnings("deprecation")
  @Override
  public Enumeration<Servlet> getServlets() {
    return null;
  }

  @SuppressWarnings("deprecation")
  @Override
  public void log(Exception e, String msg) {
  }

  @Override
  public void log(String msg) {
    log.info(msg);
  }

  @Override
  public void log(String msg, Throwable cause) {
    log.error(msg, cause);
  }

  @Override
  public void removeAttribute(String name) {
    attributes.remove(name);
  }

  @Override
  public void setAttribute(String name, Object o) {
    attributes.put(name, o);
  }

  @Override
  public String getContextPath() {
    return "";
  }

  @Override
  public Dynamic addFilter(String filterName, String className) {
    return null;
  }

  @Override
  public Dynamic addFilter(String filterName, Filter filter) {
    return null;
  }

  @Override
  public Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
    return null;
  }

  @Override
  public void addListener(String className) {
  }

  @Override
  public <T extends EventListener> void addListener(T listener) {
  }

  @Override
  public void addListener(Class<? extends EventListener> eventListener) {
  }

  @Override
  public javax.servlet.ServletRegistration.Dynamic addServlet(String servletName,
      String className) {
    return null;
  }

  @Override
  public javax.servlet.ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
    return null;
  }

  @Override
  public javax.servlet.ServletRegistration.Dynamic addServlet(String servletName,
      Class<? extends Servlet> servletClass) {
    return null;
  }

  @Override
  public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
    return null;
  }

  @Override
  public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
    return null;
  }

  @Override
  public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
    return null;
  }

  @Override
  public void declareRoles(String... roleNames) {
  }

  @Override
  public ClassLoader getClassLoader() {
    return null;
  }

  @Override
  public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
    return null;
  }

  @Override
  public int getEffectiveMajorVersion() {
    return 0;
  }

  @Override
  public int getEffectiveMinorVersion() {
    return 0;
  }

  @Override
  public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
    return null;
  }

  @Override
  public FilterRegistration getFilterRegistration(String filterName) {
    return null;
  }

  @Override
  public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
    return null;
  }

  @Override
  public JspConfigDescriptor getJspConfigDescriptor() {
    return null;
  }

  @Override
  public ServletRegistration getServletRegistration(String servletName) {
    return null;
  }

  @Override
  public Map<String, ? extends ServletRegistration> getServletRegistrations() {
    return null;
  }

  @Override
  public SessionCookieConfig getSessionCookieConfig() {
    return null;
  }

  @Override
  public boolean setInitParameter(String name, String value) {
    return false;
  }

  @Override
  public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
  }

  // methods bellow this line were introduced in servlet-api 3.1
  @Override
  public String getVirtualServerName() {
    return null;
  }

}
