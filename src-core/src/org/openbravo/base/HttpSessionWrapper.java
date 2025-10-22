/*
 ************************************************************************************
 * Copyright (C) 2014-2018 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */

package org.openbravo.base;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;

/**
 * Implements a fake HttpSession to be used when running out of a Servlet container (such as Quartz
 * scheduler or jUnit)
 * 
 * @author alostale
 *
 */
public class HttpSessionWrapper implements HttpSession {
  private Map<String, Object> attributes = new HashMap<String, Object>();

  @Override
  public Object getAttribute(String arg0) {
    return attributes.get(arg0);
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
    return null;
  }

  @Override
  public long getLastAccessedTime() {
    return 0;
  }

  @Override
  public int getMaxInactiveInterval() {
    return 0;
  }

  @Override
  public ServletContext getServletContext() {
    return null;
  }

  // Note: HttpSessionContext and related deprecated methods (getValue, getValueNames, putValue, removeValue) 
  // have been completely removed from Jakarta Servlet API and are no longer supported

  @Override
  public void invalidate() {
  }

  @Override
  public boolean isNew() {
    return false;
  }

  @Override
  public void removeAttribute(String arg0) {
    setAttribute(arg0, null);
  }

  @Override
  public void setAttribute(String arg0, Object arg1) {
    attributes.put(arg0, arg1);
  }

  @Override
  public void setMaxInactiveInterval(int arg0) {
  }
}
