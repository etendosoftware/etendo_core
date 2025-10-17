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

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;

/**
 * HttpSession mock to be used to test components requiring an HTTP session
 */
public class HttpSessionMock implements HttpSession {

  private String id;
  private ServletContext servletContext;
  private Map<String, Object> attributes = new HashMap<String, Object>();
  private boolean invalid = false;
  private int maxInactiveInterval = 60;
  private int lastAccessedTime = -1;

  public HttpSessionMock() {
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
  public long getCreationTime() {
    return 0;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public long getLastAccessedTime() {
    return lastAccessedTime;
  }

  @Override
  public int getMaxInactiveInterval() {
    return maxInactiveInterval;
  }

  @Override
  public ServletContext getServletContext() {
    return servletContext;
  }

  // Note: HttpSessionContext and related deprecated methods (getValue, getValueNames, putValue, removeValue) 
  // have been completely removed from Jakarta Servlet API and are no longer supported

  @Override
  public void invalidate() {
    attributes.clear();
    invalid = true;
  }

  @Override
  public boolean isNew() {
    return false;
  }

  @Override
  public void removeAttribute(String name) {
    attributes.remove(name);
  }

  @Override
  public void setAttribute(String name, Object value) {
    if (value == null) {
      removeAttribute(name);
    } else {
      attributes.put(name, value);
    }
  }

  @Override
  public void setMaxInactiveInterval(int seconds) {
    maxInactiveInterval = seconds;
  }

  public boolean isInvalid() {
    return invalid;
  }

  public void access() {
    lastAccessedTime = (int) System.currentTimeMillis();
  }

}
