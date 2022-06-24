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
 * All portions are Copyright (C) 2010-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.window.servlet;

import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class CalloutServletConfig implements ServletConfig {

  private String servletName;
  private ServletContext context;

  public CalloutServletConfig(String servletName, ServletContext context) {
    this.servletName = servletName;
    this.context = context;
  }

  @Override
  public String getInitParameter(String name) {
    return context.getInitParameter(name);
  }

  @Override
  public Enumeration<String> getInitParameterNames() {
    return context.getInitParameterNames();
  }

  @Override
  public ServletContext getServletContext() {
    return context;
  }

  @Override
  public String getServletName() {
    return servletName;
  }

}
