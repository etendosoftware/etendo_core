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
 * All portions are Copyright (C) 2009-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ReadListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;

/**
 * The request context serves multiple purposes:
 * <ul>
 * <li>Provides the request and response object through a ThreadLocal. This means that these do not
 * have to passed as parameters to methods.</li>
 * <li>If there is no request/response object available then Mock versions are used. This makes it
 * possible to run logic in a test environment outside of the servlet container.</li>
 * <li>Makes it possible to set request parameters to simulate a request from a client to a
 * server.</li>
 * </ul>
 * 
 * @author mtaal
 */
public class RequestContext {
  private static ServletContext servletContext = null;

  private static ThreadLocal<RequestContext> instance = new ThreadLocal<RequestContext>();

  public static ServletContext getServletContext() {
    if (servletContext == null) {
      throw new OBException("Servlet Context is null");
    }
    return servletContext;
  }

  public static void setServletContext(ServletContext aServletContext) {
    servletContext = aServletContext;
  }

  public static RequestContext get() {
    if (instance.get() == null) {
      instance.set(new RequestContext());
    }
    return instance.get();
  }

  public static void clear() {
    instance.remove();
  }

  private VariablesSecureApp variablesSecureApp;
  private HttpServletRequest request;
  private String requestContent = null;
  private HttpServletResponse response;

  public String getRequestContent() {
    if (requestContent != null) {
      return requestContent;
    }

    try {
      final BufferedReader reader = getRequest().getReader();
      if (reader == null) {
        return "";
      }
      String line;
      final StringBuilder sb = new StringBuilder();
      while ((line = reader.readLine()) != null) {
        if (sb.length() > 0) {
          sb.append("\n");
        }
        sb.append(line);
      }
      requestContent = sb.toString();
      return requestContent;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  public String getRequestParameter(String name) {
    HttpServletRequest localRequest = getRequest();
    if (localRequest == null) {
      // there is no request (ie. working in a background process)
      return null;
    } else {
      return localRequest.getParameter(name);
    }
  }

  public Map<?, ?> getParameterMap() {
    return getRequest().getParameterMap();
  }

  public void setRequestParameter(String name, String value) {
    ((HttpServletRequestWrapper) getRequest()).setParameter(name, value);
  }

  public Object getSessionAttribute(String name) {
    return getSession().getAttribute(name.toUpperCase());
  }

  public Enumeration<?> getSessionAttributeNames() {
    return getSession().getAttributeNames();
  }

  public void setSessionAttribute(String name, Object value) {
    getSession().setAttribute(name.toUpperCase(), value);
  }

  public void removeSessionAttribute(String name) {
    getSession().removeAttribute(name.toUpperCase());
  }

  public HttpSession getSession() {
    return getRequest().getSession();
  }

  public HttpServletRequest getRequest() {
    if (request != null && !(request instanceof HttpServletRequestWrapper)) {
      final HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request);
      request = wrapper;
    }
    return request;
  }

  public void setRequestContent(String requestContent) {
    HttpServletRequestWrapper req = (HttpServletRequestWrapper) getRequest();
    if (req != null) {
      req.setRequestContent(requestContent);
    }
  }

  public VariablesSecureApp getVariablesSecureApp() {
    if (variablesSecureApp != null) {
      return variablesSecureApp;
    }
    if (request == null) {
      throw new OBException("No request object set");
    }
    variablesSecureApp = new VariablesSecureApp(getRequest());
    return variablesSecureApp;
  }

  public void setVariableSecureApp(VariablesSecureApp variablesSecureApp) {
    this.variablesSecureApp = variablesSecureApp;
  }

  public void setRequest(HttpServletRequest request) {
    variablesSecureApp = null;
    this.request = request;
  }

  /**
   * This method makes only sense to be invoked from a Request in a servlet container, other cases
   * response will be null
   * 
   */
  public HttpServletResponse getResponse() {
    return response;
  }

  public void setResponse(HttpServletResponse response) {
    this.response = response;
  }

  public static class HttpServletRequestWrapper
      extends javax.servlet.http.HttpServletRequestWrapper {

    private HttpServletRequest delegate;
    private Map<String, Object> attributes = new HashMap<String, Object>();
    private Map<String, String[]> parameters = new HashMap<String, String[]>();
    private HttpSession localSession = null;
    private String requestContent;
    private ServletInputStream inputStream;

    public HttpServletRequestWrapper(HttpServletRequest request) {
      super(request);
      delegate = request;
    }

    public void setParameter(String arg0, String arg1) {
      parameters.put(arg0, new String[] { arg1 });
    }

    @Override
    public Object getAttribute(String arg0) {
      if (delegate == null) {
        return attributes.get(arg0);
      }

      if (arg0 != null && arg0.startsWith("org.apache.catalina.core") && getRequest() != null) {
        // Retrieving internal Tomcat attributes, manage it as special case. Ie. dispatcher type is
        // treated internally in a different manner and requires to be obtained from the request
        // rather than from the delegate.
        return getRequest().getAttribute(arg0);
      }

      return delegate.getAttribute(arg0);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
      if (delegate == null) {
        final List<String> names = new ArrayList<String>();
        names.addAll(attributes.keySet());
        return Collections.enumeration(names);
      }
      return delegate.getAttributeNames();
    }

    @Override
    public String getContentType() {
      if (delegate == null) {
        return "text/html";
      }
      return delegate.getContentType();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      if (delegate == null) {
        if (inputStream == null) {
          if (requestContent == null) {
            inputStream = new ServletInputStream() {
              @Override
              public int read() throws IOException {
                return 0;
              }

              // methods bellow this line were introduced in servlet-api 3.1
              @Override
              public boolean isFinished() {
                return true;
              }

              @Override
              public boolean isReady() {
                return true;
              }

              @Override
              public void setReadListener(ReadListener arg0) {
              }
            };
          } else {
            LocalServletInputStream servletInputStream = new LocalServletInputStream();
            servletInputStream.setContent(requestContent);
            inputStream = servletInputStream;
          }
        }
        return inputStream;
      }
      return delegate.getInputStream();
    }

    @Override
    public String getParameter(String arg0) {
      if (delegate == null || parameters.containsKey(arg0)) {
        final String[] param = parameters.get(arg0);
        if (param == null || param.length == 0) {
          return null;
        }
        return param[0];
      }
      return delegate.getParameter(arg0);
    }

    @Override
    public Enumeration<String> getParameterNames() {
      final List<String> names = new ArrayList<String>();
      names.addAll(parameters.keySet());
      if (delegate != null) {
        names.addAll(delegate.getParameterMap().keySet());
      }
      return Collections.enumeration(names);
    }

    @Override
    public String[] getParameterValues(String arg0) {
      if (delegate == null || parameters.containsKey(arg0)) {
        return parameters.get(arg0);
      }
      return delegate.getParameterValues(arg0);
    }

    @Override
    public String getRequestURI() {
      if (delegate == null) {
        return "";
      }
      return delegate.getRequestURI();
    }

    @Override
    public HttpSession getSession() {
      if (delegate == null) {
        if (localSession == null) {
          localSession = new HttpSessionWrapper();
        }
        return localSession;
      }
      return delegate.getSession();
    }

    @Override
    public HttpSession getSession(boolean arg0) {
      if (delegate == null) {
        if (localSession == null && !arg0) {
          return null;
        }
        return getSession();
      }
      return delegate.getSession(arg0);
    }

    @Override
    public void removeAttribute(String arg0) {
      if (delegate == null) {
        attributes.remove(arg0);
        return;
      }
      delegate.removeAttribute(arg0);
    }

    @Override
    public void setAttribute(String arg0, Object arg1) {
      if (delegate == null) {
        attributes.put(arg0, arg1);
        return;
      }
      delegate.setAttribute(arg0, arg1);
    }

    public HttpServletRequest getDelegate() {
      return delegate;
    }

    public String getRequestContent() {
      return requestContent;
    }

    public void setRequestContent(String requestContent) {
      this.requestContent = requestContent;
    }
  }

  public static class HttpSessionWrapper extends org.openbravo.base.HttpSessionWrapper {
    @Override
    public ServletContext getServletContext() {
      return servletContext;
    }
  }

  private static class LocalServletInputStream extends ServletInputStream {
    private ByteArrayInputStream localInputStream;

    public void setContent(String content) {
      try {
        localInputStream = new ByteArrayInputStream(content.getBytes("UTF-8"));
      } catch (Exception e) {
        throw new OBException(e);
      }
    }

    @Override
    public int available() {
      return localInputStream.available();
    }

    @Override
    public void close() throws IOException {
      localInputStream.close();
    }

    @Override
    public boolean equals(Object obj) {
      return localInputStream.equals(obj);
    }

    @Override
    public int hashCode() {
      return localInputStream.hashCode();
    }

    @Override
    public void mark(int readAheadLimit) {
      localInputStream.mark(readAheadLimit);
    }

    @Override
    public boolean markSupported() {
      return localInputStream.markSupported();
    }

    @Override
    public int read() {
      return localInputStream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) {
      return localInputStream.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
      return localInputStream.read(b);
    }

    @Override
    public void reset() {
      localInputStream.reset();
    }

    @Override
    public long skip(long n) {
      return localInputStream.skip(n);
    }

    @Override
    public String toString() {
      return localInputStream.toString();
    }

    // methods bellow this line were introduced in servlet-api 3.1
    @Override
    public boolean isFinished() {
      return false;
    }

    @Override
    public boolean isReady() {
      return false;
    }

    @Override
    public void setReadListener(ReadListener arg0) {
    }
  }

}
